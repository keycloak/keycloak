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
    "name": "--pf-c-data-toolbar__expandable-content--m-expanded--GridRowGap",
    "value": "1.5rem",
    "var": "var(--pf-c-data-toolbar__expandable-content--m-expanded--GridRowGap)"
  };
});