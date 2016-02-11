"use strict";

var _         = require("lodash");

var fileUtils = {
    /**
     * React to file-change events that occur on "core" namespace only
     * @param bs
     * @param data
     */
    changedFile: function (bs, data) {
        /**
         * If the event property is undefined, infer that it's a 'change'
         * event due the fact this handler is for emitter.emit("file:changed")
         */
        if (_.isUndefined(data.event)) {
            data.event = "change";
        }
        /**
         * Chokidar always sends an 'event' property - which could be
         * `add` `unlink` etc etc so we need to check for that and only
         * respond to 'change', for now.
         */
        if (data.event === "change") {
            if (!bs.paused && data.namespace === "core") {
                bs.events.emit("file:reload", fileUtils.getFileInfo(data, bs.options));
            }
        }
    },
    /**
     * @param data
     * @param options
     * @returns {{assetFileName: *, fileExtension: String}}
     */
    getFileInfo: function (data, options) {

        data.ext      = require("path").extname(data.path).slice(1);
        data.basename = require("path").basename(data.path);

        var obj = {
            ext:           data.ext,
            path:          data.path,
            basename:      data.basename,
            type:          "inject"
        };

        // RELOAD page
        if (!_.contains(options.get("injectFileTypes").toJS(), obj.ext)) {
            obj.url  = obj.path;
            obj.type = "reload";
        }

        obj.path = data.path;
        obj.log  = data.log;

        return obj;
    }
};

module.exports = fileUtils;
