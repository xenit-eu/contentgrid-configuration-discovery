package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigMapConfigurationFragmentFactory<AGG, C> implements ConfigurationFragmentFactory<ConfigMap, String, AGG, C> {
    @NonNull
    private final Function<ConfigMap, AGG> aggregationFunction;
    @NonNull
    private final Function<Map<String, String>, C> configurationFunction;

    @Override
    public ConfigurationFragment<String, AGG, C> createFragment(ConfigMap fragment) {
        return new ConfigurationFragment<>(
                fragment.getMetadata().getUid(),
                aggregationFunction.apply(fragment),
                configurationFunction.apply(fragment.getData())
        );
    }
}
