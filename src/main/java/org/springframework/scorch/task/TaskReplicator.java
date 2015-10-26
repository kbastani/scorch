package org.springframework.scorch.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scorch.event.Event;
import org.springframework.scorch.zookeeper.ZookeeperClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Component
public class TaskReplicator {
    static Integer eventPosition = -1;
    static Integer waitPosition = -1;
    private ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private final static Log log = LogFactory.getLog(TaskReplicator.class);

    @Autowired
    private ZookeeperClient zookeeperClient;

    @Autowired
    private TaskExecutor taskExecutor;

    public void init() {
        taskExecutor.execute(() -> {
            try {
                consume();
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    int consume() throws KeeperException, InterruptedException {
        Stat stat = null;
        String eventsPath = "/scorch/event";

        // Get the first element available
        while (true) {
            try {
                List<String> list = zookeeperClient.getZooKeeper().getChildren(eventsPath, true)
                        .parallelStream()
                        .filter(f -> !queue.contains(f))
                        .collect(Collectors.toList());

                if (list.size() > 0) {
                    for (String path : list) {
                        String absolutePath = path;
                        Integer min = new Integer(path.substring(1));
                        synchronized (eventPosition) {
                            if (eventPosition < min)
                                eventPosition = min;
                        }
                        System.out.println("Replicating event: " + absolutePath);

                        // Get the event data
                        Event event = zookeeperClient.get(Event.class, absolutePath, true);

                        // Add event to state machine queue
                        StateMachineRepository.getTaskListener(event.getTargetId()).getQueue().add(event.getEventType());

                        queue.add(path);
                    }
                }
            } catch (Exception ex) {
                log.info(ex);
            }
        }
    }
}

