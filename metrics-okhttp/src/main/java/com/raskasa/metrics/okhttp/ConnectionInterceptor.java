package com.raskasa.metrics.okhttp;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * An {@link Interceptor} that monitors the number of submitted, running, and
 * completed network requests.  Also, keeps a {@link Timer} for the request
 * duration.
 */
final class ConnectionInterceptor extends EventListener {
    private final Meter requests;
    private final Meter failed;
    private final Meter acquired;
    private final Meter released;

    ConnectionInterceptor(MetricRegistry registry, String name) {
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
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
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
