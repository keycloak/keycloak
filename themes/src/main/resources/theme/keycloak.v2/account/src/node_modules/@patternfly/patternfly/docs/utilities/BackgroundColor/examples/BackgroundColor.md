---
id: Background color
section: utilities
---## Examples

### Standard background colors

```html
<div class="pf-u-background-color-100">Background color 100</div>
<br />
<div class="pf-u-background-color-200">Background color 200</div>
<br />
<div class="pf-u-background-color-active-color-100">
  <div class="pf-u-color-light-200">Active color 100</div>
</div>
<br />
<div class="pf-u-background-color-active-color-300">
  <div class="pf-u-color-light-200">Active color 300</div>
</div>
<br />
<div class="pf-u-background-color-primary-color-200">
  <div class="pf-u-color-light-200">Primary color 200</div>
</div>

```

### Inverse background colors

```html
<div class="pf-u-background-color-dark-100">
  <div class="pf-u-color-light-200">Background color dark 100</div>
</div>
<br />
<div class="pf-u-background-color-dark-200">
  <div class="pf-u-color-light-200">Background color dark 200</div>
</div>
<br />
<div class="pf-u-background-color-dark-300">
  <div class="pf-u-color-light-200">Background color dark 300</div>
</div>
<br />
<div class="pf-u-background-color-dark-400">
  <div class="pf-u-color-light-200">Background color dark 400</div>
</div>

```

### Disabled background colors

```html
<div class="pf-u-background-color-disabled-color-200">Disabled color 200</div>
<br />
<div class="pf-u-background-color-disabled-color-300">Disabled color 300</div>

```

### Status and state background colors

```html
<div class="pf-u-background-color-default">Default</div>
<br />
<div class="pf-u-background-color-success">Success</div>
<br />
<div class="pf-u-background-color-info">Info</div>
<br />
<div class="pf-u-background-color-warning">Warning</div>
<br />
<div class="pf-u-background-color-danger">Danger</div>

```

## Documentation

### Overview

These background color utilities can be used to modify the background color of an element. In most cases, using the components with available modifiers should be sufficient to implement most designs, and should be preferred over customizations using these utilities.

Care should be taken especially when applying background colors, as this can have a negative effect on the readability and accessibility of text. Refer to [contrast ratios](/guidelines/colors/#contrast-ratios) for more information.

Note that "inverse" background colors are labeled as such to indicate that they are best used with the ["inverse" text colors](/utilities/text#inverse-colors). 

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), `-on-sm`, `-on-md`, `-on-lg`, and `-on-xl`. **Example .pf-u-background-color-200-on-lg**

### Usage

| Class                                                                | Applied to | Outcome                                              |
| -------------------------------------------------------------------- | ---------- | ---------------------------------------------------- |
| `.pf-u-background-color-{100, 200}{-on-[breakpoint]}`                | `*`        | Applies background color 100 or 200.                 |
| `.pf-u-background-color-active-color-{100, 300}{-on-[breakpoint]}`   | `*`        | Applies background color active 100 or 300.          |
| `.pf-u-background-color-primary-color-200{-on-[breakpoint]}`         | `*`        | Applies background color primary 200.                |
| `.pf-u-background-color-dark-{100, 200, 300, 400}{-on-[breakpoint]}` | `*`        | Applies background color dark 100, 200, 300, or 400. |
| `.pf-u-background-color-disabled-color-{200, 300}{-on-[breakpoint]}` | `*`        | Applies background color disabled 200 or 300.        |
| `.pf-u-background-color-default{-on-[breakpoint]}`                   | `*`        | Applies the background color for the default state.  |
| `.pf-u-background-color-success{-on-[breakpoint]}`                   | `*`        | Applies the background color for the success state.  |
| `.pf-u-background-color-info{-on-[breakpoint]}`                      | `*`        | Applies the background color for the info state.     |
| `.pf-u-background-color-warning{-on-[breakpoint]}`                   | `*`        | Applies the background color for the warning state.  |
| `.pf-u-background-color-danger{-on-[breakpoint]}`                    | `*`        | Applies the background color for the danger state.   |
