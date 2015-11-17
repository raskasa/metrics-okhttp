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

import static java.util.concurrent.TimeUnit.SECONDS;

/** Factor methods for creating an instrumented {@code OkHttpClient}. */
public final class InstrumentedOkHttpClients {

  /** Create an instrumented {@code OkHttpClient}. */
  public static OkHttpClient create(MetricRegistry registry) {
    final OkHttpClient client = new OkHttpClient();
    client.setConnectTimeout(10, SECONDS);
    client.setReadTimeout(10, SECONDS);
    client.setWriteTimeout(10, SECONDS);
    return new InstrumentedOkHttpClient(registry, "default-client", client);
  }

  /**
   * Create an instrumented {@code OkHttpClient}, using the given client.
   *
   * <p>{@code name} provides an identifier for the instrumented client.  This
   * is useful in situations where you have more than one instrumented client
   * in your application.</p>
   */
  public static OkHttpClient create(MetricRegistry registry, String name, OkHttpClient client) {
    return new InstrumentedOkHttpClient(registry, name, client);
  }

  private InstrumentedOkHttpClients() {
    // No instances.
  }
}
