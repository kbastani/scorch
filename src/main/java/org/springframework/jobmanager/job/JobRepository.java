package org.springframework.jobmanager.job;


import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jobs", collectionResourceRel = "jobsRepository")
public interface JobRepository extends PagingAndSortingRepository<Job, Long> {
}
