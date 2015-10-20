package org.springframework.jobmanager.event;

import java.util.List;

/**
 * An event service ingests events and dispatches the events to the state machine
 * the {@link org.springframework.jobmanager.job.Job} object graph.
 *
 * @author Kenny Bastani
 */
public interface EventService {

    /**
     * Retrieve the available {@link DomainType}
     *
     * @return the set of available {@link DomainType}
     */
    List<DomainType> getDomainTypes();

    /**
     * Retrieve the available {@link EventType} for a {@link DomainType}.
     *
     * @return the set of available {@link EventType}
     */
    List<EventType> getEventTypes(DomainType domainType);

    /**
     * Notify a state machine with a given event that will alter the current state of the machine.
     *
     * @param event is the event object
     */
    boolean sendEvent(Event event);
}
