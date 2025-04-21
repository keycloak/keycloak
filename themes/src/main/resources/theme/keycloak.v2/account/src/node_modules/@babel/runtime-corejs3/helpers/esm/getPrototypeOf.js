import _Object$setPrototypeOf from "@babel/runtime-corejs3/core-js/object/set-prototype-of";
import _bindInstanceProperty from "@babel/runtime-corejs3/core-js/instance/bind";
import _Object$getPrototypeOf from "@babel/runtime-corejs3/core-js/object/get-prototype-of";
export default function _getPrototypeOf(o) {
  var _context;

  _getPrototypeOf = _Object$setPrototypeOf ? _bindInstanceProperty(_context = _Object$getPrototypeOf).call(_context) : function _getPrototypeOf(o) {
    return o.__proto__ || _Object$getPrototypeOf(o);
  };
  return _getPrototypeOf(o);
}