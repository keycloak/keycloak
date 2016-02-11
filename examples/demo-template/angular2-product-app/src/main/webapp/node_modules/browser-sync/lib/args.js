"use strict";

/**
 * The purpose of this function is
 * to handle back-backwards compatibility
 * @param {Object} args
 * @returns {{config: {}, cb: *}}
 */
module.exports = function (args) {

    var config = {};
    var cb;

    switch (args.length) {

        case 1 :

            if (isFilesArg(args[0])) {

                config.files = args[0];

            } else if (typeof args[0] === "function") {

                cb = args[0];

            } else {

                config = args[0];

            }

            break;

        case 2 :

            // if second is a function, first MUST be config
            if (typeof args[1] === "function") {

                config = args[0] || {};
                cb = args[1];

            } else {

                config = args[1] || {};

                if (!config.files) {
                    config.files = args[0];
                }
            }

            break;

        case 3 :

            config = args[1] || {};

            if (!config.files) {
                config.files = args[0];
            }

            cb = args[2];
    }

    return {
        config: config,
        cb: cb
    };
};

/**
 * Files args were only ever strings or arrays
 * @param arg
 * @returns {*|boolean}
 */
function isFilesArg (arg) {
    return Array.isArray(arg) || typeof arg === "string";
}
