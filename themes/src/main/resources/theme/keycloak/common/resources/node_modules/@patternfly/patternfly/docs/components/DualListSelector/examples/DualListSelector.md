---
id: 'Dual list selector'
beta: true
section: components
cssPrefix: pf-c-dual-list-selector
---## Examples

### Basic

```html
<div class="pf-c-dual-list-selector">
  <div class="pf-c-dual-list-selector__pane pf-m-available">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Available options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="basic-available-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-basic-available-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-basic-available-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 5 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item1</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item2</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item pf-m-disabled">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button
              class="pf-c-dual-list-selector__item"
              disabled
              type="button"
            >
              <span class="pf-c-dual-list-selector__item-main">
                <span
                  class="pf-c-dual-list-selector__item-text"
                >Item3 (disabled)</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item4</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item5</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__controls">
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Add selected"
      >
        <i class="fas fa-fw fa-angle-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Add all">
        <i class="fas fa-fw fa-angle-double-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove all"
      >
        <i class="fas fa-fw fa-angle-double-left"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove selected"
      >
        <i class="fas fa-fw fa-angle-left"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__pane pf-m-chosen">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Chosen options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="basic-chosen-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-basic-chosen-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-basic-chosen-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 0 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list"></ul>
    </div>
  </div>
</div>

```

### Available item selected

```html
<div class="pf-c-dual-list-selector">
  <div class="pf-c-dual-list-selector__pane pf-m-available">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Available options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="available-item-selected-available-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-available-item-selected-available-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-available-item-selected-available-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">1 of 5 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item1</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-selected">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item2</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item3</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item4</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item5</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__controls">
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Add selected"
      >
        <i class="fas fa-fw fa-angle-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Add all">
        <i class="fas fa-fw fa-angle-double-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove all"
      >
        <i class="fas fa-fw fa-angle-double-left"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove selected"
      >
        <i class="fas fa-fw fa-angle-left"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__pane pf-m-chosen">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Chosen options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="available-item-selected-chosen-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-available-item-selected-chosen-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-available-item-selected-chosen-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 0 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list"></ul>
    </div>
  </div>
</div>

```

### Multiple available items selected

```html
<div class="pf-c-dual-list-selector">
  <div class="pf-c-dual-list-selector__pane pf-m-available">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Available options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="multiple-available-items-selected-available-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-multiple-available-items-selected-available-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-multiple-available-items-selected-available-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">1 of 5 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item1</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-selected">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item2</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-selected">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item3</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item4</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-selected">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item5</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__controls">
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Add selected"
      >
        <i class="fas fa-fw fa-angle-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Add all">
        <i class="fas fa-fw fa-angle-double-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove all"
      >
        <i class="fas fa-fw fa-angle-double-left"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove selected"
      >
        <i class="fas fa-fw fa-angle-left"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__pane pf-m-chosen">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Chosen options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="multiple-available-items-selected-chosen-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-multiple-available-items-selected-chosen-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-multiple-available-items-selected-chosen-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 0 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list"></ul>
    </div>
  </div>
</div>

```

### Chosen item

```html
<div class="pf-c-dual-list-selector">
  <div class="pf-c-dual-list-selector__pane pf-m-available">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Available options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="chosen-item-available-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-chosen-item-available-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-chosen-item-available-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 4 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item1</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item2</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item3</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item4</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item5</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__controls">
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Add selected"
      >
        <i class="fas fa-fw fa-angle-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Add all">
        <i class="fas fa-fw fa-angle-double-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Remove all"
      >
        <i class="fas fa-fw fa-angle-double-left"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove selected"
      >
        <i class="fas fa-fw fa-angle-left"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__pane pf-m-chosen">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Chosen options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="chosen-item-chosen-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-chosen-item-chosen-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-chosen-item-chosen-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 1 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item1</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
</div>

```

### Chosen item selected

```html
<div class="pf-c-dual-list-selector">
  <div class="pf-c-dual-list-selector__pane pf-m-available">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Available options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="chosen-item-selected-available-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-chosen-item-selected-available-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-chosen-item-selected-available-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 4 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item1</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item2</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item3</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item4</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item5</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__controls">
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Add selected"
      >
        <i class="fas fa-fw fa-angle-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Add all">
        <i class="fas fa-fw fa-angle-double-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Remove all"
      >
        <i class="fas fa-fw fa-angle-double-left"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Remove selected"
      >
        <i class="fas fa-fw fa-angle-left"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__pane pf-m-chosen">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Chosen options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="chosen-item-selected-chosen-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-chosen-item-selected-chosen-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-chosen-item-selected-chosen-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">1 of 1 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-selected">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item5</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
</div>

```

