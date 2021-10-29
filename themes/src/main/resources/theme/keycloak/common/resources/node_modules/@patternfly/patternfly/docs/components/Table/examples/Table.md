---
id: Table
section: components
cssPrefix: pf-c-table
---import './Table.css'

# Examples

## Basic table

### Basic table example

```html
<table
  class="pf-c-table pf-m-grid-md"
  role="grid"
  aria-label="This is a simple table example"
  id="table-basic"
>
  <caption>This is the table caption</caption>
  <thead>
    <tr role="row">
      <th role="columnheader" scope="col">Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 1</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 2</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 3</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 4</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>
</table>

```

### Basic table accessibility

| Attribute                       | Applied to    | Outcome                                                                                                                                                   |
| ------------------------------- | ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="grid"`                   | `.pf-c-table` | Identifies the element that serves as the grid widget container. **Required**                                                                             |
| `aria-label`                    | `.pf-c-table` | Provides an accessible name for the table when a descriptive `<caption>` or `<h*>` is not available. **Required in the absence of `<caption>` or `<h*>`** |
| `data-label="[td description]"` | `<td>`        | This attribute replaces table header in mobile viewport. It is rendered by `::before` pseudo element.                                                     |

### Basic table usage

| Class                  | Applied to     | Outcome                                 |
| ---------------------- | -------------- | --------------------------------------- |
| `.pf-c-table`          | `<table>`      | Initiates a table element. **Required** |
| `.pf-c-table__caption` | `<caption>`    | Initiates a table caption.              |
| `.pf-m-center`         | `<th>`, `<td>` | Modifies cell to center its contents.   |

## Sortable

### Sortable example

```html
<table
  class="pf-c-table pf-m-grid-lg"
  role="grid"
  aria-label="This is a sortable table example"
  id="table-sortable"
>
  <thead>
    <tr role="row">
      <th
        class="pf-c-table__sort pf-m-selected"
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
        class="pf-c-table__sort pf-m-help"
        role="columnheader"
        aria-sort="none"
        scope="col"
      >
        <div class="pf-c-table__column-help">
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Branches</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
          <span class="pf-c-table__column-help-action">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="More info"
            >
              <i class="pficon pf-icon-help" aria-hidden="true"></i>
            </button>
          </span>
        </div>
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
      <th role="columnheader" scope="col">Workspaces</th>
      <th class="pf-m-help" role="columnheader" scope="col">
        <div class="pf-c-table__column-help">
          <span class="pf-c-table__text">Last commit</span>
          <span class="pf-c-table__column-help-action">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="More info"
            >
              <i class="pficon pf-icon-help" aria-hidden="true"></i>
            </button>
          </span>
        </div>
      </th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 1</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 2</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 3</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 4</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>
</table>

```

### Sortable accessibility

| Attribute                             | Applied to          | Outcome                                                                                                                                                                    |
| ------------------------------------- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-sort=[ascending or descending]` | `.pf-c-table__sort` | Indicates if columns in a table are sorted in ascending or descending order. For each table, authors **SHOULD** apply aria-sort to only one header at a time. **Required** |

### Sortable usage

| Class                         | Applied to                                                                     | Outcome                                                                                                                                                                                                                            |
| ----------------------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-table__sort`           | `<th>`                                                                         | Initiates a table header sort cell. **Required for sortable table columns**                                                                                                                                                        |
| `.pf-c-table__button`         | `<button>`, `<a>`                                                              | Initiates a table header sort cell button. If sorting a table row generates a unique URL that can be used as the `href` value for this element, use an `<a>`. Otherwise, use a `<button>`. **Required for sortable table columns** |
| `.pf-c-table__button-content` | `<div>`                                                                        | Initiates a table header sort cell button content container. **Required for sortable table columns** Note: this is only necessary because `<button>` does not support`display: grid`.                                              |
| `.pf-c-table__sort-indicator` | `.pf-c-table__sort > .pf-c-table__button > span`                               | Initiates a sort indicator. **Required for sortable table columns**                                                                                                                                                                |
| `.pf-m-selected`              | `.pf-c-table__sort`                                                            | Modifies for sort selected state. **Required for sortable table columns**                                                                                                                                                          |
| `.pf-m-help`                  | `.pf-c-table__sort`, `.pf-c-table th`                                          | Modifies a sortable table header to accommodate a help tooltip. **Required for sortable table columns with help tooltips**                                                                                                         |
| `.fa-arrows-alt-v`            | `.pf-c-table__sort > .pf-c-table__button > .pf-c-table__sort-indicator > .fas` | Initiates icon within unsorted, sortable table header. **Required for sortable table columns**                                                                                                                                     |
| `.fa-long-arrow-alt-up`       | `.pf-c-table__sort > .pf-c-table__button > .pf-c-table__sort-indicator > .fas` | Initiates icon within ascending sorted and selected, sortable table header. **Required for sortable table columns**                                                                                                                |
| `.fa-long-arrow-alt-down`     | `.pf-c-table__sort > .pf-c-table__button > .pf-c-table__sort-indicator > .fas` | Initiates icon within descending sorted and selected, sortable table header. **Required for sortable table columns**                                                                                                               |

## With checkboxes, radio select, and actions

### Checkboxes and actions example

```html
<table
  class="pf-c-table pf-m-grid-lg"
  role="grid"
  aria-label="This is a table with checkboxes"
  id="table-checkboxes-and-actions"
>
  <thead>
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-checkboxes-and-actions-check-all"
          aria-label="Select all rows"
        />
      </td>
      <th role="columnheader" scope="col">Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
      <td></td>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-checkboxes-and-actions-checkrow1"
          aria-labelledby="table-checkboxes-and-actions-node1"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div id="table-checkboxes-and-actions-node1">Node 1</div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-checkboxes-and-actions-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-checkboxes-and-actions-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-checkboxes-and-actions-checkrow2"
          aria-labelledby="table-checkboxes-and-actions-node2"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-checkboxes-and-actions-node2">Node 2</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-checkboxes-and-actions-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-checkboxes-and-actions-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-checkboxes-and-actions-checkrow3"
          aria-labelledby="table-checkboxes-and-actions-node3"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-checkboxes-and-actions-node3">Node 3</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-checkboxes-and-actions-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-checkboxes-and-actions-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-checkboxes-and-actions-checkrow4"
          aria-labelledby="table-checkboxes-and-actions-node4"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-checkboxes-and-actions-node4">Node 4</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-checkboxes-and-actions-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-checkboxes-and-actions-dropdown-kebab-4-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Single select radio example

```html
<table
  class="pf-c-table pf-m-grid-lg"
  role="grid"
  aria-label="This is single select table with radio inputs"
  id="table-single-select-radio"
>
  <thead>
    <tr role="row">
      <td></td>
      <th role="columnheader" scope="col">Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
      <td></td>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="radio"
          name="table-single-select-radio-radio"
          aria-labelledby="table-single-select-radio-node1"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div id="table-single-select-radio-node1">Node 1</div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-single-select-radio-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-single-select-radio-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="radio"
          name="table-single-select-radio-radio"
          aria-labelledby="table-single-select-radio-node2"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-single-select-radio-node2">Node 2</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-single-select-radio-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-single-select-radio-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="radio"
          name="table-single-select-radio-radio"
          aria-labelledby="table-single-select-radio-node3"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-single-select-radio-node3">Node 3</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-single-select-radio-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-single-select-radio-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="radio"
          name="table-single-select-radio-radio"
          aria-labelledby="table-single-select-radio-node4"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-single-select-radio-node4">Node 4</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-single-select-radio-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-single-select-radio-dropdown-kebab-4-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

When including interactive elements in a table, the primary, descriptive cell in the corresponding row is a `<th>`, rather than a `<td>`. In this example, 'Node 1' and 'Node 2 siemur/test-space' are `<th>`s.

When header cells are empty or they contain interactive elements, `<th>` should be replaced with `<td>`.

### Checkboxes, radio select, and actions accessibility

| Attribute                                                               | Applied to                 | Outcome                                                                                                                              |
| ----------------------------------------------------------------------- | -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `aria-labelledby="[row_header_id]"` or `aria-label="[descriptive text]` | `.pf-c-table__check input` | Provides an accessible name for the checkbox or radio input. **Required**                                                            |
| `id`                                                                    | row header `<th> > *`      | Provides an accessible description for the checkbox or radio. **Required if using `aria-labelledby` for `.pf-c-table__check input`** |

### Checkboxes, radio select, and actions usage

| Class                             | Applied to     | Outcome                                         |
| --------------------------------- | -------------- | ----------------------------------------------- |
| `.pf-c-table__check`              | `<th>`, `<td>` | Initiates a checkbox or radio input table cell. |
| `.pf-c-table__action`             | `<th>`, `<td>` | Initiates an action table cell.                 |
| `.pf-c-table__inline-edit-action` | `<th>`, `<td>` | Initiates an inline edit action table cell.     |

## Expandable

### Expandable example

```html
<table
  class="pf-c-table pf-m-expandable pf-m-grid-lg"
  role="grid"
  aria-label="Expandable table example"
  id="table-expandable"
>
  <thead>
    <tr role="row">
      <td></td>
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-expandable-check-all"
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

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-expandable-node1 table-expandable-expandable-toggle1"
          id="table-expandable-expandable-toggle1"
          aria-label="Details"
          aria-controls="table-expandable-content1"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-expandable-checkrow1"
          aria-labelledby="table-expandable-node1"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-expandable-node1">Node 1</div>
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
            id="table-expandable-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-expandable-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td></td>
      <td></td>
      <td role="cell" colspan="4" id="table-expandable-content1">
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
          aria-labelledby="table-expandable-node2 table-expandable-expandable-toggle2"
          id="table-expandable-expandable-toggle2"
          aria-label="Details"
          aria-controls="table-expandable-content2"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-expandable-checkrow2"
          aria-labelledby="table-expandable-node2"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-expandable-node2">Node 2</div>
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
            id="table-expandable-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-expandable-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td role="cell" colspan="7" id="table-expandable-content2">
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-expandable-node3 table-expandable-expandable-toggle3"
          id="table-expandable-expandable-toggle3"
          aria-label="Details"
          aria-controls="table-expandable-content3"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-expandable-checkrow3"
          aria-labelledby="table-expandable-node3"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-expandable-node3">Node 3</div>
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
            id="table-expandable-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-expandable-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td role="cell" colspan="7" id="table-expandable-content3">
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-expandable-node4 table-expandable-expandable-toggle4"
          id="table-expandable-expandable-toggle4"
          aria-label="Details"
          aria-controls="table-expandable-content4"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-expandable-checkrow4"
          aria-labelledby="table-expandable-node4"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-expandable-node4">Node 4</div>
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
            id="table-expandable-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-expandable-dropdown-kebab-4-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td
        class="pf-m-no-padding"
        role="cell"
        colspan="7"
        id="table-expandable-content4"
      >
        <div
          class="pf-c-table__expandable-row-content"
        >Expandable row content has no padding.</div>
      </td>
    </tr>
  </tbody>
</table>

```

Note: To apply padding to `.pf-c-table__expandable-row`, wrap the content in `.pf-c-table__expandable-row-content`. For no padding add `.pf-m-no-padding` to `.pf-c-table__expandable-row` > `<td>`

### Expandable accessibility

| Attribute                                             | Applied to                             | Outcome                                                                       |
| ----------------------------------------------------- | -------------------------------------- | ----------------------------------------------------------------------------- |
| `hidden`                                              | `.pf-c-table__expandable-row`          | Indicates that the expandable content is hidden. **Required**                 |
| `aria-expanded="true"`                                | `.pf-c-table__toggle` > `.pf-c-button` | Indicates that the row is visible. **Required**                               |
| `aria-label="[descriptive text]"`                     | `.pf-c-table__toggle` > `.pf-c-button` | Provides an accessible name for toggle button. **Required**                   |
| `aria-labelledby="[title_cell_id] [button_id]"`       | `.pf-c-table__toggle` > `.pf-c-button` | Provides an accessible description for toggle button. **Required**            |
| `id="[button_id]"`                                    | `.pf-c-table__toggle` > `.pf-c-button` | Provides a reference for toggle button description. **Required**              |
| `aria-controls="[id of element the button controls]"` | `.pf-c-table__toggle` > `.pf-c-button` | Identifies the expanded content controlled by the toggle button. **Required** |

### Expandable usage

| Class                                 | Applied to                                                            | Outcome                                         |
| ------------------------------------- | --------------------------------------------------------------------- | ----------------------------------------------- |
| `.pf-c-table__toggle-icon`            | `<span>`                                                              | Initiates the table toggle icon wrapper.        |
| `.pf-c-table__expandable-row`         | `<tr>`                                                                | Initiates an expandable row.                    |
| `.pf-c-table__expandable-row-content` | `.pf-c-table__expandable-row` > `<td>` > `<div>`                      | Initiates an expandable row content wrapper.    |
| `.pf-m-expanded`                      | `.pf-c-table__toggle` > `.pf-c-button`, `.pf-c-table__expandable-row` | Modifies for expanded state.                    |
| `.pf-m-no-padding`                    | `.pf-c-table__expandable-row` > `<td>`                                | Modifies the expandable row to have no padding. |

## Compound expansion

### Compound expansion example

```html
<table
  class="pf-c-table pf-m-expandable pf-m-grid-md"
  role="grid"
  aria-label="Compound expandable table example"
  id="table-compound-expansion"
>
  <thead>
    <tr role="row">
      <th
        class="pf-c-table__sort pf-m-selected"
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
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
      <td></td>
      <td></td>
    </tr>
  </thead>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr class="pf-c-table__control-row" role="row">
      <td
        class="pf-c-table__compound-expansion-togglepf-m-expanded pf-m-expanded"
        role="cell"
        data-label="Repositories"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;10
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Branches"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
            234
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Pull requests"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
            4
          </span>
        </button>
      </td>
      <th role="columnheader" data-label="Workspaces">
        <a href="#">siemur/test-space</a>
      </th>
      <td role="cell" data-label="Last commit">
        <span>20 minutes</span>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Open in Github</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compound-expansion-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compound-expansion-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-1"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-1-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-2"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-2-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-3"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-3-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>
  </tbody>

  <tbody role="rowgroup">
    <tr class="pf-c-table__control-row" role="row">
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Repositories"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;
            2
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Branches"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
            82
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Pull requests"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
            1
          </span>
        </button>
      </td>
      <th role="columnheader" data-label="Workspaces">
        <a href="#">siemur/test-space</a>
      </th>
      <td role="cell" data-label="Last commit">
        <span>1 day ago</span>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Open in Github</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compound-expansion-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compound-expansion-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-4"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-4-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-5"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-5-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-6"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-6-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>
  </tbody>

  <tbody role="rowgroup">
    <tr class="pf-c-table__control-row" role="row">
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Repositories"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;
            4
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Branches"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
            4
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Pull requests"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
            1
          </span>
        </button>
      </td>
      <th role="columnheader" data-label="Workspaces">
        <a href="#">siemur/test-space</a>
      </th>
      <td role="cell" data-label="Last commit">
        <span>2 days ago</span>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Open in Github</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compound-expansion-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compound-expansion-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-7"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-7-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-8"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-8-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="table-compound-expansion-nested-table-9"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="table-compound-expansion-nested-table-9-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Compound expansion accessibility

| Attribute                                             | Applied to                                                | Outcome                                                                       |
| ----------------------------------------------------- | --------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `hidden`                                              | `.pf-c-table__expandable-row`                             | Indicates that the expandable content is hidden. **Required**                 |
| `aria-expanded="true"`                                | `.pf-c-table__compound-expansion-toggle` > `.pf-c-button` | Indicates that the row is visible. **Required**                               |
| `aria-controls="[id of element the button controls]"` | `.pf-c-table__compound-expansion-toggle` > `.pf-c-button` | Identifies the expanded content controlled by the toggle button. **Required** |

### Compound expansion usage

| Class                                    | Applied to                                                           | Outcome                                            |
| ---------------------------------------- | -------------------------------------------------------------------- | -------------------------------------------------- |
| `.pf-c-table__control-row`               | `.pf-c-table__expandable > <tr>`                                     | Modifies a compound expandable table control row.  |
| `.pf-m-expanded`                         | `<tbody>`, `.pf-c-table__compound-expansion-toggle` > `.pf-c-button` | Modifies a tbody with a row and an expandable row. |
| `.pf-c-table__compound-expansion-toggle` | `<td>`                                                               | Modifies a `<td>` on active/focus.                 |

## Compact variant

### Compact example

```html
<table
  class="pf-c-table pf-m-compact pf-m-grid-md"
  role="grid"
  aria-label="This is a compact table example"
  id="table-compact"
