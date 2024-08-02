package com.contentgrid.configuration.kubernetes.fabric8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.observable.Observable.UpdateEvent;
import com.contentgrid.configuration.api.observable.Observable.UpdateType;
import com.contentgrid.configuration.api.test.ObservableUtils;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class KubernetesInformerConfigurationFragmentObservableFactoryTest {
    @Container
    private static K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s"));

    private void awaitUntilAsserted(ThrowingRunnable runnable) {
        await()
                .timeout(Duration.of(1, ChronoUnit.SECONDS))
                .untilAsserted(runnable);
    }
    
    
    @Test
    void testWatchesKubernetes() throws Exception {
        var client = new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(k3s.getKubeConfigYaml()))
                .build();
        var namespace = UUID.randomUUID().toString();
        client.namespaces().resource(new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespace)
                        .endMetadata()
                        .build())
                .create();
        var factory = new KubernetesInformerConfigurationFragmentObservableFactory(
                client,
                Duration.of(1, ChronoUnit.MINUTES)
        );

        var observer = factory.inform(c -> c.secrets().inNamespace(namespace), new SecretConfigurationFragmentFactory<>(
                secret -> secret.getMetadata().getLabels().get("aggregation-key")));

        var events = ObservableUtils.eventsToList(observer);

        var secret1 = client.resource(new SecretBuilder()
                        .withNewMetadata()
                        .withNamespace(namespace)
                        .withName("test-secret")
                        .addToLabels("aggregation-key", "xyz")
                        .endMetadata()
                        .addToStringData("my-test-property", "my-value")
                        .build())
                .create();

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(
                    new UpdateEvent<>(UpdateType.ADD, new ConfigurationFragment<>(
                            secret1.getMetadata().getUid(),
                            "xyz",
                            Map.of(
                                    "my-test-property", "my-value"
                            )
                    ))
            );
        });

        client.resource(secret1).edit(secret -> {
            return new SecretBuilder(secret)
                    .addToStringData("other-property", "other-value")
                    .build();
        });

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(
                    new UpdateEvent<>(UpdateType.ADD, new ConfigurationFragment<>(
                            secret1.getMetadata().getUid(),
                            "xyz",
                            Map.of(
                                    "my-test-property", "my-value"
                            )
                    )),
                    new UpdateEvent<>(UpdateType.UPDATE, new ConfigurationFragment<>(
                            secret1.getMetadata().getUid(),
                            "xyz",
                            Map.of(
                                    "my-test-property", "my-value",
                                    "other-property", "other-value"
                            )
                    ))
            );
        });

        client.resource(secret1).delete();

        awaitUntilAsserted(() -> {
            assertThat(events).containsExactly(
                    new UpdateEvent<>(UpdateType.ADD, new ConfigurationFragment<>(
                            secret1.getMetadata().getUid(),
                            "xyz",
                            Map.of(
                                    "my-test-property", "my-value"
                            )
                    )),
                    new UpdateEvent<>(UpdateType.UPDATE, new ConfigurationFragment<>(
                            secret1.getMetadata().getUid(),
                            "xyz",
                            Map.of(
                                    "my-test-property", "my-value",
                                    "other-property", "other-value"
                            )
                    )),
                    new UpdateEvent<>(UpdateType.REMOVE, new ConfigurationFragment<>(
                            secret1.getMetadata().getUid(),
                            "xyz",
                            Map.of(
                                    "my-test-property", "my-value",
                                    "other-property", "other-value"
                            )
                    ))
            );
        });
        
        factory.close();
    }

    @Test
    void testSyntheticAddsOnLateSubscribe() throws Exception {
        var client = new KubernetesClientBuilder()
                .withConfig(Config.fromKubeconfig(k3s.getKubeConfigYaml()))
                .build();
        var namespace = UUID.randomUUID().toString();
        client.namespaces().resource(new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespace)
                        .endMetadata()
                        .build())
                .create();
        var factory = new KubernetesInformerConfigurationFragmentObservableFactory(
                client,
                Duration.of(1, ChronoUnit.MINUTES)
        );

        var observable = factory.inform(c -> c.secrets().inNamespace(namespace), new SecretConfigurationFragmentFactory<>(
                secret -> secret.getMetadata().getLabels().get("aggregation-key")));

        var secret1 = client.resource(new SecretBuilder()
                        .withNewMetadata()
                        .withNamespace(namespace)
                        .withName("test-secret")
                        .addToLabels("aggregation-key", "xyz")
                        .endMetadata()
                        .addToStringData("my-test-property", "my-value")
                        .build())
                .create();

        Thread.sleep(100); // Ensure that secret1 is already received before we subscribe

        var secret2 = client.resource(new SecretBuilder()
                        .withNewMetadata()
                        .withNamespace(namespace)
                        .withName("test-secret2")
                        .addToLabels("aggregation-key", "xyz")
                        .endMetadata()
                        .addToStringData("property-2", "value")
                        .build())
                .create();

        var lookup = ObservableUtils.eventsToConcurrentLookup(observable, ConfigurationFragment::getFragmentId);

        awaitUntilAsserted(() -> {
            assertThat(lookup.get(secret1.getMetadata().getUid())).isEqualTo(new ConfigurationFragment<>(
                    secret1.getMetadata().getUid(),
                    "xyz",
                    Map.of(
                            "my-test-property", "my-value"
                    )
            ));
            assertThat(lookup.get(secret2.getMetadata().getUid())).isEqualTo(new ConfigurationFragment<>(
                    secret2.getMetadata().getUid(),
                    "xyz",
                    Map.of(
                            "property-2", "value"
                    )
            ));
        });

        client.resource(secret2).delete();

        awaitUntilAsserted(() -> {
            assertThat(lookup.get(secret1.getMetadata().getUid())).isNotNull();
            assertThat(lookup.get(secret2.getMetadata().getUid())).isNull();
        });

        var lookup2 = ObservableUtils.eventsToConcurrentLookup(observable, ConfigurationFragment::getFragmentId);

        assertThat(lookup2.get(secret1.getMetadata().getUid())).isNotNull();
        assertThat(lookup2.get(secret2.getMetadata().getUid())).isNull();

        lookup2.close();
        lookup.close();

    }

}