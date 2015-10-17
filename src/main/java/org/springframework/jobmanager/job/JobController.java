package org.springframework.jobmanager.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jobmanager.autoconfigure.RootHypermediaLink;
import org.springframework.jobmanager.hypermedia.HypermediaController;
import org.springframework.jobmanager.hypermedia.HypermediaResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
        throw new NotImplementedException();
    }

    @RequestMapping(path = "/jobs/{jobId}", method = RequestMethod.GET)
    public HttpEntity<?> getJob(@RequestParam("jobId") Long jobId) {
        return Optional.of(new NotImplementedException()).map(u -> new ResponseEntity<>(u, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @RequestMapping(path = "/jobs/{jobId}/tasks", method = RequestMethod.GET)
    public HttpEntity<?> getTasks(@RequestParam("jobId") Long jobId) {
        return Optional.of(jobService.getTasks(jobId))
                .map(u -> new ResponseEntity<>(new HypermediaResource<>((ArrayList) u), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }
}
