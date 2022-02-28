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
    ".pf-c-table": [{
      "property": "--pf-global--Color--100",
      "value": "#151515",
      "token": "global_Color_100",
      "values": ["--pf-global--Color--dark-100", "$pf-global--Color--dark-100", "$pf-color-black-900", "#151515"]
    }, {
      "property": "--pf-global--Color--200",
      "value": "#737679",
      "token": "global_Color_200",
      "values": ["--pf-global--Color--dark-200", "$pf-global--Color--dark-200", "$pf-color-black-600", "#737679"]
    }, {
      "property": "--pf-global--BorderColor--100",
      "value": "#d2d2d2",
      "token": "global_BorderColor_100",
      "values": ["--pf-global--BorderColor--dark-100", "$pf-global--BorderColor--dark-100", "$pf-color-black-300", "#d2d2d2"]
    }, {
      "property": "--pf-global--primary-color--100",
      "value": "#06c",
      "token": "global_primary_color_100",
      "values": ["--pf-global--primary-color--dark-100", "$pf-global--primary-color--dark-100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-global--link--Color",
      "value": "#06c",
      "token": "global_link_Color",
      "values": ["--pf-global--link--Color--dark", "$pf-global--link--Color--dark", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-global--link--Color--hover",
      "value": "#004080",
      "token": "global_link_Color_hover",
      "values": ["--pf-global--link--Color--dark--hover", "$pf-global--link--Color--dark--hover", "$pf-global--primary-color--200", "$pf-color-blue-500", "#004080"]
    }, {
      "property": "--pf-global--BackgroundColor--100",
      "value": "#fff",
      "token": "global_BackgroundColor_100",
      "values": ["--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-table--BackgroundColor",
      "value": "#fff",
      "token": "c_table_BackgroundColor",
      "values": ["--pf-global--BackgroundColor--100", "$pf-global--BackgroundColor--100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-table--BorderColor",
      "value": "#d2d2d2",
      "token": "c_table_BorderColor",
      "values": ["--pf-global--BorderColor--100", "$pf-global--BorderColor--100", "$pf-color-black-300", "#d2d2d2"]
    }, {
      "property": "--pf-c-table--BorderWidth",
      "value": "1px",
      "token": "c_table_BorderWidth",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-table--FontWeight",
      "value": "400",
      "token": "c_table_FontWeight",
      "values": ["--pf-global--FontWeight--normal", "$pf-global--FontWeight--normal", "400"]
    }, {
      "property": "--pf-c-table-caption--FontSize",
      "value": "0.875rem",
      "token": "c_table_caption_FontSize",
      "values": ["--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-table-caption--Color",
      "value": "#737679",
      "token": "c_table_caption_Color",
      "values": ["--pf-global--Color--200", "$pf-global--Color--200", "$pf-color-black-600", "#737679"]
    }, {
      "property": "--pf-c-table-caption--PaddingTop",
      "value": "1rem",
      "token": "c_table_caption_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-caption--PaddingRight",
      "value": "1.5rem",
      "token": "c_table_caption_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table-caption--md--PaddingRight",
      "value": "1rem",
      "token": "c_table_caption_md_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-caption--PaddingBottom",
      "value": "1rem",
      "token": "c_table_caption_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-caption--PaddingLeft",
      "value": "1.5rem",
      "token": "c_table_caption_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table-caption--md--PaddingLeft",
      "value": "1rem",
      "token": "c_table_caption_md_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-thead--FontSize",
      "value": "0.875rem",
      "token": "c_table_thead_FontSize",
      "values": ["--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-table-thead--FontWeight",
      "value": "600",
      "token": "c_table_thead_FontWeight",
      "values": ["--pf-global--FontWeight--bold", "$pf-global--FontWeight--bold", "600"]
    }, {
      "property": "--pf-c-table-thead-cell--PaddingTop",
      "value": "1rem",
      "token": "c_table_thead_cell_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-thead-cell--PaddingBottom",
      "value": "1rem",
      "token": "c_table_thead_cell_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-cell--hidden-visible--Display",
      "value": "table-cell",
      "token": "c_table_cell_hidden_visible_Display"
    }, {
      "property": "--pf-c-table-tbody-cell--PaddingTop",
      "value": "1.5rem",
      "token": "c_table_tbody_cell_PaddingTop",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table-tbody-cell--PaddingBottom",
      "value": "1.5rem",
      "token": "c_table_tbody_cell_PaddingBottom",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "1rem",
      "token": "c_table_cell_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "1rem",
      "token": "c_table_cell_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "1rem",
      "token": "c_table_cell_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "1rem",
      "token": "c_table_cell_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-cell--FontSize",
      "value": "1rem",
      "token": "c_table_cell_FontSize",
      "values": ["--pf-global--FontSize--md", "$pf-global--FontSize--md", "pf-font-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-cell--first-last-child--PaddingLeft",
      "value": "1.5rem",
      "token": "c_table_cell_first_last_child_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table-cell--first-last-child--PaddingRight",
      "value": "1.5rem",
      "token": "c_table_cell_first_last_child_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table__toggle--c-button--MarginTop",
      "value": "calc(0.375rem * -1)",
      "token": "c_table__toggle_c_button_MarginTop"
    }, {
      "property": "--pf-c-table__toggle--c-button__toggle-icon--Transform",
      "value": "rotate(270deg)",
      "token": "c_table__toggle_c_button__toggle_icon_Transform"
    }, {
      "property": "--pf-c-table__toggle--c-button__toggle-icon--Transition",
      "value": ".2s ease-in 0s",
      "token": "c_table__toggle_c_button__toggle_icon_Transition"
    }, {
      "property": "--pf-c-table__toggle--c-button--m-expanded__toggle-icon--Transform",
      "value": "rotate(360deg)",
      "token": "c_table__toggle_c_button_m_expanded__toggle_icon_Transform"
    }, {
      "property": "--pf-c-table--m-compact__toggle--PaddingTop",
      "value": "0",
      "token": "c_table_m_compact__toggle_PaddingTop"
    }, {
      "property": "--pf-c-table--m-compact__toggle--PaddingBottom",
      "value": "0",
      "token": "c_table_m_compact__toggle_PaddingBottom"
    }, {
      "property": "--pf-c-table__check--input--MarginTop",
      "value": "0.1875rem",
      "token": "c_table__check_input_MarginTop"
    }, {
      "property": "--pf-c-table__check--input--FontSize",
      "value": "1rem",
      "token": "c_table__check_input_FontSize",
      "values": ["--pf-global--FontSize--md", "$pf-global--FontSize--md", "pf-font-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table__action--PaddingTop",
      "value": "0",
      "token": "c_table__action_PaddingTop"
    }, {
      "property": "--pf-c-table__action--PaddingRight",
      "value": "0",
      "token": "c_table__action_PaddingRight"
    }, {
      "property": "--pf-c-table__action--PaddingBottom",
      "value": "0",
      "token": "c_table__action_PaddingBottom"
    }, {
      "property": "--pf-c-table__action--PaddingLeft",
      "value": "0",
      "token": "c_table__action_PaddingLeft"
    }, {
      "property": "--pf-c-table__inline-edit-action--PaddingTop",
      "value": "0",
      "token": "c_table__inline_edit_action_PaddingTop"
    }, {
      "property": "--pf-c-table__inline-edit-action--PaddingRight",
      "value": "0",
      "token": "c_table__inline_edit_action_PaddingRight"
    }, {
      "property": "--pf-c-table__inline-edit-action--PaddingBottom",
      "value": "0",
      "token": "c_table__inline_edit_action_PaddingBottom"
    }, {
      "property": "--pf-c-table__inline-edit-action--PaddingLeft",
      "value": "0",
      "token": "c_table__inline_edit_action_PaddingLeft"
    }, {
      "property": "--pf-c-table__expandable-row--Transition",
      "value": "all 250ms cubic-bezier(.42, 0, .58, 1)",
      "token": "c_table__expandable_row_Transition",
      "values": ["--pf-global--Transition", "$pf-global--Transition", "all 250ms cubic-bezier(.42, 0, .58, 1)"]
    }, {
      "property": "--pf-c-table__expandable-row--before--Width",
      "value": "3px",
      "token": "c_table__expandable_row_before_Width",
      "values": ["--pf-global--BorderWidth--lg", "$pf-global--BorderWidth--lg", "3px"]
    }, {
      "property": "--pf-c-table__expandable-row--before--BackgroundColor",
      "value": "#06c",
      "token": "c_table__expandable_row_before_BackgroundColor",
      "values": ["--pf-global--active-color--100", "$pf-global--active-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-table__expandable-row--before--ZIndex",
      "value": "200",
      "token": "c_table__expandable_row_before_ZIndex",
      "values": ["--pf-global--ZIndex--sm", "$pf-global--ZIndex--sm", "200"]
    }, {
      "property": "--pf-c-table__expandable-row--before--Top",
      "value": "calc(1px * -1)",
      "token": "c_table__expandable_row_before_Top",
      "values": ["calc(--pf-c-table--BorderWidth * -1)", "calc(--pf-global--BorderWidth--sm * -1)", "calc($pf-global--BorderWidth--sm * -1)", "calc(1px * -1)"]
    }, {
      "property": "--pf-c-table__expandable-row--before--Bottom",
      "value": "calc(1px * -1)",
      "token": "c_table__expandable_row_before_Bottom",
      "values": ["calc(--pf-c-table--BorderWidth * -1)", "calc(--pf-global--BorderWidth--sm * -1)", "calc($pf-global--BorderWidth--sm * -1)", "calc(1px * -1)"]
    }, {
      "property": "--pf-c-table__expandable-row--MaxHeight",
      "value": "28.125rem",
      "token": "c_table__expandable_row_MaxHeight"
    }, {
      "property": "--pf-c-table__expandable-row-content--Transition",
      "value": "all 250ms cubic-bezier(.42, 0, .58, 1)",
      "token": "c_table__expandable_row_content_Transition",
      "values": ["--pf-global--Transition", "$pf-global--Transition", "all 250ms cubic-bezier(.42, 0, .58, 1)"]
    }, {
      "property": "--pf-c-table__expandable-row-content--PaddingTop",
      "value": "1.5rem",
      "token": "c_table__expandable_row_content_PaddingTop",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table__expandable-row-content--PaddingBottom",
      "value": "1.5rem",
      "token": "c_table__expandable_row_content_PaddingBottom",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table__sort-indicator--MarginLeft",
      "value": "1rem",
      "token": "c_table__sort_indicator_MarginLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table__sort-indicator--Color",
      "value": "#d2d2d2",
      "token": "c_table__sort_indicator_Color",
      "values": ["--pf-global--disabled-color--200", "$pf-global--disabled-color--200", "$pf-color-black-300", "#d2d2d2"]
    }, {
      "property": "--pf-c-table__sort-indicator--hover--Color",
      "value": "#151515",
      "token": "c_table__sort_indicator_hover_Color",
      "values": ["--pf-global--Color--100", "$pf-global--Color--100", "$pf-color-black-900", "#151515"]
    }, {
      "property": "--pf-c-table__sort--c-button--Color",
      "value": "#151515",
      "token": "c_table__sort_c_button_Color",
      "values": ["--pf-global--Color--100", "$pf-global--Color--100", "$pf-color-black-900", "#151515"]
    }, {
      "property": "--pf-c-table__sort-indicator--LineHeight",
      "value": "1.5",
      "token": "c_table__sort_indicator_LineHeight",
      "values": ["--pf-c-button--LineHeight", "--pf-global--LineHeight--md", "$pf-global--LineHeight--md", "1.5"]
    }, {
      "property": "--pf-c-table__icon-inline--MarginRight",
      "value": "0.5rem",
      "token": "c_table__icon_inline_MarginRight",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-table--nested--first-last-child--PaddingRight",
      "value": "4rem",
      "token": "c_table_nested_first_last_child_PaddingRight",
      "values": ["--pf-global--spacer--3xl", "$pf-global--spacer--3xl", "pf-size-prem(64px)", "4rem"]
    }, {
      "property": "--pf-c-table--nested--first-last-child--PaddingLeft",
      "value": "4rem",
      "token": "c_table_nested_first_last_child_PaddingLeft",
      "values": ["--pf-global--spacer--3xl", "$pf-global--spacer--3xl", "pf-size-prem(64px)", "4rem"]
    }, {
      "property": "--pf-c-table--m-compact-th--PaddingTop",
      "value": "1rem",
      "token": "c_table_m_compact_th_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table--m-compact-th--PaddingBottom",
      "value": "1rem",
      "token": "c_table_m_compact_th_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table--m-compact-cell--PaddingTop",
      "value": "0.5rem",
      "token": "c_table_m_compact_cell_PaddingTop",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-table--m-compact-cell--PaddingRight",
      "value": "0.5rem",
      "token": "c_table_m_compact_cell_PaddingRight",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-table--m-compact-cell--PaddingBottom",
      "value": "0.5rem",
      "token": "c_table_m_compact_cell_PaddingBottom",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-table--m-compact-cell--PaddingLeft",
      "value": "0.5rem",
      "token": "c_table_m_compact_cell_PaddingLeft",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-table--m-compact-cell--first-last-child--PaddingLeft",
      "value": "1.5rem",
      "token": "c_table_m_compact_cell_first_last_child_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table--m-compact-cell--first-last-child--PaddingRight",
      "value": "1.5rem",
      "token": "c_table_m_compact_cell_first_last_child_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table--m-compact--FontSize",
      "value": "0.875rem",
      "token": "c_table_m_compact_FontSize",
      "values": ["--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-table--m-compact__expandable-row-content--PaddingTop",
      "value": "1.5rem",
      "token": "c_table_m_compact__expandable_row_content_PaddingTop",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table--m-compact__expandable-row-content--PaddingRight",
      "value": "1.5rem",
      "token": "c_table_m_compact__expandable_row_content_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table--m-compact__expandable-row-content--PaddingBottom",
      "value": "1.5rem",
      "token": "c_table_m_compact__expandable_row_content_PaddingBottom",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table--m-compact__expandable-row-content--PaddingLeft",
      "value": "1.5rem",
      "token": "c_table_m_compact__expandable_row_content_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table__compound-expansion-toggle--BorderTop--BorderWidth",
      "value": "3px",
      "token": "c_table__compound_expansion_toggle_BorderTop_BorderWidth",
      "values": ["--pf-global--BorderWidth--lg", "$pf-global--BorderWidth--lg", "3px"]
    }, {
      "property": "--pf-c-table__compound-expansion-toggle--BorderTop--BorderColor",
      "value": "#06c",
      "token": "c_table__compound_expansion_toggle_BorderTop_BorderColor",
      "values": ["--pf-global--primary-color--100", "$pf-global--primary-color--100", "$pf-color-blue-400", "#06c"]
    }, {
      "property": "--pf-c-table__compound-expansion-toggle--BorderRight--BorderWidth",
      "value": "1px",
      "token": "c_table__compound_expansion_toggle_BorderRight_BorderWidth",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-table__compound-expansion-toggle--BorderLeft--BorderWidth",
      "value": "1px",
      "token": "c_table__compound_expansion_toggle_BorderLeft_BorderWidth",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-table__compound-expansion-toggle--BorderRight--BorderColor",
      "value": "#d2d2d2",
      "token": "c_table__compound_expansion_toggle_BorderRight_BorderColor",
      "values": ["--pf-global--BorderColor--100", "$pf-global--BorderColor--100", "$pf-color-black-300", "#d2d2d2"]
    }, {
      "property": "--pf-c-table__compound-expansion-toggle--BorderLeft--BorderColor",
      "value": "#d2d2d2",
      "token": "c_table__compound_expansion_toggle_BorderLeft_BorderColor",
      "values": ["--pf-global--BorderColor--100", "$pf-global--BorderColor--100", "$pf-color-black-300", "#d2d2d2"]
    }, {
      "property": "--pf-c-table__compound-expansion-toggle--BorderBottom--BorderWidth",
      "value": "1px",
      "token": "c_table__compound_expansion_toggle_BorderBottom_BorderWidth",
      "values": ["--pf-global--BorderWidth--sm", "$pf-global--BorderWidth--sm", "1px"]
    }, {
      "property": "--pf-c-table__compound-expansion-toggle--BorderBottom--BorderColor",
      "value": "#fff",
      "token": "c_table__compound_expansion_toggle_BorderBottom_BorderColor",
      "values": ["--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-table__expandable-row--m-expanded--BoxShadow",
      "value": "0 0.3125rem 0.625rem -0.25rem rgba(3, 3, 3, 0.25)",
      "token": "c_table__expandable_row_m_expanded_BoxShadow",
      "values": ["--pf-global--BoxShadow--md-bottom", "$pf-global--BoxShadow--md-bottom", "0 pf-size-prem(5) pf-size-prem(10) pf-size-prem(-4) rgba($pf-color-black-1000, .25)", "0 pf-size-prem(5) pf-size-prem(10) pf-size-prem(-4) rgba(#030303, .25)", "0 0.3125rem 0.625rem -0.25rem rgba(3, 3, 3, 0.25)"]
    }, {
      "property": "--pf-c-table__expandable-row--m-expanded--BorderBottomColor",
      "value": "#fff",
      "token": "c_table__expandable_row_m_expanded_BorderBottomColor",
      "values": ["--pf-global--BackgroundColor--light-100", "$pf-global--BackgroundColor--light-100", "$pf-color-white", "#fff"]
    }, {
      "property": "--pf-c-table__sort--sorted--Color",
      "value": "#06c",
      "token": "c_table__sort_sorted_Color",
      "values": ["--pf-global--active-color--100", "$pf-global--active-color--100", "$pf-color-blue-400", "#06c"]
    }],
    ".pf-c-table tr > *": [{
      "property": "--pf-hidden-visible--visible--Visibility",
      "value": "visible",
      "token": "hidden_visible_visible_Visibility"
    }, {
      "property": "--pf-hidden-visible--hidden--Display",
      "value": "none",
      "token": "hidden_visible_hidden_Display"
    }, {
      "property": "--pf-hidden-visible--hidden--Visibility",
      "value": "hidden",
      "token": "hidden_visible_hidden_Visibility"
    }, {
      "property": "--pf-hidden-visible--Display",
      "value": "table-cell",
      "token": "hidden_visible_Display",
      "values": ["--pf-hidden-visible--visible--Display", "--pf-c-table-cell--hidden-visible--Display", "table-cell"]
    }, {
      "property": "--pf-hidden-visible--Visibility",
      "value": "visible",
      "token": "hidden_visible_Visibility",
      "values": ["--pf-hidden-visible--visible--Visibility", "visible"]
    }, {
      "property": "--pf-hidden-visible--visible--Display",
      "value": "table-cell",
      "token": "hidden_visible_visible_Display",
      "values": ["--pf-c-table-cell--hidden-visible--Display", "table-cell"]
    }],
    ".pf-c-table tr > .pf-m-hidden": [{
      "property": "--pf-hidden-visible--Display",
      "value": "none",
      "token": "hidden_visible_Display",
      "values": ["--pf-hidden-visible--hidden--Display", "none"]
    }, {
      "property": "--pf-hidden-visible--Visibility",
      "value": "hidden",
      "token": "hidden_visible_Visibility",
      "values": ["--pf-hidden-visible--hidden--Visibility", "hidden"]
    }],
    ".pf-c-table tr > *:first-child": [{
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "1.5rem",
      "token": "c_table_cell_PaddingLeft",
      "values": ["--pf-c-table-cell--first-last-child--PaddingLeft", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }],
    ".pf-c-table tr > *:last-child": [{
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "1.5rem",
      "token": "c_table_cell_PaddingRight",
      "values": ["--pf-c-table-cell--first-last-child--PaddingRight", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }],
    ".pf-c-table thead": [{
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "1rem",
      "token": "c_table_cell_PaddingTop",
      "values": ["--pf-c-table-thead-cell--PaddingTop", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "1rem",
      "token": "c_table_cell_PaddingBottom",
      "values": ["--pf-c-table-thead-cell--PaddingBottom", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table-cell--FontSize",
      "value": "0.875rem",
      "token": "c_table_cell_FontSize",
      "values": ["--pf-c-table-thead--FontSize", "--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-table--FontWeight",
      "value": "600",
      "token": "c_table_FontWeight",
      "values": ["--pf-c-table-thead--FontWeight", "--pf-global--FontWeight--bold", "$pf-global--FontWeight--bold", "600"]
    }],
    ".pf-c-table tbody": [{
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "1.5rem",
      "token": "c_table_cell_PaddingTop",
      "values": ["--pf-c-table-tbody-cell--PaddingTop", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "1.5rem",
      "token": "c_table_cell_PaddingBottom",
      "values": ["--pf-c-table-tbody-cell--PaddingBottom", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }],
    ".pf-c-table .pf-c-table__toggle": [{
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "0",
      "token": "c_table_cell_PaddingBottom"
    }],
    ".pf-c-table__toggle": [{
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "0",
      "token": "c_table_cell_PaddingRight"
    }, {
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "0",
      "token": "c_table_cell_PaddingLeft"
    }],
    ".pf-c-table__check": [{
      "property": "--pf-c-table-cell--FontSize",
      "value": "1rem",
      "token": "c_table_cell_FontSize",
      "values": ["--pf-c-table__check--input--FontSize", "--pf-global--FontSize--md", "$pf-global--FontSize--md", "pf-font-prem(16px)", "1rem"]
    }],
    ".pf-c-table__action": [{
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "0",
      "token": "c_table_cell_PaddingTop"
    }, {
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "0",
      "token": "c_table_cell_PaddingRight",
      "values": ["--pf-c-table__action--PaddingRight", "0"]
    }, {
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "0",
      "token": "c_table_cell_PaddingBottom"
    }, {
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "0",
      "token": "c_table_cell_PaddingLeft",
      "values": ["--pf-c-table__action--PaddingLeft", "0"]
    }],
    ".pf-c-table__inline-edit-action": [{
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "0",
      "token": "c_table_cell_PaddingLeft"
    }, {
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "0",
      "token": "c_table_cell_PaddingRight"
    }],
    ".pf-c-table__compound-expansion-toggle .pf-c-button": [{
      "property": "--pf-c-button--BorderRadius",
      "value": "0",
      "token": "c_button_BorderRadius"
    }],
    ".pf-c-table__expandable-row": [{
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "0",
      "token": "c_table_cell_PaddingTop"
    }, {
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "0",
      "token": "c_table_cell_PaddingBottom"
    }],
    ".pf-c-table .pf-c-table tr > *:first-child": [{
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "4rem",
      "token": "c_table_cell_PaddingLeft",
      "values": ["--pf-c-table--nested--first-last-child--PaddingLeft", "--pf-global--spacer--3xl", "$pf-global--spacer--3xl", "pf-size-prem(64px)", "4rem"]
    }],
    ".pf-c-table .pf-c-table tr > *:last-child": [{
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "4rem",
      "token": "c_table_cell_PaddingRight",
      "values": ["--pf-c-table--nested--first-last-child--PaddingRight", "--pf-global--spacer--3xl", "$pf-global--spacer--3xl", "pf-size-prem(64px)", "4rem"]
    }],
    ".pf-c-table.pf-m-compact": [{
      "property": "--pf-c-table-cell--FontSize",
      "value": "0.875rem",
      "token": "c_table_cell_FontSize",
      "values": ["--pf-c-table--m-compact--FontSize", "--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }],
    ".pf-c-table.pf-m-compact.pf-m-no-border-rows:not(.pf-m-expandable) tbody": [{
      "property": "--pf-c-table--BorderWidth",
      "value": "0",
      "token": "c_table_BorderWidth"
    }, {
      "property": "--pf-c-table--BorderColor",
      "value": "transparent",
      "token": "c_table_BorderColor"
    }],
    ".pf-c-table.pf-m-compact.pf-m-no-border-rows:not(.pf-m-expandable) tbody tr:first-child > *": [{
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "calc(0.5rem * 2)",
      "token": "c_table_cell_PaddingTop",
      "values": ["calc(--pf-c-table--m-compact-cell--PaddingTop * 2)", "calc(--pf-global--spacer--sm * 2)", "calc($pf-global--spacer--sm * 2)", "calc(pf-size-prem(8px) * 2)", "calc(0.5rem * 2)"]
    }],
    ".pf-c-table.pf-m-compact.pf-m-no-border-rows:not(.pf-m-expandable) tbody tr:last-child > *": [{
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "calc(0.5rem * 2)",
      "token": "c_table_cell_PaddingBottom",
      "values": ["calc(--pf-c-table--m-compact-cell--PaddingBottom * 2)", "calc(--pf-global--spacer--sm * 2)", "calc($pf-global--spacer--sm * 2)", "calc(pf-size-prem(8px) * 2)", "calc(0.5rem * 2)"]
    }],
    ".pf-c-table.pf-m-compact tr": [{
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "0.5rem",
      "token": "c_table_cell_PaddingLeft",
      "values": ["--pf-c-table--m-compact-cell--PaddingLeft", "--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "0.5rem",
      "token": "c_table_cell_PaddingRight",
      "values": ["--pf-c-table--m-compact-cell--PaddingRight", "--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }],
    ".pf-c-table.pf-m-compact tr:not(.pf-c-table__expandable-row)": [{
      "property": "--pf-c-table-cell--FontSize",
      "value": "0.875rem",
      "token": "c_table_cell_FontSize",
      "values": ["--pf-c-table--m-compact--FontSize", "--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "0.5rem",
      "token": "c_table_cell_PaddingTop",
      "values": ["--pf-c-table--m-compact-cell--PaddingTop", "--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "0.5rem",
      "token": "c_table_cell_PaddingBottom",
      "values": ["--pf-c-table--m-compact-cell--PaddingBottom", "--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }],
    ".pf-c-table.pf-m-compact tr:not(.pf-c-table__expandable-row) > *:first-child": [{
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "1.5rem",
      "token": "c_table_cell_PaddingLeft",
      "values": ["--pf-c-table--m-compact-cell--first-last-child--PaddingLeft", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }],
    ".pf-c-table.pf-m-compact tr:not(.pf-c-table__expandable-row) > *:last-child": [{
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "1.5rem",
      "token": "c_table_cell_PaddingRight",
      "values": ["--pf-c-table--m-compact-cell--first-last-child--PaddingRight", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }],
    ".pf-c-table.pf-m-compact thead": [{
      "property": "--pf-c-table--m-compact-cell--PaddingTop",
      "value": "1rem",
      "token": "c_table_m_compact_cell_PaddingTop",
      "values": ["--pf-c-table--m-compact-th--PaddingTop", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-table--m-compact-cell--PaddingBottom",
      "value": "1rem",
      "token": "c_table_m_compact_cell_PaddingBottom",
      "values": ["--pf-c-table--m-compact-th--PaddingBottom", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }],
    ".pf-c-table.pf-m-compact .pf-c-table__action": [{
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "0",
      "token": "c_table_cell_PaddingTop",
      "values": ["--pf-c-table__action--PaddingTop", "0"]
    }, {
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "0",
      "token": "c_table_cell_PaddingBottom",
      "values": ["--pf-c-table__action--PaddingBottom", "0"]
    }, {
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "0",
      "token": "c_table_cell_PaddingLeft",
      "values": ["--pf-c-table__action--PaddingLeft", "0"]
    }],
    ".pf-c-table.pf-m-compact .pf-c-table__toggle": [{
      "property": "--pf-c-table-cell--PaddingTop",
      "value": "0",
      "token": "c_table_cell_PaddingTop",
      "values": ["--pf-c-table--m-compact__toggle--PaddingTop", "0"]
    }, {
      "property": "--pf-c-table-cell--PaddingBottom",
      "value": "0",
      "token": "c_table_cell_PaddingBottom",
      "values": ["--pf-c-table--m-compact__toggle--PaddingBottom", "0"]
    }],
    ".pf-c-table .pf-c-table.pf-m-compact tr > *:first-child": [{
      "property": "--pf-c-table-cell--PaddingLeft",
      "value": "4rem",
      "token": "c_table_cell_PaddingLeft",
      "values": ["--pf-c-table--nested--first-last-child--PaddingLeft", "--pf-global--spacer--3xl", "$pf-global--spacer--3xl", "pf-size-prem(64px)", "4rem"]
    }],
    ".pf-c-table .pf-c-table.pf-m-compact tr > *:last-child": [{
      "property": "--pf-c-table-cell--PaddingRight",
      "value": "4rem",
      "token": "c_table_cell_PaddingRight",
      "values": ["--pf-c-table--nested--first-last-child--PaddingRight", "--pf-global--spacer--3xl", "$pf-global--spacer--3xl", "pf-size-prem(64px)", "4rem"]
    }],
    ".pf-c-table.pf-m-compact .pf-c-table__expandable-row-content": [{
      "property": "--pf-c-table__expandable-row-content--PaddingTop",
      "value": "1.5rem",
      "token": "c_table__expandable_row_content_PaddingTop",
      "values": ["--pf-c-table--m-compact__expandable-row-content--PaddingTop", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-table__expandable-row-content--PaddingBottom",
      "value": "1.5rem",
      "token": "c_table__expandable_row_content_PaddingBottom",
      "values": ["--pf-c-table--m-compact__expandable-row-content--PaddingBottom", "--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }]
  };
});