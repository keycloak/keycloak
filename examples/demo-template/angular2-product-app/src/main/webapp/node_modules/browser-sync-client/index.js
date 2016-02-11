"use strict";

var etag  = require("etag");
var fresh = require("fresh");
var fs    = require("fs");
var path  = require("path");
var zlib  = require("zlib");

var minifiedScript   = path.join(__dirname, "/dist/index.min.js");
var unminifiedScript = path.join(__dirname, "/dist/index.js");

/**
 * Does the current request support compressed encoding?
 * @param {Object} req
 * @returns {boolean}
 */
function supportsGzip (req) {
    var accept = req.headers['accept-encoding'];
    return accept && accept.indexOf('gzip') > -1;
}

/**
 * Set headers on the response
 * @param {Object} res
 * @param {String} body
 */
function setHeaders(res, body) {

    res.setHeader("Cache-Control", "public, max-age=0");
    res.setHeader("Content-Type", "text/javascript");
    res.setHeader("ETag", etag(body));
}

/**
 * @param {Object} options
 * @param {String} connector
 * @returns {String}
 */
function getScriptBody(options, connector) {

    var script = minifiedScript;

    if (options && !options.minify) {
        script = unminifiedScript;
    }

    return connector + fs.readFileSync(script);
}

/**
 * @param {Object} req
 * @returns {String}
 */
function isConditionalGet(req) {
    return req.headers["if-none-match"] || req.headers["if-modified-since"];
}

/**
 * Return a not-modified response
 * @param {Object} res
 */
function notModified(res) {
    res.removeHeader("Content-Type");
    res.statusCode = 304;
    res.end();
}

/**
 * Public method for returning either a middleware fn
 * or the content as a string
 * @param {Object} options
 * @param {String} connector - content to be prepended
 * @param {String} type - either `file` or `middleware`
 * @returns {*}
 */
function init(options, connector, type) {

    var gzipCached;

    /**
     * Combine string to create the final version
     * @type {String}
     */
    var requestBody = getScriptBody(options, connector);

    /**
     * If the user asked for a file, simply return the string.
     */
    if (type && type === "file") {
        return requestBody;
    }

    /**
     * Otherwise return a function to be used a middleware
     */
    return function (req, res) {

        /**
         * default to using the uncompressed string
         * @type {String}
         */
        var output = requestBody;

        /**
         * Set the appropriate headers for caching
         */
        setHeaders(res, output);

        if (isConditionalGet(req) && fresh(req.headers, res._headers)) {
            return notModified(res);
        }

        /**
         * If gzip is supported, compress the string once
         * and save for future requests
         */
        if (supportsGzip(req)) {

            res.setHeader("Content-Encoding", "gzip");

            if (!gzipCached) {
                var buf = new Buffer(output, "utf-8");
                zlib.gzip(buf, function (_, result) {
                    gzipCached = result;
                    res.end(result);
                });
            } else {
                res.end(gzipCached);
            }

        } else {
            res.end(output);
        }
    };
}

module.exports.middleware = init;
module.exports.plugin = init;
module.exports.minified = function () {
    return fs.readFileSync(minifiedScript, 'utf8');
};
module.exports.unminified = function () {
    return fs.readFileSync(unminifiedScript, 'utf8');
}