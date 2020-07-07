---
title: Grid
section: layouts
cssPrefix: pf-l-grid
---

import './Grid.css'

## Examples
```hbs title=Smart-(responsive)
{{#> grid grid--modifier="pf-m-all-6-col-on-sm pf-m-all-4-col-on-md pf-m-all-2-col-on-lg pf-m-all-1-col-on-xl"}}
  {{#> grid-item}}
    item 1
  {{/grid-item}}
  {{#> grid-item}}
    item 2
  {{/grid-item}}
  {{#> grid-item}}
    item 3
  {{/grid-item}}
  {{#> grid-item}}
    item 4
  {{/grid-item}}
  {{#> grid-item}}
    item 5
  {{/grid-item}}
  {{#> grid-item}}
    item 6
  {{/grid-item}}
  {{#> grid-item}}
    item 7
  {{/grid-item}}
  {{#> grid-item}}
    item 8
  {{/grid-item}}
  {{#> grid-item}}
    item 9
  {{/grid-item}}
  {{#> grid-item}}
    item 10
  {{/grid-item}}  
  {{#> grid-item}}
    item 11
  {{/grid-item}}
  {{#> grid-item}}
    item 12
  {{/grid-item}}
{{/grid}}
```

```hbs title=Smart-with-overrides-(responsive)
{{#> grid grid--modifier="pf-m-all-6-col-on-sm pf-m-all-4-col-on-md pf-m-all-2-col-on-lg pf-m-all-1-col-on-xl"}}
  {{#> grid-item grid-item--modifier="pf-m-8-col-on-sm pf-m-4-col-on-lg pf-m-6-col-on-xl"}}
    item 1
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-4-col-on-sm pf-m-8-col-on-lg pf-m-6-col-on-xl"}}
    item 2
  {{/grid-item}}
  {{#> grid-item}}
    item 3
  {{/grid-item}}
  {{#> grid-item}}
    item 4
  {{/grid-item}}
  {{#> grid-item}}
    item 5
  {{/grid-item}}
  {{#> grid-item}}
    item 6
  {{/grid-item}}
  {{#> grid-item}}
    item 7
  {{/grid-item}}
  {{#> grid-item}}
    item 8
  {{/grid-item}}
  {{#> grid-item}}
    item 9
  {{/grid-item}}
  {{#> grid-item}}
    item 10
  {{/grid-item}}  
  {{#> grid-item}}
    item 11
  {{/grid-item}}
  {{#> grid-item}}
    item 12
  {{/grid-item}}
  {{#> grid-item}}
    item 13
  {{/grid-item}}  
  {{#> grid-item}}
    item 14
  {{/grid-item}}  
{{/grid}}
```

```hbs title=Base 
{{#> grid}}
  {{#> grid-item grid-item--modifier="pf-m-12-col"}}
      12 col
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-11-col"}}
      11 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-1-col"}}
      1 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-10-col"}}
      10 col 
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-2-col"}}
      2 col 
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-9-col"}}
      9 col 
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-3-col"}}
      3 col 
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-8-col"}}
      8 col
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-4-col"}}
      4 col
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-7-col"}}
      7 col
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-5-col"}}
      5 col
  {{/grid-item}}  
{{/grid}}
```

```hbs title=Gutter
{{#> grid grid--modifier="pf-m-gutter"}}
  {{#> grid-item grid-item--modifier="pf-m-12-col"}}
      12 col
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-11-col"}}
      11 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-1-col"}}
      1 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-10-col"}}
      10 col 
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-2-col"}}
      2 col 
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-9-col"}}
      9 col 
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-3-col"}}
      3 col 
  {{/grid-item}}    
{{/grid}}
```

