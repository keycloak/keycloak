---
id: Inline edit
section: components
cssPrefix: pf-c-inline-edit
---## Introduction

**Inline edit** is a utilitarian component that has two core elements, `.pf-c-inline-edit__value` and `.pf-c-inline-edit__input` and is based on a simple concept. When **value** is visible, **input** is hidden, and vice versa.

### Generic groups

`.pf-c-inline-edit__group`s provide basic layout support and have several available modifiers (`.pf-m-action-group`, `.pf-m-icon-group`).

### Actions

**Actions** (`.pf-c-inline-edit__action`) are hidden by default and exposed when a region of `.pf-c-inline-edit` becomes active. The default visibility of an **action** or **action group** can be inversed by adding `.pf-m-enable-editable`. `.pf-m-enable-editable` flags an element or group of elements as the controlling agents for enabling editable content and therefore is visible by default and hidden when inline editing is enabled.

## Examples

Inline edit **toggle** can be placed anywhere within `.pf-c-inline-edit`. It initiates the editability of elements. When an element becomes editable, **toggle** is hidden.

### Inline edit toggle

```html
<div class="pf-c-inline-edit" id="inline-edit-toggle-example">
  <div class="pf-c-inline-edit__action pf-m-enable-editable">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      id="inline-edit-toggle-example-edit-button"
      aria-label="Edit"
      aria-labelledby="inline-edit-toggle-example-edit-button inline-edit-toggle-example-label"
    >
      <i class="fas fa-pencil-alt" aria-hidden="true"></i>
    </button>
  </div>
</div>

```

Inline edit **value** can be placed anywhere within `.pf-c-inline-edit`. It is visible by default and hidden when inline edit becomes **editable**.

### Inline edit value

```html
<div class="pf-c-inline-edit" id="inline-edit-value-example">
  <div class="pf-c-inline-edit__value">Static value</div>
</div>

```

Inline edit **action-group** contains save and cancel actions and is only visible when inline edit is **editable**.

### Inline edit action group

```html
<div
  class="pf-c-inline-edit pf-m-inline-editable"
  id="inline-edit-action-group-example"
>
  <div class="pf-c-inline-edit__group pf-m-action-group">
    <div class="pf-c-inline-edit__action">
      <button class="pf-c-button pf-m-primary" type="button">Save</button>
    </div>
    <div class="pf-c-inline-edit__action">
      <button class="pf-c-button pf-m-secondary" type="button">Cancel</button>
    </div>
  </div>
</div>

```

### Inline edit action group icon buttons

```html
<div
  class="pf-c-inline-edit pf-m-inline-editable"
  id="inline-edit-action-group-icon-buttons-example"
>
  <div class="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group">
    <div class="pf-c-inline-edit__action pf-m-valid">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Save edits"
      >
        <i class="fas fa-check" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-inline-edit__action">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Cancel edits"
      >
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
    </div>
  </div>
</div>

```

### Single inline edit (default)

```html
<form class="pf-c-inline-edit" id="single-inline-edit-example">
  <div class="pf-c-inline-edit__group">
    <div
      class="pf-c-inline-edit__value"
      id="single-inline-edit-example-label"
    >Static value</div>
    <div class="pf-c-inline-edit__action pf-m-enable-editable">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        id="single-inline-edit-example-edit-button"
        aria-label="Edit"
        aria-labelledby="single-inline-edit-example-edit-button single-inline-edit-example-label"
      >
        <i class="fas fa-pencil-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-inline-edit__group">
    <div class="pf-c-inline-edit__input">
      <input
        class="pf-c-form-control"
        type="text"
        value="Static value"
        aria-label="Editable text input"
      />
    </div>
    <div class="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group">
      <div class="pf-c-inline-edit__action pf-m-valid">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Save edits"
        >
          <i class="fas fa-check" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-inline-edit__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Cancel edits"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </div>
</form>

```

### Single inline edit (active)

