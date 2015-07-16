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
import org.junit.Test;

import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.mockito.Mockito.mock;

// TODO: Validate that when an OkHttpClient is cloned, the clone's usage is tracked as well.

public final class InstrumentedOkHttpClientTest {
  @Test public void equality() throws Exception {
    MetricRegistry registry = mock(MetricRegistry.class);
    OkHttpClient client = new OkHttpClient();
    InstrumentedOkHttpClient iClientA = new InstrumentedOkHttpClient(registry, client);
    InstrumentedOkHttpClient iClientB = new InstrumentedOkHttpClient(registry, client);

    assertThat(iClientA).isEqualTo(iClientB);
  }

  @Test public void stringRepresentation() throws Exception {
    MetricRegistry registry = mock(MetricRegistry.class);
    OkHttpClient client = new OkHttpClient();
    InstrumentedOkHttpClient iClient = new InstrumentedOkHttpClient(registry, client);

    assertThat(client.toString()).isEqualTo(iClient.toString());
  }
}
