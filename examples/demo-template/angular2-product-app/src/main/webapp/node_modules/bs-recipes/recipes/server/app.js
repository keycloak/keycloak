/**
 * Require Browsersync
 */
var browserSync = require('browser-sync');

/**
 * Run Browsersync with server config
 */
browserSync({
    server: "app",
    files: ["app/*.html", "app/css/*.css"]
});