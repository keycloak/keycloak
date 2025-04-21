'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var apply = require('../internals/function-apply');
var anObject = require('../internals/an-object');
var getIteratorDirect = require('../internals/get-iterator-direct');
var toPositiveInteger = require('../internals/to-positive-integer');
var createAsyncIteratorProxy = require('../internals/async-iterator-create-proxy');

var AsyncIteratorProxy = createAsyncIteratorProxy(function (Promise, args) {
  var state = this;

  return new Promise(function (resolve, reject) {
    var loop = function () {
      try {
        Promise.resolve(
          anObject(apply(state.next, state.iterator, state.remaining ? [] : args))
        ).then(function (step) {
          try {
            if (anObject(step).done) {
              state.done = true;
              resolve({ done: true, value: undefined });
            } else if (state.remaining) {
              state.remaining--;
              loop();
            } else resolve({ done: false, value: step.value });
          } catch (err) { reject(err); }
        }, reject);
      } catch (error) { reject(error); }
    };

    loop();
  });
});

$({ target: 'AsyncIterator', proto: true, real: true, forced: true }, {
  drop: function drop(limit) {
    return new AsyncIteratorProxy(getIteratorDirect(this), {
      remaining: toPositiveInteger(limit)
    });
  }
});
