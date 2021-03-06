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
package eu.qualimaster.monitoring.storm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.easy.extension.QmConstants;
import eu.qualimaster.easy.extension.internal.PipelineHelper;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.ModelQuery;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.datatypes.IDatatype;
import backtype.storm.generated.Bolt;
import backtype.storm.generated.ComponentCommon;
import backtype.storm.generated.ExecutorInfo;
import backtype.storm.generated.ExecutorSummary;
import backtype.storm.generated.GlobalStreamId;
import backtype.storm.generated.Grouping;
import backtype.storm.generated.SpoutSpec;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;

/**
 * Utility functions, in particular for building up a pipeline topology.
 * 
 * @author Holger Eichelberger
 */
public class Utils {

    /**
     * Specializes {@link Processor} so that a pipeline topology can incrementally be created from Storm information.
     * 
     * @author Holger Eichelberger
     */
    private static class StormProcessor extends Processor {

        private boolean spout;
        
        /**
         * Creates a processor for Storm.
         * 
         * @param name the name
         * @param common the common instance carrying additional information
         * @param executor runtime information
         * @param spout is this a spout or a bolt?
         */
        protected StormProcessor(String name, ComponentCommon common, ExecutorSummary executor, boolean spout) {
            super(name, common.get_parallelism_hint(), toTasks(executor));
            this.spout = spout;
        }
        
        /**
         * Returns whether the processor is a Storm spout.
         * 
         * @return <code>true</code> for spout, <code>false</code> for bolt
         */
        @SuppressWarnings("unused")
        protected boolean isSpout() {
            return spout;
        }
        
        @Override
        protected void addOutput(Stream stream) {
            super.addOutput(stream);
        }

        @Override
        protected void addInput(Stream stream) {
            super.addInput(stream);
        }
        
        /**
         * Updates this processor according to the given executor instance, in particular regarding the 
         * number of tasks.
         * 
         * @param executor the executor
         */
        private void update(ExecutorSummary executor) {
            addTasks(toTasks(executor));
        }

    }
    
    /**
     * Returns whether an executor is considered to be Storm internal.
     * 
     * @param executor the executor
     * @return <code>true</code> if internal, <code>false</code> else
     */
    public static boolean isInternal(ExecutorSummary executor) {
        return isInternal(executor.get_component_id());
    }

    /**
     * Returns whether an executor name is considered to be Storm internal.
     * 
     * @param name the executor name
     * @return <code>true</code> if internal, <code>false</code> else
     */
    public static boolean isInternal(String name) {
        return name.startsWith("__"); // internal: ackers, system etc.
    }
    
    /**
     * Returns the tasks from the given <code>executor</code>.
     * 
     * @param executor the executor (may be <b>null</b>)
     * @return all task identifiers, may be <b>null</b> if <code>executor</code> is null
     */
    private static int[] toTasks(ExecutorSummary executor) {
        int[] result = null;
        if (null != executor) {
            ExecutorInfo info = executor.get_executor_info();
            int start = info.get_task_start();
            int end = info.get_task_end();
            result = new int[end - start + 1];
            for (int t = start; t <= end; t++) {
                result[t - start] = t;
            }
        }
        return result;
    }
    
    /**
     * Creates the streams for <code>common</code>.
     * 
     * @param procs already known processors
     * @param originName the name of the origin processor
     * @param common the common instance to create streams for
     * @param mapping the name mapping
     */
    private static void createStreams(Map<String, StormProcessor> procs, String originName, 
        ComponentCommon common, INameMapping mapping) {
        StormProcessor target = getProcessor(procs, originName);
        if (null != target) {
            for (Map.Entry<GlobalStreamId, Grouping> grouping : common.get_inputs().entrySet()) {
                GlobalStreamId id = grouping.getKey();
                StormProcessor origin = getProcessor(procs, mapName(mapping, id.get_componentId()));
                if (null != origin) {
                    Stream stream = new Stream(id.get_streamId(), origin, target);
                    origin.addOutput(stream);
                    target.addInput(stream);
                }
            }
        }
    }
    
