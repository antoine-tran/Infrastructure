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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.monitoring.profiling.quantizers.DoubleIntegerQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.IdentityIntegerQuantizer;
import eu.qualimaster.monitoring.profiling.quantizers.Quantizer;
import eu.qualimaster.monitoring.profiling.quantizers.ScalingDoubleQuantizer;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * A registry for quantizers.
 * 
 * @author Holger Eichelberger
 */
public class QuantizerRegistry {
    
    private static final Map<Class<? extends Serializable>, Quantizer<?>> TYPE_QUANTIZERS = new HashMap<>();
    private static final Map<IObservable, Quantizer<Double>> OBSERVABLE_QUANTIZERS = new HashMap<>();
    private static final Map<IObservable, Integer> PREDICTION_STEPS = new HashMap<>();
    
    static {
        // observable quantizers
        registerQuantizer(TimeBehavior.LATENCY, ScalingDoubleQuantizer.INSTANCE); // ms
        registerQuantizer(TimeBehavior.THROUGHPUT_ITEMS, ScalingDoubleQuantizer.INSTANCE);
        registerQuantizer(Scalability.ITEMS, ScalingDoubleQuantizer.INSTANCE);
        registerQuantizer(ResourceUsage.EXECUTORS, DoubleIntegerQuantizer.INSTANCE);
        registerQuantizer(ResourceUsage.TASKS, DoubleIntegerQuantizer.INSTANCE);

        // type quantizers for parameters
        registerQuantizer(IdentityIntegerQuantizer.INSTANCE);
        registerQuantizer(DoubleIntegerQuantizer.INSTANCE);
    }
    
    /**
     * Returns the quantizer for an observable.
     * 
     * @param observable the observable
     * @return the {@link QuantizerRegistry} (may be <b>null</b> if there is none and the observable shall be ignored)
     */
    public static Quantizer<Double> getQuantizer(IObservable observable) {
        Quantizer<Double> result = null;
        if (null != observable) {
            result = OBSERVABLE_QUANTIZERS.get(observable);
        }
        return result;
    }
    
    /**
     * Returns the quantizer for a serializable (parameter value).
     * 
     * @param serializable the serializable
     * @return the quantizer (may be <b>null</b> if there is none and the parameter shall be ignored)
     */
    public static Quantizer<?> getQuantizer(Serializable serializable) {
        Quantizer<?> result = null;
        if (null != serializable) {
            result = TYPE_QUANTIZERS.get(serializable.getClass());
        }
        return result;
    }
    
    /**
     * Registers an observable quantizer.
     * 
     * @param observable the observable
     * @param quantizer the quantizer (<b>null</b> unregisters the quantizer)
     */
    public static void registerQuantizer(IObservable observable, Quantizer<Double> quantizer) {
        if (null != observable) {
            OBSERVABLE_QUANTIZERS.put(observable, quantizer);
        }
    }
    
    /**
     * Unregisters an observable quantizer.
     * 
     * @param observable the observable
     */
    public static void unregisterQuantizer(IObservable observable) {
        if (null != observable) {
            OBSERVABLE_QUANTIZERS.remove(observable);
        }
    }

    /**
     * Registers a given type quantizer.
     * 
     * @param quantizer the quantizer
     */
    public static void registerQuantizer(Quantizer<?> quantizer) {
        if (null != quantizer && null != quantizer.handles()) {
            TYPE_QUANTIZERS.put(quantizer.handles(), quantizer);
        }
    }

    /**
     * Unregisters a given type quantizer.
     * 
     * @param quantizer the quantizer
     */
    public static void unregisterQuantizer(Quantizer<?> quantizer) {
        if (null != quantizer && null != quantizer.handles()) {
            TYPE_QUANTIZERS.remove(quantizer.handles());
        }
    }

    /**
     * Returns the number of prediction steps to apply for <code>observable</code>.
     * 
     * @param observable the observable
     * @return the number of prediction steps
     */
    public static int getPredictionSteps(IObservable observable) {
        Integer result = null;
        if (null != observable) {
            result = PREDICTION_STEPS.get(observable);
        }
        if (null == result) {
            result = 0; 
        }
        return result;
    }
    
    /**
     * Sets the prediction steps to default.
     * 
     * @param observable the observable
     */
    public static void defaultPredictionSteps(IObservable observable) {
        registerPredictionSteps(observable, -1);
    }
    
    /**
     * Registers the desired number of prediction steps.
     * 
     * @param observable the observable
     * @param steps the number of steps (negative value sets back to default)
     */
    public static void registerPredictionSteps(IObservable observable, int steps) {
        if (null != observable) {
            if (steps < 0) {
                PREDICTION_STEPS.remove(observable);
            } else {
                PREDICTION_STEPS.put(observable, steps);
            }
        }
    }
    
}
