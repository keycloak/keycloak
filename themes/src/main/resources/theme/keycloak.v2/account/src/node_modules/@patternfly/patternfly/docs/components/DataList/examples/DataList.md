---
id: Data list
section: components
cssPrefix: pf-c-data-list
---## Examples

### Basic

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Basic data list example"
  id="data-list-basic"
>
  <li class="pf-c-data-list__item" aria-labelledby="data-list-basic-item-1">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-basic-item-1">Primary content</span>
        </div>
        <div class="pf-c-data-list__cell">Secondary content</div>
      </div>
    </div>
  </li>

  <li class="pf-c-data-list__item" aria-labelledby="data-list-basic-item-2">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-no-fill">
          <span id="data-list-basic-item-2">Secondary content (pf-m-no-fill)</span>
        </div>
        <div
          class="pf-c-data-list__cell pf-m-no-fill pf-m-align-right"
        >Secondary content (pf-m-align-right pf-m-no-fill)</div>
      </div>
    </div>
  </li>
</ul>

```

### Accessibility

| Attribute         | Applied to                                         | Outcome                                                             |
| ----------------- | -------------------------------------------------- | ------------------------------------------------------------------- |
| `role="list"`     | `.pf-c-data-list`                                  | Indicates that the data list is a list. **Required**                |
| `aria-label`      | `.pf-c-data-list`                                  | Provides an accessible name for the data list. **Required**         |
| `aria-labelledby` | `.pf-c-data-list__item`                            | Provides an accessible description for data list item. **Required** |
| `id`              | `.pf-c-data-list__cell`, `.pf-c-data-list__cell *` | Provides a reference for data list item description. **Required**   |

### Usage

| Class                           | Applied to              | Outcome                                                                                            |
| ------------------------------- | ----------------------- | -------------------------------------------------------------------------------------------------- |
| `.pf-c-data-list`               | `<ul>`                  | Initiates a data list. **Required**                                                                |
| `.pf-c-data-list__item`         | `<li>`                  | Initiates a data list item. **Required**                                                           |
| `.pf-c-data-list__item-row`     | `<div>`                 | Initiates a data list item row. **Required**                                                       |
| `.pf-c-data-list__item-content` | `<div>`                 | Initiates a container for data list content. **Required**                                          |
| `.pf-c-data-list__cell`         | `*`                     | Initiates a data list content cell. **Required**                                                   |
| `.pf-c-data-list__cell-text`    | `<span>`                | Initiates a data list content cell text element.                                                   |
| `.pf-m-align-left`              | `.pf-c-data-list__cell` | Modifies a data list cell to not grow and align-left when its the first data-list\_\_cell element. |
| `.pf-m-no-fill`                 | `.pf-c-data-list__cell` | Modifies a data list cell to not fill the available horizontal space.                              |
| `.pf-m-align-right`             | `.pf-c-data-list__cell` | Modifies a data list cell to align-right.                                                          |

### With headings

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="With headings data list example"
  id="data-list-with-headings"
>
  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-with-headings-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <h2 id="data-list-with-headings-item-1">Primary content</h2>
        </div>
        <div class="pf-c-data-list__cell">Secondary content</div>
      </div>
    </div>
  </li>

  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-with-headings-item-2"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-no-fill">
          <h2
            id="data-list-with-headings-item-2"
          >Secondary content (pf-m-no-fill)</h2>
        </div>
        <div
          class="pf-c-data-list__cell pf-m-no-fill pf-m-align-right"
        >Secondary content (pf-m-align-right pf-m-no-fill)</div>
      </div>
    </div>
  </li>
</ul>

```

### Usage

When a list item includes more than one block of content, it can be difficult for some screen reader users to discern where one list item ends and the next one begins. A simple way to provide better separation of list items is to define the primary content section as a heading. Headings are useful for indicating a new section of contents, but also provide an easy way for screen reader users to navigate to specific sections on the page. The heading level should be based on the context in which the DataList component is being used. For example, if the heading for the section that contains the DataList is a level 3, then `h4` elements should be used in the DataList list items.

### Checkboxes, actions, and additional cells

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Checkbox and action data list example"
  id="data-list-checkboxes-actions-addl-cells"
>
  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-checkboxes-actions-addl-cells-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-checkboxes-actions-addl-cells-item-1-checkbox"
            aria-labelledby="data-list-checkboxes-actions-addl-cells-item-1"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-checkboxes-actions-addl-cells-item-1"
          >Primary content</span> Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div
          class="pf-c-data-list__cell"
        >Secondary content. Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.</div>
        <div class="pf-c-data-list__cell">
          <span>Tertiary Content</span> Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div class="pf-c-data-list__cell">
          <span>More Content</span> Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div class="pf-c-data-list__cell">
          <span>More Content</span> Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-checkboxes-actions-addl-cells-item-1-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-checkboxes-actions-addl-cells-item-1-dropdown-kebab-button"
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
    </div>
  </li>

  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-checkboxes-actions-addl-cells-item-2"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-checkboxes-actions-addl-cells-item-2-checkbox"
            aria-labelledby="data-list-checkboxes-actions-addl-cells-item-2"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-checkboxes-actions-addl-cells-item-2"
          >Primary content - lorem ipsum</span> dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div
          class="pf-c-data-list__cell"
        >Secondary content. Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.</div>
      </div>
      <div class="pf-c-data-list__item-action pf-m-hidden-on-lg">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-checkboxes-actions-addl-cells-item-2-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-checkboxes-actions-addl-cells-item-2-dropdown-kebab-button"
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
      <div class="pf-c-data-list__item-action pf-m-hidden pf-m-visible-on-lg">
        <button class="pf-c-button pf-m-primary" type="button">Primary</button>
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
      </div>
    </div>
  </li>

  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-checkboxes-actions-addl-cells-item-3"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-checkboxes-actions-addl-cells-item-3-checkbox"
            aria-labelledby="data-list-checkboxes-actions-addl-cells-item-3"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-checkboxes-actions-addl-cells-item-3"
          >Primary content - lorem ipsum</span> dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div
          class="pf-c-data-list__cell"
        >Secondary content. Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.</div>
      </div>
      <div class="pf-c-data-list__item-action pf-m-hidden-on-lg">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-checkboxes-actions-addl-cells-item-3-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-checkboxes-actions-addl-cells-item-3-dropdown-kebab-button"
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
      <div class="pf-c-data-list__item-action pf-m-hidden pf-m-visible-on-xl">
        <button class="pf-c-button pf-m-primary" type="button">Primary</button>
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
      </div>
    </div>
  </li>
