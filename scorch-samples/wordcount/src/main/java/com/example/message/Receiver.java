package com.example.message;

import com.example.action.Action;
import com.example.task.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class Receiver {

    Log log = LogFactory.getLog(Receiver.class);
    private ObjectMapper objectMapper;

    @Autowired
    public Receiver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = { "scorch.actions" } )
    public void receiveMessage(String stateChange) {
        try {
            // This is the hook where state change events are received
            // When a run notification is received, the driver will run an operation
            // When the driver is finished running the operation, it will signal an end
            // When all tasks in a stage are complete, the driver will then run the next stage
            log.info("Received <" + objectMapper.readValue(stateChange, StateChange.class) + ">");


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
        Action action = TaskRepository.taskActionCache.getIfPresent(taskId);

        if(action != null) {
            log.info(action.execute());
        }
    }
}