```html
<form
  class="pf-c-inline-edit pf-m-inline-editable"
  id="single-editable-example"
>
  <div class="pf-c-inline-edit__group">
    <div
      class="pf-c-inline-edit__value"
      id="single-editable-example-label"
    >Static value</div>
    <div class="pf-c-inline-edit__action pf-m-enable-editable">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        id="single-editable-example-edit-button"
        aria-label="Edit"
        aria-labelledby="single-editable-example-edit-button single-editable-example-label"
      >
        <i class="fas fa-pencil-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-inline-edit__group">
    <div class="pf-c-inline-edit__input">
      <input
        class="pf-c-form-control"
        type="text"
        value="Static value"
        aria-label="Editable text input"
      />
    </div>
    <div class="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group">
      <div class="pf-c-inline-edit__action pf-m-valid">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Save edits"
        >
          <i class="fas fa-check" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-inline-edit__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Cancel edits"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </div>
</form>

```

### Free form edit

```html
<div class="pf-c-inline-edit" id="free-form-edit-example">
  <div
    class="pf-c-inline-edit__editable-text"
    role="textbox"
    id="free-form-edit-example-text"
    aria-label="Editable text"
  >Free form text</div>
</div>

```

### Single inline edit with label (default)

```html
<form class="pf-c-inline-edit" id="single-inline-edit-with-label-example">
  <div class="pf-c-inline-edit__group">
    <label
      class="pf-c-inline-edit__label"
      id="single-inline-edit-with-label-example-label"
      for="single-inline-edit-with-label-example-input"
    >Single inline edit group</label>
    <div class="pf-c-inline-edit__action pf-m-enable-editable">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        id="single-inline-edit-with-label-example-edit-button"
        aria-label="Edit"
        aria-labelledby="single-inline-edit-with-label-example-label single-inline-edit-with-label-example-edit-button"
      >
        <i class="fas fa-pencil-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-inline-edit__value">Static value</div>
  <div class="pf-c-inline-edit__group">
    <div class="pf-c-inline-edit__input">
      <input
        class="pf-c-form-control"
        type="text"
        id="single-inline-edit-with-label-example-input"
        value="Static value"
        aria-label="Editable text input"
      />
    </div>
    <div class="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group">
      <div class="pf-c-inline-edit__action pf-m-valid">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Save edits"
        >
          <i class="fas fa-check" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-inline-edit__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Cancel edits"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </div>
</form>

```

### State valid

```html
<div class="pf-c-inline-edit pf-m-inline-editable" id="inline-edit-state-valid">
  <div class="pf-c-inline-edit__group">
    <label
      class="pf-c-inline-edit__label"
      id="inline-edit-state-valid-label"
      for="inline-edit-state-valid-input"
    >Valid example</label>
    <div class="pf-c-inline-edit__action pf-m-enable-editable">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        id="inline-edit-state-valid-edit-button"
        aria-label="Edit"
        aria-labelledby="inline-edit-state-valid-label inline-edit-state-valid-edit-button"
      >
        <i class="fas fa-pencil-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-inline-edit__value">Static value</div>
  <div class="pf-c-inline-edit__group">
    <div class="pf-c-inline-edit__input">
      <input
        class="pf-c-form-control"
        type="text"
        value="Static value"
        aria-label="Editable text input"
      />
    </div>
    <div class="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group">
      <div class="pf-c-inline-edit__action pf-m-valid">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Save edits"
        >
          <i class="fas fa-check" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-inline-edit__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Cancel edits"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </div>
</div>

```

### State invalid

