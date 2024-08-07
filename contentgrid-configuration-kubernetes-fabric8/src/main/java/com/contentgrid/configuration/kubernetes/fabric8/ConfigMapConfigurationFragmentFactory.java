package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigMapConfigurationFragmentFactory<K, C> implements ConfigurationFragmentFactory<ConfigMap, String, K, C> {
    @NonNull
    private final Function<ConfigMap, K> compositionKeyFunction;
    @NonNull
    private final Function<Map<String, String>, C> configurationFunction;

    @Override
    public ConfigurationFragment<String, K, C> createFragment(ConfigMap fragment) {
        return new ConfigurationFragment<>(
                fragment.getMetadata().getUid(),
                compositionKeyFunction.apply(fragment),
                configurationFunction.apply(fragment.getData())
        );
    }
}
