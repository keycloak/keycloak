'use strict';

var GetIntrinsic = require('get-intrinsic');

var $TypeError = GetIntrinsic('%TypeError%');

var IsIntegralNumber = require('./IsIntegralNumber');
var Type = require('./Type');

var callBound = require('call-bind/callBound');

var $slice = callBound('String.prototype.slice');

// https://262.ecma-international.org/12.0/#substring
module.exports = function substring(S, inclusiveStart, exclusiveEnd) {
	if (Type(S) !== 'String' || !IsIntegralNumber(inclusiveStart) || (arguments.length > 2 && !IsIntegralNumber(exclusiveEnd))) {
		throw new $TypeError('`S` must be a String, and `inclusiveStart` and `exclusiveEnd` must be integers');
	}
	return $slice(S, inclusiveStart, arguments.length > 2 ? exclusiveEnd : S.length);
};
