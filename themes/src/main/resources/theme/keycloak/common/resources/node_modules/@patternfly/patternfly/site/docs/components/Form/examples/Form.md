---
title: Form
section: components
cssPrefix: pf-c-form
---

## Examples
```hbs title=Vertically-aligned-labels
{{#> form form--id="vertical-align-labels"}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute=(concat 'for="' form--id '-form-name"') required="true"}}
        Name
      {{/form-label}}
      {{> form-group-label-help}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute=(concat 'type="text" id="' form--id '-form-name" name="' form--id '-form-name" required')}}{{/form-control}}
    {{/form-group-control}}
  {{/form-group}}
{{/form}}
```

```hbs title=Horizontally-aligned-labels
{{#> form form--modifier="pf-m-horizontal" form--id="horizontal-align-labels-1"}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute=(concat 'for="' form--id '-form-name"') required="true"}}
        Name
      {{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute=(concat 'type="text" id="' form--id '-form-name" name="' form--id '-form-name" required')}}{{/form-control}}
    {{/form-group-control}}
  {{/form-group}}
{{/form}}

<br>

{{#> form form--modifier="pf-m-horizontal" form--id="horizontal-align-labels-2"}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute=(concat 'for="' form--id '-form-name"')}}
        Information
      {{/form-label}}
      {{> form-group-label-help}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="textarea" form-control--attribute=(concat 'type="text" id="' form--id '-form-name-2" name="' form--id '-form-name-2" aria-label="textarea example"')}}{{/form-control}}
    {{/form-group-control}}
  {{/form-group}}
{{/form}}

<br />

{{#> form form--modifier="pf-m-horizontal" form--id="horizontal-align-labels-top"}}
  {{#> form-group}}
    {{#> form-group-label form-group-label--modifier="pf-m-no-padding-top"}}
      {{#> form-label form-label--attribute=(concat 'for="' form--id '-form-name"')}}
        Label (no top padding)
      {{/form-label}}
      {{> form-group-label-help}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> check}}
        {{#> check-input check-input--attribute='type="checkbox" id="alt-form-checkbox1" name="alt-form-checkbox1"'}}{{/check-input}}
        {{#> check-label check-label--attribute='for="alt-form-checkbox1"'}}Option 1{{/check-label}}
      {{/check}}
      {{#> check}}
        {{#> check-input check-input--attribute='type="checkbox" id="alt-form-checkbox2" name="alt-form-checkbox2"'}}{{/check-input}}
        {{#> check-label check-label--attribute='for="alt-form-checkbox2"'}}Option 2{{/check-label}}
      {{/check}}
    {{/form-group-control}}
  {{/form-group}}
{{/form}}
```

```hbs title=Help-text
{{#> form form--id="help-text"}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute=(concat 'for="' form--id '-form-name"') required='true'}}
        Name
      {{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute=(concat 'required type="text" id="' form--id '-form-name" name="' form--id '-form-name" aria-describedby="' form--id '-form-name-helper"')}}
      {{/form-control}}
      {{#> form-helper-text form-helper-text--attribute=(concat 'id="' form--id '-form-name-helper" aria-live="polite"')}}
        This is helper text
      {{/form-helper-text}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute=(concat 'for="' form--id '-form-email"') required='true'}}
        E-mail
      {{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control form-control--modifier="pf-m-warning" controlType="input" input="true" form-control--attribute=(concat 'required type="text" id="' form--id '-form-email" name="' form--id '-form-email" aria-describedby="' form--id '-form-email-helper"')}}
      {{/form-control}}
      {{#> form-helper-text  form-helper-text--modifier="pf-m-warning" form-helper-text--attribute=(concat 'id="' form--id '-form-email-helper" aria-live="polite"')}}
        This is helper text for a warning input
      {{/form-helper-text}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute=(concat 'for="' form--id '-form-address"') required='true'}}
        Address
      {{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute=(concat 'required type="text" id="' form--id '-form-address" name="' form--id '-form-address" aria-invalid="true" aria-describedby="' form--id '-form-address-helper"')}}
      {{/form-control}}
      {{#> form-helper-text form-helper-text--modifier="pf-m-error" form-helper-text--attribute=(concat 'id="' form--id '-form-address-helper" aria-live="polite"')}}
        This is helper text for an invalid input
      {{/form-helper-text}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute=(concat 'for="' form--id '-form-comment"')}}
        Comment
      {{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--modifier="pf-m-success" form-control--attribute=(concat 'value="This is a valid comment"' 'type="text" id="' form--id '-form-comment" name="' form--id '-form-comment" aria-describedby="' form--id '-form-comment-help"')}}
      {{/form-control}}
      {{#> form-helper-text form-helper-text--modifier="pf-m-success" form-helper-text--attribute=(concat 'id="' form--id '-form-comment-help" aria-live="polite"')}}
        This is helper text for success input
      {{/form-helper-text}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-label form-label--attribute=(concat 'for="' form--id '-simple-form-info"')}}
      Information
    {{/form-label}}
    {{#> form-control controlType="textarea" form-control--attribute=(concat 'id="' form--id '-simple-form-info" name="' form--id '-simple-form-info" aria-invalid="true" aria-describedby="' form--id '-simple-form-info-helper"')}}
    {{/form-control}}
    {{#> form-helper-text form-helper-text--modifier="pf-m-error" form-helper-text--attribute=(concat 'id="' form--id '-simple-form-info-helper" aria-live="polite"')}}
      {{#> form-helper-text-icon}}
        <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
      {{/form-helper-text-icon}}
      This is helper text with an icon.
    {{/form-helper-text}}
  {{/form-group}}
{{/form}}
```

