package com.contentgrid.configuration.api.lookup;

import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.api.observable.Observer;
import com.contentgrid.configuration.api.observable.Publisher;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * A thread-safe higher level data structure that wraps a map and supports creating multiple lookup indexes. Requires an
 * identity function for the data structure to be stored.
 *
 * @param <K> the type of id
 * @param <V> the type of the stored values
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcurrentLookup<K, V> implements Observer<V>, Observable<Map.Entry<K, V>>, AutoCloseable {

    @NonNull
    private final Function<V, K> identityFunction;

    @NonNull
    private final ReadWriteLock readWriteLock;


    public ConcurrentLookup(Function<V, K> identityFunction) {
        this(identityFunction, new ReentrantReadWriteLock());
    }

    public ConcurrentLookup(Function<V, K> identityFunction, Observable<V> observable) {
        this(identityFunction);
        subscribe(observable);
    }

    private final Set<Index<?, V>> indices = new HashSet<>();

    private final Map<K, V> data = new ConcurrentHashMap<>();

    private final Publisher<Map.Entry<K, V>> publisher = new Publisher<>(data::entrySet);

    public Set<K> keys() {
        return Set.copyOf(data.keySet());
    }

    public final V add(@NonNull V item) {
        var id = Objects.requireNonNull(this.identityFunction.apply(item), "identity(%s) is null".formatted(item));
        var writeLock = this.readWriteLock.writeLock();

        try {
            writeLock.lock();

            var old = this.data.put(id, item);

            if(old == null) {
                publisher.emit(UpdateType.ADD, Map.entry(id, item));
            } else {
                publisher.emit(UpdateType.UPDATE, Map.entry(id, item));
            }

            // update all the indices
            for (var index : this.indices) {

                // remove the old item from the index
                if (old != null) {
                    index.remove(old);
                }

                // store the new item in the index
                index.store(item);
            }

            return old;
        } finally {
            writeLock.unlock();
        }
    }

    public final V get(@NonNull K id) {
        return this.data.get(id);
    }

    public final V remove(@NonNull K id) {
        var writeLock = this.readWriteLock.writeLock();

        try {
            writeLock.lock();
            var old = this.data.remove(id);

            if (old != null) {
                publisher.emit(UpdateType.REMOVE, Map.entry(id, old));

                // remove the old item from the index
                for (var index : this.indices) {
                    index.remove(old);
                }
            }

            return old;
        } finally {
            writeLock.unlock();
        }
    }


    public void clear() {
        var writeLock = this.readWriteLock.writeLock();

        try {
            writeLock.lock();
            this.data.forEach((k, v) -> remove(k));
        } finally {
            writeLock.unlock();
        }
    }

    public int size() {
        return this.data.size();
    }

    private void registerIndex(Index<?, V> index) {
        var writeLock = this.readWriteLock.writeLock();

        try {
            writeLock.lock();
            this.indices.add(index);

            // rebuild the index for existing data
            for (var item : this.data.values()) {
                index.store(item);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void unregisterIndex(Index<?, V> index) {
        var writeLock = this.readWriteLock.writeLock();
        try {
            writeLock.lock();
            this.indices.remove(index);
        } finally {
            writeLock.unlock();

        }
    }

    public final <L> Lookup<L, V> createLookup(Function<V, L> indexFunction) {
        return createMultiLookup(indexFunction.andThen(Stream::of));
    }

    public final <L> Lookup<L, V> createMultiLookup(Function<V, Stream<L>> indexFunction) {
        var index = new MultiIndex<>(indexFunction, this::unregisterIndex);
        registerIndex(index);
        return index;
    }

    public Stream<V> stream() {
        return this.data.values().stream();
    }

    @Override
    public void subscribe(Observable<V> observable) {
        observable.observe().subscribe(event -> {
            switch (event.getType()) {
                case ADD, UPDATE -> add(event.getValue());
                case REMOVE -> remove(identityFunction.apply(event.getValue()));
            }
        });
    }

    @Override
    public Flux<UpdateEvent<Entry<K, V>>> observe() {
        return publisher.observe();
    }

    @Override
    public void close() throws Exception {
        var lock = readWriteLock.writeLock();
        try {
            lock.lock();
            for (Index<?, V> index : indices) {
                index.close();
            }
        } finally {
            lock.unlock();
        }
    }

    private interface Index<L, T> extends Lookup<L, T>, AutoCloseable {

        void store(T data);

        void remove(T data);

    }

    private static class MultiIndex<L, T> implements Index<L, T> {
        private final Map<L, Collection<T>> data = new ConcurrentHashMap<>();
        private final Function<T, Stream<L>> indexFunction;
        private final Publisher<Map.Entry<L, Collection<T>>> publisher = new Publisher<>(data::entrySet);
        private final Consumer<Index<L, T>> onClose;

        MultiIndex(@NonNull Function<T, Stream<L>> indexFunction, Consumer<Index<L, T>> onClose) {
            this.indexFunction = indexFunction;
            this.onClose = onClose;
        }

        @Override
        public Set<L> keys() {
            return Set.copyOf(data.keySet());
        }

        @Override
        public List<T> get(L key) {
            return List.copyOf(this.data.getOrDefault(key, Set.of()));
        }

        @Override
        public void store(T data) {
            this.indexFunction.apply(data).forEachOrdered(key -> {
                Objects.requireNonNull(key, "key cannot be null");
                this.data.compute(key, (k, dataCollection) -> {
                    if(dataCollection == null) {
                        var newCollection = Set.of(data);
                        publisher.emit(UpdateType.ADD, Map.entry(key, newCollection));
                        return newCollection;
                    } else {
                        var newCollection = Stream.concat(dataCollection.stream(), Stream.of(data))
                                .collect(Collectors.toUnmodifiableSet());
                        publisher.emit(UpdateType.UPDATE, Map.entry(key, newCollection));
                        return newCollection;
                    }
                });
            });
        }

        @Override
        public void remove(T data) {
            this.indexFunction.apply(data).forEach(key -> {
                this.data.computeIfPresent(Objects.requireNonNull(key), (k, dataCollection) -> {
                    var newCollection = new HashSet<>(dataCollection);
                    var hasRemoved = newCollection.remove(data);
                    var unmodifiableCollection = Set.copyOf(newCollection);
                    if(hasRemoved && newCollection.isEmpty()) {
                        publisher.emit(UpdateType.REMOVE, Map.entry(k, dataCollection));
                        return null;
                    } else if(hasRemoved) {
                        publisher.emit(UpdateType.UPDATE, Map.entry(k, unmodifiableCollection));
                    }
                    return unmodifiableCollection;
                });
            });
        }

        @Override
        public Flux<UpdateEvent<Entry<L, Collection<T>>>> observe() {
            return publisher.observe();
        }

        @Override
        public void close() {
            onClose.accept(this);
            publisher.close();
            data.clear();
        }
    }
}