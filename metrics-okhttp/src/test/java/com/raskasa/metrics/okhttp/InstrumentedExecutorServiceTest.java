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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class InstrumentedExecutorServiceTest {
  @Test public void reportsTasksInformation() throws Exception {
    MetricRegistry registry = new MetricRegistry();
    ExecutorService executor = MoreExecutors.newDirectExecutorService();
    final InstrumentedExecutorService instrumentedExecutorService =
        new InstrumentedExecutorService(executor, registry, "xs");
    final Meter submitted = registry.meter("xs.network-requests-submitted");
    final Counter running = registry.counter("xs.network-requests-running");
    final Meter completed = registry.meter("xs.network-requests-completed");
    final Timer duration = registry.timer("xs.network-requests-duration");

    assertThat(submitted.getCount()).isZero();
    assertThat(running.getCount()).isZero();
    assertThat(completed.getCount()).isZero();
    assertThat(duration.getCount()).isZero();

    Future<?> theFuture = instrumentedExecutorService.submit(new Runnable() {
      @Override public void run() {
        assertThat(submitted.getCount()).isEqualTo(1);
        assertThat(running.getCount()).isEqualTo(1);
        assertThat(completed.getCount()).isZero();
        assertThat(duration.getCount()).isZero();
      }
    });

    theFuture.get();

    assertThat(submitted.getCount()).isEqualTo(1);
    assertThat(running.getCount()).isZero();
    assertThat(completed.getCount()).isEqualTo(1);
    assertThat(duration.getCount()).isEqualTo(1);
    assertThat(duration.getSnapshot().size()).isEqualTo(1);
  }
}
