package org.springframework.scorch.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scorch.stage.Stage;
import org.springframework.scorch.stage.StageRepository;
import org.springframework.scorch.task.Task;
import org.springframework.scorch.task.TaskRepository;
import org.springframework.scorch.zookeeper.ZookeeperClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class JobServiceImpl implements JobService {

    private JobRepository jobRepository;
    private StageRepository stageRepository;
    private TaskRepository taskRepository;
    private ZookeeperClient zookeeperClient;
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public JobServiceImpl(JobRepository jobRepository, StageRepository stageRepository,
                          TaskRepository taskRepository, ZookeeperClient zookeeperClient,
                          RedisTemplate<String, Object> redisTemplate) {
        this.jobRepository = jobRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.zookeeperClient = zookeeperClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<Task> getTasks(Long jobId) {
        return jobRepository.findOne(jobId).getStages()
                .stream()
                .flatMap(s -> s.getTasks().stream())
                .collect(Collectors.toList());
    }

    /**
     * Create a new {@link Job}
     *
     * @param job is the new {@link Job}
     * @return the newly create {@link Job}
     */
    @Override
    public Job createJob(Job job) {

        // Save the job to the permanent data store
        Job savedJob = jobRepository.save(job);

        // Save job to zookeeper
        zookeeperClient.save(savedJob);

        return savedJob;
    }

    /**
     * Get a distributed {@link Job}.
     *
     * @param id is the identifier for the {@link Job}
     * @return a distributed {@link Job} and its associated state
     */
    @Override
    public Job getJob(Long id) {
        return null;
    }

    /**
     * Create a new {@link Task}
     *
     * @param stageId is the id of the stage to add the new task to
     * @param task  is the new task to create
     * @return the newly created {@link Task}
     */
    @Override
    public Task createTask(Long stageId, Task task) {
        Stage stage = stageRepository.findOne(stageId);
        Assert.notNull(stage, "The job stage does not exist.");
        Task savedTask = taskRepository.save(task);
        stage.addTask(savedTask);
        stageRepository.save(stage);
        return savedTask;
    }
}
