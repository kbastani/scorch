package demo.scorch.event;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import demo.scorch.job.Job;
import demo.scorch.zookeeper.Distributed;
import lombok.*;

/**
 * An {@link Event} is ingested to the {@link EventService} and applied to
 * a {@link org.springframework.statemachine.StateMachine} which is transacted
 * to affect the state of a {@link Job}.
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
