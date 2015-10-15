package org.springframework.jobmanager.tasks;

import java.io.Serializable;

public class Task implements Serializable {

    private String id;
    private boolean active;

    public Task() {
    }

    public Task(String id, boolean active) {
        this.id = id;
        this.active = active;
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
}
