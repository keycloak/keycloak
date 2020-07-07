---
title: List
section: components
cssPrefix: pf-c-list
---

## Examples
```hbs title=Unordered
{{#> list}}
  <li>In fermentum leo eu lectus mollis, quis dictum mi aliquet.</li>
  <li>Morbi eu nulla lobortis, lobortis est in, fringilla felis.</li>
  <li>Aliquam nec felis in sapien venenatis viverra fermentum nec lectus.
    {{#> list}}
    <li>In fermentum leo eu lectus mollis, quis dictum mi aliquet.</li>
    <li>Morbi eu nulla lobortis, lobortis est in, fringilla felis.</li>
    <li>Ut venenatis, nisl scelerisque.
      {{#> list list--type="ol"}}
      <li>Donec blandit a lorem id convallis.</li>
      <li>Cras gravida arcu at diam gravida gravida.</li>
      <li>Integer in volutpat libero.</li>
      {{/list}}
    </li>
    {{/list}}
  </li>
  <li>Ut non enim metus.</li>
{{/list}}
```

```hbs title=Ordered
{{#> list list--type="ol"}}
  <li>Donec blandit a lorem id convallis.</li>
  <li>Cras gravida arcu at diam gravida gravida.</li>
  <li>Integer in volutpat libero.</li>
  <li>Donec a diam tellus.</li>
  <li>Etiam auctor nisl et.
    {{#> list newcontext}}
    <li>Donec blandit a lorem id convallis.</li>
    <li>Cras gravida arcu at diam gravida gravida.</li>
    <li>Integer in volutpat libero.
      {{#> list list--type="ol"}}
      <li>Donec blandit a lorem id convallis.</li>
      <li>Cras gravida arcu at diam gravida gravida.</li>
      {{/list}}
    </li>
    {{/list}}
  <li>Aenean nec tortor orci.</li>
  <li>Quisque aliquam cursus urna, non bibendum massa viverra eget.</li>
  <li>Vivamus maximus ultricies pulvinar.</li>
{{/list}}
```

```hbs title=Inline
{{#> list list--modifier="pf-m-inline"}}
  <li>Inline list item 1</li>
  <li>Inline list item 2</li>
  <li>Inline list item 3</li>
{{/list}}
```

## Documentation
### Overview
Non-inline lists can be nested up to any level.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-list` | `<ul>, <ol>` | Initiates a list. **Required**  |
| `.pf-m-inline` | `.pf-c-list` |  Modifies for inline list style. |