---
title: 'Banner'
section: components
beta: true
cssPrefix: pf-c-banner
---

## Examples
```hbs title=Basic
{{#> banner}}
  Default banner
{{/banner}}

<br>

{{#> banner banner--modifier="pf-m-info"}}
  Info banner
{{/banner}}

<br>

{{#> banner banner--modifier="pf-m-danger"}}
  Danger banner
{{/banner}}

<br>

{{#> banner banner--modifier="pf-m-success"}}
  Success banner
{{/banner}}

<br>

{{#> banner banner--modifier="pf-m-warning"}}
  Warning banner
{{/banner}}
```

## Documentation
Add a modifier class to the default banner to change the presentation: `.pf-m-info`, `.pf-m-danger`, `.pf-m-success`, or `.pf-m-warning`.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-banner` | `<div>` |  Initiates a banner. **Required** |
| `.pf-m-info` | `.pf-c-banner` |  Modifies banner for info styles. |
| `.pf-m-danger` | `.pf-c-banner` |  Modifies banner for danger styles. |
| `.pf-m-success` | `.pf-c-banner` |  Modifies banner for success styles. |
| `.pf-m-warning` | `.pf-c-banner` |  Modifies banner for warning styles. |
| `.pf-m-sticky` | `.pf-c-banner` |  Modifies banner to be sticky to the top of its container. |
