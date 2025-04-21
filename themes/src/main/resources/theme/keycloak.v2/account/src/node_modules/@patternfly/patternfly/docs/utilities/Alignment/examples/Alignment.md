---
id: Alignment
section: utilities
---import './Alignment.css'

## Examples

### Basic

```html
<div class="pf-u-text-align-left">Text left</div>
<div class="pf-u-text-align-center">Text center</div>
<div class="pf-u-text-align-right">Text right</div>
<div class="pf-u-text-align-justify">
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
  tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
  quis nostrud exercitation ullamco laboris.
</div>

```

## Documentation

### Overview

[Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), `-on-sm`, `-on-md`, `-on-lg`, and `-on-xl`. **Example .pf-u-text-left-on-lg**

### Usage

| Class                                        | Applied to | Outcome             |
| -------------------------------------------- | ---------- | ------------------- |
| `.pf-u-text-align-left{-on-[breakpoint]}`    | `*`        | Aligns text left    |
| `.pf-u-text-align-center{-on-[breakpoint]}`  | `*`        | Aligns text center  |
| `.pf-u-text-align-right{-on-[breakpoint]}`   | `*`        | Aligns text right   |
| `.pf-u-text-align-justify{-on-[breakpoint]}` | `*`        | Aligns text justify |
