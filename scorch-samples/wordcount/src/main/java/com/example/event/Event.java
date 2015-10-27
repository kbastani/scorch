package com.example.event;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect
public class Event {

    private String id;
    private EventType eventType;
    private String targetId;

    public Event() {
    }

    public Event(String id, EventType eventType, String targetId) {
        this.id = id;
        this.eventType = eventType;
        this.targetId = targetId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", eventType=" + eventType +
                ", targetId='" + targetId + '\'' +
                '}';
    }
}
