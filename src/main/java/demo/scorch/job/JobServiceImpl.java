package demo.scorch.job;

import demo.scorch.event.Event;
import demo.scorch.event.EventType;
import demo.scorch.machine.Status;
import demo.scorch.stage.Stage;
import demo.scorch.task.Task;
import demo.scorch.zookeeper.ZookeeperClient;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class JobServiceImpl implements JobService {

    private ZookeeperClient zookeeperClient;

    @Autowired
    public JobServiceImpl(ZookeeperClient zookeeperClient) {
        this.zookeeperClient = zookeeperClient;
    }

    @Override
    public List<Task> getTasks(String jobId) {
        Job job = zookeeperClient.get(Job.class, jobId);
        List<Task> tasks = job.getStages().stream()
                .flatMap(t -> t.getTasks().stream())
                .map(m -> zookeeperClient.get(Task.class, m.getId()))
                .collect(Collectors.toList());
        return tasks;
    }

    /**
     * Create a new {@link Job}
     *
     * @param job is the new {@link Job}
     * @return the newly create {@link Job}
     */
    @Override
    public Job createJob(Job job) {
        // Save job, stages, and tasks to zookeeper
        zookeeperClient.save(job);
        job.getStages().forEach(s -> {
            zookeeperClient.save(s);
            s.setStatus(Status.STARTED);
            s.getTasks().forEach(t -> {
                t.setStatus(Status.PENDING);
                zookeeperClient.save(t);

                // Create a new event to start the state machine
                Event event = new Event();
                event.setEventType(EventType.BEGIN);
                event.setTargetId(t.getId());
                event.setId("0");

                // Send the event to ZooKeeper
                zookeeperClient.save(event, CreateMode.PERSISTENT_SEQUENTIAL);
            });
        });

        return job;
    }

    /**
     * Get a distributed {@link Job}.
     *
     * @param id is the identifier for the {@link Job}
     * @return a distributed {@link Job} and its associated state
     */
    @Override
    public Job getJob(String id) {
        // Job job = getFromStore(id);
        return zookeeperClient.get(Job.class, id);
    }

    /**
     * Create a new {@link Task}
     *
     * @param stageId is the id of the stage to add the new task to
     * @param task    is the new task to create
     * @return the newly created {@link Task}
     */
    @Override
    public Task createTask(String stageId, Task task) {
        Stage stage = zookeeperClient.get(Stage.class, stageId);
        Assert.notNull(stage, "The job stage does not exist.");
        stage.addTask(task);
        zookeeperClient.save(task);
        zookeeperClient.save(stage);
        return task;
    }
}
