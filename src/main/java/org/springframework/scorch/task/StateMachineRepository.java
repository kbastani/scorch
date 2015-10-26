package org.springframework.scorch.task;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scorch.event.EventType;
import org.springframework.scorch.machine.Status;
import org.springframework.scorch.machine.TaskListener;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.ObjectStateMachineFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class StateMachineRepository implements ApplicationContextAware {

    public static final Cache<String, StateMachine<Status, EventType>> stateMachineCache =
            CacheBuilder.newBuilder().maximumSize(20000000).build();

    public static final Cache<String, TaskListener> taskListenerCache =
            CacheBuilder.newBuilder().maximumSize(20000000).build();

    public static ApplicationContext applicationContext;

    public static TaskListener getTaskListener(String objectId) {
        getStateMachineBean(objectId);
        return taskListenerCache.getIfPresent(objectId);
    }

    public static StateMachine<Status, EventType> getStateMachineBean(String objectId) {
        StateMachine<Status, EventType> stateMachine = null;
        try {
            // Gets or creates a state machine and creates a lookup in cache
            stateMachine = stateMachineCache.get(objectId, () -> {
                StateMachine<Status, EventType> sm = ((ObjectStateMachineFactory) applicationContext.getBean("taskMachine")).getStateMachine();
                TaskListener taskListener = new TaskListener(objectId);
                sm.addStateListener(taskListener);
                taskListenerCache.put(objectId, taskListener);
                sm.start();
                return sm;
            });
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return stateMachine;
    }

    public void init() {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StateMachineRepository.applicationContext = applicationContext;
    }
}
