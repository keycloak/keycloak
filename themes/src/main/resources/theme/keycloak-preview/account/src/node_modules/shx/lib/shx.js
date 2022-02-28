#!/usr/bin/env node
'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.shx = shx;

var _shelljs = require('shelljs');

var _shelljs2 = _interopRequireDefault(_shelljs);

var _minimist = require('minimist');

var _minimist2 = _interopRequireDefault(_minimist);

var _help = require('./help');

var _help2 = _interopRequireDefault(_help);

var _config = require('./config');

var _printCmdRet = require('./printCmdRet');

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

var _fs = require('fs');

var _fs2 = _interopRequireDefault(_fs);

var _es6ObjectAssign = require('es6-object-assign');

var _es6ObjectAssign2 = _interopRequireDefault(_es6ObjectAssign);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toArray(arr) { return Array.isArray(arr) ? arr : Array.from(arr); }

_es6ObjectAssign2.default.polyfill(); // modifies the global object

_shelljs2.default.help = _help2.default;

var convertSedRegex = function convertSedRegex(args) {
  var newArgs = [];
  var lookingForSubstString = true;
  args.forEach(function (arg) {
    // A regex or replacement string can be any sequence of zero or more
    // (a) non-slashes or (b) escaped chars.
    var escapedChar = '\\\\.'; // This may match an escaped slash (i.e., "\/")
    var nonSlash = '[^/]';
    var nonSlashSequence = '(?:' + escapedChar + '|' + nonSlash + ')*';
    var sedPattern = '^s/(' + nonSlashSequence + ')/(' + nonSlashSequence + ')/(g?)$';
    var match = arg.match(new RegExp(sedPattern));
    if (match && lookingForSubstString) {
      var regexString = match[1].replace(/\\\//g, '/');
      var replacement = match[2].replace(/\\\//g, '/').replace(/\\./g, '.');
      var regexFlags = match[3];
      if (regexString === '') {
        // Unix sed gives an error if the pattern is the empty string, so we
        // forbid this case even though JavaScript's .replace() has well-defined
        // behavior.
        throw new Error('Bad sed pattern (empty regex)');
      }
      newArgs.push(new RegExp(regexString, regexFlags));
      newArgs.push(replacement);
      lookingForSubstString = false;
    } else {
      newArgs.push(arg);
    }
  });
  return newArgs;
};

function shx(argv) {
  var parsedArgs = (0, _minimist2.default)(argv.slice(2), { stopEarly: true, boolean: true });
  if (parsedArgs.version) {
    var shxVersion = require('../package.json').version;
    var shelljsVersion = require('shelljs/package.json').version;
    console.log('shx v' + shxVersion + ' (using ShellJS v' + shelljsVersion + ')');
    return _config.EXIT_CODES.SUCCESS;
  }

  var _parsedArgs$_ = _toArray(parsedArgs._),
      fnName = _parsedArgs$_[0],
      args = _parsedArgs$_.slice(1);

  if (!fnName) {
    console.error('Error: Missing ShellJS command name');
    console.error((0, _help2.default)());
    return _config.EXIT_CODES.SHX_ERROR;
  }

  // Load ShellJS plugins
  var CONFIG_PATH = _path2.default.join(process.cwd(), _config.CONFIG_FILE);
  if (_fs2.default.existsSync(CONFIG_PATH)) {
    var shxConfig = void 0;
    try {
      shxConfig = require(CONFIG_PATH);
    } catch (e) {
      throw new Error('Unable to read config file ' + _config.CONFIG_FILE);
    }

    (shxConfig.plugins || []).forEach(function (pluginName) {
      try {
        require(pluginName);
      } catch (e) {
        throw new Error('Unable to find plugin \'' + pluginName + '\'');
      }
    });
  }

  // Always load true-false plugin
  require('./plugin-true-false');

  // validate command
  if (typeof _shelljs2.default[fnName] !== 'function') {
    console.error('Error: Invalid ShellJS command: ' + fnName + '.');
    console.error((0, _help2.default)());
    return _config.EXIT_CODES.SHX_ERROR;
  } else if (_config.CMD_BLACKLIST.indexOf(fnName) > -1) {
    console.error('Warning: shx ' + fnName + ' is not supported');
    console.error('Please run `shx help` for a list of commands.');
    return _config.EXIT_CODES.SHX_ERROR;
  }

  var input = this !== null ? new _shelljs2.default.ShellString(this) : null;

  // Set shell.config with parsed options
  Object.assign(_shelljs2.default.config, parsedArgs);

  // Workaround for sed syntax
  var ret = void 0;
  if (fnName === 'sed') {
    var newArgs = convertSedRegex(args);
    ret = _shelljs2.default[fnName].apply(input, newArgs);
  } else {
    ret = _shelljs2.default[fnName].apply(input, args);
  }
  if (ret === null) ret = _shelljs2.default.ShellString('', '', 1);

  /* instanbul ignore next */
  var code = ret.hasOwnProperty('code') && ret.code;

  if ((fnName === 'pwd' || fnName === 'which') && !ret.match(/\n$/) && ret.length > 1) {
    ret += '\n';
  }

  // echo already prints
  if (fnName !== 'echo') (0, _printCmdRet.printCmdRet)(ret);
  if (typeof ret === 'boolean') {
    code = ret ? 0 : 1;
  }

  if (typeof code === 'number') {
    return code;
  } else if (_shelljs2.default.error()) {
    /* istanbul ignore next */
    return _config.EXIT_CODES.CMD_FAILED;
  }

  return _config.EXIT_CODES.SUCCESS;
}