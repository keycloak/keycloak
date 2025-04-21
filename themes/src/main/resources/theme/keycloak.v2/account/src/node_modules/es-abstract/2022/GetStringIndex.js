'use strict';

var GetIntrinsic = require('get-intrinsic');
var callBound = require('call-bind/callBound');

var $TypeError = GetIntrinsic('%TypeError%');

var IsIntegralNumber = require('./IsIntegralNumber');
var StringToCodePoints = require('./StringToCodePoints');
var Type = require('./Type');

var $indexOf = callBound('String.prototype.indexOf');

// https://ecma-international.org/ecma-262/13.0/#sec-getstringindex

module.exports = function GetStringIndex(S, e) {
	if (Type(S) !== 'String') {
		throw new $TypeError('Assertion failed: `S` must be a String');
	}
	if (!IsIntegralNumber(e) || e < 0) {
		throw new $TypeError('Assertion failed: `e` must be a non-negative integer');
	}

	if (S === '') {
		return 0;
	}
	var codepoints = StringToCodePoints(S);
	var eUTF = e >= codepoints.length ? S.length : $indexOf(S, codepoints[e]);
	return eUTF;
};
