---
id: Card
section: components
cssPrefix: pf-c-card
---import './Card.css'

## Examples

### Basic

```html
<div class="pf-c-card" id="card-basic-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### With image and action

```html
<div class="pf-c-card" id="card-action-example-1">
  <div class="pf-c-card__header">
    <div class="pf-c-card__header-main">
      <img src="/assets/images/pf_logo.svg" width="300px" alt="Logo" />
    </div>
    <div class="pf-c-card__actions">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="card-action-example-1-dropdown-kebab-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="card-action-example-1-dropdown-kebab-button"
          hidden
        >
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Link</a>
          </li>
          <li>
            <button class="pf-c-dropdown__menu-item" type="button">Action</button>
          </li>
          <li>
            <a
              class="pf-c-dropdown__menu-item pf-m-disabled"
              href="#"
              aria-disabled="true"
              tabindex="-1"
            >Disabled link</a>
          </li>
          <li>
            <button
              class="pf-c-dropdown__menu-item"
              type="button"
              disabled
            >Disabled action</button>
          </li>
          <li class="pf-c-divider" role="separator"></li>
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
          </li>
        </ul>
      </div>
      <div class="pf-c-check pf-m-standalone">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="card-action-example-1-check"
          name="card-action-example-1-check"
          aria-label="card-action-example-1 checkbox"
        />
      </div>
    </div>
  </div>
  <div class="pf-c-card__title" id="card-action-example-1-check-label">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### With title in head

```html
<div class="pf-c-card" id="card-action-example-2">
  <div class="pf-c-card__header">
    <div class="pf-c-card__actions">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="card-action-example-2-dropdown-kebab-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="card-action-example-2-dropdown-kebab-button"
          hidden
        >
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Link</a>
          </li>
          <li>
            <button class="pf-c-dropdown__menu-item" type="button">Action</button>
          </li>
          <li>
            <a
              class="pf-c-dropdown__menu-item pf-m-disabled"
              href="#"
              aria-disabled="true"
              tabindex="-1"
            >Disabled link</a>
          </li>
          <li>
            <button
              class="pf-c-dropdown__menu-item"
              type="button"
              disabled
            >Disabled action</button>
          </li>
          <li class="pf-c-divider" role="separator"></li>
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
          </li>
        </ul>
      </div>
      <div class="pf-c-check pf-m-standalone">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="card-action-example-2-check"
          name="card-action-example-2-check"
          aria-label="card-action-example-2 checkbox"
        />
      </div>
    </div>
    <div
      class="pf-c-card__title"
      id="card-action-example-2-check-label"
    >This is a really really really really really really really really really really long title</div>
  </div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### With only actions in head (no title/footer)

```html
<div class="pf-c-card" id="card-action-example-3">
  <div class="pf-c-card__header">
    <div class="pf-c-card__actions">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="card-action-example-3-dropdown-kebab-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="card-action-example-3-dropdown-kebab-button"
          hidden
        >
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Link</a>
          </li>
          <li>
            <button class="pf-c-dropdown__menu-item" type="button">Action</button>
          </li>
          <li>
            <a
              class="pf-c-dropdown__menu-item pf-m-disabled"
              href="#"
              aria-disabled="true"
              tabindex="-1"
            >Disabled link</a>
          </li>
          <li>
            <button
              class="pf-c-dropdown__menu-item"
              type="button"
              disabled
            >Disabled action</button>
          </li>
          <li class="pf-c-divider" role="separator"></li>
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
          </li>
        </ul>
      </div>
      <div class="pf-c-check pf-m-standalone">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="card-action-example-3-check"
          name="card-action-example-3-check"
          aria-label="card-action-example-3 checkbox"
        />
      </div>
    </div>
  </div>
  <div
    class="pf-c-card__body"
    id="card-action-example-3-check-label"
  >This is the card body. There are only actions in the card head.</div>
</div>

```

### Actions with no offset

```html
<div class="pf-c-card" id="card-action-no-offset">
  <div class="pf-c-card__header">
    <div class="pf-c-card__actions pf-m-no-offset">
      <button class="pf-c-button pf-m-primary" type="button">Action</button>
    </div>
    <h1
      class="pf-c-title pf-m-2xl"
      id="card-action-no-offset-check-label"
    >This is a card title</h1>
  </div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### With only image in head

```html
<div class="pf-c-card" id="card-image-head-example">
  <div class="pf-c-card__header">
    <div class="pf-c-card__header-main">
      <img src="/assets/images/pf_logo.svg" width="300px" alt="Logo" />
    </div>
  </div>
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### With no footer

```html
<div class="pf-c-card" id="card-no-footer-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">This card has no footer</div>
</div>

```

### With no title

```html
<div class="pf-c-card" id="card-no-title-example">
  <div class="pf-c-card__body">This card has no title</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### With only a content section

