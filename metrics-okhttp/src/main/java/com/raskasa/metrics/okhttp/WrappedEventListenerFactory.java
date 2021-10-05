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

import java.util.List;
import java.util.stream.Collectors;
import okhttp3.Call;
import okhttp3.EventListener;

/**
 * okhttp supports exactly one {@link EventListener.Factory} to be attached. In order to support
 * multiple listeners, this class wraps multiple {@link EventListener.Factory} instances and
 * combines them to create {@link WrappedEventListener}
 */
final class WrappedEventListenerFactory implements EventListener.Factory {

  private final List<EventListener.Factory> factories;

  public WrappedEventListenerFactory(final List<EventListener.Factory> factories) {
    this.factories = factories;
  }

  @Override
  public EventListener create(final Call call) {
    List<EventListener> listeners =
        factories.stream().map(factory -> factory.create(call)).collect(Collectors.toList());
    return new WrappedEventListener(listeners);
  }
}
