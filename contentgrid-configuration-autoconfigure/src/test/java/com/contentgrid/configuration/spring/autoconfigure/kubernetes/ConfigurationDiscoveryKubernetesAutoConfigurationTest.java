package com.contentgrid.configuration.spring.autoconfigure.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.applications.ApplicationConfiguration;
import com.contentgrid.configuration.applications.ApplicationId;
import com.contentgrid.configuration.kubernetes.fabric8.KubernetesInformerObservableFactory;
import com.contentgrid.configuration.spring.autoconfigure.ApplicationConfigurationAutoConfiguration;
import com.contentgrid.configuration.spring.autoconfigure.ClassLoaderFilters;
import com.contentgrid.configuration.spring.autoconfigure.RunApplicationRunnersOnStartup;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

class ConfigurationDiscoveryKubernetesAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .with(new RunApplicationRunnersOnStartup())
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationDiscoveryKubernetesAutoConfiguration.class,
                    ApplicationConfigurationAutoConfiguration.class
            ));

    private static final ResolvableType APPLICATION_OBSERVABLE = ResolvableType.forType(
            new ParameterizedTypeReference<Observable<ConfigurationFragment<String, ApplicationId, ApplicationConfiguration>>>() {
            });
    @Test
    void doesNotActivate_noKubernetesClient() {
        contextRunner
                .run(context -> {
                    assertThat(context.getBeanProvider(APPLICATION_OBSERVABLE)).isEmpty();
                });
    }

    @Test
    void discovers_withKubernetesClient() {
        contextRunner
                .withBean(KubernetesClient.class, () -> {
                    return Mockito.mock(KubernetesClient.class, Mockito.RETURNS_MOCKS);
                })
                .run(context -> {
                    assertThat(context.getBeanProvider(APPLICATION_OBSERVABLE)).hasSize(2);
                });
    }

    @Test
    void doesNotActivate_propertyDisabled() {
        contextRunner
                .withBean(KubernetesClient.class, () -> {
                    return Mockito.mock(KubernetesClient.class, Mockito.RETURNS_MOCKS);
                })
                .withPropertyValues("contentgrid.configuration.discovery.kubernetes.enabled=false")
                .run(context -> {
                    assertThat(context.getBeanProvider(APPLICATION_OBSERVABLE)).isEmpty();
                });
    }

    @Test
    void doesNotActivate_missingApplicationClass() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(ClassLoaderFilters.CONTENTGRID_APPS))
                .withBean(KubernetesClient.class, () -> {
                    return Mockito.mock(KubernetesClient.class, Mockito.RETURNS_MOCKS);
                })
                .run(context -> {
                    assertThat(context.getBeanProvider(APPLICATION_OBSERVABLE)).isEmpty();
                    assertThat(context).hasSingleBean(KubernetesInformerObservableFactory.class);
                });

    }
}