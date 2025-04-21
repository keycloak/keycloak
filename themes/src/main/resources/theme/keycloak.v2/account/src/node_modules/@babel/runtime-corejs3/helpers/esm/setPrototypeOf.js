import _Object$setPrototypeOf from "@babel/runtime-corejs3/core-js/object/set-prototype-of";
import _bindInstanceProperty from "@babel/runtime-corejs3/core-js/instance/bind";
export default function _setPrototypeOf(o, p) {
  var _context;

  _setPrototypeOf = _Object$setPrototypeOf ? _bindInstanceProperty(_context = _Object$setPrototypeOf).call(_context) : function _setPrototypeOf(o, p) {
    o.__proto__ = p;
    return o;
  };
  return _setPrototypeOf(o, p);
}