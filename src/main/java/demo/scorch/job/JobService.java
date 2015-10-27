package demo.scorch.job;

import demo.scorch.task.Task;

import java.util.List;

/**
 * The {@link JobService} provides a set of methods
 * for managing {@link Job} objects.
 *
 * @author Kenny Bastani
 */
public interface JobService {

    /**
     * Get all tasks for a given {@link Job} ID.
     *
     * @return a set of tasks that beString to a {@link Job}.
     */
    List<Task> getTasks(String jobId);

    /**
     * Create a new {@link Job}.
     *
     * @param job is the new {@link Job}
     * @return the newly create {@link Job}
     */
    Job createJob(Job job);

    /**
     * Get a distributed {@link Job}.
     *
     * @param id is the identifier for the {@link Job}
     * @return a distributed {@link Job} and its associated state
     */
    Job getJob(String id);

    /**
     * Create a new {@link Task}
     *
     * @param stageId is the id of the job stage to add the new task to
     * @param task    is the new task to create
     * @return the newly created {@link Task}
     */
    Task createTask(String stageId, Task task);
}
