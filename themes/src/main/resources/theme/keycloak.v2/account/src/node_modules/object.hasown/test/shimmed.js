'use strict';

require('../auto');

var test = require('tape');
var defineProperties = require('define-properties');
var callBind = require('call-bind');

var isEnumerable = Object.prototype.propertyIsEnumerable;
var functionsHaveNames = require('functions-have-names')();
var hasStrictMode = require('has-strict-mode')();

var runTests = require('./tests');

test('shimmed', function (t) {
	t.equal(Object.hasOwn.length, 2, 'Relect.hasOwn has a length of 2');
	t.test('Function name', { skip: !functionsHaveNames }, function (st) {
		st.equal(Object.hasOwn.name, 'hasOwn', 'Object.hasOwn has name "hasOwn"');
		st.end();
	});

	t.test('enumerability', { skip: !defineProperties.supportsDescriptors }, function (et) {
		et.equal(false, isEnumerable.call(Object, 'hasOwn'), 'Object.hasOwn is not enumerable');
		et.end();
	});

	t.test('bad array/this value', { skip: !hasStrictMode }, function (st) {
		st['throws'](function () { return Object.hasOwn.call(undefined); }, TypeError, 'undefined is not an object');
		st['throws'](function () { return Object.hasOwn.call(null); }, TypeError, 'null is not an object');
		st.end();
	});

	runTests(callBind(Object.hasOwn, Object), t);

	t.end();
});
