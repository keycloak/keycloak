var networkThrottle = require("./network-throttle");

const PLUGIN_NAME = "Network Throttle";

/**
 * @type {{plugin: Function, plugin:name: string, markup: string}}
 */
module.exports = {
    /**
     * Plugin init
     */
    "plugin": function (ui, bs) {
        ui.throttle = networkThrottle.init(ui, bs);
        ui.listen("network-throttle", ui.throttle);
    },

    /**
     * Hooks
     */
    "hooks": {
        "markup": fileContent("/network-throttle.html"),
        "client:js": [fileContent("/network-throttle.client.js")],
        "templates": [],
        "page": {
            path: "/network-throttle",
            title: PLUGIN_NAME,
            template: "network-throttle.html",
            controller: "NetworkThrottleController",
            order: 5,
            icon: "time"
        }
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
    return require("fs").readFileSync(getPath(filepath));
}