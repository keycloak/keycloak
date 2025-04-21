'use strict';

var getPolyfill = require('./polyfill');
var define = require('define-properties');

module.exports = function shimObjectHasOwn() {
	var polyfill = getPolyfill();
	define(
		Object,
		{ hasOwn: polyfill },
		{ hasOwn: function () { return Object.hasOwn !== polyfill; } }
	);
	return polyfill;
};
