package com.contentgrid.configuration.api;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;

public interface DynamicallyConfigurable<ID, AGG, C> {

    void register(ConfigurationFragment<ID, AGG, C> fragment);

    void revoke(ID fragmentId);
}
