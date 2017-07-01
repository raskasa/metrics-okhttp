package com.raskasa.metrics.okhttp.sample;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.raskasa.metrics.okhttp.InstrumentedOkHttpClients;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Fetches user information from the GitHub public API */
public final class App {
  public static void main(String[] args) throws Exception {
    MetricRegistry metrics = new MetricRegistry();

    ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build();
    reporter.start(15, TimeUnit.SECONDS);

    OkHttpClient client = InstrumentedOkHttpClients.create(metrics);

    // noinspection InfiniteLoopStatement
    for (;;) {

      Request request = new Request.Builder()
          .url("https://api.github.com/repos/raskasa/metrics-okhttp")
          .build();

      Response response = client.newCall(request).execute();

      ResponseBody body = response.body();
      if (body != null) {
        System.out.println(body.string());
      } else {
        throw new RuntimeException("Body not expected to be closed");
      }

      Thread.sleep(15_000);  // 15 seconds
    }
  }
}
