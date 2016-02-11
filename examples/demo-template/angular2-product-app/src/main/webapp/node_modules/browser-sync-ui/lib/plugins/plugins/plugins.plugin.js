const PLUGIN_NAME = "Plugins";

/**
 * @type {{plugin: Function, plugin:name: string, markup: string}}
 */
module.exports = {
    /**
     * @param ui
     * @param bs
     */
    "plugin": function (ui, bs) {

        ui.listen("plugins", {

            "set": function (data) {
                bs.events.emit("plugins:configure", data);
            },

            "setMany": function (data) {

                if (data.value !== true) {
                    data.value = false;
                }

                bs.getUserPlugins()
                    .filter(function (item) {
                        return item.name !== "UI    "; // todo dupe code server/client
                    })
                    .forEach(function (item) {
                        item.active = data.value;
                        bs.events.emit("plugins:configure", item);
                    });
            }
        });
    },
    /**
     * Hooks
     */
    "hooks": {
        "markup": fileContent("plugins.html"),
        "client:js": fileContent("/plugins.client.js"),
        "templates": [
            //getPath("plugins.directive.html")
        ],
        "page": {
            path: "/plugins",
            title: PLUGIN_NAME,
            template: "plugins.html",
            controller: PLUGIN_NAME + "Controller",
            order: 4,
            icon: "plug"
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