    /**
     * Obtains a processor from <code>procs</code> via its name. If the component is not found, an error is logged.
     *
     * @param procs already known processors
     * @param processorName the name of the processor to return
     * @return the processor (may be <b>null</b>)
     */
    private static StormProcessor getProcessor(Map<String, StormProcessor> procs, String processorName) {
        StormProcessor result = procs.get(processorName);
        if (null == result && !isInternal(processorName)) {
            getLogger().error("No component found for '" + processorName + "'. Topology is inconsistent. Ignoring.");
        }
        return result;
    }
    
    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return Logger.getLogger(Utils.class);
    }

    /**
     * Creates processors for a set of <code>entries</code> (bolts or spouts).
     * 
     * @param <T> the entry type of <code>entries</code>
     * @param mapping the name mapping
     * @param executors the actual executors
     * @param entries the entries to process
     * @param procs the processors to modify as a side effect
     * @param spout are we processing spouts
     */
    private static <T> void createProcessors(INameMapping mapping, Map<String, ExecutorSummary> executors, 
        Set<Map.Entry<String, T>> entries, Map<String, StormProcessor> procs, boolean spout) {
        for (Map.Entry<String, T> entry : entries) {
            String name = mapName(mapping, entry);
            ExecutorSummary executor = getNonInternalExecutor(executors, entry);
            if (null != name && null != executor) { // avoid Storm internal streams for now
                ComponentCommon common = getComponentCommon(entry.getValue());
                if (null != common) {
                    StormProcessor proc = procs.get(name);
                    if (null != proc) {
                        proc.update(executor);
                    } else {
                        procs.put(name, new StormProcessor(name, common, executor, spout));
                    }
                }
            }
        }
    }
    
    /**
     * Creates streams for a set of <code>entries</code> (bolts or spouts).
     * 
     * @param <T> the entry type of <code>entries</code>
     * @param mapping the name mapping
     * @param entries the entries to process
     * @param procs the processors to modify as a side effect
     */
    private static <T> void createStreams(INameMapping mapping, Set<Map.Entry<String, T>> entries, 
        Map<String, StormProcessor> procs) {
        for (Map.Entry<String, T> entry : entries) {
            ComponentCommon common = getComponentCommon(entry.getValue());
            if (null != common) {
                createStreams(procs, mapName(mapping, entry), common, mapping);
            }
        }
    }
    
    /**
     * Returns a component common from an object. This (casting) methods is required, as spouts and bolts do 
     * not have a common interface and currently TBase is not available.
     * 
     * @param object the object to take the component common from
     * @return the component common or <b>null</b>
     */
    private static ComponentCommon getComponentCommon(Object object) {
        ComponentCommon result = null;
        if (object instanceof SpoutSpec) {
            result = ((SpoutSpec) object).get_common();
        } else if (object instanceof Bolt) {
            result = ((Bolt) object).get_common();
        }
        return result;
    }
    
    /**
     * Creates a pipeline topology from a Storm topology.
     * 
     * @param topo the static Storm topology information
     * @param topoInfo the dynamic Storm topology information
     * @param mapping the name mapping instance
     * @return the corresponding pipeline topology
     */
    public static PipelineTopology buildPipelineTopology(StormTopology topo, TopologyInfo topoInfo, 
        INameMapping mapping) {
        Map<String, ExecutorSummary> executors = collectExecutors(topoInfo);
        Map<String, StormProcessor> procs = new HashMap<String, StormProcessor>();
        // create the processors first, SpoutSpec and Bolt do not have a common interface to access ComponentCommon
        createProcessors(mapping, executors, topo.get_spouts().entrySet(), procs, true);
        createProcessors(mapping, executors, topo.get_bolts().entrySet(), procs, false);
        // create then the flows, assuming that the Storm topology is consistent
        createStreams(mapping, topo.get_spouts().entrySet(), procs);
        createStreams(mapping, topo.get_bolts().entrySet(), procs);
        // connect algorithms
        for (StormProcessor c : procs.values()) {
            createInvisibleStreams(mapping, procs, c);
        }
        return new PipelineTopology(procs.values());
    }
    
    /**
     * Collects a mapping of the actual executors.
     * 
     * @param topoInfo the dynamic Storm topology information
     * @return a mapping between the executor name and the executor information instance
     */
    private static Map<String, ExecutorSummary> collectExecutors(TopologyInfo topoInfo) {
        Map<String, ExecutorSummary> result = new HashMap<String, ExecutorSummary>();
        List<ExecutorSummary> executors = topoInfo.get_executors();
        if (null != executors) {
            for (int e = 0; e < executors.size(); e++) {
                ExecutorSummary executor = executors.get(e);
                result.put(executor.get_component_id(), executor);
            }
        }
        return result;
    }
    
    /**
     * Creates invisible streams between pipeline elements and algorithm processors, including a pseudo-stream "for" the
     * hardware in hardware integration. This method may create more
     * connections than actual needed, in particular if a sub-algorithm defines own spouts without connections to
     * the main topology, but this shall not disturb the monitoring data aggregation.
     * 
     * @param mapping the name mapping
     * @param procs already known processors
     * @param processor the component to search for potential invisible components
     */
    private static void createInvisibleStreams(INameMapping mapping, Map<String, StormProcessor> procs, 
        StormProcessor processor) {
        Configuration cfg = RepositoryConnector.getModels(Phase.MONITORING).getConfiguration();
        IDatatype hwAlgType;
        try {
            hwAlgType = ModelQuery.findType(cfg.getProject(), QmConstants.TYPE_HARDWARE_ALGORITHM, null);
        } catch (ModelQueryException e) {
            hwAlgType = null;
        }
        Component mComponent = mapping.getPipelineNodeComponent(processor.getName()); // already mapped
        if (null != mComponent) {
            Collection<String> alts = mComponent.getAlternatives();
            if (null != alts) {
                // all alts need at least one input-output stream
                for (String alt : alts) {
                    Algorithm alg = mapping.getAlgorithm(alt);
                    if (null != alg) {
                        IDecisionVariable algVar = PipelineHelper.obtainAlgorithmByName(cfg, alg.getName());
                        List<Component> algComponents = alg.getComponents();
                        boolean isHwAlg = isAssignable(hwAlgType, algVar);
                        if (isHwAlg) {
                            // ensure internal connectivity
                            createInvisibleStreamForHwAlgorithm(algComponents, procs);
                        }
                        // ensure external connectivity
                        createInvisbleBoundaryStreams(processor, algComponents, procs);
                    }
                }
            }
        }
    }
    
    /**
     * Creates an invisible stream for the hardware integration bolt/spout.
     * 
     * @param algComponents the algorithm components
     * @param procs already known processors
     */
    private static void createInvisibleStreamForHwAlgorithm(List<Component> algComponents, 
        Map<String, StormProcessor> procs) {
        StormProcessor receiver = null;
        StormProcessor sender = null;
        for (int c = 0; c < algComponents.size(); c++) {
            Component cmp = algComponents.get(c);
            StormProcessor algComponent = getProcessor(cmp, procs);
            boolean isHw = Type.HARDWARE == cmp.getType() || 2 == algComponents.size(); // 2 == fallback
            if (null != algComponent && isHw) {
                if (algComponent.isSource() 
                    || 0 == algComponent.getStreamCount()) { // 0 == -> profiling end, prefer src in given sequence
                    receiver = algComponent;
                } else if (algComponent.isSink()) {
                    sender = algComponent;
                }
            }
        }
        if (null != receiver && null != sender) {
            // o->|->b s->|->o, here: b == sender, s = receiver
            Stream stream = new Stream("", sender, receiver);
            sender.addOutput(stream);
            receiver.addInput(stream);
        }
    }

    /**
     * Creates an invisible stream for algorithm boundary nodes. We assume that all nodes within the algorithm are 
     * connected. May be achieved by {@link #createInvisibleStreamForHwAlgorithm(List, Map)}.
     * 
     * @param algorithm the processor representing the algorithm (family)
     * @param algComponents the algorithm components
     * @param procs already known processors
     */
    private static void createInvisbleBoundaryStreams(StormProcessor algorithm, List<Component> algComponents, 
        Map<String, StormProcessor> procs) {
        List<StormProcessor> algSources = new ArrayList<StormProcessor>();
        List<StormProcessor> algSinks = new ArrayList<StormProcessor>();
        List<StormProcessor> algIntermediary = new ArrayList<StormProcessor>();
        Set<String> algComponentNames = toNameSet(algComponents);
        // determine boundary nodes - sources to be connected to algorithm, intermediaries to sinks (conn to outside)
        for (int c = 0; c < algComponents.size(); c++) {
            StormProcessor algComponent = getProcessor(algComponents.get(c), procs);
            if (null != algComponent) {
                if (algComponent.isSource()) {
                    int targets = countTargetsIn(algComponent, algComponentNames);
                    if (targets > 0) {
                        //algComponent -> internal*
                        algSources.add(algComponent);
                    } else {
                        //algComponent -> external*
                        algSinks.add(algComponent);
                    } 
                } else if (algComponent.isSink()) {
                    // internal* -> algComponent
                    algIntermediary.add(algComponent);
                }
            }
        }

        // create algorithm-source streams
        for (StormProcessor source : algSources) {
            Stream s = new Stream("", algorithm, source);
            if (!algorithm.hasOutputTo(source)) {
                algorithm.addOutput(s); // internal connection
            }
            if (!source.hasInputFrom(algorithm)) {
                source.addInput(s); // internal connection
            }
        }
        
        // create intermediary-sink streams
        for (StormProcessor intermediary : algIntermediary) {
            for (StormProcessor sink : algSinks) {
                Stream s = new Stream("", intermediary, sink);
                if (!sink.hasInputFrom(intermediary)) {
                    sink.addInput(s);
                }
                if (!intermediary.hasOutputTo(sink)) {
                    intermediary.addOutput(s);
                }
            }
        }
    }

    /**
     * Adds the names of all <code>comps</code> to the result.
     * 
     * @param comps the components to be turned into a set of names (may be <b>null</b>)
     * @return the name set or <b>null</b> if <code>comps</code> is <b>null</b>
     */
    private static Set<String> toNameSet(List<Component> comps) {
        Set<String> result;
        if (null != comps) {
            result = new HashSet<String>();
            for (Component c : comps) {
                result.add(c.getName());
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Returns whether <code>var</code> is assignable from <code>type</code>.
     * 
     * @param type the type to check for (may be <b>null</b>)
     * @param var the variable to check (may be <b>null</b>)
     * @return <code>true</code> if assignable, <code>false</code> if not or one of the parameters is <b>null</b>
     */
    private static boolean isAssignable(IDatatype type, IDecisionVariable var) {
        boolean result = false;
        if (null != var && null != type) {
            result = type.isAssignableFrom(var.getDeclaration().getType());
        }
        return result;
    }
    
    /**
     * Returns the processor for a name mapping component.
     * 
     * @param component the component to search for
     * @param procs the already known processors
     * @return the processor or <b>null</b> if not found
     */
    private static StormProcessor getProcessor(Component component, Map<String, StormProcessor> procs) {
        return procs.get(component.getName());
    }
    
    /**
     * Counts the number of all targets of <code>node</code> in <code>targets</code>.
     * 
     * @param node the node to follow the outgoing edges
     * @param targets the targets to check
     * @return the number of targets in <code>targets</code>
     */
    private static int countTargetsIn(StormProcessor node, Set<String> targets) {
        int count = 0;
        for (int o = 0; o < node.getOutputCount(); o++) {
            if (targets.contains(node.getOutput(o).getTarget().getName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the executor for a given Storm map entry.
     * 
     * @param executors the executors mapping
     * @param entry the map entry
     * @return the executor summary (<b>null</b> if unknown or internal)
     */
    private static ExecutorSummary getNonInternalExecutor(Map<String, ExecutorSummary> executors, 
        Map.Entry<String, ?> entry) {
        ExecutorSummary result = getExecutor(executors, entry);
        if (null != result && isInternal(result)) {
            result = null;
        }
        return result;
    }

    /**
     * Returns the executor for a given Storm map entry.
     * 
     * @param executors the executors mapping
     * @param entry the map entry
     * @return the executor summary (<b>null</b> if unknown)
     */
    private static ExecutorSummary getExecutor(Map<String, ExecutorSummary> executors, Map.Entry<String, ?> entry) {
        return executors.get(entry.getKey()); // do not map back, executors is also on implementation level
    }
    
    /**
     * Maps a component implementation name for a Storm map entry back to pipeline level.
     * 
     * @param mapping the name mapping
     * @param entry the map entry
     * @return the mapped name
     */
    private static String mapName(INameMapping mapping, Map.Entry<String, ?> entry) {
        return mapName(mapping, entry.getKey());
    }

    /**
     * Maps a component implementation name back to pipeline level.
     * 
     * @param mapping the name mapping
     * @param implName the implementation name to be mapped
     * @return the mapped name if available, else <code>implName</code>
     */
    private static String mapName(INameMapping mapping, String implName) {
        String result = mapping.getPipelineNodeByImplName(implName);
        if (null == result) { // just fallback
            result = implName;
        }
        return result;
    }
    
}
