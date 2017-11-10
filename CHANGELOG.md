Change Log
==========

## Version 0.3.0

_2017-11-10_

* __Support OkHttp 3.x__
* Fix: Rewrite network request instrumentation with interceptors.  The
instrumented executor service is only used when executing asynchronous
requests - it is not used when executing synchronous requests.  By using
interceptors, we can record metrics all requests regardless of whether they are
synchronous or asynchronous.
* Other minor changes. 

## Version 0.2.0

_2015-12-14_

* __Custom instrumented client names.__  Users now have the ability to provide
an identifier for each instrumented client.  This is useful when using multiple
instances of `OkHttpClient` in your application.
* Updated OkHttp dependency to 2.7.
* Other minor changes.

## Version 0.1.0

_2015-07-22_

Initial release.