```html
<div class="pf-c-card" id="card-body-example">
  <div class="pf-c-card__body">Body</div>
</div>

```

### With multiple body sections

```html
<div class="pf-c-card" id="card-multiple-body-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### With only one body that fills

```html
<div class="pf-c-card" id="card-body-fill-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body pf-m-no-fill">Body pf-m-no-fill</div>
  <div class="pf-c-card__body pf-m-no-fill">Body pf-m-no-fill</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Compact

```html
<div class="pf-c-card pf-m-compact" id="card-compact-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Large

```html
<div class="pf-c-card pf-m-display-lg" id="card-display-lg-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Hoverable

```html
<div class="pf-c-card pf-m-hoverable-raised" id="card-hoverable-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Selectable

```html
<div
  class="pf-c-card pf-m-selectable-raised"
  tabindex="0"
  id="card-selectable-example"
>
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Selected

```html
<div
  class="pf-c-card pf-m-selectable-raised pf-m-selected-raised"
  tabindex="0"
  id="card-selected-example"
>
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Selectable with a hidden input for improved screen reader accessibility

```html
<input
  class="pf-c-card__sr-input pf-screen-reader"
  type="checkbox"
  tabindex="-1"
  aria-label="Checkbox to improve screen reader accessibility of a selectable card"
/>
<div
  class="pf-c-card pf-m-selectable-raised"
  tabindex="0"
  id="card-selectable-with-input-example"
>
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Non selectable

```html
<div
  class="pf-c-card pf-m-non-selectable-raised"
  id="card-non-selectable-example"
>
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Hoverable (legacy)

```html
<div class="pf-c-card pf-m-hoverable" id="card-hoverable-legacy-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Selectable (legacy)

```html
<div
  class="pf-c-card pf-m-selectable"
  tabindex="0"
  id="card-selectable-legacy-example"
>
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Selected (legacy)

```html
<div
  class="pf-c-card pf-m-selectable pf-m-selected"
  tabindex="0"
  id="card-selected-legacy-example"
>
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Flat

```html
<div class="pf-c-card pf-m-flat" id="card-flat-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Rounded

```html
<div class="pf-c-card pf-m-rounded" id="card-rounded-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Plain

```html
<div class="pf-c-card pf-m-plain" id="card-plain-example">
  <div class="pf-c-card__title">Title</div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Expandable

```html
<div class="pf-c-card" id="card-expandable-example">
  <div class="pf-c-card__header">
    <div class="pf-c-card__header-toggle">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        id="card-expandable-example-toggle"
        aria-labelledby="card-expandable-example-title card-expandable-example-toggle"
      >
        <span class="pf-c-card__header-toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </div>
    <div class="pf-c-card__actions">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="card-expandable-example-dropdown-kebab-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="card-expandable-example-dropdown-kebab-button"
          hidden
        >
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Link</a>
          </li>
          <li>
            <button class="pf-c-dropdown__menu-item" type="button">Action</button>
          </li>
          <li>
            <a
              class="pf-c-dropdown__menu-item pf-m-disabled"
              href="#"
              aria-disabled="true"
              tabindex="-1"
            >Disabled link</a>
          </li>
          <li>
            <button
              class="pf-c-dropdown__menu-item"
              type="button"
              disabled
            >Disabled action</button>
          </li>
          <li class="pf-c-divider" role="separator"></li>
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
          </li>
        </ul>
      </div>
      <div class="pf-c-check pf-m-standalone">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="card-expandable-example-check"
          name="card-expandable-example-check"
          aria-label="card-expandable-example checkbox"
        />
      </div>
    </div>
    <div class="pf-c-card__title" id="card-expandable-example-title">Title</div>
  </div>
</div>

```

### Expandable with image

```html
<div class="pf-c-card" id="card-expandable-image-example">
  <div class="pf-c-card__header">
    <div class="pf-c-card__header-toggle">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        id="card-expandable-image-example-toggle"
        aria-labelledby="card-expandable-image-example-title card-expandable-image-example-toggle"
      >
        <span class="pf-c-card__header-toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </div>
    <img
      src="/assets/images/pf-logo-small.svg"
      alt="PatternFly logo"
      width="27px"
    />
    <div class="pf-c-card__actions">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="card-expandable-image-example-dropdown-kebab-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="card-expandable-image-example-dropdown-kebab-button"
          hidden
        >
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Link</a>
          </li>
          <li>
            <button class="pf-c-dropdown__menu-item" type="button">Action</button>
          </li>
          <li>
            <a
              class="pf-c-dropdown__menu-item pf-m-disabled"
              href="#"
              aria-disabled="true"
              tabindex="-1"
            >Disabled link</a>
          </li>
          <li>
            <button
              class="pf-c-dropdown__menu-item"
              type="button"
              disabled
            >Disabled action</button>
          </li>
          <li class="pf-c-divider" role="separator"></li>
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
          </li>
        </ul>
      </div>
      <div class="pf-c-check pf-m-standalone">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="card-expandable-image-example-check"
          name="card-expandable-image-example-check"
          aria-label="card-expandable-image-example checkbox"
        />
      </div>
    </div>
  </div>
