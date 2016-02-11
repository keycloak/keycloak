"use strict";

var info = require("./cli-info");

/**
 * $ browser-sync start <options>
 *
 * This commands starts the Browsersync servers
 * & Optionally UI.
 *
 * @param opts
 * @returns {Function}
 */
module.exports = function (opts) {

    var flags = opts.cli.flags;

    return require("../../")
        .create("cli")
        .init(flags.config ? info.getConfigFile(flags.config) : flags, opts.cb);
};
