"use strict";

var _            = require("lodash");
var Immutable    = require("immutable");
var snippetUtils = require("./snippet").utils;

module.exports = {

    /**
     *
     * @this {BrowserSync}
     * @returns {String}
     */
    "client:js": function (hooks, data) {

        var js  = snippetUtils.getClientJs(data.port, data.options);

        return hooks.reduce(function (joined, hook) {
            return joined += hook;
        }, js);
    },
    /**
     * @this {BrowserSync}
     * @returns {Array}
     */
    "client:events": function (hooks, clientEvents) {

        hooks.forEach(function (hook) {

            var result = hook(this);

            if (Array.isArray(result)) {
                clientEvents = _.union(clientEvents, result);
            } else {
                clientEvents.push(result);
            }
        }, this);

        return clientEvents;
    },
    /**
     * @returns {Array}
     */
    "server:middleware": function (hooks, initial) {

        initial = initial || [];

        _.each(hooks, function (hook) {

            var result = hook(this);

            if (Array.isArray(result)) {
                result.forEach(function (res) {
                    if (_.isFunction(res)) {
                        initial = initial.push(res);
                    }
                });

            } else {
                if (_.isFunction(result)) {
                    initial = initial.push(result);
                }
            }
        }, this);

        return initial;
    },
    /**
     * @param {Array} hooks
     * @param {Map|List} initial
     * @param pluginOptions
     * @returns {any}
     */
    "files:watch": function (hooks, initial, pluginOptions) {

        var opts;

        if (pluginOptions) {
            opts = Immutable.fromJS(pluginOptions);
            opts.forEach(function (value, key) {
                if (!value) {
                    return;
                }
                var files = value.get("files");
                if (files) {
                    var fileArg = require("./cli/cli-options").makeFilesArg(files);
                    if (fileArg) {
                        initial = initial.set(key, Immutable.fromJS(fileArg));
                    }
                }
            });
        }

        return initial;
    }
};
