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
    ".pf-c-toolbar": [{
      "property": "--pf-c-toolbar--PaddingTop",
      "value": "1rem",
      "token": "c_toolbar_PaddingTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar--PaddingRight",
      "value": "1.5rem",
      "token": "c_toolbar_PaddingRight",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-toolbar--PaddingBottom",
      "value": "1rem",
      "token": "c_toolbar_PaddingBottom",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar--PaddingLeft",
      "value": "1.5rem",
      "token": "c_toolbar_PaddingLeft",
      "values": ["--pf-global--spacer--lg", "$pf-global--spacer--lg", "pf-size-prem(24px)", "1.5rem"]
    }, {
      "property": "--pf-c-toolbar--md--PaddingRight",
      "value": "1rem",
      "token": "c_toolbar_md_PaddingRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar--md--PaddingLeft",
      "value": "1rem",
      "token": "c_toolbar_md_PaddingLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar--child--MarginLeft",
      "value": "1rem",
      "token": "c_toolbar_child_MarginLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar__bulk-select--MarginRight",
      "value": "1rem",
      "token": "c_toolbar__bulk_select_MarginRight",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar__filter--MarginTop",
      "value": "calc(1rem + 1rem)",
      "token": "c_toolbar__filter_MarginTop",
      "values": ["calc(--pf-c-toolbar--PaddingBottom + --pf-global--spacer--md)", "calc(--pf-global--spacer--md + $pf-global--spacer--md)", "calc($pf-global--spacer--md + $pf-global--spacer--md)", "calc(pf-size-prem(16px) + pf-size-prem(16px))", "calc(1rem + 1rem)"]
    }, {
      "property": "--pf-c-toolbar__filter--MarginLeft",
      "value": "0",
      "token": "c_toolbar__filter_MarginLeft"
    }, {
      "property": "--pf-c-toolbar__filter-toggle--MarginLeft",
      "value": "1rem",
      "token": "c_toolbar__filter_toggle_MarginLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar__filter-toggle--m-expanded--Color",
      "value": "#151515",
      "token": "c_toolbar__filter_toggle_m_expanded_Color",
      "values": ["--pf-global--Color--100", "$pf-global--Color--100", "$pf-color-black-900", "#151515"]
    }, {
      "property": "--pf-c-toolbar__sort--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar__sort_MarginLeft",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-toolbar__action-group--child--MarginLeft",
      "value": "1rem",
      "token": "c_toolbar__action_group_child_MarginLeft",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar__sort--action-group--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar__sort_action_group_MarginLeft",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-toolbar__filter--action-group--MarginLeft",
      "value": "2rem",
      "token": "c_toolbar__filter_action_group_MarginLeft",
      "values": ["--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }, {
      "property": "--pf-c-toolbar__action-list--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar__action_list_MarginLeft",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-toolbar__sort--action-list--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar__sort_action_list_MarginLeft",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }, {
      "property": "--pf-c-toolbar__total-items--FontSize",
      "value": "0.875rem",
      "token": "c_toolbar__total_items_FontSize",
      "values": ["--pf-global--FontSize--sm", "$pf-global--FontSize--sm", "pf-font-prem(14px)", "0.875rem"]
    }, {
      "property": "--pf-c-toolbar__filter-list--MarginTop",
      "value": "1rem",
      "token": "c_toolbar__filter_list_MarginTop",
      "values": ["--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }, {
      "property": "--pf-c-toolbar__filter-list--c-button--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar__filter_list_c_button_MarginLeft",
      "values": ["--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }],
    ".pf-c-toolbar__filter-toggle": [{
      "property": "--pf-c-toolbar--child--MarginLeft",
      "value": "1rem",
      "token": "c_toolbar_child_MarginLeft",
      "values": ["--pf-c-toolbar__filter-toggle--MarginLeft", "--pf-global--spacer--md", "$pf-global--spacer--md", "pf-size-prem(16px)", "1rem"]
    }],
    ".pf-c-toolbar__filter + .pf-c-toolbar__action-group": [{
      "property": "--pf-c-toolbar--child--MarginLeft",
      "value": "2rem",
      "token": "c_toolbar_child_MarginLeft",
      "values": ["--pf-c-toolbar__filter--action-group--MarginLeft", "--pf-global--spacer--xl", "$pf-global--spacer--xl", "pf-size-prem(32px)", "2rem"]
    }],
    ".pf-c-toolbar__sort": [{
      "property": "--pf-c-toolbar--child--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar_child_MarginLeft",
      "values": ["--pf-c-toolbar__sort--MarginLeft", "--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }],
    ".pf-c-toolbar__sort + .pf-c-toolbar__action-group": [{
      "property": "--pf-c-toolbar--child--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar_child_MarginLeft",
      "values": ["--pf-c-toolbar__sort--action-group--MarginLeft", "--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }],
    ".pf-c-toolbar__sort + .pf-c-toolbar__action-list": [{
      "property": "--pf-c-toolbar--child--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar_child_MarginLeft",
      "values": ["--pf-c-toolbar__sort--action-list--MarginLeft", "--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }],
    ".pf-c-toolbar__action-list": [{
      "property": "--pf-c-toolbar--child--MarginLeft",
      "value": "0.5rem",
      "token": "c_toolbar_child_MarginLeft",
      "values": ["--pf-c-toolbar__action-list--MarginLeft", "--pf-global--spacer--sm", "$pf-global--spacer--sm", "pf-size-prem(8px)", "0.5rem"]
    }]
  };
});