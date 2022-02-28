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
    ".pf-c-empty-state": [{
      "property": "--pf-c-empty-state--Padding",
      "value": "2rem",
      "token": "c_empty_state_Padding",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-empty-state__icon--MarginBottom",
      "value": "1.5rem",
      "token": "c_empty_state__icon_MarginBottom",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-empty-state__icon--FontSize",
      "value": "3.375rem",
      "token": "c_empty_state__icon_FontSize",
      "values": ["--pf-global--icon--FontSize--xl", "$pf-global--icon--FontSize--xl", "pf-font-prem(54px)", "3.375rem"]
    }, {
      "property": "--pf-c-empty-state__icon--Color",
      "value": "#737679",
      "token": "c_empty_state__icon_Color",
      "values": ["--pf-global--icon--Color--light", "$pf-global--icon--Color--light", "$pf-color-black-600", "#737679"]
    }, {
      "property": "--pf-c-empty-state__body--MarginTop",
      "value": "1rem",
      "token": "c_empty_state__body_MarginTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-empty-state__body--Color",
      "value": "#737679",
      "token": "c_empty_state__body_Color",
      "values": ["--pf-global--Color--200", "$pf-global--Color--200", "$pf-color-black-600", "#737679"]
    }, {
      "property": "--pf-c-empty-state--c-button--MarginTop",
      "value": "2rem",
      "token": "c_empty_state_c_button_MarginTop",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-empty-state--c-button__secondary--MarginTop",
      "value": "0.5rem",
      "token": "c_empty_state_c_button__secondary_MarginTop",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-empty-state__secondary--MarginTop",
      "value": "2rem",
      "token": "c_empty_state__secondary_MarginTop",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-empty-state__secondary--MarginRight",
      "value": "calc(0.25rem * -1)",
      "token": "c_empty_state__secondary_MarginRight",
      "values": ["calc(--pf-global--spacer--xs * -1)", "calc($pf-global--spacer--xs * -1)", "calc(pf-size-prem(4px) * -1)", "calc(0.25rem * -1)"]
    }, {
      "property": "--pf-c-empty-state__secondary--MarginBottom",
      "value": "calc(0.25rem * -1)",
      "token": "c_empty_state__secondary_MarginBottom",
      "values": ["calc(--pf-global--spacer--xs * -1)", "calc($pf-global--spacer--xs * -1)", "calc(pf-size-prem(4px) * -1)", "calc(0.25rem * -1)"]
    }, {
      "property": "--pf-c-empty-state__secondary--c-button--MarginRight",
      "value": "0.25rem",
      "token": "c_empty_state__secondary_c_button_MarginRight",
      "values": ["--pf-global--spacer--xs", "$pf-global--spacer--xs", "pf-size-prem(4px)", "0.25rem"]
    }, {
      "property": "--pf-c-empty-state__secondary--c-button--MarginBottom",
      "value": "0.25rem",
      "token": "c_empty_state__secondary_c_button_MarginBottom",
      "values": ["--pf-global--spacer--xs", "$pf-global--spacer--xs", "pf-size-prem(4px)", "0.25rem"]
    }, {
      "property": "--pf-c-empty-state--m-sm--MaxWidth",
      "value": "25rem",
      "token": "c_empty_state_m_sm_MaxWidth"
    }, {
      "property": "--pf-c-empty-state--m-lg--MaxWidth",
      "value": "37.5rem",
      "token": "c_empty_state_m_lg_MaxWidth"
    }, {
      "property": "--pf-c-empty-state--m-xl__body--FontSize",
      "value": "1.25rem",
      "token": "c_empty_state_m_xl__body_FontSize",
      "values": ["--pf-global--FontSize--xl", "$pf-global--FontSize--xl", "pf-font-prem(20px)", "1.25rem"]
    }, {
      "property": "--pf-c-empty-state--m-xl__body--MarginTop",
      "value": "1.5rem",
      "token": "c_empty_state_m_xl__body_MarginTop",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-empty-state--m-xl__icon--MarginBottom",
      "value": "2rem",
      "token": "c_empty_state_m_xl__icon_MarginBottom",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-empty-state--m-xl__icon--FontSize",
      "value": "6.25rem",
      "token": "c_empty_state_m_xl__icon_FontSize"
    }, {
      "property": "--pf-c-empty-state--m-xl--c-button__secondary--MarginTop",
      "value": "1rem",
      "token": "c_empty_state_m_xl_c_button__secondary_MarginTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }],
    ".pf-c-empty-state.pf-m-xl": [{
      "property": "--pf-c-empty-state__body--MarginTop",
      "value": "1.5rem",
      "token": "c_empty_state__body_MarginTop",
      "values": ["--pf-c-empty-state--m-xl__body--MarginTop", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-empty-state__icon--MarginBottom",
      "value": "2rem",
      "token": "c_empty_state__icon_MarginBottom",
      "values": ["--pf-c-empty-state--m-xl__icon--MarginBottom", "--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-empty-state__icon--FontSize",
      "value": "6.25rem",
      "token": "c_empty_state__icon_FontSize",
      "values": ["--pf-c-empty-state--m-xl__icon--FontSize", "6.25rem"]
    }],
    ".pf-c-empty-state.pf-m-xl > .pf-c-button.pf-m-primary + .pf-c-empty-state__secondary": [{
      "property": "--pf-c-empty-state--c-button__secondary--MarginTop",
      "value": "1rem",
      "token": "c_empty_state_c_button__secondary_MarginTop",
      "values": ["--pf-c-empty-state--m-xl--c-button__secondary--MarginTop", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }]
  };
});