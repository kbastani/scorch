package demo.scorch.job;

import demo.scorch.audit.AbstractEntity;
import demo.scorch.stage.Stage;
import demo.scorch.zookeeper.Distributed;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Each {@link Job} contains a set of {@link Stage}.
 *
 * @author Kenny Bastani
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class Job extends AbstractEntity implements Distributed {
    @NonNull
    private List<Stage> stages = new ArrayList<>();
}
