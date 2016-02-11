var connections = require("./lib/connections");

const PLUGIN_NAME = "Connections";

/**
 * @type {{plugin: Function, plugin:name: string, markup: string}}
 */
module.exports = {
    /**
     * @param {UI} ui
     * @param {BrowserSync} bs
     */
    "plugin": function (ui, bs) {
        connections.init(ui, bs);
    },
    /**
     * Hooks
     */
    "hooks": {
        "client:js": fileContent("/connections.client.js"),
        "templates": [
            getPath("/connections.directive.html")
        ]
    },
    /**
     * Plugin name
     */
    "plugin:name": PLUGIN_NAME
};

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
    return require("fs").readFileSync(getPath(filepath), "utf-8");
}