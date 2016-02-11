
/**
 * Module dependencies.
 */

var assert = require('assert');
var global;

try {
  // component
  global = require('global');
} catch (e) {
  // node.js
  global = require('../');
}

describe('global', function () {
  it('should return the `global` object', function () {
    var str = String(global);
    assert('[object global]' == str || '[object Window]' == str);
  });
});
