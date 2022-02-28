#!/usr/bin/env node

var fs = require('fs');
var tokenize = require('../');
var through = require('through2');

var minimist = require('minimist');
var argv = minimist(process.argv.slice(2), { alias: { h: 'help' } });
if (argv.help) return usage(0);

var input = argv._[0] && argv._[0] !== '-'
    ? fs.createReadStream(argv._[0])
    : process.stdin
;
var format = through.obj(function (row, enc, next) {
    row[1] = row[1].toString('utf8');
    this.push(JSON.stringify(row) + '\n');
    next();
});
input.pipe(tokenize()).pipe(format).pipe(process.stdout);

function usage (code) {
    var r = fs.createReadStream(__dirname + '/usage.txt');
    r.pipe(process.stdout);
    r.on('end', function () { if (code) process.exit(code) });
}
