package org.springframework.scorch.stage;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "stages", collectionResourceRel = "stageRepository")
public interface StageRepository extends PagingAndSortingRepository<Stage, Long> {
}
