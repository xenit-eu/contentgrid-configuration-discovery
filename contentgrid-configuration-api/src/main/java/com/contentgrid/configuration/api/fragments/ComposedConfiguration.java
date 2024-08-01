package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.AggregateIdConfiguration;
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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode
public class ComposedConfiguration<ID, AGG, C> implements AggregateIdConfiguration<AGG, C> {
    @NonNull
    @Getter
    @ToString.Include
    private final AGG aggregateId;
    @NonNull
    private final BinaryOperator<C> reducer;
    @NonNull
    private final Map<ID, ConfigurationFragment<ID, AGG, C>> fragments;

    @EqualsAndHashCode.Exclude
    private Optional<C> aggregatedConfiguration = null;

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

    private ComposedConfiguration<ID, AGG, C> withFragments(Map<ID, ConfigurationFragment<ID, AGG, C>> fragments) {
        return new ComposedConfiguration<>(aggregateId, reducer, fragments);
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

    @ToString.Include(name = "configuration")
    public Optional<C> getConfiguration() {
        if(aggregatedConfiguration == null) {
            aggregatedConfiguration = fragments.values().stream()
                    .map(ConfigurationFragment::getConfiguration)
                    .flatMap(Optional::stream)
                    .reduce(this.reducer);
        }
        return aggregatedConfiguration;
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
