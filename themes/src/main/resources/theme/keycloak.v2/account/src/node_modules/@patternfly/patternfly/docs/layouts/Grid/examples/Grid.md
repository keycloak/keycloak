---
id: Grid
section: layouts
cssPrefix: pf-l-grid
---import './Grid.css'

## Examples

### Smart (responsive)

```html
<div
  class="pf-l-grid pf-m-all-6-col-on-sm pf-m-all-4-col-on-md pf-m-all-2-col-on-lg pf-m-all-1-col-on-xl"
>
  <div class="pf-l-grid__item">item 1</div>
  <div class="pf-l-grid__item">item 2</div>
  <div class="pf-l-grid__item">item 3</div>
  <div class="pf-l-grid__item">item 4</div>
  <div class="pf-l-grid__item">item 5</div>
  <div class="pf-l-grid__item">item 6</div>
  <div class="pf-l-grid__item">item 7</div>
  <div class="pf-l-grid__item">item 8</div>
  <div class="pf-l-grid__item">item 9</div>
  <div class="pf-l-grid__item">item 10</div>
  <div class="pf-l-grid__item">item 11</div>
  <div class="pf-l-grid__item">item 12</div>
</div>

```

### Smart with overrides (responsive)

```html
<div
  class="pf-l-grid pf-m-all-6-col-on-sm pf-m-all-4-col-on-md pf-m-all-2-col-on-lg pf-m-all-1-col-on-xl"
>
  <div
    class="pf-l-grid__item pf-m-8-col-on-sm pf-m-4-col-on-lg pf-m-6-col-on-xl"
  >item 1</div>
  <div
    class="pf-l-grid__item pf-m-4-col-on-sm pf-m-8-col-on-lg pf-m-6-col-on-xl"
  >item 2</div>
  <div class="pf-l-grid__item">item 3</div>
  <div class="pf-l-grid__item">item 4</div>
  <div class="pf-l-grid__item">item 5</div>
  <div class="pf-l-grid__item">item 6</div>
  <div class="pf-l-grid__item">item 7</div>
  <div class="pf-l-grid__item">item 8</div>
  <div class="pf-l-grid__item">item 9</div>
  <div class="pf-l-grid__item">item 10</div>
  <div class="pf-l-grid__item">item 11</div>
  <div class="pf-l-grid__item">item 12</div>
  <div class="pf-l-grid__item">item 13</div>
  <div class="pf-l-grid__item">item 14</div>
</div>

```

### Base

```html
<div class="pf-l-grid">
  <div class="pf-l-grid__item pf-m-12-col">12 col</div>
  <div class="pf-l-grid__item pf-m-11-col">11 col</div>
  <div class="pf-l-grid__item pf-m-1-col">1 col</div>
  <div class="pf-l-grid__item pf-m-10-col">10 col</div>
  <div class="pf-l-grid__item pf-m-2-col">2 col</div>
  <div class="pf-l-grid__item pf-m-9-col">9 col</div>
  <div class="pf-l-grid__item pf-m-3-col">3 col</div>
  <div class="pf-l-grid__item pf-m-8-col">8 col</div>
  <div class="pf-l-grid__item pf-m-4-col">4 col</div>
  <div class="pf-l-grid__item pf-m-7-col">7 col</div>
  <div class="pf-l-grid__item pf-m-5-col">5 col</div>
</div>

```

### Gutter

```html
<div class="pf-l-grid pf-m-gutter">
  <div class="pf-l-grid__item pf-m-12-col">12 col</div>
  <div class="pf-l-grid__item pf-m-11-col">11 col</div>
  <div class="pf-l-grid__item pf-m-1-col">1 col</div>
  <div class="pf-l-grid__item pf-m-10-col">10 col</div>
  <div class="pf-l-grid__item pf-m-2-col">2 col</div>
  <div class="pf-l-grid__item pf-m-9-col">9 col</div>
  <div class="pf-l-grid__item pf-m-3-col">3 col</div>
</div>

```

### Responsive

