package com.contentgrid.configuration.api;

import java.util.Optional;

public interface HasConfiguration<C> {
    Optional<C> getConfiguration();
}
