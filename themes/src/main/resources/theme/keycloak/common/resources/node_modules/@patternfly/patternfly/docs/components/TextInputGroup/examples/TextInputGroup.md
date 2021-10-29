---
id: 'Text input group'
beta: true
section: components
cssPrefix: pf-c-text-input-group
---## Examples

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

### Utilities and icon

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
