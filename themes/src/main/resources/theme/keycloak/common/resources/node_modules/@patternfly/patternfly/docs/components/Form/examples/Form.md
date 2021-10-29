---
id: Form
section: components
cssPrefix: pf-c-form
---## Examples

### Vertically aligned labels

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-vertical-name">
        <span class="pf-c-form__label-text">Name</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
      <button class="pf-c-form__group-label-help" aria-label="More info">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="form-vertical-name"
        name="form-vertical-name"
        required
      />
    </div>
  </div>
</form>

```

### Horizontally aligned labels

```html
<form novalidate class="pf-c-form pf-m-horizontal">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-horizontal-name">
        <span class="pf-c-form__label-text">Name</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="form-horizontal-name"
        name="form-horizontal-name"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-horizontal-info">
        <span class="pf-c-form__label-text">Information</span>
      </label>
      <button class="pf-c-form__group-label-help" aria-label="More info">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <textarea
        class="pf-c-form-control"
        type="text"
        id="form-horizontal-info"
        name="form-horizontal-info"
        aria-label="Textarea example"
      ></textarea>
    </div>
  </div>
  <div
    class="pf-c-form__group"
    role="group"
    aria-labelledby="form-horizontal-checkbox-legend"
  >
    <div
      class="pf-c-form__group-label pf-m-no-padding-top"
      id="form-horizontal-checkbox-legend"
    >
      <span class="pf-c-form__label">
        <span class="pf-c-form__label-text">Label (no top padding)</span>
      </span>
      <button class="pf-c-form__group-label-help" aria-label="More info">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control pf-m-stack">
      <div class="pf-c-check">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="form-horizontal-checkbox"
          name="form-horizontal-checkbox"
        />

        <label class="pf-c-check__label" for="form-horizontal-checkbox">Option 1</label>
      </div>
      <div class="pf-c-check">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="form-horizontal-checkbox2"
          name="form-horizontal-checkbox2"
        />

        <label
          class="pf-c-check__label"
          for="form-horizontal-checkbox2"
        >Option 2</label>
      </div>
    </div>
  </div>
</form>

```

### Form sections

```html
<form novalidate class="pf-c-form">
  <section class="pf-c-form__section" role="group">
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-section-example-form-section-1-input"
        >
          <span class="pf-c-form__label-text">Form section 1 inputs</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          type="text"
          id="form-section-example-form-section-1-input"
          name="form-section-example-form-section-1-input"
          required
        />
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-section-example-form-section-1-input-2"
        >
          <span class="pf-c-form__label-text">Form section 1 inputs</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          type="text"
          id="form-section-example-form-section-1-input-2"
          name="form-section-example-form-section-1-input-2"
          required
        />
      </div>
    </div>
  </section>
  <section
    class="pf-c-form__section"
    role="group"
    aria-labelledby="form-section-example-section2-title"
  >
    <div
      class="pf-c-form__section-title"
      id="form-section-example-section2-title"
      aria-hidden="true"
    >Section 2 title (optional)</div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-section-example-form-section-2-input"
        >
          <span class="pf-c-form__label-text">Form section 2 inputs</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          type="text"
          id="form-section-example-form-section-2-input"
          name="form-section-example-form-section-2-input"
          required
        />
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-section-example-form-section-2-input-2"
        >
          <span class="pf-c-form__label-text">Form section 2 inputs</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          type="text"
          id="form-section-example-form-section-2-input-2"
          name="form-section-example-form-section-2-input-2"
          required
        />
      </div>
    </div>
  </section>
</form>

