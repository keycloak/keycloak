---
id: Empty state
section: components
cssPrefix: pf-c-empty-state
---## Examples

### Basic

```html
<div class="pf-c-empty-state">
  <div class="pf-c-empty-state__content">
    <i class="fas fa-cubes pf-c-empty-state__icon" aria-hidden="true"></i>

    <h1 class="pf-c-title pf-m-lg">Empty state</h1>
    <div
      class="pf-c-empty-state__body"
    >This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.</div>
    <button class="pf-c-button pf-m-primary" type="button">Primary action</button>
    <div class="pf-c-empty-state__secondary">
      <button class="pf-c-button pf-m-link" type="button">Multiple</button>
      <button class="pf-c-button pf-m-link" type="button">Action buttons</button>
      <button class="pf-c-button pf-m-link" type="button">Can</button>
      <button class="pf-c-button pf-m-link" type="button">Go here</button>
      <button class="pf-c-button pf-m-link" type="button">In the secondary</button>
      <button class="pf-c-button pf-m-link" type="button">Action area</button>
    </div>
  </div>
</div>

```

### Extra small

```html
<div class="pf-c-empty-state pf-m-xs">
  <div class="pf-c-empty-state__content">
    <h1 class="pf-c-title pf-m-md">Empty state</h1>
    <div
      class="pf-c-empty-state__body"
    >This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.</div>
    <div class="pf-c-empty-state__secondary">
      <button class="pf-c-button pf-m-link" type="button">Multiple</button>
      <button class="pf-c-button pf-m-link" type="button">Action buttons</button>
      <button class="pf-c-button pf-m-link" type="button">Can</button>
      <button class="pf-c-button pf-m-link" type="button">Go here</button>
      <button class="pf-c-button pf-m-link" type="button">In the secondary</button>
      <button class="pf-c-button pf-m-link" type="button">Action area</button>
    </div>
  </div>
</div>

```

### Small

```html
<div class="pf-c-empty-state pf-m-sm">
  <div class="pf-c-empty-state__content">
    <i class="fas fa-cubes pf-c-empty-state__icon" aria-hidden="true"></i>

    <h1 class="pf-c-title pf-m-lg">Empty state</h1>
    <div
      class="pf-c-empty-state__body"
    >This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.</div>
    <button class="pf-c-button pf-m-primary" type="button">Primary action</button>
    <div class="pf-c-empty-state__secondary">
      <button class="pf-c-button pf-m-link" type="button">Multiple</button>
      <button class="pf-c-button pf-m-link" type="button">Action buttons</button>
      <button class="pf-c-button pf-m-link" type="button">Can</button>
      <button class="pf-c-button pf-m-link" type="button">Go here</button>
      <button class="pf-c-button pf-m-link" type="button">In the secondary</button>
      <button class="pf-c-button pf-m-link" type="button">Action area</button>
    </div>
  </div>
</div>

```

### Large

```html
<div class="pf-c-empty-state pf-m-lg">
  <div class="pf-c-empty-state__content">
    <i class="fas fa-cubes pf-c-empty-state__icon" aria-hidden="true"></i>

    <h1 class="pf-c-title pf-m-lg">Empty state</h1>
    <div
      class="pf-c-empty-state__body"
    >This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.</div>
    <button class="pf-c-button pf-m-primary" type="button">Primary action</button>
    <div class="pf-c-empty-state__secondary">
      <button class="pf-c-button pf-m-link" type="button">Multiple</button>
      <button class="pf-c-button pf-m-link" type="button">Action buttons</button>
      <button class="pf-c-button pf-m-link" type="button">Can</button>
      <button class="pf-c-button pf-m-link" type="button">Go here</button>
      <button class="pf-c-button pf-m-link" type="button">In the secondary</button>
      <button class="pf-c-button pf-m-link" type="button">Action area</button>
    </div>
  </div>
</div>

```

### Extra large

