---
id: Progress
section: components
cssPrefix: pf-c-progress
---## Examples

### Simple

```html
<div class="pf-c-progress" id="progress-simple-example">
  <div
    class="pf-c-progress__description"
    id="progress-simple-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">33%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-simple-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Small

```html
<div class="pf-c-progress pf-m-sm" id="progress-sm-example">
  <div
    class="pf-c-progress__description"
    id="progress-sm-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">33%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-sm-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Large

```html
<div class="pf-c-progress pf-m-lg" id="progress-lg-example">
  <div
    class="pf-c-progress__description"
    id="progress-lg-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">33%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-lg-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Outside

```html
<div class="pf-c-progress pf-m-outside pf-m-lg" id="progress-outside-example">
  <div
    class="pf-c-progress__description"
    id="progress-outside-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">33%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-outside-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Inside

```html
<div class="pf-c-progress pf-m-lg pf-m-inside" id="progress-inside-example">
  <div
    class="pf-c-progress__description"
    id="progress-inside-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true"></div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-inside-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;">
      <span class="pf-c-progress__measure">33%</span>
    </div>
  </div>
</div>

```

### Success

```html
<div class="pf-c-progress pf-m-success" id="progress-success-example">
  <div
    class="pf-c-progress__description"
    id="progress-success-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">100%</span>
    <span class="pf-c-progress__status-icon">
      <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
    </span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="100"
    aria-labelledby="progress-success-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:100%;"></div>
  </div>
</div>

```

### Warning

```html
<div class="pf-c-progress pf-m-warning" id="progress-warning-example">
  <div
    class="pf-c-progress__description"
    id="progress-warning-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">100%</span>
    <span class="pf-c-progress__status-icon">
      <i class="fas fa-fw fa-exclamation-triangle" aria-hidden="true"></i>
    </span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="100"
    aria-labelledby="progress-warning-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:100%;"></div>
  </div>
</div>

```

### Failure

```html
<div class="pf-c-progress pf-m-danger" id="progress-failure-example">
  <div
    class="pf-c-progress__description"
    id="progress-failure-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">33%</span>
    <span class="pf-c-progress__status-icon">
      <i class="fas fa-fw fa-times-circle" aria-hidden="true"></i>
    </span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-failure-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Inside success

```html
<div
  class="pf-c-progress pf-m-lg pf-m-inside pf-m-success"
  id="progress-inside-success-example"
>
  <div
    class="pf-c-progress__description"
    id="progress-inside-success-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__status-icon">
      <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
    </span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="100"
    aria-labelledby="progress-inside-success-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:100%;">
      <span class="pf-c-progress__measure">100%</span>
    </div>
  </div>
</div>

```

### Inside warning

```html
<div
  class="pf-c-progress pf-m-lg pf-m-inside pf-m-warning"
  id="progress-inside-warning-example"
>
  <div
    class="pf-c-progress__description"
    id="progress-inside-warning-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__status-icon">
      <i class="fas fa-fw fa-exclamation-triangle" aria-hidden="true"></i>
    </span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="100"
    aria-labelledby="progress-inside-warning-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:100%;">
      <span class="pf-c-progress__measure">100%</span>
    </div>
  </div>
</div>

```

### Outside failure

```html
<div
  class="pf-c-progress pf-m-outside pf-m-lg pf-m-danger"
  id="progress-outside-failure-example"
>
  <div
    class="pf-c-progress__description"
    id="progress-outside-failure-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">33%</span>
    <span class="pf-c-progress__status-icon">
      <i class="fas fa-fw fa-times-circle" aria-hidden="true"></i>
    </span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-outside-failure-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Outside static width measure

