package com.contentgrid.configuration.properties.spring;

import java.util.Map;
import lombok.Data;

@Data
public class ConfigurationDiscoveryProperties {
    private String compositionKey;
    private Map<String, String> configuration;
}