>
  <thead>
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-check-all"
          aria-label="Select all rows"
        />
      </td>
      <th role="columnheader" scope="col">Contributor</th>
      <th role="columnheader" scope="col">Position</th>
      <th role="columnheader" scope="col">Location</th>
      <th role="columnheader" scope="col">Last seen</th>
      <th role="columnheader" scope="col">Numbers</th>
      <th class="pf-c-table__icon" role="columnheader" scope="col">Icons</th>
      <td></td>
      <td></td>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-checkrow1"
          aria-labelledby="table-compact-name1"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="table-compact-name1">Sam Jones</span>
      </th>
      <td role="cell" data-label="Position">CSS guru</td>
      <td role="cell" data-label="Location">Not too sure</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">0556</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-checkrow2"
          aria-labelledby="table-compact-name2"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="table-compact-name2">Amy Miller</span>
      </th>
      <td role="cell" data-label="Position">Visual design</td>
      <td role="cell" data-label="Location">Raleigh</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">9492</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-checkrow3"
          aria-labelledby="table-compact-name3"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="table-compact-name3">Steve Wilson</span>
      </th>
      <td role="cell" data-label="Position">Visual design lead</td>
      <td role="cell" data-label="Location">Westford</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">9929</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-checkrow4"
          aria-labelledby="table-compact-name4"
        />
      </td>
      <th role="columnheader" data-label="Contributor name">
        <span id="table-compact-name4">Emma Jackson</span>
      </th>
      <td role="cell" data-label="Position">Interaction design</td>
      <td role="cell" data-label="Location">Westford</td>
      <td role="cell" data-label="Workspaces">May 9, 2018</td>
      <td role="cell" data-label="Last commit">2217</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-dropdown-kebab-4-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Compact expandable example

```html
<table
  class="pf-c-table pf-m-expandable pf-m-compact pf-m-grid-md"
  role="grid"
  aria-label="Compact expandable table example"
  id="table-compact-expandable"
>
  <thead>
    <tr role="row">
      <td></td>
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-check-all"
          aria-label="Select all rows"
        />
      </td>
      <th class="pf-m-width-30" role="columnheader" scope="col">Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <td></td>
      <td></td>
    </tr>
  </thead>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-compact-expandable-node1 table-compact-expandable-expandable-toggle1"
          id="table-compact-expandable-expandable-toggle1"
          aria-label="Details"
          aria-controls="table-compact-expandable-content1"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow1"
          aria-labelledby="table-compact-expandable-node1"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <p id="table-compact-expandable-node1">Node 1</p>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td></td>
      <td></td>
      <td role="cell" colspan="4" id="table-compact-expandable-content1">
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
          aria-labelledby="table-compact-expandable-node2 table-compact-expandable-expandable-toggle2"
          id="table-compact-expandable-expandable-toggle2"
          aria-label="Details"
          aria-controls="table-compact-expandable-content2"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow2"
          aria-labelledby="table-compact-expandable-node2"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <p id="table-compact-expandable-node2">Node 2</p>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td
        class="pf-m-no-padding"
        role="cell"
        colspan="7"
        id="table-compact-expandable-content2"
      >
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-compact-expandable-node3 table-compact-expandable-expandable-toggle3"
          id="table-compact-expandable-expandable-toggle3"
          aria-label="Details"
          aria-controls="table-compact-expandable-content3"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow3"
          aria-labelledby="table-compact-expandable-node3"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <p id="table-compact-expandable-node3">Node 3</p>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td role="cell" colspan="7" id="table-compact-expandable-content3">
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-compact-expandable-node4 table-compact-expandable-expandable-toggle4"
          id="table-compact-expandable-expandable-toggle4"
          aria-label="Details"
          aria-controls="table-compact-expandable-content4"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow4"
          aria-labelledby="table-compact-expandable-node4"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <p id="table-compact-expandable-node4">Node 4</p>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-4-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td
        class="pf-m-no-padding"
        role="cell"
        colspan="7"
        id="table-compact-expandable-content4"
      >
        <div
          class="pf-c-table__expandable-row-content"
        >This content has no padding.</div>
      </td>
    </tr>
  </tbody>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-compact-expandable-node5 table-compact-expandable-expandable-toggle5"
          id="table-compact-expandable-expandable-toggle5"
          aria-label="Details"
          aria-controls="table-compact-expandable-content5"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow5"
          aria-labelledby="table-compact-expandable-node5"
        />
      </td>
      <td role="cell" data-label="Repository name">
        <p id="table-compact-expandable-node5">Node 5</p>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-5-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td role="cell" colspan="7" id="table-compact-expandable-content5">
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-compact-expandable-node6 table-compact-expandable-expandable-toggle6"
          id="table-compact-expandable-expandable-toggle6"
          aria-label="Details"
          aria-controls="table-compact-expandable-content6"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow6"
          aria-labelledby="table-compact-expandable-node6"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <p id="table-compact-expandable-node6">Node 6</p>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-6-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-6-button"
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
      </td>
    </tr>

    <tr
      class="pf-c-table__expandable-row pf-m-expanded"
      role="row"
      id="table-compact-expandable-content6"
    >
      <td></td>
      <td></td>
      <td role="cell" colspan="2">
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</div>
      </td>

      <td role="cell" colspan="2">
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
          aria-labelledby="table-compact-expandable-node7 table-compact-expandable-expandable-toggle7"
          id="table-compact-expandable-expandable-toggle7"
          aria-label="Details"
          aria-controls="table-compact-expandable-content7"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow7"
          aria-labelledby="table-compact-expandable-node7"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <p id="table-compact-expandable-node7">Node 7</p>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-7-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-7-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td role="cell" colspan="7" id="table-compact-expandable-content7">
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-compact-expandable-node8 table-compact-expandable-expandable-toggle8"
          id="table-compact-expandable-expandable-toggle8"
          aria-label="Details"
          aria-controls="table-compact-expandable-content8"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow8"
          aria-labelledby="table-compact-expandable-node8"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <p id="table-compact-expandable-node8">Node 8</p>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-8-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-8-button"
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
      </td>
    </tr>

    <tr
      class="pf-c-table__expandable-row pf-m-expanded"
      role="row"
      id="table-compact-expandable-content8"
    >
      <td role="cell" colspan="4">
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</div>
      </td>

      <td role="cell" colspan="3">
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
          aria-labelledby="table-compact-expandable-node9 table-compact-expandable-expandable-toggle9"
          id="table-compact-expandable-expandable-toggle9"
          aria-label="Details"
          aria-controls="table-compact-expandable-content9"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-compact-expandable-checkrow9"
          aria-labelledby="table-compact-expandable-node9"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <p id="table-compact-expandable-node9">Node 9</p>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-compact-expandable-dropdown-kebab-9-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-compact-expandable-dropdown-kebab-9-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td role="cell" colspan="7" id="table-compact-expandable-content9">
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>
</table>

```

### Compact Usage

| Class           | Applied to    | Outcome                       |
| --------------- | ------------- | ----------------------------- |
| `.pf-m-compact` | `.pf-c-table` | Modifies for a compact table. |

## Hoverable and selected

### Hoverable and selected example

```html
<table
  class="pf-c-table pf-m-expandable pf-m-grid-lg"
  role="grid"
  aria-label="Expandable table example"
  id="table-expandable-hoverable"
>
  <thead>
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-expandable-hoverable-check-all"
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
            <span class="pf-c-table__text">State</span>
            <span class="pf-c-table__sort-indicator">
              <i class="fas fa-long-arrow-alt-up"></i>
            </span>
          </div>
        </button>
      </th>
      <td></td>
    </tr>
  </thead>
  <tr class="pf-m-hoverable" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-1"
        aria-labelledby="table-expandable-hoverable-node-1"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-1">Hoverable</div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-1-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-1-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable pf-m-selected" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-2"
        aria-labelledby="table-expandable-hoverable-node-2"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-2">
        <b>Selected</b>
      </div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-2-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-2-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-3"
        aria-labelledby="table-expandable-hoverable-node-3"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-3">Hoverable</div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-3-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-3-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-4"
        aria-labelledby="table-expandable-hoverable-node-4"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-4">Hoverable</div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-4-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-4-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable pf-m-selected" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-5"
        aria-labelledby="table-expandable-hoverable-node-5"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-5">
        <b>Selected</b>
      </div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-5-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-5-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable pf-m-selected" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-6"
        aria-labelledby="table-expandable-hoverable-node-6"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-6">
        <b>Selected</b>
      </div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-6-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-6-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable pf-m-selected" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-7"
        aria-labelledby="table-expandable-hoverable-node-7"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-7">
        <b>Selected</b>
      </div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-7-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-7-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-8"
        aria-labelledby="table-expandable-hoverable-node-8"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-8">Hoverable</div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-8-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-8-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-9"
        aria-labelledby="table-expandable-hoverable-node-9"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-9">Hoverable</div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-9-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-9-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable pf-m-selected" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-10"
        aria-labelledby="table-expandable-hoverable-node-10"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-10">
        <b>Selected</b>
      </div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-10-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-10-button"
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
    </td>
  </tr>
  <tr class="pf-m-hoverable" role="row" tabindex="0">
    <td class="pf-c-table__check" role="cell">
      <input
        type="checkbox"
        name="table-expandable-hoverable-checkrow-selectable-11"
        aria-labelledby="table-expandable-hoverable-node-11"
      />
    </td>
    <th role="columnheader" data-label="Repository name">
      <div id="table-expandable-hoverable-node-11">Hoverable</div>
    </th>
    <td class="pf-c-table__action" role="cell">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="table-expandable-hoverable-dropdown-kebab-selectable-11-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="table-expandable-hoverable-dropdown-kebab-selectable-11-button"
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
    </td>
  </tr>
</table>

```

### Expandable, hoverable, and selected example

```html
<table
  class="pf-c-table pf-m-expandable pf-m-grid-lg"
  role="grid"
  aria-label="Expandable table example"
  id="table-tbody-expandable-hoverable"
>
  <thead>
    <tr role="row">
      <td></td>
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-check-all"
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
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-1 table-tbody-expandable-hoverable-expandable-toggle-1"
          id="table-tbody-expandable-hoverable-expandable-toggle-1"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-1"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-1"
          aria-labelledby="table-tbody-expandable-hoverable-node-1"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-1">Hoverable</div>
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
            id="table-tbody-expandable-hoverable-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-1"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable pf-m-selected" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-2 table-tbody-expandable-hoverable-expandable-toggle-2"
          id="table-tbody-expandable-hoverable-expandable-toggle-2"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-2"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-2"
          aria-labelledby="table-tbody-expandable-hoverable-node-2"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-2">
            <i>Selected and not expanded</i>
          </div>
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
            id="table-tbody-expandable-hoverable-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-2"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-3 table-tbody-expandable-hoverable-expandable-toggle-3"
          id="table-tbody-expandable-hoverable-expandable-toggle-3"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-3"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-3"
          aria-labelledby="table-tbody-expandable-hoverable-node-3"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-3">Hoverable</div>
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
            id="table-tbody-expandable-hoverable-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-3"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-4 table-tbody-expandable-hoverable-expandable-toggle-4"
          id="table-tbody-expandable-hoverable-expandable-toggle-4"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-4"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-4"
          aria-labelledby="table-tbody-expandable-hoverable-node-4"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-4">Hoverable</div>
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
            id="table-tbody-expandable-hoverable-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-4-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-4"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable pf-m-selected" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-5 table-tbody-expandable-hoverable-expandable-toggle-5"
          id="table-tbody-expandable-hoverable-expandable-toggle-5"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-5"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-5"
          aria-labelledby="table-tbody-expandable-hoverable-node-5"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-5">
            <i>Selected and not expanded</i>
          </div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 5</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-5-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-5"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable pf-m-selected" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-6 table-tbody-expandable-hoverable-expandable-toggle-6"
          id="table-tbody-expandable-hoverable-expandable-toggle-6"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-6"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-6"
          aria-labelledby="table-tbody-expandable-hoverable-node-6"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-6">
            <i>Selected and not expanded</i>
          </div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 6</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-6-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-6-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-6"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable pf-m-selected" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-7 table-tbody-expandable-hoverable-expandable-toggle-7"
          id="table-tbody-expandable-hoverable-expandable-toggle-7"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-7"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-7"
          aria-labelledby="table-tbody-expandable-hoverable-node-7"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-7">
            <i>Selected and not expanded</i>
          </div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 7</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-7-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-7-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-7"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-8 table-tbody-expandable-hoverable-expandable-toggle-8"
          id="table-tbody-expandable-hoverable-expandable-toggle-8"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-8"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-8"
          aria-labelledby="table-tbody-expandable-hoverable-node-8"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-8">Hoverable</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 8</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-8-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-8-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-8"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-9 table-tbody-expandable-hoverable-expandable-toggle-9"
          id="table-tbody-expandable-hoverable-expandable-toggle-9"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-9"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-9"
          aria-labelledby="table-tbody-expandable-hoverable-node-9"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-9">Hoverable</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 9</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-9-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-9-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-9"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-10 table-tbody-expandable-hoverable-expandable-toggle-10"
          id="table-tbody-expandable-hoverable-expandable-toggle-10"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-10"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-10"
          aria-labelledby="table-tbody-expandable-hoverable-node-10"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-10">Hoverable</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 10</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-10-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-10-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-10"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody
    class="pf-m-hoverable pf-m-selected pf-m-expanded"
    role="rowgroup"
    tabindex="0"
  >
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-tbody-expandable-hoverable-node-11 table-tbody-expandable-hoverable-expandable-toggle-11"
          id="table-tbody-expandable-hoverable-expandable-toggle-11"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-11"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-11"
          aria-labelledby="table-tbody-expandable-hoverable-node-11"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-11">
            <b>Expanded and selected</b>
          </div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 11</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-11-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-11-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-11"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-12 table-tbody-expandable-hoverable-expandable-toggle-12"
          id="table-tbody-expandable-hoverable-expandable-toggle-12"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-12"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-12"
          aria-labelledby="table-tbody-expandable-hoverable-node-12"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-12">Hoverable</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 12</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-12-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-12-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-12"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody
    class="pf-m-hoverable pf-m-selected pf-m-expanded"
    role="rowgroup"
    tabindex="0"
  >
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-tbody-expandable-hoverable-node-13 table-tbody-expandable-hoverable-expandable-toggle-13"
          id="table-tbody-expandable-hoverable-expandable-toggle-13"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-13"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-13"
          aria-labelledby="table-tbody-expandable-hoverable-node-13"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-13">
            <b>Expanded and selected</b>
          </div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 13</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-13-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-13-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-13"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody
    class="pf-m-hoverable pf-m-selected pf-m-expanded"
    role="rowgroup"
    tabindex="0"
  >
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-tbody-expandable-hoverable-node-15 table-tbody-expandable-hoverable-expandable-toggle-15"
          id="table-tbody-expandable-hoverable-expandable-toggle-15"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-15"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-15"
          aria-labelledby="table-tbody-expandable-hoverable-node-15"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-15">
            <b>Expanded and selected</b>
          </div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 15</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-15-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-15-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-15"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable pf-m-expanded" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-tbody-expandable-hoverable-node-14 table-tbody-expandable-hoverable-expandable-toggle-14"
          id="table-tbody-expandable-hoverable-expandable-toggle-14"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-14"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-14"
          aria-labelledby="table-tbody-expandable-hoverable-node-14"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div
            id="table-tbody-expandable-hoverable-node-14"
          >Expanded and not selected</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 14</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-14-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-14-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-14"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-16 table-tbody-expandable-hoverable-expandable-toggle-16"
          id="table-tbody-expandable-hoverable-expandable-toggle-16"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-16"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-16"
          aria-labelledby="table-tbody-expandable-hoverable-node-16"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-16">Hoverable</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 16</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-16-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-16-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-16"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable pf-m-expanded" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="table-tbody-expandable-hoverable-node-17 table-tbody-expandable-hoverable-expandable-toggle-17"
          id="table-tbody-expandable-hoverable-expandable-toggle-17"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-17"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-17"
          aria-labelledby="table-tbody-expandable-hoverable-node-17"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div
            id="table-tbody-expandable-hoverable-node-17"
          >Expanded and not selected</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 17</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-17-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-17-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-17"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-18 table-tbody-expandable-hoverable-expandable-toggle-18"
          id="table-tbody-expandable-hoverable-expandable-toggle-18"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-18"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-18"
          aria-labelledby="table-tbody-expandable-hoverable-node-18"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-18">Hoverable</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 18</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-18-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-18-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-18"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
  <tbody class="pf-m-hoverable" role="rowgroup" tabindex="0">
    <tr class="pf-m-expanded" role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          aria-labelledby="table-tbody-expandable-hoverable-node-19 table-tbody-expandable-hoverable-expandable-toggle-19"
          id="table-tbody-expandable-hoverable-expandable-toggle-19"
          aria-label="Details"
          aria-controls="table-tbody-expandable-hoverable-content-19"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-tbody-expandable-hoverable-checkrow-19"
          aria-labelledby="table-tbody-expandable-hoverable-node-19"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-tbody-expandable-hoverable-node-19">Hoverable</div>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Action">
        <a href="#">Link 19</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-tbody-expandable-hoverable-dropdown-kebab-19-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-tbody-expandable-hoverable-dropdown-kebab-19-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="table-tbody-expandable-hoverable-content-19"
      >
        <div class="pf-c-table__expandable-row-content">Expandable content</div>
      </td>
      <td></td>
    </tr>
  </tbody>
</table>

```

