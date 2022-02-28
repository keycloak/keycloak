var fs = require('fs');
var tokenize = require('../');
var test = require('tape');
var through = require('through2');

test('open tag ">" across buffer boundary', function (t) {
    var expected = [
        [ 'open', '<html>' ],
        [ 'open', '<body>' ],
        [ 'close', '</body>' ],
        [ 'close', '</html>' ]
    ];
    
    t.plan(expected.length * 2);
    var input = through();
    input
        .pipe(tokenize())
        .pipe(through.obj(function (row, enc, next) {
            var exp = expected.shift();
            t.equal(row[0], exp[0]);
            t.equal(row[1].toString(), exp[1]);
            next();
        }))
    ;
    
    input.write('<html>');
    input.write('<body');
    input.write('>');
    input.write('</body>');
    input.write('</html>');
    input.end();
});

test('open tag "<" across buffer boundary', function (t) {
    var expected = [
        [ 'open', '<html>' ],
        [ 'open', '<body>' ],
        [ 'close', '</body>' ],
        [ 'close', '</html>' ]
    ];

    t.plan(expected.length * 2);
    var input = through();
    input
        .pipe(tokenize())
        .pipe(through.obj(function (row, enc, next) {
            var exp = expected.shift();
            t.equal(row[0], exp[0]);
            t.equal(row[1].toString(), exp[1]);
            next();
        }))
    ;

    input.write('<html>');
    input.write('<');
    input.write('body>');
    input.write('</body>');
    input.write('</html>');
    input.end();
});

test('close tag ">" across buffer boundary', function (t) {
    var expected = [
        [ 'open', '<html>' ],
        [ 'open', '<body>' ],
        [ 'close', '</body>' ],
        [ 'close', '</html>' ]
    ];
    
    t.plan(expected.length * 2);
    var input = through();
    input
        .pipe(tokenize())
        .pipe(through.obj(function (row, enc, next) {
            var exp = expected.shift();
            t.equal(row[0], exp[0]);
            t.equal(row[1].toString(), exp[1]);
            next();
        }))
    ;
    
    input.write('<html>');
    input.write('<body>');
    input.write('</body');
    input.write('>');
    input.write('</html>');
    input.end();
});
