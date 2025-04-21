'use strict';
var call = require('../internals/function-call');
var aCallable = require('../internals/a-callable');
var anObject = require('../internals/an-object');

// https://github.com/tc39/collection-methods
module.exports = function addAll(/* ...elements */) {
  var set = anObject(this);
  var adder = aCallable(set.add);
  for (var k = 0, len = arguments.length; k < len; k++) {
    call(adder, set, arguments[k]);
  }
  return set;
};
