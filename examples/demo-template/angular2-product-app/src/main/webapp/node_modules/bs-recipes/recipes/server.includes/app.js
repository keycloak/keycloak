/**
 * Require Browsersync
 */
var browserSync = require('browser-sync').create();
var fs = require('fs');

/**
 * Run Browsersync with server config
 */
browserSync.init({
    server: 'app',
    files: ['app/*.html', 'app/css/*.css'],
    rewriteRules: [
        {
            match: /@include\("(.+?)"\)/g,
            fn: function (match, filename) {
                if (fs.existsSync(filename)) {
                    return fs.readFileSync(filename);
                } else {
                    return '<span style="color: red">'+filename+' could not be found</span>';
                }
            }
        }
    ]
});