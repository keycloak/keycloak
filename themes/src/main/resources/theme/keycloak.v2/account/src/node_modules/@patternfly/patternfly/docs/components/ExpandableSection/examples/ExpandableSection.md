---
id: Expandable section
section: components
cssPrefix: pf-c-expandable-section
---## Examples

### Hidden

```html
<div class="pf-c-expandable-section">
  <button
    type="button"
    class="pf-c-expandable-section__toggle"
    aria-expanded="false"
  >
    <span class="pf-c-expandable-section__toggle-icon">
      <i class="fas fa-angle-right" aria-hidden="true"></i>
    </span>
    <span class="pf-c-expandable-section__toggle-text">Show more</span>
  </button>
  <div
    class="pf-c-expandable-section__content"
    hidden
  >This content is visible only when the component is expanded.</div>
</div>

```

### Expanded

```html
<div class="pf-c-expandable-section pf-m-expanded">
  <button
    type="button"
    class="pf-c-expandable-section__toggle"
    aria-expanded="true"
  >
    <span class="pf-c-expandable-section__toggle-icon">
      <i class="fas fa-angle-right" aria-hidden="true"></i>
    </span>
    <span class="pf-c-expandable-section__toggle-text">Show less</span>
  </button>
  <div
    class="pf-c-expandable-section__content"
  >This content is visible only when the component is expanded.</div>
</div>

```

### Disclosure variation (hidden)

```html
<div class="pf-c-expandable-section pf-m-display-lg pf-m-limit-width">
  <button
    type="button"
    class="pf-c-expandable-section__toggle"
    aria-expanded="false"
  >
    <span class="pf-c-expandable-section__toggle-icon">
      <i class="fas fa-angle-right" aria-hidden="true"></i>
    </span>
    <span class="pf-c-expandable-section__toggle-text">Show more</span>
  </button>
  <div
    class="pf-c-expandable-section__content"
    hidden
  >This content is visible only when the component is expanded.</div>
</div>

```

### Disclosure variation (expanded)

```html
<div
  class="pf-c-expandable-section pf-m-expanded pf-m-display-lg pf-m-limit-width"
>
  <button
    type="button"
    class="pf-c-expandable-section__toggle"
    aria-expanded="true"
  >
    <span class="pf-c-expandable-section__toggle-icon">
      <i class="fas fa-angle-right" aria-hidden="true"></i>
    </span>
    <span class="pf-c-expandable-section__toggle-text">Show less</span>
  </button>
  <div
    class="pf-c-expandable-section__content"
  >This content is visible only when the component is expanded.</div>
</div>

```

### Detached toggle

```html
<div class="pf-l-stack pf-m-gutter">
  <div class="pf-l-stack__item">
    <div class="pf-c-expandable-section pf-m-expanded pf-m-detached">
      <div
        class="pf-c-expandable-section__content"
        id="detached-toggle-content"
      >This content is visible only when the component is expanded.</div>
    </div>
  </div>

  <div class="pf-l-stack__item">
    <div class="pf-c-expandable-section pf-m-expanded pf-m-detached">
      <button
        type="button"
        class="pf-c-expandable-section__toggle"
        aria-expanded="true"
        aria-controls="detached-toggle-content"
      >
        <span class="pf-c-expandable-section__toggle-icon pf-m-expand-top">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
        <span class="pf-c-expandable-section__toggle-text">Show less</span>
      </button>
    </div>
  </div>
</div>

```

### Indented

```html
<div class="pf-c-expandable-section pf-m-expanded pf-m-indented">
  <button
    type="button"
    class="pf-c-expandable-section__toggle"
    aria-expanded="true"
  >
    <span class="pf-c-expandable-section__toggle-icon">
      <i class="fas fa-angle-right" aria-hidden="true"></i>
    </span>
    <span class="pf-c-expandable-section__toggle-text">Show less</span>
  </button>
  <div
    class="pf-c-expandable-section__content"
  >This content is visible only when the component is expanded.</div>
</div>

```

## Documentation

### Accessibility

| Attribute                                 | Applied to                                                                 | Outcome                                                                                                        |
| ----------------------------------------- | -------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `aria-expanded="true"`                    | `.pf-c-expandable-section__toggle`                                         | Indicates that the expandable section content is visible. **Required**                                         |
| `aria-expanded="false"`                   | `.pf-c-expandable-section__toggle`                                         | Indicates the the expandable section content is hidden. **Required**                                           |
| `hidden`                                  | `.pf-c-expandable-section__content`                                        | Indicates that the expandable section content element is hidden. Use with `aria-expanded="false"` **Required** |
| `aria-hidden="true"`                      | `.pf-c-expandable-section__toggle-icon`                                    | Hides the icon from screen readers. **Required**                                                               |
| `aria-controls="[id of content element]"` | `.pf-c-expandable-section.pf-m-detached .pf-c-expandable-section__toggle`  | Identifies the element controlled by the toggle button. **Required**                                           |
| `id`                                      | `.pf-c-expandable-section.pf-m-detached .pf-c-expandable-section__content` | Gives the content an `id` for use with `aria-controls` on `.pf-c-expandable-section__toggle`. **Required**     |

### Usage

| Class                                   | Applied to                              | Outcome                                                                                                                                          |
| --------------------------------------- | --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `.pf-c-expandable-section`              | `<div>`                                 | Initiates the expandable section component. **Required**                                                                                         |
| `.pf-c-expandable-section__toggle`      | `<button>`                              | Initiates the expandable section toggle. **Required**                                                                                            |
| `.pf-c-expandable-section__toggle-text` | `<span>`                                | Initiates the expandable toggle text. **Required**                                                                                               |
| `.pf-c-expandable-section__toggle-icon` | `<span>`                                | Initiates the expandable toggle icon. **Required**                                                                                               |
| `.pf-c-expandable-section__content`     | `<div>`                                 | Initiates the expandable section content. **Required**                                                                                           |
| `.pf-m-expanded`                        | `.pf-c-expandable-section`              | Modifies the component for the expanded state.                                                                                                   |
| `.pf-m-display-lg`                      | `.pf-c-expandable-section`              | Modifies the styling of the component to have large display styling.                                                                             |
| `.pf-m-detached`                        | `.pf-c-expandable-section`              | Indicates that the expandable section toggle and content are detached from one another, so you can move them around independently in the markup. |
| `.pf-m-indented`                        | `.pf-c-expandable-section`              | Indicates that the expandable section content is indented and is aligned with the start of the title text to provide visual hierarchy.           |
| `.pf-m-active`                          | `.pf-c-expandable-section__toggle`      | Forces display of the active state of the toggle.                                                                                                |
| `.pf-m-expand-top`                      | `.pf-c-expandable-section__toggle-icon` | Modifies the toggle icon to point up when expanded.                                                                                              |
