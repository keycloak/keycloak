/**
 * Require Browsersync
 */
var browserSync = require('browser-sync').create();
var middleware  = require('connect-gzip-static')('./app');

/**
 * Run Browsersync with server config
 * Add middleware with override:true to ensure all files are
 * picked up.
 */
browserSync.init({
    server: 'app',
    files: ['app/*.html', 'app/css/*.css']
}, function (err, bs) {
    bs.addMiddleware("*", middleware, {
        override: true
    });
});