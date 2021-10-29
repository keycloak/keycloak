---
id: 'Drag and drop'
beta: true
section: components
cssPrefix: pf-c-drag-drop
---import './DragDrop.css'

## Examples

### Basic

```html
<div class="pf-c-droppable">
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
</div>

```

### Dragging

```html
<div class="pf-c-droppable pf-m-dragging">
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable pf-m-dragging">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
</div>

```

### Drag outside

```html
<div class="pf-c-droppable pf-m-dragging pf-m-drag-outside">
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable pf-m-dragging pf-m-drag-outside">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
  <div class="pf-c-draggable">Item</div>
</div>

```

## Documentation

### Usage

| Class                | Applied to                           | Outcome                                                                  |
| -------------------- | ------------------------------------ | ------------------------------------------------------------------------ |
| `.pf-c-draggable`    | `*`                                  | Initiates a draggable element.                                           |
| `.pf-c-droppable`    | `*`                                  | Initiates a droppable element.                                           |
| `.pf-m-dragging`     | `.pf-c-draggable`, `.pf-c-droppable` | Indicates a draggable and droppable element are in the dragging state.   |
| `.pf-m-drag-outside` | `.pf-c-draggable`, `.pf-c-droppable` | Indicates a draggable element is dragged outside of a droppable element. |
