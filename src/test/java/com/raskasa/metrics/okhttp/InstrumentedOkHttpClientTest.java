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
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
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

import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.mockito.Mockito.mock;

// TODO: Validate that when an OkHttpClient is cloned, the clone's usage is tracked as well.
// TODO: Add tests for instrumentation of the connection pool.
// TODO: Add tests for instrumentation of internal dispatcher.

public final class InstrumentedOkHttpClientTest {
  private MetricRegistry registry;

  @Before public void setUp() throws Exception {
    registry = mock(MetricRegistry.class);
  }

  @After public void tearDown() throws Exception {
    registry = null;
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

    MetricRegistry registry = new MetricRegistry();
    OkHttpClient rawClient = new OkHttpClient();
    Cache cache = new Cache(cacheRule.getRoot(), Long.MAX_VALUE);
    rawClient.setCache(cache);
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, rawClient);

    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName() + ".cache-max-size")
        .getValue())
        .isEqualTo(Long.MAX_VALUE);
    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName() + ".cache-current-size")
        .getValue())
        .isEqualTo(0L);

    Request request = new Request.Builder().url(baseUrl).build();
    Response response = client.newCall(request).execute();

    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName() + ".cache-current-size")
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

    MetricRegistry registry = new MetricRegistry();
    OkHttpClient rawClient = new OkHttpClient();
    rawClient.setConnectionPool(ConnectionPool.getDefault());
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, rawClient);

    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName() + ".connection-pool-count")
        .getValue())
        .isEqualTo(0);
    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName() + ".connection-pool-count-http")
        .getValue())
        .isEqualTo(0);
    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName()+".connection-pool-count-multiplexed")
        .getValue())
        .isEqualTo(0);

    Request req1 = new Request.Builder().url(baseUrl).build();
    Request req2 = new Request.Builder().url(baseUrl).build();
    Response resp1 = client.newCall(req1).execute();
    Response resp2 = client.newCall(req2).execute();

    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName() + ".connection-pool-count")
        .getValue())
        .isEqualTo(1);
    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName() + ".connection-pool-count-http")
        .getValue())
        .isEqualTo(1);
    assertThat(registry.getGauges()
        .get(OkHttpClient.class.getName()+".connection-pool-count-multiplexed")
        .getValue())
        .isEqualTo(0);

    resp1.body().close();
    resp2.body().close();
    server.shutdown();
  }

  @Test public void equality() throws Exception {
    OkHttpClient client = new OkHttpClient();
    InstrumentedOkHttpClient iClientA = new InstrumentedOkHttpClient(registry, client);
    InstrumentedOkHttpClient iClientB = new InstrumentedOkHttpClient(registry, client);

    assertThat(iClientA).isEqualTo(iClientB);
    assertThat(iClientA).isEqualTo(client);
    assertThat(iClientB).isEqualTo(client);
  }

  @Test public void stringRepresentation() throws Exception {
    OkHttpClient client = new OkHttpClient();
    InstrumentedOkHttpClient iClient = new InstrumentedOkHttpClient(registry, client);

    assertThat(client.toString()).isEqualTo(iClient.toString());
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
}