### Hoverable accessibility

| Attribute      | Applied to                         | Outcome                                                                                                  |
| -------------- | ---------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `tabindex="0"` | `.pf-c-table tbody.pf-m-hoverable` | Inserts the hoverable table element into the tab order of the page so that it is focusable. **Required** |

### Hoverable and selected usage

| Class             | Applied to                            | Outcome                                                         |
| ----------------- | ------------------------------------- | --------------------------------------------------------------- |
| `.pf-m-hoverable` | `.pf-c-table tbody`, `.pf-c-table tr` | Modifies a tbody or tr table element to be hoverable.           |
| `.pf-m-selected`  | `.pf-c-table tbody`, `.pf-c-table tr` | Modifies a selectable tbody or tr table element to be selected. |

## Tree table

### Tree table example

```html
<table
  class="pf-c-table pf-m-tree-view pf-m-tree-view-grid-lg"
  role="treegrid"
  aria-label="This is a simple tree table example"
  id="tree-table-basic-example"
>
  <thead>
    <tr>
      <th class="pf-c-table__tree-view-title-header-cell" scope="col">Name</th>
      <th scope="col">Count</th>
      <th scope="col">Size</th>
      <th scope="col">Data Stores</th>
      <td></td>
    </tr>
  </thead>

  <tbody>
    <tr
      class
      aria-level="1"
      aria-setsize="1"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-basic-example-node-1 tree-table-basic-example-expandable-toggle-1"
              id="tree-table-basic-example-expandable-toggle-1"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-1"
            >Level 1 all folders</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-1--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-1-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-1-actions-button"
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
      </td>
    </tr>

    <tr
      class="pf-m-tree-view-details-expanded"
      aria-level="2"
      aria-setsize="5"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="false"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain"
              aria-labelledby="tree-table-basic-example-node-2 tree-table-basic-example-expandable-toggle-2"
              id="tree-table-basic-example-expandable-toggle-2"
              aria-label="Details"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-2"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-expanded="true"
              type="button"
              aria-label="tree-table-basic-example-2--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-2-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-2-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-3"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-3--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-3-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-3-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-4"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-4--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-4-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-4-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-5"
            >Level 2 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-5--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-5-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-5-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="3"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-6"
            >Level 2 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-6--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-6-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-6-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="4"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-basic-example-node-7 tree-table-basic-example-expandable-toggle-7"
              id="tree-table-basic-example-expandable-toggle-7"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-7"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-7--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-7-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-7-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="3"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-8"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-8--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-8-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-8-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-basic-example-node-9 tree-table-basic-example-expandable-toggle-9"
              id="tree-table-basic-example-expandable-toggle-9"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-9"
            >Level 3 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-9--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-9-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-9-actions-button"
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
      </td>
    </tr>
    <tr
      class="pf-m-tree-view-details-expanded"
      aria-level="4"
      aria-setsize="1"
      aria-posinset="1"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-10"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-expanded="true"
              type="button"
              aria-label="tree-table-basic-example-10--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-10-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-10-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-11"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-11--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-11-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-11-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="5"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-basic-example-node-12 tree-table-basic-example-expandable-toggle-12"
              id="tree-table-basic-example-expandable-toggle-12"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-12"
            >Level 3 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-12--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-12-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-12-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="4"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-13"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-13--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-13-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-13-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="4"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-14"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-14--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-14-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-14-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="6"
      tabindex="0"
      aria-expanded="false"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain"
              aria-labelledby="tree-table-basic-example-node-15 tree-table-basic-example-expandable-toggle-15"
              id="tree-table-basic-example-expandable-toggle-15"
              aria-label="Details"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-15"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-15--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-15-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-15-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-16"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-16--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-16-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-16-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-basic-example-node-17"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-basic-example-17--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-basic-example-17-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-basic-example-17-actions-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Tree table with checkboxes example

```html
<table
  class="pf-c-table pf-m-tree-view pf-m-tree-view-checkboxes pf-m-tree-view-grid-lg"
  role="treegrid"
  aria-label="This is a simple tree table, with checkboxes example"
  id="tree-table-with-checkboxes-example"
>
  <thead>
    <tr>
      <th class="pf-c-table__tree-view-title-header-cell" scope="col">Name</th>
      <th scope="col">Count</th>
      <th scope="col">Size</th>
      <th scope="col">Data Stores</th>
    </tr>
  </thead>

  <tbody>
    <tr
      class
      aria-level="1"
      aria-setsize="1"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-with-checkboxes-example-node-1 tree-table-with-checkboxes-example-expandable-toggle-1"
              id="tree-table-with-checkboxes-example-expandable-toggle-1"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-1"
              aria-labelledby="tree-table-with-checkboxes-example-node-1"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-1"
            >Level 1 all folders</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-1--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-1-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-1-actions-button"
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
      </td>
    </tr>

    <tr
      class="pf-m-tree-view-details-expanded"
      aria-level="2"
      aria-setsize="5"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="false"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain"
              aria-labelledby="tree-table-with-checkboxes-example-node-2 tree-table-with-checkboxes-example-expandable-toggle-2"
              id="tree-table-with-checkboxes-example-expandable-toggle-2"
              aria-label="Details"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-2"
              aria-labelledby="tree-table-with-checkboxes-example-node-2"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-2"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-expanded="true"
              type="button"
              aria-label="tree-table-with-checkboxes-example-2--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-2-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-2-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-3"
              aria-labelledby="tree-table-with-checkboxes-example-node-3"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-3"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-3--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-3-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-3-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-4"
              aria-labelledby="tree-table-with-checkboxes-example-node-4"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-4"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-4--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-4-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-4-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-5"
              aria-labelledby="tree-table-with-checkboxes-example-node-5"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-5"
            >Level 2 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-5--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-5-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-5-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="3"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-6"
              aria-labelledby="tree-table-with-checkboxes-example-node-6"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-6"
            >Level 2 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-6--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-6-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-6-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="4"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-with-checkboxes-example-node-7 tree-table-with-checkboxes-example-expandable-toggle-7"
              id="tree-table-with-checkboxes-example-expandable-toggle-7"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-7"
              aria-labelledby="tree-table-with-checkboxes-example-node-7"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-7"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-7--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-7-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-7-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="3"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-8"
              aria-labelledby="tree-table-with-checkboxes-example-node-8"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-8"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-8--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-8-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-8-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-with-checkboxes-example-node-9 tree-table-with-checkboxes-example-expandable-toggle-9"
              id="tree-table-with-checkboxes-example-expandable-toggle-9"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-9"
              aria-labelledby="tree-table-with-checkboxes-example-node-9"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-9"
            >Level 3 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-9--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-9-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-9-actions-button"
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
      </td>
    </tr>
    <tr
      class="pf-m-tree-view-details-expanded"
      aria-level="4"
      aria-setsize="1"
      aria-posinset="1"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-10"
              aria-labelledby="tree-table-with-checkboxes-example-node-10"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-10"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-expanded="true"
              type="button"
              aria-label="tree-table-with-checkboxes-example-10--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-10-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-10-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-11"
              aria-labelledby="tree-table-with-checkboxes-example-node-11"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-11"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-11--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-11-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-11-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="5"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-with-checkboxes-example-node-12 tree-table-with-checkboxes-example-expandable-toggle-12"
              id="tree-table-with-checkboxes-example-expandable-toggle-12"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-12"
              aria-labelledby="tree-table-with-checkboxes-example-node-12"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-12"
            >Level 3 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-12--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-12-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-12-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="4"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-13"
              aria-labelledby="tree-table-with-checkboxes-example-node-13"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-13"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-13--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-13-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-13-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="4"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-14"
              aria-labelledby="tree-table-with-checkboxes-example-node-14"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-14"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-14--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-14-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-14-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="6"
      tabindex="0"
      aria-expanded="false"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain"
              aria-labelledby="tree-table-with-checkboxes-example-node-15 tree-table-with-checkboxes-example-expandable-toggle-15"
              id="tree-table-with-checkboxes-example-expandable-toggle-15"
              aria-label="Details"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-15"
              aria-labelledby="tree-table-with-checkboxes-example-node-15"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-15"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-15--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-15-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-15-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-16"
              aria-labelledby="tree-table-with-checkboxes-example-node-16"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-16"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-16--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-16-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-16-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-example-checkrow-17"
              aria-labelledby="tree-table-with-checkboxes-example-node-17"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-example-node-17"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-example-17--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-example-17-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-example-17-actions-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Tree table with checkboxes and icons example

```html
<table
  class="pf-c-table pf-m-tree-view pf-m-tree-view-checkboxes pf-m-tree-view-grid-lg"
  role="treegrid"
  aria-label="This is a simple tree table, with checkboxes and icons example"
  id="tree-table-with-checkboxes-icons-example"
>
  <thead>
    <tr>
      <th class="pf-c-table__tree-view-title-header-cell" scope="col">Name</th>
      <th scope="col">Count</th>
      <th scope="col">Size</th>
      <th scope="col">Data Stores</th>
    </tr>
  </thead>

  <tbody>
    <tr
      class
      aria-level="1"
      aria-setsize="1"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-1 tree-table-with-checkboxes-icons-example-expandable-toggle-1"
              id="tree-table-with-checkboxes-icons-example-expandable-toggle-1"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-1"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-1"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-folder" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-1"
            >Level 1 all folders</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-1--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-1-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-1-actions-button"
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
      </td>
    </tr>

    <tr
      class="pf-m-tree-view-details-expanded"
      aria-level="2"
      aria-setsize="5"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="false"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-2 tree-table-with-checkboxes-icons-example-expandable-toggle-2"
              id="tree-table-with-checkboxes-icons-example-expandable-toggle-2"
              aria-label="Details"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-2"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-2"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-folder" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-2"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-expanded="true"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-2--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-2-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-2-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-3"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-3"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-3"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-3--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-3-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-3-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-4"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-4"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-4"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-4--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-4-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-4-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-5"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-5"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-5"
            >Level 2 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-5--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-5-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-5-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="3"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-6"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-6"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-6"
            >Level 2 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-6--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-6-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-6-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="4"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-7 tree-table-with-checkboxes-icons-example-expandable-toggle-7"
              id="tree-table-with-checkboxes-icons-example-expandable-toggle-7"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-7"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-7"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-folder" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-7"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-7--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-7-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-7-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="3"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-8"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-8"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-8"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-8--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-8-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-8-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-9 tree-table-with-checkboxes-icons-example-expandable-toggle-9"
              id="tree-table-with-checkboxes-icons-example-expandable-toggle-9"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-9"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-9"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-folder" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-9"
            >Level 3 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-9--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-9-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-9-actions-button"
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
      </td>
    </tr>
    <tr
      class="pf-m-tree-view-details-expanded"
      aria-level="4"
      aria-setsize="1"
      aria-posinset="1"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-10"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-10"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-10"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-expanded="true"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-10--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-10-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-10-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-11"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-11"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-11"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-11--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-11-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-11-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="5"
      tabindex="0"
      aria-expanded="true"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain pf-m-expanded"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-12 tree-table-with-checkboxes-icons-example-expandable-toggle-12"
              id="tree-table-with-checkboxes-icons-example-expandable-toggle-12"
              aria-label="Details"
              aria-expanded="true"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-12"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-12"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-folder" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-12"
            >Level 3 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-12--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-12-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-12-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="4"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-13"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-13"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-13"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-13--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-13-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-13-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="4"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-14"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-14"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-14"
            >Level 4 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-14--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-14-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-14-actions-button"
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
      </td>
    </tr>

    <tr
      class
      aria-level="2"
      aria-setsize="5"
      aria-posinset="6"
      tabindex="0"
      aria-expanded="false"
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__toggle">
            <button
              class="pf-c-button pf-m-plain"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-15 tree-table-with-checkboxes-icons-example-expandable-toggle-15"
              id="tree-table-with-checkboxes-icons-example-expandable-toggle-15"
              aria-label="Details"
            >
              <div class="pf-c-table__toggle-icon">
                <i class="fas fa-angle-down" aria-hidden="true"></i>
              </div>
            </button>
          </span>
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-15"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-15"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-folder" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-15"
            >Level 2 node</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-15--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-15-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-15-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="1"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-16"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-16"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-16"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-16--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-16-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-16-actions-button"
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
      </td>
    </tr>
    <tr
      class
      aria-level="3"
      aria-setsize="2"
      aria-posinset="2"
      tabindex="-1"
      hidden
      role="row"
    >
      <th class="pf-c-table__tree-view-title-cell">
        <div class="pf-c-table__tree-view-main">
          <span class="pf-c-table__check">
            <input
              type="checkbox"
              name="tree-table-with-checkboxes-icons-example-checkrow-17"
              aria-labelledby="tree-table-with-checkboxes-icons-example-node-17"
            />
          </span>
          <div class="pf-c-table__tree-view-text">
            <span class="pf-c-table__tree-view-icon">
              <i class="fas fa-fw fa-leaf" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-table__text"
              id="tree-table-with-checkboxes-icons-example-node-17"
            >Level 3 leaf</span>
          </div>
          <span class="pf-c-table__tree-view-details-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="tree-table-with-checkboxes-icons-example-17--tree-table--details-toggle"
            >
              <span class="pf-c-table__details-toggle-icon">
                <i class="fas fa-ellipsis-h" aria-hidden="true"></i>
              </span>
            </button>
          </span>
        </div>
      </th>
      <td role="gridcell" data-label="Migration assessment">10</td>
      <td role="gridcell" data-label="Size of VM">25</td>
      <td role="gridcell" data-label="Number of Data Stores">5</td>
      <td class="pf-c-table__action" role="gridcell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="tree-table-with-checkboxes-icons-example-17-actions-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="tree-table-with-checkboxes-icons-example-17-actions-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Tree table accessibility

| Attribute                | Applied to                                    | Outcome                                                                                                                                                                                                                             |
| ------------------------ | --------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="treegrid"`        | `.pf-c-table.pf-m-tree-view`                  | Identifies the `table` as a treegrid. **Place on the outermost `table` only**                                                                                                                                                       |
| `role="row"`             | `.pf-c-table.pf-m-tree-view tr`               | Identifies the `tr` element as a `row`. The row role is not an implicit semantic for the tr element when in a treegrid.                                                                                                             |
| `role="gridcell"`        | `.pf-c-table.pf-m-tree-view tr`               | Identifies the `td` as a gridcell. The `gridcell` role is not an implicit semantic for the td element when in a treegrid.                                                                                                           |
| `tabindex="-1"`          | `.pf-c-table.pf-m-tree-view tr`               | Makes the element with the treeitem role focusable without including it in the tab sequence of the page.                                                                                                                            |
| `tabindex="0"`           | `.pf-c-table.pf-m-tree-view tr`               | Includes the element with the treeitem role in the tab sequence. Only one treeitem in the tree has tabindex="0". When the user moves focus in the tree, the element included in the tab sequence changes to the element with focus. |
| `aria-expanded="false"`  | `.pf-c-table.pf-m-tree-view tr`               | For an expandable item, indicates the parent node is closed, i.e., the descendant elements are not visible.                                                                                                                         |
| `aria-expanded="true"`   | `.pf-c-table.pf-m-tree-view tr.pf-m-expanded` | Indicates the parent node is open, i.e., the descendant elements are visible.                                                                                                                                                       |
| `aria-level="number"`    | `.pf-c-table.pf-m-tree-view tr`               | Defines the level of the row in the hierarchical treegrid structure. Counting is one-based. Root rows have aria-level=“1”.                                                                                                          |
| `aria-setsize="number"`  | `.pf-c-table.pf-m-tree-view tr`               | Defines the number of rows in the set of rows that are in the same branch and at the same level within the hierarchy.                                                                                                               |
| `aria-posinset="number"` | `.pf-c-table.pf-m-tree-view tr`               | Defines the position of the row within the set of other rows that are in the same branch and at the same level within the hierarchy. Counting is one-based, not zero-based.                                                         |

