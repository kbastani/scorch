package org.springframework.jobmanager.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jobmanager.stage.Stage;
import org.springframework.jobmanager.stage.StageRepository;
import org.springframework.jobmanager.task.Task;
import org.springframework.jobmanager.task.TaskRepository;
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

    @Autowired
    public JobServiceImpl(JobRepository jobRepository, StageRepository stageRepository, TaskRepository taskRepository) {
        this.jobRepository = jobRepository;
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
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
        Job savedJob = jobRepository.save(job);
        return savedJob;
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
