'use strict';
// https://github.com/tc39/proposal-iterator-helpers
var $ = require('../internals/export');
var apply = require('../internals/function-apply');
var anObject = require('../internals/an-object');
var getIteratorDirect = require('../internals/get-iterator-direct');
var createIteratorProxy = require('../internals/iterator-create-proxy');

var IteratorProxy = createIteratorProxy(function (args) {
  var result = anObject(apply(this.next, this.iterator, args));
  var done = this.done = !!result.done;
  if (!done) return [this.index++, result.value];
});

$({ target: 'Iterator', proto: true, real: true, forced: true }, {
  asIndexedPairs: function asIndexedPairs() {
    return new IteratorProxy(getIteratorDirect(this), {
      index: 0
    });
  }
});
