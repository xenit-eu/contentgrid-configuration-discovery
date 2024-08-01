package com.contentgrid.configuration.api;

import com.contentgrid.configuration.api.fragments.ConfigurationFragment;
import com.contentgrid.configuration.api.observable.Observable;
import com.contentgrid.configuration.api.observable.Observer;

public interface DynamicallyConfigurable<ID, AGG, C> extends Observer<ConfigurationFragment<ID, AGG, C>> {

    void register(ConfigurationFragment<ID, AGG, C> fragment);

    void revoke(ID fragmentId);

    @Override
    default void subscribe(Observable<ConfigurationFragment<ID, AGG, C>> observable) {
        observable.observe().subscribe(event -> {
            switch (event.getType()) {
                case ADD, UPDATE -> register(event.getValue());
                case REMOVE -> revoke(event.getValue().getFragmentId());
            }
        });
    }
}
