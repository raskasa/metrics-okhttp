package com.raskasa.metrics.okhttp;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Protocol;

/**
 * A client-scoped {@link EventListener} that records metrics around quantity, size, and duration of
 * HTTP calls using Dropwizard Metrics.
 *
 * <p>This listener will receive ALL analytics events for {@link okhttp3.OkHttpClient the given
 * instrumented client}.
 *
 * <p>This listener WILL NOT override a user-provided listener. It will ensure the user-provided
 * listener receives ALL analytics events as expected. Users usually configure a {@link
 * EventListener listener} via {@link okhttp3.OkHttpClient.Builder#eventListener(EventListener)} or
 * {@link okhttp3.OkHttpClient.Builder#eventListenerFactory(EventListener.Factory)}).
 *
 * @see EventListener for semantics and restrictions on listener implementations.
 */
final class InstrumentedEventListener extends EventListener {
  static final class Factory implements EventListener.Factory {
    private final MetricRegistry registry;
    private final EventListener.Factory delegate;
    private final String name;

    Factory(
        @Nonnull MetricRegistry registry,
        @Nonnull EventListener.Factory delegate,
        @Nullable String name) {
      this.registry = registry;
      this.delegate = delegate;
      this.name = name;
    }

    @Nonnull
    @Override
    public EventListener create(@Nonnull Call call) {
      return new InstrumentedEventListener(this.registry, this.delegate.create(call), this.name);
    }
  }

  /**
   * The user-provided {@link EventListener listener}.
   *
   * <p>If a user doesn't configure a listener, this will be {@link EventListener#NONE the default
   * listener}.
   */
  private final EventListener delegate;

  private final Timer callDuration;
  private Timer.Context callDurationContext;

  private final Meter connectionStart;
  private final Meter connectionEnd;
  private final Meter connectionFailed;
  private final Timer connectionDuration;
  private Timer.Context connectionDurationContext;
  private final Meter connectionAcquired;
  private final Meter connectionReleased;

  InstrumentedEventListener(
      @Nonnull MetricRegistry registry, @Nonnull EventListener delegate, @Nullable String name) {
    this.delegate = delegate;

    this.callDuration = registry.timer(MetricRegistry.name(name, "calls-duration"));

    this.connectionStart = registry.meter(MetricRegistry.name(name, "connections-start"));
    this.connectionEnd = registry.meter(MetricRegistry.name(name, "connections-end"));
    this.connectionFailed = registry.meter(MetricRegistry.name(name, "connections-failed"));
    this.connectionDuration = registry.timer(MetricRegistry.name(name, "connections-duration"));
    this.connectionAcquired = registry.meter(MetricRegistry.name(name, "connections-acquired"));
    this.connectionReleased = registry.meter(MetricRegistry.name(name, "connections-released"));
  }

  @Override
  public void callStart(@Nonnull Call call) {
    this.callDurationContext = this.callDuration.time();
    this.delegate.callStart(call);
  }

  @Override
  public void connectStart(
      @Nonnull Call call, @Nonnull InetSocketAddress inetSocketAddress, @Nonnull Proxy proxy) {
    this.connectionStart.mark();
    this.connectionDurationContext = this.connectionDuration.time();
    this.delegate.connectStart(call, inetSocketAddress, proxy);
  }

  @Override
  public void connectEnd(
      @Nonnull Call call,
      @Nonnull InetSocketAddress inetSocketAddress,
      @Nonnull Proxy proxy,
      @Nullable Protocol protocol) {
    this.connectionDurationContext.stop();
    this.connectionEnd.mark();
    this.delegate.connectEnd(call, inetSocketAddress, proxy, protocol);
  }

  @Override
  public void connectFailed(
      @Nonnull Call call,
      @Nonnull InetSocketAddress inetSocketAddress,
      @Nonnull Proxy proxy,
      @Nullable Protocol protocol,
      @Nonnull IOException ioe) {
    this.connectionDurationContext.stop();
    this.connectionFailed.mark();
    this.delegate.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
  }

  @Override
  public void connectionAcquired(@Nonnull Call call, @Nonnull Connection connection) {
    this.connectionAcquired.mark();
    this.delegate.connectionAcquired(call, connection);
  }

  @Override
  public void connectionReleased(@Nonnull Call call, @Nonnull Connection connection) {
    this.connectionReleased.mark();
    this.delegate.connectionReleased(call, connection);
  }

  @Override
  public void callEnd(@Nonnull Call call) {
    this.callDurationContext.stop();
    this.delegate.callEnd(call);
  }

  @Override
  public void callFailed(@Nonnull Call call, @Nonnull IOException ioe) {
    this.callDurationContext.stop();
    this.delegate.callFailed(call, ioe);
  }
}
