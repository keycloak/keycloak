if (typeof Promise === 'undefined')
  require('when/es6-shim/Promise');

var System = require('./dist/es6-module-loader-dev.src');

var filePrefix = 'file:' + (process.platform.match(/^win/) ? '/' : '') + '//';

try {
  System.paths.traceur = filePrefix + require.resolve('traceur/bin/traceur.js');
}
catch(e) {}
try {
  System.paths.babel = filePrefix + require.resolve('babel-core/browser.js');
}
catch(e) {}
try {
  System.paths.babel = System.paths.babel || filePrefix + require.resolve('babel/browser.js');
}
catch(e) {}
try {
  System.paths.typescript = filePrefix + require.resolve('typescript/bin/typescript.js');
}
catch(e) { }

module.exports = {
  Loader: global.LoaderPolyfill,
  System: System
};
