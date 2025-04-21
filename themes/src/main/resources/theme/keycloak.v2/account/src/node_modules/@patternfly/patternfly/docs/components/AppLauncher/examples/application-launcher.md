---
id: Application launcher
section: components
cssPrefix: pf-c-app-launcher
---import './application-launcher.css'

## Examples

### Collapsed

```html
<nav class="pf-c-app-launcher" aria-label="Application launcher">
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="false"
    aria-label="Application launcher"
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-app-launcher__menu" aria-labelledby="-button" hidden>
    <li>
      <a class="pf-c-app-launcher__menu-item" href="#">Link</a>
    </li>
    <li>
      <button class="pf-c-app-launcher__menu-item" type="button">Action</button>
    </li>
    <li class="pf-c-divider" role="separator"></li>
    <li>
      <a
        class="pf-c-app-launcher__menu-item pf-m-disabled"
        href="#"
        aria-disabled="true"
        tabindex="-1"
      >Disabled link</a>
    </li>
  </ul>
</nav>

```

### Disabled

```html
<nav class="pf-c-app-launcher" aria-label="Application launcher">
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="false"
    aria-label="Application launcher"
    disabled
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-app-launcher__menu" aria-labelledby="-button" hidden>
    <li>
      <a class="pf-c-app-launcher__menu-item" href="#">Link</a>
    </li>
    <li>
      <button class="pf-c-app-launcher__menu-item" type="button">Action</button>
    </li>
    <li class="pf-c-divider" role="separator"></li>
    <li>
      <a
        class="pf-c-app-launcher__menu-item pf-m-disabled"
        href="#"
        aria-disabled="true"
        tabindex="-1"
      >Disabled link</a>
    </li>
  </ul>
</nav>

```

### Expanded

```html
<nav class="pf-c-app-launcher pf-m-expanded" aria-label="Application launcher">
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="true"
    aria-label="Application launcher"
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-app-launcher__menu" aria-labelledby="-button">
    <li>
      <a class="pf-c-app-launcher__menu-item" href="#">Link</a>
    </li>
    <li>
      <button class="pf-c-app-launcher__menu-item" type="button">Action</button>
    </li>
    <li class="pf-c-divider" role="separator"></li>
    <li>
      <a
        class="pf-c-app-launcher__menu-item pf-m-disabled"
        href="#"
        aria-disabled="true"
        tabindex="-1"
      >Disabled link</a>
    </li>
  </ul>
</nav>

```

### Aligned right

```html
<nav class="pf-c-app-launcher pf-m-expanded" aria-label="Application launcher">
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="true"
    aria-label="Application launcher"
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <ul
    class="pf-c-app-launcher__menu pf-m-align-right"
    aria-labelledby="-button"
  >
    <li>
      <a class="pf-c-app-launcher__menu-item" href="#">Link</a>
    </li>
    <li>
      <button class="pf-c-app-launcher__menu-item" type="button">Action</button>
    </li>
    <li class="pf-c-divider" role="separator"></li>
    <li>
      <a
        class="pf-c-app-launcher__menu-item pf-m-disabled"
        href="#"
        aria-disabled="true"
        tabindex="-1"
      >Disabled link</a>
    </li>
  </ul>
</nav>

```

### Aligned top

```html
<nav
  class="pf-c-app-launcher pf-m-expanded pf-m-top"
  aria-label="Application launcher"
>
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="true"
    aria-label="Application launcher"
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-app-launcher__menu" aria-labelledby="-button">
    <li>
      <a class="pf-c-app-launcher__menu-item" href="#">Link</a>
    </li>
    <li>
      <button class="pf-c-app-launcher__menu-item" type="button">Action</button>
    </li>
    <li class="pf-c-divider" role="separator"></li>
    <li>
      <a
        class="pf-c-app-launcher__menu-item pf-m-disabled"
        href="#"
        aria-disabled="true"
        tabindex="-1"
      >Disabled link</a>
    </li>
  </ul>
</nav>

```

### With sections and dividers between sections

```html
<nav class="pf-c-app-launcher pf-m-expanded" aria-label="Application launcher">
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="true"
    aria-label="Application launcher"
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <div class="pf-c-app-launcher__menu">
    <section class="pf-c-app-launcher__group">
      <ul>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Link not in group</a>
        </li>
      </ul>
    </section>
    <hr class="pf-c-divider" />
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
      <ul>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Group 1 link</a>
        </li>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Group 1 link</a>
        </li>
      </ul>
    </section>
    <hr class="pf-c-divider" />
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Group 2</h1>
      <ul>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Group 2 link</a>
        </li>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Group 2 link</a>
        </li>
      </ul>
    </section>
  </div>
</nav>

```

### With sections and dividers between items