```html
<div
  class="pf-c-progress pf-m-outside pf-m-lg"
  id="progress-outside-static-width-example"
>
  <div
    class="pf-c-progress__description"
    id="progress-outside-static-width-example-description"
  ></div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure pf-m-static-width">1%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="1"
    aria-label="Progress status"
  >
    <div class="pf-c-progress__indicator" style="width:1%;"></div>
  </div>
</div>
<br />

<div
  class="pf-c-progress pf-m-outside pf-m-lg"
  id="progress-outside-static-width-2-example"
>
  <div
    class="pf-c-progress__description"
    id="progress-outside-static-width-2-example-description"
  ></div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure pf-m-static-width">50%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="50"
    aria-label="Progress status"
  >
    <div class="pf-c-progress__indicator" style="width:50%;"></div>
  </div>
</div>
<br />

<div
  class="pf-c-progress pf-m-outside pf-m-lg"
  id="progress-outside-static-width-3-example"
>
  <div
    class="pf-c-progress__description"
    id="progress-outside-static-width-3-example-description"
  ></div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure pf-m-static-width">100%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="100"
    aria-label="Progress status"
  >
    <div class="pf-c-progress__indicator" style="width:100%;"></div>
  </div>
</div>
<br />
<br />

<div
  class="pf-c-progress pf-m-outside pf-m-lg"
  id="progress-outside-static-width-4-example"
  style="--pf-c-progress__measure--m-static-width--MinWidth: 6ch;"
>
  <div
    class="pf-c-progress__description"
    id="progress-outside-static-width-4-example-description"
  ></div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure pf-m-static-width">1,000</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100000"
    aria-valuenow="1000"
    aria-label="Progress status"
  >
    <div class="pf-c-progress__indicator" style="width:1%;"></div>
  </div>
</div>
<br />

<div
  class="pf-c-progress pf-m-outside pf-m-lg"
  id="progress-outside-static-width-5-example"
  style="--pf-c-progress__measure--m-static-width--MinWidth: 6ch;"
>
  <div
    class="pf-c-progress__description"
    id="progress-outside-static-width-5-example-description"
  ></div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure pf-m-static-width">50,000</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100000"
    aria-valuenow="50000"
    aria-label="Progress status"
  >
    <div class="pf-c-progress__indicator" style="width:50%;"></div>
  </div>
</div>
<br />

<div
  class="pf-c-progress pf-m-outside pf-m-lg"
  id="progress-outside-static-width-6-example"
  style="--pf-c-progress__measure--m-static-width--MinWidth: 6ch;"
>
  <div
    class="pf-c-progress__description"
    id="progress-outside-static-width-6-example-description"
  ></div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure pf-m-static-width">100,000</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100000"
    aria-valuenow="100000"
    aria-label="Progress status"
  >
    <div class="pf-c-progress__indicator" style="width:100%;"></div>
  </div>
</div>

```

### On single line

```html
<div class="pf-c-progress pf-m-singleline" id="progress-singleline-example">
  <div
    class="pf-c-progress__description"
    id="progress-singleline-example-description"
  ></div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">33%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-label="Progress status"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Without measure

```html
<div class="pf-c-progress" id="progress-no-measure-example">
  <div
    class="pf-c-progress__description"
    id="progress-no-measure-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true"></div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-no-measure-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Failure without measure

```html
<div class="pf-c-progress pf-m-danger" id="progress-no-measure-failure-example">
  <div
    class="pf-c-progress__description"
    id="progress-no-measure-failure-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__status-icon">
      <i class="fas fa-fw fa-times-circle" aria-hidden="true"></i>
    </span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-no-measure-failure-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Finite step

```html
<div class="pf-c-progress" id="progress-finite-step-example">
  <div
    class="pf-c-progress__description"
    id="progress-finite-step-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">2 of 5 units</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="5"
    aria-valuenow="2"
    aria-valuetext="2 of 5 units"
    aria-labelledby="progress-finite-step-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:40%;"></div>
  </div>
</div>

```

### Truncate description

```html
<div class="pf-c-progress" id="progress-truncate-description-example">
  <div
    class="pf-c-progress__description pf-m-truncate"
    id="progress-truncate-description-example-description"
  >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean quis ultricies lectus, eu lobortis mauris. Morbi pretium arcu id rhoncus mollis. Donec accumsan tincidunt enim nec varius. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Suspendisse potenti.</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">33%</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="100"
    aria-valuenow="33"
    aria-labelledby="progress-truncate-description-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:33%;"></div>
  </div>
</div>

```

### Non-percantage progress

If the status that displays with the bar is not a percentage, then the ARIA tag `aria-valuetext` should be used to provide this status to screen reader users. This is the only case when setting the `aria-valuemax` to a value other than "100" is recommended, given how different screen readers handle these attributes.

### Progress step instruction

```html
<div class="pf-c-progress" id="progress-step-instruction-example">
  <div
    class="pf-c-progress__description"
    id="progress-step-instruction-example-description"
  >Title</div>
  <div class="pf-c-progress__status" aria-hidden="true">
    <span class="pf-c-progress__measure">Step 2: Copying files</span>
  </div>
  <div
    class="pf-c-progress__bar"
    role="progressbar"
    aria-valuemin="0"
    aria-valuemax="5"
    aria-valuenow="2"
    aria-valuetext="Step 2: Copying files"
    aria-labelledby="progress-step-instruction-example-description"
  >
    <div class="pf-c-progress__indicator" style="width:40%;"></div>
  </div>
</div>

