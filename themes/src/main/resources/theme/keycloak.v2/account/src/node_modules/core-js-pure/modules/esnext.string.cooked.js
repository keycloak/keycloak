var $ = require('../internals/export');
var uncurryThis = require('../internals/function-uncurry-this');
var toIndexedObject = require('../internals/to-indexed-object');
var toString = require('../internals/to-string');
var lengthOfArrayLike = require('../internals/length-of-array-like');

var $TypeError = TypeError;
var ArrayPrototype = Array.prototype;
var push = uncurryThis(ArrayPrototype.push);
var join = uncurryThis(ArrayPrototype.join);

// `String.cooked` method
// https://github.com/tc39/proposal-string-cooked
$({ target: 'String', stat: true, forced: true }, {
  cooked: function cooked(template /* , ...substitutions */) {
    var cookedTemplate = toIndexedObject(template);
    var literalSegments = lengthOfArrayLike(cookedTemplate);
    var argumentsLength = arguments.length;
    var elements = [];
    var i = 0;
    while (literalSegments > i) {
      var nextVal = cookedTemplate[i++];
      if (nextVal === undefined) throw $TypeError('Incorrect template');
      push(elements, toString(nextVal));
      if (i === literalSegments) return join(elements, '');
      if (i < argumentsLength) push(elements, toString(arguments[i]));
    }
  }
});