</ul>

```

### Accessibility

| Attribute                                                 | Applied to                                                                                                               | Outcome                                                                                              |
| --------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- |
| `aria-label="[descriptive text]"`                         | `.pf-c-data-list__action` > `.pf-c-button`                                                                               | Provides an accessible label buttons. **Required**                                                   |
| `aria-labelledby="{title_cell_id}"`                       | `.pf-c-data-list__check` > `.pf-c-check__input`                                                                          | Creates an accessible label for the checkbox based on the title cell. **Required**                   |
| `aria-labelledby="{title_cell_id} {data_list_action_id}"` | `.pf-c-data-list__action` > `.pf-c-button`                                                                               | Creates an accessible label for the action button using the title cell and button label **Required** |
| `id`                                                      | `.pf-c-data-list__cell > *`, `.pf-c-data-list__check` > `.pf-c-check__input`, `.pf-c-data-list__action` > `.pf-c-button` | Provides a reference for interactive elements. **Required**                                          |

### Usage

| Class                             | Applied to                     | Outcome                                                                                                                                                                                     |
| --------------------------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-data-list__item-control`   | `<div>`                        | Initiates a container for data list controls. For example, add `.pf-c-data-list__check` here. **Required**                                                                                  |
| `.pf-c-data-list__item-action`    | `<div>`                        | Initiates a container for the data list actions. For example, add `.pf-c-data-list__action` here. **Required**                                                                              |
| `.pf-c-data-list__check`          | `<div>`                        | Initiates a data list check cell. **Required**                                                                                                                                              |
| `.pf-c-data-list__action`         | `<div>`                        | Initiates a data list action button cell. **Required**                                                                                                                                      |
| `.pf-m-hidden{-on-[breakpoint]}`  | `.pf-c-data-list__item-action` | Hides an actions container at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes), or hides it at all breakpoints with `.pf-m-hidden`. |
| `.pf-m-visible{-on-[breakpoint]}` | `.pf-c-data-list__item-action` | Shows an actions container at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                     |

### Expandable

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Expandable data list example"
  id="data-list-expandable"
>
  <li
    class="pf-c-data-list__item pf-m-expanded"
    aria-labelledby="data-list-expandable-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-toggle1 data-list-expandable-item1"
            id="data-list-expandable-toggle1"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-expandable-content1"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-icon">
          <i class="fas fa-code-branch" aria-hidden="true"></i>
        </div>
        <div class="pf-c-data-list__cell">
          <span id="data-list-expandable-item-1">Primary content</span>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
          <a href="#">link</a>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-item-1-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-item-1-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-content1"
      aria-label="Primary content details"
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-expandable-item-2"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-toggle2 data-list-expandable-item2"
            id="data-list-expandable-toggle2"
            aria-label="Toggle details for"
            aria-expanded="false"
            aria-controls="data-list-expandable-content2"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <div id="data-list-expandable-item-2">Secondary content</div>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-item-2-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-item-2-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-content2"
      aria-label="Secondary content details"
      hidden
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item pf-m-expanded"
    aria-labelledby="data-list-expandable-item-3"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-toggle3 data-list-expandable-item3"
            id="data-list-expandable-toggle3"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-expandable-content3"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-icon">
          <i class="fas fa-code-branch" aria-hidden="true"></i>
        </div>
        <div class="pf-c-data-list__cell">
          <div id="data-list-expandable-item-3">Tertiary content</div>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-item-3-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-item-3-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-content3"
      aria-label="Tertiary content details"
    >
      <div
        class="pf-c-data-list__expandable-content-body pf-m-no-padding"
      >This expanded section has no padding.</div>
    </section>
  </li>
</ul>

```

### Expandable compact

```html
<ul
  class="pf-c-data-list pf-m-compact"
  role="list"
  aria-label="Expandable data list example"
  id="data-list-expandable"
>
  <li
    class="pf-c-data-list__item pf-m-expanded"
    aria-labelledby="data-list-expandable-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-toggle1 data-list-expandable-item1"
            id="data-list-expandable-toggle1"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-expandable-content1"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-icon">
          <i class="fas fa-code-branch" aria-hidden="true"></i>
        </div>
        <div class="pf-c-data-list__cell">
          <div id="data-list-expandable-item-1">Primary content</div>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
          <a href="#">link</a>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-item-1-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-item-1-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-content1"
      aria-label="Primary content details"
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-expandable-item-2"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-toggle2 data-list-expandable-item2"
            id="data-list-expandable-toggle2"
            aria-label="Toggle details for"
            aria-expanded="false"
            aria-controls="data-list-expandable-content2"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-icon">
          <i class="fas fa-code-branch" aria-hidden="true"></i>
        </div>
        <div class="pf-c-data-list__cell">
          <span id="data-list-expandable-item-2">Secondary content</span>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-item-2-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-item-2-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-content2"
      aria-label="Secondary content details"
      hidden
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item pf-m-expanded"
    aria-labelledby="data-list-expandable-item-3"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-toggle3 data-list-expandable-item3"
            id="data-list-expandable-toggle3"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-expandable-content3"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-icon">
          <i class="fas fa-code-branch" aria-hidden="true"></i>
        </div>
        <div class="pf-c-data-list__cell">
          <span id="data-list-expandable-item-3">Tertiary content</span>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-item-3-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-item-3-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-content3"
      aria-label="Tertiary content details"
    >
      <div
        class="pf-c-data-list__expandable-content-body pf-m-no-padding"
      >This expanded section has no padding.</div>
    </section>
  </li>
