/**
 * tFunk for colours/compiler
 */
var tfunk = require("tfunk");

/**
 * Lodash for utils
 */
var _     = require("lodash");

/**
 * opt-merger for option merging
 */
var merge = require("opt-merger");

/**
 * Default configuration.
 * Can be overridden in first constructor arg
 */
var defaults = {

    /**
     * Initial log level
     */
    level: "info",

    /**
     * Prefix for logger
     */
    prefix: "",

    /**
     * Available levels and their score
     */
    levels: {
        "trace": 100,
        "debug": 200,
        "warn":  300,
        "info":  400,
        "error": 500
    },

    /**
     * Default prefixes
     */
    prefixes: {
        "trace": "[trace] ",
        "debug": "{yellow:[debug]} ",
        "info":  "{cyan:[info]} ",
        "warn":  "{magenta:[warn]} ",
        "error": "{red:[error]} "
    },

    /**
     * Should easy log statement be prefixed with the level?
     */
    useLevelPrefixes: false
};


/**
 * @param {Object} config
 * @constructor
 */
var Logger = function(config) {

    if (!(this instanceof Logger)) {
        return new Logger(config);
    }

    config = config || {};

    this._mute = false;
    this.config = merge.set({simple: true}).merge(defaults, config);
    this.addLevelMethods(this.config.levels);
    this.compiler = new tfunk.Compiler(this.config.custom || {}, this.config);
    this._memo = {};

    return this;
};

/**
 * Set an option once
 * @param path
 * @param value
 */
Logger.prototype.setOnce = function (path, value) {

    if (!_.isUndefined(this.config[path])) {

        if (_.isUndefined(this._memo[path])) {
            this._memo[path] = this.config[path];
        }

        this.config[path] = value;
    }

    return this;
};
/**
 * Add convenience method such as
 * logger.warn("msg")
 * logger.error("msg")
 * logger.info("msg")
 *
 * instead of
 * logger.log("warn", "msg");
 * @param items
 */
Logger.prototype.addLevelMethods = function (items) {
    Object.keys(items).forEach(function (item) {
        if (!this[item]) {
            this[item] = function () {
                var args = Array.prototype.slice.call(arguments);
                this.log.apply(this, args);
                return this;
            }.bind(this, item);
        }
    }, this);
};
/**
 * Reset the state of the logger.
 * @returns {Logger}
 */
Logger.prototype.reset = function () {

    this.setLevel(defaults.level)
        .setLevelPrefixes(defaults.useLevelPrefixes)
        .mute(false);

    return this;
};

/**
 * @param {String} level
 * @returns {boolean}
 */
Logger.prototype.canLog = function (level) {
    return this.config.levels[level] >= this.config.levels[this.config.level] && !this._mute;
};

/**
 * Log to the console with prefix
 * @param {String} level
 * @param {String} msg
 * @returns {Logger}
 */
Logger.prototype.log = function (level, msg) {

    var args = Array.prototype.slice.call(arguments);

    this.logOne(args, msg, level);

    return this;
};

/**
 * Set the log level
 * @param {String} level
 * @returns {Logger}
 */
Logger.prototype.setLevel = function (level) {

    this.config.level = level;

    return this;
};

/**
 * @param {boolean} state
 * @returns {Logger}
 */
Logger.prototype.setLevelPrefixes = function (state) {

    this.config.useLevelPrefixes = state;

    return this;
};

/**
 * @param prefix
 */
Logger.prototype.setPrefix = function (prefix) {
    if (_.isString(prefix)) {
        this.compiler.prefix = this.compiler.compile(prefix, true);
    }
    if (_.isFunction(prefix)) {
        this.compiler.prefix = prefix;
    }
};

/**
 * @param {String} level
 * @param {String} msg
 * @returns {Logger}
 */
Logger.prototype.unprefixed = function (level, msg) {

    var args = Array.prototype.slice.call(arguments);

    this.logOne(args, msg, level, true);

    return this;
};

/**
 * @param {Array} args
 * @param {String} msg
 * @param {String} level
 * @param {boolean} [unprefixed]
 * @returns {Logger}
 */
Logger.prototype.logOne = function (args, msg, level, unprefixed) {

    if (!this.canLog(level)) {
        return;
    }

    args = args.slice(2);

    if (this.config.useLevelPrefixes && !unprefixed) {
        msg = this.config.prefixes[level] + msg;
    }

    msg = this.compiler.compile(msg, unprefixed);

    args.unshift(msg);

    console.log.apply(console, args);

    this.resetTemps();

    return this;
};

/**
 * Reset any temporary value
 */
Logger.prototype.resetTemps = function () {

    Object.keys(this._memo).forEach(function (key) {
        this.config[key] = this._memo[key];
    }, this);
};

/**
 * Mute the logger
 */
Logger.prototype.mute = function (bool) {

    this._mute = bool;
    return this;
};

/**
 * Clone the instance to share setup
 * @param opts
 * @returns {Logger}
 */
Logger.prototype.clone = function (opts) {

    var config = _.cloneDeep(this.config);

    if (typeof opts === "function") {
        config = opts(config) || {};
    } else {
        config = merge.set({simple: true}).merge(config, opts || {});
    }

    return new Logger(config);
};

module.exports.Logger  = Logger;
module.exports.compile = tfunk;