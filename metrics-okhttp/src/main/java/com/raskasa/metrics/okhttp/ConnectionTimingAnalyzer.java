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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import java.net.InetSocketAddress;
import java.net.Proxy;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.Protocol;

/** An {@link okhttp3.EventListener} that measures connection setup times. */
final class ConnectionTimingAnalyzer extends EventListener {

  private final Histogram setupTimes;
  private long startTime = 0;

  ConnectionTimingAnalyzer(final MetricRegistry registry, final String name) {
    this.setupTimes = registry.histogram(MetricRegistry.name(name, "connection-setup"));
  }

  @Override
  public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
    startTime = System.nanoTime();
  }

  @Override
  public void connectEnd(
      Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
    setupTimes.update(System.nanoTime() - startTime);
  }
}
