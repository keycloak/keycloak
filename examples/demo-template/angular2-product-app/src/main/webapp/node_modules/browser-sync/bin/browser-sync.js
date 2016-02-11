#!/usr/bin/env node
"use strict";

var meow         = require("meow");
var fs           = require("fs");
var path         = require("path");
var compile      = require("eazy-logger").compile;
var longest      = require("longest");
var utils        = require("../lib/utils");
var logger       = require("../lib/logger").logger;
var cmdWhitelist = ["start", "init", "reload", "recipe"];

var cli = meow({
    pkg:  "../package.json",
    help: getHelpText(path.join(__dirname, "../lib/cli/help.txt"))
});

/**
 * Handle cli input
 */
if (!module.parent) {
    handleCli({cli: cli, whitelist: cmdWhitelist});
}

/**
 * Generate & colour the help text
 * @param {String} filepath - relative file path to the help text
 * @returns {String}
 */
function getHelpText(filepath) {

    /**
     * Help text template
     */
    var template = fs.readFileSync(filepath, "utf8");

    cmdWhitelist.forEach(function (command) {

        var flags = require("../lib/cli/opts." + command + ".json");
        template = template.replace("%" + command + "flags%", listFlags(flags));
    });

    return compile(template);
}

/**
 * @param {{cli: object, [whitelist]: array, [cb]: function}} opts
 * @returns {*}
 */
function handleCli (opts) {

    opts.cb = opts.cb || utils.defaultCallback;

    var input = opts.cli.input;

    if (!opts.whitelist) {
        opts.whitelist = cmdWhitelist;
    }

    if (!input.length || opts.whitelist.indexOf(input[0]) === -1) {
        return console.log(opts.cli.help);
    }

    if (!require("../lib/cli/cli-utils").verifyOpts(input[0], opts.cli.flags)) {
        logger.info("For help, run: {cyan:browser-sync --help}");
        return opts.cb(new Error("Unknown flag given. Please refer to the documentation for help."));
    }

    return require("../lib/cli/command." + input[0])(opts);
}

/**
 * @param {Object} flags
 */
function listFlags (flags) {

    var flagKeys = Object.keys(flags);
    var maxLength = (longest(Object.keys(flags)) || "").length + 4;

    return flagKeys.map(function (item) {
        var length = maxLength - item.length;
        return "    {bold:--" + item + chars(length, " ") + "}" +
               flags[item];
    }).join("\n");
}

function chars (length, char) {
    return new Array(length).join(char);
}

module.exports = handleCli;
module.exports.getHelpText = getHelpText;
