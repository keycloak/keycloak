---
id: Tooltip
section: components
cssPrefix: pf-c-tooltip
---## Examples

### Top

```html
<div class="pf-c-tooltip pf-m-top" role="tooltip">
  <div class="pf-c-tooltip__arrow"></div>

  <div
    class="pf-c-tooltip__content"
    id="tooltip-top-content"
  >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.</div>
</div>

```

### Right

```html
<div class="pf-c-tooltip pf-m-right" role="tooltip">
  <div class="pf-c-tooltip__arrow"></div>

  <div
    class="pf-c-tooltip__content"
    id="tooltip-right-content"
  >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.</div>
</div>

```

### Bottom

```html
<div class="pf-c-tooltip pf-m-bottom" role="tooltip">
  <div class="pf-c-tooltip__arrow"></div>

  <div
    class="pf-c-tooltip__content"
    id="tooltip-bottom-content"
  >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.</div>
</div>

```

### Left

```html
<div class="pf-c-tooltip pf-m-left" role="tooltip">
  <div class="pf-c-tooltip__arrow"></div>

  <div
    class="pf-c-tooltip__content"
    id="tooltip-left-content"
  >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.</div>
</div>

```

### Left aligned text

```html
<div class="pf-c-tooltip pf-m-top" role="tooltip">
  <div class="pf-c-tooltip__arrow"></div>

  <div
    class="pf-c-tooltip__content pf-m-text-align-left"
    id="tooltip-text-align-left-example"
  >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam id feugiat augue, nec fringilla turpis.</div>
</div>

```

## Documentation

### Overview

A tooltip is used to provide contextual information for another component on hover.  The tooltip itself is made up of two elements: arrow and content. One of the directional modifiers (`.pf-m-left`, `.pf-m-top`, etc.) is required on the tooltip component.

### Accessibility

| Attribute                                                                                                 | Applied to                            | Outcome                                                                                                                                                                                                                                                                                                                                               |
| --------------------------------------------------------------------------------------------------------- | ------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="tooltip"`                                                                                          | `.pf-c-tooltip`                       | Adds a tooltip role to the tooltip component. **Required**                                                                                                                                                                                                                                                                                            |
| `aria-describedby="[id of .pf-c-tooltip__content]"` or `aria-labelledby="[id of .pf-c-tooltip__content]"` | `[element that triggers the tooltip]` | Makes the contents of the tooltip accessible to assistive technologies by associating the tooltip text with the element that triggers the tooltip. Use `aria-labelledby` if the tooltip provides a label for the element. Use `aria-describedby` if the element already has an accessible label that is different from the tooltip text. **Required** |

### Usage

| Class                    | Applied to               | Outcome                                                                                                                           |
| ------------------------ | ------------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-tooltip`          | `<div>`                  | Creates a tooltip. Always use with a modifier class that positions the tooltip relative to the element it describes. **Required** |
| `.pf-c-tooltip__arrow`   | `<div>`                  | Creates an arrow pointing towards the element the tooltip describes. **Required**                                                 |
| `.pf-c-tooltip__content` | `<div>`                  | Creates the body of the tooltip. **Required**                                                                                     |
| `.pf-m-left`             | `.pf-c-tooltip`          | Positions the tooltip to the left of the element.                                                                                 |
| `.pf-m-right`            | `.pf-c-tooltip`          | Positions the tooltip to the right of the element.                                                                                |
| `.pf-m-top`              | `.pf-c-tooltip`          | Positions the tooltip to the top of the element.                                                                                  |
| `.pf-m-bottom`           | `.pf-c-tooltip`          | Positions the tooltip to the bottom of the element.                                                                               |
| `.pf-m-text-align-left`  | `.pf-c-tooltip__content` | Modifies tooltip content to text align left.                                                                                      |
