package org.springframework.scorch.task;

import lombok.*;
import org.springframework.scorch.audit.AbstractEntity;
import org.springframework.scorch.machine.Status;
import org.springframework.scorch.zookeeper.Distributed;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Task extends AbstractEntity implements Distributed {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NonNull
    private boolean active;

    @NonNull
    @Enumerated(EnumType.STRING)
    private Status state;
}
