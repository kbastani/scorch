package org.springframework.jobmanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jobmanager.tasks.Events;
import org.springframework.jobmanager.tasks.States;
import org.springframework.jobmanager.tasks.TaskListener;
import org.springframework.jobmanager.tasks.Tasks;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListener;

@SpringBootApplication
public class Application {

    @Autowired
    Tasks tasks;

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

    @Bean
    public StateMachineListener<States, Events> stateMachineListener(StateMachine<States,Events> stateMachine) {
        TaskListener listener = new TaskListener();
        stateMachine.addStateListener(listener);
        return listener;
    }

    @Bean
    CommandLineRunner commandLineRunner(StateMachine<States,Events> stateMachine) {
        return args -> {
            stateMachine.start();
            System.out.println(stateMachine);
        };
    }
}
