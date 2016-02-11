"use strict";

var UI          = require("./lib/UI");
var config      = require("./lib/config");
var Events      = require("events").EventEmitter;

/**
 * Hooks are for attaching functionality to BrowserSync
 */
module.exports.hooks = {
    /**
     * Client JS is added to each connected client
     */
    "client:js": fileContent(config.defaults.clientJs)
};

/**
 * BrowserSync Plugin interface
 * @param {Object} opts
 * @param {BrowserSync} bs
 * @param {Function} cb
 * @returns {UI}
 */
module.exports["plugin"] = function (opts, bs, cb) {
    var ui = new UI(opts, bs, new Events());
    bs.setOption("session", new Date().getTime());
    ui.cb = cb || function () { /*noop*/ };
    ui.init();
    return ui;
};

module.exports["plugin:name"]       = config.defaults.pluginName;

/**
 * @param filepath
 * @returns {*}
 */
function getPath (filepath) {
    return require("path").join(__dirname, filepath);
}

/**
 * @param filepath
 * @returns {*}
 */
function fileContent (filepath) {
    return require("fs").readFileSync(getPath(filepath));
}