### Tree table usage

| Class                                        | Applied  | Outcome                                                                 |
| -------------------------------------------- | -------- | ----------------------------------------------------------------------- |
| `.pf-c-table__tree-view-main`                | `<div>`  | Initiates a tree view table main container. **Required with tree view** |
| `.pf-c-table__tree-view-text`                | `<div>`  | Initiates a tree view table text element. **Required with tree view**   |
| `.pf-c-table__tree-view-icon`                | `<span>` | Initiates a tree view icon wrapper. **Required with tree view**         |
| `.pf-c-table__tree-view-title-header-cell`   | `<th>`   | Initiates a tree view title header cell. **Required with tree view**    |
| `.pf-c-table__tree-view-details-toggle`      | `<span>` | Initiates a tree view details toggle container.                         |
| `.pf-c-table__tree-view-details-toggle-icon` | `<span>` | Initiates a tree view details toggle icon.                              |
| `.pf-m-treeview-details-expanded`            | `<tr>`   | Modifies a tbody with a row and an expandable row.                      |

## Borderless variant

### Borderless example

```html
<table
  class="pf-c-table pf-m-grid-md pf-m-no-border-rows"
  role="grid"
  aria-label="This is a compact table with border rows example"
  id="borderless-table"
>
  <thead>
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-check-all"
          aria-label="Select all rows"
        />
      </td>
      <th role="columnheader" scope="col">Contributor</th>
      <th role="columnheader" scope="col">Position</th>
      <th role="columnheader" scope="col">Location</th>
      <th role="columnheader" scope="col">Last seen</th>
      <th role="columnheader" scope="col">Numbers</th>
      <th class="pf-c-table__icon" role="columnheader" scope="col">Icons</th>
      <td></td>
      <td></td>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-checkrow1"
          aria-labelledby="borderless-table-name1"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="borderless-table-name1">Sam Jones</span>
      </th>
      <td role="cell" data-label="Position">CSS guru</td>
      <td role="cell" data-label="Location">Not too sure</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">0556</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-table-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-table-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-checkrow2"
          aria-labelledby="borderless-table-name2"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="borderless-table-name2">Amy Miller</span>
      </th>
      <td role="cell" data-label="Position">Visual design</td>
      <td role="cell" data-label="Location">Raleigh</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">9492</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-table-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-table-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-checkrow3"
          aria-labelledby="borderless-table-name3"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="borderless-table-name3">Steve Wilson</span>
      </th>
      <td role="cell" data-label="Position">Visual design lead</td>
      <td role="cell" data-label="Location">Westford</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">9929</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-table-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-table-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-checkrow4"
          aria-labelledby="borderless-table-name4"
        />
      </td>
      <th role="columnheader" data-label="Contributor name">
        <span id="borderless-table-name4">Emma Jackson</span>
      </th>
      <td role="cell" data-label="Position">Interaction design</td>
      <td role="cell" data-label="Location">Westford</td>
      <td role="cell" data-label="Workspaces">May 9, 2018</td>
      <td role="cell" data-label="Last commit">2217</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-table-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-table-dropdown-kebab-4-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Borderless compact example

```html
<table
  class="pf-c-table pf-m-compact pf-m-grid-md pf-m-no-border-rows"
  role="grid"
  aria-label="This is a compact table with border rows example"
  id="borderless-compact-table"
>
  <thead>
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-compact-table-check-all"
          aria-label="Select all rows"
        />
      </td>
      <th role="columnheader" scope="col">Contributor</th>
      <th role="columnheader" scope="col">Position</th>
      <th role="columnheader" scope="col">Location</th>
      <th role="columnheader" scope="col">Last seen</th>
      <th role="columnheader" scope="col">Numbers</th>
      <th class="pf-c-table__icon" role="columnheader" scope="col">Icons</th>
      <td></td>
      <td></td>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-compact-table-checkrow1"
          aria-labelledby="borderless-compact-table-name1"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="borderless-compact-table-name1">Sam Jones</span>
      </th>
      <td role="cell" data-label="Position">CSS guru</td>
      <td role="cell" data-label="Location">Not too sure</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">0556</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-compact-table-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-compact-table-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-compact-table-checkrow2"
          aria-labelledby="borderless-compact-table-name2"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="borderless-compact-table-name2">Amy Miller</span>
      </th>
      <td role="cell" data-label="Position">Visual design</td>
      <td role="cell" data-label="Location">Raleigh</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">9492</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-compact-table-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-compact-table-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-compact-table-checkrow3"
          aria-labelledby="borderless-compact-table-name3"
        />
      </td>
      <th role="columnheader" data-label="Contributor">
        <span id="borderless-compact-table-name3">Steve Wilson</span>
      </th>
      <td role="cell" data-label="Position">Visual design lead</td>
      <td role="cell" data-label="Location">Westford</td>
      <td role="cell" data-label="Last seen">May 9, 2018</td>
      <td role="cell" data-label="Numbers">9929</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-compact-table-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-compact-table-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-compact-table-checkrow4"
          aria-labelledby="borderless-compact-table-name4"
        />
      </td>
      <th role="columnheader" data-label="Contributor name">
        <span id="borderless-compact-table-name4">Emma Jackson</span>
      </th>
      <td role="cell" data-label="Position">Interaction design</td>
      <td role="cell" data-label="Location">Westford</td>
      <td role="cell" data-label="Workspaces">May 9, 2018</td>
      <td role="cell" data-label="Last commit">2217</td>
      <td class="pf-c-table__icon" role="cell" data-label="Icon">
        <i class="fas fa-check"></i>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Action link</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-compact-table-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-compact-table-dropdown-kebab-4-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Borderless expandable example

```html
<table
  class="pf-c-table pf-m-expandable pf-m-grid-lg pf-m-no-border-rows"
  role="grid"
  aria-label="Expandable table example"
  id="borderless-table-expandable"
>
  <thead>
    <tr role="row">
      <td></td>
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-expandable-check-all"
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

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="borderless-table-expandable-node1 borderless-table-expandable-expandable-toggle1"
          id="borderless-table-expandable-expandable-toggle1"
          aria-label="Details"
          aria-controls="borderless-table-expandable-content1"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-expandable-checkrow1"
          aria-labelledby="borderless-table-expandable-node1"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="borderless-table-expandable-node1">Node 1</div>
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
            id="borderless-table-expandable-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-table-expandable-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td></td>
      <td></td>
      <td
        class
        role="cell"
        colspan="4"
        id="borderless-table-expandable-content1"
      >
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
          aria-labelledby="borderless-table-expandable-node2 borderless-table-expandable-expandable-toggle2"
          id="borderless-table-expandable-expandable-toggle2"
          aria-label="Details"
          aria-controls="borderless-table-expandable-content2"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-expandable-checkrow2"
          aria-labelledby="borderless-table-expandable-node2"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="borderless-table-expandable-node2">Node 2</div>
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
            id="borderless-table-expandable-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-table-expandable-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td
        class
        role="cell"
        colspan="7"
        id="borderless-table-expandable-content2"
      >
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="borderless-table-expandable-node3 borderless-table-expandable-expandable-toggle3"
          id="borderless-table-expandable-expandable-toggle3"
          aria-label="Details"
          aria-controls="borderless-table-expandable-content3"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-expandable-checkrow3"
          aria-labelledby="borderless-table-expandable-node3"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="borderless-table-expandable-node3">Node 3</div>
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
            id="borderless-table-expandable-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-table-expandable-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td
        class
        role="cell"
        colspan="7"
        id="borderless-table-expandable-content3"
      >
        <div
          class="pf-c-table__expandable-row-content"
        >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__toggle" role="cell">
        <button
          class="pf-c-button pf-m-plain pf-m-expanded"
          aria-labelledby="borderless-table-expandable-node4 borderless-table-expandable-expandable-toggle4"
          id="borderless-table-expandable-expandable-toggle4"
          aria-label="Details"
          aria-controls="borderless-table-expandable-content4"
          aria-expanded="true"
        >
          <div class="pf-c-table__toggle-icon">
            <i class="fas fa-angle-down" aria-hidden="true"></i>
          </div>
        </button>
      </td>

      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="borderless-table-expandable-checkrow4"
          aria-labelledby="borderless-table-expandable-node4"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="borderless-table-expandable-node4">Node 4</div>
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
            id="borderless-table-expandable-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-table-expandable-dropdown-kebab-4-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td
        class="pf-m-no-padding"
        role="cell"
        colspan="7"
        id="borderless-table-expandable-content4"
      >
        <div
          class="pf-c-table__expandable-row-content"
        >Expandable row content has no padding.</div>
      </td>
    </tr>
  </tbody>
</table>

```

### Borderless with compound expansion example

```html
<table
  class="pf-c-table pf-m-expandable pf-m-grid-md pf-m-no-border-rows"
  role="grid"
  aria-label="Compound expandable table example"
  id="borderless-compound-expansion-table"
>
  <thead>
    <tr role="row">
      <th
        class="pf-c-table__sort"
        role="columnheader"
        aria-sort="none"
        scope="col"
      >
        <button class="pf-c-table__button">
          <div class="pf-c-table__button-content">
            <span class="pf-c-table__text">Repositories</span>
            <span class="pf-c-table__sort-indicator">
              <i class="fas fa-arrows-alt-v"></i>
            </span>
          </div>
        </button>
      </th>
      <th
        class="pf-c-table__sort pf-m-selected"
        role="columnheader"
        aria-sort="ascending"
        scope="col"
      >
        <button class="pf-c-table__button">
          <div class="pf-c-table__button-content">
            <span class="pf-c-table__text">Branches</span>
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
            <span class="pf-c-table__text">Pull requests</span>
            <span class="pf-c-table__sort-indicator">
              <i class="fas fa-arrows-alt-v"></i>
            </span>
          </div>
        </button>
      </th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
      <td></td>
      <td></td>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr class="pf-c-table__control-row" role="row">
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Repositories"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;10
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Branches"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
            234
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Pull requests"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
            4
          </span>
        </button>
      </td>
      <th role="columnheader" data-label="Workspaces">
        <a href="#">siemur/test-space</a>
      </th>
      <td role="cell" data-label="Last commit">
        <span>20 minutes</span>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Open in Github</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-compound-expansion-table-dropdown-kebab-1-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-compound-expansion-table-dropdown-kebab-1-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-1"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-1-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-2"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-2-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-3"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-3-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__control-row" role="row">
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Repositories"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;10
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Branches"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
            234
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Pull requests"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
            4
          </span>
        </button>
      </td>
      <th role="columnheader" data-label="Workspaces">
        <a href="#">siemur/test-space</a>
      </th>
      <td role="cell" data-label="Last commit">
        <span>20 minutes</span>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Open in Github</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-compound-expansion-table-dropdown-kebab-2-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-compound-expansion-table-dropdown-kebab-2-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-4"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-4-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-5"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-5-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-6"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-6-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>
  </tbody>

  <tbody class="pf-m-expanded" role="rowgroup">
    <tr class="pf-c-table__control-row" role="row">
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Repositories"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;
            2
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-togglepf-m-expanded pf-m-expanded"
        role="cell"
        data-label="Branches"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
            82
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Pull requests"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
            1
          </span>
        </button>
      </td>
      <th role="columnheader" data-label="Workspaces">
        <a href="#">siemur/test-space</a>
      </th>
      <td role="cell" data-label="Last commit">
        <span>1 day ago</span>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Open in Github</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-compound-expansion-table-dropdown-kebab-3-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-compound-expansion-table-dropdown-kebab-3-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-7"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-7-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-8"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-8-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-9"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-9-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>
  </tbody>

  <tbody role="rowgroup">
    <tr class="pf-c-table__control-row" role="row">
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Repositories"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;
            4
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Branches"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
            4
          </span>
        </button>
      </td>
      <td
        class="pf-c-table__compound-expansion-toggle"
        role="cell"
        data-label="Pull requests"
      >
        <button class="pf-c-table__button">
          <span class="pf-c-table__text">
            <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
            1
          </span>
        </button>
      </td>
      <th role="columnheader" data-label="Workspaces">
        <a href="#">siemur/test-space</a>
      </th>
      <td role="cell" data-label="Last commit">
        <span>2 days ago</span>
      </td>
      <td role="cell" data-label="Action">
        <a href="#">Open in Github</a>
      </td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="borderless-compound-expansion-table-dropdown-kebab-4-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="borderless-compound-expansion-table-dropdown-kebab-4-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-10"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-10-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-11"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-11-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>

    <tr class="pf-c-table__expandable-row" role="row">
      <td class="pf-m-no-padding" role="cell" colspan="7">
        <table
          class="pf-c-table pf-m-compact pf-m-no-border-rows"
          role="grid"
          id="borderless-compound-expansion-table-nested-table-12"
          aria-label="Nested table"
        >
          <thead>
            <tr role="row">
              <th
                class="pf-c-table__sort"
                role="columnheader"
                aria-sort="none"
                scope="col"
              >
                <button class="pf-c-table__button">
                  <div class="pf-c-table__button-content">
                    <span class="pf-c-table__text">Description</span>
                    <span class="pf-c-table__sort-indicator">
                      <i class="fas fa-arrows-alt-v"></i>
                    </span>
                  </div>
                </button>
              </th>

              <th role="columnheader" scope="col">Date</th>

              <th role="columnheader" scope="col">Status</th>

              <td role="cell"></td>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr role="row">
              <th role="columnheader" data-label="Description">Item one</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr1-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr1-button"
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
              <th role="columnheader" data-label="Description">Item two</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Warning</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr2-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr2-button"
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
              <th role="columnheader" data-label="Description">Item three</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr3-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr3-button"
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
              <th role="columnheader" data-label="Description">Item four</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr4-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr4-button"
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
              <th role="columnheader" data-label="Description">Item five</th>

              <td role="cell" data-label="Date">May 9, 2018</td>

              <td role="cell" data-label="Status">Active</td>

              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr5-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="borderless-compound-expansion-table-nested-table-12-dropdown-kebab-nested-tr5-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Borderless usage

| Class                  | Applied to                 | Outcome                                                                                        |
| ---------------------- | -------------------------- | ---------------------------------------------------------------------------------------------- |
| `.pf-m-no-border-rows` | `.pf-c-table.pf-m-compact` | Modifies to remove borders between rows. **Note: Does not affect `.pf-c-table__control-row`.** |
| `.pf-m-expandable`     | `.pf-c-table.pf-m-compact` | Indicates that the table has expandable rows.                                                  |

## Width modifiers

### Width modifiers examples

```html
<table
  class="pf-c-table pf-m-grid-md"
  role="grid"
  aria-label="This is a width modifier expandable"
  id="table-width-modifiers"
