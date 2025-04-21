// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var apply = require('../internals/function-apply');
var anObject = require('../internals/an-object');
var toObject = require('../internals/to-object');
var isPrototypeOf = require('../internals/object-is-prototype-of');
var AsyncIteratorPrototype = require('../internals/async-iterator-prototype');
var createAsyncIteratorProxy = require('../internals/async-iterator-create-proxy');
var getAsyncIterator = require('../internals/get-async-iterator');
var getIterator = require('../internals/get-iterator');
var getIteratorDirect = require('../internals/get-iterator-direct');
var getIteratorMethod = require('../internals/get-iterator-method');
var getMethod = require('../internals/get-method');
var wellKnownSymbol = require('../internals/well-known-symbol');
var AsyncFromSyncIterator = require('../internals/async-from-sync-iterator');

var ASYNC_ITERATOR = wellKnownSymbol('asyncIterator');

var AsyncIteratorProxy = createAsyncIteratorProxy(function (Promise, args) {
  return anObject(apply(this.next, this.iterator, args));
}, true);

$({ target: 'AsyncIterator', stat: true, forced: true }, {
  from: function from(O) {
    var object = toObject(O);
    var usingIterator = getMethod(object, ASYNC_ITERATOR);
    var iterator;
    if (usingIterator) {
      iterator = getAsyncIterator(object, usingIterator);
      if (isPrototypeOf(AsyncIteratorPrototype, iterator)) return iterator;
    }
    if (iterator === undefined) {
      usingIterator = getIteratorMethod(object);
      if (usingIterator) return new AsyncFromSyncIterator(getIterator(object, usingIterator));
    }
    return new AsyncIteratorProxy(getIteratorDirect(iterator !== undefined ? iterator : object));
  }
});
