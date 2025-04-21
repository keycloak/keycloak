var defineWellKnownSymbol = require('../internals/define-well-known-symbol');
var defineSymbolToPrimitive = require('../internals/symbol-define-to-primitive');

// `Symbol.toPrimitive` well-known symbol
// https://tc39.es/ecma262/#sec-symbol.toprimitive
defineWellKnownSymbol('toPrimitive');

// `Symbol.prototype[@@toPrimitive]` method
// https://tc39.es/ecma262/#sec-symbol.prototype-@@toprimitive
defineSymbolToPrimitive();
