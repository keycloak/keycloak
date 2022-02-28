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
    "name": "--pf-c-about-modal-box__close--c-button--BackgroundColor",
    "value": "rgba(3,3,3,0.4)",
    "var": "var(--pf-c-about-modal-box__close--c-button--BackgroundColor)"
  };
});