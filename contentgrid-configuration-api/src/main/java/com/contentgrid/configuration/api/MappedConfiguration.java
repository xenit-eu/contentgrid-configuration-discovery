package com.contentgrid.configuration.api;

import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class MappedConfiguration<AGG, C, T> implements AggregateIdConfiguration<AGG, T> {
    private final AggregateIdConfiguration<AGG, C> configuration;
    private final Function<C, T> mapper;

    @Override
    public AGG getAggregateId() {
        return configuration.getAggregateId();
    }

    @Override
    public Optional<T> getConfiguration() {
        return configuration.getConfiguration().map(mapper);
    }

    @Override
    public <T1> AggregateIdConfiguration<AGG, T1> map(Function<T, T1> mapper) {
        return new MappedConfiguration<>(configuration, this.mapper.andThen(mapper));
    }
}
