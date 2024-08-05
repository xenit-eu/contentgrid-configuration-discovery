package com.contentgrid.configuration.spring.autoconfigure;

import com.contentgrid.configuration.api.DynamicallyConfigurable;
import com.contentgrid.configuration.api.fragments.ComposedConfigurationRepository;
import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.applications.ApplicationConfiguration;
import com.contentgrid.configuration.applications.ApplicationId;
import com.contentgrid.configuration.kubernetes.fabric8.ConfigMapConfigurationFragmentFactory;
import com.contentgrid.configuration.kubernetes.fabric8.KubernetesInformerConfigurationFragmentObservableFactory;
import com.contentgrid.configuration.kubernetes.fabric8.SecretConfigurationFragmentFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({KubernetesInformerConfigurationFragmentObservableFactory.class, ApplicationConfiguration.class})
public class KubernetesConfigurationMappingApplicationConfiguration {
    @Bean
    ConfigurationFragmentFactory<ConfigMap, String, ApplicationId, ApplicationConfiguration> configMapApplicationConfigurationFragmentFactory() {
        return new ConfigMapConfigurationFragmentFactory<>(
                KubernetesConfigurationMappingApplicationConfiguration::createApplicationIdFromMetadata,
                ApplicationConfiguration::fromMap
        );
    }

    @Bean
    Observable<ConfigurationFragment<String, ApplicationId, ApplicationConfiguration>> configMapApplicationConfigurationFragmentObservable(
            ConfigurationDiscoveryKubernetesProperties kubernetesProperties,
            KubernetesInformerConfigurationFragmentObservableFactory observableFactory,
            ConfigurationFragmentFactory<ConfigMap, String, ApplicationId, ApplicationConfiguration> fragmentFactory
    ) {
        return observableFactory.inform(kc -> kc.configMaps().inNamespace(kubernetesProperties.getNamespace()).withLabelSelector(CONFIG_LABEL_SELECTOR), fragmentFactory);
    }

    @Bean
    ConfigurationFragmentFactory<Secret, String, ApplicationId, ApplicationConfiguration> secretApplicationConfigurationFragmentFactory() {
        return new SecretConfigurationFragmentFactory<>(
                KubernetesConfigurationMappingApplicationConfiguration::createApplicationIdFromMetadata,
                ApplicationConfiguration::fromMap
        );
    }

    @Bean
    Observable<ConfigurationFragment<String, ApplicationId, ApplicationConfiguration>> secretApplicationConfigurationFragmentObservable(
            ConfigurationDiscoveryKubernetesProperties kubernetesProperties,
            KubernetesInformerConfigurationFragmentObservableFactory observableFactory,
            ConfigurationFragmentFactory<Secret, String, ApplicationId, ApplicationConfiguration> fragmentFactory
    ) {
        return observableFactory.inform(kc -> kc.secrets().inNamespace(kubernetesProperties.getNamespace()).withLabelSelector(CONFIG_LABEL_SELECTOR), fragmentFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    ComposedConfigurationRepository<String, ApplicationId, ApplicationConfiguration> applicationConfigurationConfigurationRepository(
    ) {
        return new ComposedConfigurationRepository<>(ApplicationConfiguration::merge);
    }

    @Bean
    ApplicationRunner subscribeApplicationConfigurationRepository(
            List<Observable<ConfigurationFragment<String, ApplicationId, ApplicationConfiguration>>> observables,
            DynamicallyConfigurable<String, ApplicationId, ApplicationConfiguration> configurationRepository
    ) {
        return (args) -> {
            observables.forEach(configurationRepository::subscribe);
        };
    }

    private static final String LABEL_CONTENTGRID_APPID = "app.contentgrid.com/application-id";

    private static final LabelSelector CONFIG_LABEL_SELECTOR = new LabelSelectorBuilder()
            .addToMatchLabels("app.contentgrid.com/service-type", "gateway")
            .addNewMatchExpression()
            .withKey(LABEL_CONTENTGRID_APPID)
            .withOperator("Exists")
            .endMatchExpression()
            .build();


    private static ApplicationId createApplicationIdFromMetadata(HasMetadata cm) {
        return ApplicationId.from(cm.getMetadata().getLabels().get(LABEL_CONTENTGRID_APPID));
    }

}
