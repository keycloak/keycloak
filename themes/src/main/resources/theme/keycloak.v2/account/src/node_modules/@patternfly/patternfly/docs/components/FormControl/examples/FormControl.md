---
id: Form control
section: components
cssPrefix: pf-c-form-control
---import './FormControl.css'

## Examples

### Input

**Note:** In webkit browsers, inputs with status icons that are autocompleted will have their icons removed by the user agent stylesheet. If the field does not need to use autocomplete, turn it off with `autocomplete="off"` to avoid the problem. Otherwise, use [helper text](/components/helper-text/html-demos)  instead to ensure that the status will remain visible if the field is autocompleted.

```html
<input
  class="pf-c-form-control"
  type="text"
  value="Standard"
  id="input-standard"
  aria-label="Standard input example"
/>
<br />
<br />
<input
  class="pf-c-form-control"
  type="text"
  placeholder="Placeholder"
  id="input-placeholder"
  aria-label="Placeholder input example"
/>
<br />
<br />
<input
  class="pf-c-form-control"
  readonly
  type="text"
  value="Readonly"
  id="input-readonly"
  aria-label="Readonly input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-success"
  type="text"
  value="Success"
  id="input-success"
  aria-label="Success state input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-warning"
  type="text"
  value="Warning"
  id="input-warning"
  aria-label="Warning state input example"
/>
<br />
<br />
<input
  class="pf-c-form-control"
  required
  type="text"
  value="Error"
  id="input-error"
  aria-invalid="true"
  aria-label="Error state input example"
/>
<br />
<br />
<input
  class="pf-c-form-control"
  disabled
  type="text"
  value="Disabled"
  id="input-disabled"
  aria-label="Disabled input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-expanded"
  type="text"
  value="Expanded"
  id="input-expanded"
  aria-label="Expanded input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-icon pf-m-calendar"
  type="text"
  value="Calendar"
  id="input-calendar"
  name="input-calendar"
  aria-label="Calendar input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-icon pf-m-clock"
  type="text"
  value="Clock"
  id="input-clock"
  name="input-clock"
  aria-label="Clock input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-icon"
  type="text"
  value="Custom icon"
  id="input-custom-icon"
  name="custom-icon"
  aria-label="Custom icon input example"
/>

```

### Select

```html
<select
  class="pf-c-form-control pf-m-placeholder"
  id="select-standard"
  name="select-standard"
  aria-label="Standard select example"
>
  <option value selected disabled>Please choose</option>
  <option value="Mr">Mr</option>
  <option value="Miss">Miss</option>
  <option value="Mrs">Mrs</option>
  <option value="Ms">Ms</option>
  <option value="Dr">Dr</option>
  <option value="Other">Other</option>
</select>
<br />
<br />
<select
  class="pf-c-form-control pf-m-placeholder"
  id="select-placeholder-enabled"
  name="select-placeholder-enabled"
  aria-label="Placeholder enabled select example"
>
  <option value selected>Please choose</option>
  <option value="Mr">Mr</option>
  <option value="Miss">Miss</option>
  <option value="Mrs">Mrs</option>
  <option value="Ms">Ms</option>
  <option value="Dr">Dr</option>
  <option value="Other">Other</option>
</select>
<br />
<br />
<select
  class="pf-c-form-control"
  id="select-group"
  name="select-group"
  aria-label="Select group example"
>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2" selected>The second option is selected by default</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
</select>
<br />
<br />
<select
  class="pf-c-form-control pf-m-success"
  id="select-group-success"
  name="select-group-success"
  aria-label="Success state select group example"
>
  <option value>Valid option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
</select>
<br />
<br />
<select
  class="pf-c-form-control pf-m-warning"
  id="select-group-warning"
  name="select-group-warning"
  aria-label="Warning state select group example"
>
  <option value>Warning option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
</select>
<br />
<br />
<select
  class="pf-c-form-control"
  required
  aria-invalid="true"
  id="select-group-error"
  name="select-group-error"
  aria-label="Error state select group example"
>
  <option value>Invalid option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
</select>
<br />
<br />

```

### Textarea

```html
<textarea
  class="pf-c-form-control"
  name="textarea-standard"
  id="textarea-standard"
  aria-label="Standard textarea example"
>Standard
</textarea>
<br />
<br />
<textarea
  class="pf-c-form-control"
  readonly
  name="textarea-readonly"
  id="textarea-readonly"
  aria-label="Readonly textarea example"
>Readonly
</textarea>
<br />
<br />
<textarea
  class="pf-c-form-control pf-m-success"
  name="textarea-success"
  id="textarea-success"
  aria-label="Success state textarea example"
>Success
</textarea>
<br />
<br />
<textarea
  class="pf-c-form-control pf-m-warning"
  name="textarea-warning"
  id="textarea-warning"
  aria-label="Warning state textarea example"
>Warning
</textarea>
<br />
<br />
<textarea
  class="pf-c-form-control"
  required
  name="textarea-error"
  id="textarea-error"
  aria-label="Error state textarea example"
  aria-invalid="true"
>Error
</textarea>
<br />
<br />
<textarea
  class="pf-c-form-control pf-m-resize-vertical"
  name="textarea-resize-vertical"
  id="textarea-resize-vertical"
  aria-label="Resize vertical textarea example"
>Resizes vertically
</textarea>
<br />
<br />
<textarea
  class="pf-c-form-control pf-m-resize-horizontal"
  name="textarea-resize-horizontal"
  id="textarea-resize-horizontal"
  aria-label="Resize horizontal textarea example"
>Resizes horizontally
</textarea>

```

### Icon sprite

