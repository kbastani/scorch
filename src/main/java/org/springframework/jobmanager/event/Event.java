package org.springframework.jobmanager.event;

import java.io.Serializable;

/**
 * An {@link Event} is ingested to the {@link EventService} and applied to
 * a {@link org.springframework.statemachine.StateMachine} which is transacted
 * to affect the state of a {@link org.springframework.jobmanager.job.Job}.
 *
 * @author Kenny Bastani
 */
public class Event implements Serializable {

    private EventType eventType;
    private Long targetId;

    public Event() {
    }

    public Event(EventType eventType) {
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
