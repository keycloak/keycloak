'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var apply = require('../internals/function-apply');
var anObject = require('../internals/an-object');
var getIteratorDirect = require('../internals/get-iterator-direct');
var toPositiveInteger = require('../internals/to-positive-integer');
var createIteratorProxy = require('../internals/iterator-create-proxy');
var iteratorClose = require('../internals/iterator-close');

var IteratorProxy = createIteratorProxy(function (args) {
  var iterator = this.iterator;
  if (!this.remaining--) {
    this.done = true;
    return iteratorClose(iterator, 'normal', undefined);
  }
  var result = anObject(apply(this.next, iterator, args));
  var done = this.done = !!result.done;
  if (!done) return result.value;
});

$({ target: 'Iterator', proto: true, real: true, forced: true }, {
  take: function take(limit) {
    return new IteratorProxy(getIteratorDirect(this), {
      remaining: toPositiveInteger(limit)
    });
  }
});
