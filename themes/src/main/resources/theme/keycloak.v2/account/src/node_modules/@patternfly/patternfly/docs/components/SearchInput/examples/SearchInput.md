---
id: 'Search input'
beta: true
section: components
cssPrefix: pf-c-search-input
---import './SearchInput.css'

## Examples

### Basic

```html
<div class="pf-c-search-input">
  <div class="pf-c-search-input__bar">
    <span class="pf-c-search-input__text">
      <span class="pf-c-search-input__icon">
        <i class="fas fa-search fa-fw" aria-hidden="true"></i>
      </span>
      <input
        class="pf-c-search-input__text-input"
        type="text"
        placeholder="Find by name"
        aria-label="Find by name"
      />
    </span>
  </div>
</div>

```

### No match

```html
<div class="pf-c-search-input">
  <div class="pf-c-search-input__bar">
    <span class="pf-c-search-input__text">
      <span class="pf-c-search-input__icon">
        <i class="fas fa-search fa-fw" aria-hidden="true"></i>
      </span>
      <input
        class="pf-c-search-input__text-input"
        type="text"
        placeholder="Find by name"
        aria-label="Find by name"
        value="Joh"
      />
    </span>
    <span class="pf-c-search-input__utilities">
      <span class="pf-c-search-input__clear">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Clear">
          <i class="fas fa-times fa-fw" aria-hidden="true"></i>
        </button>
      </span>
    </span>
  </div>
</div>

```

### Match with result count

```html
<div class="pf-c-search-input">
  <div class="pf-c-search-input__bar">
    <span class="pf-c-search-input__text">
      <span class="pf-c-search-input__icon">
        <i class="fas fa-search fa-fw" aria-hidden="true"></i>
      </span>
      <input
        class="pf-c-search-input__text-input"
        type="text"
        placeholder="Find by name"
        aria-label="Find by name"
        value="John Doe"
      />
    </span>
    <span class="pf-c-search-input__utilities">
      <span class="pf-c-search-input__count">
        <span class="pf-c-badge pf-m-read">3</span>
      </span>
      <span class="pf-c-search-input__clear">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Clear">
          <i class="fas fa-times fa-fw" aria-hidden="true"></i>
        </button>
      </span>
    </span>
  </div>
</div>

```

### Match with navigable options

```html
<div class="pf-c-search-input">
  <div class="pf-c-search-input__bar">
    <span class="pf-c-search-input__text">
      <span class="pf-c-search-input__icon">
        <i class="fas fa-search fa-fw" aria-hidden="true"></i>
      </span>
      <input
        class="pf-c-search-input__text-input"
        type="text"
        placeholder="Find by name"
        aria-label="Find by name"
        value="John Doe"
      />
    </span>
    <span class="pf-c-search-input__utilities">
      <span class="pf-c-search-input__count">
        <span class="pf-c-badge pf-m-read">1 / 3</span>
      </span>
      <span class="pf-c-search-input__nav">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          disabled
          aria-label="Previous"
        >
          <i class="fas fa-angle-up fa-fw" aria-hidden="true"></i>
        </button>
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Next">
          <i class="fas fa-angle-down fa-fw" aria-hidden="true"></i>
        </button>
      </span>
      <span class="pf-c-search-input__clear">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Clear">
          <i class="fas fa-times fa-fw" aria-hidden="true"></i>
        </button>
      </span>
    </span>
  </div>
</div>

```

### With submit button

