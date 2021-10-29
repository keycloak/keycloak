---
id: Badge
section: components
cssPrefix: pf-c-badge
---## Examples

### Read

```html
<span class="pf-c-badge pf-m-read">7</span>
<span class="pf-c-badge pf-m-read">24</span>
<span class="pf-c-badge pf-m-read">240</span>
<span class="pf-c-badge pf-m-read">999+</span>

```

### Unread

```html
<span class="pf-c-badge pf-m-unread">7</span>
<span class="pf-c-badge pf-m-unread">24</span>
<span class="pf-c-badge pf-m-unread">240</span>
<span class="pf-c-badge pf-m-unread">999+</span>

```

## Documentation

### Overview

Always add a modifier class. Never use the class `.pf-c-badge` on its own.

### Usage

| Class          | Applied to    | Outcome                                                  |
| -------------- | ------------- | -------------------------------------------------------- |
| `.pf-c-badge`  | `<span>`      | Initiates a badge. **Always use with a modifier class.** |
| `.pf-m-read`   | `.pf-c-badge` | Applies read badge styling.                              |
| `.pf-m-unread` | `.pf-c-badge` | Applies unread badge styling.                            |
