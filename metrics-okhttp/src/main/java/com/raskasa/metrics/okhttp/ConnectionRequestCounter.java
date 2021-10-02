package com.raskasa.metrics.okhttp;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.Protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * An {@link Interceptor} that monitors the number of submitted, running,
 * completed network requests and measures connection setup times.
 */
final class ConnectionRequestCounter extends EventListener {
    private final Meter requests;
    private final Meter failed;
    private final Meter acquired;
    private final Meter released;

    ConnectionRequestCounter(final MetricRegistry registry,
                             final String name) {
        this.requests = registry.meter(MetricRegistry.name(name, "connection-requests"));
        this.failed = registry.meter(MetricRegistry.name(name, "connection-failed"));
        this.acquired = registry.meter(MetricRegistry.name(name, "connection-acquired"));
        this.released = registry.meter(MetricRegistry.name(name, "connection-released"));
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        requests.mark();
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol,
                              IOException ioe) {
        failed.mark();
    }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        acquired.mark();
    }

    @Override
    public void connectionReleased(Call call, Connection connection) {
        released.mark();
    }

}
