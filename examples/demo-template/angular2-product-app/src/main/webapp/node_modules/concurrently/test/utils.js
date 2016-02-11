var childProcess = require('child_process');
var _ = require('lodash');
var Promise = require('bluebird');
var shellQuote = require('shell-quote');

function run(cmd, opts) {
    opts = _.merge({
        pipe: true,
        cwd: undefined,
        callback: function(child) {
            // Since we return promise, we need to provide
            // this callback if one wants to access the child
            // process reference
            // Called immediately after successful child process
            // spawn
        }
    }, opts);

    var child;
    var parts = shellQuote.parse(cmd);
    try {
        child = childProcess.spawn(_.head(parts), _.tail(parts), {
            cwd: opts.cwd,
            stdio: opts.pipe ? "inherit" : null
        });
    } catch (e) {
        return Promise.reject(e);
    }
    opts.callback(child);

    return new Promise(function(resolve, reject) {
        child.on('error', function(err) {
            reject(err);
        });

        child.on('close', function(exitCode) {
            resolve(exitCode);
        });
    });
}

module.exports = {
    run: run
};