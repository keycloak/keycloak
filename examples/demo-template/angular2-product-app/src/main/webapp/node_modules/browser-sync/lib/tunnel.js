"use strict";

var _ = require("lodash");
var utils = require("util");

/**
 * @param {BrowserSync} bs
 * @param {Function} cb
 */
module.exports = function (bs, cb) {

    var opts         = {};
    var options      = bs.options;
    var port         = options.get("port");

    if (_.isString(options.get("tunnel"))) {
        opts.subdomain = options.get("tunnel");
    }

    bs.debug("Requesting a tunnel connection on port: {magenta:%s}", port);
    bs.debug("Requesting a tunnel connection with options: {magenta:%s}", utils.inspect(opts));

    require("localtunnel")(port, opts, function (err, tunnel) {
        if (err) {
            return cb(err);
        }
        return cb(null, tunnel);
    });
};
