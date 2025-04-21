'use strict';

var $isFinite = require('../helpers/isFinite');
var timeConstants = require('../helpers/timeConstants');
var msPerSecond = timeConstants.msPerSecond;
var msPerMinute = timeConstants.msPerMinute;
var msPerHour = timeConstants.msPerHour;

var ToIntegerOrInfinity = require('./ToIntegerOrInfinity');

// https://ecma-international.org/ecma-262/5.1/#sec-15.9.1.11

module.exports = function MakeTime(hour, min, sec, ms) {
	if (!$isFinite(hour) || !$isFinite(min) || !$isFinite(sec) || !$isFinite(ms)) {
		return NaN;
	}
	var h = ToIntegerOrInfinity(hour);
	var m = ToIntegerOrInfinity(min);
	var s = ToIntegerOrInfinity(sec);
	var milli = ToIntegerOrInfinity(ms);
	var t = (h * msPerHour) + (m * msPerMinute) + (s * msPerSecond) + milli;
	return t;
};
