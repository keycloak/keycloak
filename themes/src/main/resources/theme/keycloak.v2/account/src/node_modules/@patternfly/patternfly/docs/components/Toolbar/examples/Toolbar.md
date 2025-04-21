---
id: Toolbar
section: components
cssPrefix: pf-c-toolbar
---import './Toolbar.css'

## Introduction

Toolbar relies on groups (`.pf-c-toolbar__group`) and items (`.pf-c-toolbar__item`), with default spacer values. Groups and items can be siblings and/or items can be nested within groups. Modifier selectors adjust spacing based on the type of group or item. Each modifier applies a unique CSS variable, therefore, the base spacer value for all elements can be customized and item/groups spacers can be themed individually. The default spacer value for items and groups is set to `--pf-c-toolbar--spacer--base`, whose value is `--pf-global--spacer--md` or 16px.

### Default spacing for items and groups:

| Class                  | CSS Variable                    | Computed Value |
| ---------------------- | ------------------------------- | -------------- |
| `.pf-c-toolbar__item`  | `--pf-c-toolbar__item--spacer`  | `16px`         |
| `.pf-c-toolbar__group` | `--pf-c-toolbar__group--spacer` | `16px`         |

### Toolbar item types

| Class                 | Applied to            | Outcome                                                                                                      |
| --------------------- | --------------------- | ------------------------------------------------------------------------------------------------------------ |
| `.pf-m-bulk-select`   | `.pf-c-toolbar__item` | Initiates bulk select spacing. Spacer value is set to `var(--pf-c-toolbar--m-bulk-select--spacer)`.          |
| `.pf-m-overflow-menu` | `.pf-c-toolbar__item` | Initiates overflow menu spacing. Spacer value is set to `var(--pf-c-toolbar--m-overflow-menu--spacer)`.      |
| `.pf-m-pagination`    | `.pf-c-toolbar__item` | Initiates pagination spacing and margin. Spacer value is set to `var(--pf-c-toolbar--m-pagination--spacer)`. |
| `.pf-m-search-filter` | `.pf-c-toolbar__item` | Initiates search filter spacing. Spacer value is set to `var(--pf-c-toolbar--m-search-filter--spacer)`.      |

### Modifiers

| Class                                 | Applied to          | Outcome                                                                                                                                               |
| ------------------------------------- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-hidden{-on-[breakpoint]}`      | `.pf-c-toolbar > *` | Modifies toolbar element to be hidden, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).   |
| `.pf-m-visible{-on-[breakpoint]}`     | `.pf-c-toolbar > *` | Modifies toolbar element to be shown, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).    |
| `.pf-m-align-right{-on-[breakpoint]}` | `.pf-c-toolbar > *` | Modifies toolbar element to align right, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). |
| `.pf-m-align-left{-on-[breakpoint]}`  | `.pf-c-toolbar > *` | Modifies toolbar element to align left, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).  |

### Special notes

Several components in the following examples do not include functional and/or accessibility specifications (for example `.pf-c-select`, `.pf-c-options-menu`). Rather, `.pf-c-toolbar` focuses on functionality and accessibility specifications that apply to it only.

**Available [breakpoints](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) are: `-on-sm`, `-on-md`, `-on-lg`, `-on-xl`, and `-on-2xl`.**

## Examples

### Simple

```html
<div class="pf-c-toolbar" id="toolbar-simple-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__item">Item</div>
      <div class="pf-c-toolbar__item">Item</div>
      <div class="pf-c-toolbar__item">Item</div>
      <hr class="pf-c-divider pf-m-vertical" />
      <div class="pf-c-toolbar__group">
        <div class="pf-c-toolbar__item">Item</div>
        <div class="pf-c-toolbar__item">Item</div>
        <div class="pf-c-toolbar__item">Item</div>
      </div>
      <hr class="pf-c-divider pf-m-vertical" />
      <div class="pf-c-toolbar__item">Item</div>
      <div class="pf-c-toolbar__item">Item</div>
      <div class="pf-c-toolbar__item">Item</div>
    </div>
  </div>
</div>

```

### Item types

| Class                  | Applied to | Outcome                                            |
| ---------------------- | ---------- | -------------------------------------------------- |
| `.pf-c-toolbar__item`  | `<div>`    | Initiates the toolbar component item. **Required** |
| `.pf-c-toolbar__group` | `<div>`    | Initiates the toolbar component group.             |

### Spacers

In some instances, it may be necessary to adjust spacing explicitly where items are hidden/shown. For example, if a `.pf-m-toggle-group` is adjacent to an element being hidden/shown, the spacing may appear to be inconsistent. If possible, rely on modifier values. Available spacer modifiers are `.pf-m-spacer-{none, sm, md, lg}{-on-md, -on-lg, -on-xl}` and `.pf-m-space-items-{none, sm, md, lg}{-on-md, -on-lg, -on-xl}`. These modifiers will overwrite existing modifiers provided by `.pf-c-toolbar`.

### Adjusted spacers

```html
<div class="pf-c-toolbar" id="toolbar-spacer-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__item pf-m-spacer-none">Item</div>
      <div class="pf-c-toolbar__item pf-m-spacer-sm">Item</div>
      <div class="pf-c-toolbar__item pf-m-spacer-md">Item</div>
      <div class="pf-c-toolbar__item pf-m-spacer-lg">Item</div>
      <hr class="pf-c-divider pf-m-vertical" />
      <div
        class="pf-c-toolbar__item pf-m-spacer-none pf-m-spacer-sm-on-md pf-m-spacer-md-on-lg pf-m-spacer-lg-on-xl"
      >Item</div>
      <div class="pf-c-toolbar__item">Item</div>
    </div>
  </div>
</div>

```

### Adjusted group spacers

```html
<div class="pf-c-toolbar" id="toolbar-group-spacer-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-space-items-lg">
        <div class="pf-c-toolbar__item">Item</div>
        <div class="pf-c-toolbar__item">Item</div>
      </div>
      <hr class="pf-c-divider pf-m-vertical" />
      <div
        class="pf-c-toolbar__group pf-m-space-items-none pf-m-space-items-sm-on-md pf-m-space-items-md-on-lg pf-m-space-items-lg-on-xl"
      >
        <div class="pf-c-toolbar__item">Item</div>
        <div class="pf-c-toolbar__item">Item</div>
      </div>
    </div>
  </div>
</div>

```

### Insets

```html
<div
  class="pf-c-toolbar pf-m-inset-none pf-m-inset-md-on-md pf-m-inset-2xl-on-lg"
  id="toolbar-inset-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group">
        <div class="pf-c-toolbar__item">Item</div>
        <div class="pf-c-toolbar__item">Item</div>
      </div>
      <hr class="pf-c-divider pf-m-vertical" />
      <div class="pf-c-toolbar__item">Item</div>
      <div class="pf-c-toolbar__item">Item</div>
    </div>
  </div>
</div>

```

### Page insets

```html
<div class="pf-c-toolbar pf-m-page-insets" id="toolbar-page-insets-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group">
        <div class="pf-c-toolbar__item">Item</div>
        <div class="pf-c-toolbar__item">Item</div>
      </div>
      <hr class="pf-c-divider pf-m-vertical" />
      <div class="pf-c-toolbar__item">Item</div>
      <div class="pf-c-toolbar__item">Item</div>
    </div>
  </div>
</div>

```

### Toolbar spacers and insets

| Class                                                       | Applied to                                    | Outcome                                                                                                                                                                                         |
| ----------------------------------------------------------- | --------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-page-insets`                                         | `.pf-c-toolbar`                               | Modifies toolbar insets to match page section, table, page header or any other component whose inset shifts from `--pf-global--spacer--md` to `--pf-global--spacer--lg` at the `xl` breakpoint. |
| `.pf-m-spacer-{none, sm, md, lg}{-on-[breakpoint]}`         | `.pf-c-toolbar__group`, `.pf-c-toolbar__item` | Modifies toolbar group or item spacing at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                             |
| `.pf-m-space-items-{none, sm, md, lg}{-on-[breakpoint]}`    | `.pf-c-toolbar__group`                        | Modifies toolbar group child spacing at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                               |
| `.pf-m-inset-{none, sm, md, lg, xl, 2xl}{-on-[breakpoint]}` | `.pf-c-toolbar`                               | Modifies toolbar horizontal padding at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                |

### Width control

```html
<div class="pf-c-toolbar" id="toolbar-width-control">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group">
        <div
          class="pf-c-toolbar__item"
          style="--pf-c-toolbar__item--Width: 80px; --pf-c-toolbar__item--Width-on-xl: 10rem"
        >Item</div>
        <div class="pf-c-toolbar__item">Item</div>
      </div>
      <hr class="pf-c-divider pf-m-vertical" />
      <div class="pf-c-toolbar__item">Item</div>
      <div class="pf-c-toolbar__item">Item</div>
    </div>
  </div>
</div>

```

### Width control usage

| Class                                                       | Applied to            | Outcome                                                                                                                                                     |
| ----------------------------------------------------------- | --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `--pf-c-toolbar__item--Width{-on-[breakpoint]}: {width}`    | `.pf-c-toolbar__item` | Modifies the width value of a toolbar item at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).     |
| `--pf-c-toolbar__item--MinWidth{-on-[breakpoint]}: {width}` | `.pf-c-toolbar__item` | Modifies the min width value of a toolbar item at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). |

