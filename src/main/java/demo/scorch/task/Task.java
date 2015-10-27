package demo.scorch.task;

import demo.scorch.audit.AbstractEntity;
import demo.scorch.machine.Status;
import demo.scorch.zookeeper.Distributed;
import lombok.*;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Task extends AbstractEntity implements Distributed {
    @NonNull
    private boolean active;

    @NonNull
    private Status status = Status.PENDING;
}
