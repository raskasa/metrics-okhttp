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
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class InstrumentedOkHttpClientTest {
  private MetricRegistry registry;
  private OkHttpClient rawClient;

  @Before public void setUp() throws Exception {
    registry = new MetricRegistry();
    rawClient = new OkHttpClient();
  }

  @Rule public MockWebServer server = new MockWebServer();
  @Rule public TemporaryFolder cacheRule = new TemporaryFolder();

  @Test public void syncNetworkRequestsAreInstrumented() throws IOException {
    MockResponse mockResponse = new MockResponse()
        .addHeader("Cache-Control:public, max-age=31536000")
        .addHeader("Last-Modified: " + formatDate(-1, TimeUnit.HOURS))
        .addHeader("Expires: " + formatDate(1, TimeUnit.HOURS))
        .setBody("one");
    server.enqueue(mockResponse);
    HttpUrl baseUrl = server.url("/");

    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(registry, rawClient, null);

    assertThat(registry.getMeters()
        .get(client.metricId("network-requests-submitted"))
        .getCount())
        .isEqualTo(0);
    assertThat(registry.getCounters()
        .get(client.metricId("network-requests-running"))
        .getCount())
        .isEqualTo(0);
    assertThat(registry.getMeters()
        .get(client.metricId("network-requests-completed"))
        .getCount())
        .isEqualTo(0);
    assertThat(registry.getTimers()
        .get(client.metricId("network-requests-duration"))
        .getCount())
        .isEqualTo(0);

    Request request = new Request.Builder().url(baseUrl).build();

    try (Response response = client.newCall(request).execute()) {
      assertThat(registry.getMeters()
          .get(client.metricId("network-requests-submitted"))
          .getCount())
          .isEqualTo(1);
      assertThat(registry.getCounters()
          .get(client.metricId("network-requests-running"))
          .getCount())
          .isEqualTo(0);
      assertThat(registry.getMeters()
          .get(client.metricId("network-requests-completed"))
          .getCount())
          .isEqualTo(1);
      assertThat(registry.getTimers()
          .get(client.metricId("network-requests-duration"))
          .getCount())
          .isEqualTo(1);
    }
  }

  @Test public void aSyncNetworkRequestsAreInstrumented() {
    MockResponse mockResponse = new MockResponse()
        .addHeader("Cache-Control:public, max-age=31536000")
        .addHeader("Last-Modified: " + formatDate(-1, TimeUnit.HOURS))
        .addHeader("Expires: " + formatDate(1, TimeUnit.HOURS))
        .setBody("one");
    server.enqueue(mockResponse);
    HttpUrl baseUrl = server.url("/");

    final InstrumentedOkHttpClient client =
        new InstrumentedOkHttpClient(registry, rawClient, null);

    assertThat(registry.getMeters()
        .get(client.metricId("network-requests-submitted"))
        .getCount())
        .isEqualTo(0);
    assertThat(registry.getCounters()
        .get(client.metricId("network-requests-running"))
        .getCount())
        .isEqualTo(0);
    assertThat(registry.getMeters()
        .get(client.metricId("network-requests-completed"))
        .getCount())
        .isEqualTo(0);
    assertThat(registry.getTimers()
        .get(client.metricId("network-requests-duration"))
        .getCount())
        .isEqualTo(0);

    final Request request = new Request.Builder().url(baseUrl).build();

    client.newCall(request).enqueue(new Callback() {
      @Override public void onFailure(Call call, IOException e) {
        fail();
      }

      @Override public void onResponse(Call call, Response response) throws IOException {
        assertThat(registry.getMeters()
            .get(client.metricId("network-requests-submitted"))
            .getCount())
            .isEqualTo(1);
        assertThat(registry.getCounters()
            .get(client.metricId("network-requests-running"))
            .getCount())
            .isEqualTo(0);
        assertThat(registry.getMeters()
            .get(client.metricId("network-requests-completed"))
            .getCount())
            .isEqualTo(1);
        assertThat(registry.getTimers()
            .get(client.metricId("network-requests-duration"))
            .getCount())
            .isEqualTo(1);
        response.body().close();
      }
    });
  }

  @Test public void httpCacheIsInstrumented() throws Exception {
    MockResponse mockResponse = new MockResponse()
        .addHeader("Cache-Control:public, max-age=31536000")
        .addHeader("Last-Modified: " + formatDate(-1, TimeUnit.HOURS))
        .addHeader("Expires: " + formatDate(1, TimeUnit.HOURS))
        .setBody("one");
    server.enqueue(mockResponse);
    HttpUrl baseUrl = server.url("/");

    Cache cache = new Cache(cacheRule.getRoot(), Long.MAX_VALUE);
    rawClient = rawClient.newBuilder().cache(cache).build();
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
        .isEqualTo(rawClient.cache().size());

    response.body().close();
  }

  @Test public void connectionPoolIsInstrumented() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(registry, rawClient, null);

    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-total-count"))
        .getValue())
        .isEqualTo(0);
    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-idle-count"))
        .getValue())
        .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    Response resp1 = client.newCall(req1).execute();
    Response resp2 = client.newCall(req2).execute();

    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-total-count"))
        .getValue())
        .isEqualTo(2);
    assertThat(registry.getGauges()
        .get(client.metricId("connection-pool-idle-count"))
        .getValue())
        .isEqualTo(0);

    resp1.body().close();
    resp2.body().close();
  }

  @Test public void connectionInterceptorIsInstrumented() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(registry, rawClient, null);

    assertThat(registry.getMeters()
            .get(client.metricId("connection-requests"))
            .getCount())
            .isEqualTo(0);
    assertThat(registry.getMeters()
            .get(client.metricId("connection-failed"))
            .getCount())
            .isEqualTo(0);
    assertThat(registry.getMeters()
            .get(client.metricId("connection-acquired"))
            .getCount())
            .isEqualTo(0);
    assertThat(registry.getMeters()
            .get(client.metricId("connection-released"))
            .getCount())
            .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    Response resp1 = client.newCall(req1).execute();
    Response resp2 = client.newCall(req2).execute();

    resp1.body().close();
    resp2.body().close();

    assertThat(registry.getMeters()
            .get(client.metricId("connection-requests"))
            .getCount())
            .isEqualTo(2);
    assertThat(registry.getMeters()
            .get(client.metricId("connection-failed"))
            .getCount())
            .isEqualTo(0);
    assertThat(registry.getMeters()
            .get(client.metricId("connection-acquired"))
            .getCount())
            .isEqualTo(2);
    assertThat(registry.getMeters()
            .get(client.metricId("connection-released"))
            .getCount())
            .isEqualTo(2);

  }

  @Test public void executorServiceIsInstrumented() throws Exception {
    server.enqueue(new MockResponse().setBody("one"));
    server.enqueue(new MockResponse().setBody("two"));
    HttpUrl baseUrl = server.url("/");

    // Force the requests to execute on this unit tests thread.
    rawClient = rawClient.newBuilder()
        .dispatcher(new Dispatcher(MoreExecutors.newDirectExecutorService()))
        .build();
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
    String prefix = "custom";
    String baseId = "network-requests-submitted";

    assertThat(registry.getMeters()).isEmpty();

    InstrumentedOkHttpClient client = new InstrumentedOkHttpClient(registry, rawClient, prefix);
    String generatedId = client.metricId(baseId);

    assertThat(registry.getMeters().get(generatedId)).isNotNull();
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
    @Override public void onFailure(Call call, IOException e) {

    }

    @Override public void onResponse(Call call, Response response) throws IOException {
      response.body().close();
    }
  }
}
