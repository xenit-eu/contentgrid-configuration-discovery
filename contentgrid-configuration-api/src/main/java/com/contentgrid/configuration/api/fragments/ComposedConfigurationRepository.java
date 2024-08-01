package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.AggregateIdConfiguration;
import com.contentgrid.configuration.api.DynamicallyConfigurable;
import com.contentgrid.configuration.api.ConfigurationRepository;
import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.api.observable.Publisher;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ComposedConfigurationRepository<ID, AGG, C> implements ConfigurationRepository<AGG, C>,
        DynamicallyConfigurable<ID, AGG, C>,
        Observable<AggregateIdConfiguration<AGG, C>>,
        AutoCloseable
{

    @NonNull
    private final BinaryOperator<C> reducer;

    private final Map<ID, AGG> fragmentAggregates = new HashMap<>();
    private final Map<AGG, ComposedConfiguration<ID, AGG, C>> configs = new HashMap<>();

    @Delegate(types = AutoCloseable.class)
    private final Publisher<AggregateIdConfiguration<AGG, C>> publisher = new Publisher<>(() -> configs.values().stream().map(c -> c));

    public ComposedConfigurationRepository(BinaryOperator<C> reducer, Observable<ConfigurationFragment<ID, AGG, C>> observable) {
        this(reducer);
        subscribe(observable);
    }


    @Override
    public AggregateIdConfiguration<AGG, C> findConfiguration(AGG aggregationId) {
        return configs.getOrDefault(aggregationId, new ComposedConfiguration<>(aggregationId, reducer, List.of()));
    }

    @Override
    public Stream<AGG> aggregationIds() {
        return Set.copyOf(configs.keySet()).stream();
    }

    @Override
    public void register(ConfigurationFragment<ID, AGG, C> fragment) {
        var oldAggregateId = fragmentAggregates.put(fragment.getFragmentId(), fragment.getAggregateId());
        if(!Objects.equals(oldAggregateId, fragment.getAggregateId())) {
            revokeFragment(fragment.getFragmentId(), oldAggregateId);
        }
        configs.compute(fragment.getAggregateId(), (aggregateId, config) -> {
            if(config == null) {
                return emitEventFor(new ComposedConfiguration<>(aggregateId, reducer, List.of(fragment)), UpdateType.ADD);
            }
            return emitEventFor(config.withFragment(fragment), UpdateType.UPDATE);
        });

    }

    @Override
    public void revoke(ID fragmentId) {
        var aggregateId = fragmentAggregates.remove(fragmentId);
        revokeFragment(fragmentId, aggregateId);
    }

    private void revokeFragment(ID fragmentId, AGG aggregateId) {
        if(aggregateId == null) {
            return;
        }
        configs.compute(aggregateId, (_aggregateId, config) -> {
            if(config != null) {
                var newConfig = config.withoutFragment(fragmentId);
                if(!newConfig.isEmpty()) {
                    return emitEventFor(newConfig, UpdateType.UPDATE);
                }
                emitEventFor(config, UpdateType.REMOVE);
            }
            return null;
        });

    }

    private ComposedConfiguration<ID, AGG, C> emitEventFor(ComposedConfiguration<ID, AGG, C> configuration, UpdateType updateType) {
        publisher.emit(updateType, configuration);
        return configuration;
    }


    @Override
    public Flux<UpdateEvent<AggregateIdConfiguration< AGG, C>>> observe() {
        return publisher.observe();
    }
}
