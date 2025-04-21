---
id: 'Masthead'
beta: true
section: components
cssPrefix: pf-c-masthead
---## Examples

### Basic

```html
<header class="pf-c-masthead" id="basic-masthead">
  <span class="pf-c-masthead__toggle">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Global navigation"
    >
      <i class="fas fa-bars" aria-hidden="true"></i>
    </button>
  </span>
  <div class="pf-c-masthead__main">
    <a class="pf-c-masthead__brand" href="#">Logo</a>
  </div>
  <div class="pf-c-masthead__content">
    <span>Content</span>
  </div>
</header>

```

### Basic with mixed content

```html
<header class="pf-c-masthead" id="basic-masthead-with-mixed-content">
  <span class="pf-c-masthead__toggle">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Global navigation"
    >
      <i class="fas fa-bars" aria-hidden="true"></i>
    </button>
  </span>
  <div class="pf-c-masthead__main">
    <a class="pf-c-masthead__brand" href="#">Logo</a>
  </div>
  <div class="pf-c-masthead__content">
    <div class="pf-l-flex">
      <span>Testing text color</span>
      <button class="pf-c-button pf-m-primary" type="button">testing</button>
      <div class="pf-l-flex__item pf-m-align-flex-end">
        <button class="pf-c-button pf-m-primary" type="button">testing</button>
      </div>
    </div>
  </div>
</header>

```

### Display inline

```html
<header class="pf-c-masthead pf-m-display-inline" id="inline-masthead">
  <span class="pf-c-masthead__toggle">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Global navigation"
    >
      <i class="fas fa-bars" aria-hidden="true"></i>
    </button>
  </span>
  <div class="pf-c-masthead__main">
    <a class="pf-c-masthead__brand" href="#">Logo</a>
  </div>
  <div class="pf-c-masthead__content">
    <span>Content</span>
  </div>
</header>

```

### Display stack

```html
<header class="pf-c-masthead pf-m-display-stack" id="stack-masthead">
  <span class="pf-c-masthead__toggle">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Global navigation"
    >
      <i class="fas fa-bars" aria-hidden="true"></i>
    </button>
  </span>
  <div class="pf-c-masthead__main">
    <a class="pf-c-masthead__brand" href="#">Logo</a>
  </div>
  <div class="pf-c-masthead__content">
    <span>Content</span>
  </div>
</header>

```

### Display stack, display inline responsive

```html
<header
  class="pf-c-masthead pf-m-display-inline pf-m-display-stack-on-lg pf-m-display-inline-on-2xl"
  id="stack-inline-masthead"
>
  <span class="pf-c-masthead__toggle">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Global navigation"
    >
      <i class="fas fa-bars" aria-hidden="true"></i>
    </button>
  </span>
  <div class="pf-c-masthead__main">
    <a class="pf-c-masthead__brand" href="#">Logo</a>
  </div>
  <div class="pf-c-masthead__content">
    <span>Content</span>
  </div>
</header>

```

### Light variant

```html
<header class="pf-c-masthead pf-m-light" id="light-masthead">
  <span class="pf-c-masthead__toggle">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Global navigation"
    >
      <i class="fas fa-bars" aria-hidden="true"></i>
    </button>
  </span>
  <div class="pf-c-masthead__main">
    <a class="pf-c-masthead__brand" href="#">Logo</a>
  </div>
  <div class="pf-c-masthead__content">
    <span>Content</span>
  </div>
</header>

```

### Light 200 variant

```html
<header class="pf-c-masthead pf-m-light-200" id="light-masthead">
  <span class="pf-c-masthead__toggle">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Global navigation"
    >
      <i class="fas fa-bars" aria-hidden="true"></i>
    </button>
  </span>
  <div class="pf-c-masthead__main">
    <a class="pf-c-masthead__brand" href="#">Logo</a>
  </div>
  <div class="pf-c-masthead__content">
    <span>Content</span>
  </div>
</header>

```

### Insets

```html
<header class="pf-c-masthead pf-m-inset-sm" id="inset-masthead">
  <span class="pf-c-masthead__toggle">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Global navigation"
    >
      <i class="fas fa-bars" aria-hidden="true"></i>
    </button>
  </span>
  <div class="pf-c-masthead__main">
    <a class="pf-c-masthead__brand" href="#">Logo</a>
  </div>
  <div class="pf-c-masthead__content">
    <span>Content</span>
  </div>
</header>

```

## Documentation

### Usage

| Class                                                       | Applied to       | Outcome                                                                                                                                           |
| ----------------------------------------------------------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-masthead`                                            | `<header>`       | Initiates the masthead component. **Required**                                                                                                    |
| `.pf-c-masthead__main`                                      | `<div>`          | Initiates the masthead main component. **Required**                                                                                               |
| `.pf-c-masthead__toggle`                                    | `<span>`         | Initiates the masthead toggle component.                                                                                                          |
| `.pf-c-masthead__brand`                                     | `<a>, <div>`     | Initiates the masthead content component.                                                                                                         |
| `.pf-c-masthead__content`                                   | `<div>`          | Initiates the masthead content component.                                                                                                         |
| `.pf-m-inset-{none, sm, md, lg, xl, 2xl}{-on-[breakpoint]}` | `.pf-c-masthead` | Modifies masthead horizontal padding at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). |
| `.pf-m-light`                                               | `.pf-c-masthead` | Modifies a masthead component to have a light theme with a background color of `--pf-global--BackgroundColor--100`.                               |
| `.pf-m-light-200`                                           | `.pf-c-masthead` | Modifies a masthead component to have a light theme with a background color of `--pf-global--BackgroundColor--200`.                               |
