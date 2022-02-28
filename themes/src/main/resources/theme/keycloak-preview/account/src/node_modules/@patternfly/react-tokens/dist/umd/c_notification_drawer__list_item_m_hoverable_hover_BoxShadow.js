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
    "name": "--pf-c-notification-drawer__list-item--m-hoverable--hover--BoxShadow",
    "value": "0 -0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25),0 0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-c-notification-drawer__list-item--m-hoverable--hover--BoxShadow)"
  };
});