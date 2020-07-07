---
title: Overflow menu
section: components
cssPrefix: pf-c-overflow-menu
---

import './OverflowMenu.css'

## Introduction
The overflow menu component condenses actions inside `.pf-c-overflow-menu__content` container into a single dropdown button wrapped in `.pf-c-overflow-menu__control`.

The overflow menu relies on groups (`.pf-c-overflow-menu__group`) and items (`.pf-c-overflow-menu__item`), with default spacer values. Groups and items can be siblings and/or items can be nested within groups. Modifier selectors adjust spacing based on the type of group. Each modifier applies a unique CSS variable, therefore, the base spacer value for all elements can be customized and item/groups spacers can be themed individually. The default spacer value for items and groups is set to `--pf-c-toolbar--spacer--base`, whose value is `--pf-global--spacer--md` or 16px.

```hbs title=Simple-collapsed
{{#> overflow-menu overflow-menu--id="overflow-menu-simple"}}
  {{#> overflow-menu-control dropdown--IsExpanded="true" overflow-menu-button--aria-label="Generic options"}}
    {{#> overflow-menu-dropdown-item}}
      Item 1
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Item 2
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Item 3
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Item 4
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Item 5
    {{/overflow-menu-dropdown-item}}
  {{/overflow-menu-control}}
{{/overflow-menu}}
```

```hbs title=Simple-expanded
{{#> overflow-menu overflow-menu--id="overflow-menu-simple-expanded"}}
  {{#> overflow-menu-content}}
    {{#> overflow-menu-item}}
      Item 1
    {{/overflow-menu-item}}
    {{#> overflow-menu-item}}
      Item 2
    {{/overflow-menu-item}}
    {{#> overflow-menu-group}}
      {{#> overflow-menu-item}}
        Item 3
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        Item 4
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        Item 5
      {{/overflow-menu-item}}
    {{/overflow-menu-group}}
  {{/overflow-menu-content}}
{{/overflow-menu}}
```

### Default spacing for items and groups:

| Class | CSS Variable | Computed Value |
| -- | -- | -- |
| `.pf-c-overflow-menu__group` | `--pf-c-overflow-menu__group--spacer` | `16px` |
| `.pf-c-overflow-menu__item` | `--pf-c-overflow-menu__item--spacer` | `16px` |


