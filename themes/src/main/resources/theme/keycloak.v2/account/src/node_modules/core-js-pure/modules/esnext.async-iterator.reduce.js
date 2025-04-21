'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var call = require('../internals/function-call');
var aCallable = require('../internals/a-callable');
var anObject = require('../internals/an-object');
var getBuiltIn = require('../internals/get-built-in');
var getIteratorDirect = require('../internals/get-iterator-direct');

var Promise = getBuiltIn('Promise');
var $TypeError = TypeError;

$({ target: 'AsyncIterator', proto: true, real: true, forced: true }, {
  reduce: function reduce(reducer /* , initialValue */) {
    var record = getIteratorDirect(this);
    var iterator = record.iterator;
    var next = record.next;
    var noInitial = arguments.length < 2;
    var accumulator = noInitial ? undefined : arguments[1];
    aCallable(reducer);

    return new Promise(function (resolve, reject) {
      var loop = function () {
        try {
          Promise.resolve(anObject(call(next, iterator))).then(function (step) {
            try {
              if (anObject(step).done) {
                noInitial ? reject($TypeError('Reduce of empty iterator with no initial value')) : resolve(accumulator);
              } else {
                var value = step.value;
                if (noInitial) {
                  noInitial = false;
                  accumulator = value;
                  loop();
                } else {
                  Promise.resolve(reducer(accumulator, value)).then(function (result) {
                    accumulator = result;
                    loop();
                  }, reject);
                }
              }
            } catch (err) { reject(err); }
          }, reject);
        } catch (error) { reject(error); }
      };

      loop();
    });
  }
});
