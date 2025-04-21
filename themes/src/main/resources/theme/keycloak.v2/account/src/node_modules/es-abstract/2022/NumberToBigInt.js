'use strict';

var GetIntrinsic = require('get-intrinsic');

var $BigInt = GetIntrinsic('%BigInt%', true);
var $RangeError = GetIntrinsic('%RangeError%');
var $TypeError = GetIntrinsic('%TypeError%');

var IsIntegralNumber = require('./IsIntegralNumber');
var Type = require('./Type');

// https://262.ecma-international.org/11.0/#sec-numbertobigint

module.exports = function NumberToBigInt(number) {
	if (Type(number) !== 'Number') {
		throw new $TypeError('Assertion failed: `number` must be a String');
	}
	if (!IsIntegralNumber(number)) {
		throw new $RangeError('The number ' + number + ' cannot be converted to a BigInt because it is not an integer');
	}
	return $BigInt(number);
};
