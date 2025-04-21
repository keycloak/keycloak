'use strict';

var ToObject = require('es-abstract/2021/ToObject');
var ToPropertyKey = require('es-abstract/2021/ToPropertyKey');
var HasOwnProperty = require('es-abstract/2021/HasOwnProperty');

module.exports = function hasOwn(O, P) {
	var obj = ToObject(O);
	var key = ToPropertyKey(P);
	return HasOwnProperty(obj, key);
};
