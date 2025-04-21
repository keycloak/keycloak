var _Object$assign = require("@babel/runtime-corejs3/core-js/object/assign");

var _bindInstanceProperty = require("@babel/runtime-corejs3/core-js/instance/bind");

function _extends() {
  var _context;

  module.exports = _extends = _Object$assign ? _bindInstanceProperty(_context = _Object$assign).call(_context) : function (target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i];

      for (var key in source) {
        if (Object.prototype.hasOwnProperty.call(source, key)) {
          target[key] = source[key];
        }
      }
    }

    return target;
  }, module.exports.__esModule = true, module.exports["default"] = module.exports;
  return _extends.apply(this, arguments);
}

module.exports = _extends, module.exports.__esModule = true, module.exports["default"] = module.exports;