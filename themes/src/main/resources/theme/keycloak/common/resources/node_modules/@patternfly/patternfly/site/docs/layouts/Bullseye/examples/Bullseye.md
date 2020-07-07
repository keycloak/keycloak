---
title: Bullseye
section: layouts
cssPrefix: pf-l-bullseye
---

import './Bullseye.css'

## Examples
```hbs title=Basic 
{{#> bullseye}}
  {{#> bullseye-item}}content{{/bullseye-item}}
{{/bullseye}}
```

## Documentation
### Overview
The bullseye layout is designed to center a single child element horizontally and vertically within its parent.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-l-bullseye` | `<div>` | Initializes the bullseye layout. A bullseye can only have one child. |
| `.pf-l-bullseye__item` |  `<div>` | Explicitly sets the child for the bullseye. This class isn't necessary, but it is included to keep inline with BEM convention, and to provide an entity that will later be used for applying modifiers. |
