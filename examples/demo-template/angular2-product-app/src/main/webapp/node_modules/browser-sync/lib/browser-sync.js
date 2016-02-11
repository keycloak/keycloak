"use strict";

var hooks           = require("./hooks");
var asyncTasks      = require("./async-tasks");
var config          = require("./config");
var connectUtils    = require("./connect-utils");
var utils           = require("./utils");
var logger          = require("./logger");

var eachSeries      = require("async-each-series");
var _               = require("lodash");
var EE              = require("easy-extender");

/**
 * Required internal plugins.
 * Any of these can be overridden by deliberately
 * causing a name-clash.
 */
var defaultPlugins = {
    "logger":        logger,
    "socket":        require("./sockets"),
    "file:watcher":  require("./file-watcher"),
    "server":        require("./server"),
    "tunnel":        require("./tunnel"),
    "client:script": require("browser-sync-client"),
    "UI":            require("browser-sync-ui")
};

/**
 * @constructor
 */
var BrowserSync = function (emitter) {

    var bs      = this;

    bs.cwd      = process.cwd();
    bs.active   = false;
    bs.paused   = false;
    bs.config   = config;
    bs.utils    = utils;
    bs.events   = bs.emitter = emitter;

    bs._userPlugins   = [];
    bs._reloadQueue   = [];
    bs._cleanupTasks  = [];
    bs._browserReload = false;

    // Plugin management
    bs.pluginManager = new EE(defaultPlugins, hooks);
};

/**
 * Call a user-options provided callback
 * @param name
 */
BrowserSync.prototype.callback = function (name) {

    var bs  = this;
    var cb  = bs.options.getIn(["callbacks", name]);

    if (_.isFunction(cb)) {
        cb.apply(bs.publicInstance, _.toArray(arguments).slice(1));
    }
};

/**
 * @param {Map} options
 * @param {Function} cb
 * @returns {BrowserSync}
 */
BrowserSync.prototype.init = function (options, cb) {

    /**
     * Safer access to `this`
     * @type {BrowserSync}
     */
    var bs = this;

    /**
     * Set user-provided callback, or assign a noop
     * @type {Function}
     */
    bs.cb  = cb || utils.defaultCallback;

    /**
     * Verify provided config.
     * Some options are not compatible and will cause us to
     * end the process.
     */
    if (!utils.verifyConfig(options, bs.cb)) {
        return;
    }

    /**
     * Save a reference to the original options
     * @type {Map}
     * @private
     */
    bs._options = options;

    /**
     * Set additional options that depend on what the
     * user may of provided
     * @type {Map}
     */
    bs.options  = require("./options").update(options);

    /**
     * Kick off default plugins.
     */
    bs.pluginManager.init();

    /**
     * Create a base logger & debugger.
     */
    bs.logger   = bs.pluginManager.get("logger")(bs.events, bs);
    bs.debugger = bs.logger.clone({useLevelPrefixes: true});
    bs.debug    = bs.debugger.debug;

    /**
     * Run each setup task in sequence
     */
    eachSeries(
        asyncTasks,
        taskRunner(bs),
        tasksComplete(bs)
    );

    return this;
};

/**
 * Run 1 setup task.
 * Each task is a pure function.
 * They can return options or instance properties to set,
 * but they cannot set them directly.
 * @param {BrowserSync} bs
 * @returns {Function}
 */
function taskRunner (bs) {

    return function (item, cb) {

        bs.debug("-> {yellow:Starting Step: " + item.step);

        /**
         * Execute the current task.
         */
        item.fn(bs, executeTask);

        function executeTask(err, out) {

            /**
             * Exit early if any task returned an error.
             */
            if (err) {
                return cb(err);
            }

            /**
             * Act on return values (such as options to be set,
             * or instance properties to be set
             */
            if (out) {
                handleOut(bs, out);
            }

            bs.debug("+  {green:Step Complete: " + item.step);

            cb();
        }
    };
}

/**
 * @param bs
 * @param out
 */
