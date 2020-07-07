---
title: Badge
section: components
cssPrefix: pf-c-badge
---

## Examples
```hbs title=Read
{{#> badge badge--modifier="pf-m-read"}}
  7
{{/badge}}
{{#> badge badge--modifier="pf-m-read"}}
  24
{{/badge}}
{{#> badge badge--modifier="pf-m-read"}}
  240
{{/badge}}
{{#> badge badge--modifier="pf-m-read"}}
  999+
{{/badge}}
```

```hbs title=Unread
{{#> badge badge--modifier="pf-m-unread"}}
  7
{{/badge}}
{{#> badge badge--modifier="pf-m-unread"}}
  24
{{/badge}}
{{#> badge badge--modifier="pf-m-unread"}}
  240
{{/badge}}
{{#> badge badge--modifier="pf-m-unread"}}
  999+
{{/badge}}
```

## Documentation
### Overview
Always add a modifier class. Never use the class `.pf-c-badge` on its own.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-badge` | `<span>` | Initiates a badge. **Always use with a modifier class.** |
| `.pf-m-read` | `.pf-c-badge` | Applies read badge styling. |
| `.pf-m-unread` | `.pf-c-badge` | Applies unread badge styling. |
