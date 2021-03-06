/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.monitoring.profiling;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.monitoring.profiling.predictors.IAlgorithmProfilePredictor;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.observables.IObservable;

/**
 * Handles multiple predictors for various observables (separately, not integrated via the predictor).
 * 
 * @author Christopher Voges
 * @author Holger Eichelberger
 */
class SeparateObservableAlgorithmProfile implements IAlgorithmProfile {

    private static final Logger LOGGER = LogManager.getLogger(AlgorithmProfilePredictionManager.class);
    private Map<IObservable, IAlgorithmProfilePredictor> predictors = new HashMap<>();
    
    private PipelineElement element;
    private Map<Object, Serializable> key;
    
    /**
     * Generates an empty {@link SeparateObservableAlgorithmProfile}.
     * 
     * @param element the pipeline element
     * @param key the profile key
     */
    public SeparateObservableAlgorithmProfile(PipelineElement element, Map<Object, Serializable> key) {
        this.element = element;
        this.key = key;
    }
    
    /**
     * Generates a string key (identifier) based on the attributes.
     * 
     * @param observable the observable to be predicted
     * 
     * @return The key representing this {@link SeparateObservableAlgorithmProfile} instance in its 
     *     current configuration.
     */
    private String generateKey(IObservable observable) {
        boolean profiling = element.isInProfilingMode();
        String pipelineName = element.getPipeline().getName();
        String elementName = element.getName();
        String algorithm = keyToString(Constants.KEY_ALGORITHM);
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<Object, Serializable> ent : key.entrySet()) {
            String key = ent.getKey().toString();
            if (!Constants.KEY_ALGORITHM.equals(key)) {
                sorted.put(key, ent.getValue().toString());
            }
        }
        String key;
        if (profiling) {
            key = "";
        } else {
            key = "pipeline=" + pipelineName + ":element=" + elementName + ":";
        }
        key += "algorithm=" + algorithm + ":predicted=" + observable.name() + ";parameters=" + sorted;
        return key;
    }
    
    /**
     * Turns a key part into a string.
     * 
     * @param part the key part
     * @return the string representation
     */
    private String keyToString(Object part) {
        Serializable tmp = key.get(part);
        return null == tmp ? "" : tmp.toString();
    }
    
    @Override
    public void store() {
        for (Map.Entry<IObservable, IAlgorithmProfilePredictor> ent : predictors.entrySet()) {
            try {
                // this is not really efficient
                store(ent.getValue(), element.getPath(), generateKey(ent.getKey()));
            } catch (IOException e) {
                LOGGER.error("While writing profile: " + e.getMessage());
            }
        }
    }
    
    @Override
    public File getFolder(IObservable observable) {
        return getFolder(element.getPath(), generateKey(observable));
    }
    
    /**
     * Returns the folder for a predictor.
     * 
     * @param path the base path
     * @param identifier the profile identifier
     * @return the folder
     */
    private File getFolder(String path, String identifier) {
        File folder = new File(path);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(";")[0].split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        // set kind of the predictor as subfolder
        String subfolder = element.getProfileCreator().getStorageSubFolder();
        return new File(folder, subfolder);
    }
    
    /**
     * Stores a given predictor.
     * 
     * @param predictor the predictor
     * @param path the target path for persisting the predictor instances
     * @param identifier the predictor identifier
     * @throws IOException if saving the predictor fails
     */
    void store(IAlgorithmProfilePredictor predictor, String path, String identifier) throws IOException {
        File folder = getFolder(path, identifier);
        
        // Create folders, if needed
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // load map-file
        MapFile mapFile = new MapFile(folder);
        mapFile.load();
        
        boolean newEntry = false;
        int id = mapFile.get(identifier);
        if (id < 0) {
            id = mapFile.size() + 1;
            newEntry = true;
        }

        File instanceFile = MapFile.getFile(folder, id);
        predictor.store(instanceFile, identifier);
        
        // update map-file, if needed
        if (newEntry) {
            mapFile.put(identifier, id);
            mapFile.store();
        } 
    }
    
    /**
     * Loads a predictor back if possible.
     * 
     * @param predictor the predictor to load into
     * @param path the target path for persisting the predictor instances
     * @param identifier the predictor identifier
     * @throws IOException if saving the predictor fails
     */
    void load(IAlgorithmProfilePredictor predictor, String path, String identifier) throws IOException {
        File folder = getFolder(path, identifier);
        MapFile mapFile = new MapFile(folder);
        mapFile.load();
        File instanceFile = mapFile.getFile(identifier);
        predictor.load(instanceFile, identifier);
    }

    /**
     * Obtains a predictor and creates one if permissible.
     * 
     * @param observable the observable to obtain a predictor for
     * @return the predictor
     */
    private IAlgorithmProfilePredictor obtainPredictor(IObservable observable) {
        IAlgorithmProfilePredictor predictor = predictors.get(observable);
        if (null == predictor && null != QuantizerRegistry.getQuantizer(observable)) {
            predictor = element.getProfileCreator().createPredictor();
            try {
                load(predictor, element.getPath(), generateKey(observable));
            } catch (IOException e) {
                LOGGER.error("While reading predictor: " + e.getMessage());
            }
            predictors.put(observable, predictor);
        }
        return predictor;
    }

    @Override
    public double predict(IObservable observable) {
        return predict(observable, 0); // TODO really 0?
    }

    @Override
    public double predict(IObservable observable, int steps) {
        double result;
        IAlgorithmProfilePredictor predictor = obtainPredictor(observable);
        if (null != predictor) {
            result = predictor.predict(steps);
        } else {
            result = Constants.NO_PREDICTION;
        }
        return result;
    }

    @Override
    public void update(PipelineNodeSystemPart family) {
        for (IObservable obs : family.getObservables()) {
            if (family.hasValue(obs)) {
                IAlgorithmProfilePredictor predictor = obtainPredictor(obs);
                if (null != predictor) {
                    predictor.update(family.getLastUpdate(obs) / 1000, family.getObservedValue(obs));
                }
            }
        }
    }

}
