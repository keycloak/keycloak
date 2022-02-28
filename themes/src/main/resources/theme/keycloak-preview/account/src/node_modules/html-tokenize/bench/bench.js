var fs = require('fs');
var tokenize = require('../');
var through = require('through2');

var src = fs.readFileSync(__dirname + '/input.html');
var start = Date.now();
var times = 100;

(function perf (n) {
    if (n === times) {
        console.log(((Date.now() - start) / times) + ' milliseconds');
        return;
    }
    var t = tokenize();
    t.pipe(through.obj(write, end));
    t.end(src);
    
    function write (row, enc, next) { next() }
    function end () { perf(n + 1) }
})(0);
