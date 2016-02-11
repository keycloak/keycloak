"use strict";

var utils     = require("./lib/utils");

function RespModifier (opts) {

    // options
    opts           = opts || {};
    opts.blacklist = utils.toArray(opts.blacklist)   || [];
    opts.whitelist = utils.toArray(opts.whitelist)   || [];
    opts.rules     = opts.rules                      || [];
    opts.ignore    = opts.ignore || opts.excludeList || utils.defaultIgnoreTypes;

    // helper functions
    opts.regex = (function () {
        var matches = opts.rules.map(function (item) {
            return item.match.source;
        }).join("|");
        return new RegExp(matches);
    })();

    var respMod        = this;

    respMod.opts       = opts;
    respMod.middleware = respModifierMiddleware;
    respMod.update = function (key, value) {
        if (respMod.opts[key]) {
            respMod.opts[key] = value;
        }
        return respMod;
    };

    function respModifierMiddleware(req, res, next) {

        if (res._respModifier) {
            return next();
        }

        res._respModifier = true;

        var writeHead = res.writeHead;
        var runPatches = true;
        var write = res.write;
        var count = 0;
        var end = res.end;
        res.rulesWritten = [];
        var singlerules = utils.isWhiteListedForSingle(req.url, respMod.opts.rules);

        var withoutSingle = respMod.opts.rules.filter(function (rule) {
            if (rule.paths && rule.paths.length) {
                return false;
            }
            return true;
        });

        if (singlerules.length) {
            modifyResponse(singlerules, true);
        } else {
            if (utils.isWhitelisted(req.url, respMod.opts.whitelist)) {
                modifyResponse(withoutSingle, true);
            } else {
                if (!utils.hasAcceptHeaders(req) || utils.inBlackList(req.url, respMod.opts)) {
                    return next();
                } else {
                    modifyResponse(withoutSingle);
                }
            }
        }

        next();

        /**
         * Actually do the overwrite
         * @param {boolean} [force] - if true, will perform an overwrite even regardless of whether it's HTML or not.
         */
        function modifyResponse(rules, force) {

            req.headers["accept-encoding"] = "identity";

            function restore() {
                res.writeHead = writeHead;
                res.write = write;
                res.end = end;
            }

            res.push = function (chunk) {
                res.data = (res.data || "") + chunk;
            };

            res.inject = res.write = function (string, encoding) {

                if (!runPatches) {
                    return write.call(res, string, encoding);
                }

                if (string !== undefined) {
                    var body = string instanceof Buffer ? string.toString(encoding) : string;
                    // If this chunk must receive a snip, do so
                    if (force || (utils.isHtml(body) || utils.isHtml(res.data))) {
                        if (utils.exists(body, opts.regex) && !utils.snip(res.data)) {
                            res.push(utils.overwriteBody(rules, body, res));
                            return true;
                        } // If in doubt, simply buffer the data for later inspection (on `end` function)
                        else {
                            res.push(body);
                            return true;
                        }
                    } else {
                        restore();
                        return write.call(res, string, encoding);
                    }
                }
                return true;
            };

            res.writeHead = function () {
                if (!runPatches) {
                    return writeHead.apply(res, arguments);
                }

                var headers = arguments[arguments.length - 1];
                if (typeof headers === "object") {
                    for (var name in headers) {
                        if (/content-length/i.test(name)) {
                            delete headers[name];
                        }
                    }
                }

                if (res.getHeader("content-length")) {
                    res.removeHeader("content-length");
                }

                writeHead.apply(res, arguments);
            };

            res.end = function (string, encoding) {
                if (!runPatches) {
                    return end.call(res, string, encoding);
                }

                // If there are remaining bytes, save them as well
                // Also, some implementations call "end" directly with all data.
                res.inject(string);
                runPatches = false;
                // Check if our body is HTML, and if it does not already have the snippet.
                if (utils.isHtml(res.data) && !utils.snip(res.data)) {
                    // Include, if necessary, replacing the entire res.data with the included snippet.
                    res.data = utils.overwriteBody(rules, res.data, res);
                }
                if (res.data !== undefined && !res._header) {
                    res.setHeader("content-length", Buffer.byteLength(res.data, encoding));
                }
                end.call(res, res.data, encoding);
            };
        }
    }

    return respMod;
}

module.exports = function (opts) {
    var resp = new RespModifier(opts);
    return resp.middleware;
};

module.exports.create = function (opts) {
    var resp = new RespModifier(opts);
    return resp;
};

module.exports.utils = utils;
