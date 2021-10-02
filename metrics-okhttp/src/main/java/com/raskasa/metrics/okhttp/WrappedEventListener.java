package com.raskasa.metrics.okhttp;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

/**
 * This listener wraps multiple other listeners. This is required since okhttp only allows one listener to be attached
 * by default and we wouldn't want metric listener to override user provided listener of the raw client
 */
final class WrappedEventListener extends EventListener {

    private final List<EventListener> eventListeners;

    public WrappedEventListener(final List<EventListener> eventListeners) {
        this.eventListeners = eventListeners;
    }

    @Override
    public void callStart(Call call) {
        eventListeners.forEach(eventListener -> eventListener.callStart(call));
    }

    @Override
    public void dnsStart(Call call,
                         String domainName) {
        eventListeners.forEach(eventListener -> eventListener.dnsStart(call, domainName));
    }

    @Override
    public void dnsEnd(Call call,
                       String domainName,
                       List<InetAddress> inetAddressList) {
        eventListeners.forEach(eventListener -> eventListener.dnsEnd(call, domainName, inetAddressList));
    }

    @Override
    public void connectStart(Call call,
                             InetSocketAddress inetSocketAddress,
                             Proxy proxy) {
        eventListeners.forEach(eventListener -> eventListener.connectStart(call, inetSocketAddress, proxy));
    }

    @Override
    public void secureConnectStart(Call call) {
        eventListeners.forEach(eventListener -> eventListener.secureConnectStart(call));
    }

    @Override
    public void secureConnectEnd(Call call,
                                 Handshake handshake) {
        eventListeners.forEach(eventListener -> eventListener.secureConnectEnd(call, handshake));
    }

    @Override
    public void connectEnd(Call call,
                           InetSocketAddress inetSocketAddress,
                           Proxy proxy,
                           Protocol protocol) {
        eventListeners.forEach(eventListener -> eventListener.connectEnd(call, inetSocketAddress, proxy, protocol));
    }

    @Override
    public void connectFailed(Call call,
                              InetSocketAddress inetSocketAddress,
                              Proxy proxy,
                              Protocol protocol,
                              IOException ioe) {
        eventListeners.forEach(eventListener -> eventListener.connectFailed(call, inetSocketAddress, proxy, protocol,
                ioe));
    }

    @Override
    public void connectionAcquired(Call call,
                                   Connection connection) {
        eventListeners.forEach(eventListener -> eventListener.connectionAcquired(call, connection));
    }

    @Override
    public void connectionReleased(Call call,
                                   Connection connection) {
        eventListeners.forEach(eventListener -> eventListener.connectionReleased(call, connection));
    }

    @Override
    public void requestHeadersStart(Call call) {
        eventListeners.forEach(eventListener -> eventListener.requestHeadersStart(call));
    }

    @Override
    public void requestHeadersEnd(Call call,
                                  Request request) {
        eventListeners.forEach(eventListener -> eventListener.requestHeadersEnd(call, request));
    }

    @Override
    public void requestBodyStart(Call call) {
        eventListeners.forEach(eventListener -> eventListener.requestBodyStart(call));
    }

    @Override
    public void requestBodyEnd(Call call,
                               long byteCount) {
        eventListeners.forEach(eventListener -> eventListener.requestBodyEnd(call, byteCount));
    }

    @Override
    public void responseHeadersStart(Call call) {
        eventListeners.forEach(eventListener -> eventListener.responseHeadersStart(call));
    }

    @Override
    public void responseHeadersEnd(Call call,
                                   Response response) {
        eventListeners.forEach(eventListener -> eventListener.responseHeadersEnd(call, response));
    }

    @Override
    public void responseBodyStart(Call call) {
        eventListeners.forEach(eventListener -> eventListener.responseBodyStart(call));
    }

    @Override
    public void responseBodyEnd(Call call,
                                long byteCount) {
        eventListeners.forEach(eventListener -> eventListener.responseBodyEnd(call, byteCount));
    }

    @Override
    public void callEnd(Call call) {
        eventListeners.forEach(eventListener -> eventListener.callEnd(call));
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        eventListeners.forEach(eventListener -> eventListener.callFailed(call, ioe));
    }
}
