package com.contentgrid.configuration.api.lookup;

import com.contentgrid.configuration.api.observable.Observable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Lookup<L, V> extends Observable<Map.Entry<L, Collection<V>>> {
    Set<L> keys();
    Collection<V> get(L key);
}
