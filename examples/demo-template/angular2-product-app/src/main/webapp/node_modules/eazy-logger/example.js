/**
 *
 * Run `node example.js` to see the output of these examples
 *
 */
var logger  = require("./index").Logger({
    prefix: "{blue:[}{cyan:BS}{blue:] }"
}).setLevelPrefixes(true).setLevel("debug");

/**
 * Standard loggers
 */
logger.debug("Debugging Msg");
logger.info("Info statement");
logger.warn("A little warning");
logger.error("an error occurred!");

/**
 *
 */
logger.setLevelPrefixes(false);

/**
 *
 */
logger.log("error", "Use {green:%s} %s", "String substitution", "is cool");

/**
 * Without level prefixes
 */
logger.log("info", "No LEVEL prefix here");

/**
 * Un-prefixed
 */
logger.unprefixed("info", "NO PREFIX");

var clone = logger.clone(function (config) {
    return config;
});