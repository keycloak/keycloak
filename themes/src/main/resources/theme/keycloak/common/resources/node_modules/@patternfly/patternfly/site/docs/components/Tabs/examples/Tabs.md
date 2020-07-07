---
title: Tabs
section: components
cssPrefix: pf-c-tabs
---

## Examples

```hbs title=Default
{{#> tabs tabs--id="default-example"}}
  {{> __tabs-list}}
{{/tabs}}
```

```hbs title=Default-overflow-beginning-of-list
{{#> tabs tabs--id="default-overflow-beginning-of-list-example" tabs--modifier="pf-m-scrollable"}}
  {{> __tabs-list __tabs-list--DisabledFirstScrollButton="true" __tabs-list--IsScrollable="true"}}
{{/tabs}}
```

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `disabled` | `.pf-c-tabs__scroll-button` | Indicates that a scroll button is disabled, when at the first or last item of a list. **Required when disabled** |
| `aria-hidden="true"` | `.pf-c-tabs__scroll-button` | Hides the icon from assistive technologies.**Required when not scrollable** |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-scrollable` | `.pf-c-tabs` | Enables the directional scroll buttons. |
| `.pf-c-tabs__scroll-button` | `<button>` | Initiates a tabs component scroll button. |

```hbs title=Vertical
{{#> tabs tabs--id="vertical-example" tabs--modifier="pf-m-vertical"}}
  {{> __tabs-list __tabs-list--NoScrollButtons="true"}}
{{/tabs}}
```

```hbs title=Box
{{#> tabs tabs--id="box-example" tabs--modifier="pf-m-box"}}
  {{> __tabs-list}}
{{/tabs}}
```

```hbs title=Box-overflow
{{#> tabs tabs--id="box-overflow-example" tabs--modifier="pf-m-box pf-m-scrollable" __tabs-list--DisabledFirstScrollButton="true"}}
  {{> __tabs-list __tabs-list--IsScrollable="true"}}
{{/tabs}}
```

```hbs title=Box-vertical
{{#> tabs tabs--id="box-vertical-example" tabs--modifier="pf-m-box pf-m-vertical"}}
  {{> __tabs-list __tabs-list--NoScrollButtons="true"}}
{{/tabs}}
```

```hbs title=Inset
{{#> tabs tabs--id="inset-example" tabs--modifier="pf-m-inset-sm-on-md pf-m-inset-lg-on-lg pf-m-inset-2xl-on-xl"}}
  {{> __tabs-list}}
{{/tabs}}
```

```hbs title=Inset-box
{{#> tabs tabs--id="inset-box-example" tabs--modifier="pf-m-box pf-m-inset-sm-on-md pf-m-inset-lg-on-lg pf-m-inset-2xl-on-xl"}}
  {{> __tabs-list}}
{{/tabs}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-inset-{none, sm, md, lg, xl, 2xl, 3xl}{-on-[sm, md, lg, xl, 2xl]}` | `.pf-c-tabs` | Modifies the tabs component padding/inset to visually match padding of other adjacent components. |

```hbs title=Icons-and-text
{{#> tabs tabs--id="icons-example"}}
  {{> __tabs-list __tabs-list--HasIcons="true"}}
{{/tabs}}
```

```hbs title=Tabs-with-sub-tabs
{{#> tabs tabs--id="default-parent-example" tabs--modifier="pf-m-scrollable"}}
  {{> __tabs-list __tabs-list--IsScrollable="true"}}
{{/tabs}}

{{#> tabs tabs--id="default-child-example" tabs--IsSecondary="true" tabs--modifier="pf-m-scrollable"}}
  {{> __tabs-list-secondary __tabs-list-secondary--IsScrollable="true"}}
{{/tabs}}
```

```hbs title=Box-tabs-with-sub-tabs
{{#> tabs tabs--id="box-parent-example" tabs--modifier="pf-m-box pf-m-scrollable"}}
  {{> __tabs-list __tabs-list--IsScrollable="true"}}
{{/tabs}}

{{#> tabs tabs--id="box-child-example" tabs--IsSecondary="true" tabs--modifier="pf-m-scrollable"}}
  {{> __tabs-list-secondary __tabs-list-secondary--IsScrollable="true"}}
{{/tabs}}
```

```hbs title=Filled
{{#> tabs tabs--id="filled-example" tabs--modifier="pf-m-fill"}}
  {{> __tabs-list __tabs-list--IsShort="true"}}
{{/tabs}}
```

```hbs title=Filled-with-icons
{{#> tabs tabs--id="filled-with-icons-example" tabs--modifier="pf-m-fill"}}
  {{> __tabs-list __tabs-list--HasIcons="true" __tabs-list--IsShort="true"}}
{{/tabs}}
```

```hbs title=Filled-box
{{#> tabs tabs--id="filled-box-example" tabs--modifier="pf-m-fill pf-m-box"}}
  {{> __tabs-list __tabs-list--IsShort="true"}}
{{/tabs}}
```

```hbs title=Filled-box-with-icons
{{#> tabs tabs--id="filled-box-with-icons-example" tabs--modifier="pf-m-fill pf-m-box"}}
  {{> __tabs-list __tabs-list--HasIcons="true" __tabs-list--IsShort="true"}}
{{/tabs}}
```

## Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-fill`  | `.pf-c-tabs` | Modifies the tabs to fill the available space. **Required** |

```hbs title=Using-the-nav-element
{{#> tabs tabs--id="default-scroll-nav-example" tabs--type="nav" tabs--modifier="pf-m-scrollable" tabs--attribute='aria-label="Local"' tabs-link--isLink="true"}}
  {{> __tabs-list __tabs-list--IsScrollable="true"}}
{{/tabs}}
```

```hbs title=Sub-nav-using-the-nav-element
{{#> tabs tabs--id="primary-nav-example" tabs--type="nav" tabs--attribute='aria-label="Local"' tabs-link--isLink="true"}}
  {{> __tabs-list}}
{{/tabs}}

{{#> tabs tabs--id="secondary-nav-example" tabs--type="nav" tabs--attribute='aria-label="Local secondary"' tabs-link--isLink="true" tabs--modifier="pf-m-secondary"}}
  {{> __tabs-list-secondary}}
{{/tabs}}
```

The tabs component should only be used to change content views within a page. The similar-looking but semantically different [horizontal nav component](/documentation/core/components/nav) is available for general navigation use cases.

Tabs should be used with the [tab content component](/documentation/core/components/tabcontent).

Whenever a list of tabs is unique on the current page, it can be used in a `<nav>` element. Cases where the same set of tabs are duplicated in multiple regions on a page (e.g. cards on a dashboard) are less likely to benefit from using the `<nav>` element.

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-label="Descriptive text"` | `nav.pf-c-tabs`, `nav.pf-c-tabs.pf-m-secondary` | Gives the `<nav>` an accessible label. **Required when `.pf-c-tabs` is used with `<nav>`**
| `aria-label="Descriptive text"` | `.pf-c-inline-edit__toggle > button` | Provides an accessible description for toggle button. **Required**
| `disabled` | `.pf-c-tabs__scroll-button` | Indicates that a scroll button is disable, typically when at the first or last item of a list or scroll buttons are hidden. **Required** |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-tabs` | `<nav>`, `<div>` | Initiates the tabs component. **Required** |
| `.pf-c-tabs__list` | `<ul>` | Initiates a tabs component list. **Required** |
| `.pf-c-tabs__item` | `<li>` | Initiates a tabs component item. **Required** |
| `.pf-c-tabs__item-text` | `<span>` | Initiates a tabs component item icon. **Required** |
| `.pf-c-tabs__item-icon` | `<span>` | Initiates a tabs component item text. **Required** |
| `.pf-c-tabs__link` | `<button>`, `<a>` | Initiates a tabs component link. **Required** |
| `.pf-c-tabs__scroll-button` | `<button>` | Initiates a tabs component scroll button. |
| `.pf-m-secondary` | `.pf-c-tabs` | Applies secondary styling to the tab component. |
| `.pf-m-no-border-bottom` | `.pf-c-tabs` | Removes bottom border from a tab component. |
| `.pf-m-box` | `.pf-c-tabs` | Applies box styling to the tab component. |
| `.pf-m-vertical` | `.pf-c-tabs` | Applies vertical styling to the tab component. |
| `.pf-m-fill` | `.pf-c-tabs` | Modifies the tabs to fill the available space. |
| `.pf-m-current` | `.pf-c-tabs__item` | Indicates that a tab item is currently selected. |
| `.pf-m-inset-{none, sm, md, lg, xl, 2xl}{-on-[md, lg, xl, 2xl]}` | `.pf-c-tabs` | Modifies the tabs component padding/inset to visually match padding of other adjacent components. |