```html
<nav class="pf-c-app-launcher pf-m-expanded" aria-label="Application launcher">
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="true"
    aria-label="Application launcher"
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <div class="pf-c-app-launcher__menu">
    <section class="pf-c-app-launcher__group">
      <ul>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Link not in group</a>
        </li>
        <li class="pf-c-divider" role="separator"></li>
      </ul>
    </section>
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
      <ul>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Group 1 link</a>
        </li>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Group 1 link</a>
        </li>
        <li class="pf-c-divider" role="separator"></li>
      </ul>
    </section>
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Group 2</h1>
      <ul>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Group 2 link</a>
        </li>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">Group 2 link</a>
        </li>
      </ul>
    </section>
  </div>
</nav>

```

### With sections, dividers, icons, and external links

```html
<nav class="pf-c-app-launcher pf-m-expanded" aria-label="Application launcher">
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="true"
    aria-label="Application launcher"
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <div class="pf-c-app-launcher__menu">
    <section class="pf-c-app-launcher__group">
      <ul>
        <li>
          <a class="pf-c-app-launcher__menu-item" href="#">
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Link not in group
          </a>
        </li>
      </ul>
    </section>
    <li class="pf-c-divider" role="separator"></li>
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
      <ul>
        <li>
          <a
            class="pf-c-app-launcher__menu-item pf-m-external"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Group 1 link
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
        </li>
        <li>
          <a
            class="pf-c-app-launcher__menu-item pf-m-external"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Group 1 link
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
        </li>
        <li class="pf-c-divider" role="separator"></li>
      </ul>
    </section>
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Group 2</h1>
      <ul>
        <li>
          <a
            class="pf-c-app-launcher__menu-item pf-m-external"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Group 2 link
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
        </li>
        <li>
          <a
            class="pf-c-app-launcher__menu-item pf-m-external"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Group 2 link
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
        </li>
      </ul>
    </section>
  </div>
</nav>

```

### Favorites

```html
<nav class="pf-c-app-launcher pf-m-expanded" aria-label="Application launcher">
  <button
    class="pf-c-app-launcher__toggle"
    type="button"
    id="-button"
    aria-expanded="true"
    aria-label="Application launcher"
  >
    <i class="fas fa-th" aria-hidden="true"></i>
  </button>
  <div class="pf-c-app-launcher__menu">
    <div class="pf-c-app-launcher__menu-search">
      <div class="pf-c-search-input">
        <div class="pf-c-search-input__bar">
          <span class="pf-c-search-input__text">
            <span class="pf-c-search-input__icon">
              <i class="fas fa-search fa-fw" aria-hidden="true"></i>
            </span>
            <input
              class="pf-c-search-input__text-input"
              type="text"
              placeholder="Search"
              aria-label="Search"
            />
          </span>
        </div>
      </div>
    </div>
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
      <ul>
        <li class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite">
          <a
            class="pf-c-app-launcher__menu-item pf-m-link"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Link 2
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
          <button
            class="pf-c-app-launcher__menu-item pf-m-action"
            type="button"
            aria-label="Favorite"
          >
            <i class="fas fa-star" aria-hidden="true"></i>
          </button>
        </li>
        <li class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite">
          <a
            class="pf-c-app-launcher__menu-item pf-m-link"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Link 3
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
          <button
            class="pf-c-app-launcher__menu-item pf-m-action"
            type="button"
            aria-label="Favorite"
          >
            <i class="fas fa-star" aria-hidden="true"></i>
          </button>
        </li>
      </ul>
    </section>
    <hr class="pf-c-divider" />
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
      <ul>
        <li class="pf-c-app-launcher__menu-wrapper pf-m-external">
          <a
            class="pf-c-app-launcher__menu-item pf-m-link"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Link 1
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
          <button
            class="pf-c-app-launcher__menu-item pf-m-action"
            type="button"
            aria-label="Favorite"
          >
            <i class="fas fa-star" aria-hidden="true"></i>
          </button>
        </li>
        <li class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite">
          <a
            class="pf-c-app-launcher__menu-item pf-m-link"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Link 2
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
          <button
            class="pf-c-app-launcher__menu-item pf-m-action"
            type="button"
            aria-label="Favorite"
          >
            <i class="fas fa-star" aria-hidden="true"></i>
          </button>
        </li>
      </ul>
    </section>
    <hr class="pf-c-divider" />
    <section class="pf-c-app-launcher__group">
      <h1 class="pf-c-app-launcher__group-title">Group 2</h1>
      <ul>
        <li class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite">
          <a
            class="pf-c-app-launcher__menu-item pf-m-link"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Link 3
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
          <button
            class="pf-c-app-launcher__menu-item pf-m-action"
            type="button"
            aria-label="Favorite"
          >
            <i class="fas fa-star" aria-hidden="true"></i>
          </button>
        </li>
        <li class="pf-c-app-launcher__menu-wrapper pf-m-external">
          <a
            class="pf-c-app-launcher__menu-item pf-m-link"
            href="#"
            target="_blank"
          >
            <span class="pf-c-app-launcher__menu-item-icon">
              <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            </span>
            Link 4
            <span
              class="pf-c-app-launcher__menu-item-external-icon"
            >
              <i class="fas fa-external-link-alt" aria-hidden="true"></i>
            </span>
            <span class="pf-screen-reader">(opens new window)</span>
          </a>
          <button
            class="pf-c-app-launcher__menu-item pf-m-action"
            type="button"
            aria-label="Favorite"
          >
            <i class="fas fa-star" aria-hidden="true"></i>
          </button>
        </li>
      </ul>
    </section>
  </div>
</nav>

```

