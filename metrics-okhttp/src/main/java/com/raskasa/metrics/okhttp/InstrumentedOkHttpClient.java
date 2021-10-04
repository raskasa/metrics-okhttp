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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Wraps an {@link OkHttpClient} in order to provide data about its internals.
 */

/**
 * Wraps an {@link OkHttpClient} in order to provide data about its internals.
 */
final class InstrumentedOkHttpClient extends OkHttpClient {

    InstrumentedOkHttpClient(OkHttpClient rawClient) {
        super((rawClient.newBuilder()));
    }

    /**
     * Example if we have to wrap any function
     *
     * @Override public Call newCall(Request request) {
     * return super.newCall(request);
     * }
     */

    public static class InstrumentedOkHttpClientBuilder {
        private static final Logger LOG = LoggerFactory.getLogger(InstrumentedOkHttpClientBuilder.class);
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
         * Generates an identifier, with a common prefix, in order to uniquely
         * identify the {@code metric} in the registry.
         * <p>
         * <p>The generated identifier includes:
         * <p>
         * <ul>
         * <li>the fully qualified name of the {@link OkHttpClient} class</li>
         * <li>the name of the instrumented client, if provided</li>
         * <li>the given {@code metric}</li>
         * </ul>
         */
        String metricId(String metric) {
            return name(OkHttpClient.class, name, metric);
        }

        private void instrumentHttpCache() {
            if (rawClient.cache() == null) return;

            Cache cache = rawClient.cache();
            registry.register(metricId("cache-request-count"), (Gauge<Integer>) () -> {
                // The number of HTTP requests issued since this cache was created.
                return cache.requestCount();
            });
            registry.register(metricId("cache-hit-count"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    // ... the number of those requests that required network use.
                    return cache.hitCount();
                }
            });
            registry.register(metricId("cache-network-count"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    // ... the number of those requests whose responses were served by the cache.
                    return cache.networkCount();
                }
            });
            registry.register(metricId("cache-write-success-count"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return cache.writeSuccessCount();
                }
            });
            registry.register(metricId("cache-write-abort-count"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return cache.writeAbortCount();
                }
            });
            final Gauge<Long> currentCacheSize = new Gauge<Long>() {
                @Override
                public Long getValue() {
                    try {
                        return cache.size();
                    } catch (IOException ex) {
                        LOG.error(ex.getMessage(), ex);
                        return -1L;
                    }
                }
            };
            final Gauge<Long> maxCacheSize = new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return cache.maxSize();
                }
            };
            registry.register(metricId("cache-current-size"), currentCacheSize);
            registry.register(metricId("cache-max-size"), maxCacheSize);
            registry.register(metricId("cache-size"), new RatioGauge() {
                @Override
                protected Ratio getRatio() {
                    return Ratio.of(currentCacheSize.getValue(), maxCacheSize.getValue());
                }
            });
        }

        private void instrumentConnectionPool() {

            ConnectionPool connectionPool = rawClient.connectionPool();
            registry.register(metricId("connection-pool-total-count"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return connectionPool.connectionCount();
                }
            });
            registry.register(metricId("connection-pool-idle-count"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return connectionPool.idleConnectionCount();
                }
            });
        }

        private void instrumentNetworkRequests() {
            rawClient = rawClient.newBuilder()
                    .addNetworkInterceptor(
                            new InstrumentedInterceptor(registry, name(OkHttpClient.class, this.name)))
                    .build();
        }

        private void instrumentConnectionListener() {
            final List<EventListener.Factory> factories = new ArrayList<>();
            final EventListener connectionRequestCounter = new ConnectionRequestCounter(registry, name(OkHttpClient.class, this.name));
            factories.add(call -> connectionRequestCounter);
            factories.add(call -> new ConnectionTimingAnalyzer(registry, name(OkHttpClient.class, this.name)));
            final EventListener.Factory rawFactory = rawClient.eventListenerFactory();
            factories.add(rawFactory);
            rawClient = rawClient.newBuilder()
                    .eventListenerFactory(new WrappedEventListenerFactory(factories))
                    .build();
        }
    }

}
