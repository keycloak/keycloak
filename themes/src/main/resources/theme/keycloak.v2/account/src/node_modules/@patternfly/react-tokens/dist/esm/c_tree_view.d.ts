export const c_tree_view: {
  ".pf-c-tree-view": {
    "c_tree_view_PaddingTop": {
      "name": "--pf-c-tree-view--PaddingTop",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view_PaddingBottom": {
      "name": "--pf-c-tree-view--PaddingBottom",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_indent_base": {
      "name": "--pf-c-tree-view__node--indent--base",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_tree_view__node_nested_indent_base": {
      "name": "--pf-c-tree-view__node--nested-indent--base",
      "value": "calc(calc(1rem * 2 + 1rem) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md)",
        "calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md)",
        "calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px))",
        "calc(calc(1rem * 2 + 1rem) - 1rem)"
      ]
    },
    "c_tree_view__node_PaddingTop_base": {
      "name": "--pf-c-tree-view__node--PaddingTop--base",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_PaddingTop": {
      "name": "--pf-c-tree-view__node--PaddingTop",
      "value": "0.5rem",
      "values": [
        "--pf-c-tree-view__node--PaddingTop--base",
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_PaddingRight": {
      "name": "--pf-c-tree-view__node--PaddingRight",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_PaddingBottom": {
      "name": "--pf-c-tree-view__node--PaddingBottom",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-tree-view__node--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_tree_view__node_Color": {
      "name": "--pf-c-tree-view__node--Color",
      "value": "#151515",
      "values": [
        "--pf-global--Color--100",
        "$pf-global--Color--100",
        "$pf-color-black-900",
        "#151515"
      ]
    },
    "c_tree_view__node_m_current_Color": {
      "name": "--pf-c-tree-view__node--m-current--Color",
      "value": "#06c",
      "values": [
        "--pf-global--link--Color",
        "$pf-global--link--Color",
        "$pf-global--primary-color--100",
        "$pf-color-blue-400",
        "#06c"
      ]
    },
    "c_tree_view__node_m_current_FontWeight": {
      "name": "--pf-c-tree-view__node--m-current--FontWeight",
      "value": "700",
      "values": [
        "--pf-global--FontWeight--bold",
        "$pf-global--FontWeight--bold",
        "700"
      ]
    },
    "c_tree_view__node_container_Display": {
      "name": "--pf-c-tree-view__node-container--Display",
      "value": "contents"
    },
    "c_tree_view__node_content_RowGap": {
      "name": "--pf-c-tree-view__node-content--RowGap",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_content_Overflow": {
      "name": "--pf-c-tree-view__node-content--Overflow",
      "value": "visible"
    },
    "c_tree_view__node_hover_BackgroundColor": {
      "name": "--pf-c-tree-view__node--hover--BackgroundColor",
      "value": "#f0f0f0",
      "values": [
        "--pf-global--BackgroundColor--200",
        "$pf-global--BackgroundColor--200",
        "$pf-color-black-200",
        "#f0f0f0"
      ]
    },
    "c_tree_view__node_focus_BackgroundColor": {
      "name": "--pf-c-tree-view__node--focus--BackgroundColor",
      "value": "#f0f0f0",
      "values": [
        "--pf-global--palette--black-200",
        "$pf-color-black-200",
        "#f0f0f0"
      ]
    },
    "c_tree_view__list_item__list_item__node_toggle_Top": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Top",
      "value": "0.5rem",
      "values": [
        "--pf-c-tree-view__node--PaddingTop--base",
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "--pf-c-tree-view__node--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_tree_view__list_item__list_item__node_toggle_TranslateX": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--TranslateX",
      "value": "-100%"
    },
    "c_tree_view__node_toggle_Position": {
      "name": "--pf-c-tree-view__node-toggle--Position",
      "value": "absolute"
    },
    "c_tree_view__node_toggle_icon_MinWidth": {
      "name": "--pf-c-tree-view__node-toggle-icon--MinWidth",
      "value": "1rem",
      "values": [
        "--pf-global--FontSize--md",
        "$pf-global--FontSize--md",
        "pf-font-prem(16px)",
        "1rem"
      ]
    },
    "c_tree_view__node_toggle_icon_Transition": {
      "name": "--pf-c-tree-view__node-toggle-icon--Transition",
      "value": "all 250ms cubic-bezier(.42, 0, .58, 1)",
      "values": [
        "--pf-global--Transition",
        "$pf-global--Transition",
        "all 250ms cubic-bezier(.42, 0, .58, 1)"
      ]
    },
    "c_tree_view__node_toggle_button_PaddingTop": {
      "name": "--pf-c-tree-view__node-toggle-button--PaddingTop",
      "value": "0.375rem",
      "values": [
        "--pf-global--spacer--form-element",
        "$pf-global--spacer--form-element",
        "pf-size-prem(6px)",
        "0.375rem"
      ]
    },
    "c_tree_view__node_toggle_button_PaddingRight": {
      "name": "--pf-c-tree-view__node-toggle-button--PaddingRight",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_tree_view__node_toggle_button_PaddingBottom": {
      "name": "--pf-c-tree-view__node-toggle-button--PaddingBottom",
      "value": "0.375rem",
      "values": [
        "--pf-global--spacer--form-element",
        "$pf-global--spacer--form-element",
        "pf-size-prem(6px)",
        "0.375rem"
      ]
    },
    "c_tree_view__node_toggle_button_PaddingLeft": {
      "name": "--pf-c-tree-view__node-toggle-button--PaddingLeft",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_tree_view__node_toggle_button_MarginTop": {
      "name": "--pf-c-tree-view__node-toggle-button--MarginTop",
      "value": "calc(0.375rem * -1)",
      "values": [
        "calc(--pf-global--spacer--form-element * -1)",
        "calc($pf-global--spacer--form-element * -1)",
        "calc(pf-size-prem(6px) * -1)",
        "calc(0.375rem * -1)"
      ]
    },
    "c_tree_view__node_toggle_button_MarginBottom": {
      "name": "--pf-c-tree-view__node-toggle-button--MarginBottom",
      "value": "calc(0.375rem * -1)",
      "values": [
        "calc(--pf-global--spacer--form-element * -1)",
        "calc($pf-global--spacer--form-element * -1)",
        "calc(pf-size-prem(6px) * -1)",
        "calc(0.375rem * -1)"
      ]
    },
    "c_tree_view__node_check_MarginRight": {
      "name": "--pf-c-tree-view__node-check--MarginRight",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_count_MarginLeft": {
      "name": "--pf-c-tree-view__node-count--MarginLeft",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_count_c_badge_m_read_BackgroundColor": {
      "name": "--pf-c-tree-view__node-count--c-badge--m-read--BackgroundColor",
      "value": "#d2d2d2",
      "values": [
        "--pf-global--disabled-color--200",
        "$pf-global--disabled-color--200",
        "$pf-color-black-300",
        "#d2d2d2"
      ]
    },
    "c_tree_view__search_PaddingTop": {
      "name": "--pf-c-tree-view__search--PaddingTop",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__search_PaddingRight": {
      "name": "--pf-c-tree-view__search--PaddingRight",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__search_PaddingBottom": {
      "name": "--pf-c-tree-view__search--PaddingBottom",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__search_PaddingLeft": {
      "name": "--pf-c-tree-view__search--PaddingLeft",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_icon_PaddingRight": {
      "name": "--pf-c-tree-view__node-icon--PaddingRight",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_icon_Color": {
      "name": "--pf-c-tree-view__node-icon--Color",
      "value": "#6a6e73",
      "values": [
        "--pf-global--icon--Color--light",
        "$pf-global--icon--Color--light",
        "$pf-color-black-600",
        "#6a6e73"
      ]
    },
    "c_tree_view__node_toggle_icon_base_Rotate": {
      "name": "--pf-c-tree-view__node-toggle-icon--base--Rotate",
      "value": "0"
    },
    "c_tree_view__node_toggle_icon_Rotate": {
      "name": "--pf-c-tree-view__node-toggle-icon--Rotate",
      "value": "0",
      "values": [
        "--pf-c-tree-view__node-toggle-icon--base--Rotate",
        "0"
      ]
    },
    "c_tree_view__list_item_m_expanded__node_toggle_icon_Rotate": {
      "name": "--pf-c-tree-view__list-item--m-expanded__node-toggle-icon--Rotate",
      "value": "90deg"
    },
    "c_tree_view__node_text_max_lines": {
      "name": "--pf-c-tree-view__node-text--max-lines",
      "value": "1"
    },
    "c_tree_view__node_title_FontWeight": {
      "name": "--pf-c-tree-view__node-title--FontWeight",
      "value": "700",
      "values": [
        "--pf-global--FontWeight--bold",
        "$pf-global--FontWeight--bold",
        "700"
      ]
    },
    "c_tree_view__action_MarginLeft": {
      "name": "--pf-c-tree-view__action--MarginLeft",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_tree_view__action_focus_BackgroundColor": {
      "name": "--pf-c-tree-view__action--focus--BackgroundColor",
      "value": "#f0f0f0",
      "values": [
        "--pf-global--BackgroundColor--200",
        "$pf-global--BackgroundColor--200",
        "$pf-color-black-200",
        "#f0f0f0"
      ]
    },
    "c_tree_view__action_Color": {
      "name": "--pf-c-tree-view__action--Color",
      "value": "#6a6e73",
      "values": [
        "--pf-global--icon--Color--light",
        "$pf-global--icon--Color--light",
        "$pf-color-black-600",
        "#6a6e73"
      ]
    },
    "c_tree_view__action_hover_Color": {
      "name": "--pf-c-tree-view__action--hover--Color",
      "value": "#151515",
      "values": [
        "--pf-global--icon--Color--dark",
        "$pf-global--icon--Color--dark",
        "$pf-color-black-900",
        "#151515"
      ]
    },
    "c_tree_view__action_focus_Color": {
      "name": "--pf-c-tree-view__action--focus--Color",
      "value": "#151515",
      "values": [
        "--pf-global--icon--Color--dark",
        "$pf-global--icon--Color--dark",
        "$pf-color-black-900",
        "#151515"
      ]
    },
    "c_tree_view_m_guides_guide_Left": {
      "name": "--pf-c-tree-view--m-guides--guide--Left",
      "value": "calc(calc(1rem * 2 + 1rem) - 1.5rem)",
      "values": [
        "--pf-c-tree-view--m-guides--guide-left--base",
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides__list-node--guide-width--base)",
        "calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--lg)",
        "calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--lg)",
        "calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--lg)",
        "calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--lg)",
        "calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(24px))",
        "calc(calc(1rem * 2 + 1rem) - 1.5rem)"
      ]
    },
    "c_tree_view_m_guides_guide_color_base": {
      "name": "--pf-c-tree-view--m-guides--guide-color--base",
      "value": "#d2d2d2",
      "values": [
        "--pf-global--BorderColor--100",
        "$pf-global--BorderColor--100",
        "$pf-color-black-300",
        "#d2d2d2"
      ]
    },
    "c_tree_view_m_guides_guide_width_base": {
      "name": "--pf-c-tree-view--m-guides--guide-width--base",
      "value": "1px",
      "values": [
        "--pf-global--BorderWidth--sm",
        "$pf-global--BorderWidth--sm",
        "1px"
      ]
    },
    "c_tree_view_m_guides_guide_left_base": {
      "name": "--pf-c-tree-view--m-guides--guide-left--base",
      "value": "calc(calc(1rem * 2 + 1rem) - 1.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides__list-node--guide-width--base)",
        "calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--lg)",
        "calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--lg)",
        "calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--lg)",
        "calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--lg)",
        "calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(24px))",
        "calc(calc(1rem * 2 + 1rem) - 1.5rem)"
      ]
    },
    "c_tree_view_m_guides_guide_left_base_offset": {
      "name": "--pf-c-tree-view--m-guides--guide-left--base--offset",
      "value": "calc(calc(1rem * 2 + 1rem) + 1rem / 2)",
      "values": [
        "calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2)",
        "calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2)",
        "calc(--pf-c-tree-view__node--indent--base + $pf-global--FontSize--md / 2)",
        "calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) + $pf-global--FontSize--md / 2)",
        "calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) + $pf-global--FontSize--md / 2)",
        "calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) + $pf-global--FontSize--md / 2)",
        "calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) + pf-font-prem(16px) / 2)",
        "calc(calc(1rem * 2 + 1rem) + 1rem / 2)"
      ]
    },
    "c_tree_view_m_guides__list_node_guide_width_base": {
      "name": "--pf-c-tree-view--m-guides__list-node--guide-width--base",
      "value": "1.5rem",
      "values": [
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    },
    "c_tree_view_m_guides__list_item_before_Top": {
      "name": "--pf-c-tree-view--m-guides__list-item--before--Top",
      "value": "0"
    },
    "c_tree_view_m_guides__list_item_before_Width": {
      "name": "--pf-c-tree-view--m-guides__list-item--before--Width",
      "value": "1px",
      "values": [
        "--pf-c-tree-view--m-guides--guide-width--base",
        "--pf-global--BorderWidth--sm",
        "$pf-global--BorderWidth--sm",
        "1px"
      ]
    },
    "c_tree_view_m_guides__list_item_before_Height": {
      "name": "--pf-c-tree-view--m-guides__list-item--before--Height",
      "value": "100%"
    },
    "c_tree_view_m_guides__list_item_before_BackgroundColor": {
      "name": "--pf-c-tree-view--m-guides__list-item--before--BackgroundColor",
      "value": "#d2d2d2",
      "values": [
        "--pf-c-tree-view--m-guides--guide-color--base",
        "--pf-global--BorderColor--100",
        "$pf-global--BorderColor--100",
        "$pf-color-black-300",
        "#d2d2d2"
      ]
    },
    "c_tree_view_m_guides__list_item_last_child_before_Top": {
      "name": "--pf-c-tree-view--m-guides__list-item--last-child--before--Top",
      "value": "1.125rem",
      "values": [
        "--pf-c-tree-view--m-guides__node--before--Top",
        "1.125rem"
      ]
    },
    "c_tree_view_m_guides__list_item_last_child_before_Height": {
      "name": "--pf-c-tree-view--m-guides__list-item--last-child--before--Height",
      "value": "1.125rem",
      "values": [
        "--pf-c-tree-view--m-guides__list-item--last-child--before--Top",
        "--pf-c-tree-view--m-guides__node--before--Top",
        "1.125rem"
      ]
    },
    "c_tree_view_m_guides__list_item_ZIndex": {
      "name": "--pf-c-tree-view--m-guides__list-item--ZIndex",
      "value": "100",
      "values": [
        "--pf-global--ZIndex--xs",
        "$pf-global--ZIndex--xs",
        "100"
      ]
    },
    "c_tree_view_m_guides__node_before_Width": {
      "name": "--pf-c-tree-view--m-guides__node--before--Width",
      "value": "1rem"
    },
    "c_tree_view_m_guides__node_before_Height": {
      "name": "--pf-c-tree-view--m-guides__node--before--Height",
      "value": "1px",
      "values": [
        "--pf-c-tree-view--m-guides--guide-width--base",
        "--pf-global--BorderWidth--sm",
        "$pf-global--BorderWidth--sm",
        "1px"
      ]
    },
    "c_tree_view_m_guides__node_before_Top": {
      "name": "--pf-c-tree-view--m-guides__node--before--Top",
      "value": "1.125rem"
    },
    "c_tree_view_m_guides__node_before_BackgroundColor": {
      "name": "--pf-c-tree-view--m-guides__node--before--BackgroundColor",
      "value": "#d2d2d2",
      "values": [
        "--pf-c-tree-view--m-guides--guide-color--base",
        "--pf-global--BorderColor--100",
        "$pf-global--BorderColor--100",
        "$pf-color-black-300",
        "#d2d2d2"
      ]
    },
    "c_tree_view_m_compact_base_border_Left_offset": {
      "name": "--pf-c-tree-view--m-compact--base-border--Left--offset",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_tree_view_m_compact_base_border_Left": {
      "name": "--pf-c-tree-view--m-compact--base-border--Left",
      "value": "calc(calc(1rem * 2 + 1rem) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md)",
        "calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md)",
        "calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px))",
        "calc(calc(1rem * 2 + 1rem) - 1rem)"
      ]
    },
    "c_tree_view_m_compact__node_indent_base": {
      "name": "--pf-c-tree-view--m-compact__node--indent--base",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-tree-view__node--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_tree_view_m_compact__node_nested_indent_base": {
      "name": "--pf-c-tree-view--m-compact__node--nested-indent--base",
      "value": "1.5rem",
      "values": [
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    },
    "c_tree_view_m_compact_border_Left": {
      "name": "--pf-c-tree-view--m-compact--border--Left",
      "value": "calc(calc(1rem * 2 + 1rem) - 1rem)",
      "values": [
        "--pf-c-tree-view--m-compact--base-border--Left",
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md)",
        "calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md)",
        "calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px))",
        "calc(calc(1rem * 2 + 1rem) - 1rem)"
      ]
    },
    "c_tree_view_m_compact__node_PaddingTop": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingTop",
      "value": "0"
    },
    "c_tree_view_m_compact__node_PaddingBottom": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingBottom",
      "value": "0"
    },
    "c_tree_view_m_compact__node_nested_PaddingTop": {
      "name": "--pf-c-tree-view--m-compact__node--nested--PaddingTop",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view_m_compact__node_nested_PaddingBottom": {
      "name": "--pf-c-tree-view--m-compact__node--nested--PaddingBottom",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view_m_compact__list_item__list_item__node_toggle_Top": {
      "name": "--pf-c-tree-view--m-compact__list-item__list-item__node-toggle--Top",
      "value": "calc(1.5rem)",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node-container--PaddingTop)",
        "calc(--pf-global--spacer--lg)",
        "calc($pf-global--spacer--lg)",
        "calc(pf-size-prem(24px))",
        "calc(1.5rem)"
      ]
    },
    "c_tree_view_m_compact__list_item_BorderBottomWidth": {
      "name": "--pf-c-tree-view--m-compact__list-item--BorderBottomWidth",
      "value": "1px",
      "values": [
        "--pf-global--BorderWidth--sm",
        "$pf-global--BorderWidth--sm",
        "1px"
      ]
    },
    "c_tree_view_m_compact__list_item_BorderBottomColor": {
      "name": "--pf-c-tree-view--m-compact__list-item--BorderBottomColor",
      "value": "#d2d2d2",
      "values": [
        "--pf-global--BorderColor--100",
        "$pf-global--BorderColor--100",
        "$pf-color-black-300",
        "#d2d2d2"
      ]
    },
    "c_tree_view_m_compact__list_item_before_Top": {
      "name": "--pf-c-tree-view--m-compact__list-item--before--Top",
      "value": "0"
    },
    "c_tree_view_m_compact__list_item_last_child_before_Height": {
      "name": "--pf-c-tree-view--m-compact__list-item--last-child--before--Height",
      "value": "calc(1.5rem + 0.5rem + 0.25rem)",
      "values": [
        "--pf-c-tree-view--m-compact__node--before--Top",
        "calc(--pf-c-tree-view--m-compact__node-container--PaddingTop + --pf-c-tree-view--m-compact__node--nested--PaddingTop + 0.25rem)",
        "calc(--pf-global--spacer--lg + --pf-global--spacer--sm + 0.25rem)",
        "calc($pf-global--spacer--lg + $pf-global--spacer--sm + 0.25rem)",
        "calc(pf-size-prem(24px) + pf-size-prem(8px) + 0.25rem)",
        "calc(1.5rem + 0.5rem + 0.25rem)"
      ]
    },
    "c_tree_view_m_compact__list_item_nested_before_Top": {
      "name": "--pf-c-tree-view--m-compact__list-item--nested--before--Top",
      "value": "calc(0.5rem * -1)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingTop--base * -1)",
        "calc(--pf-global--spacer--sm * -1)",
        "calc($pf-global--spacer--sm * -1)",
        "calc(pf-size-prem(8px) * -1)",
        "calc(0.5rem * -1)"
      ]
    },
    "c_tree_view_m_compact__list_item_nested_last_child_before_Height": {
      "name": "--pf-c-tree-view--m-compact__list-item--nested--last-child--before--Height",
      "value": "calc(calc(1.5rem + 0.5rem + 0.25rem) + 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--before--Top + --pf-c-tree-view__node--PaddingTop--base)",
        "calc(calc(--pf-c-tree-view--m-compact__node-container--PaddingTop + --pf-c-tree-view--m-compact__node--nested--PaddingTop + 0.25rem) + --pf-global--spacer--sm)",
        "calc(calc(--pf-global--spacer--lg + --pf-global--spacer--sm + 0.25rem) + $pf-global--spacer--sm)",
        "calc(calc($pf-global--spacer--lg + $pf-global--spacer--sm + 0.25rem) + $pf-global--spacer--sm)",
        "calc(calc(pf-size-prem(24px) + pf-size-prem(8px) + 0.25rem) + pf-size-prem(8px))",
        "calc(calc(1.5rem + 0.5rem + 0.25rem) + 0.5rem)"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-tree-view--m-compact__node--indent--base",
        "--pf-c-tree-view__node--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_tree_view_m_compact__node_before_Top": {
      "name": "--pf-c-tree-view--m-compact__node--before--Top",
      "value": "calc(1.5rem + 0.5rem + 0.25rem)",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node-container--PaddingTop + --pf-c-tree-view--m-compact__node--nested--PaddingTop + 0.25rem)",
        "calc(--pf-global--spacer--lg + --pf-global--spacer--sm + 0.25rem)",
        "calc($pf-global--spacer--lg + $pf-global--spacer--sm + 0.25rem)",
        "calc(pf-size-prem(24px) + pf-size-prem(8px) + 0.25rem)",
        "calc(1.5rem + 0.5rem + 0.25rem)"
      ]
    },
    "c_tree_view_m_compact__node_level_2_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--level-2--PaddingLeft",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-tree-view--m-compact__node--indent--base",
        "--pf-c-tree-view__node--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_tree_view_m_compact__node_toggle_nested_MarginRight": {
      "name": "--pf-c-tree-view--m-compact__node-toggle--nested--MarginRight",
      "value": "calc(1rem * -.5)",
      "values": [
        "calc(--pf-c-tree-view__node-toggle-button--PaddingLeft * -.5)",
        "calc(--pf-global--spacer--md * -.5)",
        "calc($pf-global--spacer--md * -.5)",
        "calc(pf-size-prem(16px) * -.5)",
        "calc(1rem * -.5)"
      ]
    },
    "c_tree_view_m_compact__node_toggle_nested_MarginLeft": {
      "name": "--pf-c-tree-view--m-compact__node-toggle--nested--MarginLeft",
      "value": "calc(1rem * -1.5)",
      "values": [
        "calc(--pf-c-tree-view__node-toggle-button--PaddingLeft * -1.5)",
        "calc(--pf-global--spacer--md * -1.5)",
        "calc($pf-global--spacer--md * -1.5)",
        "calc(pf-size-prem(16px) * -1.5)",
        "calc(1rem * -1.5)"
      ]
    },
    "c_tree_view_m_compact__node_container_Display": {
      "name": "--pf-c-tree-view--m-compact__node-container--Display",
      "value": "flex"
    },
    "c_tree_view_m_compact__node_container_PaddingBottom_base": {
      "name": "--pf-c-tree-view--m-compact__node-container--PaddingBottom--base",
      "value": "1.5rem",
      "values": [
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    },
    "c_tree_view_m_compact__node_container_PaddingTop": {
      "name": "--pf-c-tree-view--m-compact__node-container--PaddingTop",
      "value": "1.5rem",
      "values": [
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    },
    "c_tree_view_m_compact__node_container_PaddingRight": {
      "name": "--pf-c-tree-view--m-compact__node-container--PaddingRight",
      "value": "1.5rem",
      "values": [
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    },
    "c_tree_view_m_compact__node_container_PaddingBottom": {
      "name": "--pf-c-tree-view--m-compact__node-container--PaddingBottom",
      "value": "1.5rem",
      "values": [
        "--pf-c-tree-view--m-compact__node-container--PaddingBottom--base",
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    },
    "c_tree_view_m_compact__node_container_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node-container--PaddingLeft",
      "value": "0.25rem",
      "values": [
        "--pf-global--spacer--xs",
        "$pf-global--spacer--xs",
        "pf-size-prem(4px)",
        "0.25rem"
      ]
    },
    "c_tree_view_m_compact__node_container_nested_PaddingTop": {
      "name": "--pf-c-tree-view--m-compact__node-container--nested--PaddingTop",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_tree_view_m_compact__node_container_nested_PaddingRight": {
      "name": "--pf-c-tree-view--m-compact__node-container--nested--PaddingRight",
      "value": "1.5rem",
      "values": [
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    },
    "c_tree_view_m_compact__node_container_nested_PaddingBottom": {
      "name": "--pf-c-tree-view--m-compact__node-container--nested--PaddingBottom",
      "value": "1rem",
      "values": [
        "--pf-global--spacer--md",
        "$pf-global--spacer--md",
        "pf-size-prem(16px)",
        "1rem"
      ]
    },
    "c_tree_view_m_compact__node_container_nested_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node-container--nested--PaddingLeft",
      "value": "1.5rem",
      "values": [
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    },
    "c_tree_view_m_compact__node_container_nested_BackgroundColor": {
      "name": "--pf-c-tree-view--m-compact__node-container--nested--BackgroundColor",
      "value": "#f0f0f0",
      "values": [
        "--pf-global--BackgroundColor--200",
        "$pf-global--BackgroundColor--200",
        "$pf-color-black-200",
        "#f0f0f0"
      ]
    },
    "c_tree_view_m_compact__list_item_m_expanded__node_container_PaddingBottom": {
      "name": "--pf-c-tree-view--m-compact__list-item--m-expanded__node-container--PaddingBottom",
      "value": "0"
    },
    "c_tree_view_m_no_background__node_container_BackgroundColor": {
      "name": "--pf-c-tree-view--m-no-background__node-container--BackgroundColor",
      "value": "transparent"
    },
    "c_tree_view_m_compact_m_no_background_base_border_Left_offset": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view_m_compact_m_no_background_base_border_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--base-border--Left",
      "value": "calc(calc(1rem * 2 + 1rem) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--sm)",
        "calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--sm)",
        "calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--sm)",
        "calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--sm)",
        "calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(8px))",
        "calc(calc(1rem * 2 + 1rem) - 0.5rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_indent_base": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--indent--base",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-tree-view__node--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_nested_indent_base": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base",
      "value": "3rem",
      "values": [
        "--pf-global--spacer--2xl",
        "$pf-global--spacer--2xl",
        "pf-size-prem(48px)",
        "3rem"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_nested_PaddingTop": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--nested--PaddingTop",
      "value": "0"
    },
    "c_tree_view_m_compact_m_no_background__node_nested_PaddingBottom": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--nested--PaddingBottom",
      "value": "0"
    },
    "c_tree_view_m_compact_m_no_background__node_before_Top": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--before--Top",
      "value": "calc(1rem + 0.5rem + 0.25rem)",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node-container--nested--PaddingTop + --pf-c-tree-view--m-compact__node--nested--PaddingTop + 0.25rem)",
        "calc(--pf-global--spacer--md + --pf-global--spacer--sm + 0.25rem)",
        "calc($pf-global--spacer--md + $pf-global--spacer--sm + 0.25rem)",
        "calc(pf-size-prem(16px) + pf-size-prem(8px) + 0.25rem)",
        "calc(1rem + 0.5rem + 0.25rem)"
      ]
    }
  },
  ".pf-c-tree-view.pf-m-compact .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view_m_guides_guide_Left": {
      "name": "--pf-c-tree-view--m-guides--guide--Left",
      "value": "calc(calc(1.5rem * 1 + calc(1rem * 2 + 1rem)) - calc(calc(1.5rem * 1 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "--pf-c-tree-view--m-guides--border--nested--Left",
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(--pf-c-tree-view--m-compact__node--PaddingLeft - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 1 + --pf-c-tree-view--m-compact__node--indent--base) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(--pf-global--spacer--lg * 1 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view--m-compact__node--PaddingLeft + $pf-global--FontSize--md / 2))",
        "calc(calc($pf-global--spacer--lg * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 1 + --pf-c-tree-view--m-compact__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-global--spacer--lg * 1 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc($pf-global--spacer--lg * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(pf-size-prem(24px) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(pf-size-prem(24px) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(1.5rem * 1 + calc(1rem * 2 + 1rem)) - calc(calc(1.5rem * 1 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(1.5rem * 1 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view--m-compact__node--PaddingLeft",
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 1 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 1 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 1 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_Left": {
      "name": "--pf-c-tree-view--m-compact--border--Left",
      "value": "calc(calc(1.5rem * 1 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "--pf-c-tree-view--m-compact--border--nested--Left",
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(--pf-c-tree-view--m-compact__node--PaddingLeft - --pf-global--spacer--md)",
        "calc(calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 1 + --pf-c-tree-view--m-compact__node--indent--base) - $pf-global--spacer--md)",
        "calc(calc(--pf-global--spacer--lg * 1 + --pf-c-tree-view__node--indent--base) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--lg * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(pf-size-prem(24px) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(1.5rem * 1 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact__list_item_before_Top": {
      "name": "--pf-c-tree-view--m-compact__list-item--before--Top",
      "value": "calc(0.5rem * -1)",
      "values": [
        "--pf-c-tree-view--m-compact__list-item--nested--before--Top",
        "calc(--pf-c-tree-view__node--PaddingTop--base * -1)",
        "calc(--pf-global--spacer--sm * -1)",
        "calc($pf-global--spacer--sm * -1)",
        "calc(pf-size-prem(8px) * -1)",
        "calc(0.5rem * -1)"
      ]
    },
    "c_tree_view_m_compact__list_item_last_child_before_Height": {
      "name": "--pf-c-tree-view--m-compact__list-item--last-child--before--Height",
      "value": "calc(calc(1.5rem + 0.5rem + 0.25rem) + 0.5rem)",
      "values": [
        "--pf-c-tree-view--m-compact__list-item--nested--last-child--before--Height",
        "calc(--pf-c-tree-view--m-compact__node--before--Top + --pf-c-tree-view__node--PaddingTop--base)",
        "calc(calc(--pf-c-tree-view--m-compact__node-container--PaddingTop + --pf-c-tree-view--m-compact__node--nested--PaddingTop + 0.25rem) + --pf-global--spacer--sm)",
        "calc(calc(--pf-global--spacer--lg + --pf-global--spacer--sm + 0.25rem) + $pf-global--spacer--sm)",
        "calc(calc($pf-global--spacer--lg + $pf-global--spacer--sm + 0.25rem) + $pf-global--spacer--sm)",
        "calc(calc(pf-size-prem(24px) + pf-size-prem(8px) + 0.25rem) + pf-size-prem(8px))",
        "calc(calc(1.5rem + 0.5rem + 0.25rem) + 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view.pf-m-compact .pf-c-tree-view__list-item:last-child": {
    "c_tree_view_m_compact__list_item_BorderBottomWidth": {
      "name": "--pf-c-tree-view--m-compact__list-item--BorderBottomWidth",
      "value": "0"
    }
  },
  ".pf-c-tree-view.pf-m-compact": {
    "c_tree_view__node_PaddingTop": {
      "name": "--pf-c-tree-view__node--PaddingTop",
      "value": "0",
      "values": [
        "--pf-c-tree-view--m-compact__node--PaddingTop",
        "0"
      ]
    },
    "c_tree_view__node_PaddingBottom": {
      "name": "--pf-c-tree-view__node--PaddingBottom",
      "value": "0",
      "values": [
        "--pf-c-tree-view--m-compact__node--PaddingBottom",
        "0"
      ]
    },
    "c_tree_view__node_container_Display": {
      "name": "--pf-c-tree-view__node-container--Display",
      "value": "flex",
      "values": [
        "--pf-c-tree-view--m-compact__node-container--Display",
        "flex"
      ]
    },
    "c_tree_view__node_hover_BackgroundColor": {
      "name": "--pf-c-tree-view__node--hover--BackgroundColor",
      "value": "transparent"
    },
    "c_tree_view__node_focus_BackgroundColor": {
      "name": "--pf-c-tree-view__node--focus--BackgroundColor",
      "value": "transparent"
    },
    "c_tree_view__list_item__list_item__node_toggle_Top": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Top",
      "value": "calc(1.5rem)",
      "values": [
        "--pf-c-tree-view--m-compact__list-item__list-item__node-toggle--Top",
        "calc(--pf-c-tree-view--m-compact__node-container--PaddingTop)",
        "calc(--pf-global--spacer--lg)",
        "calc($pf-global--spacer--lg)",
        "calc(pf-size-prem(24px))",
        "calc(1.5rem)"
      ]
    }
  },
  ".pf-c-tree-view.pf-m-compact .pf-c-tree-view__list-item.pf-m-expanded": {
    "c_tree_view_m_compact__node_container_PaddingBottom": {
      "name": "--pf-c-tree-view--m-compact__node-container--PaddingBottom",
      "value": "0",
      "values": [
        "--pf-c-tree-view--m-compact__list-item--m-expanded__node-container--PaddingBottom",
        "0"
      ]
    }
  },
  ".pf-c-tree-view.pf-m-compact .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__node_PaddingTop": {
      "name": "--pf-c-tree-view__node--PaddingTop",
      "value": "0.5rem",
      "values": [
        "--pf-c-tree-view--m-compact__node--nested--PaddingTop",
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_PaddingBottom": {
      "name": "--pf-c-tree-view__node--PaddingBottom",
      "value": "0.5rem",
      "values": [
        "--pf-c-tree-view--m-compact__node--nested--PaddingBottom",
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    },
    "c_tree_view__node_toggle_Position": {
      "name": "--pf-c-tree-view__node-toggle--Position",
      "value": "static"
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(1rem * 2 + 1rem)",
      "values": [
        "--pf-c-tree-view--m-compact__node--level-2--PaddingLeft",
        "--pf-c-tree-view--m-compact__node--indent--base",
        "--pf-c-tree-view__node--indent--base",
        "calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)",
        "calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)",
        "calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)",
        "calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))",
        "calc(1rem * 2 + 1rem)"
      ]
    },
    "c_tree_view__list_item__list_item__node_toggle_TranslateX": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--TranslateX",
      "value": "0"
    },
    "c_tree_view_m_compact__list_item_BorderBottomWidth": {
      "name": "--pf-c-tree-view--m-compact__list-item--BorderBottomWidth",
      "value": "0"
    },
    "c_tree_view_m_compact__node_container_PaddingBottom": {
      "name": "--pf-c-tree-view--m-compact__node-container--PaddingBottom",
      "value": "1.5rem",
      "values": [
        "--pf-c-tree-view--m-compact__node-container--PaddingBottom--base",
        "--pf-global--spacer--lg",
        "$pf-global--spacer--lg",
        "pf-size-prem(24px)",
        "1.5rem"
      ]
    }
  },
  ".pf-c-tree-view.pf-m-compact.pf-m-no-background": {
    "c_tree_view_m_compact__node_before_Top": {
      "name": "--pf-c-tree-view--m-compact__node--before--Top",
      "value": "calc(1rem + 0.5rem + 0.25rem)",
      "values": [
        "--pf-c-tree-view--m-compact--m-no-background__node--before--Top",
        "calc(--pf-c-tree-view--m-compact__node-container--nested--PaddingTop + --pf-c-tree-view--m-compact__node--nested--PaddingTop + 0.25rem)",
        "calc(--pf-global--spacer--md + --pf-global--spacer--sm + 0.25rem)",
        "calc($pf-global--spacer--md + $pf-global--spacer--sm + 0.25rem)",
        "calc(pf-size-prem(16px) + pf-size-prem(8px) + 0.25rem)",
        "calc(1rem + 0.5rem + 0.25rem)"
      ]
    }
  },
  ".pf-c-tree-view.pf-m-compact.pf-m-no-background .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__node_PaddingTop": {
      "name": "--pf-c-tree-view__node--PaddingTop",
      "value": "0",
      "values": [
        "--pf-c-tree-view--m-compact--m-no-background__node--nested--PaddingTop",
        "0"
      ]
    },
    "c_tree_view__node_PaddingBottom": {
      "name": "--pf-c-tree-view__node--PaddingBottom",
      "value": "0",
      "values": [
        "--pf-c-tree-view--m-compact--m-no-background__node--nested--PaddingBottom",
        "0"
      ]
    }
  },
  ".pf-c-tree-view.pf-m-compact.pf-m-no-background .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view_m_compact_border_Left": {
      "name": "--pf-c-tree-view--m-compact--border--Left",
      "value": "calc(calc(3rem * 1 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft - --pf-global--spacer--sm)",
        "calc(calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 1 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base) - $pf-global--spacer--sm)",
        "calc(calc(--pf-global--spacer--2xl * 1 + --pf-c-tree-view__node--indent--base) - $pf-global--spacer--sm)",
        "calc(calc($pf-global--spacer--2xl * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc($pf-global--spacer--2xl * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc($pf-global--spacer--2xl * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(pf-size-prem(48px) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(3rem * 1 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(3rem * 1 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 1 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 1 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 1 + calc(1rem * 2 + 1rem))"
      ]
    }
  },
  ".pf-c-tree-view.pf-m-no-background": {
    "c_tree_view_m_compact__node_container_nested_BackgroundColor": {
      "name": "--pf-c-tree-view--m-compact__node-container--nested--BackgroundColor",
      "value": "transparent",
      "values": [
        "--pf-c-tree-view--m-no-background__node-container--BackgroundColor",
        "transparent"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__node_toggle_icon_Rotate": {
      "name": "--pf-c-tree-view__node-toggle-icon--Rotate",
      "value": "0",
      "values": [
        "--pf-c-tree-view__node-toggle-icon--base--Rotate",
        "0"
      ]
    },
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 1 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 1 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 1 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 1 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 1 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    }
  },
  ".pf-c-tree-view__list-item.pf-m-expanded": {
    "c_tree_view__node_toggle_icon_Rotate": {
      "name": "--pf-c-tree-view__node-toggle-icon--Rotate",
      "value": "90deg",
      "values": [
        "--pf-c-tree-view__list-item--m-expanded__node-toggle-icon--Rotate",
        "90deg"
      ]
    }
  },
  ".pf-c-tree-view__node.pf-m-current": {
    "c_tree_view__node_Color": {
      "name": "--pf-c-tree-view__node--Color",
      "value": "#06c",
      "values": [
        "--pf-c-tree-view__node--m-current--Color",
        "--pf-global--link--Color",
        "$pf-global--link--Color",
        "$pf-global--primary-color--100",
        "$pf-color-blue-400",
        "#06c"
      ]
    }
  },
  ".pf-c-tree-view__node .pf-c-tree-view__node-count .pf-c-badge.pf-m-read": {
    "c_badge_m_read_BackgroundColor": {
      "name": "--pf-c-badge--m-read--BackgroundColor",
      "value": "#d2d2d2",
      "values": [
        "--pf-c-tree-view__node-count--c-badge--m-read--BackgroundColor",
        "--pf-global--disabled-color--200",
        "$pf-global--disabled-color--200",
        "$pf-color-black-300",
        "#d2d2d2"
      ]
    }
  },
  ".pf-c-tree-view__node-title.pf-m-truncate": {
    "c_tree_view__node_content_Overflow": {
      "name": "--pf-c-tree-view__node-content--Overflow",
      "value": "hidden"
    }
  },
  ".pf-c-tree-view.pf-m-truncate .pf-c-tree-view__node-title": {
    "c_tree_view__node_content_Overflow": {
      "name": "--pf-c-tree-view__node-content--Overflow",
      "value": "hidden"
    }
  },
  ".pf-c-tree-view__action:hover": {
    "c_tree_view__action_Color": {
      "name": "--pf-c-tree-view__action--Color",
      "value": "#151515",
      "values": [
        "--pf-c-tree-view__action--hover--Color",
        "--pf-global--icon--Color--dark",
        "$pf-global--icon--Color--dark",
        "$pf-color-black-900",
        "#151515"
      ]
    }
  },
  ".pf-c-tree-view__action:focus": {
    "c_tree_view__action_Color": {
      "name": "--pf-c-tree-view__action--Color",
      "value": "#151515",
      "values": [
        "--pf-c-tree-view__action--focus--Color",
        "--pf-global--icon--Color--dark",
        "$pf-global--icon--Color--dark",
        "$pf-color-black-900",
        "#151515"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 2 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 2 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 2 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 2 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 1 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 1 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 1 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 1 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 2 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 1 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 1 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 1 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 1 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 1 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 1 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 1 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 1 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 2 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 2 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 3 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 3 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 3 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 3 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 2 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 2 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 2 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 2 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 3 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 2 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 2 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 2 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 2 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 2 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 2 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 2 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 2 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 3 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 3 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 4 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 4 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 4 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 4 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 3 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 3 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 3 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 3 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 4 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 3 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 3 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 3 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 3 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 3 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 3 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 3 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 3 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 4 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 4 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 5 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 5 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 5 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 5 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 4 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 4 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 4 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 4 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 5 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 4 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 4 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 4 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 4 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 4 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 4 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 4 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 4 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 5 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 5 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 6 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 6 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 6 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 6 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 5 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 5 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 5 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 5 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 6 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 5 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 5 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 5 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 5 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 5 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 5 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 5 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 5 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 6 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 6 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 7 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 7 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 7 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 7 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 6 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 6 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 6 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 6 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 7 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 6 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 6 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 6 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 6 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 6 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 6 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 6 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 6 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 7 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 7 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 8 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 8 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 8 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 8 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 7 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 7 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 7 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 7 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 8 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 7 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 7 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 7 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 7 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 7 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 7 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 7 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 7 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 8 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 8 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 9 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 9 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 9 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 9 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 8 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 8 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 8 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 8 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 9 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 8 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 8 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 8 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 8 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 8 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 8 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 8 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 8 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 9 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 9 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  },
  ".pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item .pf-c-tree-view__list-item": {
    "c_tree_view__list_item__list_item__node_toggle_Left": {
      "name": "--pf-c-tree-view__list-item__list-item__node-toggle--Left",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem))",
      "values": [
        "--pf-c-tree-view__node--PaddingLeft",
        "calc(--pf-c-tree-view__node--nested-indent--base * 10 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 10 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 10 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view__node_PaddingLeft": {
      "name": "--pf-c-tree-view__node--PaddingLeft",
      "value": "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view__node--nested-indent--base * 10 + --pf-c-tree-view__node--indent--base)",
        "calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 10 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 10 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_guides_border_nested_Left": {
      "name": "--pf-c-tree-view--m-guides--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem)) + 1rem / 2))",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-guides--guide-left--base--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 10 + --pf-c-tree-view__node--indent--base) - calc(--pf-c-tree-view__list-item__list-item__node-toggle--Left + --pf-c-tree-view__node-toggle-icon--MinWidth / 2))",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 10 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - calc(--pf-c-tree-view__node--PaddingLeft + --pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - calc(calc(--pf-c-tree-view__node--nested-indent--base * 10 + --pf-c-tree-view__node--indent--base) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 10 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) + $pf-global--FontSize--md / 2))",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 10 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 10 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) + pf-font-prem(16px) / 2))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem)) - calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem)) + 1rem / 2))"
      ]
    },
    "c_tree_view_m_compact__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact__node--PaddingLeft",
      "value": "calc(1.5rem * 9 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact__node--nested-indent--base * 9 + --pf-c-tree-view--m-compact__node--indent--base)",
        "calc(--pf-global--spacer--lg * 9 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--lg * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--lg * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--lg * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(24px) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(1.5rem * 9 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem)) - 1rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 10 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--md)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 10 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--md)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 10 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(16px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem)) - 1rem)"
      ]
    },
    "c_tree_view_m_compact_m_no_background__node_PaddingLeft": {
      "name": "--pf-c-tree-view--m-compact--m-no-background__node--PaddingLeft",
      "value": "calc(3rem * 9 + calc(1rem * 2 + 1rem))",
      "values": [
        "calc(--pf-c-tree-view--m-compact--m-no-background__node--nested-indent--base * 9 + --pf-c-tree-view--m-compact--m-no-background__node--indent--base)",
        "calc(--pf-global--spacer--2xl * 9 + --pf-c-tree-view__node--indent--base)",
        "calc($pf-global--spacer--2xl * 9 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth))",
        "calc($pf-global--spacer--2xl * 9 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md))",
        "calc($pf-global--spacer--2xl * 9 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md))",
        "calc(pf-size-prem(48px) * 9 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)))",
        "calc(3rem * 9 + calc(1rem * 2 + 1rem))"
      ]
    },
    "c_tree_view_m_compact_m_no_background_border_nested_Left": {
      "name": "--pf-c-tree-view--m-compact--m-no-background--border--nested--Left",
      "value": "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem)) - 0.5rem)",
      "values": [
        "calc(--pf-c-tree-view__node--PaddingLeft - --pf-c-tree-view--m-compact--m-no-background--base-border--Left--offset)",
        "calc(calc(--pf-c-tree-view__node--nested-indent--base * 10 + --pf-c-tree-view__node--indent--base) - --pf-global--spacer--sm)",
        "calc(calc(calc(--pf-c-tree-view__node--indent--base - --pf-global--spacer--md) * 10 + calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(--pf-global--spacer--md * 2 + --pf-c-tree-view__node-toggle-icon--MinWidth) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + --pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md) - $pf-global--spacer--md) * 10 + calc($pf-global--spacer--md * 2 + $pf-global--FontSize--md)) - $pf-global--spacer--sm)",
        "calc(calc(calc(calc(pf-size-prem(16px) * 2 + pf-font-prem(16px)) - pf-size-prem(16px)) * 10 + calc(pf-size-prem(16px) * 2 + pf-font-prem(16px))) - pf-size-prem(8px))",
        "calc(calc(calc(calc(1rem * 2 + 1rem) - 1rem) * 10 + calc(1rem * 2 + 1rem)) - 0.5rem)"
      ]
    }
  }
};
export default c_tree_view;