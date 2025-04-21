'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var iterate = require('../internals/iterate');
var getIteratorDirect = require('../internals/get-iterator-direct');

$({ target: 'Iterator', proto: true, real: true, forced: true }, {
  forEach: function forEach(fn) {
    iterate(getIteratorDirect(this), fn, { IS_RECORD: true });
  }
});
