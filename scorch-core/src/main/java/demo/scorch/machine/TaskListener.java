package demo.scorch.machine;

import demo.scorch.event.EventType;
import demo.scorch.task.StateMachineRepository;
import demo.scorch.task.Task;
import demo.scorch.zookeeper.ZookeeperClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskListener extends StateMachineListenerAdapter<Status, EventType> {

    ConcurrentLinkedQueue<EventType> queue = new ConcurrentLinkedQueue<EventType>();
    private final TaskExecutor taskExecutor;
    private String taskId;
    private ZookeeperClient zookeeperClient;
    private boolean running = true;

    private final static Log log = LogFactory.getLog(StateMachineListenerAdapter.class);

    public TaskListener(String taskId) {
        this.taskId = taskId;
        this.zookeeperClient = (ZookeeperClient) StateMachineRepository.applicationContext.getBean("zookeeperClient");
        this.taskExecutor = (TaskExecutor) StateMachineRepository.applicationContext.getBean("taskExecutor");
    }

    @Override
    public void stateMachineStarted(StateMachine<Status, EventType> stateMachine) {
        running = true;
    }

    public ConcurrentLinkedQueue<EventType> getQueue() {
        return queue;
    }

    @Override
    public void stateEntered(State<Status, EventType> state) {
        if(state.getId() == Status.SUCCESS) {
            StateMachineRepository.getStateMachineBean(taskId).stop();
        }
    }

    @Override
    public void stateMachineStopped(StateMachine<Status, EventType> stateMachine) {
        running = false;
    }

    @Override
    public void stateChanged(State<Status, EventType> from, State<Status, EventType> to) {
        Task task = zookeeperClient.get(Task.class, taskId);

        // If the task is rebuilding state, do not save state to ZooKeeper
        if (!zookeeperClient.get(Task.class, task.getId()).getStatus().equals(to.getId()) &&
                (from != null ? from.getId() : Status.PENDING) == task.getStatus()) {
            task.setStatus(to.getId());
            zookeeperClient.save(task);
            StateMachineRepository.getStateMachineBean(taskId)
                    .getExtendedState()
                    .getVariables()
                    .put(String.format("%s--%s", task.getId(), to.getId()), true);
        }

        if (!queue.isEmpty()) {
            queue.removeIf(event -> StateMachineRepository.getStateMachineBean(taskId).sendEvent(event));
        } else {
            // Watch
            this.taskExecutor.execute(() -> {
                boolean runOnce = true;
                while (runOnce && running) {

                    if (!queue.isEmpty()) {
                        runOnce = !StateMachineRepository.getStateMachineBean(taskId).sendEvent(queue.remove());
                    }

                    if (runOnce) {
                        try {
                            Thread.sleep(300L);
                        } catch (InterruptedException e) {
                            log.error(e);
                        }
                    }
                }
            });
        }

        log.info(String.format("Task %s: %s", taskId, queue));
    }
}
