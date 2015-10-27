package com.example.action;

import java.util.function.Function;
import java.util.stream.Stream;

public interface Action<T1, T2> {
    String getTargetId();
    Function<Tuple<T1, T2>, Tuple<?, ?>> getAction();
    Stream<Tuple<?, ?>> execute();
}
