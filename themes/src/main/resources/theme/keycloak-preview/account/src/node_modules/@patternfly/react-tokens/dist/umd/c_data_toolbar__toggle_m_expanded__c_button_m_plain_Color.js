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
    "name": "--pf-c-data-toolbar__toggle--m-expanded__c-button--m-plain--Color",
    "value": "#151515",
    "var": "var(--pf-c-data-toolbar__toggle--m-expanded__c-button--m-plain--Color)"
  };
});