```html
<div
  class="pf-c-inline-edit pf-m-inline-editable"
  id="inline-edit-state-invalid"
>
  <div class="pf-c-inline-edit__group">
    <label
      class="pf-c-inline-edit__label"
      id="inline-edit-state-invalid-label"
      for="inline-edit-state-invalid-input"
    >Invalid example</label>
    <div class="pf-c-inline-edit__action pf-m-enable-editable">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        id="inline-edit-state-invalid-edit-button"
        aria-label="Edit"
        aria-labelledby="inline-edit-state-invalid-label inline-edit-state-invalid-edit-button"
      >
        <i class="fas fa-pencil-alt" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <div class="pf-c-inline-edit__value">Static value</div>
  <div class="pf-c-inline-edit__group">
    <div class="pf-c-inline-edit__input">
      <input
        class="pf-c-form-control"
        required
        value="Invalid state"
        aria-invalid="true"
        aria-label="Error state input example"
      />
    </div>
    <div class="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group">
      <div class="pf-c-inline-edit__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          disabled
          aria-label="Save edits"
        >
          <i class="fas fa-check" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-inline-edit__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Cancel edits"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
    </div>
  </div>
</div>

```

### Inline edit table row

```html
<form class="pf-c-inline-edit" id="bulk-edit-table-example">
  <table
    class="pf-c-table pf-m-grid-lg"
    role="grid"
    aria-label="Inline edit table row example"
    id="inline-edit-table-row-example"
  >
    <caption>This is the table caption</caption>
    <thead>
      <tr role="row">
        <th role="columnheader">Text input</th>
        <th role="columnheader">Disabled text input</th>
        <th role="columnheader">Checkboxes</th>
        <th role="columnheader">Radios</th>
        <th role="columnheader">Number</th>
        <td></td>

        <td></td>
      </tr>
    </thead>

    <tbody role="rowgroup">
      <tr class="pf-m-inline-editable" role="row">
        <th role="columnheader" data-label="Text input">
          <div class="pf-c-inline-edit__value">Text input description content</div>
          <div class="pf-c-inline-edit__input">
            <input
              class="pf-c-form-control"
              type="text"
              value="Text input description content"
              id="bulk-edit-table-example-row-1-text-input"
              aria-label="Text input"
            />
          </div>
        </th>
        <td role="cell" data-label="Disabled text input">
          <div
            class="pf-c-inline-edit__value"
          >Text input disabled, description content</div>
          <div class="pf-c-inline-edit__input">
            <input
              class="pf-c-form-control"
              type="text"
              value="Text input disabled, description content"
              id="bulk-edit-table-example-row-1-text-input-disabled"
              aria-label="Disabled text input"
              disabled
            />
          </div>
        </td>
        <td role="cell" data-label="Checkboxes">
          <div class="pf-c-inline-edit__value">Check 1, Check 2</div>
          <div class="pf-c-inline-edit__group pf-m-column">
            <div class="pf-c-inline-edit__input">
              <div class="pf-c-check">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="bulk-edit-table-example-row-1-check-1"
                  name="bulk-edit-table-example-row-1-example-check"
                />

                <label
                  class="pf-c-check__label"
                  for="bulk-edit-table-example-row-1-check-1"
                >Check 1</label>
              </div>
            </div>
            <div class="pf-c-inline-edit__input">
              <div class="pf-c-check">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="bulk-edit-table-example-row-1-check-2"
                  name="bulk-edit-table-example-row-1-example-check-2"
                />

                <label
                  class="pf-c-check__label"
                  for="bulk-edit-table-example-row-1-check-2"
                >Check 2</label>
              </div>
            </div>
          </div>
        </td>
        <td role="cell" data-label="Radios">
          <div class="pf-c-inline-edit__value">Radio 1, Radio 2</div>
          <div
            class="pf-c-inline-edit__group pf-m-column"
            role="radiogroup"
            aria-label="Radio group example"
          >
            <div class="pf-c-inline-edit__input">
              <div class="pf-c-radio">
                <input
                  class="pf-c-radio__input"
                  type="radio"
                  id="bulk-edit-table-example-row-1-radio-1"
                  name="bulk-edit-table-example-row-1-example-radio"
                />

                <label
                  class="pf-c-radio__label"
                  for="bulk-edit-table-example-row-1-radio-1"
                >Radio 1</label>
              </div>
            </div>
            <div class="pf-c-inline-edit__input">
              <div class="pf-c-radio">
                <input
                  class="pf-c-radio__input"
                  type="radio"
                  id="bulk-edit-table-example-row-1-radio-2"
                  name="bulk-edit-table-example-row-1-example-radio"
                />

                <label
                  class="pf-c-radio__label"
                  for="bulk-edit-table-example-row-1-radio-2"
                >Radio 2</label>
              </div>
            </div>
          </div>
        </td>
        <td role="cell" data-label="Number">
          <div class="pf-c-inline-edit__value">2</div>
          <div class="pf-c-inline-edit__input">
            <input
              class="pf-c-form-control"
              type="number"
              value="2"
              id="bulk-edit-table-example-row-1-number-input"
              aria-label="Number input"
            />
          </div>
        </td>
        <td class="pf-c-table__inline-edit-action" role="cell">
          <div
            class="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group"
          >
            <div class="pf-c-inline-edit__action pf-m-valid">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Save edits"
              >
                <i class="fas fa-check" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-inline-edit__action">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Cancel edits"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </div>
          <div class="pf-c-inline-edit__action pf-m-enable-editable">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              id="bulk-edit-table-example-row-1-edit-button"
              aria-label="Edit"
              aria-labelledby="bulk-edit-table-example-label bulk-edit-table-example-row-1-edit-button"
            >
              <i class="fas fa-pencil-alt" aria-hidden="true"></i>
            </button>
          </div>
        </td>
        <td class="pf-c-table__action" role="cell">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="inline-edit-table-row-example-row-1--dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="inline-edit-table-row-example-row-1--dropdown-kebab-button"
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
        <th role="columnheader" data-label="Text input">
          <div class="pf-c-inline-edit__value">Text input description content</div>
          <div class="pf-c-inline-edit__input">
            <input
              class="pf-c-form-control"
              type="text"
              value="Text input description content"
              id="bulk-edit-table-example-row-2-text-input"
              aria-label="Text input"
            />
          </div>
        </th>
        <td role="cell" data-label="Disabled text input">
          <div
            class="pf-c-inline-edit__value"
          >Text input disabled, description content</div>
          <div class="pf-c-inline-edit__input">
            <input
              class="pf-c-form-control"
              type="text"
              value="Text input disabled, description content"
              id="bulk-edit-table-example-row-2-text-input-disabled"
              aria-label="Disabled text input"
              disabled
            />
          </div>
        </td>
        <td role="cell" data-label="Checkboxes">
          <div class="pf-c-inline-edit__value">Check 1, Check 2</div>
          <div class="pf-c-inline-edit__group pf-m-column">
            <div class="pf-c-inline-edit__input">
              <div class="pf-c-check">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="bulk-edit-table-example-row-2-check-1"
                  name="bulk-edit-table-example-row-2-example-check"
                />

                <label
                  class="pf-c-check__label"
                  for="bulk-edit-table-example-row-2-check-1"
                >Check 1</label>
              </div>
            </div>
            <div class="pf-c-inline-edit__input">
              <div class="pf-c-check">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="bulk-edit-table-example-row-2-check-2"
                  name="bulk-edit-table-example-row-2-example-check-2"
                />

                <label
                  class="pf-c-check__label"
                  for="bulk-edit-table-example-row-2-check-2"
                >Check 2</label>
              </div>
            </div>
          </div>
        </td>
        <td role="cell" data-label="Radios">
          <div class="pf-c-inline-edit__value">Radio 1, Radio 2</div>
          <div
            class="pf-c-inline-edit__group pf-m-column"
            role="radiogroup"
            aria-label="Radio group example"
          >
            <div class="pf-c-inline-edit__input">
              <div class="pf-c-radio">
                <input
                  class="pf-c-radio__input"
                  type="radio"
                  id="bulk-edit-table-example-row-2-radio-1"
                  name="bulk-edit-table-example-row-2-example-radio-1"
                />

                <label
                  class="pf-c-radio__label"
                  for="bulk-edit-table-example-row-2-radio-1"
                >Radio 1</label>
              </div>
            </div>
            <div class="pf-c-inline-edit__input">
              <div class="pf-c-radio">
                <input
                  class="pf-c-radio__input"
                  type="radio"
                  id="bulk-edit-table-example-row-2-radio-2"
                  name="bulk-edit-table-example-row-2-example-radio-2"
                />

                <label
                  class="pf-c-radio__label"
                  for="bulk-edit-table-example-row-2-radio-2"
                >Radio 2</label>
              </div>
            </div>
          </div>
        </td>
        <td role="cell" data-label="Number">
          <div class="pf-c-inline-edit__value">2</div>
          <div class="pf-c-inline-edit__input">
            <input
              class="pf-c-form-control"
              type="number"
              value="2"
              id="bulk-edit-table-example-row-2-number-input"
              aria-label="Number input"
            />
          </div>
        </td>
        <td class="pf-c-table__inline-edit-action" role="cell">
          <div
            class="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group"
          >
            <div class="pf-c-inline-edit__action pf-m-valid">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Save edits"
              >
                <i class="fas fa-check" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-inline-edit__action">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Cancel edits"
              >
                <i class="fas fa-times" aria-hidden="true"></i>
              </button>
            </div>
          </div>
          <div class="pf-c-inline-edit__action pf-m-enable-editable">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              id="bulk-edit-table-example-row-2-edit-button"
              aria-label="Edit"
              aria-labelledby="bulk-edit-table-example-label bulk-edit-table-example-row-2-edit-button"
            >
              <i class="fas fa-pencil-alt" aria-hidden="true"></i>
            </button>
          </div>
        </td>
        <td class="pf-c-table__action" role="cell">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="inline-edit-table-row-example-row-2--dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="inline-edit-table-row-example-row-2--dropdown-kebab-button"
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
</form>

```

