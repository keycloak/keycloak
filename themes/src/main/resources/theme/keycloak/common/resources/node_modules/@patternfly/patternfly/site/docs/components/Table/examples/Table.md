---
title: Table
section: components
cssPrefix: pf-c-table
---

import './Table.css'

## Examples

```hbs title=Basic-table
{{#> table table--id="table-basic" table--grid="true" table--modifier="pf-m-grid-md" table--attribute='aria-label="This is a simple table example"'}}
  {{#> table-caption}}
    This is the table caption
  {{/table-caption}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Workspaces
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Last commit
      {{/table-th}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 1
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 2
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 3
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 4
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `role="grid"` | `.pf-c-table` | Identifies the element that serves as the grid widget container. **Required** |
| `aria-label` | `.pf-c-table` | Provides an accessible name for the table when a descriptive `<caption>` or `<h*>` is not available. **Required in the absence of `<caption>` or `<h*>`** |
| `data-label="[td description]"` | `<td>` | This attribute replaces table header in mobile viewport. It is rendered by `::before` pseudo element. |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-table` | `<table>` | Initiates a table element. **Required** |
| `.pf-c-table__caption` | `<caption>` | Initiates a table caption. |
| `.pf-m-center` | `<th>`, `<td>` | Modifies cell to center its contents. |

