"use strict";

/**
 * @param {BrowserSync} browserSync
 * @returns {Function}
 */
module.exports = function (browserSync) {

    return function (msg, timeout) {

        if (msg) {
            browserSync.events.emit("browser:notify", {
                message: msg,
                timeout: timeout || 2000
            });
        }
    };
};
