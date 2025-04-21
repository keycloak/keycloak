// TODO: Remove from `core-js@4`
var $ = require('../internals/export');
var arrayMethodIsStrict = require('../internals/array-method-is-strict');
var addToUnscopables = require('../internals/add-to-unscopables');
var $groupToMap = require('../internals/array-group-to-map');

// `Array.prototype.groupByToMap` method
// https://github.com/tc39/proposal-array-grouping
// https://bugs.webkit.org/show_bug.cgi?id=236541
$({ target: 'Array', proto: true, name: 'groupToMap', forced: !arrayMethodIsStrict('groupByToMap') }, {
  groupByToMap: $groupToMap
});

addToUnscopables('groupByToMap');