```html
<div class="pf-c-search-input">
  <div class="pf-c-input-group">
    <div class="pf-c-search-input__bar">
      <span class="pf-c-search-input__text">
        <span class="pf-c-search-input__icon">
          <i class="fas fa-search fa-fw" aria-hidden="true"></i>
        </span>
        <input
          class="pf-c-search-input__text-input"
          type="text"
          placeholder="Find by name"
          aria-label="Find by name"
        />
      </span>
    </div>
    <button class="pf-c-button pf-m-control" type="submit" aria-label="Search">
      <i class="fas fa-arrow-right" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Advanced search

```html
<div class="pf-c-search-input">
  <div class="pf-c-input-group">
    <div class="pf-c-search-input__bar">
      <span class="pf-c-search-input__text">
        <span class="pf-c-search-input__icon">
          <i class="fas fa-search fa-fw" aria-hidden="true"></i>
        </span>
        <input
          class="pf-c-search-input__text-input"
          type="text"
          placeholder="username:admin firstname:joe"
          aria-label="username:admin firstname:joe"
          value="username:root firstname:ned"
        />
      </span>
      <span class="pf-c-search-input__utilities">
        <span class="pf-c-search-input__clear">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Clear"
          >
            <i class="fas fa-times fa-fw" aria-hidden="true"></i>
          </button>
        </span>
      </span>
    </div>
    <button
      class="pf-c-button pf-m-control"
      type="button"
      aria-expanded="false"
      aria-label="Advanced search"
    >
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </button>
    <button class="pf-c-button pf-m-control" type="submit" aria-label="Search">
      <i class="fas fa-arrow-right" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Advanced search expanded

```html
<div class="pf-c-search-input">
  <div class="pf-c-input-group">
    <div class="pf-c-search-input__bar">
      <span class="pf-c-search-input__text">
        <span class="pf-c-search-input__icon">
          <i class="fas fa-search fa-fw" aria-hidden="true"></i>
        </span>
        <input
          class="pf-c-search-input__text-input"
          type="text"
          placeholder="username:admin firstname:joe"
          aria-label="username:admin firstname:joe"
          value="username:root firstname:ned"
        />
      </span>
      <span class="pf-c-search-input__utilities">
        <span class="pf-c-search-input__clear">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Clear"
          >
            <i class="fas fa-times fa-fw" aria-hidden="true"></i>
          </button>
        </span>
      </span>
    </div>
    <button
      class="pf-c-button pf-m-control pf-m-expanded"
      type="button"
      aria-expanded="true"
      aria-label="Advanced search"
    >
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </button>
    <button class="pf-c-button pf-m-control" type="submit" aria-label="Search">
      <i class="fas fa-arrow-right" aria-hidden="true"></i>
    </button>
  </div>
  <div class="pf-c-search-input__menu">
    <div class="pf-c-search-input__menu-body">
      <form novalidate class="pf-c-form">
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="advanced-search-input-form-username"
            >
              <span class="pf-c-form__label-text">Username</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              value="root"
              id="advanced-search-input-form-username"
              name="advanced-search-input-form-username"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="advanced-search-input-form-firstname"
            >
              <span class="pf-c-form__label-text">First name</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              value="ned"
              id="advanced-search-input-form-firstname"
              name="advanced-search-input-form-firstname"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="advanced-search-input-form-group"
            >
              <span class="pf-c-form__label-text">Group</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              id="advanced-search-input-form-group"
              name="advanced-search-input-form-group"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="advanced-search-input-form-email"
            >
              <span class="pf-c-form__label-text">Email</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              id="advanced-search-input-form-email"
              name="advanced-search-input-form-email"
            />
          </div>
        </div>
        <div class="pf-c-form__group pf-m-action">
          <div class="pf-c-form__actions">
            <button class="pf-c-button pf-m-primary" type="submit">Submit</button>
            <button class="pf-c-button pf-m-link" type="reset">Reset</button>
          </div>
        </div>
      </form>
    </div>
  </div>
</div>

```

### Autocomplete

