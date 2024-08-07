package com.contentgrid.configuration.api.fragments;

import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.api.observable.Observer;

public interface DynamicallyConfigurable<F, K, C> extends Observer<ConfigurationFragment<F, K, C>> {

    void register(ConfigurationFragment<F, K, C> fragment);

    void revoke(F fragmentId);

    @Override
    default void subscribe(Observable<ConfigurationFragment<F, K, C>> observable) {
        observable.observe().subscribe(event -> {
            switch (event.getType()) {
                case ADD, UPDATE -> register(event.getValue());
                case REMOVE -> revoke(event.getValue().getFragmentId());
            }
        });
    }
}
