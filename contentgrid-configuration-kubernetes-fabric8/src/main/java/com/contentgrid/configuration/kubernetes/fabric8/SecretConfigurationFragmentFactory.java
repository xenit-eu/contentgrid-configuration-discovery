package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import io.fabric8.kubernetes.api.model.Secret;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecretConfigurationFragmentFactory<K, C> implements ConfigurationFragmentFactory<Secret, String, K, C> {
    @NonNull
    private final Function<Secret, K> compositionKeyFunction;
    @NonNull
    private final Function<Map<String, String>, C> configurationFunction;

    @Override
    public ConfigurationFragment<String, K, C> createFragment(Secret fragment) {
        return new ConfigurationFragment<>(
                fragment.getMetadata().getUid(),
                compositionKeyFunction.apply(fragment),
                configurationFunction.apply(base64decode(fragment.getData()))
        );
    }

    private Map<String, String> base64decode(Map<String, String> data) {
        var decoder = Base64.getDecoder();
        var copy = new HashMap<String, String>(data.size());

        data.forEach((key, value) -> copy.put(key, new String(decoder.decode(value))));

        return copy;
    }
}

