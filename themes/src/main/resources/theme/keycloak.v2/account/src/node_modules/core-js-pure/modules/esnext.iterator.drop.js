'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var apply = require('../internals/function-apply');
var call = require('../internals/function-call');
var anObject = require('../internals/an-object');
var getIteratorDirect = require('../internals/get-iterator-direct');
var toPositiveInteger = require('../internals/to-positive-integer');
var createIteratorProxy = require('../internals/iterator-create-proxy');

var IteratorProxy = createIteratorProxy(function (args) {
  var iterator = this.iterator;
  var next = this.next;
  var result, done;
  while (this.remaining) {
    this.remaining--;
    result = anObject(call(next, iterator));
    done = this.done = !!result.done;
    if (done) return;
  }
  result = anObject(apply(next, iterator, args));
  done = this.done = !!result.done;
  if (!done) return result.value;
});

$({ target: 'Iterator', proto: true, real: true, forced: true }, {
  drop: function drop(limit) {
    return new IteratorProxy(getIteratorDirect(this), {
      remaining: toPositiveInteger(limit)
    });
  }
});
