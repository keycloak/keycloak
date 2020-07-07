---
title: Dropdown
section: components
cssPrefix: pf-c-dropdown
---

import './Dropdown.css'

## Examples

```hbs title=Expanded
{{#> dropdown id="dropdown-expanded" dropdown--IsActionMenu="true" dropdown--IsExpanded="true" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Expanded dropdown
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=Collapsed
{{#> dropdown id="dropdown-collapsed" dropdown--IsActionMenu="true" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Collapsed dropdown
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=Disabled
{{#> dropdown id="dropdown-disabled" dropdown--IsActionMenu="true" dropdown--HasToggleIcon="true" dropdown-toggle--IsDisabled="true"}}
  {{#> dropdown-toggle-text}}
    Disabled dropdown
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=Kebab
{{#> dropdown id="dropdown-kebab-disabled" dropdown--IsActionMenu="true" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions" dropdown-toggle--IsDisabled="true"}}{{/dropdown}}
{{#> dropdown id="dropdown-kebab" dropdown--IsActionMenu="true" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}{{/dropdown}}
{{#> dropdown id="dropdown-kebab-expanded" dropdown--IsActionMenu="true" dropdown--IsExpanded="true" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}{{/dropdown}}
```

```hbs title=Kebab-align-right
{{#> dropdown id="dropdown-kebab-align-right" dropdown--IsActionMenu="true" dropdown--IsExpanded="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
{{/dropdown}}
```

```hbs title=Align-right
{{#> dropdown id="dropdown-align-right" dropdown--IsActionMenu="true" dropdown--IsExpanded="true" dropdown--HasToggleIcon="true" dropdown-menu--modifier="pf-m-align-right"}}
  {{#> dropdown-toggle-text}}
    Right
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=Align-top
{{#> dropdown id="dropdown-align-top" dropdown--IsActionMenu="true" dropdown--modifier="pf-m-top" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Top
  {{/dropdown-toggle-text}}
{{/dropdown}}
{{#> dropdown id="dropdown-align-top-expanded" dropdown--IsActionMenu="true" dropdown--IsExpanded="true" dropdown--modifier="pf-m-top" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Top
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=Menu-item-icons
{{#> dropdown id="dropdown-menu-item-icons" dropdown--IsActionMenu="true" dropdown--IsExpanded="true" dropdown--HasItemIcons="true" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Expanded dropdown
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=Split-button-(checkbox)
{{#> dropdown id="dropdown-split-button-disabled" dropdown--IsSplitButton="true" dropdown-toggle--IsDisabled="true" dropdown-toggle--type="div" dropdown-toggle--modifier="pf-m-split-button"}}
  {{> dropdown-toggle-check aria-label="Select all"}}
  {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
{{/dropdown}}

{{#> dropdown id="dropdown-split-button" dropdown--IsSplitButton="true" dropdown-toggle--type="div" dropdown-toggle--modifier="pf-m-split-button"}}
  {{> dropdown-toggle-check aria-label="Select all"}}
  {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
{{/dropdown}}

{{#> dropdown id="dropdown-split-button-expanded" dropdown--IsExpanded="true" dropdown--IsSplitButton="true" dropdown-toggle--type="div" dropdown-toggle--modifier="pf-m-split-button"}}
  {{> dropdown-toggle-check aria-label="Select all"}}
  {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
{{/dropdown}}
```

```hbs title=Split-button-(checkbox-with-toggle-text)
{{#> dropdown id="dropdown-split-button-text" dropdown--IsSplitButton="true" dropdown--IsSplitButtonText="10 selected" dropdown--CheckboxIsChecked="true" dropdown--IsBulkSelect="true" dropdown-toggle--type="div" dropdown-toggle--modifier="pf-m-split-button"}}
  {{> dropdown-toggle-check aria-label="Unselect all"}}
  {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
{{/dropdown}}
```

```hbs title=Split-button-(action)
{{#> dropdown id="dropdown-split-button-action" dropdown--IsSplitButton="true" dropdown-toggle--type="div" dropdown-toggle--modifier="pf-m-split-button pf-m-action"}}
  {{#> dropdown-toggle-button}}
    Action
  {{/dropdown-toggle-button}}
  {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
{{/dropdown}}

{{#> dropdown id="dropdown-split-button-action-expanded" dropdown--IsExpanded="true" dropdown--IsSplitButton="true" dropdown-toggle--type="div" dropdown-toggle--modifier="pf-m-split-button pf-m-action"}}
  {{#> dropdown-toggle-button}}
    Action
  {{/dropdown-toggle-button}}
  {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
{{/dropdown}}

{{#> dropdown id="dropdown-split-button-action-icon" dropdown--IsSplitButton="true" dropdown-toggle--type="div" dropdown-toggle--modifier="pf-m-split-button pf-m-action"}}
  {{#> dropdown-toggle-button aria-label="Settings"}}
    <i class="fas fa-cog" aria-hidden="true"></i>
  {{/dropdown-toggle-button}}
  {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
{{/dropdown}}

{{#> dropdown id="dropdown-split-button-action-icon-expanded" dropdown--IsExpanded="true" dropdown--IsSplitButton="true" dropdown-toggle--type="div" dropdown--HasItemIcons="true" dropdown-toggle--modifier="pf-m-split-button pf-m-action"}}
  {{#> dropdown-toggle-button aria-label="Settings"}}
    <i class="fas fa-cog" aria-hidden="true"></i>
  {{/dropdown-toggle-button}}
  {{> dropdown-toggle-button dropdown--IsToggleButton="true" aria-label="Select"}}
{{/dropdown}}
```

