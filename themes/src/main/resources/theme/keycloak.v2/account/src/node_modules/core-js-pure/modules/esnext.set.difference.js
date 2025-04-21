'use strict';
var $ = require('../internals/export');
var getBuiltIn = require('../internals/get-built-in');
var call = require('../internals/function-call');
var aCallable = require('../internals/a-callable');
var anObject = require('../internals/an-object');
var speciesConstructor = require('../internals/species-constructor');
var iterate = require('../internals/iterate');

// `Set.prototype.difference` method
// https://github.com/tc39/proposal-set-methods
$({ target: 'Set', proto: true, real: true, forced: true }, {
  difference: function difference(iterable) {
    var set = anObject(this);
    var newSet = new (speciesConstructor(set, getBuiltIn('Set')))(set);
    var remover = aCallable(newSet['delete']);
    iterate(iterable, function (value) {
      call(remover, newSet, value);
    });
    return newSet;
  }
});