```html
<div class="pf-l-grid">
  <div
    class="pf-l-grid__item pf-m-1-col pf-m-6-col-on-md pf-m-11-col-on-xl"
  >1 / 6 / 11 col</div>
  <div
    class="pf-l-grid__item pf-m-11-col pf-m-6-col-on-md pf-m-1-col-on-xl"
  >11 / 6 / 1 col</div>
  <div
    class="pf-l-grid__item pf-m-2-col pf-m-6-col-on-md pf-m-10-col-on-xl"
  >2 / 6 / 10 col</div>
  <div
    class="pf-l-grid__item pf-m-10-col pf-m-6-col-on-md pf-m-2-col-on-xl"
  >10 / 6 / 2 col</div>
  <div
    class="pf-l-grid__item pf-m-3-col pf-m-6-col-on-md pf-m-9-col-on-xl"
  >3 / 6 / 9 col</div>
  <div
    class="pf-l-grid__item pf-m-9-col pf-m-6-col-on-md pf-m-3-col-on-xl"
  >9 / 6 / 3 col</div>
  <div
    class="pf-l-grid__item pf-m-4-col pf-m-6-col-on-md pf-m-8-col-on-xl"
  >4 / 6 / 8 col</div>
  <div
    class="pf-l-grid__item pf-m-8-col pf-m-6-col-on-md pf-m-4-col-on-xl"
  >8 / 6 / 4 col</div>
  <div
    class="pf-l-grid__item pf-m-5-col pf-m-6-col-on-md pf-m-7-col-on-xl"
  >5 / 6 / 7 col</div>
  <div
    class="pf-l-grid__item pf-m-7-col pf-m-6-col-on-md pf-m-5-col-on-xl"
  >7 / 6 / 5 col</div>
</div>

```

### Nested

```html
<div class="pf-l-grid">
  <div class="pf-l-grid__item pf-m-12-col">12 col</div>
  <div class="pf-l-grid__item pf-m-10-col">
    10 col
    <div class="pf-l-grid pf-m-gutter">
      <div class="pf-l-grid__item pf-m-6-col">6 col</div>
      <div class="pf-l-grid__item pf-m-6-col">6 col</div>
      <div class="pf-l-grid__item pf-m-4-col">4 col</div>
      <div class="pf-l-grid__item pf-m-8-col">8 col</div>
    </div>
  </div>
  <div class="pf-l-grid__item pf-m-2-col">2 col</div>
</div>

```

### Offsets

```html
<div class="pf-l-grid pf-m-gutter">
  <div class="pf-l-grid__item pf-m-11-col pf-m-offset-1-col">11 col, offset 1</div>
  <div class="pf-l-grid__item pf-m-10-col pf-m-offset-2-col">10 col, offset 2</div>
  <div class="pf-l-grid__item pf-m-9-col pf-m-offset-3-col">9 col, offset 3</div>
  <div class="pf-l-grid__item pf-m-8-col pf-m-offset-4-col">8 col, offset 4</div>
</div>

```

### Row spans

```html
<div class="pf-l-grid pf-m-gutter">
  <div class="pf-l-grid__item pf-m-8-col">8 col</div>
  <div class="pf-l-grid__item pf-m-4-col pf-m-2-row">4 col, 2 row</div>
  <div class="pf-l-grid__item pf-m-2-col pf-m-3-row">2 col, 3 row</div>
  <div class="pf-l-grid__item pf-m-2-col">2 col</div>
  <div class="pf-l-grid__item pf-m-4-col">4 col</div>
  <div class="pf-l-grid__item pf-m-2-col">2 col</div>
  <div class="pf-l-grid__item pf-m-2-col">2 col</div>
  <div class="pf-l-grid__item pf-m-2-col">2 col</div>
  <div class="pf-l-grid__item pf-m-4-col">4 col</div>
  <div class="pf-l-grid__item pf-m-2-col">2 col</div>
  <div class="pf-l-grid__item pf-m-4-col">4 col</div>
  <div class="pf-l-grid__item pf-m-4-col">4 col</div>
</div>

```

### Ordering

Ordering - Ordering can be applied to nested <code>.pf-l-grid</code> and <code>.pf-l-grid\_\_item</code>s. Spacing may need to be managed based on how items are ordered. Because order could apply to an innumerable number of elements, order is set inline as `--pf-l-grid--item--Order{-on-[breakpoint]}: {order}`.

### Ordering

```html
<div class="pf-l-grid pf-m-all-4-col pf-m-gutter">
  <div class="pf-l-grid__item" style="--pf-l-grid--item--Order: 2;">Item A</div>
  <div class="pf-l-grid__item">Item B</div>
  <div class="pf-l-grid__item" style="--pf-l-grid--item--Order: -1;">Item C</div>
</div>

```

### Responsive ordering

