"use strict";

var chalk      = require("chalk");
var parser     = require("./lib/parser");
var objectPath = require("object-path");

/**
 * Stateless compiler.
 * @param {String} string
 * @param {Object} [custom] - Any custom methods
 * @param {Object} [opts] - Options
 * @returns {String}
 */
function compile(string, custom, opts) {
    opts = opts || {};
    return parseAst(createAst(parser, string), custom, function (err) {
        if (err) {
            if (opts.logErrors) {
                console.log(err.msg);
            }
            if (opts.failOnError) {
                throw Error(err.msg);
            }
        }
    });
}

/**
 * @param parser
 * @param string
 * @returns {*}
 */
function createAst(parser, string) {
    return parser.parse(string);
}

/**
 * @param ast
 * @param custom
 * @param {Function} cb
 */
function parseAst(ast, custom, cb) {

    var colors = [];

    return ast.reduce(function (joined, item) {

        var fn;

        if (item.color) {
            if (item.text) {
                if (fn = resolveFun(item.color, custom)) {
                    colors.push(fn);
                    return joined + fn(item.text);
                } else {
                    cb({
                        msg: "Method does not exist: " + item.color
                    });
                    return joined + item.text;
                }
            }
        }

        if (item.buffer) {
            return colors.length
                ? joined + colors[colors.length-1](item.buffer)
                : joined + item.buffer;
        }

        if (item.reset) {
            colors.pop();
            if (item.text) {
                return colors.length
                    ? joined + colors[colors.length-1](item.text)
                    : joined + item.text;
            }
        }

        return joined;

    }, "");
}

/**
 * @param path
 * @param custom
 * @returns {*}
 */
function resolveFun(path, custom) {

    var fn;
    if (fn = getFun(custom, path)) {
        return fn.bind({compile:compile});
    }

    return  getFun(chalk, path);
}

/**
 * Get a function from an object
 */
function getFun(obj, path) {

    if (!obj) {
        return false;
    }

    return objectPath.get(obj, path);
}

/**
 * @param {Object} [opts]
 * @param {Object} custom
 * @returns {Compiler}
 */
function Compiler(custom, opts) {

    opts = opts || {};
    custom = custom || {};

    this.prefix = "";

    if (typeof opts.prefix === "string") {
        this.prefix = compile(opts.prefix, custom, opts);
    }

    if (typeof opts.prefix === "function") {
        this.prefix = opts.prefix;
    }

    this.compile = function (string, noPrefix) {

        var out = "";

        if (!noPrefix) {

            if (typeof this.prefix === "function") {
                out = this.prefix.apply({compile: compile}, [string, opts]);
            } else {
                out = this.prefix;
            }
        }

        return out + compile(string, custom, opts);

    };

    return this;
}

module.exports = compile;
module.exports.Compiler = Compiler;
