---
title: Toolbar
section: components
cssPrefix: pf-c-toolbar
---

import './Toolbar.css'

## Introduction

Toolbar relies on groups (`.pf-c-toolbar__group`) and items (`.pf-c-toolbar__item`), with default spacer values. Groups and items can be siblings and/or items can be nested within groups. Modifier selectors adjust spacing based on the type of group or item. Each modifier applies a unique CSS variable, therefore, the base spacer value for all elements can be customized and item/groups spacers can be themed individually. The default spacer value for items and groups is set to `--pf-c-toolbar--spacer--base`, whose value is `--pf-global--spacer--md` or 16px.

### Default spacing for items and groups:

| Class | CSS Variable | Computed Value |
| -- | -- | -- |
| `.pf-c-toolbar__item` | `--pf-c-toolbar__item--spacer` | `16px` |
| `.pf-c-toolbar__group` | `--pf-c-toolbar__group--spacer` | `16px` |

### Toolbar item types

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-bulk-select` | `.pf-c-toolbar__item` | Initiates bulk select spacing. Spacer value is set to `var(--pf-c-toolbar--m-bulk-select--spacer)`. |
| `.pf-m-overflow-menu` | `.pf-c-toolbar__item` | Initiates overflow menu spacing. Spacer value is set to `var(--pf-c-toolbar--m-overflow-menu--spacer)`. |
| `.pf-m-pagination` | `.pf-c-toolbar__item` | Initiates pagination spacing and margin. Spacer value is set to `var(--pf-c-toolbar--m-pagination--spacer)`. |
| `.pf-m-search-filter` | `.pf-c-toolbar__item` | Initiates search filter spacing. Spacer value is set to `var(--pf-c-toolbar--m-search-filter--spacer)`. |

### Modifiers

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-hidden{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to be hidden, at optional breakpoint. |
| `.pf-m-visible{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to be shown, at optional breakpoint. |
| `.pf-m-align-right{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to align right, at optional breakpoint. |
| `.pf-m-align-left{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to align left, at optional breakpoint. |

### Special notes

Several components in the following examples do not include functional and/or accessibility specifications (for example `.pf-c-select`, `.pf-c-options-menu`). Rather, `.pf-c-toolbar` focuses on functionality and accessibility specifications that apply to it only.

**Available breakpoints are: `-on-md, -on-lg, -on-xl, -on-2xl`.**

## Examples

```hbs title=Simple
{{#> toolbar toolbar--id="toolbar-simple-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-item}}
        Item
      {{/toolbar-item}}
      {{#> toolbar-item}}
        Item
      {{/toolbar-item}}
      {{#> toolbar-item}}
        Item
      {{/toolbar-item}}
      {{> divider divider--modifier="pf-m-vertical"}}
      {{#> toolbar-group}}
        {{#> toolbar-item}}
          Item
        {{/toolbar-item}}
        {{#> toolbar-item}}
          Item
        {{/toolbar-item}}
        {{#> toolbar-item}}
          Item
        {{/toolbar-item}}
      {{/toolbar-group}}
      {{> divider divider--modifier="pf-m-vertical"}}
      {{#> toolbar-item}}
        Item
      {{/toolbar-item}}
      {{#> toolbar-item}}
        Item
      {{/toolbar-item}}
      {{#> toolbar-item}}
        Item
      {{/toolbar-item}}
    {{/toolbar-content-section}}
  {{/toolbar-content}}
{{/toolbar}}
```

### Item types

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-toolbar__item` | `<div>` | Initiates the toolbar component item. **Required** |
| `.pf-c-toolbar__group` | `<div>` | Initiates the toolbar component group. |

### Spacers

In some instances, it may be necessary to adjust spacing explicitly where items are hidden/shown. For example, if a `.pf-m-toggle-group` is adjacent to an element being hidden/shown, the spacing may appear to be inconsistent. If possible, rely on modifier values. Available spacer modifiers are `.pf-m-spacer-{none, sm, md, lg}{-on-md, -on-lg, -on-xl}` and `.pf-m-space-items-{none, sm, md, lg}{-on-md, -on-lg, -on-xl}`. These modifiers will overwrite existing modifiers provided by `.pf-c-toolbar`.

```hbs title=Adjusted-spacers
{{#> toolbar toolbar--id="toolbar-spacer-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-item toolbar-item--modifier="pf-m-spacer-none"}}
        Item
      {{/toolbar-item}}
      {{#> toolbar-item toolbar-item--modifier="pf-m-spacer-sm"}}
        Item
      {{/toolbar-item}}
      {{#> toolbar-item toolbar-item--modifier="pf-m-spacer-md"}}
        Item
      {{/toolbar-item}}
      {{#> toolbar-item toolbar-item--modifier="pf-m-spacer-lg"}}
        Item
      {{/toolbar-item}}
      {{> divider divider--modifier="pf-m-vertical"}}
      {{#> toolbar-item toolbar-item--modifier="pf-m-spacer-none pf-m-spacer-sm-on-md pf-m-spacer-md-on-lg pf-m-spacer-lg-on-xl"}}
        Item
      {{/toolbar-item}}
      {{#> toolbar-item}}
        Item
      {{/toolbar-item}}
    {{/toolbar-content-section}}
  {{/toolbar-content}}
{{/toolbar}}
```

```hbs title=Adjusted-group-spacers
{{#> toolbar toolbar--id="toolbar-group-spacer-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-space-items-lg"}}
        {{#> toolbar-item}}
          Item
        {{/toolbar-item}}
        {{#> toolbar-item}}
          Item
        {{/toolbar-item}}
      {{/toolbar-group}}
      {{> divider divider--modifier="pf-m-vertical"}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-space-items-none pf-m-space-items-sm-on-md pf-m-space-items-md-on-lg pf-m-space-items-lg-on-xl"}}
        {{#> toolbar-item}}
          Item
        {{/toolbar-item}}
        {{#> toolbar-item}}
          Item
        {{/toolbar-item}}
      {{/toolbar-group}}
    {{/toolbar-content-section}}
  {{/toolbar-content}}
{{/toolbar}}
```

### Toolbar spacers

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-spacer-{none, sm, md, lg}{-on-[breakpoint]}` | `.pf-c-toolbar__group`, `.pf-c-toolbar__item` | Modifies toolbar group or item spacing. |
| `.pf-m-space-items-{none, sm, md, lg}{-on-[breakpoint]}` | `.pf-c-toolbar__group` | Modifies toolbar group child spacing. |

```hbs title=Group-types
{{#> toolbar toolbar--id="toolbar-group-types-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-filter-group"}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-filter1')}}
            Filter 1
          {{/select}}
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-filter2')}}
            Filter 2
          {{/select}}
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-filter3')}}
            Filter 3
          {{/select}}
        {{/toolbar-item}}
      {{/toolbar-group}}
      {{> toolbar-icon-button-group-example}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-button-group"}}
        {{#> toolbar-item}}
          {{#> button button--modifier="pf-m-primary"}}
            Action
          {{/button}}
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> button button--modifier="pf-m-secondary"}}
            Secondary
          {{/button}}
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> button button--modifier="pf-m-tertiary"}}
            Tertiary
          {{/button}}
        {{/toolbar-item}}
      {{/toolbar-group}}
    {{/toolbar-content-section}}
  {{/toolbar-content}}
{{/toolbar}}
```

### Toolbar group types

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-filter-group` | `.pf-c-toolbar__group` | Modifies toolbar group spacing. Spacer value is set to `var(--pf-c-toolbar__group--m-filter-group--spacer)`. Child spacer value is set to `var(--pf-c-toolbar__group--m-filter-group--space-items)`. |
| `.pf-m-icon-button-group` | `.pf-c-toolbar__group` | Modifies toolbar group spacing. Spacer value is set to `var(--pf-c-toolbar__group--m-toggle-group--spacer)`. Child spacer value is set to `var(--pf-c-toolbar__group--m-icon-button-group--space-items)`. |
| `.pf-m-button-group` | `.pf-c-toolbar__group` | Modifies toolbar group spacing. Spacer value is set to `var(--pf-c-toolbar__group--m-toggle-group--spacer)`. Child spacer value is set to `var(--pf-c-toolbar__group--m-button-group--space-items)`. |

```hbs title=Toggle-group
{{#> toolbar toolbar--id="toolbar-toggle-group-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-toggle-group pf-m-show-on-lg"}}
        {{> toolbar-toggle toolbar-toggle--IsExpanded="false"}}
        {{> toolbar-item-search-filter button--id="content"}}
        {{#> toolbar-group toolbar-group--modifier="pf-m-filter-group"}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-status') select--IsCheckboxSelect="true"}}
              Status
            {{/select}}
          {{/toolbar-item}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-risk') select--IsCheckboxSelect="true"}}
              Risk
            {{/select}}
          {{/toolbar-item}}
        {{/toolbar-group}}
      {{/toolbar-group}}
    {{/toolbar-content-section}}
    {{#> toolbar-expandable-content}}{{/toolbar-expandable-content}}
  {{/toolbar-content}}
{{/toolbar}}
```

```hbs title=Toggle-group-on-mobile-(filters-collapsed,-expandable-content-expanded)
{{#> toolbar toolbar--id="toolbar-toggle-group-collapsed-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-toggle-group"}}
        {{> toolbar-toggle toolbar-toggle--modifier="pf-m-expanded" toolbar-toggle--IsExpanded="true"}}
      {{/toolbar-group}}
    {{/toolbar-content-section}}
    {{#> toolbar-expandable-content toolbar-expandable-content--IsExpanded="true"}}
      {{> toolbar-item-search-filter button--id="expandable-content"}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-filter-group"}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-status-expanded') select--IsCheckboxSelect="true"}}
            Status
          {{/select}}
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-risk-expanded') select--IsCheckboxSelect="true"}}
            Risk
          {{/select}}
        {{/toolbar-item}}
      {{/toolbar-group}}
    {{/toolbar-expandable-content}}
  {{/toolbar-content}}
{{/toolbar}}
```

### Toggle group modifier

The `.pf-m-toggle-group` controls when, and at which breakpoint, filters will be hidden/shown. By default, all filters are hidden until the specified breakpoint is reached. `.pf-m-show-on-{md, lg, xl}` controls when filters are shown and when the toggle button is hidden.

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `hidden` | `.pf-c-toolbar__item`, `.pf-c-toolbar__group`, `.pf-c-toolbar__toggle`, `.pf-c-toolbar__expandable-content` |  Indicates that the toggle group element is hidden. **Required** |
| `aria-expanded="true"` | `.pf-c-toolbar__toggle > .pf-c-button` |  Indicates that the expandable content is visible. **Required** |
| `aria-expanded="false"` | `.pf-c-toolbar__toggle > .pf-c-button` |  Indicates the the expandable content is hidden. **Required** |
| `aria-controls="[id of expandable content]"` | `.pf-c-toolbar__toggle > .pf-c-button` |  Identifies the expanded content controlled by the toggle button. **Required** |
| `id="[expandable-content_id]"` | `.pf-c-toolbar__expandable-content` | Provides a reference for toggle button description. **Required** |

### Responsive attributes

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-haspopup="true"` | `.pf-c-toolbar__toggle > .pf-c-button` | When expandable content appears above content (mobile viewport), `aria-haspopup="true"` should be applied to indicate that focus should be trapped. **Required** |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-show{-on-[breakpoint]}` | `.pf-c-toolbar__group.pf-m-toggle-group`, `.pf-c-toolbar__expandable-content` | Modifies toolbar element visibility at breakpoint. This selector must be applied consistently to toggle group and expandable content. |
| `.pf-m-chip-container` | `.pf-c-toolbar__content-section`, `.pf-c-toolbar__group` | Modifies the toolbar element for applied filters layout. |
| `.pf-m-expanded` | `.pf-c-toolbar__expandable-content`, `.pf-c-toolbar__toggle` | Modifies the component for the expanded state. |

### Selected

```hbs title=Selected-filters-on-mobile-(filters-collapsed,-selected-filters-summary-visible)
{{#> toolbar toolbar--id="toolbar-selected-filters-toggle-group-collapsed-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{> toolbar-item-bulk-select}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-toggle-group"}}
        {{> toolbar-toggle toolbar-toggle--IsExpanded="false"}}
        {{> toolbar-item-search-filter button--id="content"}}
        {{#> toolbar-group toolbar-group--modifier="pf-m-filter-group"}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-status-expanded') select--IsCheckboxSelect="true"}}
              Status
            {{/select}}
          {{/toolbar-item}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-risk-expanded') select--IsCheckboxSelect="true"}}
              Risk
            {{/select}}
          {{/toolbar-item}}
        {{/toolbar-group}}
      {{/toolbar-group}}
      {{> toolbar-icon-button-group-example}}
      {{> toolbar-overflow-menu-example toolbar-overflow-menu-example--control="true"}}
    {{/toolbar-content-section}}
    {{#> toolbar-expandable-content}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-chip-container"}}
        {{> toolbar-item-chip-group chip-group--label="Status" chip-group--id=(concat toolbar--id '-chip-group-status')}}
        {{> toolbar-item-chip-group chip-group--label="Risk" chip-group--id=(concat toolbar--id '-chip-group-risk')}}
      {{/toolbar-group}}
    {{/toolbar-expandable-content}}
  {{/toolbar-content}}
  {{#> toolbar-content}}
    {{#> toolbar-item}}
      6 filters applied
    {{/toolbar-item}}
    {{> toolbar-item-clear}}
  {{/toolbar-content}}
{{/toolbar}}
```

```hbs title=Selected-filters-on-mobile-(filters-collapsed,-expandable-content-expanded)
{{#> toolbar toolbar--id="toolbar-selected-filters-toggle-group-expanded-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{> toolbar-item-bulk-select}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-toggle-group"}}
        {{> toolbar-toggle toolbar-toggle--modifier="pf-m-expanded" toolbar-toggle--IsExpanded="true"}}
      {{/toolbar-group}}
      {{> toolbar-icon-button-group-example}}
      {{> toolbar-overflow-menu-example toolbar-overflow-menu-example--control="true"}}
    {{/toolbar-content-section}}
    {{#> toolbar-expandable-content toolbar-expandable-content--IsExpanded="true"}}
      {{> toolbar-item-search-filter button--id="expanded-content"}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-filter-group"}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-status-expanded') select--IsCheckboxSelect="true"}}
            Status
          {{/select}}
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-risk-expanded') select--IsCheckboxSelect="true"}}
            Risk
          {{/select}}
        {{/toolbar-item}}
      {{/toolbar-group}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-chip-container"}}
        {{#> toolbar-group toolbar-group--modifier=""}}
          {{> toolbar-item-chip-group chip-group--label="Status" chip-group--id=(concat toolbar--id '-chip-group-status')}}
          {{> toolbar-item-chip-group chip-group--label="Risk" chip-group--id=(concat toolbar--id '-chip-group-risk')}}
        {{/toolbar-group}}
        {{> toolbar-item-clear}}
      {{/toolbar-group}}
    {{/toolbar-expandable-content}}
  {{/toolbar-content}}
{{/toolbar}}
```

```hbs title=Selected-filters-on-desktop-(not-responsive)
{{#> toolbar toolbar--id="toolbar-selected-filters-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{> toolbar-item-bulk-select}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-toggle-group pf-m-show"}}
        {{> toolbar-toggle toolbar-toggle--IsExpanded="false"}}
        {{#> toolbar-group toolbar-group--modifier="pf-m-filter-group"}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-status') select--IsCheckboxSelect="true"}}
              Status
            {{/select}}
          {{/toolbar-item}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-risk') select--IsCheckboxSelect="true"}}
              Risk
            {{/select}}
          {{/toolbar-item}}
        {{/toolbar-group}}
      {{/toolbar-group}}
      {{> toolbar-icon-button-group-example}}
      {{> toolbar-overflow-menu-example toolbar-overflow-menu-example--content="true" toolbar-overflow-menu-example--control="true"}}
    {{/toolbar-content-section}}
  {{/toolbar-content}}
  {{#> toolbar-content toolbar-content--modifier="pf-m-chip-container"}}
    {{#> toolbar-group toolbar-group--modifier=""}}
      {{> toolbar-item-chip-group chip-group--label="Status" chip-group--id=(concat toolbar--id '-chip-group-status')}}
      {{> toolbar-item-chip-group chip-group--label="Risk" chip-group--id=(concat toolbar--id '-chip-group-risk')}}
    {{/toolbar-group}}
    {{> toolbar-item-clear}}
  {{/toolbar-content}}
{{/toolbar}}
```

### Stacked

```hbs title=Stacked-on-desktop
{{#> toolbar toolbar--id="toolbar-stacked-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-toggle-group pf-m-show-on-2xl"}}
        {{> toolbar-toggle toolbar-toggle--IsExpanded="false"}}
        {{#> toolbar-group newcontext}}
          {{#> toolbar-item toolbar-item--modifier="pf-m-label" toolbar-item--attribute='aria-hidden="true"' toolbar-item--id=(concat toolbar--id '-select-checkbox-resource-label')}}
            Resource
          {{/toolbar-item}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-resource') select--IsCheckboxSelect="true" select--HasCustomLabel="true"}}
              Pod
            {{/select}}
          {{/toolbar-item}}
        {{/toolbar-group}}
        {{#> toolbar-group newcontext}}
          {{#> toolbar-item toolbar-item--modifier="pf-m-label" toolbar-item--attribute='aria-hidden="true"' toolbar-item--id=(concat toolbar--id '-select-checkbox-status-label')}}
            Status
          {{/toolbar-item}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-status') select--IsCheckboxSelect="true" select--HasCustomLabel="true"}}
              Running
            {{/select}}
          {{/toolbar-item}}
        {{/toolbar-group}}
        {{#> toolbar-group newcontext}}
          {{#> toolbar-item toolbar-item--modifier="pf-m-label" toolbar-item--attribute='aria-hidden="true"' toolbar-item--id=(concat toolbar--id '-select-checkbox-type-label')}}
            Type
          {{/toolbar-item}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-type') select--IsCheckboxSelect="true" select--HasCustomLabel="true"}}
              Any
            {{/select}}
          {{/toolbar-item}}
        {{/toolbar-group}}
      {{/toolbar-group}}
      {{> toolbar-icon-button-group-example}}
      {{> toolbar-overflow-menu-example toolbar-overflow-menu-example--content="true" toolbar-overflow-menu-example--control="true"}}
    {{/toolbar-content-section}}
    {{#> toolbar-expandable-content}}{{/toolbar-expandable-content}}
  {{/toolbar-content}}
  {{#> divider}}{{/divider}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{> toolbar-item-bulk-select}}
      {{> toolbar-item-pagination}}
    {{/toolbar-content-section}}
  {{/toolbar-content}}
{{/toolbar}}
```

```hbs title=Stacked-on-mobile-(filters-collapsed,-expandable-content-expanded)
{{#> toolbar toolbar--id="toolbar-stacked-collapsed-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-toggle-group"}}
        {{> toolbar-toggle toolbar-toggle--modifier="pf-m-expanded" toolbar-toggle--IsExpanded="true"}}
      {{/toolbar-group}}
      {{> toolbar-icon-button-group-example}}
      {{> toolbar-overflow-menu-example toolbar-overflow-menu-example--control="true"}}
    {{/toolbar-content-section}}
    {{#> toolbar-expandable-content toolbar-expandable-content--IsExpanded="true"}}
      {{#> toolbar-group}}
        {{#> toolbar-item toolbar-item--modifier="pf-m-label" toolbar-item--id=(concat toolbar--id '-select-checkbox-resource-expanded-label')}}
          Resource
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-resource-expanded') select--IsCheckboxSelect="true" select--HasCustomLabel="true"}}
            Pod
          {{/select}}
        {{/toolbar-item}}
      {{/toolbar-group}}
      {{#> toolbar-group}}
        {{#> toolbar-item toolbar-item--modifier="pf-m-label" toolbar-item--id=(concat toolbar--id '-select-checkbox-status-expanded-label')}}
          Status
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-status-expanded') select--IsCheckboxSelect="true" select--HasCustomLabel="true"}}
            Running
          {{/select}}
        {{/toolbar-item}}
      {{/toolbar-group}}
      {{#> toolbar-group}}
        {{#> toolbar-item toolbar-item--modifier="pf-m-label" toolbar-item--id=(concat toolbar--id '-select-checkbox-type-expanded-label')}}
          Type
        {{/toolbar-item}}
        {{#> toolbar-item}}
          {{#> select id=(concat toolbar--id '-select-checkbox-type-expanded') select--IsCheckboxSelect="true" select--HasCustomLabel="true"}}
            Any
          {{/select}}
        {{/toolbar-item}}
      {{/toolbar-group}}
    {{/toolbar-expandable-content}}
  {{/toolbar-content}}
  {{#> divider}}{{/divider}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{> toolbar-item-bulk-select}}
      {{> toolbar-item-pagination}}
    {{/toolbar-content-section}}
  {{/toolbar-content}}
{{/toolbar}}
```

### Expanded elements

```hbs title=Expanded-elements
{{#> toolbar toolbar--id="toolbar-expanded-elements-example"}}
  {{#> toolbar-content}}
    {{#> toolbar-content-section}}
      {{#> toolbar-item toolbar-item--modifier="pf-m-bulk-select"}}
        {{#> dropdown id=(concat toolbar--id '-dropdown') dropdown--IsBulkSelect="true" dropdown--IsExpanded="true" dropdown--IsSplitButton="true" dropdown-toggle--type="div" dropdown-toggle--modifier="pf-m-split-button"}}
          {{> dropdown-toggle-check aria-label="Select all"}}
          {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
        {{/dropdown}}
      {{/toolbar-item}}
      {{#> toolbar-group toolbar-group--modifier="pf-m-toggle-group pf-m-show-on-xl"}}
        {{> toolbar-toggle toolbar-toggle--IsExpanded="false"}}
        {{> toolbar-item-search-filter button--id="content"}}
        {{#> toolbar-group toolbar-group--modifier="pf-m-filter-group"}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-status') select--IsCheckboxSelect="true" select--IsChecked="true" select--IsExpanded="true"}}
              Status
            {{/select}}
          {{/toolbar-item}}
          {{#> toolbar-item}}
            {{#> select id=(concat toolbar--id '-select-checkbox-risk') select--IsCheckboxSelect="true" select--IsChecked="true" select--IsExpanded="true"}}
              Risk
            {{/select}}
          {{/toolbar-item}}
        {{/toolbar-group}}
      {{/toolbar-group}}
      {{> toolbar-icon-button-group-example}}
      {{> toolbar-overflow-menu-example toolbar-overflow-menu-example--content="true" toolbar-overflow-menu-example--control="true" toolbar-overflow-menu-example--IsExpanded="true"}}
    {{/toolbar-content-section}}
    {{#> toolbar-expandable-content}}{{/toolbar-expandable-content}}
  {{/toolbar-content}}
{{/toolbar}}
```

## Documentation

### Overview

As the toolbar component is a hybrid layout and component, some of its elements are presentational, while some require accessibility support.

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-toolbar` | `<div>` | Initiates the toolbar component. **Required** |
| `.pf-c-toolbar__item` | `<div>` | Initiates a toolbar item. **Required** |
| `.pf-c-toolbar__group` | `<div>` | Initiates a toolbar group. |
| `.pf-c-toolbar__content` | `<div>` | Initiates a toolbar content container. **Required** |
| `.pf-c-toolbar__content-section` | `<div>` | Initiates a toolbar content section. This is used to separate static elements from dynamic elements within a content container. There should be no more than one `.pf-c-toolbar__content-section` per `.pf-c-toolbar__content` **Required** |
| `.pf-c-toolbar__expandable-content` | `<div>` | Initiates a toolbar expandable content section. |
| `.pf-m-expanded` | `.pf-c-toolbar__expandable-content` | Modifies expandable content section for the expanded state. |
| `.pf-m-bulk-select` | `.pf-c-toolbar__item` | Initiates bulk select spacing. |
| `.pf-m-overflow-menu` | `.pf-c-toolbar__item` | Initiates overflow menu spacing. |
| `.pf-m-pagination` | `.pf-c-toolbar__item` | Initiates pagination spacing and margin. |
| `.pf-m-search-filter` | `.pf-c-toolbar__item` | Initiates search filter spacing. |
| `.pf-m-chip-group` | `.pf-c-toolbar__item` | Initiates chip group spacing. |
| `.pf-m-button-group` | `.pf-c-toolbar__group` | Initiates button group spacing. |
| `.pf-m-icon-button-group` | `.pf-c-toolbar__group` | Initiates icon button group spacing. |
| `.pf-m-filter-group` | `.pf-c-toolbar__group` | Initiates filter group spacing. |
| `.pf-m-hidden{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to be hidden, at optional breakpoint. |
| `.pf-m-visible{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to be shown, at optional breakpoint. |
| `.pf-m-align-right{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to align right, at optional breakpoint. |
| `.pf-m-align-left{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to align left, at optional breakpoint. |
| `.pf-m-label` | `.pf-c-toolbar__item` | Modifies toolbar item to label. |
| `.pf-m-chip-container` | `.pf-c-toolbar__content`, `.pf-c-toolbar__group` | Modifies the toolbar element for applied filters layout. |
| `.pf-m-expanded` | `.pf-c-toolbar__expandable-content`, `.pf-c-toolbar__toggle` | Modifies the component for the expanded state. |

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `hidden` | `.pf-c-toolbar__item`, `.pf-c-toolbar__group`, `.pf-c-toolbar__toggle`, `.pf-c-toolbar__expandable-content` |  Indicates that the toolbar element is hidden. **Required** |
| `aria-expanded="true"` | `.pf-c-toolbar__toggle > .pf-c-button` |  Indicates that the expandable content is visible. **Required** |
| `aria-expanded="false"` | `.pf-c-toolbar__toggle > .pf-c-button` |  Indicates the the expandable content is hidden. **Required** |
| `aria-controls="[id of expandable content]"` | `.pf-c-toolbar__toggle > .pf-c-button` |  Identifies the expanded content controlled by the toggle button. **Required** |
| `id="[expandable-content_id]"` | `.pf-c-toolbar__expandable-content` | Provides a reference for toggle button description. **Required** |

### Toggle group usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-toggle-group` | `.pf-c-toolbar__group` | Modifies toolbar group to control when, and at which breakpoint, filters will be hidden/shown. By default, all filters are hidden until the specified breakpoint is reached. |
| `.pf-m-show{-on-[breakpoint]}` | `.pf-c-toolbar__group.pf-m-toggle-group`, `.pf-c-toolbar__expandable-content` | Modifies toolbar element to hidden at breakpoint. This selector must be applied consistently to toggle group and expandable content. |

### Spacer system

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-spacer-{none, sm, md, lg, xl}{-on-[breakpoint]}` | `.pf-c-toolbar__group`, `.pf-c-toolbar__item` | Modifies toolbar group or item spacing. |
| `.pf-m-space-items-{none, sm, md, lg, xl}{-on-[breakpoint]}` | `.pf-c-toolbar__group` | Modifies toolbar group child spacing. |