**Note:** The icons for the success, invalid, calendar, etc varations in form control elemements are applied as background images to the form element. By default, the image URLs for these icons are data URIs. However, there may be cases where data URIs are not ideal, such as in an application with a content security policy that disallows data URIs for security reasons. The `.pf-m-icon-sprite` variation changes the icon source to an external SVG file that serves as a sprite for all of the supported icons.

```html isBeta
<input
  class="pf-c-form-control pf-m-success pf-m-icon-sprite"
  type="text"
  value="Success"
  id="input-success"
  aria-label="Success state input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-warning pf-m-icon-sprite"
  type="text"
  value="Warning"
  id="input-warning"
  aria-label="Warning state input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-icon-sprite"
  required
  type="text"
  value="Error"
  id="input-error"
  aria-invalid="true"
  aria-label="Error state input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-search pf-m-icon-sprite"
  type="search"
  value="Search"
  id="input-search"
  name="search-input"
  aria-label="Search input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-icon pf-m-calendar pf-m-icon-sprite"
  type="text"
  value="Calendar"
  id="input-calendar"
  name="input-calendar"
  aria-label="Calendar input example"
/>
<br />
<br />
<input
  class="pf-c-form-control pf-m-icon pf-m-clock pf-m-icon-sprite"
  type="text"
  value="Clock"
  id="input-clock"
  name="input-clock"
  aria-label="Clock input example"
/>
<br />
<br />
<select
  class="pf-c-form-control pf-m-success pf-m-icon-sprite"
  id="select-group-success"
  name="select-group-success"
  aria-label="Success state select group example"
>
  <option value>Valid option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
</select>
<br />
<br />
<select
  class="pf-c-form-control pf-m-warning pf-m-icon-sprite"
  id="select-group-warning"
  name="select-group-warning"
  aria-label="Warning state select group example"
>
  <option value>Warning option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
</select>
<br />
<br />
<select
  class="pf-c-form-control pf-m-icon-sprite"
  required
  aria-invalid="true"
  id="select-group-error"
  name="select-group-error"
  aria-label="Error state select group example"
>
  <option value>Invalid option</option>
  <optgroup label="Group 1">
    <option value="Option 1">The first option</option>
    <option value="Option 2">The second option</option>
  </optgroup>
  <optgroup label="Group 2">
    <option value="Option 3">The third option</option>
    <option value="Option 4">The fourth option</option>
  </optgroup>
</select>
<br />
<br />
<textarea
  class="pf-c-form-control pf-m-success pf-m-icon-sprite"
  name="textarea-success"
  id="textarea-success"
  aria-label="Success state textarea example"
>Success
</textarea>
<br />
<br />
<textarea
  class="pf-c-form-control pf-m-warning pf-m-icon-sprite"
  name="textarea-warning"
  id="textarea-warning"
  aria-label="Warning state textarea example"
>Warning
</textarea>
<br />
<br />
<textarea
  class="pf-c-form-control pf-m-icon-sprite"
  required
  name="textarea-error"
  id="textarea-error"
  aria-label="Error state textarea example"
  aria-invalid="true"
>Error
</textarea>

```

## Documentation

### Accessibility

| Attribute                       | Applied to                         | Outcome                                                                                                                                                          |
| ------------------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `id`                            | `.pf-c-form-control`               | Provides an `id` value that can be used with the `for` attribute on an associated `<label>` element to provide an accessible label for the form control element. |
| `aria-invalid="true"`           | `.pf-c-form-control`               | Indicates that the form control is in the error state and applies error state styling.                                                                           |
| `aria-label="descriptive text"` | `.pf-c-form-control`               | Provides an accessible label for assistive technology.                                                                                                           |
| `aria-expanded="true"`          | `.pf-c-form-control.pf-m-expanded` | Indicates that clicking in the form control has toggled something else to be expanded.                                                                           |

### Usage

| Class                     | Applied to                         | Outcome                                                                                                                                                                                     |
| ------------------------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-form-control`      | `<input>`,`<textarea>`, `<select>` | Initiates an input, textarea or select. For styling of checkboxes or radios see the [checkbox component](/components/checkbox) or [radio component](/components/radio). **Required**        |
| `.pf-m-resize-vertical`   | `textarea.pf-m-form-control`       | Modifies a `textarea.pf-c-form-control` element so it can only be resized vertically along the y-axis.                                                                                      |
| `.pf-m-resize-horizontal` | `textarea.pf-m-form-control`       | Modifies a `textarea.pf-c-form-control` element so it can only be resized horizontally along the x-axis.                                                                                    |
| `.pf-m-success`           | `.pf-c-form-control`               | Modifies a form control for the success state.                                                                                                                                              |
| `.pf-m-warning`           | `.pf-c-form-control`               | Modifies a form control for the warning state.                                                                                                                                              |
| `.pf-m-icon-sprite`       | `.pf-c-form-control`               | Modifies form control element to use an external SVG sprite instead of embedded data URIs for icons. For use with apps whose content security policies disallow the use of data URIs.       |
| `.pf-m-icon`              | `input.pf-c-form-control`          | Modifies a form control text input to be able to specify a custom SVG background via `--pf-c-form-control--m-icon--BackgroundUrl`, and other optional vars for other background properties. |
| `.pf-m-calendar`          | `.pf-c-form-control.pf-m-icon`     | Modifies a form control to support the calendar icon.                                                                                                                                       |
| `.pf-m-clock`             | `.pf-c-form-control.pf-m-icon`     | Modifies a form control to support the clock icon.                                                                                                                                          |
| `.pf-m-expanded`          | `input.pf-c-form-control`          | Modifies a form control for the expanded state. This is used when clicking in the text input toggles something open/closed.                                                                 |
| `.pf-m-placeholder`       | `select.pf-c-form-control`         | Modifies a form select for placeholder styles. This modifier is set programatically based on the chosen option.                                                                             |
