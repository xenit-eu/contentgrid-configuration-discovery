package com.contentgrid.configuration.api.observable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@RequiredArgsConstructor
@Slf4j
public class Publisher<T> implements AutoCloseable, Observable<T> {
    private final Sinks.Many<UpdateEvent<T>> sink;
    private final Supplier<Collection<? extends T>> existingDataSupplier;

    public Publisher() {
        this(List::of);
    }

    public Publisher(Supplier<Collection<? extends T>> existingDataSupplier) {
        this(Sinks.many().multicast().directBestEffort(), existingDataSupplier);
    }

    public void emit(UpdateEvent<T> event) {
        sink.emitNext(event, EmitFailureHandler.FAIL_FAST);
    }

    public void emit(UpdateType type, T value) {
        emit(new UpdateEvent<>(type, value));
    }

    @Override
    public void close() {
        sink.emitComplete(EmitFailureHandler.FAIL_FAST);
    }

    @Override
    public Flux<UpdateEvent<T>> observe() {
        var newData = sink.asFlux().share().onBackpressureBuffer();
        // We subscribe immediately so events emitted between starting the iteration on the existingData and subscribing on newData afterwards are buffered
        var earlySubscription = newData.subscribe();
        return Flux.concat(
                Flux.fromIterable(existingDataSupplier.get())
                        .map(data -> new UpdateEvent<>(UpdateType.ADD, (T)data))
                        .doOnNext(event -> log.trace("Emitting synthetic event {}", event)),
                newData
                        // Once we have a subscription going on newData, there is no need to keep the early subscription around
                        .doOnSubscribe((subscription) -> earlySubscription.dispose())
                        .doOnNext(event -> log.trace("Emitting event {}", event))
        );

    }
}
