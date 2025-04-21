export const c_table_tree_view: {
  ".pf-c-table": {
    "c_table__tree_view_main_indent_base": {
      "name": "--pf-c-table__tree-view-main--indent--base",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_table__tree_view_main_nested_indent_base": {
      "name": "--pf-c-table__tree-view-main--nested-indent--base",
      "value": "calc(calc(1rem * 2 + 1rem) - 1rem)",
      "values": [
        "calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md)",
        "calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md)",
        "calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px))",
        "calc(calc(1rem * 2 + 1rem) - 1rem)"
      ]
    },
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-table__tree-view-main--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_table__tree_view_main_MarginLeft": {
      "name": "--pf-c-table__tree-view-main--MarginLeft",
      "value": "calc(1rem * -1)",
      "values": [
        "calc(--pf-c-table--cell--PaddingLeft * -1)",
        "calc(--pf-c-table--cell--Padding--base * -1)",
        "calc(--pf-global--spacer--md * -1)",
        "calc($pf-global--spacer--md * -1)",
        "calc(pf-size-prem(16px) * -1)",
        "calc(1rem * -1)"
      ]
    },
    "c_table__tree_view_main_c_table__check_PaddingRight": {
      "name": "--pf-c-table__tree-view-main--c-table__check--PaddingRight",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_table__tree_view_main_c_table__check_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--c-table__check--PaddingLeft",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_table__tree_view_main_c_table__check_MarginRight": {
      "name": "--pf-c-table__tree-view-main--c-table__check--MarginRight",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_table__tree_view_icon_MinWidth": {
      "name": "--pf-c-table__tree-view-icon--MinWidth",
      "value": "1rem",
      "values": [
        "--pf-global--FontSize--md",
        "$pf-global--FontSize--md",
        "pf-font-prem(16px)",
        "1rem"
      ]
    },
    "c_table__tree_view_icon_MarginRight": {
      "name": "--pf-c-table__tree-view-icon--MarginRight",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_table_m_tree_view__toggle_Position": {
      "name": "--pf-c-table--m-tree-view__toggle--Position",
      "value": "absolute"
    },
    "c_table_m_tree_view__toggle_Left": {
      "name": "--pf-c-table--m-tree-view__toggle--Left",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-table__tree-view-main--PaddingLeft",
        "--pf-c-table__tree-view-main--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_table_m_tree_view__toggle_TranslateX": {
      "name": "--pf-c-table--m-tree-view__toggle--TranslateX",
      "value": "-100%"
    },
    "c_table_m_tree_view__toggle__toggle_icon_MinWidth": {
      "name": "--pf-c-table--m-tree-view__toggle__toggle-icon--MinWidth",
      "value": "1rem",
      "values": [
        "--pf-global--FontSize--md",
        "$pf-global--FontSize--md",
        "pf-font-prem(16px)",
        "1rem"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view > tbody > tr": {
    "c_table_m_tree_view__toggle_Left": {
      "name": "--pf-c-table--m-tree-view__toggle--Left",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-table__tree-view-main--PaddingLeft",
        "--pf-c-table__tree-view-main--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"2\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 1 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"3\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 2 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"4\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 3 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"5\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 4 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"6\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 5 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"7\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 6 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"8\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 7 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"9\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 8 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-table.pf-m-tree-view tr[aria-level=\"10\"]": {
    "c_table__tree_view_main_PaddingLeft": {
      "name": "--pf-c-table__tree-view-main--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 9 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table": {
    "c_table_m_tree_view_grid_tr_OutlineOffset": {
      "name": "--pf-c-table--m-tree-view-grid--tr--OutlineOffset",
      "value": "calc(-1 * 0.25rem)",
      "values": [
        "calc(-1 * --pf-global--spacer--xs)",
        "calc(-1 * $pf-global--spacer--xs)",
        "calc(-1 * pf-size-prem(4px))",
        "calc(-1 * 0.25rem)"
      ]
    },
    "c_table_m_tree_view_grid_tbody_cell_PaddingTop": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingTop",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_table_m_tree_view_grid_tbody_cell_PaddingBottom": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingBottom",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-table__tree-view-main--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_table_m_tree_view_grid_tbody_cell_GridColumnGap": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--GridColumnGap",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_table_m_tree_view_grid_c_table__action_PaddingTop": {
      "name": "--pf-c-table--m-tree-view-grid--c-table__action--PaddingTop",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_table_m_tree_view_grid_c_table__action_PaddingBottom": {
      "name": "--pf-c-table--m-tree-view-grid--c-table__action--PaddingBottom",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_table_m_tree_view_grid_c_table__action_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--c-table__action--PaddingLeft",
      "value": "0"
    },
    "c_table_m_tree_view_grid__tr_expanded__tree_view_title_cell_action_PaddingTop": {
      "name": "--pf-c-table--m-tree-view-grid__tr--expanded__tree-view-title-cell--action--PaddingTop",
      "value": "2rem",
      "values": [
        "--pf-global--spacer--xl",
        "$pf-global--spacer--xl",
        "pf-size-prem(32px)",
        "2rem"
      ]
    },
    "c_table_m_tree_view_grid_m_tree_view_details_expanded_PaddingBottom": {
      "name": "--pf-c-table--m-tree-view-grid--m-tree-view-details-expanded--PaddingBottom",
      "value": "2rem",
      "values": [
        "--pf-global--spacer--xl",
        "$pf-global--spacer--xl",
        "pf-size-prem(32px)",
        "2rem"
      ]
    },
    "c_table_m_tree_view_grid__tr_expanded__tree_view_title_cell_PaddingTop": {
      "name": "--pf-c-table--m-tree-view-grid__tr--expanded__tree-view-title-cell--PaddingTop",
      "value": "2rem",
      "values": [
        "--pf-global--spacer--xl",
        "$pf-global--spacer--xl",
        "pf-size-prem(32px)",
        "2rem"
      ]
    },
    "c_table_m_tree_view_grid_td_data_label_GridTemplateColumns": {
      "name": "--pf-c-table--m-tree-view-grid--td--data-label--GridTemplateColumns",
      "value": "repeat(auto-fit, minmax(150px, 1fr))"
    },
    "c_table_m_tree_view_grid_td_not_c_table__tree_view_title_cell_PaddingTop": {
      "name": "--pf-c-table--m-tree-view-grid--td--not--c-table__tree-view-title-cell--PaddingTop",
      "value": "0.25rem",
      "values": [
        "--pf-global--spacer--xs",
        "$pf-global--spacer--xs",
        "pf-size-prem(4px)",
        "0.25rem"
      ]
    },
    "c_table_m_tree_view_grid_td_not_c_table__tree_view_title_cell_PaddingBottom": {
      "name": "--pf-c-table--m-tree-view-grid--td--not--c-table__tree-view-title-cell--PaddingBottom",
      "value": "0.25rem",
      "values": [
        "--pf-global--spacer--xs",
        "$pf-global--spacer--xs",
        "pf-size-prem(4px)",
        "0.25rem"
      ]
    },
    "c_table_m_tree_view_mobile__tree_view_main_c_table__check_MarginRight": {
      "name": "--pf-c-table--m-tree-view-mobile__tree-view-main--c-table__check--MarginRight",
      "value": "0"
    },
    "c_table_m_tree_view_mobile__tree_view_main_c_table__check_Order": {
      "name": "--pf-c-table--m-tree-view-mobile__tree-view-main--c-table__check--Order",
      "value": "4"
    },
    "c_table__tree_view_text_PaddingRight": {
      "name": "--pf-c-table__tree-view-text--PaddingRight",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_table_tbody_cell_PaddingTop": {
      "name": "--pf-c-table--tbody--cell--PaddingTop",
      "value": "1rem",
      "values": [
        "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingTop",
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_table_tbody_cell_PaddingBottom": {
      "name": "--pf-c-table--tbody--cell--PaddingBottom",
      "value": "1rem",
      "values": [
        "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingBottom",
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_table__tree_view_details_toggle_MarginTop": {
      "name": "--pf-c-table__tree-view-details-toggle--MarginTop",
      "value": "calc(0.375rem * -1)"
    },
    "c_table__tree_view_details_toggle_MarginBottom": {
      "name": "--pf-c-table__tree-view-details-toggle--MarginBottom",
      "value": "calc(0.375rem * -1)"
    },
    "c_table_m_tree_view_grid_c_dropdown_MarginTop": {
      "name": "--pf-c-table--m-tree-view-grid--c-dropdown--MarginTop",
      "value": "calc(0.375rem * -1)"
    },
    "c_table_m_tree_view_grid_c_dropdown_MarginBottom": {
      "name": "--pf-c-table--m-tree-view-grid--c-dropdown--MarginBottom",
      "value": "calc(0.375rem * -1)"
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-expanded] .pf-c-table__tree-view-title-cell": {
    "c_table_cell_PaddingTop": {
      "name": "--pf-c-table--cell--PaddingTop",
      "value": "2rem",
      "values": [
        "--pf-c-table--m-tree-view-grid__tr--expanded__tree-view-title-cell--PaddingTop",
        "--pf-global--spacer--xl",
        "$pf-global--spacer--xl",
        "pf-size-prem(32px)",
        "2rem"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-expanded] .pf-c-table__tree-view-title-cell ~ .pf-c-table__action": {
    "c_table_cell_PaddingTop": {
      "name": "--pf-c-table--cell--PaddingTop",
      "value": "2rem",
      "values": [
        "--pf-c-table--m-tree-view-grid__tr--expanded__tree-view-title-cell--action--PaddingTop",
        "--pf-global--spacer--xl",
        "$pf-global--spacer--xl",
        "pf-size-prem(32px)",
        "2rem"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table td:not(.pf-c-table__tree-view-title-cell)": {
    "c_table_cell_PaddingTop": {
      "name": "--pf-c-table--cell--PaddingTop",
      "value": "0.25rem",
      "values": [
        "--pf-c-table--m-tree-view-grid--td--not--c-table__tree-view-title-cell--PaddingTop",
        "--pf-global--spacer--xs",
        "$pf-global--spacer--xs",
        "pf-size-prem(4px)",
        "0.25rem"
      ]
    },
    "c_table_cell_PaddingBottom": {
      "name": "--pf-c-table--cell--PaddingBottom",
      "value": "0.25rem",
      "values": [
        "--pf-c-table--m-tree-view-grid--td--not--c-table__tree-view-title-cell--PaddingBottom",
        "--pf-global--spacer--xs",
        "$pf-global--spacer--xs",
        "pf-size-prem(4px)",
        "0.25rem"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table .pf-c-table__action": {
    "c_table_cell_Width": {
      "name": "--pf-c-table--cell--Width",
      "value": "auto"
    },
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "0",
      "values": [
        "--pf-c-table--m-tree-view-grid--c-table__action--PaddingLeft",
        "0"
      ]
    },
    "c_table_m_tree_view_grid_td_not_c_table__tree_view_title_cell_PaddingTop": {
      "name": "--pf-c-table--m-tree-view-grid--td--not--c-table__tree-view-title-cell--PaddingTop",
      "value": "1rem",
      "values": [
        "--pf-c-table--m-tree-view-grid--c-table__action--PaddingTop",
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_table_m_tree_view_grid_td_not_c_table__tree_view_title_cell_PaddingBottom": {
      "name": "--pf-c-table--m-tree-view-grid--td--not--c-table__tree-view-title-cell--PaddingBottom",
      "value": "1rem",
      "values": [
        "--pf-c-table--m-tree-view-grid--c-table__action--PaddingBottom",
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"2\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 1 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"3\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 2 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"4\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 3 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"5\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 4 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"6\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 5 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"7\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 6 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"8\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 7 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"9\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 8 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-m-tree-view-grid.pf-c-table tr[aria-level=\"10\"]": {
    "c_table_m_tree_view_grid_tbody_cell_PaddingLeft": {
      "name": "--pf-c-table--m-tree-view-grid--tbody--cell--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-table__tree-view-main--nested-indent--base * 9 + --pf-c-table__tree-view-main--indent--base)",
        "calc(calc(--pf-c-table__tree-view-main--indent--base - --pf-global--spacer--md) * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-table__tree-view-icon--MinWidth) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem))"
      ]
    }
  }
};
export default c_table_tree_view;