package org.springframework.scorch.stage;

import lombok.*;
import org.springframework.scorch.audit.AbstractEntity;
import org.springframework.scorch.machine.Status;
import org.springframework.scorch.task.Task;
import org.springframework.scorch.zookeeper.Distributed;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Each Stage belongs to a job and contains a set of tasks
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Stage extends AbstractEntity implements Distributed {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Task> tasks = new ArrayList<>();

    @NonNull
    @Enumerated(EnumType.STRING)
    private Status status;

    public void addTask(Task task) {
        tasks.add(task);
    }

    public Optional<Task> getTask(Long id) {
        Optional<Task> task = this.getTasks()
                .stream()
                .filter(a -> Objects.equals(a.getId(), id))
                .findFirst();

        return task;
    }
}
