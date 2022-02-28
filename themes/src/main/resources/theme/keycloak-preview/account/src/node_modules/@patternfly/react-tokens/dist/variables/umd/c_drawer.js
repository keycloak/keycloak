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
    ".pf-c-drawer": [{
      "property": "--pf-c-drawer__section--BackgroundColor",
      "value": "#fff",
      "token": "c_drawer__section_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--100", "$pf-global--BackgroundColor--100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-drawer__content--FlexBasis",
      "value": "100%",
      "token": "c_drawer__content_FlexBasis"
    }, {
      "property": "--pf-c-drawer__content--BackgroundColor",
      "value": "#fff",
      "token": "c_drawer__content_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--100", "$pf-global--BackgroundColor--100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-drawer__content--ZIndex",
      "value": "100",
      "token": "c_drawer__content_ZIndex",
      "values": ["--pf-global--ZIndex--xs", "$pf-global--ZIndex--xs", "100"]
    }, {
      "property": "--pf-c-drawer__panel--FlexBasis",
      "value": "100%",
      "token": "c_drawer__panel_FlexBasis"
    }, {
      "property": "--pf-c-drawer__panel--md--FlexBasis",
      "value": "50%",
      "token": "c_drawer__panel_md_FlexBasis"
    }, {
      "property": "--pf-c-drawer__panel--MinWidth",
      "value": "50%",
      "token": "c_drawer__panel_MinWidth"
    }, {
      "property": "--pf-c-drawer__panel--xl--MinWidth",
      "value": "28.125rem",
      "token": "c_drawer__panel_xl_MinWidth"
    }, {
      "property": "--pf-c-drawer__panel--xl--FlexBasis",
      "value": "28.125rem",
      "token": "c_drawer__panel_xl_FlexBasis"
    }, {
      "property": "--pf-c-drawer__panel--ZIndex",
      "value": "200",
      "token": "c_drawer__panel_ZIndex",
      "values": ["--pf-global--ZIndex--sm", "$pf-global--ZIndex--sm", "200"]
    }, {
      "property": "--pf-c-drawer__panel--BackgroundColor",
      "value": "#fff",
      "token": "c_drawer__panel_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--100", "$pf-global--BackgroundColor--100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-drawer__panel--TransitionDuration",
      "value": ".25s",
      "token": "c_drawer__panel_TransitionDuration"
    }, {
      "property": "--pf-c-drawer__panel--TransitionProperty",
      "value": "margin, transform, box-shadow",
      "token": "c_drawer__panel_TransitionProperty"
    }, {
      "property": "--pf-c-drawer--child--PaddingTop",
      "value": "1rem",
      "token": "c_drawer_child_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--PaddingRight",
      "value": "1rem",
      "token": "c_drawer_child_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--PaddingBottom",
      "value": "1rem",
      "token": "c_drawer_child_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--PaddingLeft",
      "value": "1rem",
      "token": "c_drawer_child_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--md--PaddingTop",
      "value": "1.5rem",
      "token": "c_drawer_child_md_PaddingTop",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-drawer--child--md--PaddingRight",
      "value": "1.5rem",
      "token": "c_drawer_child_md_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-drawer--child--md--PaddingBottom",
      "value": "1.5rem",
      "token": "c_drawer_child_md_PaddingBottom",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-drawer--child--md--PaddingLeft",
      "value": "1.5rem",
      "token": "c_drawer_child_md_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-drawer--child--m-padding--PaddingTop",
      "value": "1rem",
      "token": "c_drawer_child_m_padding_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--m-padding--PaddingRight",
      "value": "1rem",
      "token": "c_drawer_child_m_padding_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--m-padding--PaddingBottom",
      "value": "1rem",
      "token": "c_drawer_child_m_padding_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--m-padding--PaddingLeft",
      "value": "1rem",
      "token": "c_drawer_child_m_padding_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--m-padding--md--PaddingTop",
      "value": "1.5rem",
      "token": "c_drawer_child_m_padding_md_PaddingTop",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-drawer--child--m-padding--md--PaddingRight",
      "value": "1.5rem",
      "token": "c_drawer_child_m_padding_md_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-drawer--child--m-padding--md--PaddingBottom",
      "value": "1.5rem",
      "token": "c_drawer_child_m_padding_md_PaddingBottom",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-drawer--child--m-padding--md--PaddingLeft",
      "value": "1.5rem",
      "token": "c_drawer_child_m_padding_md_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-drawer__content--child--PaddingTop",
      "value": "0",
      "token": "c_drawer__content_child_PaddingTop"
    }, {
      "property": "--pf-c-drawer__content--child--PaddingRight",
      "value": "0",
      "token": "c_drawer__content_child_PaddingRight"
    }, {
      "property": "--pf-c-drawer__content--child--PaddingBottom",
      "value": "0",
      "token": "c_drawer__content_child_PaddingBottom"
    }, {
      "property": "--pf-c-drawer__content--child--PaddingLeft",
      "value": "0",
      "token": "c_drawer__content_child_PaddingLeft"
    }, {
      "property": "--pf-c-drawer__actions--MarginTop",
      "value": "calc(0.375rem * -1)",
      "token": "c_drawer__actions_MarginTop"
    }, {
      "property": "--pf-c-drawer__actions--MarginRight",
      "value": "calc(0.375rem * -1)",
      "token": "c_drawer__actions_MarginRight"
    }, {
      "property": "--pf-c-drawer__panel--BoxShadow",
      "value": "none",
      "token": "c_drawer__panel_BoxShadow"
    }, {
      "property": "--pf-c-drawer--m-expanded__panel--BoxShadow",
      "value": "-0.75rem 0 0.625rem -0.25rem rgba(3, 3, 3, 0.07)",
      "token": "c_drawer_m_expanded__panel_BoxShadow",
      "values": ["--pf-global--BoxShadow--lg-left", "$pf-global--BoxShadow--lg-left", "pf-size-prem(-12) 0 pf-size-prem(10) pf-size-prem(-4) rgba($pf-color-black-1000, .07)", "pf-size-prem(-12) 0 pf-size-prem(10) pf-size-prem(-4) rgba(#030303, .07)", "-0.75rem 0 0.625rem -0.25rem rgba(3, 3, 3, 0.07)"]
    }, {
      "property": "--pf-c-drawer--m-expanded--m-panel-left__panel--BoxShadow",
      "value": "0.75rem 0 0.625rem -0.25rem rgba(3, 3, 3, 0.07)",
      "token": "c_drawer_m_expanded_m_panel_left__panel_BoxShadow",
      "values": ["--pf-global--BoxShadow--lg-right", "$pf-global--BoxShadow--lg-right", "pf-size-prem(12) 0 pf-size-prem(10) pf-size-prem(-4) rgba($pf-color-black-1000, .07)", "pf-size-prem(12) 0 pf-size-prem(10) pf-size-prem(-4) rgba(#030303, .07)", "0.75rem 0 0.625rem -0.25rem rgba(3, 3, 3, 0.07)"]
    }, {
      "property": "--pf-c-drawer__panel--after--Width",
      "value": "1px",
      "token": "c_drawer__panel_after_Width",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-drawer__panel--after--BackgroundColor",
      "value": "transparent",
      "token": "c_drawer__panel_after_BackgroundColor"
    }, {
      "property": "--pf-c-drawer--m-inline--m-expanded__panel--after--BackgroundColor",
      "value": "#d2d2d2",
      "token": "c_drawer_m_inline_m_expanded__panel_after_BackgroundColor",
      "values": ["--pf-global--BorderColor--100", "$pf-global--BorderColor--100", "$pf-color-black-300", "#d2d2d2"]
    }],
    ".pf-c-drawer__section.pf-m-no-background": [{
      "property": "--pf-c-drawer__section--BackgroundColor",
      "value": "transparent",
      "token": "c_drawer__section_BackgroundColor"
    }],
    ".pf-c-drawer__content": [{
      "property": "--pf-c-drawer--child--PaddingTop",
      "value": "0",
      "token": "c_drawer_child_PaddingTop",
      "values": ["--pf-c-drawer__content--child--PaddingTop", "0"]
    }, {
      "property": "--pf-c-drawer--child--PaddingRight",
      "value": "0",
      "token": "c_drawer_child_PaddingRight",
      "values": ["--pf-c-drawer__content--child--PaddingRight", "0"]
    }, {
      "property": "--pf-c-drawer--child--PaddingBottom",
      "value": "0",
      "token": "c_drawer_child_PaddingBottom",
      "values": ["--pf-c-drawer__content--child--PaddingBottom", "0"]
    }, {
      "property": "--pf-c-drawer--child--PaddingLeft",
      "value": "0",
      "token": "c_drawer_child_PaddingLeft",
      "values": ["--pf-c-drawer__content--child--PaddingLeft", "0"]
    }],
    ".pf-c-drawer__content.pf-m-no-background": [{
      "property": "--pf-c-drawer__content--BackgroundColor",
      "value": "transparent",
      "token": "c_drawer__content_BackgroundColor"
    }],
    ".pf-c-drawer__panel.pf-m-no-background": [{
      "property": "--pf-c-drawer__content--BackgroundColor",
      "value": "transparent",
      "token": "c_drawer__content_BackgroundColor"
    }],
    ".pf-c-drawer__body.pf-m-no-padding": [{
      "property": "--pf-c-drawer__actions--MarginTop",
      "value": "0",
      "token": "c_drawer__actions_MarginTop"
    }, {
      "property": "--pf-c-drawer__actions--MarginRight",
      "value": "0",
      "token": "c_drawer__actions_MarginRight"
    }, {
      "property": "--pf-c-drawer--child--PaddingTop",
      "value": "0",
      "token": "c_drawer_child_PaddingTop"
    }, {
      "property": "--pf-c-drawer--child--PaddingRight",
      "value": "0",
      "token": "c_drawer_child_PaddingRight"
    }, {
      "property": "--pf-c-drawer--child--PaddingBottom",
      "value": "0",
      "token": "c_drawer_child_PaddingBottom"
    }, {
      "property": "--pf-c-drawer--child--PaddingLeft",
      "value": "0",
      "token": "c_drawer_child_PaddingLeft"
    }],
    ".pf-c-drawer__body.pf-m-padding": [{
      "property": "--pf-c-drawer--child--PaddingTop",
      "value": "1rem",
      "token": "c_drawer_child_PaddingTop",
      "values": ["--pf-c-drawer--child--m-padding--PaddingTop", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--PaddingRight",
      "value": "1rem",
      "token": "c_drawer_child_PaddingRight",
      "values": ["--pf-c-drawer--child--m-padding--PaddingRight", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--PaddingBottom",
      "value": "1rem",
      "token": "c_drawer_child_PaddingBottom",
      "values": ["--pf-c-drawer--child--m-padding--PaddingBottom", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-drawer--child--PaddingLeft",
      "value": "1rem",
      "token": "c_drawer_child_PaddingLeft",
      "values": ["--pf-c-drawer--child--m-padding--PaddingLeft", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }],
    ".pf-c-drawer__body:not(.pf-m-no-padding) + *": [{
      "property": "--pf-c-drawer--child--PaddingTop",
      "value": "0",
      "token": "c_drawer_child_PaddingTop"
    }]
  };
});