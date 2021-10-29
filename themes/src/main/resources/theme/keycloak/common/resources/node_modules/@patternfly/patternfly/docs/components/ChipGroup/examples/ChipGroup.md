---
id: Chip group
section: components
cssPrefix: pf-c-chip-group
---## Examples

### Simple inline chip group overflow

```html
<div class="pf-c-chip-group">
  <div class="pf-c-chip-group__main">
    <ul class="pf-c-chip-group__list" role="list" aria-label="Chip group list">
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="simple-inline-chip-group-overflowchip_one_select_collapsed"
          >Chip one</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="simple-inline-chip-group-overflowremove_chip_one_select_collapsed simple-inline-chip-group-overflowchip_one_select_collapsed"
            aria-label="Remove"
            id="simple-inline-chip-group-overflowremove_chip_one_select_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="simple-inline-chip-group-overflowchip_two_select_collapsed"
          >Chip two</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="simple-inline-chip-group-overflowremove_chip_two_select_collapsed simple-inline-chip-group-overflowchip_two_select_collapsed"
            aria-label="Remove"
            id="simple-inline-chip-group-overflowremove_chip_two_select_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="simple-inline-chip-group-overflowchip_three_select_collapsed"
          >Chip three</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="simple-inline-chip-group-overflowremove_chip_three_select_collapsed simple-inline-chip-group-overflowchip_three_select_collapsed"
            aria-label="Remove"
            id="simple-inline-chip-group-overflowremove_chip_three_select_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <button class="pf-c-chip pf-m-overflow">
          <span class="pf-c-chip__text">2 more</span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Simple inline chip group expanded

```html
<div class="pf-c-chip-group">
  <div class="pf-c-chip-group__main">
    <ul class="pf-c-chip-group__list" role="list" aria-label="Chip group list">
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="simple-inline-chip-group-expandedchip_one_select"
          >Chip one</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="simple-inline-chip-group-expandedremove_chip_one_select simple-inline-chip-group-expandedchip_one_select"
            aria-label="Remove"
            id="simple-inline-chip-group-expandedremove_chip_one_select"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="simple-inline-chip-group-expandedchip_two_select"
          >Chip two</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="simple-inline-chip-group-expandedremove_chip_two_select simple-inline-chip-group-expandedchip_two_select"
            aria-label="Remove"
            id="simple-inline-chip-group-expandedremove_chip_two_select"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="simple-inline-chip-group-expandedchip_three_select"
          >Chip three</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="simple-inline-chip-group-expandedremove_chip_three_select simple-inline-chip-group-expandedchip_three_select"
            aria-label="Remove"
            id="simple-inline-chip-group-expandedremove_chip_three_select"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="simple-inline-chip-group-expandedchip_four_select"
          >Chip four</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="simple-inline-chip-group-expandedremove_chip_four_select simple-inline-chip-group-expandedchip_four_select"
            aria-label="Remove"
            id="simple-inline-chip-group-expandedremove_chip_four_select"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="simple-inline-chip-group-expandedchip_five_select"
          >Chip five</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="simple-inline-chip-group-expandedremove_chip_five_select simple-inline-chip-group-expandedchip_five_select"
            aria-label="Remove"
            id="simple-inline-chip-group-expandedremove_chip_five_select"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <button class="pf-c-chip pf-m-overflow">
          <span class="pf-c-chip__text">Show less</span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Chip group with categories

```html
<div class="pf-c-chip-group pf-m-category">
  <div class="pf-c-chip-group__main">
    <span
      class="pf-c-chip-group__label"
      aria-hidden="true"
      id="chip-group-with-categories-label"
    >Category one</span>
    <ul
      class="pf-c-chip-group__list"
      role="list"
      aria-labelledby="chip-group-with-categories-label"
    >
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categorieschip_one_toolbar_collapsed"
          >Chip one</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categoriesremove_chip_one_toolbar_collapsed chip-group-with-categorieschip_one_toolbar_collapsed"
            aria-label="Remove"
            id="chip-group-with-categoriesremove_chip_one_toolbar_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categorieschip_two_toolbar_collapsed"
          >Chip two</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categoriesremove_chip_two_toolbar_collapsed chip-group-with-categorieschip_two_toolbar_collapsed"
            aria-label="Remove"
            id="chip-group-with-categoriesremove_chip_two_toolbar_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categorieschip_three_toolbar_collapsed"
          >Chip three</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categoriesremove_chip_three_toolbar_collapsed chip-group-with-categorieschip_three_toolbar_collapsed"
            aria-label="Remove"
            id="chip-group-with-categoriesremove_chip_three_toolbar_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
    </ul>
  </div>
</div>

```

