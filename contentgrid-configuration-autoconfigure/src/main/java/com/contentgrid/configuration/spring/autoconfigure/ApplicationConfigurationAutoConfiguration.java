package com.contentgrid.configuration.spring.autoconfigure;

import com.contentgrid.configuration.api.fragments.ComposedConfigurationRepository;
import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.fragments.DynamicallyConfigurable;
import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.applications.ApplicationConfiguration;
import com.contentgrid.configuration.applications.ApplicationId;
import java.util.List;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;

@AutoConfiguration
@ConditionalOnClass({ComposedConfigurationRepository.class, ApplicationConfiguration.class})
public class ApplicationConfigurationAutoConfiguration {

    private static final String BEAN_PREFIX = "com.contentgrid.configuration.spring.autoconfigure.ApplicationConfigurationAutoConfiguration#";

    @Bean(name = BEAN_PREFIX + "configurationRepository")
    @ConditionalOnMissingBean
    ComposedConfigurationRepository<String, ApplicationId, ApplicationConfiguration> configurationRepository() {
        return new ComposedConfigurationRepository<>(ApplicationConfiguration::merge);
    }

    @Bean(name = BEAN_PREFIX + "subscribeRunner")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    ApplicationRunner subscribeRunner(
            List<Observable<ConfigurationFragment<String, ApplicationId, ApplicationConfiguration>>> observables,
            DynamicallyConfigurable<String, ApplicationId, ApplicationConfiguration> configurationRepository
    ) {
        return (args) -> {
            observables.forEach(configurationRepository::subscribe);
        };
    }
}
