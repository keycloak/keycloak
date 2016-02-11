"use strict";

/**
 * Default error handler - expected to be overridden
 * @param {Error} err
 */
module.exports = function (err) {
    console.log(err.message);
};
