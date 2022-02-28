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
    "name": "--pf-c-file-upload__file-select__c-button--m-control--OutlineOffset",
    "value": "calc(-1*0.25rem)",
    "var": "var(--pf-c-file-upload__file-select__c-button--m-control--OutlineOffset)"
  };
});