</ul>

```

### Expandable nested

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Expandable nested data list example"
  id="data-list-expandable-nested"
>
  <li
    class="pf-c-data-list__item pf-m-expanded"
    aria-labelledby="data-list-expandable-nested-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-nested-toggle1 data-list-expandable-nested-item1"
            id="data-list-expandable-nested-toggle1"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-expandable-nested-content1"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-icon">
          <i class="fas fa-code-branch" aria-hidden="true"></i>
        </div>
        <div class="pf-c-data-list__cell">
          <span id="data-list-expandable-nested-item-1">Primary content</span>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
          <a href="#">link</a>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-nested-item-1-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-nested-item-1-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-nested-content1"
      aria-label="Primary content details"
    >
      <div class="pf-c-data-list__expandable-content-body">
        <ul
          class="pf-c-data-list"
          role="list"
          aria-label="Expandable nested nested data list example"
          id="data-list-expandable-nested-nested"
        >
          <li
            class="pf-c-data-list__item pf-m-expanded"
            aria-labelledby="data-list-expandable-nested-nested-item-1"
          >
            <div class="pf-c-data-list__item-row">
              <div class="pf-c-data-list__item-control">
                <div class="pf-c-data-list__toggle">
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="data-list-expandable-nested-nested-toggle1 data-list-expandable-nested-nested-item1"
                    id="data-list-expandable-nested-nested-toggle1"
                    aria-label="Toggle details for"
                    aria-expanded="true"
                    aria-controls="data-list-expandable-nested-nested-content1"
                  >
                    <div class="pf-c-data-list__toggle-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </div>
                  </button>
                </div>
              </div>
              <div class="pf-c-data-list__item-content">
                <div class="pf-c-data-list__cell">
                  <span
                    id="data-list-expandable-nested-nested-item-1"
                  >Nested row 1</span>
                </div>
              </div>
            </div>
            <section
              class="pf-c-data-list__expandable-content"
              id="data-list-expandable-nested-nested-content1"
              aria-label="Nested row 1 details"
            >
              <div
                class="pf-c-data-list__expandable-content-body"
              >Nested row 1 expanded content.</div>
            </section>
          </li>

          <li
            class="pf-c-data-list__item"
            aria-labelledby="data-list-expandable-nested-nested-item-2"
          >
            <div class="pf-c-data-list__item-row">
              <div class="pf-c-data-list__item-control">
                <div class="pf-c-data-list__toggle">
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="data-list-expandable-nested-nested-toggle2 data-list-expandable-nested-nested-item2"
                    id="data-list-expandable-nested-nested-toggle2"
                    aria-label="Toggle details for"
                    aria-expanded="false"
                    aria-controls="data-list-expandable-nested-nested-content2"
                  >
                    <div class="pf-c-data-list__toggle-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </div>
                  </button>
                </div>
              </div>
              <div class="pf-c-data-list__item-content">
                <div class="pf-c-data-list__cell">
                  <div
                    id="data-list-expandable-nested-nested-item-2"
                  >Nested row 2</div>
                </div>
              </div>
            </div>
            <section
              class="pf-c-data-list__expandable-content"
              id="data-list-expandable-nested-nested-content2"
              aria-label="Nested row 2 details"
              hidden
            >
              <div
                class="pf-c-data-list__expandable-content-body"
              >Nested row 2 expanded content.</div>
            </section>
          </li>

          <li
            class="pf-c-data-list__item pf-m-expanded"
            aria-labelledby="data-list-expandable-nested-nested-item-3"
          >
            <div class="pf-c-data-list__item-row">
              <div class="pf-c-data-list__item-control">
                <div class="pf-c-data-list__toggle">
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-labelledby="data-list-expandable-nested-nested-toggle3 data-list-expandable-nested-nested-item3"
                    id="data-list-expandable-nested-nested-toggle3"
                    aria-label="Toggle details for"
                    aria-expanded="true"
                    aria-controls="data-list-expandable-nested-nested-content3"
                  >
                    <div class="pf-c-data-list__toggle-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </div>
                  </button>
                </div>
              </div>
              <div class="pf-c-data-list__item-content">
                <div class="pf-c-data-list__cell">
                  <div
                    id="data-list-expandable-nested-nested-item-3"
                  >Nested row 3</div>
                </div>
              </div>
            </div>
            <section
              class="pf-c-data-list__expandable-content"
              id="data-list-expandable-nested-nested-content3"
              aria-label="Nested row 3 details"
            >
              <div
                class="pf-c-data-list__expandable-content-body"
              >Nested row 3 expanded content.</div>
            </section>
          </li>
        </ul>
      </div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-expandable-nested-item-2"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-nested-toggle2 data-list-expandable-nested-item2"
            id="data-list-expandable-nested-toggle2"
            aria-label="Toggle details for"
            aria-expanded="false"
            aria-controls="data-list-expandable-nested-content2"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <div id="data-list-expandable-nested-item-2">Secondary content</div>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-nested-item-2-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-nested-item-2-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-nested-content2"
      aria-label="Secondary content details"
      hidden
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item pf-m-expanded"
    aria-labelledby="data-list-expandable-nested-item-3"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-expandable-nested-toggle3 data-list-expandable-nested-item3"
            id="data-list-expandable-nested-toggle3"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-expandable-nested-content3"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-icon">
          <i class="fas fa-code-branch" aria-hidden="true"></i>
        </div>
        <div class="pf-c-data-list__cell">
          <div id="data-list-expandable-nested-item-3">Tertiary content</div>
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet.</span>
        </div>
        <div class="pf-c-data-list__cell">
          <span>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</span>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-expandable-nested-item-3-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-expandable-nested-item-3-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-expandable-nested-content3"
      aria-label="Tertiary content details"
    >
      <div
        class="pf-c-data-list__expandable-content-body pf-m-no-padding"
      >This expanded section has no padding.</div>
    </section>
  </li>
</ul>

```