```html
<div class="pf-c-search-input">
  <div class="pf-c-search-input__bar">
    <span class="pf-c-search-input__text">
      <span class="pf-c-search-input__icon">
        <i class="fas fa-search fa-fw" aria-hidden="true"></i>
      </span>
      <input
        class="pf-c-search-input__text-input"
        id="search-input-autocomplete-text-input"
        type="text"
        placeholder="Keyword search"
        aria-label="Keyword search"
        value="app"
      />
    </span>
    <span class="pf-c-search-input__utilities">
      <span class="pf-c-search-input__clear">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Clear">
          <i class="fas fa-times fa-fw" aria-hidden="true"></i>
        </button>
      </span>
    </span>
  </div>
  <div class="pf-c-search-input__menu">
    <ul class="pf-c-search-input__menu-list">
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">apple</span>
        </button>
      </li>
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">appleby</span>
        </button>
      </li>
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">appleseed</span>
        </button>
      </li>
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">appleton</span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Autocomplete last option hint

```html
<div class="pf-c-search-input">
  <div class="pf-c-search-input__bar">
    <span class="pf-c-search-input__text">
      <span class="pf-c-search-input__icon">
        <i class="fas fa-search fa-fw" aria-hidden="true"></i>
      </span>
      <input
        class="pf-c-search-input__text-input pf-m-hint"
        type="text"
        disabled
        aria-hidden="true"
        placeholder="Keyword search"
        aria-label="Keyword search"
        value="appleseed"
      />
      <input
        class="pf-c-search-input__text-input"
        type="text"
        placeholder="Keyword search"
        aria-label="Keyword search"
        value="apples"
      />
    </span>
    <span class="pf-c-search-input__utilities">
      <span class="pf-c-search-input__clear">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Clear">
          <i class="fas fa-times fa-fw" aria-hidden="true"></i>
        </button>
      </span>
    </span>
  </div>
  <div class="pf-c-search-input__menu">
    <ul class="pf-c-search-input__menu-list">
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">appleseed</span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Advanced search expanded with autocomplete

