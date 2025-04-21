'use strict';

const path = require('path');
const pkgUp = require('./pkgUp').default;

exports.__esModule = true;

exports.default = function (cwd) {
  const fp = pkgUp({ cwd });
  return fp ? path.dirname(fp) : null;
};
