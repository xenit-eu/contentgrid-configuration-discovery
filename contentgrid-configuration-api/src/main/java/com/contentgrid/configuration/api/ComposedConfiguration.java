package com.contentgrid.configuration.api;

import java.util.function.Function;

public interface ComposedConfiguration<K, C> extends HasConfiguration<C> {
    K getCompositionKey();

    default <T> ComposedConfiguration<K, T> map(Function<C, T> mapper) {
        return new MappedConfiguration<>(this, mapper);
    }
}
