var weinre         = require("./weinre");
//var compression     = require("./compression");
//var noCachePlugin   = require("./no-cache");
var overlayPlugin  = require("./overlay-grid/overlay-grid");
var clientFiles    = require("./client-files");

const PLUGIN_NAME  = "Remote Debug";

/**
 * @type {{plugin: Function, plugin:name: string, markup: string}}
 */
module.exports = {
    /**
     * @param ui
     * @param bs
     */
    "plugin": function (ui, bs) {

        if (bs.options.get("scheme") === "https") {
            ui.setMany(function (item) {
                item.deleteIn(["clientFiles", "weinre"]);
            });
        } else {
            ui.weinre = weinre.init(ui);
        }

        ui.overlayGrid = overlayPlugin.init(ui, bs);

        //ui.noCache     = noCachePlugin.init(ui, bs);
        //ui.compression = compression.init(ui, bs);

        /**
         * Listen for file events
         */
        ui.listen("remote-debug:files", {
            "enableFile": function (file) {
                ui.enableElement(file);
            },
            "disableFile": function (file) {
                ui.disableElement(file);
            }
        });

        /**
         * Listen for weinre toggles
         */
        ui.listen("remote-debug:weinre", ui.weinre);

        /**
         * Listen for overlay-grid events
         */
        ui.listen("remote-debug:overlay-grid", ui.overlayGrid);
    },
    /**
     * Hooks
     */
    "hooks": {
        "markup": fileContent("remote-debug.html"),
        "client:js": [
            fileContent("/remote-debug.client.js"),
            fileContent("/overlay-grid/overlay-grid.client.js")
        ],
        "templates": [
            getPath("/overlay-grid/overlay-grid.html")
        ],
        "page": {
            path: "/remote-debug",
            title: PLUGIN_NAME,
            template: "remote-debug.html",
            controller: PLUGIN_NAME.replace(" ", "") + "Controller",
            order: 4,
            icon: "bug"
        },
        elements: clientFiles.files
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