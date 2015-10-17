package org.springframework.jobmanager.task;


import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "tasks", collectionResourceRel = "taskRepository")
public interface TaskRepository extends PagingAndSortingRepository<Task, Long> {
}
