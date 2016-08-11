package eu.qualimaster.common.switching;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.common.signal.TopologySignal;

/**
 * A switch mechanism using parallel track strategy.
 * @author Cui Qin
 *
 */
public class ParallelTrackSwitchMechanism extends AbstractSwitchMechanism {
    private AbstractSwitchStrategy strategy;
    /**
     * Creates a parallel track switch mechanism.
     * @param strategy the switch strategy
     */
    public ParallelTrackSwitchMechanism(AbstractSwitchStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void doSwitch(AbstractAlgorithm from, AbstractAlgorithm to) {
        
    }

    @Override
    public IGeneralTuple getNextTuple() {
        return strategy.produceTuple();
    }

    @Override
    public void handleSignal(TopologySignal signal) {
        strategy.doSignal(signal);
    }   

}