### Tree view

```html
<div class="pf-c-dual-list-selector">
  <div class="pf-c-dual-list-selector__pane pf-m-available">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Available options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="basic-available-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-basic-available-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-basic-available-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">1 of 11 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li
          class="pf-c-dual-list-selector__list-item pf-m-expandable pf-m-expanded"
          aria-expanded="true"
        >
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <div class="pf-c-dual-list-selector__item-toggle">
                  <span class="pf-c-dual-list-selector__item-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-0"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Colors</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read">6</span>
              </span>
            </div>
          </div>

          <ul class="pf-c-dual-list-selector__list">
            <li class="pf-c-dual-list-selector__list-item">
              <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
                <div class="pf-c-dual-list-selector__item" tabindex="0">
                  <span class="pf-c-dual-list-selector__item-main">
                    <span class="pf-c-dual-list-selector__item-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-1"
                          aria-label="Dual list selector item check"
                        />
                      </div>
                    </span>
                    <span class="pf-c-dual-list-selector__item-text">Red</span>
                  </span>
                  <span class="pf-c-dual-list-selector__item-count">
                    <span class="pf-c-badge pf-m-read"></span>
                  </span>
                </div>
              </div>
            </li>
            <li class="pf-c-dual-list-selector__list-item">
              <div
                class="pf-c-dual-list-selector__list-item-row pf-m-check pf-m-selected"
              >
                <div class="pf-c-dual-list-selector__item" tabindex="0">
                  <span class="pf-c-dual-list-selector__item-main">
                    <span class="pf-c-dual-list-selector__item-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-2"
                          checked
                          aria-label="Dual list selector item check checked"
                        />
                      </div>
                    </span>
                    <span class="pf-c-dual-list-selector__item-text">Orange</span>
                  </span>
                  <span class="pf-c-dual-list-selector__item-count">
                    <span class="pf-c-badge pf-m-read"></span>
                  </span>
                </div>
              </div>
            </li>
            <li class="pf-c-dual-list-selector__list-item">
              <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
                <div class="pf-c-dual-list-selector__item" tabindex="0">
                  <span class="pf-c-dual-list-selector__item-main">
                    <span class="pf-c-dual-list-selector__item-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-3"
                          aria-label="Dual list selector item check"
                        />
                      </div>
                    </span>
                    <span class="pf-c-dual-list-selector__item-text">Yellow</span>
                  </span>
                  <span class="pf-c-dual-list-selector__item-count">
                    <span class="pf-c-badge pf-m-read"></span>
                  </span>
                </div>
              </div>
            </li>
            <li
              class="pf-c-dual-list-selector__list-item pf-m-expandable pf-m-expanded"
              aria-expanded="true"
            >
              <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
                <div class="pf-c-dual-list-selector__item" tabindex="0">
                  <span class="pf-c-dual-list-selector__item-main">
                    <div class="pf-c-dual-list-selector__item-toggle">
                      <span class="pf-c-dual-list-selector__item-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-dual-list-selector__item-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-4"
                          aria-label="Dual list selector item check"
                        />
                      </div>
                    </span>
                    <span class="pf-c-dual-list-selector__item-text">Green</span>
                  </span>
                  <span class="pf-c-dual-list-selector__item-count">
                    <span class="pf-c-badge pf-m-read"></span>
                  </span>
                </div>
              </div>

              <ul class="pf-c-dual-list-selector__list">
                <li class="pf-c-dual-list-selector__list-item">
                  <div
                    class="pf-c-dual-list-selector__list-item-row pf-m-check"
                  >
                    <div class="pf-c-dual-list-selector__item" tabindex="0">
                      <span class="pf-c-dual-list-selector__item-main">
                        <span class="pf-c-dual-list-selector__item-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="check-5"
                              aria-label="Dual list selector item check"
                            />
                          </div>
                        </span>
                        <span
                          class="pf-c-dual-list-selector__item-text"
                        >Light green</span>
                      </span>
                      <span class="pf-c-dual-list-selector__item-count">
                        <span class="pf-c-badge pf-m-read"></span>
                      </span>
                    </div>
                  </div>
                </li>
                <li class="pf-c-dual-list-selector__list-item">
                  <div
                    class="pf-c-dual-list-selector__list-item-row pf-m-check"
                  >
                    <div class="pf-c-dual-list-selector__item" tabindex="0">
                      <span class="pf-c-dual-list-selector__item-main">
                        <span class="pf-c-dual-list-selector__item-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="check-6"
                              aria-label="Dual list selector item check"
                            />
                          </div>
                        </span>
                        <span
                          class="pf-c-dual-list-selector__item-text"
                        >Medium green</span>
                      </span>
                      <span class="pf-c-dual-list-selector__item-count">
                        <span class="pf-c-badge pf-m-read"></span>
                      </span>
                    </div>
                  </div>
                </li>
                <li class="pf-c-dual-list-selector__list-item">
                  <div
                    class="pf-c-dual-list-selector__list-item-row pf-m-check"
                  >
                    <div class="pf-c-dual-list-selector__item" tabindex="0">
                      <span class="pf-c-dual-list-selector__item-main">
                        <span class="pf-c-dual-list-selector__item-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="check-7"
                              aria-label="Dual list selector item check"
                            />
                          </div>
                        </span>
                        <span
                          class="pf-c-dual-list-selector__item-text"
                        >Dark green</span>
                      </span>
                      <span class="pf-c-dual-list-selector__item-count">
                        <span class="pf-c-badge pf-m-read"></span>
                      </span>
                    </div>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>

        <li
          class="pf-c-dual-list-selector__list-item pf-m-expandable"
          aria-expanded="false"
        >
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <div class="pf-c-dual-list-selector__item-toggle">
                  <span class="pf-c-dual-list-selector__item-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-8"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Type something</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </div>
          </div>
        </li>

        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-9"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Type something</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </div>
          </div>
        </li>

        <li
          class="pf-c-dual-list-selector__list-item pf-m-expandable"
          aria-expanded="false"
        >
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <div class="pf-c-dual-list-selector__item-toggle">
                  <span class="pf-c-dual-list-selector__item-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-10"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Type something</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </div>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__controls">
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Add selected"
      >
        <i class="fas fa-fw fa-angle-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Add all">
        <i class="fas fa-fw fa-angle-double-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove all"
      >
        <i class="fas fa-fw fa-angle-double-left"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove selected"
      >
        <i class="fas fa-fw fa-angle-left"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__pane pf-m-chosen">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Chosen options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="basic-chosen-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-basic-chosen-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-basic-chosen-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 0 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list"></ul>
    </div>
  </div>
</div>

```

