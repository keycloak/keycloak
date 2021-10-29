---
id: 'Slider'
beta: true
section: components
cssPrefix: pf-c-slider
---## Examples

### Discrete

```html
<div class="pf-c-slider" style="--pf-c-slider--value: 62.5%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div class="pf-c-slider__steps" aria-hidden="true">
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 0%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">0</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 12.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 25%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">2</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 37.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 50%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">4</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 62.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 75%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">6</div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 87.5%;">
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 100%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">8</div>
      </div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="8"
      aria-valuenow="5"
      aria-label="Value"
      tabindex="0"
    ></div>
  </div>
</div>

```

### Continuous

```html
<div class="pf-c-slider" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      tabindex="0"
    ></div>
  </div>
</div>

<div class="pf-c-slider" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div class="pf-c-slider__steps" aria-hidden="true">
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 0%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">0%</div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 100%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">100%</div>
      </div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      tabindex="0"
    ></div>
  </div>
</div>

```

### Value input

```html
<div
  class="pf-c-slider"
  style="--pf-c-slider--value: 62.5%; --pf-c-slider__value--c-form-control--width-chars: 1;"
>
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div class="pf-c-slider__steps" aria-hidden="true">
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 0%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">0</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 12.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 25%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">2</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 37.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 50%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">4</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 62.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 75%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">6</div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 87.5%;">
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 100%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">8</div>
      </div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="8"
      aria-valuenow="5"
      aria-label="Value"
      tabindex="0"
    ></div>
  </div>
  <div class="pf-c-slider__value">
    <input
      class="pf-c-form-control"
      type="number"
      value="5"
      aria-label="Slider value input"
    />
  </div>
</div>

<br />

<div class="pf-c-slider" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div class="pf-c-slider__steps" aria-hidden="true">
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 0%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">0%</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 25%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 50%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">50%</div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 75%;">
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 100%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">100%</div>
      </div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      tabindex="0"
    ></div>
  </div>
  <div class="pf-c-slider__value">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        type="number"
        value="50"
        aria-label="Slider value input"
      />
      <span class="pf-c-input-group__text pf-m-plain">%</span>
    </div>
  </div>
</div>

<br />

<div class="pf-c-slider" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      tabindex="0"
    ></div>
  </div>
  <div class="pf-c-slider__value">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        type="number"
        value="50"
        aria-label="Slider value input"
      />
      <span class="pf-c-input-group__text pf-m-plain">%</span>
    </div>
  </div>
</div>

```

### Thumb value input

```html
<div class="pf-c-slider" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      tabindex="0"
    ></div>
    <div class="pf-c-slider__value pf-m-floating">
      <div class="pf-c-input-group">
        <input
          class="pf-c-form-control"
          type="number"
          value="50"
          aria-label="Slider value input"
        />
        <span class="pf-c-input-group__text pf-m-plain">%</span>
      </div>
    </div>
  </div>
</div>

```

### Actions

```html
<div class="pf-c-slider" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__actions">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Minus">
      <i class="fas fa-fw fa-minus" aria-hidden="true"></i>
    </button>
  </div>
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      tabindex="0"
    ></div>
  </div>
  <div class="pf-c-slider__actions">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Plus">
      <i class="fas fa-fw fa-plus" aria-hidden="true"></i>
    </button>
  </div>
</div>

<br />
<br />

<div class="pf-c-slider" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      tabindex="0"
    ></div>
    <div class="pf-c-slider__value pf-m-floating">
      <div class="pf-c-input-group">
        <input
          class="pf-c-form-control"
          disabled
          type="number"
          value="50"
          aria-label="Slider value input"
        />
        <span class="pf-c-input-group__text pf-m-plain">%</span>
      </div>
    </div>
  </div>
  <div class="pf-c-slider__actions">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Locked">
      <i class="fas fa-fw fa-lock" aria-hidden="true"></i>
    </button>
  </div>
</div>

<br />
<br />

<div class="pf-c-slider" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      tabindex="0"
    ></div>
    <div class="pf-c-slider__value pf-m-floating">
      <div class="pf-c-input-group">
        <input
          class="pf-c-form-control"
          type="number"
          value="50"
          aria-label="Slider value input"
        />
        <span class="pf-c-input-group__text pf-m-plain">%</span>
      </div>
    </div>
  </div>
  <div class="pf-c-slider__actions">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Lock">
      <i class="fas fa-fw fa-lock-open" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Disabled

```html
<div class="pf-c-slider pf-m-disabled" style="--pf-c-slider--value: 62.5%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div class="pf-c-slider__steps" aria-hidden="true">
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 0%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">0</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 12.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 25%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">2</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 37.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 50%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">4</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 62.5%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 75%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">6</div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 87.5%;">
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 100%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">8</div>
      </div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="8"
      aria-valuenow="5"
      aria-label="Value"
      aria-disabled="true"
    ></div>
  </div>
