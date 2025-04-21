'use strict';

var AddEntriesFromIterable = require('es-abstract/2021/AddEntriesFromIterable');
var CreateDataPropertyOrThrow = require('es-abstract/2021/CreateDataPropertyOrThrow');
var RequireObjectCoercible = require('es-abstract/2021/RequireObjectCoercible');
var ToPropertyKey = require('es-abstract/2021/ToPropertyKey');

var adder = function addDataProperty(key, value) {
	var O = this; // eslint-disable-line no-invalid-this
	var propertyKey = ToPropertyKey(key);
	CreateDataPropertyOrThrow(O, propertyKey, value);
};

module.exports = function fromEntries(iterable) {
	RequireObjectCoercible(iterable);

	return AddEntriesFromIterable({}, iterable, adder);
};
