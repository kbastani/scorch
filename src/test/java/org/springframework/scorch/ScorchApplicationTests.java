package org.springframework.scorch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.scorch.event.Event;
import org.springframework.scorch.event.EventType;
import org.springframework.scorch.job.Job;
import org.springframework.scorch.machine.Status;
import org.springframework.scorch.stage.Stage;
import org.springframework.scorch.task.Task;
import org.springframework.scorch.zookeeper.ZookeeperClient;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ScorchApplication.class)
@WebAppConfiguration
public class ScorchApplicationTests {

    private Logger log = LoggerFactory.getLogger(ScorchApplicationTests.class);

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ZookeeperClient zookeeperClient;

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

	@Test
	public void contextLoads() {
	}

    @Test
    public void createTask() throws Exception {

        Job job = new Job();
        // Create a set of tasks to track a job
        Stage stage = new Stage(Status.READY);

        job.getStages().add(stage);

        log.info("Creating job with single stage...");

        String createdJobs = mockMvc.perform(post("/v1/job/jobs")
                .content(objectMapper.writeValueAsString(job))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        job = objectMapper.readValue(createdJobs, Job.class);

        log.info(String.format("Job created: %s", job.toString()));

        String stageId = job.getStages().stream()
                .findFirst()
                .get()
                .getId();

        log.info(String.format("Adding task to stage with id '%s'...", stageId));

        String taskResult = mockMvc.perform(post(String.format("/v1/job/stages/%s/tasks", stageId))
                .content(objectMapper.writeValueAsString(new Task(false)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        Task task = objectMapper.readValue(taskResult, Task.class);

        Event event = new Event();
        event.setEventType(EventType.RUN);
        event.setTargetId(task.getId());
        event.setId("");
        log.info(objectMapper.writeValueAsString(event));
    }

    @After
    public void after() {
        try {
            zookeeperClient.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected String json(Object o) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(o);
    }

}
