/**
 *
 * Foxy - proxy with response moddin'
 * https://github.com/shakyShane/foxy
 *
 */

/**
 * @param {String} target - a url such as http://www.bbc.co.uk or http://localhost:8181
 * @param {Object} [userConfig]
 * @returns {Foxy}
 */
function create(target, userConfig) {

    /**
     * Merge user config with defaults
     * @type {Immutable.Map}
     */
    var config = require("./lib/config")(target, userConfig);

    /**
     * Create a connect app
     */
    var Foxy    = require("./lib/server");

    return new Foxy(config);
}

module.exports = function (target, userConfig) {
    return create(target, userConfig).app;
};

module.exports.create = function (target, userConfig) {
    return create(target, userConfig);
};
