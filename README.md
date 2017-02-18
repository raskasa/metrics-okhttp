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
com.squareup.okhttp.OkHttpClient.cache-request-count
com.squareup.okhttp.OkHttpClient.cache-hit-count
com.squareup.okhttp.OkHttpClient.cache-network-count
com.squareup.okhttp.OkHttpClient.cache-current-size
com.squareup.okhttp.OkHttpClient.cache-max-size
com.squareup.okhttp.OkHttpClient.cache-size
com.squareup.okhttp.OkHttpClient.cache-write-success-count
com.squareup.okhttp.OkHttpClient.cache-write-abort-count
com.squareup.okhttp.OkHttpClient.connection-pool-count
com.squareup.okhttp.OkHttpClient.connection-pool-count-http
com.squareup.okhttp.OkHttpClient.connection-pool-count-multiplexed
com.squareup.okhttp.OkHttpClient.network-requests-completed
com.squareup.okhttp.OkHttpClient.network-requests-duration
com.squareup.okhttp.OkHttpClient.network-requests-running
com.squareup.okhttp.OkHttpClient.network-requests-submitted
```

If you provide a custom name for the instrumented client (i.e. `custom-name`),
the metrics will have the following format:

```
...
com.squareup.okhttp.OkHttpClient.custom-name.cache-write-success-count
com.squareup.okhttp.OkHttpClient.custom-name.cache-write-abort-count
com.squareup.okhttp.OkHttpClient.custom-name.connection-pool-count
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
  <version>0.2.0</version>
</dependency>
```

or Gradle:

```groovy
compile 'com.raskasa.metrics:metrics-okhttp:0.2.0'
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
  
  [metrics]: https://dropwizard.github.io/metrics/3.1.0/
  [metrics-okhttp]: https://search.maven.org/remote_content?g=com.raskasa.metrics&a=metrics-okhttp&v=LATEST
  [okhttp]: http://square.github.io/okhttp/
  [sonatype]: https://oss.sonatype.org/content/repositories/snapshots/com/raskasa/metrics/
