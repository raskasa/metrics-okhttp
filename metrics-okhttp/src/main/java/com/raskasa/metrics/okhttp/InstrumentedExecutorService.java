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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An {@link ExecutorService} that monitors the number of network requests
 * submitted, running, completed and also keeps a {@link Timer} for the request
 * duration.
 */
public final class InstrumentedExecutorService implements ExecutorService {
  private final ExecutorService delegate;
  private final Meter submitted;
  private final Counter running;
  private final Meter completed;
  private final Timer duration;

  public InstrumentedExecutorService(ExecutorService delegate, MetricRegistry registry, String name) {
    this.delegate = delegate;
    this.submitted = registry.meter(MetricRegistry.name(name, "network-requests-submitted"));
    this.running = registry.counter(MetricRegistry.name(name, "network-requests-running"));
    this.completed = registry.meter(MetricRegistry.name(name, "network-requests-completed"));
    this.duration = registry.timer(MetricRegistry.name(name, "network-requests-duration"));
  }

  @Override public void execute(Runnable runnable) {
    submitted.mark();
    delegate.execute(new InstrumentedRunnable(runnable));
  }

  @Override public Future<?> submit(Runnable runnable) {
    submitted.mark();
    return delegate.submit(new InstrumentedRunnable(runnable));
  }

  @Override public <T> Future<T> submit(Runnable runnable, T result) {
    submitted.mark();
    return delegate.submit(new InstrumentedRunnable(runnable), result);
  }

  @Override public <T> Future<T> submit(Callable<T> task) {
    submitted.mark();
    return delegate.submit(new InstrumentedCallable<T>(task));
  }

  @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    submitted.mark(tasks.size());
    Collection<? extends Callable<T>> instrumented = instrument(tasks);
    return delegate.invokeAll(instrumented);
  }

  @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
    submitted.mark(tasks.size());
    Collection<? extends Callable<T>> instrumented = instrument(tasks);
    return delegate.invokeAll(instrumented, timeout, unit);
  }

  @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
    submitted.mark(tasks.size());
    Collection<? extends Callable<T>> instrumented = instrument(tasks);
    return delegate.invokeAny(instrumented);
  }

  @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException,
      TimeoutException {
    submitted.mark(tasks.size());
    Collection<? extends Callable<T>> instrumented = instrument(tasks);
    return delegate.invokeAny(instrumented, timeout, unit);
  }

  private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
    final List<InstrumentedCallable<T>> instrumented = new ArrayList<InstrumentedCallable<T>>(tasks.size());
    for (Callable<T> task : tasks) {
      instrumented.add(new InstrumentedCallable<T>(task));
    }
    return instrumented;
  }

  @Override public void shutdown() {
    delegate.shutdown();
  }

  @Override public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
    return delegate.awaitTermination(l, timeUnit);
  }

  private final class InstrumentedRunnable implements Runnable {
    private final Runnable task;

    InstrumentedRunnable(Runnable task) {
      this.task = task;
    }

    @Override public void run() {
      running.inc();
      final Timer.Context context = duration.time();
      try {
        task.run();
      } finally {
        context.stop();
        running.dec();
        completed.mark();
      }
    }
  }

  private final class InstrumentedCallable<T> implements Callable<T> {
    private final Callable<T> callable;

    InstrumentedCallable(Callable<T> callable) {
      this.callable = callable;
    }

    @Override public T call() throws Exception {
      running.inc();
      final Timer.Context context = duration.time();
      try {
        return callable.call();
      } finally {
        context.stop();
        running.dec();
        completed.mark();
      }
    }
  }
}
