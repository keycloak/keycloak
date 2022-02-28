var fs = require('fs');
var tokenize = require('../');
var test = require('tape');
var through = require('through2');

var expected = [
    [ 'text', '3x + 6 < 8' ]
];

test('loose angle brackets buffer boundary', function (t) {
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
    input.write('3x + 6 <');
    input.end(' 8');
});
