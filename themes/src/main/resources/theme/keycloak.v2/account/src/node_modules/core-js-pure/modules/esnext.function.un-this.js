var $ = require('../internals/export');
var uncurryThis = require('../internals/function-uncurry-this');
var aCallable = require('../internals/a-callable');

// `Function.prototype.unThis` method
// https://github.com/js-choi/proposal-function-un-this
$({ target: 'Function', proto: true, forced: true }, {
  unThis: function unThis() {
    return uncurryThis(aCallable(this));
  }
});
