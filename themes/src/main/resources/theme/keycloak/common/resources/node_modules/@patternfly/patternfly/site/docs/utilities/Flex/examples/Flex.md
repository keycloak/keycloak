---
title: Flex
section: utilities
---

import './Flex.css'

## Examples
```hbs title=Basic
{{#> display display--type="flex"}}
  Display flex
{{/display}}
{{#> display display--type="inline-flex"}}
  Display inline flex
{{/display}}
```

```hbs title=Direction
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Flex row
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-flex-direction-row"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Flex row-reverse
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-flex-direction-row-reverse"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Flex column
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-flex-direction-column"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Flex column-reverse
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-flex-direction-column-reverse"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
```

```hbs title=Justified-content
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Justify content flex-start
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-justify-content-flex-start"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Justify content flex-end
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-justify-content-flex-end"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Justify content center
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-justify-content-center"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Justify content space-around
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-justify-content-space-around"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Justify content space-between
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-justify-content-space-between"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
```

```hbs title=Aligned-items
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align items flex-start
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-start"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item flex-item--modifier="ws-example-u-flex-md"}}Flex item 2{{/flex-item}}
  {{#> flex-item flex-item--modifier="ws-example-u-flex-lg"}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align items flex-end
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-flex-end"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item flex-item--modifier="ws-example-u-flex-md"}}Flex item 2{{/flex-item}}
  {{#> flex-item flex-item--modifier="ws-example-u-flex-lg"}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align items center
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-center"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item flex-item--modifier="ws-example-u-flex-md"}}Flex item 2{{/flex-item}}
  {{#> flex-item flex-item--modifier="ws-example-u-flex-lg"}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align items baseline
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-baseline"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item flex-item--modifier="ws-example-u-flex-md"}}Flex item 2{{/flex-item}}
  {{#> flex-item flex-item--modifier="ws-example-u-flex-lg"}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align items stretch
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-items-stretch"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
```

```hbs title=Aligned-self
{{#> display display--type="flex"}}
  {{#> flex-item flex-item--modifier="pf-u-align-self-flex-start"}}
    flex-start
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-align-self-center"}}
    center
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-align-self-flex-end"}}
    flex end
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-align-self-baseline"}}
    baseline
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-align-self-stretch"}}
    stretch
  {{/flex-item}}
{{/display}}
```

```hbs title=Aligned-content
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align content flex-start
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-content-flex-start"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
  {{#> flex-item}}Flex item 4{{/flex-item}}
  {{#> flex-item}}Flex item 5{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align content flex-end
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-content-flex-end"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
  {{#> flex-item}}Flex item 4{{/flex-item}}
  {{#> flex-item}}Flex item 5{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align content center
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-content-center"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
  {{#> flex-item}}Flex item 4{{/flex-item}}
  {{#> flex-item}}Flex item 5{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align content space-around
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-content-space-around"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
  {{#> flex-item}}Flex item 4{{/flex-item}}
  {{#> flex-item}}Flex item 5{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align content space-between
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-content-space-between"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
  {{#> flex-item}}Flex item 4{{/flex-item}}
  {{#> flex-item}}Flex item 5{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Align content stretch
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-align-content-stretch"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
  {{#> flex-item}}Flex item 4{{/flex-item}}
  {{#> flex-item}}Flex item 5{{/flex-item}}
{{/display}}
```

```hbs title=Shrink
{{#> display display--type="flex"}}
  {{#> flex-item flex-item--modifier="pf-u-flex-shrink-0"}}
    Flex shrink 0
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-flex-shrink-1"}}
    Flex shrink 1
  {{/flex-item}}
{{/display}}
```

```hbs title=Grow
{{#> display display--type="flex"}}
  {{#> flex-item flex-item--modifier="pf-u-flex-grow-0"}}
    Flex grow 0
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-flex-grow-1"}}
    Flex grow 1
  {{/flex-item}}
{{/display}}
```

```hbs title=Basis-and-none
{{#> display display--type="flex"}}
  {{#> flex-item flex-item--modifier="pf-u-flex-basis-0"}}
    Flex basis 0
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-flex-basis-auto"}}
    Flex basis auto
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-flex-basis-none"}}
    Flex basis none
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-flex-1"}}
    Flex 1
  {{/flex-item}}
{{/display}}
```

```hbs title=Fill
{{#> display display--type="flex"}}
  {{#> flex-item flex-item--modifier="pf-u-flex-none"}}
    Flex none
  {{/flex-item}}
  {{#> flex-item flex-item--modifier="pf-u-flex-fill"}}
    Flex fill
  {{/flex-item}}
{{/display}}
```

