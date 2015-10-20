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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jobmanager.event.EventType;
import org.springframework.jobmanager.machine.Status;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachineSystemConstants;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.annotation.OnTransition;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.Map;


@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TaskStateMachineConfiguration {

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EnableStateMachineFactory(name = "taskMachine")
    static class TaskStateMachineConfig extends EnumStateMachineConfigurerAdapter<Status, EventType> {

        @Override
        public void configure(StateMachineStateConfigurer<Status, EventType> states) throws Exception {
            states.withStates()
                    .initial(Status.READY)
                    .states(EnumSet.allOf(Status.class));
        }

        @Override
        public void configure(StateMachineTransitionConfigurer<Status, EventType> transitions) throws Exception {
            transitions.withExternal()
                        .source(Status.READY)
                        .target(Status.STARTED)
                        .event(EventType.RUN)
                        .and()
                    .withExternal()
                        .source(Status.STARTED)
                        .target(Status.RUNNING)
                        .event(EventType.END)
                        .and()
                    .withExternal()
                        .source(Status.RUNNING)
                        .target(Status.FINISHED)
                        .event(EventType.CONTINUE)
                        .and()
                    .withExternal()
                        .source(Status.RUNNING)
                        .target(Status.ERROR)
                        .event(EventType.CANCEL);
        }

        @Bean
        public Guard<Status, EventType> tasksChoiceGuard() {
            return new Guard<Status, EventType>() {

                @Override
                public boolean evaluate(StateContext<Status, EventType> context) {
                    Map<Object, Object> variables = context.getExtendedState().getVariables();
                    return !(ObjectUtils.nullSafeEquals(variables.get("T1"), true));
                }
            };
        }

        @Bean
        public Action<Status, EventType> endTask() {
            return new Action<Status, EventType>() {

                @Override
                public void execute(StateContext<Status, EventType> context) {
                    Map<Object, Object> variables = context.getExtendedState().getVariables();
                    if (ObjectUtils.nullSafeEquals(variables.get("T1"), true)) {
                        context.getStateMachine().sendEvent(EventType.CONTINUE);
                    } else {
                        context.getStateMachine().sendEvent(EventType.CANCEL);
                    }
                }
            };
        }

        @Bean
        public Action<Status, EventType> automaticAction() {
            return new Action<Status, EventType>() {

                @Override
                public void execute(StateContext<Status, EventType> context) {
                    Map<Object, Object> variables = context.getExtendedState().getVariables();
                    if (ObjectUtils.nullSafeEquals(variables.get("T1"), true)) {
                        context.getStateMachine().sendEvent(EventType.CONTINUE);
                    } else {
                        context.getStateMachine().sendEvent(EventType.FALLBACK);
                    }
                }
            };
        }

        @Bean
        public Action<Status, EventType> success() {
            return new Action<Status, EventType>() {

                @Override
                public void execute(StateContext<Status, EventType> context) {
                    Map<Object, Object> variables = context.getExtendedState().getVariables();
                    variables.put("T1", Status.SUCCESS);
                    context.getStateMachine().sendEvent(EventType.END);
                }
            };
        }

        @Bean
        public Action<Status, EventType> fixAction() {
            return new Action<Status, EventType>() {

                @Override
                public void execute(StateContext<Status, EventType> context) {
                    Map<Object, Object> variables = context.getExtendedState().getVariables();
                    variables.put("T1", true);
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

        Status[] source() default {};

        Status[] target() default {};

    }

}
