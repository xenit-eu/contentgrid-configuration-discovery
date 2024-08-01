package com.contentgrid.configuration.api;

import java.util.Optional;
import java.util.function.Function;

public interface HasConfiguration<C> {
    Optional<C> getConfiguration();
    <T> HasConfiguration<T> map(Function<C, T> mapper);
}
