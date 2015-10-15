package org.springframework.jobmanager.api;

import org.springframework.jobmanager.tasks.Events;
import org.springframework.jobmanager.tasks.States;
import org.springframework.jobmanager.tasks.Task;
import org.springframework.jobmanager.tasks.Tasks;
import org.springframework.statemachine.StateMachine;

public interface JobManagerService {

    StateMachine<States,Events> getStateMachine();

    Tasks getTasks();

    Task createTask(Task task);

    void updateTask(Task task);

    void failTask(String id);

    void fixTask(String id);
}
