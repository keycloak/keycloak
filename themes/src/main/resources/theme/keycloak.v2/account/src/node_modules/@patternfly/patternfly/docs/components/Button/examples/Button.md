---
id: Button
section: components
cssPrefix: pf-c-button
---import './Button.css'

## Examples

### Variations

```html
<button class="pf-c-button pf-m-primary" type="button">Primary</button>

<button class="pf-c-button pf-m-secondary" type="button">Secondary</button>

<button
  class="pf-c-button pf-m-secondary pf-m-danger"
  type="button"
>Secondary danger</button>

<button class="pf-c-button pf-m-tertiary" type="button">Tertiary</button>

<button class="pf-c-button pf-m-danger" type="button">Danger</button>

<button class="pf-c-button pf-m-warning" type="button">Warning</button>

<br />
<br />
<button class="pf-c-button pf-m-link" type="button">
  <span class="pf-c-button__icon pf-m-start">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
  Link
</button>

<button class="pf-c-button pf-m-link" type="button">
  Link
  <span class="pf-c-button__icon pf-m-end">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
</button>

<button class="pf-c-button pf-m-link pf-m-danger" type="button">
  <span class="pf-c-button__icon pf-m-start">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
  Link danger
</button>

<button class="pf-c-button pf-m-inline pf-m-link" type="button">Inline link</button>
<br />
<br />
<button class="pf-c-button pf-m-plain" type="button" aria-label="Remove">
  <i class="fas fa-times" aria-hidden="true"></i>
</button>
<br />
<br />
<button class="pf-c-button pf-m-control" type="button">Control</button>

<button class="pf-c-button pf-m-control" type="button" aria-label="Copy input">
  <i class="fas fa-copy" aria-hidden="true"></i>
</button>

<br />
<br />
<br />

<button class="pf-c-button pf-m-primary pf-m-small" type="button">Primary</button>

<button class="pf-c-button pf-m-secondary pf-m-small" type="button">Secondary</button>

<button
  class="pf-c-button pf-m-secondary pf-m-danger pf-m-small"
  type="button"
>Secondary danger</button>

<button class="pf-c-button pf-m-tertiary pf-m-small" type="button">Tertiary</button>

<button class="pf-c-button pf-m-danger pf-m-small" type="button">Danger</button>

<button class="pf-c-button pf-m-warning pf-m-small" type="button">Warning</button>

<button class="pf-c-button pf-m-link pf-m-small" type="button">
  Link
  <span class="pf-c-button__icon pf-m-end">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
</button>

<button class="pf-c-button pf-m-link pf-m-danger pf-m-small" type="button">
  Link danger
  <span class="pf-c-button__icon pf-m-end">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
</button>

<button
  class="pf-c-button pf-m-inline pf-m-link pf-m-small"
  type="button"
>Inline link</button>

<button class="pf-c-button pf-m-control pf-m-small" type="button">Control</button>

```

### Disabled

```html
<button class="pf-c-button pf-m-primary" type="button" disabled>Primary disabled</button>

<button
  class="pf-c-button pf-m-secondary"
  type="button"
  disabled
>Secondary disabled</button>

<button
  class="pf-c-button pf-m-secondary pf-m-danger"
  type="button"
  disabled
>Secondary danger disabled</button>

<button
  class="pf-c-button pf-m-tertiary"
  type="button"
  disabled
>Tertiary disabled</button>

<button class="pf-c-button pf-m-danger" type="button" disabled>Danger disabled</button>

<button class="pf-c-button pf-m-warning" type="button" disabled>Warning disabled</button>

<button class="pf-c-button pf-m-link" type="button" disabled>
  <span class="pf-c-button__icon pf-m-start">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
  Link disabled
</button>

<button class="pf-c-button pf-m-link pf-m-danger" type="button" disabled>
  <span class="pf-c-button__icon pf-m-start">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
  Link danger disabled
</button>

<button
  class="pf-c-button pf-m-link pf-m-inline"
  type="button"
  disabled
>Inline link disabled</button>

<button
  class="pf-c-button pf-m-plain"
  type="button"
  aria-label="Remove"
  disabled
>
  <i class="fas fa-times" aria-hidden="true"></i>
</button>

<button class="pf-c-button pf-m-control" type="button" disabled>Control disabled</button>

```

