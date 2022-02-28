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
    "name": "--pf-global--BoxShadow--md",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-global--BoxShadow--md)"
  };
});