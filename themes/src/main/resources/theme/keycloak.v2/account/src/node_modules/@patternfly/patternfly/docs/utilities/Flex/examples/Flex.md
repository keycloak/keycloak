---
id: Flex
section: utilities
---import './Flex.css'

## Examples

### Basic

```html
<div class="pf-u-display-flex">Display flex</div>
<div class="pf-u-display-inline-flex">Display inline flex</div>

```

### Direction

```html
<h2 class="pf-c-title pf-m-lg">Flex row</h2>
<div class="pf-u-display-flex pf-u-flex-direction-row">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Flex row-reverse</h2>
<div class="pf-u-display-flex pf-u-flex-direction-row-reverse">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Flex column</h2>
<div class="pf-u-display-flex pf-u-flex-direction-column">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Flex column-reverse</h2>
<div class="pf-u-display-flex pf-u-flex-direction-column-reverse">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>

```

### Justified content

```html
<h2 class="pf-c-title pf-m-lg">Justify content flex-start</h2>
<div class="pf-u-display-flex pf-u-justify-content-flex-start">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Justify content flex-end</h2>
<div class="pf-u-display-flex pf-u-justify-content-flex-end">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Justify content center</h2>
<div class="pf-u-display-flex pf-u-justify-content-center">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Justify content space-around</h2>
<div class="pf-u-display-flex pf-u-justify-content-space-around">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Justify content space-between</h2>
<div class="pf-u-display-flex pf-u-justify-content-space-between">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>

```

### Aligned items

```html
<h2 class="pf-c-title pf-m-lg">Align items flex-start</h2>
<div class="pf-u-display-flex pf-u-align-items-flex-start">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item ws-example-u-flex-md">Flex item 2</div>

  <div class="ws-example-flex-item ws-example-u-flex-lg">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align items flex-end</h2>
<div class="pf-u-display-flex pf-u-align-items-flex-end">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item ws-example-u-flex-md">Flex item 2</div>

  <div class="ws-example-flex-item ws-example-u-flex-lg">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align items center</h2>
<div class="pf-u-display-flex pf-u-align-items-center">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item ws-example-u-flex-md">Flex item 2</div>

  <div class="ws-example-flex-item ws-example-u-flex-lg">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align items baseline</h2>
<div class="pf-u-display-flex pf-u-align-items-baseline">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item ws-example-u-flex-md">Flex item 2</div>

  <div class="ws-example-flex-item ws-example-u-flex-lg">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align items stretch</h2>
<div class="pf-u-display-flex pf-u-align-items-stretch">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>

```

### Aligned self

```html
<div class="pf-u-display-flex">
  <div class="ws-example-flex-item pf-u-align-self-flex-start">flex-start</div>
  <div class="ws-example-flex-item pf-u-align-self-center">center</div>
  <div class="ws-example-flex-item pf-u-align-self-flex-end">flex end</div>
  <div class="ws-example-flex-item pf-u-align-self-baseline">baseline</div>
  <div class="ws-example-flex-item pf-u-align-self-stretch">stretch</div>
</div>

```

### Aligned content

```html
<h2 class="pf-c-title pf-m-lg">Align content flex-start</h2>
<div class="pf-u-display-flex pf-u-align-content-flex-start">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>

  <div class="ws-example-flex-item">Flex item 4</div>

  <div class="ws-example-flex-item">Flex item 5</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align content flex-end</h2>
<div class="pf-u-display-flex pf-u-align-content-flex-end">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>

  <div class="ws-example-flex-item">Flex item 4</div>

  <div class="ws-example-flex-item">Flex item 5</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align content center</h2>
<div class="pf-u-display-flex pf-u-align-content-center">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>

  <div class="ws-example-flex-item">Flex item 4</div>

  <div class="ws-example-flex-item">Flex item 5</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align content space-around</h2>
<div class="pf-u-display-flex pf-u-align-content-space-around">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>

  <div class="ws-example-flex-item">Flex item 4</div>

  <div class="ws-example-flex-item">Flex item 5</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align content space-between</h2>
<div class="pf-u-display-flex pf-u-align-content-space-between">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>

  <div class="ws-example-flex-item">Flex item 4</div>

  <div class="ws-example-flex-item">Flex item 5</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Align content stretch</h2>
<div class="pf-u-display-flex pf-u-align-content-stretch">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>

  <div class="ws-example-flex-item">Flex item 4</div>

  <div class="ws-example-flex-item">Flex item 5</div>
</div>

```

### Shrink

```html
<div class="pf-u-display-flex">
  <div class="ws-example-flex-item pf-u-flex-shrink-0">Flex shrink 0</div>
  <div class="ws-example-flex-item pf-u-flex-shrink-1">Flex shrink 1</div>
</div>

```

### Grow

```html
<div class="pf-u-display-flex">
  <div class="ws-example-flex-item pf-u-flex-grow-0">Flex grow 0</div>
  <div class="ws-example-flex-item pf-u-flex-grow-1">Flex grow 1</div>
</div>

```

### Basis and none

```html
<div class="pf-u-display-flex">
  <div class="ws-example-flex-item pf-u-flex-basis-0">Flex basis 0</div>
  <div class="ws-example-flex-item pf-u-flex-basis-auto">Flex basis auto</div>
  <div class="ws-example-flex-item pf-u-flex-basis-none">Flex basis none</div>
  <div class="ws-example-flex-item pf-u-flex-1">Flex 1</div>
</div>

```

### Fill

