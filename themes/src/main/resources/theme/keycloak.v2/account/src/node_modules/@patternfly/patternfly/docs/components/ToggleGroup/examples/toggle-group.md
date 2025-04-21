---
id: Toggle group
section: components
cssPrefix: pf-c-toggle-group
---import './toggle-group.css'

## Examples

### Default

```html
<div class="pf-c-toggle-group">
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 1</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 2</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 3</span>
    </button>
  </div>
</div>

<br />

<div class="pf-c-toggle-group">
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button pf-m-selected" type="button">
      <span class="pf-c-toggle-group__text">Option 1</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button pf-m-selected" type="button">
      <span class="pf-c-toggle-group__text">Option 2</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 3</span>
    </button>
  </div>
</div>

<br />

<div class="pf-c-toggle-group">
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 1</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 2</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button" disabled>
      <span class="pf-c-toggle-group__text">Option 3</span>
    </button>
  </div>
</div>

```

### With icon

```html
<div class="pf-c-toggle-group">
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Copy button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-copy" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Undo button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-undo" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Share button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-share-square" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>

<br />

<div class="pf-c-toggle-group">
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button pf-m-selected"
      type="button"
      aria-label="Copy button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-copy" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Undo button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-undo" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Share button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-share-square" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>

<br />

<div class="pf-c-toggle-group">
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Copy button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-copy" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Undo button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-undo" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Share button"
      disabled
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-share-square" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>

```

### Icon-and-text

```html
<div class="pf-c-toggle-group">
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-copy" aria-hidden="true"></i>
      </span>
      <span class="pf-c-toggle-group__text">Copy</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button
      class="pf-c-toggle-group__button"
      type="button"
      aria-label="Undo button"
    >
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-undo" aria-hidden="true"></i>
      </span>
      <span class="pf-c-toggle-group__text">Undo</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button pf-m-selected" type="button">
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-share-square" aria-hidden="true"></i>
      </span>
      <span class="pf-c-toggle-group__text">Share</span>
    </button>
  </div>
</div>

<br />

<div class="pf-c-toggle-group">
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Copy</span>
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-copy" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Undo</span>
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-undo" aria-hidden="true"></i>
      </span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button pf-m-selected" type="button">
      <span class="pf-c-toggle-group__text">Share</span>
      <span class="pf-c-toggle-group__icon">
        <i class="fas fa-share-square" aria-hidden="true"></i>
      </span>
    </button>
  </div>
</div>

```

### Compact

```html
<div class="pf-c-toggle-group pf-m-compact">
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 1</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 2</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 3</span>
    </button>
  </div>
</div>

<br />

<div class="pf-c-toggle-group pf-m-compact">
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button pf-m-selected" type="button">
      <span class="pf-c-toggle-group__text">Option 1</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button pf-m-selected" type="button">
      <span class="pf-c-toggle-group__text">Option 2</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 3</span>
    </button>
  </div>
</div>

<br />

<div class="pf-c-toggle-group pf-m-compact">
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 1</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button">
      <span class="pf-c-toggle-group__text">Option 2</span>
    </button>
  </div>
  <div class="pf-c-toggle-group__item">
    <button class="pf-c-toggle-group__button" type="button" disabled>
      <span class="pf-c-toggle-group__text">Option 3</span>
    </button>
  </div>
</div>

```

## Documentation

### Accessibility

| Attribute                          | Applied to                   | Outcome                                                                                                                                  |
| ---------------------------------- | ---------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-label="[button label text]"` | `.pf-c-toggle-group__button` | Provides an accessible name for the button when an icon is used instead of text. **Required when icon is used with no supporting text**  |
| `disabled`                         | `.pf-c-toggle-group__button` | When a button element is used, indicates that it is unavailable and removes it from keyboard focus. **Required when button is disabled** |

### Usage

| Class                        | Applied to                   | Outcome                                                         |
| ---------------------------- | ---------------------------- | --------------------------------------------------------------- |
| `.pf-c-toggle-group`         | `<div>`                      | Initiates the toggle group. **Required**                        |
| `.pf-c-toggle-group__button` | `<button>`                   | Initiates the toggle group button. **Required**                 |
| `.pf-c-toggle-group__item`   | `<div>`                      | Initiates the toggle group item wrapper. **Required**           |
| `.pf-c-toggle-group__text`   | `<span>`                     | Initiates the toggle button text element.                       |
| `.pf-c-toggle-group__icon`   | `<span>`                     | Initiates the toggle button icon element.                       |
| `.pf-m-compact`              | `.pf-c-toggle-group`         | Modifies the toggle group for compact styles.                   |
| `.pf-m-selected`             | `.pf-c-toggle-group__button` | Modifies the toggle button group button for the selected state. |
