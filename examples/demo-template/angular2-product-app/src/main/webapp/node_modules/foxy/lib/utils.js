"use strict";

var url         = require("url");
var path        = require("path");
var excludeList = require("./exclude");
var utils       = exports;

/**
 * @param {Object} res
 * @param {Object} req
 * @param {Object} config
 */
utils.removeExcludedHeaders = function removeHeaders (res, req, config) {
    config.excludedHeaders.forEach(function (item) {
        if (res.headers.hasOwnProperty(item)) {
            delete res.headers[item];
        }
    });
};

/**
 * Get the proxy host with optional port
 */
utils.getProxyHost = function getProxyHost(opts) {
    if (opts.port && opts.port !== 80) {
        return opts.hostname + ":" + opts.port;
    }
    return opts.hostname;
};

/**
 * Remove the domain from any cookies.
 * @param rawCookie
 * @returns {string}
 */
utils.rewriteCookies = function rewriteCookies(rawCookie) {

    var objCookie = (function () {
        // simple parse function (does not remove quotes)
        var obj = {};
        var pairs = rawCookie.split(/; */);

        pairs.forEach( function( pair ) {
            var eqIndex = pair.indexOf("=");

            // skip things that don't look like key=value
            if (eqIndex < 0) {
                return;
            }

            var key = pair.substr(0, eqIndex).trim();
            obj[key] = pair.substr(eqIndex + 1, pair.length).trim();
        });

        return obj;
    })();

    var pairs = Object.keys(objCookie)
        .filter(function (item) {
            return item !== "domain";
        })
        .map(function (key) {
            return key + "=" + objCookie[key];
        });

    if (rawCookie.match(/httponly/i)) {
        pairs.push("HttpOnly");
    }

    return pairs.join("; ");
};

/**
 * @param userServer
 * @param proxyUrl
 * @returns {{match: RegExp, fn: Function}}
 */
utils.rewriteLinks = function rewriteLinks(userServer, proxyUrl) {

    var host   = userServer.hostname;
    var string = host;
    var port = userServer.port;

    if (host && port) {
        if (parseInt(port, 10) !== 80) {
            string = host + ":" + port;
        }
    }

    return {
        match: new RegExp("https?://" + string + "(\/)?|('|\")(https?://|/|\\.)?" + string + "(\/)?(.*?)(?=[ ,'\"\\s])", "g"),
        fn:    function (match) {

            /**
             * Reject subdomains
             */
            if (match[0] === ".") {
                return match;
            }

            var captured = match[0] === "'" || match[0] === "\"" ? match[0] : "";

            /**
             * allow http https
             * @type {string}
             */
            var pre = "//";

            if (match[0] === "'" || match[0] === "\"") {
                match = match.slice(1);
            }

            /**
             * parse the url
             * @type {number|*}
             */
            var out = url.parse(match);

            /**
             * If host not set, just do a simple replace
             */
            if (!out.host) {
                string = string.replace(/^(\/)/, "");
                return captured + match.replace(string, proxyUrl);
            }

            /**
             * Only add trailing slash if one was
             * present in the original match
             */
            if (out.path === "/") {
                if (match.slice(-1) === "/") {
                    out.path = "/";
                } else {
                    out.path = "";
                }
            }

            /**
             * Finally append all of parsed url
             */
            return [
                captured,
                pre,
                proxyUrl,
                out.path || "",
                out.hash || ""
            ].join("");
        }
    };
};

/**
 * @param {Object} req
 * @returns {Object}
 */
utils.handleIe = function handleIe(req, res, next) {

    var ua = req.headers["user-agent"];
    var match = /MSIE (\d)\.\d/.exec(ua);

    if (match) {

        if (parseInt(match[1], 10) < 9) {

            var parsed = url.parse(req.url);
            var ext = path.extname(parsed.pathname);

            var excluded = excludeList.some(function (item) {
                return item === ext;
            });

            if (!excluded) {
                req.headers["accept"] = "text/html";
            }
        }
    }

    next();

    return req;
};

/**
 * @param config
 * @param host
 * @returns {*[]}
 */
utils.getRules = function getRules(config, host) {
    var rules = [];
    if (config.rules && config.rules.length) {
        var conf = config.rules;
        if (!Array.isArray(conf)) {
            conf = [conf];
        }
        conf.forEach(function (item) {
            rules.push(item);
        });
    }
    rules.push(utils.rewriteLinks(config.urlObj, host));
    return rules;
};

/**
 * Remove 'domain' from any cookies
 * @param {Object} res
 * @param {Object} req
 * @param {Immutable.Map} config
 */
utils.checkCookies = function checkCookies(res, req, config) {
    if (typeof(res.headers["set-cookie"]) !== "undefined") {
        if (config.cookies && config.cookies.stripDomain) {
            res.headers["set-cookie"] = res.headers["set-cookie"].map(function (item) {
                return utils.rewriteCookies(item);
            });
        }
    }
};

/**
 * If any redirects contain the HOST of the target or req, rewrite it,
 * otherwise let it through
 * @param config
 * @returns {Function}
 */
var redirectRegex     = /^30(1|2|7|8)$/;
utils.handleRedirect = function handleRedirect (proxyRes, req, config) {

    var whitelist = [
        config.urlObj.host,
        req.headers.host
    ];

    if (proxyRes.headers["location"] && redirectRegex.test(proxyRes.statusCode)) {
        var u = url.parse(proxyRes.headers["location"]);
        if (whitelist.indexOf(u.host) > -1) {
            u.host = req.headers.host;
            proxyRes.headers["location"] = u.format();
        }
    }
};

/**
 * Perform any required transformations on the proxyRes `res` object before it gets back to
 * the browser
 * @param config
 */
utils.proxyRes = function proxyRes(config) {
    return function (res, req) {
        config.proxyRes.forEach(function (func) {
            func(res, req, config);
        });
    };
};