### Aria-disabled

```html
<button
  class="pf-c-button pf-m-primary pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>Primary disabled</button>

<button
  class="pf-c-button pf-m-secondary pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>Secondary disabled</button>

<button
  class="pf-c-button pf-m-secondary pf-m-danger pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>Secondary danger disabled</button>

<button
  class="pf-c-button pf-m-tertiary pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>Tertiary disabled</button>

<button
  class="pf-c-button pf-m-danger pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>Danger disabled</button>

<button
  class="pf-c-button pf-m-warning pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>Warning disabled</button>

<button
  class="pf-c-button pf-m-link pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>
  <span class="pf-c-button__icon pf-m-start">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
  Link disabled
</button>

<button
  class="pf-c-button pf-m-link pf-m-danger pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>
  <span class="pf-c-button__icon pf-m-start">
    <i class="fas fa-plus-circle" aria-hidden="true"></i>
  </span>
  Link danger disabled
</button>

<button
  class="pf-c-button pf-m-link pf-m-inline pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>Inline link disabled</button>

<button
  class="pf-c-button pf-m-plain pf-m-aria-disabled"
  type="button"
  aria-label="Remove"
  aria-disabled="true"
>
  <i class="fas fa-times" aria-hidden="true"></i>
</button>

<button
  class="pf-c-button pf-m-control pf-m-aria-disabled"
  type="button"
  aria-disabled="true"
>Control disabled</button>

```

### Links as buttons

```html
<a
  class="pf-c-button pf-m-primary"
  href="https://www.w3.org/TR/WCAG20-TECHS/ARIA8.html#ARIA8-examples"
>Primary link to W3.org</a>

<a
  class="pf-c-button pf-m-secondary"
  href="#overview"
  aria-label="Read more about button documentation"
>Secondary link to anchor</a>

<a
  class="pf-c-button pf-m-secondary pf-m-danger"
  href="#overview"
  aria-label="Read more about button documentation"
>Secondary danger link to anchor</a>

<a
  class="pf-c-button pf-m-tertiary pf-m-disabled"
  href="https://www.w3.org/TR/WCAG20-TECHS/ARIA8.html#ARIA8-examples"
  aria-disabled="true"
  tabindex="-1"
>Tertiary link to W3.org</a>

<a
  class="pf-c-button pf-m-link"
  href="https://www.w3.org/TR/WCAG20-TECHS/ARIA8.html#ARIA8-examples"
>Link to W3.org</a>

<a
  class="pf-c-button pf-m-link pf-m-danger"
  href="https://www.w3.org/TR/WCAG20-TECHS/ARIA8.html#ARIA8-examples"
>Link danger to W3.org</a>

```

### Inline link as span

```html
Lorem ipsum dolor sit amet, consectetur adipiscing elit.
<span
  class="pf-c-button pf-m-link pf-m-inline"
  type="button"
  role="button"
  tabindex="0"
>This is long button text that needs to be a span so that it will wrap inline with the text around it.</span>
Sed hendrerit nisi in cursus maximus. Ut malesuada nisi turpis, in condimentum velit elementum non.

```

### Block level

```html
<button
  class="pf-c-button pf-m-primary pf-m-block"
  type="button"
>Block level button</button>

```

### Types

```html
<button class="pf-c-button pf-m-primary" type="submit">Submit</button>

<button class="pf-c-button pf-m-primary" type="reset">Reset</button>

<button class="pf-c-button pf-m-primary" type="button">Default</button>

```

### Call to action

