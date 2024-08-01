package com.contentgrid.configuration.api.observable;

import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@RequiredArgsConstructor
@Slf4j
public class Publisher<T> implements AutoCloseable, Observable<T> {
    private final Sinks.Many<UpdateEvent<T>> sink;
    private final Supplier<Stream<T>> existingDataSupplier;

    public Publisher() {
        this(Stream::empty);
    }

    public Publisher(Supplier<Stream<T>> existingDataSupplier) {
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
        return Flux.concat(
                Flux.fromStream(existingDataSupplier.get())
                        .map(value -> new UpdateEvent<>(UpdateType.ADD, value))
                        .doOnNext(event -> log.trace("Emitting synthetic event {}", event)),
                sink.asFlux()
                        .onBackpressureBuffer()
                        .doOnNext(event -> log.trace("Emitting event {}", event))
        );

    }
}
