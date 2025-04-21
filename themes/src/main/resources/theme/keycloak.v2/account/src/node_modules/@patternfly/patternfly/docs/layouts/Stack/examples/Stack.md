---
id: Stack
section: layouts
cssPrefix: pf-l-stack
---import './Stack.css'

## Examples

### Basic

```html
<div class="pf-l-stack">
  <div class="pf-l-stack__item">content</div>
  <div class="pf-l-stack__item pf-m-fill">pf-m-fill</div>
  <div class="pf-l-stack__item">content</div>
</div>

```

### With gutter

```html
<div class="pf-l-stack pf-m-gutter">
  <div class="pf-l-stack__item">content</div>
  <div class="pf-l-stack__item pf-m-fill">pf-m-fill</div>
  <div class="pf-l-stack__item">content</div>
</div>

```

## Documentation

### Overview

The stack layout is designed to position items vertically, with one item filling the available vertical space.

### Usage

| Class               | Applied to                           | Outcome                                                                 |
| ------------------- | ------------------------------------ | ----------------------------------------------------------------------- |
| `.pf-l-stack`       | `<div>`, `<section>`, or `<article>` | Initiates the stack layout.                                             |
| `.pf-l-stack__item` | `<div>`                              | Initiates a stack item. **Required**                                    |
| `.pf-m-gutter`      | `.pf-l-stack`                        | Adds space between children by using the globally defined gutter value. |
| `.pf-m-fill`        | `.pf-l-stack__item`                  | Specifies which item(s) should fill the avaiable vertical space.        |
