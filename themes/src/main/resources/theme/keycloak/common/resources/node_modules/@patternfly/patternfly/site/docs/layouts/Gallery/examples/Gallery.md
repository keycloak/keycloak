---
title: Gallery
section: layouts
cssPrefix: pf-l-gallery
---

import './Gallery.css'

## Examples
```hbs title=Basic
{{#> gallery}}
  {{#> gallery-item}}content{{/gallery-item}}
  {{#> gallery-item}}content{{/gallery-item}}
  {{#> gallery-item}}content{{/gallery-item}}
  {{#> gallery-item}}content{{/gallery-item}}
  {{#> gallery-item}}content{{/gallery-item}}
{{/gallery}}
```

```hbs title=With-gutter
{{#> gallery gallery--modifier="pf-m-gutter"}}
  {{#> gallery-item}}content{{/gallery-item}}
  {{#> gallery-item}}content{{/gallery-item}}
  {{#> gallery-item}}content{{/gallery-item}}
  {{#> gallery-item}}content{{/gallery-item}}
  {{#> gallery-item}}content{{/gallery-item}}
{{/gallery}}
```

## Documentation
### Overview
The gallery layout is designed so that all of its children are of uniform size, display horizontally, and wrap responsively.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-l-gallery` |  `<div>` |  Initializes a Gallery layout |
| `.pf-l-gallery__item` | `<div>` |  Explicitly sets the child for the gallery. This class isn't necessary, but it is included to keep inline with BEM convention, and to provide an entity that will later be used for applying modifiers. |
| `.pf-m-gutter` | `.pf-l-gallery` | Adds space between children by using the globally defined gutter value. |
