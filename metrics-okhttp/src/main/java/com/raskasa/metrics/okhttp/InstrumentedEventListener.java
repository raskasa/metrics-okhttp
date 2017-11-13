package com.raskasa.metrics.okhttp;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;

/**
 * An {@link EventListener} that monitors the quantity, size, and duration of
 * of HTTP calls.
 */
final class InstrumentedEventListener extends EventListener {
  private final Timer dnsDuration;
  private final Timer connectionDuration;
  private final Timer tlsConnectionDuration;
  private final Map<Call, Timer.Context> dnsMap = new HashMap<>();
  private final Map<Call, Timer.Context> connectionMap = new HashMap<>();
  private final Map<Call, Timer.Context> tlsConnectionMap = new HashMap<>();

  InstrumentedEventListener(MetricRegistry registry, String name) {
    this.dnsDuration = registry.timer(MetricRegistry.name(name, "dns-duration"));
    this.connectionDuration = registry.timer(MetricRegistry.name(name, "connection-duration-all"));
    this.tlsConnectionDuration = registry.timer(MetricRegistry.name(name, "connection-duration-secured"));
  }

  @Override public void dnsStart(Call call, String domainName) {
    super.dnsStart(call, domainName);
    dnsMap.put(call, dnsDuration.time());
  }

  @Override public void dnsEnd(Call call, String domainName, @Nullable List<InetAddress> inetAddressList) {
    super.dnsEnd(call, domainName, inetAddressList);
    dnsMap.get(call).stop();
    dnsMap.remove(call);
  }

  @Override public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
    super.connectStart(call, inetSocketAddress, proxy);
    connectionMap.put(call, connectionDuration.time());
  }

  @Override public void secureConnectStart(Call call) {
    super.secureConnectStart(call);
    tlsConnectionMap.put(call, tlsConnectionDuration.time());
  }

  @Override public void secureConnectEnd(Call call, @Nullable Handshake handshake) {
    super.secureConnectEnd(call, handshake);
    tlsConnectionMap.get(call).stop();
    tlsConnectionMap.remove(call);
  }

  @Override public void connectEnd(Call call, InetSocketAddress inetSocketAddress, @Nullable Proxy proxy, @Nullable Protocol protocol) {
    super.connectEnd(call, inetSocketAddress, proxy, protocol);
    connectionMap.get(call).stop();
    connectionMap.remove(call);
  }

  @Override public void connectFailed(Call call, InetSocketAddress inetSocketAddress, @Nullable Proxy proxy, @Nullable Protocol protocol, @Nullable IOException ioe) {
    super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
    connectionMap.get(call).stop();
    connectionMap.remove(call);
  }
}
