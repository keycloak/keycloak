'use strict';

var GetIntrinsic = require('get-intrinsic');

var $TypeError = GetIntrinsic('%TypeError%');

var DefinePropertyOrThrow = require('./DefinePropertyOrThrow');
var HasOwnProperty = require('./HasOwnProperty');
var IsExtensible = require('./IsExtensible');
var IsIntegralNumber = require('./IsIntegralNumber');
var Type = require('./Type');

// https://ecma-international.org/ecma-262/12.0/#sec-setfunctionlength

module.exports = function SetFunctionLength(F, length) {
	if (typeof F !== 'function' || !IsExtensible(F) || HasOwnProperty(F, 'length')) {
		throw new $TypeError('Assertion failed: `F` must be an extensible function and lack an own `length` property');
	}
	if (Type(length) !== 'Number') {
		throw new $TypeError('Assertion failed: `length` must be a Number');
	}
	if (length !== Infinity && (!IsIntegralNumber(length) || length < 0)) {
		throw new $TypeError('Assertion failed: `length` must be ∞, or an integer >= 0');
	}
	return DefinePropertyOrThrow(F, 'length', {
		'[[Configurable]]': true,
		'[[Enumerable]]': false,
		'[[Value]]': length,
		'[[Writable]]': false
	});
};
