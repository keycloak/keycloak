---
id: Spacing
section: utilities
---import './Spacing.css'

## Examples

### Margin

```html
<h2 class="pf-c-title pf-m-lg">Margin, marginX, marginY</h2>
<div class="pf-u-display-flex pf-u-align-items-flex-start pf-u-flex-wrap">
  <div class="ws-example-flex-item pf-u-m-xl">Margin - xl</div>
  <div class="ws-example-flex-item pf-u-mx-xl">Margin x axis - pf-u-mx-xl</div>
  <div class="ws-example-flex-item pf-u-my-xl">Margin y axis - pf-u-my-xl</div>
</div>
<br />
<h2
  class="pf-c-title pf-m-lg"
>Margin top, margin right, margin bottom, margin left</h2>
<div class="pf-u-display-flex pf-u-align-items-flex-start pf-u-flex-wrap">
  <div class="ws-example-flex-item pf-u-mt-xl">Margin-top - xl</div>
  <div class="ws-example-flex-item pf-u-mr-xl">Margin-right - xl</div>
  <div class="ws-example-flex-item pf-u-mb-xl">Margin-bottom - xl</div>
  <div class="ws-example-flex-item pf-u-ml-xl">Margin-left - xl</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Responsive margin</h2>
<div
  class="pf-u-display-flex pf-u-align-items-flex-start pf-u-flex-column pf-u-flex-wrap"
>
  <div class="ws-example-flex-item pf-u-mt-xl">Margin-top - xl</div>
  <div class="ws-example-flex-item pf-u-mt-xl-on-sm">Margin-top - xl on-sm</div>
  <div class="ws-example-flex-item pf-u-mt-xl-on-md">Margin-top - xl on-md</div>
  <div class="ws-example-flex-item pf-u-mt-xl-on-lg">Margin-top - xl on-lg</div>
  <div class="ws-example-flex-item pf-u-mt-xl-on-xl">Margin-top - xl on-xl</div>
</div>

```

### Padding

```html
<h2 class="pf-c-title pf-m-lg">Padding, paddingX, paddingY</h2>
<div class="pf-u-display-flex pf-u-align-items-flex-start pf-u-flex-wrap">
  <div class="ws-example-flex-item pf-u-p-xl">Padding - xl</div>
  <div class="ws-example-flex-item pf-u-px-xl">Padding x axis - pf-u-px-xl</div>
  <div class="ws-example-flex-item pf-u-py-xl">Padding y axis - pf-u-py-xl</div>
</div>
<br />
<h2
  class="pf-c-title pf-m-lg"
>Padding top, padding right, padding bottom, padding left</h2>
<div class="pf-u-display-flex pf-u-align-items-flex-start pf-u-flex-wrap">
  <div class="ws-example-flex-item pf-u-pt-xl">Padding-top - xl</div>
  <div class="ws-example-flex-item pf-u-pr-xl">Padding-right - xl</div>
  <div class="ws-example-flex-item pf-u-pb-xl">Padding-bottom - xl</div>
  <div class="ws-example-flex-item pf-u-pl-xl">Padding-left - xl</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Responsive padding</h2>
<div
  class="pf-u-display-flex pf-u-align-items-flex-start pf-u-flex-column pf-u-flex-wrap"
>
  <div class="ws-example-flex-item pf-u-pt-xl">Padding-top</div>
  <div class="ws-example-flex-item pf-u-pt-xl-on-sm">Padding-top - xl on-sm</div>
  <div class="ws-example-flex-item pf-u-pt-xl-on-md">Padding-top - xl on-md</div>
  <div class="ws-example-flex-item pf-u-pt-xl-on-lg">Padding-top - xl on-lg</div>
  <div class="ws-example-flex-item pf-u-pt-xl-on-xl">Padding-top - xl on-xl</div>
</div>

```

### Combined

```html
<h2 class="pf-c-title pf-m-lg">Combined spacers</h2>
<div class="pf-u-display-flex pf-u-align-items-flex-start pf-u-flex-wrap">
  <div class="ws-example-flex-item pf-u-p-lg pf-u-m-lg">Padding lg, margin lg</div>
  <div
    class="ws-example-flex-item pf-u-p-lg-on-lg pf-u-m-lg"
  >Padding lg on-lg, margin lg</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Combined spacers (responsive)</h2>
<div class="pf-u-display-flex pf-u-align-items-flex-start pf-u-flex-wrap">
  <div class="ws-example-flex-item pf-u-p-sm pf-u-p-lg-on-md pf-u-mr-lg">Item 1</div>
  <div class="ws-example-flex-item pf-u-p-sm pf-u-p-lg-on-md pf-u-mr-lg">Item 2</div>
  <div
    class="ws-example-flex-item pf-u-p-sm pf-u-p-lg-on-md pf-u-ml-auto-on-xl"
  >Item 3</div>
</div>

```

## Documentation

### Overview

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), `-on-sm`, `-on-md`, `-on-lg`, and `-on-xl`. **Example .pf-u-m-sm-on-lg**

### Margin properties

| Abbreviation | Property                   |
| ------------ | -------------------------- |
| `m`          | margin                     |
| `mt`         | margin-top                 |
| `mr`         | margin-right               |
| `mb`         | margin-bottom              |
| `ml`         | margin-left                |
| `mx`         | margin-left & margin-right |
| `my`         | margin-top & margin-bottom |

### Padding properties

| Abbreviation | Property                     |
| ------------ | ---------------------------- |
| `p`          | padding                      |
| `pt`         | padding-top                  |
| `pr`         | padding-right                |
| `pb`         | padding-bottom               |
| `pl`         | padding-left                 |
| `px`         | padding-left & padding-right |
| `py`         | padding-top & padding-bottom |

### Size values

| Abbreviation | Property                      |
| ------------ | ----------------------------- |
| `auto`       | auto                          |
| `0`          | 0                             |
| `xs`         | var(--pf-global--spacer--xs)  |
| `sm`         | var(--pf-global--spacer--sm)  |
| `md`         | var(--pf-global--spacer--md)  |
| `lg`         | var(--pf-global--spacer--lg)  |
| `xl`         | var(--pf-global--spacer--xl)  |
| `2xl`        | var(--pf-global--spacer--2xl) |
| `3xl`        | var(--pf-global--spacer--3xl) |
| `4xl`        | var(--pf-global--spacer--4xl) |

### Usage

| Class                                                        | Applied to | Outcome                                                                                           |
| ------------------------------------------------------------ | ---------- | ------------------------------------------------------------------------------------------------- |
| `.pf-u-{m, mt, mr, mb, ml, mx, my}-{size}{-on-[breakpoint]}` | `*`        | Sets margin top, right, bottom, left, margin-left & right, margin-top & bottom to spacer value    |
| `.pf-u-{p, pt, pr, pb, pl, px, py}-{size}{-on-[breakpoint]}` | `*`        | Sets padding top, right, bottom, left, padding-left & right, padding-top & bottom to spacer value |
