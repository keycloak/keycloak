var $ = require('../internals/export');
var global = require('../internals/global');
var setImmediate = require('../internals/task').set;

// `setImmediate` method
// http://w3c.github.io/setImmediate/#si-setImmediate
$({ global: true, bind: true, enumerable: true, forced: global.setImmediate !== setImmediate }, {
  setImmediate: setImmediate
});
