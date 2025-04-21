'use strict';
var $ = require('../internals/export');
var emplace = require('../internals/map-emplace');

// `WeakMap.prototype.emplace` method
// https://github.com/tc39/proposal-upsert
$({ target: 'WeakMap', proto: true, real: true, forced: true }, {
  emplace: emplace
});
