package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigMapConfigurationFragmentFactory implements ConfigurationFragmentFactory<ConfigMap, String, String, Map<String, String>> {
    private final String aggregationLabel;

    @Override
    public ConfigurationFragment<String, String, Map<String, String>> createFragment(ConfigMap fragment) {
        return new ConfigurationFragment<>(
                fragment.getMetadata().getUid(),
                fragment.getMetadata().getLabels().get(aggregationLabel),
                fragment.getData()
        );
    }
}
