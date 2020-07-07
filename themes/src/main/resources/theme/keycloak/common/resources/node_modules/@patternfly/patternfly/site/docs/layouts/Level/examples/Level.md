---
title: Level
section: layouts
cssPrefix: pf-l-level
---

import './Level.css'

## Examples
```hbs title=Two-items
{{#> level}}
  {{#> level-item}}
    content
  {{/level-item}}
  {{#> level-item}}
    content
  {{/level-item}}
{{/level}}
```

```hbs title=Three-items
{{#> level}}
  {{#> level-item}}
    content
  {{/level-item}}
  {{#> level-item}}
    content
  {{/level-item}}
  {{#> level-item}}
    content
  {{/level-item}}
{{/level}}
```

```hbs title=With-gutters
{{#> level level--modifier="pf-m-gutter"}}
  {{#> level-item}}
    content with gutter
  {{/level-item}}
  {{#> level-item}}
    content with gutter
  {{/level-item}}
  {{#> level-item}}
    content with gutter
  {{/level-item}}
{{/level}}
```

## Documentation
### Overview
The level layout is designed to distribute space between children evenly and center them on the x-axis. By default the children are placed horizontally and wrap responsively.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-l-level` | `<div>`, `<section>`, or `<article>` | Initializes the level layout |
| `.pf-l-level__item` | `<div>` | Explicitly sets a child of the level. This class isn't necessary, but it is included to keep inline with BEM convention, and to provide an entity that will later be used for applying modifiers. |
| `.pf-m-gutter` | `.pf-l-level` | Adds space between children by using the globally defined gutter value. |
