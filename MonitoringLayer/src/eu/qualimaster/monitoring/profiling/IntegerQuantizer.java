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

/**
 * An integer quantizer.
 * 
 * @author Holger Eichelberger
 */
public class IntegerQuantizer extends Quantizer<Integer> {

    public static final IntegerQuantizer TO_INT = new IntegerQuantizer(1);
    public static final IntegerQuantizer STEP_100 = new IntegerQuantizer(100);
    public static final IntegerQuantizer STEP_1000 = new IntegerQuantizer(1000);
    
    private int step;
    
    /**
     * Creates an integer quantizer.
     * 
     * @param step the quantization step (null or negative is ignored)
     */
    public IntegerQuantizer(int step) {
        super(Integer.class);
        this.step = Math.max(1, step);
    }

    @Override
    protected int quantizeImpl(Integer value) {
        return value.intValue() % step;
    }

}
