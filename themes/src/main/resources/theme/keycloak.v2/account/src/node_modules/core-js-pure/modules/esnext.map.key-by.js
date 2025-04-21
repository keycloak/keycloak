'use strict';
var $ = require('../internals/export');
var call = require('../internals/function-call');
var iterate = require('../internals/iterate');
var aCallable = require('../internals/a-callable');

// `Map.keyBy` method
// https://github.com/tc39/proposal-collection-methods
$({ target: 'Map', stat: true, forced: true }, {
  keyBy: function keyBy(iterable, keyDerivative) {
    var newMap = new this();
    aCallable(keyDerivative);
    var setter = aCallable(newMap.set);
    iterate(iterable, function (element) {
      call(setter, newMap, keyDerivative(element), element);
    });
    return newMap;
  }
});
