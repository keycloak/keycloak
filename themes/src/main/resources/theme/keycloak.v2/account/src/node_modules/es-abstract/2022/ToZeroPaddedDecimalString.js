'use strict';

var GetIntrinsic = require('get-intrinsic');

var $String = GetIntrinsic('%String%');
var $RangeError = GetIntrinsic('%RangeError%');

var IsIntegralNumber = require('./IsIntegralNumber');
var StringPad = require('./StringPad');

// https://ecma-international.org/ecma-262/13.0/#sec-tozeropaddeddecimalstring

module.exports = function ToZeroPaddedDecimalString(n, minLength) {
	if (!IsIntegralNumber(n) || n < 0) {
		throw new $RangeError('Assertion failed: `q` must be a non-negative integer');
	}
	var S = $String(n);
	return StringPad(S, minLength, '0', 'start');
};
