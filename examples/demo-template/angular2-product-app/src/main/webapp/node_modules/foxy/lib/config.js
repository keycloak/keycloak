"use strict";

var url       = require("url");

var utils     = require("./utils");
var errors    = require("./errors");
var merge     = require("lodash.merge");

var defaults = {
    /**
     * Error handler for proxy server.
     */
    errHandler: errors,
    /**
     * White-list for paths to open to middleware
     */
    whitelist: [],
    /**
     * Black-list for paths to open to middleware
     */
    blacklist: [],
    /**
     * Cookie options
     */
    cookies: {
        /**
         * Strip the domain attribute from cookies
         * This is `true` by default to help with logins etc
         */
        stripDomain: true
    },
    /**
     * Default req headers
     * @param config
     * @returns {{host: string, accept-encoding: string, agent: boolean}}
     */
    reqHeaders: function (config) {
        return {
            "host":            config.urlObj.host,
            "accept-encoding": "identity", // disable any compression
            "agent":           false
        };
    },
    /**
     * Serve any static files here
     */
    staticFiles: {},
    /**
     *
     */
    middleware: [utils.handleIe],
    /**
     * Proxy Options that get passed along to http-proxy
     */
    proxyOptions: {
        xfwd: false,
        headers: {},
        target: "",
        // secure: false - This forces http-proxy to set rejectUnauthorized: false which allows self-signed certs
        // which we are fine with considering this is a development tool, not to be used in production. :)
        secure:  false,
        ws: true
    },
    /**
     * Any transformations to occur after the server has sent
     * its response
     */
    proxyRes: [
        utils.checkCookies,
        utils.removeExcludedHeaders,
        utils.handleRedirect
    ],
    /**
     * Remove content-length headers as we've rewritten the response
     */
    excludedHeaders: [
        "content-length", "content-encoding"
    ]
};

/**
 * @param {String} target - a url such as http://www.bbc.co.uk or http://localhost:8181
 * @param {Object} [userConfig]
 * @returns {Object}
 */
module.exports = function (target, userConfig) {

    // Merge defaults with user config
    // + add extra needed config options

    var config = merge({}, defaults, userConfig, function(a, b) {
        if (Array.isArray(a)) {
            return a.concat(b);
        }
    });

    var urlObj  = url.parse(target);
    var _target = urlObj.protocol + "//" + urlObj.hostname;

    config.urlObj = urlObj;
    config.hostHeader = utils.getProxyHost(urlObj);

    if (urlObj.port) {
        _target = _target + ":" + urlObj.port;
    }

    config.target = _target;

    return config;
};
