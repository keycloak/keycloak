"use strict";

var path = require("path");
var url = require("url");
var _    = require("lodash");
var Immutable = require("immutable");
var isList = Immutable.List.isList;
var isMap = Immutable.Map.isMap;
var defaultConfig = require("../default-config");
var immDefs = Immutable.fromJS(defaultConfig);

var opts = exports;

/**
 * @type {{wrapPattern: Function}}
 */
opts.utils = {

    /**
     * Transform a string arg such as "*.html, css/*.css" into array
     * @param string
     * @returns {Array}
     */
    explodeFilesArg: function (string) {
        return string.split(",").map(function (item) {
            return item.trim();
        });
    },
    /**
     * @param pattern
     * @returns {*|string}
     * @private
     */
    wrapPattern: function (pattern) {
        var prefix = "!";
        var suffix = "/**";
        var lastChar = pattern.charAt(pattern.length - 1);
        var extName = path.extname(pattern);

        // If there's a file ext, don't append any suffix
        if (extName.length) {
            suffix = "";
        } else {

            if (lastChar === "/") {
                suffix = "**";
            }

            if (lastChar === "*") {
                suffix = "";
            }
        }

        return [prefix, pattern, suffix].join("");
    }
};

opts.callbacks = {

    /**
     * Merge server options
     * @param {String|Boolean|Object} value
     * @param [argv]
     * @returns {*}
     */
    server: function (value, argv) {

        if (value === false) {
            if (!argv || !argv.server) {
                return false;
            }
        }

        var obj = {
            baseDir: "./"
        };

        if (_.isString(value) || isList(value)) {
            obj.baseDir = value;
        } else {
            if (value && value !== true) {
                if (value.get("baseDir")) {
                    return value;
                }
            }
        }

        if (argv) {

            if (argv.index) {
                obj.index = argv.index;
            }

            if (argv.directory) {
                obj.directory = true;
            }
        }

        return Immutable.fromJS(obj);
    },
    /**
     * @param value
     * @param argv
     * @returns {*}
     */
    proxy: function (value) {

        var mw;
        var target;

        if (!value || value === true) {
            return false;
        }

        if (typeof value !== "string") {
            target = value.get("target");
            mw     = value.get("middleware");
        } else {
            target = value;
            value = Immutable.Map({});
        }

        if (!target.match(/^(https?):\/\//)) {
            target = "http://" + target;
        }

        var parsedUrl = url.parse(target);

        if (!parsedUrl.port) {
            parsedUrl.port = 80;
        }

        var out = {
            target: parsedUrl.protocol + "//" + parsedUrl.host,
            url: Immutable.Map(parsedUrl)
        };

        if (mw) {
            out.middleware = mw;
        }

        return value.mergeDeep(out);
    },
    /**
     * @param value
     * @private
     */
    ports: function (value) {

        var segs;
        var obj = {};

        if (typeof value === "string") {

            if (~value.indexOf(",")) {
                segs = value.split(",");
                obj.min = parseInt(segs[0], 10);
                obj.max = parseInt(segs[1], 10);
            } else {
                obj.min = parseInt(value, 10);
                obj.max = null;
            }

        } else {

            obj.min = value.get("min");
            obj.max = value.get("max") || null;
        }

        return Immutable.Map(obj);
    },
    /**
     * @param value
     * @param argv
     * @returns {*}
     */
    ghostMode: function (value, argv) {

        var trueAll = {
            clicks: true,
            scroll: true,
            forms: {
                submit: true,
                inputs: true,
                toggles: true
            }
        };

        var falseAll = {
            clicks: false,
            scroll: false,
            forms: {
                submit: false,
                inputs: false,
                toggles: false
            }
        };

        if (value === false || value === "false" || argv && argv.ghost === false) {
            return Immutable.fromJS(falseAll);
        }

        if (value === true || value === "true" || argv && argv.ghost === true) {
            return Immutable.fromJS(trueAll);
        }

        if (value.get("forms") === false) {
            return value.withMutations(function (map) {
                map.set("forms", Immutable.fromJS({
                    submit: false,
                    inputs: false,
                    toggles: false
                }));
            });
        }

        if (value.get("forms") === true) {
            return value.withMutations(function (map) {
                map.set("forms", Immutable.fromJS({
                    submit: true,
                    inputs: true,
                    toggles: true
                }));
            });
        }

        return value;
    },
    /**
     * @param value
     * @returns {*}
     */
    files: function (value) {

        var namespaces = {core: {}};

        namespaces.core.globs = [];
        namespaces.core.objs  = [];

        var processed = opts.makeFilesArg(value);

        if (processed.globs.length) {
            namespaces.core.globs = processed.globs;
        }

        if (processed.objs.length) {
            namespaces.core.objs = processed.objs;
        }

        return Immutable.fromJS(namespaces);
    },
    /**
     * @param value
     */
    extensions: function (value) {
        if (_.isString(value)) {
            var split = opts.utils.explodeFilesArg(value);
            if (split.length) {
                return Immutable.List(split);
            }
        }
    }
};

/**
 * @param {Object} values
 * @param {Object} [argv]
 * @returns {Map}
 */
opts.merge = function (values, argv) {
    return immDefs
        .mergeDeep(values)
        .withMutations(function (item) {
            item.map(function (value, key) {
                if (opts.callbacks[key]) {
                    item.set(key, opts.callbacks[key](value, argv));
                }
            });
        });
};

/**
 * @param value
 * @returns {{globs: Array, objs: Array}}
 */
opts.makeFilesArg = function (value) {

    var globs = [];
    var objs  = [];

    if (_.isString(value)) {
        globs = globs.concat(
            opts.utils.explodeFilesArg(value)
        );
    }

    if (isList(value) && value.size) {
        value.forEach(function (value) {
            if (_.isString(value)) {
                globs.push(value);
            } else {
                if (isMap(value)) {
                    objs.push(value);
                }
            }
        });
    }

    return {
        globs: globs,
        objs: objs
    };
};
