package com.example.event;

/**
 * An {@link EventType} contains information about an event and the {@link } that a
 * state machine can be notified for a given {@link EventType}
 *
 * @author Kenny Bastani
 */
public enum EventType {

    START,
    CANCEL,
    SUSPEND,
    BEGIN,
    END,
    RUN,
    FALLBACK,
    CONTINUE,
    FIX;

    @Override
    public String toString() {
        return "EventType{} " + super.toString();
    }
}