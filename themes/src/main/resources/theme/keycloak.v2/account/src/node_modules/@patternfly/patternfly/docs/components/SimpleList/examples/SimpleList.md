---
id: Simple list
section: components
cssPrefix: pf-c-simple-list
---## Examples

### Simple list

```html
<div class="pf-c-simple-list">
  <ul class="pf-c-simple-list__list">
    <li class="pf-c-simple-list__item">
      <button
        class="pf-c-simple-list__item-link pf-m-current"
        type="button"
      >List item 1</button>
    </li>
    <li class="pf-c-simple-list__item">
      <button class="pf-c-simple-list__item-link" type="button">List item 2</button>
    </li>
    <li class="pf-c-simple-list__item">
      <button class="pf-c-simple-list__item-link" type="button">List item 3</button>
    </li>
  </ul>
</div>

```

### Simple list with links

```html
<div class="pf-c-simple-list">
  <ul class="pf-c-simple-list__list">
    <li class="pf-c-simple-list__item">
      <a
        class="pf-c-simple-list__item-link pf-m-current"
        href="#"
        tabindex="0"
      >List item 1</a>
    </li>
    <li class="pf-c-simple-list__item">
      <a class="pf-c-simple-list__item-link" href="#" tabindex="0">List item 2</a>
    </li>
    <li class="pf-c-simple-list__item">
      <a class="pf-c-simple-list__item-link" href="#" tabindex="0">List item 3</a>
    </li>
  </ul>
</div>

```

### Grouped list

```html
<div class="pf-c-simple-list">
  <section class="pf-c-simple-list__section">
    <h2 class="pf-c-simple-list__title">Title</h2>
    <ul class="pf-c-simple-list__list">
      <li class="pf-c-simple-list__item">
        <button
          class="pf-c-simple-list__item-link pf-m-current"
          type="button"
        >List item 1</button>
      </li>
      <li class="pf-c-simple-list__item">
        <button class="pf-c-simple-list__item-link" type="button">List item 2</button>
      </li>
      <li class="pf-c-simple-list__item">
        <button class="pf-c-simple-list__item-link" type="button">List item 3</button>
      </li>
      <li class="pf-c-simple-list__item">
        <button class="pf-c-simple-list__item-link" type="button">List item 4</button>
      </li>
    </ul>
  </section>
  <section class="pf-c-simple-list__section">
    <h2 class="pf-c-simple-list__title">Title</h2>
    <ul class="pf-c-simple-list__list">
      <li class="pf-c-simple-list__item">
        <button class="pf-c-simple-list__item-link" type="button">List item 1</button>
      </li>
      <li class="pf-c-simple-list__item">
        <button class="pf-c-simple-list__item-link" type="button">List item 2</button>
      </li>
      <li class="pf-c-simple-list__item">
        <button class="pf-c-simple-list__item-link" type="button">List item 3</button>
      </li>
      <li class="pf-c-simple-list__item">
        <button class="pf-c-simple-list__item-link" type="button">List item 4</button>
      </li>
    </ul>
  </section>
</div>

```

## Documentation

### Accessibility

| Attribute      | Applied to                      | Outcome                                                                               |
| -------------- | ------------------------------- | ------------------------------------------------------------------------------------- |
| `tabindex="0"` | `a.pf-c-simple-list__item-link` | Inserts the link into the tab order of the page so that it is focusable. **Required** |

### Usage

| Class                          | Applied to                     | Outcome                                                                                   |
| ------------------------------ | ------------------------------ | ----------------------------------------------------------------------------------------- |
| `.pf-c-simple-list`            | `<div>`                        | Initiates a simple list.                                                                  |
| `.pf-c-simple-list__section`   | `<section>`                    | Initiates a simple list section.                                                          |
| `.pf-c-simple-list__title`     | `<h2>`                         | Initiates a simple list title.                                                            |
| `.pf-c-simple-list__list`      | `<ul>`                         | Initiates a simple list unordered list.                                                   |
| `.pf-c-simple-list__item`      | `<li>`                         | Initiates a simple list item.                                                             |
| `.pf-c-simple-list__item-link` | `<button>`, `<a>`              | Initiates a simple list item link. It can be a button or a link depending on the context. |
| `.pf-m-current`                | `.pf-c-simple-list__item-link` | Modifies the simple list item link for the current state.                                 |
