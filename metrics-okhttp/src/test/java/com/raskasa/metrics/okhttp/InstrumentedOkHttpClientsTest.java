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
import com.squareup.okhttp.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public final class InstrumentedOkHttpClientsTest {
  private MetricRegistry registry;

  @Before public void setUp() throws Exception {
    registry = mock(MetricRegistry.class);
  }

  @After public void tearDown() throws Exception {
    registry = null;
  }

  @Test public void createWithoutClient() throws Exception {
    OkHttpClient client = InstrumentedOkHttpClients.create(registry);

    // The connection, read, and write timeouts are the only configurations applied by default.
    assertThat(client.getConnectTimeout()).isEqualTo(10_000);
    assertThat(client.getReadTimeout()).isEqualTo(10_000);
    assertThat(client.getWriteTimeout()).isEqualTo(10_000);
  }

  @Test public void createWithClient() throws Exception {
    OkHttpClient rawClient = new OkHttpClient();
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, "service-name", rawClient);

    assertThat(client.getDispatcher()).isEqualTo(rawClient.getDispatcher());
    assertThat(client.getProxy()).isEqualTo(rawClient.getProxy());
    assertThat(client.getProtocols()).isEqualTo(rawClient.getProtocols());
    assertThat(client.getConnectionSpecs()).isEqualTo(rawClient.getConnectionSpecs());
    assertThat(client.getProxySelector()).isEqualTo(rawClient.getProxySelector());
    assertThat(client.getCookieHandler()).isEqualTo(rawClient.getCookieHandler());
    assertThat(client.getCache()).isEqualTo(rawClient.getCache());
    assertThat(client.getSocketFactory()).isEqualTo(rawClient.getSocketFactory());
    assertThat(client.getSslSocketFactory()).isEqualTo(rawClient.getSslSocketFactory());
    assertThat(client.getHostnameVerifier()).isEqualTo(rawClient.getHostnameVerifier());
    assertThat(client.getCertificatePinner()).isEqualTo(rawClient.getCertificatePinner());
    assertThat(client.getAuthenticator()).isEqualTo(rawClient.getAuthenticator());
    assertThat(client.getConnectionPool()).isEqualTo(rawClient.getConnectionPool());
    assertThat(client.getFollowSslRedirects()).isEqualTo(rawClient.getFollowSslRedirects());
    assertThat(client.getFollowRedirects()).isEqualTo(rawClient.getFollowRedirects());
    assertThat(client.getRetryOnConnectionFailure()).isEqualTo(rawClient.getRetryOnConnectionFailure());
    assertThat(client.getConnectTimeout()).isEqualTo(rawClient.getConnectTimeout());
    assertThat(client.getReadTimeout()).isEqualTo(rawClient.getReadTimeout());
    assertThat(client.getWriteTimeout()).isEqualTo(rawClient.getWriteTimeout());
  }
}