### Compact

```html
<ul
  class="pf-c-data-list pf-m-compact pf-m-grid-sm"
  role="list"
  aria-label="Compact data list example"
  id="data-list-compact"
>
  <li class="pf-c-data-list__item" aria-labelledby="data-list-compact-item-1">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-compact-item-1-checkbox"
            aria-labelledby="data-list-compact-item-1"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-compact-item-1">Primary content</span> Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div
          class="pf-c-data-list__cell"
        >Secondary content. Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.</div>
        <div class="pf-c-data-list__cell">
          <span>Tertiary Content</span> Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div class="pf-c-data-list__cell">
          <span>More Content</span> Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div class="pf-c-data-list__cell">
          <span>More Content</span> Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-compact-item-1-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-compact-item-1-dropdown-kebab-button"
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
    </div>
  </li>

  <li class="pf-c-data-list__item" aria-labelledby="data-list-compact-item-2">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-compact-item-2-checkbox"
            aria-labelledby="data-list-compact-item-2"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-compact-item-2">Primary content - lorem ipsum</span> dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div
          class="pf-c-data-list__cell"
        >Secondary content. Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.</div>
      </div>
      <div class="pf-c-data-list__item-action pf-m-hidden-on-lg">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-compact-item-2-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-compact-item-2-dropdown-kebab-button"
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
      <div class="pf-c-data-list__item-action pf-m-hidden pf-m-visible-on-lg">
        <button class="pf-c-button pf-m-primary" type="button">Primary</button>
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
      </div>
    </div>
  </li>

  <li class="pf-c-data-list__item" aria-labelledby="data-list-compact-item-3">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-compact-item-3-checkbox"
            aria-labelledby="data-list-compact-item-3"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-compact-item-3">Primary content - lorem ipsum</span> dolor sit amet, consectetur adipisicing elit, sed do eiusmod.
        </div>
        <div
          class="pf-c-data-list__cell"
        >Secondary content. Dolor sit amet, consectetur adipisicing elit, sed do eiusmod.</div>
      </div>
      <div class="pf-c-data-list__item-action pf-m-hidden-on-xl">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-compact-item-3-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-compact-item-3-dropdown-kebab-button"
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
      <div class="pf-c-data-list__item-action pf-m-hidden pf-m-visible-on-xl">
        <button class="pf-c-button pf-m-primary" type="button">Primary</button>
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
        <button class="pf-c-button pf-m-secondary" type="button">Secondary</button>
      </div>
    </div>
  </li>
</ul>

```

### Accessibility

| Attribute                                       | Applied to                                 | Outcome                                                                          |
| ----------------------------------------------- | ------------------------------------------ | -------------------------------------------------------------------------------- |
| `aria-expanded="true"`                          | `.pf-c-data-list__toggle` > `.pf-c-button` | Indicates that the expandable content is visible. **Required**                   |
| `hidden`                                        | `.pf-c-data-list__expandable-content`      | Indicates that the expandable content is hidden. **Required**                    |
| `aria-label="[descriptive text]"`               | `.pf-c-data-list__toggle` > `.pf-c-button` | Provides an accessible name for toggle button. **Required**                      |
| `aria-labelledby="{title_cell_id} {button_id}"` | `.pf-c-data-list__toggle` > `.pf-c-button` | Establishes relationship between aria-label text and toggle button. **Required** |
| `id="{button_id}"`                              | `.pf-c-data-list__toggle` > `.pf-c-button` | Provides a reference for toggle button description. **Required**                 |
| `aria-controls="[id of element controlled]"`    | `.pf-c-data-list__toggle` > `.pf-c-button` | Identifies the section controlled by the toggle button. **Required**             |

### Usage

| Class                                      | Applied to                                 | Outcome                                                                                                          |
| ------------------------------------------ | ------------------------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| `.pf-c-data-list__item-control`            | `<div>`                                    | Initiates a container for data list controls. For example, add `.pf-c-data-list__toggle` here. **Required**      |
| `.pf-c-data-list__toggle`                  | `<div>`                                    | Initiates a toggle button.                                                                                       |
| `.pf-c-data-list__toggle-icon`             | `<span>`                                   | Initiates a toggle icon.                                                                                         |
| `.pf-c-data-list__expandable-content`      | `<div>`                                    | Initiates an expandable content container.                                                                       |
| `.pf-c-data-list__expandable-content-body` | `<div>`                                    | Initiates an expandable content container body. **Required** when `.pf-c-data-list__expandable-content` is used. |
| `.pf-m-expanded`                           | `.pf-c-data-list__item`                    | Modifies for expanded state.                                                                                     |
| `.pf-m-compact`                            | `.pf-c-data-list`                          | Modifies for compact variation.                                                                                  |
| `.pf-m-no-padding`                         | `.pf-c-data-list__expandable-content-body` | Removes padding for the expandable content body.                                                                 |
| `.pf-m-icon`                               | `.pf-c-data-list__cell`                    | Modifies a data list cell to not grow and align-left when its the first data-list\_\_cell element.               |

### Modifiers

