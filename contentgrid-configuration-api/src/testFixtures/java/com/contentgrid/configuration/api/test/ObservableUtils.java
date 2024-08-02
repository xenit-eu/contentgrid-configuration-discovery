package com.contentgrid.configuration.api.test;

import com.contentgrid.configuration.api.lookup.ConcurrentLookup;
import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.api.observable.Observable.UpdateEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ObservableUtils {

    @SneakyThrows
    public <T> List<UpdateEvent<T>> eventsToList(Observable<T> observable) {
        var events = Collections.synchronizedList(new ArrayList<UpdateEvent<T>>());

        var latch = new CountDownLatch(1);
        observable.observe()
                .doOnSubscribe((subscription) -> latch.countDown())
                .subscribe(events::add);

        // Wait until the subscription is active before test code emits events into it
        latch.await();
        return events;
    }

    public <K, V> ConcurrentLookup<K, V> eventsToConcurrentLookup(Observable<V> observable, Function<V, K> identityFunction) {
        return new ConcurrentLookup<>(identityFunction, observable);
    }

}
