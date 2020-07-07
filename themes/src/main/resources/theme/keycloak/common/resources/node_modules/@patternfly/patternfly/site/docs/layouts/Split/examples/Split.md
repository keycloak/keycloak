---
title: Split
section: layouts
cssPrefix: pf-l-split
---

import './Split.css'

## Examples
```hbs title=Basic
{{#> split}}
  {{#> split-item}}
    content
  {{/split-item}}
  {{#> split-item split-item--modifier="pf-m-fill"}}
    pf-m-fill
  {{/split-item}}
  {{#> split-item}}
    content
  {{/split-item}}
{{/split}}
```

```hbs title=With-gutter
{{#> split split--modifier="pf-m-gutter"}}
  {{#> split-item}}
    content
  {{/split-item}}
  {{#> split-item split-item--modifier="pf-m-fill"}}
    pf-m-fill
  {{/split-item}}
  {{#> split-item}}
    content
  {{/split-item}}
{{/split}}
```

## Documentation
### Overview
The split layout is designed to position items horizontally, with one item filling the available horizontal space.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-l-split` | `<div>`, `<section>`, or `<article>` | Initiates the split layout. |
| `.pf-l-split__item` | `<div>` | Initiates a split item. **Required** |
| `.pf-m-gutter` | `.pf-l-split` | Adds space between children by using the globally defined gutter value. |
| `.pf-m-fill` | `.pf-l-split__item` | Specifies which item(s) should fill the avaiable horizontal space. |
