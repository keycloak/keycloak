'use strict';

// eslint-disable-next-line consistent-return
module.exports = function runSymbolTests(t) {
	t.equal(typeof Symbol, 'function', 'global Symbol is a function');
	t.ok(Symbol.toStringTag, 'Symbol.toStringTag exists');

	if (typeof Symbol !== 'function' || !Symbol.toStringTag) { return false; }

	var obj = {};
	obj[Symbol.toStringTag] = 'test';

	t.equal(Object.prototype.toString.call(obj), '[object test]');
};
