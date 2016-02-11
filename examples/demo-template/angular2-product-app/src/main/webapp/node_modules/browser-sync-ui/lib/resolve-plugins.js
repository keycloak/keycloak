var fs          = require("fs");
var path        = require("path");
var Immutable   = require("immutable");

/**
 * Take BrowserSync plugins and determine if
 * any UI is provided by looking at data in the the
 * modules package.json file
 * @param plugins
 * @returns {*}
 */
module.exports = function (plugins) {
    return require("immutable")
        .fromJS(plugins)
        /**
         * Exclude the UI
         */
        .filter(function (plugin) {
            return plugin.get("name") !== "UI";
        })
        /**
         * Attempt to retrieve a plugins package.json file
         */
        .map(function (plugin) {

            var moduleName = plugin.getIn(["opts", "moduleName"]);
            var pkg = {};

            if (!moduleName) {
                return plugin;
            }

            try {
                pkg = require("immutable").fromJS(require(path.join(moduleName, "package.json")));
            } catch (e) {
                console.error(e);
                return plugin;
            }

            plugin = plugin.set("pkg", pkg);

            return plugin.set("relpath", path.dirname(require.resolve(moduleName)));
        })
        /**
         * Try to load markup for each plugin
         */
        .map(function (plugin) {

            if (!plugin.hasIn(["pkg", "browser-sync:ui"])) {
                return plugin;
            }

            var markup    = plugin.getIn(["pkg", "browser-sync:ui", "hooks", "markup"]);

            if (markup) {
                plugin = plugin.set("markup", fs.readFileSync(path.resolve(plugin.get("relpath"), markup), "utf8"));
            }

            return plugin;
        })
        /**
         * Load any template files for the plugin
         */
        .map(function (plugin) {

            if (!plugin.hasIn(["pkg", "browser-sync:ui"])) {
                return plugin;
            }

            return resolveIfPluginHas(["pkg", "browser-sync:ui", "hooks", "templates"], "templates", plugin);
        })
        /**
         * Try to load Client JS for each plugin
         */
        .map(function (plugin) {

            if (!plugin.hasIn(["pkg", "browser-sync:ui"])) {
                return plugin;
            }

            return resolveIfPluginHas(["pkg", "browser-sync:ui", "hooks", "client:js"], "client:js", plugin);
        });
};

/**
 * If a plugin contains this option path, resolve/read the files
 * @param {Array} optPath - How to access the collection
 * @param {String} propName - Key for property access
 * @param {Immutable.Map} plugin
 * @returns {*}
 */
function resolveIfPluginHas(optPath, propName, plugin) {
    var opt = plugin.getIn(optPath);
    if (opt.size) {
        return plugin.set(
            propName,
            resolvePluginFiles(opt, plugin.get("relpath"))
        );
    }
    return plugin;
}

/**
 * Read & store a file from a plugin
 * @param {Array|Immutable.List} collection
 * @param {String} relPath
 * @returns {any}
 */
function resolvePluginFiles (collection, relPath) {
    return Immutable.fromJS(collection.reduce(function (all, item) {
        var full = path.join(relPath, item);
        if (fs.existsSync(full)) {
            all[full] = fs.readFileSync(full, "utf8");
        }
        return all;
    }, {}));
}