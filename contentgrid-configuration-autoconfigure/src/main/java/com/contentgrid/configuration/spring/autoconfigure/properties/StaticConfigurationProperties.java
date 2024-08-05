package com.contentgrid.configuration.spring.autoconfigure.properties;

import com.contentgrid.configuration.properties.spring.ConfigurationDiscoveryProperties;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("contentgrid.configuration.static")
public class StaticConfigurationProperties {
    private Map<String, ConfigurationDiscoveryProperties> applications;
}
