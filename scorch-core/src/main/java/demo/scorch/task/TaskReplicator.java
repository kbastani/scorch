package demo.scorch.task;

import demo.scorch.event.Event;
import demo.scorch.event.EventType;
import demo.scorch.machine.Status;
import demo.scorch.zookeeper.ZookeeperClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
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

    private final static Log log = LogFactory.getLog(TaskReplicator.class);

    private final ZookeeperClient zookeeperClient;
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private static Boolean running = false;

    /**
     * Create a new instance of the {@link TaskReplicator} module to keep the state
     * of a cluster of scorch nodes consistent.
     *
     * @param zookeeperClient is the client for {@link org.apache.zookeeper.ZooKeeper}
     */
    @Autowired
    public TaskReplicator(ZookeeperClient zookeeperClient) {
        this.zookeeperClient = zookeeperClient;
    }

    /**
     * The initialization method for the bean.
     */
    public void init() {
    }

    /**
     * Process items in the distributed event log from zookeeper to
     * keep state consistent across the cluster.
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void consume() throws KeeperException, InterruptedException {
        String eventsPath = "/scorch/event";

        if (!running) {

            running = true;

            try {
                // Compare local event queue to global queue
                // TODO: Trade off #1
                List<String> list = zookeeperClient.getZooKeeper()
                        .getChildren(eventsPath, true)
                        .parallelStream()
                        .filter(f -> !queue.contains(f))
                        .collect(Collectors.toList());

                // Order events sequentially
                list.sort((a, b) -> {
                    Integer aInt = new Integer(a.substring(1));
                    Integer bInt = new Integer(b.substring(1));
                    return aInt > bInt ? 1 : (Objects.equals(aInt, bInt)) ? 0 : -1;
                });

                // Replicate state machine events
                if (list.size() > 0) {

                    // Replicate sequentially ordered events and apply to in-memory state machines
                    for (String path : list) {

                        log.info("Replicating event: " + path);

                        // Get the event data
                        Event event = zookeeperClient.get(Event.class, path, false);

                        // Initialize the state machine or submit an event to the queue
                        replicateEvent(path, event);
                    }
                }
            } catch (Exception ex) {
                log.info(ex);
            }

            running = false;
        }
    }

    /**
     * Replicates a provided event and applies the event to a state machine.
     *
     * @param path  is the path to the ZooKeeper log for this event.
     * @param event is the event.
     */
    private void replicateEvent(String path, Event event) {
        if (event.getEventType() == EventType.BEGIN) {

            // Get the task state machine for this target
            StateMachine<Status, EventType> stateMachine = StateMachineRepository
                    .getStateMachineBean(event.getTargetId());

            // The state machine must acquire a lock on this task to ensure idempotent
            // state change notification sent to RabbitMQ
            // TODO: Trade off #2
            stateMachine
                    .getExtendedState()
                    .getVariables()
                    .put(String.format("%s--%s", event.getTargetId(), Status.PENDING), true);

            // Start the task state machine
            stateMachine.start();

        } else {

            // Add event to state machine queue
            StateMachineRepository.getTaskListener(event.getTargetId())
                    .getQueue().add(event.getEventType());

        }

        // Enqueue the event so that it is not re-processed
        queue.add(path);
    }

    /**
     * This class is disposable. In the case of failure this method
     * ensures that the process does not interrupt a transaction until
     * it has been committed.
     *
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        log.info("Shutting down task replicator...");
        while (running) {
            Thread.sleep(300L);
        }
    }
}

