var ThrottleGroup = require("stream-throttle").ThrottleGroup;

module.exports = throttle;

/**
 *
 */
function throttle (opts) {

    var options = {
        local_host:  "localhost",
        remote_host: "localhost",
        upstream:    10*1024,
        downstream:  opts.speed.speed * 1024,
        keepalive:   false
    };

    var serverOpts = {
        allowHalfOpen: true,
        rejectUnauthorized: false
    };

    var module = "net";
    var method = "createConnection";

    if (opts.key) {
        module = "tls";
        method = "connect";
        serverOpts.key = opts.key;
        serverOpts.cert = opts.cert;
    }

    return require(module).createServer(serverOpts, function (local) {

        var remote = require(module)[method]({
            host: opts.target.hostname,
            port: opts.target.port,
            allowHalfOpen: true,
            rejectUnauthorized: false
        });

        var upThrottle = new ThrottleGroup({ rate: options.upstream });
        var downThrottle = new ThrottleGroup({ rate: options.downstream });

        var localThrottle = upThrottle.throttle();
        var remoteThrottle = downThrottle.throttle();

        setTimeout(function () {
            local
                .pipe(localThrottle)
                .pipe(remote);
        }, opts.speed.latency);

        setTimeout(function () {
            remote
                .pipe(remoteThrottle)
                .pipe(local);
        }, opts.speed.latency);

        local.on("error", function() {
            remote.destroy();
            local.destroy();
        });

        remote.on("error", function() {
            local.destroy();
            remote.destroy();
        });
    });
}