'use strict';

var hasSymbols = require('has-symbols')();

module.exports = function runTests(hasOwn, t) {
	var badPropertyKey = { toString: function () { throw new SyntaxError('nope'); } };

	t['throws'](
		function () { hasOwn(null, badPropertyKey); },
		TypeError,
		'checks ToObject first'
	);

	t['throws'](
		function () { hasOwn({}, badPropertyKey); },
		SyntaxError,
		'checks ToPropertyKey next'
	);

	var obj = { a: 1 };
	t.equal('toString' in obj, true, 'object literal has non-own toString');
	t.equal(hasOwn(obj, 'toString'), false, 'toString is not an own property');
	t.equal(hasOwn(obj, 'a'), true, 'own property is recognized');

	t.equal(hasOwn([], 'length'), true, 'non-enumerable own property is recognized');

	t.test('Symbols', { skip: !hasSymbols }, function (st) {
		var o = {};
		o[Symbol.iterator] = true;
		st.equal(hasOwn(o, Symbol.iterator), true, 'own symbol is recognized');

		st.equal(hasOwn(Array.prototype, Symbol.iterator), true, 'built-in own symbol is recognized');

		st.end();
	});
};
