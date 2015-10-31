package com.example.message;

import com.example.action.Action;
import com.example.action.Tuple;
import com.example.event.Event;
import com.example.event.EventClient;
import com.example.event.EventType;
import com.example.task.TaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class Receiver {

    Log log = LogFactory.getLog(Receiver.class);
    private ObjectMapper objectMapper;
    private EventClient eventClient;
    private RedisTemplate redisTemplate;

    @Autowired
    public Receiver(ObjectMapper objectMapper, EventClient eventClient, RedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.eventClient = eventClient;
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = {"scorch.actions"})
    public void receiveMessage(String message) {
        try {
            StateChange stateChange = objectMapper.readValue(message, StateChange.class);
            // log.info("Received <" + stateChange.toString() + ">");

            // Submit action
            action(stateChange);

        } catch (IOException e) {
            log.error(e);
        }
    }

    private void action(StateChange stateChange) {
        switch (stateChange.getTargetState()) {
            case READY:
                start(stateChange.getTargetId());
                break;
            case STARTED:
                run(stateChange.getTargetId());
                break;
            case RUNNING:
                finish(stateChange.getTargetId());
                break;
            case FINISHED:
                success(stateChange.getTargetId());
                break;
            default:
                break;
        }
    }

    private void start(String taskId) {
        eventClient.sendEvent(new Event("0", EventType.RUN, taskId));
    }

    private void run(String taskId) {
        eventClient.sendEvent(new Event("0", EventType.END, taskId));
    }

    private void finish(String taskId) {
        Action<String, Integer> action = (Action<String, Integer>) TaskRepository.taskActionCache.getIfPresent(taskId);

        if (action != null) {
            List<Tuple<? extends String, ? extends Integer>> collect = action.execute().collect(Collectors.toList());

            log.info(collect);

            // Save to redis
            redisTemplate.execute(
                    (RedisCallback) redisConnection -> {
                        try {
                            redisConnection.set(taskId.getBytes(), objectMapper.writeValueAsString(collect).getBytes());
                        } catch (JsonProcessingException e) {
                            log.error(e);
                        }

                        return taskId;
                    });
        }

        // Send continue event
        eventClient.sendEvent(new Event("0", EventType.CONTINUE, taskId));
    }

    private void success(String taskId) {
        eventClient.sendEvent(new Event("0", EventType.STOP, taskId));
    }
}