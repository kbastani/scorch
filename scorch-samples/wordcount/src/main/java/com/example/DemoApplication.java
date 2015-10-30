package com.example;

import com.example.action.Run;
import com.example.action.Tuple;
import com.example.event.EventClient;
import com.example.job.Job;
import com.example.job.JobClient;
import com.example.job.Status;
import com.example.message.Receiver;
import com.example.stage.Stage;
import com.example.task.Task;
import com.example.task.TaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@EnableDiscoveryClient
@Configuration
@EnableFeignClients
@SpringBootApplication
@EnableRabbit
public class DemoApplication {

    final static String queueName = "scorch.actions";

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    private final static Log log = LogFactory.getLog(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(JobClient jobClient, EventClient eventClient) {
        return (arrrrgImAPirate) -> {

            ConcurrentLinkedQueue<String> sentences = new ConcurrentLinkedQueue<>();

            sentences.add("The first word in a sentence is interesting");
            sentences.add("The second word in a sentence is interesting");
            sentences.add("The third word in a sentence is interesting");
            sentences.add("The fourth word in a paragraph is interesting");
            sentences.add("The fifth word in a sentence is interesting");
            sentences.add("The sixth word in a paragraph is interesting");
            sentences.add("The seventh word in a sentence is interesting");
            sentences.add("The eighth word in a document is interesting");
            sentences.add("The ninth word in a sentence is interesting");
            sentences.add("The tenth word in a paragraph is interesting");
            sentences.add("The eleventh word in a sentence is interesting");
            sentences.add("The twelfth word in a paragraph is interesting");
            sentences.add("The thirteenth word in a sentence is interesting");
            sentences.add("The fourteenth word in a document is interesting");
            sentences.add("The fifteenth word in a sentence is interesting");
            sentences.add("The sixteenth word in a paragraph is interesting");
            sentences.add("The seventeenth word in a sentence is interesting");
            sentences.add("The nineteenth word in a document is interesting");
            sentences.add("The twentieth word in a sentence is interesting");
            sentences.add("The twenty-first word in a paragraph is interesting");
            sentences.add("The twenty-second word in a sentence is interesting");
            sentences.add("The twenty-third word in a document is interesting");
            sentences.add("The twenty-fourth word in a document is interesting");
            sentences.add("The twenty-fifth word in a document is interesting");
            sentences.add("The twenty-sixth word in a document is interesting");

            Stage stage1 = new Stage();
            Stage stage2 = new Stage();
            stage1.setStatus(Status.PENDING);
            stage1.setTasks(sentences.stream().map(a -> new Task(false, Status.PENDING)).collect(Collectors.toList()));
            stage2.setTasks(Collections.singletonList(new Task(false, Status.PENDING)));
            Job job = jobClient.createJob(new Job(Collections.singletonList(stage1)));

            List<Task> tasks = job.getStages().get(0).getTasks();
            List<Task> tasks2 = job.getStages().get(1).getTasks();

            for (Task task : tasks) {

                // Create an action for counting the number of words in a sentence
                TaskRepository.taskActionCache.put(task.getId(), new Run<String, Integer>(task.getId(), a ->
                        a.map(sentence -> Arrays.asList(sentence.getT1().toLowerCase().replace("\\W", "")
                                .split("\\s")).stream())
                                .flatMap(word -> word)
                                .map(word -> new Tuple<String, Integer>(word, 1)), redisTemplate, objectMapper));

                // Set initial data for task
                redisTemplate.execute(
                        (RedisCallback) redisConnection -> {
                            try {
                                redisConnection.set(task.getId().getBytes(), objectMapper.writeValueAsString(
                                        Collections.singletonList(sentences.remove())
                                                .stream()
                                                .map(sentence ->
                                                        new Tuple<>(sentence, 0))
                                                .collect(Collectors.toSet())).getBytes());
                            } catch (JsonProcessingException e) {
                                log.error(e);
                            }

                            return task.getId();
                        });
            }

            for (Task task : tasks2) {

                // Create an action for counting the number of words in a sentence
                TaskRepository.taskActionCache.put(task.getId(), new Run<String, Integer>(task.getId(), a ->
                {
                    
                }, redisTemplate, objectMapper));

                // Set initial data for task
                redisTemplate.execute(
                        (RedisCallback) redisConnection -> {
                            try {
                                redisConnection.set(task.getId().getBytes(), objectMapper.writeValueAsString(tasks.stream().map(Task::getId)).getBytes());
                            } catch (JsonProcessingException e) {
                                log.error(e);
                            }
                            return task.getId();
                        });
            }
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
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, MessageListenerAdapter
            listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    Receiver receiver(ObjectMapper objectMapper, EventClient eventClient) {
        return new Receiver(objectMapper, eventClient, redisTemplate);
    }

    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }


}
