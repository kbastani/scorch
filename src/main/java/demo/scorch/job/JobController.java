package demo.scorch.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import demo.scorch.autoconfigure.RootHypermediaLink;
import demo.scorch.hypermedia.HypermediaController;
import demo.scorch.task.Task;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.Optional;

/**
 * The {@link JobController} exposes a set of methods that manage {@link Job}
 * lifecycle.
 *
 * @author Kenny Bastani
 */
@Controller
@RootHypermediaLink("jobService")
@ExposesResourceFor(Job.class)
@RequestMapping(value = "/v1/job")
public class JobController extends HypermediaController {

    private JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        super("/v1/job/jobs");
        this.jobService = jobService;
    }

    @RequestMapping(path = "/jobs", method = RequestMethod.POST)
    public HttpEntity<?> createJob(@RequestBody Job job) {
        return Optional.of(jobService.createJob(job)).map(u -> new ResponseEntity<>(u, HttpStatus.CREATED))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @RequestMapping(path = "/jobs/{jobId}", method = RequestMethod.GET)
    public HttpEntity<?> getJob(@PathVariable("jobId") String jobId) {
        return Optional.of(jobService.getJob(jobId)).map(u -> new ResponseEntity<>(u, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @RequestMapping(path = "/jobs/{jobId}/tasks", method = RequestMethod.GET)
    public HttpEntity<?> getTasks(@PathVariable("jobId") String jobId) {
        return Optional.of(jobService.getTasks(jobId))
                .map(u -> new ResponseEntity<>((ArrayList) u, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @RequestMapping(path = "/stages/{stageId}/tasks", method = RequestMethod.POST)
    public HttpEntity<?> createTask(@PathVariable("stageId") String stageId, @RequestBody Task task) {
        return Optional.of(jobService.createTask(stageId, task))
                .map(u -> new ResponseEntity<>(task, HttpStatus.CREATED))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }
}
