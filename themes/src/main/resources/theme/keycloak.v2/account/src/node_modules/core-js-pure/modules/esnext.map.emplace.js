'use strict';
var $ = require('../internals/export');
var emplace = require('../internals/map-emplace');

// `Map.prototype.emplace` method
// https://github.com/thumbsupep/proposal-upsert
$({ target: 'Map', proto: true, real: true, forced: true }, {
  emplace: emplace
});
