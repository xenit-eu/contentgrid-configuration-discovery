package com.contentgrid.configuration.properties.spring;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpringPropertiesConfigurationFragmentFactory<AGG, C> implements ConfigurationFragmentFactory<Map.Entry<String, ConfigurationDiscoveryProperties>, String, AGG, C> {
    @NonNull
    private final Function<String, AGG> aggregationFunction;

    @NonNull
    private final Function<Map<String, String>, C> configurationFunction;

    @Override
    public ConfigurationFragment<String, AGG, C> createFragment(Map.Entry<String, ConfigurationDiscoveryProperties> fragment) {
        return new ConfigurationFragment<>(
                fragment.getKey(),
                aggregationFunction.apply(fragment.getValue().getAggregateId()),
                configurationFunction.apply(fragment.getValue().getConfiguration())
        );
    }
}
