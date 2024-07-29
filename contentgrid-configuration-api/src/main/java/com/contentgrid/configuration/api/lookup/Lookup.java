package com.contentgrid.configuration.api.lookup;

import java.util.Collection;
import java.util.function.Function;

@FunctionalInterface
public interface Lookup<L, V> extends Function<L, Collection<V>> {

}
