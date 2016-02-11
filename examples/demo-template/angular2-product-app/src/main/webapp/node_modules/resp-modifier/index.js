"use strict";

var utils     = require("./lib/utils");
var debug     = require("debug")("resp-mod");

function RespModifier (opts) {

    // options
    opts               = opts || {};
    opts.blacklist     = utils.toArray(opts.blacklist)     || [];
    opts.whitelist     = utils.toArray(opts.whitelist)     || [];
    opts.hostBlacklist = utils.toArray(opts.hostBlacklist) || [];
    opts.rules         = opts.rules                        || [];
    opts.ignore        = opts.ignore || opts.excludeList   || utils.defaultIgnoreTypes;

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
            debug("Reject req", req.url);
            return next();
        }
        debug("Accept req", req.url);

        res._respModifier = true;

        var writeHead   = res.writeHead;
        var runPatches  = true;
        var write       = res.write;
        var end         = res.end;
        var singlerules = utils.isWhiteListedForSingle(req.url, respMod.opts.rules);

        var withoutSingle = respMod.opts.rules.filter(function (rule) {
            if (rule.paths && rule.paths.length) {
                return false;
            }
            return true;
        });

        /**
         * Exit early for blacklisted domains
         */
        if (respMod.opts.hostBlacklist.indexOf(req.headers.host) > -1) {
            return next();
        }

        if (singlerules.length) {
            modifyResponse(singlerules, true);
        } else {
            if (utils.isWhitelisted(req.url, respMod.opts.whitelist)) {
                modifyResponse(withoutSingle, true);
            } else {
                if (!utils.hasAcceptHeaders(req) || utils.inBlackList(req.url, respMod.opts)) {
                    debug("Black listed or no text/html headers", req.url);
                    return next();
                } else {
                    modifyResponse(withoutSingle);
                }
            }
        }

        next();

        /**
         * Actually do the overwrite
         * @param {Array} rules
         * @param {Boolean} [force] - if true, will always attempt to perform
         * an overwrite - regardless of whether it appears to be HTML or not
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

            res.write = function (string, encoding) {

                if (!runPatches) {
                    return write.call(res, string, encoding);
                }

                if (string !== undefined) {
                    var body = string instanceof Buffer ? string.toString(encoding) : string;
                    // If this chunk appears to be valid, push onto the res.data stack
                    if (force || (utils.isHtml(body) || utils.isHtml(res.data))) {
                        res.push(body);
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

                res.data = res.data || "";

                if (typeof string === "string") {
                    res.data += string;
                }

                if (string instanceof Buffer) {
                    res.data += string.toString();
                }

                if (!runPatches) {
                    return end.call(res, string, encoding);
                }

                // Check if our body is HTML, and if it does not already have the snippet.
                if (force || utils.isHtml(res.data) && !utils.snip(res.data)) {
                    // Include, if necessary, replacing the entire res.data with the included snippet.
                    res.data = utils.applyRules(rules, res.data);
                    runPatches = false;
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
