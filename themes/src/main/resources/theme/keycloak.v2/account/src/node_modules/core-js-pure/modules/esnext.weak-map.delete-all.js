'use strict';
var $ = require('../internals/export');
var deleteAll = require('../internals/collection-delete-all');

// `WeakMap.prototype.deleteAll` method
// https://github.com/tc39/proposal-collection-methods
$({ target: 'WeakMap', proto: true, real: true, forced: true }, {
  deleteAll: deleteAll
});
