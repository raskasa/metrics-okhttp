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
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/** Wraps an {@link OkHttpClient} in order to provide data about its internals. */
final class InstrumentedOkHttpClient extends OkHttpClient {
  private static final Logger LOG = LoggerFactory.getLogger(InstrumentedOkHttpClient.class);
  private final MetricRegistry registry;
  private final OkHttpClient rawClient;
  private final String name;

  InstrumentedOkHttpClient(MetricRegistry registry, OkHttpClient rawClient, String name) {
    this.rawClient = rawClient;
    this.registry = registry;
    this.name = name;
    instrumentHttpCache();
    instrumentConnectionPool();
    instrumentExecutorService();
  }

  /**
   * Generates an identifier, with a common prefix, in order to uniquely
   * identify the {@code metric} in the registry.
   *
   * <p>The generated identifier includes:
   *
   * <ul>
   *   <li>the fully qualified name of the {@link OkHttpClient} class</li>
   *   <li>the name of the instrumented client, if provided</li>
   *   <li>the given {@code metric}</li>
   * </ul>
   */
  String metricId(String metric) {
    return name(OkHttpClient.class, name, metric);
  }

  private void instrumentHttpCache() {
    if (getCache() == null) return;

    registry.register(metricId("cache-request-count"), new Gauge<Integer>() {
      @Override public Integer getValue() {
        return rawClient.getCache().getRequestCount();  // The number of HTTP requests issued since this cache was created.
      }
    });
    registry.register(metricId("cache-hit-count"), new Gauge<Integer>() {
      @Override public Integer getValue() {
        return rawClient.getCache().getHitCount();  // ... the number of those requests that required network use.
      }
    });
    registry.register(metricId("cache-network-count"), new Gauge<Integer>() {
      @Override public Integer getValue() {
        return rawClient.getCache().getNetworkCount();  // ... the number of those requests whose responses were served by the cache.
      }
    });
    registry.register(metricId("cache-write-success-count"), new Gauge<Integer>() {
      @Override public Integer getValue() {
        return rawClient.getCache().getWriteSuccessCount();
      }
    });
    registry.register(metricId("cache-write-abort-count"), new Gauge<Integer>() {
      @Override public Integer getValue() {
        return rawClient.getCache().getWriteAbortCount();
      }
    });
    final Gauge<Long> currentCacheSize = new Gauge<Long>() {
      @Override public Long getValue() {
        try {
          return rawClient.getCache().getSize();
        } catch (IOException ex) {
          LOG.error(ex.getMessage(), ex);
          return -1L;
        }
      }
    };
    final Gauge<Long> maxCacheSize = new Gauge<Long>() {
      @Override public Long getValue() {
        return rawClient.getCache().getMaxSize();
      }
    };
    registry.register(metricId("cache-current-size"), currentCacheSize);
    registry.register(metricId("cache-max-size"), maxCacheSize);
    registry.register(metricId("cache-size"), new RatioGauge() {
      @Override protected Ratio getRatio() {
        return Ratio.of(currentCacheSize.getValue(), maxCacheSize.getValue());
      }
    });
  }

  private void instrumentConnectionPool() {
    if (getConnectionPool() == null) rawClient.setConnectionPool(ConnectionPool.getDefault());

    registry.register(metricId("connection-pool-count"), new Gauge<Integer>() {
      @Override public Integer getValue() {
        return rawClient.getConnectionPool().getConnectionCount();
      }
    });
    registry.register(metricId("connection-pool-count-http"), new Gauge<Integer>() {
      @Override public Integer getValue() {
        return rawClient.getConnectionPool().getHttpConnectionCount();
      }
    });
    registry.register(metricId("connection-pool-count-multiplexed"), new Gauge<Integer>() {
      @Override public Integer getValue() {
        return rawClient.getConnectionPool().getMultiplexedConnectionCount();
      }
    });
  }

  private void instrumentExecutorService() {
    InstrumentedExecutorService executorService =
        new InstrumentedExecutorService(rawClient.getDispatcher().getExecutorService(),
            registry,
            name(OkHttpClient.class, this.name));

    rawClient.setDispatcher(new Dispatcher(executorService));
  }

  @Override public void setConnectTimeout(long timeout, TimeUnit unit) {
    rawClient.setConnectTimeout(timeout, unit);
  }

  @Override public int getConnectTimeout() {
    return rawClient.getConnectTimeout();
  }

  @Override public void setReadTimeout(long timeout, TimeUnit unit) {
    rawClient.setReadTimeout(timeout, unit);
  }

  @Override public int getReadTimeout() {
    return rawClient.getReadTimeout();
  }

  @Override public void setWriteTimeout(long timeout, TimeUnit unit) {
    rawClient.setWriteTimeout(timeout, unit);
  }

