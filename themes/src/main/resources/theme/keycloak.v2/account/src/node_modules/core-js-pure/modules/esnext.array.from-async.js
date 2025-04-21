var $ = require('../internals/export');
var fromAsync = require('../internals/array-from-async');

// `Array.fromAsync` method
// https://github.com/tc39/proposal-array-from-async
$({ target: 'Array', stat: true, forced: true }, {
  fromAsync: fromAsync
});