### Group types

```html
<div class="pf-c-toolbar" id="toolbar-group-types-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-filter-group">
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-group-types-example-select-checkbox-filter1-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-group-types-example-select-checkbox-filter1-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-group-types-example-select-checkbox-filter1-label toolbar-group-types-example-select-checkbox-filter1-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Filter 1</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <ul
              class="pf-c-select__menu"
              role="listbox"
              aria-labelledby="toolbar-group-types-example-select-checkbox-filter1-label"
              hidden
            >
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Running</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item pf-m-selected"
                  role="option"
                  aria-selected="true"
                >
                  Stopped
                  <span class="pf-c-select__menu-item-icon">
                    <i class="fas fa-check" aria-hidden="true"></i>
                  </span>
                </button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Down</button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Degraded</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item"
                  role="option"
                >Needs maintenance</button>
              </li>
            </ul>
          </div>
        </div>
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-group-types-example-select-checkbox-filter2-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-group-types-example-select-checkbox-filter2-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-group-types-example-select-checkbox-filter2-label toolbar-group-types-example-select-checkbox-filter2-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Filter 2</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <ul
              class="pf-c-select__menu"
              role="listbox"
              aria-labelledby="toolbar-group-types-example-select-checkbox-filter2-label"
              hidden
            >
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Running</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item pf-m-selected"
                  role="option"
                  aria-selected="true"
                >
                  Stopped
                  <span class="pf-c-select__menu-item-icon">
                    <i class="fas fa-check" aria-hidden="true"></i>
                  </span>
                </button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Down</button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Degraded</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item"
                  role="option"
                >Needs maintenance</button>
              </li>
            </ul>
          </div>
        </div>
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-group-types-example-select-checkbox-filter3-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-group-types-example-select-checkbox-filter3-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-group-types-example-select-checkbox-filter3-label toolbar-group-types-example-select-checkbox-filter3-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Filter 3</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <ul
              class="pf-c-select__menu"
              role="listbox"
              aria-labelledby="toolbar-group-types-example-select-checkbox-filter3-label"
              hidden
            >
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Running</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item pf-m-selected"
                  role="option"
                  aria-selected="true"
                >
                  Stopped
                  <span class="pf-c-select__menu-item-icon">
                    <i class="fas fa-check" aria-hidden="true"></i>
                  </span>
                </button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Down</button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Degraded</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item"
                  role="option"
                >Needs maintenance</button>
              </li>
            </ul>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-icon-button-group">
        <div class="pf-c-toolbar__item">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Edit"
          >
            <i class="fas fa-edit" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__item">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Clone"
          >
            <i class="fas fa-clone" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__item">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Sync"
          >
            <i class="fas fa-sync" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-button-group">
        <div class="pf-c-toolbar__item">
          <button class="pf-c-button pf-m-primary" type="button">Action</button>
        </div>
        <div class="pf-c-toolbar__item">
          <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
        </div>
        <div class="pf-c-toolbar__item">
          <button class="pf-c-button pf-m-tertiary" type="button">Tertiary</button>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Toolbar group types

| Class                     | Applied to             | Outcome                                                                                                                                                                                                   |
| ------------------------- | ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-filter-group`      | `.pf-c-toolbar__group` | Modifies toolbar group spacing. Spacer value is set to `var(--pf-c-toolbar__group--m-filter-group--spacer)`. Child spacer value is set to `var(--pf-c-toolbar__group--m-filter-group--space-items)`.      |
| `.pf-m-icon-button-group` | `.pf-c-toolbar__group` | Modifies toolbar group spacing. Spacer value is set to `var(--pf-c-toolbar__group--m-toggle-group--spacer)`. Child spacer value is set to `var(--pf-c-toolbar__group--m-icon-button-group--space-items)`. |
| `.pf-m-button-group`      | `.pf-c-toolbar__group` | Modifies toolbar group spacing. Spacer value is set to `var(--pf-c-toolbar__group--m-toggle-group--spacer)`. Child spacer value is set to `var(--pf-c-toolbar__group--m-button-group--space-items)`.      |

### Toggle group

```html
<div class="pf-c-toolbar" id="toolbar-toggle-group-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
        <div class="pf-c-toolbar__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="false"
            aria-controls="toolbar-toggle-group-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__item pf-m-search-filter">
          <div class="pf-c-input-group" aria-label="search filter" role="group">
            <div class="pf-c-select" style="width: 175px">
              <span
                id="toolbar-toggle-group-example-select-name-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-toggle-group-example-select-name-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-toggle-group-example-select-name-label toolbar-toggle-group-example-select-name-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-icon">
                    <i class="fas fa-filter" aria-hidden="true"></i>
                  </span>
                  <span class="pf-c-select__toggle-text">Name</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="toolbar-toggle-group-example-select-name-label"
                hidden
                style="width: 175px"
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Running</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    Stopped
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Down</button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Degraded</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item"
                    role="option"
                  >Needs maintenance</button>
                </li>
              </ul>
            </div>
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Filter by name"
                    aria-label="Filter by name"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__group pf-m-filter-group">
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <span
                id="toolbar-toggle-group-example-select-checkbox-status-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-toggle-group-example-select-checkbox-status-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-toggle-group-example-select-checkbox-status-label toolbar-toggle-group-example-select-checkbox-status-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Status</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-toggle-group-example-select-checkbox-status-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-status-active"
                      name="toolbar-toggle-group-example-select-checkbox-status-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-toggle-group-example-select-checkbox-status-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-status-canceled"
                      name="toolbar-toggle-group-example-select-checkbox-status-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-toggle-group-example-select-checkbox-status-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-status-paused"
                      name="toolbar-toggle-group-example-select-checkbox-status-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-toggle-group-example-select-checkbox-status-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-status-warning"
                      name="toolbar-toggle-group-example-select-checkbox-status-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-toggle-group-example-select-checkbox-status-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-status-restarted"
                      name="toolbar-toggle-group-example-select-checkbox-status-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <span
                id="toolbar-toggle-group-example-select-checkbox-risk-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-toggle-group-example-select-checkbox-risk-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-toggle-group-example-select-checkbox-risk-label toolbar-toggle-group-example-select-checkbox-risk-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Risk</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-toggle-group-example-select-checkbox-risk-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-risk-active"
                      name="toolbar-toggle-group-example-select-checkbox-risk-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-toggle-group-example-select-checkbox-risk-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-risk-canceled"
                      name="toolbar-toggle-group-example-select-checkbox-risk-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-toggle-group-example-select-checkbox-risk-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-risk-paused"
                      name="toolbar-toggle-group-example-select-checkbox-risk-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-toggle-group-example-select-checkbox-risk-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-risk-warning"
                      name="toolbar-toggle-group-example-select-checkbox-risk-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-toggle-group-example-select-checkbox-risk-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-toggle-group-example-select-checkbox-risk-restarted"
                      name="toolbar-toggle-group-example-select-checkbox-risk-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-hidden"
      id="toolbar-toggle-group-example-expandable-content"
      hidden
    ></div>
  </div>
</div>

```

### Toggle group on mobile (filters collapsed, expandable content expanded)

