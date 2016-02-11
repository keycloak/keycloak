"use strict";

var path = require("path");
/**
 * @type {{controlPanel: {jsFile: string, baseDir: *}, socketIoScript: string, configFile: string, client: {shims: string}}}
 */
module.exports = {
    controlPanel: {
        jsFile: "/js/app.js",
        baseDir: path.join(__dirname, "control-panel")
    },
    templates: {
        scriptTag: path.join(__dirname, "templates/script-tags.tmpl"),
        connector: path.join(__dirname, "templates/connector.tmpl")
    },
    socketIoScript: "/public/socket.io.js",
    configFile: "default-config.js",
    userFile: "bs-config.js",
    template: "cli-template.js",
    httpProtocol: {
        path: "/__browser_sync__"
    },
    client: {
        shims: "/client/client-shims.js"
    },
    errors: {
        "server+proxy": "Invalid config. You cannot specify both server & proxy options.",
        "proxy+https":  "Invalid config. You set https: true, but your proxy target doesn't reflect this."
    }
};
