---
title: Stack
section: layouts
cssPrefix: pf-l-stack
---

import './Stack.css'

## Examples
```hbs title=Basic
{{#> stack}}
  {{#> stack-item}}
    content
  {{/stack-item}}
  {{#> stack-item stack-item--modifier="pf-m-fill"}}
    pf-m-fill
  {{/stack-item}}
  {{#> stack-item}}
    content
  {{/stack-item}}
{{/stack}}
```

```hbs title=With-gutter
{{#> stack stack--modifier="pf-m-gutter"}}
  {{#> stack-item}}
    content
  {{/stack-item}}
  {{#> stack-item stack-item--modifier="pf-m-fill"}}
    pf-m-fill
  {{/stack-item}}
  {{#> stack-item}}
    content
  {{/stack-item}}
{{/stack}}
```

## Documentation
### Overview
The stack layout is designed to position items vertically, with one item filling the available vertical space.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-l-stack` | `<div>`, `<section>`, or `<article>` | Initiates the stack layout. |
| `.pf-l-stack__item` | `<div>` | Initiates a stack item. **Required**  |
| `.pf-m-gutter` | `.pf-l-stack` | Adds space between children by using the globally defined gutter value. |
| `.pf-m-fill` | `.pf-l-stack__item` | Specifies which item(s) should fill the avaiable vertical space. |
