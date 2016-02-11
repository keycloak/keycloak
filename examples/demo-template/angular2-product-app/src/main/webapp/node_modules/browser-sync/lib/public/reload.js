"use strict";

var utils         = require("../utils");
var publicUtils   = require("./public-utils");
var _             = require("lodash");
var defaultConfig = require("../default-config");
var stream        = require("./stream");

/**
 * @param emitter
 * @returns {Function}
 */
module.exports = function (emitter) {

    /**
     * Inform browsers about file changes.
     *
     * eg: reload("core.css")
     */
    function browserSyncReload (opts) {

        /**
         * BACKWARDS COMPATIBILITY:
         * Passing an object as the only arg to the `reload`
         * method with at *least* the key-value pair of {stream: true},
         * was only ever used for streams support - so it's safe to check
         * for that signature here and defer to the
         * dedicated `.stream()` method instead.
         */
        if (_.isObject(opts)) {
            if (!Array.isArray(opts) && Object.keys(opts).length) {
                if (opts.stream === true) {
                    return stream(emitter)(opts);
                }
            }
        }

        /**
         * Handle single string paths such as
         * reload("core.css")
         */
        if (typeof opts === "string" && opts !== "undefined") {
            return publicUtils.emitChangeEvent(emitter, opts, true);
        }

        /**
         * Handle an array of file paths such as
         * reload(["core.css, "ie.cess"])
         */
        if (Array.isArray(opts)) {

            if (utils.willCauseReload(opts, defaultConfig.injectFileTypes)) {
                return publicUtils.emitBrowserReload(emitter);
            }

            return opts.forEach(function (filepath) {
                publicUtils.emitChangeEvent(emitter, filepath, true);
            });
        }

        /**
         * At this point the argument given was neither an object,
         * array or string so we simply perform a reload. This is to
         * allow the following syntax to work as expected
         *
         * reload();
         */
        return publicUtils.emitBrowserReload(emitter);
    }

    return browserSyncReload;
};