### Chip group with categories overflow

```html
<div class="pf-c-chip-group pf-m-category">
  <div class="pf-c-chip-group__main">
    <span
      class="pf-c-chip-group__label"
      aria-hidden="true"
      id="chip-group-with-categories-overflow-label"
    >Category one</span>
    <ul
      class="pf-c-chip-group__list"
      role="list"
      aria-labelledby="chip-group-with-categories-overflow-label"
    >
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-overflowchip_one_toolbar_collapsed"
          >Chip one</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-overflowremove_chip_one_toolbar_collapsed chip-group-with-categories-overflowchip_one_toolbar_collapsed"
            aria-label="Remove"
            id="chip-group-with-categories-overflowremove_chip_one_toolbar_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-overflowchip_two_toolbar_collapsed"
          >Chip two</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-overflowremove_chip_two_toolbar_collapsed chip-group-with-categories-overflowchip_two_toolbar_collapsed"
            aria-label="Remove"
            id="chip-group-with-categories-overflowremove_chip_two_toolbar_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-overflowchip_three_toolbar_collapsed"
          >Chip three</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-overflowremove_chip_three_toolbar_collapsed chip-group-with-categories-overflowchip_three_toolbar_collapsed"
            aria-label="Remove"
            id="chip-group-with-categories-overflowremove_chip_three_toolbar_collapsed"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <button class="pf-c-chip pf-m-overflow">
          <span class="pf-c-chip__text">2 more</span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Chip group with categories overflow expanded

```html
<div class="pf-c-chip-group pf-m-category">
  <div class="pf-c-chip-group__main">
    <span
      class="pf-c-chip-group__label"
      aria-hidden="true"
      id="chip-group-with-categories-overflow-expanded-label"
    >Category one</span>
    <ul
      class="pf-c-chip-group__list"
      role="list"
      aria-labelledby="chip-group-with-categories-overflow-expanded-label"
    >
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-overflow-expandedchip_one_toolbar"
          >Chip one</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-overflow-expandedremove_chip_one_toolbar chip-group-with-categories-overflow-expandedchip_one_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-overflow-expandedremove_chip_one_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-overflow-expandedchip_two_toolbar"
          >Chip two</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-overflow-expandedremove_chip_two_toolbar chip-group-with-categories-overflow-expandedchip_two_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-overflow-expandedremove_chip_two_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-overflow-expandedchip_three_toolbar"
          >Chip three</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-overflow-expandedremove_chip_three_toolbar chip-group-with-categories-overflow-expandedchip_three_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-overflow-expandedremove_chip_three_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-overflow-expandedchip_four_toolbar"
          >Chip four</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-overflow-expandedremove_chip_four_toolbar chip-group-with-categories-overflow-expandedchip_four_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-overflow-expandedremove_chip_four_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-overflow-expandedchip_five_select"
          >Chip five</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-overflow-expandedremove_chip_five_select chip-group-with-categories-overflow-expandedchip_five_select"
            aria-label="Remove"
            id="chip-group-with-categories-overflow-expandedremove_chip_five_select"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <button class="pf-c-chip pf-m-overflow">
          <span class="pf-c-chip__text">Show less</span>
        </button>
      </li>
    </ul>
  </div>
</div>

```

### Chip group with categories removable

```html
<div class="pf-c-chip-group pf-m-category">
  <div class="pf-c-chip-group__main">
    <span
      class="pf-c-chip-group__label"
      aria-hidden="true"
      id="chip-group-with-categories-removable-label"
    >Category one</span>
    <ul
      class="pf-c-chip-group__list"
      role="list"
      aria-labelledby="chip-group-with-categories-removable-label"
    >
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-removablechip_one_toolbar"
          >Chip one</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-removableremove_chip_one_toolbar chip-group-with-categories-removablechip_one_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-removableremove_chip_one_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-removablechip_two_toolbar"
          >Chip two</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-removableremove_chip_two_toolbar chip-group-with-categories-removablechip_two_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-removableremove_chip_two_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-removablechip_three_toolbar"
          >Chip three</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-removableremove_chip_three_toolbar chip-group-with-categories-removablechip_three_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-removableremove_chip_three_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-removablechip_four_toolbar"
          >Chip four</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-removableremove_chip_four_toolbar chip-group-with-categories-removablechip_four_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-removableremove_chip_four_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-removablechip_five_toolbar"
          >Chip five</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-removableremove_chip_five_toolbar chip-group-with-categories-removablechip_five_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-removableremove_chip_five_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
      <li class="pf-c-chip-group__list-item">
        <div class="pf-c-chip">
          <span
            class="pf-c-chip__text"
            id="chip-group-with-categories-removablechip_six_toolbar"
          >Chip six</span>
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="chip-group-with-categories-removableremove_chip_six_toolbar chip-group-with-categories-removablechip_six_toolbar"
            aria-label="Remove"
            id="chip-group-with-categories-removableremove_chip_six_toolbar"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
        </div>
      </li>
    </ul>
  </div>
  <div class="pf-c-chip-group__close">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-labelledby="chip-group-with-categories-removable-button chip-group-with-categories-removable-label"
      aria-label="Close chip group"
      id="chip-group-with-categories-removable-button"
    >
      <i class="fas fa-times-circle" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Legacy chip group examples without main element

