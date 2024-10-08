package com.contentgrid.configuration.spring.autoconfigure.kubernetes;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("contentgrid.configuration.discovery.kubernetes")
@Data
public class ConfigurationDiscoveryKubernetesProperties {
    private String namespace;
}