```html
<div class="pf-c-toolbar" id="toolbar-toggle-group-collapsed-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group">
        <div class="pf-c-toolbar__toggle pf-m-expanded">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="true"
            aria-controls="toolbar-toggle-group-collapsed-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-expanded"
      id="toolbar-toggle-group-collapsed-example-expandable-content"
    >
      <div class="pf-c-toolbar__item pf-m-search-filter">
        <div class="pf-c-input-group" aria-label="search filter" role="group">
          <div class="pf-c-select" style="width: 175px">
            <span
              id="toolbar-toggle-group-collapsed-example-select-name-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-toggle-group-collapsed-example-select-name-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-toggle-group-collapsed-example-select-name-label toolbar-toggle-group-collapsed-example-select-name-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-icon">
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </span>
                <span class="pf-c-select__toggle-text">Name</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <ul
              class="pf-c-select__menu"
              role="listbox"
              aria-labelledby="toolbar-toggle-group-collapsed-example-select-name-label"
              hidden
              style="width: 175px"
            >
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Running</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item pf-m-selected"
                  role="option"
                  aria-selected="true"
                >
                  Stopped
                  <span class="pf-c-select__menu-item-icon">
                    <i class="fas fa-check" aria-hidden="true"></i>
                  </span>
                </button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Down</button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Degraded</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item"
                  role="option"
                >Needs maintenance</button>
              </li>
            </ul>
          </div>
          <div class="pf-c-search-input">
            <div class="pf-c-search-input__bar">
              <span class="pf-c-search-input__text">
                <span class="pf-c-search-input__icon">
                  <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                </span>
                <input
                  class="pf-c-search-input__text-input"
                  type="text"
                  placeholder="Filter by name"
                  aria-label="Filter by name"
                />
              </span>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-filter-group">
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-label toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Status</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <div class="pf-c-select__menu" hidden>
              <fieldset
                class="pf-c-select__menu-fieldset"
                aria-label="Select input"
              >
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-active"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-active"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-active"
                  />

                  <span class="pf-c-check__label">Active</span>
                  <span class="pf-c-check__description">This is a description</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-canceled"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-canceled"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-canceled"
                  />

                  <span class="pf-c-check__label">Canceled</span>
                  <span
                    class="pf-c-check__description"
                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-paused"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-paused"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-paused"
                  />

                  <span class="pf-c-check__label">Paused</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-warning"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-warning"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-warning"
                  />

                  <span class="pf-c-check__label">Warning</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-restarted"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-restarted"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-status-expanded-restarted"
                  />

                  <span class="pf-c-check__label">Restarted</span>
                </label>
              </fieldset>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-label toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Risk</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <div class="pf-c-select__menu" hidden>
              <fieldset
                class="pf-c-select__menu-fieldset"
                aria-label="Select input"
              >
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-active"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-active"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-active"
                  />

                  <span class="pf-c-check__label">Active</span>
                  <span class="pf-c-check__description">This is a description</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-canceled"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-canceled"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-canceled"
                  />

                  <span class="pf-c-check__label">Canceled</span>
                  <span
                    class="pf-c-check__description"
                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-paused"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-paused"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-paused"
                  />

                  <span class="pf-c-check__label">Paused</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-warning"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-warning"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-warning"
                  />

                  <span class="pf-c-check__label">Warning</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-restarted"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-restarted"
                    name="toolbar-toggle-group-collapsed-example-select-checkbox-risk-expanded-restarted"
                  />

                  <span class="pf-c-check__label">Restarted</span>
                </label>
              </fieldset>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Toggle group modifier

The `.pf-m-toggle-group` controls when, and at which breakpoint, filters will be hidden/shown. By default, all filters are hidden until the specified breakpoint is reached. `.pf-m-show-on-{md, lg, xl}` controls when filters are shown and when the toggle button is hidden.

### Accessibility

| Attribute                                    | Applied to                                                                                                  | Outcome                                                                       |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `hidden`                                     | `.pf-c-toolbar__item`, `.pf-c-toolbar__group`, `.pf-c-toolbar__toggle`, `.pf-c-toolbar__expandable-content` | Indicates that the toggle group element is hidden. **Required**               |
| `aria-expanded="true"`                       | `.pf-c-toolbar__toggle > .pf-c-button`                                                                      | Indicates that the expandable content is visible. **Required**                |
| `aria-expanded="false"`                      | `.pf-c-toolbar__toggle > .pf-c-button`                                                                      | Indicates the the expandable content is hidden. **Required**                  |
| `aria-controls="[id of expandable content]"` | `.pf-c-toolbar__toggle > .pf-c-button`                                                                      | Identifies the expanded content controlled by the toggle button. **Required** |
| `id="[expandable-content_id]"`               | `.pf-c-toolbar__expandable-content`                                                                         | Provides a reference for toggle button description. **Required**              |

### Responsive attributes

| Attribute              | Applied to                             | Outcome                                                                                                                                                          |
| ---------------------- | -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-haspopup="true"` | `.pf-c-toolbar__toggle > .pf-c-button` | When expandable content appears above content (mobile viewport), `aria-haspopup="true"` should be applied to indicate that focus should be trapped. **Required** |

### Usage

| Class                          | Applied to                                                                    | Outcome                                                                                                                                                                                                                             |
| ------------------------------ | ----------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-show{-on-[breakpoint]}` | `.pf-c-toolbar__group.pf-m-toggle-group`, `.pf-c-toolbar__expandable-content` | Modifies toolbar element visibility at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). This selector must be applied consistently to toggle group and expandable content. |
| `.pf-m-chip-container`         | `.pf-c-toolbar__content-section`, `.pf-c-toolbar__group`                      | Modifies the toolbar element for applied filters layout.                                                                                                                                                                            |
| `.pf-m-expanded`               | `.pf-c-toolbar__expandable-content`, `.pf-c-toolbar__toggle`                  | Modifies the component for the expanded state.                                                                                                                                                                                      |

### Selected

### Selected filters on mobile (filters collapsed, selected filters summary visible)

```html
<div
  class="pf-c-toolbar"
  id="toolbar-selected-filters-toggle-group-collapsed-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__item pf-m-bulk-select">
        <div class="pf-c-dropdown">
          <div class="pf-c-dropdown__toggle pf-m-split-button">
            <label
              class="pf-c-dropdown__toggle-check"
              for="toolbar-selected-filters-toggle-group-collapsed-example-bulk-select-toggle-check"
            >
              <input
                type="checkbox"
                id="toolbar-selected-filters-toggle-group-collapsed-example-bulk-select-toggle-check"
                aria-label="Select all"
              />
            </label>

            <button
              class="pf-c-dropdown__toggle-button"
              type="button"
              aria-expanded="false"
              id="toolbar-selected-filters-toggle-group-collapsed-example-bulk-select-toggle-button"
              aria-label="Dropdown toggle"
            >
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </button>
          </div>
          <ul class="pf-c-dropdown__menu" hidden>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select all</button>
            </li>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select none</button>
            </li>
            <li>
              <button
                class="pf-c-dropdown__menu-item"
                type="button"
              >Other action</button>
            </li>
          </ul>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-toggle-group">
        <div class="pf-c-toolbar__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="false"
            aria-controls="toolbar-selected-filters-toggle-group-collapsed-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__item pf-m-search-filter">
          <div class="pf-c-input-group" aria-label="search filter" role="group">
            <div class="pf-c-select" style="width: 175px">
              <span
                id="toolbar-selected-filters-toggle-group-collapsed-example-select-name-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-selected-filters-toggle-group-collapsed-example-select-name-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-select-name-label toolbar-selected-filters-toggle-group-collapsed-example-select-name-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-icon">
                    <i class="fas fa-filter" aria-hidden="true"></i>
                  </span>
                  <span class="pf-c-select__toggle-text">Name</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-select-name-label"
                hidden
                style="width: 175px"
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Running</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    Stopped
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Down</button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Degraded</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item"
                    role="option"
                  >Needs maintenance</button>
                </li>
              </ul>
            </div>
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Filter by name"
                    aria-label="Filter by name"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__group pf-m-filter-group">
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <span
                id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-label toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Status</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-active"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-canceled"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-paused"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-warning"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-restarted"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-status-expanded-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <span
                id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-label toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Risk</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-active"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-canceled"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-paused"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-warning"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-restarted"
                      name="toolbar-selected-filters-toggle-group-collapsed-example-select-checkbox-risk-expanded-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-selected-filters-toggle-group-collapsed-example-icon-button-overflow-menu"
        >
          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-selected-filters-toggle-group-collapsed-example-icon-button-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-icon-button-overflow-menu-dropdown-toggle"
                hidden
              >
                <li>
                  <button class="pf-c-dropdown__menu-item">Edit</button>
                </li>
                <li>
                  <button class="pf-c-dropdown__menu-item">Clone</button>
                </li>
                <li>
                  <button class="pf-c-dropdown__menu-item">Sync</button>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-hidden"
      id="toolbar-selected-filters-toggle-group-collapsed-example-expandable-content"
      hidden
    >
      <div class="pf-c-toolbar__group pf-m-chip-container">
        <div class="pf-c-toolbar__item pf-m-chip-group">
          <div class="pf-c-chip-group pf-m-category">
            <div class="pf-c-chip-group__main">
              <span
                class="pf-c-chip-group__label"
                aria-hidden="true"
                id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-status-chip-group-label"
              >Status</span>
              <ul
                class="pf-c-chip-group__list"
                role="list"
                aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-status-chip-group-label"
              >
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span
                      class="pf-c-chip__text"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statuschip-one"
                    >Chip one</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statusremove-chip-one toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statuschip-one"
                      aria-label="Remove"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statusremove-chip-one"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </div>
                </li>
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span
                      class="pf-c-chip__text"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statuschip-two"
                    >Chip two</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statusremove-chip-two toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statuschip-two"
                      aria-label="Remove"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statusremove-chip-two"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </div>
                </li>
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span
                      class="pf-c-chip__text"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statuschip-three"
                    >Chip three</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statusremove-chip-three toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statuschip-three"
                      aria-label="Remove"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-statusremove-chip-three"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__item pf-m-chip-group">
          <div class="pf-c-chip-group pf-m-category">
            <div class="pf-c-chip-group__main">
              <span
                class="pf-c-chip-group__label"
                aria-hidden="true"
                id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-risk-chip-group-label"
              >Risk</span>
              <ul
                class="pf-c-chip-group__list"
                role="list"
                aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-risk-chip-group-label"
              >
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span
                      class="pf-c-chip__text"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskchip-one"
                    >Chip one</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskremove-chip-one toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskchip-one"
                      aria-label="Remove"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskremove-chip-one"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </div>
                </li>
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span
                      class="pf-c-chip__text"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskchip-two"
                    >Chip two</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskremove-chip-two toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskchip-two"
                      aria-label="Remove"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskremove-chip-two"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </div>
                </li>
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span
                      class="pf-c-chip__text"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskchip-three"
                    >Chip three</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskremove-chip-three toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskchip-three"
                      aria-label="Remove"
                      id="toolbar-selected-filters-toggle-group-collapsed-example-chip-group-riskremove-chip-three"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__item">6 filters applied</div>
    <div class="pf-c-toolbar__item">
      <button
        class="pf-c-button pf-m-link pf-m-inline"
        type="button"
      >Clear all filters</button>
    </div>
  </div>
