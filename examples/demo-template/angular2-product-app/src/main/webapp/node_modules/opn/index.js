'use strict';
var path = require('path');
var childProcess = require('child_process');
var objectAssign = require('object-assign');

module.exports = function (target, opts, cb) {
	if (typeof target !== 'string') {
		throw new Error('Expected a `target`');
	}

	if (typeof opts === 'function') {
		cb = opts;
		opts = null;
	}

	opts = objectAssign({wait: true}, opts);

	var cmd;
	var appArgs = [];
	var args = [];
	var cpOpts = {};

	if (Array.isArray(opts.app)) {
		appArgs = opts.app.slice(1);
		opts.app = opts.app[0];
	}

	if (process.platform === 'darwin') {
		cmd = 'open';

		if (cb && opts.wait) {
			args.push('-W');
		}

		if (opts.app) {
			args.push('-a', opts.app);
		}

		if (appArgs.length > 0) {
			args.push('--args');
			args = args.concat(appArgs);
		}
	} else if (process.platform === 'win32') {
		cmd = 'cmd';
		args.push('/c', 'start');
		target = target.replace(/&/g, '^&');

		if (cb && opts.wait) {
			args.push('/wait');
		}

		if (opts.app) {
			args.push(opts.app);
		}

		if (appArgs.length > 0) {
			args = args.concat(appArgs);
		}
	} else {
		if (opts.app) {
			cmd = opts.app;
		} else {
			cmd = path.join(__dirname, 'xdg-open');
		}

		if (appArgs.length > 0) {
			args = args.concat(appArgs);
		}

		if (!(cb && opts.wait)) {
			// xdg-open will block the process unless
			// stdio is ignored even if it's unref'd
			cpOpts.stdio = 'ignore';
		}
	}

	args.push(target);

	var cp = childProcess.spawn(cmd, args, cpOpts);

	if (cb) {
		cp.once('error', cb);

		cp.once('close', function (code) {
			if (code > 0) {
				cb(new Error('Exited with code ' + code));
				return;
			}

			cb();
		});
	} else {
		cp.unref();
	}

	return cp;
};
