'use strict';

var define = require('define-properties');
var getPolyfill = require('./polyfill');

module.exports = function shimArrayPrototypeReduce() {
	var polyfill = getPolyfill();
	define(
		Array.prototype,
		{ reduce: polyfill },
		{ reduce: function () { return Array.prototype.reduce !== polyfill; } }
	);
	return polyfill;
};
