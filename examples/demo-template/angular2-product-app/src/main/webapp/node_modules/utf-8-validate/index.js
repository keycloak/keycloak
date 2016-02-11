'use strict';

try {
  module.exports = require('bindings')('validation');
} catch (e) {
  module.exports = require('./fallback');
}
