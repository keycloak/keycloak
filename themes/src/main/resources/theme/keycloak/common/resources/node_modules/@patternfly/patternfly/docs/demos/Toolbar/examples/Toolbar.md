---
id: Toolbar
section: components
---import './Toolbar.css'

## Demos

### Toolbar attribute value search filter desktop

```html
<div
  class="pf-c-toolbar"
  id="toolbar-attribute-value-search-filter-desktop-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show">
        <div class="pf-c-toolbar__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="false"
            aria-controls="toolbar-attribute-value-search-filter-desktop-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__group pf-m-filter-group">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-input-group">
              <div class="pf-c-select" style="width: 150px">
                <span
                  id="toolbar-attribute-value-search-filter-desktop-example-select-name-label"
                  hidden
                >Choose one</span>

                <button
                  class="pf-c-select__toggle"
                  type="button"
                  id="toolbar-attribute-value-search-filter-desktop-example-select-name-toggle"
                  aria-haspopup="true"
                  aria-expanded="false"
                  aria-labelledby="toolbar-attribute-value-search-filter-desktop-example-select-name-label toolbar-attribute-value-search-filter-desktop-example-select-name-toggle"
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
                  aria-labelledby="toolbar-attribute-value-search-filter-desktop-example-select-name-label"
                  hidden
                  style="width: 150px"
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
                    <button
                      class="pf-c-select__menu-item"
                      role="option"
                    >Degraded</button>
                  </li>
                  <li role="presentation">
                    <button
                      class="pf-c-select__menu-item"
                      role="option"
                    >Needs maintenance</button>
                  </li>
                </ul>
              </div>
              <input
                class="pf-c-form-control"
                id="toolbar-attribute-value-search-filter-desktop-example-textInput11"
                name="textInput11"
                type="search"
                placeholder="Filter by name..."
                aria-label="Search input example"
              />
              <button
                class="pf-c-button pf-m-control"
                type="button"
                aria-label="Search button for search input"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-attribute-value-search-filter-desktop-example-overflow-menu"
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
                id="toolbar-attribute-value-search-filter-desktop-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-attribute-value-search-filter-desktop-example-overflow-menu-dropdown-toggle"
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
      <div class="pf-c-toolbar__item pf-m-pagination">
        <div
          class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
        >
          <div
            class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
          >
            <div class="pf-c-options-menu">
              <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                <span class="pf-c-options-menu__toggle-text">
                  <b>1 - 10</b>&nbsp;of&nbsp;
                  <b>36</b>
                </span>
                <button
                  class="pf-c-options-menu__toggle-button"
                  id="pagination-options-menu-bottom-example-toggle"
                  aria-haspopup="listbox"
                  aria-expanded="false"
                  aria-label="Items per page"
                >
                  <span class="pf-c-options-menu__toggle-button-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <ul
                class="pf-c-options-menu__menu"
                aria-labelledby="pagination-options-menu-bottom-example-toggle"
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
              <div class="pf-c-pagination__nav-control pf-m-prev">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  disabled
                  aria-label="Go to previous page"
                >
                  <i class="fas fa-angle-left" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-pagination__nav-control pf-m-next">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Go to next page"
                >
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </button>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-hidden"
      id="toolbar-attribute-value-search-filter-desktop-example-expandable-content"
      hidden
    ></div>
  </div>
</div>

```

### Toolbar attribute value search filter on mobile

```html
<div
  class="pf-c-toolbar"
  id="toolbar-attribute-value-search-filter-mobile-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group">
        <div class="pf-c-toolbar__toggle pf-m-expanded">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="true"
            aria-controls="toolbar-attribute-value-search-filter-mobile-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-attribute-value-search-filter-mobile-example-overflow-menu"
        >
          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-attribute-value-search-filter-mobile-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-attribute-value-search-filter-mobile-example-overflow-menu-dropdown-toggle"
                hidden
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
              </ul>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-pagination">
        <div
          class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
        >
          <div
            class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
          >
            <div class="pf-c-options-menu">
              <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                <span class="pf-c-options-menu__toggle-text">
                  <b>1 - 10</b>&nbsp;of&nbsp;
                  <b>36</b>
                </span>
                <button
                  class="pf-c-options-menu__toggle-button"
                  id="pagination-options-menu-bottom-example-toggle"
                  aria-haspopup="listbox"
                  aria-expanded="false"
                  aria-label="Items per page"
                >
                  <span class="pf-c-options-menu__toggle-button-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <ul
                class="pf-c-options-menu__menu"
                aria-labelledby="pagination-options-menu-bottom-example-toggle"
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
              <div class="pf-c-pagination__nav-control pf-m-prev">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  disabled
                  aria-label="Go to previous page"
                >
                  <i class="fas fa-angle-left" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-pagination__nav-control pf-m-next">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Go to next page"
                >
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </button>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-expanded"
      id="toolbar-attribute-value-search-filter-mobile-example-expandable-content"
    >
      <div class="pf-c-toolbar__group pf-m-filter-group">
        <div class="pf-c-toolbar__item">
          <div class="pf-c-input-group">
            <div class="pf-c-select" style="width: 150px">
              <span
                id="toolbar-attribute-value-search-filter-mobile-example-select-name-expanded-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-attribute-value-search-filter-mobile-example-select-name-expanded-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-attribute-value-search-filter-mobile-example-select-name-expanded-label toolbar-attribute-value-search-filter-mobile-example-select-name-expanded-toggle"
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
                aria-labelledby="toolbar-attribute-value-search-filter-mobile-example-select-name-expanded-label"
                hidden
                style="width: 150px"
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
            <input
              class="pf-c-form-control"
              id="toolbar-attribute-value-search-filter-mobile-example-textInput12"
              name="textInput11"
              type="search"
              placeholder="Filter by name..."
              aria-label="Search input example"
            />
            <button
              class="pf-c-button pf-m-control"
              type="button"
              aria-label="Search button for search input"
            >
              <i class="fas fa-search" aria-hidden="true"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Toolbar attribute value single select filter desktop

```html
<div
  class="pf-c-toolbar"
  id="toolbar-attribute-value-single-select-filter-desktop-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show">
        <div class="pf-c-toolbar__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="false"
            aria-controls="toolbar-attribute-value-single-select-filter-desktop-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__group pf-m-filter-group">
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select" style="width: 150px">
              <span
                id="toolbar-attribute-value-single-select-filter-desktop-example-select-status-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-attribute-value-single-select-filter-desktop-example-select-status-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-attribute-value-single-select-filter-desktop-example-select-status-label toolbar-attribute-value-single-select-filter-desktop-example-select-status-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-icon">
                    <i class="fas fa-filter" aria-hidden="true"></i>
                  </span>
                  <span class="pf-c-select__toggle-text">Status</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="toolbar-attribute-value-single-select-filter-desktop-example-select-status-label"
                hidden
                style="width: 150px"
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
            <div class="pf-c-select pf-m-expanded" style="width: 200px">
              <span
                id="toolbar-attribute-value-single-select-filter-desktop-example-select-status-two-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-attribute-value-single-select-filter-desktop-example-select-status-two-toggle"
                aria-haspopup="true"
                aria-expanded="true"
                aria-labelledby="toolbar-attribute-value-single-select-filter-desktop-example-select-status-two-label toolbar-attribute-value-single-select-filter-desktop-example-select-status-two-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Stopped</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="toolbar-attribute-value-single-select-filter-desktop-example-select-status-two-label"
                style="width: 200px"
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
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-attribute-value-single-select-filter-desktop-example-overflow-menu"
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
                id="toolbar-attribute-value-single-select-filter-desktop-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-attribute-value-single-select-filter-desktop-example-overflow-menu-dropdown-toggle"
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
      <div class="pf-c-toolbar__item pf-m-pagination">
        <div
          class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
        >
          <div
            class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
          >
            <div class="pf-c-options-menu">
              <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                <span class="pf-c-options-menu__toggle-text">
                  <b>1 - 10</b>&nbsp;of&nbsp;
                  <b>36</b>
                </span>
                <button
                  class="pf-c-options-menu__toggle-button"
                  id="pagination-options-menu-bottom-example-toggle"
                  aria-haspopup="listbox"
                  aria-expanded="false"
                  aria-label="Items per page"
                >
                  <span class="pf-c-options-menu__toggle-button-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <ul
                class="pf-c-options-menu__menu"
                aria-labelledby="pagination-options-menu-bottom-example-toggle"
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
              <div class="pf-c-pagination__nav-control pf-m-prev">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  disabled
                  aria-label="Go to previous page"
                >
                  <i class="fas fa-angle-left" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-pagination__nav-control pf-m-next">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Go to next page"
                >
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </button>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-hidden"
      id="toolbar-attribute-value-single-select-filter-desktop-example-expandable-content"
      hidden
    ></div>
  </div>
