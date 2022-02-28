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
    ".pf-l-stack": [{
      "property": "--pf-l-stack--m-gutter--MarginBottom",
      "value": "1.5rem",
      "token": "l_stack_m_gutter_MarginBottom",
      "values": ["--pf-global--gutter", "$pf-global--gutter", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-l-stack--m-gutter--md--MarginBottom",
      "value": "1rem",
      "token": "l_stack_m_gutter_md_MarginBottom",
      "values": ["--pf-global--gutter--md", "$pf-global--gutter--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }]
  };
});