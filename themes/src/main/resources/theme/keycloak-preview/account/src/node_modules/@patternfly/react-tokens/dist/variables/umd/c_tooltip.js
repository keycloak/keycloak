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
    ".pf-c-tooltip": [{
      "property": "--pf-c-tooltip--MaxWidth",
      "value": "18.75rem",
      "token": "c_tooltip_MaxWidth"
    }, {
      "property": "--pf-c-tooltip__content--PaddingTop",
      "value": "1rem",
      "token": "c_tooltip__content_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-tooltip__content--PaddingRight",
      "value": "1.5rem",
      "token": "c_tooltip__content_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-tooltip__content--PaddingBottom",
      "value": "1rem",
      "token": "c_tooltip__content_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-tooltip__content--PaddingLeft",
      "value": "1.5rem",
      "token": "c_tooltip__content_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-tooltip__content--Color",
      "value": "#fff",
      "token": "c_tooltip__content_Color",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-tooltip__content--BackgroundColor",
      "value": "#151515",
      "token": "c_tooltip__content_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-100", "$pf-global--BackgroundColor--dark-100", "$pf-color-black-900", "#151515"]
    }, {
      "property": "--pf-c-tooltip__content--FontSize",
      "value": "0.875rem",
      "token": "c_tooltip__content_FontSize",
      "values": ["--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-tooltip__arrow--Width",
      "value": "0.9375rem",
      "token": "c_tooltip__arrow_Width",
      "values": ["--pf-global--arrow--width", "$pf-global--arrow--width", "pf-font-prem(15px)", "0.9375rem"]
    }, {
      "property": "--pf-c-tooltip__arrow--Height",
      "value": "0.9375rem",
      "token": "c_tooltip__arrow_Height",
      "values": ["--pf-global--arrow--width", "$pf-global--arrow--width", "pf-font-prem(15px)", "0.9375rem"]
    }, {
      "property": "--pf-c-tooltip__arrow--m-top--Transform",
      "value": "translate(-50%, 50%) rotate(45deg)",
      "token": "c_tooltip__arrow_m_top_Transform"
    }, {
      "property": "--pf-c-tooltip__arrow--m-right--Transform",
      "value": "translate(-50%, -50%) rotate(45deg)",
      "token": "c_tooltip__arrow_m_right_Transform"
    }, {
      "property": "--pf-c-tooltip__arrow--m-bottom--Transform",
      "value": "translate(-50%, -50%) rotate(45deg)",
      "token": "c_tooltip__arrow_m_bottom_Transform"
    }, {
      "property": "--pf-c-tooltip__arrow--m-left--Transform",
      "value": "translate(50%, -50%) rotate(45deg)",
      "token": "c_tooltip__arrow_m_left_Transform"
    }]
  };
});