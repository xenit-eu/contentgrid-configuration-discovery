package com.contentgrid.configuration.spring.autoconfigure.properties;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.configuration.api.ConfigurationRepository;
import com.contentgrid.configuration.applications.ApplicationConfiguration;
import com.contentgrid.configuration.applications.ApplicationId;
import com.contentgrid.configuration.spring.autoconfigure.ApplicationConfigurationAutoConfiguration;
import com.contentgrid.configuration.spring.autoconfigure.ClassLoaderFilters;
import com.contentgrid.configuration.spring.autoconfigure.RunApplicationRunnersOnStartup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;


class SpringPropertiesAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .with(new RunApplicationRunnersOnStartup())
            .withConfiguration(AutoConfigurations.of(
                    SpringPropertiesAutoConfiguration.class,
                    ApplicationConfigurationAutoConfiguration.class
            ));

    private static final String[] PROPERTIES = new String[] {
            "contentgrid.configuration.static.contentgrid-apps.abc.composition-key=app1",
            "contentgrid.configuration.static.contentgrid-apps.abc.configuration.contentgrid.routing.domains=app1.example,extra-domain.example"
    };

    private static final ResolvableType APPLICATION_REPOSITORY = ResolvableType.forType(
            new ParameterizedTypeReference<ConfigurationRepository<ApplicationId, ApplicationConfiguration>>() {
            });

    @Test
    void createsApplicationFromProperties_empty() {
        contextRunner
                .run(context -> {
                    assertThat(context.<ConfigurationRepository<ApplicationId, ApplicationConfiguration>>getBeanProvider(APPLICATION_REPOSITORY))
                            .singleElement()
                            .satisfies(repo -> {
                                assertThat(repo.compositionKeys()).isEmpty();
                            });
                });
    }

    @Test
    void createsApplicationFromProperties_hasProperties() {
        contextRunner
                .withPropertyValues(PROPERTIES)
                .run(context -> {
                    assertThat(context.<ConfigurationRepository<ApplicationId, ApplicationConfiguration>>getBeanProvider(APPLICATION_REPOSITORY))
                            .singleElement()
                            .satisfies(repo -> {
                                assertThat(repo.compositionKeys()).containsExactly(ApplicationId.from("app1"));
                                assertThat(repo.findConfiguration(ApplicationId.from("app1")).getConfiguration())
                                        .hasValueSatisfying(config -> {
                                            assertThat(config.getRoutingDomains()).containsExactlyInAnyOrder(
                                                    "app1.example",
                                                    "extra-domain.example"
                                            );
                                        });
                            });
                });
    }

    @Test
    void doesNotProcessProperties_missingPropertiesSpring() {
        contextRunner
                .withPropertyValues(PROPERTIES)
                .withClassLoader(new FilteredClassLoader(ClassLoaderFilters.SPRING_PROPERTIES))
                .run(context -> {
                    assertThat(
                            context.<ConfigurationRepository<ApplicationId, ApplicationConfiguration>>getBeanProvider(
                                    APPLICATION_REPOSITORY))
                            .singleElement()
                            .satisfies(repo -> {
                                assertThat(repo.compositionKeys()).isEmpty();
                            });
                });
    }

    @Test
    void doesNotProcessProperties_missingApplication() {
        contextRunner
                .withPropertyValues(PROPERTIES)
                .withClassLoader(new FilteredClassLoader(ClassLoaderFilters.CONTENTGRID_APPS))
                .run(context -> {
                    assertThat(context.getBeanProvider(APPLICATION_REPOSITORY)).isEmpty();
                });
    }
}