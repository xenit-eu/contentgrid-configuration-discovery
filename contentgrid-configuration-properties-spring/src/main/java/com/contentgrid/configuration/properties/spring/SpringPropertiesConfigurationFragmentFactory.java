package com.contentgrid.configuration.properties.spring;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpringPropertiesConfigurationFragmentFactory<K, C> implements ConfigurationFragmentFactory<Map.Entry<String, ConfigurationDiscoveryProperties>, String, K, C> {
    @NonNull
    private final Function<String, K> compositionKeyFunction;

    @NonNull
    private final Function<Map<String, String>, C> configurationFunction;

    @Override
    public ConfigurationFragment<String, K, C> createFragment(Map.Entry<String, ConfigurationDiscoveryProperties> fragment) {
        return new ConfigurationFragment<>(
                fragment.getKey(),
                compositionKeyFunction.apply(fragment.getValue().getCompositionKey()),
                configurationFunction.apply(fragment.getValue().getConfiguration())
        );
    }
}
