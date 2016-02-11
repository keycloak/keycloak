"use strict";

/**
 * $ browser-sync reload <options>
 *
 * This commands starts the Browsersync servers
 * & Optionally UI.
 *
 * @param opts
 * @returns {Function}
 */
module.exports = function (opts) {

    var flags = opts.cli.flags;
    if (!flags.url) {
        flags.url = "http://localhost:" + (flags.port || 3000);
    }
    var proto  = require("../http-protocol");
    var scheme = flags.url.match(/^https/) ? "https" : "http";
    var args   = {method: "reload"};

    if (flags.files) {
        args.args = flags.files;
    }

    var url    = proto.getUrl(args, flags.url);

    require(scheme).get(url, function (res) {
        res.on("data", function () {
            if (res.statusCode === 200) {
                opts.cb(null, res);
            }
        });
    }).on("error", function (err) {
        if (err.code === "ECONNREFUSED") {
            err.message = "Browsersync not running at " + flags.url;
        }
        return opts.cb(err);
    });
};
