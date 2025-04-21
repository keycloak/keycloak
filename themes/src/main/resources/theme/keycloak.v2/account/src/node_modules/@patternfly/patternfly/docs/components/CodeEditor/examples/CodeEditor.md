---
id: Code editor
section: components
beta: true
cssPrefix: pf-c-code-editor
---## Examples

### Default

```html
<div class="pf-c-code-editor">
  <div class="pf-c-code-editor__header">
    <div class="pf-c-code-editor__controls">
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Copy to clipboard"
      >
        <i class="fas fa-copy" aria-hidden="true"></i>
      </button>
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Download code"
      >
        <i class="fas fa-download"></i>
      </button>
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Upload code"
      >
        <i class="fas fa-upload"></i>
      </button>
    </div>
    <div class="pf-c-code-editor__header-main"></div>
    <div class="pf-c-code-editor__tab">
      <span class="pf-c-code-editor__tab-icon">
        <i class="fas fa-code"></i>
      </span>
      <span class="pf-c-code-editor__tab-text">HTML</span>
    </div>
  </div>
  <div class="pf-c-code-editor__main">
    <code class="pf-c-code-editor__code">
      <pre class="pf-c-code-editor__code-pre">
                code goes here
      </pre>
    </code>
  </div>
</div>

```

### Read-only

```html
<div class="pf-c-code-editor pf-m-read-only">
  <div class="pf-c-code-editor__header">
    <div class="pf-c-code-editor__controls">
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Copy to clipboard"
      >
        <i class="fas fa-copy" aria-hidden="true"></i>
      </button>
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Download code"
      >
        <i class="fas fa-download"></i>
      </button>
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Upload code"
        disabled
      >
        <i class="fas fa-upload"></i>
      </button>
    </div>
    <div class="pf-c-code-editor__header-main"></div>
    <div class="pf-c-code-editor__tab">
      <span class="pf-c-code-editor__tab-icon">
        <i class="fas fa-code"></i>
      </span>
      <span class="pf-c-code-editor__tab-text">HTML</span>
    </div>
  </div>
  <div class="pf-c-code-editor__main">
    <code class="pf-c-code-editor__code">
      <pre class="pf-c-code-editor__code-pre">
                code goes here
      </pre>
    </code>
  </div>
</div>

```

### Without actions

```html
<div class="pf-c-code-editor">
  <div class="pf-c-code-editor__header">
    <div class="pf-c-code-editor__header-main"></div>
    <div class="pf-c-code-editor__tab">
      <span class="pf-c-code-editor__tab-icon">
        <i class="fas fa-code"></i>
      </span>
      <span class="pf-c-code-editor__tab-text">YAML</span>
    </div>
  </div>
  <div class="pf-c-code-editor__main">
    <div class="pf-c-empty-state pf-m-lg">
      <div class="pf-c-empty-state__content">
        <div class="pf-c-empty-state__icon">
          <i class="fas fa-code"></i>
        </div>
        <h1 class="pf-c-title pf-m-lg">Start editing</h1>
        <div
          class="pf-c-empty-state__body"
        >Drag a file here or browse to upload.</div>
        <button class="pf-c-button pf-m-primary" type="button">Browse</button>
        <div class="pf-c-empty-state__secondary">
          <button class="pf-c-button pf-m-link" type="button">Start from scratch</button>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Drag file and hover over component

```html
<div class="pf-c-code-editor">
  <div class="pf-c-code-editor__header">
    <div class="pf-c-code-editor__header-main"></div>
    <div class="pf-c-code-editor__tab">
      <span class="pf-c-code-editor__tab-icon">
        <i class="fas fa-code"></i>
      </span>
      <span class="pf-c-code-editor__tab-text">YAML</span>
    </div>
  </div>
  <div class="pf-c-code-editor__main pf-m-drag-hover">
    <div class="pf-c-empty-state pf-m-lg">
      <div class="pf-c-empty-state__content">
        <div class="pf-c-empty-state__icon">
          <i class="fas fa-code"></i>
        </div>
        <h1 class="pf-c-title pf-m-lg">Start editing</h1>
        <div
          class="pf-c-empty-state__body"
        >Drag a file here or browse to upload.</div>
        <button class="pf-c-button pf-m-primary" type="button">Browse</button>
        <div class="pf-c-empty-state__secondary">
          <button class="pf-c-button pf-m-link" type="button">Start from scratch</button>
        </div>
      </div>
    </div>
  </div>
</div>

```

### With optional header content and keyboard shortcuts

```html
<div class="pf-c-code-editor">
  <div class="pf-c-code-editor__header">
    <div class="pf-c-code-editor__controls">
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Copy to clipboard"
      >
        <i class="fas fa-copy" aria-hidden="true"></i>
      </button>
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Download code"
      >
        <i class="fas fa-download"></i>
      </button>
      <button
        class="pf-c-button pf-m-control"
        type="button"
        aria-label="Upload code"
      >
        <i class="fas fa-upload"></i>
      </button>
    </div>
    <div class="pf-c-code-editor__header-main">Header main content</div>
    <div class="pf-c-code-editor__keyboard-shortcuts">
      <button class="pf-c-button pf-m-link" type="button">
        <span class="pf-c-button__icon pf-m-start">
          <i class="pf-icon pf-icon-help" aria-hidden="true"></i>
        </span>
        View shortcuts
      </button>
    </div>
    <div class="pf-c-code-editor__tab">
      <span class="pf-c-code-editor__tab-icon">
        <i class="fas fa-code"></i>
      </span>
      <span class="pf-c-code-editor__tab-text">HTML</span>
    </div>
  </div>
  <div class="pf-c-code-editor__main">
    <code class="pf-c-code-editor__code">
      <pre class="pf-c-code-editor__code-pre">
                code goes here
      </pre>
    </code>
  </div>
</div>

```

## Documentation

### Overview

### Accessibility

| Class | Applied to | Outcome |
| ----- | ---------- | ------- |

### Usage

| Class                                   | Applied to | Outcome                                                                                   |
| --------------------------------------- | ---------- | ----------------------------------------------------------------------------------------- |
| `.pf-c-code-editor`                     | `<div>`    | Initiates the code editor component. **Required**                                         |
| `.pf-c-code-editor__header`             | `<div>`    | Initiates the code editor header used for the controls and tab elements. **Required**     |
| `.pf-c-code-editor__main`               | `<div>`    | Initiates the main container for a code editor e.g. Monaco **Required**                   |
| `.pf-c-code-editor__code`               | `<div>`    | Initiates the container for code without a JS code editor. Comes with PatternFly styling. |
| `.pf-c-code-editor__controls`           | `<div>`    | Initiates the code editor controls.                                                       |
| `.pf-c-code-editor__header-main`        | `<div>`    | Initiates the code editor header content area.                                            |
| `.pf-c-code-editor__keyboard-shortcuts` | `<div>`    | Initiates the code editor header keyboard shortcuts area.                                 |
| `.pf-c-code-editor__tab`                | `<div>`    | Initiates the code editor tab.                                                            |
| `.pf-c-code-editor__tab-text`           | `<span>`   | Initiates the code editor tab text.                                                       |
| `.pf-c-code-editor__tab-icon`           | `<span>`   | Initiates the code editor tab icon.                                                       |
