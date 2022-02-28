'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.shouldReadStdin = exports.SHELLJS_PIPE_INFO = exports.CONFIG_FILE = exports.OPTION_BLACKLIST = exports.CMD_BLACKLIST = exports.EXIT_CODES = undefined;

var _minimist = require('minimist');

var _minimist2 = _interopRequireDefault(_minimist);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var EXIT_CODES = exports.EXIT_CODES = {
  SHX_ERROR: 27, // https://xkcd.com/221/
  CMD_FAILED: 1, // TODO: Once shelljs/shelljs#269 lands, use `error()`
  SUCCESS: 0
};

var CMD_BLACKLIST = exports.CMD_BLACKLIST = ['cd', 'pushd', 'popd', 'dirs', 'set', 'exit', 'exec', 'ShellString'];

var OPTION_BLACKLIST = exports.OPTION_BLACKLIST = ['globOptions', // we don't have good control over globbing in the shell
'execPath', // we don't currently support exec
'bufLength', // we don't use buffers in shx
'maxdepth'];

var CONFIG_FILE = exports.CONFIG_FILE = '.shxrc.json';

var SHELLJS_PIPE_INFO = exports.SHELLJS_PIPE_INFO = {
  cat: { minArgs: 1 },
  grep: { minArgs: 2 },
  head: { minArgs: 1 },
  sed: { minArgs: 2 },
  sort: { minArgs: 1 },
  tail: { minArgs: 1 },
  uniq: { minArgs: 1 }
};

// All valid options
var allOptionsList = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

var shouldReadStdin = exports.shouldReadStdin = function shouldReadStdin(args) {
  var cmd = args[0];
  var cmdInfo = SHELLJS_PIPE_INFO[cmd];
  var parsedArgs = (0, _minimist2.default)(args.slice(1), {
    stopEarly: true,
    boolean: allOptionsList // treat all short options as booleans
  });
  var requiredNumArgs = cmdInfo ? cmdInfo.minArgs : -1;

  // If a non-boolean option is passed in, increment the required argument
  // count (this is the case for `-n` for `head` and `tail`)
  if (parsedArgs.n && (cmd === 'head' || cmd === 'tail')) {
    requiredNumArgs++;
  }

  return Boolean(!process.stdin.isTTY && parsedArgs._.length < requiredNumArgs);
};