---
title: Breadcrumb
section: components
cssPrefix: pf-c-breadcrumb
---

## Examples
```hbs title=Basic
{{#> breadcrumb}}
  {{#> breadcrumb-list}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section home
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link breadcrumb-link--current="true"}}
        Section landing
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
  {{/breadcrumb-list}}
{{/breadcrumb}}
```

```hbs title=Without-home-link
{{#> breadcrumb}}
  {{#> breadcrumb-list}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
        Section home
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link breadcrumb-link--current="true"}}
        Section landing
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
  {{/breadcrumb-list}}
{{/breadcrumb}}
```

```hbs title=With-heading
{{#> breadcrumb}}
  {{#> breadcrumb-list}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section home
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-link}}
        Section title
      {{/breadcrumb-link}}
    {{/breadcrumb-item}}
    {{#> breadcrumb-item}}
      {{> breadcrumb-item-divider}}
      {{#> breadcrumb-heading}}
        {{#> breadcrumb-link breadcrumb-link--current="true"}}
          Section title
        {{/breadcrumb-link}}
      {{/breadcrumb-heading}}
    {{/breadcrumb-item}}
  {{/breadcrumb-list}}
{{/breadcrumb}}
```

## Documentation
### Overview
A breadcrumb is a list of links to display a user's navigational hierarchy. The last item of the breadcrumb list indicates the current page's location.

* `.pf-c-breadcrumb__list` is the default breadcrumb navigation. It provides links to previous navigation pages and also shows the current page's location.

In the event that a page does not have a traditional `<h1>` page title, a heading can be included in the breadcrumbs and an optional link within.

### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-label="[landmark description]"` | `.pf-c-breadcrumb` |  Describes `<nav>` landmark. |
| `aria-label="[link name]"` | `.pf-c-breadcrumb__link` | If link has no text (icon), add an aria-label. |
| `aria-current="page"` | `.pf-c-breadcrumb__item`, `.pf-c-breadcrumb__link` |  Indicates the current page within a set of pages. |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-breadcrumb`                | `<nav>`                   | Initiates a primary breadcrumb element. **Required** |
| `.pf-c-breadcrumb__list`          | `<ol>`                    | Initiates default breadcrumb ordered list. |
| `.pf-c-breadcrumb__item`          | `<li>`                    | Initiates default breadcrumb list item. |
| `.pf-c-breadcrumb__item-divider`  | `<span>`                  | Initiates default breadcrumb list item divider. |
| `.pf-c-breadcrumb__link`          | `<a>`                     | Initiates default breadcrumb list link. |
| `.pf-c-breadcrumb__title`         | `<h1>`                    | Initiates breadcrumb header. |
| `.pf-m-current`                   | `.pf-c-breadcrumb__link`  | Modifies to display the list item as the current item. |
