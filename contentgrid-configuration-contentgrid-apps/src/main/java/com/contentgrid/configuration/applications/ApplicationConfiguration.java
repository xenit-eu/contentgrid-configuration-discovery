package com.contentgrid.configuration.applications;

import com.contentgrid.configuration.api.ComposedConfiguration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.UtilityClass;

@Value
@Builder
public class ApplicationConfiguration {
    String clientId;

    String clientSecret;

    String issuerUri;

    @NonNull
    @Singular("additionalIssuerUri")
    Set<String> additionalIssuerUris;

    @NonNull
    @Singular
    Set<String> routingDomains;

    @NonNull
    @Singular
    Set<String> corsOrigins;

    @UtilityClass
    public class Keys {

        public static final String CLIENT_ID = "contentgrid.idp.client-id";
        public static final String CLIENT_SECRET = "contentgrid.idp.client-secret";
        public static final String ISSUER_URI = "contentgrid.idp.issuer-uri";
        public static final String ADDITIONAL_ISSUER_URIS = "contentgrid.idp.additional-issuer-uris";

        public static final String ROUTING_DOMAINS = "contentgrid.routing.domains";
        public static final String CORS_ORIGINS = "contentgrid.cors.origins";

        static final String DELIMITER_REGEX = "[,;]+";
    }

    public static ApplicationConfiguration fromMap(Map<String, String> configMap) {
        return new ApplicationConfiguration(
                configMap.get(Keys.CLIENT_ID),
                configMap.get(Keys.CLIENT_SECRET),
                configMap.get(Keys.ISSUER_URI),
                split(configMap.get(Keys.ADDITIONAL_ISSUER_URIS)),
                split(configMap.get(Keys.ROUTING_DOMAINS)),
                split(configMap.get(Keys.CORS_ORIGINS))
        );
    }

    public ApplicationConfiguration merge(ApplicationConfiguration other) {
        return new ApplicationConfiguration(
                merge(clientId, other.clientId),
                merge(clientSecret, other.clientSecret),
                merge(issuerUri, other.issuerUri),
                merge(additionalIssuerUris, other.additionalIssuerUris),
                merge(routingDomains, other.routingDomains),
                merge(corsOrigins, other.corsOrigins)
        );
    }

    public ComposedConfiguration<ApplicationId, ApplicationConfiguration> forApplication(ApplicationId applicationId) {
        return new ComposedConfiguration<>() {
            @Override
            public ApplicationId getCompositionKey() {
                return applicationId;
            }

            @Override
            public Optional<ApplicationConfiguration> getConfiguration() {
                return Optional.of(ApplicationConfiguration.this);
            }
        };
    }

    private static <T> T merge(T a, T b) {
        if(a == null) {
            return b;
        }
        return a;
    }

    private static Set<String> merge(Set<String> a , Set<String> b) {
        var merged = new HashSet<String>(a.size()+b.size());
        merged.addAll(a);
        merged.addAll(b);
        return merged;
    }

    private static Set<String> split(String config) {
        if(config == null || config.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(config.split(Keys.DELIMITER_REGEX))
                .map(String::trim)
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static class ApplicationConfigurationBuilder {
        public ComposedConfiguration<ApplicationId, ApplicationConfiguration> buildForApplication(ApplicationId applicationId) {
            return build().forApplication(applicationId);
        }
    }

}
