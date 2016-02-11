"use strict";

var path       = require("path");
var micromatch = require("micromatch");
var utils      = require("./public-utils");

/**
 * @param emitter
 * @returns {Function}
 */
module.exports = function (emitter) {

    /**
     * Return a transform/through stream that listens to file
     * paths and fires internal Browsersync events.
     * @param {{once: boolean, match: string|array}} [opts]
     * @returns {Stream.Transform}
     */
    function browserSyncThroughStream (opts) {

        opts = opts || {};
        var emitted = false;
        var Transform = require("stream").Transform;
        var reload = new Transform({objectMode: true});
        var changed = [];

        reload._transform = function (file, encoding, next) {

            var stream = this;

            /**
             * End is always called to send the current file down
             * stream. Browsersync never acts upon a stream,
             * we only `listen` to it.
             */
            function end () {
                stream.push(file); // always send the file down-stream
                next();
            }

            /**
             * If {match: <pattern>} was provided, test the
             * current filepath against it
             */
            if (opts.match) {
                if (!micromatch(file.path, opts.match, {dot: true}).length) {
                    return end();
                }
            }

            /**
             * if {once: true} provided, emit the reload event for the
             * first file only
             */
            if (opts.once === true && !emitted) {

                utils.emitBrowserReload(emitter);

                emitted = true;

            } else { // handle multiple

                if (opts.once === true && emitted) {

                } else {

                    if (file.path) {

                        emitted = true;
                        utils.emitChangeEvent(emitter, file.path, false);
                        changed.push(path.basename(file.path));
                    }
                }
            }

            end();
        };

        /**
         * When this current operation has finished, emit the
         * steam:changed event so that any loggers can pick up it
         * @param next
         * @private
         */
        reload._flush = function (next) {

            if (changed.length) {
                utils.emitStreamChangedEvent(emitter, changed);
            }

            next();
        };

        return reload;
    }

    return browserSyncThroughStream;
};
