package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import io.fabric8.kubernetes.api.model.Secret;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecretConfigurationFragmentFactory<AGG> implements ConfigurationFragmentFactory<Secret, String, AGG, Map<String, String>> {
    private final Function<Secret, AGG> aggregationFunction;


    @Override
    public ConfigurationFragment<String, AGG, Map<String, String>> createFragment(Secret fragment) {
        return new ConfigurationFragment<>(
                fragment.getMetadata().getUid(),
                aggregationFunction.apply(fragment),
                base64decode(fragment.getData())
        );
    }

    private Map<String, String> base64decode(Map<String, String> data) {
        var decoder = Base64.getDecoder();
        var copy = new HashMap<String, String>(data.size());

        data.forEach((key, value) -> copy.put(key, new String(decoder.decode(value))));

        return copy;
    }
}

