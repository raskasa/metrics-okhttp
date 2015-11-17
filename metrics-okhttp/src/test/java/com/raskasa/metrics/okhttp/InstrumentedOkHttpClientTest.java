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
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// TODO: Add tests for instrumentation of the connection pool.

public final class InstrumentedOkHttpClientTest {
  private MetricRegistry mockRegistry;
  private MetricRegistry registry;
  private OkHttpClient rawClient;

  @Before public void setUp() throws Exception {
    mockRegistry = mock(MetricRegistry.class);
    registry = new MetricRegistry();
    rawClient = new OkHttpClient();
  }

  @After public void tearDown() throws Exception {
    mockRegistry = null;
    registry = null;
    rawClient = null;
  }

  @Rule public TemporaryFolder cacheRule = new TemporaryFolder();

  @Test public void httpCacheIsInstrumented() throws Exception {
    MockWebServer server = new MockWebServer();
    MockResponse mockResponse = new MockResponse()
        .addHeader("Cache-Control:public, max-age=31536000")
        .addHeader("Last-Modified: " + formatDate(-1, TimeUnit.HOURS))
        .addHeader("Expires: " + formatDate(1, TimeUnit.HOURS))
        .setBody("one");
    server.enqueue(mockResponse);
    server.start();
    URL baseUrl = server.getUrl("/");

    Cache cache = new Cache(cacheRule.getRoot(), Long.MAX_VALUE);
    rawClient.setCache(cache);
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, "service-name", rawClient);

    String baseName = OkHttpClient.class.getName() + ".service-name";

    assertThat(registry.getGauges()
        .get(baseName + ".cache-max-size")
        .getValue())
        .isEqualTo(Long.MAX_VALUE);
    assertThat(registry.getGauges()
        .get(baseName + ".cache-current-size")
        .getValue())
        .isEqualTo(0L);

    Request request = new Request.Builder().url(baseUrl).build();
    Response response = client.newCall(request).execute();

    assertThat(registry.getGauges()
        .get(baseName + ".cache-current-size")
        .getValue())
        .isEqualTo(rawClient.getCache().getSize());

    response.body().close();
    server.shutdown();
  }

  @Ignore
  @Test public void connectionPoolIsInstrumented() throws Exception {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    server.start();
    URL baseUrl = server.getUrl("/");

    rawClient.setConnectionPool(ConnectionPool.getDefault());
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, "service-name", rawClient);

    String baseName = OkHttpClient.class.getName() + ".service-name";

    assertThat(registry.getGauges()
        .get(baseName + ".connection-pool-count")
        .getValue())
        .isEqualTo(0);
    assertThat(registry.getGauges()
        .get(baseName + ".connection-pool-count-http")
        .getValue())
        .isEqualTo(0);
    assertThat(registry.getGauges()
        .get(baseName + ".connection-pool-count-multiplexed")
        .getValue())
        .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    Response resp1 = client.newCall(req1).execute();
    Response resp2 = client.newCall(req2).execute();

    assertThat(registry.getGauges()
        .get(baseName + ".connection-pool-count")
        .getValue())
        .isEqualTo(1);
    assertThat(registry.getGauges()
        .get(baseName + ".connection-pool-count-http")
        .getValue())
        .isEqualTo(1);
    assertThat(registry.getGauges()
        .get(baseName + ".connection-pool-count-multiplexed")
        .getValue())
        .isEqualTo(0);

    resp1.body().close();
    resp2.body().close();
    server.shutdown();
  }

  @Test public void executorServiceIsInstrumented() throws Exception {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    server.start();
    URL baseUrl = server.getUrl("/");

    rawClient.setDispatcher(new Dispatcher(MoreExecutors.newDirectExecutorService()));  // Force the requests to execute on this unit tests thread.
    rawClient.setConnectionPool(ConnectionPool.getDefault());
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, "service-name", rawClient);

    String baseName = OkHttpClient.class.getName() + ".service-name";

    assertThat(registry.getMeters()
        .get(baseName + ".network-requests-submitted")
        .getCount())
        .isEqualTo(0);
    assertThat(registry.getMeters()
        .get(baseName + ".network-requests-completed")
        .getCount())
        .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    client.newCall(req1).enqueue(new TestCallback());
    client.newCall(req2).enqueue(new TestCallback());

    assertThat(registry.getMeters()
        .get(baseName + ".network-requests-submitted")
        .getCount())
        .isEqualTo(2);
    assertThat(registry.getMeters()
        .get(baseName + ".network-requests-completed")
        .getCount())
        .isEqualTo(2);
  }

  @Test public void equality() throws Exception {
    InstrumentedOkHttpClient clientA = new InstrumentedOkHttpClient(mockRegistry, "service-name", rawClient);
    InstrumentedOkHttpClient clientB = new InstrumentedOkHttpClient(mockRegistry, "service-name", rawClient);

    assertThat(clientA).isEqualTo(clientB);
    assertThat(clientA).isEqualTo(rawClient);
    assertThat(clientB).isEqualTo(rawClient);
  }

  @Test public void stringRepresentation() throws Exception {
    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(mockRegistry, "service-name", rawClient);
    assertThat(client.toString()).isEqualTo(rawClient.toString());
  }

  @Test public void testNameGeneration() {
    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(mockRegistry, "serviceA-client", rawClient);
    String generatedName = client.registryName("cache-hit");
    assertThat(generatedName).isEqualTo("com.squareup.okhttp.OkHttpClient.serviceA-client.cache-hit");
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
