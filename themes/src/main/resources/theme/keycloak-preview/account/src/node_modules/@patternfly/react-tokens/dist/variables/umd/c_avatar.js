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
    ".pf-c-avatar": [{
      "property": "--pf-c-avatar--BorderRadius",
      "value": "30em",
      "token": "c_avatar_BorderRadius",
      "values": ["--pf-global--BorderRadius--lg", "$pf-global--BorderRadius--lg", "30em"]
    }, {
      "property": "--pf-c-avatar--Width",
      "value": "2.25rem",
      "token": "c_avatar_Width"
    }, {
      "property": "--pf-c-avatar--Height",
      "value": "2.25rem",
      "token": "c_avatar_Height"
    }]
  };
});