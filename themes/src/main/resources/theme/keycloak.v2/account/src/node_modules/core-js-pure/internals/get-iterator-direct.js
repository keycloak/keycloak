var aCallable = require('../internals/a-callable');
var anObject = require('../internals/an-object');

module.exports = function (obj) {
  return {
    iterator: obj,
    next: aCallable(anObject(obj).next)
  };
};
