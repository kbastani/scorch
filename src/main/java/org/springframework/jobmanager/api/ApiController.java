package org.springframework.jobmanager.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jobmanager.tasks.Task;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/v1")
public class ApiController {

    private JobManagerService jobManagerService;

    @Autowired
    public ApiController(JobManagerService jobManagerService) {
        this.jobManagerService = jobManagerService;
    }

    @RequestMapping(path = "/tasks", method = RequestMethod.POST)
    public ResponseEntity<?> addTask(@RequestBody Task task) {
        return Optional.of(jobManagerService.createTask(task))
                .map(u -> new ResponseEntity<>(HttpStatus.CREATED))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @RequestMapping(path = "/tasks", method = RequestMethod.GET)
    public ResponseEntity<?> getTasks() {
        return Optional.of(jobManagerService.getTasks()).map(u -> new ResponseEntity<>(u, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @RequestMapping(path = "/states", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getStateMachine() {
        return Optional.of(jobManagerService.getStateMachine().getStates().stream().map(State::getId).collect(Collectors.toList()))
                .map(u -> new ResponseEntity<>(u, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }
}