### Overflow menu item types

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-overflow-menu` | `<div>` | Initiates an overflow menu. **Required** |
| `.pf-c-overflow-menu__content` | `<div>` | Initiates an overflow menu content section. **Required** |
| `.pf-c-overflow-menu__control` | `<div>` | Initiates the overflow menu control. **Required** |
| `.pf-c-overflow-menu__group` | `<div>` | Initiates an overflow menu group. |
| `.pf-c-overflow-menu__item` | `<div>` | Initiates an overflow menu item. **Required** |

```hbs title=Group-types
{{#> overflow-menu overflow-menu--id="overflow-menu-button-group-example" overflow-menu-button--aria-label="Options"}}
  {{#> overflow-menu-content}}
    {{#> overflow-menu-group}}
      {{#> overflow-menu-item}}
        Item 1
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        Item 2
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        Item 3
      {{/overflow-menu-item}}
    {{/overflow-menu-group}}
    {{#> overflow-menu-group overflow-menu-group--modifier="pf-m-button-group"}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-primary"}}
          Primary
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-secondary"}}
          Secondary
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-tertiary"}}
          Tertiary
        {{/button}}
      {{/overflow-menu-item}}
    {{/overflow-menu-group}}
    {{#> overflow-menu-group overflow-menu-group--modifier="pf-m-icon-button-group"}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Align left"'}}
          <i class="fas fa-align-left" aria-hidden="true"></i>
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Align center"'}}
          <i class="fas fa-align-center" aria-hidden="true"></i>
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Align right"'}}
          <i class="fas fa-align-right" aria-hidden="true"></i>
        {{/button}}
      {{/overflow-menu-item}}
    {{/overflow-menu-group}}
  {{/overflow-menu-content}}
{{/overflow-menu}}
```

The action group consists of a primary and secondary action. Any additional actions are part of the overflow control dropdown.

### Overflow menu group types

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-overflow-menu__group` | `<div>` | Initiates an overflow menu component group. |
| `.pf-m-button-group` | `.pf-c-overflow-menu__group` | Modifies overflow menu group spacing. Spacer value is set to `var(--pf-c-overflow-menu__group--m-button-group--spacer)`. Child `.pf-c-button` spacer value is set to `var(--pf-c-overflow-menu__group--m-button-group--space-items)`. |
| `.pf-m-icon-button-group` | `.pf-c-overflow-menu__group` | Modifies overflow menu group spacing. Spacer value is set to `var(--pf-c-overflow-menu__group--m-icon-button-group--spacer)`. Child `.pf-c-button.pf-m-button-plain` spacer value is set to `var(--pf-c-overflow-menu__group--m-icon-button-group--space-items)`. |

```hbs title=Additional-options-in-dropdown-(hidden)
{{#> overflow-menu overflow-menu--id="overflow-menu-simple-additional-options-hidden"}}
  {{#> overflow-menu-control dropdown--IsExpanded="true" overflow-menu-button--aria-label="Dropdown with additional options"}}
    {{#> overflow-menu-dropdown-item}}
      Primary
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Secondary
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Tertiary
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Align left
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Align center
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Align right
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Action 7
    {{/overflow-menu-dropdown-item}}
  {{/overflow-menu-control}}
{{/overflow-menu}}
```

```hbs title=Additional-options-in-dropdown-(visible)
{{#> overflow-menu overflow-menu--id="overflow-menu-simple-additional-options-visible"}}
  {{#> overflow-menu-content}}
    {{#> overflow-menu-group overflow-menu-group--modifier="pf-m-button-group"}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-primary"}}
          Primary
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-secondary"}}
          Secondary
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-tertiary"}}
          Tertiary
        {{/button}}
      {{/overflow-menu-item}}
    {{/overflow-menu-group}}
    {{#> overflow-menu-group overflow-menu-group--modifier="pf-m-icon-button-group"}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Align left"'}}
          <i class="fas fa-align-left" aria-hidden="true"></i>
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Align center"'}}
          <i class="fas fa-align-center" aria-hidden="true"></i>
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Align right"'}}
          <i class="fas fa-align-right" aria-hidden="true"></i>
        {{/button}}
      {{/overflow-menu-item}}
    {{/overflow-menu-group}}
  {{/overflow-menu-content}}
  {{#> overflow-menu-control dropdown--IsExpanded="true" overflow-menu-button--aria-label="Dropdown with additional options"}}
    {{#> overflow-menu-dropdown-item}}
      Action 7
    {{/overflow-menu-dropdown-item}}
  {{/overflow-menu-control}}
{{/overflow-menu}}
```

## Persistent configuration

```hbs title=Persistent-additional-options-(hidden)
{{#> overflow-menu overflow-menu--id="overflow-menu-persistent-hidden"}}
  {{#> overflow-menu-content}}
    {{#> overflow-menu-group overflow-menu-group--modifier="pf-m-button-group"}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-primary"}}
          Primary
        {{/button}}
      {{/overflow-menu-item}}
    {{/overflow-menu-group}}
  {{/overflow-menu-content}}
  {{#> overflow-menu-control dropdown--IsExpanded="true" overflow-menu-button--aria-label="Dropdown for persistent example"}}
    {{#> overflow-menu-dropdown-item}}
      Secondary
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Tertiary
    {{/overflow-menu-dropdown-item}}
    {{#> overflow-menu-dropdown-item}}
      Action 4
    {{/overflow-menu-dropdown-item}}
  {{/overflow-menu-control}}
{{/overflow-menu}}
```

```hbs title=Persistent-additional-options-(visible)
{{#> overflow-menu overflow-menu--id="overflow-menu-persistent-visible-example"}}
  {{#> overflow-menu-content}}
    {{#> overflow-menu-group overflow-menu-group--modifier="pf-m-button-group"}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-primary"}}
          Primary
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-secondary"}}
          Secondary
        {{/button}}
      {{/overflow-menu-item}}
      {{#> overflow-menu-item}}
        {{#> button button--modifier="pf-m-tertiary"}}
          Tertiary
        {{/button}}
      {{/overflow-menu-item}}
    {{/overflow-menu-group}}
  {{/overflow-menu-content}}
  {{#> overflow-menu-control dropdown--IsExpanded="true" overflow-menu-button--aria-label="Dropdown for persistent example"}}
    {{#> overflow-menu-dropdown-item}}
      Action 4
    {{/overflow-menu-dropdown-item}}
  {{/overflow-menu-control}}
{{/overflow-menu}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-overflow-menu` | `<div>` | Initiates an overflow menu. **Required** |
| `.pf-c-overflow-menu__content` | `<div>` | Initiates an overflow menu content section. **Required** |
| `.pf-c-overflow-menu__control` | `<div>` | Initiates the overflow menu control. **Required** |
| `.pf-c-overflow-menu__group` | `<div>` | Initiates an overflow menu group. |
| `.pf-c-overflow-menu__item` | `<div>` | Initiates an overflow menu item. **Required** |
| `.pf-m-button-group` | `.pf-c-overflow-menu__group` | Modifies overflow menu group spacing. Spacer value is set to `var(--pf-c-overflow-menu__group--m-button-group--spacer)`. Child spacer value is set to `var(--pf-c-overflow-menu__group--m-button-group--space-items)`. |
| `.pf-m-icon-button-group` | `.pf-c-overflow-menu__group` | Modifies overflow menu group spacing. Spacer value is set to `var(--pf-c-overflow-menu__group--m-icon-button-group--spacer)`. Child spacer value is set to `var(--pf-c-overflow-menu__group--m-icon-button-group--space-items)`. |
