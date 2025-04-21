'use strict';
var $ = require('../internals/export');
var deleteAll = require('../internals/collection-delete-all');

// `WeakSet.prototype.deleteAll` method
// https://github.com/tc39/proposal-collection-methods
$({ target: 'WeakSet', proto: true, real: true, forced: true }, {
  deleteAll: deleteAll
});