</div>

```

### Toolbar attribute value single select filter on mobile

```html
<div
  class="pf-c-toolbar"
  id="toolbar-attribute-value-single-select-filter-mobile-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group">
        <div class="pf-c-toolbar__toggle pf-m-expanded">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="true"
            aria-controls="toolbar-attribute-value-single-select-filter-mobile-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-attribute-value-single-select-filter-mobile-example-overflow-menu"
        >
          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-attribute-value-single-select-filter-mobile-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-attribute-value-single-select-filter-mobile-example-overflow-menu-dropdown-toggle"
                hidden
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
              </ul>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-pagination">
        <div
          class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
        >
          <div
            class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
          >
            <div class="pf-c-options-menu">
              <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                <span class="pf-c-options-menu__toggle-text">
                  <b>1 - 10</b>&nbsp;of&nbsp;
                  <b>36</b>
                </span>
                <button
                  class="pf-c-options-menu__toggle-button"
                  id="pagination-options-menu-bottom-example-toggle"
                  aria-haspopup="listbox"
                  aria-expanded="false"
                  aria-label="Items per page"
                >
                  <span class="pf-c-options-menu__toggle-button-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <ul
                class="pf-c-options-menu__menu"
                aria-labelledby="pagination-options-menu-bottom-example-toggle"
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
              <div class="pf-c-pagination__nav-control pf-m-prev">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  disabled
                  aria-label="Go to previous page"
                >
                  <i class="fas fa-angle-left" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-pagination__nav-control pf-m-next">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Go to next page"
                >
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </button>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-expanded"
      id="toolbar-attribute-value-single-select-filter-mobile-example-expandable-content"
    >
      <div class="pf-c-toolbar__group pf-m-filter-group">
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-attribute-value-single-select-filter-mobile-example-select-status-expanded-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-attribute-value-single-select-filter-mobile-example-select-status-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-attribute-value-single-select-filter-mobile-example-select-status-expanded-label toolbar-attribute-value-single-select-filter-mobile-example-select-status-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-icon">
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </span>
                <span class="pf-c-select__toggle-text">Status</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <ul
              class="pf-c-select__menu"
              role="listbox"
              aria-labelledby="toolbar-attribute-value-single-select-filter-mobile-example-select-status-expanded-label"
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
          <div class="pf-c-select pf-m-expanded">
            <span
              id="toolbar-attribute-value-single-select-filter-mobile-example-select-status-two-expanded-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-attribute-value-single-select-filter-mobile-example-select-status-two-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="true"
              aria-labelledby="toolbar-attribute-value-single-select-filter-mobile-example-select-status-two-expanded-label toolbar-attribute-value-single-select-filter-mobile-example-select-status-two-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Stopped</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <ul
              class="pf-c-select__menu"
              role="listbox"
              aria-labelledby="toolbar-attribute-value-single-select-filter-mobile-example-select-status-two-expanded-label"
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
    </div>
  </div>
</div>

```

### Toolbar attribute value checkbox select filter desktop

```html
<div
  class="pf-c-toolbar"
  id="toolbar-attribute-value-checkbox-select-filter-desktop-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show">
        <div class="pf-c-toolbar__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="false"
            aria-controls="toolbar-attribute-value-checkbox-select-filter-desktop-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-toolbar__group pf-m-filter-group">
          <div class="pf-c-toolbar__item">
            <div class="pf-c-select" style="width: 150px">
              <span
                id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-status-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-status-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-status-label toolbar-attribute-value-checkbox-select-filter-desktop-example-select-status-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-icon">
                    <i class="fas fa-filter" aria-hidden="true"></i>
                  </span>
                  <span class="pf-c-select__toggle-text">Status</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>

              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-status-label"
                hidden
                style="width: 150px"
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
            <div class="pf-c-select pf-m-expanded">
              <span
                id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-toggle"
                aria-haspopup="true"
                aria-expanded="true"
                aria-labelledby="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-label toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">Filter by status</span>
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
                    for="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-active"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-active"
                      name="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-active"
                    />

                    <span class="pf-c-check__label">Active</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-canceled"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-canceled"
                      name="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-canceled"
                      checked
                    />

                    <span class="pf-c-check__label">Canceled</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-paused"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-paused"
                      name="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-paused"
                      checked
                    />

                    <span class="pf-c-check__label">Paused</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-warning"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-warning"
                      name="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-warning"
                    />

                    <span class="pf-c-check__label">Warning</span>
                  </label>
                  <label
                    class="pf-c-check pf-c-select__menu-item"
                    for="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-restarted"
                  >
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-restarted"
                      name="toolbar-attribute-value-checkbox-select-filter-desktop-example-select-filter-status-restarted"
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
          id="toolbar-attribute-value-checkbox-select-filter-desktop-example-overflow-menu"
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
                id="toolbar-attribute-value-checkbox-select-filter-desktop-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-attribute-value-checkbox-select-filter-desktop-example-overflow-menu-dropdown-toggle"
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
      <div class="pf-c-toolbar__item pf-m-pagination">
        <div
          class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
        >
          <div
            class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
          >
            <div class="pf-c-options-menu">
              <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                <span class="pf-c-options-menu__toggle-text">
                  <b>1 - 10</b>&nbsp;of&nbsp;
                  <b>36</b>
                </span>
                <button
                  class="pf-c-options-menu__toggle-button"
                  id="pagination-options-menu-bottom-example-toggle"
                  aria-haspopup="listbox"
                  aria-expanded="false"
                  aria-label="Items per page"
                >
                  <span class="pf-c-options-menu__toggle-button-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <ul
                class="pf-c-options-menu__menu"
                aria-labelledby="pagination-options-menu-bottom-example-toggle"
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
              <div class="pf-c-pagination__nav-control pf-m-prev">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  disabled
                  aria-label="Go to previous page"
                >
                  <i class="fas fa-angle-left" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-pagination__nav-control pf-m-next">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Go to next page"
                >
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </button>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-hidden"
      id="toolbar-attribute-value-checkbox-select-filter-desktop-example-expandable-content"
      hidden
    ></div>
  </div>
  <div class="pf-c-toolbar__content pf-m-chip-container">
    <div class="pf-c-toolbar__item pf-m-chip-group">
      <div class="pf-c-chip-group pf-m-category">
        <div class="pf-c-chip-group__main">
          <span
            class="pf-c-chip-group__label"
            aria-hidden="true"
            id="toolbar-attribute-value-checkbox-select-filter-desktop-example-chip-group-label"
          >Status</span>
          <ul
            class="pf-c-chip-group__list"
            role="list"
            aria-labelledby="toolbar-attribute-value-checkbox-select-filter-desktop-example-chip-group-label"
          >
            <li class="pf-c-chip-group__list-item">
              <div class="pf-c-chip">
                <span class="pf-c-chip__text" id="chip-one">Canceled</span>
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-labelledby="remove-chip-one chip-one"
                  aria-label="Remove"
                  id="remove-chip-one"
                >
                  <i class="fas fa-times" aria-hidden="true"></i>
                </button>
              </div>
            </li>
            <li class="pf-c-chip-group__list-item">
              <div class="pf-c-chip">
                <span class="pf-c-chip__text" id="chip-two">Paused</span>
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-labelledby="remove-chip-two chip-two"
                  aria-label="Remove"
                  id="remove-chip-two"
                >
                  <i class="fas fa-times" aria-hidden="true"></i>
                </button>
              </div>
            </li>
            <li class="pf-c-chip-group__list-item">
              <div class="pf-c-chip">
                <span class="pf-c-chip__text" id="chip-three">Restarted</span>
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-labelledby="remove-chip-three chip-three"
                  aria-label="Remove"
                  id="remove-chip-three"
                >
                  <i class="fas fa-times" aria-hidden="true"></i>
                </button>
              </div>
            </li>
          </ul>
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

