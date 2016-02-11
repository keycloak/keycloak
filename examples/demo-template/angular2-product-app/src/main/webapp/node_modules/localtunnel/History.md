# 1.8.1 (2016-01-20)

* fix bug w/ HostHeaderTransformer and binary data

# 1.8.0 (2015-11-04)

* pass socket errors up to top level

# 1.7.0 (2015-07-22)

* add short arg options

# 1.6.0 (2015-05-15)

* keep sockets alive after connecting
* add --open param to CLI

# 1.5.0 (2014-10-25)

* capture all errors on remote socket and restart the tunnel

# 1.4.0 (2014-08-31)

* don't emit errors for ETIMEDOUT

# 1.2.0 / 2014-04-28

* return `client` from `localtunnel` API instantiation

# 1.1.0 / 2014-02-24

* add a host header transform to change the 'Host' header in requests

# 1.0.0 / 2014-02-14

* default to localltunnel.me for host
* remove exported `connect` method (just export one function that does the same thing)
* change localtunnel signature to (port, opt, fn)

# 0.2.2 / 2014-01-09