>
  <thead>
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-width-modifiers-check-all"
          aria-label="Check all rows"
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
      <th class="pf-m-fit-content" role="columnheader" scope="col">Workspaces</th>
      <th class="pf-m-fit-content" role="columnheader" scope="col">Last commit</th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-width-modifiers-checkrow1"
          aria-labelledby="table-width-modifiers-node1"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div id="table-width-modifiers-node1">Node 1</div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-width-modifiers-checkrow2"
          aria-labelledby="table-width-modifiers-node2"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-width-modifiers-node2">Node 2</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-width-modifiers-checkrow3"
          aria-labelledby="table-width-modifiers-node3"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-width-modifiers-node3">Node 3</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-width-modifiers-checkrow4"
          aria-labelledby="table-width-modifiers-node4"
        />
      </td>
      <th role="columnheader" data-label="Repository name">
        <div>
          <div id="table-width-modifiers-node4">Node 4</div>
          <a href="#">siemur/test-space</a>
        </div>
      </th>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>
</table>

```

### Width modifiers usage

| Class                                                                 | Applied to     | Outcome                                                                                     |
| --------------------------------------------------------------------- | -------------- | ------------------------------------------------------------------------------------------- |
| `.pf-m-width-[10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, or 90]` | `<th>`, `<td>` | Percentage based modifier for `th` and `td` widths. **Recommended for sortable title cell** |
| `.pf-m-width-max`                                                     | `<th>`, `<td>` | Percentage based modifier for `th` and `td` maximum width.                                  |
| `.pf-m-fit-content`                                                   | `<th>`, `<td>` | Percentage based modifier for `th` and `td` minimum width with no text wrapping.            |

## Hidden/visible breakpoint modifiers

### Hidden/visible breakpoint modifiers example

```html
<table
  class="pf-c-table pf-m-grid-lg"
  role="grid"
  aria-label="Table with hidden and visible modifiers example"
  id="table-hidden-visible"
>
  <thead>
    <tr role="row">
      <th
        class="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg"
        role="columnheader"
        scope="col"
      >Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th
        class="pf-m-hidden-on-md pf-m-visible-on-lg"
        role="columnheader"
        scope="col"
      >Pull requests</th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th
        class="pf-m-hidden pf-m-visible-on-sm"
        role="columnheader"
        scope="col"
      >Last commit</th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td
        class="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg"
        role="cell"
        data-label="Repository name"
      >Visible only on md breakpoint</td>
      <td role="cell" data-label="Branches">10</td>
      <td
        class="pf-m-hidden-on-md pf-m-visible-on-lg"
        role="cell"
        data-label="Pull requests"
      >Hidden only on md breakpoint</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td
        class="pf-m-hidden pf-m-visible-on-sm"
        role="cell"
        data-label="Last commit"
      >Hidden on xs breakpoint</td>
    </tr>

    <tr role="row">
      <td
        class="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg"
        role="cell"
        data-label="Repository name"
      >Repository 2</td>
      <td role="cell" data-label="Branches">10</td>
      <td
        class="pf-m-hidden-on-md pf-m-visible-on-lg"
        role="cell"
        data-label="Pull requests"
      >25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td
        class="pf-m-hidden pf-m-visible-on-sm"
        role="cell"
        data-label="Last commit"
      >2 days ago</td>
    </tr>

    <tr role="row">
      <td
        class="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg"
        role="cell"
        data-label="Repository name"
      >Repository 3</td>
      <td role="cell" data-label="Branches">10</td>
      <td
        class="pf-m-hidden-on-md pf-m-visible-on-lg"
        role="cell"
        data-label="Pull requests"
      >25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td
        class="pf-m-hidden pf-m-visible-on-sm"
        role="cell"
        data-label="Last commit"
      >2 days ago</td>
    </tr>

    <tr role="row">
      <td
        class="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg"
        role="cell"
        data-label="Repository name"
      >Repository 4</td>
      <td role="cell" data-label="Branches">10</td>
      <td
        class="pf-m-hidden-on-md pf-m-visible-on-lg"
        role="cell"
        data-label="Pull requests"
      >25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td
        class="pf-m-hidden pf-m-visible-on-sm"
        role="cell"
        data-label="Last commit"
      >2 days ago</td>
    </tr>
  </tbody>
</table>

```

### Hidden/visible breakpoint modifiers usage

| Class                             | Applied to           | Outcome                                                                                                                                                             |
| --------------------------------- | -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-hidden{-on-[breakpoint]}`  | `.pf-c-table tr > *` | Hides a table cell at a given breakpoint, or hides it at all breakpoints with `.pf-m-hidden`. **Note: Needs to apply to all cells in the column you want to hide.** |
| `.pf-m-visible{-on-[breakpoint]}` | `.pf-c-table tr > *` | Shows a table cell at a given breakpoint.                                                                                                                           |

## Controlling text modifiers

To better control table cell behavior, PatternFly provides a series of modifiers to help contextually control layout. By default, `thead` cells are set to truncate, whereas `tbody` cells are set to wrap. Both `th` and `td` cells use a set of shared css properties mapped to customizable css variable values. Because only the shared css variables are changed by the modifier selector and not the properties, the modifier can be applied to any parent element up until `.pf-c-table` itself [`thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text`].

| Class               | Applied to                                              | Outcome                                                                                                                                                                                                                                                                                                                                                                                                  |
| ------------------- | ------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-wrap`        | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Sets table cell content to wrap. If applied to `thead`, `tbody` or `tr`, then all child cells will be affected. This is the default behavior for <code>tbody</code> cells.                                                                                                                                                                                                                               |
| `.pf-m-truncate`    | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Sets text to truncate based on a minimum width and available space adjacent table cells.  If applied to `thead`, `tbody` or `tr`, then all child cells will be affected. This is the default behavior for <code>thead</code> cells.                                                                                                                                                                      |
| `.pf-m-nowrap`      | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Unsets min/max width and sets whitespace to nowrap.  If applied to `thead`, `tbody` or `tr`, then all child cells will be affected. This is specifically beneficial for cell's whose <code>thead th</code> cells are blank. The following example highlights link text that should display inline. Be careful with this modifier, it will prioritize its cell's content above all other cell's contents. |
| `.pf-m-fit-content` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Fit column width to cell content.  If applied to `thead`, `tbody` or `tr`, then all child cells will be affected.                                                                                                                                                                                                                                                                                        |
| `.pf-m-break-word`  | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Breaks long strings wherever necessary as defined by the table layout. If applied to `thead`, `tbody` or `tr`, then all child cells will be affected.                                                                                                                                                                                                                                                    |

### Controlling text example

```html
<table
  class="pf-c-table pf-m-grid-lg"
  role="grid"
  aria-label="This is a simple table example"
  id="modifiers-without-text-wrapper-example"
>
  <thead>
    <tr role="row">
      <th
        class="pf-m-width-20"
        role="columnheader"
        scope="col"
      >Truncate (width 20%)</th>
      <th role="columnheader" scope="col">Break word</th>
      <th class="pf-m-wrap" role="columnheader" scope="col">
        Wrapping table header text. This
        <code>th</code> text will wrap instead of truncate.
      </th>
      <th class="pf-m-fit-content" role="columnheader" scope="col">Fit content</th>
      <td></td>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td
        class="pf-m-truncate"
        role="cell"
        data-label="Truncating text"
      >This text will truncate instead of wrap in table layout and wrap gracefully in grid layout.</td>
      <td class="pf-m-break-word" role="cell" data-label="Break word">
        <a
          href="#"
        >http://thisisaverylongurlthatneedstobreakusethebreakwordmodifier.org</a>
      </td>
      <td role="cell" data-label="Wrapping">
        <p>
          By default,
          <code>thead</code> cells will truncate and
          <code>tbody</code> cells will wrap. Use
          <code>.pf-m-wrap</code> on a
          <code>th</code> to change its behavior.
        </p>
      </td>
      <td
        class
        role="cell"
        data-label="Fit content"
      >This cell's content will adjust itself to the parent th width. This modifier only affects table layouts.</td>
      <td class="pf-m-nowrap" role="cell" data-label="No wrap">
        <a href="#">No wrap</a>
      </td>
    </tr>
  </tbody>
</table>

```

By default, truncation and wrapping settings do not affect the grid layout, but text will fallback gracefully by passively wrapping long strings. Truncation and wrapping settings will persist with the addition of a `.pf-c-table__text` wrapper on table cell content. In addition to `.pf-c-table__text`, all PatternFly layouts can be used in table cells and contain table text elements.

### Controlling text using the table text element example

```html
<table
  class="pf-c-table pf-m-grid-md"
  role="grid"
  aria-label="This is a simple table example"
  id="table-text-element-example"
>
  <caption>
    This table contains
    <code>.pf-c-table__text</code>&nbsp; examples. The
    <code>.pf-c-table__text</code>&nbsp; element can be using alone or in a nested configuration.
  </caption>
  <thead>
    <tr role="row">
      <th role="columnheader" scope="col">Selector/element</th>
      <th role="columnheader" scope="col">Result</th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <th
        class="pf-m-fit-content"
        role="rowheader"
        data-label="Element"
        scope="row"
      >
        <div class="pf-c-table__text">
          <b>
            <code>th.pf-m-truncate</code>
          </b>
        </div>
      </th>
      <td class="pf-m-truncate" role="cell" data-label="Truncating text">
        <span class="pf-c-table__text">
          This table cell contains a single
          <code>`.pf-c-table__text`</code>&nbsp; wrapper with the parent table cell applying
          <code>`.pf-m-truncate`</code>. The child
          <code>`.pf-c-table__text`</code>&nbsp; element will inherit the modifier settings and apply to the grid layout.
        </span>
      </td>
    </tr>
    <tr role="row">
      <th
        class="pf-m-fit-content"
        role="rowheader"
        data-label="Element"
        scope="row"
      >
        <div class="pf-c-table__text">
          <b>
            <code>.pf-l-stack</code>
          </b>
        </div>
      </th>
      <td role="cell" data-label="Truncating text">
        <div class="pf-l-stack pf-m-gutter">
          <div class="pf-l-stack__item">
            <div class="pf-c-table__text">
              Because
              <code>.pf-m-grid</code>&nbsp; applies a grid layout to
              <code>.pf-c-table</code>, child elements will stack in the grid layout. To prevent this, wrap multiple elements with a div or use a PatternFly layout.
            </div>
          </div>
          <div class="pf-l-stack__item">
            <p class="pf-c-table__text">
              The
              <b>
                <code>.pf-c-table__text</code>&nbsp;element
              </b>&nbsp; can additionally be nested, like in this example. The next
              <code>.pf-c-table__text</code> element has a very long url whose width needs be constrained.
            </p>
          </div>
          <div class="pf-l-stack__item">
            <a
              class="pf-c-table__text pf-m-truncate"
              href="#"
            >http://truncatemodifierappliedtoaverylongurlthatwillforcethetabletobreakluckilywehavethepfctabletextelement.com</a>
          </div>
          <div class="pf-l-stack__item">
            <p class="pf-c-table__text">
              This
              <b>
                <code>.pf-c-table__text</code>&nbsp;element
              </b>&nbsp; applies its own built in grid layout
              <b>
                <code>.pf-m-stack</code>
              </b>&nbsp;as well as a gutter
              <b>
                <code>.pf-m-gutter</code>
              </b>.
            </p>
          </div>
        </div>
      </td>
    </tr>
    <tr role="row">
      <th
        class="pf-m-fit-content"
        role="rowheader"
        data-label="Element"
        scope="row"
      >
        <div class="pf-c-table__text">
          <b>
            <code>.pf-l-flex</code>
          </b>
        </div>
      </th>
      <td role="cell" data-label="Truncating text">
        <div class="pf-l-flex pf-m-column pf-m-row-on-xl">
          <div class="pf-l-flex__item pf-m-flex-1">
            <p
              class="pf-c-table__text"
            >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt.</p>
          </div>
          <div class="pf-l-flex__item pf-m-flex-1">
            <a
              class="pf-c-table__text pf-m-break-word"
              href="#"
            >http://breakwordmodifierappliedtoaverylongurlthatwillforcethetabletobreakluckilywehavethepfctabletextelement.com</a>
          </div>
        </div>
      </td>
    </tr>
    <tr role="row">
      <th
        class="pf-m-fit-content"
        role="rowheader"
        data-label="Element"
        scope="row"
      >
        <div class="pf-c-table__text">
          <b>
            <code>.pf-l-flex</code>
          </b>
        </div>
      </th>
      <td role="cell" data-label="Truncating text">
        <div class="pf-l-flex pf-m-column">
          <div class="pf-l-flex">
            <div class="pf-l-flex__item">
              <i class="fas fa-code-branch" aria-hidden="true"></i>
              &nbsp;5
            </div>
            <div class="pf-l-flex__item">
              <i class="fas fa-code" aria-hidden="true"></i>
              &nbsp;9
            </div>
            <div class="pf-l-flex__item">
              <i class="fas fa-cube" aria-hidden="true"></i>
              &nbsp;2
            </div>
            <div class="pf-l-flex__item">
              <i class="fas fa-check-circle" aria-hidden="true"></i>
              &nbsp;11
            </div>
          </div>
          <div class="pf-l-flex__item">
            <p
              class="pf-c-table__text"
            >This is paragraph that we want to wrap. It doesn't need a modifier and has no extra long strings. Any modifier available for the flex layout can be used here.</p>
          </div>
          <div class="pf-l-flex__item">
            <a
              class="pf-c-table__text pf-m-break-word"
              href="#"
            >http://breakwordmodifierappliedtoaverylongurlthatwillforcethetabletobreakluckilywehavethepfctabletextelement.com</a>
          </div>
        </div>
      </td>
    </tr>
    <tr role="row">
      <th
        class="pf-m-fit-content"
        role="rowheader"
        data-label="Element"
        scope="row"
      >
        <div class="pf-c-table__text">
          <b>
            <code>.pf-l-grid</code>
          </b>
        </div>
      </th>
      <td role="cell" data-label="Truncating text">
        <div class="pf-l-grid pf-m-gutter">
          <div class="pf-l-grid__item pf-m-6-col pf-m-3-col-on-md">Item 1</div>
          <div class="pf-l-grid__item pf-m-6-col pf-m-3-col-on-md">Item 2</div>
          <div class="pf-l-grid__item pf-m-6-col pf-m-3-col-on-md">Item 3</div>
          <div class="pf-l-grid__item pf-m-6-col pf-m-3-col-on-md">Item 4</div>
          <div class="pf-l-grid__item">
            <p
              class="pf-c-table__text"
            >This is paragraph that we want to wrap. It doesn't need a modifier and has no extra long strings. Any modifier available for the flex layout can be used here.</p>
          </div>
          <div class="pf-l-grid__item">
            <a
              class="pf-c-table__text pf-m-truncate"
              href="#"
            >http://breakwordmodifierappliedtoaverylongurlthatwillforcethetabletobreakluckilywehavethepfctabletextelement.com</a>
          </div>
        </div>
      </td>
    </tr>
  </tbody>
