package org.springframework.jobmanager.job;

import lombok.*;
import org.springframework.jobmanager.audit.AbstractEntity;
import org.springframework.jobmanager.stage.Stage;

import javax.persistence.*;
import java.io.Serializable;
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
public class Job extends AbstractEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "id")
    @NonNull
    private List<Stage> stages = new ArrayList<>();
}
