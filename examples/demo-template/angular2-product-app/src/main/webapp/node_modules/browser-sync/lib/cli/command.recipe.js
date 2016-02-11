"use strict";
var logger = require("../logger").logger;

/**
 * $ browser-sync recipe <name> <options>
 *
 * This command will copy a recipe into either the current directory
 * or one given with the --output flag
 *
 * @param opts
 * @returns {Function}
 */
module.exports = function (opts) {

    var path      = require("path");
    var fs        = require("fs-extra");
    var input     = opts.cli.input.slice(1);
    var resolved  = require.resolve("bs-recipes");
    var dir       = path.dirname(resolved);

    var logRecipes = function () {
        var dirs = fs.readdirSync(path.join(dir, "recipes"), function (err, output) {
        });
        logger.info("Install one of the following with {cyan:browser-sync recipe <name>\n");
        dirs.forEach(function (name) {
            console.log("    " + name);
        });
    }

    if (!input.length) {
        logger.info("No recipe name provided!");
        logRecipes();
        return opts.cb();
    }

    input         = input[0];
    var flags     = opts.cli.flags;
    var output    = flags.output ? path.resolve(flags.output) : path.join(process.cwd(), input);
    var targetDir = path.join(dir, "recipes", input);

    if (fs.existsSync(output)) {
        return opts.cb(new Error("Target folder exists remove it first and then try again"));
    }

    if (fs.existsSync(targetDir)) {
        fs.copy(targetDir, output, function (err) {
            if (err) {
                opts.cb(err);
            } else {
                logger.info("Recipe copied into {cyan:%s}", output);
                logger.info("Next, inside that folder, run {cyan:npm i && npm start}");
                opts.cb(null);
            }
        });
    } else {
        logger.info("Recipe {cyan:%s} not found. The following are available though", input);
        logRecipes();
        opts.cb();
    }
};