```hbs title=Sortable
{{#> table table--id="table-sortable" table--grid="true" table--modifier="pf-m-grid-lg" table--attribute='aria-label="This is a sortable table example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true" table-th--selected="true" table-th--asc="true"}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true" table-th--IsColumnHelp="true"}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true"}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Workspaces
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--IsColumnHelp="true"}}
        Last commit
      {{/table-th}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 1
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 2
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 3
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 4
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-sort=[ascending or descending]` | `.pf-c-table__sort` | Indicates if columns in a table are sorted in ascending or descending order. For each table, authors __SHOULD__ apply aria-sort to only one header at a time. **Required** |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-table__sort` | `<th>` | Initiates a table header sort cell. **Required for sortable table columns** |
| `.pf-c-table__button` | `<button>` | Initiates a table header sort cell button. **Required for sortable table columns** |
| `.pf-c-table__button-content` | `<div>` | Initiates a table header sort cell button content container. **Required for sortable table columns** Note: this is only necessary because `<button>` does not support`display: grid`. |
| `.pf-c-table__sort-indicator` | `.pf-c-table__sort > button > span` | Initiates a sort indicator. **Required for sortable table columns** |
| `.pf-m-selected` | `.pf-c-table__sort` | Modifies for sort selected state. **Required for sortable table columns** |
| `.fa-arrows-alt-v` | `.pf-c-table__sort > button > span > .fas` | Initiates icon within unsorted, sortable table header. **Required for sortable table columns** |
| `.fa-long-arrow-alt-up` | `.pf-c-table__sort > button > span > .fas` | Initiates icon within ascending sorted and selected, sortable table header. **Required for sortable table columns** |
| `.fa-long-arrow-alt-down` | `.pf-c-table__sort > button > span > .fas` | Initiates icon within descending sorted and selected, sortable table header. **Required for sortable table columns** |

```hbs title=With-checkboxes-and-actions
{{#> table table--id="table-checkboxes-and-actions" table--grid="true" table--modifier="pf-m-grid-lg" table--attribute='aria-label="This is a table with checkboxes"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-check-all" aria-label="Select all rows">
      {{/table-td}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Workspaces
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Last commit
      {{/table-th}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow1" aria-labelledby="{{concat table--id '-node1'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div id="{{table--id}}-node1">Node 1</div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-1") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow2" aria-labelledby="{{concat table--id '-node2'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node2">Node 2</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-2") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow3" aria-labelledby="{{concat table--id '-node3'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node3">Node 3</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-3") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow4" aria-labelledby="{{concat table--id '-node4'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node4">Node 4</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-4") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

When including interactive elements in a table, the primary, descriptive cell in the corresponding row is a `<th>`, rather than a `<td>`. In this example, 'Node 1' and 'Node 2 siemur/test-space' are `<th>`s.

When header cells are empty or they contain interactive elements, `<th>` should be replaced with `<td>`.

### Thead accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-label="[descriptive text]"` | `.pf-c-table__check input[type="checkbox"]` | Provides an accessible name for the checkbox. **Required** |

### Tbody accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-labelledby="[row_header_id]"` or `aria-label="[descriptive text]` | `.pf-c-table__check input[type="checkbox"]` | Provides an accessible name for the checkbox. **Required** |
| `id` | row header `<th>` > `*` | Provides an accessible description for the checkbox. **Required if using `aria-labelledby` for `.pf-c-table__check input[type="checkbox"]`** |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-table__check` | `<th>`, `<td>` | Initiates a check table cell. |
| `.pf-c-table__action` | `<th>`, `<td>` | Initiates an action table cell. |
| `.pf-c-table__inline-edit-action` | `<th>`, `<td>` | Initiates an inline edit action table cell. |

```hbs title=Expandable
{{#> table table--id="table-expandable" table--grid="true" table--modifier="pf-m-grid-lg" table--expandable="true" table--attribute='aria-label="Expandable table example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{> table-td table-td--IsEmpty="true"}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-check-all" aria-label="Select all rows">
      {{/table-td}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true" table-th--modifier="pf-m-width-30" table-th--selected="true" table-th--asc="true"}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true"}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true"}}
        Pull requests
      {{/table-th}}
      {{> table-td table-td--IsEmpty="true"}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody table-tbody--modifier="pf-m-expanded"}}
    {{#> table-tr table-tr--expanded="true"}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node1 ' table--id '-expandable-toggle1" id="' table--id '-expandable-toggle1" aria-label="Details" aria-controls="' table--id '-content1"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow1" aria-labelledby="{{concat table--id '-node1'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node1">Node 1</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link 1</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-1") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true" table-tr--IsExpanded="true"}}
      {{> table-td table-td--IsEmpty="true"}}
      {{> table-td table-td--IsEmpty="true"}}
      {{#> table-td table-td--attribute=(concat 'colspan="4" id="' table--id '-content1"')}}
        {{#> table-expandable-row-content}}
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        {{/table-expandable-row-content}}
      {{/table-td}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node2 ' table--id '-expandable-toggle2" id="' table--id '-expandable-toggle2" aria-label="Details" aria-controls="' table--id '-content2"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow2" aria-labelledby="{{concat table--id '-node2'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node2">Node 2</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link 2</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-2") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute=(concat 'colspan="7" id="' table--id '-content2"')}}
        {{#> table-expandable-row-content}}
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        {{/table-expandable-row-content}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}


  {{#> table-tbody table-tbody--modifier="pf-m-expanded"}}
    {{#> table-tr table-tr--expanded="true"}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node3 expandable-toggle3" id="expandable-toggle3" aria-label="Details" aria-controls="' table--id '-content3"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow3" aria-labelledby="{{concat table--id '-node3'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node3">Node 3</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link 3</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-3") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true" table-tr--IsExpanded="true"}}
      {{#> table-td table-td--attribute=(concat 'colspan="7" id="' table--id '-content3"')}}
        {{#> table-expandable-row-content}}
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        {{/table-expandable-row-content}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody table-tbody--modifier="pf-m-expanded"}}
    {{#> table-tr table-tr--expanded="true"}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node4 expandable-toggle4" id="expandable-toggle4" aria-label="Details" aria-controls="' table--id '-content4"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow4" aria-labelledby="{{concat table--id '-node4'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node4">Node 4</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link 4</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-4") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true" table-tr--IsExpanded="true"}}
      {{#> table-td table-td--modifier="pf-m-no-padding" table-td--attribute=(concat 'colspan="7" id="' table--id '-content4"')}}
        {{#> table-expandable-row-content}}
          Expandable row content has no padding.
        {{/table-expandable-row-content}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

Note: To apply padding to `.pf-c-table__expandable-row`, wrap the content in `.pf-c-table__expandable-row-content`. For no padding add `.pf-m-no-padding` to `.pf-c-table__expandable-row` > `<td>`

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `hidden` | `.pf-c-table__expandable-row` | Indicates that the expandable content is hidden. **Required** |
| `aria-expanded="true"` | `.pf-c-table__toggle` > `.pf-c-button` | Indicates that the row is visible. **Required**|
| `aria-label="[descriptive text]"` | `.pf-c-table__toggle` > `.pf-c-button` | Provides an accessible name for toggle button. **Required**|
| `aria-labelledby="[title_cell_id] [button_id]"` | `.pf-c-table__toggle` > `.pf-c-button` | Provides an accessible description for toggle button. **Required** |
| `id="[button_id]"` | `.pf-c-table__toggle` > `.pf-c-button` | Provides a reference for toggle button description. **Required** |
| `aria-controls="[id of element the button controls]"` | `.pf-c-table__toggle` > `.pf-c-button` | Identifies the expanded content controlled by the toggle button. **Required** |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-table__toggle-icon` | `<span>` | Initiates the table toggle icon wrapper. |
| `.pf-c-table__expandable-row` | `<tr>` | Initiates an expandable row. |
| `.pf-c-table__expandable-row-content` | `.pf-c-table__expandable-row` > `<td>` > `<div>` | Initiates an expandable row content wrapper. |
| `.pf-m-expanded` | `.pf-c-table__toggle` > `.pf-c-button`, `.pf-c-table__expandable-row` | Modifies for expanded state. |
| `.pf-m-no-padding` | `.pf-c-table__expandable-row` > `<td>` | Modifies the expandable row to have no padding. |

```hbs title=With-compound-expansion
{{#> table table--id="table-compound-expansion" table--grid="true" table--modifier="pf-m-grid-md" table--expandable="true" table--attribute='aria-label="Compound expandable table example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true" table-th--selected="true" table-th--asc="true"}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true"}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true"}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
       Workspaces
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
       Last commit
      {{/table-th}}
      {{> table-td table-td--IsEmpty="true"}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody table-tbody--modifier="pf-m-expanded"}}
    {{#> table-tr table-tr--expanded="true"}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--modifier="pf-m-expanded" table-td--data-label="Repositories" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-1"')}}
        <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;10
      {{/table-td}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--data-label="Branches" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-2"')}}
        <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
        234
      {{/table-td}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--data-label="Pull requests" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-3"')}}
        <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
        4
      {{/table-td}}
      {{#> table-th table-th--data-label="Workspaces"}}
        <a href="#">siemur/test-space</a>
      {{/table-th}}
      {{#> table-td table-td--data-label="Last commit"}}
        <span>20 minutes</span>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Open in Github</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-1") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true" table-tr--IsExpanded="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-1')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-2')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-3')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--data-label="Repositories" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-4"')}}
        <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;
        2
      {{/table-td}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--data-label="Branches" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-5"')}}
        <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
        82
      {{/table-td}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--data-label="Pull requests" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-6"')}}
        <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
        1
      {{/table-td}}
      {{#> table-th table-th--data-label="Workspaces"}}
        <a href="#">siemur/test-space</a>
      {{/table-th}}
      {{#> table-td table-td--data-label="Last commit"}}
        <span>1 day ago</span>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Open in Github</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-2") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-4')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-5')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-6')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--data-label="Repositories" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-7"')}}
        <i class="fas fa-code-branch" aria-hidden="true"></i>&nbsp;
        4
      {{/table-td}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--data-label="Branches" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-8"')}}
        <i class="fas fa-code" aria-hidden="true"></i>&nbsp;
        4
      {{/table-td}}
      {{#> table-td table-td--compound-expansion-toggle="true" table-td--data-label="Pull requests" table-td--button--attribute=(concat 'aria-expanded="true" aria-controls="' table--id '-nested-table-9"')}}
        <i class="fas fa-cube" aria-hidden="true"></i>&nbsp;
        1
      {{/table-td}}
      {{#> table-th table-th--data-label="Workspaces"}}
        <a href="#">siemur/test-space</a>
      {{/table-th}}
      {{#> table-td table-td--data-label="Last commit"}}
        <span>2 days ago</span>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Open in Github</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-3") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-7')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-8')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute='colspan="7"' table-td--modifier="pf-m-no-padding"}}
        {{#> table-nested table--id=(concat table--id '-nested-table-9')}}
        {{/table-nested}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Accessibility

| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `hidden` | `.pf-c-table__expandable-row` | Indicates that the expandable content is hidden. **Required** |
| `aria-expanded="true"` | `.pf-c-table__compound-expansion-toggle` > `.pf-c-button` | Indicates that the row is visible. **Required**|
| `aria-controls="[id of element the button controls]"` | `.pf-c-table__compound-expansion-toggle` > `.pf-c-button` | Identifies the expanded content controlled by the toggle button. **Required** |

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-expanded` | `<tbody>`, `.pf-c-table__compound-expansion-toggle` > `.pf-c-button` | Modifies a tbody with a row and an expandable row. |
| `.pf-c-table__compound-expansion-toggle` | `<td>` | Modifies a `<td>` on active/focus. |

```hbs title=Compact
{{#> table table--id="table-compact" table--grid="true" table--modifier="pf-m-compact pf-m-grid-md" table--attribute='aria-label="This is a compact table example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-check-all" aria-label="Select all rows">
      {{/table-td}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Contributor
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Position
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Location
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Last seen
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Numbers
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--icon="true"}}
        Icons
      {{/table-th}}
      {{> table-td table-td--IsEmpty="true"}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow1" aria-labelledby="{{concat table--id "-name1"}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Contributor"}}
        <span id="{{concat table--id "-name1"}}">Sam Jones</span>
      {{/table-th}}
      {{#> table-td table-td--data-label="Position"}}
        CSS guru
      {{/table-td}}
      {{#> table-td table-td--data-label="Location"}}
        Not too sure
      {{/table-td}}
      {{#> table-td table-td--data-label="Last seen"}}
        May 9, 2018
      {{/table-td}}
      {{#> table-td table-td--data-label="Numbers"}}
        0556
      {{/table-td}}
      {{#> table-td table-td--data-label="Icon" table-td--icon="true"}}
        <i class="fas fa-check"></i>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Action link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-1") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow2" aria-labelledby="{{concat table--id "-name2"}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Contributor"}}
        <span id="{{concat table--id "-name2"}}">Amy Miller</span>
      {{/table-th}}
      {{#> table-td table-td--data-label="Position"}}
        Visual design
      {{/table-td}}
      {{#> table-td table-td--data-label="Location"}}
        Raleigh
      {{/table-td}}
      {{#> table-td table-td--data-label="Last seen"}}
        May 9, 2018
      {{/table-td}}
      {{#> table-td table-td--data-label="Numbers"}}
        9492
      {{/table-td}}
      {{#> table-td table-td--data-label="Icon" table-td--icon="true"}}
        <i class="fas fa-check"></i>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Action link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-2") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow3" aria-labelledby="{{concat table--id "-name3"}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Contributor"}}
        <span id="{{concat table--id "-name3"}}">Steve Wilson</span>
      {{/table-th}}
      {{#> table-td table-td--data-label="Position"}}
        Visual design lead
      {{/table-td}}
      {{#> table-td table-td--data-label="Location"}}
        Westford
      {{/table-td}}
      {{#> table-td table-td--data-label="Last seen"}}
        May 9, 2018
      {{/table-td}}
      {{#> table-td table-td--data-label="Numbers"}}
        9929
      {{/table-td}}
      {{#> table-td table-td--data-label="Icon" table-td--icon="true"}}
        <i class="fas fa-check"></i>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Action link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-3") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow4" aria-labelledby="{{concat table--id "-name4"}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Contributor name"}}
        <span id="{{concat table--id "-name4"}}">Emma Jackson</span>
      {{/table-th}}
      {{#> table-td table-td--data-label="Position"}}
        Interaction design
      {{/table-td}}
      {{#> table-td table-td--data-label="Location"}}
        Westford
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        May 9, 2018
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2217
      {{/table-td}}
      {{#> table-td table-td--data-label="Icon" table-td--icon="true"}}
        <i class="fas fa-check"></i>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Action link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-4") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-compact` | `.pf-c-table` | Modifies for a compact table. |

```hbs title=Compact-with-no-borders
{{#> table table--id="table-compact-no-borders" table--grid="true" table--modifier="pf-m-compact pf-m-grid-md pf-m-no-border-rows" table--attribute='aria-label="This is a compact table with border rows example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-check-all" aria-label="Select all rows">
      {{/table-td}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Contributor
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Position
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Location
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Last seen
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Numbers
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--icon="true"}}
        Icons
      {{/table-th}}
      {{> table-td table-td--IsEmpty="true"}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow1" aria-labelledby="{{concat table--id "-name1"}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Contributor"}}
        <span id="{{concat table--id "-name1"}}">Sam Jones</span>
      {{/table-th}}
      {{#> table-td table-td--data-label="Position"}}
        CSS guru
      {{/table-td}}
      {{#> table-td table-td--data-label="Location"}}
        Not too sure
      {{/table-td}}
      {{#> table-td table-td--data-label="Last seen"}}
        May 9, 2018
      {{/table-td}}
      {{#> table-td table-td--data-label="Numbers"}}
        0556
      {{/table-td}}
      {{#> table-td table-td--data-label="Icon" table-td--icon="true"}}
        <i class="fas fa-check"></i>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Action link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-1") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow2" aria-labelledby="{{concat table--id "-name2"}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Contributor"}}
        <span id="{{concat table--id "-name2"}}">Amy Miller</span>
      {{/table-th}}
      {{#> table-td table-td--data-label="Position"}}
        Visual design
      {{/table-td}}
      {{#> table-td table-td--data-label="Location"}}
        Raleigh
      {{/table-td}}
      {{#> table-td table-td--data-label="Last seen"}}
        May 9, 2018
      {{/table-td}}
      {{#> table-td table-td--data-label="Numbers"}}
        9492
      {{/table-td}}
      {{#> table-td table-td--data-label="Icon" table-td--icon="true"}}
        <i class="fas fa-check"></i>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Action link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-2") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow3" aria-labelledby="{{concat table--id "-name3"}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Contributor"}}
        <span id="{{concat table--id "-name3"}}">Steve Wilson</span>
      {{/table-th}}
      {{#> table-td table-td--data-label="Position"}}
        Visual design lead
      {{/table-td}}
      {{#> table-td table-td--data-label="Location"}}
        Westford
      {{/table-td}}
      {{#> table-td table-td--data-label="Last seen"}}
        May 9, 2018
      {{/table-td}}
      {{#> table-td table-td--data-label="Numbers"}}
        9929
      {{/table-td}}
      {{#> table-td table-td--data-label="Icon" table-td--icon="true"}}
        <i class="fas fa-check"></i>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Action link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-3") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow4" aria-labelledby="{{concat table--id "-name4"}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Contributor name"}}
        <span id="{{concat table--id "-name4"}}">Emma Jackson</span>
      {{/table-th}}
      {{#> table-td table-td--data-label="Position"}}
        Interaction design
      {{/table-td}}
      {{#> table-td table-td--data-label="Location"}}
        Westford
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        May 9, 2018
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2217
      {{/table-td}}
      {{#> table-td table-td--data-label="Icon" table-td--icon="true"}}
        <i class="fas fa-check"></i>
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Action link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-4") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-no-border-rows` | `.pf-c-table.pf-m-compact` | Modifies to remove borders between rows. **Note: Can not be used with `.pf-m-expandable`.** |

```hbs title=Compact-expandable
{{#> table table--id="table-compact-expandable" table--grid="true" table--modifier="pf-m-compact pf-m-grid-md" table--expandable="true" table--attribute='aria-label="Compact expandable table example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{> table-td table-td--IsEmpty="true"}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-check-all" aria-label="Select all rows">
      {{/table-td}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-width-30"}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Pull requests
      {{/table-th}}
      {{> table-td table-td--IsEmpty="true"}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody table-tbody--modifier="pf-m-expanded"}}
    {{#> table-tr table-tr--expanded="true"}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node1 ' table--id '-expandable-toggle1" id="' table--id '-expandable-toggle1" aria-label="Details" aria-controls="' table--id '-content1"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow1" aria-labelledby="{{concat table--id '-node1'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <p id="{{table--id}}-node1">Node 1</p>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-1") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true" table-tr--IsExpanded="true"}}
      {{#> table-td table-td table-td--IsEmpty="true"}}{{/table-td}}
      {{#> table-td table-td table-td--IsEmpty="true"}}{{/table-td}}
      {{#> table-td table-td--attribute=(concat 'colspan="4" id="' table--id '-content1"')}}
        <div class="pf-c-table__expandable-row-content">
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        </div>
      {{/table-td}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node2 ' table--id '-expandable-toggle2" id="' table--id '-expandable-toggle2" aria-label="Details" aria-controls="' table--id '-content2"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow2" aria-labelledby="{{concat table--id '-node2'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <p id="{{table--id}}-node2">Node 2</p>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-2") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--modifier="pf-m-no-padding" table-td--attribute=(concat 'colspan="7" id="' table--id '-content2"')}}
        {{#> table-expandable-row-content}}
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        {{/table-expandable-row-content}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody table-tbody--modifier="pf-m-expanded"}}
    {{#> table-tr table-tr--expanded="true"}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node3 ' table--id '-expandable-toggle3" id="' table--id '-expandable-toggle3" aria-label="Details" aria-controls="' table--id '-content3"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow3" aria-labelledby="{{concat table--id '-node3'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <p id="{{table--id}}-node3">Node 3</p>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-3") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true" table-tr--IsExpanded="true"}}
      {{#> table-td table-td--attribute=(concat 'colspan="7" id="' table--id '-content3"')}}
        {{#> table-expandable-row-content}}
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        {{/table-expandable-row-content}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody table-tbody--modifier="pf-m-expanded"}}
    {{#> table-tr table-tr--expanded="true"}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node4 ' table--id '-expandable-toggle4" id="' table--id '-expandable-toggle4" aria-label="Details" aria-controls="' table--id '-content4"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow4" aria-labelledby="{{concat table--id '-node4'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <p id="{{table--id}}-node4">Node 4</p>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-4") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true" table-tr--IsExpanded="true"}}
      {{#> table-td table-td--modifier="pf-m-no-padding" table-td--attribute=(concat 'colspan="7" id="' table--id '-content4"')}}
        {{#> table-expandable-row-content}}
          This content has no padding.
        {{/table-expandable-row-content}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--toggle="true" table-td--button--attribute=(concat 'aria-labelledby="' table--id '-node5 ' table--id '-expandable-toggle5" id="' table--id '-expandable-toggle5" aria-label="Details" aria-controls="' table--id '-content5"')}}{{/table-td}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow5" aria-labelledby="{{concat table--id '-node5'}}">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <p id="{{table--id}}-node5">Node 5</p>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Action"}}
        <a href="#">Link</a>
      {{/table-td}}
      {{#> table-td table-td--action="true"}}
        {{#> dropdown id=(concat table--id "-dropdown-kebab-right-aligned-5") dropdown--IsActionMenu="true" dropdown-menu--modifier="pf-m-align-right" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}
        {{/dropdown}}
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr table-tr--expandable="true"}}
      {{#> table-td table-td--attribute=(concat 'colspan="7" id="' table--id '-content5"')}}
        {{#> table-expandable-row-content}}
          Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
        {{/table-expandable-row-content}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-expandable` | `.pf-c-table.pf-m-compact` | Indicates that the table has expandable rows. **Note: Can not be used with `.pf-m-no-border-rows`.** |

```hbs title=With-width-modifiers
{{#> table table--id="table-width-modifiers" table--grid="true" table--modifier="pf-m-grid-md" table--grid="true" table--attribute='aria-label="This is a width modifier expandable"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-check-all" aria-label="Check all rows">
      {{/table-td}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true" table-th--selected="true" table-th--asc="true" table-th--modifier="pf-m-width-30"}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true"}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--sortable="true"}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-fit-content"}}
        Workspaces
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-fit-content"}}
        Last commit
      {{/table-th}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow1" aria-labelledby="{{table--id}}-node1">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div id="{{table--id}}-node1">Node 1</div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow2" aria-labelledby="{{table--id}}-node2">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node2">Node 2</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow3" aria-labelledby="{{table--id}}-node3">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node3">Node 3</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--check="true"}}
        <input type="checkbox" name="{{table--id}}-checkrow4" aria-labelledby="{{table--id}}-node4">
      {{/table-td}}
      {{#> table-th table-th--data-label="Repository name"}}
        <div>
          <div id="{{table--id}}-node4">Node 4</div>
          <a href="#">siemur/test-space</a>
        </div>
      {{/table-th}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-width-[10, 15, 20, 25, 30, 35, 40, 45, 50, 60, 70, 80, or 90]` | `<th>`, `<td>` | Percentage based modifier for `th` and `td` widths. **Recommended for sortable title cell** |
| `.pf-m-width-max` | `<th>`, `<td>` | Percentage based modifier for `th` and `td` maximum width. |
| `.pf-m-fit-content` | `<th>`, `<td>` | Percentage based modifier for `th` and `td` minimum width with no text wrapping. |

```hbs title=With-hidden/visible-breakpoint-modifiers
{{#> table table--id="table-hidden-visible" table--grid="true" table--modifier="pf-m-grid-lg" table--attribute='aria-label="Table with hidden and visible modifiers example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg"}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-hidden-on-md pf-m-visible-on-lg"}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Workspaces
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-hidden pf-m-visible-on-sm"}}
        Last commit
      {{/table-th}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--modifier="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg" table-td--data-label="Repository name"}}
        Visible only on md breakpoint
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-hidden-on-md pf-m-visible-on-lg" table-td--data-label="Pull requests"}}
        Hidden only on md breakpoint
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-hidden pf-m-visible-on-sm" table-td--data-label="Last commit"}}
        Hidden on xs breakpoint
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--modifier="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg" table-td--data-label="Repository name"}}
        Repository 2
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-hidden-on-md pf-m-visible-on-lg" table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-hidden pf-m-visible-on-sm" table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--modifier="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg" table-td--data-label="Repository name"}}
        Repository 3
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-hidden-on-md pf-m-visible-on-lg" table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-hidden pf-m-visible-on-sm" table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--modifier="pf-m-hidden pf-m-visible-on-md pf-m-hidden-on-lg" table-td--data-label="Repository name"}}
        Repository 4
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-hidden-on-md pf-m-visible-on-lg" table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-hidden pf-m-visible-on-sm" table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-hidden{-on-[breakpoint]}` | `.pf-c-table tr > *` | Hides a table cell at a given breakpoint, or hides it at all breakpoints with `.pf-m-hidden`. **Note: Needs to apply to all cells in the column you want to hide.** |
| `.pf-m-visible{-on-[breakpoint]}` | `.pf-c-table tr > *` | Shows a table cell at a given breakpoint. |

## Controlling text

To better control table cell behavior, PatternFly provides a series of modifiers to help contextually control layout. By default, `thead` cells are set to truncate, whereas `tbody` cells are set to wrap. Both `th` and `td` cells use a set of shared css properties mapped to customizable css variable values. Because only the shared css variables are changed by the modifier selector and not the properties, the modifier can be applied to any parent element up until `.pf-c-table` itself [`thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text`].

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-wrap` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Sets table cell content to wrap. If applied to `thead`, `tbody` or `tr`, then all child cells will be affected. This is the default behavior for <code>tbody</code> cells. |
| `.pf-m-truncate` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Sets text to truncate based on a minimum width and available space adjacent table cells.  If applied to `thead`, `tbody` or `tr`, then all child cells will be affected. This is the default behavior for <code>thead</code> cells. |
| `.pf-m-nowrap` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Unsets min/max width and sets whitespace to nowrap.  If applied to `thead`, `tbody` or `tr`, then all child cells will be affected. This is specifically beneficial for cell's whose <code>thead th</code> cells are blank. The following example highlights link text that should display inline. Be careful with this modifier, it will prioritize its cell's content above all other cell's contents. |
| `.pf-m-fit-content` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Fit column width to cell content.  If applied to `thead`, `tbody` or `tr`, then all child cells will be affected. |
| `.pf-m-break-word` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Breaks long strings wherever necessary as defined by the table layout. If applied to `thead`, `tbody` or `tr`, then all child cells will be affected. |

```hbs title=Modifiers-without-text-wrapper
{{#> table table--grid="true" table--modifier="pf-m-grid-lg" table--id="modifiers-without-text-wrapper-example" table--attribute='aria-label="This is a simple table example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-width-20"}}
        Truncate (width 20%)
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Break word
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-wrap"}}
        Wrapping table header text. This <code>th</code> text will wrap instead of truncate.
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-fit-content"}}
        Fit content
      {{/table-th}}
      {{> table-td table-td--IsEmpty="true"}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--modifier="pf-m-truncate" table-td--data-label="Truncating text"}}
        This text will truncate instead of wrap in table layout and wrap gracefully in grid layout.
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-break-word" table-td--data-label="Break word"}}
        <a href="#">http://thisisaverylongurlthatneedstobreakusethebreakwordmodifier.org</a>
      {{/table-td}}
      {{#> table-td table-td--data-label="Wrapping"}}
        <p>By default, <code>thead</code> cells will truncate and <code>tbody</code> cells will wrap. Use <code>.pf-m-wrap</code> on a <code>th</code> to change its behavior.</p>
      {{/table-td}}
      {{#> table-td table-td--data-label="Fit content"}}
        This cell's content will adjust itself to the parent th width. This modifier only affects table layouts.
      {{/table-td}}
      {{#> table-td table-td--modifier="pf-m-nowrap" table-td--data-label="No wrap"}}
        <a href="#">No wrap</a>
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

## The table-text element

By default, truncation and wrapping settings do not affect the grid layout, but text will fallback gracefully by passively wrapping long strings. Truncation and wrapping settings will persist with the addition of a `.pf-c-table__text` wrapper on table cell content. In addition to `.pf-c-table__text`, all PatternFly layouts can be used in table cells and contain table text elements.

```hbs title=Table-text-element
{{#> table table--grid="true" table--modifier="pf-m-grid-md" table--id="table-text-element-example" table--attribute='aria-label="This is a simple table example"'}}
  {{#> table-caption}}
    This table contains <code>.pf-c-table__text</code>&nbsp; examples. The <code>.pf-c-table__text</code>&nbsp; element can be using alone or in a nested configuration.
  {{/table-caption}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Selector/element
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Result
      {{/table-th}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-th table-th--data-label="Element" table-th--modifier="pf-m-fit-content"  table-th--isRowHeader="true" table-th--attribute='scope="row"'}}
        {{#> table-text table-text--type="div"}}
          <b><code>th.pf-m-truncate</code></b>
        {{/table-text}}
      {{/table-th}}
      {{#> table-td table-td--modifier="pf-m-truncate" table-td--data-label="Truncating text"}}
        {{#> table-text}}
          This table cell contains a single <code>`.pf-c-table__text`</code>&nbsp; wrapper with the parent table cell applying <code>`.pf-m-truncate`</code>. The child <code>`.pf-c-table__text`</code>&nbsp; element will inherit the modifier settings and apply to the grid layout.
        {{/table-text}}
      {{/table-td}}
    {{/table-tr}}
    {{#> table-tr}}
      {{#> table-th table-th--data-label="Element" table-th--modifier="pf-m-fit-content" table-th--isRowHeader="true" table-th--attribute='scope="row"'}}
        {{#> table-text table-text--type="div"}}
          <b><code>.pf-l-stack</code></b>
        {{/table-text}}
      {{/table-th}}
      {{#> table-td table-td--data-label="Truncating text"}}
        {{#> stack stack--modifier="pf-m-gutter"}}
          {{#> stack-item}}
            {{#> table-text table-text--modifier="" table-text--type="div"}}
              Because <code>.pf-m-grid</code>&nbsp; applies a grid layout to <code>.pf-c-table</code>, child elements will stack in the grid layout. To prevent this, wrap multiple elements with a div or use a PatternFly layout.
            {{/table-text}}
          {{/stack-item}}
          {{#> stack-item}}
            {{#> table-text table-text--modifier="" table-text--type="p"}}
              The <b><code>.pf-c-table__text</code>&nbsp;element</b>&nbsp; can additionally be nested, like in this example. The next <code>.pf-c-table__text</code> element has a very long url whose width needs be constrained.
            {{/table-text}}
          {{/stack-item}}
          {{#> stack-item}}
            {{#> table-text table-text--type="a" table-text--attribute='href="#"' table-text--modifier="pf-m-truncate"}}
              http://truncatemodifierappliedtoaverylongurlthatwillforcethetabletobreakluckilywehavethepfctabletextelement.com
            {{/table-text}}
          {{/stack-item}}
          {{#> stack-item}}
            {{#> table-text table-text--modifier="" table-text--type="p"}}
              This <b><code>.pf-c-table__text</code>&nbsp;element</b>&nbsp; applies its own built in grid layout <b><code>.pf-m-stack</code></b>&nbsp;as well as a gutter <b><code>.pf-m-gutter</code></b>.
            {{/table-text}}
          {{/stack-item}}
        {{/stack}}
      {{/table-td}}
    {{/table-tr}}
    {{#> table-tr}}
      {{#> table-th table-th--data-label="Element" table-th--modifier="pf-m-fit-content" table-th--isRowHeader="true" table-th--attribute='scope="row"'}}
        {{#> table-text table-text--type="div"}}
          <b><code>.pf-l-flex</code></b>
        {{/table-text}}
      {{/table-th}}
      {{#> table-td table-td--data-label="Truncating text"}}
        {{#> l-flex l-flex--modifier="pf-m-column pf-m-row-on-xl"}}
          {{#> l-flex-item l-flex-item--modifier="pf-m-flex-1"}}
            {{#> table-text table-text--modifier="" table-text--type="p"}}
              Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt.
            {{/table-text}}
          {{/l-flex-item}}
          {{#> l-flex-item l-flex-item--modifier="pf-m-flex-1"}}
            {{#> table-text newcontext table-text--type="a" table-text--attribute='href="#"' table-text--modifier="pf-m-break-word"}}
              http://breakwordmodifierappliedtoaverylongurlthatwillforcethetabletobreakluckilywehavethepfctabletextelement.com
            {{/table-text}}
          {{/l-flex-item}}
        {{/l-flex}}
      {{/table-td}}
    {{/table-tr}}
    {{#> table-tr}}
      {{#> table-th table-th--data-label="Element" table-th--modifier="pf-m-fit-content" table-th--isRowHeader="true" table-th--attribute='scope="row"'}}
        {{#> table-text table-text--type="div"}}
          <b><code>.pf-l-flex</code></b>
        {{/table-text}}
      {{/table-th}}
      {{#> table-td table-td--data-label="Truncating text"}}
        {{#> l-flex l-flex--modifier="pf-m-column"}}
          {{#> l-flex newcontext}}
            {{#> l-flex-item}}
              <i class="fas fa-code-branch" aria-hidden="true"></i>
              &nbsp;5
            {{/l-flex-item}}
            {{#> l-flex-item}}
              <i class="fas fa-code" aria-hidden="true"></i>
              &nbsp;9
            {{/l-flex-item}}
            {{#> l-flex-item}}
              <i class="fas fa-cube" aria-hidden="true"></i>
              &nbsp;2
            {{/l-flex-item}}
            {{#> l-flex-item}}
              <i class="fas fa-check-circle" aria-hidden="true"></i>
              &nbsp;11
            {{/l-flex-item}}
          {{/l-flex}}
          {{#> l-flex-item}}
            {{#> table-text newcontext table-text--type="p"}}
              This is paragraph that we want to wrap. It doesn't need a modifier and has no extra long strings. Any modifier available for the flex layout can be used here.
            {{/table-text}}
          {{/l-flex-item}}
          {{#> l-flex-item}}
            {{#> table-text newcontext table-text--type="a" table-text--attribute='href="#"' table-text--modifier="pf-m-break-word"}}
              http://breakwordmodifierappliedtoaverylongurlthatwillforcethetabletobreakluckilywehavethepfctabletextelement.com
            {{/table-text}}
          {{/l-flex-item}}
        {{/l-flex}}
      {{/table-td}}
    {{/table-tr}}
    {{#> table-tr}}
      {{#> table-th table-th--data-label="Element" table-th--modifier="pf-m-fit-content" table-th--isRowHeader="true" table-th--attribute='scope="row"'}}
        {{#> table-text table-text--type="div"}}
          <b><code>.pf-l-grid</code></b>
        {{/table-text}}
      {{/table-th}}
      {{#> table-td table-td--data-label="Truncating text"}}
        {{#> grid grid--modifier="pf-m-gutter"}}
          {{#> grid-item grid-item--modifier="pf-m-6-col pf-m-3-col-on-md"}}
            Item 1
          {{/grid-item}}
          {{#> grid-item grid-item--modifier="pf-m-6-col pf-m-3-col-on-md"}}
            Item 2
          {{/grid-item}}
          {{#> grid-item grid-item--modifier="pf-m-6-col pf-m-3-col-on-md"}}
            Item 3
          {{/grid-item}}
          {{#> grid-item grid-item--modifier="pf-m-6-col pf-m-3-col-on-md"}}
            Item 4
          {{/grid-item}}
          {{#> grid-item}}
            {{#> table-text table-text--modifier="" table-text--type="p"}}
              This is paragraph that we want to wrap. It doesn't need a modifier and has no extra long strings. Any modifier available for the flex layout can be used here.
            {{/table-text}}
          {{/grid-item}}
          {{#> grid-item}}
            {{#> table-text newcontext table-text--type="a" table-text--attribute='href="#"' table-text--modifier="pf-m-truncate"}}
              http://breakwordmodifierappliedtoaverylongurlthatwillforcethetabletobreakluckilywehavethepfctabletextelement.com
            {{/table-text}}
          {{/grid-item}}
        {{/grid}}
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-table__text` | `th > *`, `td > *` | Initiates a table text element. |
| `.pf-m-truncate` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Modifies text to truncate. |
| `.pf-m-nowrap` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Modifies text to not wrap. |
| `.pf-m-wrap` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Modifies text to wrap. |
| `.pf-m-fit-content` | `thead`, `tr`, `th`, `.pf-c-table__text` | Modifies `th` to fit its contents. |
| `.pf-m-break-word` | `thead`, `tbody`, `tr`, `th`, `td`, `.pf-c-table__text` | Modifies text strings to break. |

```hbs title=th-truncation
{{#> tooltip tooltip--modifier="pf-m-top"}}
  {{#> tooltip-content tooltip-content--attribute='id="tooltip-top-content"'}}
    Pull Requests
  {{/tooltip-content}}
{{/tooltip}}
{{#> table table--id="th-truncation-example" table--attribute='aria-label="This is a simple table example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier=""}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Workspaces
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Last commit
      {{/table-th}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Long lines of text will shrink adjacent column widths.
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        This example is not responsive. Adjacent <code>tbody</code> cells will shrink as a result of this text being a longer string and adjacent text being shorter in length. Truncation can be overridden in <code>th</code> cells with the addition of <code>.pf-m-wrap</code>, <code>.pf-m-nowrap</code> or <code>.pf-m-fit-content</code>.
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

## Long strings in table cells will push content. Add a width modifier to `thead th` to limit string length or add `.pf-m-truncate` to `tbody td`.

```hbs title=Width-constrained
{{#> table table--id="width-constrained-example" table--grid="true" table--modifier="pf-m-grid-md" table--attribute='aria-label="This is a simple table example"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"' table-th--modifier="pf-m-width-40"}}
        Width 40
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--modifier="pf-m-fit-content" table-th--attribute='scope="col"'}}
        Fit content th
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Last commit
      {{/table-th}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Since this is a long string of text and the other cells contain short strings (narrower than their table header), we'll need to control width this table header's width. Let's set width to 40%.
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--modifier="pf-m-truncate" table-td--data-label="Repository name"}}
        This string will truncate in table mode only. Since this is a long string of text and the other cells contain short strings (narrower than their table header), we'll need to control width this table header's width. Let's set width to 40%.
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

```hbs title=Sticky-header
{{#> table table--id="table-sticky-header" table--grid="true" table--modifier="pf-m-grid-md pf-m-sticky-header" table--attribute='aria-label="This is a table with sticky header cells"'}}
  {{#> table-thead}}
    {{#> table-tr}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Repositories
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Branches
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Pull requests
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Workspaces
      {{/table-th}}
      {{#> table-th table-th--attribute='scope="col"'}}
        Last commit
      {{/table-th}}
    {{/table-tr}}
  {{/table-thead}}

  {{#> table-tbody}}
    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 1
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 2
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 3
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}

    {{#> table-tr}}
      {{#> table-td table-td--data-label="Repository name"}}
        Repository 4
      {{/table-td}}
      {{#> table-td table-td--data-label="Branches"}}
        10
      {{/table-td}}
      {{#> table-td table-td--data-label="Pull requests"}}
        25
      {{/table-td}}
      {{#> table-td table-td--data-label="Workspaces"}}
        5
      {{/table-td}}
      {{#> table-td table-td--data-label="Last commit"}}
        2 days ago
      {{/table-td}}
    {{/table-tr}}
  {{/table-tbody}}
{{/table}}
```

### Usage

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-wrap` | `<th>`, `<td>` | Modifies content to wrap. |


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

- One expandable toggle button, positioned in the first cell of a non-expandable row, preceding an expandable row.
- One checkbox, positioned in the first or second cell of a non-expandable row.
- One action button, positioned in the last cell of a non-expandable row.
- Tabular data.
- Compact presentation modifier (not compatible with expandable table).

### Responsive layout modifiers

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-m-grid-md`, `.pf-m-grid-lg`, `.pf-m-grid-xl`, `.pf-m-grid-2xl` | `.pf-c-table` | Changes tabular layout to responsive, grid based layout at suffixed breakpoint. |
| `.pf-m-grid` | `.pf-c-table` | Changes tabular layout to responsive, grid based layout. This approach requires JavaScript to set this class at some prescribed viewport width value. |
| `.pf-m-sticky-header` | `.pf-c-table` | Makes the table cells in `<thead>` sticky to the top of the table on scroll. |
