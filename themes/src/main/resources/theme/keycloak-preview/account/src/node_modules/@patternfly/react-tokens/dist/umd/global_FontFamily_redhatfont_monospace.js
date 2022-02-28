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
    "name": "--pf-global--FontFamily--redhatfont--monospace",
    "value": "Liberation Mono,consolas,SFMono-Regular,menlo,monaco,Courier New,monospace",
    "var": "var(--pf-global--FontFamily--redhatfont--monospace)"
  };
});