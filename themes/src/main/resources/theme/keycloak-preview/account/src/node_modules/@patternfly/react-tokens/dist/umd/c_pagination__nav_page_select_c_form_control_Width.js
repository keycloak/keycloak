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
    "name": "--pf-c-pagination__nav-page-select--c-form-control--Width",
    "value": "calc(3.5ch + 2*1ch)",
    "var": "var(--pf-c-pagination__nav-page-select--c-form-control--Width)"
  };
});