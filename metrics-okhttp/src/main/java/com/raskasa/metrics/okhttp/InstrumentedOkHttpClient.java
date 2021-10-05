/*
 * Copyright 2015 Ras Kasa Williams
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.raskasa.metrics.okhttp;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wraps an {@link OkHttpClient} in order to provide data about its internals. */
final class InstrumentedOkHttpClient extends OkHttpClient {

  InstrumentedOkHttpClient(OkHttpClient rawClient) {
    super((rawClient.newBuilder()));
  }

  /**
   * Example if we have to wrap any function @Override public Call newCall(Request request) { return
   * super.newCall(request); }
   */
  public static class InstrumentedOkHttpClientBuilder {
    private static final Logger LOG =
        LoggerFactory.getLogger(InstrumentedOkHttpClientBuilder.class);
    private final MetricRegistry registry;
    private final String name;
    private OkHttpClient rawClient;

    public InstrumentedOkHttpClientBuilder(MetricRegistry registry, String name) {
      this.registry = registry;
      this.name = name;
    }

    public static InstrumentedOkHttpClientBuilder newBuilder(MetricRegistry registry, String name) {
      return new InstrumentedOkHttpClientBuilder(registry, name);
    }

    public InstrumentedOkHttpClientBuilder withClient(OkHttpClient rawClient) {
      this.rawClient = rawClient;
      return this;
    }

    public InstrumentedOkHttpClient build() {
      if (rawClient == null) {
        rawClient = new OkHttpClient();
      }
      instrumentHttpCache();
      instrumentConnectionPool();
      instrumentNetworkRequests();
      instrumentConnectionListener();
      return new InstrumentedOkHttpClient(rawClient);
    }

    /**
     * Generates an identifier, with a common prefix, in order to uniquely identify the {@code
     * metric} in the registry.
     *
     * <p>
     *
     * <p>The generated identifier includes:
     *
     * <p>
     *
     * <ul>
     *   <li>the fully qualified name of the {@link OkHttpClient} class
     *   <li>the name of the instrumented client, if provided
     *   <li>the given {@code metric}
     * </ul>
     */
    String metricId(String metric) {
      return name(OkHttpClient.class, name, metric);
    }

    private void instrumentHttpCache() {
      Cache cache = rawClient.cache();
      if (cache == null) return;

      // The number of HTTP requests issued since this cache was created.
      registry.register(metricId("cache-request-count"), (Gauge<Integer>) cache::requestCount);
      // ... the number of those requests that required network use.
      registry.register(metricId("cache-hit-count"), (Gauge<Integer>) cache::hitCount);
      // ... the number of those requests whose responses were served by the cache.
      registry.register(metricId("cache-network-count"), (Gauge<Integer>) cache::networkCount);
      registry.register(
          metricId("cache-write-success-count"), (Gauge<Integer>) cache::writeSuccessCount);
      registry.register(
          metricId("cache-write-abort-count"), (Gauge<Integer>) cache::writeAbortCount);
      final Gauge<Long> currentCacheSize =
          () -> {
            try {
              return cache.size();
            } catch (IOException ex) {
              LOG.error(ex.getMessage(), ex);
              return -1L;
            }
          };
      final Gauge<Long> maxCacheSize = cache::maxSize;
      registry.register(metricId("cache-current-size"), currentCacheSize);
      registry.register(metricId("cache-max-size"), maxCacheSize);
      registry.register(
          metricId("cache-size"),
          new RatioGauge() {
            @Override
            protected Ratio getRatio() {
              return Ratio.of(currentCacheSize.getValue(), maxCacheSize.getValue());
            }
          });
    }

    private void instrumentConnectionPool() {
      ConnectionPool connectionPool = rawClient.connectionPool();
      registry.register(
          metricId("connection-pool-total-count"),
          (Gauge<Integer>) connectionPool::connectionCount);
      registry.register(
          metricId("connection-pool-idle-count"),
          (Gauge<Integer>) connectionPool::idleConnectionCount);
    }

    private void instrumentNetworkRequests() {
      rawClient =
          rawClient
              .newBuilder()
              .addNetworkInterceptor(
                  new InstrumentedInterceptor(registry, name(OkHttpClient.class, this.name)))
              .build();
    }

    private void instrumentConnectionListener() {
      final List<EventListener.Factory> factories = new ArrayList<>();
      final EventListener connectionRequestCounter =
          new ConnectionRequestCounter(registry, name(OkHttpClient.class, this.name));
      factories.add(call -> connectionRequestCounter);
      factories.add(
          call -> new ConnectionTimingAnalyzer(registry, name(OkHttpClient.class, this.name)));
      final EventListener.Factory rawFactory = rawClient.eventListenerFactory();
      factories.add(rawFactory);
      rawClient =
          rawClient
              .newBuilder()
              .eventListenerFactory(new WrappedEventListenerFactory(factories))
              .build();
    }
  }
}
