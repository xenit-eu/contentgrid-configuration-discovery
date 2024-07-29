package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.AggregateIdConfiguration;
import com.contentgrid.configuration.api.DynamicallyConfigurable;
import com.contentgrid.configuration.api.HasConfiguration;
import com.contentgrid.configuration.api.ConfigurationRepository;
import com.contentgrid.configuration.api.observable.Observable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@RequiredArgsConstructor
public class ComposedConfigurationRepository<ID, AGG, C> implements ConfigurationRepository<AGG, C>,
        DynamicallyConfigurable<ID, AGG, C>,
        Observable<AggregateIdConfiguration<AGG, C>>
{

    @NonNull
    private final BinaryOperator<C> reducer;

    private final Map<ID, AGG> fragmentAggregates = new HashMap<>();
    private final Map<AGG, ComposedConfiguration<ID, AGG, C>> configs = new HashMap<>();
    private final Sinks.Many<Observable.UpdateEvent<AggregateIdConfiguration<AGG, C>>> updates = Sinks.many().multicast().onBackpressureBuffer();

    public ComposedConfigurationRepository(BinaryOperator<C> reducer, Observable<ConfigurationFragment<ID, AGG, C>> observable) {
        this(reducer);
        observable.observe()
                .doOnNext(event -> {
                    switch (event.getType()) {
                        case ADD, UPDATE -> register(event.getValue());
                        case REMOVE -> revoke(event.getValue().getFragmentId());
                    }
                });

    }


    @Override
    public HasConfiguration<C> findConfiguration(AGG aggregationId) {
        return configs.getOrDefault(aggregationId, new ComposedConfiguration<>(aggregationId, reducer, List.of()));
    }

    @Override
    public Stream<AGG> aggregationIds() {
        return Set.copyOf(configs.keySet()).stream();
    }

    @Override
    public void register(ConfigurationFragment<ID, AGG, C> fragment) {
        fragmentAggregates.put(fragment.getFragmentId(), fragment.getAggregateId());
        configs.compute(fragment.getAggregateId(), (aggregateId, config) -> {
            if(config == null) {
                return emitEventFor(new ComposedConfiguration<>(aggregateId, reducer, List.of(fragment)), UpdateType.ADD);
            }
            return emitEventFor(config.withFragment(fragment), UpdateType.UPDATE);
        });

    }

    @Override
    public void revoke(ID fragmentId) {
        configs.compute(fragmentAggregates.get(fragmentId), (aggregateId, config) -> {
            if(config != null) {
                var newConfig = config.withoutFragment(fragmentId);
                if(!newConfig.isEmpty()) {
                    return emitEventFor(newConfig, UpdateType.UPDATE);
                }
            }
            emitEventFor(config, UpdateType.REMOVE);
            return null;
        });
        fragmentAggregates.remove(fragmentId);
    }

    private ComposedConfiguration<ID, AGG, C> emitEventFor(ComposedConfiguration<ID, AGG, C> configuration, UpdateType updateType) {
        updates.emitNext(new UpdateEvent<>(updateType, configuration), EmitFailureHandler.FAIL_FAST);
        return configuration;
    }


    @Override
    public Flux<UpdateEvent<AggregateIdConfiguration< AGG, C>>> observe() {
        return updates.asFlux();
    }
}