```html
<div class="pf-u-display-flex">
  <div class="ws-example-flex-item pf-u-flex-none">Flex none</div>
  <div class="ws-example-flex-item pf-u-flex-fill">Flex fill</div>
</div>

```

### Wrap

```html
<h2 class="pf-c-title pf-m-lg">Flex wrap</h2>
<div class="pf-u-display-flex pf-u-flex-wrap">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Flex no wrap</h2>
<div class="pf-u-display-flex pf-u-flex-nowrap">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>
<br />
<h2 class="pf-c-title pf-m-lg">Flex wrap reverse</h2>
<div class="pf-u-display-flex pf-u-flex-wrap-reverse">
  <div class="ws-example-flex-item">Flex item 1</div>

  <div class="ws-example-flex-item">Flex item 2</div>

  <div class="ws-example-flex-item">Flex item 3</div>
</div>

```

## Documentation

### Overview

For these utilities to have effect, the parent element must be set to `display: flex` or `display: inline-flex`. [Breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) is optional. Breakpoints: base (no breakpoint value), `-on-sm`, `-on-md`, `-on-lg`, and `-on-xl`. **Example .pf-u-flex-row-on-lg**

<!-- ## Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `role` or `aria` | `pf-u-flex` |  accessibility notes. |
 -->

### Usage

| Class                                                   | Applied to | Outcome                             |
| ------------------------------------------------------- | ---------- | ----------------------------------- |
| `.pf-u-flex-direction-row{-on-[breakpoint]}`            | `*`        | Sets flex-direction: row            |
| `.pf-u-flex-direction-row-reverse{-on-[breakpoint]}`    | `*`        | Sets flex-direction: row-reverse    |
| `.pf-u-flex-direction-column{-on-[breakpoint]}`         | `*`        | Sets flex-direction: column         |
| `.pf-u-flex-direction-column-reverse{-on-[breakpoint]}` | `*`        | Sets flex-direction: column-reverse |
| `.pf-u-justify-content-flex-start{-on-[breakpoint]}`    | `*`        | Sets justify-content: flex-start    |
| `.pf-u-justify-content-flex-end{-on-[breakpoint]}`      | `*`        | Sets justify-content: flex-end      |
| `.pf-u-justify-content-center{-on-[breakpoint]}`        | `*`        | Sets justify-content: center        |
| `.pf-u-justify-content-space-around{-on-[breakpoint]}`  | `*`        | Sets justify-content: space-around  |
| `.pf-u-justify-content-space-between{-on-[breakpoint]}` | `*`        | Sets justify-content: space-between |
| `.pf-u-align-items-flex-start{-on-[breakpoint]}`        | `*`        | Sets align-items: flex-start        |
| `.pf-u-align-items-flex-end{-on-[breakpoint]}`          | `*`        | Sets align-items: flex-start        |
| `.pf-u-align-items-center{-on-[breakpoint]}`            | `*`        | Sets align-items: center            |
| `.pf-u-align-items-baseline{-on-[breakpoint]}`          | `*`        | Sets align-items: baseline          |
| `.pf-u-align-items-stretch{-on-[breakpoint]}`           | `*`        | Sets align-items: stretch           |
| `.pf-u-align-self-flex-start{-on-[breakpoint]}`         | `*`        | Sets align-self: flex-start         |
| `.pf-u-align-self-flex-end{-on-[breakpoint]}`           | `*`        | Sets align-self: flex-end           |
| `.pf-u-align-self-center{-on-[breakpoint]}`             | `*`        | Sets align-self: center             |
| `.pf-u-align-self-baseline{-on-[breakpoint]}`           | `*`        | Sets align-self: baseline           |
| `.pf-u-align-self-stretch{-on-[breakpoint]}`            | `*`        | Sets align-self: stretch            |
| `.pf-u-align-content-flex-start{-on-[breakpoint]}`      | `*`        | Sets align-content: flex-start      |
| `.pf-u-align-content-flex-end{-on-[breakpoint]}`        | `*`        | Sets align-content: flex-end        |
| `.pf-u-align-content-center{-on-[breakpoint]}`          | `*`        | Sets align-content: center          |
| `.pf-u-align-content-space-between{-on-[breakpoint]}`   | `*`        | Sets align-content: space-between   |
| `.pf-u-align-content-space-around{-on-[breakpoint]}`    | `*`        | Sets align-content: space-around    |
| `.pf-u-align-content-stretch{-on-[breakpoint]}`         | `*`        | Sets align-content: stretch         |
| `.pf-u-flex-shrink{1 or 0}{-on-[breakpoint]}`           | `*`        | Sets flex-shrink to 1 or 0          |
| `.pf-u-flex-grow{1 or 0}{-on-[breakpoint]}`             | `*`        | Sets flex-grow to 1 or 0            |
| `.pf-u-flex-basis{0 or auto}{-on-[breakpoint]}`         | `*`        | Sets flex-basis to 0 auto           |
| `.pf-u-flex-fill{-on-[breakpoint]}`                     | `*`        | Sets flex to 1 1 auto               |
| `.pf-u-flex-wrap{-on-[breakpoint]}`                     | `*`        | sets flex-wrap: wrap                |
| `.pf-u-flex-nowrap{-on-[breakpoint]}`                   | `*`        | sets flex-wrap: nowrap              |
| `.pf-u-flex-wrap-reverse{-on-[breakpoint]}`             | `*`        | sets flex-wrap: wrap-reverse        |
