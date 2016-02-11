var fs          = require("fs");
var path        = require("path");

var config      = require("./config");
var eachSeries  = require("async-each-series");
var asyncTasks  = require("./async-tasks");
var hooks       = require("./hooks");
var merge       = require("./opts").merge;

var defaultPlugins = {
    "sync-options":     require("./plugins/sync-options/sync-options.plugin"),
    "overview":         require("./plugins/overview/overview.plugin"),
    "history":          require("./plugins/history/history.plugin"),
    "plugins":          require("./plugins/plugins/plugins.plugin"),
    "remote-debug":     require("./plugins/remote-debug/remote-debug.plugin"),
    "help":             require("./plugins/help/help.plugin"),
    "connections":      require("./plugins/connections/connections.plugin"),
    "network-throttle": require("./plugins/network-throttle/network-throttle.plugin")
};

/**
 * @param {Object} opts - Any options specifically
 *                        passed to the control panel
 * @param {BrowserSync} bs
 * @param {EventEmitter} emitter
 * @constructor
 * @returns {UI}
 */
var UI = function (opts, bs, emitter) {

    var ui            = this;
    ui.bs             = bs;
    ui.config         = config.merge();
    ui.events         = emitter;
    ui.options        = merge(opts);
    ui.logger         = bs.getLogger(ui.config.get("pluginName"));
    ui.defaultPlugins = defaultPlugins;
    ui.listeners      = {};
    ui.clients        = bs.io.of(bs.options.getIn(["socket", "namespace"]));
    ui.socket         = bs.io.of(ui.config.getIn(["socket", "namespace"]));

    if (ui.options.get("logLevel")) {
        ui.logger.setLevel(ui.options.get("logLevel"));
    }

    /**
     *
     */
    ui.pluginManager = new bs.utils.easyExtender(defaultPlugins, hooks).init();

    /**
     * Transform/save data RE: plugins
     * @type {*}
     */
    ui.bsPlugins = require("./resolve-plugins")(bs.getUserPlugins());

    return ui;
};

/**
 * Detect an available port
 * @returns {UI}
 */
UI.prototype.init = function () {

    var ui = this;

    eachSeries(
        asyncTasks,
        taskRunner(ui),
        tasksComplete(ui)
    );

    return this;
};

/**
 * @param cb
 */
UI.prototype.getServer = function (cb) {
    var ui = this;
    if (ui.server) {
        return ui.server;
    }
    this.events.on("ui:running", function () {
        cb(null, ui.server);
    });
};

/**
 * @returns {Array}
 */
UI.prototype.getInitialTemplates = function () {
    var prefix = path.resolve(__dirname, "../templates/directives");
    return fs.readdirSync(prefix)
        .map(function (name) {
            return path.resolve(prefix, name);
        });
};

/**
 * @param event
 */
UI.prototype.delegateEvent = function (event) {

    var ui = this;
    var listeners = ui.listeners[event.namespace];

    if (listeners) {
        if (listeners.event) {
            listeners.event.call(ui, event);
        } else {
            if (event.event && listeners[event.event]) {
                listeners[event.event].call(ui, event.data);
            }
        }
    }
};

/**
 * @param cb
 */
UI.prototype.listen = function (ns, events) {
    var ui = this;
    if (Array.isArray(ns)) {
        ns = ns.join(":");
    }
    if (!ui.listeners[ns]) {
        ui.listeners[ns] = events;
    }
};

/**
 * @param name
 * @param value
 * @returns {Map|*}
 */
UI.prototype.setOption = function (name, value) {
    var ui     = this;
    ui.options = ui.options.set(name, value);
    return ui.options;
};

/**
 * @param path
 * @param value
 * @returns {Map|*}
 */
UI.prototype.setOptionIn = function (path, value) {
    this.options = this.options.setIn(path, value);
    return this.options;
};

/**
 * @param fn
 */
UI.prototype.setMany = function (fn) {
    this.options = this.options.withMutations(fn);
    return this.options;
};

/**
 * @param path
 * @returns {any|*}
 */
UI.prototype.getOptionIn = function (path) {
    return this.options.getIn(path);
};

/**
 * Run each setup task in sequence
 * @param ui
 * @returns {Function}
 */
function taskRunner (ui) {

    return function (item, cb) {

        ui.logger.debug("Starting Step: " + item.step);

        /**
         * Give each step access to the UI Instance
         */
        item.fn(ui, function (err, out) {
            if (err) {
                return cb(err);
            }
            if (out) {
                handleOut(ui, out);
            }
            ui.logger.debug("{green:Step Complete: " + item.step);
            cb();
        });
    };
}

/**
 * Setup tasks may return options or instance properties to be set
 * @param {UI} ui
 * @param {Object} out
 */
function handleOut (ui, out) {

    if (out.options) {
        Object.keys(out.options).forEach(function (key) {
            ui.options = ui.options.set(key, out.options[key]);
        });
    }

    if (out.optionsIn) {
        out.optionsIn.forEach(function (item) {
            ui.options = ui.options.setIn(item.path, item.value);
        });
    }

    if (out.instance) {
        Object.keys(out.instance).forEach(function (key) {
            ui[key] = out.instance[key];
        });
    }
}

/**
 * All async tasks complete at this point
 * @param ui
 */
function tasksComplete (ui) {

    return function (err) {

        /**
         * Log any error according to BrowserSync's Logging level
         */
        if (err) {
            ui.logger.setOnce("useLevelPrefixes", true).error(err.message || err);
        }

        /**
         * Running event
         */
        ui.events.emit("ui:running", {instance: ui, options: ui.options});

        /**
         * Finally call the user-provided callback
         */
        ui.cb(null, ui);
    };
}

module.exports = UI;