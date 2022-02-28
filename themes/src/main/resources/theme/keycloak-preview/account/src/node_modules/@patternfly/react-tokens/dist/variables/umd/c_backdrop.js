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
    ".pf-c-backdrop": [{
      "property": "--pf-c-backdrop--ZIndex",
      "value": "400",
      "token": "c_backdrop_ZIndex",
      "values": ["--pf-global--ZIndex--lg", "$pf-global--ZIndex--lg", "400"]
    }, {
      "property": "--pf-c-backdrop--Color",
      "value": "rgba(#030303, .62)",
      "token": "c_backdrop_Color",
      "values": ["--pf-global--BackgroundColor--dark-transparent-100", "$pf-global--BackgroundColor--dark-transparent-100", "rgba($pf-color-black-1000, .62)", "rgba(#030303, .62)"]
    }, {
      "property": "--pf-c-backdrop--BackdropFilter",
      "value": "blur(10px)",
      "token": "c_backdrop_BackdropFilter"
    }]
  };
});