function handleOut (bs, out) {
    /**
     * Set a single/many option.
     */
    if (out.options) {
        setOptions(bs, out.options);
    }

    /**
     * Any options returned that require path access?
     */
    if (out.optionsIn) {
        out.optionsIn.forEach(function (item) {
            bs.setOptionIn(item.path, item.value);
        });
    }

    /**
     * Any instance properties returned?
     */
    if (out.instance) {
        Object.keys(out.instance).forEach(function (key) {
            bs[key] = out.instance[key];
        });
    }
}

/**
 * Update the options Map
 * @param bs
 * @param options
 */
function setOptions (bs, options) {

    /**
     * If multiple options were set, act on the immutable map
     * in an efficient way
     */
    if (Object.keys(options).length > 1) {
        bs.setMany(function (item) {
            Object.keys(options).forEach(function (key) {
                item.set(key, options[key]);
                return item;
            });
        });
    } else {
        Object.keys(options).forEach(function (key) {
            bs.setOption(key, options[key]);
        });
    }
}

/**
 * At this point, ALL async tasks have completed
 * @param {BrowserSync} bs
 * @returns {Function}
 */
function tasksComplete (bs) {

    return function (err) {

        if (err) {
            bs.logger.setOnce("useLevelPrefixes", true).error(err.message);
        }

        /**
         * Set active flag
         */
        bs.active = true;

        /**
         * @deprecated
         */
        bs.events.emit("init", bs);

        /**
         * This is no-longer needed as the Callback now only resolves
         * when everything (including slow things, like the tunnel) is ready.
         * It's here purely for backwards compatibility.
         * @deprecated
         */
        bs.events.emit("service:running", {
            options: bs.options,
            baseDir: bs.options.getIn(["server", "baseDir"]),
            type:    bs.options.get("mode"),
            port:    bs.options.get("port"),
            url:     bs.options.getIn(["urls", "local"]),
            urls:    bs.options.get("urls").toJS(),
            tunnel:  bs.options.getIn(["urls", "tunnel"])
        });

        /**
         * Call any option-provided callbacks
         */
        bs.callback("ready", null, bs);

        /**
         * Finally, call the user-provided callback given as last arg
         */
        bs.cb(null, bs);
    };
}

/**
 * @param module
 * @param opts
 * @param cb
 */
BrowserSync.prototype.registerPlugin = function (module, opts, cb) {

    var bs = this;

    bs.pluginManager.registerPlugin(module, opts, cb);

    if (module["plugin:name"]) {
        bs._userPlugins.push(module);
    }
};

/**
 * Get a plugin by name
 * @param name
 */
BrowserSync.prototype.getUserPlugin = function (name) {

    var bs = this;

    var items = bs.getUserPlugins(function (item) {
        return item["plugin:name"] === name;
    });

    if (items && items.length) {
        return items[0];
    }

    return false;
};

/**
 * @param {Function} [filter]
 */
BrowserSync.prototype.getUserPlugins = function (filter) {

    var bs = this;

    filter = filter || function () {
        return true;
    };

    /**
     * Transform Plugins option
     */
    bs.userPlugins = bs._userPlugins.filter(filter).map(function (plugin) {
        return {
            name: plugin["plugin:name"],
            active: plugin._enabled,
            opts: bs.pluginManager.pluginOptions[plugin["plugin:name"]]
        };
    });

    return bs.userPlugins;
};

/**
 * Get middleware
 * @returns {*}
 */
BrowserSync.prototype.getMiddleware = function (type) {

    var types = {
        "connector": connectUtils.socketConnector(this.options),
        "socket-js": require("./snippet").utils.getSocketScript()
    };

    if (type in types) {
        return function (req, res) {
            res.setHeader("Content-Type", "text/javascript");
            res.end(types[type]);
        };
    }
};

/**
 * Shortcut for pushing a file-serving middleware
 * onto the stack
 * @param {String} path
 * @param {{type: string, content: string}} props
 */
BrowserSync.prototype.serveFile = function (path, props) {

    var bs = this;

    if (bs.app) {
        bs.app.use(path, function (req, res) {
            res.setHeader("Content-Type", props.type);
            res.end(props.content);
        });
    }
};

