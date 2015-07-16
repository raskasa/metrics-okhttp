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

public final class InstrumentedOkHttpClient extends OkHttpClient {
  private final MetricRegistry registry;
  private final OkHttpClient client;

  public InstrumentedOkHttpClient(MetricRegistry registry, OkHttpClient client) {
    this.registry = registry;
    this.client = client;
  }

  @Override public boolean equals(Object obj) {
    return
        obj instanceof InstrumentedOkHttpClient
        && client.equals(((InstrumentedOkHttpClient) obj).client);
  }

  @Override public String toString() {
    return client.toString();
  }
}
