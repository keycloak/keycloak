var fs         = require("fs");
var path       = require("path");

var pluginTmpl     = templateFile("/plugin.tmpl");
var configTmpl     = templateFile("/config.tmpl");
var configItem     = templateFile("/config.item.tmpl");
var inlineTemp     = templateFile("/inline.template.tmpl");
var pluginItemTmpl = fs.readFileSync(path.resolve(__dirname, "../", "templates/plugin.item.tmpl"), "utf-8");

function templateFile (filepath) {
    return fs.readFileSync(path.join(__dirname, "/../templates", filepath || ""), "utf-8");
}

/**
 * @type {{page: Function, markup: Function, client:js: Function, templates: Function}}
 */
module.exports = {
    /**
     * Create the url config for each section of the ui
     * @param hooks
     * @param ui
     */
    "page": function (hooks, ui) {

        var config = hooks
            .map(transformConfig)
            .reduce(createConfigItem, {});

        return {
            /**
             * pagesConfig - This is the angular configuration such as routes
             */
            pagesConfig: configTmpl
                .replace("%when%", hooks.reduce(
                    createAngularRoutes,
                    ""
                ))
                .replace("%pages%", JSON.stringify(
                    config,
                    null,
                    4
                )),
            /**
             * pagesConfig in object form
             */
            pagesObj: config,
            pageMarkup: function () {
                return preAngular(ui.pluginManager.plugins, config, ui);
            }
        };
    },
    /**
     * Controller markup for each plugin
     * @param hooks
     * @returns {*}
     */
    "markup": function (hooks) {
        return hooks.reduce(pluginTemplate, "");
    },
    /**
     * @param hooks
     * @param {UI} ui
     * @returns {*|string}
     */
    "client:js": function (hooks, ui) {

        /**
         * Add client JS from BrowserSync Plugins
         */
        ui.bsPlugins.forEach(function (plugin) {
            if (plugin.has("client:js")) {
                plugin.get("client:js").forEach(function (value) {
                    hooks.push(value);
                });
            }
        });

        var out = hooks.reduce(function (all, item) {
            if (typeof item === "string") {
                all += ";" + item;
            } else if (Array.isArray(item)) {
                item.forEach(function (item) {
                    all += ";" + item;
                });
            }
            return all;
        }, "");

        return out;
    },
    /**
     * @param hooks
     * @param initial
     * @param {UI} ui
     * @returns {String}
     */
    "templates": function (hooks, initial, ui) {

        /**
         * Add templates from each Browsersync registered plugin
         * @type {string}
         */
        var pluginDirectives = ui.bsPlugins.reduce(function (all, plugin) {

            if (!plugin.has("templates")) {
                return all;
            }

            /**
             * Slugify-ish the plugin name
             *  eg: Test Browsersync Plugin
             *    = test-browsersync-plugin
             * @type {string}
             */
            var slug = plugin.get("name")
                .trim()
                .split(" ")
                .map(function (word) {
                    return word.trim().toLowerCase();
                })
                .join("-");

            /**
             * For every plugin that has templates, wrap
             * the markup in the <script type="text/ng-template" id="{{slug}}"></script>
             * markup to result in the single output string.
             */
            plugin.get("templates").forEach(function (value, key) {
                all +=  angularWrap([slug, path.basename(key)].join("/"), value);
            });

            return all;

        }, "");

        /**
         * Combine the markup from the plugins done above with any
         * others registered via hooks + initial
         * to create the final markup
         */
        return [pluginDirectives, createInlineTemplates(hooks.concat([initial]))].join("");
    },
    /**
     * Allow plugins to register toggle-able elements
     * @param hooks
     * @returns {{}}
     */
    "elements": function (hooks) {
        var obj = {};
        hooks.forEach(function (elements) {
            elements.forEach(function (item) {
                if (!obj[item.name]) {
                    obj[item.name] = item;
                }
            });
        });
        return obj;
    }
};

/**
 * @param hooks
 * @returns {String}
 */
function createInlineTemplates (hooks) {
    return hooks.reduce(function (combined, item) {
        return combined + item.reduce(function (all, filepath) {
            return all + angularWrap(
                path.basename(filepath),
                fs.readFileSync(filepath));
        }, "");
    }, "");
}

/**
 * @param item
 * @returns {*}
 */
function transformConfig (item) {
    return item;
}

/**
 * @param {String} all
 * @param {Object} item
 * @returns {*}
 */
function createAngularRoutes(all, item) {
    return all + configItem.replace(/%(.+)%/g, function () {
        var key = arguments[1];
        if (item[key]) {
            return item[key];
        }
    });
}

/**
 * @param joined
 * @param item
 * @returns {*}
 */
function createConfigItem (joined, item) {
    if (item.path === "/") {
        joined["overview"] = item;
    } else {
        joined[item.path.slice(1)] = item;
    }
    return joined;
}

/**
 * @returns {*}
 */
function pluginTemplate (combined, item) {
    return [combined, pluginTmpl.replace("%markup%", item)].join("\n");
}

/**
 * @param plugins
 * @param config
 * @returns {*}
 */
function preAngular (plugins, config, ui) {

    return Object.keys(plugins)
        .filter(function (key) {
            return config[key]; // only work on plugins that have pages
        })
        .map(function (key) {
            if (key === "plugins") {
                var pluginMarkup = ui.bsPlugins.reduce(function (all, item, i) {
                    all += pluginItemTmpl
                        .replace("%content%", item.get("markup") || "")
                        .replace(/%index%/g, i)
                        .replace(/%name%/g, item.get("name"));

                    return all;
                }, "");
                plugins[key].hooks.markup = plugins[key].hooks.markup.replace("%pluginlist%", pluginMarkup);
            }
            return angularWrap(config[key].template, bindOnce(plugins[key].hooks.markup, config[key]));
        })
        .reduce(function (combined, item) {
            return combined + item;
        }, "");
}

/**
 * @param templateName
 * @param markup
 * @returns {*}
 */
function angularWrap (templateName, markup) {
    return inlineTemp
        .replace("%content%", markup)
        .replace("%id%", templateName);
}

/**
 * @param markup
 * @param config
 * @returns {*|string}
 */
function bindOnce (markup, config) {
    return markup.toString().replace(/\{\{ctrl.section\.(.+?)\}\}/g, function ($1, $2) {
        return config[$2] || "";
    });
}

module.exports.bindOnce = bindOnce;

