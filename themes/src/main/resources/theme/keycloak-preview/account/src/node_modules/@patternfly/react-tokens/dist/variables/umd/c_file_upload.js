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
    ".pf-c-file-upload": [{
      "property": "--pf-c-file-upload--m-loading__file-details--before--BackgroundColor",
      "value": "#fff",
      "token": "c_file_upload_m_loading__file_details_before_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--100", "$pf-global--BackgroundColor--100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-file-upload--m-loading__file-details--before--Left",
      "value": "1px",
      "token": "c_file_upload_m_loading__file_details_before_Left",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-file-upload--m-loading__file-details--before--Right",
      "value": "1px",
      "token": "c_file_upload_m_loading__file_details_before_Right",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-file-upload--m-loading__file-details--before--Bottom",
      "value": "1px",
      "token": "c_file_upload_m_loading__file_details_before_Bottom",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-file-upload--m-drag-hover--before--BorderWidth",
      "value": "1px",
      "token": "c_file_upload_m_drag_hover_before_BorderWidth",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-file-upload--m-drag-hover--before--BorderColor",
      "value": "#06c",
      "token": "c_file_upload_m_drag_hover_before_BorderColor",
      "values": ["--pf-global--primary-color--100", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-file-upload--m-drag-hover--before--ZIndex",
      "value": "100",
      "token": "c_file_upload_m_drag_hover_before_ZIndex",
      "values": ["--pf-global--ZIndex--xs", "$pf-global--ZIndex--xs", "100"]
    }, {
      "property": "--pf-c-file-upload--m-drag-hover--after--BackgroundColor",
      "value": "#06c",
      "token": "c_file_upload_m_drag_hover_after_BackgroundColor",
      "values": ["--pf-global--primary-color--100", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-file-upload--m-drag-hover--after--Opacity",
      "value": ".1",
      "token": "c_file_upload_m_drag_hover_after_Opacity"
    }, {
      "property": "--pf-c-file-upload__file-details__c-form-control--MinHeight",
      "value": "calc(4rem * 2)",
      "token": "c_file_upload__file_details__c_form_control_MinHeight",
      "values": ["calc(--pf-global--spacer--3xl * 2)", "calc($pf-global--spacer--3xl * 2)", "calc(pf-size-prem(64px) * 2)", "calc(4rem * 2)"]
    }, {
      "property": "--pf-c-file-upload__file-select__c-button--m-control--disabled--BackgroundColor",
      "value": "#ededed",
      "token": "c_file_upload__file_select__c_button_m_control_disabled_BackgroundColor",
      "values": ["--pf-global--disabled-color--300", "$pf-global--disabled-color--300", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderTopColor",
      "value": "#ededed",
      "token": "c_file_upload__file_select__c_button_m_control_disabled_after_BorderTopColor",
      "values": ["--pf-global--BorderColor--300", "$pf-global--BorderColor--300", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderRightColor",
      "value": "#ededed",
      "token": "c_file_upload__file_select__c_button_m_control_disabled_after_BorderRightColor",
      "values": ["--pf-global--BorderColor--300", "$pf-global--BorderColor--300", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderBottomColor",
      "value": "#8a8d90",
      "token": "c_file_upload__file_select__c_button_m_control_disabled_after_BorderBottomColor",
      "values": ["--pf-global--BorderColor--200", "$pf-global--BorderColor--200", "$pf-color-black-500", "#8a8d90"]
    }, {
      "property": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderLeftColor",
      "value": "#ededed",
      "token": "c_file_upload__file_select__c_button_m_control_disabled_after_BorderLeftColor",
      "values": ["--pf-global--BorderColor--300", "$pf-global--BorderColor--300", "$pf-color-black-200", "#ededed"]
    }, {
      "property": "--pf-c-file-upload__file-select__c-button--m-control--disabled--after--BorderWidth",
      "value": "1px",
      "token": "c_file_upload__file_select__c_button_m_control_disabled_after_BorderWidth",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-file-upload__file-select__c-button--m-control--OutlineOffset",
      "value": "calc(-1 * 0.25rem)",
      "token": "c_file_upload__file_select__c_button_m_control_OutlineOffset",
      "values": ["calc(-1 * --pf-global--spacer--xs)", "calc(-1 * $pf-global--spacer--xs)", "calc(-1 * pf-size-prem(4px))", "calc(-1 * 0.25rem)"]
    }]
  };
});