</div>

```

### Selected filters on mobile (filters collapsed, expandable content expanded)

```html
<div
  class="pf-c-toolbar"
  id="toolbar-selected-filters-toggle-group-expanded-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__item pf-m-bulk-select">
        <div class="pf-c-dropdown">
          <div class="pf-c-dropdown__toggle pf-m-split-button">
            <label
              class="pf-c-dropdown__toggle-check"
              for="toolbar-selected-filters-toggle-group-expanded-example-bulk-select-toggle-check"
            >
              <input
                type="checkbox"
                id="toolbar-selected-filters-toggle-group-expanded-example-bulk-select-toggle-check"
                aria-label="Select all"
              />
            </label>

            <button
              class="pf-c-dropdown__toggle-button"
              type="button"
              aria-expanded="false"
              id="toolbar-selected-filters-toggle-group-expanded-example-bulk-select-toggle-button"
              aria-label="Dropdown toggle"
            >
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </button>
          </div>
          <ul class="pf-c-dropdown__menu" hidden>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select all</button>
            </li>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select none</button>
            </li>
            <li>
              <button
                class="pf-c-dropdown__menu-item"
                type="button"
              >Other action</button>
            </li>
          </ul>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-toggle-group">
        <div class="pf-c-toolbar__toggle pf-m-expanded">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="true"
            aria-controls="toolbar-selected-filters-toggle-group-expanded-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-selected-filters-toggle-group-expanded-example-icon-button-overflow-menu"
        >
          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-selected-filters-toggle-group-expanded-example-icon-button-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-icon-button-overflow-menu-dropdown-toggle"
                hidden
              >
                <li>
                  <button class="pf-c-dropdown__menu-item">Edit</button>
                </li>
                <li>
                  <button class="pf-c-dropdown__menu-item">Clone</button>
                </li>
                <li>
                  <button class="pf-c-dropdown__menu-item">Sync</button>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-expanded"
      id="toolbar-selected-filters-toggle-group-expanded-example-expandable-content"
    >
      <div class="pf-c-toolbar__item pf-m-search-filter">
        <div class="pf-c-input-group" aria-label="search filter" role="group">
          <div class="pf-c-select" style="width: 175px">
            <span
              id="toolbar-selected-filters-toggle-group-expanded-example-select-name-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-selected-filters-toggle-group-expanded-example-select-name-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-select-name-label toolbar-selected-filters-toggle-group-expanded-example-select-name-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-icon">
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </span>
                <span class="pf-c-select__toggle-text">Name</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <ul
              class="pf-c-select__menu"
              role="listbox"
              aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-select-name-label"
              hidden
              style="width: 175px"
            >
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Running</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item pf-m-selected"
                  role="option"
                  aria-selected="true"
                >
                  Stopped
                  <span class="pf-c-select__menu-item-icon">
                    <i class="fas fa-check" aria-hidden="true"></i>
                  </span>
                </button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Down</button>
              </li>
              <li role="presentation">
                <button class="pf-c-select__menu-item" role="option">Degraded</button>
              </li>
              <li role="presentation">
                <button
                  class="pf-c-select__menu-item"
                  role="option"
                >Needs maintenance</button>
              </li>
            </ul>
          </div>
          <div class="pf-c-search-input">
            <div class="pf-c-search-input__bar">
              <span class="pf-c-search-input__text">
                <span class="pf-c-search-input__icon">
                  <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                </span>
                <input
                  class="pf-c-search-input__text-input"
                  type="text"
                  placeholder="Filter by name"
                  aria-label="Filter by name"
                />
              </span>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-filter-group">
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-label toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Status</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <div class="pf-c-select__menu" hidden>
              <fieldset
                class="pf-c-select__menu-fieldset"
                aria-label="Select input"
              >
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-active"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-active"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-active"
                  />

                  <span class="pf-c-check__label">Active</span>
                  <span class="pf-c-check__description">This is a description</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-canceled"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-canceled"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-canceled"
                  />

                  <span class="pf-c-check__label">Canceled</span>
                  <span
                    class="pf-c-check__description"
                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-paused"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-paused"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-paused"
                  />

                  <span class="pf-c-check__label">Paused</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-warning"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-warning"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-warning"
                  />

                  <span class="pf-c-check__label">Warning</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-restarted"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-restarted"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-status-expanded-restarted"
                  />

                  <span class="pf-c-check__label">Restarted</span>
                </label>
              </fieldset>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-label toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Risk</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <div class="pf-c-select__menu" hidden>
              <fieldset
                class="pf-c-select__menu-fieldset"
                aria-label="Select input"
              >
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-active"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-active"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-active"
                  />

                  <span class="pf-c-check__label">Active</span>
                  <span class="pf-c-check__description">This is a description</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-canceled"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-canceled"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-canceled"
                  />

                  <span class="pf-c-check__label">Canceled</span>
                  <span
                    class="pf-c-check__description"
                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-paused"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-paused"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-paused"
                  />

                  <span class="pf-c-check__label">Paused</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-warning"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-warning"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-warning"
                  />

                  <span class="pf-c-check__label">Warning</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-restarted"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-restarted"
                    name="toolbar-selected-filters-toggle-group-expanded-example-select-checkbox-risk-expanded-restarted"
                  />

                  <span class="pf-c-check__label">Restarted</span>
                </label>
              </fieldset>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-chip-container">
        <div class="pf-c-toolbar__group">
          <div class="pf-c-toolbar__item pf-m-chip-group">
            <div class="pf-c-chip-group pf-m-category">
              <div class="pf-c-chip-group__main">
                <span
                  class="pf-c-chip-group__label"
                  aria-hidden="true"
                  id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-status-chip-group-label"
                >Status</span>
                <ul
                  class="pf-c-chip-group__list"
                  role="list"
                  aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-chip-group-status-chip-group-label"
                >
                  <li class="pf-c-chip-group__list-item">
                    <div class="pf-c-chip">
                      <span
                        class="pf-c-chip__text"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statuschip-one"
                      >Chip one</span>
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statusremove-chip-one toolbar-selected-filters-toggle-group-expanded-example-chip-group-statuschip-one"
                        aria-label="Remove"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statusremove-chip-one"
                      >
                        <i class="fas fa-times" aria-hidden="true"></i>
                      </button>
                    </div>
                  </li>
                  <li class="pf-c-chip-group__list-item">
                    <div class="pf-c-chip">
                      <span
                        class="pf-c-chip__text"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statuschip-two"
                      >Chip two</span>
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statusremove-chip-two toolbar-selected-filters-toggle-group-expanded-example-chip-group-statuschip-two"
                        aria-label="Remove"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statusremove-chip-two"
                      >
                        <i class="fas fa-times" aria-hidden="true"></i>
                      </button>
                    </div>
                  </li>
                  <li class="pf-c-chip-group__list-item">
                    <div class="pf-c-chip">
                      <span
                        class="pf-c-chip__text"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statuschip-three"
                      >Chip three</span>
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statusremove-chip-three toolbar-selected-filters-toggle-group-expanded-example-chip-group-statuschip-three"
                        aria-label="Remove"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-statusremove-chip-three"
                      >
                        <i class="fas fa-times" aria-hidden="true"></i>
                      </button>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-chip-group">
            <div class="pf-c-chip-group pf-m-category">
              <div class="pf-c-chip-group__main">
                <span
                  class="pf-c-chip-group__label"
                  aria-hidden="true"
                  id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-risk-chip-group-label"
                >Risk</span>
                <ul
                  class="pf-c-chip-group__list"
                  role="list"
                  aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-chip-group-risk-chip-group-label"
                >
                  <li class="pf-c-chip-group__list-item">
                    <div class="pf-c-chip">
                      <span
                        class="pf-c-chip__text"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskchip-one"
                      >Chip one</span>
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskremove-chip-one toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskchip-one"
                        aria-label="Remove"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskremove-chip-one"
                      >
                        <i class="fas fa-times" aria-hidden="true"></i>
                      </button>
                    </div>
                  </li>
                  <li class="pf-c-chip-group__list-item">
                    <div class="pf-c-chip">
                      <span
                        class="pf-c-chip__text"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskchip-two"
                      >Chip two</span>
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskremove-chip-two toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskchip-two"
                        aria-label="Remove"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskremove-chip-two"
                      >
                        <i class="fas fa-times" aria-hidden="true"></i>
                      </button>
                    </div>
                  </li>
                  <li class="pf-c-chip-group__list-item">
                    <div class="pf-c-chip">
                      <span
                        class="pf-c-chip__text"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskchip-three"
                      >Chip three</span>
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-labelledby="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskremove-chip-three toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskchip-three"
                        aria-label="Remove"
                        id="toolbar-selected-filters-toggle-group-expanded-example-chip-group-riskremove-chip-three"
                      >
                        <i class="fas fa-times" aria-hidden="true"></i>
                      </button>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__item">
          <button
            class="pf-c-button pf-m-link pf-m-inline"
            type="button"
          >Clear all filters</button>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Selected filters on desktop (not responsive)

