package com.example.action;

import java.util.function.Function;
import java.util.stream.Stream;

public interface Action<T1 , T2> {
    String getTargetId();
    Function<Tuple<? extends T1, ? extends T2>, Tuple<? extends T1, ? extends T2>> getAction();

    @SuppressWarnings("unchecked")
    Stream<Tuple<? extends T1, ? extends T2>> execute();
}
