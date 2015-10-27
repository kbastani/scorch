package com.example;

import com.example.action.Run;
import com.example.action.Tuple;
import com.example.event.Event;
import com.example.event.EventClient;
import com.example.event.EventType;
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
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

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


            Stage stage = new Stage();
            stage.setStatus(Status.PENDING);
            stage.setTasks(sentences.stream().map(a -> new Task(false, Status.PENDING)).collect(Collectors.toList()));

            Job job = jobClient.createJob(new Job(Collections.singletonList(stage)));

            List<Task> tasks = job.getStages().get(0).getTasks();

            for (Task task : tasks) {
                TaskRepository.taskActionCache.put(task.getId(), new Run<String, Integer>(task.getId(), a ->
                        a.map(sentence -> Arrays.asList(sentence.getT1().split("\\s")).stream())
                                .flatMap(word -> word)
                                .map(word -> new Tuple<>(word, 1))));

                redisTemplate.execute(
                        (RedisCallback) redisConnection -> {
                            try {
                                ((StringRedisConnection) redisConnection).set(task.getId(), objectMapper.writeValueAsString(
                                        Collections.singletonList(sentences.remove())
                                                .stream()
                                                .map(sentence ->
                                                        new Tuple<>(sentence, 0))
                                                .collect(Collectors.toSet())));
                            } catch (JsonProcessingException e) {
                                log.error(e);
                            }

                            return task.getId();
                        });


                log.info(job);

                tasks.forEach(task1 -> eventClient.sendEvent(new Event("0", EventType.RUN, task1.getId())));
                tasks.forEach(task1 -> eventClient.sendEvent(new Event("0", EventType.END, task1.getId())));
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
    Receiver receiver(ObjectMapper objectMapper) {
        return new Receiver(objectMapper);
    }

    @Bean
    MessageListenerAdapter listenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }


}
