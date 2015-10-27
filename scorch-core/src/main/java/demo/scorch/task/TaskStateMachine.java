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
import demo.scorch.machine.Status;
import demo.scorch.message.StateChange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    @Autowired
    public TaskStateMachine(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void init() {
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.RUNNING)
    public void taskBegin(ExtendedState extendedState) {
        // Perform action associated with state change to running
        log.info(String.format("Task state transitioned to RUNNING: %s", extendedState.getVariables().keySet()));

        // Get taskId
        String taskId = (String) extendedState.getVariables().getOrDefault("id", null);

        if(taskId != null) {
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

        if(taskId != null) {
            try {
                rabbitTemplate.convertAndSend(QUEUE_NAME, objectMapper.writeValueAsString(new StateChange(taskId, Status.RUNNING, Status.FINISHED)));
            } catch (JsonProcessingException e) {
                log.error(e);
            }
        }
    }
}
