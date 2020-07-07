---
title: Alert
section: components
cssPrefix: pf-c-alert
---

## Examples
```hbs title=Types
{{#> alert alert--attribute='aria-label="Default alert"'}}
  {{#> alert-icon alert-icon--type="bell"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Default alert:{{/screen-reader}}
    Default alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-info" alert--attribute='aria-label="Information alert"'}}
  {{#> alert-icon alert-icon--type="info-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Info alert:{{/screen-reader}}
    Info alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
    Success alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-warning" alert--attribute='aria-label="Warning alert"'}}
  {{#> alert-icon alert-icon--type="exclamation-triangle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Warning alert:{{/screen-reader}}
    Warning alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-danger" alert--attribute='aria-label="Danger alert"'}}
  {{#> alert-icon alert-icon--type="exclamation-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Danger alert:{{/screen-reader}}
    Danger alert title
  {{/alert-title}}
{{/alert}}
```

```hbs title=Variations
{{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
    Success alert title
  {{/alert-title}}
  {{#> alert-action}}
    {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
  {{/alert-action}}
   {{#> alert-description}}
    Success alert description. This should tell the user more information about the alert.
  {{/alert-description}}
  {{#> alert-action-group}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      View details
    {{/button}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      Ignore
    {{/button}}
  {{/alert-action-group}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
      Success alert title
  {{/alert-title}}
  {{#> alert-action}}
    {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
  {{/alert-action}}
  {{#> alert-description}}
    Success alert description. This should tell the user more information about the alert. <a href="#">This is a link.</a>
  {{/alert-description}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
    Success alert title
  {{/alert-title}}
  {{#> alert-action}}
    {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
  {{/alert-action}}
  {{#> alert-action-group}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      View details
    {{/button}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      Ignore
    {{/button}}
  {{/alert-action-group}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
    Success alert title
  {{/alert-title}}
  {{#> alert-action}}
    {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
  {{/alert-action}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
      Success alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success alert with title truncation"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title alert-title--modifier="pf-m-truncate"}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur pellentesque neque cursus enim fringilla tincidunt. Proin lobortis aliquam dictum. Nam vel ullamcorper nulla, nec blandit dolor. Vivamus pellentesque neque justo, nec accumsan nulla rhoncus id. Suspendisse mollis, tortor quis faucibus volutpat, sem leo fringilla turpis, ac lacinia augue metus in nulla. Cras vestibulum lacinia orci. Pellentesque sodales consequat interdum. Sed porttitor tincidunt metus nec iaculis. Pellentesque non commodo justo. Morbi feugiat rhoncus neque, vitae facilisis diam aliquam nec. Sed dapibus vitae quam at tristique. Nunc vel commodo mi. Mauris et rhoncus leo.
  {{/alert-title}}
  {{#> alert-description}}
    This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.
  {{/alert-description}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success" alert--attribute='aria-label="Success alert with title truncation at 2 lines"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title alert-title--modifier="pf-m-truncate" alert-title--attribute='style="--pf-c-alert__title--max-lines: 2"'}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur pellentesque neque cursus enim fringilla tincidunt. Proin lobortis aliquam dictum. Nam vel ullamcorper nulla, nec blandit dolor. Vivamus pellentesque neque justo, nec accumsan nulla rhoncus id. Suspendisse mollis, tortor quis faucibus volutpat, sem leo fringilla turpis, ac lacinia augue metus in nulla. Cras vestibulum lacinia orci. Pellentesque sodales consequat interdum. Sed porttitor tincidunt metus nec iaculis. Pellentesque non commodo justo. Morbi feugiat rhoncus neque, vitae facilisis diam aliquam nec. Sed dapibus vitae quam at tristique. Nunc vel commodo mi. Mauris et rhoncus leo.
  {{/alert-title}}
  {{#> alert-description}}
    This example uses ".pf-m-truncate" and sets "--pf-c-alert__title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.
  {{/alert-description}}
{{/alert}}
```

```hbs title=Inline-types
{{#> alert alert--modifier="pf-m-inline" alert--attribute='aria-label="Inline default alert"'}}
  {{#> alert-icon alert-icon--type="bell"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Default inline alert:{{/screen-reader}}
    Default inline alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-info pf-m-inline" alert--attribute='aria-label="Inline information alert"'}}
  {{#> alert-icon alert-icon--type="info-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Info alert:{{/screen-reader}}
    Info inline alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success pf-m-inline" alert--attribute='aria-label="Inline success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
    Success inline alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-warning pf-m-inline" alert--attribute='aria-label="Inline warning alert"'}}
  {{#> alert-icon alert-icon--type="exclamation-triangle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Warning alert:{{/screen-reader}}
    Warning inline alert title
  {{/alert-title}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-danger pf-m-inline" alert--attribute='aria-label="Inline danger alert"'}}
  {{#> alert-icon alert-icon--type="exclamation-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Danger alert:{{/screen-reader}}
    Danger inline alert title
  {{/alert-title}}
{{/alert}}
```

