package org.springframework.scorch.task;

import lombok.*;
import org.springframework.scorch.audit.AbstractEntity;
import org.springframework.scorch.machine.Status;
import org.springframework.scorch.zookeeper.Distributed;

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