</table>

```

### Controlling text modifiers usage

| Class                | Applied to                                              | Outcome                                       |
| -------------------- | ------------------------------------------------------- | --------------------------------------------- |
| `.pf-c-table__text`  | `th > *`, `td > *`                                      | Initiates a table text element.               |
| `.pf-m-truncate`     | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Modifies text to truncate.                    |
| `.pf-m-nowrap`       | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Modifies text to not wrap.                    |
| `.pf-m-wrap`         | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Modifies text to wrap.                        |
| `.pf-m-fit-content`  | `thead`, `tr`, `th`, `.pf-c-table__text`                | Modifies `th` to fit its contents.            |
| `.pf-m-break-word`   | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Modifies text strings to break.               |
| `.pf-m-border-right` | `<th>`, `<td>`                                          | Modifies a table cell to show a right border. |
| `.pf-m-border-left`  | `<th>`, `<td>`                                          | Modifies a table cell to show a left border.  |

## Table header modifiers

### th truncation

Long strings in table cells will push content. Add a width modifier to `thead th` to limit string length or add `.pf-m-truncate` to `tbody td`.

```html
<div class="pf-c-tooltip pf-m-top" role="tooltip">
  <div class="pf-c-tooltip__arrow"></div>

  <div class="pf-c-tooltip__content" id="tooltip-top-content">Pull Requests</div>
</div>
<table
  class="pf-c-table"
  aria-label="This is a simple table example"
  id="th-truncation-example"
>
  <thead>
    <tr>
      <th scope="col">Repositories</th>
      <th scope="col">Branches</th>
      <th scope="col">Pull requests</th>
      <th scope="col">Workspaces</th>
      <th scope="col">Last commit</th>
    </tr>
  </thead>

  <tbody>
    <tr>
      <td
        class
        role="cell"
        data-label="Repository name"
      >Long lines of text will shrink adjacent column widths.</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>

  <tbody>
    <tr>
      <td role="cell" data-label="Repository name">
        This example is not responsive. Adjacent
        <code>tbody</code> cells will shrink as a result of this text being a longer string and adjacent text being shorter in length. Truncation can be overridden in
        <code>th</code> cells with the addition of
        <code>.pf-m-wrap</code>,
        <code>.pf-m-nowrap</code> or
        <code>.pf-m-fit-content</code>.
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>
</table>

```

### Width constrained

```html
<table
  class="pf-c-table pf-m-grid-md"
  role="grid"
  aria-label="This is a simple table example"
  id="width-constrained-example"
>
  <thead>
    <tr role="row">
      <th class="pf-m-width-40" role="columnheader" scope="col">Width 40</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <th
        class="pf-m-fit-content"
        role="columnheader"
        scope="col"
      >Fit content th</th>
      <th role="columnheader" scope="col">Last commit</th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td
        class
        role="cell"
        data-label="Repository name"
      >Since this is a long string of text and the other cells contain short strings (narrower than their table header), we'll need to control width this table header's width. Let's set width to 40%.</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>

  <tbody role="rowgroup">
    <tr role="row">
      <td
        class="pf-m-truncate"
        role="cell"
        data-label="Repository name"
      >This string will truncate in table mode only. Since this is a long string of text and the other cells contain short strings (narrower than their table header), we'll need to control width this table header's width. Let's set width to 40%.</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>
</table>

```

### Sticky header

```html
<table
  class="pf-c-table pf-m-grid-md pf-m-sticky-header"
  role="grid"
  aria-label="This is a table with sticky header cells"
  id="table-sticky-header"
>
  <thead>
    <tr role="row">
      <th role="columnheader" scope="col">Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 1</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 2</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 3</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td role="cell" data-label="Repository name">Repository 4</td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>
</table>

```

### Sticky column

```html
<div class="pf-c-scroll-inner-wrapper">
  <table
    class="pf-c-table"
    role="grid"
    aria-label="This is a scrollable table"
    id="-table"
  >
    <thead>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sort pf-c-table__sticky-column"
          role="columnheader"
          aria-sort="none"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">
                <span class="pf-c-table__text">
                  <div class="pf-l-flex pf-m-nowrap">
                    <span>Fact</span>
                  </div>
                </span>
              </span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-m-nowrap pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          data-label="Example th"
          scope="col"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">
                <span class="pf-c-table__text">
                  <div class="pf-l-flex pf-m-nowrap">
                    <span>State</span>
                  </div>
                </span>
              </span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 3</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 4</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 5</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 6</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 7</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 8</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 9</span>
            </div>
          </span>
        </th>
      </tr>
    </thead>

    <tbody role="rowgroup">
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 1</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 1</td>
        <td role="cell" data-label="Example td">Test cell 1-3</td>
        <td role="cell" data-label="Example td">Test cell 1-4</td>
        <td role="cell" data-label="Example td">Test cell 1-5</td>
        <td role="cell" data-label="Example td">Test cell 1-6</td>
        <td role="cell" data-label="Example td">Test cell 1-7</td>
        <td role="cell" data-label="Example td">Test cell 1-8</td>
        <td role="cell" data-label="Example td">Test cell 1-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 2</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 2</td>
        <td role="cell" data-label="Example td">Test cell 2-3</td>
        <td role="cell" data-label="Example td">Test cell 2-4</td>
        <td role="cell" data-label="Example td">Test cell 2-5</td>
        <td role="cell" data-label="Example td">Test cell 2-6</td>
        <td role="cell" data-label="Example td">Test cell 2-7</td>
        <td role="cell" data-label="Example td">Test cell 2-8</td>
        <td role="cell" data-label="Example td">Test cell 2-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 3</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 3</td>
        <td role="cell" data-label="Example td">Test cell 3-3</td>
        <td role="cell" data-label="Example td">Test cell 3-4</td>
        <td role="cell" data-label="Example td">Test cell 3-5</td>
        <td role="cell" data-label="Example td">Test cell 3-6</td>
        <td role="cell" data-label="Example td">Test cell 3-7</td>
        <td role="cell" data-label="Example td">Test cell 3-8</td>
        <td role="cell" data-label="Example td">Test cell 3-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 4</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 4</td>
        <td role="cell" data-label="Example td">Test cell 4-3</td>
        <td role="cell" data-label="Example td">Test cell 4-4</td>
        <td role="cell" data-label="Example td">Test cell 4-5</td>
        <td role="cell" data-label="Example td">Test cell 4-6</td>
        <td role="cell" data-label="Example td">Test cell 4-7</td>
        <td role="cell" data-label="Example td">Test cell 4-8</td>
        <td role="cell" data-label="Example td">Test cell 4-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 5</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 5</td>
        <td role="cell" data-label="Example td">Test cell 5-3</td>
        <td role="cell" data-label="Example td">Test cell 5-4</td>
        <td role="cell" data-label="Example td">Test cell 5-5</td>
        <td role="cell" data-label="Example td">Test cell 5-6</td>
        <td role="cell" data-label="Example td">Test cell 5-7</td>
        <td role="cell" data-label="Example td">Test cell 5-8</td>
        <td role="cell" data-label="Example td">Test cell 5-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 6</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 6</td>
        <td role="cell" data-label="Example td">Test cell 6-3</td>
        <td role="cell" data-label="Example td">Test cell 6-4</td>
        <td role="cell" data-label="Example td">Test cell 6-5</td>
        <td role="cell" data-label="Example td">Test cell 6-6</td>
        <td role="cell" data-label="Example td">Test cell 6-7</td>
        <td role="cell" data-label="Example td">Test cell 6-8</td>
        <td role="cell" data-label="Example td">Test cell 6-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 7</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 7</td>
        <td role="cell" data-label="Example td">Test cell 7-3</td>
        <td role="cell" data-label="Example td">Test cell 7-4</td>
        <td role="cell" data-label="Example td">Test cell 7-5</td>
        <td role="cell" data-label="Example td">Test cell 7-6</td>
        <td role="cell" data-label="Example td">Test cell 7-7</td>
        <td role="cell" data-label="Example td">Test cell 7-8</td>
        <td role="cell" data-label="Example td">Test cell 7-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 8</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 8</td>
        <td role="cell" data-label="Example td">Test cell 8-3</td>
        <td role="cell" data-label="Example td">Test cell 8-4</td>
        <td role="cell" data-label="Example td">Test cell 8-5</td>
        <td role="cell" data-label="Example td">Test cell 8-6</td>
        <td role="cell" data-label="Example td">Test cell 8-7</td>
        <td role="cell" data-label="Example td">Test cell 8-8</td>
        <td role="cell" data-label="Example td">Test cell 8-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 9</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">State 9</td>
        <td role="cell" data-label="Example td">Test cell 9-3</td>
        <td role="cell" data-label="Example td">Test cell 9-4</td>
        <td role="cell" data-label="Example td">Test cell 9-5</td>
        <td role="cell" data-label="Example td">Test cell 9-6</td>
        <td role="cell" data-label="Example td">Test cell 9-7</td>
        <td role="cell" data-label="Example td">Test cell 9-8</td>
        <td role="cell" data-label="Example td">Test cell 9-9</td>
      </tr>
    </tbody>
  </table>
</div>

```

### Multiple sticky columns

```html
<div class="pf-c-scroll-inner-wrapper">
  <table
    class="pf-c-table"
    role="grid"
    aria-label="This is a scrollable table"
    id="-table"
  >
    <thead>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sort pf-c-table__sticky-column"
          role="columnheader"
          aria-sort="none"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">
                <span class="pf-c-table__text">
                  <div class="pf-l-flex pf-m-nowrap">
                    <span>Fact</span>
                  </div>
                </span>
              </span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sort pf-c-table__sticky-column"
          role="columnheader"
          aria-sort="none"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">
                <span class="pf-c-table__text">
                  <div class="pf-l-flex pf-m-nowrap">
                    <span>State</span>
                  </div>
                </span>
              </span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 3</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 4</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 5</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 6</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 7</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 8</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap"
          role="columnheader"
          data-label="Example th"
          scope="col"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>Header 9</span>
            </div>
          </span>
        </th>
      </tr>
    </thead>

    <tbody role="rowgroup">
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 1</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 1</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 1-3</td>
        <td role="cell" data-label="Example td">Test cell 1-4</td>
        <td role="cell" data-label="Example td">Test cell 1-5</td>
        <td role="cell" data-label="Example td">Test cell 1-6</td>
        <td role="cell" data-label="Example td">Test cell 1-7</td>
        <td role="cell" data-label="Example td">Test cell 1-8</td>
        <td role="cell" data-label="Example td">Test cell 1-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 2</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 2</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 2-3</td>
        <td role="cell" data-label="Example td">Test cell 2-4</td>
        <td role="cell" data-label="Example td">Test cell 2-5</td>
        <td role="cell" data-label="Example td">Test cell 2-6</td>
        <td role="cell" data-label="Example td">Test cell 2-7</td>
        <td role="cell" data-label="Example td">Test cell 2-8</td>
        <td role="cell" data-label="Example td">Test cell 2-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 3</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 3</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 3-3</td>
        <td role="cell" data-label="Example td">Test cell 3-4</td>
        <td role="cell" data-label="Example td">Test cell 3-5</td>
        <td role="cell" data-label="Example td">Test cell 3-6</td>
        <td role="cell" data-label="Example td">Test cell 3-7</td>
        <td role="cell" data-label="Example td">Test cell 3-8</td>
        <td role="cell" data-label="Example td">Test cell 3-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 4</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 4</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 4-3</td>
        <td role="cell" data-label="Example td">Test cell 4-4</td>
        <td role="cell" data-label="Example td">Test cell 4-5</td>
        <td role="cell" data-label="Example td">Test cell 4-6</td>
        <td role="cell" data-label="Example td">Test cell 4-7</td>
        <td role="cell" data-label="Example td">Test cell 4-8</td>
        <td role="cell" data-label="Example td">Test cell 4-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 5</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 5</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 5-3</td>
        <td role="cell" data-label="Example td">Test cell 5-4</td>
        <td role="cell" data-label="Example td">Test cell 5-5</td>
        <td role="cell" data-label="Example td">Test cell 5-6</td>
        <td role="cell" data-label="Example td">Test cell 5-7</td>
        <td role="cell" data-label="Example td">Test cell 5-8</td>
        <td role="cell" data-label="Example td">Test cell 5-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 6</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 6</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 6-3</td>
        <td role="cell" data-label="Example td">Test cell 6-4</td>
        <td role="cell" data-label="Example td">Test cell 6-5</td>
        <td role="cell" data-label="Example td">Test cell 6-6</td>
        <td role="cell" data-label="Example td">Test cell 6-7</td>
        <td role="cell" data-label="Example td">Test cell 6-8</td>
        <td role="cell" data-label="Example td">Test cell 6-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 7</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 7</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 7-3</td>
        <td role="cell" data-label="Example td">Test cell 7-4</td>
        <td role="cell" data-label="Example td">Test cell 7-5</td>
        <td role="cell" data-label="Example td">Test cell 7-6</td>
        <td role="cell" data-label="Example td">Test cell 7-7</td>
        <td role="cell" data-label="Example td">Test cell 7-8</td>
        <td role="cell" data-label="Example td">Test cell 7-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 8</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 8</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 8-3</td>
        <td role="cell" data-label="Example td">Test cell 8-4</td>
        <td role="cell" data-label="Example td">Test cell 8-5</td>
        <td role="cell" data-label="Example td">Test cell 8-6</td>
        <td role="cell" data-label="Example td">Test cell 8-7</td>
        <td role="cell" data-label="Example td">Test cell 8-8</td>
        <td role="cell" data-label="Example td">Test cell 8-9</td>
      </tr>
      <tr role="row">
        <th
          class="pf-m-nowrap pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <span>Fact 9</span>
            </div>
          </span>
        </th>
        <th
          class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
          role="columnheader"
          data-label="Example th"
          scope="col"
          style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
        >
          <span class="pf-c-table__text">
            <div class="pf-l-flex pf-m-nowrap">
              <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
              <span>State 9</span>
            </div>
          </span>
        </th>
        <td role="cell" data-label="Example td">Test cell 9-3</td>
        <td role="cell" data-label="Example td">Test cell 9-4</td>
        <td role="cell" data-label="Example td">Test cell 9-5</td>
        <td role="cell" data-label="Example td">Test cell 9-6</td>
        <td role="cell" data-label="Example td">Test cell 9-7</td>
        <td role="cell" data-label="Example td">Test cell 9-8</td>
        <td role="cell" data-label="Example td">Test cell 9-9</td>
      </tr>
    </tbody>
  </table>
