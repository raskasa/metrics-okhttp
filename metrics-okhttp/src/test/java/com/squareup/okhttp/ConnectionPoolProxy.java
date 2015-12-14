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
package com.squareup.okhttp;

import java.net.Socket;

/**
 * Proxy to open up the
 * {@link ConnectionPool#setCleanupRunnableForTest(java.lang.Runnable)} for
 * unit testing purposes.
 */
public final class ConnectionPoolProxy implements Connection {
  public ConnectionPoolProxy(ConnectionPool pool, Runnable runnable) {
    pool.setCleanupRunnableForTest(runnable);
  }

  @Override public Route getRoute() {
    throw new UnsupportedOperationException();
  }

  @Override public Socket getSocket() {
    throw new UnsupportedOperationException();
  }

  @Override public Handshake getHandshake() {
    throw new UnsupportedOperationException();
  }

  @Override public Protocol getProtocol() {
    throw new UnsupportedOperationException();
  }
}
