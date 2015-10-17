package org.springframework.jobmanager.event;

/**
 * An {@link EventType} contains information about an event and the {@link DomainType} that a
 * state machine can be notified for a given {@link EventType}
 *
 * @author Kenny Bastani
 */
public enum EventType {

    START(DomainType.JOB),
    CANCEL(DomainType.JOB),
    SUSPEND(DomainType.JOB),
    BEGIN(DomainType.STAGE),
    END(DomainType.STAGE),
    RUN(DomainType.TASK),
    FALLBACK(DomainType.TASK),
    CONTINUE(DomainType.TASK),
    FIX(DomainType.TASK);

    private DomainType domain = null;

    private EventType(DomainType domain) {
        this.domain = domain;
    }

    public DomainType getDomain() {
        return domain;
    }

    public boolean ofDomain(DomainType domainType) {
        return domain == domainType;
    }

    @Override
    public String toString() {
        return String.format("%s.%s", domain, super.toString());
    }
}