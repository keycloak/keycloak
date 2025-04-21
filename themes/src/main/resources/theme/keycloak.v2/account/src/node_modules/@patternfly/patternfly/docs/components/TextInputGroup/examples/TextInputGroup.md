---
id: 'Text input group'
beta: true
section: components
cssPrefix: pf-c-text-input-group
---import './TextInputGroup.css'

## Examples

### Basic

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main">
    <span class="pf-c-text-input-group__text">
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value
        aria-label="Type to filter"
      />
    </span>
  </div>
</div>

```

### Disabled

```html
<div class="pf-c-text-input-group pf-m-disabled">
  <div class="pf-c-text-input-group__main">
    <span class="pf-c-text-input-group__text">
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value="Disabled"
        disabled
        aria-label="Type to filter"
      />
    </span>
  </div>
</div>

```

### Utilities and icon with placeholder text

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main pf-m-icon">
    <span class="pf-c-text-input-group__text">
      <span class="pf-c-text-input-group__icon">
        <i class="fas fa-fw fa-search"></i>
      </span>
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value
        aria-label="Type to filter"
        placeholder="placeholder"
      />
    </span>
  </div>
  <div class="pf-c-text-input-group__utilities">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Clear input"
    >
      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Filters

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main">
    <div class="pf-c-chip-group">
      <div class="pf-c-chip-group__main">
        <ul
          class="pf-c-chip-group__list"
          role="list"
          aria-label="Chip group list"
        >
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_one_select_collapsed"
              >Chip one</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_one_select_collapsed -chip-groupchip_one_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_one_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_two_select_collapsed"
              >Chip two</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_two_select_collapsed -chip-groupchip_two_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_two_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_three_select_collapsed"
              >Chip three</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_three_select_collapsed -chip-groupchip_three_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_three_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_four_select_collapsed"
              >Chip four</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_four_select_collapsed -chip-groupchip_four_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_four_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_five_select_collapsed"
              >Chip five</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_five_select_collapsed -chip-groupchip_five_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_five_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_six_select_collapsed"
              >Chip six</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_six_select_collapsed -chip-groupchip_six_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_six_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <button class="pf-c-chip pf-m-overflow">
              <span class="pf-c-chip__text">8 more</span>
            </button>
          </li>
        </ul>
      </div>
    </div>
    <span class="pf-c-text-input-group__text">
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value
        aria-label="Type to filter"
      />
    </span>
  </div>
  <div class="pf-c-text-input-group__utilities">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Clear input"
    >
      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Filters expanded

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main">
    <div class="pf-c-chip-group">
      <div class="pf-c-chip-group__main">
        <ul
          class="pf-c-chip-group__list"
          role="list"
          aria-label="Chip group list"
        >
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_one_select_collapsed"
              >Chip one</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_one_select_collapsed -chip-groupchip_one_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_one_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_two_select_collapsed"
              >Chip two</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_two_select_collapsed -chip-groupchip_two_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_two_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_three_select_collapsed"
              >Chip three</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_three_select_collapsed -chip-groupchip_three_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_three_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_four_select_collapsed"
              >Chip four</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_four_select_collapsed -chip-groupchip_four_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_four_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_five_select_collapsed"
              >Chip five</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_five_select_collapsed -chip-groupchip_five_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_five_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_six_select_collapsed"
              >Chip six</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_six_select_collapsed -chip-groupchip_six_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_six_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_seven_select_collapsed"
              >Chip seven</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_seven_select_collapsed -chip-groupchip_seven_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_seven_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_eight_select_collapsed"
              >Chip eight</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_eight_select_collapsed -chip-groupchip_eight_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_eight_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_nine_select_collapsed"
              >Chip nine</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_nine_select_collapsed -chip-groupchip_nine_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_nine_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_ten_select_collapsed"
              >Chip ten</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_ten_select_collapsed -chip-groupchip_ten_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_ten_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_eleven_select_collapsed"
              >Chip eleven</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_eleven_select_collapsed -chip-groupchip_eleven_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_eleven_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_twelve_select_collapsed"
              >Chip twelve</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_twelve_select_collapsed -chip-groupchip_twelve_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_twelve_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_thirteen_select_collapsed"
              >Chip thirteen</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_thirteen_select_collapsed -chip-groupchip_thirteen_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_thirteen_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
          <li class="pf-c-chip-group__list-item">
            <div class="pf-c-chip">
              <span
                class="pf-c-chip__text"
                id="-chip-groupchip_fourteen_select_collapsed"
              >Chip fourteen</span>
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-labelledby="-chip-groupremove_chip_fourteen_select_collapsed -chip-groupchip_fourteen_select_collapsed"
                aria-label="Remove"
                id="-chip-groupremove_chip_fourteen_select_collapsed"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </li>
        </ul>
      </div>
    </div>
    <span class="pf-c-text-input-group__text">
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value
        aria-label="Type to filter"
      />
    </span>
  </div>
  <div class="pf-c-text-input-group__utilities">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Clear input"
    >
      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Autocomplete last option hint

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main">
    <span class="pf-c-text-input-group__text">
      <input
        class="pf-c-text-input-group__text-input pf-m-hint"
        type="text"
        value="appleseed"
        disabled
        aria-hidden="true"
      />
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value="apples"
        aria-label="Type to filter"
      />
    </span>
  </div>
</div>

```

