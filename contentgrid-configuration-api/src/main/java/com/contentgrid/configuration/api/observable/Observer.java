package com.contentgrid.configuration.api.observable;

public interface Observer<T> {
    void subscribe(Observable<T> observable);
}
