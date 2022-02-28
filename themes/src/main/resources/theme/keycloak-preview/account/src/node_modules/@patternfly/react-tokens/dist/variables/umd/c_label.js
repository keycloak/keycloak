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
    ".pf-c-label": [{
      "property": "--pf-c-label--PaddingTop",
      "value": "0.25rem",
      "token": "c_label_PaddingTop",
      "values": ["--pf-global--spacer--xs", "$pf-global--spacer--xs", "pf-size-prem(4px)", "0.25rem"]
    }, {
      "property": "--pf-c-label--PaddingRight",
      "value": "0.5rem",
      "token": "c_label_PaddingRight",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-label--PaddingBottom",
      "value": "0.25rem",
      "token": "c_label_PaddingBottom",
      "values": ["--pf-global--spacer--xs", "$pf-global--spacer--xs", "pf-size-prem(4px)", "0.25rem"]
    }, {
      "property": "--pf-c-label--PaddingLeft",
      "value": "0.5rem",
      "token": "c_label_PaddingLeft",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-label--BorderRadius",
      "value": "3px",
      "token": "c_label_BorderRadius",
      "values": ["--pf-global--BorderRadius--sm", "$pf-global--BorderRadius--sm", "3px"]
    }, {
      "property": "--pf-c-label--BackgroundColor",
      "value": "#06c",
      "token": "c_label_BackgroundColor",
      "values": ["--pf-global--primary-color--100", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-label--Color",
      "value": "#fff",
      "token": "c_label_Color",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-label--FontSize",
      "value": "0.875rem",
      "token": "c_label_FontSize",
      "values": ["--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-label--m-compact--FontSize",
      "value": "0.75rem",
      "token": "c_label_m_compact_FontSize",
      "values": ["--pf-global--FontSize--xs", "$pf-global--FontSize--xs", "pf-font-prem(12px)", "0.75rem"]
    }],
    ".pf-c-label.pf-m-compact": [{
      "property": "--pf-c-label--FontSize",
      "value": "0.75rem",
      "token": "c_label_FontSize",
      "values": ["--pf-c-label--m-compact--FontSize", "--pf-global--FontSize--xs", "$pf-global--FontSize--xs", "pf-font-prem(12px)", "0.75rem"]
    }]
  };
});