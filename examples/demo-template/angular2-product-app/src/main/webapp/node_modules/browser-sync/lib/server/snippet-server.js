"use strict";

var connect = require("connect");
var utils   = require("./utils");

/**
 * Create a server for the snippet
 * @param {BrowserSync} bs
 * @param scripts
 * @returns {*}
 */
module.exports = function createSnippetServer (bs, scripts) {

    var app = connect();

    app.use(bs.options.getIn(["scriptPaths", "versioned"]), scripts)
       .use(bs.options.getIn(["scriptPaths", "path"]),      scripts);

    return utils.getServer(app, bs.options);
};
