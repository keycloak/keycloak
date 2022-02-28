var fs = require('fs');
var tokenize = require('../');
var test = require('tape');
var through = require('through2');

var expected = require('./loose_angle_brackets/expected.json');

test('loose angle brackets', function (t) {
    t.plan(expected.length * 2);
    
    fs.createReadStream(__dirname + '/loose_angle_brackets/index.html')
        .pipe(tokenize())
        .pipe(through.obj(function (row, enc, next) {
            var exp = expected.shift();
            t.equal(row[0], exp[0]);
            t.equal(row[1].toString(), exp[1]);
            next();
        }))
    ;
});
