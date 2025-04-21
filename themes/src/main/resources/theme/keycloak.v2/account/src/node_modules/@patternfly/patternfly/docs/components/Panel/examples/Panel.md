---
id: 'Panel'
beta: true
section: components
cssPrefix: pf-c-panel
---## Examples

### Basic

```html
<div class="pf-c-panel">
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">Main content</div>
  </div>
</div>

```

### Header

```html
<div class="pf-c-panel">
  <div class="pf-c-panel__header">Header content</div>
  <hr class="pf-c-divider" />
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">Main content</div>
  </div>
</div>

```

### Footer

```html
<div class="pf-c-panel">
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">Main content</div>
  </div>
  <div class="pf-c-panel__footer">Footer content</div>
</div>

```

### Header and footer

```html
<div class="pf-c-panel">
  <div class="pf-c-panel__header">Header content</div>
  <hr class="pf-c-divider" />
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">Main content</div>
  </div>
  <div class="pf-c-panel__footer">Footer content</div>
</div>

```

### No body

```html
<div class="pf-c-panel">
  <div class="pf-c-panel__main">Main content</div>
</div>

```

### Raised

```html
<div class="pf-c-panel pf-m-raised">
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">Main content</div>
  </div>
</div>

```

### Bordered

```html
<div class="pf-c-panel pf-m-bordered">
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">Main content</div>
  </div>
</div>

```

### Scrollable

```html
<div class="pf-c-panel pf-m-scrollable">
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">
      Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
    </div>
  </div>
</div>

```

### Scrollable with header and footer

```html
<div class="pf-c-panel pf-m-scrollable">
  <div class="pf-c-panel__header">Header content</div>
  <hr class="pf-c-divider" />
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">
      Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
      <br />
      <br />Main content
    </div>
  </div>
  <div class="pf-c-panel__footer">Footer content</div>
</div>

```

## Documentation

### Usage

| Class                    | Applied to    | Outcome                                   |
| ------------------------ | ------------- | ----------------------------------------- |
| `.pf-c-panel`            | `<div>`       | Initiates the panel. **Required**         |
| `.pf-c-panel__header`    | `<div>`       | Initiates the panel header.               |
| `.pf-c-panel__main`      | `<div>`       | Initiates the panel main content.         |
| `.pf-c-panel__main-body` | `<div>`       | Initiates a panel content body container. |
| `.pf-c-panel__footer`    | `<div>`       | Initiates the panel footer.               |
| `.pf-m-bordered`         | `.pf-c-panel` | Modifies the panel for bordered styles.   |
| `.pf-m-raised`           | `.pf-c-panel` | Modifies the panel for raised styles.     |
| `.pf-m-scrollable`       | `.pf-c-panel` | Modifies the panel for scrollable styles. |
