/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo.scorch.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.scorch.event.Event;
import demo.scorch.event.EventType;
import demo.scorch.job.Job;
import demo.scorch.machine.Status;
import demo.scorch.message.StateChange;
import demo.scorch.stage.Stage;
import demo.scorch.zookeeper.ZookeeperClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.CreateMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.annotation.WithStateMachine;

import java.io.Serializable;

/**
 * The {@link TaskStateMachine} is the task management module that performs
 * actions in response to changes in state.
 *
 * @author Kenny Bastani
 */
@WithStateMachine(name = "taskMachine")
@AutoConfigureBefore
@Order(Ordered.LOWEST_PRECEDENCE)
public class TaskStateMachine implements Serializable {

    @JsonIgnore
    private final static Log log = LogFactory.getLog(TaskStateMachine.class);
    public static final String QUEUE_NAME = "scorch.actions";
    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;
    private ZookeeperClient zookeeperClient;

    @Autowired
    public TaskStateMachine(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, ZookeeperClient zookeeperClient) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.zookeeperClient = zookeeperClient;
    }

    public void init() {
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.READY)
    public void taskReady(ExtendedState extendedState) {
        // Perform action associated with state change to running
        log.info(String.format("Task state transitioned to READY: %s", extendedState.getVariables().keySet()));

        // Get taskId
        String taskId = (String) extendedState.getVariables().getOrDefault("id", null);
        Boolean sendMessage = (Boolean) extendedState
                .getVariables()
                .get(String.format("%s--%s", taskId, Status.PENDING));

        if (taskId != null && sendMessage != null) {
            try {
                rabbitTemplate.convertAndSend(QUEUE_NAME, objectMapper.writeValueAsString(new StateChange(taskId, Status.PENDING, Status.READY)));
            } catch (JsonProcessingException e) {
                log.error(e);
            }
        }
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.STARTED)
    public void taskStart(ExtendedState extendedState) {
        // Perform action associated with state change to running
        log.info(String.format("Task state transitioned to STARTED: %s", extendedState.getVariables().keySet()));

        // Get taskId
        String taskId = (String) extendedState.getVariables().getOrDefault("id", null);
        Boolean sendMessage = (Boolean) extendedState
                .getVariables()
                .get(String.format("%s--%s", taskId, Status.READY));

        if (taskId != null && sendMessage != null) {
            try {
                rabbitTemplate.convertAndSend(QUEUE_NAME, objectMapper.writeValueAsString(new StateChange(taskId, Status.READY, Status.STARTED)));
            } catch (JsonProcessingException e) {
                log.error(e);
            }
        }
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.RUNNING)
    public void taskRun(ExtendedState extendedState) {
        // Perform action associated with state change to running
        log.info(String.format("Task state transitioned to RUNNING: %s", extendedState.getVariables().keySet()));

        // Get taskId
        String taskId = (String) extendedState.getVariables().getOrDefault("id", null);

        // Get stage task
        Task task = zookeeperClient.get(Task.class, taskId);

        switch (task.getType()) {
            case JOB:
                Job job = zookeeperClient.get(Job.class, task.getId());
                Stage firstStage = job.getStages().stream().findFirst().get();
                Event event = new Event();
                event.setEventType(EventType.RUN);
                event.setTargetId(firstStage.getId());
                event.setId("0");
                zookeeperClient.save(event, CreateMode.PERSISTENT_SEQUENTIAL);
                break;
            case STAGE:
                Stage stage = zookeeperClient.get(Stage.class, task.getId());
                stage.getTasks().forEach(t -> {
                    StateMachineRepository.getStateMachineBean(t.getId());
                    Event te = new Event();
                    te.setEventType(EventType.RUN);
                    te.setTargetId(t.getId());
                    te.setId("0");
                    zookeeperClient.save(te, CreateMode.PERSISTENT_SEQUENTIAL);
                });
                break;
        }

        Boolean sendMessage = (Boolean) extendedState
                .getVariables()
                .get(String.format("%s--%s", taskId, Status.STARTED));

        if (taskId != null && sendMessage != null) {
            try {
                rabbitTemplate.convertAndSend(QUEUE_NAME, objectMapper.writeValueAsString(new StateChange(taskId, Status.STARTED, Status.RUNNING)));
            } catch (JsonProcessingException e) {
                log.error(e);
            }
        }
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.FINISHED)
    public void taskFinish(ExtendedState extendedState) {
        // Perform action associated with state change to finished
        log.info("Task state transitioned to FINISHED");

        // Get taskId
        String taskId = (String) extendedState.getVariables().getOrDefault("id", null);

        Task task = zookeeperClient.get(Task.class, taskId);

        switch (task.getType()) {
            case JOB:
                if(task.getStatus() != Status.SUCCESS) {
                    extendedState
                            .getVariables()
                            .put(String.format("%s--%s", taskId, Status.RUNNING), true);
                }
                break;
            case STAGE:
                if(task.getStatus() != Status.SUCCESS) {
                    extendedState
                            .getVariables()
                            .put(String.format("%s--%s", taskId, Status.RUNNING), true);
                }
                break;
        }

        Boolean sendMessage = (Boolean) extendedState
                .getVariables()
                .get(String.format("%s--%s", taskId, Status.RUNNING));

        if (taskId != null && sendMessage != null) {
            try {
                rabbitTemplate.convertAndSend(QUEUE_NAME, objectMapper.writeValueAsString(new StateChange(taskId, Status.RUNNING, Status.FINISHED)));
            } catch (JsonProcessingException e) {
                log.error(e);
            }
        }
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.SUCCESS)
    public void taskSuccess(ExtendedState extendedState) {
        // Perform action associated with state change to finished
        log.info("Task state transitioned to SUCCESS");

        // Get taskId
        String taskId = (String) extendedState.getVariables().getOrDefault("id", null);

        Task task = zookeeperClient.get(Task.class, taskId);

        switch (task.getType()) {
            case STAGE:
                stageSuccess(taskId, task);
                break;
            case TASK:
                taskSuccess(taskId, task);
                break;
        }

        // Put into final state
        if (task.getStatus() != Status.SUCCESS) {
            task.setStatus(Status.SUCCESS);
            zookeeperClient.save(task);
        }
    }

    private void stageSuccess(String taskId, Task task) {
        Task stageTask = zookeeperClient.get(Task.class, task.getId());

        if (stageTask.getStatus() != Status.SUCCESS) {
            // Get stage and update task
            Job job = zookeeperClient.get(Job.class, task.getJobId());
            job.getStages().stream().filter(t -> t.getId().equals(taskId)).forEach(t -> {
                t.setStatus(Status.SUCCESS);
                zookeeperClient.save(t);
            });
            zookeeperClient.save(job);

            if (job.getStages().stream().allMatch(a -> a.getStatus() == Status.SUCCESS)) {
                StateMachineRepository.getStateMachineBean(job.getId());
                Event te = new Event();
                te.setEventType(EventType.END);
                te.setTargetId(job.getId());
                te.setId("0");
                zookeeperClient.save(te, CreateMode.PERSISTENT_SEQUENTIAL);
            } else {
                Stage stage = job.getStages().stream().filter(t -> t.getStatus() == Status.READY).findFirst().get();
                StateMachineRepository.getStateMachineBean(stage.getId());
                Event te = new Event();
                te.setEventType(EventType.RUN);
                te.setTargetId(stage.getId());
                te.setId("0");
                zookeeperClient.save(te, CreateMode.PERSISTENT_SEQUENTIAL);
            }
        }
    }

    private void taskSuccess(String taskId, Task task) {
        // Get stage task
        Task stageTask = zookeeperClient.get(Task.class, task.getStageId());

        if (stageTask.getStatus() != Status.SUCCESS) {
            // Get stage and update task
            Stage stage = zookeeperClient.get(Stage.class, task.getStageId());
            stage.getTasks().stream().filter(t -> t.getId().equals(taskId)).forEach(t -> {
                t.setStatus(Status.SUCCESS);
                zookeeperClient.save(t);
            });
            zookeeperClient.save(stage);

            if (stage.getTasks().stream().allMatch(a -> a.getStatus() == Status.SUCCESS)) {
                StateMachineRepository.getStateMachineBean(stage.getId());
                Event te = new Event();
                te.setEventType(EventType.END);
                te.setTargetId(stage.getId());
                te.setId("0");
                zookeeperClient.save(te, CreateMode.PERSISTENT_SEQUENTIAL);
            }
        }
    }
}
