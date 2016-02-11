"use strict";

/**
 * @param {BrowserSync} browserSync
 * @returns {Function}
 */
module.exports = function (browserSync) {

    return function () {
        browserSync.paused = true;
    };
};
