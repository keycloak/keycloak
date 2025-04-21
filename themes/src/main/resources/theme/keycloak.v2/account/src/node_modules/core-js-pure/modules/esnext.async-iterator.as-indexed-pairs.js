'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var apply = require('../internals/function-apply');
var anObject = require('../internals/an-object');
var getIteratorDirect = require('../internals/get-iterator-direct');
var createAsyncIteratorProxy = require('../internals/async-iterator-create-proxy');

var AsyncIteratorProxy = createAsyncIteratorProxy(function (Promise, args) {
  var state = this;
  var iterator = state.iterator;

  return Promise.resolve(anObject(apply(state.next, iterator, args))).then(function (step) {
    if (anObject(step).done) {
      state.done = true;
      return { done: true, value: undefined };
    }
    return { done: false, value: [state.index++, step.value] };
  });
});

$({ target: 'AsyncIterator', proto: true, real: true, forced: true }, {
  asIndexedPairs: function asIndexedPairs() {
    return new AsyncIteratorProxy(getIteratorDirect(this), {
      index: 0
    });
  }
});
