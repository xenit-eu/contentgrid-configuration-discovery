package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.HasConfiguration;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;

@Value
public class ConfigurationFragment<ID, AGG, C> implements HasConfiguration<C> {
    @NonNull
    ID fragmentId;
    @NonNull
    AGG aggregateId;

    C configuration;

    public Optional<C> getConfiguration() {
        return Optional.of(configuration);
    }
}
