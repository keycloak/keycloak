import _Symbol from "@babel/runtime-corejs3/core-js/symbol";
import _Symbol$iterator from "@babel/runtime-corejs3/core-js/symbol/iterator";
export default function _typeof(obj) {
  "@babel/helpers - typeof";

  return _typeof = "function" == typeof _Symbol && "symbol" == typeof _Symbol$iterator ? function (obj) {
    return typeof obj;
  } : function (obj) {
    return obj && "function" == typeof _Symbol && obj.constructor === _Symbol && obj !== _Symbol.prototype ? "symbol" : typeof obj;
  }, _typeof(obj);
}