package com.contentgrid.configuration.api.observable;

import java.util.function.Function;
import lombok.Value;
import reactor.core.publisher.Flux;

public interface Observable<T> {
    Flux<UpdateEvent<T>> observe();

    @Value
    class UpdateEvent<T> {
        UpdateType type;
        T value;

        public <V> UpdateEvent<V> mapValue(Function<T, V> mapper) {
            return new UpdateEvent<>(type, mapper.apply(value));
        }
    }

    enum UpdateType {
        ADD,
        UPDATE,
        REMOVE
    }
}
