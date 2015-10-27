package demo.scorch.job;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import demo.scorch.machine.Status;
import demo.scorch.stage.Stage;
import demo.scorch.task.StateMachineRepository;
import demo.scorch.task.Task;
import demo.scorch.zookeeper.ZookeeperClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class JobServiceImpl implements JobService {

    private ZookeeperClient zookeeperClient;
    private RedisTemplate<String, Object> redisTemplate;
    final Gson gson = Converters.registerDateTime(new GsonBuilder()).create();

    @Autowired
    public JobServiceImpl(ZookeeperClient zookeeperClient,
                          RedisTemplate<String, Object> redisTemplate) {
        this.zookeeperClient = zookeeperClient;
        this.redisTemplate = redisTemplate;
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
        // Save the job to redis
        addJob(job.getId(), job);

        // Save job, stages, and tasks to zookeeper
        zookeeperClient.save(job);
        job.getStages().forEach(s -> {
            zookeeperClient.save(s);
            s.setStatus(Status.STARTED);
            s.getTasks().forEach(t -> {
                t.setStatus(Status.PENDING);
                zookeeperClient.save(t);
                StateMachineRepository.getStateMachineBean(t.getId()).start();
            });
        });

        return job;
    }

    public void addJob(String jobId, Job object) {
        if (!redisTemplate.opsForValue().setIfAbsent(jobId, gson.toJson(object))) {
            throw new IllegalArgumentException("Job with id already exists...");
        }
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

    private Job getFromStore(String id) {
        Job job = gson.fromJson(redisTemplate.opsForValue().get(id).toString(), Job.class);

        if (job == null) {
            redisTemplate.opsForValue().getAndSet(id, job);
        }
        return job;
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
