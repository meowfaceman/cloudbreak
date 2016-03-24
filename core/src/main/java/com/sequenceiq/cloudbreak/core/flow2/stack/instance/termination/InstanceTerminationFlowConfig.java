package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_STATE;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.statemachine.config.builders.StateMachineConfigurationBuilder;
import org.springframework.statemachine.config.builders.StateMachineStateBuilder;
import org.springframework.statemachine.config.builders.StateMachineTransitionBuilder;
import org.springframework.statemachine.config.common.annotation.ObjectPostProcessor;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class InstanceTerminationFlowConfig extends AbstractFlowConfiguration<InstanceTerminationState, InstanceTerminationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationFlowConfig.class);
    private static final List<Transition<InstanceTerminationState, InstanceTerminationEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, TERMINATION_STATE, TERMINATION_EVENT),
            new Transition<>(TERMINATION_STATE, TERMINATION_FINISHED_STATE, TERMINATION_FINISHED_EVENT)
    );
    private static final FlowEdgeConfig<InstanceTerminationState, InstanceTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TERMINATION_FINISHED_STATE, TERMINATION_FINALIZED_EVENT, TERMINATION_FAILED_STATE,
                    TERMINATION_FAIL_HANDLED_EVENT);

    @Override
    public Flow<InstanceTerminationState, InstanceTerminationEvent> createFlow(String flowId) {
        Flow<InstanceTerminationState, InstanceTerminationEvent> flow = new Flow<>(getStateMachineFactory().getStateMachine(),
                new MessageFactory<InstanceTerminationEvent>(), new InstanceTerminationEventConverter());
        flow.initialize(flowId);
        return flow;
    }

    @Override
    protected MachineConfiguration<InstanceTerminationState, InstanceTerminationEvent> getStateMachineConfiguration() {
        StateMachineConfigurationBuilder<InstanceTerminationState, InstanceTerminationEvent> configurationBuilder =
                new StateMachineConfigurationBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineStateBuilder<InstanceTerminationState, InstanceTerminationEvent> stateBuilder =
                new StateMachineStateBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineTransitionBuilder<InstanceTerminationState, InstanceTerminationEvent> transitionBuilder =
                new StateMachineTransitionBuilder<>(ObjectPostProcessor.QUIESCENT_POSTPROCESSOR, true);
        StateMachineListener<InstanceTerminationState, InstanceTerminationEvent> listener =
                new StateMachineListenerAdapter<InstanceTerminationState, InstanceTerminationEvent>() {
                    @Override
                    public void stateChanged(State<InstanceTerminationState, InstanceTerminationEvent> from, State<InstanceTerminationState,
                            InstanceTerminationEvent> to) {
                        LOGGER.info("InstanceTerminationFlow changed from {} to {}", from, to);
                    }
                };
        return new MachineConfiguration<>(configurationBuilder, stateBuilder, transitionBuilder, listener, new SyncTaskExecutor());
    }

    @Override
    protected List<Transition<InstanceTerminationState, InstanceTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<InstanceTerminationState, InstanceTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public List<InstanceTerminationEvent> getFlowTriggerEvents() {
        return Arrays.asList(TERMINATION_EVENT);
    }

    @Override
    public InstanceTerminationEvent[] getEvents() {
        return InstanceTerminationEvent.values();
    }
}