```hbs title=With-groups
{{#> dropdown id="dropdown-groups" dropdown--IsExpanded="true" dropdown--HasToggleIcon="true" dropdown--IsGroupsMenu="true"}}
  {{#> dropdown-toggle-text}}
    Groups
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=With-groups-and-dividers-between-groups
{{#> dropdown id="dropdown-groups-and-dividers-between-groups" dropdown--IsExpanded="true" dropdown--HasToggleIcon="true" dropdown--IsGroupsMenu="true" dropdown--HasDividersGroups="true"}}
  {{#> dropdown-toggle-text}}
    Groups
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=With-groups-and-dividers-between-items
{{#> dropdown id="dropdown-groups-and-dividers-between-items" dropdown--IsExpanded="true" dropdown--HasToggleIcon="true" dropdown--IsGroupsMenu="true" dropdown--HasDividersItems="true"}}
  {{#> dropdown-toggle-text}}
    Groups
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=Panel
{{#> dropdown id="dropdown-panel" dropdown--IsExpanded="true" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Expanded dropdown
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

The dropdown panel is provided for flexibility in allowing various content within a dropdown.

```hbs title=Primary-toggle
{{#> dropdown id="dropdown-primary-toggle" dropdown-toggle--modifier="pf-m-primary" dropdown--IsActionMenu="true" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Collapsed dropdown
  {{/dropdown-toggle-text}}
{{/dropdown}}
&nbsp;
{{#> dropdown id="dropdown-primary-toggle-expanded" dropdown-toggle--modifier="pf-m-primary" dropdown--IsActionMenu="true" dropdown--IsExpanded="true" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Expanded dropdown
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

```hbs title=Dropdown-with-image-and-text
{{#> dropdown id="dropdown-with-image-and-text-example" dropdown--IsMenuToggleImageText="true" dropdown--IsExpanded="true"}}
  {{#> dropdown-toggle-image}}
    {{> avatar avatar--attribute='src="/assets/images/img_avatar.svg" alt="Avatar image"'}}
  {{/dropdown-toggle-image}}
  {{#> dropdown-toggle-text}}
    Ned Username
  {{/dropdown-toggle-text}}
  {{> dropdown-toggle-icon}}
{{/dropdown}}
```

```hbs title=Dropdown-with-description
{{#> dropdown id="dropdown-with-description" dropdown--IsDescriptionMenu="true" dropdown--IsExpanded="true" dropdown--HasToggleIcon="true"}}
  {{#> dropdown-toggle-text}}
    Expanded dropdown
  {{/dropdown-toggle-text}}
{{/dropdown}}
```

## Documentation

### Overview

The dropdown menu can contain either links or buttons, depending on the expected behavior when clicking the menu item. If you are using the menu item to navigate to another page, then menu item is a link. Otherwise, use a button for the menu item.

### Accessibility

| Attribute | Applied | Outcome |
| -- | -- | -- |
| `aria-expanded="false"` | `.pf-c-dropdown__toggle`, `.pf-c-dropdown__toggle-check`, `.pf-c-dropdown__toggle-button` |  Indicates that the menu is hidden. |
| `aria-expanded="true"` | `.pf-c-dropdown__toggle`, `.pf-c-dropdown__toggle-check`, `.pf-c-dropdown__toggle-button` |  Indicates that the menu is visible. |
| `aria-label="Actions"` | `.pf-c-dropdown__toggle`, `.pf-c-dropdown__toggle-check`, `.pf-c-dropdown__toggle-button` | Provides an accessible name for the dropdown when an icon is used instead of text. **Required when icon is used with no supporting text**. |
| `aria-hidden="true"` | `.pf-c-dropdown__toggle-icon`, `<i>`, `.pf-c-dropdown__toggle-check .pf-c-dropdown__toggle-text` | Hides the icon from assistive technologies. |
| `hidden` | `.pf-c-dropdown__menu` | Indicates that the menu is hidden so that it isn't visible in the UI and isn't accessed by assistive technologies. |
| `aria-labelledby="{toggle button id}"` | `.pf-c-dropdown__menu` | Gives the menu an accessible name by referring to the element that toggles the menu. |
| `aria-labelledby="{checkbox id} {toggle text id}"` | `.pf-m-split-button .pf-c-dropdown__toggle-check > input[type="checkbox"]` | Gives the checkbox an accessible name by referring to the element by which it is described. |
| `disabled` | `.pf-c-dropdown__toggle`, `.pf-c-dropdown__toggle-button`, `.pf-c-dropdown__toggle-check > input[type="checkbox"]` | Disables the dropdown toggle and removes it from keyboard focus. |
| `disabled` | `button.pf-c-dropdown__menu-item` | When the menu item uses a button element, indicates that it is unavailable and removes it from keyboard focus. |
| `aria-disabled="true"` | `a.pf-c-dropdown__menu-item` | When the menu item uses a link element, indicates that it is unavailable. |
| `tabindex="-1"` | `a.pf-c-dropdown__menu-item` | When the menu item uses a link element, removes it from keyboard focus. |

### Usage

| Class | Applied | Outcome |
| -- | -- | -- |
| `.pf-c-dropdown` | `<div>` | Defines the parent wrapper of the dropdown. |
| `.pf-c-dropdown__toggle` | `<button>` | Defines the dropdown toggle. |
| `.pf-c-dropdown__toggle-icon` | `<span>` | Defines the dropdown toggle icon. |
| `.pf-c-dropdown__toggle-text` | `<span>` | Defines the dropdown toggle text. **Required when text is present, adds truncation**. |
| `.pf-c-dropdown__toggle-check` | `<label>` | Defines a checkbox in the toggle area of a split button dropdown. |
| `.pf-c-dropdown__toggle-button` | `<button>` | Defines the toggle button for a split button dropdown. |
| `.pf-c-dropdown__menu` | `<ul>`, `<div>` | Defines the parent wrapper of the menu items. |
| `.pf-c-dropdown__menu-item` | `<a>` | Defines a menu item that navigates to another page. |
| `.pf-c-dropdown__menu-item-icon` | `<span>` | Defines the wrapper for the menu item icon. |
| `.pf-c-dropdown__menu-item-description` | `<div>` | Defines the wrapper for the menu item description. |
| `.pf-c-dropdown__menu-item-main` | `<div>` | Defines the wrapper for the menu item main element. Use when the description element is present. |
| `.pf-c-dropdown__toggle-image` | `<span>` | Defines the wrapper for the dropdown toggle button image. |
| `.pf-c-dropdown__menu-item` | `<button>` | Defines a menu item that performs an action on the current page. |
| `.pf-c-dropdown__group` | `<section>` | Defines a group of items in a dropdown. **Required when there is more than one group in a dropdown**. |
| `.pf-c-dropdown__group-title` | `<h1>` | Defines the title for a group of items in the dropdown menu. |
| `.pf-m-expanded` | `.pf-c-dropdown` | Modifies for the expanded state. |
| `.pf-m-top` | `.pf-c-dropdown` | Modifies to display the menu above the toggle. |
| `.pf-m-align-right` | `.pf-c-dropdown__menu` | Modifies to display the menu aligned to the right edge of the toggle. |
| `.pf-m-split-button` | `.pf-c-dropdown__toggle` | Modifies the dropdown toggle area to allow for interactive elements. |
| `.pf-m-action` | `.pf-c-dropdown__toggle.pf-m-split-button` | Modifies the dropdown toggle for when an action is placed beside a toggle button in a split button dropdown. |
| `.pf-m-text` | `.pf-c-dropdown__menu-item` | Modifies a menu item to be non-interactive text. |
| `.pf-m-plain` | `.pf-c-dropdown__toggle` | Modifies to display the toggle with no border. |
| `.pf-m-primary` | `.pf-c-dropdown__toggle` | Modifies to display the toggle with primary styles. |
| `.pf-m-disabled` | `a.pf-c-dropdown__menu-item` | Modifies to display the menu item as disabled. This applies to `a.pf-c-dropdown__menu-item` and should not be used in lieu of the `disabled` attribute on `button.pf-c-dropdown__menu-item`. |
| `.pf-m-disabled` | `div.pf-c-dropdown__toggle` | Modifies to display the dropdown toggle as disabled. This applies to `div.pf-c-dropdown__toggle` and should not be used in lieu of the `disabled` attribute on `button.pf-c-dropdown__toggle`. When this is used, `disabled` should also be added to any form elements in `div.pf-c-dropdown__toggle`. |
| `.pf-m-icon` | `.pf-c-dropdown__menu-item` | Modifies an item to support adding an icon. |
| `.pf-m-active` | `.pf-c-dropdown__toggle` | Modifies the dropdown menu toggle for the active state. |
| `.pf-m-description` | `.pf-c-dropdown__menu-item` | Modifies an item to support adding a description. |