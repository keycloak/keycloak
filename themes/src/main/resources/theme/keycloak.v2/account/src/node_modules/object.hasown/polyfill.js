'use strict';

var implementation = require('./implementation');

module.exports = function getPolyfill() {
	return Object.hasOwn || implementation;
};