```hbs title=Action-group
{{#> form}}
  {{#> form-group form-group--modifier="pf-m-action"}}
    {{#> form-actions}}
      {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
        Submit form
      {{/button}}
      {{#> button button--modifier="pf-m-secondary" button--IsReset="true"}}
        Reset form
      {{/button}}
    {{/form-actions}}
  {{/form-group}}
{{/form}}
```

## Documentation
### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `for` | `<label>` |  Each `<label>` must have a `for` attribute that matches its form field id. **Required** |
| `id` | `<input type="radio/checkbox/text">`, `<select>`, `<textarea>` |  Each `<form>` field must have an `id` attribute that matches its label's `for` value. **Required** |
| `required` | `<input>`, `<select>`, `<textarea>` | Required fields must include these attributes. |
| `id="{helper_text_id}"` | `.pf-c-form__helper-text` | Form fields with related `.pf-c-form__helper-text` require this attribute. Usage `<p class="pf-c-form__helper-text" id="{helper_text_id}">`.  |
| `aria-describedby="{helper_text_id}"` | `<input>`, `<select>`, `<textarea>` | Form fields with related `.pf-c-form__helper-text` require this attribute. Usage `<input aria-describedby="{helper_text_id}">`.  |
| `aria-invalid="true" aria-describedby="{helper_text_id}"` | `<input>`, `<select>`, `<textarea>` |  When form validation fails `aria-describedby` is used to communicate the error to the user. These attributes need to be handled with Javascript so that `aria-describedby` only references help text that explains the error, and so that `aria-invalid="true"` is only present when validation fails. For proper styling of errors `aria-invalid="true"` is required. |
| `aria-hidden="true"` | `.pf-c-form__label-required` |  Hides the required indicator from assistive technologies. |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-form` | `<form>` |  Initiates a standard form. **Required** |
| `.pf-c-form__group` | `<div>` |  Initiates a form group section. |
| `.pf-c-form__group-label` | `<div>` |  Initiates a form group label section. |
| `.pf-c-form__label` | `<label>` |  Initiates a form label. **Required** |
| `.pf-c-form__label-text` | `<span>` |  Initiates a form label text. **Required** |
| `.pf-c-form__label-required` | `<span>` |  Initiates a form label required indicator. |
| `.pf-c-form__group-label-help` | `<button>` | Initiates a field level help button. |
| `.pf-c-form__group-control` | `<div>` |  Initiates a form group control section. |
| `.pf-c-form__actions` | `<div>` | Iniates a row of actions. |
| `.pf-c-form__helper-text` | `<p>` |  Initiates a form helper text block. |
| `.pf-c-form__helper-text-icon` | `<span>` |  Initiates a form helper text icon. |
| `.pf-m-horizontal` | `.pf-c-form` | Modifies form for a horizontal layout. |
| `.pf-m-action` | `.pf-c-form__group` | Modifies form group margin-top. |
| `.pf-m-success` | `.pf-c-form__helper-text` | Modifies text color of helper text for success state. |
| `.pf-m-warning` | `.pf-c-form__helper-text` | Modifies text color of helper text for warning state. |
| `.pf-m-error` | `.pf-c-form__helper-text`| Modifies text color of helper text for error state. |
| `.pf-m-inactive` | `.pf-c-form__helper-text`| Modifies display of helper text to none. |
| `.pf-m-disabled` | `.pf-c-form__label` | Modifies form label to show disabled state. |
| `.pf-m-no-padding-top` | `.pf-c-form__group-label` | Removes top padding from the label element for labels adjacent to an element that isn't a form control. |
| `.pf-m-inline` | `.pf-c-form__group-control` | Modifies form group children to be inline (this is primarily for radio buttons and checkboxes). |
