# stream-throttle #

A rate limiter for Node.js streams.

## API usage

This module exports two classes, `Throttle` and `ThrottleGroup`.

`Throttle` creates a single throttled stream, based on `stream.Transform`. It accepts an `opts` parameter with the following keys:

* `opts.rate` is the throttling rate, in bytes per second.
* `opts.chunksize` (optional) is the maximum chunk size into which larger writes are decomposed; the default is `opts.rate`/10.

The `opts` object may also contain options to be passed to the `stream.Transform` constructor.

For example, the following code throttles stdin to stdout at 10 bytes per second:

    process.stdin.pipe(new Throttle({rate: 10})).pipe(process.stdout)

`ThrottleGroup` allows the creation of a group of streams whose aggregate bandwidth is throttled. The constructor accepts the same `opts` argument as for `Throttle`. Call `throttle` on a `ThrottleGroup` object to create a new throttled stream belonging to the group.

For example, the following code creates two HTTP connections to `www.google.com:80`, and throttles their aggregate (downstream) bandwidth to 10 KB/s:

    var addr = { host: 'www.google.com', port: 80 };
    var tg = new ThrottleGroup({rate: 10240});

    var conn1 = net.createConnection(addr),
        conn2 = net.createConnection(addr);

    var thr1 = conn1.pipe(tg.throttle()),
        thr2 = conn2.pipe(tg.throttle());

    // Reads from thr1 and thr2 are throttled to 10 KB/s in aggregate

## Command line usage

This package installs a `throttleproxy` binary which implements a command-line utility for throttling connections. Run `throttleproxy -h` for instructions.

## Contributing

Feel free to open an issue or send a pull request.

## License

BSD-style. See the LICENSE file.

## Author

Copyright © 2013 Tiago Quelhas. Contact me at `<tiagoq@gmail.com>`.
