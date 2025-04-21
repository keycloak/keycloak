# cacheable-lookup

> A cacheable [`dns.lookup(…)`](https://nodejs.org/api/dns.html#dns_dns_lookup_hostname_options_callback) that respects TTL :tada:

[![Build Status](https://travis-ci.org/szmarczak/cacheable-lookup.svg?branch=master)](https://travis-ci.org/szmarczak/cacheable-lookup)
[![Coverage Status](https://coveralls.io/repos/github/szmarczak/cacheable-lookup/badge.svg?branch=master)](https://coveralls.io/github/szmarczak/cacheable-lookup?branch=master)
[![npm](https://img.shields.io/npm/dm/cacheable-lookup.svg)](https://www.npmjs.com/package/cacheable-lookup)
[![install size](https://packagephobia.now.sh/badge?p=cacheable-lookup)](https://packagephobia.now.sh/result?p=cacheable-lookup)

Making lots of HTTP requests? You can save some time by caching DNS lookups :zap:

## Usage

### Using the `lookup` option

```js
const http = require('http');
const CacheableLookup = require('cacheable-lookup');
const cacheable = new CacheableLookup();

http.get('https://example.com', {lookup: cacheable.lookup}, response => {
	// Handle the response here
});
```

### Attaching CacheableLookup to an Agent

```js
const http = require('http');
const CacheableLookup = require('cacheable-lookup');
const cacheable = new CacheableLookup();

cacheable.install(http.globalAgent);

http.get('https://example.com', response => {
	// Handle the response here
});
```

## API

### new CacheableLookup(options)

Returns a new instance of `CacheableLookup`.

#### options

Type: `Object`<br>
Default: `{}`

Options used to cache the DNS lookups.

##### options.cacheAdapter

Type: [Keyv adapter instance](https://github.com/lukechilds/keyv)<br>
Default: `new Map()`

A Keyv adapter which stores the cache.

##### options.maxTtl

Type: `number`<br>
Default: `Infinity`

Limits the cache time (TTL in seconds).

If set to `0`, it will make a new DNS query each time.

##### options.resolver

Type: `Function`<br>
Default: [`new dns.Resolver()`](https://nodejs.org/api/dns.html#dns_class_dns_resolver)

An instance of [DNS Resolver](https://nodejs.org/api/dns.html#dns_class_dns_resolver) used to make DNS queries.

### Entry object

Type: `Object`

#### address

Type: `string`

The IP address (can be an IPv4 or IPv6 address).

#### family

Type: `number`

The IP family (`4` or `6`).

##### expires

Type: `number`

**Note**: This is not present when using the native `dns.lookup(...)`!

The timestamp (`Date.now() + ttl * 1000`) when the entry expires.

#### ttl

**Note**: This is not present when using the native `dns.lookup(...)`!

The time in seconds for its lifetime.

### Entry object (callback-style)

When `options.all` is `false`, then `callback(error, address, family, expires, ttl)` is called. <br>
When `options.all` is `true`, then `callback(error, entries)` is called.

### CacheableLookup instance

#### servers

Type: `Array`

DNS servers used to make the query. Can be overridden - then the new servers will be used.

#### [lookup(hostname, options, callback)](https://nodejs.org/api/dns.html#dns_dns_lookup_hostname_options_callback)

#### lookupAsync(hostname, options)

The asynchronous version of `dns.lookup(…)`.

Returns an [entry object](#entry-object).<br>
If `options.all` is true, returns an array of entry objects.

**Note**: If entry(ies) were not found, it will return `undefined`.

##### hostname

Type: `string`

##### options

Type: `Object`

The same as the [`dns.lookup(…)`](https://nodejs.org/api/dns.html#dns_dns_lookup_hostname_options_callback) options.

##### options.throwNotFound

Type: `boolean`<br>
Default: `false`

Throw when there's no match.

If set to `false` and it gets no match, it will return `undefined`.

**Note**: This option is meant **only** for the asynchronous implementation! The synchronous version will always give an error if no match found.

#### query(hostname, family)

An asynchronous function which returns cached DNS lookup entries. This is the base for `lookupAsync(hostname, options)` and `lookup(hostname, options, callback)`.

**Note**: This function has no options.

Returns an array of objects with `address`, `family`, `ttl` and `expires` properties.

#### queryAndCache(hostname, family)

An asynchronous function which makes a new DNS lookup query and updates the database. This is used by `query(hostname, family)` if no entry in the database is present.

Returns an array of objects with `address`, `family`, `ttl` and `expires` properties.

#### updateInterfaceInfo()

Updates interface info. For example, you need to run this when you plug or unplug your WiFi driver.

## High performance

See the benchmarks (queries `localhost`, performed on i7-7700k):

```
CacheableLookup#lookupAsync                x 265,390 ops/sec ±0.65%  (89 runs sampled)
CacheableLookup#lookupAsync.all            x 119,187 ops/sec ±2.57%  (87 runs sampled)
CacheableLookup#lookupAsync.all.ADDRCONFIG x 119,666 ops/sec ±0.75%  (89 runs sampled)
CacheableLookup#lookup                     x 116,604 ops/sec ±0.68%  (88 runs sampled)
CacheableLookup#lookup.all                 x 115,627 ops/sec ±0.72%  (89 runs sampled)
CacheableLookup#lookup.all.ADDRCONFIG      x 115,578 ops/sec ±0.90%  (88 runs sampled)
CacheableLookup#lookupAsync - zero TTL     x 60.83   ops/sec ±7.43%  (51 runs sampled)
CacheableLookup#lookup      - zero TTL     x 49.22   ops/sec ±20.58% (49 runs sampled)
dns#resolve4                               x 63.00   ops/sec ±5.88%  (51 runs sampled)
dns#lookup                                 x 21,303  ops/sec ±29.06% (35 runs sampled)
dns#lookup.all                             x 22,283  ops/sec ±21.96% (38 runs sampled)
dns#lookup.all.ADDRCONFIG                  x 5,922   ops/sec ±10.18% (38 runs sampled)
Fastest is CacheableLookup#lookupAsync
```

The package is based on [`dns.resolve4(…)`](https://nodejs.org/api/dns.html#dns_dns_resolve4_hostname_options_callback) and [`dns.resolve6(…)`](https://nodejs.org/api/dns.html#dns_dns_resolve6_hostname_options_callback).

[Why not `dns.lookup(…)`?](https://github.com/nodejs/node/issues/25560#issuecomment-455596215)

> It is not possible to use `dns.lookup(…)` because underlying calls like [getaddrinfo](http://man7.org/linux/man-pages/man3/getaddrinfo.3.html) have no concept of servers or TTL (caching is done on OS level instead).

## Related

 - [cacheable-request](https://github.com/lukechilds/cacheable-request) - Wrap native HTTP requests with RFC compliant cache support

## License

MIT
