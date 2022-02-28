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
    "name": "--pf-c-card--m-selectable--active--BoxShadow",
    "value": "0 0.1875rem 0.4375rem 0.1875rem rgba(3,3,3,0.13),0 0.6875rem 1.5rem 1rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-card--m-selectable--active--BoxShadow)"
  };
});