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
package org.springframework.jobmanager.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jobmanager.event.EventType;
import org.springframework.jobmanager.machine.Status;
import org.springframework.jobmanager.stage.Stage;
import org.springframework.jobmanager.stage.StageRepository;
import org.springframework.jobmanager.task.TaskStateMachineConfiguration.StatesOnTransition;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

@WithStateMachine(name = "taskMachine")
@AutoConfigureBefore
@Order(Ordered.LOWEST_PRECEDENCE)
public class TaskStateMachine implements Serializable {

    @JsonProperty("task")
    Stage stage;

    @JsonIgnore
    private final static Log log = LogFactory.getLog(TaskStateMachine.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private StageRepository stageRepository;

    @Autowired
    private StateMachineFactory<Status, EventType> stateMachineFactory;

    private StateMachine<Status, EventType> taskMachine;

    public void run() {
        taskMachine.sendEvent(EventType.RUN);
    }

    public void fix() {
        stage.getTasks().stream().forEach(task -> task.setActive(true));
        taskMachine.sendEvent(EventType.FIX);
    }

    public void init() {
         taskMachine = stateMachineFactory.getStateMachine();
    }

    public void fail(Long id) {
        Optional<Task> task = stage.getTask(id);

        if (task.isPresent()) {
            task.get().setActive(false);
        }

        stageRepository.save(stage);
    }

    public void createTask(Task task) {
        // Validate
        Assert.notNull(task, "Task must not be null.");

        stage.addTask(task);

        stage = stageRepository.save(stage);
    }

    @StatesOnTransition(source = Status.READY, target = Status.RUNNING)
    public void taskBegin(ExtendedState extendedState) {
        runTask(1L, extendedState);
    }

    @StatesOnTransition(target = Status.FINISHED)
    public void taskFinish(ExtendedState extendedState) {
        Map<Object, Object> variables = extendedState.getVariables();

        Task task = (Task)variables.get(1L);

        if(task != null) {
            task.setState(Status.FINISHED);
            taskRepository.save(task);
            // Queue event to stage
        }

        extendedState.getVariables().put(1L, task);
    }

    private void runTask(Long id, ExtendedState extendedState) {
        log.info("run task on " + id);
        extendedState.getVariables().put(id, taskRepository.findOne(id));
        log.info("run task on " + id + " done");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public String toString() {
        return "Tasks " + stage.toString();
    }

}
