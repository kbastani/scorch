package org.springframework.jobmanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jobmanager.event.EventType;
import org.springframework.jobmanager.machine.States;
import org.springframework.jobmanager.machine.TaskListener;
import org.springframework.jobmanager.machine.Tasks;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListener;

@SpringBootApplication
@ComponentScan
@EnableJpaAuditing
public class Application {

    @Autowired
    Tasks tasks;

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

    @Bean
    public StateMachineListener<States, EventType> stateMachineListener(StateMachine<States,EventType> stateMachine) {
        TaskListener listener = new TaskListener();
        stateMachine.addStateListener(listener);
        return listener;
    }

    @Bean
    CommandLineRunner commandLineRunner(StateMachine<States,EventType> stateMachine) {
        return args -> {
            stateMachine.start();
            System.out.println(stateMachine);
        };
    }


}
