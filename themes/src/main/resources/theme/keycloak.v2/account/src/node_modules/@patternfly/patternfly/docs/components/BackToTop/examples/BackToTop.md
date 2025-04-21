---
id: 'Back to top'
section: components
cssPrefix: pf-c-back-to-top
---import './BackToTop.css'

## Examples

### Basic

```html
<div class="pf-c-back-to-top">
  <a class="pf-c-button pf-m-primary" href="#">
    Back to top
    <span class="pf-c-button__icon pf-m-end">
      <i class="fas fa-angle-up" aria-hidden="true"></i>
    </span>
  </a>
</div>

```

## Documentation

### Usage

| Class               | Applied to          | Outcome                                           |
| ------------------- | ------------------- | ------------------------------------------------- |
| `.pf-c-back-to-top` | `<div>`             | Initiates the back to top component. **Required** |
| `.pf-m-hidden`      | `.pf-c-back-to-top` | Modifies the component to be hidden.              |
