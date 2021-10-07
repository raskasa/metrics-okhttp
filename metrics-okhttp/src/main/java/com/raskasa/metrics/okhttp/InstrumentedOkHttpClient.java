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
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.CookieJar;
import okhttp3.Dispatcher;
import okhttp3.Dns;
import okhttp3.EventListener;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wraps an {@link OkHttpClient} in order to provide data about its internals. */
final class InstrumentedOkHttpClient extends OkHttpClient {
  private static final Logger LOG = LoggerFactory.getLogger(InstrumentedOkHttpClient.class);
  private final MetricRegistry registry;
  private OkHttpClient rawClient;
  private final String name;

  InstrumentedOkHttpClient(MetricRegistry registry, OkHttpClient rawClient, String name) {
    this.rawClient = rawClient;
    this.registry = registry;
    this.name = name;
    instrumentHttpCache();
    instrumentConnectionPool();
    instrumentNetworkRequests();
    instrumentEventListener();
  }

  /**
   * Generates an identifier, with a common prefix, in order to uniquely identify the {@code metric}
   * in the registry.
   *
   * <p>The generated identifier includes:
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
    if (cache() == null) return;

    registry.register(
        metricId("cache-request-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            // The number of HTTP requests issued since this cache was created.
            return rawClient.cache().requestCount();
          }
        });
    registry.register(
        metricId("cache-hit-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            // ... the number of those requests that required network use.
            return rawClient.cache().hitCount();
          }
        });
    registry.register(
        metricId("cache-network-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            // ... the number of those requests whose responses were served by the cache.
            return rawClient.cache().networkCount();
          }
        });
    registry.register(
        metricId("cache-write-success-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return rawClient.cache().writeSuccessCount();
          }
        });
    registry.register(
        metricId("cache-write-abort-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return rawClient.cache().writeAbortCount();
          }
        });
    final Gauge<Long> currentCacheSize =
        new Gauge<Long>() {
          @Override
          public Long getValue() {
            try {
              return rawClient.cache().size();
            } catch (IOException ex) {
              LOG.error(ex.getMessage(), ex);
              return -1L;
            }
          }
        };
    final Gauge<Long> maxCacheSize =
        new Gauge<Long>() {
          @Override
          public Long getValue() {
            return rawClient.cache().maxSize();
          }
        };
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
    if (connectionPool() == null) {
      rawClient = rawClient.newBuilder().connectionPool(new ConnectionPool()).build();
    }

    registry.register(
        metricId("connection-pool-total-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return rawClient.connectionPool().connectionCount();
          }
        });
    registry.register(
        metricId("connection-pool-idle-count"),
        new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return rawClient.connectionPool().idleConnectionCount();
          }
        });
  }

  private void instrumentNetworkRequests() {
    rawClient =
        rawClient
            .newBuilder()
            .addNetworkInterceptor(
                new InstrumentedInterceptor(registry, name(OkHttpClient.class, this.name)))
            .build();
  }

  private void instrumentEventListener() {
    final EventListener.Factory delegate = this.rawClient.eventListenerFactory();
    this.rawClient =
        this.rawClient
            .newBuilder()
            .eventListenerFactory(
                new InstrumentedEventListener.Factory(
                    this.registry, delegate, name(EventListener.class, this.name)))
            .build();
  }

  @Override
  public Authenticator authenticator() {
    return rawClient.authenticator();
  }

  @Override
  public Cache cache() {
    return rawClient.cache();
  }

  @Override
  public CertificatePinner certificatePinner() {
    return rawClient.certificatePinner();
  }

  @Override
  public ConnectionPool connectionPool() {
    return rawClient.connectionPool();
  }

  @Override
  public List<ConnectionSpec> connectionSpecs() {
    return rawClient.connectionSpecs();
  }

  @Override
  public int connectTimeoutMillis() {
    return rawClient.connectTimeoutMillis();
  }

  @Override
  public CookieJar cookieJar() {
    return rawClient.cookieJar();
  }

  @Override
  public Dispatcher dispatcher() {
    return rawClient.dispatcher();
  }

  @Override
  public Dns dns() {
    return rawClient.dns();
  }

  @Override
  public boolean followRedirects() {
    return rawClient.followRedirects();
  }

  @Override
  public boolean followSslRedirects() {
    return rawClient.followSslRedirects();
  }

  @Override
  public HostnameVerifier hostnameVerifier() {
    return rawClient.hostnameVerifier();
  }

  @Override
  public List<Interceptor> interceptors() {
    return rawClient.interceptors();
  }

  @Override
  public List<Interceptor> networkInterceptors() {
    return rawClient.networkInterceptors();
  }

  @Override
  public OkHttpClient.Builder newBuilder() {
    return rawClient.newBuilder();
  }

  @Override
  public Call newCall(Request request) {
    return rawClient.newCall(request);
  }

  @Override
  public WebSocket newWebSocket(Request request, WebSocketListener listener) {
    return rawClient.newWebSocket(request, listener);
  }

  @Override
  public int pingIntervalMillis() {
    return rawClient.pingIntervalMillis();
  }

  @Override
  public List<Protocol> protocols() {
    return rawClient.protocols();
  }

  @Override
  public Proxy proxy() {
    return rawClient.proxy();
  }

  @Override
  public Authenticator proxyAuthenticator() {
    return rawClient.proxyAuthenticator();
  }

  @Override
  public ProxySelector proxySelector() {
    return rawClient.proxySelector();
  }

  @Override
  public int readTimeoutMillis() {
    return rawClient.readTimeoutMillis();
  }

  @Override
  public boolean retryOnConnectionFailure() {
    return rawClient.retryOnConnectionFailure();
  }

  @Override
  public SocketFactory socketFactory() {
    return rawClient.socketFactory();
  }

  @Override
  public SSLSocketFactory sslSocketFactory() {
    return rawClient.sslSocketFactory();
  }

  @Override
  public int writeTimeoutMillis() {
    return rawClient.writeTimeoutMillis();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof InstrumentedOkHttpClient
            && rawClient.equals(((InstrumentedOkHttpClient) obj).rawClient))
        || rawClient.equals(obj);
  }

  @Override
  public String toString() {
    return rawClient.toString();
  }
}
