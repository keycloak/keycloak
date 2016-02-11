var fs = require('fs');
var path = require('path');
var cp = require('child_process');
var LRU = require('lru-cache');

var isWin = process.platform === 'win32';
var shebangCache = LRU({ max: 50, maxAge: 30 * 1000 });

function readShebang(command) {
    var buffer;
    var fd;
    var match;
    var shebang;

    // Resolve command to an absolute path if it contains /
    if (command.indexOf(path.sep) !== -1) {
        command = path.resolve(command);
    }

    // Check if its resolved in the cache
    shebang = shebangCache.get(command);
    if (shebang) {
        return shebang;
    }

    // Read the first 150 bytes from the file
    buffer = new Buffer(150);

    try {
        fd = fs.openSync(command, 'r');
        fs.readSync(fd, buffer, 0, 150, 0);
    } catch (e) {}

    // Check if it is a shebang
    match = buffer.toString().trim().match(/\#\!\/usr\/bin\/env ([^\r\n]+)/i);
    shebang = match && match[1];

    // Store the shebang in the cache
    shebangCache.set(command, shebang);

    return shebang;
}

function escapeArg(arg, quote) {
    // Convert to string
    arg = '' + arg;

    // If we are not going to quote the argument,
    // escape shell metacharacters, including double and single quotes:
    if (!quote) {
        arg = arg.replace(/([\(\)%!\^<>&|;,"' ])/g, '^$1');
    } else {
        // Sequence of backslashes followed by a double quote:
        // double up all the backslashes and escape the double quote
        arg = arg.replace(/(\\*)"/gi, '$1$1\\"');

        // Sequence of backslashes followed by the end of the string
        // (which will become a double quote later):
        // double up all the backslashes
        arg = arg.replace(/(\\*)$/, '$1$1');

        // All other backslashes occur literally

        // Quote the whole thing:
        arg = '"' + arg + '"';
    }

    return arg;
}

function escapeCommand(command) {
    // Do not escape if this command is not dangerous..
    // We do this so that commands like "echo" or "ifconfig" work
    // Quoting them, will make them unnaccessible
    return /^[a-z0-9_-]+$/i.test(command) ? command : escapeArg(command, true);
}

function spawn(command, args, options) {
    var applyQuotes;
    var shebang;

    args = args || [];
    options = options || {};

    // Use node's spawn if not on windows
    if (!isWin) {
        return cp.spawn(command, args, options);
    }

    // Detect & add support for shebangs
    shebang = readShebang(command);
    if (shebang) {
        args.unshift(command);
        command = shebang;
    }

    // Escape command & arguments
    applyQuotes = command !== 'echo';  // Do not quote arguments for the special "echo" command
    command = escapeCommand(command);
    args = args.map(function (arg) {
        return escapeArg(arg, applyQuotes);
    });

    // Use cmd.exe
    args = ['/s', '/c', '"' + command + (args.length ? ' ' + args.join(' ') : '') + '"'];
    command = process.env.comspec || 'cmd.exe';

    // Tell node's spawn that the arguments are already escaped
    options.windowsVerbatimArguments = true;

    return cp.spawn(command, args, options);
}

module.exports = spawn;
module.exports.spawn = spawn;
