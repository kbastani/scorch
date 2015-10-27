package com.example;

import com.example.event.Event;
import com.example.event.EventClient;
import com.example.event.EventType;
import com.example.job.Job;
import com.example.job.JobClient;
import com.example.job.Status;
import com.example.message.Receiver;
import com.example.stage.Stage;
import com.example.task.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@EnableDiscoveryClient
@Configuration
@EnableFeignClients
@SpringBootApplication
@EnableRabbit
public class DemoApplication {

    final static String queueName = "scorch.actions";

    @Autowired
    RabbitTemplate rabbitTemplate;

    private final static Log log = LogFactory.getLog(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(JobClient jobClient, EventClient eventClient) {
        return (arrrrgImAPirate) -> {
            Job job = jobClient
                    .createJob(new Job(Collections
                            .singletonList(new Stage(Collections
                                    .singletonList(new Task(false, Status.PENDING)), Status.PENDING))));

            log.info(job);

            Event event = new Event("0", EventType.RUN, job.getStages().get(0).getTasks().get(0).getId());

            eventClient.sendEvent(event);

            log.info(jobClient.getJob(job.getId()));
        };
    }

    @Bean
    Queue queue() {
        return new Queue(queueName, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("scorch.exchange");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queueName);
    }

    @Bean
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    Receiver receiver(ObjectMapper objectMapper) {
        return new Receiver(objectMapper);
    }

    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