### Tree view with chosen and disabled options

```html
<div class="pf-c-dual-list-selector">
  <div class="pf-c-dual-list-selector__pane pf-m-available">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Available options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="basic-available-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-basic-available-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-basic-available-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 10 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li
          class="pf-c-dual-list-selector__list-item pf-m-expandable pf-m-expanded"
          aria-expanded="true"
        >
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <div class="pf-c-dual-list-selector__item-toggle">
                  <span class="pf-c-dual-list-selector__item-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-11"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Colors</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read">6</span>
              </span>
            </div>
          </div>

          <ul class="pf-c-dual-list-selector__list">
            <li class="pf-c-dual-list-selector__list-item">
              <div
                class="pf-c-dual-list-selector__list-item-row pf-m-check pf-m-selected"
              >
                <div class="pf-c-dual-list-selector__item" tabindex="0">
                  <span class="pf-c-dual-list-selector__item-main">
                    <span class="pf-c-dual-list-selector__item-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-12"
                          checked
                          aria-label="Dual list selector item check checked"
                        />
                      </div>
                    </span>
                    <span class="pf-c-dual-list-selector__item-text">Orange</span>
                  </span>
                  <span class="pf-c-dual-list-selector__item-count">
                    <span class="pf-c-badge pf-m-read"></span>
                  </span>
                </div>
              </div>
            </li>
            <li class="pf-c-dual-list-selector__list-item">
              <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
                <div class="pf-c-dual-list-selector__item" tabindex="0">
                  <span class="pf-c-dual-list-selector__item-main">
                    <span class="pf-c-dual-list-selector__item-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-13"
                          aria-label="Dual list selector item check"
                        />
                      </div>
                    </span>
                    <span class="pf-c-dual-list-selector__item-text">Yellow</span>
                  </span>
                  <span class="pf-c-dual-list-selector__item-count">
                    <span class="pf-c-badge pf-m-read"></span>
                  </span>
                </div>
              </div>
            </li>
            <li
              class="pf-c-dual-list-selector__list-item pf-m-expandable pf-m-expanded pf-m-disabled"
              aria-expanded="true"
            >
              <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
                <div class="pf-c-dual-list-selector__item">
                  <span class="pf-c-dual-list-selector__item-main">
                    <div class="pf-c-dual-list-selector__item-toggle">
                      <span class="pf-c-dual-list-selector__item-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-dual-list-selector__item-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          disabled
                          id="check-14"
                          aria-label="Dual list selector item check"
                        />
                      </div>
                    </span>
                    <span
                      class="pf-c-dual-list-selector__item-text"
                    >Green (disabled)</span>
                  </span>
                  <span class="pf-c-dual-list-selector__item-count">
                    <span class="pf-c-badge pf-m-read"></span>
                  </span>
                </div>
              </div>

              <ul class="pf-c-dual-list-selector__list">
                <li class="pf-c-dual-list-selector__list-item">
                  <div
                    class="pf-c-dual-list-selector__list-item-row pf-m-check"
                  >
                    <div class="pf-c-dual-list-selector__item">
                      <span class="pf-c-dual-list-selector__item-main">
                        <span class="pf-c-dual-list-selector__item-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              disabled
                              id="check-15"
                              aria-label="Dual list selector item check"
                            />
                          </div>
                        </span>
                        <span
                          class="pf-c-dual-list-selector__item-text"
                        >Light green</span>
                      </span>
                      <span class="pf-c-dual-list-selector__item-count">
                        <span class="pf-c-badge pf-m-read"></span>
                      </span>
                    </div>
                  </div>
                </li>
                <li class="pf-c-dual-list-selector__list-item">
                  <div
                    class="pf-c-dual-list-selector__list-item-row pf-m-check"
                  >
                    <div class="pf-c-dual-list-selector__item">
                      <span class="pf-c-dual-list-selector__item-main">
                        <span class="pf-c-dual-list-selector__item-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              disabled
                              id="check-16"
                              aria-label="Dual list selector item check"
                            />
                          </div>
                        </span>
                        <span
                          class="pf-c-dual-list-selector__item-text"
                        >Medium green</span>
                      </span>
                      <span class="pf-c-dual-list-selector__item-count">
                        <span class="pf-c-badge pf-m-read"></span>
                      </span>
                    </div>
                  </div>
                </li>
                <li class="pf-c-dual-list-selector__list-item">
                  <div
                    class="pf-c-dual-list-selector__list-item-row pf-m-check"
                  >
                    <div class="pf-c-dual-list-selector__item">
                      <span class="pf-c-dual-list-selector__item-main">
                        <span class="pf-c-dual-list-selector__item-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              disabled
                              id="check-17"
                              aria-label="Dual list selector item check"
                            />
                          </div>
                        </span>
                        <span
                          class="pf-c-dual-list-selector__item-text"
                        >Dark green</span>
                      </span>
                      <span class="pf-c-dual-list-selector__item-count">
                        <span class="pf-c-badge pf-m-read"></span>
                      </span>
                    </div>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-dual-list-selector__list-item pf-m-expandable"
          aria-expanded="false"
        >
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <div class="pf-c-dual-list-selector__item-toggle">
                  <span class="pf-c-dual-list-selector__item-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-18"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Type something</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </div>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-19"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Type something</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </div>
          </div>
        </li>
        <li
          class="pf-c-dual-list-selector__list-item pf-m-expandable"
          aria-expanded="false"
        >
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <div class="pf-c-dual-list-selector__item-toggle">
                  <span class="pf-c-dual-list-selector__item-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-20"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Type something</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </div>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__controls">
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Add selected"
      >
        <i class="fas fa-fw fa-angle-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Add all">
        <i class="fas fa-fw fa-angle-double-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove all"
      >
        <i class="fas fa-fw fa-angle-double-left"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove selected"
      >
        <i class="fas fa-fw fa-angle-left"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__pane pf-m-chosen">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Chosen options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="basic-chosen-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-basic-chosen-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-basic-chosen-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 0 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li
          class="pf-c-dual-list-selector__list-item pf-m-expandable pf-m-expanded"
          aria-expanded="true"
        >
          <div class="pf-c-dual-list-selector__list-item-row pf-m-check">
            <div class="pf-c-dual-list-selector__item" tabindex="0">
              <span class="pf-c-dual-list-selector__item-main">
                <div class="pf-c-dual-list-selector__item-toggle">
                  <span class="pf-c-dual-list-selector__item-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-dual-list-selector__item-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-21"
                      aria-label="Dual list selector item check"
                    />
                  </div>
                </span>
                <span class="pf-c-dual-list-selector__item-text">Colors</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </div>
          </div>

          <ul class="pf-c-dual-list-selector__list">
            <li class="pf-c-dual-list-selector__list-item">
              <div
                class="pf-c-dual-list-selector__list-item-row pf-m-check pf-m-selected"
              >
                <div class="pf-c-dual-list-selector__item" tabindex="0">
                  <span class="pf-c-dual-list-selector__item-main">
                    <span class="pf-c-dual-list-selector__item-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-22"
                          checked
                          aria-label="Dual list selector item check checked"
                        />
                      </div>
                    </span>
                    <span class="pf-c-dual-list-selector__item-text">Orange</span>
                  </span>
                  <span class="pf-c-dual-list-selector__item-count">
                    <span class="pf-c-badge pf-m-read"></span>
                  </span>
                </div>
              </div>
            </li>
          </ul>
        </li>
      </ul>
    </div>
  </div>
</div>

```

