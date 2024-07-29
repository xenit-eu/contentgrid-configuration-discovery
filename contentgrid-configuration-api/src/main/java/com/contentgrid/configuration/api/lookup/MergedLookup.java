package com.contentgrid.configuration.api.lookup;

import java.util.function.Function;

@FunctionalInterface
public interface MergedLookup<L, V> extends Function<L, V> {

}
