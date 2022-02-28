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
    ".pf-l-toolbar": [{
      "property": "--pf-l-toolbar__section--MarginTop",
      "value": "1rem",
      "token": "l_toolbar__section_MarginTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-l-toolbar__group--MarginRight",
      "value": "2rem",
      "token": "l_toolbar__group_MarginRight",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-l-toolbar__group--MarginLeft",
      "value": "2rem",
      "token": "l_toolbar__group_MarginLeft",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-l-toolbar__item--MarginRight",
      "value": "1rem",
      "token": "l_toolbar__item_MarginRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-l-toolbar__item--MarginLeft",
      "value": "1rem",
      "token": "l_toolbar__item_MarginLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }]
  };
});