```html
<button
  class="pf-c-button pf-m-primary pf-m-display-lg"
  type="button"
>Call to action</button>

<button
  class="pf-c-button pf-m-secondary pf-m-display-lg"
  type="button"
>Call to action</button>

<button
  class="pf-c-button pf-m-tertiary pf-m-display-lg"
  type="button"
>Call to action</button>

<button class="pf-c-button pf-m-link pf-m-display-lg" type="button">
  Call to action
  <span class="pf-c-button__icon pf-m-end">
    <i class="fas fa-arrow-right" aria-hidden="true"></i>
  </span>
</button>

<button class="pf-c-button pf-m-link pf-m-inline pf-m-display-lg" type="button">
  Call to action
  <span class="pf-c-button__icon pf-m-end">
    <i class="fas fa-arrow-right" aria-hidden="true"></i>
  </span>
</button>

```

### Progress

```html
<button
  class="pf-c-button pf-m-progress pf-m-primary"
  type="button"
>Primary loader</button>

<button
  class="pf-c-button pf-m-progress pf-m-in-progress pf-m-primary"
  type="button"
>
  <span class="pf-c-button__progress">
    <span
      class="pf-c-spinner pf-m-md"
      role="progressbar"
      aria-label="Loading..."
    >
      <span class="pf-c-spinner__clipper"></span>
      <span class="pf-c-spinner__lead-ball"></span>
      <span class="pf-c-spinner__tail-ball"></span>
    </span>
  </span>
  Primary loading
</button>

<button
  class="pf-c-button pf-m-progress pf-m-secondary"
  type="button"
>Secondary loader</button>

<button
  class="pf-c-button pf-m-progress pf-m-in-progress pf-m-secondary"
  type="button"
>
  <span class="pf-c-button__progress">
    <span
      class="pf-c-spinner pf-m-md"
      role="progressbar"
      aria-label="Loading..."
    >
      <span class="pf-c-spinner__clipper"></span>
      <span class="pf-c-spinner__lead-ball"></span>
      <span class="pf-c-spinner__tail-ball"></span>
    </span>
  </span>
  Secondary loading
</button>

<button class="pf-c-button pf-m-plain" type="button" aria-label="Upload">
  <i class="fas fa-upload" aria-hidden="true"></i>
</button>

<button
  class="pf-c-button pf-m-plain pf-m-in-progress"
  type="button"
  aria-label="Upload"
>
  <i class="fas fa-upload" aria-hidden="true"></i>
  <span class="pf-c-button__progress">
    <span
      class="pf-c-spinner pf-m-md"
      role="progressbar"
      aria-label="Loading..."
    >
      <span class="pf-c-spinner__clipper"></span>
      <span class="pf-c-spinner__lead-ball"></span>
      <span class="pf-c-spinner__tail-ball"></span>
    </span>
  </span>
</button>

```

## Documentation

### Overview

Always add a modifier class to add color to the button.

### Button vs link

Semantic buttons and links are important for usability as well as accessibility. Using an `a` instead of a `button` element to perform user initiated actions should be avoided, unless absolutely necessary.

### Accessibility

