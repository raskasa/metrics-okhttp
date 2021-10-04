package com.raskasa.metrics.okhttp;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * An {@link Interceptor} that monitors the number of submitted, running, and completed network
 * requests. Also, keeps a {@link Timer} for the request duration.
 */
final class InstrumentedInterceptor implements Interceptor {
  private final Meter submitted;
  private final Counter running;
  private final Meter completed;
  private final Timer duration;

  InstrumentedInterceptor(MetricRegistry registry, String name) {
    this.submitted = registry.meter(MetricRegistry.name(name, "network-requests-submitted"));
    this.running = registry.counter(MetricRegistry.name(name, "network-requests-running"));
    this.completed = registry.meter(MetricRegistry.name(name, "network-requests-completed"));
    this.duration = registry.timer(MetricRegistry.name(name, "network-requests-duration"));
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    submitted.mark();
    running.inc();
    final Timer.Context context = duration.time();
    try {
      return chain.proceed(chain.request());
    } finally {
      context.stop();
      running.dec();
      completed.mark();
    }
  }
}
