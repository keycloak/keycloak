(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports);
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports);
    global.undefined = mod.exports;
  }
})(this, function (exports) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.default = {
    "name": "--pf-c-select__toggle-arrow--m-top--m-expanded__toggle-arrow--Transform",
    "value": "rotate(180deg)",
    "var": "var(--pf-c-select__toggle-arrow--m-top--m-expanded__toggle-arrow--Transform)"
  };
});