| Attribute                          | Applied to                                                                            | Outcome                                                                                                                                                                                        |
| ---------------------------------- | ------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-pressed="true or false"`     | `.pf-c-button`                                                                        | Indicates that the button is a toggle. When set to "true", `pf-m-active` should also be set so that the button displays in an active state. **Required when button is a toggle**               |
| `aria-label="[button label text]"` | `.pf-c-button.pf-m-plain`                                                             | Provides an accessible name for the button when an icon is used instead of text. **Required when icon is used with no supporting text**                                                        |
| `aria-label="[descriptive text]"`  | `a.pf-c-button`, `span.pf-c-button.pf-m-link.pf-m-inline`                             | The button component's text should adequately describe its purpose. If it does not, `aria-label` can provide more detailed interaction information.                                            |
| `disabled`                         | `button.pf-c-button`                                                                  | When a button element is used, indicates that it is unavailable and removes it from keyboard focus. **Required when button is disabled**                                                       |
| `aria-disabled="true"`             | `button.pf-c-button`                                                                  | When a button element is used, indicates that it is unavailable but does not prevent keyboard or hover interactions. Used when a disabled button provides interactive elements like a tooltip. |
| `aria-disabled="true"`             | `a.pf-c-button.pf-m-disabled`, `span.pf-c-button.pf-m-link.pf-m-inline.pf-m-disabled` | When a non-button element is used, indicates that it is unavailable. **Required when element is disabled**                                                                                     |
| `aria-expanded="true"`             | `.pf-c-button.pf-m-expanded`                                                          | Indicates that the expanded content element is visible. **Required**                                                                                                                           |
| `tabindex="-1"`                    | `a.pf-c-button.pf-m-disabled`, `span.pf-c-button.pf-m-link.pf-m-inline.pf-m-disabled` | When a non-button element is used, removes it from keyboard focus. **Required when element is disabled**                                                                                       |
| `tabindex="0"`                     | `span.pf-c-button.pf-m-link.pf-m-inline`                                              | Inserts the span into the tab order of the page so that it is focusable. **Required when the element is a span**                                                                               |

### Usage

| Class                    | Applied to                              | Outcome                                                                                                                                                                                                                     |
| ------------------------ | --------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-button`           | `<button>`                              | Initiates a button. Always use it with a modifier class. **Required**                                                                                                                                                       |
| `.pf-c-button__icon`     | `<span>`                                | Initiates a button icon.                                                                                                                                                                                                    |
| `.pf-c-button__progress` | `<span>`                                | Initiates a button progress container.                                                                                                                                                                                      |
| `.pf-m-primary`          | `.pf-c-button`                          | Modifies for primary styles.                                                                                                                                                                                                |
| `.pf-m-secondary`        | `.pf-c-button`                          | Modifies for secondary styles.                                                                                                                                                                                              |
| `.pf-m-tertiary`         | `.pf-c-button`                          | Modifies for tertiary styles.                                                                                                                                                                                               |
| `.pf-m-danger`           | `.pf-c-button`                          | Modifies for danger styles.                                                                                                                                                                                                 |
| `.pf-m-link`             | `.pf-c-button`                          | Modifies for link styles. This button has no background or border and is styled as a link. This button would commonly appear in a form and may include an icon.                                                             |
| `.pf-m-plain`            | `.pf-c-button`                          | Modifies for icon styles. This button has no background or border, uses a standard text color, and is used for `.pf-m-plain` icon buttons such as close, expand, kebab, etc.                                                |
| `.pf-m-inline`           | `.pf-c-button.pf-m-link`                | Modifies for inline styles. This button is presented similar to a normal link and has no padding and is displayed inline with other inline content. When used as a `<span>`, the text will flow inline with text around it. |
| `.pf-m-block`            | `.pf-c-button`                          | Creates a block level button.                                                                                                                                                                                               |
| `.pf-m-control`          | `.pf-c-button`                          | Modifies for control styles. **Note:** This modifier should only be used when using buttons in the Input Group or Clipboard Copy components.                                                                                |
| `.pf-m-expanded`         | `.pf-c-button.pf-m-control`             | Modifies a control button for the expanded state.                                                                                                                                                                           |
| `.pf-m-start`            | `.pf-c-button__icon`                    | Applies right spacing to an icon inside of a button when the icon comes before text.                                                                                                                                        |
| `.pf-m-end`              | `.pf-c-button__icon`                    | Applies left spacing to an icon inside of a button when the icon comes after text.                                                                                                                                          |
| `.pf-m-active`           | `.pf-c-button`                          | Forces display of the active state of the button. This modifier should be used when `aria-pressed` is set to true so that the button displays in an active state.                                                           |
| `.pf-m-small`            | `.pf-c-button`                          | Modifies the button so that it has small font size.                                                                                                                                                                         |
| `.pf-m-aria-disabled`    | `.pf-c-button`                          | Modifies a button to be visually disabled, yet is still focusable.                                                                                                                                                          |
| `.pf-m-display-lg`       | `.pf-c-button`, `pf-c-button.pf-m-link` | Modifies the button and link button for large display styling. For example, use this modifier to achieve "call to action" styles.                                                                                           |
| `.pf-m-progress`         | `.pf-c-button`                          | Indicates that the button supports the progress state. **Note:** Not used with the plain variation.                                                                                                                         |
| `.pf-m-in-progress`      | `.pf-c-button`                          | Indicates that the button is in the in progress state.                                                                                                                                                                      |
