---
id: 'Date picker'
beta: true
section: components
cssPrefix: pf-c-date-picker
---import './DatePicker.css'

## Examples

### Basic

```html
<div class="pf-c-date-picker">
  <div class="pf-c-date-picker__input">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        type="text"
        value="2020-03-05"
        id="basic-input"
        name="basic-input"
        aria-label="Date picker"
      />
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Toggle date picker"
      >
        <i class="fas fa-calendar-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
</div>

```

### Helper text

```html
<div class="pf-c-date-picker">
  <div class="pf-c-date-picker__input">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        type="text"
        value="2020-03-05"
        id="helper-text-input"
        name="helper-text-input"
        aria-label="Date picker"
      />
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Toggle date picker"
      >
        <i class="fas fa-calendar-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-date-picker__helper-text">Select a date.</div>
</div>

```

### Invalid

```html
<div class="pf-c-date-picker">
  <div class="pf-c-date-picker__input">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        aria-invalid="true"
        type="text"
        value="2020-03-05"
        id="invalid-input"
        name="invalid-input"
        aria-label="Date picker"
      />
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Toggle date picker"
      >
        <i class="fas fa-calendar-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-date-picker__helper-text pf-m-error">Invalid date</div>
</div>

```

### Expanded

```html
<div class="pf-c-date-picker">
  <div class="pf-c-date-picker__input">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        type="text"
        value="2020-03-05"
        id="expanded-input"
        name="expanded-input"
        aria-label="Date picker"
      />
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Toggle date picker"
      >
        <i class="fas fa-calendar-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-date-picker__calendar">Calendar</div>
</div>

```

### Custom width input

```html
<div
  class="pf-c-date-picker"
  style="--pf-c-date-picker__input--c-form-control--Width: 220px;"
>
  <div class="pf-c-date-picker__input">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        type="text"
        value="November 20, 2020"
        id="basic-input"
        name="basic-input"
        aria-label="Date picker"
      />
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Toggle date picker"
      >
        <i class="fas fa-calendar-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
</div>

```

### Custom width input based on number of characters

```html
<div
  class="pf-c-date-picker"
  style="--pf-c-date-picker__input--c-form-control--width-chars: 17;"
>
  <div class="pf-c-date-picker__input">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        type="text"
        value="November 20, 2020"
        id="basic-input"
        name="basic-input"
        aria-label="Date picker"
      />
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Toggle date picker"
      >
        <i class="fas fa-calendar-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
</div>

```

## Documentation

### Usage

| Class                            | Applied to                       | Outcome                                                                                                      |
| -------------------------------- | -------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `.pf-c-date-picker`              | `<div>`                          | Initiates the date picker component. **Required**                                                            |
| `.pf-c-date-picker__input`       | `<div>`                          | Initiates the date picker input container. **Required**                                                      |
| `.pf-c-date-picker__helper-text` | `<div>`                          | Initiates the date picker helper text.                                                                       |
| `.pf-c-date-picker__calendar`    | `<div>`                          | Initiates an optional date picker calendar container. **Note:** Required in the react date picker component. |
| `.pf-m-top`                      | `.pf-c-date-picker`              | Modifies to display the calendar above the date picker.                                                      |
| `.pf-m-error`                    | `.pf-c-date-picker__helper-text` | Modifies the helper text for the invalid/error state.                                                        |
| `.pf-m-align-right`              | `.pf-c-date-picker__calendar`    | Modifies the calendar to align the calendar to the right edge of the date picker.                            |
