#!/usr/bin/env node
'use strict';

var _shx = require('./shx');

var _minimist = require('minimist');

var _minimist2 = _interopRequireDefault(_minimist);

var _config = require('./config');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var parsedArgs = (0, _minimist2.default)(process.argv.slice(2), { stopEarly: true, boolean: true });

// `input` is null if we're running from a TTY, or a string of all stdin if
// running from the right-hand side of a pipe
var run = function run(input) {
  // Pass stdin to shx as the 'this' parameter
  process.exitCode = _shx.shx.call(input, process.argv);

  // We use process.exitCode to ensure we don't terminate the process before
  // streams finish. See:
  //   https://github.com/shelljs/shx/issues/85
};

// ShellJS doesn't support input streams, so we have to collect all input first
if ((0, _config.shouldReadStdin)(parsedArgs._)) {
  // Read all stdin first, and then pass that onto ShellJS
  var chunks = [];
  process.stdin.on('data', function (data) {
    return chunks.push(data);
  });
  process.stdin.on('end', function () {
    return run(chunks.join(''));
  });
} else {
  // There's no stdin, so we can immediately invoke the ShellJS function
  run(null);
}