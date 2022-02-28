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
    ".pf-c-page__header": [{
      "property": "--pf-global--Color--100",
      "value": "#fff",
      "token": "global_Color_100",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-global--Color--200",
      "value": "#ededed",
      "token": "global_Color_200",
      "values": ["--pf-global--Color--light-200", "$pf-global--Color--light-200", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-global--BorderColor--100",
      "value": "#b8bbbe",
      "token": "global_BorderColor_100",
      "values": ["--pf-global--BorderColor--light-100", "$pf-global--BorderColor--light-100", "$pf-color-black-400", "#b8bbbe"]
    }, {
      "property": "--pf-global--primary-color--100",
      "value": "#73bcf7",
      "token": "global_primary_color_100",
      "values": ["--pf-global--primary-color--light-100", "$pf-global--primary-color--light-100", "$pf-color-blue-200", "#73bcf7"]
    }, {
      "property": "--pf-global--link--Color",
      "value": "#73bcf7",
      "token": "global_link_Color",
      "values": ["--pf-global--link--Color--light", "$pf-global--link--Color--light", "$pf-global--active-color--300", "$pf-color-blue-200", "#73bcf7"]
    }, {
      "property": "--pf-global--link--Color--hover",
      "value": "#73bcf7",
      "token": "global_link_Color_hover",
      "values": ["--pf-global--link--Color--light", "$pf-global--link--Color--light", "$pf-global--active-color--300", "$pf-color-blue-200", "#73bcf7"]
    }, {
      "property": "--pf-global--BackgroundColor--100",
      "value": "#151515",
      "token": "global_BackgroundColor_100",
      "values": ["--pf-global--BackgroundColor--dark-100", "$pf-global--BackgroundColor--dark-100", "$pf-color-black-900", "#151515"]
    }],
    ".pf-c-page__header .pf-c-card": [{
      "property": "--pf-c-card--BackgroundColor",
      "value": "rgba(#030303, .32)",
      "token": "c_card_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-transparent-200", "$pf-global--BackgroundColor--dark-transparent-200", "rgba($pf-color-black-1000, .32)", "rgba(#030303, .32)"]
    }],
    ".pf-c-page__header .pf-c-button": [{
      "property": "--pf-c-button--m-primary--Color",
      "value": "#06c",
      "token": "c_button_m_primary_Color",
      "values": ["--pf-global--primary-color--dark-100", "$pf-global--primary-color--dark-100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-button--m-primary--hover--Color",
      "value": "#06c",
      "token": "c_button_m_primary_hover_Color",
      "values": ["--pf-global--primary-color--dark-100", "$pf-global--primary-color--dark-100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-button--m-primary--focus--Color",
      "value": "#06c",
      "token": "c_button_m_primary_focus_Color",
      "values": ["--pf-global--primary-color--dark-100", "$pf-global--primary-color--dark-100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-button--m-primary--active--Color",
      "value": "#06c",
      "token": "c_button_m_primary_active_Color",
      "values": ["--pf-global--primary-color--dark-100", "$pf-global--primary-color--dark-100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-button--m-primary--BackgroundColor",
      "value": "#fff",
      "token": "c_button_m_primary_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-button--m-primary--hover--BackgroundColor",
      "value": "#ededed",
      "token": "c_button_m_primary_hover_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-300", "$pf-global--BackgroundColor--light-300", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-c-button--m-primary--focus--BackgroundColor",
      "value": "#ededed",
      "token": "c_button_m_primary_focus_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-300", "$pf-global--BackgroundColor--light-300", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-c-button--m-primary--active--BackgroundColor",
      "value": "#ededed",
      "token": "c_button_m_primary_active_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-300", "$pf-global--BackgroundColor--light-300", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-c-button--m-secondary--Color",
      "value": "#fff",
      "token": "c_button_m_secondary_Color",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-button--m-secondary--hover--Color",
      "value": "#fff",
      "token": "c_button_m_secondary_hover_Color",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-button--m-secondary--focus--Color",
      "value": "#fff",
      "token": "c_button_m_secondary_focus_Color",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-button--m-secondary--active--Color",
      "value": "#fff",
      "token": "c_button_m_secondary_active_Color",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-button--m-secondary--BorderColor",
      "value": "#fff",
      "token": "c_button_m_secondary_BorderColor",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-button--m-secondary--hover--BorderColor",
      "value": "#fff",
      "token": "c_button_m_secondary_hover_BorderColor",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-button--m-secondary--focus--BorderColor",
      "value": "#fff",
      "token": "c_button_m_secondary_focus_BorderColor",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-button--m-secondary--active--BorderColor",
      "value": "#fff",
      "token": "c_button_m_secondary_active_BorderColor",
      "values": ["--pf-global--Color--light-100", "$pf-global--Color--light-100", "$pf-color-white", "#fff"]
    }],
    ".pf-c-page": [{
      "property": "--pf-c-page--BackgroundColor",
      "value": "#151515",
      "token": "c_page_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-100", "$pf-global--BackgroundColor--dark-100", "$pf-color-black-900", "#151515"]
    }, {
      "property": "--pf-c-page__header--ZIndex",
      "value": "300",
      "token": "c_page__header_ZIndex",
      "values": ["--pf-global--ZIndex--md", "$pf-global--ZIndex--md", "300"]
    }, {
      "property": "--pf-c-page__header--MinHeight",
      "value": "4.75rem",
      "token": "c_page__header_MinHeight"
    }, {
      "property": "--pf-c-page__header-brand--PaddingLeft",
      "value": "1rem",
      "token": "c_page__header_brand_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__header-brand--md--PaddingRight",
      "value": "2rem",
      "token": "c_page__header_brand_md_PaddingRight",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-page__header-brand--md--PaddingLeft",
      "value": "1.5rem",
      "token": "c_page__header_brand_md_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__header-sidebar-toggle__c-button--PaddingTop",
      "value": "0.5rem",
      "token": "c_page__header_sidebar_toggle__c_button_PaddingTop",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-page__header-sidebar-toggle__c-button--PaddingRight",
      "value": "0.5rem",
      "token": "c_page__header_sidebar_toggle__c_button_PaddingRight",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-page__header-sidebar-toggle__c-button--PaddingBottom",
      "value": "0.5rem",
      "token": "c_page__header_sidebar_toggle__c_button_PaddingBottom",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-page__header-sidebar-toggle__c-button--PaddingLeft",
      "value": "0.5rem",
      "token": "c_page__header_sidebar_toggle__c_button_PaddingLeft",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-page__header-sidebar-toggle__c-button--MarginRight",
      "value": "1rem",
      "token": "c_page__header_sidebar_toggle__c_button_MarginRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__header-sidebar-toggle__c-button--MarginLeft",
      "value": "calc(0.5rem * -1)",
      "token": "c_page__header_sidebar_toggle__c_button_MarginLeft",
      "values": ["calc(--pf-c-page__header-sidebar-toggle__c-button--PaddingLeft * -1)", "calc(--pf-global--spacer--sm * -1)", "calc($pf-global--spacer--sm * -1)", "calc(pf-size-prem(8px) * -1)", "calc(0.5rem * -1)"]
    }, {
      "property": "--pf-c-page__header-sidebar-toggle__c-button--md--MarginLeft",
      "value": "calc(0.5rem * -1)",
      "token": "c_page__header_sidebar_toggle__c_button_md_MarginLeft",
      "values": ["calc(--pf-c-page__header-sidebar-toggle__c-button--PaddingLeft * -1)", "calc(--pf-global--spacer--sm * -1)", "calc($pf-global--spacer--sm * -1)", "calc(pf-size-prem(8px) * -1)", "calc(0.5rem * -1)"]
    }, {
      "property": "--pf-c-page__header-sidebar-toggle__c-button--FontSize",
      "value": "1.5rem",
      "token": "c_page__header_sidebar_toggle__c_button_FontSize",
      "values": ["--pf-global--FontSize--2xl", "$pf-global--FontSize--2xl", "pf-font-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__header-brand-link--c-brand--MaxHeight",
      "value": "3.75rem",
      "token": "c_page__header_brand_link_c_brand_MaxHeight"
    }, {
      "property": "--pf-c-page__header-nav--PaddingLeft",
      "value": "1rem",
      "token": "c_page__header_nav_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__header-nav--md--PaddingLeft",
      "value": "1.5rem",
      "token": "c_page__header_nav_md_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__header-nav--lg--PaddingLeft",
      "value": "0",
      "token": "c_page__header_nav_lg_PaddingLeft"
    }, {
      "property": "--pf-c-page__header-nav--lg--MarginLeft",
      "value": "2rem",
      "token": "c_page__header_nav_lg_MarginLeft",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-page__header-nav--lg--MarginRight",
      "value": "2rem",
      "token": "c_page__header_nav_lg_MarginRight",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-page__header-nav--BackgroundColor",
      "value": "#212427",
      "token": "c_page__header_nav_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-300", "$pf-global--BackgroundColor--dark-300", "$pf-color-black-850", "#212427"]
    }, {
      "property": "--pf-c-page__header-nav--lg--BackgroundColor",
      "value": "transparent",
      "token": "c_page__header_nav_lg_BackgroundColor"
    }, {
      "property": "--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--Left",
      "value": "calc(-1 * (1rem - 0.25rem))",
      "token": "c_page__header_nav_c_nav__scroll_button_nth_of_type_1_Left",
      "values": ["calc(-1 * (--pf-global--spacer--md - --pf-global--spacer--xs))", "calc(-1 * ($pf-global--spacer--md - $pf-global--spacer--xs))", "calc(-1 * (pf-size-prem(16px) - pf-size-prem(4px)))", "calc(-1 * (1rem - 0.25rem))"]
    }, {
      "property": "--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--md--Left",
      "value": "calc(-1 * (1rem - 0.25rem))",
      "token": "c_page__header_nav_c_nav__scroll_button_nth_of_type_1_md_Left",
      "values": ["calc(-1 * (--pf-global--spacer--md - --pf-global--spacer--xs))", "calc(-1 * ($pf-global--spacer--md - $pf-global--spacer--xs))", "calc(-1 * (pf-size-prem(16px) - pf-size-prem(4px)))", "calc(-1 * (1rem - 0.25rem))"]
    }, {
      "property": "--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--lg--Left",
      "value": "0",
      "token": "c_page__header_nav_c_nav__scroll_button_nth_of_type_1_lg_Left"
    }, {
      "property": "--pf-c-page__header-nav--c-nav__scroll-button--lg--BackgroundColor",
      "value": "#212427",
      "token": "c_page__header_nav_c_nav__scroll_button_lg_BackgroundColor",
      "values": ["--pf-c-page__header-nav--BackgroundColor", "--pf-global--BackgroundColor--dark-300", "$pf-global--BackgroundColor--dark-300", "$pf-color-black-850", "#212427"]
    }, {
      "property": "--pf-c-page__header-nav--c-nav__scroll-button--lg--Top",
      "value": "0",
      "token": "c_page__header_nav_c_nav__scroll_button_lg_Top"
    }, {
      "property": "--pf-c-page__header-tools--MarginTop",
      "value": "0.5rem",
      "token": "c_page__header_tools_MarginTop",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-page__header-tools--MarginRight",
      "value": "1rem",
      "token": "c_page__header_tools_MarginRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__header-tools--MarginBottom",
      "value": "0.5rem",
      "token": "c_page__header_tools_MarginBottom",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-page__header-tools--md--MarginRight",
      "value": "1.5rem",
      "token": "c_page__header_tools_md_MarginRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__header-tools--c-avatar--MarginLeft",
      "value": "1rem",
      "token": "c_page__header_tools_c_avatar_MarginLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__header-tools-group--MarginLeft",
      "value": "2rem",
      "token": "c_page__header_tools_group_MarginLeft",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-page__header-tools--c-button--m-selected--before--Width",
      "value": "2.25rem",
      "token": "c_page__header_tools_c_button_m_selected_before_Width"
    }, {
      "property": "--pf-c-page__header-tools--c-button--m-selected--before--Height",
      "value": "2.25rem",
      "token": "c_page__header_tools_c_button_m_selected_before_Height"
    }, {
      "property": "--pf-c-page__header-tools--c-button--m-selected--before--BackgroundColor",
      "value": "#3c3f42",
      "token": "c_page__header_tools_c_button_m_selected_before_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-200", "$pf-global--BackgroundColor--dark-200", "$pf-color-black-800", "#3c3f42"]
    }, {
      "property": "--pf-c-page__header-tools--c-button--m-selected--before--BorderRadius",
      "value": "30em",
      "token": "c_page__header_tools_c_button_m_selected_before_BorderRadius",
      "values": ["--pf-global--BorderRadius--lg", "$pf-global--BorderRadius--lg", "30em"]
    }, {
      "property": "--pf-c-page__header-tools--c-button--m-selected--c-notification-badge--m-unread--after--BorderColor",
      "value": "#3c3f42",
      "token": "c_page__header_tools_c_button_m_selected_c_notification_badge_m_unread_after_BorderColor",
      "values": ["--pf-global--BackgroundColor--dark-200", "$pf-global--BackgroundColor--dark-200", "$pf-color-black-800", "#3c3f42"]
    }, {
      "property": "--pf-c-page__sidebar--ZIndex",
      "value": "200",
      "token": "c_page__sidebar_ZIndex",
      "values": ["--pf-global--ZIndex--sm", "$pf-global--ZIndex--sm", "200"]
    }, {
      "property": "--pf-c-page__sidebar--Width",
      "value": "80%",
      "token": "c_page__sidebar_Width"
    }, {
      "property": "--pf-c-page__sidebar--md--Width",
      "value": "18.125rem",
      "token": "c_page__sidebar_md_Width"
    }, {
      "property": "--pf-c-page__sidebar--BackgroundColor",
      "value": "#fff",
      "token": "c_page__sidebar_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-page__sidebar--BoxShadow",
      "value": "0.75rem 0 0.625rem -0.25rem rgba(3, 3, 3, 0.07)",
      "token": "c_page__sidebar_BoxShadow",
      "values": ["--pf-global--BoxShadow--lg-right", "$pf-global--BoxShadow--lg-right", "pf-size-prem(12) 0 pf-size-prem(10) pf-size-prem(-4) rgba($pf-color-black-1000, .07)", "pf-size-prem(12) 0 pf-size-prem(10) pf-size-prem(-4) rgba(#030303, .07)", "0.75rem 0 0.625rem -0.25rem rgba(3, 3, 3, 0.07)"]
    }, {
      "property": "--pf-c-page__sidebar--Transition",
      "value": "all 250ms cubic-bezier(.42, 0, .58, 1)",
      "token": "c_page__sidebar_Transition",
      "values": ["--pf-global--Transition", "$pf-global--Transition", "all 250ms cubic-bezier(.42, 0, .58, 1)"]
    }, {
      "property": "--pf-c-page__sidebar--Transform",
      "value": "translate3d(-100%, 0, 0)",
      "token": "c_page__sidebar_Transform"
    }, {
      "property": "--pf-c-page__sidebar--m-expanded--Transform",
      "value": "translate3d(0, 0, 0)",
      "token": "c_page__sidebar_m_expanded_Transform"
    }, {
      "property": "--pf-c-page__sidebar--md--Transform",
      "value": "translate3d(0, 0, 0)",
      "token": "c_page__sidebar_md_Transform"
    }, {
      "property": "--pf-c-page__sidebar--m-dark--BackgroundColor",
      "value": "#212427",
      "token": "c_page__sidebar_m_dark_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-300", "$pf-global--BackgroundColor--dark-300", "$pf-color-black-850", "#212427"]
    }, {
      "property": "--pf-c-page__sidebar-body--PaddingTop",
      "value": "0.5rem",
      "token": "c_page__sidebar_body_PaddingTop",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-page__sidebar-body--PaddingBottom",
      "value": "1rem",
      "token": "c_page__sidebar_body_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-section--PaddingTop",
      "value": "1rem",
      "token": "c_page__main_section_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-section--PaddingRight",
      "value": "1rem",
      "token": "c_page__main_section_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-section--PaddingBottom",
      "value": "1rem",
      "token": "c_page__main_section_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-section--PaddingLeft",
      "value": "1rem",
      "token": "c_page__main_section_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-section--md--PaddingTop",
      "value": "1.5rem",
      "token": "c_page__main_section_md_PaddingTop",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-section--md--PaddingRight",
      "value": "1.5rem",
      "token": "c_page__main_section_md_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-section--md--PaddingBottom",
      "value": "1.5rem",
      "token": "c_page__main_section_md_PaddingBottom",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-section--md--PaddingLeft",
      "value": "1.5rem",
      "token": "c_page__main_section_md_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-section--m-no-padding-mobile--md--PaddingTop",
      "value": "0",
      "token": "c_page__main_section_m_no_padding_mobile_md_PaddingTop"
    }, {
      "property": "--pf-c-page__main-section--m-no-padding-mobile--md--PaddingRight",
      "value": "0",
      "token": "c_page__main_section_m_no_padding_mobile_md_PaddingRight"
    }, {
      "property": "--pf-c-page__main-section--m-no-padding-mobile--md--PaddingBottom",
      "value": "0",
      "token": "c_page__main_section_m_no_padding_mobile_md_PaddingBottom"
    }, {
      "property": "--pf-c-page__main-section--m-no-padding-mobile--md--PaddingLeft",
      "value": "0",
      "token": "c_page__main_section_m_no_padding_mobile_md_PaddingLeft"
    }, {
      "property": "--pf-c-page__main-section--BackgroundColor",
      "value": "#ededed",
      "token": "c_page__main_section_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-300", "$pf-global--BackgroundColor--light-300", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-c-page__main--ZIndex",
      "value": "100",
      "token": "c_page__main_ZIndex",
      "values": ["--pf-global--ZIndex--xs", "$pf-global--ZIndex--xs", "100"]
    }, {
      "property": "--pf-c-page__main-nav--BackgroundColor",
      "value": "#fff",
      "token": "c_page__main_nav_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-page__main-nav--PaddingTop",
      "value": "0.5rem",
      "token": "c_page__main_nav_PaddingTop",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-page__main-nav--PaddingRight",
      "value": "1rem",
      "token": "c_page__main_nav_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-nav--PaddingBottom",
      "value": "1rem",
      "token": "c_page__main_nav_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-nav--PaddingLeft",
      "value": "1rem",
      "token": "c_page__main_nav_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-nav--md--PaddingRight",
      "value": "1.5rem",
      "token": "c_page__main_nav_md_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-nav--md--PaddingLeft",
      "value": "1.5rem",
      "token": "c_page__main_nav_md_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-1--Left",
      "value": "calc(-1 * (1.5rem - 0.25rem))",
      "token": "c_page__main_nav_c_nav__scroll_button_nth_of_type_1_Left",
      "values": ["calc(-1 * (--pf-global--spacer--lg - --pf-global--spacer--xs))", "calc(-1 * ($pf-global--spacer--lg - $pf-global--spacer--xs))", "calc(-1 * (pf-size-prem(24px) - pf-size-prem(4px)))", "calc(-1 * (1.5rem - 0.25rem))"]
    }, {
      "property": "--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-1--md--Left",
      "value": "calc(-1 * (1rem - 0.25rem))",
      "token": "c_page__main_nav_c_nav__scroll_button_nth_of_type_1_md_Left",
      "values": ["calc(-1 * (--pf-global--spacer--md - --pf-global--spacer--xs))", "calc(-1 * ($pf-global--spacer--md - $pf-global--spacer--xs))", "calc(-1 * (pf-size-prem(16px) - pf-size-prem(4px)))", "calc(-1 * (1rem - 0.25rem))"]
    }, {
      "property": "--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-2--Right",
      "value": "calc(-1 * (1.5rem - 0.25rem))",
      "token": "c_page__main_nav_c_nav__scroll_button_nth_of_type_2_Right",
      "values": ["calc(-1 * (--pf-global--spacer--lg - --pf-global--spacer--xs))", "calc(-1 * ($pf-global--spacer--lg - $pf-global--spacer--xs))", "calc(-1 * (pf-size-prem(24px) - pf-size-prem(4px)))", "calc(-1 * (1.5rem - 0.25rem))"]
    }, {
      "property": "--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-2--md--Right",
      "value": "calc(-1 * (1rem - 0.25rem))",
      "token": "c_page__main_nav_c_nav__scroll_button_nth_of_type_2_md_Right",
      "values": ["calc(-1 * (--pf-global--spacer--md - --pf-global--spacer--xs))", "calc(-1 * ($pf-global--spacer--md - $pf-global--spacer--xs))", "calc(-1 * (pf-size-prem(16px) - pf-size-prem(4px)))", "calc(-1 * (1rem - 0.25rem))"]
    }, {
      "property": "--pf-c-page__main-breadcrumb--BackgroundColor",
      "value": "#fff",
      "token": "c_page__main_breadcrumb_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-page__main-breadcrumb--PaddingTop",
      "value": "1rem",
      "token": "c_page__main_breadcrumb_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-breadcrumb--PaddingRight",
      "value": "1rem",
      "token": "c_page__main_breadcrumb_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-breadcrumb--PaddingBottom",
      "value": "0",
      "token": "c_page__main_breadcrumb_PaddingBottom"
    }, {
      "property": "--pf-c-page__main-breadcrumb--PaddingLeft",
      "value": "1rem",
      "token": "c_page__main_breadcrumb_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-page__main-breadcrumb--md--PaddingTop",
      "value": "1.5rem",
      "token": "c_page__main_breadcrumb_md_PaddingTop",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-breadcrumb--md--PaddingRight",
      "value": "1.5rem",
      "token": "c_page__main_breadcrumb_md_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-breadcrumb--md--PaddingLeft",
      "value": "1.5rem",
      "token": "c_page__main_breadcrumb_md_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-page__main-nav--main-breadcrumb--PaddingTop",
      "value": "0",
      "token": "c_page__main_nav_main_breadcrumb_PaddingTop"
    }, {
      "property": "--pf-c-page__main-section--m-light--BackgroundColor",
      "value": "#fff",
      "token": "c_page__main_section_m_light_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-page__main-section--m-dark-100--BackgroundColor",
      "value": "rgba(#030303, .62)",
      "token": "c_page__main_section_m_dark_100_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-transparent-100", "$pf-global--BackgroundColor--dark-transparent-100", "rgba($pf-color-black-1000, .62)", "rgba(#030303, .62)"]
    }, {
      "property": "--pf-c-page__main-section--m-dark-200--BackgroundColor",
      "value": "rgba(#030303, .32)",
      "token": "c_page__main_section_m_dark_200_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--dark-transparent-200", "$pf-global--BackgroundColor--dark-transparent-200", "rgba($pf-color-black-1000, .32)", "rgba(#030303, .32)"]
    }, {
      "property": "--pf-c-page__main-wizard--BorderTopColor",
      "value": "#d2d2d2",
      "token": "c_page__main_wizard_BorderTopColor",
      "values": ["--pf-global--BorderColor--100", "$pf-global--BorderColor--100", "$pf-color-black-300", "#d2d2d2"]
    }, {
      "property": "--pf-c-page__main-wizard--BorderTopWidth",
      "value": "1px",
      "token": "c_page__main_wizard_BorderTopWidth",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }],
    ".pf-c-page__sidebar.pf-m-expanded": [{
      "property": "--pf-c-page__sidebar--Transform",
      "value": "translate3d(0, 0, 0)",
      "token": "c_page__sidebar_Transform",
      "values": ["--pf-c-page__sidebar--m-expanded--Transform", "translate3d(0, 0, 0)"]
    }],
    ".pf-c-page__sidebar.pf-m-dark": [{
      "property": "--pf-c-page__sidebar--BackgroundColor",
      "value": "#212427",
      "token": "c_page__sidebar_BackgroundColor",
      "values": ["--pf-c-page__sidebar--m-dark--BackgroundColor", "--pf-global--BackgroundColor--dark-300", "$pf-global--BackgroundColor--dark-300", "$pf-color-black-850", "#212427"]
    }],
    ".pf-c-page__main-nav + .pf-c-page__main-breadcrumb": [{
      "property": "--pf-c-page__main-breadcrumb--PaddingTop",
      "value": "0",
      "token": "c_page__main_breadcrumb_PaddingTop",
      "values": ["--pf-c-page__main-nav--main-breadcrumb--PaddingTop", "0"]
    }],
    ".pf-c-page__main-section.pf-m-light": [{
      "property": "--pf-c-page__main-section--BackgroundColor",
      "value": "#fff",
      "token": "c_page__main_section_BackgroundColor",
      "values": ["--pf-c-page__main-section--m-light--BackgroundColor", "--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }],
    ".pf-c-page__main-section.pf-m-dark-100": [{
      "property": "--pf-c-page__main-section--BackgroundColor",
      "value": "rgba(#030303, .62)",
      "token": "c_page__main_section_BackgroundColor",
      "values": ["--pf-c-page__main-section--m-dark-100--BackgroundColor", "--pf-global--BackgroundColor--dark-transparent-100", "$pf-global--BackgroundColor--dark-transparent-100", "rgba($pf-color-black-1000, .62)", "rgba(#030303, .62)"]
    }],
    ".pf-c-page__main-section.pf-m-dark-200": [{
      "property": "--pf-c-page__main-section--BackgroundColor",
      "value": "rgba(#030303, .32)",
      "token": "c_page__main_section_BackgroundColor",
      "values": ["--pf-c-page__main-section--m-dark-200--BackgroundColor", "--pf-global--BackgroundColor--dark-transparent-200", "$pf-global--BackgroundColor--dark-transparent-200", "rgba($pf-color-black-1000, .32)", "rgba(#030303, .32)"]
    }]
  };
});