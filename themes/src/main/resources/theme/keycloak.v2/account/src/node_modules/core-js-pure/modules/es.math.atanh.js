var $ = require('../internals/export');

// eslint-disable-next-line es-x/no-math-atanh -- required for testing
var $atanh = Math.atanh;
var log = Math.log;

// `Math.atanh` method
// https://tc39.es/ecma262/#sec-math.atanh
// Tor Browser bug: Math.atanh(-0) -> 0
$({ target: 'Math', stat: true, forced: !($atanh && 1 / $atanh(-0) < 0) }, {
  atanh: function atanh(x) {
    var n = +x;
    return n == 0 ? n : log((1 + n) / (1 - n)) / 2;
  }
});
