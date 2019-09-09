package com.raskasa.metrics.okhttp;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import okhttp3.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Histogram setupTimes;
    private final ConcurrentHashMap<InetSocketAddress, Long> initTimes;

    ConnectionInterceptor(MetricRegistry registry, String name) {
        this.requests = registry.meter(MetricRegistry.name(name, "connection-requests"));
        this.failed = registry.meter(MetricRegistry.name(name, "connection-failed"));
        this.acquired = registry.meter(MetricRegistry.name(name, "connection-acquired"));
        this.released = registry.meter(MetricRegistry.name(name, "connection-released"));
        this.setupTimes = registry.histogram(MetricRegistry.name(name, "connection-setup"));
        // TODO: configure map on the basis of connection pool size
        this.initTimes = new ConcurrentHashMap<>(128, 0.75f, 16);
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        requests.mark();
        initTimes.put(inetSocketAddress, System.currentTimeMillis());
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        Long initTime = initTimes.get(inetSocketAddress);
        if (initTime != null) {
            setupTimes.update(System.currentTimeMillis() - initTime);
            initTimes.remove(inetSocketAddress);
        }
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
