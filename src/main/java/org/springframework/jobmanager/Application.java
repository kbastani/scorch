package org.springframework.jobmanager;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jobmanager.event.EventType;
import org.springframework.jobmanager.job.Job;
import org.springframework.jobmanager.job.JobRepository;
import org.springframework.jobmanager.machine.Status;
import org.springframework.jobmanager.machine.TaskListener;
import org.springframework.jobmanager.stage.Stage;
import org.springframework.jobmanager.task.Task;
import org.springframework.jobmanager.task.TaskStateMachine;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

import java.util.List;

@SpringBootApplication
@ComponentScan
@EnableJpaAuditing
public class Application {

    @Autowired
    JobRepository jobRepository;

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

    @Bean(name = "taskStateMachine", initMethod = "init")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public TaskStateMachine taskStateMachine() {
        return new TaskStateMachine();
    }

    @Bean
    CommandLineRunner commandLineRunner(StateMachineFactory<Status,EventType> taskMachine) {
        return args -> {
            StateMachine<Status, EventType> taskStateMachine = taskMachine.getStateMachine();

            // State is a sequence of events
            List<EventType> stateEvents = Lists.newArrayList(EventType.RUN);

            Job job = new Job();
            // Create a set of tasks to track a job
            Stage stage = new Stage(Status.READY);
            stage.addTask(new Task(true, Status.READY));
            stage.addTask(new Task(true, Status.READY));
            stage.addTask(new Task(true, Status.READY));
            job.getStages().add(stage);

            job = jobRepository.save(job);
            taskStateMachine.addStateListener(new TaskListener(taskStateMachine));
            taskStateMachine.start();

        };
    }


}
