package org.springframework;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jobmanager.Application;
import org.springframework.jobmanager.event.EventType;
import org.springframework.jobmanager.job.Job;
import org.springframework.jobmanager.job.JobRepository;
import org.springframework.jobmanager.machine.Status;
import org.springframework.jobmanager.stage.Stage;
import org.springframework.jobmanager.task.Task;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class SpringJobManagerApplicationTests {

    private Logger log = LoggerFactory.getLogger(SpringJobManagerApplicationTests.class);

    @Autowired
    JobRepository jobRepository;

	@Test
	public void contextLoads() {
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



}