```html
<h2 class="Preview__section-title">Default fitting - example 1</h2>
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Width modifier data list example 1"
  id="data-list-default-fitting"
>
  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-default-fitting-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-default-fitting-item-1-checkbox"
            aria-labelledby="data-list-default-fitting-item-1"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <div class="Preview__placeholder">
            <b id="data-list-default-fitting-item-1">default</b>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
          </div>
        </div>
        <div class="pf-c-data-list__cell">
          <div class="Preview__placeholder">
            <b>default</b>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</p>
          </div>
        </div>
      </div>
    </div>
  </li>
</ul>
<h2 class="Preview__section-title">Flex modifiers - example 2</h2>
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Width modifier data list example 2"
  id="data-list-flex-modifiers"
>
  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-flex-modifiers-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-flex-modifiers-item-1-checkbox"
            aria-labelledby="data-list-flex-modifiers-item-1"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-flex-2">
          <div class="Preview__placeholder">
            <b id="data-list-flex-modifiers-item-1">.pf-m-flex-2</b>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt.</p>
          </div>
        </div>
        <div class="pf-c-data-list__cell pf-m-flex-4">
          <div class="Preview__placeholder">
            <b>.pf-m-flex-4</b>
            <p>Lorem ipsum dolor sit amet.</p>
          </div>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-flex-modifiers-item-1-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-flex-modifiers-item-1-dropdown-kebab-button"
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
    </div>
  </li>
</ul>
<h2 class="Preview__section-title">Flex modifiers - example 3</h2>
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Width modifier data list example 3"
  id="data-list-flex-modifiers-2"
>
  <li
    class="pf-c-data-list__item pf-m-expanded"
    aria-labelledby="data-list-flex-modifiers-2-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-flex-modifiers-2-toggle1 data-list-flex-modifiers-2-item1"
            id="data-list-flex-modifiers-2-toggle1"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-flex-modifiers-2-content1"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>

        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-flex-modifiers-2-item-1-checkbox"
            aria-labelledby="data-list-flex-modifiers-2-item-1"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell pf-m-flex-5">
          <div class="Preview__placeholder">
            <b id="data-list-flex-modifiers-2-item-1">.pf-m-flex-5</b>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
          </div>
        </div>
        <div class="pf-c-data-list__cell pf-m-flex-2">
          <div class="Preview__placeholder">
            <b>.pf-m-flex-2</b>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
          </div>
        </div>
        <div class="pf-c-data-list__cell pf-m-flex-3">
          <div class="Preview__placeholder">
            <b>.pf-m-flex-3</b>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
          </div>
        </div>
        <div class="pf-c-data-list__cell pf-m-flex-3">
          <div class="Preview__placeholder">
            <b>.pf-m-flex-3</b>
            <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit.</p>
          </div>
        </div>
      </div>
      <div class="pf-c-data-list__item-action">
        <div class="pf-c-data-list__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="data-list-flex-modifiers-2-item-1-dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="data-list-flex-modifiers-2-item-1-dropdown-kebab-button"
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
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-flex-modifiers-2-content1"
      aria-label="Primary content details"
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>
</ul>

```

### Accessibility

| Attribute                                    | Applied to                                 | Outcome                                                              |
| -------------------------------------------- | ------------------------------------------ | -------------------------------------------------------------------- |
| `aria-controls="[id of element controlled]"` | `.pf-c-data-list__toggle` > `.pf-c-button` | Identifies the section controlled by the toggle button. **Required** |

### Usage

| Class                        | Applied to              | Outcome                                                       |
| ---------------------------- | ----------------------- | ------------------------------------------------------------- |
| `.pf-m-flex-{1, 2, 3, 4, 5}` | `.pf-c-data-list__cell` | Percentage based modifier for `.pf-c-data-list__cell` widths. |

### Selectable rows

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Selectable rows data list example"
  id="data-list-selectable-rows"
>
  <li
    class="pf-c-data-list__item pf-m-expanded pf-m-selectable pf-m-selected"
    aria-labelledby="data-list-selectable-rows-item-1"
    tabindex="0"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-selectable-rows-item-1">Primary content</span>
        </div>
      </div>
    </div>
  </li>

  <li
    class="pf-c-data-list__item pf-m-expanded pf-m-selectable pf-m-selected"
    aria-labelledby="data-list-selectable-rows-item-2"
    tabindex="0"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-selectable-rows-item-2"
          >Secondary content (selected)</span>
        </div>
      </div>
    </div>
  </li>

  <li
    class="pf-c-data-list__item pf-m-selectable"
    aria-labelledby="data-list-selectable-rows-item-3"
    tabindex="0"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-selectable-rows-item-3">Tertiary content</span>
        </div>
      </div>
    </div>
  </li>
</ul>

