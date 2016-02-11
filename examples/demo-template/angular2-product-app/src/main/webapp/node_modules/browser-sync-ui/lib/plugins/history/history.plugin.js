var historyPlugin = require("./history");

const PLUGIN_NAME = "History";

/**
 * @type {{plugin: Function, plugin:name: string, markup: string}}
 */
module.exports = {
    /**
     * @param ui
     * @param bs
     */
    "plugin": function (ui, bs) {
        ui.history = historyPlugin.init(ui, bs);
    },
    /**
     * Hooks
     */
    "hooks": {
        "markup": fileContent("history.html"),
        "client:js": fileContent("/history.client.js"),
        "templates": [
            getPath("/history.directive.html")
        ],
        "page": {
            path: "/history",
            title: PLUGIN_NAME,
            template: "history.html",
            controller: PLUGIN_NAME + "Controller",
            order: 3,
            icon: "list2"
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
    return require("fs").readFileSync(getPath(filepath), "utf-8");
}