```html
<div class="pf-c-empty-state pf-m-xl">
  <div class="pf-c-empty-state__content">
    <i class="fas fa-cubes pf-c-empty-state__icon" aria-hidden="true"></i>

    <h1 class="pf-c-title pf-m-4xl">Empty state</h1>
    <div
      class="pf-c-empty-state__body"
    >This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.</div>
    <button class="pf-c-button pf-m-primary" type="button">Primary action</button>
  </div>
</div>

```

### With primary element

```html
<div class="pf-c-empty-state">
  <div class="pf-c-empty-state__content">
    <i class="fas fa-cubes pf-c-empty-state__icon" aria-hidden="true"></i>

    <h1 class="pf-c-title pf-m-lg">Empty State</h1>
    <div
      class="pf-c-empty-state__body"
    >This represents an the empty state pattern in PatternFly 4. Hopefully it's simple enough to use but flexible enough to meet a variety of needs.</div>
    <div class="pf-c-empty-state__primary">
      <button class="pf-c-button pf-m-link" type="button">Action buttons</button>
    </div>
  </div>
</div>

```

## Documentation

### Accessibility

| Attribute            | Applied to                | Outcome                                             |
| -------------------- | ------------------------- | --------------------------------------------------- |
| `aria-hidden="true"` | `.pf-c-empty-state__icon` | Hides icon for assistive technologies. **Required** |

### Usage

| Class                          | Applied to                           | Outcome                                                                                                                                                                                                                                                                                                                                                      |
| ------------------------------ | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `.pf-c-empty-state`            | `<div>`                              | Initiates an empty state component. The empty state centers its content (`.pf-c-empty-state__content`) vertically and horizontally. **Required**                                                                                                                                                                                                             |
| `.pf-c-empty-state__content`   | `<div>`                              | Creates the content container. **Required**                                                                                                                                                                                                                                                                                                                  |
| `.pf-c-empty-state__icon`      | `<i>`, `<div>`                       | Creates the empty state icon or icon container when used as a `<div>`.                                                                                                                                                                                                                                                                                       |
| `.pf-c-title`                  | `<h1>, <h2>, <h3>, <h4>, <h5>, <h6>` | Creates the empty state title. **Required**                                                                                                                                                                                                                                                                                                                  |
| `.pf-c-empty-state__body`      | `<div>`                              | Creates the empty state body content. You can have more than one `.pf-c-empty-state__body` elements.                                                                                                                                                                                                                                                         |
| `.pf-c-button.pf-m-primary`    | `<button>`                           | Creates the primary action button.                                                                                                                                                                                                                                                                                                                           |
| `.pf-c-empty-state__primary`   | `<div>`                              | Container for primary actions. Can be used in lieu of using `.pf-c-button.pf-m-primary`.                                                                                                                                                                                                                                                                     |
| `.pf-c-empty-state__secondary` | `<div>`                              | Container secondary actions.                                                                                                                                                                                                                                                                                                                                 |
| `.pf-m-xs`                     | `.pf-c-empty-state`                  | Modifies the empty state for a extra small variation and max-width.                                                                                                                                                                                                                                                                                          |
| `.pf-m-sm`                     | `.pf-c-empty-state`                  | Modifies the empty state for a small max-width.                                                                                                                                                                                                                                                                                                              |
| `.pf-m-lg`                     | `.pf-c-empty-state`                  | Modifies the empty state for a large max-width.                                                                                                                                                                                                                                                                                                              |
| `.pf-m-xl`                     | `.pf-c-empty-state`                  | Modifies the empty state for an x-large variation and max-width.                                                                                                                                                                                                                                                                                             |
| `.pf-m-full-height`            | `.pf-c-empty-state`                  | Modifies the empty state to be `height: 100%`. If you need the empty state content to be centered vertically, you can use this modifier to make the empty state fill the height of its container, and center `.pf-c-empty-state__content`. **Note:** this modifier requires the parent of `.pf-c-empty-state` have an implicit or explicit `height` defined. |
