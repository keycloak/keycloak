"use strict";

var _            = require("lodash");
var utils        = require("./utils");
var Immutable    = require("immutable");

/**
 * @param {BrowserSync} bs
 * @param {String} scripts
 * @returns {*}
 */
module.exports = function createProxyServer (bs, scripts) {

    var options = bs.options;

    checkProxyTarget(options, function (err) {
        if (err) {
            bs.events.emit("config:warn", {msg: err});
        }
    });

    var opts = getOptions(bs, scripts);

    bs.proxy = require("foxy").create(options.getIn(["proxy", "target"]), opts);

    var proxy = utils.getServer(bs.proxy.app, bs.options);

    bs.proxy.app.use(bs.options.getIn(["scriptPaths", "versioned"]), scripts);
    bs.proxy.app.use(bs.options.getIn(["scriptPaths", "path"]),      scripts);

    /**
     * Also proxy upgrades for Web Socket support if proxy.ws given
     */
    if (bs.options.getIn(["proxy", "ws"])) {
        proxy.server.on("upgrade", bs.proxy.app.handleUpgrade);
    }

    return proxy;
};

/**
 * @param bs
 */
function getOptions (bs) {

    var options  = bs.options;

    var proxyOptions   = options.getIn(["proxy", "proxyOptions"]);
    var rules          = bs.snippetMw;
    var cookies        = options.getIn(["proxy", "cookies"]);

    var out = {
        rules:       rules.opts.rules,
        whitelist:   rules.opts.whitelist,
        blacklist:   rules.opts.blacklist,
        middleware:  options.get("middleware").toJS().concat(bs.pluginManager.hook("server:middleware")),
        errHandler:  function (err) {
            bs.logger.debug("{red:[proxy error]} %s", err.message);
        }
    };

    if (proxyOptions) {
        out.proxyOptions = proxyOptions.toJS();
    }

    if (options.hasIn(["proxy", "proxyRes"])) {
        out.proxyRes = options.getIn(["proxy", "proxyRes"]).toJS();
    }

    if (options.hasIn(["proxy", "reqHeaders"])) {
        out.reqHeaders = options.getIn(["proxy", "reqHeaders"]);
    }

    if (!_.isUndefined(cookies)) {
        if (Immutable.Map.isMap(cookies)) {
            out.cookies = cookies.toJS();
        }
    }

    return out;
}

/**
 * @param {Object} opts
 * @param {Function} cb
 */
function checkProxyTarget (opts, cb) {

    var chunks  = [];
    var errored = false;
    var target  = opts.getIn(["proxy", "target"]);
    var scheme  = opts.get("scheme");

    function logError() {
        if (!errored) {
            cb("Proxy address not reachable - is your server running?");
            errored = true;
        }
    }

    require(scheme).get(target, function (res) {
        res.on("data", function (data) {
            chunks.push(data);
        });
    }).on("error", function (err) {
        if (_.contains(["ENOTFOUND", "ECONNREFUSED"], err.code)) {
            logError();
        }
    }).on("close", function () {
        if (!chunks.length) {
            logError();
        }
    });
}
