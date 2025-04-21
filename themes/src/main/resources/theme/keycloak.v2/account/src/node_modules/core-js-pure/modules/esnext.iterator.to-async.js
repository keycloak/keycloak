'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var AsyncFromSyncIterator = require('../internals/async-from-sync-iterator');

$({ target: 'Iterator', proto: true, real: true, forced: true }, {
  toAsync: function toAsync() {
    return new AsyncFromSyncIterator(this);
  }
});
