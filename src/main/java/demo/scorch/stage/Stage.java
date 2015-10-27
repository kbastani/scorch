package demo.scorch.stage;

import demo.scorch.zookeeper.Distributed;
import lombok.*;
import demo.scorch.audit.AbstractEntity;
import demo.scorch.machine.Status;
import demo.scorch.task.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Each Stage belongs to a job and contains a set of tasks
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Stage extends AbstractEntity implements Distributed {
    private List<Task> tasks = new ArrayList<>();

    @NonNull
    private Status status;

    public void addTask(Task task) {
        tasks.add(task);
    }

    public Optional<Task> getTask(String id) {
        Optional<Task> task = this.getTasks()
                .stream()
                .filter(a -> Objects.equals(a.getId(), id))
                .findFirst();

        return task;
    }
}