</div>

```

### Expanded

```html
<div class="pf-c-card pf-m-expanded" id="card-expanded-example">
  <div class="pf-c-card__header">
    <div class="pf-c-card__header-toggle">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        id="card-expanded-example-toggle"
        aria-labelledby="card-expanded-example-title card-expanded-example-toggle"
      >
        <span class="pf-c-card__header-toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </div>
    <div class="pf-c-card__actions">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="card-expanded-example-dropdown-kebab-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="card-expanded-example-dropdown-kebab-button"
          hidden
        >
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Link</a>
          </li>
          <li>
            <button class="pf-c-dropdown__menu-item" type="button">Action</button>
          </li>
          <li>
            <a
              class="pf-c-dropdown__menu-item pf-m-disabled"
              href="#"
              aria-disabled="true"
              tabindex="-1"
            >Disabled link</a>
          </li>
          <li>
            <button
              class="pf-c-dropdown__menu-item"
              type="button"
              disabled
            >Disabled action</button>
          </li>
          <li class="pf-c-divider" role="separator"></li>
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
          </li>
        </ul>
      </div>
      <div class="pf-c-check pf-m-standalone">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="card-expanded-example-check"
          name="card-expanded-example-check"
          aria-label="card-expanded-example checkbox"
        />
      </div>
    </div>
    <div class="pf-c-card__title" id="card-expanded-example-title">Title</div>
  </div>
  <div class="pf-c-card__expandable-content">
    <div class="pf-c-card__body">Body</div>
    <div class="pf-c-card__footer">Footer</div>
  </div>
</div>

```

### Full height card

```html
<div class="pf-c-card pf-m-full-height" id="card-full-height-example">
  <div class="pf-c-card__header">
    <div class="pf-c-card__actions">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="card-full-height-example-dropdown-kebab-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="card-full-height-example-dropdown-kebab-button"
          hidden
        >
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Link</a>
          </li>
          <li>
            <button class="pf-c-dropdown__menu-item" type="button">Action</button>
          </li>
          <li>
            <a
              class="pf-c-dropdown__menu-item pf-m-disabled"
              href="#"
              aria-disabled="true"
              tabindex="-1"
            >Disabled link</a>
          </li>
          <li>
            <button
              class="pf-c-dropdown__menu-item"
              type="button"
              disabled
            >Disabled action</button>
          </li>
          <li class="pf-c-divider" role="separator"></li>
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
          </li>
        </ul>
      </div>
      <div class="pf-c-check pf-m-standalone">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="card-full-height-example-check"
          name="card-full-height-example-check"
          aria-label="card-full-height-example checkbox"
        />
      </div>
    </div>
    <div class="pf-c-card__title" id="card-full-height-example-title">Title</div>
  </div>
  <div class="pf-c-card__body">Body</div>
  <div class="pf-c-card__footer">Footer</div>
</div>

```

### Expandable toggle on right

```html
<div class="pf-c-card" id="card-toggle-on-right-example">
  <div class="pf-c-card__header pf-m-toggle-right">
    <div class="pf-c-card__actions">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="card-toggle-on-right-example-dropdown-kebab-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="card-toggle-on-right-example-dropdown-kebab-button"
          hidden
        >
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Link</a>
          </li>
          <li>
            <button class="pf-c-dropdown__menu-item" type="button">Action</button>
          </li>
          <li>
            <a
              class="pf-c-dropdown__menu-item pf-m-disabled"
              href="#"
              aria-disabled="true"
              tabindex="-1"
            >Disabled link</a>
          </li>
          <li>
            <button
              class="pf-c-dropdown__menu-item"
              type="button"
              disabled
            >Disabled action</button>
          </li>
          <li class="pf-c-divider" role="separator"></li>
          <li>
            <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
          </li>
        </ul>
      </div>
      <div class="pf-c-check pf-m-standalone">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="card-toggle-on-right-example-check"
          name="card-toggle-on-right-example-check"
          aria-label="card-toggle-on-right-example checkbox"
        />
      </div>
    </div>
    <div class="pf-c-card__title" id="card-toggle-on-right-example-title">Title</div>
    <div class="pf-c-card__header-toggle">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        id="card-toggle-on-right-example-toggle"
        aria-labelledby="card-toggle-on-right-example-title card-toggle-on-right-example-toggle"
      >
        <span class="pf-c-card__header-toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </div>
  </div>