```

### Accessibility

| Attribute      | Applied to                              | Outcome                                                                                         |
| -------------- | --------------------------------------- | ----------------------------------------------------------------------------------------------- |
| `tabindex="0"` | `.pf-c-data-list__item.pf-m-selectable` | Inserts the selectable row into the tab order of the page so that it is focusable. **Required** |

### Usage

| Class              | Applied to              | Outcome                                             |
| ------------------ | ----------------------- | --------------------------------------------------- |
| `.pf-m-selectable` | `.pf-c-data-list__item` | Modifies a data list item so that it is selectable. |
| `.pf-m-selected`   | `.pf-c-data-list__item` | Modifies a data list item for the selected state.   |

### Selectable expandable rows

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Selectable, expandable data list example"
  id="data-list-selectable-expandable-rows"
>
  <li
    class="pf-c-data-list__item pf-m-expanded pf-m-selectable"
    aria-labelledby="data-list-selectable-expandable-rows-item-1"
    tabindex="0"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-selectable-expandable-rows-toggle1 data-list-selectable-expandable-rows-item1"
            id="data-list-selectable-expandable-rows-toggle1"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-selectable-expandable-rows-content1"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-selectable-expandable-rows-item-1"
          >Primary content (selected, expanded)</span>
        </div>
      </div>
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-selectable-expandable-rows-content1"
      aria-label="Primary content details"
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item pf-m-selectable"
    aria-labelledby="data-list-selectable-expandable-rows-item-2"
    tabindex="0"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-selectable-expandable-rows-toggle2 data-list-selectable-expandable-rows-item2"
            id="data-list-selectable-expandable-rows-toggle2"
            aria-label="Toggle details for"
            aria-expanded="false"
            aria-controls="data-list-selectable-expandable-rows-content2"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-selectable-expandable-rows-item-2"
          >Secondary content</span>
        </div>
      </div>
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-selectable-expandable-rows-content2"
      aria-label="Secondary content details"
      hidden
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item pf-m-expanded pf-m-selectable"
    aria-labelledby="data-list-selectable-expandable-rows-item-3"
    tabindex="0"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-selectable-expandable-rows-toggle3 data-list-selectable-expandable-rows-item3"
            id="data-list-selectable-expandable-rows-toggle3"
            aria-label="Toggle details for"
            aria-expanded="true"
            aria-controls="data-list-selectable-expandable-rows-content3"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-selectable-expandable-rows-item-3"
          >Tertiary content (not selected, expanded)</span>
        </div>
      </div>
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-selectable-expandable-rows-content3"
      aria-label="Tertiary content details"
    >
      <div
        class="pf-c-data-list__expandable-content-body pf-m-no-padding"
      >This expanded section has no padding.</div>
    </section>
  </li>

  <li
    class="pf-c-data-list__item pf-m-selectable"
    aria-labelledby="data-list-selectable-expandable-rows-item-4"
    tabindex="0"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <div class="pf-c-data-list__toggle">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-labelledby="data-list-selectable-expandable-rows-toggle4 data-list-selectable-expandable-rows-item4"
            id="data-list-selectable-expandable-rows-toggle4"
            aria-label="Toggle details for"
            aria-expanded="false"
            aria-controls="data-list-selectable-expandable-rows-content4"
          >
            <div class="pf-c-data-list__toggle-icon">
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </div>
          </button>
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-selectable-expandable-rows-item-4"
          >Quaternary content (selected)</span>
        </div>
      </div>
    </div>
    <section
      class="pf-c-data-list__expandable-content"
      id="data-list-selectable-expandable-rows-content4"
      aria-label="Quaternary content details"
      hidden
    >
      <div
        class="pf-c-data-list__expandable-content-body"
      >Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</div>
    </section>
  </li>
</ul>

```

### Draggable

```html
<div
  id="draggable-help"
>Activate the reorder button and use the arrow keys to reorder the list or use your mouse to drag/reorder. Press escape to cancel the reordering.</div>
<ul
  class="pf-c-data-list pf-m-compact"
  role="list"
  aria-label="Draggable data list rows"
  id="data-list-draggable"
>
  <li class="pf-c-data-list__item" aria-labelledby="data-list-draggable-item-1">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <button
          class="pf-c-data-list__item-draggable-button pf-m-disabled"
          type="button"
          aria-label="Reorder"
          aria-pressed="false"
          id="data-list-draggable-draggable-button-1"
          aria-describedby="draggable-help"
          aria-labelledby="data-list-draggable-draggable-button-1 data-list-draggable-item-1"
          disabled
        >
          <span class="pf-c-data-list__item-draggable-icon">
            <i class="fas fa-grip-vertical"></i>
          </span>
        </button>
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-draggable-item-1-checkbox"
            aria-labelledby="data-list-draggable-item-1"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            class="pf-c-data-list__cell-text"
            id="data-list-draggable-item-1"
          >Draggable icon disabled</span>
        </div>
      </div>
    </div>
  </li>

  <li class="pf-c-data-list__item" aria-labelledby="data-list-draggable-item-2">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <button
          class="pf-c-data-list__item-draggable-button"
          type="button"
          aria-label="Reorder"
          aria-pressed="false"
          id="data-list-draggable-draggable-button-2"
          aria-describedby="draggable-help"
          aria-labelledby="data-list-draggable-draggable-button-2 data-list-draggable-item-2"
        >
          <span class="pf-c-data-list__item-draggable-icon">
            <i class="fas fa-grip-vertical"></i>
          </span>
        </button>
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-draggable-item-2-checkbox"
            aria-labelledby="data-list-draggable-item-2"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            class="pf-c-data-list__cell-text"
            id="data-list-draggable-item-2"
          >List item</span>
        </div>
      </div>
    </div>
  </li>

  <li
    class="pf-c-data-list__item pf-m-ghost-row"
    aria-labelledby="data-list-draggable-item-3"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <button
          class="pf-c-data-list__item-draggable-button"
          type="button"
          aria-label="Reorder"
          aria-pressed="false"
          id="data-list-draggable-draggable-button-3"
          aria-describedby="draggable-help"
          aria-labelledby="data-list-draggable-draggable-button-3 data-list-draggable-item-3"
          disabled
        >
          <span class="pf-c-data-list__item-draggable-icon">
            <i class="fas fa-grip-vertical"></i>
          </span>
        </button>
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-draggable-item-3-checkbox"
            aria-labelledby="data-list-draggable-item-3"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            class="pf-c-data-list__cell-text"
            id="data-list-draggable-item-3"
          >Ghost row</span>
        </div>
      </div>
    </div>
  </li>

  <li class="pf-c-data-list__item" aria-labelledby="data-list-draggable-item-4">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-control">
        <button
          class="pf-c-data-list__item-draggable-button"
          type="button"
          aria-label="Reorder"
          aria-pressed="false"
          id="data-list-draggable-draggable-button-4"
          aria-describedby="draggable-help"
          aria-labelledby="data-list-draggable-draggable-button-4 data-list-draggable-item-4"
        >
          <span class="pf-c-data-list__item-draggable-icon">
            <i class="fas fa-grip-vertical"></i>
          </span>
        </button>
        <div class="pf-c-data-list__check">
          <input
            type="checkbox"
            name="data-list-draggable-item-4-checkbox"
            aria-labelledby="data-list-draggable-item-4"
          />
        </div>
      </div>
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            class="pf-c-data-list__cell-text"
            id="data-list-draggable-item-4"
          >List item</span>
        </div>
      </div>
    </div>
  </li>
</ul>
<div
  class="pf-screen-reader"
  aria-live="assertive"
>This is the aria-live section that provides real-time feedback to the user.</div>

```

