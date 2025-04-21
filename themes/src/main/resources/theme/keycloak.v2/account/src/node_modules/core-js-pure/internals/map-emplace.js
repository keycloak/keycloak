'use strict';
var call = require('../internals/function-call');
var aCallable = require('../internals/a-callable');
var anObject = require('../internals/an-object');

// `Map.prototype.emplace` method
// https://github.com/thumbsupep/proposal-upsert
module.exports = function emplace(key, handler) {
  var map = anObject(this);
  var get = aCallable(map.get);
  var has = aCallable(map.has);
  var set = aCallable(map.set);
  var value, inserted;
  if (call(has, map, key)) {
    value = call(get, map, key);
    if ('update' in handler) {
      value = handler.update(value, key, map);
      call(set, map, key, value);
    } return value;
  }
  inserted = handler.insert(key, map);
  call(set, map, key, inserted);
  return inserted;
};
