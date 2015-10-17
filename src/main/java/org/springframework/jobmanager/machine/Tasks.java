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
package org.springframework.jobmanager.machine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jobmanager.event.EventType;
import org.springframework.jobmanager.machine.StateMachineConfiguration.StatesOnTransition;
import org.springframework.jobmanager.stage.Stage;
import org.springframework.jobmanager.stage.StageRepository;
import org.springframework.jobmanager.task.Task;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

@WithStateMachine
@Service
public class Tasks implements Serializable {

    @JsonProperty("tasks")
    Stage stage;

	@JsonIgnore
	private final static Log log = LogFactory.getLog(Tasks.class);

    @Autowired
    private StageRepository stageRepository;

	@JsonIgnore
	@Autowired
	private StateMachine<States, EventType> stateMachine;

    public void init() {
        stage = new Stage();
        stage.addTask(new Task(true));
        stage.addTask(new Task(true));
        stage.addTask(new Task(true));
        stage = stageRepository.save(stage);
    }

	public void run() {
		stateMachine.sendEvent(EventType.RUN);
	}

	public void fix() {
		stage.getTasks().stream().forEach(task -> task.setActive(true));
		stateMachine.sendEvent(EventType.FIX);
	}

	public void fail(Long id) {
		Optional<Task> task = stage.getTask(id);

        if(task.isPresent()) {
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

	@StatesOnTransition(target = States.T1)
	public void taskT1(ExtendedState extendedState) {
		runTask(0L, extendedState);
	}

	@StateMachineConfiguration.StatesOnTransition(target = States.T2)
	public void taskT2(ExtendedState extendedState) {
		runTask(1L, extendedState);
	}

	@StatesOnTransition(target = States.T3)
	public void taskT3(ExtendedState extendedState) {
		runTask(2L, extendedState);
	}

	@StatesOnTransition(target = States.AUTOMATIC)
	public void automaticFix(ExtendedState extendedState) {
		Map<Object, Object> variables = extendedState.getVariables();
		variables.put(0L, true);
		stage.getTask(0L).get().setActive(true);
	}

	private void runTask(Long id, ExtendedState extendedState) {
		log.info("run task on " + id);
		sleep(2000);
		extendedState.getVariables().put(id, stage.getTask(id).get());
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
