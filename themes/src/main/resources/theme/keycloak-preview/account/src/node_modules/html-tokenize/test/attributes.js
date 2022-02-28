var fs = require('fs');
var tokenize = require('../');
var test = require('tape');
var through = require('through2');

var expected = require('./attributes/expected.json');

test('attributes', function (t) {
    t.plan(expected.length * 2);
    
    fs.createReadStream(__dirname + '/attributes/index.html')
        .pipe(tokenize())
        .pipe(through.obj(function (row, enc, next) {
            var exp = expected.shift();
            t.equal(row[0], exp[0]);
            t.equal(row[1].toString(), exp[1]);
            next();
        }))
    ;
});
