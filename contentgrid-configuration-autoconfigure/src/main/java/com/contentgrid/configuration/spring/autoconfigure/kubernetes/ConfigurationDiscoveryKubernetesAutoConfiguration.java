package com.contentgrid.configuration.spring.autoconfigure.kubernetes;

import com.contentgrid.configuration.kubernetes.fabric8.KubernetesInformerObservableFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.kubernetes.fabric8.Fabric8AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration(after = Fabric8AutoConfiguration.class)
@EnableConfigurationProperties(ConfigurationDiscoveryKubernetesProperties.class)
@ConditionalOnClass({KubernetesInformerObservableFactory.class, KubernetesClient.class})
@ConditionalOnBean(KubernetesClient.class)
@Import(KubernetesConfigurationMappingApplicationConfiguration.class)
public class ConfigurationDiscoveryKubernetesAutoConfiguration {

    @Bean(name = "com.contentgrid.configuration.kubernetes.fabric8.KubernetesInformerObservableFactory")
    @ConditionalOnMissingBean
    KubernetesInformerObservableFactory kubernetesInformerConfigurationFragmentObservableFactory(
            KubernetesClient kubernetesClient
    ) {
        return new KubernetesInformerObservableFactory(kubernetesClient, Duration.ZERO);
    }
}
