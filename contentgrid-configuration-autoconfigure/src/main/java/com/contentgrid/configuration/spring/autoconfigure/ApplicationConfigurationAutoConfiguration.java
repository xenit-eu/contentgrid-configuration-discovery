package com.contentgrid.configuration.spring.autoconfigure;

import com.contentgrid.configuration.api.fragments.ComposedConfigurationRepository;
import com.contentgrid.configuration.applications.ApplicationConfiguration;
import com.contentgrid.configuration.applications.ApplicationId;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({ComposedConfigurationRepository.class, ApplicationConfiguration.class})
public class ApplicationConfigurationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ComposedConfigurationRepository<String, ApplicationId, ApplicationConfiguration> applicationConfigurationConfigurationRepository(
    ) {
        return new ComposedConfigurationRepository<>(ApplicationConfiguration::merge);
    }

}
