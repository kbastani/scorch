package demo.scorch.task;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import demo.scorch.event.EventType;
import demo.scorch.machine.Status;
import demo.scorch.machine.TaskListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.ObjectStateMachineFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * A repository for state machine beans that are attached to a task.
 *
 * @author Kenny Bastani
 */
@Component
public class StateMachineRepository implements ApplicationContextAware {

    private static Log log = LogFactory.getLog(StateMachineRepository.class);

    // A global cache of state machines
    public static final Cache<String, StateMachine<Status, EventType>> stateMachineCache =
            CacheBuilder.newBuilder().maximumSize(20000000).build();

    // A global cache of state machine listeners
    public static final Cache<String, TaskListener> taskListenerCache =
            CacheBuilder.newBuilder().maximumSize(20000000).build();

    // The Spring application context
    public static ApplicationContext applicationContext;

    /**
     * An initialization method for the Spring bean.
     */
    public void init() {
    }

    /**
     * Get a task listener for a task's state machine.
     *
     * @param objectId is the ID of the task.
     * @return a task listener that is listening for state machine events.
     */
    public static TaskListener getTaskListener(String objectId) {
        getStateMachineBean(objectId);
        return taskListenerCache.getIfPresent(objectId);
    }

    /**
     * Get a state machine of a task from the cache.
     *
     * @param objectId is the ID of the task.
     * @return a state machine for the task.
     */
    public static StateMachine<Status, EventType> getStateMachineBean(String objectId) {

        StateMachine<Status, EventType> stateMachine = null;

        try {

            // Gets or creates a state machine and creates a lookup in cache
            stateMachine = stateMachineCache.get(objectId, () -> {

                StateMachine<Status, EventType> sm =
                        ((ObjectStateMachineFactory) applicationContext.getBean("taskMachine")).getStateMachine();

                TaskListener taskListener = new TaskListener(objectId);

                sm.addStateListener(taskListener);

                taskListenerCache.put(objectId, taskListener);

                sm.getExtendedState().getVariables().put("id", objectId);

                sm.start();

                return sm;
            });

        } catch (ExecutionException e) {
            log.error(e);
        }

        return stateMachine;
    }

    /**
     * Sets the Spring application context.
     *
     * @param applicationContext is the Spring application context.
     * @throws BeansException is an exception that occurred during the retrieval of the application context.
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StateMachineRepository.applicationContext = applicationContext;
    }
}