```html
<div class="pf-l-grid pf-m-all-4-col pf-m-gutter">
  <div class="pf-l-grid__item" style="--pf-l-grid--item--Order-on-lg: 2;">Item A</div>
  <div class="pf-l-grid__item">Item B</div>
  <div
    class="pf-l-grid__item"
    style="--pf-l-grid--item--Order: -1; --pf-l-grid--item--Order-on-md: 1;"
  >Item C</div>
</div>

```

### Grouped ordering

```html
<div class="pf-l-grid pf-m-all-6-col-on-md pf-m-gutter">
  <div class="pf-l-grid pf-m-gutter" style="--pf-l-grid--item--Order: 2;">
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 3;"
    >Set 1, Item A</div>
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 1;"
    >Set 1, Item B</div>
    <div class="pf-l-grid__item">Set 1, Item C</div>
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 2;"
    >Set 1, Item D</div>
  </div>
  <div class="pf-l-grid pf-m-gutter">
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 3;"
    >Set 2, Item A</div>
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 1;"
    >Set 2, Item B</div>
    <div class="pf-l-grid__item">Set 2, Item C</div>
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 2;"
    >Set 2, Item D</div>
  </div>
</div>

```

### Grouped, responsive ordering

```html
<div class="pf-l-grid pf-m-all-6-col-on-md pf-m-gutter">
  <div class="pf-l-grid pf-m-gutter" style="--pf-l-grid--item--Order-on-lg: 2;">
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order-on-md: 3;"
    >Set 1, Item A</div>
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order-on-md: 1;"
    >Set 1, Item B</div>
    <div class="pf-l-grid__item">Set 1, Item C</div>
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order-on-xl: 2;"
    >Set 1, Item D</div>
  </div>
  <div class="pf-l-grid pf-m-gutter">
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 3;"
    >Set 2, Item A</div>
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 1;"
    >Set 2, Item B</div>
    <div class="pf-l-grid__item">Set 2, Item C</div>
    <div
      class="pf-l-grid__item"
      style="--pf-l-grid--item--Order: 2;"
    >Set 2, Item D</div>
  </div>
</div>

```

### List type

```html
<ul class="pf-l-grid pf-m-all-6-col-on-sm">
  <li class="pf-l-grid__item">item 1</li>
  <li class="pf-l-grid__item">item 2</li>
  <li class="pf-l-grid__item">item 3</li>
  <li class="pf-l-grid__item">item 4</li>
</ul>

```

## Documentation

### Overview

The grid layout is based on CSS Grid’s two-dimensional system of columns and rows. This layout styles the parent element and its children to achieve responsive column and row spans as well as gutters.

### Usage

| Class                                                 | Applied to                                    | Outcome                                                                                                                                                                                                                                                                                                                                                                               |
| ----------------------------------------------------- | --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-l-grid`                                          | `<div>`                                       | Initializes the grid layout.                                                                                                                                                                                                                                                                                                                                                          |
| `.pf-l-grid__item`                                    | `<div>`                                       | Explicitly sets a child of the grid. This class isn't necessary, but it is included to keep inline with BEM convention, and to provide an entity that will later be used for applying modifiers.                                                                                                                                                                                      |
| `.pf-m-gutter`                                        | `.pf-l-grid`                                  | Adds space between children by using the globally defined gutter value.                                                                                                                                                                                                                                                                                                               |
| `.pf-m-all-{1-12}-col{-on-[breakpoint]}`              | `.pf-l-grid`                                  | Defines grid item size on grid container at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                                                                                                                                                                                                 |
| `.pf-m-{1-12}-col{-on-[breakpoint]}`                  | `.pf-l-grid__item`                            | Defines grid item size at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).  Although not required, they are strongly suggested. If not used, grid item will default to 12 col.                                                                                                                                               |
| `.pf-m-{2-x}-row{-on-[breakpoint]}`                   | `.pf-l-grid__item`                            | Defines grid item row span at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).  For row spans to function correctly, the value of of the current row plus the grid items to span must be equal to or less than 12. Example: .pf-m-8-col.pf-m-2-row + .pf-m-4-col + .pf-m-4-col. There is no limit to number of spanned rows. |
| `--pf-l-grid--item--Order{-on-[breakpoint]}: {order}` | `.pf-l-grid > .pf-l-grid`, `.pf-l-grid__item` | Modifies the order of the grid layout element at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                                                                                                                                                                                            |
