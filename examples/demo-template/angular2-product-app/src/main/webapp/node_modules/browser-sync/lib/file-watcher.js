"use strict";

var _ = require("lodash");

/**
 * Plugin interface
 * @returns {*|function(this:exports)}
 */
module.exports.plugin = function (bs) {

    var options = bs.options;
    var emitter = bs.emitter;

    var defaultWatchOptions = options.get("watchOptions").toJS();

    return options.get("files").reduce(function (map, glob, namespace) {

        /**
         * Default CB when not given
         * @param event
         * @param path
         */
        var fn = function (event, path) {
            emitter.emit("file:changed", {
                event: event,
                path: path,
                namespace: namespace
            });
        };

        var jsItem = glob.toJS();

        if (jsItem.globs.length) {
            var watcher = watch(jsItem.globs, defaultWatchOptions, fn);
            map[namespace] = {
                watchers: [watcher]
            };
        }

        if (jsItem.objs.length) {
            jsItem.objs.forEach(function (item) {
                if (!_.isFunction(item.fn)) {
                    item.fn = fn;
                }
                var watcher = watch(item.match, item.options || defaultWatchOptions, item.fn.bind(bs.publicInstance));
                if (!map[namespace]) {
                    map[namespace] = {
                        watchers: [watcher]
                    };
                } else {
                    map[namespace].watchers.push(watcher);
                }
            });
        }

        return map;

    }, {});
};

/**
 * @param patterns
 * @param opts
 * @param cb
 * @returns {*}
 */
function watch (patterns, opts, cb) {

    if (typeof opts === "function") {
        cb = opts;
        opts = {};
    }

    var watcher = require("chokidar")
        .watch(patterns, opts);

    if (_.isFunction(cb)) {
        watcher.on("all", cb);
    }

    return watcher;
}

module.exports.watch = watch;