</div>

<br />
<br />

<div class="pf-c-slider pf-m-disabled" style="--pf-c-slider--value: 50%;">
  <div class="pf-c-slider__main">
    <div class="pf-c-slider__rail">
      <div class="pf-c-slider__rail-track"></div>
    </div>
    <div class="pf-c-slider__steps" aria-hidden="true">
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 0%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">0%</div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 25%;"
      >
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div
        class="pf-c-slider__step pf-m-active"
        style="--pf-c-slider__step--Left: 50%;"
      >
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">50%</div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 75%;">
        <div class="pf-c-slider__step-tick"></div>
      </div>
      <div class="pf-c-slider__step" style="--pf-c-slider__step--Left: 100%;">
        <div class="pf-c-slider__step-tick"></div>
        <div class="pf-c-slider__step-label">100%</div>
      </div>
    </div>
    <div
      class="pf-c-slider__thumb"
      role="slider"
      aria-valuemin="0"
      aria-valuemax="100"
      aria-valuenow="50"
      aria-label="Value"
      aria-disabled="true"
    ></div>
  </div>
  <div class="pf-c-slider__value">
    <div class="pf-c-input-group">
      <input
        class="pf-c-form-control"
        disabled
        type="number"
        value="50"
        aria-label="Slider value input"
      />
      <span class="pf-c-input-group__text pf-m-plain">%</span>
    </div>
  </div>
</div>

```

## Documentation

### Accessibility

| Attribute                 | Applied to                                       | Outcome                                                                                                           |
| ------------------------- | ------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------- |
| `role="slider"`           | `.pf-c-slider__thumb`                            | Identifies the element as a slider. **Required**                                                                  |
| `tabindex="0"`            | `.pf-c-slider__thumb`                            | Includes the slider thumb in the page tab sequence. **Note:** only for use with non-disabled slider. **Required** |
| `aria-disabled="true"`    | `.pf-c-slider.pf-m-disabled .pf-c-slider__thumb` | Indicates that the slider thumb is disabled. **Required**                                                         |
| `aria-valuemin="[value]"` | `.pf-c-slider__thumb`                            | Specifies the minimum value of the slider. **Required**                                                           |
| `aria-valuemax="[value]"` | `.pf-c-slider__thumb`                            | Specifies the maximum value of the slider. **Required**                                                           |
| `aria-valuenow="[value]"` | `.pf-c-slider__thumb`                            | Specifies the current value of the slider. **Required**                                                           |

### Usage

| Class                      | Applied to            | Outcome                                                                    |
| -------------------------- | --------------------- | -------------------------------------------------------------------------- |
| `.pf-c-slider`             | `<div>`               | Initiates the slider component. **Required**                               |
| `.pf-c-slider__main`       | `<div>`               | Initiates the slider main element. **Required**                            |
| `.pf-c-slider__rail`       | `<div>`               | Initiates the slider rail. **Required**                                    |
| `.pf-c-slider__rail-track` | `<div>`               | Initiates the slider rail track. **Required**                              |
| `.pf-c-slider__steps`      | `<div>`               | Initiates the slider steps.                                                |
| `.pf-c-slider__step`       | `<div>`               | Initiates a slider step.                                                   |
| `.pf-c-slider__step-tick`  | `<div>`               | Initiates a slider step tick.                                              |
| `.pf-c-slider__step-label` | `<div>`               | Initiates a slider step label.                                             |
| `.pf-c-slider__thumb`      | `<div>`               | Initiates the slider thumb. **Required**                                   |
| `.pf-c-slider__value`      | `<div>`               | Initiates the slider value.                                                |
| `.pf-c-slider__actions`    | `<div>`               | Initiates the slider actions.                                              |
| `.pf-m-disabled`           | `.pf-c-slider`        | Modifies the slider for the disabled state.                                |
| `.pf-m-floating`           | `.pf-c-slider__thumb` | Modifies the slider value to float above the thumb.                        |
| `--pf-c-slider--value`     | `.pf-c-slider`        | Applies appropriate slider styles based on the current value. **Required** |
