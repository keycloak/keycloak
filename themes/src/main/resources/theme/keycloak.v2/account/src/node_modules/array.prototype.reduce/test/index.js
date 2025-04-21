'use strict';

var implementation = require('../');
var callBind = require('call-bind');
var test = require('tape');
var runTests = require('./tests');

test('as a function', function (t) {
	t.test('bad array/this value', function (st) {
		st['throws'](callBind(implementation, null, undefined, 'a'), TypeError, 'undefined is not an object');
		st['throws'](callBind(implementation, null, null, 'a'), TypeError, 'null is not an object');
		st.end();
	});

	runTests(implementation, t);

	t.end();
});
