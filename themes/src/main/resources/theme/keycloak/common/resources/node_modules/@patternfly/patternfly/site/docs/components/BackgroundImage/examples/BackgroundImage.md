---
title: Background image
section: components
cssPrefix: pf-c-background-image
---

## Examples
```hbs title=Basic isFullscreen
{{#> background-image}}
{{/background-image}}
```

## Documentation
### Overview
This component puts an image on the background with an svg filter applied to it. The svg must be inline on the page for the filter to work in all browsers.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-background-image` | `*` |  A fixed background image is applied to the background of the page. |
| `.pf-c-background-image__filter` | `*` |  The inline svg that provides the filter for the background image. |