```

### Help text

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-help-text-name">
        <span class="pf-c-form__label-text">Name</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        required
        type="text"
        id="form-help-text-name"
        name="form-help-text-name"
        aria-describedby="form-help-text-name-helper"
      />
      <p
        class="pf-c-form__helper-text"
        id="form-help-text-name-helper"
        aria-live="polite"
      >This is helper text.</p>
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-help-text-email">
        <span class="pf-c-form__label-text">E-mail</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control pf-m-warning"
        required
        type="text"
        id="form-help-text-email"
        name="form-help-text-email"
        aria-describedby="form-help-text-email-helper"
      />
      <p
        class="pf-c-form__helper-text pf-m-warning"
        id="form-help-text-email-helper"
        aria-live="polite"
      >This is helper text for a warning input.</p>
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-help-text-address">
        <span class="pf-c-form__label-text">Address</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        required
        type="text"
        id="form-help-text-address"
        name="form-help-text-address"
        aria-invalid="true"
        aria-describedby="form-help-text-address-helper"
      />
      <p
        class="pf-c-form__helper-text pf-m-error"
        id="form-help-text-address-helper"
        aria-live="polite"
      >This is helper text for an invalid input.</p>
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-help-text-comment">
        <span class="pf-c-form__label-text">Comment</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control pf-m-success"
        value="This is a valid comment"
        type="text"
        id="form-help-text-comment"
        name="form-help-text-comment"
        aria-describedby="form-help-text-comment-helper"
      />
      <p
        class="pf-c-form__helper-text pf-m-success"
        id="form-help-text-comment-helper"
        aria-live="polite"
      >This is helper text for success input.</p>
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-help-textinfo">
        <span class="pf-c-form__label-text">Information</span>
      </label>
    </div>
    <textarea
      class="pf-c-form-control"
      id="form-help-textinfo"
      name="form-help-textinfo"
      aria-invalid="true"
      aria-describedby="form-help-textinfo-helper"
    ></textarea>
    <p
      class="pf-c-form__helper-text pf-m-error"
      id="form-help-textinfo-helper"
      aria-live="polite"
    >
      <span class="pf-c-form__helper-text-icon">
        <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
      </span>
      This is helper text with an icon.
    </p>
  </div>
</form>

```

### Label with additional info

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label pf-m-info">
      <div class="pf-c-form__group-label-main">
        <label class="pf-c-form__label" for="form-additional-info-name">
          <span class="pf-c-form__label-text">Name</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>
        <button class="pf-c-form__group-label-help" aria-label="More info">
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-label-info">info</div>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="form-additional-info-name"
        name="form-additional-info-name"
        required
      />
    </div>
  </div>
</form>

```

### Action group

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group pf-m-action">
    <div class="pf-c-form__actions">
      <button class="pf-c-button pf-m-primary" type="submit">Submit form</button>
      <button class="pf-c-button pf-m-link" type="reset">Reset form</button>
    </div>
  </div>
</form>

```

