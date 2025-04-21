'use strict';

var RequireObjectCoercible = require('es-abstract/2021/RequireObjectCoercible');
var callBound = require('call-bind/callBound');

var $isEnumerable = callBound('Object.prototype.propertyIsEnumerable');
var $push = callBound('Array.prototype.push');

module.exports = function values(O) {
	var obj = RequireObjectCoercible(O);
	var vals = [];
	for (var key in obj) {
		if ($isEnumerable(obj, key)) { // checks own-ness as well
			$push(vals, obj[key]);
		}
	}
	return vals;
};
