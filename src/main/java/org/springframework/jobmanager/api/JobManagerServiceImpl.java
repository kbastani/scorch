package org.springframework.jobmanager.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jobmanager.tasks.Events;
import org.springframework.jobmanager.tasks.States;
import org.springframework.jobmanager.tasks.Task;
import org.springframework.jobmanager.tasks.Tasks;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@DependsOn()
public class JobManagerServiceImpl implements JobManagerService {

    private StateMachine<States,Events> stateMachine;
    private Tasks tasks;

    @Autowired
    public JobManagerServiceImpl(StateMachine<States,Events> stateMachine, Tasks tasks) {
        this.stateMachine = stateMachine;
        this.tasks = tasks;
    }

    @Override
    public StateMachine<States, Events> getStateMachine() {
        return stateMachine;
    }

    @Override
    public Tasks getTasks() {
        return tasks;
    }

    @Override
    public Task createTask(Task task) {
        tasks.createTask(task);
        return task;
    }

    @Override
    public void updateTask(Task task) {

    }

    @Override
    public void failTask(String id) {

    }

    @Override
    public void fixTask(String id) {

    }


}
