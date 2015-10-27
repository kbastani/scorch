package com.example.job;

import com.example.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Each {@link Job} contains a set of {@link Stage}.
 *
 * @author Kenny Bastani
 */
public class Job {

    private String id;
    private List<Stage> stages = new ArrayList<>();

    public Job() {
    }

    public Job(List<Stage> stages) {
        this.stages = stages;
    }

    public Job(String id, List<Stage> stages) {
        this.id = id;
        this.stages = stages;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id='" + id + '\'' +
                ", stages=" + stages +
                '}';
    }
}
