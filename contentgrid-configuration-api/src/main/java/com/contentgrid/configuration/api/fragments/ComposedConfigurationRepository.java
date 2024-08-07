package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.ComposedConfiguration;
import com.contentgrid.configuration.api.ConfigurationRepository;
import com.contentgrid.configuration.api.lookup.ConcurrentLookup;
import com.contentgrid.configuration.api.lookup.Lookup;
import com.contentgrid.configuration.api.observable.Observable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ComposedConfigurationRepository<F, K, C> implements ConfigurationRepository<K, C>,
        DynamicallyConfigurable<F, K, C>,
        Observable<ComposedConfiguration<K, C>>,
        AutoCloseable
{

    @NonNull
    private final BinaryOperator<C> reducer;

    private final ConcurrentLookup<F, ConfigurationFragment<F, K, C>> lookup = new ConcurrentLookup<>(ConfigurationFragment::getFragmentId);
    private final Lookup<K, ConfigurationFragment<F, K, C>> composedLookup = lookup.createLookup(ConfigurationFragment::getCompositionKey);

    public ComposedConfigurationRepository(BinaryOperator<C> reducer, Observable<ConfigurationFragment<F, K, C>> observable) {
        this(reducer);
        subscribe(observable);
    }

    @Override
    public ComposedConfiguration<K, C> findConfiguration(K compositionKey) {
        return composeConfiguration(Map.entry(compositionKey, composedLookup.get(compositionKey)));
    }

    private ComposedConfiguration<K, C> composeConfiguration(Map.Entry<K, Collection<ConfigurationFragment<F, K, C>>> entry) {
        var configuration = entry.getValue().stream()
                .map(ConfigurationFragment::getConfiguration)
                .flatMap(Optional::stream)
                .reduce(reducer);

        return new ComposedConfigurationImpl<>(entry.getKey(), configuration.orElse(null));
    }

    @Override
    public Stream<K> compositionKeys() {
        return composedLookup.keys().stream();
    }

    @Override
    public void register(ConfigurationFragment<F, K, C> fragment) {
        lookup.add(fragment);
    }

    @Override
    public void revoke(F fragmentId) {
        lookup.remove(fragmentId);
    }

    @Override
    public Flux<UpdateEvent<ComposedConfiguration<K, C>>> observe() {
        return composedLookup.observe()
                .map(event -> event.mapValue(this::composeConfiguration));
    }

    @Override
    public void close() throws Exception {
        lookup.close();
    }

    @Value
    private static class ComposedConfigurationImpl<K, C> implements ComposedConfiguration<K, C> {
        @NonNull
        K compositionKey;

        C configuration;

        @Override
        public Optional<C> getConfiguration() {
            return Optional.ofNullable(configuration);
        }
    }
}