### Toolbar attribute value checkbox select filter on mobile

```html
<div
  class="pf-c-toolbar"
  id="toolbar-attribute-value-checkbox-select-filter-mobile-example"
>
  <div class="pf-c-toolbar__content">
    <div class="pf-c-toolbar__content-section">
      <div class="pf-c-toolbar__group pf-m-toggle-group">
        <div class="pf-c-toolbar__toggle pf-m-expanded">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Show filters"
            aria-expanded="true"
            aria-controls="toolbar-attribute-value-checkbox-select-filter-mobile-example-expandable-content"
          >
            <i class="fas fa-filter" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-overflow-menu">
        <div
          class="pf-c-overflow-menu"
          id="toolbar-attribute-value-checkbox-select-filter-mobile-example-overflow-menu"
        >
          <div class="pf-c-overflow-menu__control">
            <div class="pf-c-dropdown">
              <button
                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                type="button"
                id="toolbar-attribute-value-checkbox-select-filter-mobile-example-overflow-menu-dropdown-toggle"
                aria-label="Overflow menu"
                aria-expanded="false"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <ul
                class="pf-c-dropdown__menu"
                aria-labelledby="toolbar-attribute-value-checkbox-select-filter-mobile-example-overflow-menu-dropdown-toggle"
                hidden
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
              </ul>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-toolbar__item pf-m-pagination">
        <div
          class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
        >
          <div
            class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
          >
            <div class="pf-c-options-menu">
              <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                <span class="pf-c-options-menu__toggle-text">
                  <b>1 - 10</b>&nbsp;of&nbsp;
                  <b>36</b>
                </span>
                <button
                  class="pf-c-options-menu__toggle-button"
                  id="pagination-options-menu-bottom-example-toggle"
                  aria-haspopup="listbox"
                  aria-expanded="false"
                  aria-label="Items per page"
                >
                  <span class="pf-c-options-menu__toggle-button-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <ul
                class="pf-c-options-menu__menu"
                aria-labelledby="pagination-options-menu-bottom-example-toggle"
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
              <div class="pf-c-pagination__nav-control pf-m-prev">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  disabled
                  aria-label="Go to previous page"
                >
                  <i class="fas fa-angle-left" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-pagination__nav-control pf-m-next">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Go to next page"
                >
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </button>
              </div>
            </nav>
          </div>
        </div>
      </div>
    </div>
    <div
      class="pf-c-toolbar__expandable-content pf-m-expanded"
      id="toolbar-attribute-value-checkbox-select-filter-mobile-example-expandable-content"
    >
      <div class="pf-c-toolbar__group pf-m-filter-group">
        <div class="pf-c-toolbar__item">
          <div class="pf-c-select">
            <span
              id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-status-expanded-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-status-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-status-expanded-label toolbar-attribute-value-checkbox-select-filter-mobile-example-select-status-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-icon">
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </span>
                <span class="pf-c-select__toggle-text">Status</span>
              </div>
              <span class="pf-c-select__toggle-arrow">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>

            <ul
              class="pf-c-select__menu"
              role="listbox"
              aria-labelledby="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-status-expanded-label"
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
              id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-label"
              hidden
            >Choose one</span>

            <button
              class="pf-c-select__toggle"
              type="button"
              id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-toggle"
              aria-haspopup="true"
              aria-expanded="false"
              aria-labelledby="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-label toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-toggle"
            >
              <div class="pf-c-select__toggle-wrapper">
                <span class="pf-c-select__toggle-text">Filter by status</span>
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
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-active"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-active"
                    name="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-active"
                  />

                  <span class="pf-c-check__label">Active</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-canceled"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-canceled"
                    name="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-canceled"
                    checked
                  />

                  <span class="pf-c-check__label">Canceled</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-paused"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-paused"
                    name="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-paused"
                    checked
                  />

                  <span class="pf-c-check__label">Paused</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-warning"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-warning"
                    name="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-warning"
                  />

                  <span class="pf-c-check__label">Warning</span>
                </label>
                <label
                  class="pf-c-check pf-c-select__menu-item"
                  for="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-restarted"
                >
                  <input
                    class="pf-c-check__input"
                    type="checkbox"
                    id="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-restarted"
                    name="toolbar-attribute-value-checkbox-select-filter-mobile-example-select-filter-status-expanded-restarted"
                    checked
                  />

                  <span class="pf-c-check__label">Restarted</span>
                </label>
              </fieldset>
            </div>
          </div>
        </div>
        <div class="pf-c-toolbar__item pf-m-chip-group">
          <div class="pf-c-chip-group pf-m-category">
            <div class="pf-c-chip-group__main">
              <span
                class="pf-c-chip-group__label"
                aria-hidden="true"
                id="toolbar-attribute-value-checkbox-select-filter-mobile-example-chip-group-label"
              >Status</span>
              <ul
                class="pf-c-chip-group__list"
                role="list"
                aria-labelledby="toolbar-attribute-value-checkbox-select-filter-mobile-example-chip-group-label"
              >
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span class="pf-c-chip__text" id="chip-one">Canceled</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="remove-chip-one chip-one"
                      aria-label="Remove"
                      id="remove-chip-one"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </div>
                </li>
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span class="pf-c-chip__text" id="chip-two">Paused</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="remove-chip-two chip-two"
                      aria-label="Remove"
                      id="remove-chip-two"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </div>
                </li>
                <li class="pf-c-chip-group__list-item">
                  <div class="pf-c-chip">
                    <span class="pf-c-chip__text" id="chip-three">Restarted</span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-labelledby="remove-chip-three chip-three"
                      aria-label="Remove"
                      id="remove-chip-three"
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

```

