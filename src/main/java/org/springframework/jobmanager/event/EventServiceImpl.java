package org.springframework.jobmanager.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jobmanager.machine.Status;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * An event service ingests events and dispatches the events to the state machine
 * the {@link org.springframework.jobmanager.job.Job} object graph.
 *
 * @author Kenny Bastani
 */
@Service
@Transactional
public class EventServiceImpl implements EventService {

    private StateMachineFactory<Status, EventType> stateMachineFactory;

    private StateMachine<Status, EventType> stateMachine;

    @Autowired
    public EventServiceImpl(StateMachineFactory<Status, EventType> taskMachine) {
        this.stateMachineFactory = taskMachine;
        this.stateMachine = taskMachine.getStateMachine();
    }

    /**
     * Retrieve the available {@link DomainType}
     *
     * @return the set of available {@link DomainType}
     */
    @Override
    public List<DomainType> getDomainTypes() {
        return null;
    }

    /**
     * Retrieve the available {@link EventType} for a {@link DomainType}.
     *
     * @param domainType
     * @return the set of available {@link EventType}
     */
    @Override
    public List<EventType> getEventTypes(DomainType domainType) {
        return null;
    }

    /**
     * Notify a state machine with a given event that will alter the current state of the machine.
     *
     * @param event is the event object
     */
    @Override
    public boolean sendEvent(Event event) {
        return stateMachine.sendEvent(event.getEventType());
    }
}
