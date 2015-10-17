package org.springframework.jobmanager.machine;

public enum States {
	READY,
	FORK, JOIN, CHOICE,
	TASKS, T1, T1E, T2, T2E, T3, T3E,
	ERROR, AUTOMATIC, MANUAL
}
