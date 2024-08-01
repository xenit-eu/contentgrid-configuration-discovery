package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigMapConfigurationFragmentFactory<AGG> implements ConfigurationFragmentFactory<ConfigMap, String, AGG, Map<String, String>> {
    private final Function<ConfigMap, AGG> aggregationFunction;

    @Override
    public ConfigurationFragment<String, AGG, Map<String, String>> createFragment(ConfigMap fragment) {
        return new ConfigurationFragment<>(
                fragment.getMetadata().getUid(),
                aggregationFunction.apply(fragment),
                fragment.getData()
        );
    }
}
