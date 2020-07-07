---
title: 'Hint'
section: components
beta: true
cssPrefix: pf-c-hint
---

## Examples
```hbs title=Hint-with-title
{{#> hint}}
  {{#> hint-actions}}
    {{#> dropdown id="hint-with-title-dropdown-kebab" dropdown--IsActionMenu="true" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}{{/dropdown}}
  {{/hint-actions}}
  {{#> hint-title}}
    Do more with Find it Fix it capabilities
  {{/hint-title}}
  {{#> hint-body}}
    Upgrade to Red Hat Smart Management to remediate all your systems across regions and geographies.
  {{/hint-body}}
{{/hint}}

<br>

{{#> hint}}
  {{#> hint-actions}}
    {{#> dropdown id="hint-with-title-with-footer-dropdown-kebab" dropdown--IsActionMenu="true" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}{{/dropdown}}
  {{/hint-actions}}
  {{#> hint-title}}
    Do more with Find it Fix it capabilities
  {{/hint-title}}
  {{#> hint-body}}
    Upgrade to Red Hat Smart Management to remediate all your systems across regions and geographies.
  {{/hint-body}}
  {{#> hint-footer}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      Try it for 90 days
    {{/button}}
  {{/hint-footer}}
{{/hint}}
```

```hbs title=Default-with-no-title
{{#> hint}}
  {{#> hint-body}}
    Welcome to the new documentation experience.
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      Learn more about the improved features.
    {{/button}}
  {{/hint-body}}
{{/hint}}

<br>

{{#> hint}}
  {{#> hint-actions}}
    {{#> dropdown id="hint-with-no-title-dropdown-kebab" dropdown--IsActionMenu="true" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}{{/dropdown}}
  {{/hint-actions}}
  {{#> hint-body}}
    Upgrade to Red Hat Smart Management to remediate all your systems across regions and geographies.
  {{/hint-body}}
  {{#> hint-footer}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      Try it for 90 days
    {{/button}}
  {{/hint-footer}}
{{/hint}}
```

## Documentation


### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-hint` | `<div>` | Initiates the hint component. **Required** |
| `.pf-c-hint__title` | `<div>` | Initiates the hint title element. |
| `.pf-c-hint__body` | `<div>` | Initiates the hint body element. |
| `.pf-c-hint__footer` | `<div>` | Initiates the hint footer element. |
| `.pf-c-hint__actions` | `<div>` | Initiates the hint actions element. |
