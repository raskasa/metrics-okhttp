package com.raskasa.metrics.okhttp;

import okhttp3.Call;
import okhttp3.EventListener;

import java.util.List;
import java.util.stream.Collectors;

/**
 * okhttp supports exactly one {@link EventListener.Factory} to be attached. In order to support multiple listeners,
 * this class wraps multiple {@link EventListener.Factory} instances and combines them to create
 * {@link WrappedEventListener}
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

