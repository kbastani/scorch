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
import demo.scorch.event.EventType;
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
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;

import java.io.Serializable;

@WithStateMachine(name = "taskMachine")
@AutoConfigureBefore
@Order(Ordered.LOWEST_PRECEDENCE)
public class TaskStateMachine implements Serializable, ApplicationContextAware {

    @JsonIgnore
    private final static Log log = LogFactory.getLog(TaskStateMachine.class);
    private ApplicationContext applicationContext;

    public void run(String id) {
        StateMachine<Status, EventType> stateMachine = getStateMachineById(id);
        if (stateMachine != null) {
            stateMachine.sendEvent(EventType.RUN);
        }
    }

    private StateMachine<Status, EventType> getStateMachineById(String id) {
        Object stateMachineBean = applicationContext.getBean(id);
        StateMachine<Status, EventType> stateMachine = null;
        if (stateMachineBean != null && stateMachineBean instanceof StateMachine) {
            stateMachine = (StateMachine<Status, EventType>) stateMachineBean;
        }
        return stateMachine;
    }

    public void fix(String id) {
        StateMachine<Status, EventType> stateMachine = getStateMachineById(id);
        if (stateMachine != null)
            stateMachine.sendEvent(EventType.FIX);
    }

    public void init() {
    }

    public void fail(String id) {
    }

    public void createTask(Task task) {
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.RUNNING)
    public void taskBegin(ExtendedState extendedState) {
        runTask(extendedState.get("id", String.class), extendedState);
    }

    @TaskStateMachineConfiguration.StatesOnTransition(target = Status.FINISHED)
    public void taskFinish(ExtendedState extendedState) {
    }

    private void runTask(String id, ExtendedState extendedState) {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