```hbs title=Responsive
{{#> grid}}
  {{#> grid-item grid-item--modifier="pf-m-1-col pf-m-6-col-on-md pf-m-11-col-on-xl"}}
      1 / 6 / 11 col
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-11-col pf-m-6-col-on-md pf-m-1-col-on-xl"}}
      11 / 6 / 1 col
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-2-col pf-m-6-col-on-md pf-m-10-col-on-xl"}}
      2 / 6 / 10 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-10-col pf-m-6-col-on-md pf-m-2-col-on-xl"}}
      10 / 6 / 2 col 
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-3-col pf-m-6-col-on-md pf-m-9-col-on-xl"}}
      3 / 6 / 9 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-9-col pf-m-6-col-on-md pf-m-3-col-on-xl"}}
      9 / 6 / 3 col 
  {{/grid-item}}   
  {{#> grid-item grid-item--modifier="pf-m-4-col pf-m-6-col-on-md pf-m-8-col-on-xl"}}
      4 / 6 / 8 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-8-col pf-m-6-col-on-md pf-m-4-col-on-xl"}}
      8 / 6 / 4 col 
  {{/grid-item}}   
  {{#> grid-item grid-item--modifier="pf-m-5-col pf-m-6-col-on-md pf-m-7-col-on-xl"}}
      5 / 6 / 7 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-7-col pf-m-6-col-on-md pf-m-5-col-on-xl"}}
      7 / 6 / 5 col 
  {{/grid-item}} 
{{/grid}}
```

```hbs title=Nested
{{#> grid}}
  {{#> grid-item grid-item--modifier="pf-m-12-col"}}
    12 col
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-10-col"}}
    10 col 
      {{#> grid grid--modifier="pf-m-gutter"}}
        {{#> grid-item grid-item--modifier="pf-m-6-col"}}
            6 col 
        {{/grid-item}}    
        {{#> grid-item grid-item--modifier="pf-m-6-col"}}
            6 col 
        {{/grid-item}}    
        {{#> grid-item grid-item--modifier="pf-m-4-col"}}
            4 col 
        {{/grid-item}}    
        {{#> grid-item grid-item--modifier="pf-m-8-col"}}
            8 col 
        {{/grid-item}}    
      {{/grid}}      
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-2-col"}}
      2 col 
  {{/grid-item}}   
{{/grid}}
```

```hbs title=Offsets
{{#> grid grid--modifier="pf-m-gutter"}}
  {{#> grid-item grid-item--modifier="pf-m-11-col pf-m-offset-1-col"}}
      11 col, offset 1
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-10-col pf-m-offset-2-col"}}
      10 col, offset 2
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-9-col pf-m-offset-3-col"}}
      9 col, offset 3
  {{/grid-item}}    
  {{#> grid-item grid-item--modifier="pf-m-8-col pf-m-offset-4-col"}}
      8 col, offset 4
  {{/grid-item}}  
{{/grid}}
```

```hbs title=Row-spans
{{#> grid grid--modifier="pf-m-gutter"}}
  {{#> grid-item grid-item--modifier="pf-m-8-col"}}
    8 col 
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-4-col pf-m-2-row"}}
    4 col, 2 row
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-2-col pf-m-3-row"}}
    2 col, 3 row
  {{/grid-item}}
  {{#> grid-item grid-item--modifier="pf-m-2-col"}}
    2 col 
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-4-col"}}
    4 col 
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-2-col"}}
    2 col 
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-2-col"}}
    2 col 
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-2-col"}}
    2 col 
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-4-col"}}
    4 col 
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-2-col"}}
    2 col 
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-4-col"}}
    4 col 
  {{/grid-item}}  
  {{#> grid-item grid-item--modifier="pf-m-4-col"}}
    4 col 
  {{/grid-item}}  
{{/grid}}
```

## Documentation
### Overview
The grid layout is based on CSS Grid’s two-dimensional system of columns and rows. This layout styles the parent element and its children to achieve responsive column and row spans as well as gutters.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-l-grid` | `<div>` | Initializes the grid layout. |
| `.pf-l-grid__item` | `<div>` | Explicitly sets a child of the grid. This class isn't necessary, but it is included to keep inline with BEM convention, and to provide an entity that will later be used for applying modifiers. |
| `.pf-m-gutter` | `.pf-l-grid` | Adds space between children by using the globally defined gutter value. |
| `.pf-m-all-{1-12}-col{-on-[breakpoint]}` | `.pf-l-grid` | Defines grid item size on grid container. |
| `.pf-m-{1-12}-col{-on-[breakpoint]}` | `.pf-l-grid__item` | Defines grid item size.  Although not required, they are strongly suggested. If not used, grid item will default to 12 col. |
| `.pf-m-{2-x}-row{-on-[breakpoint]}` | `.pf-l-grid__item` | Defines grid item row span.  For row spans to function correctly, the value of of the current row plus the grid items to span must be equal to or less than 12. Example: .pf-m-8-col.pf-m-2-row + .pf-m-4-col + .pf-m-4-col. There is no limit to number of spanned rows. |
