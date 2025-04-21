---
id: 'Helper text'
section: components
cssPrefix: pf-c-helper-text
---## Examples

### Static

```html
<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item">
    <span class="pf-c-helper-text__item-text">This is default helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-indeterminate">
    <span class="pf-c-helper-text__item-text">This is indeterminate helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-warning">
    <span class="pf-c-helper-text__item-text">This is warning helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-success">
    <span class="pf-c-helper-text__item-text">This is success helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-error">
    <span class="pf-c-helper-text__item-text">This is error helper text</span>
  </div>
</div>

```

### Icon

```html
<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-minus" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is default helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-indeterminate">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-minus" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is indeterminate helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-warning">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-exclamation-triangle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is warning helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-success">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is success helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-error">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is error helper text</span>
  </div>
</div>

```

### Multiple static

```html
<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item">
    <span class="pf-c-helper-text__item-text">This is default helper text</span>
  </div>
  <div class="pf-c-helper-text__item">
    <span
      class="pf-c-helper-text__item-text"
    >This is another default helper text in the same block</span>
  </div>
  <div class="pf-c-helper-text__item">
    <span
      class="pf-c-helper-text__item-text"
    >And this is more default text in the same block</span>
  </div>
</div>

```

### Dynamic

```html
<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-dynamic">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-minus" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is default helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-dynamic pf-m-indeterminate">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-minus" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is indeterminate helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-dynamic pf-m-warning">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-exclamation-triangle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is warning helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is success helper text</span>
  </div>
</div>

<div class="pf-c-helper-text">
  <div class="pf-c-helper-text__item pf-m-dynamic pf-m-error">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">This is error helper text</span>
  </div>
</div>

```

### Dynamic list

```html
<ul class="pf-c-helper-text">
  <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-helper-text__item-text">Must be at least 14 characters</span>
  </li>
  <li class="pf-c-helper-text__item pf-m-dynamic pf-m-error">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
    </span>
    <span
      class="pf-c-helper-text__item-text"
    >Cannot contain any variation of the word "redhat"</span>
  </li>
  <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
    <span class="pf-c-helper-text__item-icon">
      <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
    </span>
    <span
      class="pf-c-helper-text__item-text"
    >Must include at least 3 of the following: lowercase letter, uppercase letters, numbers, symbols</span>
  </li>
</ul>

```

### Usage

| Class                          | Applied to                | Outcome                                                                                                                                  |
| ------------------------------ | ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-helper-text`            | `<div>`, `<ul>`           | Initiates the helper text component. **Required**                                                                                        |
| `.pf-c-helper-text__item`      | `<div>`, `<li>`           | Initiates a helper text item. **Required**                                                                                               |
| `.pf-c-helper-text__item-icon` | `<span>`                  | Initiates a helper text item icon. **Required when used in `.pf-c-helper-text__item.pf-m-dynamic`**                                      |
| `.pf-c-helper-text__item-text` | `<span>`                  | Initiates a helper text item text. **Required**                                                                                          |
| `.pf-m-dynamic`                | `.pf-c-helper-text__item` | Modifies a helper text item to be dynamic. For use when the item changes state as the form field the text is associated with is updated. |
| `.pf-m-indeterminate`          | `.pf-c-helper-text__item` | Modifies a helper text item for indeterminate state styles.                                                                              |
| `.pf-m-warning`                | `.pf-c-helper-text__item` | Modifies a helper text item for warning state styles.                                                                                    |
| `.pf-m-success`                | `.pf-c-helper-text__item` | Modifies a helper text item for success state styles.                                                                                    |
| `.pf-m-error`                  | `.pf-c-helper-text__item` | Modifies a helper text item for error state styles.                                                                                      |
