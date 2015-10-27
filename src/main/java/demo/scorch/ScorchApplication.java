package demo.scorch;

import demo.scorch.task.TaskReplicator;
import demo.scorch.task.TaskStateMachine;
import demo.scorch.zookeeper.ZookeeperClient;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The main {@link SpringCloudApplication} class for the distributed
 * state manager.
 *
 * @author Kenny Bastani
 */
@SpringCloudApplication
public class ScorchApplication {

    final static String queueName = "scorch.actions";

    public static void main(String[] args) {
        new SpringApplicationBuilder(ScorchApplication.class).web(true).run(args);
    }

    @Bean(name = "taskStateMachine", initMethod = "init")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public TaskStateMachine taskStateMachine() {
        return new TaskStateMachine();
    }

    @Bean(initMethod = "init")
    public ZookeeperClient zookeeperClient() {
        return new ZookeeperClient();
    }

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(40);
        return taskExecutor;
    }

    @Bean(initMethod = "init")
    TaskReplicator taskReplicator(TaskExecutor taskExecutor, ZookeeperClient zookeeperClient) {
        return new TaskReplicator(taskExecutor, zookeeperClient);
    }

    @Bean
    Queue queue() {
        return new Queue(queueName, false);
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange("scorch.exchange");
    }

    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queueName);
    }
}

