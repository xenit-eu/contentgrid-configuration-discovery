package com.contentgrid.configuration.api;

import java.util.stream.Stream;

public interface ConfigurationRepository<AGG, C> {
    HasConfiguration<C> findConfiguration(AGG aggregationId);

    Stream<AGG> aggregationIds();
}
