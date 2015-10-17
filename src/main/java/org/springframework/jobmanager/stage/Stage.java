package org.springframework.jobmanager.stage;

import lombok.*;
import org.springframework.jobmanager.audit.AbstractEntity;
import org.springframework.jobmanager.machine.Status;
import org.springframework.jobmanager.task.Task;

import javax.persistence.*;
import java.io.Serializable;
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
public class Stage extends AbstractEntity implements Serializable {

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