</div>

```

### Sticky columns and header

```html
<div class="pf-c-scroll-outer-wrapper">
  <div class="pf-c-scroll-inner-wrapper">
    <table
      class="pf-c-table pf-m-sticky-header"
      role="grid"
      aria-label="This is a scrollable table"
      id="-table"
    >
      <thead>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sort pf-c-table__sticky-column"
            role="columnheader"
            aria-sort="none"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <button class="pf-c-table__button">
              <div class="pf-c-table__button-content">
                <span class="pf-c-table__text">
                  <span class="pf-c-table__text">
                    <div class="pf-l-flex pf-m-nowrap">
                      <span>Fact</span>
                    </div>
                  </span>
                </span>
                <span class="pf-c-table__sort-indicator">
                  <i class="fas fa-arrows-alt-v"></i>
                </span>
              </div>
            </button>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sort pf-c-table__sticky-column"
            role="columnheader"
            aria-sort="none"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <button class="pf-c-table__button">
              <div class="pf-c-table__button-content">
                <span class="pf-c-table__text">
                  <span class="pf-c-table__text">
                    <div class="pf-l-flex pf-m-nowrap">
                      <span>State</span>
                    </div>
                  </span>
                </span>
                <span class="pf-c-table__sort-indicator">
                  <i class="fas fa-arrows-alt-v"></i>
                </span>
              </div>
            </button>
          </th>
          <th
            class="pf-m-nowrap"
            role="columnheader"
            data-label="Example th"
            scope="col"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>Header 3</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap"
            role="columnheader"
            data-label="Example th"
            scope="col"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>Header 4</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap"
            role="columnheader"
            data-label="Example th"
            scope="col"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>Header 5</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap"
            role="columnheader"
            data-label="Example th"
            scope="col"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>Header 6</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap"
            role="columnheader"
            data-label="Example th"
            scope="col"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>Header 7</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap"
            role="columnheader"
            data-label="Example th"
            scope="col"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>Header 8</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap"
            role="columnheader"
            data-label="Example th"
            scope="col"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>Header 9</span>
              </div>
            </span>
          </th>
        </tr>
      </thead>

      <tbody role="rowgroup">
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 1</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 1</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 1-3</td>
          <td role="cell" data-label="Example td">Test cell 1-4</td>
          <td role="cell" data-label="Example td">Test cell 1-5</td>
          <td role="cell" data-label="Example td">Test cell 1-6</td>
          <td role="cell" data-label="Example td">Test cell 1-7</td>
          <td role="cell" data-label="Example td">Test cell 1-8</td>
          <td role="cell" data-label="Example td">Test cell 1-9</td>
        </tr>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 2</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 2</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 2-3</td>
          <td role="cell" data-label="Example td">Test cell 2-4</td>
          <td role="cell" data-label="Example td">Test cell 2-5</td>
          <td role="cell" data-label="Example td">Test cell 2-6</td>
          <td role="cell" data-label="Example td">Test cell 2-7</td>
          <td role="cell" data-label="Example td">Test cell 2-8</td>
          <td role="cell" data-label="Example td">Test cell 2-9</td>
        </tr>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 3</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 3</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 3-3</td>
          <td role="cell" data-label="Example td">Test cell 3-4</td>
          <td role="cell" data-label="Example td">Test cell 3-5</td>
          <td role="cell" data-label="Example td">Test cell 3-6</td>
          <td role="cell" data-label="Example td">Test cell 3-7</td>
          <td role="cell" data-label="Example td">Test cell 3-8</td>
          <td role="cell" data-label="Example td">Test cell 3-9</td>
        </tr>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 4</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 4</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 4-3</td>
          <td role="cell" data-label="Example td">Test cell 4-4</td>
          <td role="cell" data-label="Example td">Test cell 4-5</td>
          <td role="cell" data-label="Example td">Test cell 4-6</td>
          <td role="cell" data-label="Example td">Test cell 4-7</td>
          <td role="cell" data-label="Example td">Test cell 4-8</td>
          <td role="cell" data-label="Example td">Test cell 4-9</td>
        </tr>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 5</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 5</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 5-3</td>
          <td role="cell" data-label="Example td">Test cell 5-4</td>
          <td role="cell" data-label="Example td">Test cell 5-5</td>
          <td role="cell" data-label="Example td">Test cell 5-6</td>
          <td role="cell" data-label="Example td">Test cell 5-7</td>
          <td role="cell" data-label="Example td">Test cell 5-8</td>
          <td role="cell" data-label="Example td">Test cell 5-9</td>
        </tr>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 6</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 6</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 6-3</td>
          <td role="cell" data-label="Example td">Test cell 6-4</td>
          <td role="cell" data-label="Example td">Test cell 6-5</td>
          <td role="cell" data-label="Example td">Test cell 6-6</td>
          <td role="cell" data-label="Example td">Test cell 6-7</td>
          <td role="cell" data-label="Example td">Test cell 6-8</td>
          <td role="cell" data-label="Example td">Test cell 6-9</td>
        </tr>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 7</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 7</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 7-3</td>
          <td role="cell" data-label="Example td">Test cell 7-4</td>
          <td role="cell" data-label="Example td">Test cell 7-5</td>
          <td role="cell" data-label="Example td">Test cell 7-6</td>
          <td role="cell" data-label="Example td">Test cell 7-7</td>
          <td role="cell" data-label="Example td">Test cell 7-8</td>
          <td role="cell" data-label="Example td">Test cell 7-9</td>
        </tr>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 8</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 8</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 8-3</td>
          <td role="cell" data-label="Example td">Test cell 8-4</td>
          <td role="cell" data-label="Example td">Test cell 8-5</td>
          <td role="cell" data-label="Example td">Test cell 8-6</td>
          <td role="cell" data-label="Example td">Test cell 8-7</td>
          <td role="cell" data-label="Example td">Test cell 8-8</td>
          <td role="cell" data-label="Example td">Test cell 8-9</td>
        </tr>
        <tr role="row">
          <th
            class="pf-m-nowrap pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <span>Fact 9</span>
              </div>
            </span>
          </th>
          <th
            class="pf-m-nowrap pf-m-border-right pf-c-table__sticky-column"
            role="columnheader"
            data-label="Example th"
            scope="col"
            style="--pf-c-table__sticky-column--MinWidth: 80px; --pf-c-table__sticky-column--Left: 100px;"
          >
            <span class="pf-c-table__text">
              <div class="pf-l-flex pf-m-nowrap">
                <i class="pficon fas pf-icon-blueprint" aria-hidden="true"></i>
                <span>State 9</span>
              </div>
            </span>
          </th>
          <td role="cell" data-label="Example td">Test cell 9-3</td>
          <td role="cell" data-label="Example td">Test cell 9-4</td>
          <td role="cell" data-label="Example td">Test cell 9-5</td>
          <td role="cell" data-label="Example td">Test cell 9-6</td>
          <td role="cell" data-label="Example td">Test cell 9-7</td>
          <td role="cell" data-label="Example td">Test cell 9-8</td>
          <td role="cell" data-label="Example td">Test cell 9-9</td>
        </tr>
      </tbody>
    </table>
  </div>
</div>

```

### Sticky column usage

For sticky columns to function correctly, the parent table's width must be controlled with `.pf-c-scroll-inner-wrapper`. For sticky columns and sticky headers to function correctly, the parent table needs an inner and outer wrapper (`.pf-c-scroll-outer-wrapper` and `.pf-c-scroll-inner-wrapper`)

| Class                        | Applied to     | Outcome                                                   |
| ---------------------------- | -------------- | --------------------------------------------------------- |
| `.pf-c-scroll-outer-wrapper` | `<div>`        | Initiates a table container sticky columns outer wrapper. |
| `.pf-c-scroll-inner-wrapper` | `<div>`        | Initiates a table container sticky columns inner wrapper. |
| `.pf-c-table__sticky-column` | `<th>`, `<td>` | Initiates a sticky table cell.                            |

### Nested column headers and expandable rows

```html
<div class="pf-c-scroll-inner-wrapper">
  <table
    class="pf-c-table"
    role="grid"
    aria-label="This is a nested column header table example"
    id="nested-columns-expandable-example"
  >
    <col />
    <col />
    <col />
    <colgroup span="3"></colgroup>
    <col />
    <col />
    <thead class="pf-m-nested-column-header">
      <tr role="row">
        <td rowspan="2"></td>
        <td class="pf-c-table__check" role="cell" rowspan="2">
          <input
            type="checkbox"
            name="nested-columns-expandable-example-check-all"
            aria-label="Select all rows"
          />
        </td>
        <th
          class="pf-m-border-right pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
          rowspan="2"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Team</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-m-border-right"
          role="columnheader"
          scope="col"
          colspan="3"
        >Members</th>
        <th role="columnheader" scope="col" rowspan="2">Contact</th>
        <td rowspan="2"></td>
      </tr>

      <tr class="pf-m-first-cell-offset-reset" role="row">
        <th
          class="pf-c-table__subhead"
          role="columnheader"
          scope="col"
        >Design lead</th>
        <th
          class="pf-c-table__subhead"
          role="columnheader"
          scope="col"
        >Interaction design</th>
        <th
          class="pf-c-table__subhead pf-m-border-right"
          role="columnheader"
          scope="col"
        >Visual designers</th>
      </tr>
    </thead>

    <tbody class="pf-m-expanded" role="rowgroup">
      <tr role="row">
        <td class="pf-c-table__toggle" role="cell">
          <button
            class="pf-c-button pf-m-plain pf-m-expanded"
            aria-labelledby="nested-columns-expandable-example-node1 nested-columns-expandable-example-expandable-toggle1"
            id="nested-columns-expandable-example-expandable-toggle1"
            aria-label="Details"
            aria-controls="nested-columns-expandable-example-content1"
            aria-expanded="true"
          >
            <div class="pf-c-table__toggle-icon">
              <i class="fas fa-angle-down" aria-hidden="true"></i>
            </div>
          </button>
        </td>

        <td class="pf-c-table__check" role="cell">
          <input
            type="checkbox"
            name="nested-columns-expandable-example-checkrow1"
            aria-labelledby="nested-columns-expandable-example-node1"
          />
        </td>
        <th
          class
          role="columnheader"
          data-label="Developer program"
          id="nested-columns-expandable-example-node1"
        >Developer program</th>
        <td role="cell" data-label="Branches">Stacey Logan</td>
        <td role="cell" data-label="Pull requests">Mark Shakshober</td>
        <td role="cell" data-label="Workspaces">Kaliq Ray</td>
        <td role="cell" data-label="Last commit">
          <button
            class="pf-c-button pf-m-inline pf-m-link"
            type="button"
          >Message us!</button>
        </td>
        <td class="pf-c-table__action" role="cell">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="nested-columns-expandable-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="nested-columns-expandable-example-dropdown-kebab-1-button"
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
        </td>
      </tr>
      <tr class="pf-c-table__expandable-row pf-m-expanded" role="row">
        <td></td>
        <td></td>
        <td
          class
          role="cell"
          colspan="5"
          id="nested-columns-expandable-example-content1"
        >
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
            aria-labelledby="nested-columns-expandable-example-node2 nested-columns-expandable-example-expandable-toggle2"
            id="nested-columns-expandable-example-expandable-toggle2"
            aria-label="Details"
            aria-controls="nested-columns-expandable-example-content2"
          >
            <div class="pf-c-table__toggle-icon">
              <i class="fas fa-angle-down" aria-hidden="true"></i>
            </div>
          </button>
        </td>

        <td class="pf-c-table__check" role="cell">
          <input
            type="checkbox"
            name="nested-columns-expandable-example-checkrow2"
            aria-labelledby="nested-columns-expandable-example-node2"
          />
        </td>
        <th
          class
          role="columnheader"
          data-label="Developer program"
          id="nested-columns-expandable-example-node2"
        >Developer program</th>
        <td role="cell" data-label="Branches">Stacey Logan</td>
        <td role="cell" data-label="Pull requests">Mark Shakshober</td>
        <td role="cell" data-label="Workspaces">Kaliq Ray</td>
        <td role="cell" data-label="Last commit">
          <button
            class="pf-c-button pf-m-inline pf-m-link"
            type="button"
          >Message us!</button>
        </td>
        <td class="pf-c-table__action" role="cell">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="nested-columns-expandable-example-dropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="nested-columns-expandable-example-dropdown-kebab-2-button"
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
        </td>
      </tr>
      <tr class="pf-c-table__expandable-row" role="row">
        <td></td>
        <td></td>
        <td
          class
          role="cell"
          colspan="5"
          id="nested-columns-expandable-example-content2"
        >
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
            aria-labelledby="nested-columns-expandable-example-node3 nested-columns-expandable-example-expandable-toggle3"
            id="nested-columns-expandable-example-expandable-toggle3"
            aria-label="Details"
            aria-controls="nested-columns-expandable-example-content3"
          >
            <div class="pf-c-table__toggle-icon">
              <i class="fas fa-angle-down" aria-hidden="true"></i>
            </div>
          </button>
        </td>

        <td class="pf-c-table__check" role="cell">
          <input
            type="checkbox"
            name="nested-columns-expandable-example-checkrow3"
            aria-labelledby="nested-columns-expandable-example-node3"
          />
        </td>
        <th
          class
          role="columnheader"
          data-label="Developer program"
          id="nested-columns-expandable-example-node3"
        >Developer program</th>
        <td role="cell" data-label="Branches">Stacey Logan</td>
        <td role="cell" data-label="Pull requests">Mark Shakshober</td>
        <td role="cell" data-label="Workspaces">Kaliq Ray</td>
        <td role="cell" data-label="Last commit">
          <button
            class="pf-c-button pf-m-inline pf-m-link"
            type="button"
          >Message us!</button>
        </td>
        <td class="pf-c-table__action" role="cell">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="nested-columns-expandable-example-dropdown-kebab-3-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="nested-columns-expandable-example-dropdown-kebab-3-button"
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
        </td>
      </tr>
      <tr class="pf-c-table__expandable-row" role="row">
        <td></td>
        <td></td>
        <td
          class
          role="cell"
          colspan="5"
          id="nested-columns-expandable-example-content3"
        >
          <div
            class="pf-c-table__expandable-row-content"
          >Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>
        </td>
        <td></td>
      </tr>
    </tbody>
  </table>
</div>

