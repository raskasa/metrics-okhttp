Metrics OkHttp
==============

An [OkHttp Client][okhttp] wrapper providing [Metrics][metrics] instrumentation of connection pools, 
request durations and rates, and other useful information.

Usage
-----

`metrics-okhttp` provides `InstrumentedOkHttpClients`, a static factory class for instrumenting 
OkHttp HTTP clients.

You can create an instrumented `OkHttpClient` by doing the following:

```java
Metrics registry = ...;
OkHttpClient client = InstrumentedOkHttpClients.create(registry);
```

If you wish to provide you're own `OkHttpClient` instance, you can do that as well:

```java
Metrics registry = ...;
OkHttpClient rawClient = ...;
OkHttpClient client = InstrumentedOkHttpClients.create(registry, rawClient);
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

Download
--------

**Metrics OkHttp is currently under development.**  The API is not stable and neither is the feature set.

Snapshots of the development version are available in [Sonatype's `snapshots` repository][sonatype].

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
  [okhttp]: http://square.github.io/okhttp/
  [sonatype]: https://oss.sonatype.org/content/repositories/snapshots/
