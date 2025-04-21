var $ = require('../internals/export');
var addToUnscopables = require('../internals/add-to-unscopables');
var $groupToMap = require('../internals/array-group-to-map');

// `Array.prototype.groupToMap` method
// https://github.com/tc39/proposal-array-grouping
$({ target: 'Array', proto: true }, {
  groupToMap: $groupToMap
});

addToUnscopables('groupToMap');