### Accessibility

| Attribute                                                                                                         | Applied to                                                             | Outcome                                                                                                                                                                                                                           |
| ----------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-pressed="true or false"`                                                                                    | `.pf-c-data-list__item-draggable-button`                               | Indicates that the button is a toggle. When set to "true", `pf-m-active` should also be set so that the button displays in an active state.                                                                                       |
| `aria-live`                                                                                                       | `[element with live text]`                                             | Gives screen reader users live feedback about what's happening during interaction with the data list, both during drag and drop interactions and keyboard interactions. **Highly Recommended**                                    |
| `aria-describedby="[id value of applicable content]"`                                                             | `.pf-c-data-list__item-draggable-button`                               | Gives the draggable button an accessible description by referring to the textual content that describes how to use the button to drag elements. The example here uses a `<div id="draggable-help"></div>`. **Highly recommended** |
| `aria-labelledby="[id value of .pf-c-data-list__item-draggable-button] [id value of .pf-c-data-list__cell-text]"` | `.pf-c-data-list__item-draggable-button`                               | Provides an accessible name for the draggable button.                                                                                                                                                                             |
| `id="[]"`                                                                                                         | `.pf-c-data-list__item-draggable-button`, `.pf-c-data-list__cell-text` | Gives the button and the text element accessible IDs                                                                                                                                                                              |

### Usage

| Class                                    | Applied to                             | Outcome                                                                                       |
| ---------------------------------------- | -------------------------------------- | --------------------------------------------------------------------------------------------- |
| `.pf-c-data-list__item-draggable-button` | `<button>`                             | Initiates the draggable button. Use for drag and drop.                                        |
| `.pf-c-data-list__item-draggable-icon`   | `<span>`                               | Initiates the draggable button icon.                                                          |
| `.pf-m-draggable`                        | `.pf-c-data-list__item`                | Modifies a data list item so that it is draggable.                                            |
| `.pf-m-ghost-row`                        | `.pf-c-data-list__item.pf-m-draggable` | Modifies a draggable data list item to be the ghost row.                                      |
| `.pf-m-disabled`                         | `.pf-c-data-list__item.pf-m-draggable` | Modifies a data list draggable item for the disabled state.                                   |
| `.pf-m-drag-over`                        | `.pf-c-data-list`                      | Modifies the data list to indicate that a draggable item is being dragged over the data list. |

### Text modifiers

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Data list with text modifiers"
  id="data-list-with-text-modifiers"
>
  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-with-text-modifiers-item"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-with-text-modifiers-item"
          >This text will wrap to the next line because it has the default behavior of the data list cell.</span>
        </div>
        <div
          class="pf-c-data-list__cell pf-m-truncate"
        >This text will truncate because it is very very long.</div>
        <div
          class="pf-c-data-list__cell pf-m-break-word"
        >http://thisisaverylongurlthatneedstobreakusethebreakwordmodifier.org</div>
        <div
          class="pf-c-data-list__cell pf-m-nowrap"
        >This text will not break or wrap.</div>
      </div>
    </div>
    <div class="pf-c-data-list__item-row pf-m-truncate">
      <div class="pf-c-data-list__item-content">
        <div
          class="pf-c-data-list__cell"
        >This text will truncate because it is very very long. This text will truncate because it is very very long.</div>
        <div
          class="pf-c-data-list__cell"
        >This text will truncate because it is very very long. This text will truncate because it is very very long.</div>
        <div
          class="pf-c-data-list__cell"
        >This text will truncate because it is very very long. This text will truncate because it is very very long.</div>
        <div
          class="pf-c-data-list__cell"
        >This text will truncate because it is very very long. This text will truncate because it is very very long.</div>
      </div>
    </div>
    <div class="pf-c-data-list__item-row pf-m-break-word">
      <div class="pf-c-data-list__item-content">
        <div
          class="pf-c-data-list__cell"
        >http://thisisaverylongurlthatneedstobreakusethebreakwordmodifier.org</div>
        <div
          class="pf-c-data-list__cell"
        >http://thisisaverylongurlthatneedstobreakusethebreakwordmodifier.org</div>
        <div
          class="pf-c-data-list__cell"
        >http://thisisaverylongurlthatneedstobreakusethebreakwordmodifier.org</div>
        <div
          class="pf-c-data-list__cell"
        >http://thisisaverylongurlthatneedstobreakusethebreakwordmodifier.org</div>
      </div>
    </div>
    <div class="pf-c-data-list__item-row pf-m-nowrap">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">This text will not break or wrap.</div>
        <div class="pf-c-data-list__cell">This text will not break or wrap.</div>
        <div class="pf-c-data-list__cell">This text will not break or wrap.</div>
        <div class="pf-c-data-list__cell">This text will not break or wrap.</div>
      </div>
    </div>
  </li>
</ul>

```

### Text-modifiers-data-list-text

```html
<ul
  class="pf-c-data-list"
  role="list"
  aria-label="Data list with modifiers and text"
  id="data-list-with-text-modifiers-and-text"
>
  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-with-text-modifiers-and-text-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span
            id="data-list-with-text-modifiers-and-text-item-1"
          >This text will wrap to the next line because it has the default behavior of the data list cell.</span>
          <span
            class="pf-c-data-list__text pf-m-truncate"
          >This is data list text, you can apply `pf-m-truncate` directly to the text. This is data list text, you can apply `pf-m-truncate` directly to the text.</span>
        </div>
        <div class="pf-c-data-list__cell">
          This text will wrap to the next line because it has the default behavior of the data list cell.
          <span
            class="pf-c-data-list__text pf-m-break-word"
          >http://thisisaverylongdatalisttextthatneedstobreakusethebreakwordmodifier.org</span>
        </div>
        <div class="pf-c-data-list__cell">
          This text will wrap to the next line because it has the default behavior of the data list cell.
          <span
            class="pf-c-data-list__text pf-m-nowrap"
          >This is data list text, you can apply `pf-m-nowrap` directly to the text.</span>
        </div>
      </div>
    </div>
  </li>
</ul>

```

