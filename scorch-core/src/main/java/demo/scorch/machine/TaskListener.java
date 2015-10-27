package demo.scorch.machine;

import demo.scorch.event.EventType;
import demo.scorch.task.StateMachineRepository;
import demo.scorch.task.Task;
import demo.scorch.zookeeper.ZookeeperClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TaskListener extends StateMachineListenerAdapter<Status, EventType> {

    ConcurrentLinkedQueue<EventType> queue = new ConcurrentLinkedQueue<EventType>();
    private final TaskExecutor taskExecutor;
    private String taskId;
    private ZookeeperClient zookeeperClient;

    private final static Log log = LogFactory.getLog(StateMachineListenerAdapter.class);

    public TaskListener(String taskId) {
        this.taskId = taskId;
        this.zookeeperClient = (ZookeeperClient) StateMachineRepository.applicationContext.getBean("zookeeperClient");
        this.taskExecutor = (TaskExecutor) StateMachineRepository.applicationContext.getBean("taskExecutor");
    }

    public ConcurrentLinkedQueue<EventType> getQueue() {
        return queue;
    }

    @Override
    public void stateChanged(State<Status, EventType> from, State<Status, EventType> to) {
        Task task = zookeeperClient.get(Task.class, taskId);

        // If the task is rebuilding state, do not save state to ZooKeeper
        if (!zookeeperClient.get(Task.class, task.getId()).getStatus().equals(to.getId()) &&
                (from != null ? from.getId() : Status.READY) == task.getStatus()) {
            task.setStatus(to.getId());
            zookeeperClient.save(task);
        }

        if (!queue.isEmpty()) {
            queue.removeIf(event -> StateMachineRepository.getStateMachineBean(taskId).sendEvent(event));
        } else {
            // Watch
            this.taskExecutor.execute(() -> {
                boolean runOnce = true;
                while (runOnce) {
                    if (!queue.isEmpty()) {
                        runOnce = !StateMachineRepository.getStateMachineBean(taskId).sendEvent(queue.remove());
                    }
                }
            });
        }

        log.info(String.format("Task %s: %s", taskId, queue));
    }
}