### Toolbar pagination management on mobile

```html isFullscreen
<div class="pf-c-page" id="toolbar-pagination-management">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-toolbar-pagination-management"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="toolbar-pagination-management-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="toolbar-pagination-management-primary-nav"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </div>
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>

    <div class="pf-c-page__header-tools">
      <div class="pf-c-page__header-tools-group">
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Settings"
          >
            <i class="fas fa-cog" aria-hidden="true"></i>
          </button>
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Help"
          >
            <i class="pf-icon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-page__header-tools-group">
        <div class="pf-c-page__header-tools-item pf-m-hidden-on-lg">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="toolbar-pagination-management-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="toolbar-pagination-management-dropdown-kebab-1-button"
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
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-md"
        >
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="toolbar-pagination-management-dropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
            >
              <span class="pf-c-dropdown__toggle-text">John Smith</span>
              <span class="pf-c-dropdown__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>
            <ul
              class="pf-c-dropdown__menu"
              aria-labelledby="toolbar-pagination-management-dropdown-kebab-2-button"
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
        </div>
      </div>
      <img
        class="pf-c-avatar"
        src="/assets/images/img_avatar.svg"
        alt="Avatar image"
      />
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="toolbar-pagination-management-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-toolbar-pagination-management"
  >
    <section class="pf-c-page__main-breadcrumb pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
          <ol class="pf-c-breadcrumb__list">
            <li class="pf-c-breadcrumb__item">
              <a href="#" class="pf-c-breadcrumb__link">Section home</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a
                href="#"
                class="pf-c-breadcrumb__link pf-m-current"
                aria-current="page"
              >Section landing</a>
            </li>
          </ol>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section">
      <div
        class="pf-c-toolbar pf-m-nowrap"
        id="toolbar-pagination-management-mobile-example"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section pf-m-nowrap">
            <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show">
              <div class="pf-c-toolbar__toggle">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Show filters"
                  aria-expanded="false"
                  aria-controls="toolbar-pagination-management-mobile-example-expandable-content"
                >
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-toolbar__group pf-m-filter-group">
                <div class="pf-c-toolbar__item pf-m-search-filter">
                  <div class="pf-c-input-group">
                    <div class="pf-c-select" style="width: 150px">
                      <span
                        id="toolbar-pagination-management-mobile-example-select-name-label"
                        hidden
                      >Choose one</span>

                      <button
                        class="pf-c-select__toggle"
                        type="button"
                        id="toolbar-pagination-management-mobile-example-select-name-toggle"
                        aria-haspopup="true"
                        aria-expanded="false"
                        aria-labelledby="toolbar-pagination-management-mobile-example-select-name-label toolbar-pagination-management-mobile-example-select-name-toggle"
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
                        aria-labelledby="toolbar-pagination-management-mobile-example-select-name-label"
                        hidden
                        style="width: 150px"
                      >
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Running</button>
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
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Down</button>
                        </li>
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Degraded</button>
                        </li>
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Needs maintenance</button>
                        </li>
                      </ul>
                    </div>
                    <input
                      class="pf-c-form-control"
                      id="toolbar-pagination-management-mobile-example-textInput11"
                      name="textInput11"
                      type="search"
                      placeholder="Filter by name..."
                      aria-label="Search input example"
                    />
                    <button
                      class="pf-c-button pf-m-control"
                      type="button"
                      aria-label="Search button for search input"
                    >
                      <i class="fas fa-search" aria-hidden="true"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-overflow-menu">
              <div
                class="pf-c-overflow-menu"
                id="toolbar-pagination-management-mobile-example-overflow-menu"
              >
                <div class="pf-c-overflow-menu__control">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                      type="button"
                      id="toolbar-pagination-management-mobile-example-overflow-menu-dropdown-toggle"
                      aria-label="Overflow menu"
                      aria-expanded="false"
                    >
                      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu"
                      aria-labelledby="toolbar-pagination-management-mobile-example-overflow-menu-dropdown-toggle"
                      hidden
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
                    </ul>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-pagination">
              <div
                class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
              >
                <div
                  class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
                >
                  <div class="pf-c-options-menu">
                    <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                      <span class="pf-c-options-menu__toggle-text">
                        <b>1 - 10</b>&nbsp;of&nbsp;
                        <b>36</b>
                      </span>
                      <button
                        class="pf-c-options-menu__toggle-button"
                        id="pagination-options-menu-bottom-example-toggle"
                        aria-haspopup="listbox"
                        aria-expanded="false"
                        aria-label="Items per page"
                      >
                        <span class="pf-c-options-menu__toggle-button-icon">
                          <i class="fas fa-caret-down" aria-hidden="true"></i>
                        </span>
                      </button>
                    </div>
                    <ul
                      class="pf-c-options-menu__menu"
                      aria-labelledby="pagination-options-menu-bottom-example-toggle"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-options-menu__menu-item"
                          type="button"
                        >5 per page</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-options-menu__menu-item"
                          type="button"
                        >
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
                    <div class="pf-c-pagination__nav-control pf-m-prev">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        disabled
                        aria-label="Go to previous page"
                      >
                        <i class="fas fa-angle-left" aria-hidden="true"></i>
                      </button>
                    </div>
                    <div class="pf-c-pagination__nav-control pf-m-next">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Go to next page"
                      >
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </button>
                    </div>
                  </nav>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__expandable-content pf-m-hidden"
            id="toolbar-pagination-management-mobile-example-expandable-content"
            hidden
          ></div>
        </div>
      </div>
      <div>
        <table
          class="pf-c-table pf-m-grid-md"
          role="grid"
          aria-label="This is a table with checkboxes"
          id="toolbar-and-table-static-search-overflow-menu-collapsed-table"
        >
          <thead>
            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="check-all"
                  aria-label="Select all rows"
                />
              </td>
              <th role="columnheader" scope="col">Repositories</th>
              <th role="columnheader" scope="col">Branches</th>
              <th role="columnheader" scope="col">Pull requests</th>
              <th role="columnheader" scope="col">Workspaces</th>
              <th role="columnheader" scope="col">Last commit</th>
              <td role="cell"></td>

              <td role="cell"></td>
            </tr>
          </thead>

          <tbody role="rowgroup">
            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow1"
                  aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-node1"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-node1"
                  >Node 1</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 10
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 25
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 5
                </span>
              </td>
              <td role="cell" data-label="Last commit">2 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-1-button"
                    hidden
                  >
                    <li>
                      <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                    </li>
                    <li>
                      <button
                        class="pf-c-dropdown__menu-item"
                        type="button"
                      >Action</button>
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
                      <a
                        class="pf-c-dropdown__menu-item"
                        href="#"
                      >Separated link</a>
                    </li>
                  </ul>
                </div>
              </td>
            </tr>

            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow2"
                  aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-node2"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-node2"
                  >Node 2</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 8
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 30
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 2
                </span>
              </td>
              <td role="cell" data-label="Last commit">2 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-2-button"
                    hidden
                  >
                    <li>
                      <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                    </li>
                    <li>
                      <button
                        class="pf-c-dropdown__menu-item"
                        type="button"
                      >Action</button>
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
                      <a
                        class="pf-c-dropdown__menu-item"
                        href="#"
                      >Separated link</a>
                    </li>
                  </ul>
                </div>
              </td>
            </tr>

            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow3"
                  aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-node3"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-node3"
                  >Node 3</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 12
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 48
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 13
                </span>
              </td>
              <td role="cell" data-label="Last commit">30 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-3-button"
                    hidden
                  >
                    <li>
                      <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                    </li>
                    <li>
                      <button
                        class="pf-c-dropdown__menu-item"
                        type="button"
                      >Action</button>
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
                      <a
                        class="pf-c-dropdown__menu-item"
                        href="#"
                      >Separated link</a>
                    </li>
                  </ul>
                </div>
              </td>
            </tr>

            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow4"
                  aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-node4"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-node4"
                  >Node 4</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 3
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 8
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 20
                </span>
              </td>
              <td role="cell" data-label="Last commit">8 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-4-button"
                    hidden
                  >
                    <li>
                      <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                    </li>
                    <li>
                      <button
                        class="pf-c-dropdown__menu-item"
                        type="button"
                      >Action</button>
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
                      <a
                        class="pf-c-dropdown__menu-item"
                        href="#"
                      >Separated link</a>
                    </li>
                  </ul>
                </div>
              </td>
            </tr>

            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow5"
                  aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-node5"
                />
              </td>
              <td role="cell" data-label="Repository name">
                <div>
                  <div
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-node5"
                  >Node 5</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </td>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 34
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 21
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 26
                </span>
              </td>
              <td role="cell" data-label="Last commit">2 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="toolbar-and-table-static-search-overflow-menu-collapsed-table-dropdown-kebab-5-button"
                    hidden
                  >
                    <li>
                      <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                    </li>
                    <li>
                      <button
                        class="pf-c-dropdown__menu-item"
                        type="button"
                      >Action</button>
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
                      <a
                        class="pf-c-dropdown__menu-item"
                        href="#"
                      >Separated link</a>
                    </li>
                  </ul>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="pf-c-pagination pf-m-bottom">
          <div class="pf-c-options-menu pf-m-top">
            <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
              <span class="pf-c-options-menu__toggle-text">
                <b>1 - 10</b>&nbsp;of&nbsp;
                <b>36</b>
              </span>
              <button
                class="pf-c-options-menu__toggle-button"
                id="pagination-options-menu-bottom-collapsed-example-toggle"
                aria-haspopup="listbox"
                aria-expanded="false"
                aria-label="Items per page"
              >
                <span class="pf-c-options-menu__toggle-button-icon">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
            </div>
            <ul
              class="pf-c-options-menu__menu pf-m-top"
              aria-labelledby="pagination-options-menu-bottom-collapsed-example-toggle"
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
            <div class="pf-c-pagination__nav-control pf-m-first">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                disabled
                aria-label="Go to first page"
              >
                <i class="fas fa-angle-double-left" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-pagination__nav-control pf-m-prev">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                disabled
                aria-label="Go to previous page"
              >
                <i class="fas fa-angle-left" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-pagination__nav-page-select">
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
            <div class="pf-c-pagination__nav-control pf-m-next">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Go to next page"
              >
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-pagination__nav-control pf-m-last">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Go to last page"
              >
                <i class="fas fa-angle-double-right" aria-hidden="true"></i>
              </button>
            </div>
          </nav>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Toolbar expand all (collapsed)

```html isFullscreen
<div class="pf-c-page" id="toolbar-expand-all-collapsed">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-toolbar-expand-all-collapsed"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="toolbar-expand-all-collapsed-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="toolbar-expand-all-collapsed-primary-nav"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </div>
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>

    <div class="pf-c-page__header-tools">
      <div class="pf-c-page__header-tools-group">
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Settings"
          >
            <i class="fas fa-cog" aria-hidden="true"></i>
          </button>
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Help"
          >
            <i class="pf-icon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-page__header-tools-group">
        <div class="pf-c-page__header-tools-item pf-m-hidden-on-lg">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="toolbar-expand-all-collapsed-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="toolbar-expand-all-collapsed-dropdown-kebab-1-button"
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
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-md"
        >
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="toolbar-expand-all-collapsed-dropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
            >
              <span class="pf-c-dropdown__toggle-text">John Smith</span>
              <span class="pf-c-dropdown__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>
            <ul
              class="pf-c-dropdown__menu"
              aria-labelledby="toolbar-expand-all-collapsed-dropdown-kebab-2-button"
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
        </div>
      </div>
      <img
        class="pf-c-avatar"
        src="/assets/images/img_avatar.svg"
        alt="Avatar image"
      />
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="toolbar-expand-all-collapsed-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-toolbar-expand-all-collapsed"
  >
    <section class="pf-c-page__main-breadcrumb pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
          <ol class="pf-c-breadcrumb__list">
            <li class="pf-c-breadcrumb__item">
              <a href="#" class="pf-c-breadcrumb__link">Section home</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a
                href="#"
                class="pf-c-breadcrumb__link pf-m-current"
                aria-current="page"
              >Section landing</a>
            </li>
          </ol>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section">
      <div
        class="pf-c-toolbar pf-m-nowrap pf-m-page-insets"
        id="toolbar-expand-all-collapsed-mobile-example"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section pf-m-nowrap">
            <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show">
              <div class="pf-c-toolbar__toggle">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Show filters"
                  aria-expanded="false"
                  aria-controls="toolbar-expand-all-collapsed-mobile-example-expandable-content"
                >
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-toolbar__group pf-m-icon-button-group">
                <div class="pf-c-toolbar__item pf-m-expand-all">
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-label="Expand all"
                  >
                    <span class="pf-c-toolbar__expand-all-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </span>
                  </button>
                </div>
                <div class="pf-c-toolbar__item pf-m-bulk-select">
                  <div class="pf-c-dropdown">
                    <div class="pf-c-dropdown__toggle pf-m-split-button">
                      <label
                        class="pf-c-dropdown__toggle-check"
                        for="toolbar-expand-all-collapsed-mobile-example-bulk-select-toggle-check"
                      >
                        <input
                          type="checkbox"
                          id="toolbar-expand-all-collapsed-mobile-example-bulk-select-toggle-check"
                          aria-label="Select all"
                        />
                      </label>

                      <button
                        class="pf-c-dropdown__toggle-button"
                        type="button"
                        aria-expanded="false"
                        id="toolbar-expand-all-collapsed-mobile-example-bulk-select-toggle-button"
                        aria-label="Dropdown toggle"
                      >
                        <i class="fas fa-caret-down" aria-hidden="true"></i>
                      </button>
                    </div>
                    <ul class="pf-c-dropdown__menu" hidden>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Select all</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Select none</button>
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
              </div>
              <div class="pf-c-toolbar__group pf-m-filter-group">
                <div class="pf-c-toolbar__item pf-m-search-filter">
                  <div class="pf-c-input-group">
                    <div class="pf-c-select" style="width: 150px">
                      <span
                        id="toolbar-expand-all-collapsed-mobile-example-select-name-label"
                        hidden
                      >Choose one</span>

                      <button
                        class="pf-c-select__toggle"
                        type="button"
                        id="toolbar-expand-all-collapsed-mobile-example-select-name-toggle"
                        aria-haspopup="true"
                        aria-expanded="false"
                        aria-labelledby="toolbar-expand-all-collapsed-mobile-example-select-name-label toolbar-expand-all-collapsed-mobile-example-select-name-toggle"
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
                        aria-labelledby="toolbar-expand-all-collapsed-mobile-example-select-name-label"
                        hidden
                        style="width: 150px"
                      >
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Running</button>
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
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Down</button>
                        </li>
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Degraded</button>
                        </li>
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Needs maintenance</button>
                        </li>
                      </ul>
                    </div>
                    <input
                      class="pf-c-form-control"
                      id="toolbar-expand-all-collapsed-mobile-example-textInput11"
                      name="textInput11"
                      type="search"
                      placeholder="Filter by name..."
                      aria-label="Search input example"
                    />
                    <button
                      class="pf-c-button pf-m-control"
                      type="button"
                      aria-label="Search button for search input"
                    >
                      <i class="fas fa-search" aria-hidden="true"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-overflow-menu">
              <div
                class="pf-c-overflow-menu"
                id="toolbar-expand-all-collapsed-mobile-example-overflow-menu"
              >
                <div class="pf-c-overflow-menu__control">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                      type="button"
                      id="toolbar-expand-all-collapsed-mobile-example-overflow-menu-dropdown-toggle"
                      aria-label="Overflow menu"
                      aria-expanded="false"
                    >
                      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu"
                      aria-labelledby="toolbar-expand-all-collapsed-mobile-example-overflow-menu-dropdown-toggle"
                      hidden
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
                    </ul>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-pagination">
              <div
                class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
              >
                <div
                  class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
                >
                  <div class="pf-c-options-menu">
                    <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                      <span class="pf-c-options-menu__toggle-text">
                        <b>1 - 10</b>&nbsp;of&nbsp;
                        <b>36</b>
                      </span>
                      <button
                        class="pf-c-options-menu__toggle-button"
                        id="pagination-options-menu-bottom-example-toggle"
                        aria-haspopup="listbox"
                        aria-expanded="false"
                        aria-label="Items per page"
                      >
                        <span class="pf-c-options-menu__toggle-button-icon">
                          <i class="fas fa-caret-down" aria-hidden="true"></i>
                        </span>
                      </button>
                    </div>
                    <ul
                      class="pf-c-options-menu__menu"
                      aria-labelledby="pagination-options-menu-bottom-example-toggle"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-options-menu__menu-item"
                          type="button"
                        >5 per page</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-options-menu__menu-item"
                          type="button"
                        >
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
                    <div class="pf-c-pagination__nav-control pf-m-prev">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        disabled
                        aria-label="Go to previous page"
                      >
                        <i class="fas fa-angle-left" aria-hidden="true"></i>
                      </button>
                    </div>
                    <div class="pf-c-pagination__nav-control pf-m-next">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Go to next page"
                      >
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </button>
                    </div>
                  </nav>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__expandable-content pf-m-hidden"
            id="toolbar-expand-all-collapsed-mobile-example-expandable-content"
            hidden
          ></div>
        </div>
      </div>
      <table
        class="pf-c-table pf-m-expandable pf-m-grid-lg"
        role="grid"
        aria-label="Collapsed table example"
        id="table-collapsed"
      >
        <thead>
          <tr role="row">
            <td></td>
            <td class="pf-c-table__check" role="cell">
              <input
                type="checkbox"
                name="table-collapsed-check-all"
                aria-label="Select all rows"
              />
            </td>
            <th
              class="pf-m-width-30 pf-c-table__sort pf-m-selected"
              role="columnheader"
              aria-sort="ascending"
              scope="col"
            >
              <button class="pf-c-table__button">
                <div class="pf-c-table__button-content">
                  <span class="pf-c-table__text">Repositories</span>
                  <span class="pf-c-table__sort-indicator">
                    <i class="fas fa-long-arrow-alt-up"></i>
                  </span>
                </div>
              </button>
            </th>
            <th
              class="pf-c-table__sort"
              role="columnheader"
              aria-sort="none"
              scope="col"
            >
              <button class="pf-c-table__button">
                <div class="pf-c-table__button-content">
                  <span class="pf-c-table__text">Branches</span>
                  <span class="pf-c-table__sort-indicator">
                    <i class="fas fa-arrows-alt-v"></i>
                  </span>
                </div>
              </button>
            </th>
            <th
              class="pf-c-table__sort"
              role="columnheader"
              aria-sort="none"
              scope="col"
            >
              <button class="pf-c-table__button">
                <div class="pf-c-table__button-content">
                  <span class="pf-c-table__text">Pull requests</span>
                  <span class="pf-c-table__sort-indicator">
                    <i class="fas fa-arrows-alt-v"></i>
                  </span>
                </div>
              </button>
            </th>
            <td></td>
            <td></td>
          </tr>
        </thead>

        <tbody role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain"
                aria-labelledby="table-collapsed-node1 table-collapsed-expandable-toggle1"
                id="table-collapsed-expandable-toggle1"
                aria-label="Details"
                aria-controls="table-collapsed-content1"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <td class="pf-c-table__check" role="cell">
              <input
                type="checkbox"
                name="table-collapsed-checkrow1"
                aria-labelledby="table-collapsed-node1"
              />
            </td>
            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="table-collapsed-node1">Node 1</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">10</td>
            <td role="cell" data-label="Pull requests">25</td>
            <td role="cell" data-label="Action">
              <a href="#">Link 1</a>
            </td>
            <td class="pf-c-table__action" role="cell">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="table-collapsed-dropdown-kebab-1-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="table-collapsed-dropdown-kebab-1-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
            <td></td>
            <td></td>
            <td role="cell" colspan="4" id="table-collapsed-content1">
              <div
                class="pf-c-table__expandable-row-content"
              >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
            </td>
            <td></td>
          </tr>
        </tbody>

        <tbody role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain"
                aria-labelledby="table-collapsed-node2 table-collapsed-expandable-toggle2"
                id="table-collapsed-expandable-toggle2"
                aria-label="Details"
                aria-controls="table-collapsed-content2"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <td class="pf-c-table__check" role="cell">
              <input
                type="checkbox"
                name="table-collapsed-checkrow2"
                aria-labelledby="table-collapsed-node2"
              />
            </td>
            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="table-collapsed-node2">Node 2</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">10</td>
            <td role="cell" data-label="Pull requests">25</td>
            <td role="cell" data-label="Action">
              <a href="#">Link 2</a>
            </td>
            <td class="pf-c-table__action" role="cell">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="table-collapsed-dropdown-kebab-2-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="table-collapsed-dropdown-kebab-2-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row" role="row">
            <td role="cell" colspan="7" id="table-collapsed-content2">
              <div
                class="pf-c-table__expandable-row-content"
              >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
            </td>
          </tr>
        </tbody>

        <tbody role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain"
                aria-labelledby="table-collapsed-node3 expandable-toggle3"
                id="expandable-toggle3"
                aria-label="Details"
                aria-controls="table-collapsed-content3"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <td class="pf-c-table__check" role="cell">
              <input
                type="checkbox"
                name="table-collapsed-checkrow3"
                aria-labelledby="table-collapsed-node3"
              />
            </td>
            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="table-collapsed-node3">Node 3</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">10</td>
            <td role="cell" data-label="Pull requests">25</td>
            <td role="cell" data-label="Action">
              <a href="#">Link 3</a>
            </td>
            <td class="pf-c-table__action" role="cell">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="table-collapsed-dropdown-kebab-3-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="table-collapsed-dropdown-kebab-3-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
            <td role="cell" colspan="7" id="table-collapsed-content3">
              <div
                class="pf-c-table__expandable-row-content"
              >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
            </td>
          </tr>
        </tbody>

        <tbody role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain"
                aria-labelledby="table-collapsed-node4 expandable-toggle4"
                id="expandable-toggle4"
                aria-label="Details"
                aria-controls="table-collapsed-content4"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <td class="pf-c-table__check" role="cell">
              <input
                type="checkbox"
                name="table-collapsed-checkrow4"
                aria-labelledby="table-collapsed-node4"
              />
            </td>
            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="table-collapsed-node4">Node 4</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">10</td>
            <td role="cell" data-label="Pull requests">25</td>
            <td role="cell" data-label="Action">
              <a href="#">Link 4</a>
            </td>
            <td class="pf-c-table__action" role="cell">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="table-collapsed-dropdown-kebab-4-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="table-collapsed-dropdown-kebab-4-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
            <td
              class="pf-m-no-padding"
              role="cell"
              colspan="7"
              id="table-collapsed-content4"
            >
              <div
                class="pf-c-table__expandable-row-content"
              >Expandable row content has no padding.</div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </main>