## Documentation

### Accessibility

| Attribute                           | Applied                                           | Outcome                                                                                                            |
| ----------------------------------- | ------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `aria-label="Application launcher"` | `.pf-c-app-launcher`                              | Gives the app launcher element an accessible name. **Required**                                                    |
| `aria-expanded="false"`             | `.pf-c-button`                                    | Indicates that the menu is hidden.                                                                                 |
| `aria-expanded="true"`              | `.pf-c-button`                                    | Indicates that the menu is visible.                                                                                |
| `aria-label="Actions"`              | `.pf-c-button`                                    | Provides an accessible name for the app launcher when an icon is used. **Required**                                |
| `hidden`                            | `.pf-c-app-launcher__menu`                        | Indicates that the menu is hidden so that it isn't visible in the UI and isn't accessed by assistive technologies. |
| `disabled`                          | `.pf-c-app-launcher__toggle`                      | Disables the app launcher toggle and removes it from keyboard focus.                                               |
| `disabled`                          | `button.pf-c-app-launcher__menu-item`             | When the menu item uses a button element, indicates that it is unavailable and removes it from keyboard focus.     |
| `aria-disabled="true"`              | `a.pf-c-app-launcher__menu-item`                  | When the menu item uses a link element, indicates that it is unavailable.                                          |
| `tabindex="-1"`                     | `a.pf-c-app-launcher__menu-item`                  | When the menu item uses a link element, removes it from keyboard focus.                                            |
| `aria-hidden="true"`                | `.pf-c-app-launcher__menu-item-external-icon > *` | Hides the icon from assistive technologies.                                                                        |

### Usage

| Class                                         | Applied                                                                      | Outcome                                                                                                |
| --------------------------------------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| `.pf-c-app-launcher`                          | `<nav>`                                                                      | Defines the parent wrapper of the app launcher.                                                        |
| `.pf-c-app-launcher__toggle`                  | `<button>`                                                                   | Defines the app launcher toggle.                                                                       |
| `.pf-c-app-launcher__menu`                    | `<ul>`, `<div>`                                                              | Defines the parent wrapper of the menu items. Use a `<div>` if your app launcher has groups.           |
| `.pf-c-app-launcher__menu-search`             | `<div>`                                                                      | Defines the wrapper for the search input.                                                              |
| `.pf-c-app-launcher__group`                   | `<section>`                                                                  | Defines a group of items. Required when there is more than one group.                                  |
| `.pf-c-app-launcher__group-title`             | `<h1>`                                                                       | Defines a title for a group of items.                                                                  |
| `.pf-c-app-launcher__menu-wrapper`            | `<li>`                                                                       | Defines a menu wrapper for use with multiple actionable items in a single item row.                    |
| `.pf-c-app-launcher__menu-item`               | `<a>`, `<button>`                                                            | Defines a menu item.                                                                                   |
| `.pf-c-app-launcher__menu-item-icon`          | `<span>`                                                                     | Defines the wrapper for the menu item icon.                                                            |
| `.pf-c-app-launcher__menu-item-external-icon` | `<span>`                                                                     | Defines the wrapper for the external link icon that appears on hover/focus. Use with `.pf-m-external`. |
| `.pf-m-expanded`                              | `.pf-c-app-launcher`                                                         | Modifies for the expanded state.                                                                       |
| `.pf-m-top`                                   | `.pf-c-app-launcher`                                                         | Modifies to display the menu above the toggle.                                                         |
| `.pf-m-align-right`                           | `.pf-c-app-launcher__menu`                                                   | Modifies to display the menu aligned to the right edge of the toggle.                                  |
| `.pf-m-disabled`                              | `a.pf-c-app-launcher__menu-item`                                             | Modifies to display the menu item as disabled.                                                         |
| `.pf-m-external`                              | `.pf-c-app-launcher__menu-item`                                              | Modifies to display the menu item as having an external link icon on hover/focus.                      |
| `.pf-m-favorite`                              | `.pf-c-app-launcher__menu-wrapper`                                           | Modifies wrapper to indicate that the item row has been favorited.                                     |
| `.pf-m-link`                                  | `.pf-c-app-launcher__menu-item.pf-m-wrapper > .pf-c-app-launcher__menu-item` | Modifies item for link styles.                                                                         |
| `.pf-m-action`                                | `.pf-c-app-launcher__menu-item.pf-m-wrapper > .pf-c-app-launcher__menu-item` | Modifies item to for action styles.                                                                    |
| `.pf-m-active`                                | `.pf-c-app-launcher__toggle`                                                 | Forces display of the active state of the toggle.                                                      |
