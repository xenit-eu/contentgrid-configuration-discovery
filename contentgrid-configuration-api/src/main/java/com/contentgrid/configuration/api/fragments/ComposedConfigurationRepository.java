package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.AggregateIdConfiguration;
import com.contentgrid.configuration.api.DynamicallyConfigurable;
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
public class ComposedConfigurationRepository<ID, AGG, C> implements ConfigurationRepository<AGG, C>,
        DynamicallyConfigurable<ID, AGG, C>,
        Observable<AggregateIdConfiguration<AGG, C>>,
        AutoCloseable
{

    @NonNull
    private final BinaryOperator<C> reducer;

    private final ConcurrentLookup<ID, ConfigurationFragment<ID, AGG, C>> lookup = new ConcurrentLookup<>(ConfigurationFragment::getFragmentId);
    private final Lookup<AGG, ConfigurationFragment<ID, AGG, C>> aggregateLookup = lookup.createLookup(ConfigurationFragment::getAggregateId);

    public ComposedConfigurationRepository(BinaryOperator<C> reducer, Observable<ConfigurationFragment<ID, AGG, C>> observable) {
        this(reducer);
        subscribe(observable);
    }

    @Override
    public AggregateIdConfiguration<AGG, C> findConfiguration(AGG aggregationId) {
        return createAggregate(Map.entry(aggregationId, aggregateLookup.get(aggregationId)));
    }

    private AggregateIdConfiguration<AGG, C> createAggregate(Map.Entry<AGG, Collection<ConfigurationFragment<ID, AGG, C>>> entry) {
        var configuration = entry.getValue().stream()
                .map(ConfigurationFragment::getConfiguration)
                .flatMap(Optional::stream)
                .reduce(reducer);

        return new AggregateConfiguration<>(entry.getKey(), configuration.orElse(null));
    }

    @Override
    public Stream<AGG> aggregationIds() {
        return aggregateLookup.keys().stream();
    }

    @Override
    public void register(ConfigurationFragment<ID, AGG, C> fragment) {
        lookup.add(fragment);
    }

    @Override
    public void revoke(ID fragmentId) {
        lookup.remove(fragmentId);
    }

    @Override
    public Flux<UpdateEvent<AggregateIdConfiguration<AGG, C>>> observe() {
        return aggregateLookup.observe()
                .map(event -> event.mapValue(this::createAggregate));
    }

    @Override
    public void close() throws Exception {
        lookup.close();
    }

    @Value
    private static class AggregateConfiguration<AGG, C> implements AggregateIdConfiguration<AGG, C> {
        @NonNull
        AGG aggregateId;

        C configuration;

        @Override
        public Optional<C> getConfiguration() {
            return Optional.ofNullable(configuration);
        }
    }
}
