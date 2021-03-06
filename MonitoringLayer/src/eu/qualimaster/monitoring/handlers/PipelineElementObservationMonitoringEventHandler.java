/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.monitoring.handlers;

import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.events.PipelineElementObservationMonitoringEvent;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the handling of {@link PipelineElementObservationMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineElementObservationMonitoringEventHandler 
    extends MonitoringEventHandler<PipelineElementObservationMonitoringEvent> {

    public static final PipelineElementObservationMonitoringEventHandler INSTANCE 
        = new PipelineElementObservationMonitoringEventHandler();
    
    /**
     * Creates an event handler.
     */
    private PipelineElementObservationMonitoringEventHandler() {
        super(PipelineElementObservationMonitoringEvent.class);
    }

    @Override
    protected void handle(PipelineElementObservationMonitoringEvent event, SystemState state) {
        SystemPart target = determineAggregationPart(event, state);
        if (null != target) {
            IObservable observable = event.getObservable();
            if (!observable.isInternal()) {
                Object key = event.getKey();
                StateUtils.setValue(target, observable, event.getObservation(), key);
                if (StateUtils.changesLatency(observable)) {
                    StateUtils.updateCapacity(target, key, false);
                }
            }
        }
    }

}