```hbs title=Inline-variations
{{#> alert alert--modifier="pf-m-success pf-m-inline" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
    Success alert title
  {{/alert-title}}
  {{#> alert-action}}
    {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
  {{/alert-action}}
  {{#> alert-description}}
    Success alert description. This should tell the user more information about the alert.
  {{/alert-description}}
  {{#> alert-action-group}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      View details
    {{/button}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      Ignore
    {{/button}}
  {{/alert-action-group}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success pf-m-inline" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
      Success alert title
  {{/alert-title}}
  {{#> alert-action}}
    {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
  {{/alert-action}}
  {{#> alert-description}}
    Success alert description. This should tell the user more information about the alert. <a href="#">This is a link.</a>
  {{/alert-description}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success pf-m-inline" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
    Success alert title
  {{/alert-title}}
  {{#> alert-action}}
    {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close success alert: Success alert title"'}}
      <i class="fas fa-times" aria-hidden="true"></i>
    {{/button}}
  {{/alert-action}}
  {{#> alert-action-group}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      View details
    {{/button}}
    {{#> button button--modifier="pf-m-link pf-m-inline"}}
      Ignore
    {{/button}}
  {{/alert-action-group}}
{{/alert}}
<br>
{{#> alert alert--modifier="pf-m-success pf-m-inline" alert--attribute='aria-label="Success alert"'}}
  {{#> alert-icon alert-icon--type="check-circle"}}
  {{/alert-icon}}
  {{#> alert-title}}
    {{#> screen-reader}}Success alert:{{/screen-reader}}
      Success alert title
  {{/alert-title}}
{{/alert}}
```

## Documentation
### Overview
Add a modifier class to the default alert to change the color: `.pf-m-success`, `.pf-m-danger`, `.pf-m-warning`, or `.pf-m-info`.

### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-label="Default alert"` | `.pf-c-alert` |  Indicates the default alert. |
| `aria-label="Success alert"` | `.pf-c-alert` |  Indicates the success alert. |
| `aria-label="Danger alert"` | `.pf-c-alert` |  Indicates the danger alert. |
| `aria-label="Warning alert"` | `.pf-c-alert` |  Indicates the warning alert. |
| `aria-label="Information alert"` | `.pf-c-alert` |  Indicates the information alert. |
| `aria-label="Close success alert: Success alert title"` | `.pf-c-button.pf-m-plain` | Indicates the close button. Please provide descriptive text to ensure assistive technologies clearly state which alert is being closed.|
| `aria-hidden="true"` | `.pf-c-alert__icon <i>` |  Hides icon for assistive technologies. ** Required **|

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-screen-reader` | `.pf-c-alert__title <span>` | Content that is visually hidden but accessible to assistive technologies. This should state the type of alert.  ** Required**|

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-alert` | `<div>` |  Applies default alert styling. Always use with a modifier class. ** Required**|
| `.pf-c-alert__icon` | `<div>` |  	Defines the alert icon. ** Required **|
| `.pf-c-alert__title` | `<p>, <h1-h6>` |  Defines the alert title. ** Required **|
| `.pf-c-alert__description` | `<div>` |  Defines the alert description area. |
| `.pf-c-alert__action` | `<div>` |  Defines the action button wrapper. Should contain `.pf-c-button.pf-m-plain` for close action or `.pf-c-button.pf-m-link` for link text. It should only include one action. |
| `.pf-c-alert__action-group` | `<div>` |  Defines the action button group. Should contain `.pf-c-button.pf-m-link.pf-m-inline` for inline link text. **Note: ** only inline link buttons are supported in the alert action group. |
| `.pf-m-success` | `.pf-c-alert` |  Applies success styling. |
| `.pf-m-danger` | `.pf-c-alert` |  Applies danger styling. |
| `.pf-m-warning` | `.pf-c-alert` |  Applies warning styling. |
| `.pf-m-info` | `.pf-c-alert` |  Applies info styling. |
| `.pf-m-inline` | `.pf-c-alert` |  Applies inline styling. |
| `.pf-m-truncate` | `.pf-c-alert__title` |  Modifies the title to display a single line and truncate any overflow text with ellipses. **Note:** you can specify the max number of lines to show by setting the `--pf-c-alert__title--max-lines` (the default value is `1`). |
