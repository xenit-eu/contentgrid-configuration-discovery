package com.contentgrid.configuration.properties.spring;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import com.contentgrid.configuration.api.observable.Observable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class SpringPropertiesConfigurationFragmentObservable<AGG, C> implements Observable<ConfigurationFragment<String, AGG, C>> {
    private final Map<String, ConfigurationDiscoveryProperties> properties;
    private final ConfigurationFragmentFactory<Map.Entry<String, ConfigurationDiscoveryProperties>, String, AGG, C> fragmentFactory;

    @Override
    public Flux<UpdateEvent<ConfigurationFragment<String, AGG, C>>> observe() {
        return Flux.fromIterable(properties.entrySet())
                .map(fragmentFactory::createFragment)
                .map(fragment -> new UpdateEvent<>(UpdateType.ADD, fragment));
    }
}
