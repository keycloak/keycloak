---
title: Simple list
section: components
beta: true
cssPrefix: pf-c-simple-list
---

## Examples
```hbs title=Simple-list
{{#> simple-list}}
  {{#> simple-list-list}}
    {{#> simple-list-item}}
      {{#> simple-list-item-link simple-list-item-link--modifier="pf-m-current"}}
        List item 1
      {{/simple-list-item-link}}
    {{/simple-list-item}}
    {{#> simple-list-item}}
      {{#> simple-list-item-link}}
        List item 2
      {{/simple-list-item-link}}
    {{/simple-list-item}}
    {{#> simple-list-item}}
      {{#> simple-list-item-link}}
        List item 3
      {{/simple-list-item-link}}
    {{/simple-list-item}}
  {{/simple-list-list}}
{{/simple-list}}
```

```hbs title=Simple-list-with-links
{{#> simple-list simple-list-item-link--IsLink="true"}}
  {{#> simple-list-list}}
    {{#> simple-list-item}}
      {{#> simple-list-item-link simple-list-item-link--modifier="pf-m-current"}}
        List item 1
      {{/simple-list-item-link}}
    {{/simple-list-item}}
    {{#> simple-list-item}}
      {{#> simple-list-item-link}}
        List item 2
      {{/simple-list-item-link}}
    {{/simple-list-item}}
    {{#> simple-list-item}}
      {{#> simple-list-item-link}}
        List item 3
      {{/simple-list-item-link}}
    {{/simple-list-item}}
  {{/simple-list-list}}
{{/simple-list}}
```

```hbs title=Grouped-list
{{#> simple-list}}
  {{#> simple-list-section}}
    {{#> simple-list-title}}
      Title
    {{/simple-list-title}}
    {{#> simple-list-list}}
      {{#> simple-list-item}}
        {{#> simple-list-item-link simple-list-item-link--modifier="pf-m-current"}}
          List item 1
        {{/simple-list-item-link}}
      {{/simple-list-item}}
      {{#> simple-list-item}}
        {{#> simple-list-item-link}}
          List item 2
        {{/simple-list-item-link}}
      {{/simple-list-item}}
      {{#> simple-list-item}}
        {{#> simple-list-item-link}}
          List item 3
        {{/simple-list-item-link}}
      {{/simple-list-item}}
      {{#> simple-list-item}}
        {{#> simple-list-item-link}}
          List item 4
        {{/simple-list-item-link}}
      {{/simple-list-item}}
    {{/simple-list-list}}
  {{/simple-list-section}}
  {{#> simple-list-section}}
    {{#> simple-list-title}}
      Title
    {{/simple-list-title}}
    {{#> simple-list-list}}
      {{#> simple-list-item}}
        {{#> simple-list-item-link}}
          List item 1
        {{/simple-list-item-link}}
      {{/simple-list-item}}
      {{#> simple-list-item}}
        {{#> simple-list-item-link}}
          List item 2
        {{/simple-list-item-link}}
      {{/simple-list-item}}
      {{#> simple-list-item}}
        {{#> simple-list-item-link}}
          List item 3
        {{/simple-list-item-link}}
      {{/simple-list-item}}
      {{#> simple-list-item}}
        {{#> simple-list-item-link}}
          List item 4
      {{/simple-list-item-link}}
      {{/simple-list-item}}
    {{/simple-list-list}}
  {{/simple-list-section}}
{{/simple-list}}
```

## Documentation

### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `tabindex="0"` | `a.pf-c-simple-list__item-link` | Inserts the link into the tab order of the page so that it is focusable. **Required** |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-simple-list` | `<div>` | Initiates a simple list. |
| `.pf-c-simple-list__section` | `<section>` | Initiates a simple list section. |
| `.pf-c-simple-list__title` | `<h2>` | Initiates a simple list title. |
| `.pf-c-simple-list__list` | `<ul>` | Initiates a simple list unordered list. |
| `.pf-c-simple-list__item` | `<li>` | Initiates a simple list item. |
| `.pf-c-simple-list__item-link` | `<button>`, `<a>` | Initiates a simple list item link. It can be a button or a link depending on the context. |
| `.pf-m-current` | `.pf-c-simple-list__item-link` | Modifies the simple list item link for the current state. |
