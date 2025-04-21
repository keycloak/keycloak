import _Reflect$get from "@babel/runtime-corejs3/core-js/reflect/get";
import _bindInstanceProperty from "@babel/runtime-corejs3/core-js/instance/bind";
import _Object$getOwnPropertyDescriptor from "@babel/runtime-corejs3/core-js/object/get-own-property-descriptor";
import superPropBase from "./superPropBase.js";
export default function _get() {
  if (typeof Reflect !== "undefined" && _Reflect$get) {
    var _context;

    _get = _bindInstanceProperty(_context = _Reflect$get).call(_context);
  } else {
    _get = function _get(target, property, receiver) {
      var base = superPropBase(target, property);
      if (!base) return;

      var desc = _Object$getOwnPropertyDescriptor(base, property);

      if (desc.get) {
        return desc.get.call(arguments.length < 3 ? target : receiver);
      }

      return desc.value;
    };
  }

  return _get.apply(this, arguments);
}