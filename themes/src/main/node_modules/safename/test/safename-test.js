var expect = require('chai').expect,
	safename = require('../'),
	fs = require('fs');

describe( 'safename', function () {
	it('return safe file name', function() {
		expect( safename('My file name 1234 Ñáëîò~') ).to.equal( 'My_file_name_1234_Naeio' );
	});
});

describe('safename#low', function () {
	it('return safe file name', function() {
		expect( safename.low('My file name 1234 Ñáëîò~') ).to.equal( 'My_file_name_1234_Naeio' );
	});
});


describe('safename#middle', function () {
	it('return safe file name', function() {
		expect( safename.middle('My file name 1234 Ñáëîò~') ).to.equal( 'My-file-name-1234-Naeio' );
	});
});

describe('safename#dot', function () {
	it('return safe file name', function() {
		expect( safename.dot('My file name 1234 Ñáëîò~') ).to.equal( 'My.file.name.1234.Naeio' );
	});
});


