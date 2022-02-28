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
    "name": "--pf-c-table__toggle--c-button--m-expanded__toggle-icon--Transform",
    "value": "rotate(360deg)",
    "var": "var(--pf-c-table__toggle--c-button--m-expanded__toggle-icon--Transform)"
  };
});