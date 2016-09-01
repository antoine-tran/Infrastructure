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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Stores all running pipelines.
 * 
 * @author Holger Eichelberger
 */
public class Pipelines {

    /**
     * Stores all active pipelines.
     */     
    private static Map<String, Pipeline> pipelines = new HashMap<String, Pipeline>();

    /**
     * Returns a pipeline object and creates a new one if the requested one is not known.
     * 
     * @param name the name of the pipeline
     * @return the pipeline
     */
    static Pipeline obtainPipeline(String name) {
        Pipeline pip = pipelines.get(name);
        if (null == pip) {
            pip = new Pipeline(name);
            pipelines.put(name, pip);
        }
        return pip;
    }

    /**
     * Returns a pipeline object.
     * 
     * @param name the name of the pipeline
     * @return the pipeline (may be <b>null</b> if not known)
     */
    static Pipeline getPipeline(String name) {
        return pipelines.get(name);
    }

    /**
     * Releases a pipeline object. If known, the pipeline will be cleared and it will not be known anymore after 
     * this method.
     * 
     * @param name the name of the pipeline
     * @return the pipeline (may be <b>null</b> if not known)
     * @see Pipeline#clear()
     */
    static Pipeline releasePipeline(String name) {
        Pipeline pip = pipelines.remove(name);
        if (null != pip) {
            pip.clear();
        }
        return pip;
    }
    
    /**
     * Releases all known pipelines.
     * 
     * @see #releasePipeline(String)
     */
    static void releaseAllPipelines() {
        Iterator<Pipeline> iter = pipelines.values().iterator();
        while (iter.hasNext()) {
            iter.next().clear();
            iter.remove();
        }
    }

}
