var _Object$create = require("@babel/runtime-corejs3/core-js/object/create");

var _Object$defineProperty = require("@babel/runtime-corejs3/core-js/object/define-property");

var setPrototypeOf = require("./setPrototypeOf.js");

function _inherits(subClass, superClass) {
  if (typeof superClass !== "function" && superClass !== null) {
    throw new TypeError("Super expression must either be null or a function");
  }

  subClass.prototype = _Object$create(superClass && superClass.prototype, {
    constructor: {
      value: subClass,
      writable: true,
      configurable: true
    }
  });

  _Object$defineProperty(subClass, "prototype", {
    writable: false
  });

  if (superClass) setPrototypeOf(subClass, superClass);
}

module.exports = _inherits, module.exports.__esModule = true, module.exports["default"] = module.exports;