package org.springframework.jobmanager.task;

import lombok.*;
import org.springframework.jobmanager.audit.AbstractEntity;
import org.springframework.jobmanager.machine.Status;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Task extends AbstractEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NonNull
    private boolean active;

    @NonNull
    @Enumerated(EnumType.STRING)
    private Status state;
}