### Field groups

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-expandable-field-groups-label1">
        <span class="pf-c-form__label-text">Label 1</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>

      <button class="pf-c-form__group-label-help" aria-label="More info">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="form-expandable-field-groups-label1"
        name="form-expandable-field-groups-label1"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-expandable-field-groups-label2">
        <span class="pf-c-form__label-text">Label 2</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>

      <button class="pf-c-form__group-label-help" aria-label="More info">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="form-expandable-field-groups-label2"
        name="form-expandable-field-groups-label2"
        required
      />
    </div>
  </div>
  <div
    class="pf-c-form__field-group pf-m-expanded"
    role="group"
    aria-labelledby="form-expandable-field-groups-field-group1-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="true"
          aria-label="Details"
          id="form-expandable-field-groups-field-group1-toggle"
          aria-labelledby="form-expandable-field-groups-field-group1-title form-expandable-field-groups-field-group1-toggle"
        >
          <span class="pf-c-form__field-group-toggle-icon">
            <i class="fas fa-angle-right" aria-hidden="true"></i>
          </span>
        </button>
      </div>
    </div>
    <div class="pf-c-form__field-group-header">
      <div class="pf-c-form__field-group-header-main">
        <div class="pf-c-form__field-group-header-title">
          <div
            class="pf-c-form__field-group-header-title-text"
            id="form-expandable-field-groups-field-group1-title"
          >Field group 1</div>
        </div>
        <div
          class="pf-c-form__field-group-header-description"
        >Field group 1 description text.</div>
      </div>
      <div class="pf-c-form__field-group-header-actions">
        <button class="pf-c-button pf-m-link" type="button">Delete all</button>
        <button class="pf-c-button pf-m-secondary" type="button">Add parameter</button>
      </div>
    </div>
    <div class="pf-c-form__field-group-body">
      <div
        class="pf-c-form__field-group pf-m-expanded"
        role="group"
        aria-labelledby="form-expandable-field-groups-field-group2-title"
      >
        <div class="pf-c-form__field-group-toggle">
          <div class="pf-c-form__field-group-toggle-button">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-expanded="true"
              aria-label="Details"
              id="form-expandable-field-groups-field-group2-toggle"
              aria-labelledby="form-expandable-field-groups-field-group2-title form-expandable-field-groups-field-group2-toggle"
            >
              <span class="pf-c-form__field-group-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </button>
          </div>
        </div>
        <div class="pf-c-form__field-group-header">
          <div class="pf-c-form__field-group-header-main">
            <div class="pf-c-form__field-group-header-title">
              <div
                class="pf-c-form__field-group-header-title-text"
                id="form-expandable-field-groups-field-group2-title"
              >Nested field group 1</div>
            </div>
            <div
              class="pf-c-form__field-group-header-description"
            >Nested field group 1 description text.</div>
          </div>
          <div class="pf-c-form__field-group-header-actions">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Remove"
            >
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>
        <div class="pf-c-form__field-group-body">
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="form-expandable-field-groupsform-expandable-field-groups-field-group2-label1"
              >
                <span class="pf-c-form__label-text">Label 1</span>
                <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
              </label>

              <button
                class="pf-c-form__group-label-help"
                aria-label="More info"
              >
                <i class="pficon pf-icon-help" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                id="form-expandable-field-groupsform-expandable-field-groups-field-group2-label1"
                name="form-expandable-field-groupsform-expandable-field-groups-field-group2-label1"
                required
              />
            </div>
          </div>
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="form-expandable-field-groupsform-expandable-field-groups-field-group2-label2"
              >
                <span class="pf-c-form__label-text">Label 2</span>
                <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
              </label>

              <button
                class="pf-c-form__group-label-help"
                aria-label="More info"
              >
                <i class="pficon pf-icon-help" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                id="form-expandable-field-groupsform-expandable-field-groups-field-group2-label2"
                name="form-expandable-field-groupsform-expandable-field-groups-field-group2-label2"
                required
              />
            </div>
          </div>
        </div>
      </div>
      <div
        class="pf-c-form__field-group"
        role="group"
        aria-labelledby="form-expandable-field-groups-field-group3-title"
      >
        <div class="pf-c-form__field-group-toggle">
          <div class="pf-c-form__field-group-toggle-button">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-expanded="false"
              aria-label="Details"
              id="form-expandable-field-groups-field-group3-toggle"
              aria-labelledby="form-expandable-field-groups-field-group3-title form-expandable-field-groups-field-group3-toggle"
            >
              <span class="pf-c-form__field-group-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </button>
          </div>
        </div>
        <div class="pf-c-form__field-group-header">
          <div class="pf-c-form__field-group-header-main">
            <div class="pf-c-form__field-group-header-title">
              <div
                class="pf-c-form__field-group-header-title-text"
                id="form-expandable-field-groups-field-group3-title"
              >Nested field group 2</div>
            </div>
          </div>
          <div class="pf-c-form__field-group-header-actions">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Remove"
            >
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>
      </div>
      <div
        class="pf-c-form__field-group"
        role="group"
        aria-labelledby="form-expandable-field-groups-field-group4-title"
      >
        <div class="pf-c-form__field-group-toggle">
          <div class="pf-c-form__field-group-toggle-button">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-expanded="false"
              aria-label="Details"
              id="form-expandable-field-groups-field-group4-toggle"
              aria-labelledby="form-expandable-field-groups-field-group4-title form-expandable-field-groups-field-group4-toggle"
            >
              <span class="pf-c-form__field-group-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </button>
          </div>
        </div>
        <div class="pf-c-form__field-group-header">
          <div class="pf-c-form__field-group-header-main">
            <div class="pf-c-form__field-group-header-title">
              <div
                class="pf-c-form__field-group-header-title-text"
                id="form-expandable-field-groups-field-group4-title"
              >Nested field group 3</div>
            </div>
            <div
              class="pf-c-form__field-group-header-description"
            >Nested field group 3 description text.</div>
          </div>
          <div class="pf-c-form__field-group-header-actions">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Remove"
            >
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>
      </div>
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label
            class="pf-c-form__label"
            for="form-expandable-field-groupsform-expandable-field-groups-field-group1-label1"
          >
            <span class="pf-c-form__label-text">Label 1</span>
            <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
          </label>

          <button class="pf-c-form__group-label-help" aria-label="More info">
            <i class="pficon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            type="text"
            id="form-expandable-field-groupsform-expandable-field-groups-field-group1-label1"
            name="form-expandable-field-groupsform-expandable-field-groups-field-group1-label1"
            required
          />
        </div>
      </div>
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label
            class="pf-c-form__label"
            for="form-expandable-field-groupsform-expandable-field-groups-field-group1-label2"
          >
            <span class="pf-c-form__label-text">Label 2</span>
            <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
          </label>

          <button class="pf-c-form__group-label-help" aria-label="More info">
            <i class="pficon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            type="text"
            id="form-expandable-field-groupsform-expandable-field-groups-field-group1-label2"
            name="form-expandable-field-groupsform-expandable-field-groups-field-group1-label2"
            required
          />
        </div>
      </div>
    </div>
  </div>
  <div
    class="pf-c-form__field-group"
    role="group"
    aria-labelledby="form-expandable-field-groups-field-group5-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="false"
          aria-label="Details"
          id="form-expandable-field-groups-field-group5-toggle"
          aria-labelledby="form-expandable-field-groups-field-group5-title form-expandable-field-groups-field-group5-toggle"
        >
          <span class="pf-c-form__field-group-toggle-icon">
            <i class="fas fa-angle-right" aria-hidden="true"></i>
          </span>
        </button>
      </div>
    </div>
    <div class="pf-c-form__field-group-header">
      <div class="pf-c-form__field-group-header-main">
        <div class="pf-c-form__field-group-header-title">
          <div
            class="pf-c-form__field-group-header-title-text"
            id="form-expandable-field-groups-field-group5-title"
          >Field group 2</div>
        </div>
        <div
          class="pf-c-form__field-group-header-description"
        >Field group 1 description text.</div>
      </div>
      <div class="pf-c-form__field-group-header-actions">
        <button class="pf-c-button pf-m-link" type="button">Delete all</button>
        <button class="pf-c-button pf-m-secondary" type="button">Add parameter</button>
      </div>
    </div>
  </div>
  <div
    class="pf-c-form__field-group pf-m-expanded"
    role="group"
    aria-labelledby="form-expandable-field-groups-field-group6-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="true"
          aria-label="Details"
          id="form-expandable-field-groups-field-group6-toggle"
          aria-labelledby="form-expandable-field-groups-field-group6-title form-expandable-field-groups-field-group6-toggle"
        >
          <span class="pf-c-form__field-group-toggle-icon">
            <i class="fas fa-angle-right" aria-hidden="true"></i>
          </span>
        </button>
      </div>
    </div>
    <div class="pf-c-form__field-group-header">
      <div class="pf-c-form__field-group-header-main">
        <div class="pf-c-form__field-group-header-title">
          <div
            class="pf-c-form__field-group-header-title-text"
            id="form-expandable-field-groups-field-group6-title"
          >Field group 3</div>
        </div>
        <div
          class="pf-c-form__field-group-header-description"
        >Field group 1 description text.</div>
      </div>
    </div>
    <div class="pf-c-form__field-group-body">
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label
            class="pf-c-form__label"
            for="form-expandable-field-groupsform-expandable-field-groups-field-group6-label1"
          >
            <span class="pf-c-form__label-text">Label 1</span>
            <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
          </label>

          <button class="pf-c-form__group-label-help" aria-label="More info">
            <i class="pficon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            type="text"
            id="form-expandable-field-groupsform-expandable-field-groups-field-group6-label1"
            name="form-expandable-field-groupsform-expandable-field-groups-field-group6-label1"
            required
          />
        </div>
      </div>
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label
            class="pf-c-form__label"
            for="form-expandable-field-groupsform-expandable-field-groups-field-group6-label2"
          >
            <span class="pf-c-form__label-text">Label 2</span>
            <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
          </label>

          <button class="pf-c-form__group-label-help" aria-label="More info">
            <i class="pficon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            type="text"
            id="form-expandable-field-groupsform-expandable-field-groups-field-group6-label2"
            name="form-expandable-field-groupsform-expandable-field-groups-field-group6-label2"
            required
          />
        </div>
      </div>
      <div
        class="pf-c-form__field-group pf-m-expanded"
        role="group"
        aria-labelledby="form-expandable-field-groups-field-group7-title"
      >
        <div class="pf-c-form__field-group-header">
          <div class="pf-c-form__field-group-header-main">
            <div class="pf-c-form__field-group-header-title">
              <div
                class="pf-c-form__field-group-header-title-text"
                id="form-expandable-field-groups-field-group7-title"
              >Nested field group 1 (non-expandable)</div>
            </div>
          </div>
        </div>
        <div class="pf-c-form__field-group-body">
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="form-expandable-field-groupsform-expandable-field-groups-field-group7-label1"
              >
                <span class="pf-c-form__label-text">Label 1</span>
                <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
              </label>

              <button
                class="pf-c-form__group-label-help"
                aria-label="More info"
              >
                <i class="pficon pf-icon-help" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                id="form-expandable-field-groupsform-expandable-field-groups-field-group7-label1"
                name="form-expandable-field-groupsform-expandable-field-groups-field-group7-label1"
                required
              />
            </div>
          </div>
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="form-expandable-field-groupsform-expandable-field-groups-field-group7-label2"
              >
                <span class="pf-c-form__label-text">Label 2</span>
                <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
              </label>

              <button
                class="pf-c-form__group-label-help"
                aria-label="More info"
              >
                <i class="pficon pf-icon-help" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                id="form-expandable-field-groupsform-expandable-field-groups-field-group7-label2"
                name="form-expandable-field-groupsform-expandable-field-groups-field-group7-label2"
                required
              />
            </div>
          </div>
        </div>
      </div>
      <div
        class="pf-c-form__field-group pf-m-expanded"
        role="group"
        aria-labelledby="form-expandable-field-groups-field-group8-title"
      >
        <div class="pf-c-form__field-group-header">
          <div class="pf-c-form__field-group-header-main">
            <div class="pf-c-form__field-group-header-title">
              <div
                class="pf-c-form__field-group-header-title-text"
                id="form-expandable-field-groups-field-group8-title"
              >Nested field group 2 (non-expandable)</div>
            </div>
            <div
              class="pf-c-form__field-group-header-description"
            >Field group 1 description text.</div>
          </div>
        </div>
        <div class="pf-c-form__field-group-body">
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="form-expandable-field-groupsform-expandable-field-groups-field-group8-label1"
              >
                <span class="pf-c-form__label-text">Label 1</span>
                <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
              </label>

              <button
                class="pf-c-form__group-label-help"
                aria-label="More info"
              >
                <i class="pficon pf-icon-help" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                id="form-expandable-field-groupsform-expandable-field-groups-field-group8-label1"
                name="form-expandable-field-groupsform-expandable-field-groups-field-group8-label1"
                required
              />
            </div>
          </div>
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="form-expandable-field-groupsform-expandable-field-groups-field-group8-label2"
              >
                <span class="pf-c-form__label-text">Label 2</span>
                <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
              </label>

              <button
                class="pf-c-form__group-label-help"
                aria-label="More info"
              >
                <i class="pficon pf-icon-help" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-form__group-control">
              <input
                class="pf-c-form-control"
                type="text"
                id="form-expandable-field-groupsform-expandable-field-groups-field-group8-label2"
                name="form-expandable-field-groupsform-expandable-field-groups-field-group8-label2"
                required
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div
    class="pf-c-form__field-group"
    role="group"
    aria-labelledby="form-expandable-field-groups-field-group9-title"
  >
    <div class="pf-c-form__field-group-header">
      <div class="pf-c-form__field-group-header-main">
        <div class="pf-c-form__field-group-header-title">
          <div
            class="pf-c-form__field-group-header-title-text"
            id="form-expandable-field-groups-field-group9-title"
          >Field group 4 (non-expandable)</div>
        </div>
        <div
          class="pf-c-form__field-group-header-description"
        >Field group 1 description text.</div>
      </div>
      <div class="pf-c-form__field-group-header-actions">
        <button class="pf-c-button pf-m-link" type="button">Delete all</button>
        <button class="pf-c-button pf-m-secondary" type="button">Add parameter</button>
      </div>
    </div>
    <div class="pf-c-form__field-group-body" hidden>
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label
            class="pf-c-form__label"
            for="form-expandable-field-groupsform-expandable-field-groups-field-group9-label1"
          >
            <span class="pf-c-form__label-text">Label 1</span>
            <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
          </label>

          <button class="pf-c-form__group-label-help" aria-label="More info">
            <i class="pficon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            type="text"
            id="form-expandable-field-groupsform-expandable-field-groups-field-group9-label1"
            name="form-expandable-field-groupsform-expandable-field-groups-field-group9-label1"
            required
          />
        </div>
      </div>
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label
            class="pf-c-form__label"
            for="form-expandable-field-groupsform-expandable-field-groups-field-group9-label2"
          >
            <span class="pf-c-form__label-text">Label 2</span>
            <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
          </label>

          <button class="pf-c-form__group-label-help" aria-label="More info">
            <i class="pficon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            type="text"
            id="form-expandable-field-groupsform-expandable-field-groups-field-group9-label2"
            name="form-expandable-field-groupsform-expandable-field-groups-field-group9-label2"
            required
          />
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-expandable-field-groups-label3">
        <span class="pf-c-form__label-text">Label 3</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>

      <button class="pf-c-form__group-label-help" aria-label="More info">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="form-expandable-field-groups-label3"
        name="form-expandable-field-groups-label3"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-expandable-field-groups-label4">
        <span class="pf-c-form__label-text">Label 4</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>

      <button class="pf-c-form__group-label-help" aria-label="More info">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="form-expandable-field-groups-label4"
        name="form-expandable-field-groups-label4"
        required
      />
    </div>
  </div>
