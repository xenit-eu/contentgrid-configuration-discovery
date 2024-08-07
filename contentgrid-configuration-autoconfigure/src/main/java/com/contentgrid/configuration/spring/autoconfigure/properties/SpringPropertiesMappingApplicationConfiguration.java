package com.contentgrid.configuration.spring.autoconfigure.properties;

import com.contentgrid.configuration.api.fragments.ConfigurationFragmentFactory;
import com.contentgrid.configuration.applications.ApplicationConfiguration;
import com.contentgrid.configuration.applications.ApplicationId;
import com.contentgrid.configuration.properties.spring.ConfigurationDiscoveryProperties;
import com.contentgrid.configuration.properties.spring.SpringPropertiesConfigurationFragmentFactory;
import com.contentgrid.configuration.properties.spring.SpringPropertiesConfigurationFragmentObservable;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({SpringPropertiesConfigurationFragmentObservable.class, ApplicationConfiguration.class})
public class SpringPropertiesMappingApplicationConfiguration {
    @Bean
    ConfigurationFragmentFactory<Map.Entry<String, ConfigurationDiscoveryProperties>, String, ApplicationId, ApplicationConfiguration> propertiesApplicationConfigurationFragmentFactory() {
        return new SpringPropertiesConfigurationFragmentFactory<>(
                ApplicationId::from,
                ApplicationConfiguration::fromMap
        );
    }

    @Bean
    SpringPropertiesConfigurationFragmentObservable<ApplicationId, ApplicationConfiguration> propertiesApplicationConfigurationFragmentObservable(
            StaticConfigurationProperties staticConfigurationProperties,
            ConfigurationFragmentFactory<Map.Entry<String, ConfigurationDiscoveryProperties>, String, ApplicationId, ApplicationConfiguration> fragmentFactory
    ) {
        return new SpringPropertiesConfigurationFragmentObservable<>(
                staticConfigurationProperties.getContentgridApps(),
                fragmentFactory
        );
    }

}
