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
    ".pf-c-simple-list": [{
      "property": "--pf-c-simple-list__item-link--PaddingTop",
      "value": "0.25rem",
      "token": "c_simple_list__item_link_PaddingTop",
      "values": ["--pf-global--spacer--xs", "$pf-global--spacer--xs", "pf-size-prem(4px)", "0.25rem"]
    }, {
      "property": "--pf-c-simple-list__item-link--PaddingRight",
      "value": "1rem",
      "token": "c_simple_list__item_link_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-simple-list__item-link--PaddingBottom",
      "value": "0.25rem",
      "token": "c_simple_list__item_link_PaddingBottom",
      "values": ["--pf-global--spacer--xs", "$pf-global--spacer--xs", "pf-size-prem(4px)", "0.25rem"]
    }, {
      "property": "--pf-c-simple-list__item-link--PaddingLeft",
      "value": "1rem",
      "token": "c_simple_list__item_link_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-simple-list__item-link--BackgroundColor",
      "value": "#fff",
      "token": "c_simple_list__item_link_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--100", "$pf-global--BackgroundColor--100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-simple-list__item-link--Color",
      "value": "#151515",
      "token": "c_simple_list__item_link_Color",
      "values": ["--pf-global--Color--100", "$pf-global--Color--100", "$pf-color-black-900", "#151515"]
    }, {
      "property": "--pf-c-simple-list__item-link--FontSize",
      "value": "0.875rem",
      "token": "c_simple_list__item_link_FontSize",
      "values": ["--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-simple-list__item-link--FontWeight",
      "value": "400",
      "token": "c_simple_list__item_link_FontWeight",
      "values": ["--pf-global--FontWeight--normal", "$pf-global--FontWeight--normal", "400"]
    }, {
      "property": "--pf-c-simple-list__item-link--m-current--Color",
      "value": "#06c",
      "token": "c_simple_list__item_link_m_current_Color",
      "values": ["--pf-global--link--Color", "$pf-global--link--Color", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-simple-list__item-link--m-current--BackgroundColor",
      "value": "#f5f5f5",
      "token": "c_simple_list__item_link_m_current_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--150", "$pf-global--BackgroundColor--150", "$pf-color-black-150", "#f5f5f5"]
    }, {
      "property": "--pf-c-simple-list__item-link--m-current--FontWeight",
      "value": "500",
      "token": "c_simple_list__item_link_m_current_FontWeight",
      "values": ["--pf-global--FontWeight--semi-bold", "$pf-global--FontWeight--semi-bold", "500"]
    }, {
      "property": "--pf-c-simple-list__item-link--hover--Color",
      "value": "#06c",
      "token": "c_simple_list__item_link_hover_Color",
      "values": ["--pf-global--link--Color", "$pf-global--link--Color", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-simple-list__item-link--hover--BackgroundColor",
      "value": "#f5f5f5",
      "token": "c_simple_list__item_link_hover_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--150", "$pf-global--BackgroundColor--150", "$pf-color-black-150", "#f5f5f5"]
    }, {
      "property": "--pf-c-simple-list__item-link--focus--Color",
      "value": "#06c",
      "token": "c_simple_list__item_link_focus_Color",
      "values": ["--pf-global--link--Color", "$pf-global--link--Color", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-simple-list__item-link--focus--BackgroundColor",
      "value": "#f5f5f5",
      "token": "c_simple_list__item_link_focus_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--150", "$pf-global--BackgroundColor--150", "$pf-color-black-150", "#f5f5f5"]
    }, {
      "property": "--pf-c-simple-list__item-link--focus--FontWeight",
      "value": "500",
      "token": "c_simple_list__item_link_focus_FontWeight",
      "values": ["--pf-global--FontWeight--semi-bold", "$pf-global--FontWeight--semi-bold", "500"]
    }, {
      "property": "--pf-c-simple-list__item-link--active--Color",
      "value": "#06c",
      "token": "c_simple_list__item_link_active_Color",
      "values": ["--pf-global--link--Color", "$pf-global--link--Color", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-simple-list__item-link--active--BackgroundColor",
      "value": "#f5f5f5",
      "token": "c_simple_list__item_link_active_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--150", "$pf-global--BackgroundColor--150", "$pf-color-black-150", "#f5f5f5"]
    }, {
      "property": "--pf-c-simple-list__item-link--active--FontWeight",
      "value": "500",
      "token": "c_simple_list__item_link_active_FontWeight",
      "values": ["--pf-global--FontWeight--semi-bold", "$pf-global--FontWeight--semi-bold", "500"]
    }, {
      "property": "--pf-c-simple-list__title--PaddingTop",
      "value": "0.5rem",
      "token": "c_simple_list__title_PaddingTop",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-simple-list__title--PaddingRight",
      "value": "1rem",
      "token": "c_simple_list__title_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-simple-list__title--PaddingBottom",
      "value": "0.5rem",
      "token": "c_simple_list__title_PaddingBottom",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-simple-list__title--PaddingLeft",
      "value": "1rem",
      "token": "c_simple_list__title_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-simple-list__title--FontSize",
      "value": "0.875rem",
      "token": "c_simple_list__title_FontSize",
      "values": ["--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-simple-list__title--Color",
      "value": "#737679",
      "token": "c_simple_list__title_Color",
      "values": ["--pf-global--Color--dark-200", "$pf-global--Color--dark-200", "$pf-color-black-600", "#737679"]
    }, {
      "property": "--pf-c-simple-list__title--FontWeight",
      "value": "500",
      "token": "c_simple_list__title_FontWeight",
      "values": ["--pf-global--FontWeight--semi-bold", "$pf-global--FontWeight--semi-bold", "500"]
    }, {
      "property": "--pf-c-simple-list__section--section--MarginTop",
      "value": "0.5rem",
      "token": "c_simple_list__section_section_MarginTop",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }],
    ".pf-c-simple-list__item-link.pf-m-current": [{
      "property": "--pf-c-simple-list__item-link--FontWeight",
      "value": "500",
      "token": "c_simple_list__item_link_FontWeight",
      "values": ["--pf-c-simple-list__item-link--m-current--FontWeight", "--pf-global--FontWeight--semi-bold", "$pf-global--FontWeight--semi-bold", "500"]
    }, {
      "property": "--pf-c-simple-list__item-link--BackgroundColor",
      "value": "#f5f5f5",
      "token": "c_simple_list__item_link_BackgroundColor",
      "values": ["--pf-c-simple-list__item-link--m-current--BackgroundColor", "--pf-global--BackgroundColor--150", "$pf-global--BackgroundColor--150", "$pf-color-black-150", "#f5f5f5"]
    }, {
      "property": "--pf-c-simple-list__item-link--Color",
      "value": "#06c",
      "token": "c_simple_list__item_link_Color",
      "values": ["--pf-c-simple-list__item-link--m-current--Color", "--pf-global--link--Color", "$pf-global--link--Color", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }],
    ".pf-c-simple-list__item-link.pf-m-hover": [{
      "property": "--pf-c-simple-list__item-link--BackgroundColor",
      "value": "#f5f5f5",
      "token": "c_simple_list__item_link_BackgroundColor",
      "values": ["--pf-c-simple-list__item-link--hover--BackgroundColor", "--pf-global--BackgroundColor--150", "$pf-global--BackgroundColor--150", "$pf-color-black-150", "#f5f5f5"]
    }, {
      "property": "--pf-c-simple-list__item-link--Color",
      "value": "#06c",
      "token": "c_simple_list__item_link_Color",
      "values": ["--pf-c-simple-list__item-link--hover--Color", "--pf-global--link--Color", "$pf-global--link--Color", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }],
    ".pf-c-simple-list__item-link.pf-m-focus": [{
      "property": "--pf-c-simple-list__item-link--FontWeight",
      "value": "500",
      "token": "c_simple_list__item_link_FontWeight",
      "values": ["--pf-c-simple-list__item-link--focus--FontWeight", "--pf-global--FontWeight--semi-bold", "$pf-global--FontWeight--semi-bold", "500"]
    }, {
      "property": "--pf-c-simple-list__item-link--BackgroundColor",
      "value": "#f5f5f5",
      "token": "c_simple_list__item_link_BackgroundColor",
      "values": ["--pf-c-simple-list__item-link--focus--BackgroundColor", "--pf-global--BackgroundColor--150", "$pf-global--BackgroundColor--150", "$pf-color-black-150", "#f5f5f5"]
    }, {
      "property": "--pf-c-simple-list__item-link--Color",
      "value": "#06c",
      "token": "c_simple_list__item_link_Color",
      "values": ["--pf-c-simple-list__item-link--focus--Color", "--pf-global--link--Color", "$pf-global--link--Color", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }],
    ".pf-c-simple-list__item-link.pf-m-active": [{
      "property": "--pf-c-simple-list__item-link--FontWeight",
      "value": "500",
      "token": "c_simple_list__item_link_FontWeight",
      "values": ["--pf-c-simple-list__item-link--active--FontWeight", "--pf-global--FontWeight--semi-bold", "$pf-global--FontWeight--semi-bold", "500"]
    }, {
      "property": "--pf-c-simple-list__item-link--BackgroundColor",
      "value": "#f5f5f5",
      "token": "c_simple_list__item_link_BackgroundColor",
      "values": ["--pf-c-simple-list__item-link--active--BackgroundColor", "--pf-global--BackgroundColor--150", "$pf-global--BackgroundColor--150", "$pf-color-black-150", "#f5f5f5"]
    }, {
      "property": "--pf-c-simple-list__item-link--Color",
      "value": "#06c",
      "token": "c_simple_list__item_link_Color",
      "values": ["--pf-c-simple-list__item-link--active--Color", "--pf-global--link--Color", "$pf-global--link--Color", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }]
  };
});