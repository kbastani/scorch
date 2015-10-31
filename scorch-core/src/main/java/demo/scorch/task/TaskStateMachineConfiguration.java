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

import demo.scorch.event.EventType;
import demo.scorch.machine.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.StateMachineSystemConstants;
import org.springframework.statemachine.annotation.OnTransition;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumSet;

/**
 * Describes the structure and configuration of a {@link TaskStateMachine}.
 *
 * @author Kenny Bastani
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TaskStateMachineConfiguration {

    /**
     * The {@link TaskStateMachineConfig} is the configuration for the state machine of a task.
     */
    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EnableStateMachineFactory(name = "taskMachine")
    public static class TaskStateMachineConfig extends EnumStateMachineConfigurerAdapter<Status, EventType> {

        /**
         * Configure the state machine.
         *
         * @param states is the state machine configurer.
         * @throws Exception is the exception that is thrown during configuration.
         */
        @Override
        public void configure(StateMachineStateConfigurer<Status, EventType> states) throws Exception {
            states.withStates()
                    .initial(Status.READY)
                    .states(EnumSet.allOf(Status.class));
        }

        /**
         * Configure the procedures of a state machine for a task.
         *
         * @param transitions is the transitions for the state machine configurer.
         * @throws Exception is the exception that is thrown during configuration.
         */
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
                    .event(EventType.CANCEL)
                    .and()
                    .withExternal()
                    .source(Status.FINISHED)
                    .target(Status.SUCCESS)
                    .event(EventType.STOP);
        }

        /**
         * Is a bean for a task executor for the state machine.
         *
         * @return a {@link TaskExecutor} for the state machine.
         */
        @Bean(name = StateMachineSystemConstants.TASK_EXECUTOR_BEAN_NAME)
        public TaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
            taskExecutor.setCorePoolSize(40);
            return taskExecutor;
        }
    }

    /**
     * Is an annotation for transitioning the state of a task's state machine.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @OnTransition
    public @interface StatesOnTransition {
        Status[] source() default {};

        Status[] target() default {};
    }
}