```

### Nested column headers

```html
<div class="pf-c-scroll-inner-wrapper">
  <table
    class="pf-c-table"
    role="grid"
    aria-label="This is a nested column header table example"
    id="table-nested-column-headers-example"
  >
    <colgroup span="3"></colgroup>
    <colgroup span="2"></colgroup>
    <col />
    <col />
    <col />
    <col />
    <thead class="pf-m-nested-column-header">
      <tr role="row">
        <th
          class="pf-m-border-right"
          role="columnheader"
          scope="col"
          colspan="3"
        >Pods</th>
        <th
          class="pf-m-border-right"
          role="columnheader"
          scope="col"
          colspan="2"
        >Ports</th>
        <th
          class="pf-m-border-right pf-m-fit-content pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
          rowspan="2"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Protocol</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-m-border-right pf-m-fit-content pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
          rowspan="2"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Flow rate</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-m-border-right pf-m-fit-content pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
          rowspan="2"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Traffic</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-m-fit-content pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
          rowspan="2"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Packets</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
      </tr>

      <tr role="row">
        <th
          class="pf-c-table__subhead pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Source</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-c-table__subhead pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Destination</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-c-table__subhead pf-m-fit-content pf-m-border-right pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Date & Time</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-c-table__subhead pf-m-fit-content pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Source</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
        <th
          class="pf-c-table__subhead pf-m-fit-content pf-m-border-right pf-c-table__sort"
          role="columnheader"
          aria-sort="none"
          scope="col"
        >
          <button class="pf-c-table__button">
            <div class="pf-c-table__button-content">
              <span class="pf-c-table__text">Destination</span>
              <span class="pf-c-table__sort-indicator">
                <i class="fas fa-arrows-alt-v"></i>
              </span>
            </div>
          </button>
        </th>
      </tr>
    </thead>

    <tbody role="rowgroup">
      <tr role="row">
        <td role="cell" data-label="Source">
          <div class="pf-l-flex pf-m-nowrap">
            <div class="pf-l-flex__item">
              <span class="pf-c-label pf-m-cyan">
                <span class="pf-c-label__content">P</span>
              </span>
            </div>
            <div class="pf-l-flex__item pf-m-flex-1">
              <span class="pf-c-table__text pf-m-truncate">
                <a href="#">api-pod-source-name</a>
              </span>
            </div>
          </div>
        </td>
        <td role="cell" data-label="Destination">
          <div class="pf-l-flex pf-m-nowrap">
            <div class="pf-l-flex__item">
              <span class="pf-c-label pf-m-cyan">
                <span class="pf-c-label__content">P</span>
              </span>
            </div>
            <div class="pf-l-flex__item pf-m-flex-1">
              <span class="pf-c-table__text pf-m-truncate">
                <a href="#">api-pod-destination-name</a>
              </span>
            </div>
          </div>
        </td>
        <td role="cell" data-label="Date &amp; time">
          <div class="pf-l-stack">
            <span>June 22, 2021</span>
            <span class="pf-u-color-200">3:58:24 PM</span>
          </div>
        </td>
        <td role="cell" data-label="Source">
          <div class="pf-l-stack">
            <span>443</span>
            <span class="pf-u-color-200">(HTTPS)</span>
          </div>
        </td>
        <td role="cell" data-label="Destination">
          <div class="pf-l-stack">
            <span>24</span>
            <span class="pf-u-color-200">(smtp)</span>
          </div>
        </td>
        <td role="cell" data-label="Protocol">TCP</td>
        <td role="cell" data-label="Flow rate">1.9 Kbps</td>
        <td role="cell" data-label="Traffic">2.1 KB</td>
        <td role="cell" data-label="Packets">3</td>
      </tr>
    </tbody>
  </table>
</div>

```

## Favorites

### Favorites examples

```html
<table
  class="pf-c-table pf-m-grid-md"
  role="grid"
  aria-label="This is a favorites table example"
  id="table-favorites"
>
  <thead>
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-favorites-check-all"
          aria-label="Select all rows"
        />
      </td>
      <td></td>
      <th role="columnheader" scope="col">Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
      <td></td>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-favorites-checkrow1"
          aria-labelledby="table-favorites-node1"
        />
      </td>
      <td class="pf-c-table__favorite pf-m-favorited" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Starred"
          id="table-favorites-favorite-button1"
          aria-labelledby="table-favorites-node1 table-favorites-favorite-button1"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <div>
          <span id="table-favorites-node1">Repository 1</span>. This is a long title that will wrap to multiple lines. This is a long title that will wrap to multiple lines. This is a long title that will wrap to multiple lines. This is a long title that will wrap to multiple lines.
        </div>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-favorites-dropdown-kebab-11-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-favorites-dropdown-kebab-11-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-favorites-checkrow2"
          aria-labelledby="table-favorites-node2"
        />
      </td>
      <td class="pf-c-table__favorite" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Not starred"
          id="table-favorites-favorite-button2"
          aria-labelledby="table-favorites-node2 table-favorites-favorite-button2"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-favorites-node2">Repository 2</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-favorites-dropdown-kebab-22-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-favorites-dropdown-kebab-22-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-favorites-checkrow3"
          aria-labelledby="table-favorites-node3"
        />
      </td>
      <td class="pf-c-table__favorite pf-m-favorited" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Starred"
          id="table-favorites-favorite-button3"
          aria-labelledby="table-favorites-node3 table-favorites-favorite-button3"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-favorites-node3">Repository 3</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-favorites-dropdown-kebab-33-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-favorites-dropdown-kebab-33-button"
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
      </td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__check" role="cell">
        <input
          type="checkbox"
          name="table-favorites-checkrow4"
          aria-labelledby="table-favorites-node4"
        />
      </td>
      <td class="pf-c-table__favorite" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Not starred"
          id="table-favorites-favorite-button4"
          aria-labelledby="table-favorites-node4 table-favorites-favorite-button4"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-favorites-node4">Repository 4</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
      <td class="pf-c-table__action" role="cell">
        <div class="pf-c-dropdown">
          <button
            class="pf-c-dropdown__toggle pf-m-plain"
            id="table-favorites-dropdown-kebab-44-button"
            aria-expanded="false"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
          <ul
            class="pf-c-dropdown__menu pf-m-align-right"
            aria-labelledby="table-favorites-dropdown-kebab-44-button"
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
      </td>
    </tr>
  </tbody>
</table>

```

### Favorites sortable example

```html
<table
  class="pf-c-table pf-m-grid-md"
  role="grid"
  aria-label="This is a sortable with favorites table example"
  id="table-favorites-sortable"
>
  <thead>
    <tr role="row">
      <th
        class="pf-c-table__sort pf-m-selected pf-m-favorite"
        role="columnheader"
        aria-sort="none"
        scope="col"
      >
        <button class="pf-c-table__button" aria-label="Favorite">
          <div class="pf-c-table__button-content">
            <span class="pf-c-table__text">
              <i class="fas fa-star" aria-hidden="true"></i>
            </span>
            <span class="pf-c-table__sort-indicator">
              <i class="fas fa-long-arrow-alt-down"></i>
            </span>
          </div>
        </button>
      </th>
      <th role="columnheader" scope="col">Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__favorite pf-m-favorited" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Starred"
          id="table-favorites-sortable-favorite-button1"
          aria-labelledby="table-favorites-sortable-node1 table-favorites-sortable-favorite-button1"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <div>
          <span id="table-favorites-sortable-node1">Repository 1</span>. This is a long title that will wrap to multiple lines. This is a long title that will wrap to multiple lines. This is a long title that will wrap to multiple lines. This is a long title that will wrap to multiple lines.
        </div>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__favorite pf-m-favorited" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Starred"
          id="table-favorites-sortable-favorite-button3"
          aria-labelledby="table-favorites-sortable-node3 table-favorites-sortable-favorite-button3"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-favorites-sortable-node3">Repository 3</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__favorite" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Not starred"
          id="table-favorites-sortable-favorite-button2"
          aria-labelledby="table-favorites-sortable-node2 table-favorites-sortable-favorite-button2"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-favorites-sortable-node2">Repository 2</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__favorite" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Not starred"
          id="table-favorites-sortable-favorite-button4"
          aria-labelledby="table-favorites-sortable-node4 table-favorites-sortable-favorite-button4"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-favorites-sortable-node4">Repository 4</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__favorite" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Not starred"
          id="table-favorites-sortable-favorite-button5"
          aria-labelledby="table-favorites-sortable-node5 table-favorites-sortable-favorite-button5"
        >
          <i class="fas fa-star" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-favorites-sortable-node5">Repository 5</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>
</table>

```

### Favorites accessibility

| Attribute                       | Applied to    | Outcome                                                                                                                                                   |
| ------------------------------- | ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="grid"`                   | `.pf-c-table` | Identifies the element that serves as the grid widget container. **Required**                                                                             |
| `aria-label`                    | `.pf-c-table` | Provides an accessible name for the table when a descriptive `<caption>` or `<h*>` is not available. **Required in the absence of `<caption>` or `<h*>`** |
| `data-label="[td description]"` | `<td>`        | This attribute replaces table header in mobile viewport. It is rendered by `::before` pseudo element.                                                     |

### Favorites usage

| Class                   | Applied to              | Outcome                                                                |
| ----------------------- | ----------------------- | ---------------------------------------------------------------------- |
| `.pf-c-table__favorite` | `td`                    | Initiates a favorite table body cell.                                  |
| `.pf-m-favorited`       | `.pf-c-table__favorite` | Modifies a favorite cell for the favorited state.                      |
| `.pf-m-favorite`        | `.pf-c-table__sort`     | Modifies a sortable table header cell for use with a favorites column. |

## Draggable rows

### Draggable rows example

```html
<div
  id="table-draggable-rows-help"
>Activate the reorder button and use the arrow keys to reorder the list or use your mouse to drag/reorder. Press escape to cancel the reordering.</div>
<table
  class="pf-c-table pf-m-grid-md"
  role="grid"
  aria-label="This is a table with draggable rows example"
  id="table-draggable-rows"
>
  <caption>This is the table caption</caption>
  <thead>
    <tr role="row">
      <td></td>
      <th role="columnheader" scope="col">Repositories</th>
      <th role="columnheader" scope="col">Branches</th>
      <th role="columnheader" scope="col">Pull requests</th>
      <th role="columnheader" scope="col">Workspaces</th>
      <th role="columnheader" scope="col">Last commit</th>
    </tr>
  </thead>

  <tbody role="rowgroup">
    <tr role="row">
      <td class="pf-c-table__draggable" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          disabled
          aria-pressed="false"
          aria-label="Reorder"
          aria-describedby="table-draggable-rows-help"
          id="table-draggable-rows-row-1-draggable-button"
          aria-labelledby="table-draggable-rows-row-1-draggable-button table-draggable-rows-row-1-node"
        >
          <i class="fas fa-grip-vertical" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-draggable-rows-row-1-node">Draggable icon disabled</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__draggable" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-pressed="false"
          aria-label="Reorder"
          aria-describedby="table-draggable-rows-help"
          id="table-draggable-rows-row-2-draggable-button"
          aria-labelledby="table-draggable-rows-row-2-draggable-button table-draggable-rows-row-2-node"
        >
          <i class="fas fa-grip-vertical" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-draggable-rows-row-2-node">Table cell</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr class="pf-m-ghost-row" role="row">
      <td class="pf-c-table__draggable" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          disabled
          aria-pressed="false"
          aria-label="Reorder"
          aria-describedby="table-draggable-rows-help"
          id="table-draggable-rows-row-3-draggable-button"
          aria-labelledby="table-draggable-rows-row-3-draggable-button table-draggable-rows-row-3-node"
        >
          <i class="fas fa-grip-vertical" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-draggable-rows-row-3-node">Ghost row</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>

    <tr role="row">
      <td class="pf-c-table__draggable" role="cell">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-pressed="false"
          aria-label="Reorder"
          aria-describedby="table-draggable-rows-help"
          id="table-draggable-rows-row-4-draggable-button"
          aria-labelledby="table-draggable-rows-row-4-draggable-button table-draggable-rows-row-4-node"
        >
          <i class="fas fa-grip-vertical" aria-hidden="true"></i>
        </button>
      </td>
      <td role="cell" data-label="Repository name">
        <span id="table-draggable-rows-row-4-node">Table cell</span>
      </td>
      <td role="cell" data-label="Branches">10</td>
      <td role="cell" data-label="Pull requests">25</td>
      <td role="cell" data-label="Workspaces">5</td>
      <td role="cell" data-label="Last commit">2 days ago</td>
    </tr>
  </tbody>
</table>
<div
  class="pf-screen-reader"
  aria-live="assertive"
>This is the aria-live section that provides real-time feedback to the user.</div>

```

### Draggable rows accessibility

| Attribute                                                                              | Applied to                                                             | Outcome                                                                                                                                                                                                                                      |
| -------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-pressed="true or false"`                                                         | `.pf-c-table__draggable .pf-c-button`                                  | Indicates whether the button is currently pressed or not.                                                                                                                                                                                    |
| `aria-live`                                                                            | `[element with live text]`                                             | To give screen reader users live feedback about what's happening during interaction with the table, both during drag and drop interactions and keyboard interactions. **Highly Recommended**                                                 |
| `aria-describedby="[id value of applicable content]"`                                  | `.pf-c-table__draggable .pf-c-button`                                  | Gives the draggable button an accessible description by referring to the textual content that describes how to use the button to drag elements. The example here uses a `<div id="table-draggable-rows-help"></div>`. **Highly recommended** |
| `aria-labelledby="[id of .pf-c-table__draggable .pf-c-button] [id of row title text]"` | `.pf-c-table__draggable .pf-c-button`                                  | Provides an accessible name for the draggable button.                                                                                                                                                                                        |
| `id="[]"`                                                                              | `.pf-c-table__draggable .pf-c-button`, `[element with row title text]` | Gives the button and the text element accessible IDs.                                                                                                                                                                                        |

### Draggable rows usage

| Class                    | Applied to    | Outcome                                                                               |
| ------------------------ | ------------- | ------------------------------------------------------------------------------------- |
| `.pf-c-table__draggable` | `<td>`        | Initiates a draggable table cell.                                                     |
| `.pf-m-drag-over`        | `.pf-c-table` | Modifies the table to indicate that a draggable item is being dragged over the table. |

## Documentation

### Overview

Because the table component is not used for layout and presents tabular data only, it requires the use of `role="grid"`. Expandable table content (`.pf-c-table__expandable-content`) is placed within a singular `<td>` per expandable row, that can span multiple columns.

### Role="grid"

Applying `role="grid"` to tables enhances accessible interaction while in table layout, however the responsive, css grid based layout can cause unexpected interactions. Therefore, for css grid layout, it is recommended that `role="grid"` be removed.

### Sortable tables

Table columns may shift when expanding/collapsing. To address this, set `.pf-m-fit-content`, or assign a width `.pf-m-width-[width]` to the corresponding `<th>` defining the column or `<td>` within the column. Width values are `[10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, 90]` or `max`.

### Table header cells

By default, all table header cells are set to `white-space: nowrap`. If a `<th>`'s content needs to wrap, apply `.pf-m-wrap`.

### Implementation support

-   One expandable toggle button, positioned in the first cell of a non-expandable row, preceding an expandable row.
-   One checkbox or radio input, positioned in the first or second cell of a non-expandable row.
-   One action button, positioned in the last cell of a non-expandable row.
-   Tabular data.
-   Compact presentation modifier (not compatible with expandable table).

### Responsive layout modifiers

| Class                                                               | Applied to    | Outcome                                                                                                                                               |
| ------------------------------------------------------------------- | ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-m-grid-md`, `.pf-m-grid-lg`, `.pf-m-grid-xl`, `.pf-m-grid-2xl` | `.pf-c-table` | Changes tabular layout to responsive, grid based layout at suffixed breakpoint.                                                                       |
| `.pf-m-grid`                                                        | `.pf-c-table` | Changes tabular layout to responsive, grid based layout. This approach requires JavaScript to set this class at some prescribed viewport width value. |
| `.pf-m-sticky-header`                                               | `.pf-c-table` | Makes the table cells in `<thead>` sticky to the top of the table on scroll.                                                                          |
