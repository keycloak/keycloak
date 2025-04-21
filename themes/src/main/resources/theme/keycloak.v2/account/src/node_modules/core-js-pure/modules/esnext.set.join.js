'use strict';
var $ = require('../internals/export');
var uncurryThis = require('../internals/function-uncurry-this');
var anObject = require('../internals/an-object');
var toString = require('../internals/to-string');
var getSetIterator = require('../internals/get-set-iterator');
var iterate = require('../internals/iterate');

var arrayJoin = uncurryThis([].join);
var push = [].push;

// `Set.prototype.join` method
// https://github.com/tc39/proposal-collection-methods
$({ target: 'Set', proto: true, real: true, forced: true }, {
  join: function join(separator) {
    var set = anObject(this);
    var iterator = getSetIterator(set);
    var sep = separator === undefined ? ',' : toString(separator);
    var result = [];
    iterate(iterator, push, { that: result, IS_ITERATOR: true });
    return arrayJoin(result, sep);
  }
});
