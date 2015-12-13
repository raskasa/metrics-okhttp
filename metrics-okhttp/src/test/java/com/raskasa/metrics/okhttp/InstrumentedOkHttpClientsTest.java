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
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public final class InstrumentedOkHttpClientsTest {
  private MetricRegistry registry;

  @Before public void setUp() {
    registry = mock(MetricRegistry.class);
  }

  @Test public void instrumentDefaultClient() {
    OkHttpClient client = InstrumentedOkHttpClients.create(registry);

    // The connection, read, and write timeouts are the only configurations applied by default.
    assertThat(client.getConnectTimeout()).isEqualTo(10_000);
    assertThat(client.getReadTimeout()).isEqualTo(10_000);
    assertThat(client.getWriteTimeout()).isEqualTo(10_000);
  }

  @Test public void instrumentAndNameDefaultClient() {
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, "custom");

    // The connection, read, and write timeouts are the only configurations applied by default.
    assertThat(client.getConnectTimeout()).isEqualTo(10_000);
    assertThat(client.getReadTimeout()).isEqualTo(10_000);
    assertThat(client.getWriteTimeout()).isEqualTo(10_000);
  }

  @Test public void instrumentProvidedClient() {
    OkHttpClient rawClient = new OkHttpClient();
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, rawClient);
    assertThatClientsAreEqual(client, rawClient);
  }

  @Test public void instrumentAndNameProvidedClient() {
    OkHttpClient rawClient = new OkHttpClient();
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, rawClient, "custom");
    assertThatClientsAreEqual(client, rawClient);
  }

  private void assertThatClientsAreEqual(OkHttpClient clientA, OkHttpClient clientB) {
    assertThat(clientA.getDispatcher()).isEqualTo(clientB.getDispatcher());
    assertThat(clientA.getProxy()).isEqualTo(clientB.getProxy());
    assertThat(clientA.getProtocols()).isEqualTo(clientB.getProtocols());
    assertThat(clientA.getConnectionSpecs()).isEqualTo(clientB.getConnectionSpecs());
    assertThat(clientA.getProxySelector()).isEqualTo(clientB.getProxySelector());
    assertThat(clientA.getCookieHandler()).isEqualTo(clientB.getCookieHandler());
    assertThat(clientA.getCache()).isEqualTo(clientB.getCache());
    assertThat(clientA.getSocketFactory()).isEqualTo(clientB.getSocketFactory());
    assertThat(clientA.getSslSocketFactory()).isEqualTo(clientB.getSslSocketFactory());
    assertThat(clientA.getHostnameVerifier()).isEqualTo(clientB.getHostnameVerifier());
    assertThat(clientA.getCertificatePinner()).isEqualTo(clientB.getCertificatePinner());
    assertThat(clientA.getAuthenticator()).isEqualTo(clientB.getAuthenticator());
    assertThat(clientA.getConnectionPool()).isEqualTo(clientB.getConnectionPool());
    assertThat(clientA.getFollowSslRedirects()).isEqualTo(clientB.getFollowSslRedirects());
    assertThat(clientA.getFollowRedirects()).isEqualTo(clientB.getFollowRedirects());
    assertThat(clientA.getRetryOnConnectionFailure()).isEqualTo(clientB.getRetryOnConnectionFailure());
    assertThat(clientA.getConnectTimeout()).isEqualTo(clientB.getConnectTimeout());
    assertThat(clientA.getReadTimeout()).isEqualTo(clientB.getReadTimeout());
    assertThat(clientA.getWriteTimeout()).isEqualTo(clientB.getWriteTimeout());
  }
}
