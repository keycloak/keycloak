---
title: Spacing
section: utilities
---

import './Spacing.css'

## Examples
```hbs title=Margin
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Margin, marginX, marginY
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start pf-u-flex-wrap"}}
  {{#> flex-item flex-item--modifier="pf-u-m-xl"}}
    Margin - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-mx-xl"}}
    Margin x axis - pf-u-mx-xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-my-xl"}}
    Margin y axis - pf-u-my-xl
  {{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Margin top, margin right, margin bottom, margin left
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start pf-u-flex-wrap"}}
  {{#> flex-item flex-item--modifier="pf-u-mt-xl"}}
    Margin-top - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-mr-xl"}}
    Margin-right - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-mb-xl"}}
    Margin-bottom - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-ml-xl"}}
    Margin-left - xl
  {{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Responsive margin
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start pf-u-flex-column pf-u-flex-wrap"}}
  {{#> flex-item flex-item--modifier="pf-u-mt-xl"}}
    Margin-top - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-mt-xl-on-sm"}}
    Margin-top - xl on-sm
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-mt-xl-on-md"}}
    Margin-top - xl on-md
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-mt-xl-on-lg"}}
    Margin-top - xl on-lg
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-mt-xl-on-xl"}}
    Margin-top - xl on-xl
  {{/flex-item}}
{{/display}}
```

```hbs title=Padding
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Padding, paddingX, paddingY
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start pf-u-flex-wrap"}}
  {{#> flex-item flex-item--modifier="pf-u-p-xl"}}
    Padding - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-px-xl"}}
    Padding x axis - pf-u-px-xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-py-xl"}}
    Padding y axis - pf-u-py-xl
  {{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Padding top, padding right, padding bottom, padding left
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start pf-u-flex-wrap"}}
  {{#> flex-item flex-item--modifier="pf-u-pt-xl"}}
    Padding-top - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-pr-xl"}}
    Padding-right - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-pb-xl"}}
    Padding-bottom - xl
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-pl-xl"}}
    Padding-left - xl
  {{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Responsive padding
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start pf-u-flex-column pf-u-flex-wrap"}}
  {{#> flex-item flex-item--modifier="pf-u-pt-xl"}}
    Padding-top
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-pt-xl-on-sm"}}
    Padding-top - xl on-sm
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-pt-xl-on-md"}}
    Padding-top - xl on-md
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-pt-xl-on-lg"}}
    Padding-top - xl on-lg
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-pt-xl-on-xl"}}
    Padding-top - xl on-xl
  {{/flex-item}}
{{/display}}
```

```hbs title=Combined
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Combined spacers
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start pf-u-flex-wrap"}}
  {{#> flex-item flex-item--modifier="pf-u-p-lg pf-u-m-lg"}}
    Padding lg, margin lg
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-p-lg-on-lg pf-u-m-lg"}}
    Padding lg on-lg, margin lg
  {{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Combined spacers (responsive)
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start pf-u-flex-wrap"}}
  {{#> flex-item flex-item--modifier="pf-u-p-sm pf-u-p-lg-on-md pf-u-mr-lg"}}
    Item 1
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-p-sm pf-u-p-lg-on-md pf-u-mr-lg"}}
    Item 2
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-p-sm pf-u-p-lg-on-md pf-u-ml-auto-on-xl"}}
    Item 3
  {{/flex-item}}
{{/display}}
```

## Documentation
### Overview
Breakpoint is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example .pf-u-m-sm-on-lg**

### Margin properties
| Abbreviation | Property |
| -- | -- |
| `m` |  margin |
| `mt` | margin-top |
| `mr` | margin-right |
| `mb` | margin-bottom |
| `ml` | margin-left |
| `mx` | margin-left & margin-right |
| `my` | margin-top & margin-bottom |

### Padding properties
| Abbreviation | Property |
| -- | -- |
| `p` |  padding |
| `pt` | padding-top |
| `pr` | padding-right |
| `pb` | padding-bottom |
| `pl` | padding-left |
| `px` | padding-left & padding-right |
| `py` | padding-top & padding-bottom |

### Size values
| Abbreviation | Property |
| -- | -- |
| `auto` |  auto |
| `0` | 0 |
| `xs` | var(--pf-global--spacer--xs) |
| `sm` | var(--pf-global--spacer--sm) |
| `md` | var(--pf-global--spacer--md) |
| `lg` | var(--pf-global--spacer--lg) |
| `xl` | var(--pf-global--spacer--xl) |
| `2xl` | var(--pf-global--spacer--2xl) |
| `3xl` | var(--pf-global--spacer--3xl) |
| `4xl` | var(--pf-global--spacer--4xl) |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-u-{m, mt, mr, mb, ml, mx, my}-{size}{-on-[breakpoint]}`  | `*` |  Sets margin top, right, bottom, left, margin-left & right, margin-top & bottom to spacer value |
| `.pf-u-{p, pt, pr, pb, pl, px, py}-{size}{-on-[breakpoint]}`  | `*` |  Sets padding top, right, bottom, left, padding-left & right, padding-top & bottom to spacer value |