```hbs title=Wrap
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Flex wrap
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-flex-wrap"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Flex no wrap
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-flex-nowrap"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
<br>
{{#> title titleType="h2" title--modifier="pf-m-lg"}}
  Flex wrap reverse
{{/title}}
{{#> display display--type="flex" display--modifier="pf-u-flex-wrap-reverse"}}
  {{#> flex-item}}Flex item 1{{/flex-item}}
  {{#> flex-item}}Flex item 2{{/flex-item}}
  {{#> flex-item}}Flex item 3{{/flex-item}}
{{/display}}
```

## Documentation
### Overview
For these utilities to have effect, the parent element must be set to `display: flex` or `display: inline-flex`. Breakpoint is optional. Breakpoints: base (no breakpoint value), -on-sm, -on-md, -on-lg, -on-xl. **Example .pf-u-flex-row-on-lg**

<!-- ## Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `role` or `aria` | `pf-u-flex` |  accessibility notes. |
 -->

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-u-flex-direction-row{-on-[breakpoint]}`            | `*` |  Sets flex-direction: row |
| `.pf-u-flex-direction-row-reverse{-on-[breakpoint]}`    | `*` |  Sets flex-direction: row-reverse |
| `.pf-u-flex-direction-column{-on-[breakpoint]}`         | `*` |  Sets flex-direction: column |
| `.pf-u-flex-direction-column-reverse{-on-[breakpoint]}` | `*` |  Sets flex-direction: column-reverse |
| `.pf-u-justify-content-flex-start{-on-[breakpoint]}`    | `*` |  Sets justify-content: flex-start |
| `.pf-u-justify-content-flex-end{-on-[breakpoint]}`      | `*` |  Sets justify-content: flex-end |
| `.pf-u-justify-content-center{-on-[breakpoint]}`        | `*` |  Sets justify-content: center |
| `.pf-u-justify-content-space-around{-on-[breakpoint]}`  | `*` |  Sets justify-content: space-around |
| `.pf-u-justify-content-space-between{-on-[breakpoint]}` | `*` |  Sets justify-content: space-between |
| `.pf-u-align-items-flex-start{-on-[breakpoint]}`        | `*` |  Sets align-items: flex-start |
| `.pf-u-align-items-flex-end{-on-[breakpoint]}`          | `*` |  Sets align-items: flex-start |
| `.pf-u-align-items-center{-on-[breakpoint]}`            | `*` |  Sets align-items: center |
| `.pf-u-align-items-baseline{-on-[breakpoint]}`          | `*` |  Sets align-items: baseline |
| `.pf-u-align-items-stretch{-on-[breakpoint]}`           | `*` |  Sets align-items: stretch |
| `.pf-u-align-self-flex-start{-on-[breakpoint]}`         | `*` |  Sets align-self: flex-start |
| `.pf-u-align-self-flex-end{-on-[breakpoint]}`           | `*` |  Sets align-self: flex-end |
| `.pf-u-align-self-center{-on-[breakpoint]}`             | `*` |  Sets align-self: center |
| `.pf-u-align-self-baseline{-on-[breakpoint]}`           | `*` |  Sets align-self: baseline |
| `.pf-u-align-self-stretch{-on-[breakpoint]}`            | `*` |  Sets align-self: stretch |
| `.pf-u-align-content-flex-start{-on-[breakpoint]}`      | `*` |  Sets align-content: flex-start |
| `.pf-u-align-content-flex-end{-on-[breakpoint]}`        | `*` |  Sets align-content: flex-end |
| `.pf-u-align-content-center{-on-[breakpoint]}`          | `*` |  Sets align-content: center |
| `.pf-u-align-content-space-between{-on-[breakpoint]}`   | `*` |  Sets align-content: space-between |
| `.pf-u-align-content-space-around{-on-[breakpoint]}`    | `*` |  Sets align-content: space-around |
| `.pf-u-align-content-stretch{-on-[breakpoint]}`         | `*` |  Sets align-content: stretch |
| `.pf-u-flex-shrink{1 or 0}{-on-[breakpoint]}`           | `*` |  Sets flex-shrink to 1 or 0 |
| `.pf-u-flex-grow{1 or 0}{-on-[breakpoint]}`             | `*` |  Sets flex-grow to 1 or 0 |
| `.pf-u-flex-basis{0 or auto}{-on-[breakpoint]}`         | `*` |  Sets flex-basis to 0 auto |
| `.pf-u-flex-fill{-on-[breakpoint]}`                     | `*` |  Sets flex to 1 1 auto |
| `.pf-u-flex-wrap{-on-[breakpoint]}`                     | `*` |  sets flex-wrap: wrap |
| `.pf-u-flex-nowrap{-on-[breakpoint]}`                   | `*` |  sets flex-wrap: nowrap |
| `.pf-u-flex-wrap-reverse{-on-[breakpoint]}`             | `*` |  sets flex-wrap: wrap-reverse |
