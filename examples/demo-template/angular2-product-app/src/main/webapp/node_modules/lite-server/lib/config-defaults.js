var historyFallback = require('connect-history-api-fallback');
var log = require('connect-logger');
/*
 | For up-to-date information about the options:
 |   http://www.browsersync.io/docs/options/
 */
module.exports = {
  injectChanges: false, // workaround for Angular 2 styleUrls loading
  files: ['./**/*.{html,htm,css,js}'],
  server: {
    baseDir: './',
    middleware: [
      log({format: '%date %status %method %url'}),
      historyFallback({"index": '/index.html'})
    ]
  }
};
