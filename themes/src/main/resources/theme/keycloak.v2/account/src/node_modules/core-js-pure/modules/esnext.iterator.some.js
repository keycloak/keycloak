'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var iterate = require('../internals/iterate');
var aCallable = require('../internals/a-callable');
var getIteratorDirect = require('../internals/get-iterator-direct');

$({ target: 'Iterator', proto: true, real: true, forced: true }, {
  some: function some(fn) {
    var record = getIteratorDirect(this);
    aCallable(fn);
    return iterate(record, function (value, stop) {
      if (fn(value)) return stop();
    }, { IS_RECORD: true, INTERRUPTED: true }).stopped;
  }
});
