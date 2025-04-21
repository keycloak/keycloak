var classof = require('../internals/classof');
var uncurryThis = require('../internals/function-uncurry-this');

var slice = uncurryThis(''.slice);

module.exports = function (it) {
  return slice(classof(it), 0, 3) === 'Big';
};
