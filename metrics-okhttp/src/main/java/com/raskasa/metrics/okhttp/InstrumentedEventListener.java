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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

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

  private final Meter callStart;
  private final Meter callEnd;
  private final Meter callFailed;
  private final Timer callDuration;
  private Timer.Context callDurationContext;

  private final Meter dnsStart;
  private final Meter dnsEnd;
  private final Timer dnsDuration;
  private Timer.Context dnsDurationContext;

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

    this.callStart = registry.meter(MetricRegistry.name(name, "calls-start"));
    this.callEnd = registry.meter(MetricRegistry.name(name, "calls-end"));
    this.callFailed = registry.meter(MetricRegistry.name(name, "calls-failed"));
    this.callDuration = registry.timer(MetricRegistry.name(name, "calls-duration"));

    this.dnsStart = registry.meter(MetricRegistry.name(name, "dns-start"));
    this.dnsEnd = registry.meter(MetricRegistry.name(name, "dns-end"));
    this.dnsDuration = registry.timer(MetricRegistry.name(name, "dns-duration"));

    this.connectionStart = registry.meter(MetricRegistry.name(name, "connections-start"));
    this.connectionEnd = registry.meter(MetricRegistry.name(name, "connections-end"));
    this.connectionFailed = registry.meter(MetricRegistry.name(name, "connections-failed"));
    this.connectionDuration = registry.timer(MetricRegistry.name(name, "connections-duration"));
    this.connectionAcquired = registry.meter(MetricRegistry.name(name, "connections-acquired"));
    this.connectionReleased = registry.meter(MetricRegistry.name(name, "connections-released"));
  }

  @Override
  public void callStart(@Nonnull Call call) {
    this.callStart.mark();
    this.callDurationContext = this.callDuration.time();
    this.delegate.callStart(call);
  }

  @Override
  public void dnsStart(@Nonnull Call call, @Nonnull String domainName) {
    this.dnsStart.mark();
    this.dnsDurationContext = this.dnsDuration.time();
    this.delegate.dnsStart(call, domainName);
  }

  @Override
  public void dnsEnd(
      @Nonnull Call call, @Nonnull String domainName, @Nonnull List<InetAddress> inetAddressList) {
    this.dnsDurationContext.stop();
    this.dnsEnd.mark();
    this.delegate.dnsEnd(call, domainName, inetAddressList);
  }

  @Override
  public void connectStart(
      @Nonnull Call call, @Nonnull InetSocketAddress inetSocketAddress, @Nonnull Proxy proxy) {
    this.connectionStart.mark();
    this.connectionDurationContext = this.connectionDuration.time();
    this.delegate.connectStart(call, inetSocketAddress, proxy);
  }

  @Override
  public void secureConnectStart(@Nonnull Call call) {
    this.delegate.secureConnectStart(call);
  }

  @Override
  public void secureConnectEnd(@Nonnull Call call, @Nullable Handshake handshake) {
    this.delegate.secureConnectEnd(call, handshake);
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
  public void requestHeadersStart(@Nonnull Call call) {
    this.delegate.requestHeadersStart(call);
  }

  @Override
  public void requestHeadersEnd(@Nonnull Call call, @Nonnull Request request) {
    this.delegate.requestHeadersEnd(call, request);
  }

  @Override
  public void requestBodyStart(@Nonnull Call call) {
    this.delegate.requestBodyStart(call);
  }

  @Override
  public void requestBodyEnd(@Nonnull Call call, long byteCount) {
    this.delegate.requestBodyEnd(call, byteCount);
  }

  @Override
  public void requestFailed(@Nonnull Call call, @Nonnull IOException ioe) {
    this.delegate.requestFailed(call, ioe);
  }

  @Override
  public void responseHeadersStart(@Nonnull Call call) {
    this.delegate.responseHeadersStart(call);
  }

  @Override
  public void responseHeadersEnd(@Nonnull Call call, @Nonnull Response response) {
    this.delegate.responseHeadersEnd(call, response);
  }

  @Override
  public void responseBodyStart(@Nonnull Call call) {
    this.delegate.responseBodyStart(call);
  }

  @Override
  public void responseBodyEnd(@Nonnull Call call, long byteCount) {
    this.delegate.responseBodyEnd(call, byteCount);
  }

  @Override
  public void responseFailed(@Nonnull Call call, @Nonnull IOException ioe) {
    this.delegate.responseFailed(call, ioe);
  }

  @Override
  public void callEnd(@Nonnull Call call) {
    this.callDurationContext.stop();
    this.callEnd.mark();
    this.delegate.callEnd(call);
  }

  @Override
  public void callFailed(@Nonnull Call call, @Nonnull IOException ioe) {
    this.callDurationContext.stop();
    this.callFailed.mark();
    this.delegate.callFailed(call, ioe);
  }
}
