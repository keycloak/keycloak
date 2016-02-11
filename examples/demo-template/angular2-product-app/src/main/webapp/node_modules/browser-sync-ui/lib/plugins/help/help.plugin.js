const PLUGIN_NAME = "Help / About";

/**
 * @type {{plugin: Function, plugin:name: string, markup: string}}
 */
module.exports = {
    /**
     * Plugin init
     */
    "plugin": function () {},
    /**
     * Hooks
     */
    "hooks": {
        "markup": fileContent("/../../../static/content/help.content.html"),
        "client:js": fileContent("/help.client.js"),
        "templates": [
            getPath("/help.directive.html")
        ],
        "page": {
            path: "/help",
            title: PLUGIN_NAME,
            template: "help.html",
            controller: "HelpAboutController",
            order: 6,
            icon: "help"
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