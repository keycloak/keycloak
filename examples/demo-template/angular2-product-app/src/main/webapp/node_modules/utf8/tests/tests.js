(function(root) {
	'use strict';

	var noop = Function.prototype;

	var load = (typeof require == 'function' && !(root.define && define.amd)) ?
		require :
		(!root.document && root.java && root.load) || noop;

	var QUnit = (function() {
		return root.QUnit || (
			root.addEventListener || (root.addEventListener = noop),
			root.setTimeout || (root.setTimeout = noop),
			root.QUnit = load('../node_modules/qunitjs/qunit/qunit.js') || root.QUnit,
			addEventListener === noop && delete root.addEventListener,
			root.QUnit
		);
	}());

	var qe = load('../node_modules/qunit-extras/qunit-extras.js');
	if (qe) {
		qe.runInContext(root);
	}

	/** The `utf8` object to test */
	var utf8 = root.utf8 || (root.utf8 = (
		utf8 = load('../utf8.js') || root.utf8,
		utf8 = utf8.utf8 || utf8
	));

	/*--------------------------------------------------------------------------*/

	function forEach(array, fn) {
		var index = -1;
		var length = array.length;
		while (++index < length) {
			fn(array[index]);
		}
	}

	// Quick and dirty test to see if we’re in Node & need extended tests
	var runExtendedTests = (function() {
		try {
			return process.argv[0] == 'node' && process.argv[2] == '--extended';
		} catch(error) { }
	}());

	var data = [
		// 1-byte
		{
			'codePoint': 0x0000,
			'decoded': '\0',
			'encoded': '\0'
		},
		{
			'codePoint': 0x005C,
			'decoded': '\x5C',
			'encoded': '\x5C'
		},
		{
			'codePoint': 0x007F,
			'decoded': '\x7F',
			'encoded': '\x7F'
		},

		// 2-byte
		{
			'codePoint': 0x0080,
			'decoded': '\x80',
			'encoded': '\xC2\x80'
		},
		{
			'codePoint': 0x05CA,
			'decoded': '\u05CA',
			'encoded': '\xD7\x8A'
		},
		{
			'codePoint': 0x07FF,
			'decoded': '\u07FF',
			'encoded': '\xDF\xBF',
		},

		// 3-byte
		{
			'codePoint': 0x0800,
			'decoded': '\u0800',
			'encoded': '\xE0\xA0\x80',
		},
		{
			'codePoint': 0x2C3C,
			'decoded': '\u2C3C',
			'encoded': '\xE2\xB0\xBC'
		},
		{
			'codePoint': 0xFFFF,
			'decoded': '\uFFFF',
			'encoded': '\xEF\xBF\xBF'
		},
		// unmatched surrogate halves
		// high surrogates: 0xD800 to 0xDBFF
		{
			'codePoint': 0xD800,
			'decoded': '\uD800',
			'encoded': '\xED\xA0\x80',
			'error': true
		},
		{
			'description': 'High surrogate followed by another high surrogate',
			'decoded': '\uD800\uD800',
			'encoded': '\xED\xA0\x80\xED\xA0\x80',
			'error': true
		},
		{
			'description': 'High surrogate followed by a symbol that is not a surrogate',
			'decoded': '\uD800A',
			'encoded': '\xED\xA0\x80A',
			'error': true
		},
		{
			'description': 'Unmatched high surrogate, followed by a surrogate pair, followed by an unmatched high surrogate',
			'decoded': '\uD800\uD834\uDF06\uD800',
			'encoded': '\xED\xA0\x80\xF0\x9D\x8C\x86\xED\xA0\x80',
			'error': true
		},
		{
			'codePoint': 0xD9AF,
			'decoded': '\uD9AF',
			'encoded': '\xED\xA6\xAF',
			'error': true
		},
		{
			'codePoint': 0xDBFF,
			'decoded': '\uDBFF',
			'encoded': '\xED\xAF\xBF',
			'error': true
		},
		// low surrogates: 0xDC00 to 0xDFFF
		{
			'codePoint': 0xDC00,
			'decoded': '\uDC00',
			'encoded': '\xED\xB0\x80',
			'error': true
		},
		{
			'description': 'Low surrogate followed by another low surrogate',
			'decoded': '\uDC00\uDC00',
			'encoded': '\xED\xB0\x80\xED\xB0\x80',
			'error': true
		},
		{
			'description': 'Low surrogate followed by a symbol that is not a surrogate',
			'decoded': '\uDC00A',
			'encoded': '\xED\xB0\x80A',
			'error': true
		},
		{
			'description': 'Unmatched low surrogate, followed by a surrogate pair, followed by an unmatched low surrogate',
			'decoded': '\uDC00\uD834\uDF06\uDC00',
			'encoded': '\xED\xB0\x80\xF0\x9D\x8C\x86\xED\xB0\x80',
			'error': true
		},
		{
			'codePoint': 0xDEEE,
			'decoded': '\uDEEE',
			'encoded': '\xED\xBB\xAE',
			'error': true
		},
		{
			'codePoint': 0xDFFF,
			'decoded': '\uDFFF',
			'encoded': '\xED\xBF\xBF',
			'error': true
		},

		// 4-byte
		{
			'codePoint': 0x010000,
			'decoded': '\uD800\uDC00',
			'encoded': '\xF0\x90\x80\x80'
		},
		{
			'codePoint': 0x01D306,
			'decoded': '\uD834\uDF06',
			'encoded': '\xF0\x9D\x8C\x86'
		},
		{
			'codePoint': 0x10FFF,
			'decoded': '\uDBFF\uDFFF',
			'encoded': '\xF4\x8F\xBF\xBF'
		}
	];

	if (runExtendedTests) {
		data = data.concat(require('./data.json'));
	}

	// `throws` is a reserved word in ES3; alias it to avoid errors
	var raises = QUnit.assert['throws'];

	// explicitly call `QUnit.module()` instead of `module()`
	// in case we are in a CLI environment
	QUnit.module('utf8.js');

	test('encode/decode', function() {
		forEach(data, function(object) {
			var description = object.description || 'U+' + object.codePoint.toString(16).toUpperCase();
			;
			if (object.error) {
				raises(
					function() {
						utf8.decode(object.encoded);
					},
					Error,
					'Error: non-scalar value detected'
				);
				raises(
					function() {
						utf8.encode(object.decoded);
					},
					Error,
					'Error: non-scalar value detected'
				);
			} else {
				equal(
					object.encoded,
					utf8.encode(object.decoded),
					'Encoding: ' + description
				);
				equal(
					object.decoded,
					utf8.decode(object.encoded),
					'Decoding: ' + description
				);
			}
		});

		// Error handling
		raises(
			function() {
				utf8.decode('\uFFFF');
			},
			Error,
			'Error: invalid UTF-8 detected'
		);
		raises(
			function() {
				utf8.decode('\xE9\x00\x00');
			},
			Error,
			'Error: invalid continuation byte (4-byte sequence expected)'
		);
		raises(
			function() {
				utf8.decode('\xC2\uFFFF');
			},
			Error,
			'Error: invalid continuation byte'
		);
		raises(
			function() {
				utf8.decode('\xF0\x9D');
			},
			Error,
			'Error: invalid byte index'
		);
	});

	/*--------------------------------------------------------------------------*/

	// configure QUnit and call `QUnit.start()` for
	// Narwhal, Node.js, PhantomJS, Rhino, and RingoJS
	if (!root.document || root.phantom) {
		QUnit.config.noglobals = true;
		QUnit.start();
	}
}(typeof global == 'object' && global || this));
