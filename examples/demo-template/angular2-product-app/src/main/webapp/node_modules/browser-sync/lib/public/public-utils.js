"use strict";

var _ = require("lodash");

module.exports = {
    /**
     * Emit the internal `file:change` event
     * @param {EventEmitter} emitter
     * @param {string} path
     * @param {boolean} [log]
     */
    emitChangeEvent: function emitChangeEvent (emitter, path, log) {
        emitter.emit("file:changed", {
            path:      path,
            log:       log,
            namespace: "core"
        });
    },
    /**
     * Emit the internal `browser:reload` event
     * @param {EventEmitter} emitter
     */
    emitBrowserReload: function emitChangeEvent (emitter) {
        emitter.emit("browser:reload");
    },
    /**
     * Emit the internal `stream:changed` event
     * @param {EventEmitter} emitter
     * @param {Array} changed
     */
    emitStreamChangedEvent: function (emitter, changed) {
        emitter.emit("stream:changed", {changed: changed});
    },
    /**
     * This code handles the switch between .reload & .stream
     * since 2.6.0
     * @param name
     * @param args
     * @returns {boolean}
     */
    isStreamArg: function (name, args) {

        if (name === "stream") {
            return true;
        }

        if (name !== "reload") {
            return false;
        }

        var firstArg = args[0];

        /**
         * If here, it's reload with args
         */
        if (_.isObject(firstArg)) {
            if (!Array.isArray(firstArg) && Object.keys(firstArg).length) {
                return firstArg.stream === true;
            }
        }

        return false;
    }
};
