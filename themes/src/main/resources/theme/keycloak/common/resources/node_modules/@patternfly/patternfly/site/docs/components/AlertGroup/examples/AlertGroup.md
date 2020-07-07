---
title: Alert group
section: components
cssPrefix: pf-c-alert-group
---

## Examples
```hbs title=Static-alert-group
{{#> alert-group}}
  {{#> alert-item}}
    {{#> alert alert--modifier="pf-m-inline pf-m-success" alert--attribute='aria-label="Success alert"'}}
      {{#> alert-icon alert-icon--type="check-circle"}}
      {{/alert-icon}}
      {{#> alert-title}}
        {{#> screen-reader}}Success alert:{{/screen-reader}}
        Success alert title
      {{/alert-title}}
    {{/alert}}
  {{/alert-item}}

  {{#> alert-item}}
    {{#> alert alert--modifier="pf-m-inline pf-m-danger" alert--attribute='aria-label="Danger alert"'}}
      {{#> alert-icon alert-icon--type="exclamation-circle"}}
      {{/alert-icon}}
      {{#> alert-title}}
        {{#> screen-reader}}Danger alert:{{/screen-reader}}
        Danger alert title
      {{/alert-title}}
    {{/alert}}
  {{/alert-item}}

  {{#> alert-item}}
    {{#> alert alert--modifier="pf-m-inline pf-m-info" alert--attribute='aria-label="Information alert"'}}
      {{#> alert-icon alert-icon--type="info-circle"}}
      {{/alert-icon}}
      {{#> alert-title}}
        {{#> screen-reader}}Info alert:{{/screen-reader}}
        Info alert title
      {{/alert-title}}
      {{#> alert-description}}
        Info alert description. <a href="#">This is a link.</a>
      {{/alert-description}}
    {{/alert}}
  {{/alert-item}}
{{/alert-group}}
```

### Overview
`.pf-c-alert-group` is optional when only one alert is needed. It becomes required when more than one alert is used in a list.
### Usage
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-alert-group` | `<ul>` | Creates an alert group component. **Required** |
| `.pf-c-alert-group__item` | `<li>` | Creates an alert group item. **Required** |

```hbs title=Toast-alert-group isFullscreen=true
{{#> alert-group alert-group--modifier="pf-m-toast"}}
  {{#> alert-item}}
    {{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success toast alert"'}}
      {{#> alert-icon alert-icon--type="check-circle"}}
      {{/alert-icon}}
      {{#> alert-title alert-title--attribute='id="alert_one_title"'}}
        {{#> screen-reader}}Success alert:{{/screen-reader}}
        Success toast alert title
      {{/alert-title}}
      {{#> alert-action}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
          <i class="fas fa-times" aria-hidden="true"></i>
        {{/button}}
      {{/alert-action}}
    {{/alert}}
  {{/alert-item}}

  {{#> alert-item}}
    {{#> alert alert--modifier="pf-m-danger" alert--attribute='aria-label="Danger toast alert"'}}
      {{#> alert-icon alert-icon--type="exclamation-circle"}}
      {{/alert-icon}}
      {{#> alert-title alert-title--attribute='id="alert_two_title"'}}
        {{#> screen-reader}}Danger alert:{{/screen-reader}}
        Danger toast alert title
      {{/alert-title}}
      {{#> alert-action}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
          <i class="fas fa-times" aria-hidden="true"></i>
        {{/button}}
      {{/alert-action}}
    {{/alert}}
  {{/alert-item}}

  {{#> alert-item}}
    {{#> alert alert--modifier="pf-m-info" alert--attribute='aria-label="Information toast alert"'}}
      {{#> alert-icon alert-icon--type="info-circle"}}
      {{/alert-icon}}
      {{#> alert-title alert-title--attribute='id="alert_three_title"'}}
        {{#> screen-reader}}Info alert:{{/screen-reader}}
        Info toast alert title
      {{/alert-title}}
      {{#> alert-description}}
        Info toast alert description. <a href="#">This is a link.</a>
      {{/alert-description}}
      {{#> alert-action}}
        {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
          <i class="fas fa-times" aria-hidden="true"></i>
        {{/button}}
      {{/alert-action}}
    {{/alert}}
  {{/alert-item}}
{{/alert-group}}
```
### Overview
An alert group that includes the `.pf-m-toast` modifier becomes a toast alert group with unique positioning in the top-right corner of the window. `.pf-c-alert-group` is required to create a toast alert group.

Every toast alert must include a close button to dismiss the alert.

### Modifiers
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-toast`| `.pf-c-alert-group` | Applies toast alert styling to an alert group. |

## Documentation
### Overview
Alert groups are used to contain and align consecutive alerts. Groups can either be embedded alongside a page's content or in the top-right corner as a toast group using the `.pf-m-toast` modifier.
