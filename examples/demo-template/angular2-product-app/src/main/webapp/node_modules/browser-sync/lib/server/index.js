"use strict";

/**
 * Browsersync server
 * Three available modes: Snippet, Server or Proxy
 */
module.exports.plugin = function (bs, scripts) {

    var debug   = bs.debug;
    var proxy   = bs.options.get("proxy");
    var type    = bs.options.get("mode");

    var bsServer   = createServer(bs, scripts);

    if (type === "server" || type === "snippet") {
        debug("Static Server running ({magenta:%s}) ...", bs.options.get("scheme"));
    }

    if (proxy) {
        debug("Proxy running, proxing: {magenta:%s}", proxy.get("target"));
    }

    if (bsServer) {
        bsServer.server.listen(bs.options.get("port"));
        bs.registerCleanupTask(function () {
            bsServer.server.close();
        });
    }

    debug("Running mode: %s", type.toUpperCase());

    return {
        server: bsServer.server,
        app:    bsServer.app
    };
};

/**
 * Launch the server for serving the client JS plus static files
 * @param {BrowserSync} bs
 * @param {String} clientScripts
 * @returns {{staticServer: (http.Server), proxyServer: (http.Server)}}
 */
function createServer (bs, clientScripts) {

    var proxy  = bs.options.get("proxy");
    var server = bs.options.get("server");

    if (!proxy && !server) {
        return require("./snippet-server")(bs, clientScripts);
    }

    if (proxy) {
        return require("./proxy-server")(bs, clientScripts);
    }

    if (server) {
        return require("./static-server")(bs, clientScripts);
    }
}

module.exports.createServer = createServer;
