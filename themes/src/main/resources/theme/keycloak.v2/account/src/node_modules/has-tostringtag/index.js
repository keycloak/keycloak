'use strict';

var hasSymbols = require('has-symbols');

module.exports = function hasToStringTag() {
	return hasSymbols() && typeof Symbol.toStringTag === 'symbol';
};