/**
 * Add middlewares on the fly
 * @param route
 * @param handle
 * @param opts
 */
BrowserSync.prototype.addMiddleware = function (route, handle, opts) {

    var bs   = this;

    if (!bs.app) {
        return;
    }

    var mode = bs.options.get("mode");

    opts = opts || {};

    if (!opts.id) {
        opts.id = "bs-mw-" + Math.random();
    }

    if (route === "*") {
        route = "";
    }

    if (opts.override) {
        return bs.app.stack.unshift({id: opts.id, route: route, handle: handle});
    }

    if (mode === "proxy") {
        return bs.app.use(route, handle, opts);
    }

    return bs.app.stack.push({
        id: opts.id,
        route: route,
        handle: handle
    }); // function + route;
};

/**
 * Remove middlewares on the fly
 * @param {String} id
 * @returns {Server}
 */
BrowserSync.prototype.removeMiddleware = function (id) {

    var bs = this;

    if (!bs.app) {
        return;
    }

    bs.app.stack = bs.app.stack.filter(function (item) {
        if (!item.id) {
            return true;
        }

        return item.id !== id;
    });

    return bs.app;
};

/**
 * Middleware for socket connection (external usage)
 * @param opts
 * @returns {*}
 */
BrowserSync.prototype.getSocketConnector = function (opts) {

    var bs = this;

    return function (req, res) {
        res.setHeader("Content-Type", "text/javascript");
        res.end(bs.getExternalSocketConnector(opts));
    };
};

/**
 * Socket connector as a string
 * @param {Object} opts
 * @returns {*}
 */
BrowserSync.prototype.getExternalSocketConnector = function (opts) {

    var bs = this;

    return connectUtils.socketConnector(
        bs.options.withMutations(function (item) {
            item.set("socket", item.get("socket").merge(opts));
            if (!bs.options.getIn(["proxy", "ws"])) {
                item.set("mode", "snippet");
            }
        })
    );
};

/**
 * Socket io as string (for embedding)
 * @returns {*}
 */
BrowserSync.prototype.getSocketIoScript = function () {

    return require("./snippet").utils.getSocketScript();
};

/**
 * Callback helper
 * @param name
 */
BrowserSync.prototype.getOption = function (name) {

    this.debug("Getting option: {magenta:%s", name);
    return this.options.get(name);
};

/**
 * Callback helper
 * @param path
 */
BrowserSync.prototype.getOptionIn = function (path) {

    this.debug("Getting option via path: {magenta:%s", path);
    return this.options.getIn(path);
};

/**
 * @returns {BrowserSync.options}
 */
BrowserSync.prototype.getOptions = function () {
    return this.options;
};

/**
 * @returns {BrowserSync.options}
 */
BrowserSync.prototype.getLogger = logger.getLogger;

/**
 * @param {String} name
 * @param {*} value
 * @returns {BrowserSync.options|*}
 */
BrowserSync.prototype.setOption = function (name, value, opts) {

    var bs = this;

    opts = opts || {};

    bs.debug("Setting Option: {cyan:%s} - {magenta:%s", name, value.toString());

    bs.options = bs.options.set(name, value);

    if (!opts.silent) {
        bs.events.emit("options:set", {path: name, value: value, options: bs.options});
    }
    return this.options;
};

/**
 * @param path
 * @param value
 * @param opts
 * @returns {Map|*|BrowserSync.options}
 */
BrowserSync.prototype.setOptionIn = function (path, value, opts) {

    var bs = this;

    opts = opts || {};

    bs.debug("Setting Option: {cyan:%s} - {magenta:%s", path.join("."), value.toString());
    bs.options = bs.options.setIn(path, value);
    if (!opts.silent) {
        bs.events.emit("options:set", {path: path, value: value, options: bs.options});
    }
    return bs.options;
};

/**
 * Set multiple options with mutations
 * @param fn
 * @param opts
 * @returns {Map|*}
 */
BrowserSync.prototype.setMany = function (fn, opts) {

    var bs = this;

    opts = opts || {};

    bs.debug("Setting multiple Options");
    bs.options = bs.options.withMutations(fn);
    if (!opts.silent) {
        bs.events.emit("options:set", {options: bs.options.toJS()});
    }
    return this.options;
};