```html
<div class="pf-c-toolbar" id="toolbar-selected-filters-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__item pf-m-bulk-select">
        <div class="pf-c-dropdown">
          <div class="pf-c-dropdown__toggle pf-m-split-button">
            <label
              class="pf-c-dropdown__toggle-check"
              for="toolbar-selected-filters-example-bulk-select-toggle-check"
            >
              <input
                type="checkbox"
                id="toolbar-selected-filters-example-bulk-select-toggle-check"
                aria-label="Select all"
              />
            </label>

            <button
              class="pf-c-dropdown__toggle-button"
              type="button"
              aria-expanded="false"
              id="toolbar-selected-filters-example-bulk-select-toggle-button"
              aria-label="Dropdown toggle"
            >
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </button>
          </div>
          <ul class="pf-c-dropdown__menu" hidden>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select all</button>
            </li>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select none</button>
            </li>
            <li>
              <button
                class="pf-c-dropdown__menu-item"
                type="button"
              >Other action</button>
            </li>
          </ul>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show">
        <div class="pf-c-toolbar__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="false"
            aria-controls="toolbar-selected-filters-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__group pf-m-filter-group">
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <span
                id="toolbar-selected-filters-example-select-checkbox-status-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-selected-filters-example-select-checkbox-status-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-selected-filters-example-select-checkbox-status-label toolbar-selected-filters-example-select-checkbox-status-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Status</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-selected-filters-example-select-checkbox-status-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-status-active"
                      name="toolbar-selected-filters-example-select-checkbox-status-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-selected-filters-example-select-checkbox-status-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-status-canceled"
                      name="toolbar-selected-filters-example-select-checkbox-status-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-example-select-checkbox-status-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-status-paused"
                      name="toolbar-selected-filters-example-select-checkbox-status-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-example-select-checkbox-status-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-status-warning"
                      name="toolbar-selected-filters-example-select-checkbox-status-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-example-select-checkbox-status-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-status-restarted"
                      name="toolbar-selected-filters-example-select-checkbox-status-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <span
                id="toolbar-selected-filters-example-select-checkbox-risk-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-selected-filters-example-select-checkbox-risk-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-selected-filters-example-select-checkbox-risk-label toolbar-selected-filters-example-select-checkbox-risk-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Risk</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-selected-filters-example-select-checkbox-risk-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-risk-active"
                      name="toolbar-selected-filters-example-select-checkbox-risk-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="toolbar-selected-filters-example-select-checkbox-risk-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-risk-canceled"
                      name="toolbar-selected-filters-example-select-checkbox-risk-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-example-select-checkbox-risk-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-risk-paused"
                      name="toolbar-selected-filters-example-select-checkbox-risk-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-example-select-checkbox-risk-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-risk-warning"
                      name="toolbar-selected-filters-example-select-checkbox-risk-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-selected-filters-example-select-checkbox-risk-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-selected-filters-example-select-checkbox-risk-restarted"
                      name="toolbar-selected-filters-example-select-checkbox-risk-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-selected-filters-example-icon-button-overflow-menu"
        >
          <div class="pf-c-overflow-menu__content">
            <div class="pf-c-overflow-menu__group pf-m-icon-button-group">
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Edit"
                >
                  <i class="fas fa-edit" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Clone"
                >
                  <i class="fas fa-clone" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Sync"
                >
                  <i class="fas fa-sync" aria-hidden="true"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-selected-filters-example-overflow-menu"
        >
          <div class="pf-c-overflow-menu__content">
            <div class="pf-c-overflow-menu__group pf-m-button-group">
              <div class="pf-c-overflow-menu__item">
                <button class="pf-c-button pf-m-primary" type="button">Primary</button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-secondary"
                  type="button"
                >Secondary</button>
              </div>
            </div>
          </div>

          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-selected-filters-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-selected-filters-example-overflow-menu-dropdown-toggle"
                hidden
              >
                <li>
                  <button class="pf-c-dropdown__menu-item">Tertiary</button>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-toolbar__content pf-m-chip-container">
    <div class="pf-c-toolbar__group">
      <div class="pf-c-toolbar__item pf-m-chip-group">
        <div class="pf-c-chip-group pf-m-category">
          <div class="pf-c-chip-group__main">
            <span
              class="pf-c-chip-group__label"
              aria-hidden="true"
              id="toolbar-selected-filters-example-chip-group-status-chip-group-label"
            >Status</span>
            <ul
              class="pf-c-chip-group__list"
              role="list"
              aria-labelledby="toolbar-selected-filters-example-chip-group-status-chip-group-label"
            >
              <li class="pf-c-chip-group__list-item">
                <div class="pf-c-chip">
                  <span
                    class="pf-c-chip__text"
                    id="toolbar-selected-filters-example-chip-group-statuschip-one"
                  >Chip one</span>
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="toolbar-selected-filters-example-chip-group-statusremove-chip-one toolbar-selected-filters-example-chip-group-statuschip-one"
                    aria-label="Remove"
                    id="toolbar-selected-filters-example-chip-group-statusremove-chip-one"
                  >
                    <i class="fas fa-times" aria-hidden="true"></i>
                  </button>
                </div>
              </li>
              <li class="pf-c-chip-group__list-item">
                <div class="pf-c-chip">
                  <span
                    class="pf-c-chip__text"
                    id="toolbar-selected-filters-example-chip-group-statuschip-two"
                  >Chip two</span>
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="toolbar-selected-filters-example-chip-group-statusremove-chip-two toolbar-selected-filters-example-chip-group-statuschip-two"
                    aria-label="Remove"
                    id="toolbar-selected-filters-example-chip-group-statusremove-chip-two"
                  >
                    <i class="fas fa-times" aria-hidden="true"></i>
                  </button>
                </div>
              </li>
              <li class="pf-c-chip-group__list-item">
                <div class="pf-c-chip">
                  <span
                    class="pf-c-chip__text"
                    id="toolbar-selected-filters-example-chip-group-statuschip-three"
                  >Chip three</span>
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="toolbar-selected-filters-example-chip-group-statusremove-chip-three toolbar-selected-filters-example-chip-group-statuschip-three"
                    aria-label="Remove"
                    id="toolbar-selected-filters-example-chip-group-statusremove-chip-three"
                  >
                    <i class="fas fa-times" aria-hidden="true"></i>
                  </button>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-chip-group">
        <div class="pf-c-chip-group pf-m-category">
          <div class="pf-c-chip-group__main">
            <span
              class="pf-c-chip-group__label"
              aria-hidden="true"
              id="toolbar-selected-filters-example-chip-group-risk-chip-group-label"
            >Risk</span>
            <ul
              class="pf-c-chip-group__list"
              role="list"
              aria-labelledby="toolbar-selected-filters-example-chip-group-risk-chip-group-label"
            >
              <li class="pf-c-chip-group__list-item">
                <div class="pf-c-chip">
                  <span
                    class="pf-c-chip__text"
                    id="toolbar-selected-filters-example-chip-group-riskchip-one"
                  >Chip one</span>
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="toolbar-selected-filters-example-chip-group-riskremove-chip-one toolbar-selected-filters-example-chip-group-riskchip-one"
                    aria-label="Remove"
                    id="toolbar-selected-filters-example-chip-group-riskremove-chip-one"
                  >
                    <i class="fas fa-times" aria-hidden="true"></i>
                  </button>
                </div>
              </li>
              <li class="pf-c-chip-group__list-item">
                <div class="pf-c-chip">
                  <span
                    class="pf-c-chip__text"
                    id="toolbar-selected-filters-example-chip-group-riskchip-two"
                  >Chip two</span>
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="toolbar-selected-filters-example-chip-group-riskremove-chip-two toolbar-selected-filters-example-chip-group-riskchip-two"
                    aria-label="Remove"
                    id="toolbar-selected-filters-example-chip-group-riskremove-chip-two"
                  >
                    <i class="fas fa-times" aria-hidden="true"></i>
                  </button>
                </div>
              </li>
              <li class="pf-c-chip-group__list-item">
                <div class="pf-c-chip">
                  <span
                    class="pf-c-chip__text"
                    id="toolbar-selected-filters-example-chip-group-riskchip-three"
                  >Chip three</span>
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="toolbar-selected-filters-example-chip-group-riskremove-chip-three toolbar-selected-filters-example-chip-group-riskchip-three"
                    aria-label="Remove"
                    id="toolbar-selected-filters-example-chip-group-riskremove-chip-three"
                  >
                    <i class="fas fa-times" aria-hidden="true"></i>
                  </button>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
    <div class="pf-c-toolbar__item">
      <button
        class="pf-c-button pf-m-link pf-m-inline"
        type="button"
      >Clear all filters</button>
    </div>
  </div>
</div>

```

### Stacked

### Stacked on desktop

