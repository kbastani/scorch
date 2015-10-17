package org.springframework.jobmanager.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jobmanager.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class JobServiceImpl implements JobService {

    private JobRepository jobRepository;

    @Autowired
    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public List<Task> getTasks(Long jobId) {
        return jobRepository.findOne(jobId).getStages()
                .stream()
                .flatMap(s -> s.getTasks().stream())
                .collect(Collectors.toList());
    }

    /**
     * Create a new {@link Job}
     *
     * @param job is the new {@link Job}
     * @return the newly create {@link Job}
     */
    @Override
    public Job createJob(Job job) {
        try {
            jobRepository.save(job);
        } catch (Exception ex) {

        }
        return null;
    }
}