/**
 * Remove a rewrite rule by id
 */
BrowserSync.prototype.removeRewriteRule = function (id) {
    var bs = this;

    bs.setRewriteRules(bs.rewriteRules.filter(fn));

    function fn (item) {
        if (item.id) {
            return item.id !== id;
        }
        return true;
    }
};

/**
 * Add a new rewrite rule to the stack
 * @param {Object} rule
 */
BrowserSync.prototype.addRewriteRule = function (rule) {
    var bs = this;

    bs.setRewriteRules(bs.rewriteRules.concat(rule));
};

/**
 * Completely replace all rules
 * @param {Array} rules
 */
BrowserSync.prototype.setRewriteRules = function (rules) {
    var bs = this;

    bs.rewriteRules = rules;

    if (bs.options.get("mode") === "server") {
        bs.snippetMw.opts.rules = rules;
    }

    if (bs.options.get("mode") === "proxy") {
        bs.proxy.config.rules = rules;
    }
};

/**
 * Handle Browser Reloads
 */
BrowserSync.prototype.doBrowserReload = function () {

    var bs = this;

    if (bs._browserReload) {
        return;
    }
    bs._browserReload = setTimeout(function () {
        bs.io.sockets.emit("browser:reload");
        clearTimeout(bs._browserReload);
        bs._browserReload = false;
    }, bs.options.get("reloadDelay"));
};

/**
 * Handle a queue of reloads
 * @param {Object} data
 */
BrowserSync.prototype.doFileReload = function (data) {

    var bs = this;

    bs._reloadQueue = bs._reloadQueue || [];
    bs._reloadQueue.push(data);

    if (bs._reloadTimer) {
        return;
    }

    var willReload = utils.willCauseReload(
        bs._reloadQueue.map(function (item) { return item.path; }),
        bs.options.get("injectFileTypes").toJS()
    );

    bs._reloadTimer = setTimeout(function () {

        if (willReload) {
            if (!bs._reloadDebounced) {
                bs._reloadDebounced = setTimeout(function () {
                    bs._reloadDebounced = false;
                }, bs.options.get("reloadDebounce"));
                bs.io.sockets.emit("browser:reload");
            }
        } else {
            bs._reloadQueue.forEach(function (item) {
                bs.io.sockets.emit("file:reload", item);
            });
        }

        clearTimeout(bs._reloadTimer);

        bs._reloadTimer = undefined;
        bs._reloadQueue = [];

    }, bs.options.get("reloadDelay"));
};

/**
 * @param fn
 */
BrowserSync.prototype.registerCleanupTask = function (fn) {

    this._cleanupTasks.push(fn);
};

/**
 * Instance Cleanup
 */
BrowserSync.prototype.cleanup = function (cb) {

    var bs = this;
    if (!bs.active) {
        return;
    }

    // Remove all event listeners
    if (bs.events) {
        bs.debug("Removing event listeners...");
        bs.events.removeAllListeners();
    }

    // Close any core file watchers
    if (bs.watchers) {
        Object.keys(bs.watchers).forEach(function (key) {
            bs.watchers[key].watchers.forEach(function (watcher) {
                watcher.close();
            });
        });
    }

    // Run any additional clean up tasks
    bs._cleanupTasks.forEach(function (fn) {
        if (_.isFunction(fn)) {
            fn(bs);
        }
    });

    // Reset the flag
    bs.debug("Setting {magenta:active: false");
    bs.active = false;
    bs.paused = false;

    bs.pluginManager.plugins        = {};
    bs.pluginManager.pluginOptions  = {};
    bs.pluginManager.defaultPlugins = defaultPlugins;

    bs._userPlugins                = [];
    bs.userPlugins                 = [];
    bs._reloadTimer                = undefined;
    bs._reloadQueue                = [];
    bs._cleanupTasks               = [];

    if (_.isFunction(cb)) {
        cb(null, bs);
    }
};

module.exports = BrowserSync;
