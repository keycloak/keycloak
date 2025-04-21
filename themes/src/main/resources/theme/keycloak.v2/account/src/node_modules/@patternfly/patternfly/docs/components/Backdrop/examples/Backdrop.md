---
id: Backdrop
section: components
cssPrefix: pf-c-backdrop
---## Examples

### Basic

```html isFullscreen
<div class="pf-c-backdrop"></div>

```

## Documentation

### Overview

This component puts a backdrop over the entire viewport.

### Usage

| Class                  | Applied to | Outcome                                                                                                                                                    |
| ---------------------- | ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-backdrop`       | `<div>`    | Initiates backdrop. **Required**                                                                                                                           |
| `.pf-c-backdrop__open` | `<body>`   | Lock scrolling when backdrop is active. This class should be set on `<body>` while backdrop is active and removed while backdrop is inactive. **Required** |