## Documentation

### Accessibility

All accessibility requirements for inputs apply to elements within inline edit.

| Attribute                                                  | Applied to                           | Outcome                                                            |
| ---------------------------------------------------------- | ------------------------------------ | ------------------------------------------------------------------ |
| `id`                                                       | `[labelling element]`                | Provides a reference for toggle button and/or editable content.    |
| `aria-label="descriptive text"`                            | `.pf-c-inline-edit__toggle > button` | Provides an accessible description for toggle button. **Required** |
| `aria-labelledby="[labelling element] [toggle button id]"` | `.pf-c-inline-edit__toggle > button` | Provides an accessible description for toggle button. **Required** |
| `aria-label="descriptive text"`                            | `[radio button group]`               | Provides an accessible description for radio groups. **Required**  |
| `contenteditable="true"`                                   | `.pf-c-inline-edit__editable-text`   | Ensures the text node is editable.                                 |
| `role="textbox"`                                           | `.pf-c-inline-edit__editable-text`   | Identifies an element that allows the input of free-form text.     |
| `role="radiogroup"`                                        | `[radio button group]`               | Provides an accessible role for radio buttons groups. **Required** |

### Usage

| Class                              | Applied to                                                     | Outcome                                                                                   |
| ---------------------------------- | -------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `.pf-c-inline-edit`                | `<form>`, `<div>`                                              | Initiates the inline edit component. **Required**                                         |
| `.pf-c-inline-edit__value`         | `*`                                                            | Initiates an inline edit value. **Required**                                              |
| `.pf-c-inline-edit__input`         | `*`                                                            | Initiates an inline edit input. **Required**                                              |
| `.pf-c-inline-edit__editable-text` | `*`                                                            | Initiates an inline editable text element.                                                |
| `.pf-c-inline-edit__label`         | `*`                                                            | Initiates an inline edit label.                                                           |
| `.pf-c-inline-edit__action`        | `*`                                                            | Initiates an inline edit action (visible when inline edit region is active). **Required** |
| `.pf-m-inline-editable`            | `.pf-c-inline-edit`, `.pf-c-inline-edit [block level element]` | Modifies an inline edit region for editable state.                                        |
| `.pf-m-action-group`               | `.pf-c-inline-edit__group`                                     | Modifies group for action group.                                                          |
| `.pf-m-icon-group`                 | `.pf-c-inline-edit__group`                                     | Modifies an action group item spacing.                                                    |
| `.pf-m-column`                     | `.pf-c-inline-edit__group`                                     | Modifies an action group flex direction.                                                  |
| `.pf-m-footer`                     | `.pf-c-inline-edit__group`                                     | Modifies an inline edit group margin-top.                                                 |
| `.pf-m-bold`                       | `.pf-c-inline-edit__label`                                     | Modifies an inline edit label's `font-weight`.                                            |
| `.pf-m-valid`                      | `.pf-c-inline-edit__action`                                    | Modifies the action button state.                                                         |
| `.pf-m-enable-editable`            | `.pf-c-inline-edit__action`                                    | Exposes an inline edit action by default.                                                 |

