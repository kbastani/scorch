package demo.scorch.task;

import demo.scorch.event.Event;
import demo.scorch.event.EventType;
import demo.scorch.machine.Status;
import demo.scorch.zookeeper.ZookeeperClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * The {@link TaskReplicator} is responsible for keeping the state of a cluster
 * consistent. A queue is maintained of events from the event log in Zookeeper.
 * When a new scorch node is started, it will reconstruct its state to become
 * consistent with the state of the cluster.
 *
 * @author Kenny Bastani
 */
@Component
public class TaskReplicator implements AutoCloseable {

    private final TaskExecutor taskExecutor;
    private final ZookeeperClient zookeeperClient;
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private static Integer eventPosition = -1;
    private static boolean running = false;
    private final static Log log = LogFactory.getLog(TaskReplicator.class);

    /**
     * Create a new instance of the {@link TaskReplicator} module to keep the state
     * of a cluster of scorch nodes consistent.
     *
     * @param taskExecutor    is an executor for running the background replicator process
     * @param zookeeperClient is the client for {@link org.apache.zookeeper.ZooKeeper}
     */
    @Autowired
    public TaskReplicator(TaskExecutor taskExecutor, ZookeeperClient zookeeperClient) {
        this.taskExecutor = taskExecutor;
        this.zookeeperClient = zookeeperClient;
    }

    /**
     * The initialization method for the bean.
     */
    public void init() {
        taskExecutor.execute(() -> {
            try {
                consume();
            } catch (KeeperException | InterruptedException e) {
                log.error(e);
            }
        });
    }

    /**
     * Process items in the distributed event log from zookeeper to
     * keep state consistent across the cluster.
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void consume() throws KeeperException, InterruptedException {
        String eventsPath = "/scorch/event";

        running = true;

        // Get the first element available
        while (running) {
            try {
                List<String> list = zookeeperClient.getZooKeeper()
                        .getChildren(eventsPath, true)
                        .parallelStream()
                        .filter(f -> !queue.contains(f))
                        .collect(Collectors.toList());

                if (list.size() > 0) {
                    for (String path : list) {
                        Integer min = new Integer(path.substring(1));

                        if (eventPosition < min)
                            eventPosition = min;

                        log.info("Replicating event: " + path);

                        // Get the event data
                        Event event = zookeeperClient.get(Event.class, path, true);

                        // Initialize the state machine or submit an event to the queue
                        if (event.getEventType() == EventType.BEGIN) {
                            StateMachine<Status, EventType> stateMachine = StateMachineRepository
                                    .getStateMachineBean(event.getTargetId());
                            stateMachine.start();
                        } else {
                            // Add event to state machine queue
                            StateMachineRepository.getTaskListener(event.getTargetId())
                                    .getQueue().add(event.getEventType());
                        }

                        queue.add(path);
                    }
                }
            } catch (Exception ex) {
                log.info(ex);
            }
        }
    }

    @Override
    public void close() throws Exception {
        running = false;
    }
}