### Search input group

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main pf-m-icon">
    <span class="pf-c-text-input-group__text">
      <span class="pf-c-text-input-group__icon">
        <i class="fas fa-fw fa-search"></i>
      </span>
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value
        aria-label="Type to filter"
        placeholder="Find by name"
      />
    </span>
  </div>
</div>

```

### Search input group, no match

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main pf-m-icon">
    <span class="pf-c-text-input-group__text">
      <span class="pf-c-text-input-group__icon">
        <i class="fas fa-fw fa-search"></i>
      </span>
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value="Joh"
        aria-label="Type to filter"
      />
    </span>
  </div>
  <div class="pf-c-text-input-group__utilities">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Clear input"
    >
      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Search input group, match with result count

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main pf-m-icon">
    <span class="pf-c-text-input-group__text">
      <span class="pf-c-text-input-group__icon">
        <i class="fas fa-fw fa-search"></i>
      </span>
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value="John Doe"
        aria-label="Type to filter"
      />
    </span>
  </div>
  <div class="pf-c-text-input-group__utilities">
    <span class="pf-c-badge pf-m-read">3</span>
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Clear input"
    >
      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Search input group, match with navigable options

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main pf-m-icon">
    <span class="pf-c-text-input-group__text">
      <span class="pf-c-text-input-group__icon">
        <i class="fas fa-fw fa-search"></i>
      </span>
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value="John Doe"
        aria-label="Type to filter"
      />
    </span>
  </div>
  <div class="pf-c-text-input-group__utilities">
    <span class="pf-c-badge pf-m-read">1 / 3</span>
    <div class="pf-c-text-input-group__group">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        disabled
        aria-label="Next"
      >
        <i class="fas fa-angle-up fa-fw" aria-hidden="true"></i>
      </button>
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Next">
        <i class="fas fa-angle-down fa-fw" aria-hidden="true"></i>
      </button>
    </div>
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Clear input"
    >
      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Search input group, with submit button

```html
<div class="pf-c-input-group">
  <div class="pf-c-text-input-group">
    <div class="pf-c-text-input-group__main pf-m-icon">
      <span class="pf-c-text-input-group__text">
        <span class="pf-c-text-input-group__icon">
          <i class="fas fa-fw fa-search"></i>
        </span>
        <input
          class="pf-c-text-input-group__text-input"
          type="text"
          value
          placeholder="Find by name"
          aria-label="Type to filter"
        />
      </span>
    </div>
  </div>
  <button class="pf-c-button pf-m-control" type="submit" aria-label="Search">
    <i class="fas fa-arrow-right" aria-hidden="true"></i>
  </button>
</div>

```

### Search input group, advanced search

```html
<div class="pf-c-input-group">
  <div class="pf-c-text-input-group">
    <div class="pf-c-text-input-group__main pf-m-icon">
      <span class="pf-c-text-input-group__text">
        <span class="pf-c-text-input-group__icon">
          <i class="fas fa-fw fa-search"></i>
        </span>
        <input
          class="pf-c-text-input-group__text-input"
          type="text"
          value="username:root firstname:ned"
          aria-label="Type to filter"
        />
      </span>
    </div>
    <div class="pf-c-text-input-group__utilities">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Clear input"
      >
        <i class="fas fa-times fa-fw" aria-hidden="true"></i>
      </button>
    </div>
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