```html
<div class="pf-c-toolbar" id="toolbar-stacked-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-2xl">
        <div class="pf-c-toolbar__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="false"
            aria-controls="toolbar-stacked-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__group">
          <div
            class="pf-c-toolbar__item pf-m-label"
            id="-select-checkbox-resource-label"
            aria-hidden="true"
          >Resource</div>
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <button
                class="pf-c-select__toggle"
                type="button"
                id="-select-checkbox-resource-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="-select-checkbox-resource-label -select-checkbox-resource-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Pod</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="-select-checkbox-resource-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-resource-active"
                      name="-select-checkbox-resource-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="-select-checkbox-resource-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-resource-canceled"
                      name="-select-checkbox-resource-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-resource-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-resource-paused"
                      name="-select-checkbox-resource-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-resource-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-resource-warning"
                      name="-select-checkbox-resource-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-resource-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-resource-restarted"
                      name="-select-checkbox-resource-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__group">
          <div
            class="pf-c-toolbar__item pf-m-label"
            id="-select-checkbox-status-label"
            aria-hidden="true"
          >Status</div>
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <button
                class="pf-c-select__toggle"
                type="button"
                id="-select-checkbox-status-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="-select-checkbox-status-label -select-checkbox-status-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Running</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="-select-checkbox-status-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-status-active"
                      name="-select-checkbox-status-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="-select-checkbox-status-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-status-canceled"
                      name="-select-checkbox-status-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-status-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-status-paused"
                      name="-select-checkbox-status-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-status-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-status-warning"
                      name="-select-checkbox-status-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-status-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-status-restarted"
                      name="-select-checkbox-status-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__group">
          <div
            class="pf-c-toolbar__item pf-m-label"
            id="-select-checkbox-type-label"
            aria-hidden="true"
          >Type</div>
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select">
              <button
                class="pf-c-select__toggle"
                type="button"
                id="-select-checkbox-type-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="-select-checkbox-type-label -select-checkbox-type-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Any</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu" hidden>
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="-select-checkbox-type-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-type-active"
                      name="-select-checkbox-type-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                    <span class="pf-c-check__description">This is a description</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                    for="-select-checkbox-type-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-type-canceled"
                      name="-select-checkbox-type-canceled"
                    />

                    <span class="pf-c-check__label">Canceled</span>
                    <span
                      class="pf-c-check__description"
                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-type-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-type-paused"
                      name="-select-checkbox-type-paused"
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-type-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-type-warning"
                      name="-select-checkbox-type-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="-select-checkbox-type-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="-select-checkbox-type-restarted"
                      name="-select-checkbox-type-restarted"
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-stacked-example-icon-button-overflow-menu"
        >
          <div class="pf-c-overflow-menu__content">
            <div class="pf-c-overflow-menu__group pf-m-icon-button-group">
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Edit"
                >
                  <i class="fas fa-edit" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Clone"
                >
                  <i class="fas fa-clone" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Sync"
                >
                  <i class="fas fa-sync" aria-hidden="true"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-stacked-example-overflow-menu"
        >
          <div class="pf-c-overflow-menu__content">
            <div class="pf-c-overflow-menu__group pf-m-button-group">
              <div class="pf-c-overflow-menu__item">
                <button class="pf-c-button pf-m-primary" type="button">Primary</button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-secondary"
                  type="button"
                >Secondary</button>
              </div>
            </div>
          </div>

          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-stacked-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-stacked-example-overflow-menu-dropdown-toggle"
                hidden
              >
                <li>
                  <button class="pf-c-dropdown__menu-item">Tertiary</button>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-hidden"
      id="toolbar-stacked-example-expandable-content"
      hidden
    ></div>
  </div>
  <hr class="pf-c-divider" />

  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__item pf-m-bulk-select">
        <div class="pf-c-dropdown">
          <div class="pf-c-dropdown__toggle pf-m-split-button">
            <label
              class="pf-c-dropdown__toggle-check"
              for="toolbar-stacked-example-bulk-select-toggle-check"
            >
              <input
                type="checkbox"
                id="toolbar-stacked-example-bulk-select-toggle-check"
                aria-label="Select all"
              />
            </label>

            <button
              class="pf-c-dropdown__toggle-button"
              type="button"
              aria-expanded="false"
              id="toolbar-stacked-example-bulk-select-toggle-button"
              aria-label="Dropdown toggle"
            >
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </button>
          </div>
          <ul class="pf-c-dropdown__menu" hidden>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select all</button>
            </li>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select none</button>
            </li>
            <li>
              <button
                class="pf-c-dropdown__menu-item"
                type="button"
              >Other action</button>
            </li>
          </ul>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-pagination">
        <div class="pf-c-pagination" aria-label="Element pagination">
          <div class="pf-c-pagination__total-items">37 items</div>
          <div class="pf-c-options-menu">
            <button
              class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
              type="button"
              id="toolbar-stacked-example-pagination-options-menu-toggle"
              aria-haspopup="listbox"
              aria-expanded="false"
            >
              <span class="pf-c-options-menu__toggle-text">
                <b>1 - 10</b>&nbsp;of&nbsp;
                <b>36</b>
              </span>
              <div class="pf-c-options-menu__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </div>
            </button>
            <ul
              class="pf-c-options-menu__menu"
              aria-labelledby="toolbar-stacked-example-pagination-options-menu-toggle"
              hidden
            >
              <li>
                <button
                  class="pf-c-options-menu__menu-item"
                  type="button"
                >5 per page</button>
              </li>
              <li>
                <button class="pf-c-options-menu__menu-item" type="button">
                  10 per page
                  <div class="pf-c-options-menu__menu-item-icon">
                    <i class="fas fa-check" aria-hidden="true"></i>
                  </div>
                </button>
              </li>
              <li>
                <button
                  class="pf-c-options-menu__menu-item"
                  type="button"
                >20 per page</button>
              </li>
            </ul>
          </div>

          <nav class="pf-c-pagination__nav" aria-label="Pagination">
            <button
              class="pf-c-button pf-m-plain pf-m-disabled"
              type="button"
              aria-label="Go to first page"
              aria-disabled="true"
            >
              <i class="fas fa-angle-double-left" aria-hidden="true"></i>
            </button>
            <button
              class="pf-c-button pf-m-plain pf-m-disabled"
              type="button"
              aria-label="Go to previous page"
              aria-disabled="true"
            >
              <i class="fas fa-angle-left" aria-hidden="true"></i>
            </button>

            <div
              class="pf-c-pagination__nav-page-select"
              aria-label="Current page 1 of 4"
            >
              <input
                class="pf-c-form-control"
                aria-label="Current page"
                type="number"
                min="1"
                max="4"
                value="1"
              />
              <span aria-hidden="true">of 4</span>
            </div>
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Go to next page"
            >
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </button>
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Go to last page"
            >
              <i class="fas fa-angle-double-right" aria-hidden="true"></i>
            </button>
          </nav>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Stacked on mobile (filters collapsed, expandable content expanded)

```html
<div class="pf-c-toolbar" id="toolbar-stacked-collapsed-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group">
        <div class="pf-c-toolbar__toggle pf-m-expanded">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="true"
            aria-controls="toolbar-stacked-collapsed-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-stacked-collapsed-example-icon-button-overflow-menu"
        >
          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-stacked-collapsed-example-icon-button-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-stacked-collapsed-example-icon-button-overflow-menu-dropdown-toggle"
                hidden
              >
                <li>
                  <button class="pf-c-dropdown__menu-item">Edit</button>
                </li>
                <li>
                  <button class="pf-c-dropdown__menu-item">Clone</button>
                </li>
                <li>
                  <button class="pf-c-dropdown__menu-item">Sync</button>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-expanded"
      id="toolbar-stacked-collapsed-example-expandable-content"
    >
      <div class="pf-c-toolbar__group">
        <div
          class="pf-c-toolbar__item pf-m-label"
          id="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-label"
        >Resource</div>
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-label toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Pod</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <div class="pf-c-select__menu" hidden>
              <fieldset
                class="pf-c-select__menu-fieldset"
                aria-label="Select input"
              >
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-active"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-active"
                    name="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-active"
                  />

                  <span class="pf-c-check__label">Active</span>
                  <span class="pf-c-check__description">This is a description</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-canceled"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-canceled"
                    name="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-canceled"
                  />

                  <span class="pf-c-check__label">Canceled</span>
                  <span
                    class="pf-c-check__description"
                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-paused"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-paused"
                    name="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-paused"
                  />

                  <span class="pf-c-check__label">Paused</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-warning"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-warning"
                    name="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-warning"
                  />

                  <span class="pf-c-check__label">Warning</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-restarted"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-restarted"
                    name="toolbar-stacked-collapsed-example-select-checkbox-resource-expanded-restarted"
                  />

                  <span class="pf-c-check__label">Restarted</span>
                </label>
              </fieldset>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__group">
        <div
          class="pf-c-toolbar__item pf-m-label"
          id="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-label"
        >Status</div>
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-label toolbar-stacked-collapsed-example-select-checkbox-status-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Running</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <div class="pf-c-select__menu" hidden>
              <fieldset
                class="pf-c-select__menu-fieldset"
                aria-label="Select input"
              >
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-active"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-active"
                    name="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-active"
                  />

                  <span class="pf-c-check__label">Active</span>
                  <span class="pf-c-check__description">This is a description</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-canceled"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-canceled"
                    name="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-canceled"
                  />

                  <span class="pf-c-check__label">Canceled</span>
                  <span
                    class="pf-c-check__description"
                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-paused"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-paused"
                    name="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-paused"
                  />

                  <span class="pf-c-check__label">Paused</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-warning"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-warning"
                    name="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-warning"
                  />

                  <span class="pf-c-check__label">Warning</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-restarted"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-restarted"
                    name="toolbar-stacked-collapsed-example-select-checkbox-status-expanded-restarted"
                  />

                  <span class="pf-c-check__label">Restarted</span>
                </label>
              </fieldset>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__group">
        <div
          class="pf-c-toolbar__item pf-m-label"
          id="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-label"
        >Type</div>
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-label toolbar-stacked-collapsed-example-select-checkbox-type-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Any</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <div class="pf-c-select__menu" hidden>
              <fieldset
                class="pf-c-select__menu-fieldset"
                aria-label="Select input"
              >
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-active"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-active"
                    name="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-active"
                  />

                  <span class="pf-c-check__label">Active</span>
                  <span class="pf-c-check__description">This is a description</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                  for="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-canceled"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-canceled"
                    name="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-canceled"
                  />

                  <span class="pf-c-check__label">Canceled</span>
                  <span
                    class="pf-c-check__description"
                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-paused"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-paused"
                    name="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-paused"
                  />

                  <span class="pf-c-check__label">Paused</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-warning"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-warning"
                    name="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-warning"
                  />

                  <span class="pf-c-check__label">Warning</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-restarted"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-restarted"
                    name="toolbar-stacked-collapsed-example-select-checkbox-type-expanded-restarted"
                  />

                  <span class="pf-c-check__label">Restarted</span>
                </label>
              </fieldset>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <hr class="pf-c-divider" />

  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__item pf-m-bulk-select">
        <div class="pf-c-dropdown">
          <div class="pf-c-dropdown__toggle pf-m-split-button">
            <label
              class="pf-c-dropdown__toggle-check"
              for="toolbar-stacked-collapsed-example-bulk-select-toggle-check"
            >
              <input
                type="checkbox"
                id="toolbar-stacked-collapsed-example-bulk-select-toggle-check"
                aria-label="Select all"
              />
            </label>

            <button
              class="pf-c-dropdown__toggle-button"
              type="button"
              aria-expanded="false"
              id="toolbar-stacked-collapsed-example-bulk-select-toggle-button"
              aria-label="Dropdown toggle"
            >
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </button>
          </div>
          <ul class="pf-c-dropdown__menu" hidden>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select all</button>
            </li>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select none</button>
            </li>
            <li>
              <button
                class="pf-c-dropdown__menu-item"
                type="button"
              >Other action</button>
            </li>
          </ul>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-pagination">
        <div class="pf-c-pagination" aria-label="Element pagination">
          <div class="pf-c-pagination__total-items">37 items</div>
          <div class="pf-c-options-menu">
            <button
              class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
              type="button"
              id="toolbar-stacked-collapsed-example-pagination-options-menu-toggle"
              aria-haspopup="listbox"
              aria-expanded="false"
            >
              <span class="pf-c-options-menu__toggle-text">
                <b>1 - 10</b>&nbsp;of&nbsp;
                <b>36</b>
              </span>
              <div class="pf-c-options-menu__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </div>
            </button>
            <ul
              class="pf-c-options-menu__menu"
              aria-labelledby="toolbar-stacked-collapsed-example-pagination-options-menu-toggle"
              hidden
            >
              <li>
                <button
                  class="pf-c-options-menu__menu-item"
                  type="button"
                >5 per page</button>
              </li>
              <li>
                <button class="pf-c-options-menu__menu-item" type="button">
                  10 per page
                  <div class="pf-c-options-menu__menu-item-icon">
                    <i class="fas fa-check" aria-hidden="true"></i>
                  </div>
                </button>
              </li>
              <li>
                <button
                  class="pf-c-options-menu__menu-item"
                  type="button"
                >20 per page</button>
              </li>
            </ul>
          </div>

          <nav class="pf-c-pagination__nav" aria-label="Pagination">
            <button
              class="pf-c-button pf-m-plain pf-m-disabled"
              type="button"
              aria-label="Go to first page"
              aria-disabled="true"
            >
              <i class="fas fa-angle-double-left" aria-hidden="true"></i>
            </button>
            <button
              class="pf-c-button pf-m-plain pf-m-disabled"
              type="button"
              aria-label="Go to previous page"
              aria-disabled="true"
            >
              <i class="fas fa-angle-left" aria-hidden="true"></i>
            </button>

            <div
              class="pf-c-pagination__nav-page-select"
              aria-label="Current page 1 of 4"
            >
              <input
                class="pf-c-form-control"
                aria-label="Current page"
                type="number"
                min="1"
                max="4"
                value="1"
              />
              <span aria-hidden="true">of 4</span>
            </div>
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Go to next page"
            >
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </button>
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Go to last page"
            >
              <i class="fas fa-angle-double-right" aria-hidden="true"></i>
            </button>
          </nav>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Expanded elements