```html
<div class="pf-c-search-input">
  <div class="pf-c-input-group">
    <div class="pf-c-search-input__bar">
      <span class="pf-c-search-input__text">
        <span class="pf-c-search-input__icon">
          <i class="fas fa-search fa-fw" aria-hidden="true"></i>
        </span>
        <input
          class="pf-c-search-input__text-input"
          type="text"
          placeholder="username:admin firstname:joe"
          aria-label="username:admin firstname:joe"
          value="username:root firstname:n"
        />
      </span>
      <span class="pf-c-search-input__utilities">
        <span class="pf-c-search-input__clear">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Clear"
          >
            <i class="fas fa-times fa-fw" aria-hidden="true"></i>
          </button>
        </span>
      </span>
    </div>
    <button
      class="pf-c-button pf-m-control pf-m-expanded"
      type="button"
      aria-expanded="true"
      aria-label="Advanced search"
    >
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </button>
    <button class="pf-c-button pf-m-control" type="submit" aria-label="Search">
      <i class="fas fa-arrow-right" aria-hidden="true"></i>
    </button>
  </div>
  <div class="pf-c-search-input__menu">
    <div class="pf-c-search-input__menu-body">
      <form novalidate class="pf-c-form">
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="advanced-search-input-form-username"
            >
              <span class="pf-c-form__label-text">Username</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              value="root"
              id="advanced-search-input-form-username"
              name="advanced-search-input-form-username"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="advanced-search-input-form-firstname"
            >
              <span class="pf-c-form__label-text">First name</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              value="n"
              id="advanced-search-input-form-firstname"
              name="advanced-search-input-form-firstname"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="advanced-search-input-form-group"
            >
              <span class="pf-c-form__label-text">Group</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              id="advanced-search-input-form-group"
              name="advanced-search-input-form-group"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="advanced-search-input-form-email"
            >
              <span class="pf-c-form__label-text">Email</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              id="advanced-search-input-form-email"
              name="advanced-search-input-form-email"
            />
          </div>
        </div>
        <div class="pf-c-form__group pf-m-action">
          <div class="pf-c-form__actions">
            <button class="pf-c-button pf-m-primary" type="submit">Submit</button>
            <button class="pf-c-button pf-m-link" type="reset">Reset</button>
          </div>
        </div>
      </form>
    </div>
  </div>
  <div class="pf-c-search-input__menu">
    <ul class="pf-c-search-input__menu-list">
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">nancy</span>
        </button>
      </li>
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">ned</span>
        </button>
      </li>
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">neil</span>
        </button>
      </li>
      <li class="pf-c-search-input__menu-list-item">
        <button class="pf-c-search-input__menu-item" type="button">
          <span class="pf-c-search-input__menu-item-text">nicole</span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Accessibility

| Attributes                             | Applied to                                 | Outcome                                                                                                                    |
| -------------------------------------- | ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------- |
| `aria-hidden="true"`                   | `.pf-c-search-input__icon > *`             | Hides the search icon from assistive technologies. **Required**                                                            |
| `aria-label="Previous"`                | `.pf-c-search-input__nav > .pf-c-button`   | Provides an accessible label for the previous nav button. **Required**                                                     |
| `aria-label="Next"`                    | `.pf-c-search-input__nav > .pf-c-button`   | Provides an accessible label for the next nav button. **Required**                                                         |
| `aria-label="[descriptive text]"`      | `.pf-c-search-input__text-input`           | Provides an accessible label for the search input. **Required**                                                            |
| `aria-label="Clear"`                   | `.pf-c-search-input__clear > .pf-c-button` | Provides an accessible label for the clear button. **Required**                                                            |
| `aria-label="Search"`                  | `.pf-c-button`                             | Provides an accessible label for the search button. **Required**                                                           |
| `aria-label="Advanced search"`         | `.pf-c-button`                             | Provides an accessible label for the advanced search toggle. **Required**                                                  |
| `aria-expanded="[true/false]"`         | `.pf-c-button`                             | Indicates whether the advanced search menu is expanded or collapsed. **Required**                                          |
| `id`                                   | `.pf-c-search-input__text-input`           | Assigns an ID that is used with `aria-labelledby` on `.pf-c-search-input__menu-list`. **Required when using autocomplete** |
| `aria-labelledby="[id of text input]"` | `.pf-c-search-input__menu-list`            | Gives the menu list an accessible label. **Required when using autocomplete**                                              |
| `disabled`                             | `.pf-c-search-input__text-input.pf-m-hint` | Disables the hint text input from being submitted with the search input. **Required when using hint text**                 |
| `aria-hidden="true"`                   | `.pf-c-search-input__text-input.pf-m-hint` | Hides the hint text input from assistive technology. **Required when using hint text**                                     |

### Usage

| Class                                | Applied to                       | Outcome                                                                                   |
| ------------------------------------ | -------------------------------- | ----------------------------------------------------------------------------------------- |
| `.pf-c-search-input`                 | `<div>`                          | Initiates the custom search input component. **Required**                                 |
| `.pf-c-search-input__bar`            | `<div>`                          | Initiates the custom search input bar. **Required**                                       |
| `.pf-c-search-input__text`           | `<span>`                         | Initiates the text area. **Required**                                                     |
| `.pf-c-search-input__text-input`     | `<input>`                        | Initiates the search input. **Required**                                                  |
| `.pf-c-search-input__icon`           | `<span>`                         | Initiates the search icon container. **Required**                                         |
| `.pf-c-search-input__utilities`      | `<span>`                         | Initiates the utilities area beside the search input.                                     |
| `.pf-c-search-input__count`          | `<span>`                         | Initiates the item count container.                                                       |
| `.pf-c-search-input__nav`            | `<span>`                         | Initiates the navigable buttons container.                                                |
| `.pf-c-search-input__clear`          | `<span>`                         | Initiates the clear button container. **Required when there is text in the search input** |
| `.pf-c-search-input__menu`           | `<div>`                          | Initiates the search input dropdown menu.                                                 |
| `.pf-c-search-input__menu-body`      | `<div>`                          | Initiates the search input dropdown menu body element.                                    |
| `.pf-c-search-input__menu-list`      | `<div>`                          | Initiates the search input dropdown menu list.                                            |
| `.pf-c-search-input__menu-list-item` | `<div>`                          | Initiates the search input dropdown menu list item.                                       |
| `.pf-c-search-input__menu-item`      | `<div>`                          | Initiates the search input dropdown menu item.                                            |
| `.pf-c-search-input__menu-item-text` | `<span>`                         | Initiates the search input dropdown menu item text.                                       |
| `.pf-m-hint`                         | `.pf-c-search-input__text-input` | Modifies the text input for hint text styles.                                             |
