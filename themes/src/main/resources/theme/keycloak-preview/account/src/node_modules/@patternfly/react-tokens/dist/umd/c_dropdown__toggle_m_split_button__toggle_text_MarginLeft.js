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
    "name": "--pf-c-dropdown__toggle--m-split-button__toggle-text--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button__toggle-text--MarginLeft)"
  };
});