</div>

```

### Toolbar expand all (expanded)

```html isFullscreen
<div class="pf-c-page" id="toolbar-expand-all-expanded">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-toolbar-expand-all-expanded"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="toolbar-expand-all-expanded-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="toolbar-expand-all-expanded-primary-nav"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </div>
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>

    <div class="pf-c-page__header-tools">
      <div class="pf-c-page__header-tools-group">
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Settings"
          >
            <i class="fas fa-cog" aria-hidden="true"></i>
          </button>
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Help"
          >
            <i class="pf-icon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-page__header-tools-group">
        <div class="pf-c-page__header-tools-item pf-m-hidden-on-lg">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="toolbar-expand-all-expanded-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="toolbar-expand-all-expanded-dropdown-kebab-1-button"
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
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-md"
        >
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="toolbar-expand-all-expanded-dropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
            >
              <span class="pf-c-dropdown__toggle-text">John Smith</span>
              <span class="pf-c-dropdown__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>
            <ul
              class="pf-c-dropdown__menu"
              aria-labelledby="toolbar-expand-all-expanded-dropdown-kebab-2-button"
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
        </div>
      </div>
      <img
        class="pf-c-avatar"
        src="/assets/images/img_avatar.svg"
        alt="Avatar image"
      />
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="toolbar-expand-all-expanded-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-toolbar-expand-all-expanded"
  >
    <section class="pf-c-page__main-breadcrumb pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
          <ol class="pf-c-breadcrumb__list">
            <li class="pf-c-breadcrumb__item">
              <a href="#" class="pf-c-breadcrumb__link">Section home</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a
                href="#"
                class="pf-c-breadcrumb__link pf-m-current"
                aria-current="page"
              >Section landing</a>
            </li>
          </ol>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section">
      <div
        class="pf-c-toolbar pf-m-nowrap pf-m-page-insets"
        id="toolbar-expand-all-expanded-mobile-example"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section pf-m-nowrap">
            <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show">
              <div class="pf-c-toolbar__toggle">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Show filters"
                  aria-expanded="false"
                  aria-controls="toolbar-expand-all-expanded-mobile-example-expandable-content"
                >
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-toolbar__item pf-m-expand-all pf-m-expanded">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Collapse all"
                >
                  <span class="pf-c-toolbar__expand-all-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div class="pf-c-toolbar__group pf-m-filter-group">
                <div class="pf-c-toolbar__item pf-m-search-filter">
                  <div class="pf-c-input-group">
                    <div class="pf-c-select" style="width: 150px">
                      <span
                        id="toolbar-expand-all-expanded-mobile-example-select-name-label"
                        hidden
                      >Choose one</span>

                      <button
                        class="pf-c-select__toggle"
                        type="button"
                        id="toolbar-expand-all-expanded-mobile-example-select-name-toggle"
                        aria-haspopup="true"
                        aria-expanded="false"
                        aria-labelledby="toolbar-expand-all-expanded-mobile-example-select-name-label toolbar-expand-all-expanded-mobile-example-select-name-toggle"
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
                        aria-labelledby="toolbar-expand-all-expanded-mobile-example-select-name-label"
                        hidden
                        style="width: 150px"
                      >
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Running</button>
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
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Down</button>
                        </li>
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Degraded</button>
                        </li>
                        <li role="presentation">
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Needs maintenance</button>
                        </li>
                      </ul>
                    </div>
                    <input
                      class="pf-c-form-control"
                      id="toolbar-expand-all-expanded-mobile-example-textInput11"
                      name="textInput11"
                      type="search"
                      placeholder="Filter by name..."
                      aria-label="Search input example"
                    />
                    <button
                      class="pf-c-button pf-m-control"
                      type="button"
                      aria-label="Search button for search input"
                    >
                      <i class="fas fa-search" aria-hidden="true"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-overflow-menu">
              <div
                class="pf-c-overflow-menu"
                id="toolbar-expand-all-expanded-mobile-example-overflow-menu"
              >
                <div class="pf-c-overflow-menu__control">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                      type="button"
                      id="toolbar-expand-all-expanded-mobile-example-overflow-menu-dropdown-toggle"
                      aria-label="Overflow menu"
                      aria-expanded="false"
                    >
                      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu"
                      aria-labelledby="toolbar-expand-all-expanded-mobile-example-overflow-menu-dropdown-toggle"
                      hidden
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
                    </ul>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-pagination">
              <div
                class="pf-c-pagination pf-m-compact pf-m-hidden pf-m-visible-on-md"
              >
                <div
                  class="pf-c-pagination pf-m-compact pf-m-compact pf-m-hidden pf-m-visible-on-md"
                >
                  <div class="pf-c-options-menu">
                    <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                      <span class="pf-c-options-menu__toggle-text">
                        <b>1 - 10</b>&nbsp;of&nbsp;
                        <b>36</b>
                      </span>
                      <button
                        class="pf-c-options-menu__toggle-button"
                        id="pagination-options-menu-bottom-example-toggle"
                        aria-haspopup="listbox"
                        aria-expanded="false"
                        aria-label="Items per page"
                      >
                        <span class="pf-c-options-menu__toggle-button-icon">
                          <i class="fas fa-caret-down" aria-hidden="true"></i>
                        </span>
                      </button>
                    </div>
                    <ul
                      class="pf-c-options-menu__menu"
                      aria-labelledby="pagination-options-menu-bottom-example-toggle"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-options-menu__menu-item"
                          type="button"
                        >5 per page</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-options-menu__menu-item"
                          type="button"
                        >
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
                    <div class="pf-c-pagination__nav-control pf-m-prev">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        disabled
                        aria-label="Go to previous page"
                      >
                        <i class="fas fa-angle-left" aria-hidden="true"></i>
                      </button>
                    </div>
                    <div class="pf-c-pagination__nav-control pf-m-next">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Go to next page"
                      >
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </button>
                    </div>
                  </nav>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__expandable-content pf-m-hidden"
            id="toolbar-expand-all-expanded-mobile-example-expandable-content"
            hidden
          ></div>
        </div>
      </div>
      <table
        class="pf-c-table pf-m-expandable pf-m-grid-xl"
        role="grid"
        aria-label="Expanded table example"
        id="toolbar-expand-all-expanded-table"
      >
        <thead>
          <tr role="row">
            <td role="cell"></td>

            <th
              class="pf-m-width-30"
              role="columnheader"
              scope="col"
            >Repositories</th>
            <th role="columnheader" scope="col">Branches</th>
            <th role="columnheader" scope="col">Pull requests</th>
            <th role="columnheader" scope="col">Work spaces</th>
            <th role="columnheader" scope="col">Last commit</th>
            <td role="cell"></td>
          </tr>
        </thead>

        <tbody class="pf-m-expanded" role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain pf-m-expanded"
                aria-labelledby="toolbar-expand-all-expanded-table-node1 expandable-toggle1"
                id="expandable-toggle1"
                aria-label="Details"
                aria-controls="toolbar-expand-all-expanded-table-content1"
                aria-expanded="true"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="toolbar-expand-all-expanded-table-node1">Node 2</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">
              <span>
                <i class="fas fa-code-branch"></i>
                <span>10</span>
              </span>
            </td>
            <td role="cell" data-label="Pull requests">
              <span>
                <i class="fas fa-code"></i>
                <span>5</span>
              </span>
            </td>
            <td role="cell" data-label="Work spaces">
              <span>
                <i class="fas fa-cube"></i>
                <span>8</span>
              </span>
            </td>
            <td role="cell" data-label="Last commit">2 days ago</td>
            <td role="cell" data-label="Action">
              <a href="#">Action link</a>
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
            <td role="cell"></td>

            <td
              class
              role="cell"
              colspan="6"
              id="toolbar-expand-all-expanded-table-content1"
            >
              <div class="pf-c-table__expandable-row-content">
                Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
                tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
                quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
                consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
                cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
                proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
              </div>
            </td>
          </tr>
        </tbody>

        <tbody class="pf-m-expanded" role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain pf-m-expanded"
                aria-labelledby="toolbar-expand-all-expanded-table-node2 expandable-toggle2"
                id="expandable-toggle2"
                aria-label="Details"
                aria-controls="toolbar-expand-all-expanded-table-content2"
                aria-expanded="true"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="toolbar-expand-all-expanded-table-node2">Node 2</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">
              <span>
                <i class="fas fa-code-branch"></i>
                <span>10</span>
              </span>
            </td>
            <td role="cell" data-label="Pull requests">
              <span>
                <i class="fas fa-code"></i>
                <span>5</span>
              </span>
            </td>
            <td role="cell" data-label="Work spaces">
              <span>
                <i class="fas fa-cube"></i>
                <span>8</span>
              </span>
            </td>
            <td role="cell" data-label="Last commit">2 days ago</td>
            <td role="cell" data-label="Action">
              <a href="#">Action link</a>
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
            <td role="cell"></td>

            <td
              class
              role="cell"
              colspan="6"
              id="toolbar-expand-all-expanded-table-content2"
            >
              <div class="pf-c-table__expandable-row-content">
                Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
                tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
                quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
                consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
                cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
                proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
              </div>
            </td>
          </tr>
        </tbody>

        <tbody class="pf-m-expanded" role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain pf-m-expanded"
                aria-labelledby="toolbar-expand-all-expanded-table-node3 expandable-toggle3"
                id="expandable-toggle3"
                aria-label="Details"
                aria-controls="toolbar-expand-all-expanded-table-content3"
                aria-expanded="true"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="toolbar-expand-all-expanded-table-node3">Node 2</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">
              <span>
                <i class="fas fa-code-branch"></i>
                <span>10</span>
              </span>
            </td>
            <td role="cell" data-label="Pull requests">
              <span>
                <i class="fas fa-code"></i>
                <span>5</span>
              </span>
            </td>
            <td role="cell" data-label="Work spaces">
              <span>
                <i class="fas fa-cube"></i>
                <span>8</span>
              </span>
            </td>
            <td role="cell" data-label="Last commit">2 days ago</td>
            <td role="cell" data-label="Action">
              <a href="#">Action link</a>
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
            <td role="cell"></td>

            <td
              class
              role="cell"
              colspan="6"
              id="toolbar-expand-all-expanded-table-content3"
            >
              <div class="pf-c-table__expandable-row-content">
                Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
                tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
                quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
                consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
                cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
                proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
              </div>
            </td>
          </tr>
        </tbody>

        <tbody class="pf-m-expanded" role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain pf-m-expanded"
                aria-labelledby="toolbar-expand-all-expanded-table-node4 expandable-toggle4"
                id="expandable-toggle4"
                aria-label="Details"
                aria-controls="toolbar-expand-all-expanded-table-content4"
                aria-expanded="true"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="toolbar-expand-all-expanded-table-node4">Node 2</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">
              <span>
                <i class="fas fa-code-branch"></i>
                <span>10</span>
              </span>
            </td>
            <td role="cell" data-label="Pull requests">
              <span>
                <i class="fas fa-code"></i>
                <span>5</span>
              </span>
            </td>
            <td role="cell" data-label="Work spaces">
              <span>
                <i class="fas fa-cube"></i>
                <span>8</span>
              </span>
            </td>
            <td role="cell" data-label="Last commit">2 days ago</td>
            <td role="cell" data-label="Action">
              <a href="#">Action link</a>
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
            <td role="cell"></td>

            <td
              class
              role="cell"
              colspan="6"
              id="toolbar-expand-all-expanded-table-content4"
            >
              <div class="pf-c-table__expandable-row-content">
                Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
                tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
                quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
                consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
                cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
                proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
              </div>
            </td>
          </tr>
        </tbody>

        <tbody class="pf-m-expanded" role="rowgroup">
          <tr role="row">
            <td class="pf-c-table__toggle" role="cell">
              <button
                class="pf-c-button pf-m-plain pf-m-expanded"
                aria-labelledby="toolbar-expand-all-expanded-table-node5 expandable-toggle5"
                id="expandable-toggle5"
                aria-label="Details"
                aria-controls="toolbar-expand-all-expanded-table-content5"
                aria-expanded="true"
              >
                <div class="pf-c-table__toggle-icon">
                  <i class="fas fa-angle-down" aria-hidden="true"></i>
                </div>
              </button>
            </td>

            <th role="columnheader" data-label="Repository name">
              <div>
                <div id="toolbar-expand-all-expanded-table-node5">Node 2</div>
                <a href="#">siemur/test-space</a>
              </div>
            </th>
            <td role="cell" data-label="Branches">
              <span>
                <i class="fas fa-code-branch"></i>
                <span>10</span>
              </span>
            </td>
            <td role="cell" data-label="Pull requests">
              <span>
                <i class="fas fa-code"></i>
                <span>5</span>
              </span>
            </td>
            <td role="cell" data-label="Work spaces">
              <span>
                <i class="fas fa-cube"></i>
                <span>8</span>
              </span>
            </td>
            <td role="cell" data-label="Last commit">2 days ago</td>
            <td role="cell" data-label="Action">
              <a href="#">Action link</a>
            </td>
          </tr>

          <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
            <td role="cell"></td>

            <td
              class
              role="cell"
              colspan="6"
              id="toolbar-expand-all-expanded-table-content5"
            >
              <div class="pf-c-table__expandable-row-content">
                Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
                tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
                quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
                consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse
                cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non
                proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </main>
</div>

```
