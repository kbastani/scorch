package org.springframework;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jobmanager.Application;
import org.springframework.jobmanager.event.EventType;
import org.springframework.jobmanager.job.Job;
import org.springframework.jobmanager.job.JobRepository;
import org.springframework.jobmanager.machine.Status;
import org.springframework.jobmanager.stage.Stage;
import org.springframework.jobmanager.task.Task;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class SpringJobManagerApplicationTests {

    private Logger log = LoggerFactory.getLogger(SpringJobManagerApplicationTests.class);

    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    JobRepository jobRepository;

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

        job = new Gson().fromJson(mockMvc.perform(post("/v1/job/jobs")
                .content(this.json(job))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), Job.class);

        log.info(String.format("Job created: %s", job.toString()));

        Long stageId = job.getStages().stream()
                .findFirst()
                .get()
                .getId();

        log.info(String.format("Adding task to stage with id '%s'...", stageId));

        String taskResult = mockMvc.perform(post(String.format("/v1/job/stages/%s/tasks", stageId))
                .content(this.json(new Task(false, Status.READY)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        Task task = new Gson().fromJson(taskResult, Task.class);

        log.info(task.toString());
    }

	@Test
	public void testTaskWorkflow() {

        Job job = new Job();
        // Create a set of tasks to track a job
        Stage stage = new Stage(Status.READY);
        stage.addTask(new Task(true, Status.READY));
        job.getStages().add(stage);

        job = jobRepository.save(job);

        log.info(job.toString());

        log.info( EventType.BEGIN.toString());
	}

    protected String json(Object o) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(o);
    }

}
