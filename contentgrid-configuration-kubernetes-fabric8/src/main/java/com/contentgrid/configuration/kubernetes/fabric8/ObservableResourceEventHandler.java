package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.api.observable.Publisher;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import java.util.Objects;
import lombok.experimental.Delegate;
import reactor.core.publisher.Flux;

class ObservableResourceEventHandler<T extends HasMetadata> implements ResourceEventHandler<T>, Observable<T>, AutoCloseable {

    @Delegate(types = {AutoCloseable.class})
    private final Publisher<T> publisher = new Publisher<>();

    @Override
    public void onAdd(T obj) {
        publisher.emit(UpdateType.ADD, obj);

    }

    @Override
    public void onUpdate(T oldObj, T newObj) {
        if(!Objects.equals(oldObj.getMetadata().getResourceVersion(), newObj.getMetadata().getResourceVersion())) {
            publisher.emit(UpdateType.UPDATE, newObj);
        }
    }

    @Override
    public void onDelete(T obj, boolean deletedFinalStateUnknown) {
        publisher.emit(UpdateType.REMOVE, obj);
    }

    @Override
    public Flux<UpdateEvent<T>> observe() {
        return publisher.observe();
    }
}