</div>

```

### Card with dividers between sections

```html
<div class="pf-c-card">
  <div class="pf-c-card__title">Title</div>
  <hr class="pf-c-divider" />
  <div class="pf-c-card__body">Body</div>
  <hr class="pf-c-divider" />
  <div class="pf-c-card__body">Body</div>
  <hr class="pf-c-divider" />
  <div class="pf-c-card__footer">Footer</div>
</div>

```

## Documentation

### Overview

A card is a generic rectangular container that can be used to build other components. Use a default card for regular page content and the compact variation for dashboard or small cards.

### Accessibility

| Attribute      | Applied to                   | Outcome                                                                                          |
| -------------- | ---------------------------- | ------------------------------------------------------------------------------------------------ |
| `tabindex="0"` | `.pf-c-card.pf-m-selectable` | Inserts the selectable card into the tab order of the page so that it is focusable. **Required** |

### Usage

| Class                            | Applied                             | Outcome                                                                                                                                                                        |
| -------------------------------- | ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `.pf-c-card`                     | `<div>`                             | Creates a card component.  **Required**                                                                                                                                        |
| `.pf-c-card__title`              | `<div>`                             | Creates the title of a card.                                                                                                                                                   |
| `.pf-c-card__body`               | `<div>`                             | Creates the body of a card. By default, the body element fills the available space in the card. You can use multiple `.pf-c-card__body` elements.                              |
| `.pf-c-card__footer`             | `<div>`                             | Creates the footer of a card.                                                                                                                                                  |
| `.pf-c-card__header`             | `<div>`                             | Creates the header of the card where images, actions, and/or the card title can go.                                                                                            |
| `.pf-c-card__header-toggle`      | `<div>`                             | Creates the expandable card toggle.                                                                                                                                            |
| `.pf-c-card__header-toggle-icon` | `<span>`                            | Creates the expandable card toggle icon.                                                                                                                                       |
| `.pf-c-card__actions`            | `<div>`                             | Creates an actions element to be used in the card header.                                                                                                                      |
| `.pf-c-card__header-main`        | `<div>`                             | Creates a wrapper element to be used in the card header when using an image, logo, or text.                                                                                    |
| `.pf-c-card__expandable-content` | `<div>`                             | Creates the expandable card's expandable content.                                                                                                                              |
| `.pf-c-card__sr-input`           | `<input>`                           | Creates an input which, when focused, makes a following `.pf-c-card` appear focused.                                                                                           |
| `.pf-m-compact`                  | `.pf-c-card`                        | Creates a compact variation of the card component that involves smaller font sizes and spacing. This variation is for use on dashboards and where a smaller card is preferred. |
| `.pf-m-display-lg`               | `.pf-c-card`                        | Creates a large variation of the card component that involves larger font sizes and spacing. This variation is for marketing use cases.                                        |
| `.pf-m-no-fill`                  | `.pf-c-card__body`                  | Sets a `.pf-c-card__body` to not fill the available space in `.pf-c-card`. `.pf-m-no-fill` can be added to multiple card bodies.                                               |
| `.pf-m-hoverable-raised`         | `.pf-c-card`                        | Modifies the card to include hover styles on `:hover`.                                                                                                                         |
| `.pf-m-selectable-raised`        | `.pf-c-card`                        | Modifies a selectable card so that it is selectable.                                                                                                                           |
| `.pf-m-selected-raised`          | `.pf-c-card.pf-m-selectable-raised` | Modifies a selectable card for the selected state.                                                                                                                             |
| `.pf-m-non-selectable-raised`    | `.pf-c-card`                        | Modifies a selectable card so that it is not selectable.                                                                                                                       |
| `.pf-m-flat`                     | `.pf-c-card`                        | Modifies the card to have a border instead of a shadow. `.pf-m-flat` is for use in layouts where cards are against a white background.                                         |
| `.pf-m-rounded`                  | `.pf-c-card`                        | Modifies the card to have rounded corners.                                                                                                                                     |
| `.pf-m-plain`                    | `.pf-c-card`                        | Modifies the card to have no box shadow and no background color.                                                                                                               |
| `.pf-m-expanded`                 | `.pf-c-card`                        | Modifies the card for the expanded state.                                                                                                                                      |
| `.pf-m-toggle-right`             | `.pf-c-card__header`                | Modifies the expandable card header toggle to be positioned at flex-end.                                                                                                       |
| `.pf-m-full-height`              | `.pf-c-card`                        | Modifies the card to full height of its parent.                                                                                                                                |
| `.pf-m-no-offset`                | `.pf-c-card__actions`               | Removes the negative vertical margins on the actions element intended to align the action content with the card title.                                                         |
