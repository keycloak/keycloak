'use strict';
var $ = require('../internals/export');
var call = require('../internals/function-call');
var uncurryThis = require('../internals/function-uncurry-this');
var aCallable = require('../internals/a-callable');
var getIterator = require('../internals/get-iterator');
var iterate = require('../internals/iterate');

var push = uncurryThis([].push);

// `Map.groupBy` method
// https://github.com/tc39/proposal-collection-methods
$({ target: 'Map', stat: true, forced: true }, {
  groupBy: function groupBy(iterable, keyDerivative) {
    aCallable(keyDerivative);
    var iterator = getIterator(iterable);
    var newMap = new this();
    var has = aCallable(newMap.has);
    var get = aCallable(newMap.get);
    var set = aCallable(newMap.set);
    iterate(iterator, function (element) {
      var derivedKey = keyDerivative(element);
      if (!call(has, newMap, derivedKey)) call(set, newMap, derivedKey, [element]);
      else push(call(get, newMap, derivedKey), element);
    }, { IS_ITERATOR: true });
    return newMap;
  }
});
