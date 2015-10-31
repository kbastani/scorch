package demo.scorch.task;

import demo.scorch.audit.AbstractEntity;
import demo.scorch.event.DomainType;
import demo.scorch.machine.Status;
import demo.scorch.zookeeper.Distributed;
import lombok.*;

/**
 * A {@link Task} represents a shared distributed state machine that is
 * replicated across a Scorch cluster. A globally ordered transaction log
 * of {@link demo.scorch.event.Event} are processed over in-memory state machines
 * which drives the state of a {@link Task}.
 *
 * @author Kenny Bastani
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Task extends AbstractEntity implements Distributed {
    @NonNull
    private DomainType type;
    private String stageId;
    private String jobId;
    @NonNull
    private Status status = Status.PENDING;
}