### Draggable

```html
<div
  id="draggable-help"
>Activate the reorder button and use the arrow keys to reorder the list or use your mouse to drag/reorder. Press escape to cancel the reordering.</div>
<div class="pf-c-dual-list-selector">
  <div class="pf-c-dual-list-selector__pane pf-m-available">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Available options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="draggable-available-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-draggable-available-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-draggable-available-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 5 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item1</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item4</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span class="pf-c-dual-list-selector__item-text">Item6</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__controls">
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Add selected"
      >
        <i class="fas fa-fw fa-angle-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Add all">
        <i class="fas fa-fw fa-angle-double-right"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove all"
      >
        <i class="fas fa-fw fa-angle-double-left"></i>
      </button>
    </div>
    <div class="pf-c-dual-list-selector__controls-item">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Remove selected"
      >
        <i class="fas fa-fw fa-angle-left"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-dual-list-selector__pane pf-m-chosen">
    <div class="pf-c-dual-list-selector__header">
      <div class="pf-c-dual-list-selector__title">
        <div class="pf-c-dual-list-selector__title-text">Chosen options</div>
      </div>
    </div>
    <div class="pf-c-dual-list-selector__tools">
      <div class="pf-c-dual-list-selector__tools-filter">
        <input
          class="pf-c-form-control pf-m-search"
          type="text"
          placeholder="Filter options"
          id="draggable-chosen-filter"
          aria-label="Filter options"
        />
      </div>
      <div class="pf-c-dual-list-selector__tools-actions">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Sort">
          <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
        </button>
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="dropdown-kebab-draggable-chosen-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu"
            aria-labelledby="dropdown-kebab-draggable-chosen-button"
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
    <div class="pf-c-dual-list-selector__status">
      <span class="pf-c-dual-list-selector__status-text">0 of 0 items selected</span>
    </div>
    <div class="pf-c-dual-list-selector__menu">
      <ul class="pf-c-dual-list-selector__list">
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <div class="pf-c-dual-list-selector__draggable">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                disabled
                aria-pressed="false"
                aria-label="Reorder"
                id="draggable-list-item-2-draggable-button"
                aria-labelledby="draggable-list-item-2-draggable-button draggable-list-item-2-item-text"
                aria-describedby="draggable-help"
              >
                <i class="fas fa-grip-vertical" aria-hidden="true"></i>
              </button>
            </div>
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span
                  class="pf-c-dual-list-selector__item-text"
                  id="draggable-list-item-2-item-text"
                >Item2 - draggable icon disabled</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row">
            <div class="pf-c-dual-list-selector__draggable">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-pressed="false"
                aria-label="Reorder"
                id="draggable-list-item-3-draggable-button"
                aria-labelledby="draggable-list-item-3-draggable-button draggable-list-item-3-item-text"
                aria-describedby="draggable-help"
              >
                <i class="fas fa-grip-vertical" aria-hidden="true"></i>
              </button>
            </div>
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span
                  class="pf-c-dual-list-selector__item-text"
                  id="draggable-list-item-3-item-text"
                >Item3</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-ghost-row">
            <div class="pf-c-dual-list-selector__draggable">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                disabled
                aria-pressed="false"
                aria-label="Reorder"
                id="draggable-list-item-5-draggable-button"
                aria-labelledby="draggable-list-item-5-draggable-button draggable-list-item-5-item-text"
                aria-describedby="draggable-help"
              >
                <i class="fas fa-grip-vertical" aria-hidden="true"></i>
              </button>
            </div>
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span
                  class="pf-c-dual-list-selector__item-text"
                  id="draggable-list-item-5-item-text"
                >Item5 - ghost row</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
        <li class="pf-c-dual-list-selector__list-item">
          <div class="pf-c-dual-list-selector__list-item-row pf-m-selected">
            <div class="pf-c-dual-list-selector__draggable">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-pressed="false"
                aria-label="Reorder"
                id="draggable-list-item-7-draggable-button"
                aria-labelledby="draggable-list-item-7-draggable-button draggable-list-item-7-item-text"
                aria-describedby="draggable-help"
              >
                <i class="fas fa-grip-vertical" aria-hidden="true"></i>
              </button>
            </div>
            <button class="pf-c-dual-list-selector__item" type="button">
              <span class="pf-c-dual-list-selector__item-main">
                <span
                  class="pf-c-dual-list-selector__item-text"
                  id="draggable-list-item-7-item-text"
                >Item7 - selected</span>
              </span>
              <span class="pf-c-dual-list-selector__item-count">
                <span class="pf-c-badge pf-m-read"></span>
              </span>
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
</div>
<div
  class="pf-screen-reader"
  aria-live="assertive"
>This is the aria-live section that provides real-time feedback to the user.</div>

```

