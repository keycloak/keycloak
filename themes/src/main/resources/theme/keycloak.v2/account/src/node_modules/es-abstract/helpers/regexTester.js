'use strict';

var callBound = require('call-bind/callBound');

var $exec = callBound('RegExp.prototype.exec');

module.exports = function regexTester(regex) {
	return function test(s) { return $exec(regex, s) !== null; };
};
