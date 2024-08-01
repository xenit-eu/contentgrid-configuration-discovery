package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.AggregateIdConfiguration;
import java.util.Optional;
import java.util.function.Function;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@ToString
public class ConfigurationFragment<ID, AGG, C> implements AggregateIdConfiguration<AGG, C> {
    @NonNull
    ID fragmentId;
    @NonNull
    AGG aggregateId;

    C configuration;

    public Optional<C> getConfiguration() {
        return Optional.of(configuration);
    }

    @Override
    public <T> ConfigurationFragment<ID, AGG, T> map(Function<C, T> mapper) {
        if(configuration == null) {
            // The mismatch of configuration does not matter, because it is null anyways
            return (ConfigurationFragment<ID, AGG, T>) this;
        }
        return new ConfigurationFragment<>(fragmentId, aggregateId, mapper.apply(configuration));
    }
}