  @Override public int getWriteTimeout() {
    return rawClient.getWriteTimeout();
  }

  @Override public OkHttpClient setProxy(Proxy proxy) {
    return rawClient.setProxy(proxy);
  }

  @Override public Proxy getProxy() {
    return rawClient.getProxy();
  }

  @Override public OkHttpClient setProxySelector(ProxySelector proxySelector) {
    return rawClient.setProxySelector(proxySelector);
  }

  @Override public ProxySelector getProxySelector() {
    return rawClient.getProxySelector();
  }

  @Override public OkHttpClient setCookieHandler(CookieHandler cookieHandler) {
    return rawClient.setCookieHandler(cookieHandler);
  }

  @Override public CookieHandler getCookieHandler() {
    return rawClient.getCookieHandler();
  }

  @Override public OkHttpClient setCache(Cache cache) {
    return rawClient.setCache(cache);
  }

  @Override public Cache getCache() {
    return rawClient.getCache();
  }

  @Override public OkHttpClient setSocketFactory(SocketFactory socketFactory) {
    return rawClient.setSocketFactory(socketFactory);
  }

  @Override public SocketFactory getSocketFactory() {
    return rawClient.getSocketFactory();
  }

  @Override public OkHttpClient setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
    return rawClient.setSslSocketFactory(sslSocketFactory);
  }

  @Override public SSLSocketFactory getSslSocketFactory() {
    return rawClient.getSslSocketFactory();
  }

  @Override public OkHttpClient setHostnameVerifier(HostnameVerifier hostnameVerifier) {
    return rawClient.setHostnameVerifier(hostnameVerifier);
  }

  @Override public HostnameVerifier getHostnameVerifier() {
    return rawClient.getHostnameVerifier();
  }

  @Override public OkHttpClient setCertificatePinner(CertificatePinner certificatePinner) {
    return rawClient.setCertificatePinner(certificatePinner);
  }

  @Override public CertificatePinner getCertificatePinner() {
    return rawClient.getCertificatePinner();
  }

  @Override public OkHttpClient setAuthenticator(Authenticator authenticator) {
    return rawClient.setAuthenticator(authenticator);
  }

  @Override public Authenticator getAuthenticator() {
    return rawClient.getAuthenticator();
  }

  @Override public OkHttpClient setConnectionPool(ConnectionPool connectionPool) {
    return rawClient.setConnectionPool(connectionPool);
  }

  @Override public ConnectionPool getConnectionPool() {
    return rawClient.getConnectionPool();
  }

  @Override public OkHttpClient setFollowSslRedirects(boolean followProtocolRedirects) {
    return rawClient.setFollowSslRedirects(followProtocolRedirects);
  }

  @Override public boolean getFollowSslRedirects() {
    return rawClient.getFollowSslRedirects();
  }

  @Override public void setFollowRedirects(boolean followRedirects) {
    rawClient.setFollowRedirects(followRedirects);
  }

  @Override public boolean getFollowRedirects() {
    return rawClient.getFollowRedirects();
  }

  @Override public void setRetryOnConnectionFailure(boolean retryOnConnectionFailure) {
    rawClient.setRetryOnConnectionFailure(retryOnConnectionFailure);
  }

  @Override public boolean getRetryOnConnectionFailure() {
    return rawClient.getRetryOnConnectionFailure();
  }

  @Override public OkHttpClient setDispatcher(Dispatcher dispatcher) {
    return rawClient.setDispatcher(dispatcher);
  }

  @Override public Dispatcher getDispatcher() {
    return rawClient.getDispatcher();
  }

  @Override public OkHttpClient setProtocols(List<Protocol> protocols) {
    return rawClient.setProtocols(protocols);
  }

  @Override public List<Protocol> getProtocols() {
    return rawClient.getProtocols();
  }

  @Override public OkHttpClient setConnectionSpecs(List<ConnectionSpec> connectionSpecs) {
    return rawClient.setConnectionSpecs(connectionSpecs);
  }

  @Override public List<ConnectionSpec> getConnectionSpecs() {
    return rawClient.getConnectionSpecs();
  }

  @Override public List<Interceptor> interceptors() {
    return rawClient.interceptors();
  }

  @Override public List<Interceptor> networkInterceptors() {
    return rawClient.networkInterceptors();
  }

  @Override public Call newCall(Request request) {
    return rawClient.newCall(request);
  }

  @Override public OkHttpClient cancel(Object tag) {
    return rawClient.cancel(tag);
  }

  @Override public OkHttpClient clone() {
    return rawClient.clone();
  }

  @Override public boolean equals(Object obj) {
    return (obj instanceof InstrumentedOkHttpClient
        && rawClient.equals(((InstrumentedOkHttpClient) obj).rawClient))
        || rawClient.equals(obj);
  }

  @Override public String toString() {
    return rawClient.toString();
  }
}
