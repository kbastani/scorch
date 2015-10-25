package org.springframework.scorch.job;

import lombok.*;
import org.springframework.scorch.audit.AbstractEntity;
import org.springframework.scorch.stage.Stage;
import org.springframework.scorch.zookeeper.Distributed;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Each {@link Job} contains a set of {@link Stage}.
 *
 * @author Kenny Bastani
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class Job extends AbstractEntity implements Distributed {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "id")
    @NonNull
    private List<Stage> stages = new ArrayList<>();
}
