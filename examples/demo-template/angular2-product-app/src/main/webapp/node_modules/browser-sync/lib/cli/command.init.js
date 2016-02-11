"use strict";

var info          = require("./cli-info");

/**
 * $ browser-sync init
 *
 * This command will generate a configuration
 * file in the current directory
 *
 * @param opts
 */
module.exports = function (opts) {
    info.makeConfig(process.cwd(), opts.cb);
};
