package org.springframework.scorch.machine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scorch.event.EventType;
import org.springframework.scorch.task.StateMachineRepository;
import org.springframework.scorch.task.Task;
import org.springframework.scorch.zookeeper.ZookeeperClient;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class TaskListener extends StateMachineListenerAdapter<Status, EventType> {

    final Object lock = new Object();
    public boolean ready = false;
    private String taskId;
    ZookeeperClient zookeeperClient;
    final ConcurrentLinkedQueue<EventType> queue = new ConcurrentLinkedQueue<EventType>();
    private final static Log log = LogFactory.getLog(StateMachineListenerAdapter.class);

    public TaskListener(String taskId) {
        this.taskId = taskId;
        this.zookeeperClient = (ZookeeperClient) StateMachineRepository.applicationContext.getBean("zookeeperClient");
        TaskExecutor taskExecutor = (TaskExecutor) StateMachineRepository.applicationContext.getBean("taskExecutor");
        taskExecutor.execute(this::observeQueue);
    }

    volatile CountDownLatch stateChangedLatch = new CountDownLatch(1);
    volatile CountDownLatch stateEnteredLatch = new CountDownLatch(2);
    volatile CountDownLatch stateExitedLatch = new CountDownLatch(0);
    volatile CountDownLatch transitionLatch = new CountDownLatch(0);
    volatile int stateChangedCount = 0;
    volatile int transitionCount = 0;
    List<State<Status, EventType>> statesEntered = new ArrayList<State<Status, EventType>>();
    List<State<Status, EventType>> statesExited = new ArrayList<State<Status, EventType>>();

    public ConcurrentLinkedQueue<EventType> getQueue() {
        return queue;
    }

    @Override
    public void stateChanged(State<Status, EventType> from, State<Status, EventType> to) {
        stateChangedCount++;

        Task task = zookeeperClient.get(Task.class, taskId);
        task.setStatus(to.getId());
        if (zookeeperClient.get(Task.class, task.getId()).getStatus() != task.getStatus()) {
            zookeeperClient.save(task);
        }
        log.info(task);
        log.info(queue);

//            if(from == null && containsState(to, Status.READY)) {
//                // Transition from stored state
//                StateMachineRepository.getStateMachineBean(taskId).sendEvent(EventType.RUN);
//            }
//            else if (containsState(from, Status.READY) && containsState(to, Status.STARTED)) {
//                // Transition from stored state
//                stateMachine.sendEvent(EventType.END);
//            } else if (containsState(from, Status.STARTED) && containsState(to, Status.RUNNING)) {
//                // Transition from stored state
//                stateMachine.sendEvent(EventType.CONTINUE);
//            }
    }

    private boolean containsState(State<Status, EventType> state, Status status) {
        return state.getStates().stream().anyMatch(a -> a.getId() == status);
    }

    @Override
    public void stateEntered(State<Status, EventType> state) {
        ready = true;
        statesEntered.add(state);
        stateEnteredLatch.countDown();
    }

    @Override
    public void stateExited(State<Status, EventType> state) {
        ready = false;
        statesExited.add(state);
        stateExitedLatch.countDown();
    }

    @Override
    public void transitionEnded(Transition<Status, EventType> transition) {
        synchronized (lock) {
            transitionCount++;
            transitionLatch.countDown();
        }
    }

    public void observeQueue() {

        while (true) {
            if (ready && !StateMachineRepository.getTaskListener(taskId).getQueue().isEmpty()) {
                synchronized (lock) {
                    StateMachineRepository.getTaskListener(taskId).getQueue().removeIf(event -> StateMachineRepository.getStateMachineBean(taskId).sendEvent(event));
                }
            }

        }
    }
}
