package com.contentgrid.configuration.api;

import java.util.function.Function;

public interface AggregateIdConfiguration<AGG, C> extends HasConfiguration<C> {
    AGG getAggregateId();

    default <T> AggregateIdConfiguration<AGG, T> map(Function<C, T> mapper) {
        return new MappedConfiguration<>(this, mapper);
    }
}
