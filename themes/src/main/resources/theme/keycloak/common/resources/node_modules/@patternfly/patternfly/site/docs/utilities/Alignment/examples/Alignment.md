---
title: Alignment
section: utilities
---

import './Alignment.css'

## Examples
```hbs title=Basic
{{#> alignment alignment--type="left"}}
  Text left
{{/alignment}}
{{#> alignment alignment--type="center"}}
  Text center
{{/alignment}}
{{#> alignment alignment--type="right"}}
  Text right
{{/alignment}}
{{#> alignment alignment--type="justify"}}
  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
  tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
  quis nostrud exercitation ullamco laboris.
{{/alignment}}
```

## Documentation
### Overview
Breakpoint is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example .pf-u-text-left-on-lg**

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-u-text-align-left{-on-[breakpoint]}` | `*` |  Aligns text left |
| `.pf-u-text-align-center{-on-[breakpoint]}` | `*` |  Aligns text center |
| `.pf-u-text-align-right{-on-[breakpoint]}` | `*` |  Aligns text right |
| `.pf-u-text-align-justify{-on-[breakpoint]}` | `*` |  Aligns text justify |
