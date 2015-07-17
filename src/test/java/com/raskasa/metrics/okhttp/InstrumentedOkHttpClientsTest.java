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
    OkHttpClient originalClient = new OkHttpClient();
    OkHttpClient instrumentedClient = InstrumentedOkHttpClients.create(registry, originalClient);

    assertThat(instrumentedClient.getDispatcher()).isEqualTo(originalClient.getDispatcher());
    assertThat(instrumentedClient.getProxy()).isEqualTo(originalClient.getProxy());
    assertThat(instrumentedClient.getProtocols()).isEqualTo(originalClient.getProtocols());
    assertThat(instrumentedClient.getConnectionSpecs()).isEqualTo(originalClient.getConnectionSpecs());
    assertThat(instrumentedClient.getProxySelector()).isEqualTo(originalClient.getProxySelector());
    assertThat(instrumentedClient.getCookieHandler()).isEqualTo(originalClient.getCookieHandler());
    assertThat(instrumentedClient.getCache()).isEqualTo(originalClient.getCache());
    assertThat(instrumentedClient.getSocketFactory()).isEqualTo(originalClient.getSocketFactory());
    assertThat(instrumentedClient.getSslSocketFactory()).isEqualTo(originalClient.getSslSocketFactory());
    assertThat(instrumentedClient.getHostnameVerifier()).isEqualTo(originalClient.getHostnameVerifier());
    assertThat(instrumentedClient.getCertificatePinner()).isEqualTo(originalClient.getCertificatePinner());
    assertThat(instrumentedClient.getAuthenticator()).isEqualTo(originalClient.getAuthenticator());
    assertThat(instrumentedClient.getConnectionPool()).isEqualTo(originalClient.getConnectionPool());
    assertThat(instrumentedClient.getFollowSslRedirects()).isEqualTo(originalClient.getFollowSslRedirects());
    assertThat(instrumentedClient.getFollowRedirects()).isEqualTo(originalClient.getFollowRedirects());
    assertThat(instrumentedClient.getRetryOnConnectionFailure()).isEqualTo(originalClient.getRetryOnConnectionFailure());
    assertThat(instrumentedClient.getConnectTimeout()).isEqualTo(originalClient.getConnectTimeout());
    assertThat(instrumentedClient.getReadTimeout()).isEqualTo(originalClient.getReadTimeout());
    assertThat(instrumentedClient.getWriteTimeout()).isEqualTo(originalClient.getWriteTimeout());
  }
}