```

### Search input group, advanced search expanded

```html
<div class="pf-c-input-group">
  <div class="pf-c-text-input-group">
    <div class="pf-c-text-input-group__main pf-m-icon">
      <span class="pf-c-text-input-group__text">
        <span class="pf-c-text-input-group__icon">
          <i class="fas fa-fw fa-search"></i>
        </span>
        <input
          class="pf-c-text-input-group__text-input"
          type="text"
          value="username:root firstname:ned"
          aria-label="Type to filter"
        />
      </span>
    </div>
    <div class="pf-c-text-input-group__utilities">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Clear input"
      >
        <i class="fas fa-times fa-fw" aria-hidden="true"></i>
      </button>
    </div>
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

<div class="pf-c-panel pf-m-raised">
  <div class="pf-c-panel__main">
    <div class="pf-c-panel__main-body">
      <form novalidate class="pf-c-form">
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="text-input-group-advanced-search-input-expanded-legacy-form-example-username"
            >
              <span class="pf-c-form__label-text">Username</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              value="root"
              id="text-input-group-advanced-search-input-expanded-legacy-form-example-username"
              name="text-input-group-advanced-search-input-expanded-legacy-form-example-username"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="text-input-group-advanced-search-input-expanded-legacy-form-example-firstname"
            >
              <span class="pf-c-form__label-text">First name</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              value="ned"
              id="text-input-group-advanced-search-input-expanded-legacy-form-example-firstname"
              name="text-input-group-advanced-search-input-expanded-legacy-form-example-firstname"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="text-input-group-advanced-search-input-expanded-legacy-form-example-group"
            >
              <span class="pf-c-form__label-text">Group</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              id="text-input-group-advanced-search-input-expanded-legacy-form-example-group"
              name="text-input-group-advanced-search-input-expanded-legacy-form-example-group"
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="text-input-group-advanced-search-input-expanded-legacy-form-example-email"
            >
              <span class="pf-c-form__label-text">Email</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              id="text-input-group-advanced-search-input-expanded-legacy-form-example-email"
              name="text-input-group-advanced-search-input-expanded-legacy-form-example-email"
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

### Search input group, autocomplete

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main pf-m-icon">
    <span class="pf-c-text-input-group__text">
      <span class="pf-c-text-input-group__icon">
        <i class="fas fa-fw fa-search"></i>
      </span>
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value="app"
        aria-label="Type to filter"
      />
    </span>
  </div>
  <div class="pf-c-text-input-group__utilities">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Clear input"
    >
      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
    </button>
  </div>
</div>

<div class="pf-c-menu">
  <div class="pf-c-menu__content">
    <ul class="pf-c-menu__list" role="menu">
      <li class="pf-c-menu__list-item" role="none">
        <button class="pf-c-menu__item" type="button" role="menuitem">
          <span class="pf-c-menu__item-main">
            <span class="pf-c-menu__item-text">apple</span>
          </span>
        </button>
      </li>
      <li class="pf-c-menu__list-item" role="none">
        <button class="pf-c-menu__item" type="button" role="menuitem">
          <span class="pf-c-menu__item-main">
            <span class="pf-c-menu__item-text">appleby</span>
          </span>
        </button>
      </li>
      <li class="pf-c-menu__list-item" role="none">
        <button class="pf-c-menu__item" type="button" role="menuitem">
          <span class="pf-c-menu__item-main">
            <span class="pf-c-menu__item-text">appleseed</span>
          </span>
        </button>
      </li>
      <li class="pf-c-menu__list-item" role="none">
        <button class="pf-c-menu__item" type="button" role="menuitem">
          <span class="pf-c-menu__item-main">
            <span class="pf-c-menu__item-text">appleton</span>
          </span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Search input group, autocomplete last option hint

