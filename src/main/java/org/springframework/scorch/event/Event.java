package org.springframework.scorch.event;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.*;
import org.springframework.scorch.zookeeper.Distributed;

/**
 * An {@link Event} is ingested to the {@link EventService} and applied to
 * a {@link org.springframework.statemachine.StateMachine} which is transacted
 * to affect the state of a {@link org.springframework.scorch.job.Job}.
 *
 * @author Kenny Bastani
 */
@Data
@ToString
@NoArgsConstructor
@JsonAutoDetect
public class Event implements Distributed {

    private String id;

    @NonNull
    private EventType eventType;

    @NonNull
    private String targetId;
}
