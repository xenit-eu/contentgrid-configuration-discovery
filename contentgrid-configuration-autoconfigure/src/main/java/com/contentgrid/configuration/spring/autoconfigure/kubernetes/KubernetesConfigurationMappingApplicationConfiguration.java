package com.contentgrid.configuration.spring.autoconfigure.kubernetes;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.applications.ApplicationConfiguration;
import com.contentgrid.configuration.applications.ApplicationId;
import com.contentgrid.configuration.kubernetes.fabric8.ConfigMapConfigurationFragmentFactory;
import com.contentgrid.configuration.kubernetes.fabric8.KubernetesInformerObservableFactory;
import com.contentgrid.configuration.kubernetes.fabric8.SecretConfigurationFragmentFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({KubernetesInformerObservableFactory.class, ApplicationConfiguration.class})
public class KubernetesConfigurationMappingApplicationConfiguration {
    private static final String BEAN_PREFIX = "com.contentgrid.configuration.spring.autoconfigure.KubernetesConfigurationMappingApplicationConfiguration#";
    private static final String LABEL_CONTENTGRID_APPID = "app.contentgrid.com/application-id";

    private static final LabelSelector CONFIG_LABEL_SELECTOR = new LabelSelectorBuilder()
            .addToMatchLabels("app.contentgrid.com/service-type", "gateway")
            .addNewMatchExpression()
            .withKey(LABEL_CONTENTGRID_APPID)
            .withOperator("Exists")
            .endMatchExpression()
            .build();

    @Bean(name = "com.contentgrid.configuration.spring.autoconfigure.kubernetes.KubernetesResourceFilter")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    KubernetesResourceFilter kubernetesResourceFilter(ConfigurationDiscoveryKubernetesProperties configurationDiscoveryKubernetesProperties) {
        return new KubernetesResourceFilter(configurationDiscoveryKubernetesProperties.getNamespace(), CONFIG_LABEL_SELECTOR);
    }

    private ConfigurationFragmentFactory<ConfigMap, String, ApplicationId, ApplicationConfiguration> configMapFragmentFactory() {
        return new ConfigMapConfigurationFragmentFactory<>(
                KubernetesConfigurationMappingApplicationConfiguration::createApplicationIdFromMetadata,
                ApplicationConfiguration::fromMap
        );
    }

    @Bean(name = BEAN_PREFIX + "configMapObservable")
    Observable<ConfigurationFragment<String, ApplicationId, ApplicationConfiguration>> configMapObservable(
            KubernetesResourceFilter resourceFilter,
            KubernetesInformerObservableFactory observableFactory,
            ObjectProvider<ConfigurationFragmentFactory<ConfigMap, String, ApplicationId, ApplicationConfiguration>> fragmentFactory
    ) {
        return observableFactory.inform(kc -> resourceFilter.filter(kc.configMaps()), fragmentFactory.getIfAvailable(this::configMapFragmentFactory));
    }

    private ConfigurationFragmentFactory<Secret, String, ApplicationId, ApplicationConfiguration> secretFragmentFactory() {
        return new SecretConfigurationFragmentFactory<>(
                KubernetesConfigurationMappingApplicationConfiguration::createApplicationIdFromMetadata,
                ApplicationConfiguration::fromMap
        );
    }

    @Bean(name = BEAN_PREFIX + "secretObservable")
    Observable<ConfigurationFragment<String, ApplicationId, ApplicationConfiguration>> secretObservable(
            KubernetesResourceFilter resourceFilter,
            KubernetesInformerObservableFactory observableFactory,
            ObjectProvider<ConfigurationFragmentFactory<Secret, String, ApplicationId, ApplicationConfiguration>> fragmentFactory
    ) {
        return observableFactory.inform(kc -> resourceFilter.filter(kc.secrets()), fragmentFactory.getIfAvailable(this::secretFragmentFactory));
    }

    private static ApplicationId createApplicationIdFromMetadata(HasMetadata cm) {
        return ApplicationId.from(cm.getMetadata().getLabels().get(LABEL_CONTENTGRID_APPID));
    }

}
