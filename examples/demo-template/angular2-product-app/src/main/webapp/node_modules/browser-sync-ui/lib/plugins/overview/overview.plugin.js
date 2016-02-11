const PLUGIN_NAME = "Overview";

/**
 * @type {{plugin: Function, plugin:name: string, markup: string}}
 */
module.exports = {
    /**
     * Plugin init
     */
    "plugin": function () { /* noop */ },

    /**
     * Hooks
     */
    "hooks": {
        "markup": fileContent("/overview.html"),
        "client:js": fileContent("/overview.client.js"),
        "templates": [
            getPath("/snippet-info.html"),
            getPath("/url-info.html")
        ],
        "page": {
            path: "/",
            title: PLUGIN_NAME,
            template: "overview.html",
            controller: PLUGIN_NAME.replace(" ", "") + "Controller",
            order: 1,
            icon: "cog"
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