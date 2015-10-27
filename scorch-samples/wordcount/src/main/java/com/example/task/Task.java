package com.example.task;

import com.example.job.Status;

import java.io.Serializable;

public class Task implements Serializable {

    private String id;
    private boolean active;
    private Status status = Status.PENDING;

    public Task() {
    }

    public Task(boolean active, Status status) {
        this.active = active;
        this.status = status;
    }

    public Task(String id, boolean active, Status status) {
        this.id = id;
        this.active = active;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", active=" + active +
                ", status=" + status +
                '}';
    }
}
