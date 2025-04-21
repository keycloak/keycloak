---
id: Overflow menu
section: components
cssPrefix: pf-c-overflow-menu
---import './overflow-menu.css'

## Introduction

The overflow menu component condenses actions inside `.pf-c-overflow-menu__content` container into a single dropdown button wrapped in `.pf-c-overflow-menu__control`.

The overflow menu relies on groups (`.pf-c-overflow-menu__group`) and items (`.pf-c-overflow-menu__item`), with default spacer values. Groups and items can be siblings and/or items can be nested within groups. Modifier selectors adjust spacing based on the type of group. Each modifier applies a unique CSS variable, therefore, the base spacer value for all elements can be customized and item/groups spacers can be themed individually. The default spacer value for items and groups is set to `--pf-c-toolbar--spacer--base`, whose value is `--pf-global--spacer--md` or 16px.

### Simple collapsed

```html
<div class="pf-c-overflow-menu" id="overflow-menu-simple">
  <div class="pf-c-overflow-menu__control">
    <div class="pf-c-dropdown pf-m-expanded">
      <button
        class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
        type="button"
        id="overflow-menu-simple-dropdown-toggle"
        aria-label="Generic options"
        aria-expanded="true"
      >
        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
      </button>
      <ul
        class="pf-c-dropdown__menu"
        aria-labelledby="overflow-menu-simple-dropdown-toggle"
      >
        <li>
          <button class="pf-c-dropdown__menu-item">Item 1</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Item 2</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Item 3</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Item 4</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Item 5</button>
        </li>
      </ul>
    </div>
  </div>
</div>

```

### Simple expanded

```html
<div class="pf-c-overflow-menu" id="overflow-menu-simple-expanded">
  <div class="pf-c-overflow-menu__content">
    <div class="pf-c-overflow-menu__item">Item 1</div>
    <div class="pf-c-overflow-menu__item">Item 2</div>
    <div class="pf-c-overflow-menu__group">
      <div class="pf-c-overflow-menu__item">Item 3</div>
      <div class="pf-c-overflow-menu__item">Item 4</div>
      <div class="pf-c-overflow-menu__item">Item 5</div>
    </div>
  </div>
</div>

```

### Default spacing for items and groups:

| Class                        | CSS Variable                          | Computed Value |
| ---------------------------- | ------------------------------------- | -------------- |
| `.pf-c-overflow-menu__group` | `--pf-c-overflow-menu__group--spacer` | `16px`         |
| `.pf-c-overflow-menu__item`  | `--pf-c-overflow-menu__item--spacer`  | `16px`         |

### Overflow menu item types

| Class                          | Applied to | Outcome                                                  |
| ------------------------------ | ---------- | -------------------------------------------------------- |
| `.pf-c-overflow-menu`          | `<div>`    | Initiates an overflow menu. **Required**                 |
| `.pf-c-overflow-menu__content` | `<div>`    | Initiates an overflow menu content section. **Required** |
| `.pf-c-overflow-menu__control` | `<div>`    | Initiates the overflow menu control. **Required**        |
| `.pf-c-overflow-menu__group`   | `<div>`    | Initiates an overflow menu group.                        |
| `.pf-c-overflow-menu__item`    | `<div>`    | Initiates an overflow menu item. **Required**            |

### Group types

```html
<div class="pf-c-overflow-menu" id="overflow-menu-button-group-example">
  <div class="pf-c-overflow-menu__content">
    <div class="pf-c-overflow-menu__group">
      <div class="pf-c-overflow-menu__item">Item 1</div>
      <div class="pf-c-overflow-menu__item">Item 2</div>
      <div class="pf-c-overflow-menu__item">Item 3</div>
    </div>
    <div class="pf-c-overflow-menu__group pf-m-button-group">
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-primary" type="button">Primary</button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-tertiary" type="button">Tertiary</button>
      </div>
    </div>
    <div class="pf-c-overflow-menu__group pf-m-icon-button-group">
      <div class="pf-c-overflow-menu__item">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Align left"
        >
          <i class="fas fa-align-left" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Align center"
        >
          <i class="fas fa-align-center" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Align right"
        >
          <i class="fas fa-align-right" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </div>
</div>

```

The action group consists of a primary and secondary action. Any additional actions are part of the overflow control dropdown.

### Overflow menu group types

| Class                        | Applied to                   | Outcome                                                                                                                                                                                                                                                           |
| ---------------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-overflow-menu__group` | `<div>`                      | Initiates an overflow menu component group.                                                                                                                                                                                                                       |
| `.pf-m-button-group`         | `.pf-c-overflow-menu__group` | Modifies overflow menu group spacing. Spacer value is set to `var(--pf-c-overflow-menu__group--m-button-group--spacer)`. Child `.pf-c-button` spacer value is set to `var(--pf-c-overflow-menu__group--m-button-group--space-items)`.                             |
| `.pf-m-icon-button-group`    | `.pf-c-overflow-menu__group` | Modifies overflow menu group spacing. Spacer value is set to `var(--pf-c-overflow-menu__group--m-icon-button-group--spacer)`. Child `.pf-c-button.pf-m-button-plain` spacer value is set to `var(--pf-c-overflow-menu__group--m-icon-button-group--space-items)`. |

### Additional options in dropdown (hidden)

```html
<div
  class="pf-c-overflow-menu"
  id="overflow-menu-simple-additional-options-hidden"
