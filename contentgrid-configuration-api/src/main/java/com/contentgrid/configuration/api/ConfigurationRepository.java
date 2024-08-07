package com.contentgrid.configuration.api;

import java.util.stream.Stream;

public interface ConfigurationRepository<K, C> {
    HasConfiguration<C> findConfiguration(K compositionKey);

    Stream<K> compositionKeys();
}
