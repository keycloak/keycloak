'use strict';

var GetIntrinsic = require('get-intrinsic');

var $BigInt = GetIntrinsic('%BigInt%', true);
var $asIntN = GetIntrinsic('%BigInt.asIntN%', true);
var $Number = GetIntrinsic('%Number%');
var $SyntaxError = GetIntrinsic('%SyntaxError%');

var ToPrimitive = require('./ToPrimitive');

// https://262.ecma-international.org/11.0/#sec-tobigint

module.exports = function ToBigInt(argument) {
	if (!$BigInt) {
		throw new $SyntaxError('BigInts are not supported in this environment');
	}

	var prim = ToPrimitive(argument, $Number);

	if (typeof prim === 'number') {
		return $asIntN(0, prim);
	}
	return $BigInt(prim);
};
