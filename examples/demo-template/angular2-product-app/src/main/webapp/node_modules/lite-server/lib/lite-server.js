'use strict';
/**
 * lite-server : Simple server for angular/SPA projects
 *
 * Simply loads some default browser-sync options that apply to SPAs,
 * applies custom config overrides from user's own local `bs-config.{js|json}` file,
 * and launches browser-sync.
 */
var browserSync = require('browser-sync').create();
var path = require('path');
var _ = require('lodash');

// Load defaults
var options = require('./config-defaults');

// Load optional browser-sync config file from user's project dir
var bsConfigPath = path.resolve('bs-config');
var overrides = {};
try {
  overrides = require(bsConfigPath);
} catch (err) {
  if (err.code && err.code === 'MODULE_NOT_FOUND') {
    console.info(
      'Did not detect a `bs-config.json` or `bs-config.js` override file.' +
      ' Using lite-server defaults...'
    );
  } else {
    throw(err);
  }
}
_.merge(options, overrides);

// Fixes browsersync error when overriding middleware array
if (options.server.middleware) {
  options.server.middleware = _.compact(options.server.middleware);
}

console.log('** browser-sync options **');
console.log(options);

// Run browser-sync
browserSync.init(options);
