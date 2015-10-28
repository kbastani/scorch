package com.example.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class Run<T1, T2> implements Action<T1, T2> {

    Log log = LogFactory.getLog(Run.class);

    private String taskId;
    private Function<Stream<Tuple<? extends T1, ? extends T2>>, Stream<Tuple<? extends T1, ? extends T2>>> action;
    private RedisTemplate<String, Object> redisTemplate;
    private ObjectMapper objectMapper;

    public Run(String taskId, Function<Stream<Tuple<? extends T1, ? extends T2>>, Stream<Tuple<? extends T1, ? extends T2>>> action, RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.taskId = taskId;
        this.action = action;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getTargetId() {
        return taskId;
    }

    @Override
    public Function getAction() {
        return action;
    }

    @SuppressWarnings("unchecked")
    public Stream<Tuple<? extends T1, ? extends T2>> execute() {
        List<LinkedHashMap> data = redisTemplate.execute((RedisConnection connection) -> {
            List<LinkedHashMap> tuple = null;
            try {
                tuple = objectMapper.readValue(connection.get(taskId.getBytes()), List.class);
            } catch (IOException e) {
                log.error(e);
            }
            return tuple;
        });

        return action.apply(data.stream().map(a -> new Tuple<>((T1)a.get("t1"), (T2)a.get("t2"))));
    }
}
