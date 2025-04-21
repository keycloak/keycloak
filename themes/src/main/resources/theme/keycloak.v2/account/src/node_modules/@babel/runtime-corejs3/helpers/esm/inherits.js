import _Object$create from "@babel/runtime-corejs3/core-js/object/create";
import _Object$defineProperty from "@babel/runtime-corejs3/core-js/object/define-property";
import setPrototypeOf from "./setPrototypeOf.js";
export default function _inherits(subClass, superClass) {
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