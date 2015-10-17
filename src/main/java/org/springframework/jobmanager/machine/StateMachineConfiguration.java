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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jobmanager.event.EventType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachineSystemConstants;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.annotation.OnTransition;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;


@Configuration
@DependsOn("stageRepository")
public class StateMachineConfiguration {

	@Configuration
	@EnableStateMachine
	static class StateMachineConfig
			extends EnumStateMachineConfigurerAdapter<States, EventType> {

		@Override
		public void configure(StateMachineStateConfigurer<States, EventType> states)
				throws Exception {
			states
				.withStates()
					.initial(States.READY)
					.fork(States.FORK)
					.state(States.TASKS)
					.join(States.JOIN)
					.choice(States.CHOICE)
					.state(States.ERROR)
					.and()
					.withStates()
						.parent(States.TASKS)
						.initial(States.T1)
						.end(States.T1E)
						.and()
					.withStates()
						.parent(States.TASKS)
						.initial(States.T2)
						.end(States.T2E)
						.and()
					.withStates()
						.parent(States.TASKS)
						.initial(States.T3)
						.end(States.T3E)
						.and()
					.withStates()
						.parent(States.ERROR)
						.initial(States.AUTOMATIC)
						.state(States.AUTOMATIC, automaticAction(), null)
						.state(States.MANUAL);
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<States, EventType> transitions)
				throws Exception {
			transitions
				.withExternal()
					.source(States.READY).target(States.FORK)
					.event(EventType.RUN)
					.and()
				.withFork()
					.source(States.FORK).target(States.TASKS)
					.and()
				.withExternal()
					.source(States.T1).target(States.T1E)
					.and()
				.withExternal()
					.source(States.T2).target(States.T2E)
					.and()
				.withExternal()
					.source(States.T3).target(States.T3E)
					.and()
				.withJoin()
					.source(States.TASKS).target(States.JOIN)
					.and()
				.withExternal()
					.source(States.JOIN).target(States.CHOICE)
					.and()
				.withChoice()
					.source(States.CHOICE)
					.first(States.ERROR, tasksChoiceGuard())
					.last(States.READY)
					.and()
				.withExternal()
					.source(States.ERROR).target(States.READY)
					.event(EventType.CONTINUE)
					.and()
				.withExternal()
					.source(States.AUTOMATIC).target(States.MANUAL)
					.event(EventType.FALLBACK)
					.and()
				.withInternal()
					.source(States.MANUAL)
					.action(fixAction())
					.event(EventType.FIX);
		}

		@Bean
		public Guard<States, EventType> tasksChoiceGuard() {
			return new Guard<States, EventType>() {

				@Override
				public boolean evaluate(StateContext<States, EventType> context) {
					Map<Object, Object> variables = context.getExtendedState().getVariables();
					return !(ObjectUtils.nullSafeEquals(variables.get("T1"), true)
							&& ObjectUtils.nullSafeEquals(variables.get("T2"), true)
							&& ObjectUtils.nullSafeEquals(variables.get("T3"), true));
				}
			};
		}

		@Bean
		public Action<States, EventType> automaticAction() {
			return new Action<States, EventType>() {

				@Override
				public void execute(StateContext<States, EventType> context) {
					Map<Object, Object> variables = context.getExtendedState().getVariables();
					if (ObjectUtils.nullSafeEquals(variables.get("T1"), true)
							&& ObjectUtils.nullSafeEquals(variables.get("T2"), true)
							&& ObjectUtils.nullSafeEquals(variables.get("T3"), true)) {
						context.getStateMachine().sendEvent(EventType.CONTINUE);
					} else {
						context.getStateMachine().sendEvent(EventType.FALLBACK);
					}
				}
			};
		}

		@Bean
		public Action<States, EventType> fixAction() {
			return new Action<States, EventType>() {

				@Override
				public void execute(StateContext<States, EventType> context) {
					Map<Object, Object> variables = context.getExtendedState().getVariables();
					variables.put("T1", true);
					variables.put("T2", true);
					variables.put("T3", true);
					context.getStateMachine().sendEvent(EventType.CONTINUE);
				}
			};
		}

		@Bean(name = StateMachineSystemConstants.TASK_EXECUTOR_BEAN_NAME)
		public TaskExecutor taskExecutor() {
			ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
			taskExecutor.setCorePoolSize(5);
			return taskExecutor;
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@OnTransition
	public static @interface StatesOnTransition {

		States[] source() default {};

		States[] target() default {};

	}

}
