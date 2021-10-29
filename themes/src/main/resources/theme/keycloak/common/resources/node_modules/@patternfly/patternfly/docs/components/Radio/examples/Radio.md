---
id: Radio
section: components
cssPrefix: pf-c-radio
---## Examples

### Basic

```html
<div class="pf-c-radio">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-simple"
    name="exampleRadioSimple"
  />

  <label class="pf-c-radio__label" for="radio-simple">Radio</label>
</div>

```

### Checked

```html
<div class="pf-c-radio">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-checked"
    name="exampleRadioChecked"
    checked
  />

  <label class="pf-c-radio__label" for="radio-checked">Radio checked</label>
</div>

```

### Label wrapping input

```html
<label class="pf-c-radio" for="radio-wrap">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-wrap"
    name="exampleRadioWrap"
  />

  <span class="pf-c-radio__label">Radio label wraps input</span>
</label>

```

### Reversed

```html
<div class="pf-c-radio">
  <label class="pf-c-radio__label" for="radio-rev">Radio reversed</label>

  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-rev"
    name="exampleRadioReversed"
  />
</div>

```

### Disabled

```html
<div class="pf-c-radio">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-disabled"
    name="exampleRadioDisabled"
    disabled
  />

  <label
    class="pf-c-radio__label pf-m-disabled"
    for="radio-disabled"
  >Radio disabled</label>
</div>

<div class="pf-c-radio">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-disabled-checked"
    name="exampleRadioDisabledChecked"
    disabled
    checked
  />

  <label
    class="pf-c-radio__label pf-m-disabled"
    for="radio-disabled-checked"
  >Radio disabled checked</label>
</div>

```

### With description

```html
<div class="pf-c-radio">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-description"
    name="exampleRadioDescription"
  />

  <label
    class="pf-c-radio__label"
    for="radio-description"
  >Radio with description</label>

  <span
    class="pf-c-radio__description"
  >Single-tenant cloud service hosted and managed by Red Hat that offers high-availability enterprise-grade clusters in a virtual private cloud on AWS od GCP.</span>
</div>

```

### With body

```html
<div class="pf-c-radio">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-body"
    name="exampleRadioBody"
  />

  <label class="pf-c-radio__label" for="radio-body">Radio with body</label>

  <span class="pf-c-radio__body">This is where custom content goes.</span>
</div>

```

### With description and body

```html
<div class="pf-c-radio">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-description-body"
    name="exampleRadioDescriptionBody"
  />

  <label
    class="pf-c-radio__label"
    for="radio-description-body"
  >Radio with description and body</label>

  <span
    class="pf-c-radio__description"
  >Single-tenant cloud service hosted and managed by Red Hat that offers high-availability enterprise-grade clusters in a virtual private cloud on AWS od GCP.</span>
  <span class="pf-c-radio__body">This is where custom content goes.</span>
</div>

```

### Standalone input

```html
<div class="pf-c-radio pf-m-standalone">
  <input
    class="pf-c-radio__input"
    type="radio"
    id="radio-standalone"
    name="exampleRadioStandalone"
    aria-label="Standalone input"
  />
</div>

```

## Documentation

### Overview

The Radio component is provided for use cases outside of forms. If it is used without label text ensure some sort of label for assistive technologies. (for example: `aria-label`)

If you extend this component or modify the styles of this component, then make sure any hover styles defined are applied to the clickable elements, like `<input>` or `<label>` since hover styles are used to convey the clickable target area of an element. To maximize the target area, use the example html where the `<label>` is the wrapping element.

### Accessibility

| Attribute  | Applied to             | Outcome                                                                                                           |
| ---------- | ---------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `disabled` | `<input type="radio">` | Indicates that the element is unavailable and removes it from keyboard focus. **Required when input is disabled** |

### Usage

| Class                      | Applied to             | Outcome                                                                                                            |
| -------------------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `.pf-c-radio`              | `<div>`, `<label>`     | Initiates the radio component. **Required**                                                                        |
| `.pf-c-radio__input`       | `<input type="radio">` | Initiates a radio input. **Required**                                                                              |
| `.pf-c-radio__label`       | `<label>`, `<span>`    | Initiates a label. **Required**                                                                                    |
| `.pf-c-radio__description` | `<span>`               | Initiates a radio description.                                                                                     |
| `.pf-c-radio__body`        | `<span>`               | Initiates a radio body.                                                                                            |
| `.pf-m-standalone`         | `.pf-c-radio`          | Modifies the radio component for use with a standalone `<input type="radio">`. **Required when there is no label** |
| `.pf-m-disabled`           | `.pf-c-radio__label`   | Modifies the radio component for the disabled state. **Required when input is disabled**                           |
