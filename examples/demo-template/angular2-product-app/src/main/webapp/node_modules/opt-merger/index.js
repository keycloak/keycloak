var _    = require("lodash");
var args = require("minimist")(process.argv.slice(2));

/**
 * @type {{ignoreCli: boolean, simple: boolean}}
 * @private
 * @const
 */
const _opts = {
    ignoreCli  : false,
    simple     : false
};

/**
 * Make a clone of _opts per scope
 * @returns {*}
 */
function getOpts() {
    return _.cloneDeep(_opts);
}

/**
 * Allow options to be set & return a function with
 * partially applied args.
 * @param obj
 */
module.exports.set = function (obj) {
    return {
        merge: merge.bind(null, _.merge(getOpts(), obj))
    }
};

/**
 * @param {Object} defaults  - your default configuration
 * @param {Object} config    - the values to merge
 * @param {Object} callbacks - Any callbacks to handle the merge
 * @returns {Object}
 */
module.exports.merge = function (defaults, config, callbacks) {
    return merge(getOpts(), defaults, config, callbacks);
};

/**
 * @returns {argv|exports}
 */
module.exports.getArgs = function () {
    return args;
};

/**
 * @param opts
 * @param defaults
 * @param config
 * @param callbacks
 * @returns {*}
 */
function merge(opts, defaults, config, callbacks) {

    var toMerge;
    var commandLineArgs = exports.getArgs();

    defaults = _.cloneDeep(defaults);
    config   = _.cloneDeep(config);

    if ((config && Object.keys(config).length) || opts.simple || opts.ignoreCli) {
        toMerge = config;
    } else {
        toMerge = commandLineArgs;
    }

    var simpleMerged = _.merge(_.cloneDeep(defaults), toMerge, function (a, b) {
        return _.isArray(a) ? _.union(a, b) : undefined;
    });

    if (callbacks && Object.keys(callbacks).length) {

        return exports.mergeOptions(opts, defaults, simpleMerged, config, callbacks);

    } else {

        return opts.simple
            ? simpleMerged
            : _.merge(simpleMerged, commandLineArgs);

    }
}

/**
 * @returns {Object}
 */
module.exports.mergeOptions = function (opts, defaults, merged, config, callbacks) {

    var args = exports.getArgs();

    Object.keys(callbacks).forEach(function (item) {

        var newValue;

        if (!opts.ignoreCli && args && !_.isUndefined(args[item])) {
            newValue = args[item];
        } else {
            newValue = config[item];
        }

        if (_.isFunction(callbacks[item]) && !_.isUndefined(defaults[item])) {
            // there's a callback, a default ARG & a newValue
            merged[item] = callbacks[item](defaults[item], merged[item], newValue, opts.ignoreCli ? undefined : args, config);
        }
    });

    return merged;
};