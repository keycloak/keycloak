'use strict';
var $ = require('../internals/export');
var aCallable = require('../internals/a-callable');
var anObject = require('../internals/an-object');
var getSetIterator = require('../internals/get-set-iterator');
var iterate = require('../internals/iterate');

var $TypeError = TypeError;

// `Set.prototype.reduce` method
// https://github.com/tc39/proposal-collection-methods
$({ target: 'Set', proto: true, real: true, forced: true }, {
  reduce: function reduce(callbackfn /* , initialValue */) {
    var set = anObject(this);
    var iterator = getSetIterator(set);
    var noInitial = arguments.length < 2;
    var accumulator = noInitial ? undefined : arguments[1];
    aCallable(callbackfn);
    iterate(iterator, function (value) {
      if (noInitial) {
        noInitial = false;
        accumulator = value;
      } else {
        accumulator = callbackfn(accumulator, value, value, set);
      }
    }, { IS_ITERATOR: true });
    if (noInitial) throw $TypeError('Reduce of empty set with no initial value');
    return accumulator;
  }
});
