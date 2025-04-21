'use strict';

var $DateGetTime = require('call-bind/callBound')('Date.prototype.getTime');

// https://ecma-international.org/ecma-262/6.0/#sec-properties-of-the-date-prototype-object

module.exports = function thisTimeValue(value) {
	return $DateGetTime(value);
};
