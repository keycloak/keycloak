var expect = require('chai').expect,
	Filesaver = require('../src/Filesaver'),
	fs = require('fs');

var deleteFolderRecursive = function (path) {
	if (fs.existsSync(path)) {
		fs.readdirSync(path).forEach( function (file, index) {
			var curPath;
			curPath = path + "/" + file;
			if (fs.statSync( curPath ).isDirectory()) {
				return deleteFolderRecursive( curPath );
			} else {
				return fs.unlinkSync( curPath );
			}
		});
		return fs.rmdirSync( path );
	}
};


var folders = {
	books: './uploads/books',
	images: './uploads/img'
};


after( function (done) {
	deleteFolderRecursive('./uploads');
	done();
});


before( function (done) {
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/one.txt'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/two'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/three'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/four'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/five.txt'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/six.txt'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/seven.txt'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/eight.txt'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/n a m e.txt'));
	fs.createReadStream('./test/assets/test.txt').pipe(fs.createWriteStream('./test/assets/n a m e .txt'));
	done();
});



describe( 'Filesaver constructor', function () {
	var filesaver = new Filesaver( {folders: folders} );

	it('Filesaver constructor create folders if necesary', function() {
		expect( fs.existsSync('./uploads/books') && fs.existsSync('./uploads/books') ).to.equal(true);
	});
});

describe('filesaver#folder', function () {
	var filesaver = new Filesaver();
	filesaver.folder( 'things', './uploads/things' );
	it( 'add a new folder to filesaver.folders', function () {
		expect( filesaver.folders.things ).to.equal( './uploads/things' );
	});
	it('create folder if necessary', function (done) {
		fs.exists( './uploads/things', function (exists){
			expect( exists ).to.equal( true );
			done();
		});
	});
});


describe('filesaver#put', function () {
	var filesaver = new Filesaver( {folders: folders} );
	it('saves a file at newPath argument', function (done) {
		filesaver.put( 'books', './test/assets/one.txt', 'ONE.txt', function () {
			fs.exists( './uploads/books/ONE.txt', function (exists) {
				expect( exists ).to.equal( true );
				done();
			});
		});
	});

	it('if target is ommited: saves file with same name as origin file', function (done) {
		filesaver.put( 'books', './test/assets/two', function () {
			fs.exists( './uploads/books/two', function (exists) {
				expect( exists ).to.equal( true );
				done();
			});
		});
	});

	it( 'use safenames', function (done) {
		var filesaver = new Filesaver({folders: folders, safenames: true });
		filesaver.put( 'books', './test/assets/n a m e .txt', function () {
			fs.exists( './uploads/books/n_a_m_e_.txt', function (exists) {
				expect( exists ).to.equal( true );
				done();
			});
		});
	});

	it( 'works without callback', function (done) {
		var filesaver = new Filesaver({folders: folders});
		filesaver.put( 'books', './test/assets/seven.txt' );
		setTimeout( function () {
			fs.exists( './uploads/books/seven.txt', function (exists) {
				expect( exists ).to.equal( true );
				done();
			});
		}, 500);
	});
});


describe( 'filesaver#add', function () {

	it( 'saves a file at target argument', function (done) {
		var filesaver = new Filesaver( {folders: folders} );
		filesaver.add( 'books', './test/assets/three', 'three', function () {
			fs.exists( './uploads/books/three', function (exists) {
				expect( exists ).to.equal( true );
				done();
			});
		});
	});

	it( 'if target is ommited: saves file with same name as origin file', function (done) {
		var filesaver = new Filesaver( {folders: folders} );
		filesaver.add( 'books', './test/assets/four', function () {
			fs.exists( './uploads/books/four', function (exists) {
				expect( exists ).to.equal( true );
				done();
			});
		});
	});

	it( 'extend basename with suffix', function (done) {
		var filesaver = new Filesaver( {folders: folders} );
		filesaver.add( 'books', './test/assets/five.txt', 'five.txt', function () {
			filesaver.add( 'books', './test/assets/six.txt', 'five.txt', function () {
				fs.exists( './uploads/books/five_1.txt', function (exists) {
					expect( exists ).to.equal( true );
					done();
				});
			});
		});
	});


	it( 'use safenames', function (done) {
		var filesaver = new Filesaver({folders: folders, safenames: true });
		filesaver.add( 'books', './test/assets/n a m e.txt', function () {
			fs.exists( './uploads/books/n_a_m_e.txt', function (exists) {
				expect( exists ).to.equal( true );
				done();
			});
		});
	});

	it( 'works without callback', function (done) {
		var filesaver = new Filesaver({folders: folders});
		filesaver.add( 'books', './test/assets/eight.txt' );
		setTimeout( function () {
			fs.exists( './uploads/books/eight.txt', function (exists) {
				expect( exists ).to.equal( true );
				done();
			});
		}, 500);
	});
});



