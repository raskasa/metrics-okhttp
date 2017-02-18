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
import okhttp3.OkHttpClient;
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
    assertThat(client.connectTimeoutMillis()).isEqualTo(10_000);
    assertThat(client.readTimeoutMillis()).isEqualTo(10_000);
    assertThat(client.writeTimeoutMillis()).isEqualTo(10_000);
  }

  @Test public void instrumentAndNameDefaultClient() {
    OkHttpClient client = InstrumentedOkHttpClients.create(registry, "custom");

    // The connection, read, and write timeouts are the only configurations applied by default.
    assertThat(client.connectTimeoutMillis()).isEqualTo(10_000);
    assertThat(client.readTimeoutMillis()).isEqualTo(10_000);
    assertThat(client.writeTimeoutMillis()).isEqualTo(10_000);
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
    assertThat(clientA.authenticator()).isEqualTo(clientB.authenticator());
    assertThat(clientA.cache()).isEqualTo(clientB.cache());
    assertThat(clientA.certificatePinner()).isEqualTo(clientB.certificatePinner());
    assertThat(clientA.connectionPool()).isEqualTo(clientB.connectionPool());
    assertThat(clientA.connectionSpecs()).isEqualTo(clientB.connectionSpecs());
    assertThat(clientA.connectTimeoutMillis()).isEqualTo(clientB.connectTimeoutMillis());
    assertThat(clientA.cookieJar()).isEqualTo(clientB.cookieJar());
    assertThat(clientA.followRedirects()).isEqualTo(clientB.followRedirects());
    assertThat(clientA.followSslRedirects()).isEqualTo(clientB.followSslRedirects());
    assertThat(clientA.hostnameVerifier()).isEqualTo(clientB.hostnameVerifier());
    assertThat(clientA.protocols()).isEqualTo(clientB.protocols());
    assertThat(clientA.proxy()).isEqualTo(clientB.proxy());
    assertThat(clientA.proxySelector()).isEqualTo(clientB.proxySelector());
    assertThat(clientA.readTimeoutMillis()).isEqualTo(clientB.readTimeoutMillis());
    assertThat(clientA.retryOnConnectionFailure()).isEqualTo(clientB.retryOnConnectionFailure());
    assertThat(clientA.socketFactory()).isEqualTo(clientB.socketFactory());
    assertThat(clientA.sslSocketFactory()).isEqualTo(clientB.sslSocketFactory());
    assertThat(clientA.writeTimeoutMillis()).isEqualTo(clientB.writeTimeoutMillis());
  }
}
