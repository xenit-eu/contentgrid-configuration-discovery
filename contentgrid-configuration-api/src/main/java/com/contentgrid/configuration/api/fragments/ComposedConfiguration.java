package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.AggregateIdConfiguration;
import com.contentgrid.configuration.api.HasConfiguration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ComposedConfiguration<ID, AGG, C> implements AggregateIdConfiguration<AGG, C> {
    @NonNull
    @Getter
    private final AGG aggregateId;
    @NonNull
    private final BinaryOperator<C> reducer;
    @NonNull
    @With(AccessLevel.PRIVATE)
    private final Map<ID, ConfigurationFragment<ID, AGG, C>> fragments;

    public ComposedConfiguration(
            @NonNull AGG aggregateId,
            @NonNull BinaryOperator<C> reducer,
            @NonNull Collection<ConfigurationFragment<ID, AGG, C>> fragments
    ) {
        this.aggregateId = aggregateId;
        this.reducer = reducer;
        this.fragments = Collections.unmodifiableMap(
                fragments.stream()
                        .map(Objects::requireNonNull)
                        .map(this::verifyAggregateId)
                        .collect(Collectors.toMap(ConfigurationFragment::getFragmentId, Function.identity()))
        );
    }

    private ConfigurationFragment<ID, AGG, C> verifyAggregateId(ConfigurationFragment<ID, AGG, C> fragment) {
        if(!Objects.equals(aggregateId, fragment.getAggregateId())) {
            throw new IllegalArgumentException("Fragment has aggregate id %s; expected %s".formatted(fragment.getAggregateId(), aggregateId));
        }
        return fragment;
    }

    public boolean isEmpty() {
        return fragments.isEmpty();
    }

    public Optional<C> getConfiguration() {
        return fragments.values().stream()
                .map(ConfigurationFragment::getConfiguration)
                .flatMap(Optional::stream)
                .reduce(this.reducer);
    }

    public ComposedConfiguration<ID, AGG, C> withFragment(@NonNull ConfigurationFragment<ID, AGG, C> fragment) {
        verifyAggregateId(fragment);

        var fragmentsCopy = new HashMap<>(fragments);
        fragmentsCopy.put(fragment.getFragmentId(), fragment);
        return withFragments(fragmentsCopy);

    }

    public ComposedConfiguration<ID, AGG, C> withoutFragment(@NonNull ID fragmentId) {
        var fragmentsCopy = new HashMap<>(fragments);
        if (fragmentsCopy.remove(fragmentId) == null) {
            return this;
        }
        return withFragments(fragmentsCopy);
    }

}
