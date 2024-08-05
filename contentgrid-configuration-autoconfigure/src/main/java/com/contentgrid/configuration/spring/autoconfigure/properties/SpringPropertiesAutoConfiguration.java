package com.contentgrid.configuration.spring.autoconfigure.properties;

import com.contentgrid.configuration.properties.spring.SpringPropertiesConfigurationFragmentObservable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnClass({SpringPropertiesConfigurationFragmentObservable.class})
@Import(SpringPropertiesMappingApplicationConfiguration.class)
@EnableConfigurationProperties(StaticConfigurationProperties.class)
public class SpringPropertiesAutoConfiguration {


}
