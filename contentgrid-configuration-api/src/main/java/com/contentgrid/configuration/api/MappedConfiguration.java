package com.contentgrid.configuration.api;

import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class MappedConfiguration<F, C, T> implements ComposedConfiguration<F, T> {
    private final ComposedConfiguration<F, C> configuration;
    private final Function<C, T> mapper;

    @Override
    public F getCompositionKey() {
        return configuration.getCompositionKey();
    }

    @Override
    public Optional<T> getConfiguration() {
        return configuration.getConfiguration().map(mapper);
    }

    @Override
    public <T1> ComposedConfiguration<F, T1> map(Function<T, T1> mapper) {
        return new MappedConfiguration<>(configuration, this.mapper.andThen(mapper));
    }
}
