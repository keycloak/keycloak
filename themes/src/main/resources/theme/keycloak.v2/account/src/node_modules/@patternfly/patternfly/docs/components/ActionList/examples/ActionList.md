---
id: Action list
section: components
cssPrefix: pf-c-action-list
---## Examples

### Action list single group

```html
<div class="pf-c-action-list">
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-primary" type="button">Next</button>
  </div>
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-secondary" type="button">Back</button>
  </div>
</div>
<br />With kebab
<div class="pf-c-action-list">
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-primary" type="button">Next</button>
  </div>
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-secondary" type="button">Back</button>
  </div>
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Kebab">
      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Action list with icons

```html
<div class="pf-c-action-list pf-m-icons">
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
  </div>
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Kebab">
      <i class="fas fa-check" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Action list multiple groups

```html
<div class="pf-c-action-list">
  <div class="pf-c-action-list__group">
    <div class="pf-c-action-list__item">
      <button class="pf-c-button pf-m-primary" type="button">Next</button>
    </div>
    <div class="pf-c-action-list__item">
      <button class="pf-c-button pf-m-secondary" type="button">Back</button>
    </div>
  </div>
  <div class="pf-c-action-list__group">
    <div class="pf-c-action-list__item">
      <button class="pf-c-button pf-m-primary" type="button">Submit</button>
    </div>
    <div class="pf-c-action-list__item">
      <button class="pf-c-button pf-m-link" type="button">Cancel</button>
    </div>
  </div>
</div>

```

### Action list with cancel button

```html
In modals, forms, data lists
<div class="pf-c-action-list">
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-primary" type="button">Save</button>
  </div>
  <div class="pf-c-action-list__item">
    <button class="pf-c-button pf-m-link" type="button">Cancel</button>
  </div>
</div>
<br />In wizards
<div class="pf-c-action-list">
  <div class="pf-c-action-list__group">
    <div class="pf-c-action-list__item">
      <button class="pf-c-button pf-m-primary" type="button">Next</button>
    </div>
    <div class="pf-c-action-list__item">
      <button class="pf-c-button pf-m-secondary" type="button">Back</button>
    </div>
  </div>
  <div class="pf-c-action-list__group">
    <div class="pf-c-action-list__item">
      <button class="pf-c-button pf-m-link" type="button">Cancel</button>
    </div>
  </div>
</div>

```

### Overview

### Usage

| Attribute                  | Applied to                                      | Outcome                                           |
| -------------------------- | ----------------------------------------------- | ------------------------------------------------- |
| `.pf-c-action-list`        | `<div>`                                         | Initiates the action list container.              |
| `.pf-c-action-list__item`  | `<div>`                                         | Initiates the action list item container.         |
| `.pf-c-action-list__group` | `<div>`                                         | Initiates the action list group container.        |
| `.pf-m-icons`              | `.pf-c-action-list`, `.pf-c-action-list__group` | Modifies the action list to support button icons. |
