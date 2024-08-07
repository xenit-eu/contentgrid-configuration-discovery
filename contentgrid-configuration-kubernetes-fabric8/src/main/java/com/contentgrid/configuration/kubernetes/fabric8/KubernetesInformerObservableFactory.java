package com.contentgrid.configuration.kubernetes.fabric8;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import com.contentgrid.configuration.api.observable.Observable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Informable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KubernetesInformerObservableFactory implements AutoCloseable {
    private final KubernetesClient kubernetesClient;
    private final Duration resyncInterval;

    private final Set<AutoCloseable> closeables = new HashSet<>();


    public <T extends HasMetadata, F, K, C> Observable<ConfigurationFragment<F, K, C>> inform(
            Function<KubernetesClient, Informable<T>> resourceSelector,
            ConfigurationFragmentFactory<T, F, K, C> configurationFragmentFactory
    ) {
        var informer = resourceSelector.apply(kubernetesClient)
                .runnableInformer(resyncInterval.toMillis());

        closeables.add(informer);

        var observableEventHandler = new ObservableResourceEventHandler<T>(informer);

        closeables.add(observableEventHandler);

        informer.addEventHandler(observableEventHandler);

        informer.run();

        return () -> {
            return observableEventHandler.observe()
                    .map(event -> event.mapValue(configurationFragmentFactory::createFragment));
        };
    }

    @Override
    public void close() throws Exception {
        var toClose = Set.copyOf(this.closeables);
        this.closeables.clear();
        for (AutoCloseable closeable : toClose) {
            closeable.close();
        }
    }
}
