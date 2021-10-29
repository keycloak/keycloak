---
id: Number input
section: components
cssPrefix: pf-c-number-input
---## Examples

### Default

```html
<div class="pf-c-number-input">
  <div class="pf-c-input-group">
    <button class="pf-c-button pf-m-control" type="button" aria-label="Minus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="90"
      name="number-input-default-name"
      aria-label="Number input"
    />
    <button class="pf-c-button pf-m-control" type="button" aria-label="Plus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>

```

### With unit

```html
<div class="pf-c-number-input">
  <div class="pf-c-input-group">
    <button class="pf-c-button pf-m-control" type="button" aria-label="Minus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="90"
      name="number-input-unit-name"
      aria-label="Number input"
    />
    <button class="pf-c-button pf-m-control" type="button" aria-label="Plus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <span class="pf-c-number-input__unit">%</span>
</div>
<br />
<br />
<div class="pf-c-number-input">
  <span class="pf-c-number-input__unit">$</span>

  <div class="pf-c-input-group">
    <button class="pf-c-button pf-m-control" type="button" aria-label="Minus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="1.00"
      name="number-input-unit2-name"
      aria-label="Number input"
    />
    <button class="pf-c-button pf-m-control" type="button" aria-label="Plus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>

```

### With unit and lower threshold reached

```html
<div class="pf-c-number-input">
  <div class="pf-c-input-group">
    <button
      class="pf-c-button pf-m-control"
      type="button"
      aria-label="Minus"
      disabled
    >
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="0"
      min="0"
      name="number-input-unit-lower-threshold-name"
      aria-label="Number input"
    />
    <button class="pf-c-button pf-m-control" type="button" aria-label="Plus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <span class="pf-c-number-input__unit">%</span>
</div>

```

### With unit and upper threshold reached

```html
<div class="pf-c-number-input">
  <div class="pf-c-input-group">
    <button class="pf-c-button pf-m-control" type="button" aria-label="Minus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="100"
      max="100"
      name="number-input-unit-upper-threshold-name"
      aria-label="Number input"
    />
    <button
      class="pf-c-button pf-m-control"
      type="button"
      aria-label="Plus"
      disabled
    >
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <span class="pf-c-number-input__unit">%</span>
</div>

```

### Disabled

```html
<div class="pf-c-number-input">
  <div class="pf-c-input-group">
    <button
      class="pf-c-button pf-m-control"
      type="button"
      aria-label="Minus"
      disabled
    >
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="100"
      name="number-input-disabled-name"
      aria-label="Number input"
      disabled
    />
    <button
      class="pf-c-button pf-m-control"
      type="button"
      aria-label="Plus"
      disabled
    >
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <span class="pf-c-number-input__unit">%</span>
</div>

```

### Varying sizes

```html
<div
  class="pf-c-number-input"
  style="--pf-c-number-input--c-form-control--width-chars: 1;"
>
  <div class="pf-c-input-group">
    <button class="pf-c-button pf-m-control" type="button" aria-label="Minus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="1"
      name="number-input-sizes-name"
      aria-label="Number input"
    />
    <button class="pf-c-button pf-m-control" type="button" aria-label="Plus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>
<br />
<br />
<div
  class="pf-c-number-input"
  style="--pf-c-number-input--c-form-control--width-chars: 10;"
>
  <div class="pf-c-input-group">
    <button class="pf-c-button pf-m-control" type="button" aria-label="Minus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="1234567890"
      name="number-input-sizes2-name"
      aria-label="Number input"
    />
    <button class="pf-c-button pf-m-control" type="button" aria-label="Plus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>
<br />
<br />
<div
  class="pf-c-number-input"
  style="--pf-c-number-input--c-form-control--width-chars: 5;"
>
  <div class="pf-c-input-group">
    <button class="pf-c-button pf-m-control" type="button" aria-label="Minus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="5"
      name="number-input-sizes3-name"
      aria-label="Number input"
    />
    <button class="pf-c-button pf-m-control" type="button" aria-label="Plus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>
<br />
<br />
<div
  class="pf-c-number-input"
  style="--pf-c-number-input--c-form-control--width-chars: 5;"
>
  <div class="pf-c-input-group">
    <button class="pf-c-button pf-m-control" type="button" aria-label="Minus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-minus" aria-hidden="true"></i>
      </span>
    </button>
    <input
      class="pf-c-form-control"
      type="number"
      value="12345"
      name="number-input-sizes4-name"
      aria-label="Number input"
    />
    <button class="pf-c-button pf-m-control" type="button" aria-label="Plus">
      <span class="pf-c-number-input__icon">
        <i class="fas fa-plus" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>

```

## Documentation

### Accessibility

| Attribute                                 | Applied to                               | Outcome                                                                    |
| ----------------------------------------- | ---------------------------------------- | -------------------------------------------------------------------------- |
| `aria-label="Plus"`, `aria-label="Minus"` | `.pf-c-button.pf-m-control`              | Provides an accessible name for the outer plus/minus buttons. **Required** |
| `min`                                     | `input[type="number"].pf-c-form-control` | Provides an optional minimum value for the input.                          |
| `max`                                     | `input[type="number"].pf-c-form-control` | Provides an optional maximum value for the input.                          |

### Usage

| Class                                              | Applied              | Outcome                                                  |
| -------------------------------------------------- | -------------------- | -------------------------------------------------------- |
| `.pf-c-number-input`                               | `<div>`              | Initiates the number input component.                    |
| `.pf-c-number-input__icon`                         | `<span>`             | Initiates the number input icon.                         |
| `.pf-c-number-input__unit`                         | `<span>`             | Initiates the number input unit.                         |
| `--pf-c-number-input--c-form-control--width-chars` | `.pf-c-number-input` | Specifies the number of characters to show in the input. |
