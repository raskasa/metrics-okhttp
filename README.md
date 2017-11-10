Metrics Integration for OkHttp
==============================

An [OkHttp][okhttp] HTTP client wrapper providing [Metrics][metrics]
instrumentation of connection pools, request durations and rates, and other
useful information.

Usage
-----

Metrics Integration for OkHttp provides `InstrumentedOkHttpClients`, a static
factory class for instrumenting OkHttp HTTP clients.

You can create an instrumented `OkHttpClient` by doing the following:

```java
MetricRegistry registry = ...;
OkHttpClient client = InstrumentedOkHttpClients.create(registry);
```

If you wish to provide your own `OkHttpClient` instance, you can do that as well:

```java
MetricRegistry registry = ...;
OkHttpClient rawClient = ...;
OkHttpClient client = InstrumentedOkHttpClients.create(registry, rawClient);
```

If you use more than one OkHttpClient instance in your application, you may
want to provide a custom name when instrumenting the clients in order to
properly distinguish them:

```java
MetricRegistry registry = ...;
OkHttpClient client = InstrumentedOkHttpClients.create(registry, "custom-name");

MetricRegistry registry = ...;
OkHttpClient rawClient = ...;
OkHttpClient client = InstrumentedOkHttpClients.create(registry, rawClient, "custom-name");
```

An instrumented OkHttp HTTP client provides the following metrics:

```
okhttp3.OkHttpClient.cache-request-count
okhttp3.OkHttpClient.cache-hit-count
okhttp3.OkHttpClient.cache-network-count
okhttp3.OkHttpClient.cache-current-size
okhttp3.OkHttpClient.cache-max-size
okhttp3.OkHttpClient.cache-size
okhttp3.OkHttpClient.cache-write-success-count
okhttp3.OkHttpClient.cache-write-abort-count
okhttp3.OkHttpClient.connection-pool-count
okhttp3.OkHttpClient.connection-pool-count-http
okhttp3.OkHttpClient.connection-pool-count-multiplexed
okhttp3.OkHttpClient.network-requests-completed
okhttp3.OkHttpClient.network-requests-duration
okhttp3.OkHttpClient.network-requests-running
okhttp3.OkHttpClient.network-requests-submitted
```

If you provide a custom name for the instrumented client (i.e. `custom-name`),
the metrics will have the following format:

```
...
okhttp3.OkHttpClient.custom-name.cache-write-success-count
okhttp3.OkHttpClient.custom-name.cache-write-abort-count
okhttp3.OkHttpClient.custom-name.connection-pool-count
...
```

Download
--------

**Metrics Integration for OkHttp is currently under development.**  The API is
not stable and neither is the feature set.

Download [the latest jar][metrics-okhttp] or depend on Maven:

```xml
<dependency>
  <groupId>com.raskasa.metrics</groupId>
  <artifactId>metrics-okhttp</artifactId>
  <version>0.3.0</version>
</dependency>
```

or Gradle:

```groovy
compile 'com.raskasa.metrics:metrics-okhttp:0.3.0'
```

Snapshots of the development version are available in
[Sonatype's `snapshots` repository][sonatype].

License
-------

    Copyright 2015 Ras Kasa Williams

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
  
  [metrics]: https://dropwizard.github.io/metrics/3.2.3/
  [metrics-okhttp]: https://search.maven.org/remote_content?g=com.raskasa.metrics&a=metrics-okhttp&v=LATEST
  [okhttp]: http://square.github.io/okhttp/
  [sonatype]: https://oss.sonatype.org/content/repositories/snapshots/com/raskasa/metrics/
