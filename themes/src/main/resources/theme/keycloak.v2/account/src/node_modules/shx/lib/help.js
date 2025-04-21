'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _shelljs = require('shelljs');

var _shelljs2 = _interopRequireDefault(_shelljs);

var _config = require('./config');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

// Global options defined directly in shx.
var locallyDefinedOptions = ['version'];

var shxOptions = Object.keys(_shelljs2.default.config).filter(function (key) {
  return typeof _shelljs2.default.config[key] !== 'function';
}).filter(function (key) {
  return _config.OPTION_BLOCKLIST.indexOf(key) === -1;
}).concat(locallyDefinedOptions).map(function (key) {
  return '    * --' + key;
});

exports.default = function () {
  // Note: compute this at runtime so that we have all plugins loaded.
  var commandList = Object.keys(_shelljs2.default).filter(function (cmd) {
    return typeof _shelljs2.default[cmd] === 'function';
  }).filter(function (cmd) {
    return _config.CMD_BLOCKLIST.indexOf(cmd) === -1;
  }).map(function (cmd) {
    return '    * ' + cmd;
  });

  return '\nshx: A wrapper for shelljs UNIX commands.\n\nUsage: shx [shx-options] <command> [cmd-options] [cmd-args]\n\nExample:\n\n    $ shx ls .\n    foo.txt\n    baz.js\n    $ shx rm -rf *.txt && shx ls .\n    baz.js\n\nCommands:\n\n' + commandList.join('\n') + '\n\nShx Options (please see https://github.com/shelljs/shelljs for details on each\noption):\n\n' + shxOptions.join('\n') + '\n';
};