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
 * actions in response to changes in the state of a {@link Task}.
 *
 * @author Kenny Bastani
 */
@WithStateMachine(name = "taskMachine")
@AutoConfigureBefore
@Order(Ordered.LOWEST_PRECEDENCE)
public class TaskStateMachine implements Serializable {

    @JsonIgnore
    private final static Log log = LogFactory.getLog(TaskStateMachine.class);

    // The RabbitMQ queue to send state change notifications to
    public static final String QUEUE_NAME = "scorch.actions";

    // The RabbitMQ client
    private RabbitTemplate rabbitTemplate;

    // Used for object serialization
    private ObjectMapper objectMapper;

    // The ZooKeeper client
    private ZookeeperClient zookeeperClient;

    /**
     * Constructs a new {@link TaskStateMachine} instance injecting it with its bean dependencies.
     *
     * @param rabbitTemplate  is the RabbitMQ client
     * @param objectMapper    is used for serialization
     * @param zookeeperClient is the ZooKeeper client
     */
    @Autowired
    public TaskStateMachine(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, ZookeeperClient zookeeperClient) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.zookeeperClient = zookeeperClient;
    }

    /**
     * Initialization method for this Spring bean.
     */
    public void init() {
    }

    /**
     * Get the task's ID from the state machine's context.
     *
     * @param extendedState is the context for the state machine.
     * @param state         is the state that the state machine has transitioned to.
     * @return the ID of the state machine's task.
     */
    private String getTaskId(ExtendedState extendedState, String state) {

        // Get taskId
        String taskId = (String) extendedState.getVariables().getOrDefault("id", null);

        // Perform action associated with state change to running
        log.info(String.format("Task %s state transitioned to %s: %s", taskId, state, extendedState.getVariables().keySet()));

        return taskId;
    }

    /**
     * Send a state change notification to RabbitMQ if the state machine is transitioning state
     * for the first time on the Scorch cluster.
     *
     * @param extendedState is the context of the state machine.
     * @param taskId        is the ID of the task.
     * @param from          is the state that the task is transitioning from.
     * @param to            is the state that the task is transitioning to.
     */
    private void sendStateChange(ExtendedState extendedState, String taskId, Status from, Status to) {

        Boolean sendMessage = (Boolean) extendedState
                .getVariables()
                .get(String.format("%s--%s", taskId, from));

        if (taskId != null && sendMessage != null) {
            try {
                rabbitTemplate.convertAndSend(QUEUE_NAME,
                        objectMapper.writeValueAsString(new StateChange(taskId, from, to)));
            } catch (JsonProcessingException e) {
                log.error(e);
            }
        }
    }

    /**
     * Dispatch a task's actions for the RUNNING transition.
     *
     * @param task is the task that is transitioning to RUNNING.
     */
    private void dispatchRun(Task task) {
        switch (task.getType()) {
            case JOB:
                runJob(task);
                break;
            case STAGE:
                runStage(task);
                break;
        }
    }

    /**
     * Perform an action for a running transition of a {@link Stage}.
     *
     * @param task is the task that represents the state of the stage.
     */
    private void runStage(Task task) {
        Stage stage = zookeeperClient.get(Stage.class, task.getId());
        stage.getTasks().forEach(t -> {
            StateMachineRepository.getStateMachineBean(t.getId());
            Event te = new Event();
            te.setEventType(EventType.RUN);
            te.setTargetId(t.getId());
            te.setId("0");
            zookeeperClient.save(te, CreateMode.PERSISTENT_SEQUENTIAL);
        });
    }

    /**
     * Perform an action for a running transition of a {@link Job}.
     *
     * @param task is the task that represents the state of the job.
     */
    private void runJob(Task task) {
        Job job = zookeeperClient.get(Job.class, task.getId());
        Stage firstStage = job.getStages().stream().findFirst().get();
        Event event = new Event();
        event.setEventType(EventType.RUN);
        event.setTargetId(firstStage.getId());
        event.setId("0");
        zookeeperClient.save(event, CreateMode.PERSISTENT_SEQUENTIAL);
    }

    /**
     * Dispatches an action for a task that is transitioning to FINISH.
     *
     * @param extendedState is the context of the state machine.
     * @param taskId        is the ID of the task.
     * @param task          is the task of the state machine.
     */
    private void dispatchFinish(ExtendedState extendedState, String taskId, Task task) {
        switch (task.getType()) {
            case JOB:
                if (task.getStatus() != Status.SUCCESS) {
                    extendedState
                            .getVariables()
                            .put(String.format("%s--%s", taskId, Status.RUNNING), true);
                }
                break;
            case STAGE:
                if (task.getStatus() != Status.SUCCESS) {
                    extendedState
                            .getVariables()
                            .put(String.format("%s--%s", taskId, Status.RUNNING), true);
                }
                break;
        }
    }

    /**
     * Performs an action for a stage that is transitioning to SUCCESS.
     *
     * @param taskId is the ID of the stage's state machine.
     * @param task   is the task of the stage's state machine.
     */
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

    /**
     * Performs an action for a task that is transitioning to SUCCESS.
     *
     * @param taskId is the ID of the task.
     * @param task   is the task.
     */
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

    /**
     * Dispatches an action for a task that is transitioning to SUCCESS.
     *
     * @param taskId is the ID of the task.
     * @param task   is the task.
     */
    private void dispatchSuccess(String taskId, Task task) {
        switch (task.getType()) {
            case STAGE:
                stageSuccess(taskId, task);
                break;
            case TASK:
                taskSuccess(taskId, task);
                break;
        }
    }

    /**
     * Executed when a {@link Task} transitions to the READY state.
     *
     * @param extendedState is the attached context of the task's state machine.
     */
    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.READY)
    public void taskReady(ExtendedState extendedState) {

        String taskId = getTaskId(extendedState, "READY");

        // Send state change notification
        sendStateChange(extendedState, taskId, Status.PENDING, Status.READY);
    }

    /**
     * Executed when a {@link Task} transitions to the STARTED state.
     *
     * @param extendedState is the attached context of the task's state machine.
     */
    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.STARTED)
    public void taskStart(ExtendedState extendedState) {

        String taskId = getTaskId(extendedState, "STARTED");

        // Send state change notification
        sendStateChange(extendedState, taskId, Status.READY, Status.STARTED);
    }

    /**
     * Executed when a {@link Task} transitions to the RUNNING state.
     *
     * @param extendedState is the attached context of the task's state machine.
     */
    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.RUNNING)
    public void taskRun(ExtendedState extendedState) {

        String taskId = getTaskId(extendedState, "RUNNING");

        // Get stage task
        Task task = zookeeperClient.get(Task.class, taskId);

        // Tasks can target jobs, stages, and tasks
        dispatchRun(task);

        // Send state change notification
        sendStateChange(extendedState, taskId, Status.STARTED, Status.RUNNING);
    }

    /**
     * Executed when a {@link Task} transitions to the FINISHED state.
     *
     * @param extendedState is the attached context of the task's state machine.
     */
    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.FINISHED)
    public void taskFinish(ExtendedState extendedState) {

        // Perform action associated with state change to finished
        String taskId = getTaskId(extendedState, "FINISHED");
        Task task = zookeeperClient.get(Task.class, taskId);
        dispatchFinish(extendedState, taskId, task);
        sendStateChange(extendedState, taskId, Status.RUNNING, Status.FINISHED);
    }

    /**
     * Executed when a {@link Task} transitions to the SUCCESS state.
     *
     * @param extendedState is the attached context of the task's state machine.
     */
    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.SUCCESS)
    public void taskSuccess(ExtendedState extendedState) {

        // Perform action associated with state change to finished
        String taskId = getTaskId(extendedState, "SUCCESS");
        Task task = zookeeperClient.get(Task.class, taskId);
        dispatchSuccess(taskId, task);

        // Save the final state of the task
        if (task.getStatus() != Status.SUCCESS) {
            task.setStatus(Status.SUCCESS);
            zookeeperClient.save(task);
        }
    }
}
