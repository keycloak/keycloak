'use strict';
exports.__esModule = true;

const findUp = require('find-up');

exports.default = function pkgUp(opts) {
  return findUp.sync('package.json', opts);
};
