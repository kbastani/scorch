package org.springframework.scorch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scorch.task.TaskReplicator;
import org.springframework.scorch.task.TaskStateMachine;
import org.springframework.scorch.zookeeper.ZookeeperClient;

@SpringCloudApplication
public class ScorchApplication {

    private final static Log log = LogFactory.getLog(ScorchApplication.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder(ScorchApplication.class).web(true).run(args);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(40);
        return taskExecutor;
    }

    @Bean(initMethod = "init")
    TaskReplicator taskReplicator() {
        return new TaskReplicator();
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

}

