package com.example.stage;

import com.example.job.Status;
import com.example.task.Task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Each Stage belongs to a job and contains a set of tasks
 */
public class Stage implements Serializable {

    private String id;
    private List<Task> tasks = new ArrayList<>();
    private Status status;

    public Stage() {
    }

    public Stage(List<Task> tasks, Status status) {
        this.tasks = tasks;
        this.status = status;
    }

    public Stage(String id, List<Task> tasks, Status status) {
        this.id = id;
        this.tasks = tasks;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public Optional<Task> getTask(String id) {
        Optional<Task> task = this.tasks
                .stream()
                .filter(a -> Objects.equals(a.getId(), id))
                .findFirst();

        return task;
    }

    @Override
    public String toString() {
        return "Stage{" +
                "id='" + id + '\'' +
                ", tasks=" + tasks +
                ", status=" + status +
                '}';
    }
}
