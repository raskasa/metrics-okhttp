package com.raskasa.metrics.okhttp;

import okhttp3.Call;
import okhttp3.EventListener;

import java.util.List;
import java.util.stream.Collectors;

/*
This class wraps event listener factory so that an additional metric event listener can be added.
 */
final class WrappedEventListenerFactory implements EventListener.Factory {

    private final List<EventListener.Factory> factories;

    public WrappedEventListenerFactory(final List<EventListener.Factory> factories) {
        this.factories = factories;
    }

    @Override
    public EventListener create(final Call call) {
        List<EventListener> listeners =
                factories.stream().map(factory -> factory.create(call)).collect(Collectors.toList());
        return new WrappedEventListener(listeners);
    }
}
