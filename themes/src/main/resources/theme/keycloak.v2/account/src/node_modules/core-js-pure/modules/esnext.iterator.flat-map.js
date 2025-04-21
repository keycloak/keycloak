'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var call = require('../internals/function-call');
var aCallable = require('../internals/a-callable');
var anObject = require('../internals/an-object');
var getIteratorDirect = require('../internals/get-iterator-direct');
var getIteratorMethod = require('../internals/get-iterator-method');
var createIteratorProxy = require('../internals/iterator-create-proxy');
var iteratorClose = require('../internals/iterator-close');

var $TypeError = TypeError;

var IteratorProxy = createIteratorProxy(function () {
  var iterator = this.iterator;
  var mapper = this.mapper;
  var result, mapped, iteratorMethod, innerIterator;

  while (true) {
    try {
      if (innerIterator = this.innerIterator) {
        result = anObject(call(this.innerNext, innerIterator));
        if (!result.done) return result.value;
        this.innerIterator = this.innerNext = null;
      }

      result = anObject(call(this.next, iterator));

      if (this.done = !!result.done) return;

      mapped = mapper(result.value);
      iteratorMethod = getIteratorMethod(mapped);

      if (!iteratorMethod) {
        throw $TypeError('.flatMap callback should return an iterable object');
      }

      this.innerIterator = innerIterator = anObject(call(iteratorMethod, mapped));
      this.innerNext = aCallable(innerIterator.next);
    } catch (error) {
      iteratorClose(iterator, 'throw', error);
    }
  }
});

$({ target: 'Iterator', proto: true, real: true, forced: true }, {
  flatMap: function flatMap(mapper) {
    return new IteratorProxy(getIteratorDirect(this), {
      mapper: aCallable(mapper),
      innerIterator: null,
      innerNext: null
    });
  }
});
