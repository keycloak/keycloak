'use strict';

try {
  module.exports = require('bindings')('bufferutil');
} catch (e) {
  module.exports = require('./fallback');
}
