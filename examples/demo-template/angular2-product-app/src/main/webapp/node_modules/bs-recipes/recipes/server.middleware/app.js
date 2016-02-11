/**
 * Require Browsersync
 */
var browserSync = require('browser-sync').create();
var historyApiFallback = require('connect-history-api-fallback')

/**
 * Run Browsersync with server config
 */
browserSync.init({
    server: "app",
    files: ["app/*.html", "app/css/*.css"],
    middleware: [require("connect-logger")(), historyApiFallback()]
});