>
  <div class="pf-c-overflow-menu__control">
    <div class="pf-c-dropdown pf-m-expanded">
      <button
        class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
        type="button"
        id="overflow-menu-simple-additional-options-hidden-dropdown-toggle"
        aria-label="Dropdown with additional options"
        aria-expanded="true"
      >
        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
      </button>
      <ul
        class="pf-c-dropdown__menu"
        aria-labelledby="overflow-menu-simple-additional-options-hidden-dropdown-toggle"
      >
        <li>
          <button class="pf-c-dropdown__menu-item">Primary</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Secondary</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Tertiary</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Align left</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Align center</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Align right</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Action 7</button>
        </li>
      </ul>
    </div>
  </div>
</div>

```

### Additional options in dropdown (visible)

```html
<div
  class="pf-c-overflow-menu"
  id="overflow-menu-simple-additional-options-visible"
>
  <div class="pf-c-overflow-menu__content">
    <div class="pf-c-overflow-menu__group pf-m-button-group">
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-primary" type="button">Primary</button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-tertiary" type="button">Tertiary</button>
      </div>
    </div>
    <div class="pf-c-overflow-menu__group pf-m-icon-button-group">
      <div class="pf-c-overflow-menu__item">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Align left"
        >
          <i class="fas fa-align-left" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Align center"
        >
          <i class="fas fa-align-center" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Align right"
        >
          <i class="fas fa-align-right" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </div>
  <div class="pf-c-overflow-menu__control">
    <div class="pf-c-dropdown pf-m-expanded">
      <button
        class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
        type="button"
        id="overflow-menu-simple-additional-options-visible-dropdown-toggle"
        aria-label="Dropdown with additional options"
        aria-expanded="true"
      >
        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
      </button>
      <ul
        class="pf-c-dropdown__menu"
        aria-labelledby="overflow-menu-simple-additional-options-visible-dropdown-toggle"
      >
        <li>
          <button class="pf-c-dropdown__menu-item">Action 7</button>
        </li>
      </ul>
    </div>
  </div>
</div>

```

## Persistent configuration

### Persistent additional options (hidden)

```html
<div class="pf-c-overflow-menu" id="overflow-menu-persistent-hidden">
  <div class="pf-c-overflow-menu__content">
    <div class="pf-c-overflow-menu__group pf-m-button-group">
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-primary" type="button">Primary</button>
      </div>
    </div>
  </div>
  <div class="pf-c-overflow-menu__control">
    <div class="pf-c-dropdown pf-m-expanded">
      <button
        class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
        type="button"
        id="overflow-menu-persistent-hidden-dropdown-toggle"
        aria-label="Dropdown for persistent example"
        aria-expanded="true"
      >
        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
      </button>
      <ul
        class="pf-c-dropdown__menu"
        aria-labelledby="overflow-menu-persistent-hidden-dropdown-toggle"
      >
        <li>
          <button class="pf-c-dropdown__menu-item">Secondary</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Tertiary</button>
        </li>
        <li>
          <button class="pf-c-dropdown__menu-item">Action 4</button>
        </li>
      </ul>
    </div>
  </div>
</div>

```

### Persistent additional options (visible)

```html
<div class="pf-c-overflow-menu" id="overflow-menu-persistent-visible-example">
  <div class="pf-c-overflow-menu__content">
    <div class="pf-c-overflow-menu__group pf-m-button-group">
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-primary" type="button">Primary</button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
      </div>
      <div class="pf-c-overflow-menu__item">
        <button class="pf-c-button pf-m-tertiary" type="button">Tertiary</button>
      </div>
    </div>
  </div>
  <div class="pf-c-overflow-menu__control">
    <div class="pf-c-dropdown pf-m-expanded">
      <button
        class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
        type="button"
        id="overflow-menu-persistent-visible-example-dropdown-toggle"
        aria-label="Dropdown for persistent example"
        aria-expanded="true"
      >
        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
      </button>
      <ul
        class="pf-c-dropdown__menu"
        aria-labelledby="overflow-menu-persistent-visible-example-dropdown-toggle"
      >
        <li>
          <button class="pf-c-dropdown__menu-item">Action 4</button>
        </li>
      </ul>
    </div>
  </div>
</div>

```

### Usage

| Class                          | Applied to                   | Outcome                                                                                                                                                                                                                          |
| ------------------------------ | ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-overflow-menu`          | `<div>`                      | Initiates an overflow menu. **Required**                                                                                                                                                                                         |
| `.pf-c-overflow-menu__content` | `<div>`                      | Initiates an overflow menu content section. **Required**                                                                                                                                                                         |
| `.pf-c-overflow-menu__control` | `<div>`                      | Initiates the overflow menu control. **Required**                                                                                                                                                                                |
| `.pf-c-overflow-menu__group`   | `<div>`                      | Initiates an overflow menu group.                                                                                                                                                                                                |
| `.pf-c-overflow-menu__item`    | `<div>`                      | Initiates an overflow menu item. **Required**                                                                                                                                                                                    |
| `.pf-m-button-group`           | `.pf-c-overflow-menu__group` | Modifies overflow menu group spacing. Spacer value is set to `var(--pf-c-overflow-menu__group--m-button-group--spacer)`. Child spacer value is set to `var(--pf-c-overflow-menu__group--m-button-group--space-items)`.           |
| `.pf-m-icon-button-group`      | `.pf-c-overflow-menu__group` | Modifies overflow menu group spacing. Spacer value is set to `var(--pf-c-overflow-menu__group--m-icon-button-group--spacer)`. Child spacer value is set to `var(--pf-c-overflow-menu__group--m-icon-button-group--space-items)`. |
