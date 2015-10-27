package com.example.job;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("scorch")
public interface JobClient {
    @RequestMapping(method = RequestMethod.POST, value = "/v1/job/jobs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Job createJob(Job job);

    @RequestMapping(method = RequestMethod.GET, value = "/v1/job/jobs/{jobId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Job getJob(@RequestParam("jobId") String jobId);
}