### Usage

| Class                   | Applied to                                                                                       | Outcome                                                                              |
| ----------------------- | ------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------ |
| `.pf-c-data-list__text` | `*`                                                                                              | Inserts the data list text element. Use this class to modify specific text directly. |
| `.pf-m-truncate`        | `.pf-c-data-list`, `.pf-c-data-list__item-row`, `.pf-c-data-list__cell`, `.pf-c-data-list__text` | Modifies the data list element so that text is truncated.                            |
| `.pf-m-break-word`      | `.pf-c-data-list`, `.pf-c-data-list__item-row`, `.pf-c-data-list__cell`, `.pf-c-data-list__text` | Modifies the data list element so that text breaks to the next line.                 |
| `.pf-m-nowrap`          | `.pf-c-data-list`, `.pf-c-data-list__item-row`, `.pf-c-data-list__cell`, `.pf-c-data-list__text` | Modifies the data list element so that text does not wrap to the next line.          |

## Documentation

### Overview

The DataList component provides a flexible alternative to the Table component, wherein individual data points may or may not exist within each row. DataList relies upon PatternFly layouts to achieve desired presentation within `pf-c-data-list__cell`s. DataLists do not have headers. If headers are required, use the [table component](/components/table).

### Grid

```html
<ul
  class="pf-c-data-list pf-m-grid"
  role="list"
  aria-label="Grid data list example"
  id="data-list-grid"
>
  <li class="pf-c-data-list__item" aria-labelledby="data-list-grid-item-1">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-grid-item-1">Cell 1</span>
        </div>
        <div class="pf-c-data-list__cell">Cell 2</div>
        <div class="pf-c-data-list__cell">Cell 3</div>
        <div class="pf-c-data-list__cell">Cell 4</div>
        <div class="pf-c-data-list__cell">Cell 5</div>
        <div class="pf-c-data-list__cell">Cell 6</div>
      </div>
    </div>
  </li>
  <li class="pf-c-data-list__item" aria-labelledby="data-list-grid-item-2">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-grid-item-2">Cell 1</span>
        </div>
        <div class="pf-c-data-list__cell">Cell 2</div>
        <div class="pf-c-data-list__cell">Cell 3</div>
        <div class="pf-c-data-list__cell">Cell 4</div>
        <div class="pf-c-data-list__cell">Cell 5</div>
        <div class="pf-c-data-list__cell">Cell 6</div>
      </div>
    </div>
  </li>
</ul>

```

### Grid (small breakpoint)

```html
<ul
  class="pf-c-data-list pf-m-grid-sm"
  role="list"
  aria-label="Grid small data list example"
  id="data-list-grid-small"
>
  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-grid-small-item-1"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-grid-small-item-1">Cell 1</span>
        </div>
        <div class="pf-c-data-list__cell">Cell 2</div>
        <div class="pf-c-data-list__cell">Cell 3</div>
        <div class="pf-c-data-list__cell">Cell 4</div>
        <div class="pf-c-data-list__cell">Cell 5</div>
        <div class="pf-c-data-list__cell">Cell 6</div>
      </div>
    </div>
  </li>
  <li
    class="pf-c-data-list__item"
    aria-labelledby="data-list-grid-small-item-2"
  >
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-grid-small-item-2">Cell 1</span>
        </div>
        <div class="pf-c-data-list__cell">Cell 2</div>
        <div class="pf-c-data-list__cell">Cell 3</div>
        <div class="pf-c-data-list__cell">Cell 4</div>
        <div class="pf-c-data-list__cell">Cell 5</div>
        <div class="pf-c-data-list__cell">Cell 6</div>
      </div>
    </div>
  </li>
</ul>

```

### Grid none

```html
<ul
  class="pf-c-data-list pf-m-grid-none"
  role="list"
  aria-label="Grid none data list example"
  id="data-list-grid-none"
>
  <li class="pf-c-data-list__item" aria-labelledby="data-list-grid-none-item-1">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-grid-none-item-1">Cell 1</span>
        </div>
        <div class="pf-c-data-list__cell">Cell 2</div>
        <div class="pf-c-data-list__cell">Cell 3</div>
        <div class="pf-c-data-list__cell">Cell 4</div>
        <div class="pf-c-data-list__cell">Cell 5</div>
        <div class="pf-c-data-list__cell">Cell 6</div>
      </div>
    </div>
  </li>
  <li class="pf-c-data-list__item" aria-labelledby="data-list-grid-none-item-2">
    <div class="pf-c-data-list__item-row">
      <div class="pf-c-data-list__item-content">
        <div class="pf-c-data-list__cell">
          <span id="data-list-grid-none-item-2">Cell 1</span>
        </div>
        <div class="pf-c-data-list__cell">Cell 2</div>
        <div class="pf-c-data-list__cell">Cell 3</div>
        <div class="pf-c-data-list__cell">Cell 4</div>
        <div class="pf-c-data-list__cell">Cell 5</div>
        <div class="pf-c-data-list__cell">Cell 6</div>
      </div>
    </div>
  </li>
</ul>

```

### Usage

| Class                                      | Applied to        | Outcome                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ------------------------------------------ | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-grid{-[none, sm, md, lg, xl, 2xl]}` | `.pf-c-data-list` | Modifies the data list to switch to a grid layout at a specified [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). `.pf-m-grid` will display the grid layout at all breakpoints. `.pf-m-grid-none` will display the desktop layout at all breakpoints. **Note:** Without a grid modifier, the data list will display the grid layout by default and switch to the desktop layout at a medium breakpoint. |
