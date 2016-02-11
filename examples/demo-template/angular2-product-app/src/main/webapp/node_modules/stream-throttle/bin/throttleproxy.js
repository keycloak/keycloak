#!/usr/bin/env node

var net = require('net');
var opts = require('commander');
var Throttle = require('../src/throttle.js').Throttle;

function parseAddr(addr) {
    var result = /^(([^:]*):)?(\d+)$/.exec(addr);
    if (!result)
        return null;
    return {
        host: result[2],
        port: result[3]
    };
}

function parseInteger(s) {
    if (!/^\d+$/.test(s))
        return undefined;
    return parseInt(s, 10);
}

function runProxy(localAddr, remoteAddr, downRate, upRate) {
    var server = net.createServer(function(local) {

        var remote = net.createConnection(remoteAddr);

        var localThrottle = new Throttle({rate: upRate});
        var remoteThrottle = new Throttle({rate: downRate});

        local.pipe(localThrottle).pipe(remote);
        local.on('error', function() {
            remote.destroy();
            local.destroy();
        });

        remote.pipe(remoteThrottle).pipe(local);
        remote.on('error', function() {
            local.destroy();
            remote.destroy();
        });
    });

    server.listen(localAddr.port, localAddr.host);
}

function main() {
    var localAddr, remoteAddr, downRate, upRate;

    opts
    .option('-l, --localaddr <addr>', 'local address, default 0.0.0.0:8080')
    .option('-r, --remoteaddr <addr>', 'remote address, default localhost:80')
    .option('-d, --downstream <bps>', 'downstream bandwidth', parseInteger)
    .option('-u, --upstream <bps>', 'upstream bandwidth, default equal to downstream', parseInteger)
    .parse(process.argv);

    if (opts.localaddr !== undefined) {
        localAddr = parseAddr(opts.localaddr);
        if (!localAddr)
            opts.help();
    } else
        localAddr = {host: undefined, port: 8080};

    if (opts.remoteaddr !== undefined) {
        remoteAddr = parseAddr(opts.remoteaddr);
        if (!remoteAddr)
            opts.help();
    } else
        remoteAddr = {host: undefined, port: 80};

    if (opts.downstream === undefined)
        opts.help();
    downRate = opts.downstream;

    if (opts.upstream !== undefined)
        upRate = opts.upstream;
    else
        upRate = downRate;

    runProxy(localAddr, remoteAddr, downRate, upRate);
}

main();
