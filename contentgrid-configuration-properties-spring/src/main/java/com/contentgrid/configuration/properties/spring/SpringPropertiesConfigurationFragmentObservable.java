package com.contentgrid.configuration.properties.spring;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import com.contentgrid.configuration.api.observable.Observable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class SpringPropertiesConfigurationFragmentObservable<K, C> implements Observable<ConfigurationFragment<String, K, C>> {
    private final Map<String, ConfigurationDiscoveryProperties> properties;
    private final ConfigurationFragmentFactory<Map.Entry<String, ConfigurationDiscoveryProperties>, String, K, C> fragmentFactory;

    @Override
    public Flux<UpdateEvent<ConfigurationFragment<String, K, C>>> observe() {
        return Flux.fromIterable(properties.entrySet())
                .map(fragmentFactory::createFragment)
                .map(fragment -> new UpdateEvent<>(UpdateType.ADD, fragment));
    }
}