```html
<div class="pf-c-text-input-group">
  <div class="pf-c-text-input-group__main pf-m-icon">
    <span class="pf-c-text-input-group__text">
      <span class="pf-c-text-input-group__icon">
        <i class="fas fa-fw fa-search"></i>
      </span>
      <input
        class="pf-c-text-input-group__text-input pf-m-hint"
        type="text"
        value="appleseed"
        disabled
        aria-hidden="true"
      />
      <input
        class="pf-c-text-input-group__text-input"
        type="text"
        value="apples"
        aria-label="Type to filter"
      />
    </span>
  </div>
  <div class="pf-c-text-input-group__utilities">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Clear input"
    >
      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
    </button>
  </div>
</div>

<div class="pf-c-menu">
  <div class="pf-c-menu__content">
    <ul class="pf-c-menu__list" role="menu">
      <li class="pf-c-menu__list-item" role="none">
        <button class="pf-c-menu__item" type="button" role="menuitem">
          <span class="pf-c-menu__item-main">
            <span class="pf-c-menu__item-text">appleseed</span>
          </span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Search input group, advanced search expanded with autocomplete

```html
<div class="ws-example-wrapper">
  <div class="pf-c-input-group">
    <div class="pf-c-text-input-group">
      <div class="pf-c-text-input-group__main pf-m-icon">
        <span class="pf-c-text-input-group__text">
          <span class="pf-c-text-input-group__icon">
            <i class="fas fa-fw fa-search"></i>
          </span>
          <input
            class="pf-c-text-input-group__text-input"
            type="text"
            value="username:root firstname:n"
            aria-label="Type to filter"
          />
        </span>
      </div>
      <div class="pf-c-text-input-group__utilities">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Clear input"
        >
          <i class="fas fa-times fa-fw" aria-hidden="true"></i>
        </button>
      </div>
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

  <div class="pf-c-panel pf-m-raised">
    <div class="pf-c-panel__main">
      <div class="pf-c-panel__main-body">
        <form novalidate class="pf-c-form">
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="text-input-group-advanced-search-input-form-with-autocomplete-example-username"
              >
                <span class="pf-c-form__label-text">Username</span>
              </label>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                value="root"
                id="text-input-group-advanced-search-input-form-with-autocomplete-example-username"
                name="text-input-group-advanced-search-input-form-with-autocomplete-example-username"
              />
            </div>
          </div>
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="text-input-group-advanced-search-input-form-with-autocomplete-example-firstname"
              >
                <span class="pf-c-form__label-text">First name</span>
              </label>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                value="ned"
                id="text-input-group-advanced-search-input-form-with-autocomplete-example-firstname"
                name="text-input-group-advanced-search-input-form-with-autocomplete-example-firstname"
              />
            </div>
          </div>
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="text-input-group-advanced-search-input-form-with-autocomplete-example-group"
              >
                <span class="pf-c-form__label-text">Group</span>
              </label>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                id="text-input-group-advanced-search-input-form-with-autocomplete-example-group"
                name="text-input-group-advanced-search-input-form-with-autocomplete-example-group"
              />
            </div>
          </div>
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="text-input-group-advanced-search-input-form-with-autocomplete-example-email"
              >
                <span class="pf-c-form__label-text">Email</span>
              </label>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                id="text-input-group-advanced-search-input-form-with-autocomplete-example-email"
                name="text-input-group-advanced-search-input-form-with-autocomplete-example-email"
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

  <div class="pf-c-menu">
    <div class="pf-c-menu__content">
      <ul class="pf-c-menu__list" role="menu">
        <li class="pf-c-menu__list-item" role="none">
          <button class="pf-c-menu__item" type="button" role="menuitem">
            <span class="pf-c-menu__item-main">
              <span class="pf-c-menu__item-text">nancy</span>
            </span>
          </button>
        </li>
        <li class="pf-c-menu__list-item" role="none">
          <button class="pf-c-menu__item" type="button" role="menuitem">
            <span class="pf-c-menu__item-main">
              <span class="pf-c-menu__item-text">ned</span>
            </span>
          </button>
        </li>
        <li class="pf-c-menu__list-item" role="none">
          <button class="pf-c-menu__item" type="button" role="menuitem">
            <span class="pf-c-menu__item-main">
              <span class="pf-c-menu__item-text">neil</span>
            </span>
          </button>
        </li>
        <li class="pf-c-menu__list-item" role="none">
          <button class="pf-c-menu__item" type="button" role="menuitem">
            <span class="pf-c-menu__item-main">
              <span class="pf-c-menu__item-text">nicole</span>
            </span>
          </button>
        </li>
      </ul>
    </div>
  </div>
</div>

```