```

## Documentation

### Overview

### Accessibility

If this component is describing the loading progress of a particular region of a page, the author should use `aria-describedby` to point to the status, and set the `aria-busy` attribute to `true` on the region until it is finished loading.

| Attribute                                                        | Applied to               | Outcome                                                                                                                                                                                                                                                                                                                                                                    |
| ---------------------------------------------------------------- | ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="progressbar"`                                             | `.pf-c-progress__bar`    | This role is used for an element that displays the progress status for a task that takes a long time or consists of several steps.                                                                                                                                                                                                                                         |
| `aria-valuenow=""`                                               | `.pf-c-progress__bar`    | This value needs to be updated as progress continues.                                                                                                                                                                                                                                                                                                                      |
| `aria-valuemin="0"`                                              | `.pf-c-progress__bar`    | The minimum value for the progress bar.                                                                                                                                                                                                                                                                                                                                    |
| `aria-valuemax="100"`                                            | `.pf-c-progress__bar`    | The maximum value for the progress bar. If the progress is only defined using `aria-valuenow` (e.g a percentage), the value should be set to "100". If the progress is defined using `aria-valuetext`, then this value can be a number other than 100. For example, if `aria-valuetext` is "2 of 5 units", then `aria-valuemax` can be "5" and `aria-valuenow` can be "2". |
| `aria-label="[id of .pf-c-progress__description]"`               | `.pf-c-progress__bar`    | Provides an accessible name for the progress component.                                                                                                                                                                                                                                                                                                                    |
| `aria-labelledby="[id element that labels the progress]"`        | `.pf-c-progress__bar`    | Provides an accessible name for the progress component.                                                                                                                                                                                                                                                                                                                    |
| `aria-describedby="[id of element that describes the progress]"` | `.pf-c-progress__bar`    | Provides an accessible description for the progress component.                                                                                                                                                                                                                                                                                                             |
| `aria-valuetext="[loading state]"`                               | `.pf-c-progress__bar`    | Provide a text string that communicates current status. Only use if the important information about status is included in the text string. Do not use if percentage is the most important value to communicate. Some screen readers will ignore the percentage value determined from `aria-valuenow` when `aria-valuetext` is used.                                        |
| `aria-hidden="true"`                                             | `.pf-c-progress__status` | Hides the visible progress bar status from screen readers. This information is communicated by the aria attributes defined on the `.pf-c-progress__bar` element.                                                                                                                                                                                                           |

### Usage

| Class                         | Applied to                                            | Outcome                                                                                                                                                       |
| ----------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-progress`              | `<div>`                                               | Initiates a progress component.                                                                                                                               |
| `.pf-c-progress__description` | `<div>`                                               | The description for a progress bar.                                                                                                                           |
| `.pf-c-progress__status`      | `<div>`                                               | Displays the % of progress and status icons.                                                                                                                  |
| `.pf-c-progress__measure`     | `<span>`                                              | Displays the % complete.                                                                                                                                      |
| `.pf-c-progress__status-icon` | `<span>`                                              | Displays the status icon. (optional)                                                                                                                          |
| `.pf-c-progress__bar`         | `<div>`                                               | Displays across the entire width and represents the completed state.                                                                                          |
| `.pf-c-progress__indicator`   | `<div>`                                               | Displays with the `.pf-c-progress__bar` to indicate the progress so far.                                                                                      |
| `.pf-m-lg`                    | `.pf-c-progress`                                      | Modifies the progress bar to be larger.                                                                                                                       |
| `.pf-m-sm`                    | `.pf-c-progress`                                      | Modifies the progress bar to be smaller.                                                                                                                      |
| `.pf-m-inside`                | `.pf-c-progress`                                      | Shows the measure within the progress indicator. NOTE: This option requires `.pf-m-lg`.                                                                       |
| `.pf-m-outside`               | `.pf-c-progress`                                      | Shows the measure and status icon to the right of the progress bar.                                                                                           |
| `.pf-m-singleline`            | `.pf-c-progress`                                      | Modifies the progress component to exist on one row. If a measure is needed, use with `.pf-m-inside` or `.pf-m-outside`                                       |
| `.pf-m-success`               | `.pf-c-progress`                                      | Changes the appearance of the progess component to indicate a success state.                                                                                  |
| `.pf-m-warning`               | `.pf-c-progress`                                      | Changes the appearance of the progess component to indicate a warning state.                                                                                  |
| `.pf-m-danger`                | `.pf-c-progress`                                      | Changes the appearance of the progess component to indicate a danger (failure) state.                                                                         |
| `.pf-m-truncate`              | `.pf-c-progress__description`                         | Modifies the description to display a single line and truncate any overflow text with ellipses.                                                               |
| `.pf-m-static-width`          | `.pf-c-progress.pf-m-outside .pf-c-progress__measure` | Modifies the measure element to have a static `min-width` that will hold 0-100%. Overridable by setting `--pf-c-progress__measure--m-static-width--MinWidth`. |
