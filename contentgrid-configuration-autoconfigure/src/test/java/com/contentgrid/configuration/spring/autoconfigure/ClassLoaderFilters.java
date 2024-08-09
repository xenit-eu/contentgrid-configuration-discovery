package com.contentgrid.configuration.spring.autoconfigure;

import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.boot.test.context.FilteredClassLoader;

@UtilityClass
public class ClassLoaderFilters {
    public static final Predicate<String> CONTENTGRID_APPS = FilteredClassLoader.PackageFilter.of("com.contentgrid.configuration.applications");
    public static final Predicate<String> KUBERNETES = FilteredClassLoader.PackageFilter.of("com.contentgrid.configuration.kubernetes.fabric8");
    public static final Predicate<String> SPRING_PROPERTIES = FilteredClassLoader.PackageFilter.of("com.contentgrid.configuration.properties.spring");
}
