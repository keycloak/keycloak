---
id: Chip
section: components
cssPrefix: pf-c-chip
---## Examples

### Basic

```html
<div class="pf-c-chip">
  <span class="pf-c-chip__text" id="chip_one">Chip</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-labelledby="remove_chip_one chip_one"
    aria-label="Remove"
    id="remove_chip_one"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</div>
<br />
<br />
<div class="pf-c-chip">
  <span
    class="pf-c-chip__text"
    id="chip_two"
  >Really long chip that goes on and on</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-labelledby="remove_chip_two chip_two"
    aria-label="Remove"
    id="remove_chip_two"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</div>
<br />
<br />
<div class="pf-c-chip">
  <span class="pf-c-chip__text" id="chip_three">Chip</span>
  <span class="pf-c-badge pf-m-read">00</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-labelledby="remove_chip_three chip_three"
    aria-label="Remove"
    id="remove_chip_three"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</div>
<br />
<br />
<div class="pf-c-chip">
  <span class="pf-c-chip__text">Read-only chip</span>
</div>
<br />
<br />
<button class="pf-c-chip pf-m-overflow">
  <span class="pf-c-chip__text">Overflow chip</span>
</button>
<br />
<br />
<div class="pf-c-chip pf-m-draggable">
  <span class="pf-c-chip__icon">
    <i class="fas fa-grip-vertical" role="img" aria-label="Drag"></i>
  </span>
  <span class="pf-c-chip__text">Draggable chip</span>
</div>

```

## Documentation

### Overview

A Chip is used to display items that have been filtered or selected from a larger group. They comprise of a text element and a button component that is used to remove the chip from selection. When the text overflows it is truncated using ellipses. A chip can be grouped by using the "chip-group" component.

## Accessibility

| Attribute                                      | Applied to     | Outcome                                                                                                                                                            |
| ---------------------------------------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `aria-label="[button label text]"`             | `.pf-c-button` | Provides an accessible name for the button when an icon is used instead of text. Required when an icon is used with no supporting text.                            |
| `aria-labelledby="[id value of .pf-c-button]"` | `.pf-c-button` | Gives the button an accessible name by referring to the element that provides the position of the button within a list. Required when the button is being removed. |
| `aria-hidden="true"`                           | `<i>`          | Hides the icon from assistive technologies.                                                                                                                        |

## Usage

| Class              | Applied to            | Outcome                                                                         |
| ------------------ | --------------------- | ------------------------------------------------------------------------------- |
| `.pf-c-chip`       | `<div>`, `<button>`,  | Initiates the chip component. Use a `<button>` with overflow chips **Required** |
| `.pf-c-chip__text` | `<span>`              | Initiates the text inside of the chip. **Required**                             |
| `.pf-c-chip__icon` | `<span>`              | Initiates the icon inside of the chip.                                          |
| `.pf-c-button`     | `.pf-c-chip <button>` | Initiates the button used to remove the chip.                                   |
| `.pf-c-badge`      | `<span>`              | Initiates the badge inside the chip.                                            |
| `.pf-m-overflow`   | `button.pf-c-chip`    | Applies styling of the overflow chip.                                           |
| `.pf-m-draggable`  | `.pf-c-chip`          | Modifies the chip to be in the draggable state.                                 |
