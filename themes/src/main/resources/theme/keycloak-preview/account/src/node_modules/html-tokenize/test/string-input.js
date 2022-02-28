var tokenize = require('../');
var test = require('tape');

test('string input', function (t) {
    var expected = [
        ['open', '<div>'],
        ['close', '</div>']
    ];

    var tok = tokenize();
    tok.on('data', function (row) {
        var exp = expected.shift();
        t.equal(exp[0], row[0]);
        t.equal(exp[1], row[1].toString());
    });
    tok.on('end', function () {
        t.end();
    });
    tok.end('<div></div>');
});

