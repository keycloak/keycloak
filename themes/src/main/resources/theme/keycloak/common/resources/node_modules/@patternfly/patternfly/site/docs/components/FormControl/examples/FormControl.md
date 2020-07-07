---
title: Form control
section: components
cssPrefix: pf-c-form-control
---

## Examples
```hbs title=Input
{{#> form-control controlType="input" input="true" form-control--attribute='type="text" value="Standard" id="input-standard" aria-label="Standard input example"'}}
{{/form-control}}
<br><br>
{{#> form-control controlType="input" input="true" form-control--attribute='readonly type="text" value="Readonly" id="input-readonly" aria-label="Readonly input example"'}}
{{/form-control}}
<br><br>
{{#> form-control controlType="input" input="true" form-control--modifier="pf-m-success" form-control--attribute='type="text" value="Success" id="input-success" aria-label="Success state input example"'}}
{{/form-control}}
<br><br>
{{#> form-control controlType="input" input="true" form-control--modifier="pf-m-warning" form-control--attribute='type="text" value="Warning" id="input-warning" aria-label="Warning state input example"'}}
{{/form-control}}
<br><br>
{{#> form-control controlType="input" input="true" form-control--attribute='required type="text" value="Error" id="input-error" aria-invalid="true" aria-label="Error state input example"'}}
{{/form-control}}
<br><br>
{{#> form-control controlType="input" input="true" form-control--attribute='disabled type="text" value="Disabled" id="input-disabled" aria-label="Disabled input example"'}}
{{/form-control}}
<br><br>
{{#> form-control controlType="input" input="true" form-control--modifier="pf-m-search" form-control--attribute='type="search" id="input-search" name="search-input" aria-label="Search input example"'}}
{{/form-control}}
```

```hbs title=Select
{{#> form-control controlType="select" form-control--attribute='id="select-standard" name="select-standard" aria-label="Standard select example"'}}
  <option value="" selected>Please choose</option>
  <option value="Mr">Mr</option>
  <option value="Miss">Miss</option>
  <option value="Mrs">Mrs</option>
  <option value="Ms">Ms</option>
  <option value="Dr">Dr</option>
  <option value="Other">Other</option>
{{/form-control}}
<br><br>
{{#> form-control controlType="select" form-control--attribute='id="select-group" name="select-group" aria-label="Select group example"'}}
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2" selected>The second option is selected by default</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
{{/form-control}}
<br><br>
{{#> form-control controlType="select" form-control--modifier="pf-m-success" form-control--attribute='id="select-group-success" name="select-group-success" aria-label="Success state select group example"'}}
  <option value="">Valid option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
{{/form-control}}
<br><br>
{{#> form-control controlType="select" form-control--modifier="pf-m-warning" form-control--attribute='id="select-group-warning" name="select-group-warning" aria-label="Warning state select group example"'}}
  <option value="">Warning option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
{{/form-control}}
<br><br>
{{#> form-control controlType="select" form-control--attribute='required aria-invalid="true" id="select-group-error" name="select-group-error" aria-label="Error state select group example"'}}
  <option value="">Invalid option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
{{/form-control}}
<br><br>
```

```hbs title=Textarea
{{#> form-control controlType="textarea" form-control--attribute='name="textarea-standard" id="textarea-standard" aria-label="Standard textarea example"'}}
Standard
{{/form-control}}
<br><br>
{{#> form-control controlType="textarea" form-control--attribute='readonly name="textarea-readonly" id="textarea-readonly" aria-label="Readonly textarea example"'}}
Readonly
{{/form-control}}
<br><br>
{{#> form-control controlType="textarea" form-control--modifier="pf-m-success" form-control--attribute='name="textarea-success" id="textarea-success" aria-label="Success state textarea example"'}}
Success
{{/form-control}}
<br><br>
{{#> form-control controlType="textarea" form-control--modifier="pf-m-warning" form-control--attribute='name="textarea-warning" id="textarea-warning" aria-label="Warning state textarea example"'}}
Warning
{{/form-control}}
<br><br>
{{#> form-control controlType="textarea" form-control--attribute='required name="textarea-error" id="textarea-error" aria-label="Error state textarea example" aria-invalid="true"'}}
Error
{{/form-control}}
<br><br>
{{#> form-control controlType="textarea" form-control--attribute='name="textarea-resize-vertical" id="textarea-resize-vertical" aria-label="Resize vertical textarea example"' form-control--modifier="pf-m-resize-vertical"}}
Resizes vertically
{{/form-control}}
<br><br>
{{#> form-control controlType="textarea" form-control--attribute='name="textarea-resize-horizontal" id="textarea-resize-horizontal" aria-label="Resize horizontal textarea example"' form-control--modifier="pf-m-resize-horizontal"}}
Resizes horizontally
{{/form-control}}
```

## Documentation

### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `id` | `.pf-c-form-control` | Provides an `id` value that can be used with the `for` attribute on an associated `<label>` element to provide an accessible label for the form control element.
| `aria-invalid="true"` | `.pf-c-form-control` | Indicates that the form control is in the error state and applies error state styling. |
| `aria-label="descriptive text"` | `.pf-c-form-control` | Provides an accessible label for assistive technology. |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-form-control` | `<input>`,`<textarea>`, `<select>` |  Initiates an input, textarea or select. For styling of checkboxes or radios see the [check component](/documentation/core/components/check). **Required**  |
| `.pf-m-resize-vertical` | `textarea.pf-m-form-control` | Modifies a `textarea.pf-c-form-control` element so it can only be resized vertically along the y-axis. |
| `.pf-m-resize-horizontal` | `textarea.pf-m-form-control` | Modifies a `textarea.pf-c-form-control` element so it can only be resized horizontally along the x-axis. |
| `.pf-m-success` | `.pf-c-form-control` | Modifies a form control for the success state. |
| `.pf-m-warning` | `.pf-c-form-control` | Modifies a form control for the warning state. |
| `.pf-m-search` | `.pf-c-form-control` | Modifies a form control for the search input. |
