package com.contentgrid.configuration.api.fragments;

public interface ConfigurationFragmentFactory<T, ID, AGG, C> {

    ConfigurationFragment<ID, AGG, C> createFragment(T fragment);
}
