var http = require('http');
var https = require('https');
var url = require('url');
var assert = require('assert');

var localtunnel = require('../');

test('setup local http server', function(done) {
    var server = http.createServer();
    server.on('request', function(req, res) {
        res.write(req.headers.host);
        res.end();
    });
    server.listen(function() {
        var port = server.address().port;

        test._fake_port = port;
        console.log('local http on:', port);
        done();
    });
});

test('setup localtunnel client', function(done) {
    localtunnel(test._fake_port, function(err, tunnel) {
        assert.ifError(err);
        assert.ok(new RegExp('^https:\/\/.*localtunnel.me' + '$').test(tunnel.url));
        test._fake_url = tunnel.url;
        done();
    });
});

test('query localtunnel server w/ ident', function(done) {
    var uri = test._fake_url;
    var parsed = url.parse(uri);

    var opt = {
        host: parsed.host,
        port: 443,
        headers: {
            host: parsed.hostname
        },
        path: '/'
    };

    var req = https.request(opt, function(res) {
        res.setEncoding('utf8');
        var body = '';

        res.on('data', function(chunk) {
            body += chunk;
        });

        res.on('end', function() {
            assert(/.*[.]localtunnel[.]me/.test(body), body);
            done();
        });
    });

    req.end();
});

test('request specific domain', function(done) {
    localtunnel(test._fake_port, { subdomain: 'abcd' }, function(err, tunnel) {
        assert.ifError(err);
        assert.ok(new RegExp('^https:\/\/abcd.localtunnel.me' + '$').test(tunnel.url));
        tunnel.close();
        done();
    });
});

suite('--local-host localhost');

test('setup localtunnel client', function(done) {
    var opt = {
        local_host: 'localhost'
    };
    localtunnel(test._fake_port, opt, function(err, tunnel) {
        assert.ifError(err);
        assert.ok(new RegExp('^https:\/\/.*localtunnel.me' + '$').test(tunnel.url));
        test._fake_url = tunnel.url;
        done();
    });
});

test('override Host header with local-host', function(done) {
    var uri = test._fake_url;
    var parsed = url.parse(uri);

    var opt = {
        host: parsed.host,
        port: 443,
        headers: {
            host: parsed.hostname
        },
        path: '/'
    };

    var req = https.request(opt, function(res) {
        res.setEncoding('utf8');
        var body = '';

        res.on('data', function(chunk) {
            body += chunk;
        });

        res.on('end', function() {
            assert.equal(body, 'localhost');
            done();
        });
    });

    req.end();
});

suite('--local-host 127.0.0.1');

test('setup localtunnel client', function(done) {
    var opt = {
        local_host: '127.0.0.1'
    };
    localtunnel(test._fake_port, opt, function(err, tunnel) {
        assert.ifError(err);
        assert.ok(new RegExp('^https:\/\/.*localtunnel.me' + '$').test(tunnel.url));
        test._fake_url = tunnel.url;
        done();
    });
});

test('override Host header with local-host', function(done) {
    var uri = test._fake_url;
    var parsed = url.parse(uri);

    var opt = {
        host: parsed.host,
        port: 443,
        headers: {
            host: parsed.hostname
        },
        path: '/'
    };

    var req = https.request(opt, function(res) {
        res.setEncoding('utf8');
        var body = '';

        res.on('data', function(chunk) {
            body += chunk;
        });

        res.on('end', function() {
            assert.equal(body, '127.0.0.1');
            done();
        });
    });

    req.end();
});

test('send chunked request', function(done) {
    var uri = test._fake_url;
    var parsed = url.parse(uri);

    var opt = {
        host: parsed.host,
        port: 443,
        headers: {
            host: parsed.hostname,
            'Transfer-Encoding': 'chunked'
        },
        path: '/'
    };

    var req = https.request(opt, function(res) {
        res.setEncoding('utf8');
        var body = '';

        res.on('data', function(chunk) {
            body += chunk;
        });

        res.on('end', function() {
            assert.equal(body, '127.0.0.1');
            done();
        });
    });

    req.end(require('crypto').randomBytes(1024 * 8).toString('base64'));
});
