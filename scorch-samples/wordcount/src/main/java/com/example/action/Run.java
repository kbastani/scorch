package com.example.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class Run<T1, T2> implements ApplicationContextAware, Action {

    Log log = LogFactory.getLog(Run.class);

    private String taskId;
    private Function<Stream<Tuple<T1, T2>>, Stream<Tuple<?, ?>>> action;
    private ApplicationContext applicationContext;
    private RedisTemplate<String, Object> redisTemplate;
    private ObjectMapper objectMapper;

    public Run(String taskId, Function<Stream<Tuple<T1, T2>>, Stream<Tuple<?, ?>>> action) {
        this.taskId = taskId;
        this.action = action;
        this.redisTemplate = (RedisTemplate<String, Object>) applicationContext.getBean("redisTemplate");
        this.objectMapper = (ObjectMapper) applicationContext.getBean("objectMapper");
    }

    @Override
    public String getTargetId() {
        return taskId;
    }

    @Override
    public Function getAction() {
        return action;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<Tuple<?, ?>> execute() {
        Set<Tuple<T1, T2>> data = redisTemplate.execute((RedisConnection connection) -> {
            Set<Tuple<T1, T2>> tuple = null;
            try {
                tuple = objectMapper.readValue(((StringRedisConnection) connection).get(taskId), HashSet.class);
            } catch (IOException e) {
                log.error(e);
            }
            return tuple;
        });
        return action.apply(data.stream());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