```html
<div class="pf-c-toolbar" id="toolbar-expanded-elements-example">
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__item pf-m-bulk-select">
        <div class="pf-c-dropdown">
          <div class="pf-c-dropdown__toggle pf-m-split-button">
            <label
              class="pf-c-dropdown__toggle-check"
              for="toolbar-expanded-elements-example-bulk-select-toggle-check"
            >
              <input
                type="checkbox"
                id="toolbar-expanded-elements-example-bulk-select-toggle-check"
                aria-label="Select all"
              />
            </label>

            <button
              class="pf-c-dropdown__toggle-button"
              type="button"
              aria-expanded="false"
              id="toolbar-expanded-elements-example-bulk-select-toggle-button"
              aria-label="Dropdown toggle"
            >
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </button>
          </div>
          <ul class="pf-c-dropdown__menu" hidden>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select all</button>
            </li>
            <li>
              <button class="pf-c-dropdown__menu-item" type="button">Select none</button>
            </li>
            <li>
              <button
                class="pf-c-dropdown__menu-item"
                type="button"
              >Other action</button>
            </li>
          </ul>
        </div>
      </div>
      <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-xl">
        <div class="pf-c-toolbar__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="false"
            aria-controls="toolbar-expanded-elements-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__item pf-m-search-filter">
          <div class="pf-c-input-group" aria-label="search filter" role="group">
            <div class="pf-c-select" style="width: 175px">
              <span
                id="toolbar-expanded-elements-example-select-name-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-expanded-elements-example-select-name-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-expanded-elements-example-select-name-label toolbar-expanded-elements-example-select-name-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-icon">
                    <i class="fas fa-filter" aria-hidden="true"></i>
                  </span>
                  <span class="pf-c-select__toggle-text">Name</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="toolbar-expanded-elements-example-select-name-label"
                hidden
                style="width: 175px"
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Running</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    Stopped
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Down</button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Degraded</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item"
                    role="option"
                  >Needs maintenance</button>
                </li>
              </ul>
            </div>
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Filter by name"
                    aria-label="Filter by name"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__group pf-m-filter-group">
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select pf-m-expanded">
              <span
                id="toolbar-expanded-elements-example-select-checkbox-status-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-expanded-elements-example-select-checkbox-status-toggle"
                aria-haspopup="true"
                aria-expanded="true"
                aria-labelledby="toolbar-expanded-elements-example-select-checkbox-status-label toolbar-expanded-elements-example-select-checkbox-status-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Status</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu">
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-status-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-status-active"
                      name="toolbar-expanded-elements-example-select-checkbox-status-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-status-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-status-canceled"
                      name="toolbar-expanded-elements-example-select-checkbox-status-canceled"
                      checked
                    />

                    <span class="pf-c-check__label">Canceled</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-status-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-status-paused"
                      name="toolbar-expanded-elements-example-select-checkbox-status-paused"
                      checked
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-status-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-status-warning"
                      name="toolbar-expanded-elements-example-select-checkbox-status-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-status-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-status-restarted"
                      name="toolbar-expanded-elements-example-select-checkbox-status-restarted"
                      checked
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select pf-m-expanded">
              <span
                id="toolbar-expanded-elements-example-select-checkbox-risk-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-expanded-elements-example-select-checkbox-risk-toggle"
                aria-haspopup="true"
                aria-expanded="true"
                aria-labelledby="toolbar-expanded-elements-example-select-checkbox-risk-label toolbar-expanded-elements-example-select-checkbox-risk-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Risk</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <div class="pf-c-select__menu">
                <fieldset
                  class="pf-c-select__menu-fieldset"
                  aria-label="Select input"
                >
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-risk-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-risk-active"
                      name="toolbar-expanded-elements-example-select-checkbox-risk-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-risk-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-risk-canceled"
                      name="toolbar-expanded-elements-example-select-checkbox-risk-canceled"
                      checked
                    />

                    <span class="pf-c-check__label">Canceled</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-risk-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-risk-paused"
                      name="toolbar-expanded-elements-example-select-checkbox-risk-paused"
                      checked
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-risk-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-risk-warning"
                      name="toolbar-expanded-elements-example-select-checkbox-risk-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-expanded-elements-example-select-checkbox-risk-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-expanded-elements-example-select-checkbox-risk-restarted"
                      name="toolbar-expanded-elements-example-select-checkbox-risk-restarted"
                      checked
                    />

                    <span class="pf-c-check__label">Restarted</span>
                  </label>
                </fieldset>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-expanded-elements-example-icon-button-overflow-menu"
        >
          <div class="pf-c-overflow-menu__content">
            <div class="pf-c-overflow-menu__group pf-m-icon-button-group">
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Edit"
                >
                  <i class="fas fa-edit" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Clone"
                >
                  <i class="fas fa-clone" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Sync"
                >
                  <i class="fas fa-sync" aria-hidden="true"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-expanded-elements-example-overflow-menu"
        >
          <div class="pf-c-overflow-menu__content">
            <div class="pf-c-overflow-menu__group pf-m-button-group">
              <div class="pf-c-overflow-menu__item">
                <button class="pf-c-button pf-m-primary" type="button">Primary</button>
              </div>
              <div class="pf-c-overflow-menu__item">
                <button
                  class="pf-c-button pf-m-secondary"
                  type="button"
                >Secondary</button>
              </div>
            </div>
          </div>

          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown pf-m-expanded">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-expanded-elements-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="true"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-expanded-elements-example-overflow-menu-dropdown-toggle"
              >
                <li>
                  <button class="pf-c-dropdown__menu-item">Tertiary</button>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-hidden"
      id="toolbar-expanded-elements-example-expandable-content"
      hidden
    ></div>
  </div>
</div>

```

