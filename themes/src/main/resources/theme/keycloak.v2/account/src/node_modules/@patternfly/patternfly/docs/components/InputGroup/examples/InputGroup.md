---
id: Input group
section: components
cssPrefix: pf-c-input-group
---### Overview

Use the input group to extend form controls by adding text, buttons, selects, etc. The input group handles border overlap.

## Examples

### Variations

```html
<div class="pf-c-input-group">
  <button
    class="pf-c-button pf-m-control"
    type="button"
    id="textAreaButton1"
  >Button</button>
  <textarea
    class="pf-c-form-control"
    name="textarea1"
    id="textarea1"
    aria-label="Textarea with buttons"
    aria-describedby="textAreaButton1"
  ></textarea>
  <button class="pf-c-button pf-m-control" type="button">Button</button>
</div>
<br />
<div class="pf-c-input-group">
  <textarea
    class="pf-c-form-control"
    name="textarea2"
    id="textarea2"
    aria-label="Textarea with button"
    aria-describedby="textAreaButton2"
  ></textarea>
  <button
    class="pf-c-button pf-m-control"
    type="button"
    id="textAreaButton2"
  >Button</button>
</div>
<br />
<div class="pf-c-input-group">
  <button
    class="pf-c-button pf-m-control"
    type="button"
    id="textAreaButton3"
  >Button</button>
  <button class="pf-c-button pf-m-control" type="button">Button</button>
  <textarea
    class="pf-c-form-control"
    name="textarea3"
    id="textarea3"
    aria-label="Textarea with buttons"
    aria-describedby="textAreaButton3"
  ></textarea>
  <button class="pf-c-button pf-m-control" type="button">Button</button>
</div>
<br />
<div class="pf-c-input-group">
  <div class="pf-c-select" style="width: 100px;">
    <span id="select-example-collapsed1-label" hidden>Choose one</span>

    <button
      class="pf-c-select__toggle"
      type="button"
      id="select-example-collapsed1-toggle"
      aria-haspopup="true"
      aria-expanded="false"
      aria-labelledby="select-example-collapsed1-label select-example-collapsed1-toggle"
    >
      <div class="pf-c-select__toggle-wrapper">
        <span class="pf-c-select__toggle-text">Select</span>
      </div>
      <span class="pf-c-select__toggle-arrow">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </button>

    <ul
      class="pf-c-select__menu"
      role="listbox"
      aria-labelledby="select-example-collapsed1-label"
      hidden
      style="width: 100px;"
    >
      <li role="presentation">
        <button class="pf-c-select__menu-item" role="option">Running</button>
      </li>
      <li role="presentation">
        <button
          class="pf-c-select__menu-item pf-m-selected"
          role="option"
          aria-selected="true"
        >
          Stopped
          <span class="pf-c-select__menu-item-icon">
            <i class="fas fa-check" aria-hidden="true"></i>
          </span>
        </button>
      </li>
      <li role="presentation">
        <button class="pf-c-select__menu-item" role="option">Down</button>
      </li>
      <li role="presentation">
        <button class="pf-c-select__menu-item" role="option">Degraded</button>
      </li>
      <li role="presentation">
        <button class="pf-c-select__menu-item" role="option">Needs maintenance</button>
      </li>
    </ul>
  </div>
  <input
    class="pf-c-form-control"
    type="text"
    id="textInput4"
    name="textInput4"
    aria-label="Input with select and button"
    aria-describedby="inputSelectButton1"
  />
  <button
    class="pf-c-button pf-m-control"
    type="button"
    id="inputSelectButton1"
  >Button</button>
</div>
<br />
<div class="pf-c-input-group">
  <span class="pf-c-input-group__text">
    <i class="fas fa-dollar-sign" aria-hidden="true"></i>
  </span>
  <input
    class="pf-c-form-control"
    type="number"
    id="textInput5"
    name="textInput5"
    aria-label=" Dollar amount input example"
  />
  <span class="pf-c-input-group__text">.00</span>
</div>
<br />
<div class="pf-c-input-group">
  <input
    class="pf-c-form-control"
    type="email"
    id="textInput6"
    name="textInput6"
    aria-label="Email input field"
    aria-describedby="email-example"
  />
  <span class="pf-c-input-group__text" id="email-example">@example.com</span>
</div>
<br />
<div class="pf-c-input-group">
  <span class="pf-c-input-group__text">
    <i class="fas fa-at" aria-hidden="true"></i>
  </span>
  <input
    class="pf-c-form-control"
    required
    type="email"
    id="textInput7"
    name="textInput7"
    aria-invalid="true"
    aria-label="Error state username example"
  />
</div>
<br />
<div class="pf-c-input-group">
  <input
    class="pf-c-form-control"
    type="text"
    id="textInput13"
    name="textInput13"
    aria-label="Input example with popover"
  />
  <button
    class="pf-c-button pf-m-control"
    type="button"
    aria-label="Popover for input"
  >
    <i class="fas fa-question-circle" aria-hidden="true"></i>
  </button>
</div>
<br />
<div class="pf-c-input-group">
  <input
    class="pf-c-form-control"
    type="text"
    id="textInput12"
    name="textInput12"
    aria-label="Input example with popover"
  />
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Popover for input"
  >
    <i class="fas fa-question-circle" aria-hidden="true"></i>
  </button>
</div>
<br />
<div class="pf-c-input-group">
  <input
    class="pf-c-form-control"
    type="number"
    id="textInput14"
    name="textInput14"
    aria-label="Input example with plain unit"
  />
  <span class="pf-c-input-group__text pf-m-plain">%</span>
</div>

```

## Documentation

### Accessibility

When using the `.pf-c-input-group` always ensure labels are used outside the input group with the `.pf-screen-reader` class applied. You can also make use of the `aria-describedby`, `aria-label`, or `aria-labelledby` attributes. For more information on accessibility and forms see the [form component](/components/form).

| Attribute          | Applied to           | Outcome                                                                                                  |
| ------------------ | -------------------- | -------------------------------------------------------------------------------------------------------- |
| `aria-describedby` | `.pf-c-form-control` | When using `.pf-c-input-group__text` or `.pf-c-input-group__action` make use of this on the input field. |

### Usage

| Class                     | Applied to                | Outcome                                                                                      |
| ------------------------- | ------------------------- | -------------------------------------------------------------------------------------------- |
| `.pf-c-input-group`       | `<div>`                   | Initiates the input group. **Required**                                                      |
| `.pf-c-input-group__text` | `<span>`                  | Initiates the input group text. This can be used to show text, radio, icons, or check boxes. |
| `.pf-m-plain`             | `.pf-c-input-group__text` | Removes the border from the text element.                                                    |
