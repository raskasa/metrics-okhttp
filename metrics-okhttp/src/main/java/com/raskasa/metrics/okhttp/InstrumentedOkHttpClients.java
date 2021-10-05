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

import static com.raskasa.metrics.okhttp.InstrumentedOkHttpClient.InstrumentedOkHttpClientBuilder;

import com.codahale.metrics.MetricRegistry;
import okhttp3.OkHttpClient;

/** Static factory methods for instrumenting an {@link OkHttpClient}. */
public final class InstrumentedOkHttpClients {

  /** Create and instrument an {@link OkHttpClient}. */
  public static OkHttpClient create(MetricRegistry registry) {
    return InstrumentedOkHttpClientBuilder.newBuilder(registry, null).build();
  }

  /** Instrument the given {@link OkHttpClient}. */
  public static OkHttpClient create(MetricRegistry registry, OkHttpClient client) {
    return InstrumentedOkHttpClientBuilder.newBuilder(registry, null).withClient(client).build();
  }

  /**
   * Create and instrument an {@link OkHttpClient} and give it the provided {@code name}.
   *
   * <p>{@code name} provides an identifier for the instrumented client. This is useful in
   * situations where you have more than one instrumented client in your application.
   */
  public static OkHttpClient create(MetricRegistry registry, String name) {
    return InstrumentedOkHttpClientBuilder.newBuilder(registry, name).build();
  }

  /**
   * Instrument the given {@link OkHttpClient} and give it the provided name.
   *
   * <p>{@code name} provides an identifier for the instrumented client. This is useful in
   * situations where you have more than one instrumented client in your application.
   */
  public static OkHttpClient create(MetricRegistry registry, OkHttpClient client, String name) {
    return InstrumentedOkHttpClientBuilder.newBuilder(registry, name).withClient(client).build();
  }

  private InstrumentedOkHttpClients() {
    // No instances.
  }
}