## Documentation

### Overview

As the toolbar component is a hybrid layout and component, some of its elements are presentational, while some require accessibility support.

### Usage

| Class                                 | Applied to                                                                                                | Outcome                                                                                                                                                                                                                                     |
| ------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-toolbar`                       | `<div>`                                                                                                   | Initiates the toolbar component. **Required**                                                                                                                                                                                               |
| `.pf-c-toolbar__item`                 | `<div>`                                                                                                   | Initiates a toolbar item. **Required**                                                                                                                                                                                                      |
| `.pf-c-toolbar__group`                | `<div>`                                                                                                   | Initiates a toolbar group.                                                                                                                                                                                                                  |
| `.pf-c-toolbar__content`              | `<div>`                                                                                                   | Initiates a toolbar content container. **Required**                                                                                                                                                                                         |
| `.pf-c-toolbar__content-section`      | `<div>`                                                                                                   | Initiates a toolbar content section. This is used to separate static elements from dynamic elements within a content container. There should be no more than one `.pf-c-toolbar__content-section` per `.pf-c-toolbar__content` **Required** |
| `.pf-c-toolbar__expandable-content`   | `<div>`                                                                                                   | Initiates a toolbar expandable content section.                                                                                                                                                                                             |
| `.pf-m-sticky`                        | `.pf-c-toolbar`                                                                                           | Modifies toolbar component to be sticky to the top of its container.                                                                                                                                                                        |
| `.pf-m-full-height`                   | `.pf-c-toolbar`                                                                                           | Modifies toolbar component to full height of its container and removes vertical padding.                                                                                                                                                    |
| `.pf-m-static`                        | `.pf-c-toolbar`                                                                                           | Modifies expandable content section to position itself to the nearest absolutely positioned parent outside of the toolbar component. This is used primarily for masthead toolbar.                                                           |
| `.pf-m-expanded`                      | `.pf-c-toolbar__expandable-content`                                                                       | Modifies expandable content section for the expanded state.                                                                                                                                                                                 |
| `.pf-m-bulk-select`                   | `.pf-c-toolbar__item`                                                                                     | Initiates bulk select spacing.                                                                                                                                                                                                              |
| `.pf-m-overflow-menu`                 | `.pf-c-toolbar__item`                                                                                     | Initiates overflow menu spacing.                                                                                                                                                                                                            |
| `.pf-m-pagination`                    | `.pf-c-toolbar__item`                                                                                     | Initiates pagination spacing and margin.                                                                                                                                                                                                    |
| `.pf-m-search-filter`                 | `.pf-c-toolbar__item`                                                                                     | Initiates search filter spacing.                                                                                                                                                                                                            |
| `.pf-m-chip-group`                    | `.pf-c-toolbar__item`                                                                                     | Initiates chip group spacing.                                                                                                                                                                                                               |
| `.pf-m-expand-all`                    | `.pf-c-toolbar__item`                                                                                     | Initiates an item for an expand all button.                                                                                                                                                                                                 |
| `.pf-m-expanded`                      | `.pf-c-toolbar__item.pf-m-expand-all`                                                                     | Modifies an expand all button for the expanded state.                                                                                                                                                                                       |
| `.pf-m-button-group`                  | `.pf-c-toolbar__group`                                                                                    | Initiates button group spacing.                                                                                                                                                                                                             |
| `.pf-m-icon-button-group`             | `.pf-c-toolbar__group`                                                                                    | Initiates icon button group spacing.                                                                                                                                                                                                        |
| `.pf-m-filter-group`                  | `.pf-c-toolbar__group`                                                                                    | Initiates filter group spacing.                                                                                                                                                                                                             |
| `.pf-m-hidden{-on-[breakpoint]}`      | `.pf-c-toolbar__content`, `.pf-c-toolbar__content-section`, `.pf-c-toolbar__item`, `.pf-c-toolbar__group` | Modifies toolbar element to be hidden, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                                                         |
| `.pf-m-visible{-on-[breakpoint]}`     | `.pf-c-toolbar__content`, `.pf-c-toolbar__content-section`, `.pf-c-toolbar__item`, `.pf-c-toolbar__group` | Modifies toolbar element to be shown, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                                                          |
| `.pf-m-align-right{-on-[breakpoint]}` | `.pf-c-toolbar > *`                                                                                       | Modifies toolbar element to align right, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                                                       |
| `.pf-m-align-left{-on-[breakpoint]}`  | `.pf-c-toolbar > *`                                                                                       | Modifies toolbar element to align left, at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                                                        |
| `.pf-m-label`                         | `.pf-c-toolbar__item`                                                                                     | Modifies toolbar item to label.                                                                                                                                                                                                             |
| `.pf-m-chip-container`                | `.pf-c-toolbar__content`, `.pf-c-toolbar__group`                                                          | Modifies the toolbar element for applied filters layout.                                                                                                                                                                                    |
| `.pf-m-overflow-container`            | `.pf-c-toolbar__item`, `.pf-c-toolbar__group`                                                             | Modifies the toolbar element to hide overflow and respond to available space. Used for horizontal navigation.                                                                                                                               |
| `.pf-m-expanded`                      | `.pf-c-toolbar__expandable-content`, `.pf-c-toolbar__toggle`                                              | Modifies the component for the expanded state.                                                                                                                                                                                              |
| `.pf-m-wrap`                          | `.pf-c-toolbar`, `.pf-c-toolbar__content-section`, `.pf-c-toolbar__group`                                 | Modifies the toolbar element to wrap.                                                                                                                                                                                                       |
| `.pf-m-nowrap`                        | `.pf-c-toolbar`, `.pf-c-toolbar__group`                                                                   | Modifies the toolbar element to nowrap.                                                                                                                                                                                                     |

### Accessibility

| Attribute                                    | Applied to                                                                                                  | Outcome                                                                       |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `hidden`                                     | `.pf-c-toolbar__item`, `.pf-c-toolbar__group`, `.pf-c-toolbar__toggle`, `.pf-c-toolbar__expandable-content` | Indicates that the toolbar element is hidden. **Required**                    |
| `aria-expanded="true"`                       | `.pf-c-toolbar__toggle > .pf-c-button`                                                                      | Indicates that the expandable content is visible. **Required**                |
| `aria-expanded="false"`                      | `.pf-c-toolbar__toggle > .pf-c-button`                                                                      | Indicates the the expandable content is hidden. **Required**                  |
| `aria-controls="[id of expandable content]"` | `.pf-c-toolbar__toggle > .pf-c-button`                                                                      | Identifies the expanded content controlled by the toggle button. **Required** |
| `id="[expandable-content_id]"`               | `.pf-c-toolbar__expandable-content`                                                                         | Provides a reference for toggle button description. **Required**              |
| `aria-label="Expand all"`                    | `.pf-c-toolbar__item.pf-m-expand-all`                                                                       | Provides an accessible label for the expand all item button. **Required**     |
| `aria-label="Collapse all"`                  | `.pf-c-toolbar__item.pf-m-expand-all.pf-m-expanded`                                                         | Provides an accessible label for the expand all item button. **Required**     |

### Toggle group usage

| Class                          | Applied to                                                                    | Outcome                                                                                                                                                                                                                            |
| ------------------------------ | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-toggle-group`           | `.pf-c-toolbar__group`                                                        | Modifies toolbar group to control when, and at which breakpoint, filters will be hidden/shown. By default, all filters are hidden until the specified breakpoint is reached.                                                       |
| `.pf-m-show{-on-[breakpoint]}` | `.pf-c-toolbar__group.pf-m-toggle-group`, `.pf-c-toolbar__expandable-content` | Modifies toolbar element to hidden at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). This selector must be applied consistently to toggle group and expandable content. |

### Spacer system

| Class                                                        | Applied to                                    | Outcome                                                                                                                                             |
| ------------------------------------------------------------ | --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-spacer-{none, sm, md, lg, xl}{-on-[breakpoint]}`      | `.pf-c-toolbar__group`, `.pf-c-toolbar__item` | Modifies toolbar group or item spacing at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). |
| `.pf-m-space-items-{none, sm, md, lg, xl}{-on-[breakpoint]}` | `.pf-c-toolbar__group`                        | Modifies toolbar group child spacing at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).   |
