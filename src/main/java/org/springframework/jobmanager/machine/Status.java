package org.springframework.jobmanager.machine;

public enum Status {
    READY,
    FORK, JOIN, CHOICE,
    STARTED, RUNNING, FINISHED,
    ERROR, AUTOMATIC, MANUAL, SUCCESS,
    ACTIVE,
    COMPLETE,
    FAILED,
    WAITING
}
