package demo.scorch;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.scorch.event.Event;
import demo.scorch.event.EventType;
import demo.scorch.job.Job;
import demo.scorch.machine.Status;
import demo.scorch.stage.Stage;
import demo.scorch.task.Task;
import demo.scorch.zookeeper.ZookeeperClient;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = ScorchApplication.class)
@WebAppConfiguration
public class ScorchApplicationTests {

    private Logger log = LoggerFactory.getLogger(ScorchApplicationTests.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ZookeeperClient zookeeperClient;

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
    public void createJob() throws Exception {
        Job job = createAndGetJob();
        log.info(String.format("Job created: %s", job.toString()));
        Assert.notNull(job);

        // Clean up
        zookeeperClient.delete(Job.class, job.getId());
    }

    private Job createAndGetJob() throws Exception {
        Job job = new Job();

        log.info("Creating job...");

        String createdJobs = mockMvc.perform(post("/v1/job/jobs")
                .content(objectMapper.writeValueAsString(job))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        job = objectMapper.readValue(createdJobs, Job.class);

        return job;
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
        String eventResult = sendTaskEvent(task, EventType.RUN);
        Assert.isTrue(Boolean.parseBoolean(eventResult));
        eventResult = sendTaskEvent(task, EventType.END);
        Assert.isTrue(Boolean.parseBoolean(eventResult));
        eventResult = sendTaskEvent(task, EventType.CONTINUE);
        Assert.isTrue(Boolean.parseBoolean(eventResult));
    }

    private void cleanUp(Job job, Stage stage, Task task) throws KeeperException, InterruptedException {
        // Clean up
        zookeeperClient.delete(Job.class, job.getId());
        zookeeperClient.delete(Stage.class, stage.getId());
        zookeeperClient.delete(Task.class, task.getId());
        zookeeperClient.getZooKeeper()
            .getChildren("/scorch/event", false)
            .forEach(event -> zookeeperClient.delete(Event.class, event));
    }

    private String sendTaskEvent(Task task, EventType runEvent) throws Exception {
        Event event = new Event();
        event.setEventType(runEvent);
        event.setTargetId(task.getId());
        event.setId("0");

        log.info("Sending event: " + objectMapper.writeValueAsString(event));

        return mockMvc.perform(post("/v1/event/events", task.getId())
                .content(objectMapper.writeValueAsString(event))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @After
    public void after() {
        try {
            zookeeperClient.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
