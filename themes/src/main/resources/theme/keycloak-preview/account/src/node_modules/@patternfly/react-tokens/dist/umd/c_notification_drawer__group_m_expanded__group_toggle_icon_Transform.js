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
    "name": "--pf-c-notification-drawer__group--m-expanded__group-toggle-icon--Transform",
    "value": "rotate(90deg)",
    "var": "var(--pf-c-notification-drawer__group--m-expanded__group-toggle-icon--Transform)"
  };
});