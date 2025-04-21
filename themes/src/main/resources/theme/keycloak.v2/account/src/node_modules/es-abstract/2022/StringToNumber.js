'use strict';

var GetIntrinsic = require('get-intrinsic');

var $Number = GetIntrinsic('%Number%');
var $RegExp = GetIntrinsic('%RegExp%');
var $TypeError = GetIntrinsic('%TypeError%');
var $parseInteger = GetIntrinsic('%parseInt%');

var callBound = require('call-bind/callBound');
var regexTester = require('../helpers/regexTester');

var $strSlice = callBound('String.prototype.slice');
var isBinary = regexTester(/^0b[01]+$/i);
var isOctal = regexTester(/^0o[0-7]+$/i);
var isInvalidHexLiteral = regexTester(/^[-+]0x[0-9a-f]+$/i);
var nonWS = ['\u0085', '\u200b', '\ufffe'].join('');
var nonWSregex = new $RegExp('[' + nonWS + ']', 'g');
var hasNonWS = regexTester(nonWSregex);

// whitespace from: https://es5.github.io/#x15.5.4.20
// implementation from https://github.com/es-shims/es5-shim/blob/v3.4.0/es5-shim.js#L1304-L1324
var ws = [
	'\x09\x0A\x0B\x0C\x0D\x20\xA0\u1680\u180E\u2000\u2001\u2002\u2003',
	'\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u202F\u205F\u3000\u2028',
	'\u2029\uFEFF'
].join('');
var trimRegex = new RegExp('(^[' + ws + ']+)|([' + ws + ']+$)', 'g');
var $replace = callBound('String.prototype.replace');
var $trim = function (value) {
	return $replace(value, trimRegex, '');
};

var Type = require('./Type');

// https://ecma-international.org/ecma-262/13.0/#sec-stringtonumber

module.exports = function StringToNumber(argument) {
	if (Type(argument) !== 'String') {
		throw new $TypeError('Conversion from \'BigInt\' to \'number\' is not allowed.');
	}
	if (isBinary(argument)) {
		return $Number($parseInteger($strSlice(argument, 2), 2));
	}
	if (isOctal(argument)) {
		return $Number($parseInteger($strSlice(argument, 2), 8));
	}
	if (hasNonWS(argument) || isInvalidHexLiteral(argument)) {
		return NaN;
	}
	var trimmed = $trim(argument);
	if (trimmed !== argument) {
		return StringToNumber(trimmed);
	}
	return $Number(argument);
};