</form>

```

## Documentation

### Accessibility

| Attribute                                                 | Applied to                                                            | Outcome                                                                                                                                                                                                                                                                                                                                                                |                                                                                         |
| --------------------------------------------------------- | --------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `for`                                                     | `<label>`                                                             | Each `<label>` must have a `for` attribute that matches its form field id. **Required**                                                                                                                                                                                                                                                                                |                                                                                         |
| `id`                                                      | `<input type="radio/checkbox/text">`, `<select>`, `<textarea>`        | Each `<form>` field must have an `id` attribute that matches its label's `for` value. **Required**                                                                                                                                                                                                                                                                     |                                                                                         |
| `required`                                                | `<input>`, `<select>`, `<textarea>`                                   | Required fields must include these attributes.                                                                                                                                                                                                                                                                                                                         |                                                                                         |
| `id="{helper_text_id}"`                                   | `.pf-c-form__helper-text`                                             | Form fields with related `.pf-c-form__helper-text` require this attribute. Usage `<p class="pf-c-form__helper-text" id="{helper_text_id}">`.                                                                                                                                                                                                                           |                                                                                         |
| `aria-describedby="{helper_text_id}"`                     | `<input>`, `<select>`, `<textarea>`                                   | Form fields with related `.pf-c-form__helper-text` require this attribute. Usage `<input aria-describedby="{helper_text_id}">`.                                                                                                                                                                                                                                        |                                                                                         |
| `aria-invalid="true" aria-describedby="{helper_text_id}"` | `<input>`, `<select>`, `<textarea>`                                   | When form validation fails `aria-describedby` is used to communicate the error to the user. These attributes need to be handled with Javascript so that `aria-describedby` only references help text that explains the error, and so that `aria-invalid="true"` is only present when validation fails. For proper styling of errors `aria-invalid="true"` is required. |                                                                                         |
| `aria-hidden="true"`                                      | `.pf-c-form__label-required`                                          | Hides the required indicator from assistive technologies.                                                                                                                                                                                                                                                                                                              |                                                                                         |
| `role="group"`                                            | `.pf-c-form__group`, `.pf-c-form__section`, `.pf-c-form__field-group` | Provides group role for form groups, form sections, and form field groups. **Required for checkbox groups, form groups, form sections, and form field groups.**                                                                                                                                                                                                        |                                                                                         |
| `role="radiogroup"`                                       | `.pf-c-form__group`                                                   | Provides group role for radio input groups. **Required for radio input groups**                                                                                                                                                                                                                                                                                        |                                                                                         |
| `id`                                                      | `.pf-c-form__group-label`                                             | Generates an `id` for use in the `aria-labelledby` attribute in a checkbox or radio form group.                                                                                                                                                                                                                                                                        |                                                                                         |
| `id`                                                      | `.pf-c-form__field-group-title-text`                                  | Generates an `id` for use in the `aria-labelledby` attribute in an expandable field group's toggle button.                                                                                                                                                                                                                                                             |                                                                                         |
| `id`                                                      | `.pf-c-form__field-group-toggle-button > button`                      | Generates an `id` for use in the `aria-labelledby` attribute in an expandable field group's toggle button.                                                                                                                                                                                                                                                             |                                                                                         |
| `aria-labelledby="{label id}"`                            | `.pf-c-form__group`, `.pf-c-form__section`, `.pf-c-form__field-group` | Provides an accessible label for form groups, form sections, and form field groups.                                                                                                                                                                                                                                                                                    | **Required for form groups, form sections, and form field groups that contain labels.** |
| `aria-label`                                              | `.pf-c-form__field-group-toggle-button > button`                      | Provides an accessible label for the field group toggle button.                                                                                                                                                                                                                                                                                                        |                                                                                         |
| `aria-labelledby="{title id} {toggle button id}"`         | `.pf-c-form__field-group-toggle-button > button`                      | Provides an accessible label for the field group toggle button.                                                                                                                                                                                                                                                                                                        |                                                                                         |
| `aria-expanded="true/false"`                              | `.pf-c-form__field-group-toggle-button > button`                      | Indicates whether the field group body is visible or hidden.                                                                                                                                                                                                                                                                                                           |                                                                                         |

### Usage

| Class                                        | Applied to                                         | Outcome                                                                                                 |
| -------------------------------------------- | -------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `.pf-c-form`                                 | `<form>`                                           | Initiates a standard form. **Required**                                                                 |
| `.pf-c-form__section`                        | `<div>, <section>`                                 | Initiates a form section.                                                                               |
| `.pf-c-form__section-title`                  | `<h1>`,`<h2>`,`<h3>`,`<h4>`,`<h5>`,`<h6>`, `<div>` | Initiates a form section title.                                                                         |
| `.pf-c-form__group`                          | `<div>`                                            | Initiates a form group.                                                                                 |
| `.pf-c-form__group-label`                    | `<div>`                                            | Initiates a form group label.                                                                           |
| `.pf-c-form__label`                          | `<label>`, `<span>`                                | Initiates a form label. **Required**                                                                    |
| `.pf-c-form__label-text`                     | `<span>`                                           | Initiates a form label text. **Required**                                                               |
| `.pf-c-form__label-required`                 | `<span>`                                           | Initiates a form label required indicator.                                                              |
| `.pf-c-form__group-label-main`               | `<div>`                                            | Initiates a form group label main container.                                                            |
| `.pf-c-form__group-label-info`               | `<div>`                                            | Initiates a form group info label.                                                                      |
| `.pf-c-form__group-label-help`               | `<button>`                                         | Initiates a field level help button.                                                                    |
| `.pf-c-form__group-control`                  | `<div>`                                            | Initiates a form group control section.                                                                 |
| `.pf-c-form__actions`                        | `<div>`                                            | Iniates a row of actions.                                                                               |
| `.pf-c-form__helper-text`                    | `<p>`, `<div>`                                     | Initiates a form helper text block.                                                                     |
| `.pf-c-form__helper-text-icon`               | `<span>`                                           | Initiates a form helper text icon.                                                                      |
| `.pf-c-form__alert`                          | `<div>`                                            | Initiates the form alert container for inline alerts.                                                   |
| `.pf-c-form__field-group`                    | `<div>`                                            | Initiates a form field group.                                                                           |
| `.pf-c-form__field-group-toggle`             | `<div>`                                            | Initiates the form field group toggle.                                                                  |
| `.pf-c-form__field-group-toggle-button`      | `<div>`                                            | Initiates the form field group toggle button.                                                           |
| `.pf-c-form__field-group-toggle-icon`        | `<span>`                                           | Initiates the form field group toggle icon.                                                             |
| `.pf-c-form__field-group-header`             | `<div>`                                            | Initiates the form field group header.                                                                  |
| `.pf-c-form__field-group-header-main`        | `<div>`                                            | Initiates the form field group main section.                                                            |
| `.pf-c-form__field-group-header-title`       | `<div>`                                            | Initiates the form field group title.                                                                   |
| `.pf-c-form__field-group-header-title-text`  | `<div>`                                            | Initiates the form field group title text.                                                              |
| `.pf-c-form__field-group-header-description` | `<div>`                                            | Initiates the form field group description.                                                             |
| `.pf-c-form__field-group-header-actions`     | `<div>`                                            | Initiates the form field group actions container.                                                       |
| `.pf-c-form__field-group-body`               | `<div>`                                            | Initiates the form field group body.                                                                    |
| `.pf-m-horizontal`                           | `.pf-c-form`                                       | Modifies form for a horizontal layout.                                                                  |
| `.pf-m-info`                                 | `.pf-c-form__group-label`                          | Modifies the form group label to contain form group label info.                                         |
| `.pf-m-action`                               | `.pf-c-form__group`                                | Modifies form group margin-top.                                                                         |
| `.pf-m-success`                              | `.pf-c-form__helper-text`                          | Modifies text color of helper text for success state.                                                   |
| `.pf-m-warning`                              | `.pf-c-form__helper-text`                          | Modifies text color of helper text for warning state.                                                   |
| `.pf-m-error`                                | `.pf-c-form__helper-text`                          | Modifies text color of helper text for error state.                                                     |
| `.pf-m-inactive`                             | `.pf-c-form__helper-text`                          | Modifies display of helper text to none.                                                                |
| `.pf-m-disabled`                             | `.pf-c-form__label`                                | Modifies form label to show disabled state.                                                             |
| `.pf-m-no-padding-top`                       | `.pf-c-form__group-label`                          | Removes top padding from the label element for labels adjacent to an element that isn't a form control. |
| `.pf-m-inline`                               | `.pf-c-form__group-control`                        | Modifies form group children to be inline (this is primarily for radio buttons and checkboxes).         |
| `.pf-m-stack`                                | `.pf-c-form__group-control`                        | Modifies form group children to be stacked with space between children.                                 |
| `.pf-m-expanded`                             | `.pf-c-form__field-group`                          | Modifies an expandable field group for the expanded state.                                              |
