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
import demo.scorch.machine.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
public class TaskStateMachine implements Serializable, ApplicationContextAware {

    @JsonIgnore
    private final static Log log = LogFactory.getLog(TaskStateMachine.class);
    private ApplicationContext applicationContext;

    public void init() {

    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.RUNNING)
    public void taskBegin(ExtendedState extendedState) {
        // Perform action associated with state change to running
        log.info("Task state transitioned to RUNNING");
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.FINISHED)
    public void taskFinish(ExtendedState extendedState) {
        // Perform action associated with state change to finished
        log.info("Task state transitioned to FINISHED");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
