package com.contentgrid.configuration.api.fragments;

public interface ConfigurationFragmentFactory<T, F, K, C> {

    ConfigurationFragment<F, K, C> createFragment(T fragment);
}
