---
title: Basic forms
section: demos
---

## Demos
```hbs title=Basic
{{#> form}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute='for="simple-form-name"' required="true"}}Name{{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute='required type="text" id="simple-form-name" name="simple-form-name" aria-describedby="simple-form-name-helper-1"'}}{{/form-control}}
      {{#> form-helper-text form-helper-text--attribute='id="simple-form-name-helper-1" aria-live="polite"'}}Please provide your full name{{/form-helper-text}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute='for="simple-form-email"' required="true"}}Email{{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute='required type="email" id="simple-form-email" name="simple-form-email"'}}{{/form-control}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute='for="simple-form-number"'}}Phone number{{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute='type="tel" id="simple-form-number" name="simple-form-number" placeholder="555-555-5555"'}}{{/form-control}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label required="true"}}How can we contact you?{{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control form-group-control--modifier="pf-m-inline"}}
      {{#> check}}
        {{#> check-input check-input--attribute='type="radio" id="inlineradio1" name="inlineradio1" required'}}{{/check-input}}
        {{#> check-label check-label--attribute='for="inlineradio1"'}}Email{{/check-label}}
      {{/check}}
      {{#> check}}
        {{#> check-input check-input--attribute='type="radio" id="inlineradio2" name="inlineradio2"'}}{{/check-input}}
        {{#> check-label check-label--attribute='for="inlineradio2"'}}Phone{{/check-label}}
      {{/check}}
      {{#> check}}
        {{#> check-input check-input--attribute='type="radio" id="inlineradio3" name="inlineradio3"'}}{{/check-input}}
        {{#> check-label check-label--attribute='for="inlineradio3"'}}Please don't contact me{{/check-label}}
      {{/check}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-control}}
      {{#> check}}
        {{#> check-input check-input--attribute='type="checkbox" id="checkbox1" name="checkbox1"'}}{{/check-input}}
        {{#> check-label check-label--attribute='for="checkbox1"'}}I'd like updates via email{{/check-label}}
      {{/check}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group form-group--modifier="pf-m-action"}}
    {{#> form-actions}}
      {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
        Submit form
      {{/button}}
      {{#> button button--modifier="pf-m-secondary"}}
        Cancel
      {{/button}}
    {{/form-actions}}
  {{/form-group}}
{{/form}}
```

```hbs title=Horizontal
{{#> form form--modifier="pf-m-horizontal"}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute='for="horizontal-form-name"' required="true"}}Name{{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute='required type="text" id="horizontal-form-name" name="horizontal-form-name" aria-describedby="horizontal-form-name-helper2"'}}
      {{/form-control}}
      {{#> form-helper-text form-helper-text--attribute='id="horizontal-form-name-helper2" aria-live="polite"'}}Please provide your full name{{/form-helper-text}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute='for="horizontal-form-email"'}}Email{{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="input" input="true" form-control--attribute='type="email" id="horizontal-form-email" name="horizontal-form-email"'}}
      {{/form-control}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute='for="horizontal-form-title"'}}Your title{{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="select" form-control--attribute='id="horizontal-form-title" name="horizontal-form-title"'}}
        <option value="" selected>Please choose</option>
        <option value="Mr">Mr</option>
        <option value="Miss">Miss</option>
        <option value="Mrs">Mrs</option>
        <option value="Ms">Ms</option>
        <option value="Dr">Dr</option>
        <option value="Other">Other</option>
      {{/form-control}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-label}}
      {{#> form-label form-label--attribute='for="horizontal-form-exp"'}}Your experience{{/form-label}}
    {{/form-group-label}}
    {{#> form-group-control}}
      {{#> form-control controlType="textarea" form-control--attribute='name="horizontal-form-exp" id="horizontal-form-exp"'}}{{/form-control}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-control}}
      {{#> check}}
        {{#> check-input check-input--attribute='type="checkbox" id="alt-form-checkbox1" name="alt-form-checkbox1"'}}{{/check-input}}
        {{#> check-label check-label--attribute='for="alt-form-checkbox1"'}}Follow up via email{{/check-label}}
      {{/check}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group}}
    {{#> form-group-control}}
      {{#> check}}
        {{#> check-input check-input--attribute='type="checkbox" id="alt-form-checkbox2" name="alt-form-checkbox2"'}}{{/check-input}}
        {{#> check-label check-label--attribute='for="alt-form-checkbox2"'}}Remember my password for 30 days{{/check-label}}
      {{/check}}
    {{/form-group-control}}
  {{/form-group}}
  {{#> form-group form-group--modifier="pf-m-action"}}
    {{#> form-group-control}}
      {{#> form-actions}}
        {{#> button button--modifier="pf-m-primary" button--IsSubmit="true"}}
          Submit form
        {{/button}}
        {{#> button button--modifier="pf-m-secondary"}}
          Cancel
        {{/button}}
      {{/form-actions}}
    {{/form-group-control}}
  {{/form-group}}
{{/form}}
```
