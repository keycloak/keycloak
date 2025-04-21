---
id: Divider
section: components
cssPrefix: pf-c-divider
---import './Divider.css'

## Examples

### hr

```html
<hr class="pf-c-divider" />

```

### li

```html
<ul>
  <li>List item one</li>
  <li class="pf-c-divider" role="separator"></li>
  <li>List item two</li>
</ul>

```

### div

```html
<div class="pf-c-divider" role="separator"></div>

```

### Inset medium

```html
<div class="pf-c-divider pf-m-inset-md" role="separator"></div>

```

### Md inset, no inset on md, 3xl inset on lg, lg inset on xl

```html
<div
  class="pf-c-divider pf-m-inset-md pf-m-inset-none-on-md pf-m-inset-3xl-on-lg pf-m-inset-lg-on-xl"
  role="separator"
></div>

```

### Vertical

```html
<div class="pf-c-divider pf-m-vertical pf-m-inset-md" role="separator"></div>

```

### Vertical, inset medium

```html
<div class="pf-c-divider pf-m-vertical pf-m-inset-md" role="separator"></div>

```

### Vertical, md inset, no inset on md, lg inset on lg, sm inset on xl

```html
<div
  class="pf-c-divider pf-m-vertical pf-m-inset-md pf-m-inset-none-on-md pf-m-inset-lg-on-lg pf-m-inset-sm-on-xl"
  role="separator"
></div>

```

### Vertical on lg

```html
<div class="pf-c-divider pf-m-horizontal pf-m-vertical-on-lg" role="separator"></div>

```

### Horizontal on lg

```html
<div class="pf-c-divider pf-m-horizontal-on-lg pf-m-vertical" role="separator"></div>

```

## Documentation

### Overview

The divider renders as an `<hr>` by default. It is possible to make the divider render as an `li` or a `div` to match the HTML5 specification and context of the divider.

| Attribute          | Applied to                            | Outcome                                      |
| ------------------ | ------------------------------------- | -------------------------------------------- |
| `role="separator"` | `li.pf-c-divider`, `div.pf-c-divider` | Indicates that the separator is a separator. |

### Usage

| Class                                                                     | Applied to              | Outcome                                                                                                                                                                           |
| ------------------------------------------------------------------------- | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-divider`                                                           | `<hr>`, `<li>`, `<div>` | Defines the divider component.                                                                                                                                                    |
| `.pf-m-vertical`                                                          | `.pf-c-divider`         | Modifies the divider component from horizontal to vertical. This modifier requires that the parent has an explicit or implicit height, or has a flex or grid based layout parent. |
| `.pf-m-inset-{none, sm, md, lg, xl, 2xl, 3xl}{-on-[sm, md, lg, xl, 2xl]}` | `.pf-c-divider`         | Modifies divider padding/inset to visually match padding of other adjacent components.                                                                                            |
| `.pf-m-hidden{-on-[breakpoint]}`                                          | `.pf-c-divider`         | Modifies a divider to be hidden, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                     |
| `.pf-m-visible{-on-[breakpoint]}`                                         | `.pf-c-divider`         | Modifies a divider to be shown, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                      |
