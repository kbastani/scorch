package com.example.task;

import com.example.action.Action;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class TaskRepository {
    public static final Cache<String, Action> taskActionCache =
            CacheBuilder.newBuilder().maximumSize(20000000).build();
}
