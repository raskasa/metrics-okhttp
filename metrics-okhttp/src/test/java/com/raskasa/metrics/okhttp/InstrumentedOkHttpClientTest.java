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

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.MoreExecutors;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.ConnectionPoolProxy;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public final class InstrumentedOkHttpClientTest {
  private Runnable emptyRunnable = new Runnable() {
    @Override public void run() {
    }
  };

  private MetricRegistry mockRegistry;
  private MetricRegistry registry;
  private OkHttpClient rawClient;

  @Before public void setUp() throws Exception {
    mockRegistry = mock(MetricRegistry.class);
    registry = new MetricRegistry();
    rawClient = new OkHttpClient();
  }

  @Rule public MockWebServer server = new MockWebServer();
  @Rule public TemporaryFolder cacheRule = new TemporaryFolder();

  @Test public void httpCacheIsInstrumented() throws Exception {
    MockResponse mockResponse = new MockResponse()
        .addHeader("Cache-Control:public, max-age=31536000")
        .addHeader("Last-Modified: " + formatDate(-1, TimeUnit.HOURS))
        .addHeader("Expires: " + formatDate(1, TimeUnit.HOURS))
        .setBody("one");
    server.enqueue(mockResponse);
    HttpUrl baseUrl = server.url("/");

    Cache cache = new Cache(cacheRule.getRoot(), Long.MAX_VALUE);
    rawClient.setCache(cache);
    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(registry, rawClient, null);

    assertThat(registry.getGauges()
        .get(client.metricId("cache-max-size"))
        .getValue())
        .isEqualTo(Long.MAX_VALUE);
    assertThat(registry.getGauges()
        .get(client.metricId("cache-current-size"))
        .getValue())
        .isEqualTo(0L);

    Request request = new Request.Builder().url(baseUrl).build();
    Response response = client.newCall(request).execute();

    assertThat(registry.getGauges()
        .get(client.metricId("cache-current-size"))
        .getValue())
        .isEqualTo(rawClient.getCache().getSize());

    response.body().close();
  }

  @Test public void connectionPoolIsInstrumented() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    ConnectionPool pool = new ConnectionPool(Integer.MAX_VALUE, 100L);
    new ConnectionPoolProxy(pool, emptyRunnable);  // Used so that the connection pool can be properly unit tested
    rawClient.setConnectionPool(pool);
    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(registry, rawClient, null);

    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-count"))
        .getValue())
        .isEqualTo(0);
    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-count-http"))
        .getValue())
        .isEqualTo(0);
    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-count-multiplexed"))
        .getValue())
        .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    Response resp1 = client.newCall(req1).execute();
    Response resp2 = client.newCall(req2).execute();

    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-count"))
        .getValue())
        .isEqualTo(2);
    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-count-http"))
        .getValue())
        .isEqualTo(2);
    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-count-multiplexed"))
        .getValue())
        .isEqualTo(0);

    resp1.body().close();
    resp2.body().close();
    pool.evictAll();
  }

  @Test public void executorServiceIsInstrumented() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    rawClient.setDispatcher(new Dispatcher(MoreExecutors.newDirectExecutorService()));  // Force the requests to execute on this unit tests thread.
    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(registry, rawClient, null);

    assertThat(registry.getMeters()
        .get(client.metricId("network-requests-submitted"))
        .getCount())
        .isEqualTo(0);
    assertThat(registry.getMeters()
        .get(client.metricId("network-requests-completed"))
        .getCount())
        .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    client.newCall(req1).enqueue(new TestCallback());
    client.newCall(req2).enqueue(new TestCallback());

    assertThat(registry.getMeters()
        .get(client.metricId("network-requests-submitted"))
        .getCount())
        .isEqualTo(2);
    assertThat(registry.getMeters()
        .get(client.metricId("network-requests-completed"))
        .getCount())
        .isEqualTo(2);
  }

  @Test public void providedNameUsedInMetricId() {
    String randomMetric = "network-requests-submitted";
    assertThat(registry.getMeters()).isEmpty();

    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(registry, rawClient, null);
    String generatedId = client.metricId(randomMetric);
    assertThat(registry.getMeters().get(generatedId)).isNotNull();

    client = new InstrumentedOkHttpClient(registry, rawClient, "custom");
    generatedId = client.metricId(randomMetric);
    assertThat(registry.getMeters().get(generatedId)).isNotNull();
  }

  @Test public void equality() throws Exception {
    InstrumentedOkHttpClient clientA = new InstrumentedOkHttpClient(mockRegistry, rawClient, null);
    InstrumentedOkHttpClient clientB = new InstrumentedOkHttpClient(mockRegistry, rawClient, null);

    assertThat(clientA).isEqualTo(clientB);
    assertThat(clientA).isEqualTo(rawClient);
    assertThat(clientB).isEqualTo(rawClient);
  }

  @Test public void stringRepresentation() throws Exception {
    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(mockRegistry, rawClient, null);
    assertThat(client.toString()).isEqualTo(rawClient.toString());
  }

  /**
   * @param delta the offset from the current date to use. Negative
   * values yield dates in the past; positive values yield dates in the
   * future.
   */
  private String formatDate(long delta, TimeUnit timeUnit) {
    return formatDate(new Date(System.currentTimeMillis() + timeUnit.toMillis(delta)));
  }

  private String formatDate(Date date) {
    DateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    rfc1123.setTimeZone(TimeZone.getTimeZone("GMT"));
    return rfc1123.format(date);
  }

  private static final class TestCallback implements Callback {
    @Override public void onFailure(Request request, IOException e) {
    }

    @Override public void onResponse(Response response) throws IOException {
      response.body().close();
    }
  }
}
