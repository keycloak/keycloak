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
  const global_Color_100 = exports.global_Color_100 = {
    "name": "--pf-global--Color--100",
    "value": "#151515",
    "var": "var(--pf-global--Color--100)"
  };
  const global_Color_200 = exports.global_Color_200 = {
    "name": "--pf-global--Color--200",
    "value": "#737679",
    "var": "var(--pf-global--Color--200)"
  };
  const global_BorderColor_100 = exports.global_BorderColor_100 = {
    "name": "--pf-global--BorderColor--100",
    "value": "#d2d2d2",
    "var": "var(--pf-global--BorderColor--100)"
  };
  const global_primary_color_100 = exports.global_primary_color_100 = {
    "name": "--pf-global--primary-color--100",
    "value": "#06c",
    "var": "var(--pf-global--primary-color--100)"
  };
  const global_link_Color = exports.global_link_Color = {
    "name": "--pf-global--link--Color",
    "value": "#004080",
    "var": "var(--pf-global--link--Color)"
  };
  const global_link_Color_hover = exports.global_link_Color_hover = {
    "name": "--pf-global--link--Color--hover",
    "value": "#004080",
    "var": "var(--pf-global--link--Color--hover)"
  };
  const global_BackgroundColor_100 = exports.global_BackgroundColor_100 = {
    "name": "--pf-global--BackgroundColor--100",
    "value": "#fff",
    "var": "var(--pf-global--BackgroundColor--100)"
  };
  const c_card_BackgroundColor = exports.c_card_BackgroundColor = {
    "name": "--pf-c-card--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-card--BackgroundColor)"
  };
  const c_button_m_primary_Color = exports.c_button_m_primary_Color = {
    "name": "--pf-c-button--m-primary--Color",
    "value": "#fff",
    "var": "var(--pf-c-button--m-primary--Color)"
  };
  const c_button_m_primary_hover_Color = exports.c_button_m_primary_hover_Color = {
    "name": "--pf-c-button--m-primary--hover--Color",
    "value": "#fff",
    "var": "var(--pf-c-button--m-primary--hover--Color)"
  };
  const c_button_m_primary_focus_Color = exports.c_button_m_primary_focus_Color = {
    "name": "--pf-c-button--m-primary--focus--Color",
    "value": "#fff",
    "var": "var(--pf-c-button--m-primary--focus--Color)"
  };
  const c_button_m_primary_active_Color = exports.c_button_m_primary_active_Color = {
    "name": "--pf-c-button--m-primary--active--Color",
    "value": "#fff",
    "var": "var(--pf-c-button--m-primary--active--Color)"
  };
  const c_button_m_primary_BackgroundColor = exports.c_button_m_primary_BackgroundColor = {
    "name": "--pf-c-button--m-primary--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-button--m-primary--BackgroundColor)"
  };
  const c_button_m_primary_hover_BackgroundColor = exports.c_button_m_primary_hover_BackgroundColor = {
    "name": "--pf-c-button--m-primary--hover--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-button--m-primary--hover--BackgroundColor)"
  };
  const c_button_m_primary_focus_BackgroundColor = exports.c_button_m_primary_focus_BackgroundColor = {
    "name": "--pf-c-button--m-primary--focus--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-button--m-primary--focus--BackgroundColor)"
  };
  const c_button_m_primary_active_BackgroundColor = exports.c_button_m_primary_active_BackgroundColor = {
    "name": "--pf-c-button--m-primary--active--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-button--m-primary--active--BackgroundColor)"
  };
  const c_button_m_secondary_Color = exports.c_button_m_secondary_Color = {
    "name": "--pf-c-button--m-secondary--Color",
    "value": "#06c",
    "var": "var(--pf-c-button--m-secondary--Color)"
  };
  const c_button_m_secondary_hover_Color = exports.c_button_m_secondary_hover_Color = {
    "name": "--pf-c-button--m-secondary--hover--Color",
    "value": "#06c",
    "var": "var(--pf-c-button--m-secondary--hover--Color)"
  };
  const c_button_m_secondary_focus_Color = exports.c_button_m_secondary_focus_Color = {
    "name": "--pf-c-button--m-secondary--focus--Color",
    "value": "#06c",
    "var": "var(--pf-c-button--m-secondary--focus--Color)"
  };
  const c_button_m_secondary_active_Color = exports.c_button_m_secondary_active_Color = {
    "name": "--pf-c-button--m-secondary--active--Color",
    "value": "#06c",
    "var": "var(--pf-c-button--m-secondary--active--Color)"
  };
  const c_button_m_secondary_BorderColor = exports.c_button_m_secondary_BorderColor = {
    "name": "--pf-c-button--m-secondary--BorderColor",
    "value": "#06c",
    "var": "var(--pf-c-button--m-secondary--BorderColor)"
  };
  const c_button_m_secondary_hover_BorderColor = exports.c_button_m_secondary_hover_BorderColor = {
    "name": "--pf-c-button--m-secondary--hover--BorderColor",
    "value": "#06c",
    "var": "var(--pf-c-button--m-secondary--hover--BorderColor)"
  };
  const c_button_m_secondary_focus_BorderColor = exports.c_button_m_secondary_focus_BorderColor = {
    "name": "--pf-c-button--m-secondary--focus--BorderColor",
    "value": "#06c",
    "var": "var(--pf-c-button--m-secondary--focus--BorderColor)"
  };
  const c_button_m_secondary_active_BorderColor = exports.c_button_m_secondary_active_BorderColor = {
    "name": "--pf-c-button--m-secondary--active--BorderColor",
    "value": "#06c",
    "var": "var(--pf-c-button--m-secondary--active--BorderColor)"
  };
  const c_about_modal_box_BackgroundColor = exports.c_about_modal_box_BackgroundColor = {
    "name": "--pf-c-about-modal-box--BackgroundColor",
    "value": "#030303",
    "var": "var(--pf-c-about-modal-box--BackgroundColor)"
  };
  const c_about_modal_box_BoxShadow = exports.c_about_modal_box_BoxShadow = {
    "name": "--pf-c-about-modal-box--BoxShadow",
    "value": "0 0 100px 0 hsla(0,0%,100%,0.05)",
    "var": "var(--pf-c-about-modal-box--BoxShadow)"
  };
  const c_about_modal_box_ZIndex = exports.c_about_modal_box_ZIndex = {
    "name": "--pf-c-about-modal-box--ZIndex",
    "value": "500",
    "var": "var(--pf-c-about-modal-box--ZIndex)"
  };
  const c_about_modal_box_Height = exports.c_about_modal_box_Height = {
    "name": "--pf-c-about-modal-box--Height",
    "value": "100%",
    "var": "var(--pf-c-about-modal-box--Height)"
  };
  const c_about_modal_box_lg_Height = exports.c_about_modal_box_lg_Height = {
    "name": "--pf-c-about-modal-box--lg--Height",
    "value": "47.625rem",
    "var": "var(--pf-c-about-modal-box--lg--Height)"
  };
  const c_about_modal_box_lg_MaxHeight = exports.c_about_modal_box_lg_MaxHeight = {
    "name": "--pf-c-about-modal-box--lg--MaxHeight",
    "value": "calc(100% - 2rem)",
    "var": "var(--pf-c-about-modal-box--lg--MaxHeight)"
  };
  const c_about_modal_box_Width = exports.c_about_modal_box_Width = {
    "name": "--pf-c-about-modal-box--Width",
    "value": "100vw",
    "var": "var(--pf-c-about-modal-box--Width)"
  };
  const c_about_modal_box_lg_Width = exports.c_about_modal_box_lg_Width = {
    "name": "--pf-c-about-modal-box--lg--Width",
    "value": "calc(100% - 4rem*2)",
    "var": "var(--pf-c-about-modal-box--lg--Width)"
  };
  const c_about_modal_box_lg_MaxWidth = exports.c_about_modal_box_lg_MaxWidth = {
    "name": "--pf-c-about-modal-box--lg--MaxWidth",
    "value": "77rem",
    "var": "var(--pf-c-about-modal-box--lg--MaxWidth)"
  };
  const c_about_modal_box_PaddingTop = exports.c_about_modal_box_PaddingTop = {
    "name": "--pf-c-about-modal-box--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box--PaddingTop)"
  };
  const c_about_modal_box_PaddingRight = exports.c_about_modal_box_PaddingRight = {
    "name": "--pf-c-about-modal-box--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box--PaddingRight)"
  };
  const c_about_modal_box_PaddingBottom = exports.c_about_modal_box_PaddingBottom = {
    "name": "--pf-c-about-modal-box--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box--PaddingBottom)"
  };
  const c_about_modal_box_PaddingLeft = exports.c_about_modal_box_PaddingLeft = {
    "name": "--pf-c-about-modal-box--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box--PaddingLeft)"
  };
  const c_about_modal_box_sm_PaddingTop = exports.c_about_modal_box_sm_PaddingTop = {
    "name": "--pf-c-about-modal-box--sm--PaddingTop",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box--sm--PaddingTop)"
  };
  const c_about_modal_box_sm_PaddingRight = exports.c_about_modal_box_sm_PaddingRight = {
    "name": "--pf-c-about-modal-box--sm--PaddingRight",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box--sm--PaddingRight)"
  };
  const c_about_modal_box_sm_PaddingBottom = exports.c_about_modal_box_sm_PaddingBottom = {
    "name": "--pf-c-about-modal-box--sm--PaddingBottom",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box--sm--PaddingBottom)"
  };
  const c_about_modal_box_sm_PaddingLeft = exports.c_about_modal_box_sm_PaddingLeft = {
    "name": "--pf-c-about-modal-box--sm--PaddingLeft",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box--sm--PaddingLeft)"
  };
  const c_about_modal_box_sm_grid_template_columns = exports.c_about_modal_box_sm_grid_template_columns = {
    "name": "--pf-c-about-modal-box--sm--grid-template-columns",
    "value": "5fr 1fr",
    "var": "var(--pf-c-about-modal-box--sm--grid-template-columns)"
  };
  const c_about_modal_box_lg_grid_template_columns = exports.c_about_modal_box_lg_grid_template_columns = {
    "name": "--pf-c-about-modal-box--lg--grid-template-columns",
    "value": "1fr .6fr",
    "var": "var(--pf-c-about-modal-box--lg--grid-template-columns)"
  };
  const c_about_modal_box__brand_PaddingTop = exports.c_about_modal_box__brand_PaddingTop = {
    "name": "--pf-c-about-modal-box__brand--PaddingTop",
    "value": "3rem",
    "var": "var(--pf-c-about-modal-box__brand--PaddingTop)"
  };
  const c_about_modal_box__brand_PaddingRight = exports.c_about_modal_box__brand_PaddingRight = {
    "name": "--pf-c-about-modal-box__brand--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__brand--PaddingRight)"
  };
  const c_about_modal_box__brand_PaddingLeft = exports.c_about_modal_box__brand_PaddingLeft = {
    "name": "--pf-c-about-modal-box__brand--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__brand--PaddingLeft)"
  };
  const c_about_modal_box__brand_PaddingBottom = exports.c_about_modal_box__brand_PaddingBottom = {
    "name": "--pf-c-about-modal-box__brand--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__brand--PaddingBottom)"
  };
  const c_about_modal_box__brand_sm_PaddingRight = exports.c_about_modal_box__brand_sm_PaddingRight = {
    "name": "--pf-c-about-modal-box__brand--sm--PaddingRight",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__brand--sm--PaddingRight)"
  };
  const c_about_modal_box__brand_sm_PaddingLeft = exports.c_about_modal_box__brand_sm_PaddingLeft = {
    "name": "--pf-c-about-modal-box__brand--sm--PaddingLeft",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__brand--sm--PaddingLeft)"
  };
  const c_about_modal_box__brand_sm_PaddingBottom = exports.c_about_modal_box__brand_sm_PaddingBottom = {
    "name": "--pf-c-about-modal-box__brand--sm--PaddingBottom",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__brand--sm--PaddingBottom)"
  };
  const c_about_modal_box__close_ZIndex = exports.c_about_modal_box__close_ZIndex = {
    "name": "--pf-c-about-modal-box__close--ZIndex",
    "value": "600",
    "var": "var(--pf-c-about-modal-box__close--ZIndex)"
  };
  const c_about_modal_box__close_PaddingTop = exports.c_about_modal_box__close_PaddingTop = {
    "name": "--pf-c-about-modal-box__close--PaddingTop",
    "value": "3rem",
    "var": "var(--pf-c-about-modal-box__close--PaddingTop)"
  };
  const c_about_modal_box__close_PaddingRight = exports.c_about_modal_box__close_PaddingRight = {
    "name": "--pf-c-about-modal-box__close--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__close--PaddingRight)"
  };
  const c_about_modal_box__close_PaddingBottom = exports.c_about_modal_box__close_PaddingBottom = {
    "name": "--pf-c-about-modal-box__close--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__close--PaddingBottom)"
  };
  const c_about_modal_box__close_sm_PaddingBottom = exports.c_about_modal_box__close_sm_PaddingBottom = {
    "name": "--pf-c-about-modal-box__close--sm--PaddingBottom",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__close--sm--PaddingBottom)"
  };
  const c_about_modal_box__close_sm_PaddingRight = exports.c_about_modal_box__close_sm_PaddingRight = {
    "name": "--pf-c-about-modal-box__close--sm--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-about-modal-box__close--sm--PaddingRight)"
  };
  const c_about_modal_box__close_lg_PaddingRight = exports.c_about_modal_box__close_lg_PaddingRight = {
    "name": "--pf-c-about-modal-box__close--lg--PaddingRight",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__close--lg--PaddingRight)"
  };
  const c_about_modal_box__close_c_button_Color = exports.c_about_modal_box__close_c_button_Color = {
    "name": "--pf-c-about-modal-box__close--c-button--Color",
    "value": "#151515",
    "var": "var(--pf-c-about-modal-box__close--c-button--Color)"
  };
  const c_about_modal_box__close_c_button_FontSize = exports.c_about_modal_box__close_c_button_FontSize = {
    "name": "--pf-c-about-modal-box__close--c-button--FontSize",
    "value": "1.25rem",
    "var": "var(--pf-c-about-modal-box__close--c-button--FontSize)"
  };
  const c_about_modal_box__close_c_button_BorderRadius = exports.c_about_modal_box__close_c_button_BorderRadius = {
    "name": "--pf-c-about-modal-box__close--c-button--BorderRadius",
    "value": "30em",
    "var": "var(--pf-c-about-modal-box__close--c-button--BorderRadius)"
  };
  const c_about_modal_box__close_c_button_Width = exports.c_about_modal_box__close_c_button_Width = {
    "name": "--pf-c-about-modal-box__close--c-button--Width",
    "value": "calc(1.25rem*2)",
    "var": "var(--pf-c-about-modal-box__close--c-button--Width)"
  };
  const c_about_modal_box__close_c_button_Height = exports.c_about_modal_box__close_c_button_Height = {
    "name": "--pf-c-about-modal-box__close--c-button--Height",
    "value": "calc(1.25rem*2)",
    "var": "var(--pf-c-about-modal-box__close--c-button--Height)"
  };
  const c_about_modal_box__close_c_button_BackgroundColor = exports.c_about_modal_box__close_c_button_BackgroundColor = {
    "name": "--pf-c-about-modal-box__close--c-button--BackgroundColor",
    "value": "rgba(3,3,3,0.4)",
    "var": "var(--pf-c-about-modal-box__close--c-button--BackgroundColor)"
  };
  const c_about_modal_box__close_c_button_hover_BackgroundColor = exports.c_about_modal_box__close_c_button_hover_BackgroundColor = {
    "name": "--pf-c-about-modal-box__close--c-button--hover--BackgroundColor",
    "value": "rgba(3,3,3,0.4)",
    "var": "var(--pf-c-about-modal-box__close--c-button--hover--BackgroundColor)"
  };
  const c_about_modal_box__hero_sm_BackgroundImage = exports.c_about_modal_box__hero_sm_BackgroundImage = {
    "name": "--pf-c-about-modal-box__hero--sm--BackgroundImage",
    "value": "url(assets/images/pfbg_992@2x.jpg)",
    "var": "var(--pf-c-about-modal-box__hero--sm--BackgroundImage)"
  };
  const c_about_modal_box__hero_sm_BackgroundPosition = exports.c_about_modal_box__hero_sm_BackgroundPosition = {
    "name": "--pf-c-about-modal-box__hero--sm--BackgroundPosition",
    "value": "top left",
    "var": "var(--pf-c-about-modal-box__hero--sm--BackgroundPosition)"
  };
  const c_about_modal_box__hero_sm_BackgroundSize = exports.c_about_modal_box__hero_sm_BackgroundSize = {
    "name": "--pf-c-about-modal-box__hero--sm--BackgroundSize",
    "value": "cover",
    "var": "var(--pf-c-about-modal-box__hero--sm--BackgroundSize)"
  };
  const c_about_modal_box__brand_image_Height = exports.c_about_modal_box__brand_image_Height = {
    "name": "--pf-c-about-modal-box__brand-image--Height",
    "value": "2.5rem",
    "var": "var(--pf-c-about-modal-box__brand-image--Height)"
  };
  const c_about_modal_box__header_PaddingRight = exports.c_about_modal_box__header_PaddingRight = {
    "name": "--pf-c-about-modal-box__header--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__header--PaddingRight)"
  };
  const c_about_modal_box__header_PaddingBottom = exports.c_about_modal_box__header_PaddingBottom = {
    "name": "--pf-c-about-modal-box__header--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-about-modal-box__header--PaddingBottom)"
  };
  const c_about_modal_box__header_PaddingLeft = exports.c_about_modal_box__header_PaddingLeft = {
    "name": "--pf-c-about-modal-box__header--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__header--PaddingLeft)"
  };
  const c_about_modal_box__header_sm_PaddingRight = exports.c_about_modal_box__header_sm_PaddingRight = {
    "name": "--pf-c-about-modal-box__header--sm--PaddingRight",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__header--sm--PaddingRight)"
  };
  const c_about_modal_box__header_sm_PaddingLeft = exports.c_about_modal_box__header_sm_PaddingLeft = {
    "name": "--pf-c-about-modal-box__header--sm--PaddingLeft",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__header--sm--PaddingLeft)"
  };
  const c_about_modal_box__strapline_PaddingTop = exports.c_about_modal_box__strapline_PaddingTop = {
    "name": "--pf-c-about-modal-box__strapline--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__strapline--PaddingTop)"
  };
  const c_about_modal_box__strapline_FontSize = exports.c_about_modal_box__strapline_FontSize = {
    "name": "--pf-c-about-modal-box__strapline--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-about-modal-box__strapline--FontSize)"
  };
  const c_about_modal_box__strapline_sm_PaddingTop = exports.c_about_modal_box__strapline_sm_PaddingTop = {
    "name": "--pf-c-about-modal-box__strapline--sm--PaddingTop",
    "value": "3rem",
    "var": "var(--pf-c-about-modal-box__strapline--sm--PaddingTop)"
  };
  const c_about_modal_box__content_MarginTop = exports.c_about_modal_box__content_MarginTop = {
    "name": "--pf-c-about-modal-box__content--MarginTop",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__content--MarginTop)"
  };
  const c_about_modal_box__content_MarginRight = exports.c_about_modal_box__content_MarginRight = {
    "name": "--pf-c-about-modal-box__content--MarginRight",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__content--MarginRight)"
  };
  const c_about_modal_box__content_MarginBottom = exports.c_about_modal_box__content_MarginBottom = {
    "name": "--pf-c-about-modal-box__content--MarginBottom",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__content--MarginBottom)"
  };
  const c_about_modal_box__content_MarginLeft = exports.c_about_modal_box__content_MarginLeft = {
    "name": "--pf-c-about-modal-box__content--MarginLeft",
    "value": "2rem",
    "var": "var(--pf-c-about-modal-box__content--MarginLeft)"
  };
  const c_about_modal_box__content_sm_MarginTop = exports.c_about_modal_box__content_sm_MarginTop = {
    "name": "--pf-c-about-modal-box__content--sm--MarginTop",
    "value": "3rem",
    "var": "var(--pf-c-about-modal-box__content--sm--MarginTop)"
  };
  const c_about_modal_box__content_sm_MarginRight = exports.c_about_modal_box__content_sm_MarginRight = {
    "name": "--pf-c-about-modal-box__content--sm--MarginRight",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__content--sm--MarginRight)"
  };
  const c_about_modal_box__content_sm_MarginBottom = exports.c_about_modal_box__content_sm_MarginBottom = {
    "name": "--pf-c-about-modal-box__content--sm--MarginBottom",
    "value": "3rem",
    "var": "var(--pf-c-about-modal-box__content--sm--MarginBottom)"
  };
  const c_about_modal_box__content_sm_MarginLeft = exports.c_about_modal_box__content_sm_MarginLeft = {
    "name": "--pf-c-about-modal-box__content--sm--MarginLeft",
    "value": "4rem",
    "var": "var(--pf-c-about-modal-box__content--sm--MarginLeft)"
  };
  const c_accordion_BackgroundColor = exports.c_accordion_BackgroundColor = {
    "name": "--pf-c-accordion--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-accordion--BackgroundColor)"
  };
  const c_accordion_BorderWidth = exports.c_accordion_BorderWidth = {
    "name": "--pf-c-accordion--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-accordion--BorderWidth)"
  };
  const c_accordion_BoxShadow = exports.c_accordion_BoxShadow = {
    "name": "--pf-c-accordion--BoxShadow",
    "value": "none",
    "var": "var(--pf-c-accordion--BoxShadow)"
  };
  const c_accordion_PaddingTop = exports.c_accordion_PaddingTop = {
    "name": "--pf-c-accordion--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-accordion--PaddingTop)"
  };
  const c_accordion_PaddingBottom = exports.c_accordion_PaddingBottom = {
    "name": "--pf-c-accordion--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-accordion--PaddingBottom)"
  };
  const c_accordion__toggle_PaddingTop = exports.c_accordion__toggle_PaddingTop = {
    "name": "--pf-c-accordion__toggle--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-accordion__toggle--PaddingTop)"
  };
  const c_accordion__toggle_PaddingRight = exports.c_accordion__toggle_PaddingRight = {
    "name": "--pf-c-accordion__toggle--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-accordion__toggle--PaddingRight)"
  };
  const c_accordion__toggle_PaddingBottom = exports.c_accordion__toggle_PaddingBottom = {
    "name": "--pf-c-accordion__toggle--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-accordion__toggle--PaddingBottom)"
  };
  const c_accordion__toggle_PaddingLeft = exports.c_accordion__toggle_PaddingLeft = {
    "name": "--pf-c-accordion__toggle--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-accordion__toggle--PaddingLeft)"
  };
  const c_accordion__toggle_BorderLeftColor = exports.c_accordion__toggle_BorderLeftColor = {
    "name": "--pf-c-accordion__toggle--BorderLeftColor",
    "value": "#06c",
    "var": "var(--pf-c-accordion__toggle--BorderLeftColor)"
  };
  const c_accordion__toggle_hover_BackgroundColor = exports.c_accordion__toggle_hover_BackgroundColor = {
    "name": "--pf-c-accordion__toggle--hover--BackgroundColor",
    "value": "#f5f5f5",
    "var": "var(--pf-c-accordion__toggle--hover--BackgroundColor)"
  };
  const c_accordion__toggle_focus_BackgroundColor = exports.c_accordion__toggle_focus_BackgroundColor = {
    "name": "--pf-c-accordion__toggle--focus--BackgroundColor",
    "value": "#f5f5f5",
    "var": "var(--pf-c-accordion__toggle--focus--BackgroundColor)"
  };
  const c_accordion__toggle_active_BackgroundColor = exports.c_accordion__toggle_active_BackgroundColor = {
    "name": "--pf-c-accordion__toggle--active--BackgroundColor",
    "value": "#f5f5f5",
    "var": "var(--pf-c-accordion__toggle--active--BackgroundColor)"
  };
  const c_accordion__toggle_m_expanded_BorderWidth = exports.c_accordion__toggle_m_expanded_BorderWidth = {
    "name": "--pf-c-accordion__toggle--m-expanded--BorderWidth",
    "value": "3px",
    "var": "var(--pf-c-accordion__toggle--m-expanded--BorderWidth)"
  };
  const c_accordion__toggle_m_expanded_BorderLeftColor = exports.c_accordion__toggle_m_expanded_BorderLeftColor = {
    "name": "--pf-c-accordion__toggle--m-expanded--BorderLeftColor",
    "value": "#06c",
    "var": "var(--pf-c-accordion__toggle--m-expanded--BorderLeftColor)"
  };
  const c_accordion__toggle_text_hover_Color = exports.c_accordion__toggle_text_hover_Color = {
    "name": "--pf-c-accordion__toggle-text--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-accordion__toggle-text--hover--Color)"
  };
  const c_accordion__toggle_text_hover_FontWeight = exports.c_accordion__toggle_text_hover_FontWeight = {
    "name": "--pf-c-accordion__toggle-text--hover--FontWeight",
    "value": "700",
    "var": "var(--pf-c-accordion__toggle-text--hover--FontWeight)"
  };
  const c_accordion__toggle_text_active_Color = exports.c_accordion__toggle_text_active_Color = {
    "name": "--pf-c-accordion__toggle-text--active--Color",
    "value": "#004080",
    "var": "var(--pf-c-accordion__toggle-text--active--Color)"
  };
  const c_accordion__toggle_text_active_FontWeight = exports.c_accordion__toggle_text_active_FontWeight = {
    "name": "--pf-c-accordion__toggle-text--active--FontWeight",
    "value": "700",
    "var": "var(--pf-c-accordion__toggle-text--active--FontWeight)"
  };
  const c_accordion__toggle_text_focus_Color = exports.c_accordion__toggle_text_focus_Color = {
    "name": "--pf-c-accordion__toggle-text--focus--Color",
    "value": "#004080",
    "var": "var(--pf-c-accordion__toggle-text--focus--Color)"
  };
  const c_accordion__toggle_text_focus_FontWeight = exports.c_accordion__toggle_text_focus_FontWeight = {
    "name": "--pf-c-accordion__toggle-text--focus--FontWeight",
    "value": "700",
    "var": "var(--pf-c-accordion__toggle-text--focus--FontWeight)"
  };
  const c_accordion__toggle_text_expanded_Color = exports.c_accordion__toggle_text_expanded_Color = {
    "name": "--pf-c-accordion__toggle-text--expanded--Color",
    "value": "#004080",
    "var": "var(--pf-c-accordion__toggle-text--expanded--Color)"
  };
  const c_accordion__toggle_text_expanded_FontWeight = exports.c_accordion__toggle_text_expanded_FontWeight = {
    "name": "--pf-c-accordion__toggle-text--expanded--FontWeight",
    "value": "700",
    "var": "var(--pf-c-accordion__toggle-text--expanded--FontWeight)"
  };
  const c_accordion__toggle_text_MaxWidth = exports.c_accordion__toggle_text_MaxWidth = {
    "name": "--pf-c-accordion__toggle-text--MaxWidth",
    "value": "calc(100% - 1.5rem)",
    "var": "var(--pf-c-accordion__toggle-text--MaxWidth)"
  };
  const c_accordion__toggle_icon_LineHeight = exports.c_accordion__toggle_icon_LineHeight = {
    "name": "--pf-c-accordion__toggle-icon--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-accordion__toggle-icon--LineHeight)"
  };
  const c_accordion__toggle_icon_Transition = exports.c_accordion__toggle_icon_Transition = {
    "name": "--pf-c-accordion__toggle-icon--Transition",
    "value": ".2s ease-in 0s",
    "var": "var(--pf-c-accordion__toggle-icon--Transition)"
  };
  const c_accordion__toggle_m_expanded__toggle_icon_Transform = exports.c_accordion__toggle_m_expanded__toggle_icon_Transform = {
    "name": "--pf-c-accordion__toggle--m-expanded__toggle-icon--Transform",
    "value": "rotate(90deg)",
    "var": "var(--pf-c-accordion__toggle--m-expanded__toggle-icon--Transform)"
  };
  const c_accordion__expanded_content_body_PaddingTop = exports.c_accordion__expanded_content_body_PaddingTop = {
    "name": "--pf-c-accordion__expanded-content-body--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-accordion__expanded-content-body--PaddingTop)"
  };
  const c_accordion__expanded_content_body_PaddingRight = exports.c_accordion__expanded_content_body_PaddingRight = {
    "name": "--pf-c-accordion__expanded-content-body--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-accordion__expanded-content-body--PaddingRight)"
  };
  const c_accordion__expanded_content_body_PaddingBottom = exports.c_accordion__expanded_content_body_PaddingBottom = {
    "name": "--pf-c-accordion__expanded-content-body--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-accordion__expanded-content-body--PaddingBottom)"
  };
  const c_accordion__expanded_content_body_PaddingLeft = exports.c_accordion__expanded_content_body_PaddingLeft = {
    "name": "--pf-c-accordion__expanded-content-body--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-accordion__expanded-content-body--PaddingLeft)"
  };
  const c_accordion__expanded_content_Color = exports.c_accordion__expanded_content_Color = {
    "name": "--pf-c-accordion__expanded-content--Color",
    "value": "#737679",
    "var": "var(--pf-c-accordion__expanded-content--Color)"
  };
  const c_accordion__expanded_content_FontSize = exports.c_accordion__expanded_content_FontSize = {
    "name": "--pf-c-accordion__expanded-content--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-accordion__expanded-content--FontSize)"
  };
  const c_accordion__expanded_content_BorderLeftColor = exports.c_accordion__expanded_content_BorderLeftColor = {
    "name": "--pf-c-accordion__expanded-content--BorderLeftColor",
    "value": "#06c",
    "var": "var(--pf-c-accordion__expanded-content--BorderLeftColor)"
  };
  const c_accordion__expanded_content_m_expanded_BorderWidth = exports.c_accordion__expanded_content_m_expanded_BorderWidth = {
    "name": "--pf-c-accordion__expanded-content--m-expanded--BorderWidth",
    "value": "3px",
    "var": "var(--pf-c-accordion__expanded-content--m-expanded--BorderWidth)"
  };
  const c_accordion__expanded_content_m_expanded_BorderLeftColor = exports.c_accordion__expanded_content_m_expanded_BorderLeftColor = {
    "name": "--pf-c-accordion__expanded-content--m-expanded--BorderLeftColor",
    "value": "#06c",
    "var": "var(--pf-c-accordion__expanded-content--m-expanded--BorderLeftColor)"
  };
  const c_accordion__expanded_content_m_fixed_MaxHeight = exports.c_accordion__expanded_content_m_fixed_MaxHeight = {
    "name": "--pf-c-accordion__expanded-content--m-fixed--MaxHeight",
    "value": "9.375rem",
    "var": "var(--pf-c-accordion__expanded-content--m-fixed--MaxHeight)"
  };
  const c_alert_BoxShadow = exports.c_alert_BoxShadow = {
    "name": "--pf-c-alert--BoxShadow",
    "value": "none",
    "var": "var(--pf-c-alert--BoxShadow)"
  };
  const c_alert_BackgroundColor = exports.c_alert_BackgroundColor = {
    "name": "--pf-c-alert--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-alert--BackgroundColor)"
  };
  const c_alert_grid_template_columns = exports.c_alert_grid_template_columns = {
    "name": "--pf-c-alert--grid-template-columns",
    "value": "max-content 1fr max-content",
    "var": "var(--pf-c-alert--grid-template-columns)"
  };
  const c_alert_grid_template_rows = exports.c_alert_grid_template_rows = {
    "name": "--pf-c-alert--grid-template-rows",
    "value": "1fr auto",
    "var": "var(--pf-c-alert--grid-template-rows)"
  };
  const c_alert__icon_Padding = exports.c_alert__icon_Padding = {
    "name": "--pf-c-alert__icon--Padding",
    "value": "1rem",
    "var": "var(--pf-c-alert__icon--Padding)"
  };
  const c_alert__icon_Color = exports.c_alert__icon_Color = {
    "name": "--pf-c-alert__icon--Color",
    "value": "#009596",
    "var": "var(--pf-c-alert__icon--Color)"
  };
  const c_alert__icon_BackgroundColor = exports.c_alert__icon_BackgroundColor = {
    "name": "--pf-c-alert__icon--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-alert__icon--BackgroundColor)"
  };
  const c_alert__icon_FontSize = exports.c_alert__icon_FontSize = {
    "name": "--pf-c-alert__icon--FontSize",
    "value": "1.125rem",
    "var": "var(--pf-c-alert__icon--FontSize)"
  };
  const c_alert__title_FontSize = exports.c_alert__title_FontSize = {
    "name": "--pf-c-alert__title--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-alert__title--FontSize)"
  };
  const c_alert__title_Color = exports.c_alert__title_Color = {
    "name": "--pf-c-alert__title--Color",
    "value": "#004368",
    "var": "var(--pf-c-alert__title--Color)"
  };
  const c_alert__title_PaddingTop = exports.c_alert__title_PaddingTop = {
    "name": "--pf-c-alert__title--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-alert__title--PaddingTop)"
  };
  const c_alert__title_PaddingRight = exports.c_alert__title_PaddingRight = {
    "name": "--pf-c-alert__title--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-alert__title--PaddingRight)"
  };
  const c_alert__title_PaddingBottom = exports.c_alert__title_PaddingBottom = {
    "name": "--pf-c-alert__title--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-alert__title--PaddingBottom)"
  };
  const c_alert__title_PaddingLeft = exports.c_alert__title_PaddingLeft = {
    "name": "--pf-c-alert__title--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-alert__title--PaddingLeft)"
  };
  const c_alert__description_PaddingRight = exports.c_alert__description_PaddingRight = {
    "name": "--pf-c-alert__description--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-alert__description--PaddingRight)"
  };
  const c_alert__description_PaddingBottom = exports.c_alert__description_PaddingBottom = {
    "name": "--pf-c-alert__description--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-alert__description--PaddingBottom)"
  };
  const c_alert__description_PaddingLeft = exports.c_alert__description_PaddingLeft = {
    "name": "--pf-c-alert__description--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-alert__description--PaddingLeft)"
  };
  const c_alert__description_MarginTop = exports.c_alert__description_MarginTop = {
    "name": "--pf-c-alert__description--MarginTop",
    "value": "calc(-1*0.5rem)",
    "var": "var(--pf-c-alert__description--MarginTop)"
  };
  const c_alert__action_PaddingTop = exports.c_alert__action_PaddingTop = {
    "name": "--pf-c-alert__action--PaddingTop",
    "value": "0.6875rem",
    "var": "var(--pf-c-alert__action--PaddingTop)"
  };
  const c_alert__action_PaddingRight = exports.c_alert__action_PaddingRight = {
    "name": "--pf-c-alert__action--PaddingRight",
    "value": "0.25rem",
    "var": "var(--pf-c-alert__action--PaddingRight)"
  };
  const c_alert_m_success__icon_Color = exports.c_alert_m_success__icon_Color = {
    "name": "--pf-c-alert--m-success__icon--Color",
    "value": "#486b00",
    "var": "var(--pf-c-alert--m-success__icon--Color)"
  };
  const c_alert_m_success__icon_BackgroundColor = exports.c_alert_m_success__icon_BackgroundColor = {
    "name": "--pf-c-alert--m-success__icon--BackgroundColor",
    "value": "#92d400",
    "var": "var(--pf-c-alert--m-success__icon--BackgroundColor)"
  };
  const c_alert_m_success__title_Color = exports.c_alert_m_success__title_Color = {
    "name": "--pf-c-alert--m-success__title--Color",
    "value": "#486b00",
    "var": "var(--pf-c-alert--m-success__title--Color)"
  };
  const c_alert_m_danger__icon_Color = exports.c_alert_m_danger__icon_Color = {
    "name": "--pf-c-alert--m-danger__icon--Color",
    "value": "#470000",
    "var": "var(--pf-c-alert--m-danger__icon--Color)"
  };
  const c_alert_m_danger__icon_BackgroundColor = exports.c_alert_m_danger__icon_BackgroundColor = {
    "name": "--pf-c-alert--m-danger__icon--BackgroundColor",
    "value": "#c9190b",
    "var": "var(--pf-c-alert--m-danger__icon--BackgroundColor)"
  };
  const c_alert_m_danger__title_Color = exports.c_alert_m_danger__title_Color = {
    "name": "--pf-c-alert--m-danger__title--Color",
    "value": "#a30000",
    "var": "var(--pf-c-alert--m-danger__title--Color)"
  };
  const c_alert_m_warning__icon_Color = exports.c_alert_m_warning__icon_Color = {
    "name": "--pf-c-alert--m-warning__icon--Color",
    "value": "#795600",
    "var": "var(--pf-c-alert--m-warning__icon--Color)"
  };
  const c_alert_m_warning__icon_BackgroundColor = exports.c_alert_m_warning__icon_BackgroundColor = {
    "name": "--pf-c-alert--m-warning__icon--BackgroundColor",
    "value": "#f0ab00",
    "var": "var(--pf-c-alert--m-warning__icon--BackgroundColor)"
  };
  const c_alert_m_warning__icon_FontSize = exports.c_alert_m_warning__icon_FontSize = {
    "name": "--pf-c-alert--m-warning__icon--FontSize",
    "value": "1.25rem",
    "var": "var(--pf-c-alert--m-warning__icon--FontSize)"
  };
  const c_alert_m_warning__title_Color = exports.c_alert_m_warning__title_Color = {
    "name": "--pf-c-alert--m-warning__title--Color",
    "value": "#795600",
    "var": "var(--pf-c-alert--m-warning__title--Color)"
  };
  const c_alert_m_info__icon_Color = exports.c_alert_m_info__icon_Color = {
    "name": "--pf-c-alert--m-info__icon--Color",
    "value": "#004368",
    "var": "var(--pf-c-alert--m-info__icon--Color)"
  };
  const c_alert_m_info__icon_BackgroundColor = exports.c_alert_m_info__icon_BackgroundColor = {
    "name": "--pf-c-alert--m-info__icon--BackgroundColor",
    "value": "#73bcf7",
    "var": "var(--pf-c-alert--m-info__icon--BackgroundColor)"
  };
  const c_alert_m_info__title_Color = exports.c_alert_m_info__title_Color = {
    "name": "--pf-c-alert--m-info__title--Color",
    "value": "#004368",
    "var": "var(--pf-c-alert--m-info__title--Color)"
  };
  const c_alert_m_inline_BoxShadow = exports.c_alert_m_inline_BoxShadow = {
    "name": "--pf-c-alert--m-inline--BoxShadow",
    "value": "none",
    "var": "var(--pf-c-alert--m-inline--BoxShadow)"
  };
  const c_alert_m_inline_BorderColor = exports.c_alert_m_inline_BorderColor = {
    "name": "--pf-c-alert--m-inline--BorderColor",
    "value": "#ededed",
    "var": "var(--pf-c-alert--m-inline--BorderColor)"
  };
  const c_alert_m_inline_BorderStyle = exports.c_alert_m_inline_BorderStyle = {
    "name": "--pf-c-alert--m-inline--BorderStyle",
    "value": "solid",
    "var": "var(--pf-c-alert--m-inline--BorderStyle)"
  };
  const c_alert_m_inline_BorderTopWidth = exports.c_alert_m_inline_BorderTopWidth = {
    "name": "--pf-c-alert--m-inline--BorderTopWidth",
    "value": "1px",
    "var": "var(--pf-c-alert--m-inline--BorderTopWidth)"
  };
  const c_alert_m_inline_BorderRightWidth = exports.c_alert_m_inline_BorderRightWidth = {
    "name": "--pf-c-alert--m-inline--BorderRightWidth",
    "value": "1px",
    "var": "var(--pf-c-alert--m-inline--BorderRightWidth)"
  };
  const c_alert_m_inline_BorderBottomWidth = exports.c_alert_m_inline_BorderBottomWidth = {
    "name": "--pf-c-alert--m-inline--BorderBottomWidth",
    "value": "1px",
    "var": "var(--pf-c-alert--m-inline--BorderBottomWidth)"
  };
  const c_alert_m_inline_BorderLeftWidth = exports.c_alert_m_inline_BorderLeftWidth = {
    "name": "--pf-c-alert--m-inline--BorderLeftWidth",
    "value": "0",
    "var": "var(--pf-c-alert--m-inline--BorderLeftWidth)"
  };
  const c_alert_m_inline_before_Width = exports.c_alert_m_inline_before_Width = {
    "name": "--pf-c-alert--m-inline--before--Width",
    "value": "3px",
    "var": "var(--pf-c-alert--m-inline--before--Width)"
  };
  const c_alert_m_inline_before_Top = exports.c_alert_m_inline_before_Top = {
    "name": "--pf-c-alert--m-inline--before--Top",
    "value": "calc(-1*1px)",
    "var": "var(--pf-c-alert--m-inline--before--Top)"
  };
  const c_alert_m_inline_before_Bottom = exports.c_alert_m_inline_before_Bottom = {
    "name": "--pf-c-alert--m-inline--before--Bottom",
    "value": "calc(-1*1px)",
    "var": "var(--pf-c-alert--m-inline--before--Bottom)"
  };
  const c_alert_m_inline_before_BackgroundColor = exports.c_alert_m_inline_before_BackgroundColor = {
    "name": "--pf-c-alert--m-inline--before--BackgroundColor",
    "value": "#73bcf7",
    "var": "var(--pf-c-alert--m-inline--before--BackgroundColor)"
  };
  const c_alert_m_inline__icon_FontSize = exports.c_alert_m_inline__icon_FontSize = {
    "name": "--pf-c-alert--m-inline__icon--FontSize",
    "value": "1.125rem",
    "var": "var(--pf-c-alert--m-inline__icon--FontSize)"
  };
  const c_alert_m_inline__icon_Color = exports.c_alert_m_inline__icon_Color = {
    "name": "--pf-c-alert--m-inline__icon--Color",
    "value": "#009596",
    "var": "var(--pf-c-alert--m-inline__icon--Color)"
  };
  const c_alert_m_inline__icon_BackgroundColor = exports.c_alert_m_inline__icon_BackgroundColor = {
    "name": "--pf-c-alert--m-inline__icon--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-alert--m-inline__icon--BackgroundColor)"
  };
  const c_alert_m_inline__icon_PaddingTop = exports.c_alert_m_inline__icon_PaddingTop = {
    "name": "--pf-c-alert--m-inline__icon--PaddingTop",
    "value": "calc(1rem + (1.5rem - 1.125rem)/2)",
    "var": "var(--pf-c-alert--m-inline__icon--PaddingTop)"
  };
  const c_alert_m_inline__icon_PaddingRight = exports.c_alert_m_inline__icon_PaddingRight = {
    "name": "--pf-c-alert--m-inline__icon--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-alert--m-inline__icon--PaddingRight)"
  };
  const c_alert_m_inline__icon_PaddingBottom = exports.c_alert_m_inline__icon_PaddingBottom = {
    "name": "--pf-c-alert--m-inline__icon--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-alert--m-inline__icon--PaddingBottom)"
  };
  const c_alert_m_inline__icon_PaddingLeft = exports.c_alert_m_inline__icon_PaddingLeft = {
    "name": "--pf-c-alert--m-inline__icon--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-alert--m-inline__icon--PaddingLeft)"
  };
  const c_alert_m_inline_m_warning__icon_FontSize = exports.c_alert_m_inline_m_warning__icon_FontSize = {
    "name": "--pf-c-alert--m-inline--m-warning__icon--FontSize",
    "value": "1.0625rem",
    "var": "var(--pf-c-alert--m-inline--m-warning__icon--FontSize)"
  };
  const c_alert_m_inline_m_success__icon_Color = exports.c_alert_m_inline_m_success__icon_Color = {
    "name": "--pf-c-alert--m-inline--m-success__icon--Color",
    "value": "#92d400",
    "var": "var(--pf-c-alert--m-inline--m-success__icon--Color)"
  };
  const c_alert_m_inline_m_success_before_BackgroundColor = exports.c_alert_m_inline_m_success_before_BackgroundColor = {
    "name": "--pf-c-alert--m-inline--m-success--before--BackgroundColor",
    "value": "#92d400",
    "var": "var(--pf-c-alert--m-inline--m-success--before--BackgroundColor)"
  };
  const c_alert_m_inline_m_danger__icon_Color = exports.c_alert_m_inline_m_danger__icon_Color = {
    "name": "--pf-c-alert--m-inline--m-danger__icon--Color",
    "value": "#c9190b",
    "var": "var(--pf-c-alert--m-inline--m-danger__icon--Color)"
  };
  const c_alert_m_inline_m_danger_before_BackgroundColor = exports.c_alert_m_inline_m_danger_before_BackgroundColor = {
    "name": "--pf-c-alert--m-inline--m-danger--before--BackgroundColor",
    "value": "#c9190b",
    "var": "var(--pf-c-alert--m-inline--m-danger--before--BackgroundColor)"
  };
  const c_alert_m_inline_m_warning__icon_Color = exports.c_alert_m_inline_m_warning__icon_Color = {
    "name": "--pf-c-alert--m-inline--m-warning__icon--Color",
    "value": "#f0ab00",
    "var": "var(--pf-c-alert--m-inline--m-warning__icon--Color)"
  };
  const c_alert_m_inline_m_warning_before_BackgroundColor = exports.c_alert_m_inline_m_warning_before_BackgroundColor = {
    "name": "--pf-c-alert--m-inline--m-warning--before--BackgroundColor",
    "value": "#f0ab00",
    "var": "var(--pf-c-alert--m-inline--m-warning--before--BackgroundColor)"
  };
  const c_alert_m_inline_m_info__icon_Color = exports.c_alert_m_inline_m_info__icon_Color = {
    "name": "--pf-c-alert--m-inline--m-info__icon--Color",
    "value": "#73bcf7",
    "var": "var(--pf-c-alert--m-inline--m-info__icon--Color)"
  };
  const c_alert_m_inline_m_info_before_BackgroundColor = exports.c_alert_m_inline_m_info_before_BackgroundColor = {
    "name": "--pf-c-alert--m-inline--m-info--before--BackgroundColor",
    "value": "#73bcf7",
    "var": "var(--pf-c-alert--m-inline--m-info--before--BackgroundColor)"
  };
  const c_alert_group__item_MarginTop = exports.c_alert_group__item_MarginTop = {
    "name": "--pf-c-alert-group__item--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-alert-group__item--MarginTop)"
  };
  const c_alert_group_m_toast_Top = exports.c_alert_group_m_toast_Top = {
    "name": "--pf-c-alert-group--m-toast--Top",
    "value": "3rem",
    "var": "var(--pf-c-alert-group--m-toast--Top)"
  };
  const c_alert_group_m_toast_Right = exports.c_alert_group_m_toast_Right = {
    "name": "--pf-c-alert-group--m-toast--Right",
    "value": "2rem",
    "var": "var(--pf-c-alert-group--m-toast--Right)"
  };
  const c_alert_group_m_toast_MaxWidth = exports.c_alert_group_m_toast_MaxWidth = {
    "name": "--pf-c-alert-group--m-toast--MaxWidth",
    "value": "37.5rem",
    "var": "var(--pf-c-alert-group--m-toast--MaxWidth)"
  };
  const c_alert_group_m_toast_ZIndex = exports.c_alert_group_m_toast_ZIndex = {
    "name": "--pf-c-alert-group--m-toast--ZIndex",
    "value": "600",
    "var": "var(--pf-c-alert-group--m-toast--ZIndex)"
  };
  const c_app_launcher__menu_BackgroundColor = exports.c_app_launcher__menu_BackgroundColor = {
    "name": "--pf-c-app-launcher__menu--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-app-launcher__menu--BackgroundColor)"
  };
  const c_app_launcher__menu_BorderWidth = exports.c_app_launcher__menu_BorderWidth = {
    "name": "--pf-c-app-launcher__menu--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-app-launcher__menu--BorderWidth)"
  };
  const c_app_launcher__menu_BoxShadow = exports.c_app_launcher__menu_BoxShadow = {
    "name": "--pf-c-app-launcher__menu--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-app-launcher__menu--BoxShadow)"
  };
  const c_app_launcher__menu_PaddingTop = exports.c_app_launcher__menu_PaddingTop = {
    "name": "--pf-c-app-launcher__menu--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__menu--PaddingTop)"
  };
  const c_app_launcher__menu_PaddingBottom = exports.c_app_launcher__menu_PaddingBottom = {
    "name": "--pf-c-app-launcher__menu--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__menu--PaddingBottom)"
  };
  const c_app_launcher__menu_Top = exports.c_app_launcher__menu_Top = {
    "name": "--pf-c-app-launcher__menu--Top",
    "value": "0",
    "var": "var(--pf-c-app-launcher__menu--Top)"
  };
  const c_app_launcher__menu_ZIndex = exports.c_app_launcher__menu_ZIndex = {
    "name": "--pf-c-app-launcher__menu--ZIndex",
    "value": "200",
    "var": "var(--pf-c-app-launcher__menu--ZIndex)"
  };
  const c_app_launcher_m_top__menu_Top = exports.c_app_launcher_m_top__menu_Top = {
    "name": "--pf-c-app-launcher--m-top__menu--Top",
    "value": "0",
    "var": "var(--pf-c-app-launcher--m-top__menu--Top)"
  };
  const c_app_launcher_m_top__menu_Transform = exports.c_app_launcher_m_top__menu_Transform = {
    "name": "--pf-c-app-launcher--m-top__menu--Transform",
    "value": "translateY(calc(-100% - 0.25rem))",
    "var": "var(--pf-c-app-launcher--m-top__menu--Transform)"
  };
  const c_app_launcher__toggle_PaddingTop = exports.c_app_launcher__toggle_PaddingTop = {
    "name": "--pf-c-app-launcher__toggle--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-app-launcher__toggle--PaddingTop)"
  };
  const c_app_launcher__toggle_PaddingRight = exports.c_app_launcher__toggle_PaddingRight = {
    "name": "--pf-c-app-launcher__toggle--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__toggle--PaddingRight)"
  };
  const c_app_launcher__toggle_PaddingBottom = exports.c_app_launcher__toggle_PaddingBottom = {
    "name": "--pf-c-app-launcher__toggle--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-app-launcher__toggle--PaddingBottom)"
  };
  const c_app_launcher__toggle_PaddingLeft = exports.c_app_launcher__toggle_PaddingLeft = {
    "name": "--pf-c-app-launcher__toggle--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__toggle--PaddingLeft)"
  };
  const c_app_launcher__toggle_Color = exports.c_app_launcher__toggle_Color = {
    "name": "--pf-c-app-launcher__toggle--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-app-launcher__toggle--Color)"
  };
  const c_app_launcher__toggle_hover_Color = exports.c_app_launcher__toggle_hover_Color = {
    "name": "--pf-c-app-launcher__toggle--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-app-launcher__toggle--hover--Color)"
  };
  const c_app_launcher__toggle_active_Color = exports.c_app_launcher__toggle_active_Color = {
    "name": "--pf-c-app-launcher__toggle--active--Color",
    "value": "#151515",
    "var": "var(--pf-c-app-launcher__toggle--active--Color)"
  };
  const c_app_launcher__toggle_focus_Color = exports.c_app_launcher__toggle_focus_Color = {
    "name": "--pf-c-app-launcher__toggle--focus--Color",
    "value": "#151515",
    "var": "var(--pf-c-app-launcher__toggle--focus--Color)"
  };
  const c_app_launcher__toggle_disabled_Color = exports.c_app_launcher__toggle_disabled_Color = {
    "name": "--pf-c-app-launcher__toggle--disabled--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-app-launcher__toggle--disabled--Color)"
  };
  const c_app_launcher__toggle_m_expanded_Color = exports.c_app_launcher__toggle_m_expanded_Color = {
    "name": "--pf-c-app-launcher__toggle--m-expanded--Color",
    "value": "#151515",
    "var": "var(--pf-c-app-launcher__toggle--m-expanded--Color)"
  };
  const c_app_launcher__menu_search_PaddingTop = exports.c_app_launcher__menu_search_PaddingTop = {
    "name": "--pf-c-app-launcher__menu-search--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__menu-search--PaddingTop)"
  };
  const c_app_launcher__menu_search_PaddingRight = exports.c_app_launcher__menu_search_PaddingRight = {
    "name": "--pf-c-app-launcher__menu-search--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__menu-search--PaddingRight)"
  };
  const c_app_launcher__menu_search_PaddingBottom = exports.c_app_launcher__menu_search_PaddingBottom = {
    "name": "--pf-c-app-launcher__menu-search--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__menu-search--PaddingBottom)"
  };
  const c_app_launcher__menu_search_PaddingLeft = exports.c_app_launcher__menu_search_PaddingLeft = {
    "name": "--pf-c-app-launcher__menu-search--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__menu-search--PaddingLeft)"
  };
  const c_app_launcher__menu_search_BottomBorderColor = exports.c_app_launcher__menu_search_BottomBorderColor = {
    "name": "--pf-c-app-launcher__menu-search--BottomBorderColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-app-launcher__menu-search--BottomBorderColor)"
  };
  const c_app_launcher__menu_search_BottomBorderWidth = exports.c_app_launcher__menu_search_BottomBorderWidth = {
    "name": "--pf-c-app-launcher__menu-search--BottomBorderWidth",
    "value": "1px",
    "var": "var(--pf-c-app-launcher__menu-search--BottomBorderWidth)"
  };
  const c_app_launcher__menu_search_MarginBottom = exports.c_app_launcher__menu_search_MarginBottom = {
    "name": "--pf-c-app-launcher__menu-search--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__menu-search--MarginBottom)"
  };
  const c_app_launcher__menu_item_PaddingTop = exports.c_app_launcher__menu_item_PaddingTop = {
    "name": "--pf-c-app-launcher__menu-item--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__menu-item--PaddingTop)"
  };
  const c_app_launcher__menu_item_PaddingRight = exports.c_app_launcher__menu_item_PaddingRight = {
    "name": "--pf-c-app-launcher__menu-item--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-app-launcher__menu-item--PaddingRight)"
  };
  const c_app_launcher__menu_item_PaddingBottom = exports.c_app_launcher__menu_item_PaddingBottom = {
    "name": "--pf-c-app-launcher__menu-item--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__menu-item--PaddingBottom)"
  };
  const c_app_launcher__menu_item_PaddingLeft = exports.c_app_launcher__menu_item_PaddingLeft = {
    "name": "--pf-c-app-launcher__menu-item--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__menu-item--PaddingLeft)"
  };
  const c_app_launcher__menu_item_Color = exports.c_app_launcher__menu_item_Color = {
    "name": "--pf-c-app-launcher__menu-item--Color",
    "value": "#737679",
    "var": "var(--pf-c-app-launcher__menu-item--Color)"
  };
  const c_app_launcher__menu_item_FontWeight = exports.c_app_launcher__menu_item_FontWeight = {
    "name": "--pf-c-app-launcher__menu-item--FontWeight",
    "value": "400",
    "var": "var(--pf-c-app-launcher__menu-item--FontWeight)"
  };
  const c_app_launcher__menu_item_Width = exports.c_app_launcher__menu_item_Width = {
    "name": "--pf-c-app-launcher__menu-item--Width",
    "value": "auto",
    "var": "var(--pf-c-app-launcher__menu-item--Width)"
  };
  const c_app_launcher__menu_item_disabled_Color = exports.c_app_launcher__menu_item_disabled_Color = {
    "name": "--pf-c-app-launcher__menu-item--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-app-launcher__menu-item--disabled--Color)"
  };
  const c_app_launcher__menu_item_hover_BackgroundColor = exports.c_app_launcher__menu_item_hover_BackgroundColor = {
    "name": "--pf-c-app-launcher__menu-item--hover--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-app-launcher__menu-item--hover--BackgroundColor)"
  };
  const c_app_launcher__menu_item_m_link_PaddingRight = exports.c_app_launcher__menu_item_m_link_PaddingRight = {
    "name": "--pf-c-app-launcher__menu-item--m-link--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-app-launcher__menu-item--m-link--PaddingRight)"
  };
  const c_app_launcher__menu_item_m_link_hover_BackgroundColor = exports.c_app_launcher__menu_item_m_link_hover_BackgroundColor = {
    "name": "--pf-c-app-launcher__menu-item--m-link--hover--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-app-launcher__menu-item--m-link--hover--BackgroundColor)"
  };
  const c_app_launcher__menu_item_m_action_hover_BackgroundColor = exports.c_app_launcher__menu_item_m_action_hover_BackgroundColor = {
    "name": "--pf-c-app-launcher__menu-item--m-action--hover--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-app-launcher__menu-item--m-action--hover--BackgroundColor)"
  };
  const c_app_launcher__menu_item_m_action_Color = exports.c_app_launcher__menu_item_m_action_Color = {
    "name": "--pf-c-app-launcher__menu-item--m-action--Color",
    "value": "#151515",
    "var": "var(--pf-c-app-launcher__menu-item--m-action--Color)"
  };
  const c_app_launcher__menu_item_m_action_Width = exports.c_app_launcher__menu_item_m_action_Width = {
    "name": "--pf-c-app-launcher__menu-item--m-action--Width",
    "value": "auto",
    "var": "var(--pf-c-app-launcher__menu-item--m-action--Width)"
  };
  const c_app_launcher__menu_item_m_action_FontSize = exports.c_app_launcher__menu_item_m_action_FontSize = {
    "name": "--pf-c-app-launcher__menu-item--m-action--FontSize",
    "value": "0.625rem",
    "var": "var(--pf-c-app-launcher__menu-item--m-action--FontSize)"
  };
  const c_app_launcher__menu_item_hover__menu_item_m_action_Color = exports.c_app_launcher__menu_item_hover__menu_item_m_action_Color = {
    "name": "--pf-c-app-launcher__menu-item--hover__menu-item--m-action--Color",
    "value": "#737679",
    "var": "var(--pf-c-app-launcher__menu-item--hover__menu-item--m-action--Color)"
  };
  const c_app_launcher__menu_item_m_action_hover_Color = exports.c_app_launcher__menu_item_m_action_hover_Color = {
    "name": "--pf-c-app-launcher__menu-item--m-action--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-app-launcher__menu-item--m-action--hover--Color)"
  };
  const c_app_launcher__menu_item_m_favorite__menu_item_m_action_Color = exports.c_app_launcher__menu_item_m_favorite__menu_item_m_action_Color = {
    "name": "--pf-c-app-launcher__menu-item--m-favorite__menu-item--m-action--Color",
    "value": "#f0ab00",
    "var": "var(--pf-c-app-launcher__menu-item--m-favorite__menu-item--m-action--Color)"
  };
  const c_app_launcher__menu_item_icon_MarginRight = exports.c_app_launcher__menu_item_icon_MarginRight = {
    "name": "--pf-c-app-launcher__menu-item-icon--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__menu-item-icon--MarginRight)"
  };
  const c_app_launcher__menu_item_icon_Width = exports.c_app_launcher__menu_item_icon_Width = {
    "name": "--pf-c-app-launcher__menu-item-icon--Width",
    "value": "1.5rem",
    "var": "var(--pf-c-app-launcher__menu-item-icon--Width)"
  };
  const c_app_launcher__menu_item_icon_Height = exports.c_app_launcher__menu_item_icon_Height = {
    "name": "--pf-c-app-launcher__menu-item-icon--Height",
    "value": "1.5rem",
    "var": "var(--pf-c-app-launcher__menu-item-icon--Height)"
  };
  const c_app_launcher__menu_item_external_icon_Color = exports.c_app_launcher__menu_item_external_icon_Color = {
    "name": "--pf-c-app-launcher__menu-item-external-icon--Color",
    "value": "#004080",
    "var": "var(--pf-c-app-launcher__menu-item-external-icon--Color)"
  };
  const c_app_launcher__menu_item_external_icon_PaddingLeft = exports.c_app_launcher__menu_item_external_icon_PaddingLeft = {
    "name": "--pf-c-app-launcher__menu-item-external-icon--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__menu-item-external-icon--PaddingLeft)"
  };
  const c_app_launcher__menu_item_external_icon_Transform = exports.c_app_launcher__menu_item_external_icon_Transform = {
    "name": "--pf-c-app-launcher__menu-item-external-icon--Transform",
    "value": "translateY(-0.0625rem)",
    "var": "var(--pf-c-app-launcher__menu-item-external-icon--Transform)"
  };
  const c_app_launcher__menu_item_external_icon_FontSize = exports.c_app_launcher__menu_item_external_icon_FontSize = {
    "name": "--pf-c-app-launcher__menu-item-external-icon--FontSize",
    "value": "0.625rem",
    "var": "var(--pf-c-app-launcher__menu-item-external-icon--FontSize)"
  };
  const c_app_launcher__c_divider_MarginTop = exports.c_app_launcher__c_divider_MarginTop = {
    "name": "--pf-c-app-launcher__c-divider--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__c-divider--MarginTop)"
  };
  const c_app_launcher__c_divider_MarginBottom = exports.c_app_launcher__c_divider_MarginBottom = {
    "name": "--pf-c-app-launcher__c-divider--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__c-divider--MarginBottom)"
  };
  const c_app_launcher__separator_Height = exports.c_app_launcher__separator_Height = {
    "name": "--pf-c-app-launcher__separator--Height",
    "value": "1px",
    "var": "var(--pf-c-app-launcher__separator--Height)"
  };
  const c_app_launcher__separator_BackgroundColor = exports.c_app_launcher__separator_BackgroundColor = {
    "name": "--pf-c-app-launcher__separator--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-app-launcher__separator--BackgroundColor)"
  };
  const c_app_launcher__separator_MarginTop = exports.c_app_launcher__separator_MarginTop = {
    "name": "--pf-c-app-launcher__separator--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__separator--MarginTop)"
  };
  const c_app_launcher__separator_MarginBottom = exports.c_app_launcher__separator_MarginBottom = {
    "name": "--pf-c-app-launcher__separator--MarginBottom",
    "value": "0",
    "var": "var(--pf-c-app-launcher__separator--MarginBottom)"
  };
  const c_app_launcher__group_PaddingTop = exports.c_app_launcher__group_PaddingTop = {
    "name": "--pf-c-app-launcher__group--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__group--PaddingTop)"
  };
  const c_app_launcher__group_group_PaddingTop = exports.c_app_launcher__group_group_PaddingTop = {
    "name": "--pf-c-app-launcher__group--group--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__group--group--PaddingTop)"
  };
  const c_app_launcher__group_first_child_PaddingTop = exports.c_app_launcher__group_first_child_PaddingTop = {
    "name": "--pf-c-app-launcher__group--first-child--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-app-launcher__group--first-child--PaddingTop)"
  };
  const c_app_launcher__group_title_PaddingTop = exports.c_app_launcher__group_title_PaddingTop = {
    "name": "--pf-c-app-launcher__group-title--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__group-title--PaddingTop)"
  };
  const c_app_launcher__group_title_PaddingRight = exports.c_app_launcher__group_title_PaddingRight = {
    "name": "--pf-c-app-launcher__group-title--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__group-title--PaddingRight)"
  };
  const c_app_launcher__group_title_PaddingBottom = exports.c_app_launcher__group_title_PaddingBottom = {
    "name": "--pf-c-app-launcher__group-title--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-app-launcher__group-title--PaddingBottom)"
  };
  const c_app_launcher__group_title_PaddingLeft = exports.c_app_launcher__group_title_PaddingLeft = {
    "name": "--pf-c-app-launcher__group-title--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-app-launcher__group-title--PaddingLeft)"
  };
  const c_app_launcher__group_title_FontSize = exports.c_app_launcher__group_title_FontSize = {
    "name": "--pf-c-app-launcher__group-title--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-app-launcher__group-title--FontSize)"
  };
  const c_app_launcher__group_title_FontWeight = exports.c_app_launcher__group_title_FontWeight = {
    "name": "--pf-c-app-launcher__group-title--FontWeight",
    "value": "700",
    "var": "var(--pf-c-app-launcher__group-title--FontWeight)"
  };
  const c_app_launcher__group_title_Color = exports.c_app_launcher__group_title_Color = {
    "name": "--pf-c-app-launcher__group-title--Color",
    "value": "#737679",
    "var": "var(--pf-c-app-launcher__group-title--Color)"
  };
  const c_avatar_BorderRadius = exports.c_avatar_BorderRadius = {
    "name": "--pf-c-avatar--BorderRadius",
    "value": "30em",
    "var": "var(--pf-c-avatar--BorderRadius)"
  };
  const c_avatar_Width = exports.c_avatar_Width = {
    "name": "--pf-c-avatar--Width",
    "value": "2.25rem",
    "var": "var(--pf-c-avatar--Width)"
  };
  const c_avatar_Height = exports.c_avatar_Height = {
    "name": "--pf-c-avatar--Height",
    "value": "2.25rem",
    "var": "var(--pf-c-avatar--Height)"
  };
  const c_backdrop_ZIndex = exports.c_backdrop_ZIndex = {
    "name": "--pf-c-backdrop--ZIndex",
    "value": "400",
    "var": "var(--pf-c-backdrop--ZIndex)"
  };
  const c_backdrop_Color = exports.c_backdrop_Color = {
    "name": "--pf-c-backdrop--Color",
    "value": "rgba(3,3,3,0.62)",
    "var": "var(--pf-c-backdrop--Color)"
  };
  const c_backdrop_BackdropFilter = exports.c_backdrop_BackdropFilter = {
    "name": "--pf-c-backdrop--BackdropFilter",
    "value": "blur(10px)",
    "var": "var(--pf-c-backdrop--BackdropFilter)"
  };
  const c_background_image_BackgroundColor = exports.c_background_image_BackgroundColor = {
    "name": "--pf-c-background-image--BackgroundColor",
    "value": "#151515",
    "var": "var(--pf-c-background-image--BackgroundColor)"
  };
  const c_background_image_BackgroundImage = exports.c_background_image_BackgroundImage = {
    "name": "--pf-c-background-image--BackgroundImage",
    "value": "url(assets/images/pfbg_576.jpg)",
    "var": "var(--pf-c-background-image--BackgroundImage)"
  };
  const c_background_image_BackgroundImage_2x = exports.c_background_image_BackgroundImage_2x = {
    "name": "--pf-c-background-image--BackgroundImage-2x",
    "value": "url(assets/images/pfbg_576@2x.jpg)",
    "var": "var(--pf-c-background-image--BackgroundImage-2x)"
  };
  const c_background_image_BackgroundImage_sm = exports.c_background_image_BackgroundImage_sm = {
    "name": "--pf-c-background-image--BackgroundImage--sm",
    "value": "url(assets/images/pfbg_768.jpg)",
    "var": "var(--pf-c-background-image--BackgroundImage--sm)"
  };
  const c_background_image_BackgroundImage_sm_2x = exports.c_background_image_BackgroundImage_sm_2x = {
    "name": "--pf-c-background-image--BackgroundImage--sm-2x",
    "value": "url(assets/images/pfbg_768@2x.jpg)",
    "var": "var(--pf-c-background-image--BackgroundImage--sm-2x)"
  };
  const c_background_image_BackgroundImage_lg = exports.c_background_image_BackgroundImage_lg = {
    "name": "--pf-c-background-image--BackgroundImage--lg",
    "value": "url(assets/images/pfbg_2000.jpg)",
    "var": "var(--pf-c-background-image--BackgroundImage--lg)"
  };
  const c_background_image_Filter = exports.c_background_image_Filter = {
    "name": "--pf-c-background-image--Filter",
    "value": "url(#image_overlay)",
    "var": "var(--pf-c-background-image--Filter)"
  };
  const c_badge_BorderRadius = exports.c_badge_BorderRadius = {
    "name": "--pf-c-badge--BorderRadius",
    "value": "30em",
    "var": "var(--pf-c-badge--BorderRadius)"
  };
  const c_badge_FontSize = exports.c_badge_FontSize = {
    "name": "--pf-c-badge--FontSize",
    "value": "0.75rem",
    "var": "var(--pf-c-badge--FontSize)"
  };
  const c_badge_FontWeight = exports.c_badge_FontWeight = {
    "name": "--pf-c-badge--FontWeight",
    "value": "700",
    "var": "var(--pf-c-badge--FontWeight)"
  };
  const c_badge_PaddingRight = exports.c_badge_PaddingRight = {
    "name": "--pf-c-badge--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-badge--PaddingRight)"
  };
  const c_badge_PaddingLeft = exports.c_badge_PaddingLeft = {
    "name": "--pf-c-badge--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-badge--PaddingLeft)"
  };
  const c_badge_Color = exports.c_badge_Color = {
    "name": "--pf-c-badge--Color",
    "value": "#fff",
    "var": "var(--pf-c-badge--Color)"
  };
  const c_badge_MinWidth = exports.c_badge_MinWidth = {
    "name": "--pf-c-badge--MinWidth",
    "value": "2rem",
    "var": "var(--pf-c-badge--MinWidth)"
  };
  const c_badge_m_read_BackgroundColor = exports.c_badge_m_read_BackgroundColor = {
    "name": "--pf-c-badge--m-read--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-badge--m-read--BackgroundColor)"
  };
  const c_badge_m_read_Color = exports.c_badge_m_read_Color = {
    "name": "--pf-c-badge--m-read--Color",
    "value": "#151515",
    "var": "var(--pf-c-badge--m-read--Color)"
  };
  const c_badge_m_unread_BackgroundColor = exports.c_badge_m_unread_BackgroundColor = {
    "name": "--pf-c-badge--m-unread--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-badge--m-unread--BackgroundColor)"
  };
  const c_badge_m_unread_Color = exports.c_badge_m_unread_Color = {
    "name": "--pf-c-badge--m-unread--Color",
    "value": "#fff",
    "var": "var(--pf-c-badge--m-unread--Color)"
  };
  const c_badge_BackgroundColor = exports.c_badge_BackgroundColor = {
    "name": "--pf-c-badge--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-badge--BackgroundColor)"
  };
  const c_breadcrumb__item_FontSize = exports.c_breadcrumb__item_FontSize = {
    "name": "--pf-c-breadcrumb__item--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-breadcrumb__item--FontSize)"
  };
  const c_breadcrumb__item_FontWeight = exports.c_breadcrumb__item_FontWeight = {
    "name": "--pf-c-breadcrumb__item--FontWeight",
    "value": "400",
    "var": "var(--pf-c-breadcrumb__item--FontWeight)"
  };
  const c_breadcrumb__item_LineHeight = exports.c_breadcrumb__item_LineHeight = {
    "name": "--pf-c-breadcrumb__item--LineHeight",
    "value": "1.3",
    "var": "var(--pf-c-breadcrumb__item--LineHeight)"
  };
  const c_breadcrumb__item_MarginRight = exports.c_breadcrumb__item_MarginRight = {
    "name": "--pf-c-breadcrumb__item--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-breadcrumb__item--MarginRight)"
  };
  const c_breadcrumb__item_divider_Color = exports.c_breadcrumb__item_divider_Color = {
    "name": "--pf-c-breadcrumb__item-divider--Color",
    "value": "#8a8d90",
    "var": "var(--pf-c-breadcrumb__item-divider--Color)"
  };
  const c_breadcrumb__item_divider_MarginLeft = exports.c_breadcrumb__item_divider_MarginLeft = {
    "name": "--pf-c-breadcrumb__item-divider--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-breadcrumb__item-divider--MarginLeft)"
  };
  const c_breadcrumb__item_divider_FontSize = exports.c_breadcrumb__item_divider_FontSize = {
    "name": "--pf-c-breadcrumb__item-divider--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-breadcrumb__item-divider--FontSize)"
  };
  const c_breadcrumb__link_FontWeight = exports.c_breadcrumb__link_FontWeight = {
    "name": "--pf-c-breadcrumb__link--FontWeight",
    "value": "400",
    "var": "var(--pf-c-breadcrumb__link--FontWeight)"
  };
  const c_breadcrumb__link_m_current_Color = exports.c_breadcrumb__link_m_current_Color = {
    "name": "--pf-c-breadcrumb__link--m-current--Color",
    "value": "#151515",
    "var": "var(--pf-c-breadcrumb__link--m-current--Color)"
  };
  const c_breadcrumb__heading_FontSize = exports.c_breadcrumb__heading_FontSize = {
    "name": "--pf-c-breadcrumb__heading--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-breadcrumb__heading--FontSize)"
  };
  const c_button_PaddingTop = exports.c_button_PaddingTop = {
    "name": "--pf-c-button--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-button--PaddingTop)"
  };
  const c_button_PaddingRight = exports.c_button_PaddingRight = {
    "name": "--pf-c-button--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-button--PaddingRight)"
  };
  const c_button_PaddingBottom = exports.c_button_PaddingBottom = {
    "name": "--pf-c-button--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-button--PaddingBottom)"
  };
  const c_button_PaddingLeft = exports.c_button_PaddingLeft = {
    "name": "--pf-c-button--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-button--PaddingLeft)"
  };
  const c_button_LineHeight = exports.c_button_LineHeight = {
    "name": "--pf-c-button--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-button--LineHeight)"
  };
  const c_button_FontWeight = exports.c_button_FontWeight = {
    "name": "--pf-c-button--FontWeight",
    "value": "400",
    "var": "var(--pf-c-button--FontWeight)"
  };
  const c_button_FontSize = exports.c_button_FontSize = {
    "name": "--pf-c-button--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-button--FontSize)"
  };
  const c_button_BorderRadius = exports.c_button_BorderRadius = {
    "name": "--pf-c-button--BorderRadius",
    "value": "0",
    "var": "var(--pf-c-button--BorderRadius)"
  };
  const c_button_BorderColor = exports.c_button_BorderColor = {
    "name": "--pf-c-button--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-button--BorderColor)"
  };
  const c_button_BorderWidth = exports.c_button_BorderWidth = {
    "name": "--pf-c-button--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-button--BorderWidth)"
  };
  const c_button_hover_BorderWidth = exports.c_button_hover_BorderWidth = {
    "name": "--pf-c-button--hover--BorderWidth",
    "value": "2px",
    "var": "var(--pf-c-button--hover--BorderWidth)"
  };
  const c_button_focus_BorderWidth = exports.c_button_focus_BorderWidth = {
    "name": "--pf-c-button--focus--BorderWidth",
    "value": "2px",
    "var": "var(--pf-c-button--focus--BorderWidth)"
  };
  const c_button_active_BorderWidth = exports.c_button_active_BorderWidth = {
    "name": "--pf-c-button--active--BorderWidth",
    "value": "2px",
    "var": "var(--pf-c-button--active--BorderWidth)"
  };
  const c_button_disabled_Color = exports.c_button_disabled_Color = {
    "name": "--pf-c-button--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-button--disabled--Color)"
  };
  const c_button_disabled_BackgroundColor = exports.c_button_disabled_BackgroundColor = {
    "name": "--pf-c-button--disabled--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-button--disabled--BackgroundColor)"
  };
  const c_button_disabled_BorderColor = exports.c_button_disabled_BorderColor = {
    "name": "--pf-c-button--disabled--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-button--disabled--BorderColor)"
  };
  const c_button_m_secondary_BackgroundColor = exports.c_button_m_secondary_BackgroundColor = {
    "name": "--pf-c-button--m-secondary--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-secondary--BackgroundColor)"
  };
  const c_button_m_secondary_hover_BackgroundColor = exports.c_button_m_secondary_hover_BackgroundColor = {
    "name": "--pf-c-button--m-secondary--hover--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-secondary--hover--BackgroundColor)"
  };
  const c_button_m_secondary_focus_BackgroundColor = exports.c_button_m_secondary_focus_BackgroundColor = {
    "name": "--pf-c-button--m-secondary--focus--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-secondary--focus--BackgroundColor)"
  };
  const c_button_m_secondary_active_BackgroundColor = exports.c_button_m_secondary_active_BackgroundColor = {
    "name": "--pf-c-button--m-secondary--active--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-secondary--active--BackgroundColor)"
  };
  const c_button_m_tertiary_BackgroundColor = exports.c_button_m_tertiary_BackgroundColor = {
    "name": "--pf-c-button--m-tertiary--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-tertiary--BackgroundColor)"
  };
  const c_button_m_tertiary_BorderColor = exports.c_button_m_tertiary_BorderColor = {
    "name": "--pf-c-button--m-tertiary--BorderColor",
    "value": "#151515",
    "var": "var(--pf-c-button--m-tertiary--BorderColor)"
  };
  const c_button_m_tertiary_Color = exports.c_button_m_tertiary_Color = {
    "name": "--pf-c-button--m-tertiary--Color",
    "value": "#151515",
    "var": "var(--pf-c-button--m-tertiary--Color)"
  };
  const c_button_m_tertiary_hover_BackgroundColor = exports.c_button_m_tertiary_hover_BackgroundColor = {
    "name": "--pf-c-button--m-tertiary--hover--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-tertiary--hover--BackgroundColor)"
  };
  const c_button_m_tertiary_hover_BorderColor = exports.c_button_m_tertiary_hover_BorderColor = {
    "name": "--pf-c-button--m-tertiary--hover--BorderColor",
    "value": "#151515",
    "var": "var(--pf-c-button--m-tertiary--hover--BorderColor)"
  };
  const c_button_m_tertiary_hover_Color = exports.c_button_m_tertiary_hover_Color = {
    "name": "--pf-c-button--m-tertiary--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-button--m-tertiary--hover--Color)"
  };
  const c_button_m_tertiary_focus_BackgroundColor = exports.c_button_m_tertiary_focus_BackgroundColor = {
    "name": "--pf-c-button--m-tertiary--focus--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-tertiary--focus--BackgroundColor)"
  };
  const c_button_m_tertiary_focus_BorderColor = exports.c_button_m_tertiary_focus_BorderColor = {
    "name": "--pf-c-button--m-tertiary--focus--BorderColor",
    "value": "#151515",
    "var": "var(--pf-c-button--m-tertiary--focus--BorderColor)"
  };
  const c_button_m_tertiary_focus_Color = exports.c_button_m_tertiary_focus_Color = {
    "name": "--pf-c-button--m-tertiary--focus--Color",
    "value": "#151515",
    "var": "var(--pf-c-button--m-tertiary--focus--Color)"
  };
  const c_button_m_tertiary_active_BackgroundColor = exports.c_button_m_tertiary_active_BackgroundColor = {
    "name": "--pf-c-button--m-tertiary--active--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-tertiary--active--BackgroundColor)"
  };
  const c_button_m_tertiary_active_BorderColor = exports.c_button_m_tertiary_active_BorderColor = {
    "name": "--pf-c-button--m-tertiary--active--BorderColor",
    "value": "#151515",
    "var": "var(--pf-c-button--m-tertiary--active--BorderColor)"
  };
  const c_button_m_tertiary_active_Color = exports.c_button_m_tertiary_active_Color = {
    "name": "--pf-c-button--m-tertiary--active--Color",
    "value": "#151515",
    "var": "var(--pf-c-button--m-tertiary--active--Color)"
  };
  const c_button_m_danger_BackgroundColor = exports.c_button_m_danger_BackgroundColor = {
    "name": "--pf-c-button--m-danger--BackgroundColor",
    "value": "#a30000",
    "var": "var(--pf-c-button--m-danger--BackgroundColor)"
  };
  const c_button_m_danger_Color = exports.c_button_m_danger_Color = {
    "name": "--pf-c-button--m-danger--Color",
    "value": "#fff",
    "var": "var(--pf-c-button--m-danger--Color)"
  };
  const c_button_m_danger_hover_BackgroundColor = exports.c_button_m_danger_hover_BackgroundColor = {
    "name": "--pf-c-button--m-danger--hover--BackgroundColor",
    "value": "#a30000",
    "var": "var(--pf-c-button--m-danger--hover--BackgroundColor)"
  };
  const c_button_m_danger_hover_Color = exports.c_button_m_danger_hover_Color = {
    "name": "--pf-c-button--m-danger--hover--Color",
    "value": "#fff",
    "var": "var(--pf-c-button--m-danger--hover--Color)"
  };
  const c_button_m_danger_focus_BackgroundColor = exports.c_button_m_danger_focus_BackgroundColor = {
    "name": "--pf-c-button--m-danger--focus--BackgroundColor",
    "value": "#a30000",
    "var": "var(--pf-c-button--m-danger--focus--BackgroundColor)"
  };
  const c_button_m_danger_focus_Color = exports.c_button_m_danger_focus_Color = {
    "name": "--pf-c-button--m-danger--focus--Color",
    "value": "#fff",
    "var": "var(--pf-c-button--m-danger--focus--Color)"
  };
  const c_button_m_danger_active_BackgroundColor = exports.c_button_m_danger_active_BackgroundColor = {
    "name": "--pf-c-button--m-danger--active--BackgroundColor",
    "value": "#a30000",
    "var": "var(--pf-c-button--m-danger--active--BackgroundColor)"
  };
  const c_button_m_danger_active_Color = exports.c_button_m_danger_active_Color = {
    "name": "--pf-c-button--m-danger--active--Color",
    "value": "#fff",
    "var": "var(--pf-c-button--m-danger--active--Color)"
  };
  const c_button_m_link_Color = exports.c_button_m_link_Color = {
    "name": "--pf-c-button--m-link--Color",
    "value": "#004080",
    "var": "var(--pf-c-button--m-link--Color)"
  };
  const c_button_m_link_hover_Color = exports.c_button_m_link_hover_Color = {
    "name": "--pf-c-button--m-link--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-button--m-link--hover--Color)"
  };
  const c_button_m_link_focus_Color = exports.c_button_m_link_focus_Color = {
    "name": "--pf-c-button--m-link--focus--Color",
    "value": "#004080",
    "var": "var(--pf-c-button--m-link--focus--Color)"
  };
  const c_button_m_link_active_Color = exports.c_button_m_link_active_Color = {
    "name": "--pf-c-button--m-link--active--Color",
    "value": "#004080",
    "var": "var(--pf-c-button--m-link--active--Color)"
  };
  const c_button_m_link_disabled_BackgroundColor = exports.c_button_m_link_disabled_BackgroundColor = {
    "name": "--pf-c-button--m-link--disabled--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-link--disabled--BackgroundColor)"
  };
  const c_button_m_link_m_inline_hover_TextDecoration = exports.c_button_m_link_m_inline_hover_TextDecoration = {
    "name": "--pf-c-button--m-link--m-inline--hover--TextDecoration",
    "value": "underline",
    "var": "var(--pf-c-button--m-link--m-inline--hover--TextDecoration)"
  };
  const c_button_m_link_m_inline_hover_Color = exports.c_button_m_link_m_inline_hover_Color = {
    "name": "--pf-c-button--m-link--m-inline--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-button--m-link--m-inline--hover--Color)"
  };
  const c_button_m_plain_Color = exports.c_button_m_plain_Color = {
    "name": "--pf-c-button--m-plain--Color",
    "value": "#004080",
    "var": "var(--pf-c-button--m-plain--Color)"
  };
  const c_button_m_plain_hover_Color = exports.c_button_m_plain_hover_Color = {
    "name": "--pf-c-button--m-plain--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-button--m-plain--hover--Color)"
  };
  const c_button_m_plain_focus_Color = exports.c_button_m_plain_focus_Color = {
    "name": "--pf-c-button--m-plain--focus--Color",
    "value": "#151515",
    "var": "var(--pf-c-button--m-plain--focus--Color)"
  };
  const c_button_m_plain_active_Color = exports.c_button_m_plain_active_Color = {
    "name": "--pf-c-button--m-plain--active--Color",
    "value": "#151515",
    "var": "var(--pf-c-button--m-plain--active--Color)"
  };
  const c_button_m_plain_disabled_Color = exports.c_button_m_plain_disabled_Color = {
    "name": "--pf-c-button--m-plain--disabled--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-button--m-plain--disabled--Color)"
  };
  const c_button_m_plain_disabled_BackgroundColor = exports.c_button_m_plain_disabled_BackgroundColor = {
    "name": "--pf-c-button--m-plain--disabled--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-plain--disabled--BackgroundColor)"
  };
  const c_button_m_control_after_BorderWidth = exports.c_button_m_control_after_BorderWidth = {
    "name": "--pf-c-button--m-control--after--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-button--m-control--after--BorderWidth)"
  };
  const c_button_m_control_after_BorderTopColor = exports.c_button_m_control_after_BorderTopColor = {
    "name": "--pf-c-button--m-control--after--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-button--m-control--after--BorderTopColor)"
  };
  const c_button_m_control_after_BorderRightColor = exports.c_button_m_control_after_BorderRightColor = {
    "name": "--pf-c-button--m-control--after--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-button--m-control--after--BorderRightColor)"
  };
  const c_button_m_control_after_BorderBottomColor = exports.c_button_m_control_after_BorderBottomColor = {
    "name": "--pf-c-button--m-control--after--BorderBottomColor",
    "value": "#ededed",
    "var": "var(--pf-c-button--m-control--after--BorderBottomColor)"
  };
  const c_button_m_control_after_BorderLeftColor = exports.c_button_m_control_after_BorderLeftColor = {
    "name": "--pf-c-button--m-control--after--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-button--m-control--after--BorderLeftColor)"
  };
  const c_button_m_control_hover_after_BorderBottomWidth = exports.c_button_m_control_hover_after_BorderBottomWidth = {
    "name": "--pf-c-button--m-control--hover--after--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-button--m-control--hover--after--BorderBottomWidth)"
  };
  const c_button_m_control_hover_after_BorderBottomColor = exports.c_button_m_control_hover_after_BorderBottomColor = {
    "name": "--pf-c-button--m-control--hover--after--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-button--m-control--hover--after--BorderBottomColor)"
  };
  const c_button_m_control_active_after_BorderBottomWidth = exports.c_button_m_control_active_after_BorderBottomWidth = {
    "name": "--pf-c-button--m-control--active--after--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-button--m-control--active--after--BorderBottomWidth)"
  };
  const c_button_m_control_active_after_BorderBottomColor = exports.c_button_m_control_active_after_BorderBottomColor = {
    "name": "--pf-c-button--m-control--active--after--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-button--m-control--active--after--BorderBottomColor)"
  };
  const c_button_m_control_focus_after_BorderBottomWidth = exports.c_button_m_control_focus_after_BorderBottomWidth = {
    "name": "--pf-c-button--m-control--focus--after--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-button--m-control--focus--after--BorderBottomWidth)"
  };
  const c_button_m_control_focus_after_BorderBottomColor = exports.c_button_m_control_focus_after_BorderBottomColor = {
    "name": "--pf-c-button--m-control--focus--after--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-button--m-control--focus--after--BorderBottomColor)"
  };
  const c_button_m_control_m_expanded_after_BorderBottomWidth = exports.c_button_m_control_m_expanded_after_BorderBottomWidth = {
    "name": "--pf-c-button--m-control--m-expanded--after--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-button--m-control--m-expanded--after--BorderBottomWidth)"
  };
  const c_button_m_control_m_expanded_after_BorderBottomColor = exports.c_button_m_control_m_expanded_after_BorderBottomColor = {
    "name": "--pf-c-button--m-control--m-expanded--after--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-button--m-control--m-expanded--after--BorderBottomColor)"
  };
  const c_button_m_control_disabled_after_BorderBottomColor = exports.c_button_m_control_disabled_after_BorderBottomColor = {
    "name": "--pf-c-button--m-control--disabled--after--BorderBottomColor",
    "value": "#ededed",
    "var": "var(--pf-c-button--m-control--disabled--after--BorderBottomColor)"
  };
  const c_button_m_control_disabled_BackgroundColor = exports.c_button_m_control_disabled_BackgroundColor = {
    "name": "--pf-c-button--m-control--disabled--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-button--m-control--disabled--BackgroundColor)"
  };
  const c_button__icon_MarginRight = exports.c_button__icon_MarginRight = {
    "name": "--pf-c-button__icon--MarginRight",
    "value": "0.25rem",
    "var": "var(--pf-c-button__icon--MarginRight)"
  };
  const c_button__text_icon_MarginLeft = exports.c_button__text_icon_MarginLeft = {
    "name": "--pf-c-button__text--icon--MarginLeft",
    "value": "0.25rem",
    "var": "var(--pf-c-button__text--icon--MarginLeft)"
  };
  const c_card_BoxShadow = exports.c_card_BoxShadow = {
    "name": "--pf-c-card--BoxShadow",
    "value": "0 0.0625rem 0.125rem 0 rgba(3,3,3,0.2)",
    "var": "var(--pf-c-card--BoxShadow)"
  };
  const c_card_m_hoverable_hover_BoxShadow = exports.c_card_m_hoverable_hover_BoxShadow = {
    "name": "--pf-c-card--m-hoverable--hover--BoxShadow",
    "value": "0 0.1875rem 0.4375rem 0.1875rem rgba(3,3,3,0.13),0 0.6875rem 1.5rem 1rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-card--m-hoverable--hover--BoxShadow)"
  };
  const c_card_m_selectable_hover_BoxShadow = exports.c_card_m_selectable_hover_BoxShadow = {
    "name": "--pf-c-card--m-selectable--hover--BoxShadow",
    "value": "0 0.1875rem 0.4375rem 0.1875rem rgba(3,3,3,0.13),0 0.6875rem 1.5rem 1rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-card--m-selectable--hover--BoxShadow)"
  };
  const c_card_m_selectable_focus_BoxShadow = exports.c_card_m_selectable_focus_BoxShadow = {
    "name": "--pf-c-card--m-selectable--focus--BoxShadow",
    "value": "0 0.1875rem 0.4375rem 0.1875rem rgba(3,3,3,0.13),0 0.6875rem 1.5rem 1rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-card--m-selectable--focus--BoxShadow)"
  };
  const c_card_m_selectable_active_BoxShadow = exports.c_card_m_selectable_active_BoxShadow = {
    "name": "--pf-c-card--m-selectable--active--BoxShadow",
    "value": "0 0.1875rem 0.4375rem 0.1875rem rgba(3,3,3,0.13),0 0.6875rem 1.5rem 1rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-card--m-selectable--active--BoxShadow)"
  };
  const c_card_m_selectable_m_selected_BoxShadow = exports.c_card_m_selectable_m_selected_BoxShadow = {
    "name": "--pf-c-card--m-selectable--m-selected--BoxShadow",
    "value": "0 0.1875rem 0.4375rem 0.1875rem rgba(3,3,3,0.13),0 0.6875rem 1.5rem 1rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-card--m-selectable--m-selected--BoxShadow)"
  };
  const c_card_m_selectable_m_selected_before_Height = exports.c_card_m_selectable_m_selected_before_Height = {
    "name": "--pf-c-card--m-selectable--m-selected--before--Height",
    "value": "3px",
    "var": "var(--pf-c-card--m-selectable--m-selected--before--Height)"
  };
  const c_card_m_selectable_m_selected_before_BackgroundColor = exports.c_card_m_selectable_m_selected_before_BackgroundColor = {
    "name": "--pf-c-card--m-selectable--m-selected--before--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-card--m-selectable--m-selected--before--BackgroundColor)"
  };
  const c_card_m_compact__body_FontSize = exports.c_card_m_compact__body_FontSize = {
    "name": "--pf-c-card--m-compact__body--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-card--m-compact__body--FontSize)"
  };
  const c_card_m_compact__footer_FontSize = exports.c_card_m_compact__footer_FontSize = {
    "name": "--pf-c-card--m-compact__footer--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-card--m-compact__footer--FontSize)"
  };
  const c_card_m_compact_first_child_PaddingTop = exports.c_card_m_compact_first_child_PaddingTop = {
    "name": "--pf-c-card--m-compact--first-child--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-card--m-compact--first-child--PaddingTop)"
  };
  const c_card_m_compact_child_PaddingRight = exports.c_card_m_compact_child_PaddingRight = {
    "name": "--pf-c-card--m-compact--child--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-card--m-compact--child--PaddingRight)"
  };
  const c_card_m_compact_child_PaddingBottom = exports.c_card_m_compact_child_PaddingBottom = {
    "name": "--pf-c-card--m-compact--child--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-card--m-compact--child--PaddingBottom)"
  };
  const c_card_m_compact_child_PaddingLeft = exports.c_card_m_compact_child_PaddingLeft = {
    "name": "--pf-c-card--m-compact--child--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-card--m-compact--child--PaddingLeft)"
  };
  const c_card_m_compact__header_not_last_child_PaddingBottom = exports.c_card_m_compact__header_not_last_child_PaddingBottom = {
    "name": "--pf-c-card--m-compact__header--not-last-child--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-card--m-compact__header--not-last-child--PaddingBottom)"
  };
  const c_card_first_child_PaddingTop = exports.c_card_first_child_PaddingTop = {
    "name": "--pf-c-card--first-child--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-card--first-child--PaddingTop)"
  };
  const c_card_child_PaddingRight = exports.c_card_child_PaddingRight = {
    "name": "--pf-c-card--child--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-card--child--PaddingRight)"
  };
  const c_card_child_PaddingBottom = exports.c_card_child_PaddingBottom = {
    "name": "--pf-c-card--child--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-card--child--PaddingBottom)"
  };
  const c_card_child_PaddingLeft = exports.c_card_child_PaddingLeft = {
    "name": "--pf-c-card--child--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-card--child--PaddingLeft)"
  };
  const c_card__header_not_last_child_PaddingBottom = exports.c_card__header_not_last_child_PaddingBottom = {
    "name": "--pf-c-card__header--not-last-child--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-card__header--not-last-child--PaddingBottom)"
  };
  const c_card__body_FontSize = exports.c_card__body_FontSize = {
    "name": "--pf-c-card__body--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-card__body--FontSize)"
  };
  const c_card__footer_FontSize = exports.c_card__footer_FontSize = {
    "name": "--pf-c-card__footer--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-card__footer--FontSize)"
  };
  const c_card__actions_PaddingLeft = exports.c_card__actions_PaddingLeft = {
    "name": "--pf-c-card__actions--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-card__actions--PaddingLeft)"
  };
  const c_card__actions_child_MarginLeft = exports.c_card__actions_child_MarginLeft = {
    "name": "--pf-c-card__actions--child--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-card__actions--child--MarginLeft)"
  };
  const c_check_GridGap = exports.c_check_GridGap = {
    "name": "--pf-c-check--GridGap",
    "value": "0.25rem 0.5rem",
    "var": "var(--pf-c-check--GridGap)"
  };
  const c_check__label_disabled_Color = exports.c_check__label_disabled_Color = {
    "name": "--pf-c-check__label--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-check__label--disabled--Color)"
  };
  const c_check__label_Color = exports.c_check__label_Color = {
    "name": "--pf-c-check__label--Color",
    "value": "#737679",
    "var": "var(--pf-c-check__label--Color)"
  };
  const c_check__label_FontWeight = exports.c_check__label_FontWeight = {
    "name": "--pf-c-check__label--FontWeight",
    "value": "400",
    "var": "var(--pf-c-check__label--FontWeight)"
  };
  const c_check__label_FontSize = exports.c_check__label_FontSize = {
    "name": "--pf-c-check__label--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-check__label--FontSize)"
  };
  const c_check__label_LineHeight = exports.c_check__label_LineHeight = {
    "name": "--pf-c-check__label--LineHeight",
    "value": "1.3",
    "var": "var(--pf-c-check__label--LineHeight)"
  };
  const c_check__input_MarginTop = exports.c_check__input_MarginTop = {
    "name": "--pf-c-check__input--MarginTop",
    "value": "-0.1875rem",
    "var": "var(--pf-c-check__input--MarginTop)"
  };
  const c_check__description_FontSize = exports.c_check__description_FontSize = {
    "name": "--pf-c-check__description--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-check__description--FontSize)"
  };
  const c_check__description_Color = exports.c_check__description_Color = {
    "name": "--pf-c-check__description--Color",
    "value": "#737679",
    "var": "var(--pf-c-check__description--Color)"
  };
  const c_chip_PaddingLeft = exports.c_chip_PaddingLeft = {
    "name": "--pf-c-chip--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-chip--PaddingLeft)"
  };
  const c_chip_BackgroundColor = exports.c_chip_BackgroundColor = {
    "name": "--pf-c-chip--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-chip--BackgroundColor)"
  };
  const c_chip_BorderColor = exports.c_chip_BorderColor = {
    "name": "--pf-c-chip--BorderColor",
    "value": "#737679",
    "var": "var(--pf-c-chip--BorderColor)"
  };
  const c_chip_BorderWidth = exports.c_chip_BorderWidth = {
    "name": "--pf-c-chip--BorderWidth",
    "value": "0",
    "var": "var(--pf-c-chip--BorderWidth)"
  };
  const c_chip_BorderRadius = exports.c_chip_BorderRadius = {
    "name": "--pf-c-chip--BorderRadius",
    "value": "3px",
    "var": "var(--pf-c-chip--BorderRadius)"
  };
  const c_chip_m_overflow_BackgroundColor = exports.c_chip_m_overflow_BackgroundColor = {
    "name": "--pf-c-chip--m-overflow--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-chip--m-overflow--BackgroundColor)"
  };
  const c_chip_m_overflow_PaddingLeft = exports.c_chip_m_overflow_PaddingLeft = {
    "name": "--pf-c-chip--m-overflow--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-chip--m-overflow--PaddingLeft)"
  };
  const c_chip_m_overflow_BorderWidth = exports.c_chip_m_overflow_BorderWidth = {
    "name": "--pf-c-chip--m-overflow--BorderWidth",
    "value": "0",
    "var": "var(--pf-c-chip--m-overflow--BorderWidth)"
  };
  const c_chip_m_overflow_c_button_BorderRadius = exports.c_chip_m_overflow_c_button_BorderRadius = {
    "name": "--pf-c-chip--m-overflow--c-button--BorderRadius",
    "value": "3px",
    "var": "var(--pf-c-chip--m-overflow--c-button--BorderRadius)"
  };
  const c_chip_m_overflow_c_button_BorderWidth = exports.c_chip_m_overflow_c_button_BorderWidth = {
    "name": "--pf-c-chip--m-overflow--c-button--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-chip--m-overflow--c-button--BorderWidth)"
  };
  const c_chip_m_overflow_c_button_PaddingLeft = exports.c_chip_m_overflow_c_button_PaddingLeft = {
    "name": "--pf-c-chip--m-overflow--c-button--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-chip--m-overflow--c-button--PaddingLeft)"
  };
  const c_chip_m_overflow_c_button_PaddingRight = exports.c_chip_m_overflow_c_button_PaddingRight = {
    "name": "--pf-c-chip--m-overflow--c-button--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-chip--m-overflow--c-button--PaddingRight)"
  };
  const c_chip_m_overflow_c_button_hover_BorderWidth = exports.c_chip_m_overflow_c_button_hover_BorderWidth = {
    "name": "--pf-c-chip--m-overflow--c-button--hover--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-chip--m-overflow--c-button--hover--BorderWidth)"
  };
  const c_chip_m_overflow_c_button_hover_BorderColor = exports.c_chip_m_overflow_c_button_hover_BorderColor = {
    "name": "--pf-c-chip--m-overflow--c-button--hover--BorderColor",
    "value": "#737679",
    "var": "var(--pf-c-chip--m-overflow--c-button--hover--BorderColor)"
  };
  const c_chip_m_overflow_c_button_active_BorderWidth = exports.c_chip_m_overflow_c_button_active_BorderWidth = {
    "name": "--pf-c-chip--m-overflow--c-button--active--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-chip--m-overflow--c-button--active--BorderWidth)"
  };
  const c_chip_m_overflow_c_button_active_BorderColor = exports.c_chip_m_overflow_c_button_active_BorderColor = {
    "name": "--pf-c-chip--m-overflow--c-button--active--BorderColor",
    "value": "#737679",
    "var": "var(--pf-c-chip--m-overflow--c-button--active--BorderColor)"
  };
  const c_chip_m_overflow_c_button_focus_BorderWidth = exports.c_chip_m_overflow_c_button_focus_BorderWidth = {
    "name": "--pf-c-chip--m-overflow--c-button--focus--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-chip--m-overflow--c-button--focus--BorderWidth)"
  };
  const c_chip_m_overflow_c_button_focus_BorderColor = exports.c_chip_m_overflow_c_button_focus_BorderColor = {
    "name": "--pf-c-chip--m-overflow--c-button--focus--BorderColor",
    "value": "#737679",
    "var": "var(--pf-c-chip--m-overflow--c-button--focus--BorderColor)"
  };
  const c_chip_m_read_only_PaddingTop = exports.c_chip_m_read_only_PaddingTop = {
    "name": "--pf-c-chip--m-read-only--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-chip--m-read-only--PaddingTop)"
  };
  const c_chip_m_read_only_PaddingRight = exports.c_chip_m_read_only_PaddingRight = {
    "name": "--pf-c-chip--m-read-only--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-chip--m-read-only--PaddingRight)"
  };
  const c_chip_m_read_only_PaddingBottom = exports.c_chip_m_read_only_PaddingBottom = {
    "name": "--pf-c-chip--m-read-only--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-chip--m-read-only--PaddingBottom)"
  };
  const c_chip__text_FontSize = exports.c_chip__text_FontSize = {
    "name": "--pf-c-chip__text--FontSize",
    "value": "0.75rem",
    "var": "var(--pf-c-chip__text--FontSize)"
  };
  const c_chip__text_Color = exports.c_chip__text_Color = {
    "name": "--pf-c-chip__text--Color",
    "value": "#151515",
    "var": "var(--pf-c-chip__text--Color)"
  };
  const c_chip__text_MaxWidth = exports.c_chip__text_MaxWidth = {
    "name": "--pf-c-chip__text--MaxWidth",
    "value": "7.5rem",
    "var": "var(--pf-c-chip__text--MaxWidth)"
  };
  const c_chip_c_button_PaddingLeft = exports.c_chip_c_button_PaddingLeft = {
    "name": "--pf-c-chip--c-button--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-chip--c-button--PaddingLeft)"
  };
  const c_chip_c_button_PaddingRight = exports.c_chip_c_button_PaddingRight = {
    "name": "--pf-c-chip--c-button--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-chip--c-button--PaddingRight)"
  };
  const c_chip_c_button_FontSize = exports.c_chip_c_button_FontSize = {
    "name": "--pf-c-chip--c-button--FontSize",
    "value": "0.75rem",
    "var": "var(--pf-c-chip--c-button--FontSize)"
  };
  const c_chip_c_badge_MarginLeft = exports.c_chip_c_badge_MarginLeft = {
    "name": "--pf-c-chip--c-badge--MarginLeft",
    "value": "0.25rem",
    "var": "var(--pf-c-chip--c-badge--MarginLeft)"
  };
  const c_chip_m_overflow_c_button_BorderColor = exports.c_chip_m_overflow_c_button_BorderColor = {
    "name": "--pf-c-chip--m-overflow--c-button--BorderColor",
    "value": "#737679",
    "var": "var(--pf-c-chip--m-overflow--c-button--BorderColor)"
  };
  const c_chip_group_MarginRight = exports.c_chip_group_MarginRight = {
    "name": "--pf-c-chip-group--MarginRight",
    "value": "0",
    "var": "var(--pf-c-chip-group--MarginRight)"
  };
  const c_chip_group_MarginBottom = exports.c_chip_group_MarginBottom = {
    "name": "--pf-c-chip-group--MarginBottom",
    "value": "calc(0.25rem*-1)",
    "var": "var(--pf-c-chip-group--MarginBottom)"
  };
  const c_chip_group_m_toolbar_PaddingTop = exports.c_chip_group_m_toolbar_PaddingTop = {
    "name": "--pf-c-chip-group--m-toolbar-PaddingTop",
    "value": "0.25rem",
    "var": "var(--pf-c-chip-group--m-toolbar-PaddingTop)"
  };
  const c_chip_group_m_toolbar_PaddingRight = exports.c_chip_group_m_toolbar_PaddingRight = {
    "name": "--pf-c-chip-group--m-toolbar-PaddingRight",
    "value": "0.25rem",
    "var": "var(--pf-c-chip-group--m-toolbar-PaddingRight)"
  };
  const c_chip_group_m_toolbar_PaddingBottom = exports.c_chip_group_m_toolbar_PaddingBottom = {
    "name": "--pf-c-chip-group--m-toolbar-PaddingBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-chip-group--m-toolbar-PaddingBottom)"
  };
  const c_chip_group_m_toolbar_PaddingLeft = exports.c_chip_group_m_toolbar_PaddingLeft = {
    "name": "--pf-c-chip-group--m-toolbar-PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-chip-group--m-toolbar-PaddingLeft)"
  };
  const c_chip_group_m_toolbar_BorderRadius = exports.c_chip_group_m_toolbar_BorderRadius = {
    "name": "--pf-c-chip-group--m-toolbar-BorderRadius",
    "value": "3px",
    "var": "var(--pf-c-chip-group--m-toolbar-BorderRadius)"
  };
  const c_chip_group_m_toolbar_BackgroundColor = exports.c_chip_group_m_toolbar_BackgroundColor = {
    "name": "--pf-c-chip-group--m-toolbar--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-chip-group--m-toolbar--BackgroundColor)"
  };
  const c_chip_group__li_m_toolbar_MarginRight = exports.c_chip_group__li_m_toolbar_MarginRight = {
    "name": "--pf-c-chip-group__li--m-toolbar--MarginRight",
    "value": "0",
    "var": "var(--pf-c-chip-group__li--m-toolbar--MarginRight)"
  };
  const c_chip_group__label_PaddingTop = exports.c_chip_group__label_PaddingTop = {
    "name": "--pf-c-chip-group__label--PaddingTop",
    "value": "0.25rem",
    "var": "var(--pf-c-chip-group__label--PaddingTop)"
  };
  const c_chip_group__label_PaddingRight = exports.c_chip_group__label_PaddingRight = {
    "name": "--pf-c-chip-group__label--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-chip-group__label--PaddingRight)"
  };
  const c_chip_group__label_PaddingBottom = exports.c_chip_group__label_PaddingBottom = {
    "name": "--pf-c-chip-group__label--PaddingBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-chip-group__label--PaddingBottom)"
  };
  const c_chip_group__label_PaddingLeft = exports.c_chip_group__label_PaddingLeft = {
    "name": "--pf-c-chip-group__label--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-chip-group__label--PaddingLeft)"
  };
  const c_chip_group__label_FontSize = exports.c_chip_group__label_FontSize = {
    "name": "--pf-c-chip-group__label--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-chip-group__label--FontSize)"
  };
  const c_chip_group__label_Maxwidth = exports.c_chip_group__label_Maxwidth = {
    "name": "--pf-c-chip-group__label--Maxwidth",
    "value": "7.5rem",
    "var": "var(--pf-c-chip-group__label--Maxwidth)"
  };
  const c_chip_group_c_chip_MarginRight = exports.c_chip_group_c_chip_MarginRight = {
    "name": "--pf-c-chip-group--c-chip--MarginRight",
    "value": "0",
    "var": "var(--pf-c-chip-group--c-chip--MarginRight)"
  };
  const c_chip_group_c_chip_MarginBottom = exports.c_chip_group_c_chip_MarginBottom = {
    "name": "--pf-c-chip-group--c-chip--MarginBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-chip-group--c-chip--MarginBottom)"
  };
  const c_clipboard_copy__group_toggle_PaddingRight = exports.c_clipboard_copy__group_toggle_PaddingRight = {
    "name": "--pf-c-clipboard-copy__group-toggle--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-clipboard-copy__group-toggle--PaddingRight)"
  };
  const c_clipboard_copy__group_toggle_PaddingLeft = exports.c_clipboard_copy__group_toggle_PaddingLeft = {
    "name": "--pf-c-clipboard-copy__group-toggle--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-clipboard-copy__group-toggle--PaddingLeft)"
  };
  const c_clipboard_copy__group_toggle_BorderWidth = exports.c_clipboard_copy__group_toggle_BorderWidth = {
    "name": "--pf-c-clipboard-copy__group-toggle--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-clipboard-copy__group-toggle--BorderWidth)"
  };
  const c_clipboard_copy__group_toggle_BorderTopColor = exports.c_clipboard_copy__group_toggle_BorderTopColor = {
    "name": "--pf-c-clipboard-copy__group-toggle--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-clipboard-copy__group-toggle--BorderTopColor)"
  };
  const c_clipboard_copy__group_toggle_BorderRightColor = exports.c_clipboard_copy__group_toggle_BorderRightColor = {
    "name": "--pf-c-clipboard-copy__group-toggle--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-clipboard-copy__group-toggle--BorderRightColor)"
  };
  const c_clipboard_copy__group_toggle_BorderBottomColor = exports.c_clipboard_copy__group_toggle_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-toggle--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-toggle--BorderBottomColor)"
  };
  const c_clipboard_copy__group_toggle_BorderLeftColor = exports.c_clipboard_copy__group_toggle_BorderLeftColor = {
    "name": "--pf-c-clipboard-copy__group-toggle--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-clipboard-copy__group-toggle--BorderLeftColor)"
  };
  const c_clipboard_copy__group_toggle_hover_BorderBottomColor = exports.c_clipboard_copy__group_toggle_hover_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-toggle--hover--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-toggle--hover--BorderBottomColor)"
  };
  const c_clipboard_copy__group_toggle_active_BorderBottomWidth = exports.c_clipboard_copy__group_toggle_active_BorderBottomWidth = {
    "name": "--pf-c-clipboard-copy__group-toggle--active--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-clipboard-copy__group-toggle--active--BorderBottomWidth)"
  };
  const c_clipboard_copy__group_toggle_active_BorderBottomColor = exports.c_clipboard_copy__group_toggle_active_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-toggle--active--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-toggle--active--BorderBottomColor)"
  };
  const c_clipboard_copy__group_toggle_focus_BorderBottomWidth = exports.c_clipboard_copy__group_toggle_focus_BorderBottomWidth = {
    "name": "--pf-c-clipboard-copy__group-toggle--focus--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-clipboard-copy__group-toggle--focus--BorderBottomWidth)"
  };
  const c_clipboard_copy__group_toggle_focus_BorderBottomColor = exports.c_clipboard_copy__group_toggle_focus_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-toggle--focus--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-toggle--focus--BorderBottomColor)"
  };
  const c_clipboard_copy__group_toggle_m_expanded_BorderBottomWidth = exports.c_clipboard_copy__group_toggle_m_expanded_BorderBottomWidth = {
    "name": "--pf-c-clipboard-copy__group-toggle--m-expanded--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-clipboard-copy__group-toggle--m-expanded--BorderBottomWidth)"
  };
  const c_clipboard_copy__group_toggle_m_expanded_BorderBottomColor = exports.c_clipboard_copy__group_toggle_m_expanded_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-toggle--m-expanded--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-toggle--m-expanded--BorderBottomColor)"
  };
  const c_clipboard_copy__group_toggle_OutlineOffset = exports.c_clipboard_copy__group_toggle_OutlineOffset = {
    "name": "--pf-c-clipboard-copy__group-toggle--OutlineOffset",
    "value": "calc(-1*0.25rem)",
    "var": "var(--pf-c-clipboard-copy__group-toggle--OutlineOffset)"
  };
  const c_clipboard_copy__group_toggle_icon_Transition = exports.c_clipboard_copy__group_toggle_icon_Transition = {
    "name": "--pf-c-clipboard-copy__group-toggle-icon--Transition",
    "value": ".2s ease-in 0s",
    "var": "var(--pf-c-clipboard-copy__group-toggle-icon--Transition)"
  };
  const c_clipboard_copy_m_expanded__group_toggle_icon_Transform = exports.c_clipboard_copy_m_expanded__group_toggle_icon_Transform = {
    "name": "--pf-c-clipboard-copy--m-expanded__group-toggle-icon--Transform",
    "value": "rotate(90deg)",
    "var": "var(--pf-c-clipboard-copy--m-expanded__group-toggle-icon--Transform)"
  };
  const c_clipboard_copy__group_copy_PaddingRight = exports.c_clipboard_copy__group_copy_PaddingRight = {
    "name": "--pf-c-clipboard-copy__group-copy--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-clipboard-copy__group-copy--PaddingRight)"
  };
  const c_clipboard_copy__group_copy_PaddingLeft = exports.c_clipboard_copy__group_copy_PaddingLeft = {
    "name": "--pf-c-clipboard-copy__group-copy--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-clipboard-copy__group-copy--PaddingLeft)"
  };
  const c_clipboard_copy__group_copy_BorderWidth = exports.c_clipboard_copy__group_copy_BorderWidth = {
    "name": "--pf-c-clipboard-copy__group-copy--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-clipboard-copy__group-copy--BorderWidth)"
  };
  const c_clipboard_copy__group_copy_BorderTopColor = exports.c_clipboard_copy__group_copy_BorderTopColor = {
    "name": "--pf-c-clipboard-copy__group-copy--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-clipboard-copy__group-copy--BorderTopColor)"
  };
  const c_clipboard_copy__group_copy_BorderRightColor = exports.c_clipboard_copy__group_copy_BorderRightColor = {
    "name": "--pf-c-clipboard-copy__group-copy--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-clipboard-copy__group-copy--BorderRightColor)"
  };
  const c_clipboard_copy__group_copy_BorderBottomColor = exports.c_clipboard_copy__group_copy_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-copy--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-copy--BorderBottomColor)"
  };
  const c_clipboard_copy__group_copy_BorderLeftColor = exports.c_clipboard_copy__group_copy_BorderLeftColor = {
    "name": "--pf-c-clipboard-copy__group-copy--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-clipboard-copy__group-copy--BorderLeftColor)"
  };
  const c_clipboard_copy__group_copy_hover_BorderBottomColor = exports.c_clipboard_copy__group_copy_hover_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-copy--hover--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-copy--hover--BorderBottomColor)"
  };
  const c_clipboard_copy__group_copy_active_BorderBottomWidth = exports.c_clipboard_copy__group_copy_active_BorderBottomWidth = {
    "name": "--pf-c-clipboard-copy__group-copy--active--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-clipboard-copy__group-copy--active--BorderBottomWidth)"
  };
  const c_clipboard_copy__group_copy_active_BorderBottomColor = exports.c_clipboard_copy__group_copy_active_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-copy--active--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-copy--active--BorderBottomColor)"
  };
  const c_clipboard_copy__group_copy_focus_BorderBottomWidth = exports.c_clipboard_copy__group_copy_focus_BorderBottomWidth = {
    "name": "--pf-c-clipboard-copy__group-copy--focus--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-clipboard-copy__group-copy--focus--BorderBottomWidth)"
  };
  const c_clipboard_copy__group_copy_focus_BorderBottomColor = exports.c_clipboard_copy__group_copy_focus_BorderBottomColor = {
    "name": "--pf-c-clipboard-copy__group-copy--focus--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-clipboard-copy__group-copy--focus--BorderBottomColor)"
  };
  const c_clipboard_copy__expandable_content_PaddingTop = exports.c_clipboard_copy__expandable_content_PaddingTop = {
    "name": "--pf-c-clipboard-copy__expandable-content--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-clipboard-copy__expandable-content--PaddingTop)"
  };
  const c_clipboard_copy__expandable_content_PaddingRight = exports.c_clipboard_copy__expandable_content_PaddingRight = {
    "name": "--pf-c-clipboard-copy__expandable-content--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-clipboard-copy__expandable-content--PaddingRight)"
  };
  const c_clipboard_copy__expandable_content_PaddingBottom = exports.c_clipboard_copy__expandable_content_PaddingBottom = {
    "name": "--pf-c-clipboard-copy__expandable-content--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-clipboard-copy__expandable-content--PaddingBottom)"
  };
  const c_clipboard_copy__expandable_content_PaddingLeft = exports.c_clipboard_copy__expandable_content_PaddingLeft = {
    "name": "--pf-c-clipboard-copy__expandable-content--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-clipboard-copy__expandable-content--PaddingLeft)"
  };
  const c_clipboard_copy__expandable_content_BackgroundColor = exports.c_clipboard_copy__expandable_content_BackgroundColor = {
    "name": "--pf-c-clipboard-copy__expandable-content--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-clipboard-copy__expandable-content--BackgroundColor)"
  };
  const c_clipboard_copy__expandable_content_BorderWidth = exports.c_clipboard_copy__expandable_content_BorderWidth = {
    "name": "--pf-c-clipboard-copy__expandable-content--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-clipboard-copy__expandable-content--BorderWidth)"
  };
  const c_clipboard_copy__expandable_content_BoxShadow = exports.c_clipboard_copy__expandable_content_BoxShadow = {
    "name": "--pf-c-clipboard-copy__expandable-content--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-clipboard-copy__expandable-content--BoxShadow)"
  };
  const c_clipboard_copy__expandable_content_OutlineOffset = exports.c_clipboard_copy__expandable_content_OutlineOffset = {
    "name": "--pf-c-clipboard-copy__expandable-content--OutlineOffset",
    "value": "calc(-1*0.25rem)",
    "var": "var(--pf-c-clipboard-copy__expandable-content--OutlineOffset)"
  };
  const c_content_MarginBottom = exports.c_content_MarginBottom = {
    "name": "--pf-c-content--MarginBottom",
    "value": "1rem",
    "var": "var(--pf-c-content--MarginBottom)"
  };
  const c_content_LineHeight = exports.c_content_LineHeight = {
    "name": "--pf-c-content--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-content--LineHeight)"
  };
  const c_content_FontSize = exports.c_content_FontSize = {
    "name": "--pf-c-content--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-content--FontSize)"
  };
  const c_content_FontWeight = exports.c_content_FontWeight = {
    "name": "--pf-c-content--FontWeight",
    "value": "400",
    "var": "var(--pf-c-content--FontWeight)"
  };
  const c_content_Color = exports.c_content_Color = {
    "name": "--pf-c-content--Color",
    "value": "#151515",
    "var": "var(--pf-c-content--Color)"
  };
  const c_content_heading_FontFamily = exports.c_content_heading_FontFamily = {
    "name": "--pf-c-content--heading--FontFamily",
    "value": "RedHatDisplay,Overpass,overpass,helvetica,arial,sans-serif",
    "var": "var(--pf-c-content--heading--FontFamily)"
  };
  const c_content_h1_MarginTop = exports.c_content_h1_MarginTop = {
    "name": "--pf-c-content--h1--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-content--h1--MarginTop)"
  };
  const c_content_h1_MarginBottom = exports.c_content_h1_MarginBottom = {
    "name": "--pf-c-content--h1--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-content--h1--MarginBottom)"
  };
  const c_content_h1_LineHeight = exports.c_content_h1_LineHeight = {
    "name": "--pf-c-content--h1--LineHeight",
    "value": "1.3",
    "var": "var(--pf-c-content--h1--LineHeight)"
  };
  const c_content_h1_FontSize = exports.c_content_h1_FontSize = {
    "name": "--pf-c-content--h1--FontSize",
    "value": "1.5rem",
    "var": "var(--pf-c-content--h1--FontSize)"
  };
  const c_content_h1_FontWeight = exports.c_content_h1_FontWeight = {
    "name": "--pf-c-content--h1--FontWeight",
    "value": "400",
    "var": "var(--pf-c-content--h1--FontWeight)"
  };
  const c_content_h2_MarginTop = exports.c_content_h2_MarginTop = {
    "name": "--pf-c-content--h2--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-content--h2--MarginTop)"
  };
  const c_content_h2_MarginBottom = exports.c_content_h2_MarginBottom = {
    "name": "--pf-c-content--h2--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-content--h2--MarginBottom)"
  };
  const c_content_h2_LineHeight = exports.c_content_h2_LineHeight = {
    "name": "--pf-c-content--h2--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-content--h2--LineHeight)"
  };
  const c_content_h2_FontSize = exports.c_content_h2_FontSize = {
    "name": "--pf-c-content--h2--FontSize",
    "value": "1.25rem",
    "var": "var(--pf-c-content--h2--FontSize)"
  };
  const c_content_h2_FontWeight = exports.c_content_h2_FontWeight = {
    "name": "--pf-c-content--h2--FontWeight",
    "value": "400",
    "var": "var(--pf-c-content--h2--FontWeight)"
  };
  const c_content_h3_MarginTop = exports.c_content_h3_MarginTop = {
    "name": "--pf-c-content--h3--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-content--h3--MarginTop)"
  };
  const c_content_h3_MarginBottom = exports.c_content_h3_MarginBottom = {
    "name": "--pf-c-content--h3--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-content--h3--MarginBottom)"
  };
  const c_content_h3_LineHeight = exports.c_content_h3_LineHeight = {
    "name": "--pf-c-content--h3--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-content--h3--LineHeight)"
  };
  const c_content_h3_FontSize = exports.c_content_h3_FontSize = {
    "name": "--pf-c-content--h3--FontSize",
    "value": "1.125rem",
    "var": "var(--pf-c-content--h3--FontSize)"
  };
  const c_content_h3_FontWeight = exports.c_content_h3_FontWeight = {
    "name": "--pf-c-content--h3--FontWeight",
    "value": "400",
    "var": "var(--pf-c-content--h3--FontWeight)"
  };
  const c_content_h4_MarginTop = exports.c_content_h4_MarginTop = {
    "name": "--pf-c-content--h4--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-content--h4--MarginTop)"
  };
  const c_content_h4_MarginBottom = exports.c_content_h4_MarginBottom = {
    "name": "--pf-c-content--h4--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-content--h4--MarginBottom)"
  };
  const c_content_h4_LineHeight = exports.c_content_h4_LineHeight = {
    "name": "--pf-c-content--h4--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-content--h4--LineHeight)"
  };
  const c_content_h4_FontSize = exports.c_content_h4_FontSize = {
    "name": "--pf-c-content--h4--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-content--h4--FontSize)"
  };
  const c_content_h4_FontWeight = exports.c_content_h4_FontWeight = {
    "name": "--pf-c-content--h4--FontWeight",
    "value": "400",
    "var": "var(--pf-c-content--h4--FontWeight)"
  };
  const c_content_h5_MarginTop = exports.c_content_h5_MarginTop = {
    "name": "--pf-c-content--h5--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-content--h5--MarginTop)"
  };
  const c_content_h5_MarginBottom = exports.c_content_h5_MarginBottom = {
    "name": "--pf-c-content--h5--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-content--h5--MarginBottom)"
  };
  const c_content_h5_LineHeight = exports.c_content_h5_LineHeight = {
    "name": "--pf-c-content--h5--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-content--h5--LineHeight)"
  };
  const c_content_h5_FontSize = exports.c_content_h5_FontSize = {
    "name": "--pf-c-content--h5--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-content--h5--FontSize)"
  };
  const c_content_h5_FontWeight = exports.c_content_h5_FontWeight = {
    "name": "--pf-c-content--h5--FontWeight",
    "value": "400",
    "var": "var(--pf-c-content--h5--FontWeight)"
  };
  const c_content_h6_MarginTop = exports.c_content_h6_MarginTop = {
    "name": "--pf-c-content--h6--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-content--h6--MarginTop)"
  };
  const c_content_h6_MarginBottom = exports.c_content_h6_MarginBottom = {
    "name": "--pf-c-content--h6--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-content--h6--MarginBottom)"
  };
  const c_content_h6_LineHeight = exports.c_content_h6_LineHeight = {
    "name": "--pf-c-content--h6--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-content--h6--LineHeight)"
  };
  const c_content_h6_FontSize = exports.c_content_h6_FontSize = {
    "name": "--pf-c-content--h6--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-content--h6--FontSize)"
  };
  const c_content_h6_FontWeight = exports.c_content_h6_FontWeight = {
    "name": "--pf-c-content--h6--FontWeight",
    "value": "400",
    "var": "var(--pf-c-content--h6--FontWeight)"
  };
  const c_content_small_MarginBottom = exports.c_content_small_MarginBottom = {
    "name": "--pf-c-content--small--MarginBottom",
    "value": "1rem",
    "var": "var(--pf-c-content--small--MarginBottom)"
  };
  const c_content_small_LineHeight = exports.c_content_small_LineHeight = {
    "name": "--pf-c-content--small--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-content--small--LineHeight)"
  };
  const c_content_small_FontSize = exports.c_content_small_FontSize = {
    "name": "--pf-c-content--small--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-content--small--FontSize)"
  };
  const c_content_small_Color = exports.c_content_small_Color = {
    "name": "--pf-c-content--small--Color",
    "value": "#737679",
    "var": "var(--pf-c-content--small--Color)"
  };
  const c_content_small_FontWeight = exports.c_content_small_FontWeight = {
    "name": "--pf-c-content--small--FontWeight",
    "value": "700",
    "var": "var(--pf-c-content--small--FontWeight)"
  };
  const c_content_a_Color = exports.c_content_a_Color = {
    "name": "--pf-c-content--a--Color",
    "value": "#004080",
    "var": "var(--pf-c-content--a--Color)"
  };
  const c_content_a_TextDecoration = exports.c_content_a_TextDecoration = {
    "name": "--pf-c-content--a--TextDecoration",
    "value": "underline",
    "var": "var(--pf-c-content--a--TextDecoration)"
  };
  const c_content_a_hover_Color = exports.c_content_a_hover_Color = {
    "name": "--pf-c-content--a--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-content--a--hover--Color)"
  };
  const c_content_a_hover_TextDecoration = exports.c_content_a_hover_TextDecoration = {
    "name": "--pf-c-content--a--hover--TextDecoration",
    "value": "underline",
    "var": "var(--pf-c-content--a--hover--TextDecoration)"
  };
  const c_content_blockquote_PaddingTop = exports.c_content_blockquote_PaddingTop = {
    "name": "--pf-c-content--blockquote--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-content--blockquote--PaddingTop)"
  };
  const c_content_blockquote_PaddingRight = exports.c_content_blockquote_PaddingRight = {
    "name": "--pf-c-content--blockquote--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-content--blockquote--PaddingRight)"
  };
  const c_content_blockquote_PaddingBottom = exports.c_content_blockquote_PaddingBottom = {
    "name": "--pf-c-content--blockquote--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-content--blockquote--PaddingBottom)"
  };
  const c_content_blockquote_PaddingLeft = exports.c_content_blockquote_PaddingLeft = {
    "name": "--pf-c-content--blockquote--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-content--blockquote--PaddingLeft)"
  };
  const c_content_blockquote_FontWeight = exports.c_content_blockquote_FontWeight = {
    "name": "--pf-c-content--blockquote--FontWeight",
    "value": "400",
    "var": "var(--pf-c-content--blockquote--FontWeight)"
  };
  const c_content_blockquote_Color = exports.c_content_blockquote_Color = {
    "name": "--pf-c-content--blockquote--Color",
    "value": "#737679",
    "var": "var(--pf-c-content--blockquote--Color)"
  };
  const c_content_blockquote_BorderLeftColor = exports.c_content_blockquote_BorderLeftColor = {
    "name": "--pf-c-content--blockquote--BorderLeftColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-content--blockquote--BorderLeftColor)"
  };
  const c_content_blockquote_BorderLeftWidth = exports.c_content_blockquote_BorderLeftWidth = {
    "name": "--pf-c-content--blockquote--BorderLeftWidth",
    "value": "3px",
    "var": "var(--pf-c-content--blockquote--BorderLeftWidth)"
  };
  const c_content_ol_PaddingLeft = exports.c_content_ol_PaddingLeft = {
    "name": "--pf-c-content--ol--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-content--ol--PaddingLeft)"
  };
  const c_content_ol_MarginTop = exports.c_content_ol_MarginTop = {
    "name": "--pf-c-content--ol--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-content--ol--MarginTop)"
  };
  const c_content_ol_MarginLeft = exports.c_content_ol_MarginLeft = {
    "name": "--pf-c-content--ol--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-content--ol--MarginLeft)"
  };
  const c_content_ol_nested_MarginTop = exports.c_content_ol_nested_MarginTop = {
    "name": "--pf-c-content--ol--nested--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-content--ol--nested--MarginTop)"
  };
  const c_content_ol_nested_MarginLeft = exports.c_content_ol_nested_MarginLeft = {
    "name": "--pf-c-content--ol--nested--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-content--ol--nested--MarginLeft)"
  };
  const c_content_ul_PaddingLeft = exports.c_content_ul_PaddingLeft = {
    "name": "--pf-c-content--ul--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-content--ul--PaddingLeft)"
  };
  const c_content_ul_MarginTop = exports.c_content_ul_MarginTop = {
    "name": "--pf-c-content--ul--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-content--ul--MarginTop)"
  };
  const c_content_ul_MarginLeft = exports.c_content_ul_MarginLeft = {
    "name": "--pf-c-content--ul--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-content--ul--MarginLeft)"
  };
  const c_content_ul_nested_MarginTop = exports.c_content_ul_nested_MarginTop = {
    "name": "--pf-c-content--ul--nested--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-content--ul--nested--MarginTop)"
  };
  const c_content_ul_nested_MarginLeft = exports.c_content_ul_nested_MarginLeft = {
    "name": "--pf-c-content--ul--nested--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-content--ul--nested--MarginLeft)"
  };
  const c_content_ul_ListStyle = exports.c_content_ul_ListStyle = {
    "name": "--pf-c-content--ul--ListStyle",
    "value": "disc outside",
    "var": "var(--pf-c-content--ul--ListStyle)"
  };
  const c_content_li_MarginTop = exports.c_content_li_MarginTop = {
    "name": "--pf-c-content--li--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-content--li--MarginTop)"
  };
  const c_content_dl_ColumnGap = exports.c_content_dl_ColumnGap = {
    "name": "--pf-c-content--dl--ColumnGap",
    "value": "3rem",
    "var": "var(--pf-c-content--dl--ColumnGap)"
  };
  const c_content_dl_RowGap = exports.c_content_dl_RowGap = {
    "name": "--pf-c-content--dl--RowGap",
    "value": "1rem",
    "var": "var(--pf-c-content--dl--RowGap)"
  };
  const c_content_dt_FontWeight = exports.c_content_dt_FontWeight = {
    "name": "--pf-c-content--dt--FontWeight",
    "value": "700",
    "var": "var(--pf-c-content--dt--FontWeight)"
  };
  const c_content_dt_MarginTop = exports.c_content_dt_MarginTop = {
    "name": "--pf-c-content--dt--MarginTop",
    "value": "1rem",
    "var": "var(--pf-c-content--dt--MarginTop)"
  };
  const c_content_dt_sm_MarginTop = exports.c_content_dt_sm_MarginTop = {
    "name": "--pf-c-content--dt--sm--MarginTop",
    "value": "0",
    "var": "var(--pf-c-content--dt--sm--MarginTop)"
  };
  const c_content_hr_Height = exports.c_content_hr_Height = {
    "name": "--pf-c-content--hr--Height",
    "value": "1px",
    "var": "var(--pf-c-content--hr--Height)"
  };
  const c_content_hr_BackgroundColor = exports.c_content_hr_BackgroundColor = {
    "name": "--pf-c-content--hr--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-content--hr--BackgroundColor)"
  };
  const c_context_selector_Width = exports.c_context_selector_Width = {
    "name": "--pf-c-context-selector--Width",
    "value": "15.625rem",
    "var": "var(--pf-c-context-selector--Width)"
  };
  const c_context_selector__toggle_PaddingTop = exports.c_context_selector__toggle_PaddingTop = {
    "name": "--pf-c-context-selector__toggle--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-context-selector__toggle--PaddingTop)"
  };
  const c_context_selector__toggle_PaddingRight = exports.c_context_selector__toggle_PaddingRight = {
    "name": "--pf-c-context-selector__toggle--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-context-selector__toggle--PaddingRight)"
  };
  const c_context_selector__toggle_PaddingBottom = exports.c_context_selector__toggle_PaddingBottom = {
    "name": "--pf-c-context-selector__toggle--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-context-selector__toggle--PaddingBottom)"
  };
  const c_context_selector__toggle_PaddingLeft = exports.c_context_selector__toggle_PaddingLeft = {
    "name": "--pf-c-context-selector__toggle--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-context-selector__toggle--PaddingLeft)"
  };
  const c_context_selector__toggle_BorderWidth = exports.c_context_selector__toggle_BorderWidth = {
    "name": "--pf-c-context-selector__toggle--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-context-selector__toggle--BorderWidth)"
  };
  const c_context_selector__toggle_BorderTopColor = exports.c_context_selector__toggle_BorderTopColor = {
    "name": "--pf-c-context-selector__toggle--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-context-selector__toggle--BorderTopColor)"
  };
  const c_context_selector__toggle_BorderRightColor = exports.c_context_selector__toggle_BorderRightColor = {
    "name": "--pf-c-context-selector__toggle--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-context-selector__toggle--BorderRightColor)"
  };
  const c_context_selector__toggle_BorderBottomColor = exports.c_context_selector__toggle_BorderBottomColor = {
    "name": "--pf-c-context-selector__toggle--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-context-selector__toggle--BorderBottomColor)"
  };
  const c_context_selector__toggle_BorderLeftColor = exports.c_context_selector__toggle_BorderLeftColor = {
    "name": "--pf-c-context-selector__toggle--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-context-selector__toggle--BorderLeftColor)"
  };
  const c_context_selector__toggle_Color = exports.c_context_selector__toggle_Color = {
    "name": "--pf-c-context-selector__toggle--Color",
    "value": "#151515",
    "var": "var(--pf-c-context-selector__toggle--Color)"
  };
  const c_context_selector__toggle_hover_BorderBottomColor = exports.c_context_selector__toggle_hover_BorderBottomColor = {
    "name": "--pf-c-context-selector__toggle--hover--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-context-selector__toggle--hover--BorderBottomColor)"
  };
  const c_context_selector__toggle_active_BorderBottomWidth = exports.c_context_selector__toggle_active_BorderBottomWidth = {
    "name": "--pf-c-context-selector__toggle--active--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-context-selector__toggle--active--BorderBottomWidth)"
  };
  const c_context_selector__toggle_active_BorderBottomColor = exports.c_context_selector__toggle_active_BorderBottomColor = {
    "name": "--pf-c-context-selector__toggle--active--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-context-selector__toggle--active--BorderBottomColor)"
  };
  const c_context_selector__toggle_expanded_BorderBottomWidth = exports.c_context_selector__toggle_expanded_BorderBottomWidth = {
    "name": "--pf-c-context-selector__toggle--expanded--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-context-selector__toggle--expanded--BorderBottomWidth)"
  };
  const c_context_selector__toggle_expanded_BorderBottomColor = exports.c_context_selector__toggle_expanded_BorderBottomColor = {
    "name": "--pf-c-context-selector__toggle--expanded--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-context-selector__toggle--expanded--BorderBottomColor)"
  };
  const c_context_selector__toggle_text_FontSize = exports.c_context_selector__toggle_text_FontSize = {
    "name": "--pf-c-context-selector__toggle-text--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-context-selector__toggle-text--FontSize)"
  };
  const c_context_selector__toggle_text_FontWeight = exports.c_context_selector__toggle_text_FontWeight = {
    "name": "--pf-c-context-selector__toggle-text--FontWeight",
    "value": "400",
    "var": "var(--pf-c-context-selector__toggle-text--FontWeight)"
  };
  const c_context_selector__toggle_text_LineHeight = exports.c_context_selector__toggle_text_LineHeight = {
    "name": "--pf-c-context-selector__toggle-text--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-context-selector__toggle-text--LineHeight)"
  };
  const c_context_selector__toggle_icon_MarginRight = exports.c_context_selector__toggle_icon_MarginRight = {
    "name": "--pf-c-context-selector__toggle-icon--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-context-selector__toggle-icon--MarginRight)"
  };
  const c_context_selector__toggle_icon_MarginLeft = exports.c_context_selector__toggle_icon_MarginLeft = {
    "name": "--pf-c-context-selector__toggle-icon--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-context-selector__toggle-icon--MarginLeft)"
  };
  const c_context_selector__menu_Top = exports.c_context_selector__menu_Top = {
    "name": "--pf-c-context-selector__menu--Top",
    "value": "calc(100% + 0.25rem)",
    "var": "var(--pf-c-context-selector__menu--Top)"
  };
  const c_context_selector__menu_ZIndex = exports.c_context_selector__menu_ZIndex = {
    "name": "--pf-c-context-selector__menu--ZIndex",
    "value": "200",
    "var": "var(--pf-c-context-selector__menu--ZIndex)"
  };
  const c_context_selector__menu_PaddingTop = exports.c_context_selector__menu_PaddingTop = {
    "name": "--pf-c-context-selector__menu--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-context-selector__menu--PaddingTop)"
  };
  const c_context_selector__menu_BackgroundColor = exports.c_context_selector__menu_BackgroundColor = {
    "name": "--pf-c-context-selector__menu--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-context-selector__menu--BackgroundColor)"
  };
  const c_context_selector__menu_BorderWidth = exports.c_context_selector__menu_BorderWidth = {
    "name": "--pf-c-context-selector__menu--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-context-selector__menu--BorderWidth)"
  };
  const c_context_selector__menu_BoxShadow = exports.c_context_selector__menu_BoxShadow = {
    "name": "--pf-c-context-selector__menu--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-context-selector__menu--BoxShadow)"
  };
  const c_context_selector__menu_input_PaddingTop = exports.c_context_selector__menu_input_PaddingTop = {
    "name": "--pf-c-context-selector__menu-input--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-context-selector__menu-input--PaddingTop)"
  };
  const c_context_selector__menu_input_PaddingRight = exports.c_context_selector__menu_input_PaddingRight = {
    "name": "--pf-c-context-selector__menu-input--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-context-selector__menu-input--PaddingRight)"
  };
  const c_context_selector__menu_input_PaddingBottom = exports.c_context_selector__menu_input_PaddingBottom = {
    "name": "--pf-c-context-selector__menu-input--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-context-selector__menu-input--PaddingBottom)"
  };
  const c_context_selector__menu_input_PaddingLeft = exports.c_context_selector__menu_input_PaddingLeft = {
    "name": "--pf-c-context-selector__menu-input--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-context-selector__menu-input--PaddingLeft)"
  };
  const c_context_selector__menu_input_BottomBorderColor = exports.c_context_selector__menu_input_BottomBorderColor = {
    "name": "--pf-c-context-selector__menu-input--BottomBorderColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-context-selector__menu-input--BottomBorderColor)"
  };
  const c_context_selector__menu_input_BottomBorderWidth = exports.c_context_selector__menu_input_BottomBorderWidth = {
    "name": "--pf-c-context-selector__menu-input--BottomBorderWidth",
    "value": "1px",
    "var": "var(--pf-c-context-selector__menu-input--BottomBorderWidth)"
  };
  const c_context_selector__menu_list_MaxHeight = exports.c_context_selector__menu_list_MaxHeight = {
    "name": "--pf-c-context-selector__menu-list--MaxHeight",
    "value": "12.5rem",
    "var": "var(--pf-c-context-selector__menu-list--MaxHeight)"
  };
  const c_context_selector__menu_list_item_PaddingTop = exports.c_context_selector__menu_list_item_PaddingTop = {
    "name": "--pf-c-context-selector__menu-list-item--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-context-selector__menu-list-item--PaddingTop)"
  };
  const c_context_selector__menu_list_item_PaddingRight = exports.c_context_selector__menu_list_item_PaddingRight = {
    "name": "--pf-c-context-selector__menu-list-item--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-context-selector__menu-list-item--PaddingRight)"
  };
  const c_context_selector__menu_list_item_PaddingBottom = exports.c_context_selector__menu_list_item_PaddingBottom = {
    "name": "--pf-c-context-selector__menu-list-item--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-context-selector__menu-list-item--PaddingBottom)"
  };
  const c_context_selector__menu_list_item_PaddingLeft = exports.c_context_selector__menu_list_item_PaddingLeft = {
    "name": "--pf-c-context-selector__menu-list-item--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-context-selector__menu-list-item--PaddingLeft)"
  };
  const c_context_selector__menu_list_item_hover_BackgroundColor = exports.c_context_selector__menu_list_item_hover_BackgroundColor = {
    "name": "--pf-c-context-selector__menu-list-item--hover--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-context-selector__menu-list-item--hover--BackgroundColor)"
  };
  const c_context_selector__menu_list_item_disabled_Color = exports.c_context_selector__menu_list_item_disabled_Color = {
    "name": "--pf-c-context-selector__menu-list-item--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-context-selector__menu-list-item--disabled--Color)"
  };
  const hidden_visible_visible_Visibility = exports.hidden_visible_visible_Visibility = {
    "name": "--pf-hidden-visible--visible--Visibility",
    "value": "visible",
    "var": "var(--pf-hidden-visible--visible--Visibility)"
  };
  const hidden_visible_hidden_Display = exports.hidden_visible_hidden_Display = {
    "name": "--pf-hidden-visible--hidden--Display",
    "value": "none",
    "var": "var(--pf-hidden-visible--hidden--Display)"
  };
  const hidden_visible_hidden_Visibility = exports.hidden_visible_hidden_Visibility = {
    "name": "--pf-hidden-visible--hidden--Visibility",
    "value": "hidden",
    "var": "var(--pf-hidden-visible--hidden--Visibility)"
  };
  const hidden_visible_Display = exports.hidden_visible_Display = {
    "name": "--pf-hidden-visible--Display",
    "value": "none",
    "var": "var(--pf-hidden-visible--Display)"
  };
  const hidden_visible_Visibility = exports.hidden_visible_Visibility = {
    "name": "--pf-hidden-visible--Visibility",
    "value": "hidden",
    "var": "var(--pf-hidden-visible--Visibility)"
  };
  const c_data_list_BackgroundColor = exports.c_data_list_BackgroundColor = {
    "name": "--pf-c-data-list--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-data-list--BackgroundColor)"
  };
  const c_data_list_BorderTopColor = exports.c_data_list_BorderTopColor = {
    "name": "--pf-c-data-list--BorderTopColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-data-list--BorderTopColor)"
  };
  const c_data_list_BorderTopWidth = exports.c_data_list_BorderTopWidth = {
    "name": "--pf-c-data-list--BorderTopWidth",
    "value": "1px",
    "var": "var(--pf-c-data-list--BorderTopWidth)"
  };
  const c_data_list_sm_BorderTopWidth = exports.c_data_list_sm_BorderTopWidth = {
    "name": "--pf-c-data-list--sm--BorderTopWidth",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list--sm--BorderTopWidth)"
  };
  const c_data_list_sm_BorderTopColor = exports.c_data_list_sm_BorderTopColor = {
    "name": "--pf-c-data-list--sm--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-data-list--sm--BorderTopColor)"
  };
  const c_data_list_BorderBottomColor = exports.c_data_list_BorderBottomColor = {
    "name": "--pf-c-data-list--BorderBottomColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-data-list--BorderBottomColor)"
  };
  const c_data_list_BorderBottomWidth = exports.c_data_list_BorderBottomWidth = {
    "name": "--pf-c-data-list--BorderBottomWidth",
    "value": "0",
    "var": "var(--pf-c-data-list--BorderBottomWidth)"
  };
  const c_data_list__item_m_expanded_before_BackgroundColor = exports.c_data_list__item_m_expanded_before_BackgroundColor = {
    "name": "--pf-c-data-list__item--m-expanded--before--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-data-list__item--m-expanded--before--BackgroundColor)"
  };
  const c_data_list__item_m_selected_before_BackgroundColor = exports.c_data_list__item_m_selected_before_BackgroundColor = {
    "name": "--pf-c-data-list__item--m-selected--before--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-data-list__item--m-selected--before--BackgroundColor)"
  };
  const c_data_list__item_m_selected_BoxShadow = exports.c_data_list__item_m_selected_BoxShadow = {
    "name": "--pf-c-data-list__item--m-selected--BoxShadow",
    "value": "0 -0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12),0 0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-data-list__item--m-selected--BoxShadow)"
  };
  const c_data_list__item_m_selectable_OutlineOffset = exports.c_data_list__item_m_selectable_OutlineOffset = {
    "name": "--pf-c-data-list__item--m-selectable--OutlineOffset",
    "value": "-0.25rem",
    "var": "var(--pf-c-data-list__item--m-selectable--OutlineOffset)"
  };
  const c_data_list__item_m_selectable_hover_ZIndex = exports.c_data_list__item_m_selectable_hover_ZIndex = {
    "name": "--pf-c-data-list__item--m-selectable--hover--ZIndex",
    "value": "100",
    "var": "var(--pf-c-data-list__item--m-selectable--hover--ZIndex)"
  };
  const c_data_list__item_m_selectable_hover_BoxShadow = exports.c_data_list__item_m_selectable_hover_BoxShadow = {
    "name": "--pf-c-data-list__item--m-selectable--hover--BoxShadow",
    "value": "0 -0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12),0 0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-data-list__item--m-selectable--hover--BoxShadow)"
  };
  const c_data_list__item_m_selectable_focus_BoxShadow = exports.c_data_list__item_m_selectable_focus_BoxShadow = {
    "name": "--pf-c-data-list__item--m-selectable--focus--BoxShadow",
    "value": "0 -0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12),0 0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-data-list__item--m-selectable--focus--BoxShadow)"
  };
  const c_data_list__item_m_selectable_active_BoxShadow = exports.c_data_list__item_m_selectable_active_BoxShadow = {
    "name": "--pf-c-data-list__item--m-selectable--active--BoxShadow",
    "value": "0 -0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12),0 0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-data-list__item--m-selectable--active--BoxShadow)"
  };
  const c_data_list__item_m_expanded_m_selectable_before_BackgroundColor = exports.c_data_list__item_m_expanded_m_selectable_before_BackgroundColor = {
    "name": "--pf-c-data-list__item--m-expanded--m-selectable--before--BackgroundColor",
    "value": "#73bcf7",
    "var": "var(--pf-c-data-list__item--m-expanded--m-selectable--before--BackgroundColor)"
  };
  const c_data_list__item_item_BorderTopColor = exports.c_data_list__item_item_BorderTopColor = {
    "name": "--pf-c-data-list__item-item--BorderTopColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-data-list__item-item--BorderTopColor)"
  };
  const c_data_list__item_item_BorderTopWidth = exports.c_data_list__item_item_BorderTopWidth = {
    "name": "--pf-c-data-list__item-item--BorderTopWidth",
    "value": "0",
    "var": "var(--pf-c-data-list__item-item--BorderTopWidth)"
  };
  const c_data_list__item_hover_item_BorderTopColor = exports.c_data_list__item_hover_item_BorderTopColor = {
    "name": "--pf-c-data-list__item--hover--item--BorderTopColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-data-list__item--hover--item--BorderTopColor)"
  };
  const c_data_list__item_hover_item_BorderTopWidth = exports.c_data_list__item_hover_item_BorderTopWidth = {
    "name": "--pf-c-data-list__item--hover--item--BorderTopWidth",
    "value": "1px",
    "var": "var(--pf-c-data-list__item--hover--item--BorderTopWidth)"
  };
  const c_data_list__item_item_sm_BorderTopWidth = exports.c_data_list__item_item_sm_BorderTopWidth = {
    "name": "--pf-c-data-list__item-item--sm--BorderTopWidth",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list__item-item--sm--BorderTopWidth)"
  };
  const c_data_list__item_item_sm_BorderTopColor = exports.c_data_list__item_item_sm_BorderTopColor = {
    "name": "--pf-c-data-list__item-item--sm--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-data-list__item-item--sm--BorderTopColor)"
  };
  const c_data_list__item_before_BackgroundColor = exports.c_data_list__item_before_BackgroundColor = {
    "name": "--pf-c-data-list__item--before--BackgroundColor",
    "value": "#73bcf7",
    "var": "var(--pf-c-data-list__item--before--BackgroundColor)"
  };
  const c_data_list__item_before_Width = exports.c_data_list__item_before_Width = {
    "name": "--pf-c-data-list__item--before--Width",
    "value": "3px",
    "var": "var(--pf-c-data-list__item--before--Width)"
  };
  const c_data_list__item_before_Transition = exports.c_data_list__item_before_Transition = {
    "name": "--pf-c-data-list__item--before--Transition",
    "value": "all 250ms ease-in-out",
    "var": "var(--pf-c-data-list__item--before--Transition)"
  };
  const c_data_list__item_before_ZIndex = exports.c_data_list__item_before_ZIndex = {
    "name": "--pf-c-data-list__item--before--ZIndex",
    "value": "500",
    "var": "var(--pf-c-data-list__item--before--ZIndex)"
  };
  const c_data_list__item_before_Top = exports.c_data_list__item_before_Top = {
    "name": "--pf-c-data-list__item--before--Top",
    "value": "0",
    "var": "var(--pf-c-data-list__item--before--Top)"
  };
  const c_data_list__item_before_Bottom = exports.c_data_list__item_before_Bottom = {
    "name": "--pf-c-data-list__item--before--Bottom",
    "value": "0",
    "var": "var(--pf-c-data-list__item--before--Bottom)"
  };
  const c_data_list__item_before_Height = exports.c_data_list__item_before_Height = {
    "name": "--pf-c-data-list__item--before--Height",
    "value": "initial",
    "var": "var(--pf-c-data-list__item--before--Height)"
  };
  const c_data_list__item_item_before_Top = exports.c_data_list__item_item_before_Top = {
    "name": "--pf-c-data-list__item-item--before--Top",
    "value": "calc(1px*-1)",
    "var": "var(--pf-c-data-list__item-item--before--Top)"
  };
  const c_data_list__item_item_before_Height = exports.c_data_list__item_item_before_Height = {
    "name": "--pf-c-data-list__item-item--before--Height",
    "value": "initial",
    "var": "var(--pf-c-data-list__item-item--before--Height)"
  };
  const c_data_list__item_row_PaddingRight = exports.c_data_list__item_row_PaddingRight = {
    "name": "--pf-c-data-list__item-row--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-data-list__item-row--PaddingRight)"
  };
  const c_data_list__item_row_md_PaddingRight = exports.c_data_list__item_row_md_PaddingRight = {
    "name": "--pf-c-data-list__item-row--md--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-row--md--PaddingRight)"
  };
  const c_data_list__item_row_PaddingLeft = exports.c_data_list__item_row_PaddingLeft = {
    "name": "--pf-c-data-list__item-row--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-data-list__item-row--PaddingLeft)"
  };
  const c_data_list__item_row_md_PaddingLeft = exports.c_data_list__item_row_md_PaddingLeft = {
    "name": "--pf-c-data-list__item-row--md--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-row--md--PaddingLeft)"
  };
  const c_data_list__item_content_PaddingBottom = exports.c_data_list__item_content_PaddingBottom = {
    "name": "--pf-c-data-list__item-content--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list__item-content--PaddingBottom)"
  };
  const c_data_list__cell_PaddingTop = exports.c_data_list__cell_PaddingTop = {
    "name": "--pf-c-data-list__cell--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list__cell--PaddingTop)"
  };
  const c_data_list__cell_PaddingBottom = exports.c_data_list__cell_PaddingBottom = {
    "name": "--pf-c-data-list__cell--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list__cell--PaddingBottom)"
  };
  const c_data_list__cell_md_PaddingBottom = exports.c_data_list__cell_md_PaddingBottom = {
    "name": "--pf-c-data-list__cell--md--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-data-list__cell--md--PaddingBottom)"
  };
  const c_data_list__cell_m_icon_MarginRight = exports.c_data_list__cell_m_icon_MarginRight = {
    "name": "--pf-c-data-list__cell--m-icon--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list__cell--m-icon--MarginRight)"
  };
  const c_data_list__cell_cell_PaddingTop = exports.c_data_list__cell_cell_PaddingTop = {
    "name": "--pf-c-data-list__cell-cell--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-data-list__cell-cell--PaddingTop)"
  };
  const c_data_list__cell_cell_md_PaddingTop = exports.c_data_list__cell_cell_md_PaddingTop = {
    "name": "--pf-c-data-list__cell-cell--md--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-data-list__cell-cell--md--PaddingTop)"
  };
  const c_data_list__cell_cell_MarginRight = exports.c_data_list__cell_cell_MarginRight = {
    "name": "--pf-c-data-list__cell-cell--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list__cell-cell--MarginRight)"
  };
  const c_data_list__toggle_MarginLeft = exports.c_data_list__toggle_MarginLeft = {
    "name": "--pf-c-data-list__toggle--MarginLeft",
    "value": "calc(0.5rem*-1)",
    "var": "var(--pf-c-data-list__toggle--MarginLeft)"
  };
  const c_data_list__toggle_MarginTop = exports.c_data_list__toggle_MarginTop = {
    "name": "--pf-c-data-list__toggle--MarginTop",
    "value": "calc(0.375rem*-1)",
    "var": "var(--pf-c-data-list__toggle--MarginTop)"
  };
  const c_data_list__toggle_icon_Transition = exports.c_data_list__toggle_icon_Transition = {
    "name": "--pf-c-data-list__toggle-icon--Transition",
    "value": ".2s ease-in 0s",
    "var": "var(--pf-c-data-list__toggle-icon--Transition)"
  };
  const c_data_list__item_m_expanded__toggle_c_button_icon_Transform = exports.c_data_list__item_m_expanded__toggle_c_button_icon_Transform = {
    "name": "--pf-c-data-list__item--m-expanded__toggle--c-button-icon--Transform",
    "value": "rotate(90deg)",
    "var": "var(--pf-c-data-list__item--m-expanded__toggle--c-button-icon--Transform)"
  };
  const c_data_list__item_control_PaddingTop = exports.c_data_list__item_control_PaddingTop = {
    "name": "--pf-c-data-list__item-control--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list__item-control--PaddingTop)"
  };
  const c_data_list__item_control_PaddingBottom = exports.c_data_list__item_control_PaddingBottom = {
    "name": "--pf-c-data-list__item-control--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-data-list__item-control--PaddingBottom)"
  };
  const c_data_list__item_control_MarginRight = exports.c_data_list__item_control_MarginRight = {
    "name": "--pf-c-data-list__item-control--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-control--MarginRight)"
  };
  const c_data_list__item_control_md_MarginRight = exports.c_data_list__item_control_md_MarginRight = {
    "name": "--pf-c-data-list__item-control--md--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-control--md--MarginRight)"
  };
  const c_data_list__item_control_not_last_child_MarginRight = exports.c_data_list__item_control_not_last_child_MarginRight = {
    "name": "--pf-c-data-list__item-control--not-last-child--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-control--not-last-child--MarginRight)"
  };
  const c_data_list__item_action_Display = exports.c_data_list__item_action_Display = {
    "name": "--pf-c-data-list__item-action--Display",
    "value": "flex",
    "var": "var(--pf-c-data-list__item-action--Display)"
  };
  const c_data_list__item_action_PaddingTop = exports.c_data_list__item_action_PaddingTop = {
    "name": "--pf-c-data-list__item-action--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list__item-action--PaddingTop)"
  };
  const c_data_list__item_action_PaddingBottom = exports.c_data_list__item_action_PaddingBottom = {
    "name": "--pf-c-data-list__item-action--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list__item-action--PaddingBottom)"
  };
  const c_data_list__item_action_MarginLeft = exports.c_data_list__item_action_MarginLeft = {
    "name": "--pf-c-data-list__item-action--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-action--MarginLeft)"
  };
  const c_data_list__item_action_md_MarginLeft = exports.c_data_list__item_action_md_MarginLeft = {
    "name": "--pf-c-data-list__item-action--md--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-action--md--MarginLeft)"
  };
  const c_data_list__item_action_not_last_child_MarginRight = exports.c_data_list__item_action_not_last_child_MarginRight = {
    "name": "--pf-c-data-list__item-action--not-last-child--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-action--not-last-child--MarginRight)"
  };
  const c_data_list__item_action_not_last_child_lg_MarginBottom = exports.c_data_list__item_action_not_last_child_lg_MarginBottom = {
    "name": "--pf-c-data-list__item-action--not-last-child--lg--MarginBottom",
    "value": "1rem",
    "var": "var(--pf-c-data-list__item-action--not-last-child--lg--MarginBottom)"
  };
  const c_data_list__action_MarginTop = exports.c_data_list__action_MarginTop = {
    "name": "--pf-c-data-list__action--MarginTop",
    "value": "calc(0.375rem*-1)",
    "var": "var(--pf-c-data-list__action--MarginTop)"
  };
  const c_data_list__expandable_content_BorderTopColor = exports.c_data_list__expandable_content_BorderTopColor = {
    "name": "--pf-c-data-list__expandable-content--BorderTopColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-data-list__expandable-content--BorderTopColor)"
  };
  const c_data_list__expandable_content_MarginRight = exports.c_data_list__expandable_content_MarginRight = {
    "name": "--pf-c-data-list__expandable-content--MarginRight",
    "value": "calc(1.5rem*-1)",
    "var": "var(--pf-c-data-list__expandable-content--MarginRight)"
  };
  const c_data_list__expandable_content_MarginLeft = exports.c_data_list__expandable_content_MarginLeft = {
    "name": "--pf-c-data-list__expandable-content--MarginLeft",
    "value": "calc(1.5rem*-1)",
    "var": "var(--pf-c-data-list__expandable-content--MarginLeft)"
  };
  const c_data_list__expandable_content_BoxShadow = exports.c_data_list__expandable_content_BoxShadow = {
    "name": "--pf-c-data-list__expandable-content--BoxShadow",
    "value": "0 0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-c-data-list__expandable-content--BoxShadow)"
  };
  const c_data_list__expandable_content_md_MaxHeight = exports.c_data_list__expandable_content_md_MaxHeight = {
    "name": "--pf-c-data-list__expandable-content--md--MaxHeight",
    "value": "37.5rem",
    "var": "var(--pf-c-data-list__expandable-content--md--MaxHeight)"
  };
  const c_data_list__expandable_content_before_Top = exports.c_data_list__expandable_content_before_Top = {
    "name": "--pf-c-data-list__expandable-content--before--Top",
    "value": "calc(1px*-1)",
    "var": "var(--pf-c-data-list__expandable-content--before--Top)"
  };
  const c_data_list__item_m_expanded__expandable_content_BorderTopWidth = exports.c_data_list__item_m_expanded__expandable_content_BorderTopWidth = {
    "name": "--pf-c-data-list__item--m-expanded__expandable-content--BorderTopWidth",
    "value": "1px",
    "var": "var(--pf-c-data-list__item--m-expanded__expandable-content--BorderTopWidth)"
  };
  const c_data_list__expandable_content_body_PaddingTop = exports.c_data_list__expandable_content_body_PaddingTop = {
    "name": "--pf-c-data-list__expandable-content-body--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-data-list__expandable-content-body--PaddingTop)"
  };
  const c_data_list__expandable_content_body_PaddingRight = exports.c_data_list__expandable_content_body_PaddingRight = {
    "name": "--pf-c-data-list__expandable-content-body--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-data-list__expandable-content-body--PaddingRight)"
  };
  const c_data_list__expandable_content_body_PaddingBottom = exports.c_data_list__expandable_content_body_PaddingBottom = {
    "name": "--pf-c-data-list__expandable-content-body--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-data-list__expandable-content-body--PaddingBottom)"
  };
  const c_data_list__expandable_content_body_PaddingLeft = exports.c_data_list__expandable_content_body_PaddingLeft = {
    "name": "--pf-c-data-list__expandable-content-body--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-data-list__expandable-content-body--PaddingLeft)"
  };
  const c_data_list__expandable_content_body_md_PaddingTop = exports.c_data_list__expandable_content_body_md_PaddingTop = {
    "name": "--pf-c-data-list__expandable-content-body--md--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-data-list__expandable-content-body--md--PaddingTop)"
  };
  const c_data_list__expandable_content_body_md_PaddingRight = exports.c_data_list__expandable_content_body_md_PaddingRight = {
    "name": "--pf-c-data-list__expandable-content-body--md--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list__expandable-content-body--md--PaddingRight)"
  };
  const c_data_list__expandable_content_body_md_PaddingBottom = exports.c_data_list__expandable_content_body_md_PaddingBottom = {
    "name": "--pf-c-data-list__expandable-content-body--md--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-data-list__expandable-content-body--md--PaddingBottom)"
  };
  const c_data_list__expandable_content_body_md_PaddingLeft = exports.c_data_list__expandable_content_body_md_PaddingLeft = {
    "name": "--pf-c-data-list__expandable-content-body--md--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-data-list__expandable-content-body--md--PaddingLeft)"
  };
  const c_data_list_m_compact__cell_PaddingTop = exports.c_data_list_m_compact__cell_PaddingTop = {
    "name": "--pf-c-data-list--m-compact__cell--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list--m-compact__cell--PaddingTop)"
  };
  const c_data_list_m_compact__cell_PaddingBottom = exports.c_data_list_m_compact__cell_PaddingBottom = {
    "name": "--pf-c-data-list--m-compact__cell--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list--m-compact__cell--PaddingBottom)"
  };
  const c_data_list_m_compact__cell_md_PaddingBottom = exports.c_data_list_m_compact__cell_md_PaddingBottom = {
    "name": "--pf-c-data-list--m-compact__cell--md--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-data-list--m-compact__cell--md--PaddingBottom)"
  };
  const c_data_list_m_compact__cell_cell_PaddingTop = exports.c_data_list_m_compact__cell_cell_PaddingTop = {
    "name": "--pf-c-data-list--m-compact__cell-cell--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-data-list--m-compact__cell-cell--PaddingTop)"
  };
  const c_data_list_m_compact__cell_cell_md_PaddingTop = exports.c_data_list_m_compact__cell_cell_md_PaddingTop = {
    "name": "--pf-c-data-list--m-compact__cell-cell--md--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list--m-compact__cell-cell--md--PaddingTop)"
  };
  const c_data_list_m_compact__cell_cell_MarginRight = exports.c_data_list_m_compact__cell_cell_MarginRight = {
    "name": "--pf-c-data-list--m-compact__cell-cell--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list--m-compact__cell-cell--MarginRight)"
  };
  const c_data_list_m_compact__item_control_PaddingTop = exports.c_data_list_m_compact__item_control_PaddingTop = {
    "name": "--pf-c-data-list--m-compact__item-control--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list--m-compact__item-control--PaddingTop)"
  };
  const c_data_list_m_compact__item_control_PaddingBottom = exports.c_data_list_m_compact__item_control_PaddingBottom = {
    "name": "--pf-c-data-list--m-compact__item-control--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-data-list--m-compact__item-control--PaddingBottom)"
  };
  const c_data_list_m_compact__item_control_MarginRight = exports.c_data_list_m_compact__item_control_MarginRight = {
    "name": "--pf-c-data-list--m-compact__item-control--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-data-list--m-compact__item-control--MarginRight)"
  };
  const c_data_list_m_compact__item_action_PaddingTop = exports.c_data_list_m_compact__item_action_PaddingTop = {
    "name": "--pf-c-data-list--m-compact__item-action--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list--m-compact__item-action--PaddingTop)"
  };
  const c_data_list_m_compact__item_action_PaddingBottom = exports.c_data_list_m_compact__item_action_PaddingBottom = {
    "name": "--pf-c-data-list--m-compact__item-action--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list--m-compact__item-action--PaddingBottom)"
  };
  const c_data_list_m_compact__item_action_MarginLeft = exports.c_data_list_m_compact__item_action_MarginLeft = {
    "name": "--pf-c-data-list--m-compact__item-action--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-data-list--m-compact__item-action--MarginLeft)"
  };
  const c_data_list_m_compact__item_content_PaddingBottom = exports.c_data_list_m_compact__item_content_PaddingBottom = {
    "name": "--pf-c-data-list--m-compact__item-content--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-data-list--m-compact__item-content--PaddingBottom)"
  };
  const hidden_visible_visible_Display = exports.hidden_visible_visible_Display = {
    "name": "--pf-hidden-visible--visible--Display",
    "value": "table-cell",
    "var": "var(--pf-hidden-visible--visible--Display)"
  };
  const c_data_toolbar_BackgroundColor = exports.c_data_toolbar_BackgroundColor = {
    "name": "--pf-c-data-toolbar--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-data-toolbar--BackgroundColor)"
  };
  const c_data_toolbar_RowGap = exports.c_data_toolbar_RowGap = {
    "name": "--pf-c-data-toolbar--RowGap",
    "value": "1.5rem",
    "var": "var(--pf-c-data-toolbar--RowGap)"
  };
  const c_data_toolbar_PaddingTop = exports.c_data_toolbar_PaddingTop = {
    "name": "--pf-c-data-toolbar--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar--PaddingTop)"
  };
  const c_data_toolbar_PaddingBottom = exports.c_data_toolbar_PaddingBottom = {
    "name": "--pf-c-data-toolbar--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar--PaddingBottom)"
  };
  const c_data_toolbar__content_PaddingRight = exports.c_data_toolbar__content_PaddingRight = {
    "name": "--pf-c-data-toolbar__content--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__content--PaddingRight)"
  };
  const c_data_toolbar__content_PaddingLeft = exports.c_data_toolbar__content_PaddingLeft = {
    "name": "--pf-c-data-toolbar__content--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__content--PaddingLeft)"
  };
  const c_data_toolbar__expandable_content_PaddingTop = exports.c_data_toolbar__expandable_content_PaddingTop = {
    "name": "--pf-c-data-toolbar__expandable-content--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-data-toolbar__expandable-content--PaddingTop)"
  };
  const c_data_toolbar__expandable_content_PaddingRight = exports.c_data_toolbar__expandable_content_PaddingRight = {
    "name": "--pf-c-data-toolbar__expandable-content--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__expandable-content--PaddingRight)"
  };
  const c_data_toolbar__expandable_content_PaddingBottom = exports.c_data_toolbar__expandable_content_PaddingBottom = {
    "name": "--pf-c-data-toolbar__expandable-content--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__expandable-content--PaddingBottom)"
  };
  const c_data_toolbar__expandable_content_PaddingLeft = exports.c_data_toolbar__expandable_content_PaddingLeft = {
    "name": "--pf-c-data-toolbar__expandable-content--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__expandable-content--PaddingLeft)"
  };
  const c_data_toolbar__expandable_content_lg_PaddingRight = exports.c_data_toolbar__expandable_content_lg_PaddingRight = {
    "name": "--pf-c-data-toolbar__expandable-content--lg--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-data-toolbar__expandable-content--lg--PaddingRight)"
  };
  const c_data_toolbar__expandable_content_lg_PaddingBottom = exports.c_data_toolbar__expandable_content_lg_PaddingBottom = {
    "name": "--pf-c-data-toolbar__expandable-content--lg--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-data-toolbar__expandable-content--lg--PaddingBottom)"
  };
  const c_data_toolbar__expandable_content_lg_PaddingLeft = exports.c_data_toolbar__expandable_content_lg_PaddingLeft = {
    "name": "--pf-c-data-toolbar__expandable-content--lg--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-data-toolbar__expandable-content--lg--PaddingLeft)"
  };
  const c_data_toolbar__expandable_content_ZIndex = exports.c_data_toolbar__expandable_content_ZIndex = {
    "name": "--pf-c-data-toolbar__expandable-content--ZIndex",
    "value": "100",
    "var": "var(--pf-c-data-toolbar__expandable-content--ZIndex)"
  };
  const c_data_toolbar__expandable_content_BoxShadow = exports.c_data_toolbar__expandable_content_BoxShadow = {
    "name": "--pf-c-data-toolbar__expandable-content--BoxShadow",
    "value": "0 0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-c-data-toolbar__expandable-content--BoxShadow)"
  };
  const c_data_toolbar__expandable_content_BackgroundColor = exports.c_data_toolbar__expandable_content_BackgroundColor = {
    "name": "--pf-c-data-toolbar__expandable-content--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-data-toolbar__expandable-content--BackgroundColor)"
  };
  const c_data_toolbar__expandable_content_m_expanded_GridRowGap = exports.c_data_toolbar__expandable_content_m_expanded_GridRowGap = {
    "name": "--pf-c-data-toolbar__expandable-content--m-expanded--GridRowGap",
    "value": "1.5rem",
    "var": "var(--pf-c-data-toolbar__expandable-content--m-expanded--GridRowGap)"
  };
  const c_data_toolbar__group_m_chip_container_MarginTop = exports.c_data_toolbar__group_m_chip_container_MarginTop = {
    "name": "--pf-c-data-toolbar__group--m-chip-container--MarginTop",
    "value": "calc(1rem*-1)",
    "var": "var(--pf-c-data-toolbar__group--m-chip-container--MarginTop)"
  };
  const c_data_toolbar__group_m_chip_container__item_MarginTop = exports.c_data_toolbar__group_m_chip_container__item_MarginTop = {
    "name": "--pf-c-data-toolbar__group--m-chip-container__item--MarginTop",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__group--m-chip-container__item--MarginTop)"
  };
  const c_data_toolbar_spacer_base = exports.c_data_toolbar_spacer_base = {
    "name": "--pf-c-data-toolbar--spacer--base",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar--spacer--base)"
  };
  const c_data_toolbar__item_spacer = exports.c_data_toolbar__item_spacer = {
    "name": "--pf-c-data-toolbar__item--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__item--spacer)"
  };
  const c_data_toolbar__group_spacer = exports.c_data_toolbar__group_spacer = {
    "name": "--pf-c-data-toolbar__group--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__group--spacer)"
  };
  const c_data_toolbar__group_m_toggle_group_spacer = exports.c_data_toolbar__group_m_toggle_group_spacer = {
    "name": "--pf-c-data-toolbar__group--m-toggle-group--spacer",
    "value": "0.5rem",
    "var": "var(--pf-c-data-toolbar__group--m-toggle-group--spacer)"
  };
  const c_data_toolbar__group_m_toggle_group_m_show_spacer = exports.c_data_toolbar__group_m_toggle_group_m_show_spacer = {
    "name": "--pf-c-data-toolbar__group--m-toggle-group--m-show--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__group--m-toggle-group--m-show--spacer)"
  };
  const c_data_toolbar__group_m_icon_button_group_spacer = exports.c_data_toolbar__group_m_icon_button_group_spacer = {
    "name": "--pf-c-data-toolbar__group--m-icon-button-group--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__group--m-icon-button-group--spacer)"
  };
  const c_data_toolbar__group_m_icon_button_group_space_items = exports.c_data_toolbar__group_m_icon_button_group_space_items = {
    "name": "--pf-c-data-toolbar__group--m-icon-button-group--space-items",
    "value": "0",
    "var": "var(--pf-c-data-toolbar__group--m-icon-button-group--space-items)"
  };
  const c_data_toolbar__group_m_button_group_spacer = exports.c_data_toolbar__group_m_button_group_spacer = {
    "name": "--pf-c-data-toolbar__group--m-button-group--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__group--m-button-group--spacer)"
  };
  const c_data_toolbar__group_m_button_group_space_items = exports.c_data_toolbar__group_m_button_group_space_items = {
    "name": "--pf-c-data-toolbar__group--m-button-group--space-items",
    "value": "0.5rem",
    "var": "var(--pf-c-data-toolbar__group--m-button-group--space-items)"
  };
  const c_data_toolbar__group_m_filter_group_spacer = exports.c_data_toolbar__group_m_filter_group_spacer = {
    "name": "--pf-c-data-toolbar__group--m-filter-group--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__group--m-filter-group--spacer)"
  };
  const c_data_toolbar__group_m_filter_group_space_items = exports.c_data_toolbar__group_m_filter_group_space_items = {
    "name": "--pf-c-data-toolbar__group--m-filter-group--space-items",
    "value": "0",
    "var": "var(--pf-c-data-toolbar__group--m-filter-group--space-items)"
  };
  const c_data_toolbar__item_m_separator_spacer = exports.c_data_toolbar__item_m_separator_spacer = {
    "name": "--pf-c-data-toolbar__item--m-separator--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__item--m-separator--spacer)"
  };
  const c_data_toolbar__item_m_separator_BackgroundColor = exports.c_data_toolbar__item_m_separator_BackgroundColor = {
    "name": "--pf-c-data-toolbar__item--m-separator--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-data-toolbar__item--m-separator--BackgroundColor)"
  };
  const c_data_toolbar__item_m_separator_Width = exports.c_data_toolbar__item_m_separator_Width = {
    "name": "--pf-c-data-toolbar__item--m-separator--Width",
    "value": "2px",
    "var": "var(--pf-c-data-toolbar__item--m-separator--Width)"
  };
  const c_data_toolbar__item_m_separator_Height = exports.c_data_toolbar__item_m_separator_Height = {
    "name": "--pf-c-data-toolbar__item--m-separator--Height",
    "value": "1.125rem",
    "var": "var(--pf-c-data-toolbar__item--m-separator--Height)"
  };
  const c_data_toolbar__item_m_overflow_menu_spacer = exports.c_data_toolbar__item_m_overflow_menu_spacer = {
    "name": "--pf-c-data-toolbar__item--m-overflow-menu--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__item--m-overflow-menu--spacer)"
  };
  const c_data_toolbar__item_m_bulk_select_spacer = exports.c_data_toolbar__item_m_bulk_select_spacer = {
    "name": "--pf-c-data-toolbar__item--m-bulk-select--spacer",
    "value": "1.5rem",
    "var": "var(--pf-c-data-toolbar__item--m-bulk-select--spacer)"
  };
  const c_data_toolbar__item_m_search_filter_spacer = exports.c_data_toolbar__item_m_search_filter_spacer = {
    "name": "--pf-c-data-toolbar__item--m-search-filter--spacer",
    "value": "0.5rem",
    "var": "var(--pf-c-data-toolbar__item--m-search-filter--spacer)"
  };
  const c_data_toolbar__item_m_chip_group_spacer = exports.c_data_toolbar__item_m_chip_group_spacer = {
    "name": "--pf-c-data-toolbar__item--m-chip-group--spacer",
    "value": "0.5rem",
    "var": "var(--pf-c-data-toolbar__item--m-chip-group--spacer)"
  };
  const c_data_toolbar__item_m_label_spacer = exports.c_data_toolbar__item_m_label_spacer = {
    "name": "--pf-c-data-toolbar__item--m-label--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar__item--m-label--spacer)"
  };
  const c_data_toolbar__item_m_label_FontWeight = exports.c_data_toolbar__item_m_label_FontWeight = {
    "name": "--pf-c-data-toolbar__item--m-label--FontWeight",
    "value": "700",
    "var": "var(--pf-c-data-toolbar__item--m-label--FontWeight)"
  };
  const c_data_toolbar__toggle_m_expanded__c_button_m_plain_Color = exports.c_data_toolbar__toggle_m_expanded__c_button_m_plain_Color = {
    "name": "--pf-c-data-toolbar__toggle--m-expanded__c-button--m-plain--Color",
    "value": "#151515",
    "var": "var(--pf-c-data-toolbar__toggle--m-expanded__c-button--m-plain--Color)"
  };
  const c_data_toolbar_c_divider_m_vertical_spacer = exports.c_data_toolbar_c_divider_m_vertical_spacer = {
    "name": "--pf-c-data-toolbar--c-divider--m-vertical--spacer",
    "value": "1rem",
    "var": "var(--pf-c-data-toolbar--c-divider--m-vertical--spacer)"
  };
  const c_data_toolbar_spacer = exports.c_data_toolbar_spacer = {
    "name": "--pf-c-data-toolbar--spacer",
    "value": "0",
    "var": "var(--pf-c-data-toolbar--spacer)"
  };
  const c_divider_Height = exports.c_divider_Height = {
    "name": "--pf-c-divider--Height",
    "value": "1px",
    "var": "var(--pf-c-divider--Height)"
  };
  const c_divider_BackgroundColor = exports.c_divider_BackgroundColor = {
    "name": "--pf-c-divider--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-divider--BackgroundColor)"
  };
  const c_divider_after_Height = exports.c_divider_after_Height = {
    "name": "--pf-c-divider--after--Height",
    "value": "1px",
    "var": "var(--pf-c-divider--after--Height)"
  };
  const c_divider_after_BackgroundColor = exports.c_divider_after_BackgroundColor = {
    "name": "--pf-c-divider--after--BackgroundColor",
    "value": "#212427",
    "var": "var(--pf-c-divider--after--BackgroundColor)"
  };
  const c_divider_after_FlexBasis = exports.c_divider_after_FlexBasis = {
    "name": "--pf-c-divider--after--FlexBasis",
    "value": "100%",
    "var": "var(--pf-c-divider--after--FlexBasis)"
  };
  const c_divider_after_Inset = exports.c_divider_after_Inset = {
    "name": "--pf-c-divider--after--Inset",
    "value": "4rem",
    "var": "var(--pf-c-divider--after--Inset)"
  };
  const c_divider_m_vertical_after_FlexBasis = exports.c_divider_m_vertical_after_FlexBasis = {
    "name": "--pf-c-divider--m-vertical--after--FlexBasis",
    "value": "100%",
    "var": "var(--pf-c-divider--m-vertical--after--FlexBasis)"
  };
  const c_divider_m_vertical_after_Width = exports.c_divider_m_vertical_after_Width = {
    "name": "--pf-c-divider--m-vertical--after--Width",
    "value": "1px",
    "var": "var(--pf-c-divider--m-vertical--after--Width)"
  };
  const c_drawer__section_BackgroundColor = exports.c_drawer__section_BackgroundColor = {
    "name": "--pf-c-drawer__section--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-drawer__section--BackgroundColor)"
  };
  const c_drawer__content_FlexBasis = exports.c_drawer__content_FlexBasis = {
    "name": "--pf-c-drawer__content--FlexBasis",
    "value": "100%",
    "var": "var(--pf-c-drawer__content--FlexBasis)"
  };
  const c_drawer__content_BackgroundColor = exports.c_drawer__content_BackgroundColor = {
    "name": "--pf-c-drawer__content--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-drawer__content--BackgroundColor)"
  };
  const c_drawer__content_ZIndex = exports.c_drawer__content_ZIndex = {
    "name": "--pf-c-drawer__content--ZIndex",
    "value": "100",
    "var": "var(--pf-c-drawer__content--ZIndex)"
  };
  const c_drawer__panel_FlexBasis = exports.c_drawer__panel_FlexBasis = {
    "name": "--pf-c-drawer__panel--FlexBasis",
    "value": "100%",
    "var": "var(--pf-c-drawer__panel--FlexBasis)"
  };
  const c_drawer__panel_md_FlexBasis = exports.c_drawer__panel_md_FlexBasis = {
    "name": "--pf-c-drawer__panel--md--FlexBasis",
    "value": "50%",
    "var": "var(--pf-c-drawer__panel--md--FlexBasis)"
  };
  const c_drawer__panel_MinWidth = exports.c_drawer__panel_MinWidth = {
    "name": "--pf-c-drawer__panel--MinWidth",
    "value": "50%",
    "var": "var(--pf-c-drawer__panel--MinWidth)"
  };
  const c_drawer__panel_xl_MinWidth = exports.c_drawer__panel_xl_MinWidth = {
    "name": "--pf-c-drawer__panel--xl--MinWidth",
    "value": "28.125rem",
    "var": "var(--pf-c-drawer__panel--xl--MinWidth)"
  };
  const c_drawer__panel_xl_FlexBasis = exports.c_drawer__panel_xl_FlexBasis = {
    "name": "--pf-c-drawer__panel--xl--FlexBasis",
    "value": "28.125rem",
    "var": "var(--pf-c-drawer__panel--xl--FlexBasis)"
  };
  const c_drawer__panel_ZIndex = exports.c_drawer__panel_ZIndex = {
    "name": "--pf-c-drawer__panel--ZIndex",
    "value": "200",
    "var": "var(--pf-c-drawer__panel--ZIndex)"
  };
  const c_drawer__panel_BackgroundColor = exports.c_drawer__panel_BackgroundColor = {
    "name": "--pf-c-drawer__panel--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-drawer__panel--BackgroundColor)"
  };
  const c_drawer__panel_TransitionDuration = exports.c_drawer__panel_TransitionDuration = {
    "name": "--pf-c-drawer__panel--TransitionDuration",
    "value": ".25s",
    "var": "var(--pf-c-drawer__panel--TransitionDuration)"
  };
  const c_drawer__panel_TransitionProperty = exports.c_drawer__panel_TransitionProperty = {
    "name": "--pf-c-drawer__panel--TransitionProperty",
    "value": "margin,transform,box-shadow",
    "var": "var(--pf-c-drawer__panel--TransitionProperty)"
  };
  const c_drawer_child_PaddingTop = exports.c_drawer_child_PaddingTop = {
    "name": "--pf-c-drawer--child--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-drawer--child--PaddingTop)"
  };
  const c_drawer_child_PaddingRight = exports.c_drawer_child_PaddingRight = {
    "name": "--pf-c-drawer--child--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-drawer--child--PaddingRight)"
  };
  const c_drawer_child_PaddingBottom = exports.c_drawer_child_PaddingBottom = {
    "name": "--pf-c-drawer--child--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-drawer--child--PaddingBottom)"
  };
  const c_drawer_child_PaddingLeft = exports.c_drawer_child_PaddingLeft = {
    "name": "--pf-c-drawer--child--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-drawer--child--PaddingLeft)"
  };
  const c_drawer_child_md_PaddingTop = exports.c_drawer_child_md_PaddingTop = {
    "name": "--pf-c-drawer--child--md--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-drawer--child--md--PaddingTop)"
  };
  const c_drawer_child_md_PaddingRight = exports.c_drawer_child_md_PaddingRight = {
    "name": "--pf-c-drawer--child--md--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-drawer--child--md--PaddingRight)"
  };
  const c_drawer_child_md_PaddingBottom = exports.c_drawer_child_md_PaddingBottom = {
    "name": "--pf-c-drawer--child--md--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-drawer--child--md--PaddingBottom)"
  };
  const c_drawer_child_md_PaddingLeft = exports.c_drawer_child_md_PaddingLeft = {
    "name": "--pf-c-drawer--child--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-drawer--child--md--PaddingLeft)"
  };
  const c_drawer_child_m_padding_PaddingTop = exports.c_drawer_child_m_padding_PaddingTop = {
    "name": "--pf-c-drawer--child--m-padding--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-drawer--child--m-padding--PaddingTop)"
  };
  const c_drawer_child_m_padding_PaddingRight = exports.c_drawer_child_m_padding_PaddingRight = {
    "name": "--pf-c-drawer--child--m-padding--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-drawer--child--m-padding--PaddingRight)"
  };
  const c_drawer_child_m_padding_PaddingBottom = exports.c_drawer_child_m_padding_PaddingBottom = {
    "name": "--pf-c-drawer--child--m-padding--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-drawer--child--m-padding--PaddingBottom)"
  };
  const c_drawer_child_m_padding_PaddingLeft = exports.c_drawer_child_m_padding_PaddingLeft = {
    "name": "--pf-c-drawer--child--m-padding--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-drawer--child--m-padding--PaddingLeft)"
  };
  const c_drawer_child_m_padding_md_PaddingTop = exports.c_drawer_child_m_padding_md_PaddingTop = {
    "name": "--pf-c-drawer--child--m-padding--md--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-drawer--child--m-padding--md--PaddingTop)"
  };
  const c_drawer_child_m_padding_md_PaddingRight = exports.c_drawer_child_m_padding_md_PaddingRight = {
    "name": "--pf-c-drawer--child--m-padding--md--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-drawer--child--m-padding--md--PaddingRight)"
  };
  const c_drawer_child_m_padding_md_PaddingBottom = exports.c_drawer_child_m_padding_md_PaddingBottom = {
    "name": "--pf-c-drawer--child--m-padding--md--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-drawer--child--m-padding--md--PaddingBottom)"
  };
  const c_drawer_child_m_padding_md_PaddingLeft = exports.c_drawer_child_m_padding_md_PaddingLeft = {
    "name": "--pf-c-drawer--child--m-padding--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-drawer--child--m-padding--md--PaddingLeft)"
  };
  const c_drawer__content_child_PaddingTop = exports.c_drawer__content_child_PaddingTop = {
    "name": "--pf-c-drawer__content--child--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-drawer__content--child--PaddingTop)"
  };
  const c_drawer__content_child_PaddingRight = exports.c_drawer__content_child_PaddingRight = {
    "name": "--pf-c-drawer__content--child--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-drawer__content--child--PaddingRight)"
  };
  const c_drawer__content_child_PaddingBottom = exports.c_drawer__content_child_PaddingBottom = {
    "name": "--pf-c-drawer__content--child--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-drawer__content--child--PaddingBottom)"
  };
  const c_drawer__content_child_PaddingLeft = exports.c_drawer__content_child_PaddingLeft = {
    "name": "--pf-c-drawer__content--child--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-drawer__content--child--PaddingLeft)"
  };
  const c_drawer__actions_MarginTop = exports.c_drawer__actions_MarginTop = {
    "name": "--pf-c-drawer__actions--MarginTop",
    "value": "0",
    "var": "var(--pf-c-drawer__actions--MarginTop)"
  };
  const c_drawer__actions_MarginRight = exports.c_drawer__actions_MarginRight = {
    "name": "--pf-c-drawer__actions--MarginRight",
    "value": "0",
    "var": "var(--pf-c-drawer__actions--MarginRight)"
  };
  const c_drawer__panel_BoxShadow = exports.c_drawer__panel_BoxShadow = {
    "name": "--pf-c-drawer__panel--BoxShadow",
    "value": "none",
    "var": "var(--pf-c-drawer__panel--BoxShadow)"
  };
  const c_drawer_m_expanded__panel_BoxShadow = exports.c_drawer_m_expanded__panel_BoxShadow = {
    "name": "--pf-c-drawer--m-expanded__panel--BoxShadow",
    "value": "-0.75rem 0 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-c-drawer--m-expanded__panel--BoxShadow)"
  };
  const c_drawer_m_expanded_m_panel_left__panel_BoxShadow = exports.c_drawer_m_expanded_m_panel_left__panel_BoxShadow = {
    "name": "--pf-c-drawer--m-expanded--m-panel-left__panel--BoxShadow",
    "value": "0.75rem 0 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-c-drawer--m-expanded--m-panel-left__panel--BoxShadow)"
  };
  const c_drawer__panel_after_Width = exports.c_drawer__panel_after_Width = {
    "name": "--pf-c-drawer__panel--after--Width",
    "value": "1px",
    "var": "var(--pf-c-drawer__panel--after--Width)"
  };
  const c_drawer__panel_after_BackgroundColor = exports.c_drawer__panel_after_BackgroundColor = {
    "name": "--pf-c-drawer__panel--after--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-drawer__panel--after--BackgroundColor)"
  };
  const c_drawer_m_inline_m_expanded__panel_after_BackgroundColor = exports.c_drawer_m_inline_m_expanded__panel_after_BackgroundColor = {
    "name": "--pf-c-drawer--m-inline--m-expanded__panel--after--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-drawer--m-inline--m-expanded__panel--after--BackgroundColor)"
  };
  const c_dropdown__toggle_PaddingTop = exports.c_dropdown__toggle_PaddingTop = {
    "name": "--pf-c-dropdown__toggle--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-dropdown__toggle--PaddingTop)"
  };
  const c_dropdown__toggle_PaddingRight = exports.c_dropdown__toggle_PaddingRight = {
    "name": "--pf-c-dropdown__toggle--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--PaddingRight)"
  };
  const c_dropdown__toggle_PaddingBottom = exports.c_dropdown__toggle_PaddingBottom = {
    "name": "--pf-c-dropdown__toggle--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-dropdown__toggle--PaddingBottom)"
  };
  const c_dropdown__toggle_PaddingLeft = exports.c_dropdown__toggle_PaddingLeft = {
    "name": "--pf-c-dropdown__toggle--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--PaddingLeft)"
  };
  const c_dropdown__toggle_MinWidth = exports.c_dropdown__toggle_MinWidth = {
    "name": "--pf-c-dropdown__toggle--MinWidth",
    "value": "44px",
    "var": "var(--pf-c-dropdown__toggle--MinWidth)"
  };
  const c_dropdown__toggle_FontSize = exports.c_dropdown__toggle_FontSize = {
    "name": "--pf-c-dropdown__toggle--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-dropdown__toggle--FontSize)"
  };
  const c_dropdown__toggle_FontWeight = exports.c_dropdown__toggle_FontWeight = {
    "name": "--pf-c-dropdown__toggle--FontWeight",
    "value": "400",
    "var": "var(--pf-c-dropdown__toggle--FontWeight)"
  };
  const c_dropdown__toggle_Color = exports.c_dropdown__toggle_Color = {
    "name": "--pf-c-dropdown__toggle--Color",
    "value": "#fff",
    "var": "var(--pf-c-dropdown__toggle--Color)"
  };
  const c_dropdown__toggle_LineHeight = exports.c_dropdown__toggle_LineHeight = {
    "name": "--pf-c-dropdown__toggle--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-dropdown__toggle--LineHeight)"
  };
  const c_dropdown__toggle_BackgroundColor = exports.c_dropdown__toggle_BackgroundColor = {
    "name": "--pf-c-dropdown__toggle--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-dropdown__toggle--BackgroundColor)"
  };
  const c_dropdown__toggle_BorderWidth = exports.c_dropdown__toggle_BorderWidth = {
    "name": "--pf-c-dropdown__toggle--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-dropdown__toggle--BorderWidth)"
  };
  const c_dropdown__toggle_BorderTopColor = exports.c_dropdown__toggle_BorderTopColor = {
    "name": "--pf-c-dropdown__toggle--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-dropdown__toggle--BorderTopColor)"
  };
  const c_dropdown__toggle_BorderRightColor = exports.c_dropdown__toggle_BorderRightColor = {
    "name": "--pf-c-dropdown__toggle--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-dropdown__toggle--BorderRightColor)"
  };
  const c_dropdown__toggle_BorderBottomColor = exports.c_dropdown__toggle_BorderBottomColor = {
    "name": "--pf-c-dropdown__toggle--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-dropdown__toggle--BorderBottomColor)"
  };
  const c_dropdown__toggle_BorderLeftColor = exports.c_dropdown__toggle_BorderLeftColor = {
    "name": "--pf-c-dropdown__toggle--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-dropdown__toggle--BorderLeftColor)"
  };
  const c_dropdown__toggle_hover_BorderBottomColor = exports.c_dropdown__toggle_hover_BorderBottomColor = {
    "name": "--pf-c-dropdown__toggle--hover--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-dropdown__toggle--hover--BorderBottomColor)"
  };
  const c_dropdown__toggle_active_BorderBottomWidth = exports.c_dropdown__toggle_active_BorderBottomWidth = {
    "name": "--pf-c-dropdown__toggle--active--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-dropdown__toggle--active--BorderBottomWidth)"
  };
  const c_dropdown__toggle_active_BorderBottomColor = exports.c_dropdown__toggle_active_BorderBottomColor = {
    "name": "--pf-c-dropdown__toggle--active--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-dropdown__toggle--active--BorderBottomColor)"
  };
  const c_dropdown__toggle_focus_BorderBottomWidth = exports.c_dropdown__toggle_focus_BorderBottomWidth = {
    "name": "--pf-c-dropdown__toggle--focus--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-dropdown__toggle--focus--BorderBottomWidth)"
  };
  const c_dropdown__toggle_focus_BorderBottomColor = exports.c_dropdown__toggle_focus_BorderBottomColor = {
    "name": "--pf-c-dropdown__toggle--focus--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-dropdown__toggle--focus--BorderBottomColor)"
  };
  const c_dropdown__toggle_expanded_BorderBottomWidth = exports.c_dropdown__toggle_expanded_BorderBottomWidth = {
    "name": "--pf-c-dropdown__toggle--expanded--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-dropdown__toggle--expanded--BorderBottomWidth)"
  };
  const c_dropdown__toggle_expanded_BorderBottomColor = exports.c_dropdown__toggle_expanded_BorderBottomColor = {
    "name": "--pf-c-dropdown__toggle--expanded--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-dropdown__toggle--expanded--BorderBottomColor)"
  };
  const c_dropdown__toggle_disabled_BackgroundColor = exports.c_dropdown__toggle_disabled_BackgroundColor = {
    "name": "--pf-c-dropdown__toggle--disabled--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-dropdown__toggle--disabled--BackgroundColor)"
  };
  const c_dropdown__toggle_m_plain_BorderColor = exports.c_dropdown__toggle_m_plain_BorderColor = {
    "name": "--pf-c-dropdown__toggle--m-plain--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-dropdown__toggle--m-plain--BorderColor)"
  };
  const c_dropdown__toggle_m_plain_Color = exports.c_dropdown__toggle_m_plain_Color = {
    "name": "--pf-c-dropdown__toggle--m-plain--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-dropdown__toggle--m-plain--Color)"
  };
  const c_dropdown__toggle_m_plain_hover_Color = exports.c_dropdown__toggle_m_plain_hover_Color = {
    "name": "--pf-c-dropdown__toggle--m-plain--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-dropdown__toggle--m-plain--hover--Color)"
  };
  const c_dropdown__toggle_m_plain_disabled_Color = exports.c_dropdown__toggle_m_plain_disabled_Color = {
    "name": "--pf-c-dropdown__toggle--m-plain--disabled--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-dropdown__toggle--m-plain--disabled--Color)"
  };
  const c_dropdown__toggle_m_primary_Color = exports.c_dropdown__toggle_m_primary_Color = {
    "name": "--pf-c-dropdown__toggle--m-primary--Color",
    "value": "#fff",
    "var": "var(--pf-c-dropdown__toggle--m-primary--Color)"
  };
  const c_dropdown__toggle_m_primary_BackgroundColor = exports.c_dropdown__toggle_m_primary_BackgroundColor = {
    "name": "--pf-c-dropdown__toggle--m-primary--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-dropdown__toggle--m-primary--BackgroundColor)"
  };
  const c_dropdown__toggle_m_primary_hover_BackgroundColor = exports.c_dropdown__toggle_m_primary_hover_BackgroundColor = {
    "name": "--pf-c-dropdown__toggle--m-primary--hover--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-dropdown__toggle--m-primary--hover--BackgroundColor)"
  };
  const c_dropdown__toggle_m_primary_active_BackgroundColor = exports.c_dropdown__toggle_m_primary_active_BackgroundColor = {
    "name": "--pf-c-dropdown__toggle--m-primary--active--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-dropdown__toggle--m-primary--active--BackgroundColor)"
  };
  const c_dropdown__toggle_m_primary_focus_BackgroundColor = exports.c_dropdown__toggle_m_primary_focus_BackgroundColor = {
    "name": "--pf-c-dropdown__toggle--m-primary--focus--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-dropdown__toggle--m-primary--focus--BackgroundColor)"
  };
  const c_dropdown_m_expanded__toggle_m_primary_BackgroundColor = exports.c_dropdown_m_expanded__toggle_m_primary_BackgroundColor = {
    "name": "--pf-c-dropdown--m-expanded__toggle--m-primary--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-dropdown--m-expanded__toggle--m-primary--BackgroundColor)"
  };
  const c_dropdown__toggle_button_Color = exports.c_dropdown__toggle_button_Color = {
    "name": "--pf-c-dropdown__toggle-button--Color",
    "value": "#151515",
    "var": "var(--pf-c-dropdown__toggle-button--Color)"
  };
  const c_dropdown__toggle_m_split_button_child_PaddingTop = exports.c_dropdown__toggle_m_split_button_child_PaddingTop = {
    "name": "--pf-c-dropdown__toggle--m-split-button--child--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--child--PaddingTop)"
  };
  const c_dropdown__toggle_m_split_button_child_PaddingRight = exports.c_dropdown__toggle_m_split_button_child_PaddingRight = {
    "name": "--pf-c-dropdown__toggle--m-split-button--child--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--child--PaddingRight)"
  };
  const c_dropdown__toggle_m_split_button_child_PaddingBottom = exports.c_dropdown__toggle_m_split_button_child_PaddingBottom = {
    "name": "--pf-c-dropdown__toggle--m-split-button--child--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--child--PaddingBottom)"
  };
  const c_dropdown__toggle_m_split_button_child_PaddingLeft = exports.c_dropdown__toggle_m_split_button_child_PaddingLeft = {
    "name": "--pf-c-dropdown__toggle--m-split-button--child--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--child--PaddingLeft)"
  };
  const c_dropdown__toggle_m_split_button_child_BackgroundColor = exports.c_dropdown__toggle_m_split_button_child_BackgroundColor = {
    "name": "--pf-c-dropdown__toggle--m-split-button--child--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--child--BackgroundColor)"
  };
  const c_dropdown__toggle_m_split_button_first_child_PaddingLeft = exports.c_dropdown__toggle_m_split_button_first_child_PaddingLeft = {
    "name": "--pf-c-dropdown__toggle--m-split-button--first-child--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--first-child--PaddingLeft)"
  };
  const c_dropdown__toggle_m_split_button_last_child_PaddingRight = exports.c_dropdown__toggle_m_split_button_last_child_PaddingRight = {
    "name": "--pf-c-dropdown__toggle--m-split-button--last-child--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--last-child--PaddingRight)"
  };
  const c_dropdown__toggle_m_split_button_m_action_child_PaddingLeft = exports.c_dropdown__toggle_m_split_button_m_action_child_PaddingLeft = {
    "name": "--pf-c-dropdown__toggle--m-split-button--m-action--child--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--m-action--child--PaddingLeft)"
  };
  const c_dropdown__toggle_m_split_button_m_action_child_PaddingRight = exports.c_dropdown__toggle_m_split_button_m_action_child_PaddingRight = {
    "name": "--pf-c-dropdown__toggle--m-split-button--m-action--child--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--m-action--child--PaddingRight)"
  };
  const c_dropdown__toggle_m_split_button_m_action__toggle_button_MarginRight = exports.c_dropdown__toggle_m_split_button_m_action__toggle_button_MarginRight = {
    "name": "--pf-c-dropdown__toggle--m-split-button--m-action__toggle-button--MarginRight",
    "value": "0",
    "var": "var(--pf-c-dropdown__toggle--m-split-button--m-action__toggle-button--MarginRight)"
  };
  const c_dropdown__toggle_m_split_button__toggle_check__input_Transform = exports.c_dropdown__toggle_m_split_button__toggle_check__input_Transform = {
    "name": "--pf-c-dropdown__toggle--m-split-button__toggle-check__input--Transform",
    "value": "translateY(-0.0625rem)",
    "var": "var(--pf-c-dropdown__toggle--m-split-button__toggle-check__input--Transform)"
  };
  const c_dropdown__toggle_m_split_button__toggle_text_MarginLeft = exports.c_dropdown__toggle_m_split_button__toggle_text_MarginLeft = {
    "name": "--pf-c-dropdown__toggle--m-split-button__toggle-text--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle--m-split-button__toggle-text--MarginLeft)"
  };
  const c_dropdown__toggle_icon_MarginRight = exports.c_dropdown__toggle_icon_MarginRight = {
    "name": "--pf-c-dropdown__toggle-icon--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__toggle-icon--MarginRight)"
  };
  const c_dropdown__toggle_icon_MarginLeft = exports.c_dropdown__toggle_icon_MarginLeft = {
    "name": "--pf-c-dropdown__toggle-icon--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-dropdown__toggle-icon--MarginLeft)"
  };
  const c_dropdown_m_top_m_expanded__toggle_icon_Transform = exports.c_dropdown_m_top_m_expanded__toggle_icon_Transform = {
    "name": "--pf-c-dropdown--m-top--m-expanded__toggle-icon--Transform",
    "value": "rotate(180deg)",
    "var": "var(--pf-c-dropdown--m-top--m-expanded__toggle-icon--Transform)"
  };
  const c_dropdown__menu_BackgroundColor = exports.c_dropdown__menu_BackgroundColor = {
    "name": "--pf-c-dropdown__menu--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-dropdown__menu--BackgroundColor)"
  };
  const c_dropdown__menu_BorderWidth = exports.c_dropdown__menu_BorderWidth = {
    "name": "--pf-c-dropdown__menu--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-dropdown__menu--BorderWidth)"
  };
  const c_dropdown__menu_BoxShadow = exports.c_dropdown__menu_BoxShadow = {
    "name": "--pf-c-dropdown__menu--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-dropdown__menu--BoxShadow)"
  };
  const c_dropdown__menu_PaddingTop = exports.c_dropdown__menu_PaddingTop = {
    "name": "--pf-c-dropdown__menu--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__menu--PaddingTop)"
  };
  const c_dropdown__menu_PaddingBottom = exports.c_dropdown__menu_PaddingBottom = {
    "name": "--pf-c-dropdown__menu--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__menu--PaddingBottom)"
  };
  const c_dropdown__menu_Top = exports.c_dropdown__menu_Top = {
    "name": "--pf-c-dropdown__menu--Top",
    "value": "0",
    "var": "var(--pf-c-dropdown__menu--Top)"
  };
  const c_dropdown__menu_ZIndex = exports.c_dropdown__menu_ZIndex = {
    "name": "--pf-c-dropdown__menu--ZIndex",
    "value": "200",
    "var": "var(--pf-c-dropdown__menu--ZIndex)"
  };
  const c_dropdown_m_top__menu_Top = exports.c_dropdown_m_top__menu_Top = {
    "name": "--pf-c-dropdown--m-top__menu--Top",
    "value": "0",
    "var": "var(--pf-c-dropdown--m-top__menu--Top)"
  };
  const c_dropdown_m_top__menu_Transform = exports.c_dropdown_m_top__menu_Transform = {
    "name": "--pf-c-dropdown--m-top__menu--Transform",
    "value": "translateY(calc(-100% - 0.25rem))",
    "var": "var(--pf-c-dropdown--m-top__menu--Transform)"
  };
  const c_dropdown__menu_item_BackgroundColor = exports.c_dropdown__menu_item_BackgroundColor = {
    "name": "--pf-c-dropdown__menu-item--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-dropdown__menu-item--BackgroundColor)"
  };
  const c_dropdown__menu_item_PaddingTop = exports.c_dropdown__menu_item_PaddingTop = {
    "name": "--pf-c-dropdown__menu-item--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__menu-item--PaddingTop)"
  };
  const c_dropdown__menu_item_PaddingRight = exports.c_dropdown__menu_item_PaddingRight = {
    "name": "--pf-c-dropdown__menu-item--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-dropdown__menu-item--PaddingRight)"
  };
  const c_dropdown__menu_item_PaddingBottom = exports.c_dropdown__menu_item_PaddingBottom = {
    "name": "--pf-c-dropdown__menu-item--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__menu-item--PaddingBottom)"
  };
  const c_dropdown__menu_item_PaddingLeft = exports.c_dropdown__menu_item_PaddingLeft = {
    "name": "--pf-c-dropdown__menu-item--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-dropdown__menu-item--PaddingLeft)"
  };
  const c_dropdown__menu_item_FontSize = exports.c_dropdown__menu_item_FontSize = {
    "name": "--pf-c-dropdown__menu-item--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-dropdown__menu-item--FontSize)"
  };
  const c_dropdown__menu_item_FontWeight = exports.c_dropdown__menu_item_FontWeight = {
    "name": "--pf-c-dropdown__menu-item--FontWeight",
    "value": "400",
    "var": "var(--pf-c-dropdown__menu-item--FontWeight)"
  };
  const c_dropdown__menu_item_LineHeight = exports.c_dropdown__menu_item_LineHeight = {
    "name": "--pf-c-dropdown__menu-item--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-dropdown__menu-item--LineHeight)"
  };
  const c_dropdown__menu_item_Color = exports.c_dropdown__menu_item_Color = {
    "name": "--pf-c-dropdown__menu-item--Color",
    "value": "#737679",
    "var": "var(--pf-c-dropdown__menu-item--Color)"
  };
  const c_dropdown__menu_item_hover_Color = exports.c_dropdown__menu_item_hover_Color = {
    "name": "--pf-c-dropdown__menu-item--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-dropdown__menu-item--hover--Color)"
  };
  const c_dropdown__menu_item_disabled_Color = exports.c_dropdown__menu_item_disabled_Color = {
    "name": "--pf-c-dropdown__menu-item--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-dropdown__menu-item--disabled--Color)"
  };
  const c_dropdown__menu_item_hover_BackgroundColor = exports.c_dropdown__menu_item_hover_BackgroundColor = {
    "name": "--pf-c-dropdown__menu-item--hover--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-dropdown__menu-item--hover--BackgroundColor)"
  };
  const c_dropdown__menu_item_disabled_BackgroundColor = exports.c_dropdown__menu_item_disabled_BackgroundColor = {
    "name": "--pf-c-dropdown__menu-item--disabled--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-dropdown__menu-item--disabled--BackgroundColor)"
  };
  const c_dropdown__menu_item_icon_MarginRight = exports.c_dropdown__menu_item_icon_MarginRight = {
    "name": "--pf-c-dropdown__menu-item-icon--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__menu-item-icon--MarginRight)"
  };
  const c_dropdown__menu_item_icon_Width = exports.c_dropdown__menu_item_icon_Width = {
    "name": "--pf-c-dropdown__menu-item-icon--Width",
    "value": "1.5rem",
    "var": "var(--pf-c-dropdown__menu-item-icon--Width)"
  };
  const c_dropdown__menu_item_icon_Height = exports.c_dropdown__menu_item_icon_Height = {
    "name": "--pf-c-dropdown__menu-item-icon--Height",
    "value": "1.5rem",
    "var": "var(--pf-c-dropdown__menu-item-icon--Height)"
  };
  const c_dropdown__c_divider_MarginTop = exports.c_dropdown__c_divider_MarginTop = {
    "name": "--pf-c-dropdown__c-divider--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__c-divider--MarginTop)"
  };
  const c_dropdown__c_divider_MarginBottom = exports.c_dropdown__c_divider_MarginBottom = {
    "name": "--pf-c-dropdown__c-divider--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__c-divider--MarginBottom)"
  };
  const c_dropdown__separator_Height = exports.c_dropdown__separator_Height = {
    "name": "--pf-c-dropdown__separator--Height",
    "value": "1px",
    "var": "var(--pf-c-dropdown__separator--Height)"
  };
  const c_dropdown__separator_BackgroundColor = exports.c_dropdown__separator_BackgroundColor = {
    "name": "--pf-c-dropdown__separator--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-dropdown__separator--BackgroundColor)"
  };
  const c_dropdown__separator_MarginTop = exports.c_dropdown__separator_MarginTop = {
    "name": "--pf-c-dropdown__separator--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__separator--MarginTop)"
  };
  const c_dropdown__separator_MarginBottom = exports.c_dropdown__separator_MarginBottom = {
    "name": "--pf-c-dropdown__separator--MarginBottom",
    "value": "0",
    "var": "var(--pf-c-dropdown__separator--MarginBottom)"
  };
  const c_dropdown__group_PaddingTop = exports.c_dropdown__group_PaddingTop = {
    "name": "--pf-c-dropdown__group--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__group--PaddingTop)"
  };
  const c_dropdown__group_group_PaddingTop = exports.c_dropdown__group_group_PaddingTop = {
    "name": "--pf-c-dropdown__group--group--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__group--group--PaddingTop)"
  };
  const c_dropdown__group_first_child_PaddingTop = exports.c_dropdown__group_first_child_PaddingTop = {
    "name": "--pf-c-dropdown__group--first-child--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-dropdown__group--first-child--PaddingTop)"
  };
  const c_dropdown__group_title_PaddingTop = exports.c_dropdown__group_title_PaddingTop = {
    "name": "--pf-c-dropdown__group-title--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__group-title--PaddingTop)"
  };
  const c_dropdown__group_title_PaddingRight = exports.c_dropdown__group_title_PaddingRight = {
    "name": "--pf-c-dropdown__group-title--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-dropdown__group-title--PaddingRight)"
  };
  const c_dropdown__group_title_PaddingBottom = exports.c_dropdown__group_title_PaddingBottom = {
    "name": "--pf-c-dropdown__group-title--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-dropdown__group-title--PaddingBottom)"
  };
  const c_dropdown__group_title_PaddingLeft = exports.c_dropdown__group_title_PaddingLeft = {
    "name": "--pf-c-dropdown__group-title--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-dropdown__group-title--PaddingLeft)"
  };
  const c_dropdown__group_title_FontSize = exports.c_dropdown__group_title_FontSize = {
    "name": "--pf-c-dropdown__group-title--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-dropdown__group-title--FontSize)"
  };
  const c_dropdown__group_title_FontWeight = exports.c_dropdown__group_title_FontWeight = {
    "name": "--pf-c-dropdown__group-title--FontWeight",
    "value": "700",
    "var": "var(--pf-c-dropdown__group-title--FontWeight)"
  };
  const c_dropdown__group_title_Color = exports.c_dropdown__group_title_Color = {
    "name": "--pf-c-dropdown__group-title--Color",
    "value": "#737679",
    "var": "var(--pf-c-dropdown__group-title--Color)"
  };
  const c_empty_state_Padding = exports.c_empty_state_Padding = {
    "name": "--pf-c-empty-state--Padding",
    "value": "2rem",
    "var": "var(--pf-c-empty-state--Padding)"
  };
  const c_empty_state__icon_MarginBottom = exports.c_empty_state__icon_MarginBottom = {
    "name": "--pf-c-empty-state__icon--MarginBottom",
    "value": "2rem",
    "var": "var(--pf-c-empty-state__icon--MarginBottom)"
  };
  const c_empty_state__icon_FontSize = exports.c_empty_state__icon_FontSize = {
    "name": "--pf-c-empty-state__icon--FontSize",
    "value": "6.25rem",
    "var": "var(--pf-c-empty-state__icon--FontSize)"
  };
  const c_empty_state__icon_Color = exports.c_empty_state__icon_Color = {
    "name": "--pf-c-empty-state__icon--Color",
    "value": "#737679",
    "var": "var(--pf-c-empty-state__icon--Color)"
  };
  const c_empty_state__body_MarginTop = exports.c_empty_state__body_MarginTop = {
    "name": "--pf-c-empty-state__body--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-empty-state__body--MarginTop)"
  };
  const c_empty_state__body_Color = exports.c_empty_state__body_Color = {
    "name": "--pf-c-empty-state__body--Color",
    "value": "#737679",
    "var": "var(--pf-c-empty-state__body--Color)"
  };
  const c_empty_state_c_button_MarginTop = exports.c_empty_state_c_button_MarginTop = {
    "name": "--pf-c-empty-state--c-button--MarginTop",
    "value": "2rem",
    "var": "var(--pf-c-empty-state--c-button--MarginTop)"
  };
  const c_empty_state_c_button__secondary_MarginTop = exports.c_empty_state_c_button__secondary_MarginTop = {
    "name": "--pf-c-empty-state--c-button__secondary--MarginTop",
    "value": "1rem",
    "var": "var(--pf-c-empty-state--c-button__secondary--MarginTop)"
  };
  const c_empty_state__secondary_MarginTop = exports.c_empty_state__secondary_MarginTop = {
    "name": "--pf-c-empty-state__secondary--MarginTop",
    "value": "2rem",
    "var": "var(--pf-c-empty-state__secondary--MarginTop)"
  };
  const c_empty_state__secondary_MarginRight = exports.c_empty_state__secondary_MarginRight = {
    "name": "--pf-c-empty-state__secondary--MarginRight",
    "value": "calc(0.25rem*-1)",
    "var": "var(--pf-c-empty-state__secondary--MarginRight)"
  };
  const c_empty_state__secondary_MarginBottom = exports.c_empty_state__secondary_MarginBottom = {
    "name": "--pf-c-empty-state__secondary--MarginBottom",
    "value": "calc(0.25rem*-1)",
    "var": "var(--pf-c-empty-state__secondary--MarginBottom)"
  };
  const c_empty_state__secondary_c_button_MarginRight = exports.c_empty_state__secondary_c_button_MarginRight = {
    "name": "--pf-c-empty-state__secondary--c-button--MarginRight",
    "value": "0.25rem",
    "var": "var(--pf-c-empty-state__secondary--c-button--MarginRight)"
  };
  const c_empty_state__secondary_c_button_MarginBottom = exports.c_empty_state__secondary_c_button_MarginBottom = {
    "name": "--pf-c-empty-state__secondary--c-button--MarginBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-empty-state__secondary--c-button--MarginBottom)"
  };
  const c_empty_state_m_sm_MaxWidth = exports.c_empty_state_m_sm_MaxWidth = {
    "name": "--pf-c-empty-state--m-sm--MaxWidth",
    "value": "25rem",
    "var": "var(--pf-c-empty-state--m-sm--MaxWidth)"
  };
  const c_empty_state_m_lg_MaxWidth = exports.c_empty_state_m_lg_MaxWidth = {
    "name": "--pf-c-empty-state--m-lg--MaxWidth",
    "value": "37.5rem",
    "var": "var(--pf-c-empty-state--m-lg--MaxWidth)"
  };
  const c_empty_state_m_xl__body_FontSize = exports.c_empty_state_m_xl__body_FontSize = {
    "name": "--pf-c-empty-state--m-xl__body--FontSize",
    "value": "1.25rem",
    "var": "var(--pf-c-empty-state--m-xl__body--FontSize)"
  };
  const c_empty_state_m_xl__body_MarginTop = exports.c_empty_state_m_xl__body_MarginTop = {
    "name": "--pf-c-empty-state--m-xl__body--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-empty-state--m-xl__body--MarginTop)"
  };
  const c_empty_state_m_xl__icon_MarginBottom = exports.c_empty_state_m_xl__icon_MarginBottom = {
    "name": "--pf-c-empty-state--m-xl__icon--MarginBottom",
    "value": "2rem",
    "var": "var(--pf-c-empty-state--m-xl__icon--MarginBottom)"
  };
  const c_empty_state_m_xl__icon_FontSize = exports.c_empty_state_m_xl__icon_FontSize = {
    "name": "--pf-c-empty-state--m-xl__icon--FontSize",
    "value": "6.25rem",
    "var": "var(--pf-c-empty-state--m-xl__icon--FontSize)"
  };
  const c_empty_state_m_xl_c_button__secondary_MarginTop = exports.c_empty_state_m_xl_c_button__secondary_MarginTop = {
    "name": "--pf-c-empty-state--m-xl--c-button__secondary--MarginTop",
    "value": "1rem",
    "var": "var(--pf-c-empty-state--m-xl--c-button__secondary--MarginTop)"
  };
  const c_expandable__toggle_PaddingTop = exports.c_expandable__toggle_PaddingTop = {
    "name": "--pf-c-expandable__toggle--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-expandable__toggle--PaddingTop)"
  };
  const c_expandable__toggle_PaddingRight = exports.c_expandable__toggle_PaddingRight = {
    "name": "--pf-c-expandable__toggle--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-expandable__toggle--PaddingRight)"
  };
  const c_expandable__toggle_PaddingBottom = exports.c_expandable__toggle_PaddingBottom = {
    "name": "--pf-c-expandable__toggle--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-expandable__toggle--PaddingBottom)"
  };
  const c_expandable__toggle_PaddingLeft = exports.c_expandable__toggle_PaddingLeft = {
    "name": "--pf-c-expandable__toggle--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-expandable__toggle--PaddingLeft)"
  };
  const c_expandable__toggle_FontWeight = exports.c_expandable__toggle_FontWeight = {
    "name": "--pf-c-expandable__toggle--FontWeight",
    "value": "400",
    "var": "var(--pf-c-expandable__toggle--FontWeight)"
  };
  const c_expandable__toggle_Color = exports.c_expandable__toggle_Color = {
    "name": "--pf-c-expandable__toggle--Color",
    "value": "#004080",
    "var": "var(--pf-c-expandable__toggle--Color)"
  };
  const c_expandable__toggle_hover_Color = exports.c_expandable__toggle_hover_Color = {
    "name": "--pf-c-expandable__toggle--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-expandable__toggle--hover--Color)"
  };
  const c_expandable__toggle_active_Color = exports.c_expandable__toggle_active_Color = {
    "name": "--pf-c-expandable__toggle--active--Color",
    "value": "#004080",
    "var": "var(--pf-c-expandable__toggle--active--Color)"
  };
  const c_expandable__toggle_focus_Color = exports.c_expandable__toggle_focus_Color = {
    "name": "--pf-c-expandable__toggle--focus--Color",
    "value": "#004080",
    "var": "var(--pf-c-expandable__toggle--focus--Color)"
  };
  const c_expandable__toggle_m_expanded_Color = exports.c_expandable__toggle_m_expanded_Color = {
    "name": "--pf-c-expandable__toggle--m-expanded--Color",
    "value": "#004080",
    "var": "var(--pf-c-expandable__toggle--m-expanded--Color)"
  };
  const c_expandable__toggle_icon_Transition = exports.c_expandable__toggle_icon_Transition = {
    "name": "--pf-c-expandable__toggle-icon--Transition",
    "value": ".2s ease-in 0s",
    "var": "var(--pf-c-expandable__toggle-icon--Transition)"
  };
  const c_expandable_m_expanded__toggle_icon_Transform = exports.c_expandable_m_expanded__toggle_icon_Transform = {
    "name": "--pf-c-expandable--m-expanded__toggle-icon--Transform",
    "value": "rotate(90deg)",
    "var": "var(--pf-c-expandable--m-expanded__toggle-icon--Transform)"
  };
  const c_expandable__toggle_icon_MarginRight = exports.c_expandable__toggle_icon_MarginRight = {
    "name": "--pf-c-expandable__toggle-icon--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-expandable__toggle-icon--MarginRight)"
  };
  const c_expandable__toggle_icon_Color = exports.c_expandable__toggle_icon_Color = {
    "name": "--pf-c-expandable__toggle-icon--Color",
    "value": "#151515",
    "var": "var(--pf-c-expandable__toggle-icon--Color)"
  };
  const c_expandable__content_MarginTop = exports.c_expandable__content_MarginTop = {
    "name": "--pf-c-expandable__content--MarginTop",
    "value": "1rem",
    "var": "var(--pf-c-expandable__content--MarginTop)"
  };
  const c_file_upload_m_loading__file_details_before_BackgroundColor = exports.c_file_upload_m_loading__file_details_before_BackgroundColor = {
    "name": "--pf-c-file-upload--m-loading__file-details--before--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-file-upload--m-loading__file-details--before--BackgroundColor)"
  };
  const c_file_upload_m_loading__file_details_before_Left = exports.c_file_upload_m_loading__file_details_before_Left = {
    "name": "--pf-c-file-upload--m-loading__file-details--before--Left",
    "value": "1px",
    "var": "var(--pf-c-file-upload--m-loading__file-details--before--Left)"
  };
  const c_file_upload_m_loading__file_details_before_Right = exports.c_file_upload_m_loading__file_details_before_Right = {
    "name": "--pf-c-file-upload--m-loading__file-details--before--Right",
    "value": "1px",
    "var": "var(--pf-c-file-upload--m-loading__file-details--before--Right)"
  };
  const c_file_upload_m_loading__file_details_before_Bottom = exports.c_file_upload_m_loading__file_details_before_Bottom = {
    "name": "--pf-c-file-upload--m-loading__file-details--before--Bottom",
    "value": "1px",
    "var": "var(--pf-c-file-upload--m-loading__file-details--before--Bottom)"
  };
  const c_file_upload_m_drag_hover_before_BorderWidth = exports.c_file_upload_m_drag_hover_before_BorderWidth = {
    "name": "--pf-c-file-upload--m-drag-hover--before--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-file-upload--m-drag-hover--before--BorderWidth)"
  };
  const c_file_upload_m_drag_hover_before_BorderColor = exports.c_file_upload_m_drag_hover_before_BorderColor = {
    "name": "--pf-c-file-upload--m-drag-hover--before--BorderColor",
    "value": "#06c",
    "var": "var(--pf-c-file-upload--m-drag-hover--before--BorderColor)"
  };
  const c_file_upload_m_drag_hover_before_ZIndex = exports.c_file_upload_m_drag_hover_before_ZIndex = {
    "name": "--pf-c-file-upload--m-drag-hover--before--ZIndex",
    "value": "100",
    "var": "var(--pf-c-file-upload--m-drag-hover--before--ZIndex)"
  };
  const c_file_upload_m_drag_hover_after_BackgroundColor = exports.c_file_upload_m_drag_hover_after_BackgroundColor = {
    "name": "--pf-c-file-upload--m-drag-hover--after--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-file-upload--m-drag-hover--after--BackgroundColor)"
  };
  const c_file_upload_m_drag_hover_after_Opacity = exports.c_file_upload_m_drag_hover_after_Opacity = {
    "name": "--pf-c-file-upload--m-drag-hover--after--Opacity",
    "value": ".1",
    "var": "var(--pf-c-file-upload--m-drag-hover--after--Opacity)"
  };
  const c_file_upload__file_details__c_form_control_MinHeight = exports.c_file_upload__file_details__c_form_control_MinHeight = {
    "name": "--pf-c-file-upload__file-details__c-form-control--MinHeight",
    "value": "calc(4rem*2)",
    "var": "var(--pf-c-file-upload__file-details__c-form-control--MinHeight)"
  };
  const c_file_upload__file_select__c_button_m_control_disabled_BackgroundColor = exports.c_file_upload__file_select__c_button_m_control_disabled_BackgroundColor = {
    "name": "--pf-c-file-upload__file-select__c-button--m-control--disabled--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-file-upload__file-select__c-button--m-control--disabled--BackgroundColor)"
  };
  const c_file_upload__file_select__c_button_m_control_disabled_after_BorderTopColor = exports.c_file_upload__file_select__c_button_m_control_disabled_after_BorderTopColor = {
    "name": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderTopColor)"
  };
  const c_file_upload__file_select__c_button_m_control_disabled_after_BorderRightColor = exports.c_file_upload__file_select__c_button_m_control_disabled_after_BorderRightColor = {
    "name": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderRightColor)"
  };
  const c_file_upload__file_select__c_button_m_control_disabled_after_BorderBottomColor = exports.c_file_upload__file_select__c_button_m_control_disabled_after_BorderBottomColor = {
    "name": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderBottomColor",
    "value": "#8a8d90",
    "var": "var(--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderBottomColor)"
  };
  const c_file_upload__file_select__c_button_m_control_disabled_after_BorderLeftColor = exports.c_file_upload__file_select__c_button_m_control_disabled_after_BorderLeftColor = {
    "name": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderLeftColor)"
  };
  const c_file_upload__file_select__c_button_m_control_disabled_after_BorderWidth = exports.c_file_upload__file_select__c_button_m_control_disabled_after_BorderWidth = {
    "name": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderWidth)"
  };
  const c_file_upload__file_select__c_button_m_control_OutlineOffset = exports.c_file_upload__file_select__c_button_m_control_OutlineOffset = {
    "name": "--pf-c-file-upload__file-select__c-button--m-control--OutlineOffset",
    "value": "calc(-1*0.25rem)",
    "var": "var(--pf-c-file-upload__file-select__c-button--m-control--OutlineOffset)"
  };
  const c_form_GridGap = exports.c_form_GridGap = {
    "name": "--pf-c-form--GridGap",
    "value": "1.5rem",
    "var": "var(--pf-c-form--GridGap)"
  };
  const c_form__label_Color = exports.c_form__label_Color = {
    "name": "--pf-c-form__label--Color",
    "value": "#737679",
    "var": "var(--pf-c-form__label--Color)"
  };
  const c_form__label_FontWeight = exports.c_form__label_FontWeight = {
    "name": "--pf-c-form__label--FontWeight",
    "value": "400",
    "var": "var(--pf-c-form__label--FontWeight)"
  };
  const c_form__label_FontSize = exports.c_form__label_FontSize = {
    "name": "--pf-c-form__label--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-form__label--FontSize)"
  };
  const c_form__label_LineHeight = exports.c_form__label_LineHeight = {
    "name": "--pf-c-form__label--LineHeight",
    "value": "1.3",
    "var": "var(--pf-c-form__label--LineHeight)"
  };
  const c_form__label_PaddingTop = exports.c_form__label_PaddingTop = {
    "name": "--pf-c-form__label--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-form__label--PaddingTop)"
  };
  const c_form__label_PaddingBottom = exports.c_form__label_PaddingBottom = {
    "name": "--pf-c-form__label--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-form__label--PaddingBottom)"
  };
  const c_form__label_m_disabled_Color = exports.c_form__label_m_disabled_Color = {
    "name": "--pf-c-form__label--m-disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-form__label--m-disabled--Color)"
  };
  const c_form__label_text_FontWeight = exports.c_form__label_text_FontWeight = {
    "name": "--pf-c-form__label-text--FontWeight",
    "value": "700",
    "var": "var(--pf-c-form__label-text--FontWeight)"
  };
  const c_form__label_required_MarginLeft = exports.c_form__label_required_MarginLeft = {
    "name": "--pf-c-form__label-required--MarginLeft",
    "value": "0.25rem",
    "var": "var(--pf-c-form__label-required--MarginLeft)"
  };
  const c_form__label_required_FontSize = exports.c_form__label_required_FontSize = {
    "name": "--pf-c-form__label-required--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-form__label-required--FontSize)"
  };
  const c_form__label_required_Color = exports.c_form__label_required_Color = {
    "name": "--pf-c-form__label-required--Color",
    "value": "#c9190b",
    "var": "var(--pf-c-form__label-required--Color)"
  };
  const c_form__group_MarginLeft = exports.c_form__group_MarginLeft = {
    "name": "--pf-c-form__group--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-form__group--MarginLeft)"
  };
  const c_form_m_horizontal_md__group_GridTemplateColumns = exports.c_form_m_horizontal_md__group_GridTemplateColumns = {
    "name": "--pf-c-form--m-horizontal--md__group--GridTemplateColumns",
    "value": "150px 1fr",
    "var": "var(--pf-c-form--m-horizontal--md__group--GridTemplateColumns)"
  };
  const c_form__group_m_action_MarginTop = exports.c_form__group_m_action_MarginTop = {
    "name": "--pf-c-form__group--m-action--MarginTop",
    "value": "2rem",
    "var": "var(--pf-c-form__group--m-action--MarginTop)"
  };
  const c_form__actions_child_MarginTop = exports.c_form__actions_child_MarginTop = {
    "name": "--pf-c-form__actions--child--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-form__actions--child--MarginTop)"
  };
  const c_form__actions_child_MarginRight = exports.c_form__actions_child_MarginRight = {
    "name": "--pf-c-form__actions--child--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-form__actions--child--MarginRight)"
  };
  const c_form__actions_child_MarginBottom = exports.c_form__actions_child_MarginBottom = {
    "name": "--pf-c-form__actions--child--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-form__actions--child--MarginBottom)"
  };
  const c_form__actions_child_MarginLeft = exports.c_form__actions_child_MarginLeft = {
    "name": "--pf-c-form__actions--child--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-form__actions--child--MarginLeft)"
  };
  const c_form__actions_MarginTop = exports.c_form__actions_MarginTop = {
    "name": "--pf-c-form__actions--MarginTop",
    "value": "calc(0.5rem*-1)",
    "var": "var(--pf-c-form__actions--MarginTop)"
  };
  const c_form__actions_MarginRight = exports.c_form__actions_MarginRight = {
    "name": "--pf-c-form__actions--MarginRight",
    "value": "calc(0.5rem*-1)",
    "var": "var(--pf-c-form__actions--MarginRight)"
  };
  const c_form__actions_MarginBottom = exports.c_form__actions_MarginBottom = {
    "name": "--pf-c-form__actions--MarginBottom",
    "value": "calc(0.5rem*-1)",
    "var": "var(--pf-c-form__actions--MarginBottom)"
  };
  const c_form__actions_MarginLeft = exports.c_form__actions_MarginLeft = {
    "name": "--pf-c-form__actions--MarginLeft",
    "value": "calc(0.5rem*-1)",
    "var": "var(--pf-c-form__actions--MarginLeft)"
  };
  const c_form__helper_text_MarginTop = exports.c_form__helper_text_MarginTop = {
    "name": "--pf-c-form__helper-text--MarginTop",
    "value": "0.25rem",
    "var": "var(--pf-c-form__helper-text--MarginTop)"
  };
  const c_form__helper_text_FontSize = exports.c_form__helper_text_FontSize = {
    "name": "--pf-c-form__helper-text--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-form__helper-text--FontSize)"
  };
  const c_form__helper_text_Color = exports.c_form__helper_text_Color = {
    "name": "--pf-c-form__helper-text--Color",
    "value": "#486b00",
    "var": "var(--pf-c-form__helper-text--Color)"
  };
  const c_form_m_inline_MarginRight = exports.c_form_m_inline_MarginRight = {
    "name": "--pf-c-form--m-inline--MarginRight",
    "value": "1.5rem",
    "var": "var(--pf-c-form--m-inline--MarginRight)"
  };
  const c_form_m_error_Color = exports.c_form_m_error_Color = {
    "name": "--pf-c-form--m-error--Color",
    "value": "#c9190b",
    "var": "var(--pf-c-form--m-error--Color)"
  };
  const c_form_m_success_Color = exports.c_form_m_success_Color = {
    "name": "--pf-c-form--m-success--Color",
    "value": "#486b00",
    "var": "var(--pf-c-form--m-success--Color)"
  };
  const c_form_control_FontSize = exports.c_form_control_FontSize = {
    "name": "--pf-c-form-control--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-form-control--FontSize)"
  };
  const c_form_control_LineHeight = exports.c_form_control_LineHeight = {
    "name": "--pf-c-form-control--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-form-control--LineHeight)"
  };
  const c_form_control_BorderWidth = exports.c_form_control_BorderWidth = {
    "name": "--pf-c-form-control--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-form-control--BorderWidth)"
  };
  const c_form_control_BorderTopColor = exports.c_form_control_BorderTopColor = {
    "name": "--pf-c-form-control--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-form-control--BorderTopColor)"
  };
  const c_form_control_BorderRightColor = exports.c_form_control_BorderRightColor = {
    "name": "--pf-c-form-control--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-form-control--BorderRightColor)"
  };
  const c_form_control_BorderBottomColor = exports.c_form_control_BorderBottomColor = {
    "name": "--pf-c-form-control--BorderBottomColor",
    "value": "#92d400",
    "var": "var(--pf-c-form-control--BorderBottomColor)"
  };
  const c_form_control_BorderLeftColor = exports.c_form_control_BorderLeftColor = {
    "name": "--pf-c-form-control--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-form-control--BorderLeftColor)"
  };
  const c_form_control_BorderRadius = exports.c_form_control_BorderRadius = {
    "name": "--pf-c-form-control--BorderRadius",
    "value": "0",
    "var": "var(--pf-c-form-control--BorderRadius)"
  };
  const c_form_control_BackgroundColor = exports.c_form_control_BackgroundColor = {
    "name": "--pf-c-form-control--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-form-control--BackgroundColor)"
  };
  const c_form_control_Height = exports.c_form_control_Height = {
    "name": "--pf-c-form-control--Height",
    "value": "calc(1rem*1.5 + 1px*2 + calc(0.375rem - 1px) + calc(0.375rem - 1px))",
    "var": "var(--pf-c-form-control--Height)"
  };
  const c_form_control_PaddingTop = exports.c_form_control_PaddingTop = {
    "name": "--pf-c-form-control--PaddingTop",
    "value": "calc(0.375rem - 1px)",
    "var": "var(--pf-c-form-control--PaddingTop)"
  };
  const c_form_control_PaddingBottom = exports.c_form_control_PaddingBottom = {
    "name": "--pf-c-form-control--PaddingBottom",
    "value": "calc(0.375rem - 1px)",
    "var": "var(--pf-c-form-control--PaddingBottom)"
  };
  const c_form_control_PaddingRight = exports.c_form_control_PaddingRight = {
    "name": "--pf-c-form-control--PaddingRight",
    "value": "calc(0.5rem + 3rem)",
    "var": "var(--pf-c-form-control--PaddingRight)"
  };
  const c_form_control_PaddingLeft = exports.c_form_control_PaddingLeft = {
    "name": "--pf-c-form-control--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-form-control--PaddingLeft)"
  };
  const c_form_control_hover_BorderBottomColor = exports.c_form_control_hover_BorderBottomColor = {
    "name": "--pf-c-form-control--hover--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-form-control--hover--BorderBottomColor)"
  };
  const c_form_control_focus_BorderBottomWidth = exports.c_form_control_focus_BorderBottomWidth = {
    "name": "--pf-c-form-control--focus--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-form-control--focus--BorderBottomWidth)"
  };
  const c_form_control_focus_PaddingBottom = exports.c_form_control_focus_PaddingBottom = {
    "name": "--pf-c-form-control--focus--PaddingBottom",
    "value": "calc(0.375rem - 2px)",
    "var": "var(--pf-c-form-control--focus--PaddingBottom)"
  };
  const c_form_control_focus_BorderBottomColor = exports.c_form_control_focus_BorderBottomColor = {
    "name": "--pf-c-form-control--focus--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-form-control--focus--BorderBottomColor)"
  };
  const c_form_control_placeholder_Color = exports.c_form_control_placeholder_Color = {
    "name": "--pf-c-form-control--placeholder--Color",
    "value": "#737679",
    "var": "var(--pf-c-form-control--placeholder--Color)"
  };
  const c_form_control_disabled_Color = exports.c_form_control_disabled_Color = {
    "name": "--pf-c-form-control--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-form-control--disabled--Color)"
  };
  const c_form_control_disabled_BackgroundColor = exports.c_form_control_disabled_BackgroundColor = {
    "name": "--pf-c-form-control--disabled--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-form-control--disabled--BackgroundColor)"
  };
  const c_form_control_disabled_BorderColor = exports.c_form_control_disabled_BorderColor = {
    "name": "--pf-c-form-control--disabled--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-form-control--disabled--BorderColor)"
  };
  const c_form_control_readonly_focus_BackgroundColor = exports.c_form_control_readonly_focus_BackgroundColor = {
    "name": "--pf-c-form-control--readonly--focus--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-form-control--readonly--focus--BackgroundColor)"
  };
  const c_form_control_readonly_focus_PaddingBottom = exports.c_form_control_readonly_focus_PaddingBottom = {
    "name": "--pf-c-form-control--readonly--focus--PaddingBottom",
    "value": "calc(0.375rem - 1px)",
    "var": "var(--pf-c-form-control--readonly--focus--PaddingBottom)"
  };
  const c_form_control_readonly_focus_BorderBottomWidth = exports.c_form_control_readonly_focus_BorderBottomWidth = {
    "name": "--pf-c-form-control--readonly--focus--BorderBottomWidth",
    "value": "1px",
    "var": "var(--pf-c-form-control--readonly--focus--BorderBottomWidth)"
  };
  const c_form_control_readonly_focus_BorderBottomColor = exports.c_form_control_readonly_focus_BorderBottomColor = {
    "name": "--pf-c-form-control--readonly--focus--BorderBottomColor",
    "value": "#737679",
    "var": "var(--pf-c-form-control--readonly--focus--BorderBottomColor)"
  };
  const c_form_control_invalid_BorderBottomWidth = exports.c_form_control_invalid_BorderBottomWidth = {
    "name": "--pf-c-form-control--invalid--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-form-control--invalid--BorderBottomWidth)"
  };
  const c_form_control_invalid_PaddingBottom = exports.c_form_control_invalid_PaddingBottom = {
    "name": "--pf-c-form-control--invalid--PaddingBottom",
    "value": "calc(0.375rem - 2px)",
    "var": "var(--pf-c-form-control--invalid--PaddingBottom)"
  };
  const c_form_control_invalid_BorderBottomColor = exports.c_form_control_invalid_BorderBottomColor = {
    "name": "--pf-c-form-control--invalid--BorderBottomColor",
    "value": "#c9190b",
    "var": "var(--pf-c-form-control--invalid--BorderBottomColor)"
  };
  const c_form_control_invalid_PaddingRight = exports.c_form_control_invalid_PaddingRight = {
    "name": "--pf-c-form-control--invalid--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-form-control--invalid--PaddingRight)"
  };
  const c_form_control_invalid_BackgroundPosition = exports.c_form_control_invalid_BackgroundPosition = {
    "name": "--pf-c-form-control--invalid--BackgroundPosition",
    "value": "calc(100% - 0.5rem - 1.5rem)",
    "var": "var(--pf-c-form-control--invalid--BackgroundPosition)"
  };
  const c_form_control_invalid_BackgroundSize = exports.c_form_control_invalid_BackgroundSize = {
    "name": "--pf-c-form-control--invalid--BackgroundSize",
    "value": "1rem 1rem",
    "var": "var(--pf-c-form-control--invalid--BackgroundSize)"
  };
  const c_form_control_invalid_BackgroundUrl = exports.c_form_control_invalid_BackgroundUrl = {
    "name": "--pf-c-form-control--invalid--BackgroundUrl",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%23c9190b' d='M504 256c0 136.997-111.043 248-248 248S8 392.997 8 256C8 119.083 119.043 8 256 8s248 111.083 248 248zm-248 50c-25.405 0-46 20.595-46 46s20.595 46 46 46 46-20.595 46-46-20.595-46-46-46zm-43.673-165.346l7.418 136c.347 6.364 5.609 11.346 11.982 11.346h48.546c6.373 0 11.635-4.982 11.982-11.346l7.418-136c.375-6.874-5.098-12.654-11.982-12.654h-63.383c-6.884 0-12.356 5.78-11.981 12.654z'/%3E%3C/svg%3E\")",
    "var": "var(--pf-c-form-control--invalid--BackgroundUrl)"
  };
  const c_form_control_invalid_exclamation_Background = exports.c_form_control_invalid_exclamation_Background = {
    "name": "--pf-c-form-control--invalid--exclamation--Background",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%23c9190b' d='M504 256c0 136.997-111.043 248-248 248S8 392.997 8 256C8 119.083 119.043 8 256 8s248 111.083 248 248zm-248 50c-25.405 0-46 20.595-46 46s20.595 46 46 46 46-20.595 46-46-20.595-46-46-46zm-43.673-165.346l7.418 136c.347 6.364 5.609 11.346 11.982 11.346h48.546c6.373 0 11.635-4.982 11.982-11.346l7.418-136c.375-6.874-5.098-12.654-11.982-12.654h-63.383c-6.884 0-12.356 5.78-11.981 12.654z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) 0.5rem/1rem 1rem no-repeat",
    "var": "var(--pf-c-form-control--invalid--exclamation--Background)"
  };
  const c_form_control_invalid_Background = exports.c_form_control_invalid_Background = {
    "name": "--pf-c-form-control--invalid--Background",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%23c9190b' d='M504 256c0 136.997-111.043 248-248 248S8 392.997 8 256C8 119.083 119.043 8 256 8s248 111.083 248 248zm-248 50c-25.405 0-46 20.595-46 46s20.595 46 46 46 46-20.595 46-46-20.595-46-46-46zm-43.673-165.346l7.418 136c.347 6.364 5.609 11.346 11.982 11.346h48.546c6.373 0 11.635-4.982 11.982-11.346l7.418-136c.375-6.874-5.098-12.654-11.982-12.654h-63.383c-6.884 0-12.356 5.78-11.981 12.654z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) 0.5rem/1rem 1rem no-repeat,#fff url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 512'%3E%3Cpath fill='%23urrentColor' d='M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) center/0.875rem no-repeat",
    "var": "var(--pf-c-form-control--invalid--Background)"
  };
  const c_form_control_success_BorderBottomWidth = exports.c_form_control_success_BorderBottomWidth = {
    "name": "--pf-c-form-control--success--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-form-control--success--BorderBottomWidth)"
  };
  const c_form_control_success_PaddingBottom = exports.c_form_control_success_PaddingBottom = {
    "name": "--pf-c-form-control--success--PaddingBottom",
    "value": "calc(0.375rem - 2px)",
    "var": "var(--pf-c-form-control--success--PaddingBottom)"
  };
  const c_form_control_success_BorderBottomColor = exports.c_form_control_success_BorderBottomColor = {
    "name": "--pf-c-form-control--success--BorderBottomColor",
    "value": "#92d400",
    "var": "var(--pf-c-form-control--success--BorderBottomColor)"
  };
  const c_form_control_success_PaddingRight = exports.c_form_control_success_PaddingRight = {
    "name": "--pf-c-form-control--success--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-form-control--success--PaddingRight)"
  };
  const c_form_control_success_BackgroundPosition = exports.c_form_control_success_BackgroundPosition = {
    "name": "--pf-c-form-control--success--BackgroundPosition",
    "value": "calc(100% - 0.5rem - 1.5rem)",
    "var": "var(--pf-c-form-control--success--BackgroundPosition)"
  };
  const c_form_control_success_BackgroundSize = exports.c_form_control_success_BackgroundSize = {
    "name": "--pf-c-form-control--success--BackgroundSize",
    "value": "1rem 1rem",
    "var": "var(--pf-c-form-control--success--BackgroundSize)"
  };
  const c_form_control_success_BackgroundUrl = exports.c_form_control_success_BackgroundUrl = {
    "name": "--pf-c-form-control--success--BackgroundUrl",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%2392d400' d='M504 256c0 136.967-111.033 248-248 248S8 392.967 8 256 119.033 8 256 8s248 111.033 248 248zM227.314 387.314l184-184c6.248-6.248 6.248-16.379 0-22.627l-22.627-22.627c-6.248-6.249-16.379-6.249-22.628 0L216 308.118l-70.059-70.059c-6.248-6.248-16.379-6.248-22.628 0l-22.627 22.627c-6.248 6.248-6.248 16.379 0 22.627l104 104c6.249 6.249 16.379 6.249 22.628.001z'/%3E%3C/svg%3E\")",
    "var": "var(--pf-c-form-control--success--BackgroundUrl)"
  };
  const c_form_control_success_check_Background = exports.c_form_control_success_check_Background = {
    "name": "--pf-c-form-control--success--check--Background",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%2392d400' d='M504 256c0 136.967-111.033 248-248 248S8 392.967 8 256 119.033 8 256 8s248 111.033 248 248zM227.314 387.314l184-184c6.248-6.248 6.248-16.379 0-22.627l-22.627-22.627c-6.248-6.249-16.379-6.249-22.628 0L216 308.118l-70.059-70.059c-6.248-6.248-16.379-6.248-22.628 0l-22.627 22.627c-6.248 6.248-6.248 16.379 0 22.627l104 104c6.249 6.249 16.379 6.249 22.628.001z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) 0.5rem/1rem 1rem no-repeat",
    "var": "var(--pf-c-form-control--success--check--Background)"
  };
  const c_form_control_success_Background = exports.c_form_control_success_Background = {
    "name": "--pf-c-form-control--success--Background",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%2392d400' d='M504 256c0 136.967-111.033 248-248 248S8 392.967 8 256 119.033 8 256 8s248 111.033 248 248zM227.314 387.314l184-184c6.248-6.248 6.248-16.379 0-22.627l-22.627-22.627c-6.248-6.249-16.379-6.249-22.628 0L216 308.118l-70.059-70.059c-6.248-6.248-16.379-6.248-22.628 0l-22.627 22.627c-6.248 6.248-6.248 16.379 0 22.627l104 104c6.249 6.249 16.379 6.249 22.628.001z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) 0.5rem/1rem 1rem no-repeat,#fff url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 512'%3E%3Cpath fill='%23urrentColor' d='M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) center/0.875rem no-repeat",
    "var": "var(--pf-c-form-control--success--Background)"
  };
  const c_form_control_m_search_PaddingLeft = exports.c_form_control_m_search_PaddingLeft = {
    "name": "--pf-c-form-control--m-search--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-form-control--m-search--PaddingLeft)"
  };
  const c_form_control_m_search_BackgroundPosition = exports.c_form_control_m_search_BackgroundPosition = {
    "name": "--pf-c-form-control--m-search--BackgroundPosition",
    "value": "0.5rem",
    "var": "var(--pf-c-form-control--m-search--BackgroundPosition)"
  };
  const c_form_control_m_search_BackgroundSize = exports.c_form_control_m_search_BackgroundSize = {
    "name": "--pf-c-form-control--m-search--BackgroundSize",
    "value": "1rem 1rem",
    "var": "var(--pf-c-form-control--m-search--BackgroundSize)"
  };
  const c_form_control_m_search_BackgroundUrl = exports.c_form_control_m_search_BackgroundUrl = {
    "name": "--pf-c-form-control--m-search--BackgroundUrl",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%23737679' d='M505 442.7L405.3 343c-4.5-4.5-10.6-7-17-7H372c27.6-35.3 44-79.7 44-128C416 93.1 322.9 0 208 0S0 93.1 0 208s93.1 208 208 208c48.3 0 92.7-16.4 128-44v16.3c0 6.4 2.5 12.5 7 17l99.7 99.7c9.4 9.4 24.6 9.4 33.9 0l28.3-28.3c9.4-9.4 9.4-24.6.1-34zM208 336c-70.7 0-128-57.2-128-128 0-70.7 57.2-128 128-128 70.7 0 128 57.2 128 128 0 70.7-57.2 128-128 128z'/%3E%3C/svg%3E\")",
    "var": "var(--pf-c-form-control--m-search--BackgroundUrl)"
  };
  const c_form_control_m_search_Background = exports.c_form_control_m_search_Background = {
    "name": "--pf-c-form-control--m-search--Background",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%23737679' d='M505 442.7L405.3 343c-4.5-4.5-10.6-7-17-7H372c27.6-35.3 44-79.7 44-128C416 93.1 322.9 0 208 0S0 93.1 0 208s93.1 208 208 208c48.3 0 92.7-16.4 128-44v16.3c0 6.4 2.5 12.5 7 17l99.7 99.7c9.4 9.4 24.6 9.4 33.9 0l28.3-28.3c9.4-9.4 9.4-24.6.1-34zM208 336c-70.7 0-128-57.2-128-128 0-70.7 57.2-128 128-128 70.7 0 128 57.2 128 128 0 70.7-57.2 128-128 128z'/%3E%3C/svg%3E\") 0.5rem/1rem 1rem no-repeat",
    "var": "var(--pf-c-form-control--m-search--Background)"
  };
  const c_form_control__select_PaddingRight = exports.c_form_control__select_PaddingRight = {
    "name": "--pf-c-form-control__select--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-form-control__select--PaddingRight)"
  };
  const c_form_control__select_BackgroundUrl = exports.c_form_control__select_BackgroundUrl = {
    "name": "--pf-c-form-control__select--BackgroundUrl",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 512'%3E%3Cpath fill='%23urrentColor' d='M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z'/%3E%3C/svg%3E\")",
    "var": "var(--pf-c-form-control__select--BackgroundUrl)"
  };
  const c_form_control__select_BackgroundSize = exports.c_form_control__select_BackgroundSize = {
    "name": "--pf-c-form-control__select--BackgroundSize",
    "value": "0.875rem",
    "var": "var(--pf-c-form-control__select--BackgroundSize)"
  };
  const c_form_control__select_BackgroundPosition = exports.c_form_control__select_BackgroundPosition = {
    "name": "--pf-c-form-control__select--BackgroundPosition",
    "value": "calc(100% - 0.5rem) center",
    "var": "var(--pf-c-form-control__select--BackgroundPosition)"
  };
  const c_form_control__select_arrow_Background = exports.c_form_control__select_arrow_Background = {
    "name": "--pf-c-form-control__select--arrow--Background",
    "value": "#fff url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 512'%3E%3Cpath fill='%23urrentColor' d='M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) center/0.875rem no-repeat",
    "var": "var(--pf-c-form-control__select--arrow--Background)"
  };
  const c_form_control__select_Background = exports.c_form_control__select_Background = {
    "name": "--pf-c-form-control__select--Background",
    "value": "#fff url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 512'%3E%3Cpath fill='%23urrentColor' d='M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) center/0.875rem no-repeat",
    "var": "var(--pf-c-form-control__select--Background)"
  };
  const c_form_control__select_invalid_Background = exports.c_form_control__select_invalid_Background = {
    "name": "--pf-c-form-control__select--invalid--Background",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%23c9190b' d='M504 256c0 136.997-111.043 248-248 248S8 392.997 8 256C8 119.083 119.043 8 256 8s248 111.083 248 248zm-248 50c-25.405 0-46 20.595-46 46s20.595 46 46 46 46-20.595 46-46-20.595-46-46-46zm-43.673-165.346l7.418 136c.347 6.364 5.609 11.346 11.982 11.346h48.546c6.373 0 11.635-4.982 11.982-11.346l7.418-136c.375-6.874-5.098-12.654-11.982-12.654h-63.383c-6.884 0-12.356 5.78-11.981 12.654z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) 0.5rem/1rem 1rem no-repeat,#fff url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 512'%3E%3Cpath fill='%23urrentColor' d='M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) center/0.875rem no-repeat",
    "var": "var(--pf-c-form-control__select--invalid--Background)"
  };
  const c_form_control__select_invalid_PaddingRight = exports.c_form_control__select_invalid_PaddingRight = {
    "name": "--pf-c-form-control__select--invalid--PaddingRight",
    "value": "calc(0.5rem + 3rem)",
    "var": "var(--pf-c-form-control__select--invalid--PaddingRight)"
  };
  const c_form_control__select_success_Background = exports.c_form_control__select_success_Background = {
    "name": "--pf-c-form-control__select--success--Background",
    "value": "url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'%3E%3Cpath fill='%2392d400' d='M504 256c0 136.967-111.033 248-248 248S8 392.967 8 256 119.033 8 256 8s248 111.033 248 248zM227.314 387.314l184-184c6.248-6.248 6.248-16.379 0-22.627l-22.627-22.627c-6.248-6.249-16.379-6.249-22.628 0L216 308.118l-70.059-70.059c-6.248-6.248-16.379-6.248-22.628 0l-22.627 22.627c-6.248 6.248-6.248 16.379 0 22.627l104 104c6.249 6.249 16.379 6.249 22.628.001z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) 0.5rem/1rem 1rem no-repeat,#fff url(\"data:image/svg+xml;charset=utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 512'%3E%3Cpath fill='%23urrentColor' d='M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z'/%3E%3C/svg%3E\") calc(100% - 0.5rem) center/0.875rem no-repeat",
    "var": "var(--pf-c-form-control__select--success--Background)"
  };
  const c_form_control__select_success_PaddingRight = exports.c_form_control__select_success_PaddingRight = {
    "name": "--pf-c-form-control__select--success--PaddingRight",
    "value": "calc(0.5rem + 3rem)",
    "var": "var(--pf-c-form-control__select--success--PaddingRight)"
  };
  const c_form_control_Color = exports.c_form_control_Color = {
    "name": "--pf-c-form-control--Color",
    "value": "#737679",
    "var": "var(--pf-c-form-control--Color)"
  };
  const c_inline_edit__group_item_MarginRight = exports.c_inline_edit__group_item_MarginRight = {
    "name": "--pf-c-inline-edit__group--item--MarginRight",
    "value": "0",
    "var": "var(--pf-c-inline-edit__group--item--MarginRight)"
  };
  const c_inline_edit__action_c_button_m_valid_m_plain_Color = exports.c_inline_edit__action_c_button_m_valid_m_plain_Color = {
    "name": "--pf-c-inline-edit__action--c-button--m-valid--m-plain--Color",
    "value": "#004080",
    "var": "var(--pf-c-inline-edit__action--c-button--m-valid--m-plain--Color)"
  };
  const c_inline_edit__action_c_button_m_valid_m_plain_hover_Color = exports.c_inline_edit__action_c_button_m_valid_m_plain_hover_Color = {
    "name": "--pf-c-inline-edit__action--c-button--m-valid--m-plain--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-inline-edit__action--c-button--m-valid--m-plain--hover--Color)"
  };
  const c_inline_edit__action_m_icon_group_item_MarginRight = exports.c_inline_edit__action_m_icon_group_item_MarginRight = {
    "name": "--pf-c-inline-edit__action--m-icon-group--item--MarginRight",
    "value": "0",
    "var": "var(--pf-c-inline-edit__action--m-icon-group--item--MarginRight)"
  };
  const c_inline_edit__group_m_footer_MarginTop = exports.c_inline_edit__group_m_footer_MarginTop = {
    "name": "--pf-c-inline-edit__group--m-footer--MarginTop",
    "value": "2rem",
    "var": "var(--pf-c-inline-edit__group--m-footer--MarginTop)"
  };
  const c_inline_edit__label_m_bold_FontWeight = exports.c_inline_edit__label_m_bold_FontWeight = {
    "name": "--pf-c-inline-edit__label--m-bold--FontWeight",
    "value": "700",
    "var": "var(--pf-c-inline-edit__label--m-bold--FontWeight)"
  };
  const c_input_group_BackgroundColor = exports.c_input_group_BackgroundColor = {
    "name": "--pf-c-input-group--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-input-group--BackgroundColor)"
  };
  const c_input_group_BorderRadius = exports.c_input_group_BorderRadius = {
    "name": "--pf-c-input-group--BorderRadius",
    "value": "3px",
    "var": "var(--pf-c-input-group--BorderRadius)"
  };
  const c_input_group__text_FontSize = exports.c_input_group__text_FontSize = {
    "name": "--pf-c-input-group__text--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-input-group__text--FontSize)"
  };
  const c_input_group__text_PaddingRight = exports.c_input_group__text_PaddingRight = {
    "name": "--pf-c-input-group__text--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-input-group__text--PaddingRight)"
  };
  const c_input_group__text_PaddingLeft = exports.c_input_group__text_PaddingLeft = {
    "name": "--pf-c-input-group__text--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-input-group__text--PaddingLeft)"
  };
  const c_input_group__text_Color = exports.c_input_group__text_Color = {
    "name": "--pf-c-input-group__text--Color",
    "value": "#737679",
    "var": "var(--pf-c-input-group__text--Color)"
  };
  const c_input_group__text_BorderWidth = exports.c_input_group__text_BorderWidth = {
    "name": "--pf-c-input-group__text--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-input-group__text--BorderWidth)"
  };
  const c_input_group__text_BorderTopColor = exports.c_input_group__text_BorderTopColor = {
    "name": "--pf-c-input-group__text--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-input-group__text--BorderTopColor)"
  };
  const c_input_group__text_BorderRightColor = exports.c_input_group__text_BorderRightColor = {
    "name": "--pf-c-input-group__text--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-input-group__text--BorderRightColor)"
  };
  const c_input_group__text_BorderBottomColor = exports.c_input_group__text_BorderBottomColor = {
    "name": "--pf-c-input-group__text--BorderBottomColor",
    "value": "#8a8d90",
    "var": "var(--pf-c-input-group__text--BorderBottomColor)"
  };
  const c_input_group__text_BorderLeftColor = exports.c_input_group__text_BorderLeftColor = {
    "name": "--pf-c-input-group__text--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-input-group__text--BorderLeftColor)"
  };
  const c_input_group__text_BackgroundColor = exports.c_input_group__text_BackgroundColor = {
    "name": "--pf-c-input-group__text--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-input-group__text--BackgroundColor)"
  };
  const c_input_group__textarea_MinHeight = exports.c_input_group__textarea_MinHeight = {
    "name": "--pf-c-input-group__textarea--MinHeight",
    "value": "2rem",
    "var": "var(--pf-c-input-group__textarea--MinHeight)"
  };
  const c_input_group_c_button_BorderRadius = exports.c_input_group_c_button_BorderRadius = {
    "name": "--pf-c-input-group--c-button--BorderRadius",
    "value": "3px",
    "var": "var(--pf-c-input-group--c-button--BorderRadius)"
  };
  const c_input_group_c_form_control_invalid_ZIndex = exports.c_input_group_c_form_control_invalid_ZIndex = {
    "name": "--pf-c-input-group--c-form-control--invalid--ZIndex",
    "value": "100",
    "var": "var(--pf-c-input-group--c-form-control--invalid--ZIndex)"
  };
  const c_input_group_c_form_control_MarginRight = exports.c_input_group_c_form_control_MarginRight = {
    "name": "--pf-c-input-group--c-form-control--MarginRight",
    "value": "1px",
    "var": "var(--pf-c-input-group--c-form-control--MarginRight)"
  };
  const c_label_PaddingTop = exports.c_label_PaddingTop = {
    "name": "--pf-c-label--PaddingTop",
    "value": "0.25rem",
    "var": "var(--pf-c-label--PaddingTop)"
  };
  const c_label_PaddingRight = exports.c_label_PaddingRight = {
    "name": "--pf-c-label--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-label--PaddingRight)"
  };
  const c_label_PaddingBottom = exports.c_label_PaddingBottom = {
    "name": "--pf-c-label--PaddingBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-label--PaddingBottom)"
  };
  const c_label_PaddingLeft = exports.c_label_PaddingLeft = {
    "name": "--pf-c-label--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-label--PaddingLeft)"
  };
  const c_label_BorderRadius = exports.c_label_BorderRadius = {
    "name": "--pf-c-label--BorderRadius",
    "value": "3px",
    "var": "var(--pf-c-label--BorderRadius)"
  };
  const c_label_BackgroundColor = exports.c_label_BackgroundColor = {
    "name": "--pf-c-label--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-label--BackgroundColor)"
  };
  const c_label_Color = exports.c_label_Color = {
    "name": "--pf-c-label--Color",
    "value": "#fff",
    "var": "var(--pf-c-label--Color)"
  };
  const c_label_FontSize = exports.c_label_FontSize = {
    "name": "--pf-c-label--FontSize",
    "value": "0.75rem",
    "var": "var(--pf-c-label--FontSize)"
  };
  const c_label_m_compact_FontSize = exports.c_label_m_compact_FontSize = {
    "name": "--pf-c-label--m-compact--FontSize",
    "value": "0.75rem",
    "var": "var(--pf-c-label--m-compact--FontSize)"
  };
  const c_list_PaddingLeft = exports.c_list_PaddingLeft = {
    "name": "--pf-c-list--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-list--PaddingLeft)"
  };
  const c_list_nested_MarginTop = exports.c_list_nested_MarginTop = {
    "name": "--pf-c-list--nested--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-list--nested--MarginTop)"
  };
  const c_list_nested_MarginLeft = exports.c_list_nested_MarginLeft = {
    "name": "--pf-c-list--nested--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-list--nested--MarginLeft)"
  };
  const c_list_ul_ListStyle = exports.c_list_ul_ListStyle = {
    "name": "--pf-c-list--ul--ListStyle",
    "value": "disc outside",
    "var": "var(--pf-c-list--ul--ListStyle)"
  };
  const c_list_li_MarginTop = exports.c_list_li_MarginTop = {
    "name": "--pf-c-list--li--MarginTop",
    "value": "0",
    "var": "var(--pf-c-list--li--MarginTop)"
  };
  const c_list_m_inline_li_MarginRight = exports.c_list_m_inline_li_MarginRight = {
    "name": "--pf-c-list--m-inline--li--MarginRight",
    "value": "1.5rem",
    "var": "var(--pf-c-list--m-inline--li--MarginRight)"
  };
  const c_login_PaddingTop = exports.c_login_PaddingTop = {
    "name": "--pf-c-login--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-login--PaddingTop)"
  };
  const c_login_PaddingBottom = exports.c_login_PaddingBottom = {
    "name": "--pf-c-login--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-login--PaddingBottom)"
  };
  const c_login_xl_BackgroundImage = exports.c_login_xl_BackgroundImage = {
    "name": "--pf-c-login--xl--BackgroundImage",
    "value": "none",
    "var": "var(--pf-c-login--xl--BackgroundImage)"
  };
  const c_login__container_xl_GridColumnGap = exports.c_login__container_xl_GridColumnGap = {
    "name": "--pf-c-login__container--xl--GridColumnGap",
    "value": "4rem",
    "var": "var(--pf-c-login__container--xl--GridColumnGap)"
  };
  const c_login__container_MaxWidth = exports.c_login__container_MaxWidth = {
    "name": "--pf-c-login__container--MaxWidth",
    "value": "31.25rem",
    "var": "var(--pf-c-login__container--MaxWidth)"
  };
  const c_login__container_xl_MaxWidth = exports.c_login__container_xl_MaxWidth = {
    "name": "--pf-c-login__container--xl--MaxWidth",
    "value": "none",
    "var": "var(--pf-c-login__container--xl--MaxWidth)"
  };
  const c_login__container_PaddingLeft = exports.c_login__container_PaddingLeft = {
    "name": "--pf-c-login__container--PaddingLeft",
    "value": "6.125rem",
    "var": "var(--pf-c-login__container--PaddingLeft)"
  };
  const c_login__container_PaddingRight = exports.c_login__container_PaddingRight = {
    "name": "--pf-c-login__container--PaddingRight",
    "value": "6.125rem",
    "var": "var(--pf-c-login__container--PaddingRight)"
  };
  const c_login__container_xl_GridTemplateColumns = exports.c_login__container_xl_GridTemplateColumns = {
    "name": "--pf-c-login__container--xl--GridTemplateColumns",
    "value": "34rem minmax(auto,34rem)",
    "var": "var(--pf-c-login__container--xl--GridTemplateColumns)"
  };
  const c_login__header_MarginBottom = exports.c_login__header_MarginBottom = {
    "name": "--pf-c-login__header--MarginBottom",
    "value": "1rem",
    "var": "var(--pf-c-login__header--MarginBottom)"
  };
  const c_login__header_sm_PaddingLeft = exports.c_login__header_sm_PaddingLeft = {
    "name": "--pf-c-login__header--sm--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-login__header--sm--PaddingLeft)"
  };
  const c_login__header_sm_PaddingRight = exports.c_login__header_sm_PaddingRight = {
    "name": "--pf-c-login__header--sm--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-login__header--sm--PaddingRight)"
  };
  const c_login__header_xl_MarginBottom = exports.c_login__header_xl_MarginBottom = {
    "name": "--pf-c-login__header--xl--MarginBottom",
    "value": "3rem",
    "var": "var(--pf-c-login__header--xl--MarginBottom)"
  };
  const c_login__header_xl_MarginTop = exports.c_login__header_xl_MarginTop = {
    "name": "--pf-c-login__header--xl--MarginTop",
    "value": "4rem",
    "var": "var(--pf-c-login__header--xl--MarginTop)"
  };
  const c_login__header_c_brand_MarginBottom = exports.c_login__header_c_brand_MarginBottom = {
    "name": "--pf-c-login__header--c-brand--MarginBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-login__header--c-brand--MarginBottom)"
  };
  const c_login__header_c_brand_xl_MarginBottom = exports.c_login__header_c_brand_xl_MarginBottom = {
    "name": "--pf-c-login__header--c-brand--xl--MarginBottom",
    "value": "3rem",
    "var": "var(--pf-c-login__header--c-brand--xl--MarginBottom)"
  };
  const c_login__main_BackgroundColor = exports.c_login__main_BackgroundColor = {
    "name": "--pf-c-login__main--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-login__main--BackgroundColor)"
  };
  const c_login__main_xl_MarginBottom = exports.c_login__main_xl_MarginBottom = {
    "name": "--pf-c-login__main--xl--MarginBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-login__main--xl--MarginBottom)"
  };
  const c_login__main_header_PaddingTop = exports.c_login__main_header_PaddingTop = {
    "name": "--pf-c-login__main-header--PaddingTop",
    "value": "3rem",
    "var": "var(--pf-c-login__main-header--PaddingTop)"
  };
  const c_login__main_header_PaddingRight = exports.c_login__main_header_PaddingRight = {
    "name": "--pf-c-login__main-header--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-login__main-header--PaddingRight)"
  };
  const c_login__main_header_PaddingBottom = exports.c_login__main_header_PaddingBottom = {
    "name": "--pf-c-login__main-header--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-login__main-header--PaddingBottom)"
  };
  const c_login__main_header_PaddingLeft = exports.c_login__main_header_PaddingLeft = {
    "name": "--pf-c-login__main-header--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-login__main-header--PaddingLeft)"
  };
  const c_login__main_header_md_PaddingRight = exports.c_login__main_header_md_PaddingRight = {
    "name": "--pf-c-login__main-header--md--PaddingRight",
    "value": "3rem",
    "var": "var(--pf-c-login__main-header--md--PaddingRight)"
  };
  const c_login__main_header_md_PaddingLeft = exports.c_login__main_header_md_PaddingLeft = {
    "name": "--pf-c-login__main-header--md--PaddingLeft",
    "value": "3rem",
    "var": "var(--pf-c-login__main-header--md--PaddingLeft)"
  };
  const c_login__main_header_ColumnGap = exports.c_login__main_header_ColumnGap = {
    "name": "--pf-c-login__main-header--ColumnGap",
    "value": "1rem",
    "var": "var(--pf-c-login__main-header--ColumnGap)"
  };
  const c_login__main_header_RowGap = exports.c_login__main_header_RowGap = {
    "name": "--pf-c-login__main-header--RowGap",
    "value": "1rem",
    "var": "var(--pf-c-login__main-header--RowGap)"
  };
  const c_login__main_header_desc_MarginBottom = exports.c_login__main_header_desc_MarginBottom = {
    "name": "--pf-c-login__main-header-desc--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-login__main-header-desc--MarginBottom)"
  };
  const c_login__main_header_desc_md_MarginBottom = exports.c_login__main_header_desc_md_MarginBottom = {
    "name": "--pf-c-login__main-header-desc--md--MarginBottom",
    "value": "0",
    "var": "var(--pf-c-login__main-header-desc--md--MarginBottom)"
  };
  const c_login__main_header_desc_FontSize = exports.c_login__main_header_desc_FontSize = {
    "name": "--pf-c-login__main-header-desc--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-login__main-header-desc--FontSize)"
  };
  const c_login__main_body_PaddingRight = exports.c_login__main_body_PaddingRight = {
    "name": "--pf-c-login__main-body--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-login__main-body--PaddingRight)"
  };
  const c_login__main_body_PaddingBottom = exports.c_login__main_body_PaddingBottom = {
    "name": "--pf-c-login__main-body--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-login__main-body--PaddingBottom)"
  };
  const c_login__main_body_PaddingLeft = exports.c_login__main_body_PaddingLeft = {
    "name": "--pf-c-login__main-body--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-login__main-body--PaddingLeft)"
  };
  const c_login__main_body_md_PaddingRight = exports.c_login__main_body_md_PaddingRight = {
    "name": "--pf-c-login__main-body--md--PaddingRight",
    "value": "3rem",
    "var": "var(--pf-c-login__main-body--md--PaddingRight)"
  };
  const c_login__main_body_md_PaddingLeft = exports.c_login__main_body_md_PaddingLeft = {
    "name": "--pf-c-login__main-body--md--PaddingLeft",
    "value": "3rem",
    "var": "var(--pf-c-login__main-body--md--PaddingLeft)"
  };
  const c_login__main_body_c_form__helper_text_icon_FontSize = exports.c_login__main_body_c_form__helper_text_icon_FontSize = {
    "name": "--pf-c-login__main-body--c-form__helper-text-icon--FontSize",
    "value": "1.125rem",
    "var": "var(--pf-c-login__main-body--c-form__helper-text-icon--FontSize)"
  };
  const c_login__main_body_c_form__helper_text_icon_MarginRight = exports.c_login__main_body_c_form__helper_text_icon_MarginRight = {
    "name": "--pf-c-login__main-body--c-form__helper-text-icon--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-login__main-body--c-form__helper-text-icon--MarginRight)"
  };
  const c_login__main_footer_PaddingBottom = exports.c_login__main_footer_PaddingBottom = {
    "name": "--pf-c-login__main-footer--PaddingBottom",
    "value": "4rem",
    "var": "var(--pf-c-login__main-footer--PaddingBottom)"
  };
  const c_login__main_footer_c_title_MarginBottom = exports.c_login__main_footer_c_title_MarginBottom = {
    "name": "--pf-c-login__main-footer--c-title--MarginBottom",
    "value": "1rem",
    "var": "var(--pf-c-login__main-footer--c-title--MarginBottom)"
  };
  const c_login__main_footer_links_PaddingTop = exports.c_login__main_footer_links_PaddingTop = {
    "name": "--pf-c-login__main-footer-links--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-login__main-footer-links--PaddingTop)"
  };
  const c_login__main_footer_links_PaddingRight = exports.c_login__main_footer_links_PaddingRight = {
    "name": "--pf-c-login__main-footer-links--PaddingRight",
    "value": "4rem",
    "var": "var(--pf-c-login__main-footer-links--PaddingRight)"
  };
  const c_login__main_footer_links_PaddingBottom = exports.c_login__main_footer_links_PaddingBottom = {
    "name": "--pf-c-login__main-footer-links--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-login__main-footer-links--PaddingBottom)"
  };
  const c_login__main_footer_links_PaddingLeft = exports.c_login__main_footer_links_PaddingLeft = {
    "name": "--pf-c-login__main-footer-links--PaddingLeft",
    "value": "4rem",
    "var": "var(--pf-c-login__main-footer-links--PaddingLeft)"
  };
  const c_login__main_footer_links_item_PaddingRight = exports.c_login__main_footer_links_item_PaddingRight = {
    "name": "--pf-c-login__main-footer-links-item--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-login__main-footer-links-item--PaddingRight)"
  };
  const c_login__main_footer_links_item_PaddingLeft = exports.c_login__main_footer_links_item_PaddingLeft = {
    "name": "--pf-c-login__main-footer-links-item--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-login__main-footer-links-item--PaddingLeft)"
  };
  const c_login__main_footer_links_item_MarginBottom = exports.c_login__main_footer_links_item_MarginBottom = {
    "name": "--pf-c-login__main-footer-links-item--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-login__main-footer-links-item--MarginBottom)"
  };
  const c_login__main_footer_links_item_link_svg_Fill = exports.c_login__main_footer_links_item_link_svg_Fill = {
    "name": "--pf-c-login__main-footer-links-item-link-svg--Fill",
    "value": "#737679",
    "var": "var(--pf-c-login__main-footer-links-item-link-svg--Fill)"
  };
  const c_login__main_footer_links_item_link_svg_Width = exports.c_login__main_footer_links_item_link_svg_Width = {
    "name": "--pf-c-login__main-footer-links-item-link-svg--Width",
    "value": "1.5rem",
    "var": "var(--pf-c-login__main-footer-links-item-link-svg--Width)"
  };
  const c_login__main_footer_links_item_link_svg_Height = exports.c_login__main_footer_links_item_link_svg_Height = {
    "name": "--pf-c-login__main-footer-links-item-link-svg--Height",
    "value": "1.5rem",
    "var": "var(--pf-c-login__main-footer-links-item-link-svg--Height)"
  };
  const c_login__main_footer_links_item_link_svg_hover_Fill = exports.c_login__main_footer_links_item_link_svg_hover_Fill = {
    "name": "--pf-c-login__main-footer-links-item-link-svg--hover--Fill",
    "value": "#151515",
    "var": "var(--pf-c-login__main-footer-links-item-link-svg--hover--Fill)"
  };
  const c_login__main_footer_band_PaddingTop = exports.c_login__main_footer_band_PaddingTop = {
    "name": "--pf-c-login__main-footer-band--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-login__main-footer-band--PaddingTop)"
  };
  const c_login__main_footer_band_PaddingRight = exports.c_login__main_footer_band_PaddingRight = {
    "name": "--pf-c-login__main-footer-band--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-login__main-footer-band--PaddingRight)"
  };
  const c_login__main_footer_band_PaddingBottom = exports.c_login__main_footer_band_PaddingBottom = {
    "name": "--pf-c-login__main-footer-band--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-login__main-footer-band--PaddingBottom)"
  };
  const c_login__main_footer_band_PaddingLeft = exports.c_login__main_footer_band_PaddingLeft = {
    "name": "--pf-c-login__main-footer-band--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-login__main-footer-band--PaddingLeft)"
  };
  const c_login__main_footer_band_BackgroundColor = exports.c_login__main_footer_band_BackgroundColor = {
    "name": "--pf-c-login__main-footer-band--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-login__main-footer-band--BackgroundColor)"
  };
  const c_login__main_footer_band_item_PaddingTop = exports.c_login__main_footer_band_item_PaddingTop = {
    "name": "--pf-c-login__main-footer-band-item--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-login__main-footer-band-item--PaddingTop)"
  };
  const c_login__footer_sm_PaddingLeft = exports.c_login__footer_sm_PaddingLeft = {
    "name": "--pf-c-login__footer--sm--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-login__footer--sm--PaddingLeft)"
  };
  const c_login__footer_sm_PaddingRight = exports.c_login__footer_sm_PaddingRight = {
    "name": "--pf-c-login__footer--sm--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-login__footer--sm--PaddingRight)"
  };
  const c_login__footer_c_list_PaddingTop = exports.c_login__footer_c_list_PaddingTop = {
    "name": "--pf-c-login__footer--c-list--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-login__footer--c-list--PaddingTop)"
  };
  const c_login__footer_c_list_xl_PaddingTop = exports.c_login__footer_c_list_xl_PaddingTop = {
    "name": "--pf-c-login__footer--c-list--xl--PaddingTop",
    "value": "3rem",
    "var": "var(--pf-c-login__footer--c-list--xl--PaddingTop)"
  };
  const c_modal_box_BackgroundColor = exports.c_modal_box_BackgroundColor = {
    "name": "--pf-c-modal-box--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-modal-box--BackgroundColor)"
  };
  const c_modal_box_BorderColor = exports.c_modal_box_BorderColor = {
    "name": "--pf-c-modal-box--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-modal-box--BorderColor)"
  };
  const c_modal_box_PaddingTop = exports.c_modal_box_PaddingTop = {
    "name": "--pf-c-modal-box--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-modal-box--PaddingTop)"
  };
  const c_modal_box_PaddingRight = exports.c_modal_box_PaddingRight = {
    "name": "--pf-c-modal-box--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-modal-box--PaddingRight)"
  };
  const c_modal_box_PaddingBottom = exports.c_modal_box_PaddingBottom = {
    "name": "--pf-c-modal-box--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-modal-box--PaddingBottom)"
  };
  const c_modal_box_PaddingLeft = exports.c_modal_box_PaddingLeft = {
    "name": "--pf-c-modal-box--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-modal-box--PaddingLeft)"
  };
  const c_modal_box_BorderSize = exports.c_modal_box_BorderSize = {
    "name": "--pf-c-modal-box--BorderSize",
    "value": "1px",
    "var": "var(--pf-c-modal-box--BorderSize)"
  };
  const c_modal_box_BoxShadow = exports.c_modal_box_BoxShadow = {
    "name": "--pf-c-modal-box--BoxShadow",
    "value": "0 0.1875rem 0.4375rem 0.1875rem rgba(3,3,3,0.13),0 0.6875rem 1.5rem 1rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-modal-box--BoxShadow)"
  };
  const c_modal_box_ZIndex = exports.c_modal_box_ZIndex = {
    "name": "--pf-c-modal-box--ZIndex",
    "value": "500",
    "var": "var(--pf-c-modal-box--ZIndex)"
  };
  const c_modal_box_Width = exports.c_modal_box_Width = {
    "name": "--pf-c-modal-box--Width",
    "value": "100%",
    "var": "var(--pf-c-modal-box--Width)"
  };
  const c_modal_box_MaxWidth = exports.c_modal_box_MaxWidth = {
    "name": "--pf-c-modal-box--MaxWidth",
    "value": "calc(100% - 2rem)",
    "var": "var(--pf-c-modal-box--MaxWidth)"
  };
  const c_modal_box_m_sm_sm_MaxWidth = exports.c_modal_box_m_sm_sm_MaxWidth = {
    "name": "--pf-c-modal-box--m-sm--sm--MaxWidth",
    "value": "35rem",
    "var": "var(--pf-c-modal-box--m-sm--sm--MaxWidth)"
  };
  const c_modal_box_m_lg_lg_MaxWidth = exports.c_modal_box_m_lg_lg_MaxWidth = {
    "name": "--pf-c-modal-box--m-lg--lg--MaxWidth",
    "value": "70rem",
    "var": "var(--pf-c-modal-box--m-lg--lg--MaxWidth)"
  };
  const c_modal_box_MaxHeight = exports.c_modal_box_MaxHeight = {
    "name": "--pf-c-modal-box--MaxHeight",
    "value": "calc(100vh - 3rem)",
    "var": "var(--pf-c-modal-box--MaxHeight)"
  };
  const c_modal_box__c_title_description_MarginTop = exports.c_modal_box__c_title_description_MarginTop = {
    "name": "--pf-c-modal-box__c-title--description--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-modal-box__c-title--description--MarginTop)"
  };
  const c_modal_box_body_MinHeight = exports.c_modal_box_body_MinHeight = {
    "name": "--pf-c-modal-box--body--MinHeight",
    "value": "calc(1rem*1.5)",
    "var": "var(--pf-c-modal-box--body--MinHeight)"
  };
  const c_modal_box__description_body_MarginTop = exports.c_modal_box__description_body_MarginTop = {
    "name": "--pf-c-modal-box__description--body--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-modal-box__description--body--MarginTop)"
  };
  const c_modal_box_c_title_body_MarginTop = exports.c_modal_box_c_title_body_MarginTop = {
    "name": "--pf-c-modal-box--c-title--body--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-modal-box--c-title--body--MarginTop)"
  };
  const c_modal_box_c_button_Top = exports.c_modal_box_c_button_Top = {
    "name": "--pf-c-modal-box--c-button--Top",
    "value": "calc(2rem - 0.375rem + 0.0625rem)",
    "var": "var(--pf-c-modal-box--c-button--Top)"
  };
  const c_modal_box_c_button_Right = exports.c_modal_box_c_button_Right = {
    "name": "--pf-c-modal-box--c-button--Right",
    "value": "1rem",
    "var": "var(--pf-c-modal-box--c-button--Right)"
  };
  const c_modal_box_c_button_sibling_MarginRight = exports.c_modal_box_c_button_sibling_MarginRight = {
    "name": "--pf-c-modal-box--c-button--sibling--MarginRight",
    "value": "2rem",
    "var": "var(--pf-c-modal-box--c-button--sibling--MarginRight)"
  };
  const c_modal_box__footer_MarginTop = exports.c_modal_box__footer_MarginTop = {
    "name": "--pf-c-modal-box__footer--MarginTop",
    "value": "2rem",
    "var": "var(--pf-c-modal-box__footer--MarginTop)"
  };
  const c_modal_box__footer_c_button_MarginRight = exports.c_modal_box__footer_c_button_MarginRight = {
    "name": "--pf-c-modal-box__footer--c-button--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-modal-box__footer--c-button--MarginRight)"
  };
  const c_modal_box__footer_c_button_sm_MarginRight = exports.c_modal_box__footer_c_button_sm_MarginRight = {
    "name": "--pf-c-modal-box__footer--c-button--sm--MarginRight",
    "value": "calc(1rem/2)",
    "var": "var(--pf-c-modal-box__footer--c-button--sm--MarginRight)"
  };
  const c_modal_box__footer__c_button_first_of_type_MarginLeft = exports.c_modal_box__footer__c_button_first_of_type_MarginLeft = {
    "name": "--pf-c-modal-box__footer__c-button--first-of-type--MarginLeft",
    "value": "0",
    "var": "var(--pf-c-modal-box__footer__c-button--first-of-type--MarginLeft)"
  };
  const c_nav_Width = exports.c_nav_Width = {
    "name": "--pf-c-nav--Width",
    "value": "18.125rem",
    "var": "var(--pf-c-nav--Width)"
  };
  const c_nav_Transition = exports.c_nav_Transition = {
    "name": "--pf-c-nav--Transition",
    "value": "all 250ms ease-in-out",
    "var": "var(--pf-c-nav--Transition)"
  };
  const c_nav__item_m_expanded__toggle_icon_Transform = exports.c_nav__item_m_expanded__toggle_icon_Transform = {
    "name": "--pf-c-nav__item--m-expanded__toggle-icon--Transform",
    "value": "rotate(90deg)",
    "var": "var(--pf-c-nav__item--m-expanded__toggle-icon--Transform)"
  };
  const c_nav_m_dark__list_link_PaddingTop = exports.c_nav_m_dark__list_link_PaddingTop = {
    "name": "--pf-c-nav--m-dark__list-link--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-nav--m-dark__list-link--PaddingTop)"
  };
  const c_nav_m_dark__list_link_PaddingBottom = exports.c_nav_m_dark__list_link_PaddingBottom = {
    "name": "--pf-c-nav--m-dark__list-link--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-nav--m-dark__list-link--PaddingBottom)"
  };
  const c_nav_m_dark__list_link_Color = exports.c_nav_m_dark__list_link_Color = {
    "name": "--pf-c-nav--m-dark__list-link--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-nav--m-dark__list-link--Color)"
  };
  const c_nav_m_dark__list_link_m_current_Color = exports.c_nav_m_dark__list_link_m_current_Color = {
    "name": "--pf-c-nav--m-dark__list-link--m-current--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__list-link--m-current--Color)"
  };
  const c_nav_m_dark__list_link_hover_Color = exports.c_nav_m_dark__list_link_hover_Color = {
    "name": "--pf-c-nav--m-dark__list-link--hover--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__list-link--hover--Color)"
  };
  const c_nav_m_dark__list_link_focus_Color = exports.c_nav_m_dark__list_link_focus_Color = {
    "name": "--pf-c-nav--m-dark__list-link--focus--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__list-link--focus--Color)"
  };
  const c_nav_m_dark__list_link_after_Bottom = exports.c_nav_m_dark__list_link_after_Bottom = {
    "name": "--pf-c-nav--m-dark__list-link--after--Bottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav--m-dark__list-link--after--Bottom)"
  };
  const c_nav_m_dark__list_link_active_Color = exports.c_nav_m_dark__list_link_active_Color = {
    "name": "--pf-c-nav--m-dark__list-link--active--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__list-link--active--Color)"
  };
  const c_nav_m_dark__simple_list_link_Color = exports.c_nav_m_dark__simple_list_link_Color = {
    "name": "--pf-c-nav--m-dark__simple-list-link--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--Color)"
  };
  const c_nav_m_dark__simple_list_link_hover_Color = exports.c_nav_m_dark__simple_list_link_hover_Color = {
    "name": "--pf-c-nav--m-dark__simple-list-link--hover--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--hover--Color)"
  };
  const c_nav_m_dark__simple_list_link_focus_Color = exports.c_nav_m_dark__simple_list_link_focus_Color = {
    "name": "--pf-c-nav--m-dark__simple-list-link--focus--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--focus--Color)"
  };
  const c_nav_m_dark__simple_list_link_active_Color = exports.c_nav_m_dark__simple_list_link_active_Color = {
    "name": "--pf-c-nav--m-dark__simple-list-link--active--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--active--Color)"
  };
  const c_nav_m_dark__simple_list_link_hover_BackgroundColor = exports.c_nav_m_dark__simple_list_link_hover_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__simple-list-link--hover--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--hover--BackgroundColor)"
  };
  const c_nav_m_dark__simple_list_link_focus_BackgroundColor = exports.c_nav_m_dark__simple_list_link_focus_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__simple-list-link--focus--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--focus--BackgroundColor)"
  };
  const c_nav_m_dark__simple_list_link_active_BackgroundColor = exports.c_nav_m_dark__simple_list_link_active_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__simple-list-link--active--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--active--BackgroundColor)"
  };
  const c_nav_m_dark__simple_list_link_m_current_Color = exports.c_nav_m_dark__simple_list_link_m_current_Color = {
    "name": "--pf-c-nav--m-dark__simple-list-link--m-current--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--m-current--Color)"
  };
  const c_nav_m_dark__simple_list_link_m_current_BackgroundColor = exports.c_nav_m_dark__simple_list_link_m_current_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__simple-list-link--m-current--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav--m-dark__simple-list-link--m-current--BackgroundColor)"
  };
  const c_nav_m_dark__item_m_expanded_PaddingBottom = exports.c_nav_m_dark__item_m_expanded_PaddingBottom = {
    "name": "--pf-c-nav--m-dark__item--m-expanded--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav--m-dark__item--m-expanded--PaddingBottom)"
  };
  const c_nav_m_dark__item_m_expanded__list_link_after_Bottom = exports.c_nav_m_dark__item_m_expanded__list_link_after_Bottom = {
    "name": "--pf-c-nav--m-dark__item--m-expanded__list-link--after--Bottom",
    "value": "0",
    "var": "var(--pf-c-nav--m-dark__item--m-expanded__list-link--after--Bottom)"
  };
  const c_nav_m_dark__item_m_expanded__list_link_PaddingBottom = exports.c_nav_m_dark__item_m_expanded__list_link_PaddingBottom = {
    "name": "--pf-c-nav--m-dark__item--m-expanded__list-link--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav--m-dark__item--m-expanded__list-link--PaddingBottom)"
  };
  const c_nav_m_dark__item_m_current_BackgroundColor = exports.c_nav_m_dark__item_m_current_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__item--m-current--BackgroundColor",
    "value": "#3c3f42",
    "var": "var(--pf-c-nav--m-dark__item--m-current--BackgroundColor)"
  };
  const c_nav_m_dark__item_m_current__simple_list_link_Color = exports.c_nav_m_dark__item_m_current__simple_list_link_Color = {
    "name": "--pf-c-nav--m-dark__item--m-current__simple-list-link--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__item--m-current__simple-list-link--Color)"
  };
  const c_nav_m_dark__item_m_current__list_link_Color = exports.c_nav_m_dark__item_m_current__list_link_Color = {
    "name": "--pf-c-nav--m-dark__item--m-current__list-link--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__item--m-current__list-link--Color)"
  };
  const c_nav_m_dark__section_title_Color = exports.c_nav_m_dark__section_title_Color = {
    "name": "--pf-c-nav--m-dark__section-title--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav--m-dark__section-title--Color)"
  };
  const c_nav_m_dark__c_divider_BackgroundColor = exports.c_nav_m_dark__c_divider_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__c-divider--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav--m-dark__c-divider--BackgroundColor)"
  };
  const c_nav_m_dark__separator_BackgroundColor = exports.c_nav_m_dark__separator_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__separator--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav--m-dark__separator--BackgroundColor)"
  };
  const c_nav_m_dark__item_m_current__c_divider_BackgroundColor = exports.c_nav_m_dark__item_m_current__c_divider_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__item--m-current__c-divider--BackgroundColor",
    "value": "#212427",
    "var": "var(--pf-c-nav--m-dark__item--m-current__c-divider--BackgroundColor)"
  };
  const c_nav_m_dark__item_m_current__separator_BackgroundColor = exports.c_nav_m_dark__item_m_current__separator_BackgroundColor = {
    "name": "--pf-c-nav--m-dark__item--m-current__separator--BackgroundColor",
    "value": "#212427",
    "var": "var(--pf-c-nav--m-dark__item--m-current__separator--BackgroundColor)"
  };
  const c_nav_m_dark__subnav_MarginTop = exports.c_nav_m_dark__subnav_MarginTop = {
    "name": "--pf-c-nav--m-dark__subnav--MarginTop",
    "value": "0",
    "var": "var(--pf-c-nav--m-dark__subnav--MarginTop)"
  };
  const c_nav_m_dark__list_link_after_Width = exports.c_nav_m_dark__list_link_after_Width = {
    "name": "--pf-c-nav--m-dark__list-link--after--Width",
    "value": "2.5rem",
    "var": "var(--pf-c-nav--m-dark__list-link--after--Width)"
  };
  const c_nav__c_divider_MarginTop = exports.c_nav__c_divider_MarginTop = {
    "name": "--pf-c-nav__c-divider--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__c-divider--MarginTop)"
  };
  const c_nav__c_divider_MarginBottom = exports.c_nav__c_divider_MarginBottom = {
    "name": "--pf-c-nav__c-divider--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__c-divider--MarginBottom)"
  };
  const c_nav__c_divider_MarginLeft = exports.c_nav__c_divider_MarginLeft = {
    "name": "--pf-c-nav__c-divider--MarginLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__c-divider--MarginLeft)"
  };
  const c_nav__simple_list__c_divider_MarginLeft = exports.c_nav__simple_list__c_divider_MarginLeft = {
    "name": "--pf-c-nav__simple-list__c-divider--MarginLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__simple-list__c-divider--MarginLeft)"
  };
  const c_nav__simple_list_nested__c_divider_MarginLeft = exports.c_nav__simple_list_nested__c_divider_MarginLeft = {
    "name": "--pf-c-nav__simple-list--nested__c-divider--MarginLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__simple-list--nested__c-divider--MarginLeft)"
  };
  const c_nav__list_link_PaddingTop = exports.c_nav__list_link_PaddingTop = {
    "name": "--pf-c-nav__list-link--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__list-link--PaddingTop)"
  };
  const c_nav__list_link_PaddingRight = exports.c_nav__list_link_PaddingRight = {
    "name": "--pf-c-nav__list-link--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-nav__list-link--PaddingRight)"
  };
  const c_nav__list_link_md_PaddingRight = exports.c_nav__list_link_md_PaddingRight = {
    "name": "--pf-c-nav__list-link--md--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__list-link--md--PaddingRight)"
  };
  const c_nav__list_link_PaddingBottom = exports.c_nav__list_link_PaddingBottom = {
    "name": "--pf-c-nav__list-link--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__list-link--PaddingBottom)"
  };
  const c_nav__list_link_PaddingLeft = exports.c_nav__list_link_PaddingLeft = {
    "name": "--pf-c-nav__list-link--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-nav__list-link--PaddingLeft)"
  };
  const c_nav__list_link_md_PaddingLeft = exports.c_nav__list_link_md_PaddingLeft = {
    "name": "--pf-c-nav__list-link--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__list-link--md--PaddingLeft)"
  };
  const c_nav__list_link_after_Width = exports.c_nav__list_link_after_Width = {
    "name": "--pf-c-nav__list-link--after--Width",
    "value": "2.5rem",
    "var": "var(--pf-c-nav__list-link--after--Width)"
  };
  const c_nav__list_link_after_Height = exports.c_nav__list_link_after_Height = {
    "name": "--pf-c-nav__list-link--after--Height",
    "value": "0.1875rem",
    "var": "var(--pf-c-nav__list-link--after--Height)"
  };
  const c_nav__list_link_FontWeight = exports.c_nav__list_link_FontWeight = {
    "name": "--pf-c-nav__list-link--FontWeight",
    "value": "700",
    "var": "var(--pf-c-nav__list-link--FontWeight)"
  };
  const c_nav__list_link_active_FontWeight = exports.c_nav__list_link_active_FontWeight = {
    "name": "--pf-c-nav__list-link--active--FontWeight",
    "value": "400",
    "var": "var(--pf-c-nav__list-link--active--FontWeight)"
  };
  const c_nav__list_link_focus_FontWeight = exports.c_nav__list_link_focus_FontWeight = {
    "name": "--pf-c-nav__list-link--focus--FontWeight",
    "value": "400",
    "var": "var(--pf-c-nav__list-link--focus--FontWeight)"
  };
  const c_nav__list_link_Color = exports.c_nav__list_link_Color = {
    "name": "--pf-c-nav__list-link--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__list-link--Color)"
  };
  const c_nav__list_link_hover_Color = exports.c_nav__list_link_hover_Color = {
    "name": "--pf-c-nav__list-link--hover--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__list-link--hover--Color)"
  };
  const c_nav__list_link_active_Color = exports.c_nav__list_link_active_Color = {
    "name": "--pf-c-nav__list-link--active--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__list-link--active--Color)"
  };
  const c_nav__list_link_focus_Color = exports.c_nav__list_link_focus_Color = {
    "name": "--pf-c-nav__list-link--focus--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__list-link--focus--Color)"
  };
  const c_nav__list_link_after_Bottom = exports.c_nav__list_link_after_Bottom = {
    "name": "--pf-c-nav__list-link--after--Bottom",
    "value": "0",
    "var": "var(--pf-c-nav__list-link--after--Bottom)"
  };
  const c_nav__list_link_after_Left = exports.c_nav__list_link_after_Left = {
    "name": "--pf-c-nav__list-link--after--Left",
    "value": "1rem",
    "var": "var(--pf-c-nav__list-link--after--Left)"
  };
  const c_nav__list_link_after_md_Left = exports.c_nav__list_link_after_md_Left = {
    "name": "--pf-c-nav__list-link--after--md--Left",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__list-link--after--md--Left)"
  };
  const c_nav__list_link_hover_after_BackgroundColor = exports.c_nav__list_link_hover_after_BackgroundColor = {
    "name": "--pf-c-nav__list-link--hover--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__list-link--hover--after--BackgroundColor)"
  };
  const c_nav__list_link_active_after_BackgroundColor = exports.c_nav__list_link_active_after_BackgroundColor = {
    "name": "--pf-c-nav__list-link--active--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__list-link--active--after--BackgroundColor)"
  };
  const c_nav__list_link_focus_after_BackgroundColor = exports.c_nav__list_link_focus_after_BackgroundColor = {
    "name": "--pf-c-nav__list-link--focus--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__list-link--focus--after--BackgroundColor)"
  };
  const c_nav__simple_list_link_PaddingTop = exports.c_nav__simple_list_link_PaddingTop = {
    "name": "--pf-c-nav__simple-list-link--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__simple-list-link--PaddingTop)"
  };
  const c_nav__simple_list_link_PaddingRight = exports.c_nav__simple_list_link_PaddingRight = {
    "name": "--pf-c-nav__simple-list-link--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__simple-list-link--PaddingRight)"
  };
  const c_nav__simple_list_link_PaddingBottom = exports.c_nav__simple_list_link_PaddingBottom = {
    "name": "--pf-c-nav__simple-list-link--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__simple-list-link--PaddingBottom)"
  };
  const c_nav__simple_list_link_PaddingLeft = exports.c_nav__simple_list_link_PaddingLeft = {
    "name": "--pf-c-nav__simple-list-link--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__simple-list-link--PaddingLeft)"
  };
  const c_nav__simple_list_link_nested_PaddingLeft = exports.c_nav__simple_list_link_nested_PaddingLeft = {
    "name": "--pf-c-nav__simple-list-link--nested--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__simple-list-link--nested--PaddingLeft)"
  };
  const c_nav__simple_list_link_nested_md_PaddingLeft = exports.c_nav__simple_list_link_nested_md_PaddingLeft = {
    "name": "--pf-c-nav__simple-list-link--nested--md--PaddingLeft",
    "value": "calc(1.5rem + 1rem)",
    "var": "var(--pf-c-nav__simple-list-link--nested--md--PaddingLeft)"
  };
  const c_nav__simple_list_link_FontWeight = exports.c_nav__simple_list_link_FontWeight = {
    "name": "--pf-c-nav__simple-list-link--FontWeight",
    "value": "700",
    "var": "var(--pf-c-nav__simple-list-link--FontWeight)"
  };
  const c_nav__simple_list_link_active_FontWeight = exports.c_nav__simple_list_link_active_FontWeight = {
    "name": "--pf-c-nav__simple-list-link--active--FontWeight",
    "value": "700",
    "var": "var(--pf-c-nav__simple-list-link--active--FontWeight)"
  };
  const c_nav__simple_list_link_focus_FontWeight = exports.c_nav__simple_list_link_focus_FontWeight = {
    "name": "--pf-c-nav__simple-list-link--focus--FontWeight",
    "value": "700",
    "var": "var(--pf-c-nav__simple-list-link--focus--FontWeight)"
  };
  const c_nav__simple_list_link_Color = exports.c_nav__simple_list_link_Color = {
    "name": "--pf-c-nav__simple-list-link--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__simple-list-link--Color)"
  };
  const c_nav__simple_list_link_hover_Color = exports.c_nav__simple_list_link_hover_Color = {
    "name": "--pf-c-nav__simple-list-link--hover--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__simple-list-link--hover--Color)"
  };
  const c_nav__simple_list_link_active_Color = exports.c_nav__simple_list_link_active_Color = {
    "name": "--pf-c-nav__simple-list-link--active--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__simple-list-link--active--Color)"
  };
  const c_nav__simple_list_link_focus_Color = exports.c_nav__simple_list_link_focus_Color = {
    "name": "--pf-c-nav__simple-list-link--focus--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__simple-list-link--focus--Color)"
  };
  const c_nav__simple_list_link_hover_BackgroundColor = exports.c_nav__simple_list_link_hover_BackgroundColor = {
    "name": "--pf-c-nav__simple-list-link--hover--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav__simple-list-link--hover--BackgroundColor)"
  };
  const c_nav__simple_list_link_active_BackgroundColor = exports.c_nav__simple_list_link_active_BackgroundColor = {
    "name": "--pf-c-nav__simple-list-link--active--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav__simple-list-link--active--BackgroundColor)"
  };
  const c_nav__simple_list_link_focus_BackgroundColor = exports.c_nav__simple_list_link_focus_BackgroundColor = {
    "name": "--pf-c-nav__simple-list-link--focus--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav__simple-list-link--focus--BackgroundColor)"
  };
  const c_nav__horizontal_list_item_MarginRight = exports.c_nav__horizontal_list_item_MarginRight = {
    "name": "--pf-c-nav__horizontal-list-item--MarginRight",
    "value": "2rem",
    "var": "var(--pf-c-nav__horizontal-list-item--MarginRight)"
  };
  const c_nav__horizontal_list_link_PaddingTop = exports.c_nav__horizontal_list_link_PaddingTop = {
    "name": "--pf-c-nav__horizontal-list-link--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__horizontal-list-link--PaddingTop)"
  };
  const c_nav__horizontal_list_link_md_PaddingTop = exports.c_nav__horizontal_list_link_md_PaddingTop = {
    "name": "--pf-c-nav__horizontal-list-link--md--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-nav__horizontal-list-link--md--PaddingTop)"
  };
  const c_nav__horizontal_list_link_PaddingBottom = exports.c_nav__horizontal_list_link_PaddingBottom = {
    "name": "--pf-c-nav__horizontal-list-link--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__horizontal-list-link--PaddingBottom)"
  };
  const c_nav__horizontal_list_link_lg_PaddingBottom = exports.c_nav__horizontal_list_link_lg_PaddingBottom = {
    "name": "--pf-c-nav__horizontal-list-link--lg--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__horizontal-list-link--lg--PaddingBottom)"
  };
  const c_nav__horizontal_list_link_FontWeight = exports.c_nav__horizontal_list_link_FontWeight = {
    "name": "--pf-c-nav__horizontal-list-link--FontWeight",
    "value": "400",
    "var": "var(--pf-c-nav__horizontal-list-link--FontWeight)"
  };
  const c_nav__horizontal_list_link_Color = exports.c_nav__horizontal_list_link_Color = {
    "name": "--pf-c-nav__horizontal-list-link--Color",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--Color)"
  };
  const c_nav__horizontal_list_link_hover_Color = exports.c_nav__horizontal_list_link_hover_Color = {
    "name": "--pf-c-nav__horizontal-list-link--hover--Color",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--hover--Color)"
  };
  const c_nav__horizontal_list_link_active_Color = exports.c_nav__horizontal_list_link_active_Color = {
    "name": "--pf-c-nav__horizontal-list-link--active--Color",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--active--Color)"
  };
  const c_nav__horizontal_list_link_focus_Color = exports.c_nav__horizontal_list_link_focus_Color = {
    "name": "--pf-c-nav__horizontal-list-link--focus--Color",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--focus--Color)"
  };
  const c_nav__horizontal_list_link_after_Height = exports.c_nav__horizontal_list_link_after_Height = {
    "name": "--pf-c-nav__horizontal-list-link--after--Height",
    "value": "0.1875rem",
    "var": "var(--pf-c-nav__horizontal-list-link--after--Height)"
  };
  const c_nav__horizontal_list_link_hover_after_BackgroundColor = exports.c_nav__horizontal_list_link_hover_after_BackgroundColor = {
    "name": "--pf-c-nav__horizontal-list-link--hover--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--hover--after--BackgroundColor)"
  };
  const c_nav__horizontal_list_link_active_after_BackgroundColor = exports.c_nav__horizontal_list_link_active_after_BackgroundColor = {
    "name": "--pf-c-nav__horizontal-list-link--active--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--active--after--BackgroundColor)"
  };
  const c_nav__horizontal_list_link_focus_after_BackgroundColor = exports.c_nav__horizontal_list_link_focus_after_BackgroundColor = {
    "name": "--pf-c-nav__horizontal-list-link--focus--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--focus--after--BackgroundColor)"
  };
  const c_nav__tertiary_list_item_MarginRight = exports.c_nav__tertiary_list_item_MarginRight = {
    "name": "--pf-c-nav__tertiary-list-item--MarginRight",
    "value": "2rem",
    "var": "var(--pf-c-nav__tertiary-list-item--MarginRight)"
  };
  const c_nav__tertiary_list_link_PaddingTop = exports.c_nav__tertiary_list_link_PaddingTop = {
    "name": "--pf-c-nav__tertiary-list-link--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-nav__tertiary-list-link--PaddingTop)"
  };
  const c_nav__tertiary_list_link_PaddingBottom = exports.c_nav__tertiary_list_link_PaddingBottom = {
    "name": "--pf-c-nav__tertiary-list-link--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__tertiary-list-link--PaddingBottom)"
  };
  const c_nav__tertiary_list_link_FontWeight = exports.c_nav__tertiary_list_link_FontWeight = {
    "name": "--pf-c-nav__tertiary-list-link--FontWeight",
    "value": "400",
    "var": "var(--pf-c-nav__tertiary-list-link--FontWeight)"
  };
  const c_nav__tertiary_list_link_Color = exports.c_nav__tertiary_list_link_Color = {
    "name": "--pf-c-nav__tertiary-list-link--Color",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--Color)"
  };
  const c_nav__tertiary_list_link_hover_Color = exports.c_nav__tertiary_list_link_hover_Color = {
    "name": "--pf-c-nav__tertiary-list-link--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--hover--Color)"
  };
  const c_nav__tertiary_list_link_active_Color = exports.c_nav__tertiary_list_link_active_Color = {
    "name": "--pf-c-nav__tertiary-list-link--active--Color",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--active--Color)"
  };
  const c_nav__tertiary_list_link_focus_Color = exports.c_nav__tertiary_list_link_focus_Color = {
    "name": "--pf-c-nav__tertiary-list-link--focus--Color",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--focus--Color)"
  };
  const c_nav__tertiary_list_link_after_Height = exports.c_nav__tertiary_list_link_after_Height = {
    "name": "--pf-c-nav__tertiary-list-link--after--Height",
    "value": "0.1875rem",
    "var": "var(--pf-c-nav__tertiary-list-link--after--Height)"
  };
  const c_nav__tertiary_list_link_hover_after_BackgroundColor = exports.c_nav__tertiary_list_link_hover_after_BackgroundColor = {
    "name": "--pf-c-nav__tertiary-list-link--hover--after--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--hover--after--BackgroundColor)"
  };
  const c_nav__tertiary_list_link_active_after_BackgroundColor = exports.c_nav__tertiary_list_link_active_after_BackgroundColor = {
    "name": "--pf-c-nav__tertiary-list-link--active--after--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--active--after--BackgroundColor)"
  };
  const c_nav__tertiary_list_link_focus_after_BackgroundColor = exports.c_nav__tertiary_list_link_focus_after_BackgroundColor = {
    "name": "--pf-c-nav__tertiary-list-link--focus--after--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--focus--after--BackgroundColor)"
  };
  const c_nav__subnav_MarginTop = exports.c_nav__subnav_MarginTop = {
    "name": "--pf-c-nav__subnav--MarginTop",
    "value": "0",
    "var": "var(--pf-c-nav__subnav--MarginTop)"
  };
  const c_nav__subnav_MaxHeight = exports.c_nav__subnav_MaxHeight = {
    "name": "--pf-c-nav__subnav--MaxHeight",
    "value": "100%",
    "var": "var(--pf-c-nav__subnav--MaxHeight)"
  };
  const c_nav__list_toggle_PaddingRight = exports.c_nav__list_toggle_PaddingRight = {
    "name": "--pf-c-nav__list-toggle--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__list-toggle--PaddingRight)"
  };
  const c_nav__list_toggle_PaddingLeft = exports.c_nav__list_toggle_PaddingLeft = {
    "name": "--pf-c-nav__list-toggle--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__list-toggle--PaddingLeft)"
  };
  const c_nav__list_toggle_FontSize = exports.c_nav__list_toggle_FontSize = {
    "name": "--pf-c-nav__list-toggle--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-nav__list-toggle--FontSize)"
  };
  const c_nav__list_toggle_Transition = exports.c_nav__list_toggle_Transition = {
    "name": "--pf-c-nav__list-toggle--Transition",
    "value": ".2s ease-in 0s",
    "var": "var(--pf-c-nav__list-toggle--Transition)"
  };
  const c_nav__section_MarginTop = exports.c_nav__section_MarginTop = {
    "name": "--pf-c-nav__section--MarginTop",
    "value": "2rem",
    "var": "var(--pf-c-nav__section--MarginTop)"
  };
  const c_nav__section__section_MarginTop = exports.c_nav__section__section_MarginTop = {
    "name": "--pf-c-nav__section__section--MarginTop",
    "value": "2rem",
    "var": "var(--pf-c-nav__section__section--MarginTop)"
  };
  const c_nav__section_title_PaddingTop = exports.c_nav__section_title_PaddingTop = {
    "name": "--pf-c-nav__section-title--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__section-title--PaddingTop)"
  };
  const c_nav__section_title_PaddingRight = exports.c_nav__section_title_PaddingRight = {
    "name": "--pf-c-nav__section-title--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-nav__section-title--PaddingRight)"
  };
  const c_nav__section_title_md_PaddingRight = exports.c_nav__section_title_md_PaddingRight = {
    "name": "--pf-c-nav__section-title--md--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__section-title--md--PaddingRight)"
  };
  const c_nav__section_title_PaddingBottom = exports.c_nav__section_title_PaddingBottom = {
    "name": "--pf-c-nav__section-title--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__section-title--PaddingBottom)"
  };
  const c_nav__section_title_PaddingLeft = exports.c_nav__section_title_PaddingLeft = {
    "name": "--pf-c-nav__section-title--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-nav__section-title--PaddingLeft)"
  };
  const c_nav__section_title_md_PaddingLeft = exports.c_nav__section_title_md_PaddingLeft = {
    "name": "--pf-c-nav__section-title--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__section-title--md--PaddingLeft)"
  };
  const c_nav__section_title_FontSize = exports.c_nav__section_title_FontSize = {
    "name": "--pf-c-nav__section-title--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-nav__section-title--FontSize)"
  };
  const c_nav__section_title_Color = exports.c_nav__section_title_Color = {
    "name": "--pf-c-nav__section-title--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__section-title--Color)"
  };
  const c_nav__section_title_FontWeight = exports.c_nav__section_title_FontWeight = {
    "name": "--pf-c-nav__section-title--FontWeight",
    "value": "700",
    "var": "var(--pf-c-nav__section-title--FontWeight)"
  };
  const c_nav__list_link_m_current_Color = exports.c_nav__list_link_m_current_Color = {
    "name": "--pf-c-nav__list-link--m-current--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__list-link--m-current--Color)"
  };
  const c_nav__list_link_m_current_FontWeight = exports.c_nav__list_link_m_current_FontWeight = {
    "name": "--pf-c-nav__list-link--m-current--FontWeight",
    "value": "700",
    "var": "var(--pf-c-nav__list-link--m-current--FontWeight)"
  };
  const c_nav__list_link_m_current_after_BackgroundColor = exports.c_nav__list_link_m_current_after_BackgroundColor = {
    "name": "--pf-c-nav__list-link--m-current--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__list-link--m-current--after--BackgroundColor)"
  };
  const c_nav__simple_list_link_m_current_Color = exports.c_nav__simple_list_link_m_current_Color = {
    "name": "--pf-c-nav__simple-list-link--m-current--Color",
    "value": "#fff",
    "var": "var(--pf-c-nav__simple-list-link--m-current--Color)"
  };
  const c_nav__simple_list_link_m_current_FontWeight = exports.c_nav__simple_list_link_m_current_FontWeight = {
    "name": "--pf-c-nav__simple-list-link--m-current--FontWeight",
    "value": "700",
    "var": "var(--pf-c-nav__simple-list-link--m-current--FontWeight)"
  };
  const c_nav__simple_list_link_m_current_BackgroundColor = exports.c_nav__simple_list_link_m_current_BackgroundColor = {
    "name": "--pf-c-nav__simple-list-link--m-current--BackgroundColor",
    "value": "#4f5255",
    "var": "var(--pf-c-nav__simple-list-link--m-current--BackgroundColor)"
  };
  const c_nav__separator_Height = exports.c_nav__separator_Height = {
    "name": "--pf-c-nav__separator--Height",
    "value": "1px",
    "var": "var(--pf-c-nav__separator--Height)"
  };
  const c_nav__separator_BackgroundColor = exports.c_nav__separator_BackgroundColor = {
    "name": "--pf-c-nav__separator--BackgroundColor",
    "value": "#212427",
    "var": "var(--pf-c-nav__separator--BackgroundColor)"
  };
  const c_nav__separator_MarginTop = exports.c_nav__separator_MarginTop = {
    "name": "--pf-c-nav__separator--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__separator--MarginTop)"
  };
  const c_nav__separator_MarginBottom = exports.c_nav__separator_MarginBottom = {
    "name": "--pf-c-nav__separator--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__separator--MarginBottom)"
  };
  const c_nav__separator_MarginLeft = exports.c_nav__separator_MarginLeft = {
    "name": "--pf-c-nav__separator--MarginLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__separator--MarginLeft)"
  };
  const c_nav__simple_list__separator_MarginLeft = exports.c_nav__simple_list__separator_MarginLeft = {
    "name": "--pf-c-nav__simple-list__separator--MarginLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__simple-list__separator--MarginLeft)"
  };
  const c_nav__simple_list_nested__separator_MarginLeft = exports.c_nav__simple_list_nested__separator_MarginLeft = {
    "name": "--pf-c-nav__simple-list--nested__separator--MarginLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-nav__simple-list--nested__separator--MarginLeft)"
  };
  const c_nav__horizontal_list_link_m_current_Color = exports.c_nav__horizontal_list_link_m_current_Color = {
    "name": "--pf-c-nav__horizontal-list-link--m-current--Color",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--m-current--Color)"
  };
  const c_nav__horizontal_list_link_m_current_after_BackgroundColor = exports.c_nav__horizontal_list_link_m_current_after_BackgroundColor = {
    "name": "--pf-c-nav__horizontal-list-link--m-current--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__horizontal-list-link--m-current--after--BackgroundColor)"
  };
  const c_nav__tertiary_list_link_m_current_Color = exports.c_nav__tertiary_list_link_m_current_Color = {
    "name": "--pf-c-nav__tertiary-list-link--m-current--Color",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--m-current--Color)"
  };
  const c_nav__tertiary_list_link_m_current_after_BackgroundColor = exports.c_nav__tertiary_list_link_m_current_after_BackgroundColor = {
    "name": "--pf-c-nav__tertiary-list-link--m-current--after--BackgroundColor",
    "value": "#004080",
    "var": "var(--pf-c-nav__tertiary-list-link--m-current--after--BackgroundColor)"
  };
  const c_nav__scroll_button_Top = exports.c_nav__scroll_button_Top = {
    "name": "--pf-c-nav__scroll-button--Top",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__scroll-button--Top)"
  };
  const c_nav__scroll_button_ZIndex = exports.c_nav__scroll_button_ZIndex = {
    "name": "--pf-c-nav__scroll-button--ZIndex",
    "value": "100",
    "var": "var(--pf-c-nav__scroll-button--ZIndex)"
  };
  const c_nav__scroll_button_Width = exports.c_nav__scroll_button_Width = {
    "name": "--pf-c-nav__scroll-button--Width",
    "value": "2rem",
    "var": "var(--pf-c-nav__scroll-button--Width)"
  };
  const c_nav__scroll_button_Height = exports.c_nav__scroll_button_Height = {
    "name": "--pf-c-nav__scroll-button--Height",
    "value": "2.5rem",
    "var": "var(--pf-c-nav__scroll-button--Height)"
  };
  const c_nav__scroll_button_PaddingRight = exports.c_nav__scroll_button_PaddingRight = {
    "name": "--pf-c-nav__scroll-button--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__scroll-button--PaddingRight)"
  };
  const c_nav__scroll_button_PaddingLeft = exports.c_nav__scroll_button_PaddingLeft = {
    "name": "--pf-c-nav__scroll-button--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-nav__scroll-button--PaddingLeft)"
  };
  const c_nav__scroll_button_BackgroundColor = exports.c_nav__scroll_button_BackgroundColor = {
    "name": "--pf-c-nav__scroll-button--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-nav__scroll-button--BackgroundColor)"
  };
  const c_nav__scroll_button_hover_Color = exports.c_nav__scroll_button_hover_Color = {
    "name": "--pf-c-nav__scroll-button--hover--Color",
    "value": "#2b9af3",
    "var": "var(--pf-c-nav__scroll-button--hover--Color)"
  };
  const c_nav__scroll_button_nth_of_type_1_BoxShadow = exports.c_nav__scroll_button_nth_of_type_1_BoxShadow = {
    "name": "--pf-c-nav__scroll-button--nth-of-type-1--BoxShadow",
    "value": "20px 0 10px -4px hsla(0,0%,100%,0.7)",
    "var": "var(--pf-c-nav__scroll-button--nth-of-type-1--BoxShadow)"
  };
  const c_nav__scroll_button_nth_of_type_2_BoxShadow = exports.c_nav__scroll_button_nth_of_type_2_BoxShadow = {
    "name": "--pf-c-nav__scroll-button--nth-of-type-2--BoxShadow",
    "value": "-20px 0 10px -4px hsla(0,0%,100%,0.7)",
    "var": "var(--pf-c-nav__scroll-button--nth-of-type-2--BoxShadow)"
  };
  const c_nav__scroll_button_m_dark_nth_of_type_1_BoxShadow = exports.c_nav__scroll_button_m_dark_nth_of_type_1_BoxShadow = {
    "name": "--pf-c-nav__scroll-button--m-dark--nth-of-type-1--BoxShadow",
    "value": "20px 0 10px -4px rgba(21,21,21,0.7)",
    "var": "var(--pf-c-nav__scroll-button--m-dark--nth-of-type-1--BoxShadow)"
  };
  const c_nav__scroll_button_m_dark_nth_of_type_2_BoxShadow = exports.c_nav__scroll_button_m_dark_nth_of_type_2_BoxShadow = {
    "name": "--pf-c-nav__scroll-button--m-dark--nth-of-type-2--BoxShadow",
    "value": "-20px 0 10px -4px rgba(21,21,21,0.7)",
    "var": "var(--pf-c-nav__scroll-button--m-dark--nth-of-type-2--BoxShadow)"
  };
  const c_notification_badge_after_BorderColor = exports.c_notification_badge_after_BorderColor = {
    "name": "--pf-c-notification-badge--after--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-notification-badge--after--BorderColor)"
  };
  const c_notification_badge_after_BorderRadius = exports.c_notification_badge_after_BorderRadius = {
    "name": "--pf-c-notification-badge--after--BorderRadius",
    "value": "30em",
    "var": "var(--pf-c-notification-badge--after--BorderRadius)"
  };
  const c_notification_badge_after_BorderWidth = exports.c_notification_badge_after_BorderWidth = {
    "name": "--pf-c-notification-badge--after--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-notification-badge--after--BorderWidth)"
  };
  const c_notification_badge_after_Top = exports.c_notification_badge_after_Top = {
    "name": "--pf-c-notification-badge--after--Top",
    "value": "0",
    "var": "var(--pf-c-notification-badge--after--Top)"
  };
  const c_notification_badge_after_Right = exports.c_notification_badge_after_Right = {
    "name": "--pf-c-notification-badge--after--Right",
    "value": "0",
    "var": "var(--pf-c-notification-badge--after--Right)"
  };
  const c_notification_badge_after_Width = exports.c_notification_badge_after_Width = {
    "name": "--pf-c-notification-badge--after--Width",
    "value": "calc(0.5rem + 1px + 1px)",
    "var": "var(--pf-c-notification-badge--after--Width)"
  };
  const c_notification_badge_after_Height = exports.c_notification_badge_after_Height = {
    "name": "--pf-c-notification-badge--after--Height",
    "value": "calc(0.5rem + 1px + 1px)",
    "var": "var(--pf-c-notification-badge--after--Height)"
  };
  const c_notification_badge_after_BackgroundColor = exports.c_notification_badge_after_BackgroundColor = {
    "name": "--pf-c-notification-badge--after--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-notification-badge--after--BackgroundColor)"
  };
  const c_notification_badge_after_TranslateX = exports.c_notification_badge_after_TranslateX = {
    "name": "--pf-c-notification-badge--after--TranslateX",
    "value": "50%",
    "var": "var(--pf-c-notification-badge--after--TranslateX)"
  };
  const c_notification_badge_after_TranslateY = exports.c_notification_badge_after_TranslateY = {
    "name": "--pf-c-notification-badge--after--TranslateY",
    "value": "calc(1px*-1)",
    "var": "var(--pf-c-notification-badge--after--TranslateY)"
  };
  const c_notification_badge__i_Width = exports.c_notification_badge__i_Width = {
    "name": "--pf-c-notification-badge__i--Width",
    "value": "1rem",
    "var": "var(--pf-c-notification-badge__i--Width)"
  };
  const c_notification_badge__i_Height = exports.c_notification_badge__i_Height = {
    "name": "--pf-c-notification-badge__i--Height",
    "value": "1rem",
    "var": "var(--pf-c-notification-badge__i--Height)"
  };
  const c_notification_badge_m_read_after_BackgroundColor = exports.c_notification_badge_m_read_after_BackgroundColor = {
    "name": "--pf-c-notification-badge--m-read--after--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-notification-badge--m-read--after--BackgroundColor)"
  };
  const c_notification_badge_m_read_after_BorderColor = exports.c_notification_badge_m_read_after_BorderColor = {
    "name": "--pf-c-notification-badge--m-read--after--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-notification-badge--m-read--after--BorderColor)"
  };
  const c_notification_badge_m_unread_after_BackgroundColor = exports.c_notification_badge_m_unread_after_BackgroundColor = {
    "name": "--pf-c-notification-badge--m-unread--after--BackgroundColor",
    "value": "#2b9af3",
    "var": "var(--pf-c-notification-badge--m-unread--after--BackgroundColor)"
  };
  const c_notification_drawer_BackgroundColor = exports.c_notification_drawer_BackgroundColor = {
    "name": "--pf-c-notification-drawer--BackgroundColor",
    "value": "#fafafa",
    "var": "var(--pf-c-notification-drawer--BackgroundColor)"
  };
  const c_notification_drawer__header_PaddingTop = exports.c_notification_drawer__header_PaddingTop = {
    "name": "--pf-c-notification-drawer__header--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__header--PaddingTop)"
  };
  const c_notification_drawer__header_PaddingRight = exports.c_notification_drawer__header_PaddingRight = {
    "name": "--pf-c-notification-drawer__header--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__header--PaddingRight)"
  };
  const c_notification_drawer__header_PaddingBottom = exports.c_notification_drawer__header_PaddingBottom = {
    "name": "--pf-c-notification-drawer__header--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__header--PaddingBottom)"
  };
  const c_notification_drawer__header_PaddingLeft = exports.c_notification_drawer__header_PaddingLeft = {
    "name": "--pf-c-notification-drawer__header--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__header--PaddingLeft)"
  };
  const c_notification_drawer__header_BackgroundColor = exports.c_notification_drawer__header_BackgroundColor = {
    "name": "--pf-c-notification-drawer__header--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-notification-drawer__header--BackgroundColor)"
  };
  const c_notification_drawer__header_BoxShadow = exports.c_notification_drawer__header_BoxShadow = {
    "name": "--pf-c-notification-drawer__header--BoxShadow",
    "value": "0 0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-notification-drawer__header--BoxShadow)"
  };
  const c_notification_drawer__header_ZIndex = exports.c_notification_drawer__header_ZIndex = {
    "name": "--pf-c-notification-drawer__header--ZIndex",
    "value": "200",
    "var": "var(--pf-c-notification-drawer__header--ZIndex)"
  };
  const c_notification_drawer__header_title_FontSize = exports.c_notification_drawer__header_title_FontSize = {
    "name": "--pf-c-notification-drawer__header-title--FontSize",
    "value": "1.25rem",
    "var": "var(--pf-c-notification-drawer__header-title--FontSize)"
  };
  const c_notification_drawer__header_status_MarginLeft = exports.c_notification_drawer__header_status_MarginLeft = {
    "name": "--pf-c-notification-drawer__header-status--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__header-status--MarginLeft)"
  };
  const c_notification_drawer__body_ZIndex = exports.c_notification_drawer__body_ZIndex = {
    "name": "--pf-c-notification-drawer__body--ZIndex",
    "value": "100",
    "var": "var(--pf-c-notification-drawer__body--ZIndex)"
  };
  const c_notification_drawer__list_item_PaddingTop = exports.c_notification_drawer__list_item_PaddingTop = {
    "name": "--pf-c-notification-drawer__list-item--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__list-item--PaddingTop)"
  };
  const c_notification_drawer__list_item_PaddingRight = exports.c_notification_drawer__list_item_PaddingRight = {
    "name": "--pf-c-notification-drawer__list-item--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__list-item--PaddingRight)"
  };
  const c_notification_drawer__list_item_PaddingBottom = exports.c_notification_drawer__list_item_PaddingBottom = {
    "name": "--pf-c-notification-drawer__list-item--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__list-item--PaddingBottom)"
  };
  const c_notification_drawer__list_item_PaddingLeft = exports.c_notification_drawer__list_item_PaddingLeft = {
    "name": "--pf-c-notification-drawer__list-item--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__list-item--PaddingLeft)"
  };
  const c_notification_drawer__list_item_BackgroundColor = exports.c_notification_drawer__list_item_BackgroundColor = {
    "name": "--pf-c-notification-drawer__list-item--BackgroundColor",
    "value": "#fafafa",
    "var": "var(--pf-c-notification-drawer__list-item--BackgroundColor)"
  };
  const c_notification_drawer__list_item_BoxShadow = exports.c_notification_drawer__list_item_BoxShadow = {
    "name": "--pf-c-notification-drawer__list-item--BoxShadow",
    "value": "inset 0 0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-notification-drawer__list-item--BoxShadow)"
  };
  const c_notification_drawer__list_item_BorderBottomWidth = exports.c_notification_drawer__list_item_BorderBottomWidth = {
    "name": "--pf-c-notification-drawer__list-item--BorderBottomWidth",
    "value": "0",
    "var": "var(--pf-c-notification-drawer__list-item--BorderBottomWidth)"
  };
  const c_notification_drawer__list_item_BorderBottomColor = exports.c_notification_drawer__list_item_BorderBottomColor = {
    "name": "--pf-c-notification-drawer__list-item--BorderBottomColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-notification-drawer__list-item--BorderBottomColor)"
  };
  const c_notification_drawer__list_item_OutlineOffset = exports.c_notification_drawer__list_item_OutlineOffset = {
    "name": "--pf-c-notification-drawer__list-item--OutlineOffset",
    "value": "-0.25rem",
    "var": "var(--pf-c-notification-drawer__list-item--OutlineOffset)"
  };
  const c_notification_drawer__list_item_before_Width = exports.c_notification_drawer__list_item_before_Width = {
    "name": "--pf-c-notification-drawer__list-item--before--Width",
    "value": "3px",
    "var": "var(--pf-c-notification-drawer__list-item--before--Width)"
  };
  const c_notification_drawer__list_item_before_Top = exports.c_notification_drawer__list_item_before_Top = {
    "name": "--pf-c-notification-drawer__list-item--before--Top",
    "value": "calc(1px*-1)",
    "var": "var(--pf-c-notification-drawer__list-item--before--Top)"
  };
  const c_notification_drawer__list_item_before_Bottom = exports.c_notification_drawer__list_item_before_Bottom = {
    "name": "--pf-c-notification-drawer__list-item--before--Bottom",
    "value": "0",
    "var": "var(--pf-c-notification-drawer__list-item--before--Bottom)"
  };
  const c_notification_drawer__list_item_m_info__list_item_header_icon_Color = exports.c_notification_drawer__list_item_m_info__list_item_header_icon_Color = {
    "name": "--pf-c-notification-drawer__list-item--m-info__list-item-header-icon--Color",
    "value": "#73bcf7",
    "var": "var(--pf-c-notification-drawer__list-item--m-info__list-item-header-icon--Color)"
  };
  const c_notification_drawer__list_item_m_info__list_item_before_BackgroundColor = exports.c_notification_drawer__list_item_m_info__list_item_before_BackgroundColor = {
    "name": "--pf-c-notification-drawer__list-item--m-info__list-item--before--BackgroundColor",
    "value": "#73bcf7",
    "var": "var(--pf-c-notification-drawer__list-item--m-info__list-item--before--BackgroundColor)"
  };
  const c_notification_drawer__list_item_m_warning__list_item_header_icon_Color = exports.c_notification_drawer__list_item_m_warning__list_item_header_icon_Color = {
    "name": "--pf-c-notification-drawer__list-item--m-warning__list-item-header-icon--Color",
    "value": "#f0ab00",
    "var": "var(--pf-c-notification-drawer__list-item--m-warning__list-item-header-icon--Color)"
  };
  const c_notification_drawer__list_item_m_warning__list_item_before_BackgroundColor = exports.c_notification_drawer__list_item_m_warning__list_item_before_BackgroundColor = {
    "name": "--pf-c-notification-drawer__list-item--m-warning__list-item--before--BackgroundColor",
    "value": "#f0ab00",
    "var": "var(--pf-c-notification-drawer__list-item--m-warning__list-item--before--BackgroundColor)"
  };
  const c_notification_drawer__list_item_m_danger__list_item_header_icon_Color = exports.c_notification_drawer__list_item_m_danger__list_item_header_icon_Color = {
    "name": "--pf-c-notification-drawer__list-item--m-danger__list-item-header-icon--Color",
    "value": "#c9190b",
    "var": "var(--pf-c-notification-drawer__list-item--m-danger__list-item-header-icon--Color)"
  };
  const c_notification_drawer__list_item_m_danger__list_item_before_BackgroundColor = exports.c_notification_drawer__list_item_m_danger__list_item_before_BackgroundColor = {
    "name": "--pf-c-notification-drawer__list-item--m-danger__list-item--before--BackgroundColor",
    "value": "#c9190b",
    "var": "var(--pf-c-notification-drawer__list-item--m-danger__list-item--before--BackgroundColor)"
  };
  const c_notification_drawer__list_item_m_success__list_item_header_icon_Color = exports.c_notification_drawer__list_item_m_success__list_item_header_icon_Color = {
    "name": "--pf-c-notification-drawer__list-item--m-success__list-item-header-icon--Color",
    "value": "#92d400",
    "var": "var(--pf-c-notification-drawer__list-item--m-success__list-item-header-icon--Color)"
  };
  const c_notification_drawer__list_item_m_success__list_item_before_BackgroundColor = exports.c_notification_drawer__list_item_m_success__list_item_before_BackgroundColor = {
    "name": "--pf-c-notification-drawer__list-item--m-success__list-item--before--BackgroundColor",
    "value": "#92d400",
    "var": "var(--pf-c-notification-drawer__list-item--m-success__list-item--before--BackgroundColor)"
  };
  const c_notification_drawer__list_item_m_read_BackgroundColor = exports.c_notification_drawer__list_item_m_read_BackgroundColor = {
    "name": "--pf-c-notification-drawer__list-item--m-read--BackgroundColor",
    "value": "#fafafa",
    "var": "var(--pf-c-notification-drawer__list-item--m-read--BackgroundColor)"
  };
  const c_notification_drawer__list_item_m_read_BorderBottomColor = exports.c_notification_drawer__list_item_m_read_BorderBottomColor = {
    "name": "--pf-c-notification-drawer__list-item--m-read--BorderBottomColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-notification-drawer__list-item--m-read--BorderBottomColor)"
  };
  const c_notification_drawer__list_item_m_read_before_Top = exports.c_notification_drawer__list_item_m_read_before_Top = {
    "name": "--pf-c-notification-drawer__list-item--m-read--before--Top",
    "value": "calc(1px*-1)",
    "var": "var(--pf-c-notification-drawer__list-item--m-read--before--Top)"
  };
  const c_notification_drawer__list_item_m_read_before_Bottom = exports.c_notification_drawer__list_item_m_read_before_Bottom = {
    "name": "--pf-c-notification-drawer__list-item--m-read--before--Bottom",
    "value": "0",
    "var": "var(--pf-c-notification-drawer__list-item--m-read--before--Bottom)"
  };
  const c_notification_drawer__list_item_m_read_before_BackgroundColor = exports.c_notification_drawer__list_item_m_read_before_BackgroundColor = {
    "name": "--pf-c-notification-drawer__list-item--m-read--before--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-notification-drawer__list-item--m-read--before--BackgroundColor)"
  };
  const c_notification_drawer__list_item_list_item_m_read_before_Top = exports.c_notification_drawer__list_item_list_item_m_read_before_Top = {
    "name": "--pf-c-notification-drawer__list-item--list-item--m-read--before--Top",
    "value": "0",
    "var": "var(--pf-c-notification-drawer__list-item--list-item--m-read--before--Top)"
  };
  const c_notification_drawer__list_item_list_item_m_read_BoxShadow = exports.c_notification_drawer__list_item_list_item_m_read_BoxShadow = {
    "name": "--pf-c-notification-drawer__list-item--list-item--m-read--BoxShadow",
    "value": "inset 0 0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-c-notification-drawer__list-item--list-item--m-read--BoxShadow)"
  };
  const c_notification_drawer__list_item_m_hoverable_hover_ZIndex = exports.c_notification_drawer__list_item_m_hoverable_hover_ZIndex = {
    "name": "--pf-c-notification-drawer__list-item--m-hoverable--hover--ZIndex",
    "value": "100",
    "var": "var(--pf-c-notification-drawer__list-item--m-hoverable--hover--ZIndex)"
  };
  const c_notification_drawer__list_item_m_hoverable_hover_BoxShadow = exports.c_notification_drawer__list_item_m_hoverable_hover_BoxShadow = {
    "name": "--pf-c-notification-drawer__list-item--m-hoverable--hover--BoxShadow",
    "value": "0 -0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25),0 0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-c-notification-drawer__list-item--m-hoverable--hover--BoxShadow)"
  };
  const c_notification_drawer__list_item_header_MarginBottom = exports.c_notification_drawer__list_item_header_MarginBottom = {
    "name": "--pf-c-notification-drawer__list-item-header--MarginBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-notification-drawer__list-item-header--MarginBottom)"
  };
  const c_notification_drawer__list_item_header_icon_Color = exports.c_notification_drawer__list_item_header_icon_Color = {
    "name": "--pf-c-notification-drawer__list-item-header-icon--Color",
    "value": "#92d400",
    "var": "var(--pf-c-notification-drawer__list-item-header-icon--Color)"
  };
  const c_notification_drawer__list_item_header_icon_MarginRight = exports.c_notification_drawer__list_item_header_icon_MarginRight = {
    "name": "--pf-c-notification-drawer__list-item-header-icon--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-notification-drawer__list-item-header-icon--MarginRight)"
  };
  const c_notification_drawer__list_item_header_title_FontWeight = exports.c_notification_drawer__list_item_header_title_FontWeight = {
    "name": "--pf-c-notification-drawer__list-item-header-title--FontWeight",
    "value": "400",
    "var": "var(--pf-c-notification-drawer__list-item-header-title--FontWeight)"
  };
  const c_notification_drawer__list_item_m_read__list_item_header_title_FontWeight = exports.c_notification_drawer__list_item_m_read__list_item_header_title_FontWeight = {
    "name": "--pf-c-notification-drawer__list-item--m-read__list-item-header-title--FontWeight",
    "value": "400",
    "var": "var(--pf-c-notification-drawer__list-item--m-read__list-item-header-title--FontWeight)"
  };
  const c_notification_drawer__list_item_description_MarginBottom = exports.c_notification_drawer__list_item_description_MarginBottom = {
    "name": "--pf-c-notification-drawer__list-item-description--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-notification-drawer__list-item-description--MarginBottom)"
  };
  const c_notification_drawer__list_item_timestamp_Color = exports.c_notification_drawer__list_item_timestamp_Color = {
    "name": "--pf-c-notification-drawer__list-item-timestamp--Color",
    "value": "#737679",
    "var": "var(--pf-c-notification-drawer__list-item-timestamp--Color)"
  };
  const c_notification_drawer__list_item_timestamp_FontSize = exports.c_notification_drawer__list_item_timestamp_FontSize = {
    "name": "--pf-c-notification-drawer__list-item-timestamp--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-notification-drawer__list-item-timestamp--FontSize)"
  };
  const c_notification_drawer__group_m_expanded_group_BorderTopWidth = exports.c_notification_drawer__group_m_expanded_group_BorderTopWidth = {
    "name": "--pf-c-notification-drawer__group--m-expanded--group--BorderTopWidth",
    "value": "1px",
    "var": "var(--pf-c-notification-drawer__group--m-expanded--group--BorderTopWidth)"
  };
  const c_notification_drawer__group_m_expanded_group_BorderTopColor = exports.c_notification_drawer__group_m_expanded_group_BorderTopColor = {
    "name": "--pf-c-notification-drawer__group--m-expanded--group--BorderTopColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-notification-drawer__group--m-expanded--group--BorderTopColor)"
  };
  const c_notification_drawer__group_m_expanded_MinHeight = exports.c_notification_drawer__group_m_expanded_MinHeight = {
    "name": "--pf-c-notification-drawer__group--m-expanded--MinHeight",
    "value": "18.75rem",
    "var": "var(--pf-c-notification-drawer__group--m-expanded--MinHeight)"
  };
  const c_notification_drawer__group_toggle_PaddingTop = exports.c_notification_drawer__group_toggle_PaddingTop = {
    "name": "--pf-c-notification-drawer__group-toggle--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__group-toggle--PaddingTop)"
  };
  const c_notification_drawer__group_toggle_PaddingRight = exports.c_notification_drawer__group_toggle_PaddingRight = {
    "name": "--pf-c-notification-drawer__group-toggle--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__group-toggle--PaddingRight)"
  };
  const c_notification_drawer__group_toggle_PaddingBottom = exports.c_notification_drawer__group_toggle_PaddingBottom = {
    "name": "--pf-c-notification-drawer__group-toggle--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__group-toggle--PaddingBottom)"
  };
  const c_notification_drawer__group_toggle_PaddingLeft = exports.c_notification_drawer__group_toggle_PaddingLeft = {
    "name": "--pf-c-notification-drawer__group-toggle--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__group-toggle--PaddingLeft)"
  };
  const c_notification_drawer__group_toggle_BackgroundColor = exports.c_notification_drawer__group_toggle_BackgroundColor = {
    "name": "--pf-c-notification-drawer__group-toggle--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-notification-drawer__group-toggle--BackgroundColor)"
  };
  const c_notification_drawer__group_toggle_BorderColor = exports.c_notification_drawer__group_toggle_BorderColor = {
    "name": "--pf-c-notification-drawer__group-toggle--BorderColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-notification-drawer__group-toggle--BorderColor)"
  };
  const c_notification_drawer__group_toggle_BorderBottomWidth = exports.c_notification_drawer__group_toggle_BorderBottomWidth = {
    "name": "--pf-c-notification-drawer__group-toggle--BorderBottomWidth",
    "value": "1px",
    "var": "var(--pf-c-notification-drawer__group-toggle--BorderBottomWidth)"
  };
  const c_notification_drawer__group_toggle_OutlineOffset = exports.c_notification_drawer__group_toggle_OutlineOffset = {
    "name": "--pf-c-notification-drawer__group-toggle--OutlineOffset",
    "value": "-0.25rem",
    "var": "var(--pf-c-notification-drawer__group-toggle--OutlineOffset)"
  };
  const c_notification_drawer__group_toggle_count_MarginRight = exports.c_notification_drawer__group_toggle_count_MarginRight = {
    "name": "--pf-c-notification-drawer__group-toggle-count--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__group-toggle-count--MarginRight)"
  };
  const c_notification_drawer__group_toggle_icon_MarginRight = exports.c_notification_drawer__group_toggle_icon_MarginRight = {
    "name": "--pf-c-notification-drawer__group-toggle-icon--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-notification-drawer__group-toggle-icon--MarginRight)"
  };
  const c_notification_drawer__group_toggle_icon_Color = exports.c_notification_drawer__group_toggle_icon_Color = {
    "name": "--pf-c-notification-drawer__group-toggle-icon--Color",
    "value": "#737679",
    "var": "var(--pf-c-notification-drawer__group-toggle-icon--Color)"
  };
  const c_notification_drawer__group_toggle_icon_Transition = exports.c_notification_drawer__group_toggle_icon_Transition = {
    "name": "--pf-c-notification-drawer__group-toggle-icon--Transition",
    "value": ".2s ease-in 0s",
    "var": "var(--pf-c-notification-drawer__group-toggle-icon--Transition)"
  };
  const c_notification_drawer__group_m_expanded__group_toggle_icon_Transform = exports.c_notification_drawer__group_m_expanded__group_toggle_icon_Transform = {
    "name": "--pf-c-notification-drawer__group--m-expanded__group-toggle-icon--Transform",
    "value": "rotate(90deg)",
    "var": "var(--pf-c-notification-drawer__group--m-expanded__group-toggle-icon--Transform)"
  };
  const c_notification_drawer__list_item_before_BackgroundColor = exports.c_notification_drawer__list_item_before_BackgroundColor = {
    "name": "--pf-c-notification-drawer__list-item--before--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-notification-drawer__list-item--before--BackgroundColor)"
  };
  const c_options_menu__toggle_Background = exports.c_options_menu__toggle_Background = {
    "name": "--pf-c-options-menu__toggle--Background",
    "value": "#ededed",
    "var": "var(--pf-c-options-menu__toggle--Background)"
  };
  const c_options_menu__toggle_PaddingTop = exports.c_options_menu__toggle_PaddingTop = {
    "name": "--pf-c-options-menu__toggle--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-options-menu__toggle--PaddingTop)"
  };
  const c_options_menu__toggle_PaddingRight = exports.c_options_menu__toggle_PaddingRight = {
    "name": "--pf-c-options-menu__toggle--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__toggle--PaddingRight)"
  };
  const c_options_menu__toggle_PaddingBottom = exports.c_options_menu__toggle_PaddingBottom = {
    "name": "--pf-c-options-menu__toggle--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-options-menu__toggle--PaddingBottom)"
  };
  const c_options_menu__toggle_PaddingLeft = exports.c_options_menu__toggle_PaddingLeft = {
    "name": "--pf-c-options-menu__toggle--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__toggle--PaddingLeft)"
  };
  const c_options_menu__toggle_MinWidth = exports.c_options_menu__toggle_MinWidth = {
    "name": "--pf-c-options-menu__toggle--MinWidth",
    "value": "44px",
    "var": "var(--pf-c-options-menu__toggle--MinWidth)"
  };
  const c_options_menu__toggle_LineHeight = exports.c_options_menu__toggle_LineHeight = {
    "name": "--pf-c-options-menu__toggle--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-options-menu__toggle--LineHeight)"
  };
  const c_options_menu__toggle_BorderWidth = exports.c_options_menu__toggle_BorderWidth = {
    "name": "--pf-c-options-menu__toggle--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-options-menu__toggle--BorderWidth)"
  };
  const c_options_menu__toggle_BorderTopColor = exports.c_options_menu__toggle_BorderTopColor = {
    "name": "--pf-c-options-menu__toggle--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-options-menu__toggle--BorderTopColor)"
  };
  const c_options_menu__toggle_BorderRightColor = exports.c_options_menu__toggle_BorderRightColor = {
    "name": "--pf-c-options-menu__toggle--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-options-menu__toggle--BorderRightColor)"
  };
  const c_options_menu__toggle_BorderBottomColor = exports.c_options_menu__toggle_BorderBottomColor = {
    "name": "--pf-c-options-menu__toggle--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-options-menu__toggle--BorderBottomColor)"
  };
  const c_options_menu__toggle_BorderLeftColor = exports.c_options_menu__toggle_BorderLeftColor = {
    "name": "--pf-c-options-menu__toggle--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-options-menu__toggle--BorderLeftColor)"
  };
  const c_options_menu__toggle_Color = exports.c_options_menu__toggle_Color = {
    "name": "--pf-c-options-menu__toggle--Color",
    "value": "#151515",
    "var": "var(--pf-c-options-menu__toggle--Color)"
  };
  const c_options_menu__toggle_hover_BorderBottomColor = exports.c_options_menu__toggle_hover_BorderBottomColor = {
    "name": "--pf-c-options-menu__toggle--hover--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-options-menu__toggle--hover--BorderBottomColor)"
  };
  const c_options_menu__toggle_active_BorderBottomWidth = exports.c_options_menu__toggle_active_BorderBottomWidth = {
    "name": "--pf-c-options-menu__toggle--active--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-options-menu__toggle--active--BorderBottomWidth)"
  };
  const c_options_menu__toggle_active_BorderBottomColor = exports.c_options_menu__toggle_active_BorderBottomColor = {
    "name": "--pf-c-options-menu__toggle--active--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-options-menu__toggle--active--BorderBottomColor)"
  };
  const c_options_menu__toggle_focus_BorderBottomWidth = exports.c_options_menu__toggle_focus_BorderBottomWidth = {
    "name": "--pf-c-options-menu__toggle--focus--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-options-menu__toggle--focus--BorderBottomWidth)"
  };
  const c_options_menu__toggle_focus_BorderBottomColor = exports.c_options_menu__toggle_focus_BorderBottomColor = {
    "name": "--pf-c-options-menu__toggle--focus--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-options-menu__toggle--focus--BorderBottomColor)"
  };
  const c_options_menu__toggle_expanded_BorderBottomWidth = exports.c_options_menu__toggle_expanded_BorderBottomWidth = {
    "name": "--pf-c-options-menu__toggle--expanded--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-options-menu__toggle--expanded--BorderBottomWidth)"
  };
  const c_options_menu__toggle_expanded_BorderBottomColor = exports.c_options_menu__toggle_expanded_BorderBottomColor = {
    "name": "--pf-c-options-menu__toggle--expanded--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-options-menu__toggle--expanded--BorderBottomColor)"
  };
  const c_options_menu__toggle_disabled_BackgroundColor = exports.c_options_menu__toggle_disabled_BackgroundColor = {
    "name": "--pf-c-options-menu__toggle--disabled--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-options-menu__toggle--disabled--BackgroundColor)"
  };
  const c_options_menu__toggle_m_plain_Color = exports.c_options_menu__toggle_m_plain_Color = {
    "name": "--pf-c-options-menu__toggle--m-plain--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-options-menu__toggle--m-plain--Color)"
  };
  const c_options_menu__toggle_m_plain_hover_Color = exports.c_options_menu__toggle_m_plain_hover_Color = {
    "name": "--pf-c-options-menu__toggle--m-plain--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-options-menu__toggle--m-plain--hover--Color)"
  };
  const c_options_menu__toggle_m_plain_disabled_Color = exports.c_options_menu__toggle_m_plain_disabled_Color = {
    "name": "--pf-c-options-menu__toggle--m-plain--disabled--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-options-menu__toggle--m-plain--disabled--Color)"
  };
  const c_options_menu__toggle_icon_MarginRight = exports.c_options_menu__toggle_icon_MarginRight = {
    "name": "--pf-c-options-menu__toggle-icon--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__toggle-icon--MarginRight)"
  };
  const c_options_menu__toggle_icon_MarginLeft = exports.c_options_menu__toggle_icon_MarginLeft = {
    "name": "--pf-c-options-menu__toggle-icon--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-options-menu__toggle-icon--MarginLeft)"
  };
  const c_options_menu_m_top_m_expanded__toggle_icon_Transform = exports.c_options_menu_m_top_m_expanded__toggle_icon_Transform = {
    "name": "--pf-c-options-menu--m-top--m-expanded__toggle-icon--Transform",
    "value": "rotate(180deg)",
    "var": "var(--pf-c-options-menu--m-top--m-expanded__toggle-icon--Transform)"
  };
  const c_options_menu__toggle_button_Background = exports.c_options_menu__toggle_button_Background = {
    "name": "--pf-c-options-menu__toggle-button--Background",
    "value": "transparent",
    "var": "var(--pf-c-options-menu__toggle-button--Background)"
  };
  const c_options_menu__toggle_button_PaddingTop = exports.c_options_menu__toggle_button_PaddingTop = {
    "name": "--pf-c-options-menu__toggle-button--PaddingTop",
    "value": "0.375rem",
    "var": "var(--pf-c-options-menu__toggle-button--PaddingTop)"
  };
  const c_options_menu__toggle_button_PaddingRight = exports.c_options_menu__toggle_button_PaddingRight = {
    "name": "--pf-c-options-menu__toggle-button--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__toggle-button--PaddingRight)"
  };
  const c_options_menu__toggle_button_PaddingBottom = exports.c_options_menu__toggle_button_PaddingBottom = {
    "name": "--pf-c-options-menu__toggle-button--PaddingBottom",
    "value": "0.375rem",
    "var": "var(--pf-c-options-menu__toggle-button--PaddingBottom)"
  };
  const c_options_menu__toggle_button_PaddingLeft = exports.c_options_menu__toggle_button_PaddingLeft = {
    "name": "--pf-c-options-menu__toggle-button--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__toggle-button--PaddingLeft)"
  };
  const c_options_menu__menu_BackgroundColor = exports.c_options_menu__menu_BackgroundColor = {
    "name": "--pf-c-options-menu__menu--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-options-menu__menu--BackgroundColor)"
  };
  const c_options_menu__menu_BorderWidth = exports.c_options_menu__menu_BorderWidth = {
    "name": "--pf-c-options-menu__menu--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-options-menu__menu--BorderWidth)"
  };
  const c_options_menu__menu_BoxShadow = exports.c_options_menu__menu_BoxShadow = {
    "name": "--pf-c-options-menu__menu--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-options-menu__menu--BoxShadow)"
  };
  const c_options_menu__menu_PaddingTop = exports.c_options_menu__menu_PaddingTop = {
    "name": "--pf-c-options-menu__menu--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__menu--PaddingTop)"
  };
  const c_options_menu__menu_PaddingBottom = exports.c_options_menu__menu_PaddingBottom = {
    "name": "--pf-c-options-menu__menu--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__menu--PaddingBottom)"
  };
  const c_options_menu__menu_Top = exports.c_options_menu__menu_Top = {
    "name": "--pf-c-options-menu__menu--Top",
    "value": "0",
    "var": "var(--pf-c-options-menu__menu--Top)"
  };
  const c_options_menu__menu_ZIndex = exports.c_options_menu__menu_ZIndex = {
    "name": "--pf-c-options-menu__menu--ZIndex",
    "value": "200",
    "var": "var(--pf-c-options-menu__menu--ZIndex)"
  };
  const c_options_menu_m_top__menu_Top = exports.c_options_menu_m_top__menu_Top = {
    "name": "--pf-c-options-menu--m-top__menu--Top",
    "value": "0",
    "var": "var(--pf-c-options-menu--m-top__menu--Top)"
  };
  const c_options_menu_m_top__menu_Transform = exports.c_options_menu_m_top__menu_Transform = {
    "name": "--pf-c-options-menu--m-top__menu--Transform",
    "value": "translateY(calc(-100% - 0.25rem))",
    "var": "var(--pf-c-options-menu--m-top__menu--Transform)"
  };
  const c_options_menu__menu_item_Background = exports.c_options_menu__menu_item_Background = {
    "name": "--pf-c-options-menu__menu-item--Background",
    "value": "transparent",
    "var": "var(--pf-c-options-menu__menu-item--Background)"
  };
  const c_options_menu__menu_item_FontSize = exports.c_options_menu__menu_item_FontSize = {
    "name": "--pf-c-options-menu__menu-item--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-options-menu__menu-item--FontSize)"
  };
  const c_options_menu__menu_item_PaddingTop = exports.c_options_menu__menu_item_PaddingTop = {
    "name": "--pf-c-options-menu__menu-item--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__menu-item--PaddingTop)"
  };
  const c_options_menu__menu_item_PaddingRight = exports.c_options_menu__menu_item_PaddingRight = {
    "name": "--pf-c-options-menu__menu-item--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-options-menu__menu-item--PaddingRight)"
  };
  const c_options_menu__menu_item_PaddingBottom = exports.c_options_menu__menu_item_PaddingBottom = {
    "name": "--pf-c-options-menu__menu-item--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__menu-item--PaddingBottom)"
  };
  const c_options_menu__menu_item_PaddingLeft = exports.c_options_menu__menu_item_PaddingLeft = {
    "name": "--pf-c-options-menu__menu-item--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-options-menu__menu-item--PaddingLeft)"
  };
  const c_options_menu__menu_item_disabled_Color = exports.c_options_menu__menu_item_disabled_Color = {
    "name": "--pf-c-options-menu__menu-item--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-options-menu__menu-item--disabled--Color)"
  };
  const c_options_menu__menu_item_hover_BackgroundColor = exports.c_options_menu__menu_item_hover_BackgroundColor = {
    "name": "--pf-c-options-menu__menu-item--hover--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-options-menu__menu-item--hover--BackgroundColor)"
  };
  const c_options_menu__menu_item_disabled_BackgroundColor = exports.c_options_menu__menu_item_disabled_BackgroundColor = {
    "name": "--pf-c-options-menu__menu-item--disabled--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-options-menu__menu-item--disabled--BackgroundColor)"
  };
  const c_options_menu__menu_item_icon_Color = exports.c_options_menu__menu_item_icon_Color = {
    "name": "--pf-c-options-menu__menu-item-icon--Color",
    "value": "#06c",
    "var": "var(--pf-c-options-menu__menu-item-icon--Color)"
  };
  const c_options_menu__menu_item_icon_FontSize = exports.c_options_menu__menu_item_icon_FontSize = {
    "name": "--pf-c-options-menu__menu-item-icon--FontSize",
    "value": "0.625rem",
    "var": "var(--pf-c-options-menu__menu-item-icon--FontSize)"
  };
  const c_options_menu__menu_item_icon_PaddingLeft = exports.c_options_menu__menu_item_icon_PaddingLeft = {
    "name": "--pf-c-options-menu__menu-item-icon--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-options-menu__menu-item-icon--PaddingLeft)"
  };
  const c_options_menu__c_divider_MarginTop = exports.c_options_menu__c_divider_MarginTop = {
    "name": "--pf-c-options-menu__c-divider--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__c-divider--MarginTop)"
  };
  const c_options_menu__c_divider_MarginBottom = exports.c_options_menu__c_divider_MarginBottom = {
    "name": "--pf-c-options-menu__c-divider--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__c-divider--MarginBottom)"
  };
  const c_options_menu__separator_Height = exports.c_options_menu__separator_Height = {
    "name": "--pf-c-options-menu__separator--Height",
    "value": "1px",
    "var": "var(--pf-c-options-menu__separator--Height)"
  };
  const c_options_menu__separator_BackgroundColor = exports.c_options_menu__separator_BackgroundColor = {
    "name": "--pf-c-options-menu__separator--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-options-menu__separator--BackgroundColor)"
  };
  const c_options_menu__separator_MarginTop = exports.c_options_menu__separator_MarginTop = {
    "name": "--pf-c-options-menu__separator--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__separator--MarginTop)"
  };
  const c_options_menu__separator_MarginBottom = exports.c_options_menu__separator_MarginBottom = {
    "name": "--pf-c-options-menu__separator--MarginBottom",
    "value": "0",
    "var": "var(--pf-c-options-menu__separator--MarginBottom)"
  };
  const c_options_menu__group_group_PaddingTop = exports.c_options_menu__group_group_PaddingTop = {
    "name": "--pf-c-options-menu__group--group--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__group--group--PaddingTop)"
  };
  const c_options_menu__group_title_PaddingTop = exports.c_options_menu__group_title_PaddingTop = {
    "name": "--pf-c-options-menu__group-title--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__group-title--PaddingTop)"
  };
  const c_options_menu__group_title_PaddingRight = exports.c_options_menu__group_title_PaddingRight = {
    "name": "--pf-c-options-menu__group-title--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-options-menu__group-title--PaddingRight)"
  };
  const c_options_menu__group_title_PaddingBottom = exports.c_options_menu__group_title_PaddingBottom = {
    "name": "--pf-c-options-menu__group-title--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-options-menu__group-title--PaddingBottom)"
  };
  const c_options_menu__group_title_PaddingLeft = exports.c_options_menu__group_title_PaddingLeft = {
    "name": "--pf-c-options-menu__group-title--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-options-menu__group-title--PaddingLeft)"
  };
  const c_options_menu__group_title_FontSize = exports.c_options_menu__group_title_FontSize = {
    "name": "--pf-c-options-menu__group-title--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-options-menu__group-title--FontSize)"
  };
  const c_options_menu__group_title_FontWeight = exports.c_options_menu__group_title_FontWeight = {
    "name": "--pf-c-options-menu__group-title--FontWeight",
    "value": "700",
    "var": "var(--pf-c-options-menu__group-title--FontWeight)"
  };
  const c_options_menu__group_title_Color = exports.c_options_menu__group_title_Color = {
    "name": "--pf-c-options-menu__group-title--Color",
    "value": "#737679",
    "var": "var(--pf-c-options-menu__group-title--Color)"
  };
  const c_overflow_menu_spacer_base = exports.c_overflow_menu_spacer_base = {
    "name": "--pf-c-overflow-menu--spacer--base",
    "value": "1rem",
    "var": "var(--pf-c-overflow-menu--spacer--base)"
  };
  const c_overflow_menu_spacer = exports.c_overflow_menu_spacer = {
    "name": "--pf-c-overflow-menu--spacer",
    "value": "0",
    "var": "var(--pf-c-overflow-menu--spacer)"
  };
  const c_overflow_menu__group_spacer = exports.c_overflow_menu__group_spacer = {
    "name": "--pf-c-overflow-menu__group--spacer",
    "value": "1rem",
    "var": "var(--pf-c-overflow-menu__group--spacer)"
  };
  const c_overflow_menu__item_spacer = exports.c_overflow_menu__item_spacer = {
    "name": "--pf-c-overflow-menu__item--spacer",
    "value": "1rem",
    "var": "var(--pf-c-overflow-menu__item--spacer)"
  };
  const c_overflow_menu_c_divider_m_vertical_spacer = exports.c_overflow_menu_c_divider_m_vertical_spacer = {
    "name": "--pf-c-overflow-menu--c-divider--m-vertical--spacer",
    "value": "1rem",
    "var": "var(--pf-c-overflow-menu--c-divider--m-vertical--spacer)"
  };
  const c_overflow_menu__group_m_button_group_spacer = exports.c_overflow_menu__group_m_button_group_spacer = {
    "name": "--pf-c-overflow-menu__group--m-button-group--spacer",
    "value": "1rem",
    "var": "var(--pf-c-overflow-menu__group--m-button-group--spacer)"
  };
  const c_overflow_menu__group_m_button_group_space_items = exports.c_overflow_menu__group_m_button_group_space_items = {
    "name": "--pf-c-overflow-menu__group--m-button-group--space-items",
    "value": "0.5rem",
    "var": "var(--pf-c-overflow-menu__group--m-button-group--space-items)"
  };
  const c_overflow_menu__group_m_icon_button_group_spacer = exports.c_overflow_menu__group_m_icon_button_group_spacer = {
    "name": "--pf-c-overflow-menu__group--m-icon-button-group--spacer",
    "value": "1rem",
    "var": "var(--pf-c-overflow-menu__group--m-icon-button-group--spacer)"
  };
  const c_overflow_menu__group_m_icon_button_group_space_items = exports.c_overflow_menu__group_m_icon_button_group_space_items = {
    "name": "--pf-c-overflow-menu__group--m-icon-button-group--space-items",
    "value": "0",
    "var": "var(--pf-c-overflow-menu__group--m-icon-button-group--space-items)"
  };
  const c_page_BackgroundColor = exports.c_page_BackgroundColor = {
    "name": "--pf-c-page--BackgroundColor",
    "value": "#151515",
    "var": "var(--pf-c-page--BackgroundColor)"
  };
  const c_page__header_ZIndex = exports.c_page__header_ZIndex = {
    "name": "--pf-c-page__header--ZIndex",
    "value": "300",
    "var": "var(--pf-c-page__header--ZIndex)"
  };
  const c_page__header_MinHeight = exports.c_page__header_MinHeight = {
    "name": "--pf-c-page__header--MinHeight",
    "value": "4.75rem",
    "var": "var(--pf-c-page__header--MinHeight)"
  };
  const c_page__header_brand_PaddingLeft = exports.c_page__header_brand_PaddingLeft = {
    "name": "--pf-c-page__header-brand--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-page__header-brand--PaddingLeft)"
  };
  const c_page__header_brand_md_PaddingRight = exports.c_page__header_brand_md_PaddingRight = {
    "name": "--pf-c-page__header-brand--md--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-page__header-brand--md--PaddingRight)"
  };
  const c_page__header_brand_md_PaddingLeft = exports.c_page__header_brand_md_PaddingLeft = {
    "name": "--pf-c-page__header-brand--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-page__header-brand--md--PaddingLeft)"
  };
  const c_page__header_sidebar_toggle__c_button_PaddingTop = exports.c_page__header_sidebar_toggle__c_button_PaddingTop = {
    "name": "--pf-c-page__header-sidebar-toggle__c-button--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-page__header-sidebar-toggle__c-button--PaddingTop)"
  };
  const c_page__header_sidebar_toggle__c_button_PaddingRight = exports.c_page__header_sidebar_toggle__c_button_PaddingRight = {
    "name": "--pf-c-page__header-sidebar-toggle__c-button--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-page__header-sidebar-toggle__c-button--PaddingRight)"
  };
  const c_page__header_sidebar_toggle__c_button_PaddingBottom = exports.c_page__header_sidebar_toggle__c_button_PaddingBottom = {
    "name": "--pf-c-page__header-sidebar-toggle__c-button--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-page__header-sidebar-toggle__c-button--PaddingBottom)"
  };
  const c_page__header_sidebar_toggle__c_button_PaddingLeft = exports.c_page__header_sidebar_toggle__c_button_PaddingLeft = {
    "name": "--pf-c-page__header-sidebar-toggle__c-button--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-page__header-sidebar-toggle__c-button--PaddingLeft)"
  };
  const c_page__header_sidebar_toggle__c_button_MarginRight = exports.c_page__header_sidebar_toggle__c_button_MarginRight = {
    "name": "--pf-c-page__header-sidebar-toggle__c-button--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-page__header-sidebar-toggle__c-button--MarginRight)"
  };
  const c_page__header_sidebar_toggle__c_button_MarginLeft = exports.c_page__header_sidebar_toggle__c_button_MarginLeft = {
    "name": "--pf-c-page__header-sidebar-toggle__c-button--MarginLeft",
    "value": "calc(0.5rem*-1)",
    "var": "var(--pf-c-page__header-sidebar-toggle__c-button--MarginLeft)"
  };
  const c_page__header_sidebar_toggle__c_button_md_MarginLeft = exports.c_page__header_sidebar_toggle__c_button_md_MarginLeft = {
    "name": "--pf-c-page__header-sidebar-toggle__c-button--md--MarginLeft",
    "value": "calc(0.5rem*-1)",
    "var": "var(--pf-c-page__header-sidebar-toggle__c-button--md--MarginLeft)"
  };
  const c_page__header_sidebar_toggle__c_button_FontSize = exports.c_page__header_sidebar_toggle__c_button_FontSize = {
    "name": "--pf-c-page__header-sidebar-toggle__c-button--FontSize",
    "value": "1.5rem",
    "var": "var(--pf-c-page__header-sidebar-toggle__c-button--FontSize)"
  };
  const c_page__header_brand_link_c_brand_MaxHeight = exports.c_page__header_brand_link_c_brand_MaxHeight = {
    "name": "--pf-c-page__header-brand-link--c-brand--MaxHeight",
    "value": "3.75rem",
    "var": "var(--pf-c-page__header-brand-link--c-brand--MaxHeight)"
  };
  const c_page__header_nav_PaddingLeft = exports.c_page__header_nav_PaddingLeft = {
    "name": "--pf-c-page__header-nav--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-page__header-nav--PaddingLeft)"
  };
  const c_page__header_nav_md_PaddingLeft = exports.c_page__header_nav_md_PaddingLeft = {
    "name": "--pf-c-page__header-nav--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-page__header-nav--md--PaddingLeft)"
  };
  const c_page__header_nav_lg_PaddingLeft = exports.c_page__header_nav_lg_PaddingLeft = {
    "name": "--pf-c-page__header-nav--lg--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-page__header-nav--lg--PaddingLeft)"
  };
  const c_page__header_nav_lg_MarginLeft = exports.c_page__header_nav_lg_MarginLeft = {
    "name": "--pf-c-page__header-nav--lg--MarginLeft",
    "value": "2rem",
    "var": "var(--pf-c-page__header-nav--lg--MarginLeft)"
  };
  const c_page__header_nav_lg_MarginRight = exports.c_page__header_nav_lg_MarginRight = {
    "name": "--pf-c-page__header-nav--lg--MarginRight",
    "value": "2rem",
    "var": "var(--pf-c-page__header-nav--lg--MarginRight)"
  };
  const c_page__header_nav_BackgroundColor = exports.c_page__header_nav_BackgroundColor = {
    "name": "--pf-c-page__header-nav--BackgroundColor",
    "value": "#212427",
    "var": "var(--pf-c-page__header-nav--BackgroundColor)"
  };
  const c_page__header_nav_lg_BackgroundColor = exports.c_page__header_nav_lg_BackgroundColor = {
    "name": "--pf-c-page__header-nav--lg--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-page__header-nav--lg--BackgroundColor)"
  };
  const c_page__header_nav_c_nav__scroll_button_nth_of_type_1_Left = exports.c_page__header_nav_c_nav__scroll_button_nth_of_type_1_Left = {
    "name": "--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--Left",
    "value": "calc(-1*(1rem - 0.25rem))",
    "var": "var(--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--Left)"
  };
  const c_page__header_nav_c_nav__scroll_button_nth_of_type_1_md_Left = exports.c_page__header_nav_c_nav__scroll_button_nth_of_type_1_md_Left = {
    "name": "--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--md--Left",
    "value": "calc(-1*(1rem - 0.25rem))",
    "var": "var(--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--md--Left)"
  };
  const c_page__header_nav_c_nav__scroll_button_nth_of_type_1_lg_Left = exports.c_page__header_nav_c_nav__scroll_button_nth_of_type_1_lg_Left = {
    "name": "--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--lg--Left",
    "value": "0",
    "var": "var(--pf-c-page__header-nav--c-nav__scroll-button--nth-of-type-1--lg--Left)"
  };
  const c_page__header_nav_c_nav__scroll_button_lg_BackgroundColor = exports.c_page__header_nav_c_nav__scroll_button_lg_BackgroundColor = {
    "name": "--pf-c-page__header-nav--c-nav__scroll-button--lg--BackgroundColor",
    "value": "#212427",
    "var": "var(--pf-c-page__header-nav--c-nav__scroll-button--lg--BackgroundColor)"
  };
  const c_page__header_nav_c_nav__scroll_button_lg_Top = exports.c_page__header_nav_c_nav__scroll_button_lg_Top = {
    "name": "--pf-c-page__header-nav--c-nav__scroll-button--lg--Top",
    "value": "0",
    "var": "var(--pf-c-page__header-nav--c-nav__scroll-button--lg--Top)"
  };
  const c_page__header_tools_MarginTop = exports.c_page__header_tools_MarginTop = {
    "name": "--pf-c-page__header-tools--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-page__header-tools--MarginTop)"
  };
  const c_page__header_tools_MarginRight = exports.c_page__header_tools_MarginRight = {
    "name": "--pf-c-page__header-tools--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-page__header-tools--MarginRight)"
  };
  const c_page__header_tools_MarginBottom = exports.c_page__header_tools_MarginBottom = {
    "name": "--pf-c-page__header-tools--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-page__header-tools--MarginBottom)"
  };
  const c_page__header_tools_md_MarginRight = exports.c_page__header_tools_md_MarginRight = {
    "name": "--pf-c-page__header-tools--md--MarginRight",
    "value": "1.5rem",
    "var": "var(--pf-c-page__header-tools--md--MarginRight)"
  };
  const c_page__header_tools_c_avatar_MarginLeft = exports.c_page__header_tools_c_avatar_MarginLeft = {
    "name": "--pf-c-page__header-tools--c-avatar--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-page__header-tools--c-avatar--MarginLeft)"
  };
  const c_page__header_tools_group_MarginLeft = exports.c_page__header_tools_group_MarginLeft = {
    "name": "--pf-c-page__header-tools-group--MarginLeft",
    "value": "2rem",
    "var": "var(--pf-c-page__header-tools-group--MarginLeft)"
  };
  const c_page__header_tools_c_button_m_selected_before_Width = exports.c_page__header_tools_c_button_m_selected_before_Width = {
    "name": "--pf-c-page__header-tools--c-button--m-selected--before--Width",
    "value": "2.25rem",
    "var": "var(--pf-c-page__header-tools--c-button--m-selected--before--Width)"
  };
  const c_page__header_tools_c_button_m_selected_before_Height = exports.c_page__header_tools_c_button_m_selected_before_Height = {
    "name": "--pf-c-page__header-tools--c-button--m-selected--before--Height",
    "value": "2.25rem",
    "var": "var(--pf-c-page__header-tools--c-button--m-selected--before--Height)"
  };
  const c_page__header_tools_c_button_m_selected_before_BackgroundColor = exports.c_page__header_tools_c_button_m_selected_before_BackgroundColor = {
    "name": "--pf-c-page__header-tools--c-button--m-selected--before--BackgroundColor",
    "value": "#3c3f42",
    "var": "var(--pf-c-page__header-tools--c-button--m-selected--before--BackgroundColor)"
  };
  const c_page__header_tools_c_button_m_selected_before_BorderRadius = exports.c_page__header_tools_c_button_m_selected_before_BorderRadius = {
    "name": "--pf-c-page__header-tools--c-button--m-selected--before--BorderRadius",
    "value": "30em",
    "var": "var(--pf-c-page__header-tools--c-button--m-selected--before--BorderRadius)"
  };
  const c_page__header_tools_c_button_m_selected_c_notification_badge_m_unread_after_BorderColor = exports.c_page__header_tools_c_button_m_selected_c_notification_badge_m_unread_after_BorderColor = {
    "name": "--pf-c-page__header-tools--c-button--m-selected--c-notification-badge--m-unread--after--BorderColor",
    "value": "#3c3f42",
    "var": "var(--pf-c-page__header-tools--c-button--m-selected--c-notification-badge--m-unread--after--BorderColor)"
  };
  const c_page__sidebar_ZIndex = exports.c_page__sidebar_ZIndex = {
    "name": "--pf-c-page__sidebar--ZIndex",
    "value": "200",
    "var": "var(--pf-c-page__sidebar--ZIndex)"
  };
  const c_page__sidebar_Width = exports.c_page__sidebar_Width = {
    "name": "--pf-c-page__sidebar--Width",
    "value": "80%",
    "var": "var(--pf-c-page__sidebar--Width)"
  };
  const c_page__sidebar_md_Width = exports.c_page__sidebar_md_Width = {
    "name": "--pf-c-page__sidebar--md--Width",
    "value": "18.125rem",
    "var": "var(--pf-c-page__sidebar--md--Width)"
  };
  const c_page__sidebar_BackgroundColor = exports.c_page__sidebar_BackgroundColor = {
    "name": "--pf-c-page__sidebar--BackgroundColor",
    "value": "#212427",
    "var": "var(--pf-c-page__sidebar--BackgroundColor)"
  };
  const c_page__sidebar_BoxShadow = exports.c_page__sidebar_BoxShadow = {
    "name": "--pf-c-page__sidebar--BoxShadow",
    "value": "0.75rem 0 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-c-page__sidebar--BoxShadow)"
  };
  const c_page__sidebar_Transition = exports.c_page__sidebar_Transition = {
    "name": "--pf-c-page__sidebar--Transition",
    "value": "all 250ms ease-in-out",
    "var": "var(--pf-c-page__sidebar--Transition)"
  };
  const c_page__sidebar_Transform = exports.c_page__sidebar_Transform = {
    "name": "--pf-c-page__sidebar--Transform",
    "value": "translateZ(0)",
    "var": "var(--pf-c-page__sidebar--Transform)"
  };
  const c_page__sidebar_m_expanded_Transform = exports.c_page__sidebar_m_expanded_Transform = {
    "name": "--pf-c-page__sidebar--m-expanded--Transform",
    "value": "translateZ(0)",
    "var": "var(--pf-c-page__sidebar--m-expanded--Transform)"
  };
  const c_page__sidebar_md_Transform = exports.c_page__sidebar_md_Transform = {
    "name": "--pf-c-page__sidebar--md--Transform",
    "value": "translateZ(0)",
    "var": "var(--pf-c-page__sidebar--md--Transform)"
  };
  const c_page__sidebar_m_dark_BackgroundColor = exports.c_page__sidebar_m_dark_BackgroundColor = {
    "name": "--pf-c-page__sidebar--m-dark--BackgroundColor",
    "value": "#212427",
    "var": "var(--pf-c-page__sidebar--m-dark--BackgroundColor)"
  };
  const c_page__sidebar_body_PaddingTop = exports.c_page__sidebar_body_PaddingTop = {
    "name": "--pf-c-page__sidebar-body--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-page__sidebar-body--PaddingTop)"
  };
  const c_page__sidebar_body_PaddingBottom = exports.c_page__sidebar_body_PaddingBottom = {
    "name": "--pf-c-page__sidebar-body--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-page__sidebar-body--PaddingBottom)"
  };
  const c_page__main_section_PaddingTop = exports.c_page__main_section_PaddingTop = {
    "name": "--pf-c-page__main-section--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-page__main-section--PaddingTop)"
  };
  const c_page__main_section_PaddingRight = exports.c_page__main_section_PaddingRight = {
    "name": "--pf-c-page__main-section--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-page__main-section--PaddingRight)"
  };
  const c_page__main_section_PaddingBottom = exports.c_page__main_section_PaddingBottom = {
    "name": "--pf-c-page__main-section--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-page__main-section--PaddingBottom)"
  };
  const c_page__main_section_PaddingLeft = exports.c_page__main_section_PaddingLeft = {
    "name": "--pf-c-page__main-section--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-page__main-section--PaddingLeft)"
  };
  const c_page__main_section_md_PaddingTop = exports.c_page__main_section_md_PaddingTop = {
    "name": "--pf-c-page__main-section--md--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-section--md--PaddingTop)"
  };
  const c_page__main_section_md_PaddingRight = exports.c_page__main_section_md_PaddingRight = {
    "name": "--pf-c-page__main-section--md--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-section--md--PaddingRight)"
  };
  const c_page__main_section_md_PaddingBottom = exports.c_page__main_section_md_PaddingBottom = {
    "name": "--pf-c-page__main-section--md--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-section--md--PaddingBottom)"
  };
  const c_page__main_section_md_PaddingLeft = exports.c_page__main_section_md_PaddingLeft = {
    "name": "--pf-c-page__main-section--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-section--md--PaddingLeft)"
  };
  const c_page__main_section_m_no_padding_mobile_md_PaddingTop = exports.c_page__main_section_m_no_padding_mobile_md_PaddingTop = {
    "name": "--pf-c-page__main-section--m-no-padding-mobile--md--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-page__main-section--m-no-padding-mobile--md--PaddingTop)"
  };
  const c_page__main_section_m_no_padding_mobile_md_PaddingRight = exports.c_page__main_section_m_no_padding_mobile_md_PaddingRight = {
    "name": "--pf-c-page__main-section--m-no-padding-mobile--md--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-page__main-section--m-no-padding-mobile--md--PaddingRight)"
  };
  const c_page__main_section_m_no_padding_mobile_md_PaddingBottom = exports.c_page__main_section_m_no_padding_mobile_md_PaddingBottom = {
    "name": "--pf-c-page__main-section--m-no-padding-mobile--md--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-page__main-section--m-no-padding-mobile--md--PaddingBottom)"
  };
  const c_page__main_section_m_no_padding_mobile_md_PaddingLeft = exports.c_page__main_section_m_no_padding_mobile_md_PaddingLeft = {
    "name": "--pf-c-page__main-section--m-no-padding-mobile--md--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-page__main-section--m-no-padding-mobile--md--PaddingLeft)"
  };
  const c_page__main_section_BackgroundColor = exports.c_page__main_section_BackgroundColor = {
    "name": "--pf-c-page__main-section--BackgroundColor",
    "value": "rgba(3,3,3,0.32)",
    "var": "var(--pf-c-page__main-section--BackgroundColor)"
  };
  const c_page__main_ZIndex = exports.c_page__main_ZIndex = {
    "name": "--pf-c-page__main--ZIndex",
    "value": "100",
    "var": "var(--pf-c-page__main--ZIndex)"
  };
  const c_page__main_nav_BackgroundColor = exports.c_page__main_nav_BackgroundColor = {
    "name": "--pf-c-page__main-nav--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-page__main-nav--BackgroundColor)"
  };
  const c_page__main_nav_PaddingTop = exports.c_page__main_nav_PaddingTop = {
    "name": "--pf-c-page__main-nav--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-page__main-nav--PaddingTop)"
  };
  const c_page__main_nav_PaddingRight = exports.c_page__main_nav_PaddingRight = {
    "name": "--pf-c-page__main-nav--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-page__main-nav--PaddingRight)"
  };
  const c_page__main_nav_PaddingBottom = exports.c_page__main_nav_PaddingBottom = {
    "name": "--pf-c-page__main-nav--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-page__main-nav--PaddingBottom)"
  };
  const c_page__main_nav_PaddingLeft = exports.c_page__main_nav_PaddingLeft = {
    "name": "--pf-c-page__main-nav--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-page__main-nav--PaddingLeft)"
  };
  const c_page__main_nav_md_PaddingRight = exports.c_page__main_nav_md_PaddingRight = {
    "name": "--pf-c-page__main-nav--md--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-nav--md--PaddingRight)"
  };
  const c_page__main_nav_md_PaddingLeft = exports.c_page__main_nav_md_PaddingLeft = {
    "name": "--pf-c-page__main-nav--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-nav--md--PaddingLeft)"
  };
  const c_page__main_nav_c_nav__scroll_button_nth_of_type_1_Left = exports.c_page__main_nav_c_nav__scroll_button_nth_of_type_1_Left = {
    "name": "--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-1--Left",
    "value": "calc(-1*(1.5rem - 0.25rem))",
    "var": "var(--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-1--Left)"
  };
  const c_page__main_nav_c_nav__scroll_button_nth_of_type_1_md_Left = exports.c_page__main_nav_c_nav__scroll_button_nth_of_type_1_md_Left = {
    "name": "--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-1--md--Left",
    "value": "calc(-1*(1rem - 0.25rem))",
    "var": "var(--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-1--md--Left)"
  };
  const c_page__main_nav_c_nav__scroll_button_nth_of_type_2_Right = exports.c_page__main_nav_c_nav__scroll_button_nth_of_type_2_Right = {
    "name": "--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-2--Right",
    "value": "calc(-1*(1.5rem - 0.25rem))",
    "var": "var(--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-2--Right)"
  };
  const c_page__main_nav_c_nav__scroll_button_nth_of_type_2_md_Right = exports.c_page__main_nav_c_nav__scroll_button_nth_of_type_2_md_Right = {
    "name": "--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-2--md--Right",
    "value": "calc(-1*(1rem - 0.25rem))",
    "var": "var(--pf-c-page__main-nav--c-nav__scroll-button--nth-of-type-2--md--Right)"
  };
  const c_page__main_breadcrumb_BackgroundColor = exports.c_page__main_breadcrumb_BackgroundColor = {
    "name": "--pf-c-page__main-breadcrumb--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-page__main-breadcrumb--BackgroundColor)"
  };
  const c_page__main_breadcrumb_PaddingTop = exports.c_page__main_breadcrumb_PaddingTop = {
    "name": "--pf-c-page__main-breadcrumb--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-page__main-breadcrumb--PaddingTop)"
  };
  const c_page__main_breadcrumb_PaddingRight = exports.c_page__main_breadcrumb_PaddingRight = {
    "name": "--pf-c-page__main-breadcrumb--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-page__main-breadcrumb--PaddingRight)"
  };
  const c_page__main_breadcrumb_PaddingBottom = exports.c_page__main_breadcrumb_PaddingBottom = {
    "name": "--pf-c-page__main-breadcrumb--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-page__main-breadcrumb--PaddingBottom)"
  };
  const c_page__main_breadcrumb_PaddingLeft = exports.c_page__main_breadcrumb_PaddingLeft = {
    "name": "--pf-c-page__main-breadcrumb--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-page__main-breadcrumb--PaddingLeft)"
  };
  const c_page__main_breadcrumb_md_PaddingTop = exports.c_page__main_breadcrumb_md_PaddingTop = {
    "name": "--pf-c-page__main-breadcrumb--md--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-breadcrumb--md--PaddingTop)"
  };
  const c_page__main_breadcrumb_md_PaddingRight = exports.c_page__main_breadcrumb_md_PaddingRight = {
    "name": "--pf-c-page__main-breadcrumb--md--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-breadcrumb--md--PaddingRight)"
  };
  const c_page__main_breadcrumb_md_PaddingLeft = exports.c_page__main_breadcrumb_md_PaddingLeft = {
    "name": "--pf-c-page__main-breadcrumb--md--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-page__main-breadcrumb--md--PaddingLeft)"
  };
  const c_page__main_nav_main_breadcrumb_PaddingTop = exports.c_page__main_nav_main_breadcrumb_PaddingTop = {
    "name": "--pf-c-page__main-nav--main-breadcrumb--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-page__main-nav--main-breadcrumb--PaddingTop)"
  };
  const c_page__main_section_m_light_BackgroundColor = exports.c_page__main_section_m_light_BackgroundColor = {
    "name": "--pf-c-page__main-section--m-light--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-page__main-section--m-light--BackgroundColor)"
  };
  const c_page__main_section_m_dark_100_BackgroundColor = exports.c_page__main_section_m_dark_100_BackgroundColor = {
    "name": "--pf-c-page__main-section--m-dark-100--BackgroundColor",
    "value": "rgba(3,3,3,0.62)",
    "var": "var(--pf-c-page__main-section--m-dark-100--BackgroundColor)"
  };
  const c_page__main_section_m_dark_200_BackgroundColor = exports.c_page__main_section_m_dark_200_BackgroundColor = {
    "name": "--pf-c-page__main-section--m-dark-200--BackgroundColor",
    "value": "rgba(3,3,3,0.32)",
    "var": "var(--pf-c-page__main-section--m-dark-200--BackgroundColor)"
  };
  const c_page__main_wizard_BorderTopColor = exports.c_page__main_wizard_BorderTopColor = {
    "name": "--pf-c-page__main-wizard--BorderTopColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-page__main-wizard--BorderTopColor)"
  };
  const c_page__main_wizard_BorderTopWidth = exports.c_page__main_wizard_BorderTopWidth = {
    "name": "--pf-c-page__main-wizard--BorderTopWidth",
    "value": "1px",
    "var": "var(--pf-c-page__main-wizard--BorderTopWidth)"
  };
  const c_pagination_child_MarginRight = exports.c_pagination_child_MarginRight = {
    "name": "--pf-c-pagination--child--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-pagination--child--MarginRight)"
  };
  const c_pagination_m_compact_child_MarginRight = exports.c_pagination_m_compact_child_MarginRight = {
    "name": "--pf-c-pagination--m-compact--child--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-pagination--m-compact--child--MarginRight)"
  };
  const c_pagination_c_options_menu__toggle_FontSize = exports.c_pagination_c_options_menu__toggle_FontSize = {
    "name": "--pf-c-pagination--c-options-menu__toggle--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-pagination--c-options-menu__toggle--FontSize)"
  };
  const c_pagination__menu_text_PaddingLeft = exports.c_pagination__menu_text_PaddingLeft = {
    "name": "--pf-c-pagination__menu-text--PaddingLeft",
    "value": "0.25rem",
    "var": "var(--pf-c-pagination__menu-text--PaddingLeft)"
  };
  const c_pagination__menu_text_FontSize = exports.c_pagination__menu_text_FontSize = {
    "name": "--pf-c-pagination__menu-text--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-pagination__menu-text--FontSize)"
  };
  const c_pagination__menu_text_Color = exports.c_pagination__menu_text_Color = {
    "name": "--pf-c-pagination__menu-text--Color",
    "value": "#737679",
    "var": "var(--pf-c-pagination__menu-text--Color)"
  };
  const c_pagination__nav_c_button_PaddingLeft = exports.c_pagination__nav_c_button_PaddingLeft = {
    "name": "--pf-c-pagination__nav--c-button--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-pagination__nav--c-button--PaddingLeft)"
  };
  const c_pagination__nav_c_button_PaddingRight = exports.c_pagination__nav_c_button_PaddingRight = {
    "name": "--pf-c-pagination__nav--c-button--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-pagination__nav--c-button--PaddingRight)"
  };
  const c_pagination__nav_c_button_FontSize = exports.c_pagination__nav_c_button_FontSize = {
    "name": "--pf-c-pagination__nav--c-button--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-pagination__nav--c-button--FontSize)"
  };
  const c_pagination_m_compact__nav_c_button_MarginLeft = exports.c_pagination_m_compact__nav_c_button_MarginLeft = {
    "name": "--pf-c-pagination--m-compact__nav--c-button--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-pagination--m-compact__nav--c-button--MarginLeft)"
  };
  const c_pagination__nav_page_select_FontSize = exports.c_pagination__nav_page_select_FontSize = {
    "name": "--pf-c-pagination__nav-page-select--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-pagination__nav-page-select--FontSize)"
  };
  const c_pagination__nav_page_select_PaddingLeft = exports.c_pagination__nav_page_select_PaddingLeft = {
    "name": "--pf-c-pagination__nav-page-select--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-pagination__nav-page-select--PaddingLeft)"
  };
  const c_pagination__nav_page_select_PaddingRight = exports.c_pagination__nav_page_select_PaddingRight = {
    "name": "--pf-c-pagination__nav-page-select--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-pagination__nav-page-select--PaddingRight)"
  };
  const c_pagination__nav_page_select_child_MarginRight = exports.c_pagination__nav_page_select_child_MarginRight = {
    "name": "--pf-c-pagination__nav-page-select--child--MarginRight",
    "value": "0.25rem",
    "var": "var(--pf-c-pagination__nav-page-select--child--MarginRight)"
  };
  const c_pagination__nav_page_select_c_form_control_width_base = exports.c_pagination__nav_page_select_c_form_control_width_base = {
    "name": "--pf-c-pagination__nav-page-select--c-form-control--width-base",
    "value": "3.5ch",
    "var": "var(--pf-c-pagination__nav-page-select--c-form-control--width-base)"
  };
  const c_pagination__nav_page_select_c_form_control_width_chars = exports.c_pagination__nav_page_select_c_form_control_width_chars = {
    "name": "--pf-c-pagination__nav-page-select--c-form-control--width-chars",
    "value": "2",
    "var": "var(--pf-c-pagination__nav-page-select--c-form-control--width-chars)"
  };
  const c_pagination__nav_page_select_c_form_control_Width = exports.c_pagination__nav_page_select_c_form_control_Width = {
    "name": "--pf-c-pagination__nav-page-select--c-form-control--Width",
    "value": "calc(3.5ch + 2*1ch)",
    "var": "var(--pf-c-pagination__nav-page-select--c-form-control--Width)"
  };
  const c_popover_MinWidth = exports.c_popover_MinWidth = {
    "name": "--pf-c-popover--MinWidth",
    "value": "calc(2rem + 18.75rem)",
    "var": "var(--pf-c-popover--MinWidth)"
  };
  const c_popover_MaxWidth = exports.c_popover_MaxWidth = {
    "name": "--pf-c-popover--MaxWidth",
    "value": "calc(2rem + 18.75rem)",
    "var": "var(--pf-c-popover--MaxWidth)"
  };
  const c_popover_BoxShadow = exports.c_popover_BoxShadow = {
    "name": "--pf-c-popover--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-popover--BoxShadow)"
  };
  const c_popover__content_BackgroundColor = exports.c_popover__content_BackgroundColor = {
    "name": "--pf-c-popover__content--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-popover__content--BackgroundColor)"
  };
  const c_popover__content_PaddingTop = exports.c_popover__content_PaddingTop = {
    "name": "--pf-c-popover__content--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-popover__content--PaddingTop)"
  };
  const c_popover__content_PaddingRight = exports.c_popover__content_PaddingRight = {
    "name": "--pf-c-popover__content--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-popover__content--PaddingRight)"
  };
  const c_popover__content_PaddingBottom = exports.c_popover__content_PaddingBottom = {
    "name": "--pf-c-popover__content--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-popover__content--PaddingBottom)"
  };
  const c_popover__content_PaddingLeft = exports.c_popover__content_PaddingLeft = {
    "name": "--pf-c-popover__content--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-popover__content--PaddingLeft)"
  };
  const c_popover__arrow_Width = exports.c_popover__arrow_Width = {
    "name": "--pf-c-popover__arrow--Width",
    "value": "1.5625rem",
    "var": "var(--pf-c-popover__arrow--Width)"
  };
  const c_popover__arrow_Height = exports.c_popover__arrow_Height = {
    "name": "--pf-c-popover__arrow--Height",
    "value": "1.5625rem",
    "var": "var(--pf-c-popover__arrow--Height)"
  };
  const c_popover__arrow_BoxShadow = exports.c_popover__arrow_BoxShadow = {
    "name": "--pf-c-popover__arrow--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-popover__arrow--BoxShadow)"
  };
  const c_popover__arrow_BackgroundColor = exports.c_popover__arrow_BackgroundColor = {
    "name": "--pf-c-popover__arrow--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-popover__arrow--BackgroundColor)"
  };
  const c_popover__arrow_m_top_Transform = exports.c_popover__arrow_m_top_Transform = {
    "name": "--pf-c-popover__arrow--m-top--Transform",
    "value": "translate(-50%,50%) rotate(45deg)",
    "var": "var(--pf-c-popover__arrow--m-top--Transform)"
  };
  const c_popover__arrow_m_right_Transform = exports.c_popover__arrow_m_right_Transform = {
    "name": "--pf-c-popover__arrow--m-right--Transform",
    "value": "translate(-50%,-50%) rotate(45deg)",
    "var": "var(--pf-c-popover__arrow--m-right--Transform)"
  };
  const c_popover__arrow_m_bottom_Transform = exports.c_popover__arrow_m_bottom_Transform = {
    "name": "--pf-c-popover__arrow--m-bottom--Transform",
    "value": "translate(-50%,-50%) rotate(45deg)",
    "var": "var(--pf-c-popover__arrow--m-bottom--Transform)"
  };
  const c_popover__arrow_m_left_Transform = exports.c_popover__arrow_m_left_Transform = {
    "name": "--pf-c-popover__arrow--m-left--Transform",
    "value": "translate(50%,-50%) rotate(45deg)",
    "var": "var(--pf-c-popover__arrow--m-left--Transform)"
  };
  const c_popover_c_button_MarginLeft = exports.c_popover_c_button_MarginLeft = {
    "name": "--pf-c-popover--c-button--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-popover--c-button--MarginLeft)"
  };
  const c_popover_c_button_Top = exports.c_popover_c_button_Top = {
    "name": "--pf-c-popover--c-button--Top",
    "value": "calc(2rem - 0.375rem + 0.0625rem)",
    "var": "var(--pf-c-popover--c-button--Top)"
  };
  const c_popover_c_button_Right = exports.c_popover_c_button_Right = {
    "name": "--pf-c-popover--c-button--Right",
    "value": "1rem",
    "var": "var(--pf-c-popover--c-button--Right)"
  };
  const c_popover_c_button_sibling_PaddingRight = exports.c_popover_c_button_sibling_PaddingRight = {
    "name": "--pf-c-popover--c-button--sibling--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-popover--c-button--sibling--PaddingRight)"
  };
  const c_popover_c_title_MarginBottom = exports.c_popover_c_title_MarginBottom = {
    "name": "--pf-c-popover--c-title--MarginBottom",
    "value": "1rem",
    "var": "var(--pf-c-popover--c-title--MarginBottom)"
  };
  const c_popover__footer_MarginTop = exports.c_popover__footer_MarginTop = {
    "name": "--pf-c-popover__footer--MarginTop",
    "value": "1.5rem",
    "var": "var(--pf-c-popover__footer--MarginTop)"
  };
  const c_progress_GridGap = exports.c_progress_GridGap = {
    "name": "--pf-c-progress--GridGap",
    "value": "1rem",
    "var": "var(--pf-c-progress--GridGap)"
  };
  const c_progress__bar_before_BackgroundColor = exports.c_progress__bar_before_BackgroundColor = {
    "name": "--pf-c-progress__bar--before--BackgroundColor",
    "value": "#c9190b",
    "var": "var(--pf-c-progress__bar--before--BackgroundColor)"
  };
  const c_progress__bar_Height = exports.c_progress__bar_Height = {
    "name": "--pf-c-progress__bar--Height",
    "value": "1.5rem",
    "var": "var(--pf-c-progress__bar--Height)"
  };
  const c_progress__bar_BackgroundColor = exports.c_progress__bar_BackgroundColor = {
    "name": "--pf-c-progress__bar--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-progress__bar--BackgroundColor)"
  };
  const c_progress__status_icon_Color = exports.c_progress__status_icon_Color = {
    "name": "--pf-c-progress__status-icon--Color",
    "value": "#c9190b",
    "var": "var(--pf-c-progress__status-icon--Color)"
  };
  const c_progress__status_icon_MarginLeft = exports.c_progress__status_icon_MarginLeft = {
    "name": "--pf-c-progress__status-icon--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-progress__status-icon--MarginLeft)"
  };
  const c_progress__bar_before_Opacity = exports.c_progress__bar_before_Opacity = {
    "name": "--pf-c-progress__bar--before--Opacity",
    "value": ".2",
    "var": "var(--pf-c-progress__bar--before--Opacity)"
  };
  const c_progress__indicator_Height = exports.c_progress__indicator_Height = {
    "name": "--pf-c-progress__indicator--Height",
    "value": "1rem",
    "var": "var(--pf-c-progress__indicator--Height)"
  };
  const c_progress__indicator_BackgroundColor = exports.c_progress__indicator_BackgroundColor = {
    "name": "--pf-c-progress__indicator--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-progress__indicator--BackgroundColor)"
  };
  const c_progress_m_danger__bar_BackgroundColor = exports.c_progress_m_danger__bar_BackgroundColor = {
    "name": "--pf-c-progress--m-danger__bar--BackgroundColor",
    "value": "#c9190b",
    "var": "var(--pf-c-progress--m-danger__bar--BackgroundColor)"
  };
  const c_progress_m_success__bar_BackgroundColor = exports.c_progress_m_success__bar_BackgroundColor = {
    "name": "--pf-c-progress--m-success__bar--BackgroundColor",
    "value": "#92d400",
    "var": "var(--pf-c-progress--m-success__bar--BackgroundColor)"
  };
  const c_progress_m_danger__status_icon_Color = exports.c_progress_m_danger__status_icon_Color = {
    "name": "--pf-c-progress--m-danger__status-icon--Color",
    "value": "#c9190b",
    "var": "var(--pf-c-progress--m-danger__status-icon--Color)"
  };
  const c_progress_m_success__status_icon_Color = exports.c_progress_m_success__status_icon_Color = {
    "name": "--pf-c-progress--m-success__status-icon--Color",
    "value": "#92d400",
    "var": "var(--pf-c-progress--m-success__status-icon--Color)"
  };
  const c_progress_m_inside__indicator_MinWidth = exports.c_progress_m_inside__indicator_MinWidth = {
    "name": "--pf-c-progress--m-inside__indicator--MinWidth",
    "value": "2rem",
    "var": "var(--pf-c-progress--m-inside__indicator--MinWidth)"
  };
  const c_progress_m_inside__measure_Color = exports.c_progress_m_inside__measure_Color = {
    "name": "--pf-c-progress--m-inside__measure--Color",
    "value": "#151515",
    "var": "var(--pf-c-progress--m-inside__measure--Color)"
  };
  const c_progress_m_success_m_inside__measure_Color = exports.c_progress_m_success_m_inside__measure_Color = {
    "name": "--pf-c-progress--m-success--m-inside__measure--Color",
    "value": "#151515",
    "var": "var(--pf-c-progress--m-success--m-inside__measure--Color)"
  };
  const c_progress_m_inside__measure_FontSize = exports.c_progress_m_inside__measure_FontSize = {
    "name": "--pf-c-progress--m-inside__measure--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-progress--m-inside__measure--FontSize)"
  };
  const c_progress_m_outside__measure_FontSize = exports.c_progress_m_outside__measure_FontSize = {
    "name": "--pf-c-progress--m-outside__measure--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-progress--m-outside__measure--FontSize)"
  };
  const c_progress_m_sm__bar_Height = exports.c_progress_m_sm__bar_Height = {
    "name": "--pf-c-progress--m-sm__bar--Height",
    "value": "0.5rem",
    "var": "var(--pf-c-progress--m-sm__bar--Height)"
  };
  const c_progress_m_sm__description_FontSize = exports.c_progress_m_sm__description_FontSize = {
    "name": "--pf-c-progress--m-sm__description--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-progress--m-sm__description--FontSize)"
  };
  const c_progress_m_sm__measure_FontSize = exports.c_progress_m_sm__measure_FontSize = {
    "name": "--pf-c-progress--m-sm__measure--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-progress--m-sm__measure--FontSize)"
  };
  const c_progress_m_lg__bar_Height = exports.c_progress_m_lg__bar_Height = {
    "name": "--pf-c-progress--m-lg__bar--Height",
    "value": "1.5rem",
    "var": "var(--pf-c-progress--m-lg__bar--Height)"
  };
  const c_radio_GridGap = exports.c_radio_GridGap = {
    "name": "--pf-c-radio--GridGap",
    "value": "0.25rem 0.5rem",
    "var": "var(--pf-c-radio--GridGap)"
  };
  const c_radio__label_disabled_Color = exports.c_radio__label_disabled_Color = {
    "name": "--pf-c-radio__label--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-radio__label--disabled--Color)"
  };
  const c_radio__label_Color = exports.c_radio__label_Color = {
    "name": "--pf-c-radio__label--Color",
    "value": "#737679",
    "var": "var(--pf-c-radio__label--Color)"
  };
  const c_radio__label_FontWeight = exports.c_radio__label_FontWeight = {
    "name": "--pf-c-radio__label--FontWeight",
    "value": "400",
    "var": "var(--pf-c-radio__label--FontWeight)"
  };
  const c_radio__label_FontSize = exports.c_radio__label_FontSize = {
    "name": "--pf-c-radio__label--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-radio__label--FontSize)"
  };
  const c_radio__label_LineHeight = exports.c_radio__label_LineHeight = {
    "name": "--pf-c-radio__label--LineHeight",
    "value": "1.3",
    "var": "var(--pf-c-radio__label--LineHeight)"
  };
  const c_radio__input_MarginTop = exports.c_radio__input_MarginTop = {
    "name": "--pf-c-radio__input--MarginTop",
    "value": "-0.1875rem",
    "var": "var(--pf-c-radio__input--MarginTop)"
  };
  const c_radio__description_FontSize = exports.c_radio__description_FontSize = {
    "name": "--pf-c-radio__description--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-radio__description--FontSize)"
  };
  const c_radio__description_Color = exports.c_radio__description_Color = {
    "name": "--pf-c-radio__description--Color",
    "value": "#737679",
    "var": "var(--pf-c-radio__description--Color)"
  };
  const c_select__toggle_PaddingTop = exports.c_select__toggle_PaddingTop = {
    "name": "--pf-c-select__toggle--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-select__toggle--PaddingTop)"
  };
  const c_select__toggle_PaddingRight = exports.c_select__toggle_PaddingRight = {
    "name": "--pf-c-select__toggle--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-select__toggle--PaddingRight)"
  };
  const c_select__toggle_PaddingBottom = exports.c_select__toggle_PaddingBottom = {
    "name": "--pf-c-select__toggle--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-select__toggle--PaddingBottom)"
  };
  const c_select__toggle_PaddingLeft = exports.c_select__toggle_PaddingLeft = {
    "name": "--pf-c-select__toggle--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-select__toggle--PaddingLeft)"
  };
  const c_select__toggle_MinWidth = exports.c_select__toggle_MinWidth = {
    "name": "--pf-c-select__toggle--MinWidth",
    "value": "44px",
    "var": "var(--pf-c-select__toggle--MinWidth)"
  };
  const c_select__toggle_FontSize = exports.c_select__toggle_FontSize = {
    "name": "--pf-c-select__toggle--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-select__toggle--FontSize)"
  };
  const c_select__toggle_FontWeight = exports.c_select__toggle_FontWeight = {
    "name": "--pf-c-select__toggle--FontWeight",
    "value": "400",
    "var": "var(--pf-c-select__toggle--FontWeight)"
  };
  const c_select__toggle_LineHeight = exports.c_select__toggle_LineHeight = {
    "name": "--pf-c-select__toggle--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-select__toggle--LineHeight)"
  };
  const c_select__toggle_BackgroundColor = exports.c_select__toggle_BackgroundColor = {
    "name": "--pf-c-select__toggle--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-select__toggle--BackgroundColor)"
  };
  const c_select__toggle_BorderWidth = exports.c_select__toggle_BorderWidth = {
    "name": "--pf-c-select__toggle--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-select__toggle--BorderWidth)"
  };
  const c_select__toggle_BorderTopColor = exports.c_select__toggle_BorderTopColor = {
    "name": "--pf-c-select__toggle--BorderTopColor",
    "value": "#ededed",
    "var": "var(--pf-c-select__toggle--BorderTopColor)"
  };
  const c_select__toggle_BorderRightColor = exports.c_select__toggle_BorderRightColor = {
    "name": "--pf-c-select__toggle--BorderRightColor",
    "value": "#ededed",
    "var": "var(--pf-c-select__toggle--BorderRightColor)"
  };
  const c_select__toggle_BorderBottomColor = exports.c_select__toggle_BorderBottomColor = {
    "name": "--pf-c-select__toggle--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-select__toggle--BorderBottomColor)"
  };
  const c_select__toggle_BorderLeftColor = exports.c_select__toggle_BorderLeftColor = {
    "name": "--pf-c-select__toggle--BorderLeftColor",
    "value": "#ededed",
    "var": "var(--pf-c-select__toggle--BorderLeftColor)"
  };
  const c_select__toggle_Color = exports.c_select__toggle_Color = {
    "name": "--pf-c-select__toggle--Color",
    "value": "#151515",
    "var": "var(--pf-c-select__toggle--Color)"
  };
  const c_select__toggle_hover_BorderBottomColor = exports.c_select__toggle_hover_BorderBottomColor = {
    "name": "--pf-c-select__toggle--hover--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-select__toggle--hover--BorderBottomColor)"
  };
  const c_select__toggle_active_BorderBottomWidth = exports.c_select__toggle_active_BorderBottomWidth = {
    "name": "--pf-c-select__toggle--active--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-select__toggle--active--BorderBottomWidth)"
  };
  const c_select__toggle_active_BorderBottomColor = exports.c_select__toggle_active_BorderBottomColor = {
    "name": "--pf-c-select__toggle--active--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-select__toggle--active--BorderBottomColor)"
  };
  const c_select__toggle_expanded_BorderBottomWidth = exports.c_select__toggle_expanded_BorderBottomWidth = {
    "name": "--pf-c-select__toggle--expanded--BorderBottomWidth",
    "value": "2px",
    "var": "var(--pf-c-select__toggle--expanded--BorderBottomWidth)"
  };
  const c_select__toggle_expanded_BorderBottomColor = exports.c_select__toggle_expanded_BorderBottomColor = {
    "name": "--pf-c-select__toggle--expanded--BorderBottomColor",
    "value": "#06c",
    "var": "var(--pf-c-select__toggle--expanded--BorderBottomColor)"
  };
  const c_select__toggle_disabled_BackgroundColor = exports.c_select__toggle_disabled_BackgroundColor = {
    "name": "--pf-c-select__toggle--disabled--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-select__toggle--disabled--BackgroundColor)"
  };
  const c_select__toggle_m_plain_BorderColor = exports.c_select__toggle_m_plain_BorderColor = {
    "name": "--pf-c-select__toggle--m-plain--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-select__toggle--m-plain--BorderColor)"
  };
  const c_select__toggle_m_plain_Color = exports.c_select__toggle_m_plain_Color = {
    "name": "--pf-c-select__toggle--m-plain--Color",
    "value": "#151515",
    "var": "var(--pf-c-select__toggle--m-plain--Color)"
  };
  const c_select__toggle_m_plain_hover_Color = exports.c_select__toggle_m_plain_hover_Color = {
    "name": "--pf-c-select__toggle--m-plain--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-select__toggle--m-plain--hover--Color)"
  };
  const c_select__toggle_wrapper_m_typeahead_PaddingTop = exports.c_select__toggle_wrapper_m_typeahead_PaddingTop = {
    "name": "--pf-c-select__toggle-wrapper--m-typeahead--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-select__toggle-wrapper--m-typeahead--PaddingTop)"
  };
  const c_select__toggle_wrapper_not_last_child_MarginRight = exports.c_select__toggle_wrapper_not_last_child_MarginRight = {
    "name": "--pf-c-select__toggle-wrapper--not-last-child--MarginRight",
    "value": "0.25rem",
    "var": "var(--pf-c-select__toggle-wrapper--not-last-child--MarginRight)"
  };
  const c_select__toggle_wrapper_MaxWidth = exports.c_select__toggle_wrapper_MaxWidth = {
    "name": "--pf-c-select__toggle-wrapper--MaxWidth",
    "value": "calc(100% - 1.5rem)",
    "var": "var(--pf-c-select__toggle-wrapper--MaxWidth)"
  };
  const c_select__toggle_wrapper_c_chip_group_MarginTop = exports.c_select__toggle_wrapper_c_chip_group_MarginTop = {
    "name": "--pf-c-select__toggle-wrapper--c-chip-group--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-select__toggle-wrapper--c-chip-group--MarginTop)"
  };
  const c_select__toggle_wrapper_c_chip_group_MarginBottom = exports.c_select__toggle_wrapper_c_chip_group_MarginBottom = {
    "name": "--pf-c-select__toggle-wrapper--c-chip-group--MarginBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-select__toggle-wrapper--c-chip-group--MarginBottom)"
  };
  const c_select__toggle_wrapper_c_chip_c_button_PaddingTop = exports.c_select__toggle_wrapper_c_chip_c_button_PaddingTop = {
    "name": "--pf-c-select__toggle-wrapper--c-chip--c-button--PaddingTop",
    "value": "0.25rem",
    "var": "var(--pf-c-select__toggle-wrapper--c-chip--c-button--PaddingTop)"
  };
  const c_select__toggle_wrapper_c_chip_c_button_PaddingBottom = exports.c_select__toggle_wrapper_c_chip_c_button_PaddingBottom = {
    "name": "--pf-c-select__toggle-wrapper--c-chip--c-button--PaddingBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-select__toggle-wrapper--c-chip--c-button--PaddingBottom)"
  };
  const c_select__toggle_typeahead_FlexBasis = exports.c_select__toggle_typeahead_FlexBasis = {
    "name": "--pf-c-select__toggle-typeahead--FlexBasis",
    "value": "10em",
    "var": "var(--pf-c-select__toggle-typeahead--FlexBasis)"
  };
  const c_select__toggle_typeahead_BackgroundColor = exports.c_select__toggle_typeahead_BackgroundColor = {
    "name": "--pf-c-select__toggle-typeahead--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-select__toggle-typeahead--BackgroundColor)"
  };
  const c_select__toggle_typeahead_BorderTop = exports.c_select__toggle_typeahead_BorderTop = {
    "name": "--pf-c-select__toggle-typeahead--BorderTop",
    "value": "none",
    "var": "var(--pf-c-select__toggle-typeahead--BorderTop)"
  };
  const c_select__toggle_typeahead_BorderRight = exports.c_select__toggle_typeahead_BorderRight = {
    "name": "--pf-c-select__toggle-typeahead--BorderRight",
    "value": "none",
    "var": "var(--pf-c-select__toggle-typeahead--BorderRight)"
  };
  const c_select__toggle_typeahead_BorderLeft = exports.c_select__toggle_typeahead_BorderLeft = {
    "name": "--pf-c-select__toggle-typeahead--BorderLeft",
    "value": "none",
    "var": "var(--pf-c-select__toggle-typeahead--BorderLeft)"
  };
  const c_select__toggle_typeahead_form_MinWidth = exports.c_select__toggle_typeahead_form_MinWidth = {
    "name": "--pf-c-select__toggle-typeahead-form--MinWidth",
    "value": "7.5rem",
    "var": "var(--pf-c-select__toggle-typeahead-form--MinWidth)"
  };
  const c_select__toggle_typeahead_active_PaddingBottom = exports.c_select__toggle_typeahead_active_PaddingBottom = {
    "name": "--pf-c-select__toggle-typeahead--active--PaddingBottom",
    "value": "calc(0.375rem - 1px)",
    "var": "var(--pf-c-select__toggle-typeahead--active--PaddingBottom)"
  };
  const c_select__toggle_icon_toggle_text_MarginLeft = exports.c_select__toggle_icon_toggle_text_MarginLeft = {
    "name": "--pf-c-select__toggle-icon--toggle-text--MarginLeft",
    "value": "0.25rem",
    "var": "var(--pf-c-select__toggle-icon--toggle-text--MarginLeft)"
  };
  const c_select__toggle_badge_PaddingLeft = exports.c_select__toggle_badge_PaddingLeft = {
    "name": "--pf-c-select__toggle-badge--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-select__toggle-badge--PaddingLeft)"
  };
  const c_select__toggle_arrow_MarginLeft = exports.c_select__toggle_arrow_MarginLeft = {
    "name": "--pf-c-select__toggle-arrow--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-select__toggle-arrow--MarginLeft)"
  };
  const c_select__toggle_arrow_MarginRight = exports.c_select__toggle_arrow_MarginRight = {
    "name": "--pf-c-select__toggle-arrow--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-select__toggle-arrow--MarginRight)"
  };
  const c_select__toggle_arrow_with_clear_MarginLeft = exports.c_select__toggle_arrow_with_clear_MarginLeft = {
    "name": "--pf-c-select__toggle-arrow--with-clear--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-select__toggle-arrow--with-clear--MarginLeft)"
  };
  const c_select__toggle_arrow_m_top_m_expanded__toggle_arrow_Transform = exports.c_select__toggle_arrow_m_top_m_expanded__toggle_arrow_Transform = {
    "name": "--pf-c-select__toggle-arrow--m-top--m-expanded__toggle-arrow--Transform",
    "value": "rotate(180deg)",
    "var": "var(--pf-c-select__toggle-arrow--m-top--m-expanded__toggle-arrow--Transform)"
  };
  const c_select__toggle_clear_PaddingRight = exports.c_select__toggle_clear_PaddingRight = {
    "name": "--pf-c-select__toggle-clear--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-select__toggle-clear--PaddingRight)"
  };
  const c_select__toggle_clear_PaddingLeft = exports.c_select__toggle_clear_PaddingLeft = {
    "name": "--pf-c-select__toggle-clear--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-select__toggle-clear--PaddingLeft)"
  };
  const c_select__toggle_button_PaddingLeft = exports.c_select__toggle_button_PaddingLeft = {
    "name": "--pf-c-select__toggle-button--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-select__toggle-button--PaddingLeft)"
  };
  const c_select__toggle_button_Color = exports.c_select__toggle_button_Color = {
    "name": "--pf-c-select__toggle-button--Color",
    "value": "#151515",
    "var": "var(--pf-c-select__toggle-button--Color)"
  };
  const c_select__menu_BackgroundColor = exports.c_select__menu_BackgroundColor = {
    "name": "--pf-c-select__menu--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-select__menu--BackgroundColor)"
  };
  const c_select__menu_BorderWidth = exports.c_select__menu_BorderWidth = {
    "name": "--pf-c-select__menu--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-select__menu--BorderWidth)"
  };
  const c_select__menu_BoxShadow = exports.c_select__menu_BoxShadow = {
    "name": "--pf-c-select__menu--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-select__menu--BoxShadow)"
  };
  const c_select__menu_PaddingTop = exports.c_select__menu_PaddingTop = {
    "name": "--pf-c-select__menu--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-select__menu--PaddingTop)"
  };
  const c_select__menu_PaddingBottom = exports.c_select__menu_PaddingBottom = {
    "name": "--pf-c-select__menu--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-select__menu--PaddingBottom)"
  };
  const c_select__menu_Top = exports.c_select__menu_Top = {
    "name": "--pf-c-select__menu--Top",
    "value": "calc(100% + 0.25rem)",
    "var": "var(--pf-c-select__menu--Top)"
  };
  const c_select__menu_ZIndex = exports.c_select__menu_ZIndex = {
    "name": "--pf-c-select__menu--ZIndex",
    "value": "200",
    "var": "var(--pf-c-select__menu--ZIndex)"
  };
  const c_select__menu_m_top_Transform = exports.c_select__menu_m_top_Transform = {
    "name": "--pf-c-select__menu--m-top--Transform",
    "value": "translateY(calc(-100% - 0.25rem))",
    "var": "var(--pf-c-select__menu--m-top--Transform)"
  };
  const c_select__menu_item_PaddingTop = exports.c_select__menu_item_PaddingTop = {
    "name": "--pf-c-select__menu-item--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-select__menu-item--PaddingTop)"
  };
  const c_select__menu_item_PaddingRight = exports.c_select__menu_item_PaddingRight = {
    "name": "--pf-c-select__menu-item--PaddingRight",
    "value": "3rem",
    "var": "var(--pf-c-select__menu-item--PaddingRight)"
  };
  const c_select__menu_item_m_selected_PaddingRight = exports.c_select__menu_item_m_selected_PaddingRight = {
    "name": "--pf-c-select__menu-item--m-selected--PaddingRight",
    "value": "3rem",
    "var": "var(--pf-c-select__menu-item--m-selected--PaddingRight)"
  };
  const c_select__menu_item_PaddingBottom = exports.c_select__menu_item_PaddingBottom = {
    "name": "--pf-c-select__menu-item--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-select__menu-item--PaddingBottom)"
  };
  const c_select__menu_item_PaddingLeft = exports.c_select__menu_item_PaddingLeft = {
    "name": "--pf-c-select__menu-item--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-select__menu-item--PaddingLeft)"
  };
  const c_select__menu_item_FontSize = exports.c_select__menu_item_FontSize = {
    "name": "--pf-c-select__menu-item--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-select__menu-item--FontSize)"
  };
  const c_select__menu_item_FontWeight = exports.c_select__menu_item_FontWeight = {
    "name": "--pf-c-select__menu-item--FontWeight",
    "value": "400",
    "var": "var(--pf-c-select__menu-item--FontWeight)"
  };
  const c_select__menu_item_LineHeight = exports.c_select__menu_item_LineHeight = {
    "name": "--pf-c-select__menu-item--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-select__menu-item--LineHeight)"
  };
  const c_select__menu_item_Color = exports.c_select__menu_item_Color = {
    "name": "--pf-c-select__menu-item--Color",
    "value": "#151515",
    "var": "var(--pf-c-select__menu-item--Color)"
  };
  const c_select__menu_item_disabled_Color = exports.c_select__menu_item_disabled_Color = {
    "name": "--pf-c-select__menu-item--disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-select__menu-item--disabled--Color)"
  };
  const c_select__menu_item_hover_BackgroundColor = exports.c_select__menu_item_hover_BackgroundColor = {
    "name": "--pf-c-select__menu-item--hover--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-select__menu-item--hover--BackgroundColor)"
  };
  const c_select__menu_item_disabled_BackgroundColor = exports.c_select__menu_item_disabled_BackgroundColor = {
    "name": "--pf-c-select__menu-item--disabled--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-select__menu-item--disabled--BackgroundColor)"
  };
  const c_select__menu_item_icon_Color = exports.c_select__menu_item_icon_Color = {
    "name": "--pf-c-select__menu-item-icon--Color",
    "value": "#06c",
    "var": "var(--pf-c-select__menu-item-icon--Color)"
  };
  const c_select__menu_item_icon_FontSize = exports.c_select__menu_item_icon_FontSize = {
    "name": "--pf-c-select__menu-item-icon--FontSize",
    "value": "0.625rem",
    "var": "var(--pf-c-select__menu-item-icon--FontSize)"
  };
  const c_select__menu_item_icon_Right = exports.c_select__menu_item_icon_Right = {
    "name": "--pf-c-select__menu-item-icon--Right",
    "value": "1rem",
    "var": "var(--pf-c-select__menu-item-icon--Right)"
  };
  const c_select__menu_item_icon_Top = exports.c_select__menu_item_icon_Top = {
    "name": "--pf-c-select__menu-item-icon--Top",
    "value": "50%",
    "var": "var(--pf-c-select__menu-item-icon--Top)"
  };
  const c_select__menu_item_icon_Transform = exports.c_select__menu_item_icon_Transform = {
    "name": "--pf-c-select__menu-item-icon--Transform",
    "value": "translateY(-50%)",
    "var": "var(--pf-c-select__menu-item-icon--Transform)"
  };
  const c_select__menu_item_match_FontWeight = exports.c_select__menu_item_match_FontWeight = {
    "name": "--pf-c-select__menu-item--match--FontWeight",
    "value": "700",
    "var": "var(--pf-c-select__menu-item--match--FontWeight)"
  };
  const c_select__menu_input_PaddingTop = exports.c_select__menu_input_PaddingTop = {
    "name": "--pf-c-select__menu-input--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-select__menu-input--PaddingTop)"
  };
  const c_select__menu_input_PaddingRight = exports.c_select__menu_input_PaddingRight = {
    "name": "--pf-c-select__menu-input--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-select__menu-input--PaddingRight)"
  };
  const c_select__menu_input_PaddingBottom = exports.c_select__menu_input_PaddingBottom = {
    "name": "--pf-c-select__menu-input--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-select__menu-input--PaddingBottom)"
  };
  const c_select__menu_input_PaddingLeft = exports.c_select__menu_input_PaddingLeft = {
    "name": "--pf-c-select__menu-input--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-select__menu-input--PaddingLeft)"
  };
  const c_select__separator_Height = exports.c_select__separator_Height = {
    "name": "--pf-c-select__separator--Height",
    "value": "1px",
    "var": "var(--pf-c-select__separator--Height)"
  };
  const c_select__separator_BackgroundColor = exports.c_select__separator_BackgroundColor = {
    "name": "--pf-c-select__separator--BackgroundColor",
    "value": "#ededed",
    "var": "var(--pf-c-select__separator--BackgroundColor)"
  };
  const c_select__separator_MarginTop = exports.c_select__separator_MarginTop = {
    "name": "--pf-c-select__separator--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-select__separator--MarginTop)"
  };
  const c_select__separator_MarginBottom = exports.c_select__separator_MarginBottom = {
    "name": "--pf-c-select__separator--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-select__separator--MarginBottom)"
  };
  const c_select__menu_group_not_first_of_type_PaddingTop = exports.c_select__menu_group_not_first_of_type_PaddingTop = {
    "name": "--pf-c-select__menu-group--not--first-of-type--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-select__menu-group--not--first-of-type--PaddingTop)"
  };
  const c_select__menu_group_title_PaddingTop = exports.c_select__menu_group_title_PaddingTop = {
    "name": "--pf-c-select__menu-group-title--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-select__menu-group-title--PaddingTop)"
  };
  const c_select__menu_group_title_PaddingRight = exports.c_select__menu_group_title_PaddingRight = {
    "name": "--pf-c-select__menu-group-title--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-select__menu-group-title--PaddingRight)"
  };
  const c_select__menu_group_title_PaddingBottom = exports.c_select__menu_group_title_PaddingBottom = {
    "name": "--pf-c-select__menu-group-title--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-select__menu-group-title--PaddingBottom)"
  };
  const c_select__menu_group_title_PaddingLeft = exports.c_select__menu_group_title_PaddingLeft = {
    "name": "--pf-c-select__menu-group-title--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-select__menu-group-title--PaddingLeft)"
  };
  const c_select__menu_group_title_FontSize = exports.c_select__menu_group_title_FontSize = {
    "name": "--pf-c-select__menu-group-title--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-select__menu-group-title--FontSize)"
  };
  const c_select__menu_group_title_FontWeight = exports.c_select__menu_group_title_FontWeight = {
    "name": "--pf-c-select__menu-group-title--FontWeight",
    "value": "700",
    "var": "var(--pf-c-select__menu-group-title--FontWeight)"
  };
  const c_select__menu_group_title_Color = exports.c_select__menu_group_title_Color = {
    "name": "--pf-c-select__menu-group-title--Color",
    "value": "#737679",
    "var": "var(--pf-c-select__menu-group-title--Color)"
  };
  const c_simple_list__item_link_PaddingTop = exports.c_simple_list__item_link_PaddingTop = {
    "name": "--pf-c-simple-list__item-link--PaddingTop",
    "value": "0.25rem",
    "var": "var(--pf-c-simple-list__item-link--PaddingTop)"
  };
  const c_simple_list__item_link_PaddingRight = exports.c_simple_list__item_link_PaddingRight = {
    "name": "--pf-c-simple-list__item-link--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-simple-list__item-link--PaddingRight)"
  };
  const c_simple_list__item_link_PaddingBottom = exports.c_simple_list__item_link_PaddingBottom = {
    "name": "--pf-c-simple-list__item-link--PaddingBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-simple-list__item-link--PaddingBottom)"
  };
  const c_simple_list__item_link_PaddingLeft = exports.c_simple_list__item_link_PaddingLeft = {
    "name": "--pf-c-simple-list__item-link--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-simple-list__item-link--PaddingLeft)"
  };
  const c_simple_list__item_link_BackgroundColor = exports.c_simple_list__item_link_BackgroundColor = {
    "name": "--pf-c-simple-list__item-link--BackgroundColor",
    "value": "#f5f5f5",
    "var": "var(--pf-c-simple-list__item-link--BackgroundColor)"
  };
  const c_simple_list__item_link_Color = exports.c_simple_list__item_link_Color = {
    "name": "--pf-c-simple-list__item-link--Color",
    "value": "#004080",
    "var": "var(--pf-c-simple-list__item-link--Color)"
  };
  const c_simple_list__item_link_FontSize = exports.c_simple_list__item_link_FontSize = {
    "name": "--pf-c-simple-list__item-link--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-simple-list__item-link--FontSize)"
  };
  const c_simple_list__item_link_FontWeight = exports.c_simple_list__item_link_FontWeight = {
    "name": "--pf-c-simple-list__item-link--FontWeight",
    "value": "700",
    "var": "var(--pf-c-simple-list__item-link--FontWeight)"
  };
  const c_simple_list__item_link_m_current_Color = exports.c_simple_list__item_link_m_current_Color = {
    "name": "--pf-c-simple-list__item-link--m-current--Color",
    "value": "#004080",
    "var": "var(--pf-c-simple-list__item-link--m-current--Color)"
  };
  const c_simple_list__item_link_m_current_BackgroundColor = exports.c_simple_list__item_link_m_current_BackgroundColor = {
    "name": "--pf-c-simple-list__item-link--m-current--BackgroundColor",
    "value": "#f5f5f5",
    "var": "var(--pf-c-simple-list__item-link--m-current--BackgroundColor)"
  };
  const c_simple_list__item_link_m_current_FontWeight = exports.c_simple_list__item_link_m_current_FontWeight = {
    "name": "--pf-c-simple-list__item-link--m-current--FontWeight",
    "value": "700",
    "var": "var(--pf-c-simple-list__item-link--m-current--FontWeight)"
  };
  const c_simple_list__item_link_hover_Color = exports.c_simple_list__item_link_hover_Color = {
    "name": "--pf-c-simple-list__item-link--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-simple-list__item-link--hover--Color)"
  };
  const c_simple_list__item_link_hover_BackgroundColor = exports.c_simple_list__item_link_hover_BackgroundColor = {
    "name": "--pf-c-simple-list__item-link--hover--BackgroundColor",
    "value": "#f5f5f5",
    "var": "var(--pf-c-simple-list__item-link--hover--BackgroundColor)"
  };
  const c_simple_list__item_link_focus_Color = exports.c_simple_list__item_link_focus_Color = {
    "name": "--pf-c-simple-list__item-link--focus--Color",
    "value": "#004080",
    "var": "var(--pf-c-simple-list__item-link--focus--Color)"
  };
  const c_simple_list__item_link_focus_BackgroundColor = exports.c_simple_list__item_link_focus_BackgroundColor = {
    "name": "--pf-c-simple-list__item-link--focus--BackgroundColor",
    "value": "#f5f5f5",
    "var": "var(--pf-c-simple-list__item-link--focus--BackgroundColor)"
  };
  const c_simple_list__item_link_focus_FontWeight = exports.c_simple_list__item_link_focus_FontWeight = {
    "name": "--pf-c-simple-list__item-link--focus--FontWeight",
    "value": "700",
    "var": "var(--pf-c-simple-list__item-link--focus--FontWeight)"
  };
  const c_simple_list__item_link_active_Color = exports.c_simple_list__item_link_active_Color = {
    "name": "--pf-c-simple-list__item-link--active--Color",
    "value": "#004080",
    "var": "var(--pf-c-simple-list__item-link--active--Color)"
  };
  const c_simple_list__item_link_active_BackgroundColor = exports.c_simple_list__item_link_active_BackgroundColor = {
    "name": "--pf-c-simple-list__item-link--active--BackgroundColor",
    "value": "#f5f5f5",
    "var": "var(--pf-c-simple-list__item-link--active--BackgroundColor)"
  };
  const c_simple_list__item_link_active_FontWeight = exports.c_simple_list__item_link_active_FontWeight = {
    "name": "--pf-c-simple-list__item-link--active--FontWeight",
    "value": "700",
    "var": "var(--pf-c-simple-list__item-link--active--FontWeight)"
  };
  const c_simple_list__title_PaddingTop = exports.c_simple_list__title_PaddingTop = {
    "name": "--pf-c-simple-list__title--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-simple-list__title--PaddingTop)"
  };
  const c_simple_list__title_PaddingRight = exports.c_simple_list__title_PaddingRight = {
    "name": "--pf-c-simple-list__title--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-simple-list__title--PaddingRight)"
  };
  const c_simple_list__title_PaddingBottom = exports.c_simple_list__title_PaddingBottom = {
    "name": "--pf-c-simple-list__title--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-simple-list__title--PaddingBottom)"
  };
  const c_simple_list__title_PaddingLeft = exports.c_simple_list__title_PaddingLeft = {
    "name": "--pf-c-simple-list__title--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-simple-list__title--PaddingLeft)"
  };
  const c_simple_list__title_FontSize = exports.c_simple_list__title_FontSize = {
    "name": "--pf-c-simple-list__title--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-simple-list__title--FontSize)"
  };
  const c_simple_list__title_Color = exports.c_simple_list__title_Color = {
    "name": "--pf-c-simple-list__title--Color",
    "value": "#737679",
    "var": "var(--pf-c-simple-list__title--Color)"
  };
  const c_simple_list__title_FontWeight = exports.c_simple_list__title_FontWeight = {
    "name": "--pf-c-simple-list__title--FontWeight",
    "value": "700",
    "var": "var(--pf-c-simple-list__title--FontWeight)"
  };
  const c_simple_list__section_section_MarginTop = exports.c_simple_list__section_section_MarginTop = {
    "name": "--pf-c-simple-list__section--section--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-simple-list__section--section--MarginTop)"
  };
  const c_skip_to_content_Top = exports.c_skip_to_content_Top = {
    "name": "--pf-c-skip-to-content--Top",
    "value": "1rem",
    "var": "var(--pf-c-skip-to-content--Top)"
  };
  const c_skip_to_content_ZIndex = exports.c_skip_to_content_ZIndex = {
    "name": "--pf-c-skip-to-content--ZIndex",
    "value": "600",
    "var": "var(--pf-c-skip-to-content--ZIndex)"
  };
  const c_skip_to_content_focus_Left = exports.c_skip_to_content_focus_Left = {
    "name": "--pf-c-skip-to-content--focus--Left",
    "value": "1rem",
    "var": "var(--pf-c-skip-to-content--focus--Left)"
  };
  const c_spinner_AnimationDuration = exports.c_spinner_AnimationDuration = {
    "name": "--pf-c-spinner--AnimationDuration",
    "value": "1.5s",
    "var": "var(--pf-c-spinner--AnimationDuration)"
  };
  const c_spinner_AnimationTimingFunction = exports.c_spinner_AnimationTimingFunction = {
    "name": "--pf-c-spinner--AnimationTimingFunction",
    "value": "cubic-bezier(.77,.005,.315,1)",
    "var": "var(--pf-c-spinner--AnimationTimingFunction)"
  };
  const c_spinner_diameter = exports.c_spinner_diameter = {
    "name": "--pf-c-spinner--diameter",
    "value": "3.375rem",
    "var": "var(--pf-c-spinner--diameter)"
  };
  const c_spinner_stroke_width_multiplier = exports.c_spinner_stroke_width_multiplier = {
    "name": "--pf-c-spinner--stroke-width-multiplier",
    "value": ".1",
    "var": "var(--pf-c-spinner--stroke-width-multiplier)"
  };
  const c_spinner_stroke_width = exports.c_spinner_stroke_width = {
    "name": "--pf-c-spinner--stroke-width",
    "value": "calc(3.375rem*.1)",
    "var": "var(--pf-c-spinner--stroke-width)"
  };
  const c_spinner_Width = exports.c_spinner_Width = {
    "name": "--pf-c-spinner--Width",
    "value": "3.375rem",
    "var": "var(--pf-c-spinner--Width)"
  };
  const c_spinner_Height = exports.c_spinner_Height = {
    "name": "--pf-c-spinner--Height",
    "value": "3.375rem",
    "var": "var(--pf-c-spinner--Height)"
  };
  const c_spinner_Color = exports.c_spinner_Color = {
    "name": "--pf-c-spinner--Color",
    "value": "#06c",
    "var": "var(--pf-c-spinner--Color)"
  };
  const c_spinner_m_sm_diameter = exports.c_spinner_m_sm_diameter = {
    "name": "--pf-c-spinner--m-sm--diameter",
    "value": "0.625rem",
    "var": "var(--pf-c-spinner--m-sm--diameter)"
  };
  const c_spinner_m_md_diameter = exports.c_spinner_m_md_diameter = {
    "name": "--pf-c-spinner--m-md--diameter",
    "value": "1.125rem",
    "var": "var(--pf-c-spinner--m-md--diameter)"
  };
  const c_spinner_m_lg_diameter = exports.c_spinner_m_lg_diameter = {
    "name": "--pf-c-spinner--m-lg--diameter",
    "value": "1.5rem",
    "var": "var(--pf-c-spinner--m-lg--diameter)"
  };
  const c_spinner_m_xl_diameter = exports.c_spinner_m_xl_diameter = {
    "name": "--pf-c-spinner--m-xl--diameter",
    "value": "3.375rem",
    "var": "var(--pf-c-spinner--m-xl--diameter)"
  };
  const c_spinner__clipper_Width = exports.c_spinner__clipper_Width = {
    "name": "--pf-c-spinner__clipper--Width",
    "value": "3.375rem",
    "var": "var(--pf-c-spinner__clipper--Width)"
  };
  const c_spinner__clipper_Height = exports.c_spinner__clipper_Height = {
    "name": "--pf-c-spinner__clipper--Height",
    "value": "3.375rem",
    "var": "var(--pf-c-spinner__clipper--Height)"
  };
  const c_spinner__clipper_after_BoxShadowColor = exports.c_spinner__clipper_after_BoxShadowColor = {
    "name": "--pf-c-spinner__clipper--after--BoxShadowColor",
    "value": "#06c",
    "var": "var(--pf-c-spinner__clipper--after--BoxShadowColor)"
  };
  const c_spinner__clipper_after_Width = exports.c_spinner__clipper_after_Width = {
    "name": "--pf-c-spinner__clipper--after--Width",
    "value": "3.375rem",
    "var": "var(--pf-c-spinner__clipper--after--Width)"
  };
  const c_spinner__clipper_after_Height = exports.c_spinner__clipper_after_Height = {
    "name": "--pf-c-spinner__clipper--after--Height",
    "value": "3.375rem",
    "var": "var(--pf-c-spinner__clipper--after--Height)"
  };
  const c_spinner__clipper_after_BoxShadowSpreadRadius = exports.c_spinner__clipper_after_BoxShadowSpreadRadius = {
    "name": "--pf-c-spinner__clipper--after--BoxShadowSpreadRadius",
    "value": "calc(3.375rem*.1)",
    "var": "var(--pf-c-spinner__clipper--after--BoxShadowSpreadRadius)"
  };
  const c_spinner__lead_ball_after_BackgroundColor = exports.c_spinner__lead_ball_after_BackgroundColor = {
    "name": "--pf-c-spinner__lead-ball--after--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-spinner__lead-ball--after--BackgroundColor)"
  };
  const c_spinner__ball_after_Width = exports.c_spinner__ball_after_Width = {
    "name": "--pf-c-spinner__ball--after--Width",
    "value": "calc(3.375rem*.1)",
    "var": "var(--pf-c-spinner__ball--after--Width)"
  };
  const c_spinner__ball_after_Height = exports.c_spinner__ball_after_Height = {
    "name": "--pf-c-spinner__ball--after--Height",
    "value": "calc(3.375rem*.1)",
    "var": "var(--pf-c-spinner__ball--after--Height)"
  };
  const c_spinner__tail_ball_after_BackgroundColor = exports.c_spinner__tail_ball_after_BackgroundColor = {
    "name": "--pf-c-spinner__tail-ball--after--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-spinner__tail-ball--after--BackgroundColor)"
  };
  const c_switch_FontSize = exports.c_switch_FontSize = {
    "name": "--pf-c-switch--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-switch--FontSize)"
  };
  const c_switch__toggle_icon_FontSize = exports.c_switch__toggle_icon_FontSize = {
    "name": "--pf-c-switch__toggle-icon--FontSize",
    "value": "calc(1rem*0.625)",
    "var": "var(--pf-c-switch__toggle-icon--FontSize)"
  };
  const c_switch__toggle_icon_Color = exports.c_switch__toggle_icon_Color = {
    "name": "--pf-c-switch__toggle-icon--Color",
    "value": "#fff",
    "var": "var(--pf-c-switch__toggle-icon--Color)"
  };
  const c_switch__toggle_icon_PaddingLeft = exports.c_switch__toggle_icon_PaddingLeft = {
    "name": "--pf-c-switch__toggle-icon--PaddingLeft",
    "value": "calc(1rem*0.4)",
    "var": "var(--pf-c-switch__toggle-icon--PaddingLeft)"
  };
  const c_switch__toggle_icon_Top = exports.c_switch__toggle_icon_Top = {
    "name": "--pf-c-switch__toggle-icon--Top",
    "value": "50%",
    "var": "var(--pf-c-switch__toggle-icon--Top)"
  };
  const c_switch__toggle_icon_Left = exports.c_switch__toggle_icon_Left = {
    "name": "--pf-c-switch__toggle-icon--Left",
    "value": "0",
    "var": "var(--pf-c-switch__toggle-icon--Left)"
  };
  const c_switch__toggle_icon_Transform = exports.c_switch__toggle_icon_Transform = {
    "name": "--pf-c-switch__toggle-icon--Transform",
    "value": "translateY(-50%)",
    "var": "var(--pf-c-switch__toggle-icon--Transform)"
  };
  const c_switch__toggle_icon_Offset = exports.c_switch__toggle_icon_Offset = {
    "name": "--pf-c-switch__toggle-icon--Offset",
    "value": "0.125rem",
    "var": "var(--pf-c-switch__toggle-icon--Offset)"
  };
  const c_switch_LineHeight = exports.c_switch_LineHeight = {
    "name": "--pf-c-switch--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-switch--LineHeight)"
  };
  const c_switch_Height = exports.c_switch_Height = {
    "name": "--pf-c-switch--Height",
    "value": "calc(1rem*1.5)",
    "var": "var(--pf-c-switch--Height)"
  };
  const c_switch__input_checked__toggle_BackgroundColor = exports.c_switch__input_checked__toggle_BackgroundColor = {
    "name": "--pf-c-switch__input--checked__toggle--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-switch__input--checked__toggle--BackgroundColor)"
  };
  const c_switch__input_checked__toggle_before_Transform = exports.c_switch__input_checked__toggle_before_Transform = {
    "name": "--pf-c-switch__input--checked__toggle--before--Transform",
    "value": "translateX(calc(100% + 0.125rem))",
    "var": "var(--pf-c-switch__input--checked__toggle--before--Transform)"
  };
  const c_switch__input_checked__label_Color = exports.c_switch__input_checked__label_Color = {
    "name": "--pf-c-switch__input--checked__label--Color",
    "value": "#151515",
    "var": "var(--pf-c-switch__input--checked__label--Color)"
  };
  const c_switch__input_not_checked__label_Color = exports.c_switch__input_not_checked__label_Color = {
    "name": "--pf-c-switch__input--not-checked__label--Color",
    "value": "#737679",
    "var": "var(--pf-c-switch__input--not-checked__label--Color)"
  };
  const c_switch__input_disabled__label_Color = exports.c_switch__input_disabled__label_Color = {
    "name": "--pf-c-switch__input--disabled__label--Color",
    "value": "#737679",
    "var": "var(--pf-c-switch__input--disabled__label--Color)"
  };
  const c_switch__input_disabled__toggle_BackgroundColor = exports.c_switch__input_disabled__toggle_BackgroundColor = {
    "name": "--pf-c-switch__input--disabled__toggle--BackgroundColor",
    "value": "#737679",
    "var": "var(--pf-c-switch__input--disabled__toggle--BackgroundColor)"
  };
  const c_switch__input_disabled__toggle_before_BackgroundColor = exports.c_switch__input_disabled__toggle_before_BackgroundColor = {
    "name": "--pf-c-switch__input--disabled__toggle--before--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-switch__input--disabled__toggle--before--BackgroundColor)"
  };
  const c_switch__input_focus__toggle_OutlineWidth = exports.c_switch__input_focus__toggle_OutlineWidth = {
    "name": "--pf-c-switch__input--focus__toggle--OutlineWidth",
    "value": "2px",
    "var": "var(--pf-c-switch__input--focus__toggle--OutlineWidth)"
  };
  const c_switch__input_focus__toggle_OutlineOffset = exports.c_switch__input_focus__toggle_OutlineOffset = {
    "name": "--pf-c-switch__input--focus__toggle--OutlineOffset",
    "value": "0.5rem",
    "var": "var(--pf-c-switch__input--focus__toggle--OutlineOffset)"
  };
  const c_switch__input_focus__toggle_OutlineColor = exports.c_switch__input_focus__toggle_OutlineColor = {
    "name": "--pf-c-switch__input--focus__toggle--OutlineColor",
    "value": "#06c",
    "var": "var(--pf-c-switch__input--focus__toggle--OutlineColor)"
  };
  const c_switch__toggle_BackgroundColor = exports.c_switch__toggle_BackgroundColor = {
    "name": "--pf-c-switch__toggle--BackgroundColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-switch__toggle--BackgroundColor)"
  };
  const c_switch__toggle_BorderRadius = exports.c_switch__toggle_BorderRadius = {
    "name": "--pf-c-switch__toggle--BorderRadius",
    "value": "calc(1rem*1.5)",
    "var": "var(--pf-c-switch__toggle--BorderRadius)"
  };
  const c_switch__toggle_before_Width = exports.c_switch__toggle_before_Width = {
    "name": "--pf-c-switch__toggle--before--Width",
    "value": "calc(1rem - 0.125rem)",
    "var": "var(--pf-c-switch__toggle--before--Width)"
  };
  const c_switch__toggle_before_Height = exports.c_switch__toggle_before_Height = {
    "name": "--pf-c-switch__toggle--before--Height",
    "value": "calc(1rem - 0.125rem)",
    "var": "var(--pf-c-switch__toggle--before--Height)"
  };
  const c_switch__toggle_before_Top = exports.c_switch__toggle_before_Top = {
    "name": "--pf-c-switch__toggle--before--Top",
    "value": "calc((calc(1rem*1.5) - calc(1rem - 0.125rem))/2)",
    "var": "var(--pf-c-switch__toggle--before--Top)"
  };
  const c_switch__toggle_before_Left = exports.c_switch__toggle_before_Left = {
    "name": "--pf-c-switch__toggle--before--Left",
    "value": "calc((calc(1rem*1.5) - calc(1rem - 0.125rem))/2)",
    "var": "var(--pf-c-switch__toggle--before--Left)"
  };
  const c_switch__toggle_before_BackgroundColor = exports.c_switch__toggle_before_BackgroundColor = {
    "name": "--pf-c-switch__toggle--before--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-switch__toggle--before--BackgroundColor)"
  };
  const c_switch__toggle_before_BorderRadius = exports.c_switch__toggle_before_BorderRadius = {
    "name": "--pf-c-switch__toggle--before--BorderRadius",
    "value": "30em",
    "var": "var(--pf-c-switch__toggle--before--BorderRadius)"
  };
  const c_switch__toggle_before_BoxShadow = exports.c_switch__toggle_before_BoxShadow = {
    "name": "--pf-c-switch__toggle--before--BoxShadow",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-c-switch__toggle--before--BoxShadow)"
  };
  const c_switch__toggle_before_Transition = exports.c_switch__toggle_before_Transition = {
    "name": "--pf-c-switch__toggle--before--Transition",
    "value": "transform .25s ease 0s",
    "var": "var(--pf-c-switch__toggle--before--Transition)"
  };
  const c_switch_Width = exports.c_switch_Width = {
    "name": "--pf-c-switch--Width",
    "value": "calc(calc(1rem*1.5) + 0.125rem + calc(1rem - 0.125rem))",
    "var": "var(--pf-c-switch--Width)"
  };
  const c_switch__label_PaddingLeft = exports.c_switch__label_PaddingLeft = {
    "name": "--pf-c-switch__label--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-switch__label--PaddingLeft)"
  };
  const c_switch__label_FontSize = exports.c_switch__label_FontSize = {
    "name": "--pf-c-switch__label--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-switch__label--FontSize)"
  };
  const c_switch__label_FontWeight = exports.c_switch__label_FontWeight = {
    "name": "--pf-c-switch__label--FontWeight",
    "value": "400",
    "var": "var(--pf-c-switch__label--FontWeight)"
  };
  const c_switch__label_LineHeight = exports.c_switch__label_LineHeight = {
    "name": "--pf-c-switch__label--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-switch__label--LineHeight)"
  };
  const c_switch__label_Color = exports.c_switch__label_Color = {
    "name": "--pf-c-switch__label--Color",
    "value": "#151515",
    "var": "var(--pf-c-switch__label--Color)"
  };
  const c_table_responsive_BorderColor = exports.c_table_responsive_BorderColor = {
    "name": "--pf-c-table--responsive--BorderColor",
    "value": "#ededed",
    "var": "var(--pf-c-table--responsive--BorderColor)"
  };
  const c_table_tbody_responsive_MarginTop = exports.c_table_tbody_responsive_MarginTop = {
    "name": "--pf-c-table-tbody--responsive--MarginTop",
    "value": "0.25rem",
    "var": "var(--pf-c-table-tbody--responsive--MarginTop)"
  };
  const c_table_tbody_m_expanded_before_Top = exports.c_table_tbody_m_expanded_before_Top = {
    "name": "--pf-c-table-tbody--m-expanded--before--Top",
    "value": "0.5rem",
    "var": "var(--pf-c-table-tbody--m-expanded--before--Top)"
  };
  const c_table_tbody_responsive_BorderWidth = exports.c_table_tbody_responsive_BorderWidth = {
    "name": "--pf-c-table-tbody--responsive--BorderWidth",
    "value": "0.5rem",
    "var": "var(--pf-c-table-tbody--responsive--BorderWidth)"
  };
  const c_table_tr_responsive_BorderWidth = exports.c_table_tr_responsive_BorderWidth = {
    "name": "--pf-c-table-tr--responsive--BorderWidth",
    "value": "0.5rem",
    "var": "var(--pf-c-table-tr--responsive--BorderWidth)"
  };
  const c_table_tr_responsive_last_child_BorderWidth = exports.c_table_tr_responsive_last_child_BorderWidth = {
    "name": "--pf-c-table-tr--responsive--last-child--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-table-tr--responsive--last-child--BorderWidth)"
  };
  const c_table_tr_responsive_GridColumnGap = exports.c_table_tr_responsive_GridColumnGap = {
    "name": "--pf-c-table-tr--responsive--GridColumnGap",
    "value": "1rem",
    "var": "var(--pf-c-table-tr--responsive--GridColumnGap)"
  };
  const c_table_tr_responsive_MarginTop = exports.c_table_tr_responsive_MarginTop = {
    "name": "--pf-c-table-tr--responsive--MarginTop",
    "value": "0.5rem",
    "var": "var(--pf-c-table-tr--responsive--MarginTop)"
  };
  const c_table_tr_responsive_PaddingTop = exports.c_table_tr_responsive_PaddingTop = {
    "name": "--pf-c-table-tr--responsive--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-table-tr--responsive--PaddingTop)"
  };
  const c_table_tr_responsive_PaddingRight = exports.c_table_tr_responsive_PaddingRight = {
    "name": "--pf-c-table-tr--responsive--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-table-tr--responsive--PaddingRight)"
  };
  const c_table_tr_responsive_md_PaddingRight = exports.c_table_tr_responsive_md_PaddingRight = {
    "name": "--pf-c-table-tr--responsive--md--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-table-tr--responsive--md--PaddingRight)"
  };
  const c_table_tr_responsive_PaddingBottom = exports.c_table_tr_responsive_PaddingBottom = {
    "name": "--pf-c-table-tr--responsive--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-table-tr--responsive--PaddingBottom)"
  };
  const c_table_tr_responsive_PaddingLeft = exports.c_table_tr_responsive_PaddingLeft = {
    "name": "--pf-c-table-tr--responsive--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-table-tr--responsive--PaddingLeft)"
  };
  const c_table_tr_responsive_md_PaddingLeft = exports.c_table_tr_responsive_md_PaddingLeft = {
    "name": "--pf-c-table-tr--responsive--md--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-table-tr--responsive--md--PaddingLeft)"
  };
  const c_table_tr_responsive_nested_table_PaddingTop = exports.c_table_tr_responsive_nested_table_PaddingTop = {
    "name": "--pf-c-table-tr--responsive--nested-table--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-table-tr--responsive--nested-table--PaddingTop)"
  };
  const c_table_tr_responsive_nested_table_PaddingRight = exports.c_table_tr_responsive_nested_table_PaddingRight = {
    "name": "--pf-c-table-tr--responsive--nested-table--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-table-tr--responsive--nested-table--PaddingRight)"
  };
  const c_table_tr_responsive_nested_table_PaddingBottom = exports.c_table_tr_responsive_nested_table_PaddingBottom = {
    "name": "--pf-c-table-tr--responsive--nested-table--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-table-tr--responsive--nested-table--PaddingBottom)"
  };
  const c_table_tr_responsive_nested_table_PaddingLeft = exports.c_table_tr_responsive_nested_table_PaddingLeft = {
    "name": "--pf-c-table-tr--responsive--nested-table--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-table-tr--responsive--nested-table--PaddingLeft)"
  };
  const c_table_cell_m_grid_hidden_visible_Display = exports.c_table_cell_m_grid_hidden_visible_Display = {
    "name": "--pf-c-table-cell--m-grid--hidden-visible--Display",
    "value": "grid",
    "var": "var(--pf-c-table-cell--m-grid--hidden-visible--Display)"
  };
  const c_table_td_responsive_GridColumnGap = exports.c_table_td_responsive_GridColumnGap = {
    "name": "--pf-c-table-td--responsive--GridColumnGap",
    "value": "1rem",
    "var": "var(--pf-c-table-td--responsive--GridColumnGap)"
  };
  const c_table_cell_responsive_PaddingTop = exports.c_table_cell_responsive_PaddingTop = {
    "name": "--pf-c-table-cell--responsive--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-table-cell--responsive--PaddingTop)"
  };
  const c_table_cell_responsive_PaddingBottom = exports.c_table_cell_responsive_PaddingBottom = {
    "name": "--pf-c-table-cell--responsive--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-table-cell--responsive--PaddingBottom)"
  };
  const c_table_cell_th_responsive_PaddingTop = exports.c_table_cell_th_responsive_PaddingTop = {
    "name": "--pf-c-table-cell-th--responsive--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-table-cell-th--responsive--PaddingTop)"
  };
  const c_table_cell_responsive_PaddingRight = exports.c_table_cell_responsive_PaddingRight = {
    "name": "--pf-c-table-cell--responsive--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-table-cell--responsive--PaddingRight)"
  };
  const c_table_cell_responsive_PaddingLeft = exports.c_table_cell_responsive_PaddingLeft = {
    "name": "--pf-c-table-cell--responsive--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-table-cell--responsive--PaddingLeft)"
  };
  const c_table_m_compact_tr_td_responsive_PaddingTop = exports.c_table_m_compact_tr_td_responsive_PaddingTop = {
    "name": "--pf-c-table--m-compact-tr-td--responsive--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-table--m-compact-tr-td--responsive--PaddingTop)"
  };
  const c_table_m_compact_tr_td_responsive_PaddingBottom = exports.c_table_m_compact_tr_td_responsive_PaddingBottom = {
    "name": "--pf-c-table--m-compact-tr-td--responsive--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-table--m-compact-tr-td--responsive--PaddingBottom)"
  };
  const c_table__expandable_row_content_responsive_PaddingRight = exports.c_table__expandable_row_content_responsive_PaddingRight = {
    "name": "--pf-c-table__expandable-row-content--responsive--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-table__expandable-row-content--responsive--PaddingRight)"
  };
  const c_table__expandable_row_content_responsive_PaddingLeft = exports.c_table__expandable_row_content_responsive_PaddingLeft = {
    "name": "--pf-c-table__expandable-row-content--responsive--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-table__expandable-row-content--responsive--PaddingLeft)"
  };
  const c_table__expandable_row_content_BackgroundColor = exports.c_table__expandable_row_content_BackgroundColor = {
    "name": "--pf-c-table__expandable-row-content--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-table__expandable-row-content--BackgroundColor)"
  };
  const c_table__check_responsive_MarginLeft = exports.c_table__check_responsive_MarginLeft = {
    "name": "--pf-c-table__check--responsive--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-table__check--responsive--MarginLeft)"
  };
  const c_table__check_responsive_MarginTop = exports.c_table__check_responsive_MarginTop = {
    "name": "--pf-c-table__check--responsive--MarginTop",
    "value": "0.375rem",
    "var": "var(--pf-c-table__check--responsive--MarginTop)"
  };
  const c_table__action_responsive_MarginLeft = exports.c_table__action_responsive_MarginLeft = {
    "name": "--pf-c-table__action--responsive--MarginLeft",
    "value": "2rem",
    "var": "var(--pf-c-table__action--responsive--MarginLeft)"
  };
  const c_table__toggle__icon_Transition = exports.c_table__toggle__icon_Transition = {
    "name": "--pf-c-table__toggle__icon--Transition",
    "value": ".2s ease-in 0s",
    "var": "var(--pf-c-table__toggle__icon--Transition)"
  };
  const c_table__toggle_m_expanded__icon_Transform = exports.c_table__toggle_m_expanded__icon_Transform = {
    "name": "--pf-c-table__toggle--m-expanded__icon--Transform",
    "value": "rotate(180deg)",
    "var": "var(--pf-c-table__toggle--m-expanded__icon--Transform)"
  };
  const c_table_cell_hidden_visible_Display = exports.c_table_cell_hidden_visible_Display = {
    "name": "--pf-c-table-cell--hidden-visible--Display",
    "value": "table-cell",
    "var": "var(--pf-c-table-cell--hidden-visible--Display)"
  };
  const c_table_cell_PaddingLeft = exports.c_table_cell_PaddingLeft = {
    "name": "--pf-c-table-cell--PaddingLeft",
    "value": "4rem",
    "var": "var(--pf-c-table-cell--PaddingLeft)"
  };
  const c_table_cell_PaddingRight = exports.c_table_cell_PaddingRight = {
    "name": "--pf-c-table-cell--PaddingRight",
    "value": "4rem",
    "var": "var(--pf-c-table-cell--PaddingRight)"
  };
  const c_table_BackgroundColor = exports.c_table_BackgroundColor = {
    "name": "--pf-c-table--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-table--BackgroundColor)"
  };
  const c_table_BorderColor = exports.c_table_BorderColor = {
    "name": "--pf-c-table--BorderColor",
    "value": "transparent",
    "var": "var(--pf-c-table--BorderColor)"
  };
  const c_table_BorderWidth = exports.c_table_BorderWidth = {
    "name": "--pf-c-table--BorderWidth",
    "value": "0",
    "var": "var(--pf-c-table--BorderWidth)"
  };
  const c_table_FontWeight = exports.c_table_FontWeight = {
    "name": "--pf-c-table--FontWeight",
    "value": "700",
    "var": "var(--pf-c-table--FontWeight)"
  };
  const c_table_caption_FontSize = exports.c_table_caption_FontSize = {
    "name": "--pf-c-table-caption--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-table-caption--FontSize)"
  };
  const c_table_caption_Color = exports.c_table_caption_Color = {
    "name": "--pf-c-table-caption--Color",
    "value": "#737679",
    "var": "var(--pf-c-table-caption--Color)"
  };
  const c_table_caption_PaddingTop = exports.c_table_caption_PaddingTop = {
    "name": "--pf-c-table-caption--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-table-caption--PaddingTop)"
  };
  const c_table_caption_PaddingRight = exports.c_table_caption_PaddingRight = {
    "name": "--pf-c-table-caption--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-table-caption--PaddingRight)"
  };
  const c_table_caption_md_PaddingRight = exports.c_table_caption_md_PaddingRight = {
    "name": "--pf-c-table-caption--md--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-table-caption--md--PaddingRight)"
  };
  const c_table_caption_PaddingBottom = exports.c_table_caption_PaddingBottom = {
    "name": "--pf-c-table-caption--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-table-caption--PaddingBottom)"
  };
  const c_table_caption_PaddingLeft = exports.c_table_caption_PaddingLeft = {
    "name": "--pf-c-table-caption--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-table-caption--PaddingLeft)"
  };
  const c_table_caption_md_PaddingLeft = exports.c_table_caption_md_PaddingLeft = {
    "name": "--pf-c-table-caption--md--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-table-caption--md--PaddingLeft)"
  };
  const c_table_thead_FontSize = exports.c_table_thead_FontSize = {
    "name": "--pf-c-table-thead--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-table-thead--FontSize)"
  };
  const c_table_thead_FontWeight = exports.c_table_thead_FontWeight = {
    "name": "--pf-c-table-thead--FontWeight",
    "value": "700",
    "var": "var(--pf-c-table-thead--FontWeight)"
  };
  const c_table_thead_cell_PaddingTop = exports.c_table_thead_cell_PaddingTop = {
    "name": "--pf-c-table-thead-cell--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-table-thead-cell--PaddingTop)"
  };
  const c_table_thead_cell_PaddingBottom = exports.c_table_thead_cell_PaddingBottom = {
    "name": "--pf-c-table-thead-cell--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-table-thead-cell--PaddingBottom)"
  };
  const c_table_tbody_cell_PaddingTop = exports.c_table_tbody_cell_PaddingTop = {
    "name": "--pf-c-table-tbody-cell--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-table-tbody-cell--PaddingTop)"
  };
  const c_table_tbody_cell_PaddingBottom = exports.c_table_tbody_cell_PaddingBottom = {
    "name": "--pf-c-table-tbody-cell--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-table-tbody-cell--PaddingBottom)"
  };
  const c_table_cell_PaddingTop = exports.c_table_cell_PaddingTop = {
    "name": "--pf-c-table-cell--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-table-cell--PaddingTop)"
  };
  const c_table_cell_PaddingBottom = exports.c_table_cell_PaddingBottom = {
    "name": "--pf-c-table-cell--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-table-cell--PaddingBottom)"
  };
  const c_table_cell_FontSize = exports.c_table_cell_FontSize = {
    "name": "--pf-c-table-cell--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-table-cell--FontSize)"
  };
  const c_table_cell_first_last_child_PaddingLeft = exports.c_table_cell_first_last_child_PaddingLeft = {
    "name": "--pf-c-table-cell--first-last-child--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-table-cell--first-last-child--PaddingLeft)"
  };
  const c_table_cell_first_last_child_PaddingRight = exports.c_table_cell_first_last_child_PaddingRight = {
    "name": "--pf-c-table-cell--first-last-child--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-table-cell--first-last-child--PaddingRight)"
  };
  const c_table__toggle_c_button_MarginTop = exports.c_table__toggle_c_button_MarginTop = {
    "name": "--pf-c-table__toggle--c-button--MarginTop",
    "value": "-0.375rem",
    "var": "var(--pf-c-table__toggle--c-button--MarginTop)"
  };
  const c_table__toggle_c_button__toggle_icon_Transform = exports.c_table__toggle_c_button__toggle_icon_Transform = {
    "name": "--pf-c-table__toggle--c-button__toggle-icon--Transform",
    "value": "rotate(270deg)",
    "var": "var(--pf-c-table__toggle--c-button__toggle-icon--Transform)"
  };
  const c_table__toggle_c_button__toggle_icon_Transition = exports.c_table__toggle_c_button__toggle_icon_Transition = {
    "name": "--pf-c-table__toggle--c-button__toggle-icon--Transition",
    "value": ".2s ease-in 0s",
    "var": "var(--pf-c-table__toggle--c-button__toggle-icon--Transition)"
  };
  const c_table__toggle_c_button_m_expanded__toggle_icon_Transform = exports.c_table__toggle_c_button_m_expanded__toggle_icon_Transform = {
    "name": "--pf-c-table__toggle--c-button--m-expanded__toggle-icon--Transform",
    "value": "rotate(360deg)",
    "var": "var(--pf-c-table__toggle--c-button--m-expanded__toggle-icon--Transform)"
  };
  const c_table_m_compact__toggle_PaddingTop = exports.c_table_m_compact__toggle_PaddingTop = {
    "name": "--pf-c-table--m-compact__toggle--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-table--m-compact__toggle--PaddingTop)"
  };
  const c_table_m_compact__toggle_PaddingBottom = exports.c_table_m_compact__toggle_PaddingBottom = {
    "name": "--pf-c-table--m-compact__toggle--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-table--m-compact__toggle--PaddingBottom)"
  };
  const c_table__check_input_MarginTop = exports.c_table__check_input_MarginTop = {
    "name": "--pf-c-table__check--input--MarginTop",
    "value": "0.1875rem",
    "var": "var(--pf-c-table__check--input--MarginTop)"
  };
  const c_table__check_input_FontSize = exports.c_table__check_input_FontSize = {
    "name": "--pf-c-table__check--input--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-table__check--input--FontSize)"
  };
  const c_table__action_PaddingTop = exports.c_table__action_PaddingTop = {
    "name": "--pf-c-table__action--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-table__action--PaddingTop)"
  };
  const c_table__action_PaddingRight = exports.c_table__action_PaddingRight = {
    "name": "--pf-c-table__action--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-table__action--PaddingRight)"
  };
  const c_table__action_PaddingBottom = exports.c_table__action_PaddingBottom = {
    "name": "--pf-c-table__action--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-table__action--PaddingBottom)"
  };
  const c_table__action_PaddingLeft = exports.c_table__action_PaddingLeft = {
    "name": "--pf-c-table__action--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-table__action--PaddingLeft)"
  };
  const c_table__inline_edit_action_PaddingTop = exports.c_table__inline_edit_action_PaddingTop = {
    "name": "--pf-c-table__inline-edit-action--PaddingTop",
    "value": "0",
    "var": "var(--pf-c-table__inline-edit-action--PaddingTop)"
  };
  const c_table__inline_edit_action_PaddingRight = exports.c_table__inline_edit_action_PaddingRight = {
    "name": "--pf-c-table__inline-edit-action--PaddingRight",
    "value": "0",
    "var": "var(--pf-c-table__inline-edit-action--PaddingRight)"
  };
  const c_table__inline_edit_action_PaddingBottom = exports.c_table__inline_edit_action_PaddingBottom = {
    "name": "--pf-c-table__inline-edit-action--PaddingBottom",
    "value": "0",
    "var": "var(--pf-c-table__inline-edit-action--PaddingBottom)"
  };
  const c_table__inline_edit_action_PaddingLeft = exports.c_table__inline_edit_action_PaddingLeft = {
    "name": "--pf-c-table__inline-edit-action--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-table__inline-edit-action--PaddingLeft)"
  };
  const c_table__expandable_row_Transition = exports.c_table__expandable_row_Transition = {
    "name": "--pf-c-table__expandable-row--Transition",
    "value": "all 250ms ease-in-out",
    "var": "var(--pf-c-table__expandable-row--Transition)"
  };
  const c_table__expandable_row_before_Width = exports.c_table__expandable_row_before_Width = {
    "name": "--pf-c-table__expandable-row--before--Width",
    "value": "3px",
    "var": "var(--pf-c-table__expandable-row--before--Width)"
  };
  const c_table__expandable_row_before_BackgroundColor = exports.c_table__expandable_row_before_BackgroundColor = {
    "name": "--pf-c-table__expandable-row--before--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-table__expandable-row--before--BackgroundColor)"
  };
  const c_table__expandable_row_before_ZIndex = exports.c_table__expandable_row_before_ZIndex = {
    "name": "--pf-c-table__expandable-row--before--ZIndex",
    "value": "200",
    "var": "var(--pf-c-table__expandable-row--before--ZIndex)"
  };
  const c_table__expandable_row_before_Top = exports.c_table__expandable_row_before_Top = {
    "name": "--pf-c-table__expandable-row--before--Top",
    "value": "calc(1px*-1)",
    "var": "var(--pf-c-table__expandable-row--before--Top)"
  };
  const c_table__expandable_row_before_Bottom = exports.c_table__expandable_row_before_Bottom = {
    "name": "--pf-c-table__expandable-row--before--Bottom",
    "value": "calc(1px*-1)",
    "var": "var(--pf-c-table__expandable-row--before--Bottom)"
  };
  const c_table__expandable_row_MaxHeight = exports.c_table__expandable_row_MaxHeight = {
    "name": "--pf-c-table__expandable-row--MaxHeight",
    "value": "28.125rem",
    "var": "var(--pf-c-table__expandable-row--MaxHeight)"
  };
  const c_table__expandable_row_content_Transition = exports.c_table__expandable_row_content_Transition = {
    "name": "--pf-c-table__expandable-row-content--Transition",
    "value": "all 250ms ease-in-out",
    "var": "var(--pf-c-table__expandable-row-content--Transition)"
  };
  const c_table__expandable_row_content_PaddingTop = exports.c_table__expandable_row_content_PaddingTop = {
    "name": "--pf-c-table__expandable-row-content--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-table__expandable-row-content--PaddingTop)"
  };
  const c_table__expandable_row_content_PaddingBottom = exports.c_table__expandable_row_content_PaddingBottom = {
    "name": "--pf-c-table__expandable-row-content--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-table__expandable-row-content--PaddingBottom)"
  };
  const c_table__sort_indicator_MarginLeft = exports.c_table__sort_indicator_MarginLeft = {
    "name": "--pf-c-table__sort-indicator--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-table__sort-indicator--MarginLeft)"
  };
  const c_table__sort_indicator_Color = exports.c_table__sort_indicator_Color = {
    "name": "--pf-c-table__sort-indicator--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-c-table__sort-indicator--Color)"
  };
  const c_table__sort_indicator_hover_Color = exports.c_table__sort_indicator_hover_Color = {
    "name": "--pf-c-table__sort-indicator--hover--Color",
    "value": "#151515",
    "var": "var(--pf-c-table__sort-indicator--hover--Color)"
  };
  const c_table__sort_c_button_Color = exports.c_table__sort_c_button_Color = {
    "name": "--pf-c-table__sort--c-button--Color",
    "value": "#151515",
    "var": "var(--pf-c-table__sort--c-button--Color)"
  };
  const c_table__sort_indicator_LineHeight = exports.c_table__sort_indicator_LineHeight = {
    "name": "--pf-c-table__sort-indicator--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-table__sort-indicator--LineHeight)"
  };
  const c_table__icon_inline_MarginRight = exports.c_table__icon_inline_MarginRight = {
    "name": "--pf-c-table__icon-inline--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-table__icon-inline--MarginRight)"
  };
  const c_table_nested_first_last_child_PaddingRight = exports.c_table_nested_first_last_child_PaddingRight = {
    "name": "--pf-c-table--nested--first-last-child--PaddingRight",
    "value": "4rem",
    "var": "var(--pf-c-table--nested--first-last-child--PaddingRight)"
  };
  const c_table_nested_first_last_child_PaddingLeft = exports.c_table_nested_first_last_child_PaddingLeft = {
    "name": "--pf-c-table--nested--first-last-child--PaddingLeft",
    "value": "4rem",
    "var": "var(--pf-c-table--nested--first-last-child--PaddingLeft)"
  };
  const c_table_m_compact_th_PaddingTop = exports.c_table_m_compact_th_PaddingTop = {
    "name": "--pf-c-table--m-compact-th--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-table--m-compact-th--PaddingTop)"
  };
  const c_table_m_compact_th_PaddingBottom = exports.c_table_m_compact_th_PaddingBottom = {
    "name": "--pf-c-table--m-compact-th--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-table--m-compact-th--PaddingBottom)"
  };
  const c_table_m_compact_cell_PaddingTop = exports.c_table_m_compact_cell_PaddingTop = {
    "name": "--pf-c-table--m-compact-cell--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-table--m-compact-cell--PaddingTop)"
  };
  const c_table_m_compact_cell_PaddingRight = exports.c_table_m_compact_cell_PaddingRight = {
    "name": "--pf-c-table--m-compact-cell--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-table--m-compact-cell--PaddingRight)"
  };
  const c_table_m_compact_cell_PaddingBottom = exports.c_table_m_compact_cell_PaddingBottom = {
    "name": "--pf-c-table--m-compact-cell--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-table--m-compact-cell--PaddingBottom)"
  };
  const c_table_m_compact_cell_PaddingLeft = exports.c_table_m_compact_cell_PaddingLeft = {
    "name": "--pf-c-table--m-compact-cell--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-table--m-compact-cell--PaddingLeft)"
  };
  const c_table_m_compact_cell_first_last_child_PaddingLeft = exports.c_table_m_compact_cell_first_last_child_PaddingLeft = {
    "name": "--pf-c-table--m-compact-cell--first-last-child--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-table--m-compact-cell--first-last-child--PaddingLeft)"
  };
  const c_table_m_compact_cell_first_last_child_PaddingRight = exports.c_table_m_compact_cell_first_last_child_PaddingRight = {
    "name": "--pf-c-table--m-compact-cell--first-last-child--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-table--m-compact-cell--first-last-child--PaddingRight)"
  };
  const c_table_m_compact_FontSize = exports.c_table_m_compact_FontSize = {
    "name": "--pf-c-table--m-compact--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-table--m-compact--FontSize)"
  };
  const c_table_m_compact__expandable_row_content_PaddingTop = exports.c_table_m_compact__expandable_row_content_PaddingTop = {
    "name": "--pf-c-table--m-compact__expandable-row-content--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-table--m-compact__expandable-row-content--PaddingTop)"
  };
  const c_table_m_compact__expandable_row_content_PaddingRight = exports.c_table_m_compact__expandable_row_content_PaddingRight = {
    "name": "--pf-c-table--m-compact__expandable-row-content--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-table--m-compact__expandable-row-content--PaddingRight)"
  };
  const c_table_m_compact__expandable_row_content_PaddingBottom = exports.c_table_m_compact__expandable_row_content_PaddingBottom = {
    "name": "--pf-c-table--m-compact__expandable-row-content--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-table--m-compact__expandable-row-content--PaddingBottom)"
  };
  const c_table_m_compact__expandable_row_content_PaddingLeft = exports.c_table_m_compact__expandable_row_content_PaddingLeft = {
    "name": "--pf-c-table--m-compact__expandable-row-content--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-table--m-compact__expandable-row-content--PaddingLeft)"
  };
  const c_table__compound_expansion_toggle_BorderTop_BorderWidth = exports.c_table__compound_expansion_toggle_BorderTop_BorderWidth = {
    "name": "--pf-c-table__compound-expansion-toggle--BorderTop--BorderWidth",
    "value": "3px",
    "var": "var(--pf-c-table__compound-expansion-toggle--BorderTop--BorderWidth)"
  };
  const c_table__compound_expansion_toggle_BorderTop_BorderColor = exports.c_table__compound_expansion_toggle_BorderTop_BorderColor = {
    "name": "--pf-c-table__compound-expansion-toggle--BorderTop--BorderColor",
    "value": "#06c",
    "var": "var(--pf-c-table__compound-expansion-toggle--BorderTop--BorderColor)"
  };
  const c_table__compound_expansion_toggle_BorderRight_BorderWidth = exports.c_table__compound_expansion_toggle_BorderRight_BorderWidth = {
    "name": "--pf-c-table__compound-expansion-toggle--BorderRight--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-table__compound-expansion-toggle--BorderRight--BorderWidth)"
  };
  const c_table__compound_expansion_toggle_BorderLeft_BorderWidth = exports.c_table__compound_expansion_toggle_BorderLeft_BorderWidth = {
    "name": "--pf-c-table__compound-expansion-toggle--BorderLeft--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-table__compound-expansion-toggle--BorderLeft--BorderWidth)"
  };
  const c_table__compound_expansion_toggle_BorderRight_BorderColor = exports.c_table__compound_expansion_toggle_BorderRight_BorderColor = {
    "name": "--pf-c-table__compound-expansion-toggle--BorderRight--BorderColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-table__compound-expansion-toggle--BorderRight--BorderColor)"
  };
  const c_table__compound_expansion_toggle_BorderLeft_BorderColor = exports.c_table__compound_expansion_toggle_BorderLeft_BorderColor = {
    "name": "--pf-c-table__compound-expansion-toggle--BorderLeft--BorderColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-table__compound-expansion-toggle--BorderLeft--BorderColor)"
  };
  const c_table__compound_expansion_toggle_BorderBottom_BorderWidth = exports.c_table__compound_expansion_toggle_BorderBottom_BorderWidth = {
    "name": "--pf-c-table__compound-expansion-toggle--BorderBottom--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-table__compound-expansion-toggle--BorderBottom--BorderWidth)"
  };
  const c_table__compound_expansion_toggle_BorderBottom_BorderColor = exports.c_table__compound_expansion_toggle_BorderBottom_BorderColor = {
    "name": "--pf-c-table__compound-expansion-toggle--BorderBottom--BorderColor",
    "value": "#fff",
    "var": "var(--pf-c-table__compound-expansion-toggle--BorderBottom--BorderColor)"
  };
  const c_table__expandable_row_m_expanded_BoxShadow = exports.c_table__expandable_row_m_expanded_BoxShadow = {
    "name": "--pf-c-table__expandable-row--m-expanded--BoxShadow",
    "value": "0 0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-c-table__expandable-row--m-expanded--BoxShadow)"
  };
  const c_table__expandable_row_m_expanded_BorderBottomColor = exports.c_table__expandable_row_m_expanded_BorderBottomColor = {
    "name": "--pf-c-table__expandable-row--m-expanded--BorderBottomColor",
    "value": "#fff",
    "var": "var(--pf-c-table__expandable-row--m-expanded--BorderBottomColor)"
  };
  const c_table__sort_sorted_Color = exports.c_table__sort_sorted_Color = {
    "name": "--pf-c-table__sort--sorted--Color",
    "value": "#06c",
    "var": "var(--pf-c-table__sort--sorted--Color)"
  };
  const c_tabs__item_BackgroundColor = exports.c_tabs__item_BackgroundColor = {
    "name": "--pf-c-tabs__item--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-tabs__item--BackgroundColor)"
  };
  const c_tabs__item_BorderColor = exports.c_tabs__item_BorderColor = {
    "name": "--pf-c-tabs__item--BorderColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-tabs__item--BorderColor)"
  };
  const c_tabs__item_BorderWidth = exports.c_tabs__item_BorderWidth = {
    "name": "--pf-c-tabs__item--BorderWidth",
    "value": "1px",
    "var": "var(--pf-c-tabs__item--BorderWidth)"
  };
  const c_tabs__item_m_current_ZIndex = exports.c_tabs__item_m_current_ZIndex = {
    "name": "--pf-c-tabs__item--m-current--ZIndex",
    "value": "200",
    "var": "var(--pf-c-tabs__item--m-current--ZIndex)"
  };
  const c_tabs__item_m_current_Color = exports.c_tabs__item_m_current_Color = {
    "name": "--pf-c-tabs__item--m-current--Color",
    "value": "#06c",
    "var": "var(--pf-c-tabs__item--m-current--Color)"
  };
  const c_tabs__item_m_current_BorderTopWidth = exports.c_tabs__item_m_current_BorderTopWidth = {
    "name": "--pf-c-tabs__item--m-current--BorderTopWidth",
    "value": "2px",
    "var": "var(--pf-c-tabs__item--m-current--BorderTopWidth)"
  };
  const c_tabs__item_hover_Color = exports.c_tabs__item_hover_Color = {
    "name": "--pf-c-tabs__item--hover--Color",
    "value": "#737679",
    "var": "var(--pf-c-tabs__item--hover--Color)"
  };
  const c_tabs__button_Color = exports.c_tabs__button_Color = {
    "name": "--pf-c-tabs__button--Color",
    "value": "#151515",
    "var": "var(--pf-c-tabs__button--Color)"
  };
  const c_tabs__button_FontWeight = exports.c_tabs__button_FontWeight = {
    "name": "--pf-c-tabs__button--FontWeight",
    "value": "400",
    "var": "var(--pf-c-tabs__button--FontWeight)"
  };
  const c_tabs__button_Background = exports.c_tabs__button_Background = {
    "name": "--pf-c-tabs__button--Background",
    "value": "transparent",
    "var": "var(--pf-c-tabs__button--Background)"
  };
  const c_tabs__button_OutlineOffset = exports.c_tabs__button_OutlineOffset = {
    "name": "--pf-c-tabs__button--OutlineOffset",
    "value": "calc(-1*0.25rem)",
    "var": "var(--pf-c-tabs__button--OutlineOffset)"
  };
  const c_tabs__button_PaddingTop = exports.c_tabs__button_PaddingTop = {
    "name": "--pf-c-tabs__button--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-tabs__button--PaddingTop)"
  };
  const c_tabs__button_PaddingRight = exports.c_tabs__button_PaddingRight = {
    "name": "--pf-c-tabs__button--PaddingRight",
    "value": "0.5rem",
    "var": "var(--pf-c-tabs__button--PaddingRight)"
  };
  const c_tabs__button_PaddingBottom = exports.c_tabs__button_PaddingBottom = {
    "name": "--pf-c-tabs__button--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-tabs__button--PaddingBottom)"
  };
  const c_tabs__button_PaddingLeft = exports.c_tabs__button_PaddingLeft = {
    "name": "--pf-c-tabs__button--PaddingLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-tabs__button--PaddingLeft)"
  };
  const c_tabs__scroll_button_Width = exports.c_tabs__scroll_button_Width = {
    "name": "--pf-c-tabs__scroll-button--Width",
    "value": "2rem",
    "var": "var(--pf-c-tabs__scroll-button--Width)"
  };
  const c_tabs__scroll_button_ZIndex = exports.c_tabs__scroll_button_ZIndex = {
    "name": "--pf-c-tabs__scroll-button--ZIndex",
    "value": "100",
    "var": "var(--pf-c-tabs__scroll-button--ZIndex)"
  };
  const c_tabs__scroll_button_m_secondary_hover_Color = exports.c_tabs__scroll_button_m_secondary_hover_Color = {
    "name": "--pf-c-tabs__scroll-button--m-secondary--hover--Color",
    "value": "#06c",
    "var": "var(--pf-c-tabs__scroll-button--m-secondary--hover--Color)"
  };
  const c_tabs__scroll_button_m_secondary_right_m_start_current_Color = exports.c_tabs__scroll_button_m_secondary_right_m_start_current_Color = {
    "name": "--pf-c-tabs__scroll-button--m-secondary-right--m-start-current--Color",
    "value": "#06c",
    "var": "var(--pf-c-tabs__scroll-button--m-secondary-right--m-start-current--Color)"
  };
  const c_tabs__scroll_button_m_secondary_nth_of_type_1_BoxShadow = exports.c_tabs__scroll_button_m_secondary_nth_of_type_1_BoxShadow = {
    "name": "--pf-c-tabs__scroll-button--m-secondary--nth-of-type-1--BoxShadow",
    "value": "0.75rem 0 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-c-tabs__scroll-button--m-secondary--nth-of-type-1--BoxShadow)"
  };
  const c_tabs__scroll_button_m_secondary_nth_of_type_2_BoxShadow = exports.c_tabs__scroll_button_m_secondary_nth_of_type_2_BoxShadow = {
    "name": "--pf-c-tabs__scroll-button--m-secondary--nth-of-type-2--BoxShadow",
    "value": "-0.75rem 0 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-c-tabs__scroll-button--m-secondary--nth-of-type-2--BoxShadow)"
  };
  const c_title_FontFamily = exports.c_title_FontFamily = {
    "name": "--pf-c-title--FontFamily",
    "value": "RedHatDisplay,Overpass,overpass,helvetica,arial,sans-serif",
    "var": "var(--pf-c-title--FontFamily)"
  };
  const c_title_m_4xl_LineHeight = exports.c_title_m_4xl_LineHeight = {
    "name": "--pf-c-title--m-4xl--LineHeight",
    "value": "1.3",
    "var": "var(--pf-c-title--m-4xl--LineHeight)"
  };
  const c_title_m_4xl_FontSize = exports.c_title_m_4xl_FontSize = {
    "name": "--pf-c-title--m-4xl--FontSize",
    "value": "2.25rem",
    "var": "var(--pf-c-title--m-4xl--FontSize)"
  };
  const c_title_m_4xl_FontWeight = exports.c_title_m_4xl_FontWeight = {
    "name": "--pf-c-title--m-4xl--FontWeight",
    "value": "400",
    "var": "var(--pf-c-title--m-4xl--FontWeight)"
  };
  const c_title_m_3xl_LineHeight = exports.c_title_m_3xl_LineHeight = {
    "name": "--pf-c-title--m-3xl--LineHeight",
    "value": "1.3",
    "var": "var(--pf-c-title--m-3xl--LineHeight)"
  };
  const c_title_m_3xl_FontSize = exports.c_title_m_3xl_FontSize = {
    "name": "--pf-c-title--m-3xl--FontSize",
    "value": "1.75rem",
    "var": "var(--pf-c-title--m-3xl--FontSize)"
  };
  const c_title_m_3xl_FontWeight = exports.c_title_m_3xl_FontWeight = {
    "name": "--pf-c-title--m-3xl--FontWeight",
    "value": "400",
    "var": "var(--pf-c-title--m-3xl--FontWeight)"
  };
  const c_title_m_2xl_LineHeight = exports.c_title_m_2xl_LineHeight = {
    "name": "--pf-c-title--m-2xl--LineHeight",
    "value": "1.3",
    "var": "var(--pf-c-title--m-2xl--LineHeight)"
  };
  const c_title_m_2xl_FontSize = exports.c_title_m_2xl_FontSize = {
    "name": "--pf-c-title--m-2xl--FontSize",
    "value": "1.5rem",
    "var": "var(--pf-c-title--m-2xl--FontSize)"
  };
  const c_title_m_2xl_FontWeight = exports.c_title_m_2xl_FontWeight = {
    "name": "--pf-c-title--m-2xl--FontWeight",
    "value": "400",
    "var": "var(--pf-c-title--m-2xl--FontWeight)"
  };
  const c_title_m_xl_LineHeight = exports.c_title_m_xl_LineHeight = {
    "name": "--pf-c-title--m-xl--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-title--m-xl--LineHeight)"
  };
  const c_title_m_xl_FontSize = exports.c_title_m_xl_FontSize = {
    "name": "--pf-c-title--m-xl--FontSize",
    "value": "1.25rem",
    "var": "var(--pf-c-title--m-xl--FontSize)"
  };
  const c_title_m_xl_FontWeight = exports.c_title_m_xl_FontWeight = {
    "name": "--pf-c-title--m-xl--FontWeight",
    "value": "400",
    "var": "var(--pf-c-title--m-xl--FontWeight)"
  };
  const c_title_m_lg_LineHeight = exports.c_title_m_lg_LineHeight = {
    "name": "--pf-c-title--m-lg--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-title--m-lg--LineHeight)"
  };
  const c_title_m_lg_FontSize = exports.c_title_m_lg_FontSize = {
    "name": "--pf-c-title--m-lg--FontSize",
    "value": "1.125rem",
    "var": "var(--pf-c-title--m-lg--FontSize)"
  };
  const c_title_m_lg_FontWeight = exports.c_title_m_lg_FontWeight = {
    "name": "--pf-c-title--m-lg--FontWeight",
    "value": "400",
    "var": "var(--pf-c-title--m-lg--FontWeight)"
  };
  const c_title_m_md_LineHeight = exports.c_title_m_md_LineHeight = {
    "name": "--pf-c-title--m-md--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-title--m-md--LineHeight)"
  };
  const c_title_m_md_FontSize = exports.c_title_m_md_FontSize = {
    "name": "--pf-c-title--m-md--FontSize",
    "value": "1rem",
    "var": "var(--pf-c-title--m-md--FontSize)"
  };
  const c_title_m_md_FontWeight = exports.c_title_m_md_FontWeight = {
    "name": "--pf-c-title--m-md--FontWeight",
    "value": "400",
    "var": "var(--pf-c-title--m-md--FontWeight)"
  };
  const c_toolbar_PaddingTop = exports.c_toolbar_PaddingTop = {
    "name": "--pf-c-toolbar--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-toolbar--PaddingTop)"
  };
  const c_toolbar_PaddingRight = exports.c_toolbar_PaddingRight = {
    "name": "--pf-c-toolbar--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-toolbar--PaddingRight)"
  };
  const c_toolbar_PaddingBottom = exports.c_toolbar_PaddingBottom = {
    "name": "--pf-c-toolbar--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-toolbar--PaddingBottom)"
  };
  const c_toolbar_PaddingLeft = exports.c_toolbar_PaddingLeft = {
    "name": "--pf-c-toolbar--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-toolbar--PaddingLeft)"
  };
  const c_toolbar_md_PaddingRight = exports.c_toolbar_md_PaddingRight = {
    "name": "--pf-c-toolbar--md--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-toolbar--md--PaddingRight)"
  };
  const c_toolbar_md_PaddingLeft = exports.c_toolbar_md_PaddingLeft = {
    "name": "--pf-c-toolbar--md--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-toolbar--md--PaddingLeft)"
  };
  const c_toolbar_child_MarginLeft = exports.c_toolbar_child_MarginLeft = {
    "name": "--pf-c-toolbar--child--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-toolbar--child--MarginLeft)"
  };
  const c_toolbar__bulk_select_MarginRight = exports.c_toolbar__bulk_select_MarginRight = {
    "name": "--pf-c-toolbar__bulk-select--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-toolbar__bulk-select--MarginRight)"
  };
  const c_toolbar__filter_MarginTop = exports.c_toolbar__filter_MarginTop = {
    "name": "--pf-c-toolbar__filter--MarginTop",
    "value": "calc(1rem + 1rem)",
    "var": "var(--pf-c-toolbar__filter--MarginTop)"
  };
  const c_toolbar__filter_MarginLeft = exports.c_toolbar__filter_MarginLeft = {
    "name": "--pf-c-toolbar__filter--MarginLeft",
    "value": "0",
    "var": "var(--pf-c-toolbar__filter--MarginLeft)"
  };
  const c_toolbar__filter_toggle_MarginLeft = exports.c_toolbar__filter_toggle_MarginLeft = {
    "name": "--pf-c-toolbar__filter-toggle--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-toolbar__filter-toggle--MarginLeft)"
  };
  const c_toolbar__filter_toggle_m_expanded_Color = exports.c_toolbar__filter_toggle_m_expanded_Color = {
    "name": "--pf-c-toolbar__filter-toggle--m-expanded--Color",
    "value": "#151515",
    "var": "var(--pf-c-toolbar__filter-toggle--m-expanded--Color)"
  };
  const c_toolbar__sort_MarginLeft = exports.c_toolbar__sort_MarginLeft = {
    "name": "--pf-c-toolbar__sort--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-toolbar__sort--MarginLeft)"
  };
  const c_toolbar__action_group_child_MarginLeft = exports.c_toolbar__action_group_child_MarginLeft = {
    "name": "--pf-c-toolbar__action-group--child--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-toolbar__action-group--child--MarginLeft)"
  };
  const c_toolbar__sort_action_group_MarginLeft = exports.c_toolbar__sort_action_group_MarginLeft = {
    "name": "--pf-c-toolbar__sort--action-group--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-toolbar__sort--action-group--MarginLeft)"
  };
  const c_toolbar__filter_action_group_MarginLeft = exports.c_toolbar__filter_action_group_MarginLeft = {
    "name": "--pf-c-toolbar__filter--action-group--MarginLeft",
    "value": "2rem",
    "var": "var(--pf-c-toolbar__filter--action-group--MarginLeft)"
  };
  const c_toolbar__action_list_MarginLeft = exports.c_toolbar__action_list_MarginLeft = {
    "name": "--pf-c-toolbar__action-list--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-toolbar__action-list--MarginLeft)"
  };
  const c_toolbar__sort_action_list_MarginLeft = exports.c_toolbar__sort_action_list_MarginLeft = {
    "name": "--pf-c-toolbar__sort--action-list--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-toolbar__sort--action-list--MarginLeft)"
  };
  const c_toolbar__total_items_FontSize = exports.c_toolbar__total_items_FontSize = {
    "name": "--pf-c-toolbar__total-items--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-toolbar__total-items--FontSize)"
  };
  const c_toolbar__filter_list_MarginTop = exports.c_toolbar__filter_list_MarginTop = {
    "name": "--pf-c-toolbar__filter-list--MarginTop",
    "value": "1rem",
    "var": "var(--pf-c-toolbar__filter-list--MarginTop)"
  };
  const c_toolbar__filter_list_c_button_MarginLeft = exports.c_toolbar__filter_list_c_button_MarginLeft = {
    "name": "--pf-c-toolbar__filter-list--c-button--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-toolbar__filter-list--c-button--MarginLeft)"
  };
  const c_tooltip_MaxWidth = exports.c_tooltip_MaxWidth = {
    "name": "--pf-c-tooltip--MaxWidth",
    "value": "18.75rem",
    "var": "var(--pf-c-tooltip--MaxWidth)"
  };
  const c_tooltip__content_PaddingTop = exports.c_tooltip__content_PaddingTop = {
    "name": "--pf-c-tooltip__content--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-tooltip__content--PaddingTop)"
  };
  const c_tooltip__content_PaddingRight = exports.c_tooltip__content_PaddingRight = {
    "name": "--pf-c-tooltip__content--PaddingRight",
    "value": "1.5rem",
    "var": "var(--pf-c-tooltip__content--PaddingRight)"
  };
  const c_tooltip__content_PaddingBottom = exports.c_tooltip__content_PaddingBottom = {
    "name": "--pf-c-tooltip__content--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-tooltip__content--PaddingBottom)"
  };
  const c_tooltip__content_PaddingLeft = exports.c_tooltip__content_PaddingLeft = {
    "name": "--pf-c-tooltip__content--PaddingLeft",
    "value": "1.5rem",
    "var": "var(--pf-c-tooltip__content--PaddingLeft)"
  };
  const c_tooltip__content_Color = exports.c_tooltip__content_Color = {
    "name": "--pf-c-tooltip__content--Color",
    "value": "#fff",
    "var": "var(--pf-c-tooltip__content--Color)"
  };
  const c_tooltip__content_BackgroundColor = exports.c_tooltip__content_BackgroundColor = {
    "name": "--pf-c-tooltip__content--BackgroundColor",
    "value": "#151515",
    "var": "var(--pf-c-tooltip__content--BackgroundColor)"
  };
  const c_tooltip__content_FontSize = exports.c_tooltip__content_FontSize = {
    "name": "--pf-c-tooltip__content--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-tooltip__content--FontSize)"
  };
  const c_tooltip__arrow_Width = exports.c_tooltip__arrow_Width = {
    "name": "--pf-c-tooltip__arrow--Width",
    "value": "0.9375rem",
    "var": "var(--pf-c-tooltip__arrow--Width)"
  };
  const c_tooltip__arrow_Height = exports.c_tooltip__arrow_Height = {
    "name": "--pf-c-tooltip__arrow--Height",
    "value": "0.9375rem",
    "var": "var(--pf-c-tooltip__arrow--Height)"
  };
  const c_tooltip__arrow_m_top_Transform = exports.c_tooltip__arrow_m_top_Transform = {
    "name": "--pf-c-tooltip__arrow--m-top--Transform",
    "value": "translate(-50%,50%) rotate(45deg)",
    "var": "var(--pf-c-tooltip__arrow--m-top--Transform)"
  };
  const c_tooltip__arrow_m_right_Transform = exports.c_tooltip__arrow_m_right_Transform = {
    "name": "--pf-c-tooltip__arrow--m-right--Transform",
    "value": "translate(-50%,-50%) rotate(45deg)",
    "var": "var(--pf-c-tooltip__arrow--m-right--Transform)"
  };
  const c_tooltip__arrow_m_bottom_Transform = exports.c_tooltip__arrow_m_bottom_Transform = {
    "name": "--pf-c-tooltip__arrow--m-bottom--Transform",
    "value": "translate(-50%,-50%) rotate(45deg)",
    "var": "var(--pf-c-tooltip__arrow--m-bottom--Transform)"
  };
  const c_tooltip__arrow_m_left_Transform = exports.c_tooltip__arrow_m_left_Transform = {
    "name": "--pf-c-tooltip__arrow--m-left--Transform",
    "value": "translate(50%,-50%) rotate(45deg)",
    "var": "var(--pf-c-tooltip__arrow--m-left--Transform)"
  };
  const c_wizard_BoxShadow = exports.c_wizard_BoxShadow = {
    "name": "--pf-c-wizard--BoxShadow",
    "value": "none",
    "var": "var(--pf-c-wizard--BoxShadow)"
  };
  const c_wizard_Height = exports.c_wizard_Height = {
    "name": "--pf-c-wizard--Height",
    "value": "100%",
    "var": "var(--pf-c-wizard--Height)"
  };
  const c_wizard_Width = exports.c_wizard_Width = {
    "name": "--pf-c-wizard--Width",
    "value": "auto",
    "var": "var(--pf-c-wizard--Width)"
  };
  const c_wizard_lg_Width = exports.c_wizard_lg_Width = {
    "name": "--pf-c-wizard--lg--Width",
    "value": "calc(100% - 3rem*2)",
    "var": "var(--pf-c-wizard--lg--Width)"
  };
  const c_wizard_lg_MaxWidth = exports.c_wizard_lg_MaxWidth = {
    "name": "--pf-c-wizard--lg--MaxWidth",
    "value": "77rem",
    "var": "var(--pf-c-wizard--lg--MaxWidth)"
  };
  const c_wizard_lg_Height = exports.c_wizard_lg_Height = {
    "name": "--pf-c-wizard--lg--Height",
    "value": "47.625rem",
    "var": "var(--pf-c-wizard--lg--Height)"
  };
  const c_wizard_lg_MaxHeight = exports.c_wizard_lg_MaxHeight = {
    "name": "--pf-c-wizard--lg--MaxHeight",
    "value": "calc(100% - 3rem*2)",
    "var": "var(--pf-c-wizard--lg--MaxHeight)"
  };
  const c_wizard_m_full_width_lg_MaxWidth = exports.c_wizard_m_full_width_lg_MaxWidth = {
    "name": "--pf-c-wizard--m-full-width--lg--MaxWidth",
    "value": "auto",
    "var": "var(--pf-c-wizard--m-full-width--lg--MaxWidth)"
  };
  const c_wizard_m_full_height_lg_Height = exports.c_wizard_m_full_height_lg_Height = {
    "name": "--pf-c-wizard--m-full-height--lg--Height",
    "value": "100%",
    "var": "var(--pf-c-wizard--m-full-height--lg--Height)"
  };
  const c_wizard_m_in_page_BoxShadow = exports.c_wizard_m_in_page_BoxShadow = {
    "name": "--pf-c-wizard--m-in-page--BoxShadow",
    "value": "none",
    "var": "var(--pf-c-wizard--m-in-page--BoxShadow)"
  };
  const c_wizard_m_in_page_Height = exports.c_wizard_m_in_page_Height = {
    "name": "--pf-c-wizard--m-in-page--Height",
    "value": "100%",
    "var": "var(--pf-c-wizard--m-in-page--Height)"
  };
  const c_wizard_m_in_page_Width = exports.c_wizard_m_in_page_Width = {
    "name": "--pf-c-wizard--m-in-page--Width",
    "value": "auto",
    "var": "var(--pf-c-wizard--m-in-page--Width)"
  };
  const c_wizard_m_in_page_lg_MaxWidth = exports.c_wizard_m_in_page_lg_MaxWidth = {
    "name": "--pf-c-wizard--m-in-page--lg--MaxWidth",
    "value": "none",
    "var": "var(--pf-c-wizard--m-in-page--lg--MaxWidth)"
  };
  const c_wizard_m_in_page_lg_MaxHeight = exports.c_wizard_m_in_page_lg_MaxHeight = {
    "name": "--pf-c-wizard--m-in-page--lg--MaxHeight",
    "value": "none",
    "var": "var(--pf-c-wizard--m-in-page--lg--MaxHeight)"
  };
  const c_wizard__header_BackgroundColor = exports.c_wizard__header_BackgroundColor = {
    "name": "--pf-c-wizard__header--BackgroundColor",
    "value": "#151515",
    "var": "var(--pf-c-wizard__header--BackgroundColor)"
  };
  const c_wizard__header_ZIndex = exports.c_wizard__header_ZIndex = {
    "name": "--pf-c-wizard__header--ZIndex",
    "value": "300",
    "var": "var(--pf-c-wizard__header--ZIndex)"
  };
  const c_wizard__header_PaddingTop = exports.c_wizard__header_PaddingTop = {
    "name": "--pf-c-wizard__header--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-wizard__header--PaddingTop)"
  };
  const c_wizard__header_PaddingRight = exports.c_wizard__header_PaddingRight = {
    "name": "--pf-c-wizard__header--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-wizard__header--PaddingRight)"
  };
  const c_wizard__header_PaddingBottom = exports.c_wizard__header_PaddingBottom = {
    "name": "--pf-c-wizard__header--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-wizard__header--PaddingBottom)"
  };
  const c_wizard__header_PaddingLeft = exports.c_wizard__header_PaddingLeft = {
    "name": "--pf-c-wizard__header--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-wizard__header--PaddingLeft)"
  };
  const c_wizard__header_lg_PaddingRight = exports.c_wizard__header_lg_PaddingRight = {
    "name": "--pf-c-wizard__header--lg--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-wizard__header--lg--PaddingRight)"
  };
  const c_wizard__header_lg_PaddingLeft = exports.c_wizard__header_lg_PaddingLeft = {
    "name": "--pf-c-wizard__header--lg--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-wizard__header--lg--PaddingLeft)"
  };
  const c_wizard__close_Top = exports.c_wizard__close_Top = {
    "name": "--pf-c-wizard__close--Top",
    "value": "calc(1.5rem - 0.375rem)",
    "var": "var(--pf-c-wizard__close--Top)"
  };
  const c_wizard__close_Right = exports.c_wizard__close_Right = {
    "name": "--pf-c-wizard__close--Right",
    "value": "0",
    "var": "var(--pf-c-wizard__close--Right)"
  };
  const c_wizard__close_lg_Right = exports.c_wizard__close_lg_Right = {
    "name": "--pf-c-wizard__close--lg--Right",
    "value": "1rem",
    "var": "var(--pf-c-wizard__close--lg--Right)"
  };
  const c_wizard__close_FontSize = exports.c_wizard__close_FontSize = {
    "name": "--pf-c-wizard__close--FontSize",
    "value": "1.25rem",
    "var": "var(--pf-c-wizard__close--FontSize)"
  };
  const c_wizard__title_PaddingRight = exports.c_wizard__title_PaddingRight = {
    "name": "--pf-c-wizard__title--PaddingRight",
    "value": "3rem",
    "var": "var(--pf-c-wizard__title--PaddingRight)"
  };
  const c_wizard__description_PaddingTop = exports.c_wizard__description_PaddingTop = {
    "name": "--pf-c-wizard__description--PaddingTop",
    "value": "0.5rem",
    "var": "var(--pf-c-wizard__description--PaddingTop)"
  };
  const c_wizard__description_Color = exports.c_wizard__description_Color = {
    "name": "--pf-c-wizard__description--Color",
    "value": "#ededed",
    "var": "var(--pf-c-wizard__description--Color)"
  };
  const c_wizard__nav_link_Color = exports.c_wizard__nav_link_Color = {
    "name": "--pf-c-wizard__nav-link--Color",
    "value": "#737679",
    "var": "var(--pf-c-wizard__nav-link--Color)"
  };
  const c_wizard__nav_link_TextDecoration = exports.c_wizard__nav_link_TextDecoration = {
    "name": "--pf-c-wizard__nav-link--TextDecoration",
    "value": "underline",
    "var": "var(--pf-c-wizard__nav-link--TextDecoration)"
  };
  const c_wizard__nav_link_hover_Color = exports.c_wizard__nav_link_hover_Color = {
    "name": "--pf-c-wizard__nav-link--hover--Color",
    "value": "#004080",
    "var": "var(--pf-c-wizard__nav-link--hover--Color)"
  };
  const c_wizard__nav_link_focus_Color = exports.c_wizard__nav_link_focus_Color = {
    "name": "--pf-c-wizard__nav-link--focus--Color",
    "value": "#004080",
    "var": "var(--pf-c-wizard__nav-link--focus--Color)"
  };
  const c_wizard__nav_link_m_current_Color = exports.c_wizard__nav_link_m_current_Color = {
    "name": "--pf-c-wizard__nav-link--m-current--Color",
    "value": "#004080",
    "var": "var(--pf-c-wizard__nav-link--m-current--Color)"
  };
  const c_wizard__nav_link_m_disabled_Color = exports.c_wizard__nav_link_m_disabled_Color = {
    "name": "--pf-c-wizard__nav-link--m-disabled--Color",
    "value": "#737679",
    "var": "var(--pf-c-wizard__nav-link--m-disabled--Color)"
  };
  const c_wizard__nav_list__nav_list__nav_link_m_current_FontWeight = exports.c_wizard__nav_list__nav_list__nav_link_m_current_FontWeight = {
    "name": "--pf-c-wizard__nav-list__nav-list__nav-link--m-current--FontWeight",
    "value": "700",
    "var": "var(--pf-c-wizard__nav-list__nav-list__nav-link--m-current--FontWeight)"
  };
  const c_wizard__nav_link_before_Width = exports.c_wizard__nav_link_before_Width = {
    "name": "--pf-c-wizard__nav-link--before--Width",
    "value": "1.5rem",
    "var": "var(--pf-c-wizard__nav-link--before--Width)"
  };
  const c_wizard__nav_link_before_Height = exports.c_wizard__nav_link_before_Height = {
    "name": "--pf-c-wizard__nav-link--before--Height",
    "value": "1.5rem",
    "var": "var(--pf-c-wizard__nav-link--before--Height)"
  };
  const c_wizard__nav_link_before_Top = exports.c_wizard__nav_link_before_Top = {
    "name": "--pf-c-wizard__nav-link--before--Top",
    "value": "0",
    "var": "var(--pf-c-wizard__nav-link--before--Top)"
  };
  const c_wizard__nav_link_before_BackgroundColor = exports.c_wizard__nav_link_before_BackgroundColor = {
    "name": "--pf-c-wizard__nav-link--before--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-wizard__nav-link--before--BackgroundColor)"
  };
  const c_wizard__nav_link_before_BorderRadius = exports.c_wizard__nav_link_before_BorderRadius = {
    "name": "--pf-c-wizard__nav-link--before--BorderRadius",
    "value": "30em",
    "var": "var(--pf-c-wizard__nav-link--before--BorderRadius)"
  };
  const c_wizard__nav_link_before_Color = exports.c_wizard__nav_link_before_Color = {
    "name": "--pf-c-wizard__nav-link--before--Color",
    "value": "#737679",
    "var": "var(--pf-c-wizard__nav-link--before--Color)"
  };
  const c_wizard__nav_link_before_FontSize = exports.c_wizard__nav_link_before_FontSize = {
    "name": "--pf-c-wizard__nav-link--before--FontSize",
    "value": "0.875rem",
    "var": "var(--pf-c-wizard__nav-link--before--FontSize)"
  };
  const c_wizard__nav_link_before_Transform = exports.c_wizard__nav_link_before_Transform = {
    "name": "--pf-c-wizard__nav-link--before--Transform",
    "value": "translateX(calc(-100% - 0.5rem))",
    "var": "var(--pf-c-wizard__nav-link--before--Transform)"
  };
  const c_wizard__nav_link_m_current_before_BackgroundColor = exports.c_wizard__nav_link_m_current_before_BackgroundColor = {
    "name": "--pf-c-wizard__nav-link--m-current--before--BackgroundColor",
    "value": "#06c",
    "var": "var(--pf-c-wizard__nav-link--m-current--before--BackgroundColor)"
  };
  const c_wizard__nav_link_m_current_before_Color = exports.c_wizard__nav_link_m_current_before_Color = {
    "name": "--pf-c-wizard__nav-link--m-current--before--Color",
    "value": "#fff",
    "var": "var(--pf-c-wizard__nav-link--m-current--before--Color)"
  };
  const c_wizard__nav_link_m_disabled_before_BackgroundColor = exports.c_wizard__nav_link_m_disabled_before_BackgroundColor = {
    "name": "--pf-c-wizard__nav-link--m-disabled--before--BackgroundColor",
    "value": "transparent",
    "var": "var(--pf-c-wizard__nav-link--m-disabled--before--BackgroundColor)"
  };
  const c_wizard__nav_link_m_disabled_before_Color = exports.c_wizard__nav_link_m_disabled_before_Color = {
    "name": "--pf-c-wizard__nav-link--m-disabled--before--Color",
    "value": "#737679",
    "var": "var(--pf-c-wizard__nav-link--m-disabled--before--Color)"
  };
  const c_wizard__toggle_BackgroundColor = exports.c_wizard__toggle_BackgroundColor = {
    "name": "--pf-c-wizard__toggle--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-wizard__toggle--BackgroundColor)"
  };
  const c_wizard__toggle_ZIndex = exports.c_wizard__toggle_ZIndex = {
    "name": "--pf-c-wizard__toggle--ZIndex",
    "value": "300",
    "var": "var(--pf-c-wizard__toggle--ZIndex)"
  };
  const c_wizard__toggle_BoxShadow = exports.c_wizard__toggle_BoxShadow = {
    "name": "--pf-c-wizard__toggle--BoxShadow",
    "value": "0 0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-c-wizard__toggle--BoxShadow)"
  };
  const c_wizard__toggle_PaddingTop = exports.c_wizard__toggle_PaddingTop = {
    "name": "--pf-c-wizard__toggle--PaddingTop",
    "value": "1.5rem",
    "var": "var(--pf-c-wizard__toggle--PaddingTop)"
  };
  const c_wizard__toggle_PaddingRight = exports.c_wizard__toggle_PaddingRight = {
    "name": "--pf-c-wizard__toggle--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-wizard__toggle--PaddingRight)"
  };
  const c_wizard__toggle_PaddingBottom = exports.c_wizard__toggle_PaddingBottom = {
    "name": "--pf-c-wizard__toggle--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-wizard__toggle--PaddingBottom)"
  };
  const c_wizard__toggle_PaddingLeft = exports.c_wizard__toggle_PaddingLeft = {
    "name": "--pf-c-wizard__toggle--PaddingLeft",
    "value": "calc(1rem + 1.5rem + 0.5rem)",
    "var": "var(--pf-c-wizard__toggle--PaddingLeft)"
  };
  const c_wizard_m_in_page__toggle_md_PaddingLeft = exports.c_wizard_m_in_page__toggle_md_PaddingLeft = {
    "name": "--pf-c-wizard--m-in-page__toggle--md--PaddingLeft",
    "value": "calc(2rem + 1.5rem + 0.5rem)",
    "var": "var(--pf-c-wizard--m-in-page__toggle--md--PaddingLeft)"
  };
  const c_wizard__toggle_num_before_Top = exports.c_wizard__toggle_num_before_Top = {
    "name": "--pf-c-wizard__toggle-num--before--Top",
    "value": "0",
    "var": "var(--pf-c-wizard__toggle-num--before--Top)"
  };
  const c_wizard__toggle_list_item_not_last_child_MarginRight = exports.c_wizard__toggle_list_item_not_last_child_MarginRight = {
    "name": "--pf-c-wizard__toggle-list-item--not-last-child--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-wizard__toggle-list-item--not-last-child--MarginRight)"
  };
  const c_wizard__toggle_list_item_MarginBottom = exports.c_wizard__toggle_list_item_MarginBottom = {
    "name": "--pf-c-wizard__toggle-list-item--MarginBottom",
    "value": "0.25rem",
    "var": "var(--pf-c-wizard__toggle-list-item--MarginBottom)"
  };
  const c_wizard__toggle_list_MarginRight = exports.c_wizard__toggle_list_MarginRight = {
    "name": "--pf-c-wizard__toggle-list--MarginRight",
    "value": "0.5rem",
    "var": "var(--pf-c-wizard__toggle-list--MarginRight)"
  };
  const c_wizard__toggle_list_MarginBottom = exports.c_wizard__toggle_list_MarginBottom = {
    "name": "--pf-c-wizard__toggle-list--MarginBottom",
    "value": "calc(0.25rem*-1)",
    "var": "var(--pf-c-wizard__toggle-list--MarginBottom)"
  };
  const c_wizard__toggle_separator_MarginLeft = exports.c_wizard__toggle_separator_MarginLeft = {
    "name": "--pf-c-wizard__toggle-separator--MarginLeft",
    "value": "0.5rem",
    "var": "var(--pf-c-wizard__toggle-separator--MarginLeft)"
  };
  const c_wizard__toggle_separator_Color = exports.c_wizard__toggle_separator_Color = {
    "name": "--pf-c-wizard__toggle-separator--Color",
    "value": "#8a8d90",
    "var": "var(--pf-c-wizard__toggle-separator--Color)"
  };
  const c_wizard__toggle_icon_MarginTop = exports.c_wizard__toggle_icon_MarginTop = {
    "name": "--pf-c-wizard__toggle-icon--MarginTop",
    "value": "0",
    "var": "var(--pf-c-wizard__toggle-icon--MarginTop)"
  };
  const c_wizard__toggle_icon_LineHeight = exports.c_wizard__toggle_icon_LineHeight = {
    "name": "--pf-c-wizard__toggle-icon--LineHeight",
    "value": "1.5",
    "var": "var(--pf-c-wizard__toggle-icon--LineHeight)"
  };
  const c_wizard__toggle_m_expanded__toggle_icon_Transform = exports.c_wizard__toggle_m_expanded__toggle_icon_Transform = {
    "name": "--pf-c-wizard__toggle--m-expanded__toggle-icon--Transform",
    "value": "rotate(180deg)",
    "var": "var(--pf-c-wizard__toggle--m-expanded__toggle-icon--Transform)"
  };
  const c_wizard__nav_ZIndex = exports.c_wizard__nav_ZIndex = {
    "name": "--pf-c-wizard__nav--ZIndex",
    "value": "200",
    "var": "var(--pf-c-wizard__nav--ZIndex)"
  };
  const c_wizard__nav_BackgroundColor = exports.c_wizard__nav_BackgroundColor = {
    "name": "--pf-c-wizard__nav--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-wizard__nav--BackgroundColor)"
  };
  const c_wizard__nav_BoxShadow = exports.c_wizard__nav_BoxShadow = {
    "name": "--pf-c-wizard__nav--BoxShadow",
    "value": "0 0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-c-wizard__nav--BoxShadow)"
  };
  const c_wizard__nav_lg_BoxShadow = exports.c_wizard__nav_lg_BoxShadow = {
    "name": "--pf-c-wizard__nav--lg--BoxShadow",
    "value": "0.75rem 0 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-c-wizard__nav--lg--BoxShadow)"
  };
  const c_wizard__nav_Width = exports.c_wizard__nav_Width = {
    "name": "--pf-c-wizard__nav--Width",
    "value": "100%",
    "var": "var(--pf-c-wizard__nav--Width)"
  };
  const c_wizard__nav_lg_Width = exports.c_wizard__nav_lg_Width = {
    "name": "--pf-c-wizard__nav--lg--Width",
    "value": "18.75rem",
    "var": "var(--pf-c-wizard__nav--lg--Width)"
  };
  const c_wizard_m_compact_nav__nav_lg_Width = exports.c_wizard_m_compact_nav__nav_lg_Width = {
    "name": "--pf-c-wizard--m-compact-nav__nav--lg--Width",
    "value": "15.625rem",
    "var": "var(--pf-c-wizard--m-compact-nav__nav--lg--Width)"
  };
  const c_wizard_m_in_page__nav_lg_Width = exports.c_wizard_m_in_page__nav_lg_Width = {
    "name": "--pf-c-wizard--m-in-page__nav--lg--Width",
    "value": "15.625rem",
    "var": "var(--pf-c-wizard--m-in-page__nav--lg--Width)"
  };
  const c_wizard_m_in_page__nav_lg_BoxShadow = exports.c_wizard_m_in_page__nav_lg_BoxShadow = {
    "name": "--pf-c-wizard--m-in-page__nav--lg--BoxShadow",
    "value": "none",
    "var": "var(--pf-c-wizard--m-in-page__nav--lg--BoxShadow)"
  };
  const c_wizard_m_in_page__nav_lg_BorderRightWidth = exports.c_wizard_m_in_page__nav_lg_BorderRightWidth = {
    "name": "--pf-c-wizard--m-in-page__nav--lg--BorderRightWidth",
    "value": "1px",
    "var": "var(--pf-c-wizard--m-in-page__nav--lg--BorderRightWidth)"
  };
  const c_wizard_m_in_page__nav_lg_BorderRightColor = exports.c_wizard_m_in_page__nav_lg_BorderRightColor = {
    "name": "--pf-c-wizard--m-in-page__nav--lg--BorderRightColor",
    "value": "#d2d2d2",
    "var": "var(--pf-c-wizard--m-in-page__nav--lg--BorderRightColor)"
  };
  const c_wizard__nav_list_PaddingTop = exports.c_wizard__nav_list_PaddingTop = {
    "name": "--pf-c-wizard__nav-list--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-wizard__nav-list--PaddingTop)"
  };
  const c_wizard__nav_list_PaddingRight = exports.c_wizard__nav_list_PaddingRight = {
    "name": "--pf-c-wizard__nav-list--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-wizard__nav-list--PaddingRight)"
  };
  const c_wizard__nav_list_PaddingBottom = exports.c_wizard__nav_list_PaddingBottom = {
    "name": "--pf-c-wizard__nav-list--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-wizard__nav-list--PaddingBottom)"
  };
  const c_wizard__nav_list_PaddingLeft = exports.c_wizard__nav_list_PaddingLeft = {
    "name": "--pf-c-wizard__nav-list--PaddingLeft",
    "value": "calc(1rem + 1.5rem + 0.5rem)",
    "var": "var(--pf-c-wizard__nav-list--PaddingLeft)"
  };
  const c_wizard__nav_list_lg_PaddingRight = exports.c_wizard__nav_list_lg_PaddingRight = {
    "name": "--pf-c-wizard__nav-list--lg--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-wizard__nav-list--lg--PaddingRight)"
  };
  const c_wizard__nav_list_lg_PaddingLeft = exports.c_wizard__nav_list_lg_PaddingLeft = {
    "name": "--pf-c-wizard__nav-list--lg--PaddingLeft",
    "value": "calc(2rem + 1.5rem + 0.5rem)",
    "var": "var(--pf-c-wizard__nav-list--lg--PaddingLeft)"
  };
  const c_wizard__nav_list_nested_MarginLeft = exports.c_wizard__nav_list_nested_MarginLeft = {
    "name": "--pf-c-wizard__nav-list--nested--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-c-wizard__nav-list--nested--MarginLeft)"
  };
  const c_wizard__nav_list_nested_MarginTop = exports.c_wizard__nav_list_nested_MarginTop = {
    "name": "--pf-c-wizard__nav-list--nested--MarginTop",
    "value": "1rem",
    "var": "var(--pf-c-wizard__nav-list--nested--MarginTop)"
  };
  const c_wizard_m_in_page__nav_list_md_PaddingLeft = exports.c_wizard_m_in_page__nav_list_md_PaddingLeft = {
    "name": "--pf-c-wizard--m-in-page__nav-list--md--PaddingLeft",
    "value": "calc(2rem + 1.5rem + 0.5rem)",
    "var": "var(--pf-c-wizard--m-in-page__nav-list--md--PaddingLeft)"
  };
  const c_wizard__nav_item_MarginTop = exports.c_wizard__nav_item_MarginTop = {
    "name": "--pf-c-wizard__nav-item--MarginTop",
    "value": "1rem",
    "var": "var(--pf-c-wizard__nav-item--MarginTop)"
  };
  const c_wizard__outer_wrap_BackgroundColor = exports.c_wizard__outer_wrap_BackgroundColor = {
    "name": "--pf-c-wizard__outer-wrap--BackgroundColor",
    "value": "#fff",
    "var": "var(--pf-c-wizard__outer-wrap--BackgroundColor)"
  };
  const c_wizard__outer_wrap_lg_PaddingLeft = exports.c_wizard__outer_wrap_lg_PaddingLeft = {
    "name": "--pf-c-wizard__outer-wrap--lg--PaddingLeft",
    "value": "0",
    "var": "var(--pf-c-wizard__outer-wrap--lg--PaddingLeft)"
  };
  const c_wizard__main_ZIndex = exports.c_wizard__main_ZIndex = {
    "name": "--pf-c-wizard__main--ZIndex",
    "value": "100",
    "var": "var(--pf-c-wizard__main--ZIndex)"
  };
  const c_wizard__main_body_PaddingTop = exports.c_wizard__main_body_PaddingTop = {
    "name": "--pf-c-wizard__main-body--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-wizard__main-body--PaddingTop)"
  };
  const c_wizard__main_body_PaddingRight = exports.c_wizard__main_body_PaddingRight = {
    "name": "--pf-c-wizard__main-body--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-wizard__main-body--PaddingRight)"
  };
  const c_wizard__main_body_PaddingBottom = exports.c_wizard__main_body_PaddingBottom = {
    "name": "--pf-c-wizard__main-body--PaddingBottom",
    "value": "1rem",
    "var": "var(--pf-c-wizard__main-body--PaddingBottom)"
  };
  const c_wizard__main_body_PaddingLeft = exports.c_wizard__main_body_PaddingLeft = {
    "name": "--pf-c-wizard__main-body--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-wizard__main-body--PaddingLeft)"
  };
  const c_wizard__main_body_lg_PaddingTop = exports.c_wizard__main_body_lg_PaddingTop = {
    "name": "--pf-c-wizard__main-body--lg--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-wizard__main-body--lg--PaddingTop)"
  };
  const c_wizard__main_body_lg_PaddingRight = exports.c_wizard__main_body_lg_PaddingRight = {
    "name": "--pf-c-wizard__main-body--lg--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-wizard__main-body--lg--PaddingRight)"
  };
  const c_wizard__main_body_lg_PaddingBottom = exports.c_wizard__main_body_lg_PaddingBottom = {
    "name": "--pf-c-wizard__main-body--lg--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-wizard__main-body--lg--PaddingBottom)"
  };
  const c_wizard__main_body_lg_PaddingLeft = exports.c_wizard__main_body_lg_PaddingLeft = {
    "name": "--pf-c-wizard__main-body--lg--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-wizard__main-body--lg--PaddingLeft)"
  };
  const c_wizard_m_in_page__main_body_md_PaddingTop = exports.c_wizard_m_in_page__main_body_md_PaddingTop = {
    "name": "--pf-c-wizard--m-in-page__main-body--md--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-wizard--m-in-page__main-body--md--PaddingTop)"
  };
  const c_wizard_m_in_page__main_body_md_PaddingRight = exports.c_wizard_m_in_page__main_body_md_PaddingRight = {
    "name": "--pf-c-wizard--m-in-page__main-body--md--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-wizard--m-in-page__main-body--md--PaddingRight)"
  };
  const c_wizard_m_in_page__main_body_md_PaddingBottom = exports.c_wizard_m_in_page__main_body_md_PaddingBottom = {
    "name": "--pf-c-wizard--m-in-page__main-body--md--PaddingBottom",
    "value": "2rem",
    "var": "var(--pf-c-wizard--m-in-page__main-body--md--PaddingBottom)"
  };
  const c_wizard_m_in_page__main_body_md_PaddingLeft = exports.c_wizard_m_in_page__main_body_md_PaddingLeft = {
    "name": "--pf-c-wizard--m-in-page__main-body--md--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-wizard--m-in-page__main-body--md--PaddingLeft)"
  };
  const c_wizard__footer_PaddingTop = exports.c_wizard__footer_PaddingTop = {
    "name": "--pf-c-wizard__footer--PaddingTop",
    "value": "1rem",
    "var": "var(--pf-c-wizard__footer--PaddingTop)"
  };
  const c_wizard__footer_PaddingRight = exports.c_wizard__footer_PaddingRight = {
    "name": "--pf-c-wizard__footer--PaddingRight",
    "value": "1rem",
    "var": "var(--pf-c-wizard__footer--PaddingRight)"
  };
  const c_wizard__footer_PaddingBottom = exports.c_wizard__footer_PaddingBottom = {
    "name": "--pf-c-wizard__footer--PaddingBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-wizard__footer--PaddingBottom)"
  };
  const c_wizard__footer_PaddingLeft = exports.c_wizard__footer_PaddingLeft = {
    "name": "--pf-c-wizard__footer--PaddingLeft",
    "value": "1rem",
    "var": "var(--pf-c-wizard__footer--PaddingLeft)"
  };
  const c_wizard__footer_lg_PaddingTop = exports.c_wizard__footer_lg_PaddingTop = {
    "name": "--pf-c-wizard__footer--lg--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-wizard__footer--lg--PaddingTop)"
  };
  const c_wizard__footer_lg_PaddingRight = exports.c_wizard__footer_lg_PaddingRight = {
    "name": "--pf-c-wizard__footer--lg--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-wizard__footer--lg--PaddingRight)"
  };
  const c_wizard__footer_lg_PaddingBottom = exports.c_wizard__footer_lg_PaddingBottom = {
    "name": "--pf-c-wizard__footer--lg--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-wizard__footer--lg--PaddingBottom)"
  };
  const c_wizard__footer_lg_PaddingLeft = exports.c_wizard__footer_lg_PaddingLeft = {
    "name": "--pf-c-wizard__footer--lg--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-wizard__footer--lg--PaddingLeft)"
  };
  const c_wizard__footer_child_MarginRight = exports.c_wizard__footer_child_MarginRight = {
    "name": "--pf-c-wizard__footer--child--MarginRight",
    "value": "1rem",
    "var": "var(--pf-c-wizard__footer--child--MarginRight)"
  };
  const c_wizard__footer_child_MarginBottom = exports.c_wizard__footer_child_MarginBottom = {
    "name": "--pf-c-wizard__footer--child--MarginBottom",
    "value": "0.5rem",
    "var": "var(--pf-c-wizard__footer--child--MarginBottom)"
  };
  const c_wizard_m_in_page__footer_md_PaddingTop = exports.c_wizard_m_in_page__footer_md_PaddingTop = {
    "name": "--pf-c-wizard--m-in-page__footer--md--PaddingTop",
    "value": "2rem",
    "var": "var(--pf-c-wizard--m-in-page__footer--md--PaddingTop)"
  };
  const c_wizard_m_in_page__footer_md_PaddingRight = exports.c_wizard_m_in_page__footer_md_PaddingRight = {
    "name": "--pf-c-wizard--m-in-page__footer--md--PaddingRight",
    "value": "2rem",
    "var": "var(--pf-c-wizard--m-in-page__footer--md--PaddingRight)"
  };
  const c_wizard_m_in_page__footer_md_PaddingBottom = exports.c_wizard_m_in_page__footer_md_PaddingBottom = {
    "name": "--pf-c-wizard--m-in-page__footer--md--PaddingBottom",
    "value": "1.5rem",
    "var": "var(--pf-c-wizard--m-in-page__footer--md--PaddingBottom)"
  };
  const c_wizard_m_in_page__footer_md_PaddingLeft = exports.c_wizard_m_in_page__footer_md_PaddingLeft = {
    "name": "--pf-c-wizard--m-in-page__footer--md--PaddingLeft",
    "value": "2rem",
    "var": "var(--pf-c-wizard--m-in-page__footer--md--PaddingLeft)"
  };
  const l_bullseye_Padding = exports.l_bullseye_Padding = {
    "name": "--pf-l-bullseye--Padding",
    "value": "0",
    "var": "var(--pf-l-bullseye--Padding)"
  };
  const l_flex_Display = exports.l_flex_Display = {
    "name": "--pf-l-flex--Display",
    "value": "inline-flex",
    "var": "var(--pf-l-flex--Display)"
  };
  const l_flex_FlexWrap = exports.l_flex_FlexWrap = {
    "name": "--pf-l-flex--FlexWrap",
    "value": "wrap",
    "var": "var(--pf-l-flex--FlexWrap)"
  };
  const l_flex_AlignItems = exports.l_flex_AlignItems = {
    "name": "--pf-l-flex--AlignItems",
    "value": "baseline",
    "var": "var(--pf-l-flex--AlignItems)"
  };
  const l_flex_m_row_AlignItems = exports.l_flex_m_row_AlignItems = {
    "name": "--pf-l-flex--m-row--AlignItems",
    "value": "baseline",
    "var": "var(--pf-l-flex--m-row--AlignItems)"
  };
  const l_flex_m_row_reverse_AlignItems = exports.l_flex_m_row_reverse_AlignItems = {
    "name": "--pf-l-flex--m-row-reverse--AlignItems",
    "value": "baseline",
    "var": "var(--pf-l-flex--m-row-reverse--AlignItems)"
  };
  const l_flex_spacer_base = exports.l_flex_spacer_base = {
    "name": "--pf-l-flex--spacer-base",
    "value": "1rem",
    "var": "var(--pf-l-flex--spacer-base)"
  };
  const l_flex_spacer = exports.l_flex_spacer = {
    "name": "--pf-l-flex--spacer",
    "value": "4rem",
    "var": "var(--pf-l-flex--spacer)"
  };
  const l_flex_spacer_none = exports.l_flex_spacer_none = {
    "name": "--pf-l-flex--spacer--none",
    "value": "0",
    "var": "var(--pf-l-flex--spacer--none)"
  };
  const l_flex_spacer_xs = exports.l_flex_spacer_xs = {
    "name": "--pf-l-flex--spacer--xs",
    "value": "0.25rem",
    "var": "var(--pf-l-flex--spacer--xs)"
  };
  const l_flex_spacer_sm = exports.l_flex_spacer_sm = {
    "name": "--pf-l-flex--spacer--sm",
    "value": "0.5rem",
    "var": "var(--pf-l-flex--spacer--sm)"
  };
  const l_flex_spacer_md = exports.l_flex_spacer_md = {
    "name": "--pf-l-flex--spacer--md",
    "value": "1rem",
    "var": "var(--pf-l-flex--spacer--md)"
  };
  const l_flex_spacer_lg = exports.l_flex_spacer_lg = {
    "name": "--pf-l-flex--spacer--lg",
    "value": "1.5rem",
    "var": "var(--pf-l-flex--spacer--lg)"
  };
  const l_flex_spacer_xl = exports.l_flex_spacer_xl = {
    "name": "--pf-l-flex--spacer--xl",
    "value": "2rem",
    "var": "var(--pf-l-flex--spacer--xl)"
  };
  const l_flex_spacer_2xl = exports.l_flex_spacer_2xl = {
    "name": "--pf-l-flex--spacer--2xl",
    "value": "3rem",
    "var": "var(--pf-l-flex--spacer--2xl)"
  };
  const l_flex_spacer_3xl = exports.l_flex_spacer_3xl = {
    "name": "--pf-l-flex--spacer--3xl",
    "value": "4rem",
    "var": "var(--pf-l-flex--spacer--3xl)"
  };
  const l_gallery_m_gutter_GridGap = exports.l_gallery_m_gutter_GridGap = {
    "name": "--pf-l-gallery--m-gutter--GridGap",
    "value": "1.5rem",
    "var": "var(--pf-l-gallery--m-gutter--GridGap)"
  };
  const l_gallery_m_gutter_md_GridGap = exports.l_gallery_m_gutter_md_GridGap = {
    "name": "--pf-l-gallery--m-gutter--md--GridGap",
    "value": "1rem",
    "var": "var(--pf-l-gallery--m-gutter--md--GridGap)"
  };
  const l_gallery_GridTemplateColumns = exports.l_gallery_GridTemplateColumns = {
    "name": "--pf-l-gallery--GridTemplateColumns",
    "value": "repeat(auto-fill,minmax(250px,1fr))",
    "var": "var(--pf-l-gallery--GridTemplateColumns)"
  };
  const l_gallery_GridTemplateRows = exports.l_gallery_GridTemplateRows = {
    "name": "--pf-l-gallery--GridTemplateRows",
    "value": "auto",
    "var": "var(--pf-l-gallery--GridTemplateRows)"
  };
  const l_grid_m_gutter_GridGap = exports.l_grid_m_gutter_GridGap = {
    "name": "--pf-l-grid--m-gutter--GridGap",
    "value": "1.5rem",
    "var": "var(--pf-l-grid--m-gutter--GridGap)"
  };
  const l_grid_m_gutter_md_GridGap = exports.l_grid_m_gutter_md_GridGap = {
    "name": "--pf-l-grid--m-gutter--md--GridGap",
    "value": "1rem",
    "var": "var(--pf-l-grid--m-gutter--md--GridGap)"
  };
  const l_grid__item_GridColumnStart = exports.l_grid__item_GridColumnStart = {
    "name": "--pf-l-grid__item--GridColumnStart",
    "value": "col-start 13",
    "var": "var(--pf-l-grid__item--GridColumnStart)"
  };
  const l_grid__item_GridColumnEnd = exports.l_grid__item_GridColumnEnd = {
    "name": "--pf-l-grid__item--GridColumnEnd",
    "value": "span 12",
    "var": "var(--pf-l-grid__item--GridColumnEnd)"
  };
  const l_level_m_gutter_MarginRight = exports.l_level_m_gutter_MarginRight = {
    "name": "--pf-l-level--m-gutter--MarginRight",
    "value": "1.5rem",
    "var": "var(--pf-l-level--m-gutter--MarginRight)"
  };
  const l_level_m_gutter_md_MarginRight = exports.l_level_m_gutter_md_MarginRight = {
    "name": "--pf-l-level--m-gutter--md--MarginRight",
    "value": "1rem",
    "var": "var(--pf-l-level--m-gutter--md--MarginRight)"
  };
  const l_split_m_gutter_MarginRight = exports.l_split_m_gutter_MarginRight = {
    "name": "--pf-l-split--m-gutter--MarginRight",
    "value": "1.5rem",
    "var": "var(--pf-l-split--m-gutter--MarginRight)"
  };
  const l_split_m_gutter_md_MarginRight = exports.l_split_m_gutter_md_MarginRight = {
    "name": "--pf-l-split--m-gutter--md--MarginRight",
    "value": "1rem",
    "var": "var(--pf-l-split--m-gutter--md--MarginRight)"
  };
  const l_stack_m_gutter_MarginBottom = exports.l_stack_m_gutter_MarginBottom = {
    "name": "--pf-l-stack--m-gutter--MarginBottom",
    "value": "1.5rem",
    "var": "var(--pf-l-stack--m-gutter--MarginBottom)"
  };
  const l_stack_m_gutter_md_MarginBottom = exports.l_stack_m_gutter_md_MarginBottom = {
    "name": "--pf-l-stack--m-gutter--md--MarginBottom",
    "value": "1rem",
    "var": "var(--pf-l-stack--m-gutter--md--MarginBottom)"
  };
  const l_toolbar__section_MarginTop = exports.l_toolbar__section_MarginTop = {
    "name": "--pf-l-toolbar__section--MarginTop",
    "value": "1rem",
    "var": "var(--pf-l-toolbar__section--MarginTop)"
  };
  const l_toolbar__group_MarginRight = exports.l_toolbar__group_MarginRight = {
    "name": "--pf-l-toolbar__group--MarginRight",
    "value": "2rem",
    "var": "var(--pf-l-toolbar__group--MarginRight)"
  };
  const l_toolbar__group_MarginLeft = exports.l_toolbar__group_MarginLeft = {
    "name": "--pf-l-toolbar__group--MarginLeft",
    "value": "2rem",
    "var": "var(--pf-l-toolbar__group--MarginLeft)"
  };
  const l_toolbar__item_MarginRight = exports.l_toolbar__item_MarginRight = {
    "name": "--pf-l-toolbar__item--MarginRight",
    "value": "1rem",
    "var": "var(--pf-l-toolbar__item--MarginRight)"
  };
  const l_toolbar__item_MarginLeft = exports.l_toolbar__item_MarginLeft = {
    "name": "--pf-l-toolbar__item--MarginLeft",
    "value": "1rem",
    "var": "var(--pf-l-toolbar__item--MarginLeft)"
  };
  const global_palette_black_100 = exports.global_palette_black_100 = {
    "name": "--pf-global--palette--black-100",
    "value": "#fafafa",
    "var": "var(--pf-global--palette--black-100)"
  };
  const global_palette_black_150 = exports.global_palette_black_150 = {
    "name": "--pf-global--palette--black-150",
    "value": "#f5f5f5",
    "var": "var(--pf-global--palette--black-150)"
  };
  const global_palette_black_200 = exports.global_palette_black_200 = {
    "name": "--pf-global--palette--black-200",
    "value": "#ededed",
    "var": "var(--pf-global--palette--black-200)"
  };
  const global_palette_black_300 = exports.global_palette_black_300 = {
    "name": "--pf-global--palette--black-300",
    "value": "#d2d2d2",
    "var": "var(--pf-global--palette--black-300)"
  };
  const global_palette_black_400 = exports.global_palette_black_400 = {
    "name": "--pf-global--palette--black-400",
    "value": "#b8bbbe",
    "var": "var(--pf-global--palette--black-400)"
  };
  const global_palette_black_500 = exports.global_palette_black_500 = {
    "name": "--pf-global--palette--black-500",
    "value": "#8a8d90",
    "var": "var(--pf-global--palette--black-500)"
  };
  const global_palette_black_600 = exports.global_palette_black_600 = {
    "name": "--pf-global--palette--black-600",
    "value": "#737679",
    "var": "var(--pf-global--palette--black-600)"
  };
  const global_palette_black_700 = exports.global_palette_black_700 = {
    "name": "--pf-global--palette--black-700",
    "value": "#4f5255",
    "var": "var(--pf-global--palette--black-700)"
  };
  const global_palette_black_800 = exports.global_palette_black_800 = {
    "name": "--pf-global--palette--black-800",
    "value": "#3c3f42",
    "var": "var(--pf-global--palette--black-800)"
  };
  const global_palette_black_850 = exports.global_palette_black_850 = {
    "name": "--pf-global--palette--black-850",
    "value": "#212427",
    "var": "var(--pf-global--palette--black-850)"
  };
  const global_palette_black_900 = exports.global_palette_black_900 = {
    "name": "--pf-global--palette--black-900",
    "value": "#151515",
    "var": "var(--pf-global--palette--black-900)"
  };
  const global_palette_black_1000 = exports.global_palette_black_1000 = {
    "name": "--pf-global--palette--black-1000",
    "value": "#030303",
    "var": "var(--pf-global--palette--black-1000)"
  };
  const global_palette_blue_50 = exports.global_palette_blue_50 = {
    "name": "--pf-global--palette--blue-50",
    "value": "#def3ff",
    "var": "var(--pf-global--palette--blue-50)"
  };
  const global_palette_blue_100 = exports.global_palette_blue_100 = {
    "name": "--pf-global--palette--blue-100",
    "value": "#bee1f4",
    "var": "var(--pf-global--palette--blue-100)"
  };
  const global_palette_blue_200 = exports.global_palette_blue_200 = {
    "name": "--pf-global--palette--blue-200",
    "value": "#73bcf7",
    "var": "var(--pf-global--palette--blue-200)"
  };
  const global_palette_blue_300 = exports.global_palette_blue_300 = {
    "name": "--pf-global--palette--blue-300",
    "value": "#2b9af3",
    "var": "var(--pf-global--palette--blue-300)"
  };
  const global_palette_blue_400 = exports.global_palette_blue_400 = {
    "name": "--pf-global--palette--blue-400",
    "value": "#06c",
    "var": "var(--pf-global--palette--blue-400)"
  };
  const global_palette_blue_500 = exports.global_palette_blue_500 = {
    "name": "--pf-global--palette--blue-500",
    "value": "#004080",
    "var": "var(--pf-global--palette--blue-500)"
  };
  const global_palette_blue_600 = exports.global_palette_blue_600 = {
    "name": "--pf-global--palette--blue-600",
    "value": "#004368",
    "var": "var(--pf-global--palette--blue-600)"
  };
  const global_palette_blue_700 = exports.global_palette_blue_700 = {
    "name": "--pf-global--palette--blue-700",
    "value": "#002235",
    "var": "var(--pf-global--palette--blue-700)"
  };
  const global_palette_cyan_100 = exports.global_palette_cyan_100 = {
    "name": "--pf-global--palette--cyan-100",
    "value": "#a2d9d9",
    "var": "var(--pf-global--palette--cyan-100)"
  };
  const global_palette_cyan_200 = exports.global_palette_cyan_200 = {
    "name": "--pf-global--palette--cyan-200",
    "value": "#73c5c5",
    "var": "var(--pf-global--palette--cyan-200)"
  };
  const global_palette_cyan_300 = exports.global_palette_cyan_300 = {
    "name": "--pf-global--palette--cyan-300",
    "value": "#009596",
    "var": "var(--pf-global--palette--cyan-300)"
  };
  const global_palette_cyan_400 = exports.global_palette_cyan_400 = {
    "name": "--pf-global--palette--cyan-400",
    "value": "#005f60",
    "var": "var(--pf-global--palette--cyan-400)"
  };
  const global_palette_cyan_500 = exports.global_palette_cyan_500 = {
    "name": "--pf-global--palette--cyan-500",
    "value": "#003737",
    "var": "var(--pf-global--palette--cyan-500)"
  };
  const global_palette_cyan_600 = exports.global_palette_cyan_600 = {
    "name": "--pf-global--palette--cyan-600",
    "value": "#003d44",
    "var": "var(--pf-global--palette--cyan-600)"
  };
  const global_palette_cyan_700 = exports.global_palette_cyan_700 = {
    "name": "--pf-global--palette--cyan-700",
    "value": "#001f22",
    "var": "var(--pf-global--palette--cyan-700)"
  };
  const global_palette_gold_100 = exports.global_palette_gold_100 = {
    "name": "--pf-global--palette--gold-100",
    "value": "#f9e0a2",
    "var": "var(--pf-global--palette--gold-100)"
  };
  const global_palette_gold_200 = exports.global_palette_gold_200 = {
    "name": "--pf-global--palette--gold-200",
    "value": "#f6d173",
    "var": "var(--pf-global--palette--gold-200)"
  };
  const global_palette_gold_300 = exports.global_palette_gold_300 = {
    "name": "--pf-global--palette--gold-300",
    "value": "#f4c145",
    "var": "var(--pf-global--palette--gold-300)"
  };
  const global_palette_gold_400 = exports.global_palette_gold_400 = {
    "name": "--pf-global--palette--gold-400",
    "value": "#f0ab00",
    "var": "var(--pf-global--palette--gold-400)"
  };
  const global_palette_gold_500 = exports.global_palette_gold_500 = {
    "name": "--pf-global--palette--gold-500",
    "value": "#c58c00",
    "var": "var(--pf-global--palette--gold-500)"
  };
  const global_palette_gold_600 = exports.global_palette_gold_600 = {
    "name": "--pf-global--palette--gold-600",
    "value": "#795600",
    "var": "var(--pf-global--palette--gold-600)"
  };
  const global_palette_gold_700 = exports.global_palette_gold_700 = {
    "name": "--pf-global--palette--gold-700",
    "value": "#3d2c00",
    "var": "var(--pf-global--palette--gold-700)"
  };
  const global_palette_green_100 = exports.global_palette_green_100 = {
    "name": "--pf-global--palette--green-100",
    "value": "#bde5b8",
    "var": "var(--pf-global--palette--green-100)"
  };
  const global_palette_green_200 = exports.global_palette_green_200 = {
    "name": "--pf-global--palette--green-200",
    "value": "#95d58e",
    "var": "var(--pf-global--palette--green-200)"
  };
  const global_palette_green_300 = exports.global_palette_green_300 = {
    "name": "--pf-global--palette--green-300",
    "value": "#6ec664",
    "var": "var(--pf-global--palette--green-300)"
  };
  const global_palette_green_400 = exports.global_palette_green_400 = {
    "name": "--pf-global--palette--green-400",
    "value": "#5ba352",
    "var": "var(--pf-global--palette--green-400)"
  };
  const global_palette_green_500 = exports.global_palette_green_500 = {
    "name": "--pf-global--palette--green-500",
    "value": "#467f40",
    "var": "var(--pf-global--palette--green-500)"
  };
  const global_palette_green_600 = exports.global_palette_green_600 = {
    "name": "--pf-global--palette--green-600",
    "value": "#1e4f18",
    "var": "var(--pf-global--palette--green-600)"
  };
  const global_palette_green_700 = exports.global_palette_green_700 = {
    "name": "--pf-global--palette--green-700",
    "value": "#0f280d",
    "var": "var(--pf-global--palette--green-700)"
  };
  const global_palette_light_blue_100 = exports.global_palette_light_blue_100 = {
    "name": "--pf-global--palette--light-blue-100",
    "value": "#beedf9",
    "var": "var(--pf-global--palette--light-blue-100)"
  };
  const global_palette_light_blue_200 = exports.global_palette_light_blue_200 = {
    "name": "--pf-global--palette--light-blue-200",
    "value": "#7cdbf3",
    "var": "var(--pf-global--palette--light-blue-200)"
  };
  const global_palette_light_blue_300 = exports.global_palette_light_blue_300 = {
    "name": "--pf-global--palette--light-blue-300",
    "value": "#35caed",
    "var": "var(--pf-global--palette--light-blue-300)"
  };
  const global_palette_light_blue_400 = exports.global_palette_light_blue_400 = {
    "name": "--pf-global--palette--light-blue-400",
    "value": "#00b9e4",
    "var": "var(--pf-global--palette--light-blue-400)"
  };
  const global_palette_light_blue_500 = exports.global_palette_light_blue_500 = {
    "name": "--pf-global--palette--light-blue-500",
    "value": "#008bad",
    "var": "var(--pf-global--palette--light-blue-500)"
  };
  const global_palette_light_blue_600 = exports.global_palette_light_blue_600 = {
    "name": "--pf-global--palette--light-blue-600",
    "value": "#005c73",
    "var": "var(--pf-global--palette--light-blue-600)"
  };
  const global_palette_light_blue_700 = exports.global_palette_light_blue_700 = {
    "name": "--pf-global--palette--light-blue-700",
    "value": "#002d39",
    "var": "var(--pf-global--palette--light-blue-700)"
  };
  const global_palette_light_green_100 = exports.global_palette_light_green_100 = {
    "name": "--pf-global--palette--light-green-100",
    "value": "#e4f5bc",
    "var": "var(--pf-global--palette--light-green-100)"
  };
  const global_palette_light_green_200 = exports.global_palette_light_green_200 = {
    "name": "--pf-global--palette--light-green-200",
    "value": "#c8eb79",
    "var": "var(--pf-global--palette--light-green-200)"
  };
  const global_palette_light_green_300 = exports.global_palette_light_green_300 = {
    "name": "--pf-global--palette--light-green-300",
    "value": "#ace12e",
    "var": "var(--pf-global--palette--light-green-300)"
  };
  const global_palette_light_green_400 = exports.global_palette_light_green_400 = {
    "name": "--pf-global--palette--light-green-400",
    "value": "#92d400",
    "var": "var(--pf-global--palette--light-green-400)"
  };
  const global_palette_light_green_500 = exports.global_palette_light_green_500 = {
    "name": "--pf-global--palette--light-green-500",
    "value": "#6ca100",
    "var": "var(--pf-global--palette--light-green-500)"
  };
  const global_palette_light_green_600 = exports.global_palette_light_green_600 = {
    "name": "--pf-global--palette--light-green-600",
    "value": "#486b00",
    "var": "var(--pf-global--palette--light-green-600)"
  };
  const global_palette_light_green_700 = exports.global_palette_light_green_700 = {
    "name": "--pf-global--palette--light-green-700",
    "value": "#253600",
    "var": "var(--pf-global--palette--light-green-700)"
  };
  const global_palette_orange_100 = exports.global_palette_orange_100 = {
    "name": "--pf-global--palette--orange-100",
    "value": "#f4b678",
    "var": "var(--pf-global--palette--orange-100)"
  };
  const global_palette_orange_200 = exports.global_palette_orange_200 = {
    "name": "--pf-global--palette--orange-200",
    "value": "#ef9234",
    "var": "var(--pf-global--palette--orange-200)"
  };
  const global_palette_orange_300 = exports.global_palette_orange_300 = {
    "name": "--pf-global--palette--orange-300",
    "value": "#ec7a08",
    "var": "var(--pf-global--palette--orange-300)"
  };
  const global_palette_orange_400 = exports.global_palette_orange_400 = {
    "name": "--pf-global--palette--orange-400",
    "value": "#c46100",
    "var": "var(--pf-global--palette--orange-400)"
  };
  const global_palette_orange_500 = exports.global_palette_orange_500 = {
    "name": "--pf-global--palette--orange-500",
    "value": "#8f4700",
    "var": "var(--pf-global--palette--orange-500)"
  };
  const global_palette_orange_600 = exports.global_palette_orange_600 = {
    "name": "--pf-global--palette--orange-600",
    "value": "#773d00",
    "var": "var(--pf-global--palette--orange-600)"
  };
  const global_palette_orange_700 = exports.global_palette_orange_700 = {
    "name": "--pf-global--palette--orange-700",
    "value": "#3b1f00",
    "var": "var(--pf-global--palette--orange-700)"
  };
  const global_palette_purple_100 = exports.global_palette_purple_100 = {
    "name": "--pf-global--palette--purple-100",
    "value": "#cbc1ff",
    "var": "var(--pf-global--palette--purple-100)"
  };
  const global_palette_purple_200 = exports.global_palette_purple_200 = {
    "name": "--pf-global--palette--purple-200",
    "value": "#b2a3ff",
    "var": "var(--pf-global--palette--purple-200)"
  };
  const global_palette_purple_300 = exports.global_palette_purple_300 = {
    "name": "--pf-global--palette--purple-300",
    "value": "#a18fff",
    "var": "var(--pf-global--palette--purple-300)"
  };
  const global_palette_purple_400 = exports.global_palette_purple_400 = {
    "name": "--pf-global--palette--purple-400",
    "value": "#8476d1",
    "var": "var(--pf-global--palette--purple-400)"
  };
  const global_palette_purple_500 = exports.global_palette_purple_500 = {
    "name": "--pf-global--palette--purple-500",
    "value": "#6753ac",
    "var": "var(--pf-global--palette--purple-500)"
  };
  const global_palette_purple_600 = exports.global_palette_purple_600 = {
    "name": "--pf-global--palette--purple-600",
    "value": "#40199a",
    "var": "var(--pf-global--palette--purple-600)"
  };
  const global_palette_purple_700 = exports.global_palette_purple_700 = {
    "name": "--pf-global--palette--purple-700",
    "value": "#1f0066",
    "var": "var(--pf-global--palette--purple-700)"
  };
  const global_palette_red_100 = exports.global_palette_red_100 = {
    "name": "--pf-global--palette--red-100",
    "value": "#c9190b",
    "var": "var(--pf-global--palette--red-100)"
  };
  const global_palette_red_200 = exports.global_palette_red_200 = {
    "name": "--pf-global--palette--red-200",
    "value": "#a30000",
    "var": "var(--pf-global--palette--red-200)"
  };
  const global_palette_red_300 = exports.global_palette_red_300 = {
    "name": "--pf-global--palette--red-300",
    "value": "#7d1007",
    "var": "var(--pf-global--palette--red-300)"
  };
  const global_palette_red_400 = exports.global_palette_red_400 = {
    "name": "--pf-global--palette--red-400",
    "value": "#470000",
    "var": "var(--pf-global--palette--red-400)"
  };
  const global_palette_red_500 = exports.global_palette_red_500 = {
    "name": "--pf-global--palette--red-500",
    "value": "#2c0000",
    "var": "var(--pf-global--palette--red-500)"
  };
  const global_palette_white = exports.global_palette_white = {
    "name": "--pf-global--palette--white",
    "value": "#fff",
    "var": "var(--pf-global--palette--white)"
  };
  const global_BackgroundColor_150 = exports.global_BackgroundColor_150 = {
    "name": "--pf-global--BackgroundColor--150",
    "value": "#f5f5f5",
    "var": "var(--pf-global--BackgroundColor--150)"
  };
  const global_BackgroundColor_200 = exports.global_BackgroundColor_200 = {
    "name": "--pf-global--BackgroundColor--200",
    "value": "#fafafa",
    "var": "var(--pf-global--BackgroundColor--200)"
  };
  const global_BackgroundColor_300 = exports.global_BackgroundColor_300 = {
    "name": "--pf-global--BackgroundColor--300",
    "value": "#ededed",
    "var": "var(--pf-global--BackgroundColor--300)"
  };
  const global_BackgroundColor_light_100 = exports.global_BackgroundColor_light_100 = {
    "name": "--pf-global--BackgroundColor--light-100",
    "value": "#fff",
    "var": "var(--pf-global--BackgroundColor--light-100)"
  };
  const global_BackgroundColor_light_200 = exports.global_BackgroundColor_light_200 = {
    "name": "--pf-global--BackgroundColor--light-200",
    "value": "#fafafa",
    "var": "var(--pf-global--BackgroundColor--light-200)"
  };
  const global_BackgroundColor_light_300 = exports.global_BackgroundColor_light_300 = {
    "name": "--pf-global--BackgroundColor--light-300",
    "value": "#ededed",
    "var": "var(--pf-global--BackgroundColor--light-300)"
  };
  const global_BackgroundColor_dark_100 = exports.global_BackgroundColor_dark_100 = {
    "name": "--pf-global--BackgroundColor--dark-100",
    "value": "#151515",
    "var": "var(--pf-global--BackgroundColor--dark-100)"
  };
  const global_BackgroundColor_dark_200 = exports.global_BackgroundColor_dark_200 = {
    "name": "--pf-global--BackgroundColor--dark-200",
    "value": "#3c3f42",
    "var": "var(--pf-global--BackgroundColor--dark-200)"
  };
  const global_BackgroundColor_dark_300 = exports.global_BackgroundColor_dark_300 = {
    "name": "--pf-global--BackgroundColor--dark-300",
    "value": "#212427",
    "var": "var(--pf-global--BackgroundColor--dark-300)"
  };
  const global_BackgroundColor_dark_400 = exports.global_BackgroundColor_dark_400 = {
    "name": "--pf-global--BackgroundColor--dark-400",
    "value": "#4f5255",
    "var": "var(--pf-global--BackgroundColor--dark-400)"
  };
  const global_BackgroundColor_dark_transparent_100 = exports.global_BackgroundColor_dark_transparent_100 = {
    "name": "--pf-global--BackgroundColor--dark-transparent-100",
    "value": "rgba(3,3,3,0.62)",
    "var": "var(--pf-global--BackgroundColor--dark-transparent-100)"
  };
  const global_BackgroundColor_dark_transparent_200 = exports.global_BackgroundColor_dark_transparent_200 = {
    "name": "--pf-global--BackgroundColor--dark-transparent-200",
    "value": "rgba(3,3,3,0.32)",
    "var": "var(--pf-global--BackgroundColor--dark-transparent-200)"
  };
  const global_Color_300 = exports.global_Color_300 = {
    "name": "--pf-global--Color--300",
    "value": "#3c3f42",
    "var": "var(--pf-global--Color--300)"
  };
  const global_Color_400 = exports.global_Color_400 = {
    "name": "--pf-global--Color--400",
    "value": "#8a8d90",
    "var": "var(--pf-global--Color--400)"
  };
  const global_Color_light_100 = exports.global_Color_light_100 = {
    "name": "--pf-global--Color--light-100",
    "value": "#fff",
    "var": "var(--pf-global--Color--light-100)"
  };
  const global_Color_light_200 = exports.global_Color_light_200 = {
    "name": "--pf-global--Color--light-200",
    "value": "#ededed",
    "var": "var(--pf-global--Color--light-200)"
  };
  const global_Color_light_300 = exports.global_Color_light_300 = {
    "name": "--pf-global--Color--light-300",
    "value": "#d2d2d2",
    "var": "var(--pf-global--Color--light-300)"
  };
  const global_Color_dark_100 = exports.global_Color_dark_100 = {
    "name": "--pf-global--Color--dark-100",
    "value": "#151515",
    "var": "var(--pf-global--Color--dark-100)"
  };
  const global_Color_dark_200 = exports.global_Color_dark_200 = {
    "name": "--pf-global--Color--dark-200",
    "value": "#737679",
    "var": "var(--pf-global--Color--dark-200)"
  };
  const global_active_color_100 = exports.global_active_color_100 = {
    "name": "--pf-global--active-color--100",
    "value": "#06c",
    "var": "var(--pf-global--active-color--100)"
  };
  const global_active_color_200 = exports.global_active_color_200 = {
    "name": "--pf-global--active-color--200",
    "value": "#bee1f4",
    "var": "var(--pf-global--active-color--200)"
  };
  const global_active_color_300 = exports.global_active_color_300 = {
    "name": "--pf-global--active-color--300",
    "value": "#73bcf7",
    "var": "var(--pf-global--active-color--300)"
  };
  const global_active_color_400 = exports.global_active_color_400 = {
    "name": "--pf-global--active-color--400",
    "value": "#2b9af3",
    "var": "var(--pf-global--active-color--400)"
  };
  const global_disabled_color_100 = exports.global_disabled_color_100 = {
    "name": "--pf-global--disabled-color--100",
    "value": "#737679",
    "var": "var(--pf-global--disabled-color--100)"
  };
  const global_disabled_color_200 = exports.global_disabled_color_200 = {
    "name": "--pf-global--disabled-color--200",
    "value": "#d2d2d2",
    "var": "var(--pf-global--disabled-color--200)"
  };
  const global_disabled_color_300 = exports.global_disabled_color_300 = {
    "name": "--pf-global--disabled-color--300",
    "value": "#ededed",
    "var": "var(--pf-global--disabled-color--300)"
  };
  const global_primary_color_200 = exports.global_primary_color_200 = {
    "name": "--pf-global--primary-color--200",
    "value": "#004080",
    "var": "var(--pf-global--primary-color--200)"
  };
  const global_primary_color_light_100 = exports.global_primary_color_light_100 = {
    "name": "--pf-global--primary-color--light-100",
    "value": "#73bcf7",
    "var": "var(--pf-global--primary-color--light-100)"
  };
  const global_primary_color_dark_100 = exports.global_primary_color_dark_100 = {
    "name": "--pf-global--primary-color--dark-100",
    "value": "#06c",
    "var": "var(--pf-global--primary-color--dark-100)"
  };
  const global_secondary_color_100 = exports.global_secondary_color_100 = {
    "name": "--pf-global--secondary-color--100",
    "value": "#737679",
    "var": "var(--pf-global--secondary-color--100)"
  };
  const global_default_color_100 = exports.global_default_color_100 = {
    "name": "--pf-global--default-color--100",
    "value": "#73c5c5",
    "var": "var(--pf-global--default-color--100)"
  };
  const global_default_color_200 = exports.global_default_color_200 = {
    "name": "--pf-global--default-color--200",
    "value": "#009596",
    "var": "var(--pf-global--default-color--200)"
  };
  const global_default_color_300 = exports.global_default_color_300 = {
    "name": "--pf-global--default-color--300",
    "value": "#003737",
    "var": "var(--pf-global--default-color--300)"
  };
  const global_success_color_100 = exports.global_success_color_100 = {
    "name": "--pf-global--success-color--100",
    "value": "#92d400",
    "var": "var(--pf-global--success-color--100)"
  };
  const global_success_color_200 = exports.global_success_color_200 = {
    "name": "--pf-global--success-color--200",
    "value": "#486b00",
    "var": "var(--pf-global--success-color--200)"
  };
  const global_info_color_100 = exports.global_info_color_100 = {
    "name": "--pf-global--info-color--100",
    "value": "#73bcf7",
    "var": "var(--pf-global--info-color--100)"
  };
  const global_info_color_200 = exports.global_info_color_200 = {
    "name": "--pf-global--info-color--200",
    "value": "#004368",
    "var": "var(--pf-global--info-color--200)"
  };
  const global_warning_color_100 = exports.global_warning_color_100 = {
    "name": "--pf-global--warning-color--100",
    "value": "#f0ab00",
    "var": "var(--pf-global--warning-color--100)"
  };
  const global_warning_color_200 = exports.global_warning_color_200 = {
    "name": "--pf-global--warning-color--200",
    "value": "#795600",
    "var": "var(--pf-global--warning-color--200)"
  };
  const global_danger_color_100 = exports.global_danger_color_100 = {
    "name": "--pf-global--danger-color--100",
    "value": "#c9190b",
    "var": "var(--pf-global--danger-color--100)"
  };
  const global_danger_color_200 = exports.global_danger_color_200 = {
    "name": "--pf-global--danger-color--200",
    "value": "#a30000",
    "var": "var(--pf-global--danger-color--200)"
  };
  const global_danger_color_300 = exports.global_danger_color_300 = {
    "name": "--pf-global--danger-color--300",
    "value": "#470000",
    "var": "var(--pf-global--danger-color--300)"
  };
  const global_BoxShadow_sm = exports.global_BoxShadow_sm = {
    "name": "--pf-global--BoxShadow--sm",
    "value": "0 0.0625rem 0.125rem 0 rgba(3,3,3,0.2)",
    "var": "var(--pf-global--BoxShadow--sm)"
  };
  const global_BoxShadow_sm_right = exports.global_BoxShadow_sm_right = {
    "name": "--pf-global--BoxShadow--sm-right",
    "value": "0.25rem 0 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-global--BoxShadow--sm-right)"
  };
  const global_BoxShadow_sm_left = exports.global_BoxShadow_sm_left = {
    "name": "--pf-global--BoxShadow--sm-left",
    "value": "-0.25rem 0 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-global--BoxShadow--sm-left)"
  };
  const global_BoxShadow_sm_bottom = exports.global_BoxShadow_sm_bottom = {
    "name": "--pf-global--BoxShadow--sm-bottom",
    "value": "0 0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-global--BoxShadow--sm-bottom)"
  };
  const global_BoxShadow_sm_top = exports.global_BoxShadow_sm_top = {
    "name": "--pf-global--BoxShadow--sm-top",
    "value": "0 -0.25rem 0.625rem -0.25rem rgba(3,3,3,0.12)",
    "var": "var(--pf-global--BoxShadow--sm-top)"
  };
  const global_BoxShadow_md = exports.global_BoxShadow_md = {
    "name": "--pf-global--BoxShadow--md",
    "value": "0 0.0625rem 0.0625rem 0rem rgba(3,3,3,0.05),0 0.25rem 0.5rem 0.25rem rgba(3,3,3,0.06)",
    "var": "var(--pf-global--BoxShadow--md)"
  };
  const global_BoxShadow_md_right = exports.global_BoxShadow_md_right = {
    "name": "--pf-global--BoxShadow--md-right",
    "value": "0.3125rem 0 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-global--BoxShadow--md-right)"
  };
  const global_BoxShadow_md_left = exports.global_BoxShadow_md_left = {
    "name": "--pf-global--BoxShadow--md-left",
    "value": "-0.3125rem 0 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-global--BoxShadow--md-left)"
  };
  const global_BoxShadow_md_bottom = exports.global_BoxShadow_md_bottom = {
    "name": "--pf-global--BoxShadow--md-bottom",
    "value": "0 0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-global--BoxShadow--md-bottom)"
  };
  const global_BoxShadow_md_top = exports.global_BoxShadow_md_top = {
    "name": "--pf-global--BoxShadow--md-top",
    "value": "0 -0.3125rem 0.625rem -0.25rem rgba(3,3,3,0.25)",
    "var": "var(--pf-global--BoxShadow--md-top)"
  };
  const global_BoxShadow_lg = exports.global_BoxShadow_lg = {
    "name": "--pf-global--BoxShadow--lg",
    "value": "0 0.1875rem 0.4375rem 0.1875rem rgba(3,3,3,0.13),0 0.6875rem 1.5rem 1rem rgba(3,3,3,0.12)",
    "var": "var(--pf-global--BoxShadow--lg)"
  };
  const global_BoxShadow_lg_right = exports.global_BoxShadow_lg_right = {
    "name": "--pf-global--BoxShadow--lg-right",
    "value": "0.75rem 0 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-global--BoxShadow--lg-right)"
  };
  const global_BoxShadow_lg_left = exports.global_BoxShadow_lg_left = {
    "name": "--pf-global--BoxShadow--lg-left",
    "value": "-0.75rem 0 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-global--BoxShadow--lg-left)"
  };
  const global_BoxShadow_lg_bottom = exports.global_BoxShadow_lg_bottom = {
    "name": "--pf-global--BoxShadow--lg-bottom",
    "value": "0 0.75rem 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-global--BoxShadow--lg-bottom)"
  };
  const global_BoxShadow_lg_top = exports.global_BoxShadow_lg_top = {
    "name": "--pf-global--BoxShadow--lg-top",
    "value": "0 -0.75rem 0.625rem -0.25rem rgba(3,3,3,0.07)",
    "var": "var(--pf-global--BoxShadow--lg-top)"
  };
  const global_BoxShadow_inset = exports.global_BoxShadow_inset = {
    "name": "--pf-global--BoxShadow--inset",
    "value": "inset 0 0 0.625rem 0 rgba(3,3,3,0.25)",
    "var": "var(--pf-global--BoxShadow--inset)"
  };
  const global_font_path = exports.global_font_path = {
    "name": "--pf-global--font-path",
    "value": "./assets/fonts",
    "var": "var(--pf-global--font-path)"
  };
  const global_fonticon_path = exports.global_fonticon_path = {
    "name": "--pf-global--fonticon-path",
    "value": "./assets/pficon",
    "var": "var(--pf-global--fonticon-path)"
  };
  const global_spacer_xs = exports.global_spacer_xs = {
    "name": "--pf-global--spacer--xs",
    "value": "0.25rem",
    "var": "var(--pf-global--spacer--xs)"
  };
  const global_spacer_sm = exports.global_spacer_sm = {
    "name": "--pf-global--spacer--sm",
    "value": "0.5rem",
    "var": "var(--pf-global--spacer--sm)"
  };
  const global_spacer_md = exports.global_spacer_md = {
    "name": "--pf-global--spacer--md",
    "value": "1rem",
    "var": "var(--pf-global--spacer--md)"
  };
  const global_spacer_lg = exports.global_spacer_lg = {
    "name": "--pf-global--spacer--lg",
    "value": "1.5rem",
    "var": "var(--pf-global--spacer--lg)"
  };
  const global_spacer_xl = exports.global_spacer_xl = {
    "name": "--pf-global--spacer--xl",
    "value": "2rem",
    "var": "var(--pf-global--spacer--xl)"
  };
  const global_spacer_2xl = exports.global_spacer_2xl = {
    "name": "--pf-global--spacer--2xl",
    "value": "3rem",
    "var": "var(--pf-global--spacer--2xl)"
  };
  const global_spacer_3xl = exports.global_spacer_3xl = {
    "name": "--pf-global--spacer--3xl",
    "value": "4rem",
    "var": "var(--pf-global--spacer--3xl)"
  };
  const global_spacer_form_element = exports.global_spacer_form_element = {
    "name": "--pf-global--spacer--form-element",
    "value": "0.375rem",
    "var": "var(--pf-global--spacer--form-element)"
  };
  const global_gutter = exports.global_gutter = {
    "name": "--pf-global--gutter",
    "value": "1.5rem",
    "var": "var(--pf-global--gutter)"
  };
  const global_gutter_md = exports.global_gutter_md = {
    "name": "--pf-global--gutter--md",
    "value": "1rem",
    "var": "var(--pf-global--gutter--md)"
  };
  const global_golden_ratio = exports.global_golden_ratio = {
    "name": "--pf-global--golden-ratio",
    "value": "1.681",
    "var": "var(--pf-global--golden-ratio)"
  };
  const global_ZIndex_xs = exports.global_ZIndex_xs = {
    "name": "--pf-global--ZIndex--xs",
    "value": "100",
    "var": "var(--pf-global--ZIndex--xs)"
  };
  const global_ZIndex_sm = exports.global_ZIndex_sm = {
    "name": "--pf-global--ZIndex--sm",
    "value": "200",
    "var": "var(--pf-global--ZIndex--sm)"
  };
  const global_ZIndex_md = exports.global_ZIndex_md = {
    "name": "--pf-global--ZIndex--md",
    "value": "300",
    "var": "var(--pf-global--ZIndex--md)"
  };
  const global_ZIndex_lg = exports.global_ZIndex_lg = {
    "name": "--pf-global--ZIndex--lg",
    "value": "400",
    "var": "var(--pf-global--ZIndex--lg)"
  };
  const global_ZIndex_xl = exports.global_ZIndex_xl = {
    "name": "--pf-global--ZIndex--xl",
    "value": "500",
    "var": "var(--pf-global--ZIndex--xl)"
  };
  const global_ZIndex_2xl = exports.global_ZIndex_2xl = {
    "name": "--pf-global--ZIndex--2xl",
    "value": "600",
    "var": "var(--pf-global--ZIndex--2xl)"
  };
  const global_breakpoint_xs = exports.global_breakpoint_xs = {
    "name": "--pf-global--breakpoint--xs",
    "value": "0",
    "var": "var(--pf-global--breakpoint--xs)"
  };
  const global_breakpoint_sm = exports.global_breakpoint_sm = {
    "name": "--pf-global--breakpoint--sm",
    "value": "576px",
    "var": "var(--pf-global--breakpoint--sm)"
  };
  const global_breakpoint_md = exports.global_breakpoint_md = {
    "name": "--pf-global--breakpoint--md",
    "value": "768px",
    "var": "var(--pf-global--breakpoint--md)"
  };
  const global_breakpoint_lg = exports.global_breakpoint_lg = {
    "name": "--pf-global--breakpoint--lg",
    "value": "992px",
    "var": "var(--pf-global--breakpoint--lg)"
  };
  const global_breakpoint_xl = exports.global_breakpoint_xl = {
    "name": "--pf-global--breakpoint--xl",
    "value": "1200px",
    "var": "var(--pf-global--breakpoint--xl)"
  };
  const global_breakpoint_2xl = exports.global_breakpoint_2xl = {
    "name": "--pf-global--breakpoint--2xl",
    "value": "1450px",
    "var": "var(--pf-global--breakpoint--2xl)"
  };
  const global_link_Color_light = exports.global_link_Color_light = {
    "name": "--pf-global--link--Color--light",
    "value": "#73bcf7",
    "var": "var(--pf-global--link--Color--light)"
  };
  const global_link_Color_light_hover = exports.global_link_Color_light_hover = {
    "name": "--pf-global--link--Color--light--hover",
    "value": "#2b9af3",
    "var": "var(--pf-global--link--Color--light--hover)"
  };
  const global_link_Color_dark = exports.global_link_Color_dark = {
    "name": "--pf-global--link--Color--dark",
    "value": "#06c",
    "var": "var(--pf-global--link--Color--dark)"
  };
  const global_link_Color_dark_hover = exports.global_link_Color_dark_hover = {
    "name": "--pf-global--link--Color--dark--hover",
    "value": "#004080",
    "var": "var(--pf-global--link--Color--dark--hover)"
  };
  const global_link_FontWeight = exports.global_link_FontWeight = {
    "name": "--pf-global--link--FontWeight",
    "value": "400",
    "var": "var(--pf-global--link--FontWeight)"
  };
  const global_link_TextDecoration = exports.global_link_TextDecoration = {
    "name": "--pf-global--link--TextDecoration",
    "value": "underline",
    "var": "var(--pf-global--link--TextDecoration)"
  };
  const global_link_TextDecoration_hover = exports.global_link_TextDecoration_hover = {
    "name": "--pf-global--link--TextDecoration--hover",
    "value": "underline",
    "var": "var(--pf-global--link--TextDecoration--hover)"
  };
  const global_BorderWidth_sm = exports.global_BorderWidth_sm = {
    "name": "--pf-global--BorderWidth--sm",
    "value": "1px",
    "var": "var(--pf-global--BorderWidth--sm)"
  };
  const global_BorderWidth_md = exports.global_BorderWidth_md = {
    "name": "--pf-global--BorderWidth--md",
    "value": "2px",
    "var": "var(--pf-global--BorderWidth--md)"
  };
  const global_BorderWidth_lg = exports.global_BorderWidth_lg = {
    "name": "--pf-global--BorderWidth--lg",
    "value": "3px",
    "var": "var(--pf-global--BorderWidth--lg)"
  };
  const global_BorderColor_200 = exports.global_BorderColor_200 = {
    "name": "--pf-global--BorderColor--200",
    "value": "#8a8d90",
    "var": "var(--pf-global--BorderColor--200)"
  };
  const global_BorderColor_300 = exports.global_BorderColor_300 = {
    "name": "--pf-global--BorderColor--300",
    "value": "#ededed",
    "var": "var(--pf-global--BorderColor--300)"
  };
  const global_BorderColor_dark_100 = exports.global_BorderColor_dark_100 = {
    "name": "--pf-global--BorderColor--dark-100",
    "value": "#d2d2d2",
    "var": "var(--pf-global--BorderColor--dark-100)"
  };
  const global_BorderColor_light_100 = exports.global_BorderColor_light_100 = {
    "name": "--pf-global--BorderColor--light-100",
    "value": "#b8bbbe",
    "var": "var(--pf-global--BorderColor--light-100)"
  };
  const global_BorderRadius_sm = exports.global_BorderRadius_sm = {
    "name": "--pf-global--BorderRadius--sm",
    "value": "3px",
    "var": "var(--pf-global--BorderRadius--sm)"
  };
  const global_BorderRadius_lg = exports.global_BorderRadius_lg = {
    "name": "--pf-global--BorderRadius--lg",
    "value": "30em",
    "var": "var(--pf-global--BorderRadius--lg)"
  };
  const global_icon_Color_light = exports.global_icon_Color_light = {
    "name": "--pf-global--icon--Color--light",
    "value": "#737679",
    "var": "var(--pf-global--icon--Color--light)"
  };
  const global_icon_Color_dark = exports.global_icon_Color_dark = {
    "name": "--pf-global--icon--Color--dark",
    "value": "#151515",
    "var": "var(--pf-global--icon--Color--dark)"
  };
  const global_icon_FontSize_sm = exports.global_icon_FontSize_sm = {
    "name": "--pf-global--icon--FontSize--sm",
    "value": "0.625rem",
    "var": "var(--pf-global--icon--FontSize--sm)"
  };
  const global_icon_FontSize_md = exports.global_icon_FontSize_md = {
    "name": "--pf-global--icon--FontSize--md",
    "value": "1.125rem",
    "var": "var(--pf-global--icon--FontSize--md)"
  };
  const global_icon_FontSize_lg = exports.global_icon_FontSize_lg = {
    "name": "--pf-global--icon--FontSize--lg",
    "value": "1.5rem",
    "var": "var(--pf-global--icon--FontSize--lg)"
  };
  const global_icon_FontSize_xl = exports.global_icon_FontSize_xl = {
    "name": "--pf-global--icon--FontSize--xl",
    "value": "3.375rem",
    "var": "var(--pf-global--icon--FontSize--xl)"
  };
  const global_FontFamily_sans_serif = exports.global_FontFamily_sans_serif = {
    "name": "--pf-global--FontFamily--sans-serif",
    "value": "RedHatText,Overpass,overpass,helvetica,arial,sans-serif",
    "var": "var(--pf-global--FontFamily--sans-serif)"
  };
  const global_FontFamily_heading_sans_serif = exports.global_FontFamily_heading_sans_serif = {
    "name": "--pf-global--FontFamily--heading--sans-serif",
    "value": "RedHatDisplay,Overpass,overpass,helvetica,arial,sans-serif",
    "var": "var(--pf-global--FontFamily--heading--sans-serif)"
  };
  const global_FontFamily_monospace = exports.global_FontFamily_monospace = {
    "name": "--pf-global--FontFamily--monospace",
    "value": "Liberation Mono,consolas,SFMono-Regular,menlo,monaco,Courier New,monospace",
    "var": "var(--pf-global--FontFamily--monospace)"
  };
  const global_FontFamily_redhatfont_sans_serif = exports.global_FontFamily_redhatfont_sans_serif = {
    "name": "--pf-global--FontFamily--redhatfont--sans-serif",
    "value": "RedHatText,Overpass,overpass,helvetica,arial,sans-serif",
    "var": "var(--pf-global--FontFamily--redhatfont--sans-serif)"
  };
  const global_FontFamily_redhatfont_heading_sans_serif = exports.global_FontFamily_redhatfont_heading_sans_serif = {
    "name": "--pf-global--FontFamily--redhatfont--heading--sans-serif",
    "value": "RedHatDisplay,Overpass,overpass,helvetica,arial,sans-serif",
    "var": "var(--pf-global--FontFamily--redhatfont--heading--sans-serif)"
  };
  const global_FontFamily_redhatfont_monospace = exports.global_FontFamily_redhatfont_monospace = {
    "name": "--pf-global--FontFamily--redhatfont--monospace",
    "value": "Liberation Mono,consolas,SFMono-Regular,menlo,monaco,Courier New,monospace",
    "var": "var(--pf-global--FontFamily--redhatfont--monospace)"
  };
  const global_FontSize_4xl = exports.global_FontSize_4xl = {
    "name": "--pf-global--FontSize--4xl",
    "value": "2.25rem",
    "var": "var(--pf-global--FontSize--4xl)"
  };
  const global_FontSize_3xl = exports.global_FontSize_3xl = {
    "name": "--pf-global--FontSize--3xl",
    "value": "1.75rem",
    "var": "var(--pf-global--FontSize--3xl)"
  };
  const global_FontSize_2xl = exports.global_FontSize_2xl = {
    "name": "--pf-global--FontSize--2xl",
    "value": "1.5rem",
    "var": "var(--pf-global--FontSize--2xl)"
  };
  const global_FontSize_xl = exports.global_FontSize_xl = {
    "name": "--pf-global--FontSize--xl",
    "value": "1.25rem",
    "var": "var(--pf-global--FontSize--xl)"
  };
  const global_FontSize_lg = exports.global_FontSize_lg = {
    "name": "--pf-global--FontSize--lg",
    "value": "1.125rem",
    "var": "var(--pf-global--FontSize--lg)"
  };
  const global_FontSize_md = exports.global_FontSize_md = {
    "name": "--pf-global--FontSize--md",
    "value": "1rem",
    "var": "var(--pf-global--FontSize--md)"
  };
  const global_FontSize_sm = exports.global_FontSize_sm = {
    "name": "--pf-global--FontSize--sm",
    "value": "0.875rem",
    "var": "var(--pf-global--FontSize--sm)"
  };
  const global_FontSize_xs = exports.global_FontSize_xs = {
    "name": "--pf-global--FontSize--xs",
    "value": "0.75rem",
    "var": "var(--pf-global--FontSize--xs)"
  };
  const global_FontWeight_light = exports.global_FontWeight_light = {
    "name": "--pf-global--FontWeight--light",
    "value": "300",
    "var": "var(--pf-global--FontWeight--light)"
  };
  const global_FontWeight_normal = exports.global_FontWeight_normal = {
    "name": "--pf-global--FontWeight--normal",
    "value": "400",
    "var": "var(--pf-global--FontWeight--normal)"
  };
  const global_FontWeight_semi_bold = exports.global_FontWeight_semi_bold = {
    "name": "--pf-global--FontWeight--semi-bold",
    "value": "700",
    "var": "var(--pf-global--FontWeight--semi-bold)"
  };
  const global_FontWeight_bold = exports.global_FontWeight_bold = {
    "name": "--pf-global--FontWeight--bold",
    "value": "700",
    "var": "var(--pf-global--FontWeight--bold)"
  };
  const global_FontWeight_redhatfont_bold = exports.global_FontWeight_redhatfont_bold = {
    "name": "--pf-global--FontWeight--redhatfont--bold",
    "value": "700",
    "var": "var(--pf-global--FontWeight--redhatfont--bold)"
  };
  const global_LineHeight_sm = exports.global_LineHeight_sm = {
    "name": "--pf-global--LineHeight--sm",
    "value": "1.3",
    "var": "var(--pf-global--LineHeight--sm)"
  };
  const global_LineHeight_md = exports.global_LineHeight_md = {
    "name": "--pf-global--LineHeight--md",
    "value": "1.5",
    "var": "var(--pf-global--LineHeight--md)"
  };
  const global_ListStyle = exports.global_ListStyle = {
    "name": "--pf-global--ListStyle",
    "value": "disc outside",
    "var": "var(--pf-global--ListStyle)"
  };
  const global_Transition = exports.global_Transition = {
    "name": "--pf-global--Transition",
    "value": "all 250ms ease-in-out",
    "var": "var(--pf-global--Transition)"
  };
  const global_TimingFunction = exports.global_TimingFunction = {
    "name": "--pf-global--TimingFunction",
    "value": "cubic-bezier(0.645,0.045,0.355,1)",
    "var": "var(--pf-global--TimingFunction)"
  };
  const global_TransitionDuration = exports.global_TransitionDuration = {
    "name": "--pf-global--TransitionDuration",
    "value": "250ms",
    "var": "var(--pf-global--TransitionDuration)"
  };
  const global_arrow_width = exports.global_arrow_width = {
    "name": "--pf-global--arrow--width",
    "value": "0.9375rem",
    "var": "var(--pf-global--arrow--width)"
  };
  const global_arrow_width_lg = exports.global_arrow_width_lg = {
    "name": "--pf-global--arrow--width-lg",
    "value": "1.5625rem",
    "var": "var(--pf-global--arrow--width-lg)"
  };
  const global_target_size_MinWidth = exports.global_target_size_MinWidth = {
    "name": "--pf-global--target-size--MinWidth",
    "value": "44px",
    "var": "var(--pf-global--target-size--MinWidth)"
  };
  const global_target_size_MinHeight = exports.global_target_size_MinHeight = {
    "name": "--pf-global--target-size--MinHeight",
    "value": "44px",
    "var": "var(--pf-global--target-size--MinHeight)"
  };
  const chart_color_blue_100 = exports.chart_color_blue_100 = {
    "name": "--pf-chart-color-blue-100",
    "value": "#8bc1f7",
    "var": "var(--pf-chart-color-blue-100)"
  };
  const chart_color_blue_200 = exports.chart_color_blue_200 = {
    "name": "--pf-chart-color-blue-200",
    "value": "#519de9",
    "var": "var(--pf-chart-color-blue-200)"
  };
  const chart_color_blue_300 = exports.chart_color_blue_300 = {
    "name": "--pf-chart-color-blue-300",
    "value": "#06c",
    "var": "var(--pf-chart-color-blue-300)"
  };
  const chart_color_blue_400 = exports.chart_color_blue_400 = {
    "name": "--pf-chart-color-blue-400",
    "value": "#004b95",
    "var": "var(--pf-chart-color-blue-400)"
  };
  const chart_color_blue_500 = exports.chart_color_blue_500 = {
    "name": "--pf-chart-color-blue-500",
    "value": "#002f5d",
    "var": "var(--pf-chart-color-blue-500)"
  };
  const chart_color_green_100 = exports.chart_color_green_100 = {
    "name": "--pf-chart-color-green-100",
    "value": "#bde2b9",
    "var": "var(--pf-chart-color-green-100)"
  };
  const chart_color_green_200 = exports.chart_color_green_200 = {
    "name": "--pf-chart-color-green-200",
    "value": "#7cc674",
    "var": "var(--pf-chart-color-green-200)"
  };
  const chart_color_green_300 = exports.chart_color_green_300 = {
    "name": "--pf-chart-color-green-300",
    "value": "#4cb140",
    "var": "var(--pf-chart-color-green-300)"
  };
  const chart_color_green_400 = exports.chart_color_green_400 = {
    "name": "--pf-chart-color-green-400",
    "value": "#38812f",
    "var": "var(--pf-chart-color-green-400)"
  };
  const chart_color_green_500 = exports.chart_color_green_500 = {
    "name": "--pf-chart-color-green-500",
    "value": "#23511e",
    "var": "var(--pf-chart-color-green-500)"
  };
  const chart_color_cyan_100 = exports.chart_color_cyan_100 = {
    "name": "--pf-chart-color-cyan-100",
    "value": "#a2d9d9",
    "var": "var(--pf-chart-color-cyan-100)"
  };
  const chart_color_cyan_200 = exports.chart_color_cyan_200 = {
    "name": "--pf-chart-color-cyan-200",
    "value": "#73c5c5",
    "var": "var(--pf-chart-color-cyan-200)"
  };
  const chart_color_cyan_300 = exports.chart_color_cyan_300 = {
    "name": "--pf-chart-color-cyan-300",
    "value": "#009596",
    "var": "var(--pf-chart-color-cyan-300)"
  };
  const chart_color_cyan_400 = exports.chart_color_cyan_400 = {
    "name": "--pf-chart-color-cyan-400",
    "value": "#005f60",
    "var": "var(--pf-chart-color-cyan-400)"
  };
  const chart_color_cyan_500 = exports.chart_color_cyan_500 = {
    "name": "--pf-chart-color-cyan-500",
    "value": "#003737",
    "var": "var(--pf-chart-color-cyan-500)"
  };
  const chart_color_purple_100 = exports.chart_color_purple_100 = {
    "name": "--pf-chart-color-purple-100",
    "value": "#b2b0ea",
    "var": "var(--pf-chart-color-purple-100)"
  };
  const chart_color_purple_200 = exports.chart_color_purple_200 = {
    "name": "--pf-chart-color-purple-200",
    "value": "#8481dd",
    "var": "var(--pf-chart-color-purple-200)"
  };
  const chart_color_purple_300 = exports.chart_color_purple_300 = {
    "name": "--pf-chart-color-purple-300",
    "value": "#5752d1",
    "var": "var(--pf-chart-color-purple-300)"
  };
  const chart_color_purple_400 = exports.chart_color_purple_400 = {
    "name": "--pf-chart-color-purple-400",
    "value": "#3c3d99",
    "var": "var(--pf-chart-color-purple-400)"
  };
  const chart_color_purple_500 = exports.chart_color_purple_500 = {
    "name": "--pf-chart-color-purple-500",
    "value": "#2a265f",
    "var": "var(--pf-chart-color-purple-500)"
  };
  const chart_color_gold_100 = exports.chart_color_gold_100 = {
    "name": "--pf-chart-color-gold-100",
    "value": "#f9e0a2",
    "var": "var(--pf-chart-color-gold-100)"
  };
  const chart_color_gold_200 = exports.chart_color_gold_200 = {
    "name": "--pf-chart-color-gold-200",
    "value": "#f6d173",
    "var": "var(--pf-chart-color-gold-200)"
  };
  const chart_color_gold_300 = exports.chart_color_gold_300 = {
    "name": "--pf-chart-color-gold-300",
    "value": "#f4c145",
    "var": "var(--pf-chart-color-gold-300)"
  };
  const chart_color_gold_400 = exports.chart_color_gold_400 = {
    "name": "--pf-chart-color-gold-400",
    "value": "#f0ab00",
    "var": "var(--pf-chart-color-gold-400)"
  };
  const chart_color_gold_500 = exports.chart_color_gold_500 = {
    "name": "--pf-chart-color-gold-500",
    "value": "#c58c00",
    "var": "var(--pf-chart-color-gold-500)"
  };
  const chart_color_orange_100 = exports.chart_color_orange_100 = {
    "name": "--pf-chart-color-orange-100",
    "value": "#f4b678",
    "var": "var(--pf-chart-color-orange-100)"
  };
  const chart_color_orange_200 = exports.chart_color_orange_200 = {
    "name": "--pf-chart-color-orange-200",
    "value": "#ef9234",
    "var": "var(--pf-chart-color-orange-200)"
  };
  const chart_color_orange_300 = exports.chart_color_orange_300 = {
    "name": "--pf-chart-color-orange-300",
    "value": "#ec7a08",
    "var": "var(--pf-chart-color-orange-300)"
  };
  const chart_color_orange_400 = exports.chart_color_orange_400 = {
    "name": "--pf-chart-color-orange-400",
    "value": "#c46100",
    "var": "var(--pf-chart-color-orange-400)"
  };
  const chart_color_orange_500 = exports.chart_color_orange_500 = {
    "name": "--pf-chart-color-orange-500",
    "value": "#8f4700",
    "var": "var(--pf-chart-color-orange-500)"
  };
  const chart_color_red_100 = exports.chart_color_red_100 = {
    "name": "--pf-chart-color-red-100",
    "value": "#c9190b",
    "var": "var(--pf-chart-color-red-100)"
  };
  const chart_color_red_200 = exports.chart_color_red_200 = {
    "name": "--pf-chart-color-red-200",
    "value": "#a30000",
    "var": "var(--pf-chart-color-red-200)"
  };
  const chart_color_red_300 = exports.chart_color_red_300 = {
    "name": "--pf-chart-color-red-300",
    "value": "#7d1007",
    "var": "var(--pf-chart-color-red-300)"
  };
  const chart_color_red_400 = exports.chart_color_red_400 = {
    "name": "--pf-chart-color-red-400",
    "value": "#470000",
    "var": "var(--pf-chart-color-red-400)"
  };
  const chart_color_red_500 = exports.chart_color_red_500 = {
    "name": "--pf-chart-color-red-500",
    "value": "#2c0000",
    "var": "var(--pf-chart-color-red-500)"
  };
  const chart_color_black_100 = exports.chart_color_black_100 = {
    "name": "--pf-chart-color-black-100",
    "value": "#ededed",
    "var": "var(--pf-chart-color-black-100)"
  };
  const chart_color_black_200 = exports.chart_color_black_200 = {
    "name": "--pf-chart-color-black-200",
    "value": "#d2d2d2",
    "var": "var(--pf-chart-color-black-200)"
  };
  const chart_color_black_300 = exports.chart_color_black_300 = {
    "name": "--pf-chart-color-black-300",
    "value": "#b8bbbe",
    "var": "var(--pf-chart-color-black-300)"
  };
  const chart_color_black_400 = exports.chart_color_black_400 = {
    "name": "--pf-chart-color-black-400",
    "value": "#8a8d90",
    "var": "var(--pf-chart-color-black-400)"
  };
  const chart_color_black_500 = exports.chart_color_black_500 = {
    "name": "--pf-chart-color-black-500",
    "value": "#737679",
    "var": "var(--pf-chart-color-black-500)"
  };
  const chart_global_FontSize_xs = exports.chart_global_FontSize_xs = {
    "name": "--pf-chart-global--FontSize--xs",
    "value": 12,
    "var": "var(--pf-chart-global--FontSize--xs)"
  };
  const chart_global_FontSize_sm = exports.chart_global_FontSize_sm = {
    "name": "--pf-chart-global--FontSize--sm",
    "value": 14,
    "var": "var(--pf-chart-global--FontSize--sm)"
  };
  const chart_global_FontSize_lg = exports.chart_global_FontSize_lg = {
    "name": "--pf-chart-global--FontSize--lg",
    "value": 18,
    "var": "var(--pf-chart-global--FontSize--lg)"
  };
  const chart_global_FontSize_2xl = exports.chart_global_FontSize_2xl = {
    "name": "--pf-chart-global--FontSize--2xl",
    "value": 24,
    "var": "var(--pf-chart-global--FontSize--2xl)"
  };
  const chart_global_FontFamily = exports.chart_global_FontFamily = {
    "name": "--pf-chart-global--FontFamily",
    "value": "overpass, overpass, open sans, -apple-system, blinkmacsystemfont, Segoe UI, roboto, Helvetica Neue, arial, sans-serif, Apple Color Emoji, Segoe UI Emoji, Segoe UI Symbol",
    "var": "var(--pf-chart-global--FontFamily)"
  };
  const chart_global_letter_spacing = exports.chart_global_letter_spacing = {
    "name": "--pf-chart-global--letter-spacing",
    "value": "normal",
    "var": "var(--pf-chart-global--letter-spacing)"
  };
  const chart_global_label_Padding = exports.chart_global_label_Padding = {
    "name": "--pf-chart-global--label--Padding",
    "value": 10,
    "var": "var(--pf-chart-global--label--Padding)"
  };
  const chart_global_label_Margin = exports.chart_global_label_Margin = {
    "name": "--pf-chart-global--label--Margin",
    "value": 8,
    "var": "var(--pf-chart-global--label--Margin)"
  };
  const chart_global_label_stroke = exports.chart_global_label_stroke = {
    "name": "--pf-chart-global--label--stroke",
    "value": "transparent",
    "var": "var(--pf-chart-global--label--stroke)"
  };
  const chart_global_label_text_anchor = exports.chart_global_label_text_anchor = {
    "name": "--pf-chart-global--label--text-anchor",
    "value": "middle",
    "var": "var(--pf-chart-global--label--text-anchor)"
  };
  const chart_global_label_stroke_Width = exports.chart_global_label_stroke_Width = {
    "name": "--pf-chart-global--label--stroke--Width",
    "value": 0,
    "var": "var(--pf-chart-global--label--stroke--Width)"
  };
  const chart_global_label_Fill = exports.chart_global_label_Fill = {
    "name": "--pf-chart-global--label--Fill",
    "value": "#151515",
    "var": "var(--pf-chart-global--label--Fill)"
  };
  const chart_global_layout_Padding = exports.chart_global_layout_Padding = {
    "name": "--pf-chart-global--layout--Padding",
    "value": 50,
    "var": "var(--pf-chart-global--layout--Padding)"
  };
  const chart_global_layout_Height = exports.chart_global_layout_Height = {
    "name": "--pf-chart-global--layout--Height",
    "value": 300,
    "var": "var(--pf-chart-global--layout--Height)"
  };
  const chart_global_layout_Width = exports.chart_global_layout_Width = {
    "name": "--pf-chart-global--layout--Width",
    "value": 450,
    "var": "var(--pf-chart-global--layout--Width)"
  };
  const chart_global_stroke_Width_xs = exports.chart_global_stroke_Width_xs = {
    "name": "--pf-chart-global--stroke--Width--xs",
    "value": 1,
    "var": "var(--pf-chart-global--stroke--Width--xs)"
  };
  const chart_global_stroke_Width_sm = exports.chart_global_stroke_Width_sm = {
    "name": "--pf-chart-global--stroke--Width--sm",
    "value": 2,
    "var": "var(--pf-chart-global--stroke--Width--sm)"
  };
  const chart_global_BorderWidth_xs = exports.chart_global_BorderWidth_xs = {
    "name": "--pf-chart-global--BorderWidth--xs",
    "value": 1,
    "var": "var(--pf-chart-global--BorderWidth--xs)"
  };
  const chart_global_BorderWidth_sm = exports.chart_global_BorderWidth_sm = {
    "name": "--pf-chart-global--BorderWidth--sm",
    "value": 2,
    "var": "var(--pf-chart-global--BorderWidth--sm)"
  };
  const chart_global_BorderWidth_lg = exports.chart_global_BorderWidth_lg = {
    "name": "--pf-chart-global--BorderWidth--lg",
    "value": 8,
    "var": "var(--pf-chart-global--BorderWidth--lg)"
  };
  const chart_global_stroke_line_cap = exports.chart_global_stroke_line_cap = {
    "name": "--pf-chart-global--stroke-line-cap",
    "value": "round",
    "var": "var(--pf-chart-global--stroke-line-cap)"
  };
  const chart_global_stroke_line_join = exports.chart_global_stroke_line_join = {
    "name": "--pf-chart-global--stroke-line-join",
    "value": "round",
    "var": "var(--pf-chart-global--stroke-line-join)"
  };
  const chart_global_danger_Color_100 = exports.chart_global_danger_Color_100 = {
    "name": "--pf-chart-global--danger--Color--100",
    "value": "#c9190b",
    "var": "var(--pf-chart-global--danger--Color--100)"
  };
  const chart_global_warning_Color_100 = exports.chart_global_warning_Color_100 = {
    "name": "--pf-chart-global--warning--Color--100",
    "value": "#ec7a08",
    "var": "var(--pf-chart-global--warning--Color--100)"
  };
  const chart_global_warning_Color_200 = exports.chart_global_warning_Color_200 = {
    "name": "--pf-chart-global--warning--Color--200",
    "value": "#f0ab00",
    "var": "var(--pf-chart-global--warning--Color--200)"
  };
  const chart_global_success_Color_100 = exports.chart_global_success_Color_100 = {
    "name": "--pf-chart-global--success--Color--100",
    "value": "#06c",
    "var": "var(--pf-chart-global--success--Color--100)"
  };
  const chart_global_Fill_Color_900 = exports.chart_global_Fill_Color_900 = {
    "name": "--pf-chart-global--Fill--Color--900",
    "value": "#151515",
    "var": "var(--pf-chart-global--Fill--Color--900)"
  };
  const chart_global_Fill_Color_700 = exports.chart_global_Fill_Color_700 = {
    "name": "--pf-chart-global--Fill--Color--700",
    "value": "#4f5255",
    "var": "var(--pf-chart-global--Fill--Color--700)"
  };
  const chart_global_Fill_Color_500 = exports.chart_global_Fill_Color_500 = {
    "name": "--pf-chart-global--Fill--Color--500",
    "value": "#8a8d90",
    "var": "var(--pf-chart-global--Fill--Color--500)"
  };
  const chart_global_Fill_Color_400 = exports.chart_global_Fill_Color_400 = {
    "name": "--pf-chart-global--Fill--Color--400",
    "value": "#b8bbbe",
    "var": "var(--pf-chart-global--Fill--Color--400)"
  };
  const chart_global_Fill_Color_300 = exports.chart_global_Fill_Color_300 = {
    "name": "--pf-chart-global--Fill--Color--300",
    "value": "#d2d2d2",
    "var": "var(--pf-chart-global--Fill--Color--300)"
  };
  const chart_global_Fill_Color_200 = exports.chart_global_Fill_Color_200 = {
    "name": "--pf-chart-global--Fill--Color--200",
    "value": "#ededed",
    "var": "var(--pf-chart-global--Fill--Color--200)"
  };
  const chart_global_Fill_Color_white = exports.chart_global_Fill_Color_white = {
    "name": "--pf-chart-global--Fill--Color--white",
    "value": "#fff",
    "var": "var(--pf-chart-global--Fill--Color--white)"
  };
  const chart_area_Opacity = exports.chart_area_Opacity = {
    "name": "--pf-chart-area--Opacity",
    "value": 0.3,
    "var": "var(--pf-chart-area--Opacity)"
  };
  const chart_area_stroke_Width = exports.chart_area_stroke_Width = {
    "name": "--pf-chart-area--stroke--Width",
    "value": 2,
    "var": "var(--pf-chart-area--stroke--Width)"
  };
  const chart_area_data_Fill = exports.chart_area_data_Fill = {
    "name": "--pf-chart-area--data--Fill",
    "value": "#151515",
    "var": "var(--pf-chart-area--data--Fill)"
  };
  const chart_axis_axis_stroke_Width = exports.chart_axis_axis_stroke_Width = {
    "name": "--pf-chart-axis--axis--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-axis--axis--stroke--Width)"
  };
  const chart_axis_axis_stroke_Color = exports.chart_axis_axis_stroke_Color = {
    "name": "--pf-chart-axis--axis--stroke--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-chart-axis--axis--stroke--Color)"
  };
  const chart_axis_axis_Fill = exports.chart_axis_axis_Fill = {
    "name": "--pf-chart-axis--axis--Fill",
    "value": "transparent",
    "var": "var(--pf-chart-axis--axis--Fill)"
  };
  const chart_axis_axis_label_Padding = exports.chart_axis_axis_label_Padding = {
    "name": "--pf-chart-axis--axis-label--Padding",
    "value": 40,
    "var": "var(--pf-chart-axis--axis-label--Padding)"
  };
  const chart_axis_axis_label_stroke_Color = exports.chart_axis_axis_label_stroke_Color = {
    "name": "--pf-chart-axis--axis-label--stroke--Color",
    "value": "transparent",
    "var": "var(--pf-chart-axis--axis-label--stroke--Color)"
  };
  const chart_axis_grid_Fill = exports.chart_axis_grid_Fill = {
    "name": "--pf-chart-axis--grid--Fill",
    "value": "none",
    "var": "var(--pf-chart-axis--grid--Fill)"
  };
  const chart_axis_grid_stroke_Color = exports.chart_axis_grid_stroke_Color = {
    "name": "--pf-chart-axis--grid--stroke--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-chart-axis--grid--stroke--Color)"
  };
  const chart_axis_grid_PointerEvents = exports.chart_axis_grid_PointerEvents = {
    "name": "--pf-chart-axis--grid--PointerEvents",
    "value": "painted",
    "var": "var(--pf-chart-axis--grid--PointerEvents)"
  };
  const chart_axis_tick_Fill = exports.chart_axis_tick_Fill = {
    "name": "--pf-chart-axis--tick--Fill",
    "value": "transparent",
    "var": "var(--pf-chart-axis--tick--Fill)"
  };
  const chart_axis_tick_Size = exports.chart_axis_tick_Size = {
    "name": "--pf-chart-axis--tick--Size",
    "value": 5,
    "var": "var(--pf-chart-axis--tick--Size)"
  };
  const chart_axis_tick_Width = exports.chart_axis_tick_Width = {
    "name": "--pf-chart-axis--tick--Width",
    "value": 1,
    "var": "var(--pf-chart-axis--tick--Width)"
  };
  const chart_axis_tick_stroke_Color = exports.chart_axis_tick_stroke_Color = {
    "name": "--pf-chart-axis--tick--stroke--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-chart-axis--tick--stroke--Color)"
  };
  const chart_axis_tick_label_Fill = exports.chart_axis_tick_label_Fill = {
    "name": "--pf-chart-axis--tick-label--Fill",
    "value": "#4f5255",
    "var": "var(--pf-chart-axis--tick-label--Fill)"
  };
  const chart_bar_Width = exports.chart_bar_Width = {
    "name": "--pf-chart-bar--Width",
    "value": 10,
    "var": "var(--pf-chart-bar--Width)"
  };
  const chart_bar_data_stroke = exports.chart_bar_data_stroke = {
    "name": "--pf-chart-bar--data--stroke",
    "value": "none",
    "var": "var(--pf-chart-bar--data--stroke)"
  };
  const chart_bar_data_Fill = exports.chart_bar_data_Fill = {
    "name": "--pf-chart-bar--data--Fill",
    "value": "#151515",
    "var": "var(--pf-chart-bar--data--Fill)"
  };
  const chart_bar_data_Padding = exports.chart_bar_data_Padding = {
    "name": "--pf-chart-bar--data--Padding",
    "value": 8,
    "var": "var(--pf-chart-bar--data--Padding)"
  };
  const chart_bar_data_stroke_Width = exports.chart_bar_data_stroke_Width = {
    "name": "--pf-chart-bar--data-stroke--Width",
    "value": 0,
    "var": "var(--pf-chart-bar--data-stroke--Width)"
  };
  const chart_boxplot_max_Padding = exports.chart_boxplot_max_Padding = {
    "name": "--pf-chart-boxplot--max--Padding",
    "value": 8,
    "var": "var(--pf-chart-boxplot--max--Padding)"
  };
  const chart_boxplot_max_stroke_Color = exports.chart_boxplot_max_stroke_Color = {
    "name": "--pf-chart-boxplot--max--stroke--Color",
    "value": "#151515",
    "var": "var(--pf-chart-boxplot--max--stroke--Color)"
  };
  const chart_boxplot_max_stroke_Width = exports.chart_boxplot_max_stroke_Width = {
    "name": "--pf-chart-boxplot--max--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-boxplot--max--stroke--Width)"
  };
  const chart_boxplot_median_Padding = exports.chart_boxplot_median_Padding = {
    "name": "--pf-chart-boxplot--median--Padding",
    "value": 8,
    "var": "var(--pf-chart-boxplot--median--Padding)"
  };
  const chart_boxplot_median_stroke_Color = exports.chart_boxplot_median_stroke_Color = {
    "name": "--pf-chart-boxplot--median--stroke--Color",
    "value": "#151515",
    "var": "var(--pf-chart-boxplot--median--stroke--Color)"
  };
  const chart_boxplot_median_stroke_Width = exports.chart_boxplot_median_stroke_Width = {
    "name": "--pf-chart-boxplot--median--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-boxplot--median--stroke--Width)"
  };
  const chart_boxplot_min_Padding = exports.chart_boxplot_min_Padding = {
    "name": "--pf-chart-boxplot--min--Padding",
    "value": 8,
    "var": "var(--pf-chart-boxplot--min--Padding)"
  };
  const chart_boxplot_min_stroke_Width = exports.chart_boxplot_min_stroke_Width = {
    "name": "--pf-chart-boxplot--min--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-boxplot--min--stroke--Width)"
  };
  const chart_boxplot_min_stroke_Color = exports.chart_boxplot_min_stroke_Color = {
    "name": "--pf-chart-boxplot--min--stroke--Color",
    "value": "#151515",
    "var": "var(--pf-chart-boxplot--min--stroke--Color)"
  };
  const chart_boxplot_lower_quartile_Padding = exports.chart_boxplot_lower_quartile_Padding = {
    "name": "--pf-chart-boxplot--lower-quartile--Padding",
    "value": 8,
    "var": "var(--pf-chart-boxplot--lower-quartile--Padding)"
  };
  const chart_boxplot_lower_quartile_Fill = exports.chart_boxplot_lower_quartile_Fill = {
    "name": "--pf-chart-boxplot--lower-quartile--Fill",
    "value": "#8a8d90",
    "var": "var(--pf-chart-boxplot--lower-quartile--Fill)"
  };
  const chart_boxplot_upper_quartile_Padding = exports.chart_boxplot_upper_quartile_Padding = {
    "name": "--pf-chart-boxplot--upper-quartile--Padding",
    "value": 8,
    "var": "var(--pf-chart-boxplot--upper-quartile--Padding)"
  };
  const chart_boxplot_upper_quartile_Fill = exports.chart_boxplot_upper_quartile_Fill = {
    "name": "--pf-chart-boxplot--upper-quartile--Fill",
    "value": "#8a8d90",
    "var": "var(--pf-chart-boxplot--upper-quartile--Fill)"
  };
  const chart_boxplot_box_Width = exports.chart_boxplot_box_Width = {
    "name": "--pf-chart-boxplot--box--Width",
    "value": 20,
    "var": "var(--pf-chart-boxplot--box--Width)"
  };
  const chart_bullet_axis_tick_count = exports.chart_bullet_axis_tick_count = {
    "name": "--pf-chart-bullet--axis--tick--count",
    "value": 5,
    "var": "var(--pf-chart-bullet--axis--tick--count)"
  };
  const chart_bullet_comparative_measure_Fill_Color = exports.chart_bullet_comparative_measure_Fill_Color = {
    "name": "--pf-chart-bullet--comparative-measure--Fill--Color",
    "value": "#4f5255",
    "var": "var(--pf-chart-bullet--comparative-measure--Fill--Color)"
  };
  const chart_bullet_comparative_measure_stroke_Color = exports.chart_bullet_comparative_measure_stroke_Color = {
    "name": "--pf-chart-bullet--comparative-measure--stroke--Color",
    "value": "#4f5255",
    "var": "var(--pf-chart-bullet--comparative-measure--stroke--Color)"
  };
  const chart_bullet_comparative_measure_stroke_Width = exports.chart_bullet_comparative_measure_stroke_Width = {
    "name": "--pf-chart-bullet--comparative-measure--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-bullet--comparative-measure--stroke--Width)"
  };
  const chart_bullet_comparative_measure_error_Fill_Color = exports.chart_bullet_comparative_measure_error_Fill_Color = {
    "name": "--pf-chart-bullet--comparative-measure--error--Fill--Color",
    "value": "#c9190b",
    "var": "var(--pf-chart-bullet--comparative-measure--error--Fill--Color)"
  };
  const chart_bullet_comparative_measure_error_stroke_Color = exports.chart_bullet_comparative_measure_error_stroke_Color = {
    "name": "--pf-chart-bullet--comparative-measure--error--stroke--Color",
    "value": "#c9190b",
    "var": "var(--pf-chart-bullet--comparative-measure--error--stroke--Color)"
  };
  const chart_bullet_comparative_measure_error_stroke_Width = exports.chart_bullet_comparative_measure_error_stroke_Width = {
    "name": "--pf-chart-bullet--comparative-measure--error--stroke--Width",
    "value": 2,
    "var": "var(--pf-chart-bullet--comparative-measure--error--stroke--Width)"
  };
  const chart_bullet_comparative_measure_error_Width = exports.chart_bullet_comparative_measure_error_Width = {
    "name": "--pf-chart-bullet--comparative-measure--error--Width",
    "value": 30,
    "var": "var(--pf-chart-bullet--comparative-measure--error--Width)"
  };
  const chart_bullet_comparative_measure_warning_Fill_Color = exports.chart_bullet_comparative_measure_warning_Fill_Color = {
    "name": "--pf-chart-bullet--comparative-measure--warning--Fill--Color",
    "value": "#ec7a08",
    "var": "var(--pf-chart-bullet--comparative-measure--warning--Fill--Color)"
  };
  const chart_bullet_comparative_measure_warning_stroke_Color = exports.chart_bullet_comparative_measure_warning_stroke_Color = {
    "name": "--pf-chart-bullet--comparative-measure--warning--stroke--Color",
    "value": "#ec7a08",
    "var": "var(--pf-chart-bullet--comparative-measure--warning--stroke--Color)"
  };
  const chart_bullet_comparative_measure_warning_stroke_Width = exports.chart_bullet_comparative_measure_warning_stroke_Width = {
    "name": "--pf-chart-bullet--comparative-measure--warning--stroke--Width",
    "value": 2,
    "var": "var(--pf-chart-bullet--comparative-measure--warning--stroke--Width)"
  };
  const chart_bullet_comparative_measure_warning_Width = exports.chart_bullet_comparative_measure_warning_Width = {
    "name": "--pf-chart-bullet--comparative-measure--warning--Width",
    "value": 30,
    "var": "var(--pf-chart-bullet--comparative-measure--warning--Width)"
  };
  const chart_bullet_comparative_measure_Width = exports.chart_bullet_comparative_measure_Width = {
    "name": "--pf-chart-bullet--comparative-measure--Width",
    "value": 30,
    "var": "var(--pf-chart-bullet--comparative-measure--Width)"
  };
  const chart_bullet_group_title_divider_Fill_Color = exports.chart_bullet_group_title_divider_Fill_Color = {
    "name": "--pf-chart-bullet--group-title--divider--Fill--Color",
    "value": "#ededed",
    "var": "var(--pf-chart-bullet--group-title--divider--Fill--Color)"
  };
  const chart_bullet_group_title_divider_stroke_Color = exports.chart_bullet_group_title_divider_stroke_Color = {
    "name": "--pf-chart-bullet--group-title--divider--stroke--Color",
    "value": "#ededed",
    "var": "var(--pf-chart-bullet--group-title--divider--stroke--Color)"
  };
  const chart_bullet_group_title_divider_stroke_Width = exports.chart_bullet_group_title_divider_stroke_Width = {
    "name": "--pf-chart-bullet--group-title--divider--stroke--Width",
    "value": 2,
    "var": "var(--pf-chart-bullet--group-title--divider--stroke--Width)"
  };
  const chart_bullet_Height = exports.chart_bullet_Height = {
    "name": "--pf-chart-bullet--Height",
    "value": 140,
    "var": "var(--pf-chart-bullet--Height)"
  };
  const chart_bullet_label_subtitle_Fill = exports.chart_bullet_label_subtitle_Fill = {
    "name": "--pf-chart-bullet--label--subtitle--Fill",
    "value": "#b8bbbe",
    "var": "var(--pf-chart-bullet--label--subtitle--Fill)"
  };
  const chart_bullet_primary_measure_dot_size = exports.chart_bullet_primary_measure_dot_size = {
    "name": "--pf-chart-bullet--primary-measure--dot--size",
    "value": 6,
    "var": "var(--pf-chart-bullet--primary-measure--dot--size)"
  };
  const chart_bullet_primary_measure_segmented_Width = exports.chart_bullet_primary_measure_segmented_Width = {
    "name": "--pf-chart-bullet--primary-measure--segmented--Width",
    "value": 9,
    "var": "var(--pf-chart-bullet--primary-measure--segmented--Width)"
  };
  const chart_bullet_qualitative_range_Width = exports.chart_bullet_qualitative_range_Width = {
    "name": "--pf-chart-bullet--qualitative-range--Width",
    "value": 30,
    "var": "var(--pf-chart-bullet--qualitative-range--Width)"
  };
  const chart_candelstick_data_stroke_Width = exports.chart_candelstick_data_stroke_Width = {
    "name": "--pf-chart-candelstick--data--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-candelstick--data--stroke--Width)"
  };
  const chart_candelstick_data_stroke_Color = exports.chart_candelstick_data_stroke_Color = {
    "name": "--pf-chart-candelstick--data--stroke--Color",
    "value": "#151515",
    "var": "var(--pf-chart-candelstick--data--stroke--Color)"
  };
  const chart_candelstick_candle_positive_Color = exports.chart_candelstick_candle_positive_Color = {
    "name": "--pf-chart-candelstick--candle--positive--Color",
    "value": "#fff",
    "var": "var(--pf-chart-candelstick--candle--positive--Color)"
  };
  const chart_candelstick_candle_negative_Color = exports.chart_candelstick_candle_negative_Color = {
    "name": "--pf-chart-candelstick--candle--negative--Color",
    "value": "#151515",
    "var": "var(--pf-chart-candelstick--candle--negative--Color)"
  };
  const chart_donut_label_subtitle_Fill = exports.chart_donut_label_subtitle_Fill = {
    "name": "--pf-chart-donut--label--subtitle--Fill",
    "value": "#b8bbbe",
    "var": "var(--pf-chart-donut--label--subtitle--Fill)"
  };
  const chart_donut_label_subtitle_position = exports.chart_donut_label_subtitle_position = {
    "name": "--pf-chart-donut--label--subtitle--position",
    "value": "center",
    "var": "var(--pf-chart-donut--label--subtitle--position)"
  };
  const chart_donut_pie_Height = exports.chart_donut_pie_Height = {
    "name": "--pf-chart-donut--pie--Height",
    "value": 230,
    "var": "var(--pf-chart-donut--pie--Height)"
  };
  const chart_donut_pie_angle_Padding = exports.chart_donut_pie_angle_Padding = {
    "name": "--pf-chart-donut--pie--angle--Padding",
    "value": 1,
    "var": "var(--pf-chart-donut--pie--angle--Padding)"
  };
  const chart_donut_pie_Padding = exports.chart_donut_pie_Padding = {
    "name": "--pf-chart-donut--pie--Padding",
    "value": 20,
    "var": "var(--pf-chart-donut--pie--Padding)"
  };
  const chart_donut_pie_Width = exports.chart_donut_pie_Width = {
    "name": "--pf-chart-donut--pie--Width",
    "value": 230,
    "var": "var(--pf-chart-donut--pie--Width)"
  };
  const chart_donut_threshold_first_Color = exports.chart_donut_threshold_first_Color = {
    "name": "--pf-chart-donut--threshold--first--Color",
    "value": "#ededed",
    "var": "var(--pf-chart-donut--threshold--first--Color)"
  };
  const chart_donut_threshold_second_Color = exports.chart_donut_threshold_second_Color = {
    "name": "--pf-chart-donut--threshold--second--Color",
    "value": "#d2d2d2",
    "var": "var(--pf-chart-donut--threshold--second--Color)"
  };
  const chart_donut_threshold_third_Color = exports.chart_donut_threshold_third_Color = {
    "name": "--pf-chart-donut--threshold--third--Color",
    "value": "#b8bbbe",
    "var": "var(--pf-chart-donut--threshold--third--Color)"
  };
  const chart_donut_threshold_warning_Color = exports.chart_donut_threshold_warning_Color = {
    "name": "--pf-chart-donut--threshold--warning--Color",
    "value": "#f0ab00",
    "var": "var(--pf-chart-donut--threshold--warning--Color)"
  };
  const chart_donut_threshold_danger_Color = exports.chart_donut_threshold_danger_Color = {
    "name": "--pf-chart-donut--threshold--danger--Color",
    "value": "#c9190b",
    "var": "var(--pf-chart-donut--threshold--danger--Color)"
  };
  const chart_donut_threshold_dynamic_pie_Height = exports.chart_donut_threshold_dynamic_pie_Height = {
    "name": "--pf-chart-donut--threshold--dynamic--pie--Height",
    "value": 202,
    "var": "var(--pf-chart-donut--threshold--dynamic--pie--Height)"
  };
  const chart_donut_threshold_dynamic_pie_Padding = exports.chart_donut_threshold_dynamic_pie_Padding = {
    "name": "--pf-chart-donut--threshold--dynamic--pie--Padding",
    "value": 20,
    "var": "var(--pf-chart-donut--threshold--dynamic--pie--Padding)"
  };
  const chart_donut_threshold_dynamic_pie_Width = exports.chart_donut_threshold_dynamic_pie_Width = {
    "name": "--pf-chart-donut--threshold--dynamic--pie--Width",
    "value": 202,
    "var": "var(--pf-chart-donut--threshold--dynamic--pie--Width)"
  };
  const chart_donut_threshold_static_pie_Height = exports.chart_donut_threshold_static_pie_Height = {
    "name": "--pf-chart-donut--threshold--static--pie--Height",
    "value": 230,
    "var": "var(--pf-chart-donut--threshold--static--pie--Height)"
  };
  const chart_donut_threshold_static_pie_angle_Padding = exports.chart_donut_threshold_static_pie_angle_Padding = {
    "name": "--pf-chart-donut--threshold--static--pie--angle--Padding",
    "value": 1,
    "var": "var(--pf-chart-donut--threshold--static--pie--angle--Padding)"
  };
  const chart_donut_threshold_static_pie_Padding = exports.chart_donut_threshold_static_pie_Padding = {
    "name": "--pf-chart-donut--threshold--static--pie--Padding",
    "value": 20,
    "var": "var(--pf-chart-donut--threshold--static--pie--Padding)"
  };
  const chart_donut_threshold_static_pie_Width = exports.chart_donut_threshold_static_pie_Width = {
    "name": "--pf-chart-donut--threshold--static--pie--Width",
    "value": 230,
    "var": "var(--pf-chart-donut--threshold--static--pie--Width)"
  };
  const chart_donut_utilization_dynamic_pie_Height = exports.chart_donut_utilization_dynamic_pie_Height = {
    "name": "--pf-chart-donut--utilization--dynamic--pie--Height",
    "value": 230,
    "var": "var(--pf-chart-donut--utilization--dynamic--pie--Height)"
  };
  const chart_donut_utilization_dynamic_pie_angle_Padding = exports.chart_donut_utilization_dynamic_pie_angle_Padding = {
    "name": "--pf-chart-donut--utilization--dynamic--pie--angle--Padding",
    "value": 1,
    "var": "var(--pf-chart-donut--utilization--dynamic--pie--angle--Padding)"
  };
  const chart_donut_utilization_dynamic_pie_Padding = exports.chart_donut_utilization_dynamic_pie_Padding = {
    "name": "--pf-chart-donut--utilization--dynamic--pie--Padding",
    "value": 20,
    "var": "var(--pf-chart-donut--utilization--dynamic--pie--Padding)"
  };
  const chart_donut_utilization_dynamic_pie_Width = exports.chart_donut_utilization_dynamic_pie_Width = {
    "name": "--pf-chart-donut--utilization--dynamic--pie--Width",
    "value": 230,
    "var": "var(--pf-chart-donut--utilization--dynamic--pie--Width)"
  };
  const chart_donut_utilization_static_pie_Padding = exports.chart_donut_utilization_static_pie_Padding = {
    "name": "--pf-chart-donut--utilization--static--pie--Padding",
    "value": 20,
    "var": "var(--pf-chart-donut--utilization--static--pie--Padding)"
  };
  const chart_errorbar_BorderWidth = exports.chart_errorbar_BorderWidth = {
    "name": "--pf-chart-errorbar--BorderWidth",
    "value": 8,
    "var": "var(--pf-chart-errorbar--BorderWidth)"
  };
  const chart_errorbar_data_Fill = exports.chart_errorbar_data_Fill = {
    "name": "--pf-chart-errorbar--data--Fill",
    "value": "transparent",
    "var": "var(--pf-chart-errorbar--data--Fill)"
  };
  const chart_errorbar_data_Opacity = exports.chart_errorbar_data_Opacity = {
    "name": "--pf-chart-errorbar--data--Opacity",
    "value": 1,
    "var": "var(--pf-chart-errorbar--data--Opacity)"
  };
  const chart_errorbar_data_stroke_Width = exports.chart_errorbar_data_stroke_Width = {
    "name": "--pf-chart-errorbar--data-stroke--Width",
    "value": 2,
    "var": "var(--pf-chart-errorbar--data-stroke--Width)"
  };
  const chart_errorbar_data_stroke_Color = exports.chart_errorbar_data_stroke_Color = {
    "name": "--pf-chart-errorbar--data-stroke--Color",
    "value": "#151515",
    "var": "var(--pf-chart-errorbar--data-stroke--Color)"
  };
  const chart_legend_gutter_Width = exports.chart_legend_gutter_Width = {
    "name": "--pf-chart-legend--gutter--Width",
    "value": 20,
    "var": "var(--pf-chart-legend--gutter--Width)"
  };
  const chart_legend_orientation = exports.chart_legend_orientation = {
    "name": "--pf-chart-legend--orientation",
    "value": "horizontal",
    "var": "var(--pf-chart-legend--orientation)"
  };
  const chart_legend_position = exports.chart_legend_position = {
    "name": "--pf-chart-legend--position",
    "value": "right",
    "var": "var(--pf-chart-legend--position)"
  };
  const chart_legend_title_orientation = exports.chart_legend_title_orientation = {
    "name": "--pf-chart-legend--title--orientation",
    "value": "top",
    "var": "var(--pf-chart-legend--title--orientation)"
  };
  const chart_legend_data_type = exports.chart_legend_data_type = {
    "name": "--pf-chart-legend--data--type",
    "value": "square",
    "var": "var(--pf-chart-legend--data--type)"
  };
  const chart_legend_title_Padding = exports.chart_legend_title_Padding = {
    "name": "--pf-chart-legend--title--Padding",
    "value": 2,
    "var": "var(--pf-chart-legend--title--Padding)"
  };
  const chart_legend_Margin = exports.chart_legend_Margin = {
    "name": "--pf-chart-legend--Margin",
    "value": 16,
    "var": "var(--pf-chart-legend--Margin)"
  };
  const chart_line_data_Fill = exports.chart_line_data_Fill = {
    "name": "--pf-chart-line--data--Fill",
    "value": "transparent",
    "var": "var(--pf-chart-line--data--Fill)"
  };
  const chart_line_data_Opacity = exports.chart_line_data_Opacity = {
    "name": "--pf-chart-line--data--Opacity",
    "value": 1,
    "var": "var(--pf-chart-line--data--Opacity)"
  };
  const chart_line_data_stroke_Width = exports.chart_line_data_stroke_Width = {
    "name": "--pf-chart-line--data--stroke--Width",
    "value": 2,
    "var": "var(--pf-chart-line--data--stroke--Width)"
  };
  const chart_line_data_stroke_Color = exports.chart_line_data_stroke_Color = {
    "name": "--pf-chart-line--data--stroke--Color",
    "value": "#151515",
    "var": "var(--pf-chart-line--data--stroke--Color)"
  };
  const chart_pie_Padding = exports.chart_pie_Padding = {
    "name": "--pf-chart-pie--Padding",
    "value": 20,
    "var": "var(--pf-chart-pie--Padding)"
  };
  const chart_pie_data_Padding = exports.chart_pie_data_Padding = {
    "name": "--pf-chart-pie--data--Padding",
    "value": 8,
    "var": "var(--pf-chart-pie--data--Padding)"
  };
  const chart_pie_data_stroke_Width = exports.chart_pie_data_stroke_Width = {
    "name": "--pf-chart-pie--data--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-pie--data--stroke--Width)"
  };
  const chart_pie_data_stroke_Color = exports.chart_pie_data_stroke_Color = {
    "name": "--pf-chart-pie--data--stroke--Color",
    "value": "transparent",
    "var": "var(--pf-chart-pie--data--stroke--Color)"
  };
  const chart_pie_labels_Padding = exports.chart_pie_labels_Padding = {
    "name": "--pf-chart-pie--labels--Padding",
    "value": 8,
    "var": "var(--pf-chart-pie--labels--Padding)"
  };
  const chart_pie_Height = exports.chart_pie_Height = {
    "name": "--pf-chart-pie--Height",
    "value": 230,
    "var": "var(--pf-chart-pie--Height)"
  };
  const chart_pie_Width = exports.chart_pie_Width = {
    "name": "--pf-chart-pie--Width",
    "value": 230,
    "var": "var(--pf-chart-pie--Width)"
  };
  const chart_scatter_data_stroke_Color = exports.chart_scatter_data_stroke_Color = {
    "name": "--pf-chart-scatter--data--stroke--Color",
    "value": "transparent",
    "var": "var(--pf-chart-scatter--data--stroke--Color)"
  };
  const chart_scatter_data_stroke_Width = exports.chart_scatter_data_stroke_Width = {
    "name": "--pf-chart-scatter--data--stroke--Width",
    "value": 0,
    "var": "var(--pf-chart-scatter--data--stroke--Width)"
  };
  const chart_scatter_data_Opacity = exports.chart_scatter_data_Opacity = {
    "name": "--pf-chart-scatter--data--Opacity",
    "value": 1,
    "var": "var(--pf-chart-scatter--data--Opacity)"
  };
  const chart_scatter_data_Fill = exports.chart_scatter_data_Fill = {
    "name": "--pf-chart-scatter--data--Fill",
    "value": "#151515",
    "var": "var(--pf-chart-scatter--data--Fill)"
  };
  const chart_scatter_active_size = exports.chart_scatter_active_size = {
    "name": "--pf-chart-scatter--active--size",
    "value": 5,
    "var": "var(--pf-chart-scatter--active--size)"
  };
  const chart_scatter_size = exports.chart_scatter_size = {
    "name": "--pf-chart-scatter--size",
    "value": 3,
    "var": "var(--pf-chart-scatter--size)"
  };
  const chart_stack_data_stroke_Width = exports.chart_stack_data_stroke_Width = {
    "name": "--pf-chart-stack--data--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-stack--data--stroke--Width)"
  };
  const chart_threshold_stroke_dash_array = exports.chart_threshold_stroke_dash_array = {
    "name": "--pf-chart-threshold--stroke-dash-array",
    "value": "4,2",
    "var": "var(--pf-chart-threshold--stroke-dash-array)"
  };
  const chart_threshold_stroke_Width = exports.chart_threshold_stroke_Width = {
    "name": "--pf-chart-threshold--stroke--Width",
    "value": 1.5,
    "var": "var(--pf-chart-threshold--stroke--Width)"
  };
  const chart_tooltip_corner_radius = exports.chart_tooltip_corner_radius = {
    "name": "--pf-chart-tooltip--corner-radius",
    "value": 0,
    "var": "var(--pf-chart-tooltip--corner-radius)"
  };
  const chart_tooltip_pointer_length = exports.chart_tooltip_pointer_length = {
    "name": "--pf-chart-tooltip--pointer-length",
    "value": 10,
    "var": "var(--pf-chart-tooltip--pointer-length)"
  };
  const chart_tooltip_Fill = exports.chart_tooltip_Fill = {
    "name": "--pf-chart-tooltip--Fill",
    "value": "#ededed",
    "var": "var(--pf-chart-tooltip--Fill)"
  };
  const chart_tooltip_flyoutStyle_corner_radius = exports.chart_tooltip_flyoutStyle_corner_radius = {
    "name": "--pf-chart-tooltip--flyoutStyle--corner-radius",
    "value": 0,
    "var": "var(--pf-chart-tooltip--flyoutStyle--corner-radius)"
  };
  const chart_tooltip_flyoutStyle_stroke_Width = exports.chart_tooltip_flyoutStyle_stroke_Width = {
    "name": "--pf-chart-tooltip--flyoutStyle--stroke--Width",
    "value": 0,
    "var": "var(--pf-chart-tooltip--flyoutStyle--stroke--Width)"
  };
  const chart_tooltip_flyoutStyle_PointerEvents = exports.chart_tooltip_flyoutStyle_PointerEvents = {
    "name": "--pf-chart-tooltip--flyoutStyle--PointerEvents",
    "value": "none",
    "var": "var(--pf-chart-tooltip--flyoutStyle--PointerEvents)"
  };
  const chart_tooltip_flyoutStyle_stroke_Color = exports.chart_tooltip_flyoutStyle_stroke_Color = {
    "name": "--pf-chart-tooltip--flyoutStyle--stroke--Color",
    "value": "#151515",
    "var": "var(--pf-chart-tooltip--flyoutStyle--stroke--Color)"
  };
  const chart_tooltip_flyoutStyle_Fill = exports.chart_tooltip_flyoutStyle_Fill = {
    "name": "--pf-chart-tooltip--flyoutStyle--Fill",
    "value": "#151515",
    "var": "var(--pf-chart-tooltip--flyoutStyle--Fill)"
  };
  const chart_tooltip_pointer_Width = exports.chart_tooltip_pointer_Width = {
    "name": "--pf-chart-tooltip--pointer--Width",
    "value": 20,
    "var": "var(--pf-chart-tooltip--pointer--Width)"
  };
  const chart_tooltip_Padding = exports.chart_tooltip_Padding = {
    "name": "--pf-chart-tooltip--Padding",
    "value": 16,
    "var": "var(--pf-chart-tooltip--Padding)"
  };
  const chart_tooltip_PointerEvents = exports.chart_tooltip_PointerEvents = {
    "name": "--pf-chart-tooltip--PointerEvents",
    "value": "none",
    "var": "var(--pf-chart-tooltip--PointerEvents)"
  };
  const chart_voronoi_data_Fill = exports.chart_voronoi_data_Fill = {
    "name": "--pf-chart-voronoi--data--Fill",
    "value": "transparent",
    "var": "var(--pf-chart-voronoi--data--Fill)"
  };
  const chart_voronoi_data_stroke_Color = exports.chart_voronoi_data_stroke_Color = {
    "name": "--pf-chart-voronoi--data--stroke--Color",
    "value": "transparent",
    "var": "var(--pf-chart-voronoi--data--stroke--Color)"
  };
  const chart_voronoi_data_stroke_Width = exports.chart_voronoi_data_stroke_Width = {
    "name": "--pf-chart-voronoi--data--stroke--Width",
    "value": 0,
    "var": "var(--pf-chart-voronoi--data--stroke--Width)"
  };
  const chart_voronoi_labels_Padding = exports.chart_voronoi_labels_Padding = {
    "name": "--pf-chart-voronoi--labels--Padding",
    "value": 8,
    "var": "var(--pf-chart-voronoi--labels--Padding)"
  };
  const chart_voronoi_labels_Fill = exports.chart_voronoi_labels_Fill = {
    "name": "--pf-chart-voronoi--labels--Fill",
    "value": "#ededed",
    "var": "var(--pf-chart-voronoi--labels--Fill)"
  };
  const chart_voronoi_labels_PointerEvents = exports.chart_voronoi_labels_PointerEvents = {
    "name": "--pf-chart-voronoi--labels--PointerEvents",
    "value": "none",
    "var": "var(--pf-chart-voronoi--labels--PointerEvents)"
  };
  const chart_voronoi_flyout_stroke_Width = exports.chart_voronoi_flyout_stroke_Width = {
    "name": "--pf-chart-voronoi--flyout--stroke--Width",
    "value": 1,
    "var": "var(--pf-chart-voronoi--flyout--stroke--Width)"
  };
  const chart_voronoi_flyout_PointerEvents = exports.chart_voronoi_flyout_PointerEvents = {
    "name": "--pf-chart-voronoi--flyout--PointerEvents",
    "value": "none",
    "var": "var(--pf-chart-voronoi--flyout--PointerEvents)"
  };
  const chart_voronoi_flyout_stroke_Color = exports.chart_voronoi_flyout_stroke_Color = {
    "name": "--pf-chart-voronoi--flyout--stroke--Color",
    "value": "#151515",
    "var": "var(--pf-chart-voronoi--flyout--stroke--Color)"
  };
  const chart_voronoi_flyout_stroke_Fill = exports.chart_voronoi_flyout_stroke_Fill = {
    "name": "--pf-chart-voronoi--flyout--stroke--Fill",
    "value": "#151515",
    "var": "var(--pf-chart-voronoi--flyout--stroke--Fill)"
  };
});