package com.example.message;

import com.example.action.Action;
import com.example.event.Event;
import com.example.event.EventClient;
import com.example.event.EventType;
import com.example.task.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;


@Component
public class Receiver {

    Log log = LogFactory.getLog(Receiver.class);
    private ObjectMapper objectMapper;
    private EventClient eventClient;

    @Autowired
    public Receiver(ObjectMapper objectMapper, EventClient eventClient) {
        this.objectMapper = objectMapper;
        this.eventClient = eventClient;
    }

    @RabbitListener(queues = { "scorch.actions" } )
    public void receiveMessage(String message) {
        try {
            StateChange stateChange = objectMapper.readValue(message, StateChange.class);
            log.info("Received <" + stateChange.toString() + ">");

            // Submit action
            action(stateChange);

        } catch (IOException e) {
            log.error(e);
        }
    }

    private void action(StateChange stateChange) {
        switch (stateChange.getTargetState()) {
            case RUNNING:
                run(stateChange.getTargetId());
                break;
            case FINISHED:
                finish(stateChange.getTargetId());
                break;
            default:
                break;
        }
    }

    private void finish(String targetId) {
    }

    private void run(String taskId) {
        Action<String, Integer> action = (Action<String, Integer>)TaskRepository.taskActionCache.getIfPresent(taskId);

        if(action != null) {
            log.info(action.execute().collect(Collectors.toList()));
            // Send continue event
            eventClient.sendEvent(new Event("0", EventType.CONTINUE, taskId));
        }
    }
}