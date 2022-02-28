'use strict';

var _plugin = require('shelljs/plugin');

var _plugin2 = _interopRequireDefault(_plugin);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var shellTrue = function shellTrue() {
  return '';
};

var shellFalse = function shellFalse() {
  _plugin2.default.error('', { silent: true });
};

_plugin2.default.register('true', shellTrue, {
  allowGlobbing: false,
  wrapOutput: true
});

_plugin2.default.register('false', shellFalse, {
  allowGlobbing: false,
  wrapOutput: true
});