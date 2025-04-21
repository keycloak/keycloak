'use strict';

var abs = require('./abs');
var floor = require('./floor');
var Type = require('./Type');

var $isNaN = require('../helpers/isNaN');
var $isFinite = require('../helpers/isFinite');

// https://tc39.es/ecma262/#sec-isintegralnumber

module.exports = function IsIntegralNumber(argument) {
	if (Type(argument) !== 'Number' || $isNaN(argument) || !$isFinite(argument)) {
		return false;
	}
	var absValue = abs(argument);
	return floor(absValue) === absValue;
};