## Documentation

### Accessibility

| Attribute                                                                                      | Applied to                                                        | Outcome                                                                                                                                                                                                   |
| ---------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-pressed="true or false"`                                                                 | `.pf-c-dual-list-selector__draggable .pf-c-button`                | Indicates whether the button is currently pressed or not.                                                                                                                                                 |
| `aria-live`                                                                                    | `[element with live text]`                                        | To give screen reader users live feedback about what's happening during interaction with the dual list selector, both during drag and drop interactions and keyboard interactions. **Highly Recommended** |
| `aria-describedby="[id value of applicable content]"`                                          | `.pf-c-dual-list-selector__draggable .pf-c-button`                | Gives the draggable button an accessible description by referring to the textual content that describes how to use the button to drag elements. **Highly recommended**                                    |
| `aria-labelledby="[id of .pf-c-dual-list-selector__draggable .pf-c-button] [id of item text]"` | `.pf-c-table__dual-list-selector .pf-c-button`                    | Provides an accessible name for the draggable button.                                                                                                                                                     |
| `id="[]"`                                                                                      | `.pf-c-dual-list-selector__draggable .pf-c-button`, `[item text]` | Gives the button and the text element accessible IDs.                                                                                                                                                     |
| `disabled`                                                                                     | `.pf-c-dual-list-selector__item [button, check]`                  | Disables interactive elements in a disabled item. **Required** when an item is disabled.                                                                                                                  |

### Usage

| Class                                        | Applied                                   | Outcome                                                                                                                   |
| -------------------------------------------- | ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-dual-list-selector`                   | `<div>`                                   | Initiates the dual list selector component. **Required**                                                                  |
| `.pf-c-dual-list-selector__pane`             | `<div>`                                   | Initiates a dual list selector pane. **Required**                                                                         |
| `.pf-c-dual-list-selector__header`           | `<div>`                                   | Initiates a dual list selector pane header. **Required**                                                                  |
| `.pf-c-dual-list-selector__title`            | `<div>`                                   | Initiates a dual list selector pane title. **Required**                                                                   |
| `.pf-c-dual-list-selector__title-text`       | `<div>`                                   | Initiates a dual list selector pane title text. **Required**                                                              |
| `.pf-c-dual-list-selector__tools`            | `<div>`                                   | Initiates a dual list selector tools. **Required**                                                                        |
| `.pf-c-dual-list-selector__filter`           | `<div>`                                   | Initiates a dual list selector pane filter. **Required**                                                                  |
| `.pf-c-dual-list-selector__actions`          | `<div>`                                   | Initiates a dual list selector pane actions.                                                                              |
| `.pf-c-dual-list-selector__actions-item`     | `<div>`                                   | Initiates a dual list selector pane actions item.                                                                         |
| `.pf-c-dual-list-selector__status`           | `<div>`                                   | Initiates a dual list selector pane selected status. **Required**                                                         |
| `.pf-c-dual-list-selector__status-text`      | `<span>`                                  | Initiates a dual list selector pane selected status text. **Required**                                                    |
| `.pf-c-dual-list-selector__menu`             | `<div>`                                   | Initiates a dual list selector pane menu container. **Required**                                                          |
| `.pf-c-dual-list-selector__list`             | `<ul>`                                    | Initiates a dual list selector pane menu list. **Required**                                                               |
| `.pf-c-dual-list-selector__list-item`        | `<li>`                                    | Initiates a dual list selector pane menu list item. **Required**                                                          |
| `.pf-c-dual-list-selector__list-item-row`    | `<div>`                                   | Initiates a dual list selector pane menu list item row. **Required**                                                      |
| `.pf-c-dual-list-selector__draggable`        | `<div>`                                   | Initiates a dual list selector pane draggable element.                                                                    |
| `.pf-c-dual-list-selector__item`             | `<button>`, `<div>`                       | Initiates a dual list selector pane menu item. **Required**                                                               |
| `.pf-c-dual-list-selector__item-main`        | `<span>`                                  | Initiates a dual list selector pane menu item main container. **Required**                                                |
| `.pf-c-dual-list-selector__item-check`       | `<span>`                                  | Initiates the dual list selector item check.                                                                              |
| `.pf-c-dual-list-selector__item-count`       | `<span>`                                  | Initiates the dual list selector item count.                                                                              |
| `.pf-c-dual-list-selector__item-toggle-icon` | `<span>`                                  | Initiates the dual list selector item toggle icon.                                                                        |
| `.pf-c-dual-list-selector__item-toggle`      | `<button>`                                | Initiates the dual list selector item toggle.                                                                             |
| `.pf-c-dual-list-selector__item-text`        | `<span>`                                  | Initiates a dual list selector pane menu item text. **Required**                                                          |
| `.pf-c-dual-list-selector__controls`         | `<div>`                                   | Initiates the dual list selector controls. **Required**                                                                   |
| `.pf-c-dual-list-selector__controls-item`    | `<div>`                                   | Initiates the dual list selector controls item. **Required**                                                              |
| `.pf-m-available`                            | `.pf-c-dual-list-selector__pane`          | Defines a pane as the available list.                                                                                     |
| `.pf-m-chosen`                               | `.pf-c-dual-list-selector__pane`          | Defines a pane as the chosen list.                                                                                        |
| `.pf-m-drag-over`                            | `.pf-c-dual-list-selector__list`          | Modifies the dual list selector list to indicate that a draggable item is being dragged over the list.                    |
| `.pf-m-ghost-row`                            | `.pf-c-dual-list-selector__list-item-row` | Modifies the list item main to be a ghost row.                                                                            |
| `.pf-m-selected`                             | `.pf-c-dual-list-selector__list-item-row` | Modifies the menu item for the selected state.                                                                            |
| `.pf-m-check`                                | `.pf-c-dual-list-selector__list-item-row` | Indicates that an item is selectable with a checkbox.                                                                     |
| `.pf-m-expandable`                           | `.pf-c-dual-list-selector__list-item`     | Indicates that an item is expandable.                                                                                     |
| `.pf-m-expanded`                             | `.pf-c-dual-list-selector__list-item`     | Indicates that an item is expanded.                                                                                       |
| `.pf-m-disabled`                             | `.pf-c-dual-list-selector__list-item`     | Indicates that an item is disabled. **Note:** If an item is expandable, only the top level item needs the disabled class. |
