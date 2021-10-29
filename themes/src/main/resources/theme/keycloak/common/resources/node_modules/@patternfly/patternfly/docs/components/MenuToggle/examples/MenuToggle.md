---
id: 'Menu toggle'
beta: true
section: components
cssPrefix: pf-c-menu-toggle
---import './MenuToggle.css'

## Examples

### Collapsed

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false">
  <span class="pf-c-menu-toggle__text">Collapsed</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Expanded

```html
<button
  class="pf-c-menu-toggle pf-m-expanded"
  type="button"
  aria-expanded="true"
>
  <span class="pf-c-menu-toggle__text">Expanded</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Disabled

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false" disabled>
  <span class="pf-c-menu-toggle__text">Disabled</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Icon

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false">
  <span class="pf-c-menu-toggle__icon">
    <i class="fas fa-cog" aria-hidden="true"></i>
  </span>
  <span class="pf-c-menu-toggle__text">Icon</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Count

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false">
  <span class="pf-c-menu-toggle__text">Count</span>
  <span class="pf-c-menu-toggle__count">
    <span class="pf-c-badge pf-m-unread">4 selected</span>
  </span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Primary

```html
<button
  class="pf-c-menu-toggle pf-m-primary"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__text">Collapsed</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-primary"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__icon">
    <i class="fas fa-cog" aria-hidden="true"></i>
  </span>
  <span class="pf-c-menu-toggle__text">Icon</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-primary pf-m-expanded"
  type="button"
  aria-expanded="true"
>
  <span class="pf-c-menu-toggle__text">Expanded</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-primary"
  type="button"
  aria-expanded="false"
  disabled
>
  <span class="pf-c-menu-toggle__text">Disabled</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Plain

```html
<button
  class="pf-c-menu-toggle pf-m-plain"
  type="button"
  aria-expanded="false"
  aria-label="Actions"
>
  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-plain pf-m-expanded"
  type="button"
  aria-expanded="true"
  aria-label="Actions"
>
  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-plain"
  type="button"
  aria-expanded="false"
  disabled
  aria-label="Actions"
>
  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
</button>

```

### With image and text

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false">
  <span class="pf-c-menu-toggle__image">
    <img
      class="pf-c-avatar"
      src="/assets/images/img_avatar.svg"
      alt="Avatar image"
    />
  </span>
  <span class="pf-c-menu-toggle__text">Ned Username</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Full height

```html
<button
  class="pf-c-menu-toggle pf-m-full-height"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__text">Full height</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Accessibility

| Class                           | Applied to                     | Outcome                                                             |
| ------------------------------- | ------------------------------ | ------------------------------------------------------------------- |
| `aria-expanded="true"`          | `.pf-c-menu-toggle`            | Indicates that the menu toggle component is in the expanded state.  |
| `aria-expanded="false"`         | `.pf-c-menu-toggle`            | Indicates that the menu toggle component is in the collapsed state. |
| `aria-label="Descriptive text"` | `.pf-c-menu-toggle.pf-m-plain` | Gives the plain menu toggle component an accessible label.          |
| `disabled`                      | `.pf-c-menu-toggle`            | Indicates that the menu toggle component is disabled.               |

### Usage

| Class                            | Applied             | Outcome                                                       |
| -------------------------------- | ------------------- | ------------------------------------------------------------- |
| `.pf-c-menu-toggle`              | `<button>`          | Initiates the menu toggle component.                          |
| `.pf-c-menu-toggle__icon`        | `<span>`            | Defines the menu toggle component icon.                       |
| `.pf-c-menu-toggle__image`       | `<span>`            | Defines the menu toggle component image.                      |
| `.pf-c-menu-toggle__text`        | `<span>`            | Defines the menu toggle component text.                       |
| `.pf-c-menu-toggle__count`       | `<span>`            | Defines the menu toggle component count.                      |
| `.pf-c-menu-toggle__controls`    | `<span>`            | Defines the menu toggle component controls.                   |
| `.pf-c-menu-toggle__toggle-icon` | `<span>`            | Defines the menu toggle component toggle/arrow icon.          |
| `.pf-m-primary`                  | `.pf-c-menu-toggle` | Modifies the menu toggle component for the primary variation. |
| `.pf-m-plain`                    | `.pf-c-menu-toggle` | Modifies the menu toggle component for the plain variation.   |
| `.pf-m-expanded`                 | `.pf-c-menu-toggle` | Modifies the menu toggle component for the expanded state.    |
| `.pf-m-full-height`              | `.pf-c-menu-toggle` | Modifies the menu toggle component to full height of parent.  |
