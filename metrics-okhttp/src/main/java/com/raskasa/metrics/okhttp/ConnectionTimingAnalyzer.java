package com.raskasa.metrics.okhttp;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.Protocol;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * An {@link okhttp3.EventListener} that measures connection setup times.
 */
final class ConnectionTimingAnalyzer extends EventListener {

    private final Histogram setupTimes;
    private long startTime = 0;

    ConnectionTimingAnalyzer(final MetricRegistry registry,
                             final String name) {
        this.setupTimes = registry.histogram(MetricRegistry.name(name, "connection-setup"));
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        startTime = System.nanoTime();
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        setupTimes.update(System.nanoTime() - startTime);
    }
}
