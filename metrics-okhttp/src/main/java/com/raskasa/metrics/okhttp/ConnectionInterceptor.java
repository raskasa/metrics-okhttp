package com.raskasa.metrics.okhttp;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import okhttp3.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link Interceptor} that monitors the number of submitted, running,
 * completed network requests and measures connection setup times.
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
        // Use a histogram to capture the connection setup times (tcp + ssl handshake)
        // This will provide visibility on the latencies incurred in the underlying network infrastructure
        // as well as server load
        this.setupTimes = registry.histogram(MetricRegistry.name(name, "connection-setup"));
        this.initTimes = new ConcurrentHashMap<>(128, 0.75f);
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        requests.mark();
        initTimes.put(inetSocketAddress, System.nanoTime());
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        updateSetupTime(inetSocketAddress);
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        initTimes.remove(inetSocketAddress);
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

    private void updateSetupTime(final InetSocketAddress inetSocketAddress) {
        Long initTime = initTimes.get(inetSocketAddress);
        if (initTime != null) {
            // always remove from map first before updating histogram for some exception may be thrown
            initTimes.remove(inetSocketAddress);
            setupTimes.update(System.nanoTime() - initTime);
        }
    }
}
