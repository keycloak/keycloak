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
    ".pf-c-skip-to-content": [{
      "property": "--pf-c-skip-to-content--Top",
      "value": "1rem",
      "token": "c_skip_to_content_Top",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-skip-to-content--ZIndex",
      "value": "600",
      "token": "c_skip_to_content_ZIndex",
      "values": ["--pf-global--ZIndex--2xl", "$pf-global--ZIndex--2xl", "600"]
    }, {
      "property": "--pf-c-skip-to-content--focus--Left",
      "value": "1rem",
      "token": "c_skip_to_content_focus_Left",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }]
  };
});