"use strict";

var utils    = exports;
var logger   = require("../logger").logger;
var _        = require("lodash");

utils.verifyOpts = function (optskey, cliFlags) {

    var flags         = require("./opts." + optskey + ".json");
    var flagKeys      = Object.keys(flags);
    var flagWhitelist = flagKeys.map(dropPrefix).map(_.camelCase);

    return Object.keys(cliFlags).every(function (key) {

        if (_.contains(flagWhitelist, key) || _.contains(flagKeys, key)) {
            return true;
        }

        logger.info("Unknown flag:  {yellow:`%s`", key);

        return false;
    });
};

/**
 * @param {String} item
 * @returns {String}
 */
function dropPrefix (item) {
    return item.replace("no-", "");
}