```html
<div class="pf-c-chip-group">
  <ul class="pf-c-chip-group__list" role="list" aria-label="Chip group list">
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simplechip_one_select_collapsed"
        >Chip one</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simpleremove_chip_one_select_collapsed legacy-simplechip_one_select_collapsed"
          aria-label="Remove"
          id="legacy-simpleremove_chip_one_select_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simplechip_two_select_collapsed"
        >Chip two</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simpleremove_chip_two_select_collapsed legacy-simplechip_two_select_collapsed"
          aria-label="Remove"
          id="legacy-simpleremove_chip_two_select_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simplechip_three_select_collapsed"
        >Chip three</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simpleremove_chip_three_select_collapsed legacy-simplechip_three_select_collapsed"
          aria-label="Remove"
          id="legacy-simpleremove_chip_three_select_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
  </ul>
</div>

<br />
<br />

<div class="pf-c-chip-group">
  <ul class="pf-c-chip-group__list" role="list" aria-label="Chip group list">
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simple-removablechip_one_toolbar"
        >Chip one</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simple-removableremove_chip_one_toolbar legacy-simple-removablechip_one_toolbar"
          aria-label="Remove"
          id="legacy-simple-removableremove_chip_one_toolbar"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simple-removablechip_two_toolbar"
        >Chip two</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simple-removableremove_chip_two_toolbar legacy-simple-removablechip_two_toolbar"
          aria-label="Remove"
          id="legacy-simple-removableremove_chip_two_toolbar"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simple-removablechip_three_toolbar"
        >Chip three</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simple-removableremove_chip_three_toolbar legacy-simple-removablechip_three_toolbar"
          aria-label="Remove"
          id="legacy-simple-removableremove_chip_three_toolbar"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simple-removablechip_four_toolbar"
        >Chip four</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simple-removableremove_chip_four_toolbar legacy-simple-removablechip_four_toolbar"
          aria-label="Remove"
          id="legacy-simple-removableremove_chip_four_toolbar"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simple-removablechip_five_toolbar"
        >Chip five</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simple-removableremove_chip_five_toolbar legacy-simple-removablechip_five_toolbar"
          aria-label="Remove"
          id="legacy-simple-removableremove_chip_five_toolbar"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-simple-removablechip_six_toolbar"
        >Chip six</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-simple-removableremove_chip_six_toolbar legacy-simple-removablechip_six_toolbar"
          aria-label="Remove"
          id="legacy-simple-removableremove_chip_six_toolbar"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
  </ul>
  <div class="pf-c-chip-group__close">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-labelledby="legacy-simple-removable-button legacy-simple-removable-label"
      aria-label="Close chip group"
      id="legacy-simple-removable-button"
    >
      <i class="fas fa-times-circle" aria-hidden="true"></i>
    </button>
  </div>
</div>

<br />
<br />

<div class="pf-c-chip-group pf-m-category">
  <span
    class="pf-c-chip-group__label"
    aria-hidden="true"
    id="legacy-category-label"
  >Category one</span>
  <ul
    class="pf-c-chip-group__list"
    role="list"
    aria-labelledby="legacy-category-label"
  >
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-categorychip_one_toolbar_collapsed"
        >Chip one</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-categoryremove_chip_one_toolbar_collapsed legacy-categorychip_one_toolbar_collapsed"
          aria-label="Remove"
          id="legacy-categoryremove_chip_one_toolbar_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-categorychip_two_toolbar_collapsed"
        >Chip two</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-categoryremove_chip_two_toolbar_collapsed legacy-categorychip_two_toolbar_collapsed"
          aria-label="Remove"
          id="legacy-categoryremove_chip_two_toolbar_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-categorychip_three_toolbar_collapsed"
        >Chip three</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-categoryremove_chip_three_toolbar_collapsed legacy-categorychip_three_toolbar_collapsed"
          aria-label="Remove"
          id="legacy-categoryremove_chip_three_toolbar_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
  </ul>
</div>

<br />
<br />

<div class="pf-c-chip-group pf-m-category">
  <span
    class="pf-c-chip-group__label"
    aria-hidden="true"
    id="legacy-category-removable-label"
  >Category one</span>
  <ul
    class="pf-c-chip-group__list"
    role="list"
    aria-labelledby="legacy-category-removable-label"
  >
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-category-removablechip_one_toolbar_collapsed"
        >Chip one</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-category-removableremove_chip_one_toolbar_collapsed legacy-category-removablechip_one_toolbar_collapsed"
          aria-label="Remove"
          id="legacy-category-removableremove_chip_one_toolbar_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-category-removablechip_two_toolbar_collapsed"
        >Chip two</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-category-removableremove_chip_two_toolbar_collapsed legacy-category-removablechip_two_toolbar_collapsed"
          aria-label="Remove"
          id="legacy-category-removableremove_chip_two_toolbar_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
    <li class="pf-c-chip-group__list-item">
      <div class="pf-c-chip">
        <span
          class="pf-c-chip__text"
          id="legacy-category-removablechip_three_toolbar_collapsed"
        >Chip three</span>
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-labelledby="legacy-category-removableremove_chip_three_toolbar_collapsed legacy-category-removablechip_three_toolbar_collapsed"
          aria-label="Remove"
          id="legacy-category-removableremove_chip_three_toolbar_collapsed"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </li>
  </ul>
  <div class="pf-c-chip-group__close">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-labelledby="legacy-category-removable-button legacy-category-removable-label"
      aria-label="Close chip group"
      id="legacy-category-removable-button"
    >
      <i class="fas fa-times-circle" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

### Overview

A chip group is constrained to the width of its container and will wrap when it exceeds that width. An overflow value can be set and when the number of chips exceeds that value, additional chips will be hidden by default. The default overflow value will be set to 3 chips but this can be adjusted per application needs. The toggle button after the last chip allows the group to be expanded (or collapsed).

If you want to create sub-groupings of chips to represent multiple values applied against the same category, chips can be grouped by category. This can be useful in filtering use cases, for example, where you want all items that match more than one value of the same attribute, e.g., ‘status = down OR needs maintenance’.

The chip group requires the [chip component](/components/chip).

### Accessibility

**All single chip accessibility and usage requirements apply.**

| Attributes for closable chip group button                                                                | Applied to                         | Outcome                                                                                                                                                                                                 |
| -------------------------------------------------------------------------------------------------------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="list"`                                                                                            | `.pf-c-chip-group__list`           | Indicates that the chip group list is a list element. This role is redundant since `.pf-c-chip-group__list` is a `<ul>` but is required for screen readers to announce the list propertly. **Required** |
| `aria-label="[button label text]"`                                                                       | `.pf-c-chip-group__close > button` | Provides an accessible name for a chip group close when an icon is used instead of text. Required when an icon is used with no supporting text. **Required**                                            |
| `aria-labelledby="[id value of .pf-c-chip-group__close > button] [id value of .pf-c-chip-group__label]"` | `.pf-c-chip-group__close > button` | Provides an accessible name for the button. **Required**                                                                                                                                                |

### Usage

| Class                         | Applied to                         | Outcome                                                                             |
| ----------------------------- | ---------------------------------- | ----------------------------------------------------------------------------------- |
| `.pf-c-chip-group`            | `<div>`                            | Initiates the chip group component. **Required.**                                   |
| `.pf-c-chip-group__list`      | `<ul>`                             | Initiates the container for a list of chips. **Required.**                          |
| `.pf-c-chip-group__list-item` | `<li>`                             | Initiates the list item inside of the chip group. **Required.**                     |
| `.pf-c-chip-group__label`     | `<span>`                           | Initiates the label to be used in the chip group.                                   |
| `.pf-c-chip-group__close`     | `<div>`                            | Initiates the container used for the button to remove the chip group.               |
| `.pf-c-chip-group__main`      | `<div>`                            | Initiates the container for the label and list elements so that they wrap together. |
| `.pf-c-button`                | `.pf-c-chip-group__close <button>` | Initiates the button used to remove the chip group.                                 |
| `.pf-m-category`              | `.pf-c-chip-group`                 | Modifies the chip group to support category styling.                                |
