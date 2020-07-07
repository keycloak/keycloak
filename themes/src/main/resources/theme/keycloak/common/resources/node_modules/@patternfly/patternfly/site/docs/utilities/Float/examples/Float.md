---
title: Float
section: utilities
---

import './Float.css'

## Examples
```hbs title=Basic
{{#> float float--type="left"}}
  Float left
{{/float}}
{{#> float float--type="right"}}
  Float right
{{/float}}
<p>Lorem, ipsum dolor sit amet consectetur adipisicing elit. Earum, odit fugit eaque ad assumenda fuga alias aut ipsum repudiandae enim pariatur ullam distinctio omnis dolorem at voluptatum saepe, beatae officiis?
</p>
```

## Documentation
### Overview
Breakpoint is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example .pf-u-text-left-on-lg**

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-u-float-left{-on-[breakpoint]}` | `*` |  Float element left |
| `.pf-u-float-right{-on-[breakpoint]}` | `*` |  Float element right |
