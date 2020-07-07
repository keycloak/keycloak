---
title: Progress
section: components
cssPrefix: pf-c-progress
---

## Examples
```hbs title=Simple
{{#> progress 
  progress__value="33" 
  progress__description="Title" 
  progress__id="progress-simple-example"
}}
{{/progress}}
```

```hbs title=Small
{{#> progress 
  progress__value="33" 
  progress__description="Title" 
  progress--modifier="pf-m-sm"
  progress__id="progress-sm-example"
}}
{{/progress}}
```

```hbs title=Large
{{#> progress 
  progress__value="33" 
  progress__description="Title" 
  progress--modifier="pf-m-lg"
  progress__id="progress-lg-example"
}}
{{/progress}}
```

```hbs title=Outside
{{#> progress 
  progress__value="33" 
  progress__description="Title" 
  progress--modifier="pf-m-outside pf-m-lg"
  progress__id="progress-outside-example"
}}
{{/progress}}
```

```hbs title=Inside
{{#> progress 
  progress__value="33" 
  progress__description="Title" 
  progress--modifier="pf-m-lg"
  progress--inside="inside"
  progress__id="progress-inside-example"
}}
{{/progress}}
```

```hbs title=Success
{{#> progress 
  progress__value="100" 
  progress__description="Title" 
  progress--success="success"
  progress__id="progress-success-example"
}}
{{/progress}}
```

```hbs title=Failure
{{#> progress 
  progress__value="33" 
  progress__description="Title" 
  progress--danger="true"
  progress__id="progress-failure-example"
}}
{{/progress}}
```

```hbs title=Inside-success
{{#> progress 
  progress__value="100" 
  progress__description="Title" 
  progress--modifier="pf-m-lg"
  progress--inside="inside"
  progress--success="success"
  progress__id="progress-inside-success-example"
}}
{{/progress}}
```

```hbs title=Outside-failure
{{#> progress 
  progress__value="33" 
  progress__description="Title" 
  progress--modifier="pf-m-outside pf-m-lg"
  progress--danger="danger"
  progress__id="progress-outside-failure-example"
}}
{{/progress}}
```

```hbs title=On-single-line
{{#> progress 
  progress__value="33"
  progress--modifier="pf-m-singleline"
  progress__id="progress-singleline-example"
}}
{{/progress}}
```

```hbs title=Without-measure
{{#> progress 
  progress__value="33" 
  progress__description="Title" 
  progress--no-measure="true"
  progress__id="progress-no-measure-example"
}}
{{/progress}}
```

```hbs title=Failure-without-measure
{{#> progress 
  progress__value="33" 
  progress--no-measure="true"
  progress__description="Title" 
  progress--danger="true"
  progress__id="progress-no-measure-failure-example"
}}
{{/progress}}
```

```hbs title=Finite-step
{{#> progress 
  progress__value="2"
  progress__valuemax="5" 
  progress__width="40"
  progress__valuetext="2 of 5 units"
  progress__description="Title"
  progress--dynamic="true"
  progress__id="progress-finite-step-example"
}}
{{/progress}}
```
### Non-percantage progress
If the status that displays with the bar is not a percentage, then the ARIA tag `aria-valuetext` should be used to provide this status to screen reader users. This is the only case when setting the `aria-valuemax` to a value other than "100" is recommended, given how different screen readers handle these attributes.

```hbs title=Progress-step-instruction
{{#> progress 
  progress__value="2"
  progress__valuemax="5" 
  progress__width="40"
  progress__valuetext="Step 2: Copying files"
  progress__description="Title"
  progress--dynamic="true"
  progress__id="progress-step-instruction-example"
}}
{{/progress}}
```

## Documentation
### Overview
### Accessibility
If this component is describing the loading progress of a particular region of a page, the author should use `aria-describedby` to point to the status, and set the `aria-busy` attribute to `true` on the region until it is finished loading. 

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `role="progressbar"` | `.pf-c-progress__bar` |  This role is used for an element that displays the progress status for a task that takes a long time or consists of several steps. |
| `aria-valuenow=""` | `.pf-c-progress__bar` |  This value needs to be updated as progress continues. |
| `aria-valuemin="0"` | `.pf-c-progress__bar` |  The minimum value for the progress bar. |
| `aria-valuemax="100"` | `.pf-c-progress__bar` |  The maximum value for the progress bar. If the progress is only defined using `aria-valuenow` (e.g a percentage), the value should be set to "100". If the progress is defined using `aria-valuetext`, then this value can be a number other than 100. For example, if `aria-valuetext` is "2 of 5 units", then `aria-valuemax` can be "5" and `aria-valuenow` can be "2". |
| `aria-describedby="[id of .pf-c-progress__description]"` | `.pf-c-progress__bar` |  The description of what progress is being shown. |
| `aria-valuetext="[loading state]"` | `.pf-c-progress__bar` |  Provide a text string that communicates current status. Only use if the important information about status is included in the text string. Do not use if percentage is the most important value to communicate. Some screen readers will ignore the percentage value determined from `aria-valuenow` when `aria-valuetext` is used. |
| `aria-hidden="true"` | `.pf-c-progress__status` |  Hides the visible progress bar status from screen readers. This information is communicated by the aria attributes defined on the `.pf-c-progress__bar` element. |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-progress` | `<div>` |  Initiates a progress component. |
| `.pf-c-progress__description` | `<div>` |  The description for a progress bar. |
| `.pf-c-progress__status` | `<div>` |  Displays the % of progress and status icons. |
| `.pf-c-progress__measure` | `<span>` |  Displays the % complete. |
| `.pf-c-progress__status-icon` | `<span>` |  Displays the status icon. (optional) |
| `.pf-c-progress__bar` | `<div>` |  Displays across the entire width and represents the completed state. |
| `.pf-c-progress__indicator` | `<div>` |  Displays with the `.pf-c-progress__bar` to indicate the progress so far. |
| `.pf-m-lg` | `.pf-c-progress` |  Modifies the progress bar to be larger. |
| `.pf-m-sm` | `.pf-c-progress` |  Modifies the progress bar to be smaller. |
| `.pf-m-inside` | `.pf-c-progress` |  Shows the measure within the progress indicator. NOTE: This option requires `.pf-m-lg`.|
| `.pf-m-outside` | `.pf-c-progress` |  Shows the measure and status icon to the right of the progress bar. |
| `.pf-m-singleline` | `.pf-c-progress` | Modifies the progress component to exist on one row. If a measure is needed, use with `.pf-m-inside` or `.pf-m-outside`|
| `.pf-m-success` | `.pf-c-progress` |  Changes the appearance of the progess component to indicate a success state. |
| `.pf-m-danger` | `.pf-c-progress` |  Changes the appearance of the progess component to indicate a danger (failure) state. |
