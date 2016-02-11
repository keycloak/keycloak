#!/usr/bin/env node
var meow   = require("meow");
var devip  = require("dev-ip")();
var logger = require("./lib/logger");
var foxy   = require("./");
var http   = require("http");
var path   = require("path");
var fs     = require("fs");
var https  = require("https");

/**
 * Handle cli input
 */
if (!module.parent) {
    handleCli(meow({
        help: fs.readFileSync(path.join(__dirname, "/help.txt"), "utf8")
    }));
}

module.exports = handleCli;

/**
 * @param cli
 */
function handleCli (cli) {

    if (!cli.input.length) {
        logger.info("At least 1 argument is required {yellow:(url)}");
        logger.info("For help, run {cyan:foxy --help");
        return false;
    }

    var url    = require("url").parse(cli.input[0]);
    var scheme = url.protocol === "https:" ? "https" : "http";
    var target = url.protocol + "//" + url.host;
    var app    = foxy(target, cli.flags);
    var server = getServer(scheme, app).listen(cli.flags.port);
    var port   = server.address().port;
    var urls   = [scheme + "://localhost:" + port];

    if (devip.length) {
        urls.push(scheme + "://" + devip[0] + ":" + server.address().port);
    }

    urls.forEach(function (url) {
        logger.info("Server running at: {magenta:%s", url);
    });

    return {
        server: server,
        app:    app,
        urls:   urls
    };
}

/**
 * @param scheme
 * @param app
 * @returns {{staticServer: http.Server, proxyServer: http.Server}}
 */
function getServer (scheme, app) {

    var server;

    if (scheme === "https") {
        server = https.createServer({
            key:  fs.readFileSync(path.resolve(__dirname, "test/fixtures/certs/server.key")),
            cert: fs.readFileSync(path.resolve(__dirname, "test/fixtures/certs/server.cert"))
        }, app);
    } else {
        server = http.createServer(app);
    }

    /**
     * Proxy web sockets
     */
    server.on("upgrade", app.handleUpgrade);

    return server;
}
