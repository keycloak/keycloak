const PLUGIN_NAME = "Sync Options";

/**
 * @type {{plugin: Function, plugin:name: string, hooks: object}}
 */
module.exports = {

    "plugin": function (ui, bs) {

        ui.listen("sync-options", {

            "set": function (data) {

                ui.logger.debug("Setting option: {magenta:%s}:{cyan:%s}", data.path.join("."), data.value);
                bs.setOptionIn(data.path, data.value);

            },

            "setMany": function (data) {

                ui.logger.debug("Setting Many options...");

                if (data.value !== true) {
                    data.value = false;
                }

                bs.setMany(function (item) {
                    [
                        ["codeSync"],
                        ["ghostMode", "clicks"],
                        ["ghostMode", "scroll"],
                        ["ghostMode", "forms", "inputs"],
                        ["ghostMode", "forms", "toggles"],
                        ["ghostMode", "forms", "submit"]
                    ].forEach(function (option) {
                            item.setIn(option, data.value);
                        });
                });

                return bs;
            }
        });
    },
    "hooks": {
        "markup": fileContent("sync-options.html"),
        "client:js": fileContent("sync-options.client.js"),
        "templates": [],
        "page": {
            path: "/sync-options",
            title: PLUGIN_NAME,
            template: "sync-options.html",
            controller: PLUGIN_NAME.replace(" ", "") + "Controller",
            order: 2,
            icon: "sync"
        }
    },
    "plugin:name": PLUGIN_NAME
};

function getPath (filepath) {
    return require("path").join(__dirname, filepath);
}

function fileContent (filepath) {
    return require("fs").readFileSync(getPath(filepath), "utf-8");
}
