"use strict";

var connect      = require("connect");
var utils        = require("./utils.js");

/**
 * @param {BrowserSync} bs
 * @param scripts
 * @returns {*}
 */
module.exports = function createServer (bs, scripts) {

    var options            = bs.options;
    var server             = options.get("server");
    var middleware         = options.get("middleware");
    var basedir            = server.get("baseDir");
    var serveStaticOptions = server.get("serveStaticOptions");

    var app = connect();

    /**
     * Handle Old IE
     */
    app.use(utils.handleOldIE);

    /**
     * Serve the Client-side JS from both version and static paths
     */
    app.use(options.getIn(["scriptPaths", "versioned"]), scripts)
        .use(options.getIn(["scriptPaths", "path"]),     scripts);

    /**
     * Add directory middleware
     */
    if (server.get("directory")) {
        utils.addDirectory(app, basedir);
    }

    /**
     * Add snippet injection middleware
     * This also includes any additional middleware given from the user
     */
    app.use(bs.snippetMw.middleware);

    /**
     * Add user-provided middlewares
     */
    utils.addMiddleware(app, middleware);

    /**
     * Add Serve static middlewares
     */
    utils.addBaseDir(app, basedir, serveStaticOptions);

    /**
     * Add further Serve static middlewares for routes
     */
    if (server.get("routes")) {
        utils.addRoutes(app, server.get("routes").toJS());
    }

    /**
     * Finally, return the server + App
     */
    return utils.getServer(app, bs.options);
};
