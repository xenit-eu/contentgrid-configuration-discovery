package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import com.contentgrid.configuration.api.observable.Observable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@RequiredArgsConstructor
public class KubernetesInformerConfigurationFragmentObservable<T extends HasMetadata, ID, AGG, C> implements Observable<ConfigurationFragment<ID, AGG, C>> {
    private final SharedIndexInformer<T> informer;
    private final long resyncSeconds;
    private final ConfigurationFragmentFactory<T, ID, AGG, C> configurationFragmentFactory;

    @Override
    public Flux<UpdateEvent<ConfigurationFragment<ID, AGG, C>>> observe() {
        var handler = new UpdateEmitterResourceEventHandler<T>();
        informer.addEventHandlerWithResyncPeriod(handler, resyncSeconds*1000L);

        return handler.observe()
                .map(event -> event.mapValue(configurationFragmentFactory::createFragment));
    }

    private static class UpdateEmitterResourceEventHandler<T> implements ResourceEventHandler<T>, Observable<T> {
        private final Sinks.Many<Observable.UpdateEvent<T>> updates = Sinks.many().multicast().onBackpressureBuffer();

        @Override
        public void onAdd(T obj) {
            emit(UpdateType.ADD, obj);

        }

        @Override
        public void onUpdate(T oldObj, T newObj) {
            emit(UpdateType.UPDATE, newObj);

        }

        @Override
        public void onDelete(T obj, boolean deletedFinalStateUnknown) {
            emit(UpdateType.REMOVE, obj);
        }

        private void emit(UpdateType updateType, T object) {
            updates.emitNext(new UpdateEvent<>(updateType, object), EmitFailureHandler.FAIL_FAST);
        }


        @Override
        public Flux<UpdateEvent<T>> observe() {
            return updates.asFlux();
        }
    }
}