<!--
### Bulk edit dl (default)
```hbs
{{#> inline-edit inline-edit--type="form" inline-edit--id="bulk-edit-dl-example-default"}}
  {{#> inline-edit-dl}}{{/inline-edit-dl}}
{{/inline-edit}}
```

### Bulk edit dl (active)
```hbs
{{#> inline-edit inline-edit--type="form" inline-edit--id="bulk-edit-dl-example-active" inline-edit--modifier="pf-m-inline-editable"}}
  {{#> inline-edit-dl}}{{/inline-edit-dl}}
{{/inline-edit}}
```

### Inline edit dl example
```hbs
{{#> list list--type="dl" list--modifier="pf-m-2-col" list--attribute=(concat 'aria-label="Inline edit description list example"')}}
  <div>
    <dt>Name</dt>
    <dd>main</dd>
  </div>
  {{#> inline-edit inline-edit--id="inline-edit-dl-example-1"}}
    <dt>
      {{#> inline-edit-group}}
        {{#> inline-edit-label}}
          Description
        {{/inline-edit-label}}
        {{#> inline-edit-toggle}}
          {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' inline-edit--id '-edit-button" aria-label="Edit" aria-labelledby="' inline-edit--id '-edit-button"')}}
            <i class="fas fa-pencil-alt" aria-hidden="true"></i>
          {{/button}}
        {{/inline-edit-toggle}}
      {{/inline-edit-group}}
    </dt>
    <dd>
      {{#> inline-edit-value}}
        test cluster
      {{/inline-edit-value}}
      {{#> inline-edit-input}}
        {{> form-control controlType="input" input="true" form-control--attribute=(concat 'type="text" value="Text input description content" id="' inline-edit--id '-input"')}}
      {{/inline-edit-input}}
    </dd>
  {{/inline-edit}}

  {{#> inline-edit inline-edit--id="inline-edit-dl-example-2" inline-edit--modifier="pf-m-inline-editable"}}
    <dt>
      {{#> inline-edit-group}}
        {{#> inline-edit-label}}
          Description (editable)
        {{/inline-edit-label}}
        {{#> inline-edit-toggle}}
          {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' inline-edit--id '-edit-button" aria-label="Edit" aria-labelledby="' inline-edit--id '-label ' inline-edit--id '-edit-button"')}}
            <i class="fas fa-pencil-alt" aria-hidden="true"></i>
          {{/button}}
        {{/inline-edit-toggle}}
      {{/inline-edit-group}}
    </dt>
    <dd>
      {{#> inline-edit-value}}
        Text input description content
      {{/inline-edit-value}}
      {{#> inline-edit-input}}
        {{> form-control controlType="input" input="true" form-control--attribute=(concat 'type="text" value="Text input description content" id="' inline-edit--id '-input"')}}
      {{/inline-edit-input}}
    </dd>
  {{/inline-edit}}

  <div>
    <dt>Tags</dt>
    <dd>
      {{#> chip chip--type="div" chip--modifier="pf-m-read-only"}}
        {{#> chip-text}}
          alertmanager=main
        {{/chip-text}}
      {{/chip}}
    </dd>
  </div>

  {{#> inline-edit inline-edit--id="inline-edit-dl-example-3"}}
    <dt>
      {{#> inline-edit-group}}
        {{#> inline-edit-label}}
          Value
        {{/inline-edit-label}}
        {{#> inline-edit-toggle}}
          {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' inline-edit--id '-edit-button" aria-label="Edit" aria-labelledby="' inline-edit--id '-label ' inline-edit--id '-edit-button"')}}
            <i class="fas fa-pencil-alt" aria-hidden="true"></i>
          {{/button}}
        {{/inline-edit-toggle}}
      {{/inline-edit-group}}
    </dt>
    <dd>
      {{#> inline-edit-value}}
        True
      {{/inline-edit-value}}
      {{#> inline-edit-group inline-edit-group--attribute='role="radiogroup" aria-label="Radio group example"' inline-edit-group--modifier="pf-m-column"}}
        {{#> inline-edit-input}}
          {{#> radio}}
            {{#> radio-input radio-input--attribute=(concat 'id="' inline-edit--id '-radio1" name="' inline-edit--id '-input"')}}{{/radio-input}}
            {{#> radio-label radio-label--attribute=(concat 'for="' inline-edit--id '-radio1"')}}True{{/radio-label}}
          {{/radio}}
        {{/inline-edit-input}}
        {{#> inline-edit-input}}
          {{#> radio}}
            {{#> radio-input radio-input--attribute=(concat 'id="' inline-edit--id '-radio2" name="' inline-edit--id '-input"')}}{{/radio-input}}
            {{#> radio-label radio-label--attribute=(concat 'for="' inline-edit--id '-radio2"')}}False{{/radio-label}}
          {{/radio}}
        {{/inline-edit-input}}
      {{/inline-edit-group}}
    </dd>
  {{/inline-edit}}

  {{#> inline-edit inline-edit--id="inline-edit-dl-example-4" inline-edit--modifier="pf-m-inline-editable"}}
    <dt>
      {{#> inline-edit-group}}
        {{#> inline-edit-label}}
          Value
        {{/inline-edit-label}}
        {{#> inline-edit-toggle}}
          {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' inline-edit--id '-edit-button" aria-label="Edit" aria-labelledby="' inline-edit--id '-label ' inline-edit--id '-edit-button"')}}
            <i class="fas fa-pencil-alt" aria-hidden="true"></i>
          {{/button}}
        {{/inline-edit-toggle}}
      {{/inline-edit-group}}
    </dt>
    <dd>
      {{#> inline-edit-value}}
        True
      {{/inline-edit-value}}
      {{#> inline-edit-group inline-edit-group--attribute='role="radiogroup" aria-label="Radio group example"' inline-edit-group--modifier="pf-m-column"}}
        {{#> inline-edit-input}}
          {{#> radio}}
            {{#> radio-input radio-input--attribute=(concat 'id="' inline-edit--id '-radio1" name="' inline-edit--id '-input"')}}{{/radio-input}}
            {{#> radio-label radio-label--attribute=(concat 'for="' inline-edit--id '-radio1"')}}True{{/radio-label}}
          {{/radio}}
        {{/inline-edit-input}}
        {{#> inline-edit-input}}
          {{#> radio}}
            {{#> radio-input radio-input--attribute=(concat 'id="' inline-edit--id '-radio2" name="' inline-edit--id '-input"')}}{{/radio-input}}
            {{#> radio-label radio-label--attribute=(concat 'for="' inline-edit--id '-radio2"')}}False{{/radio-label}}
          {{/radio}}
        {{/inline-edit-input}}
      {{/inline-edit-group}}
    </dd>
  {{/inline-edit}}
{{/list}}
```
-->
