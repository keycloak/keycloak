---
id: Form
section: components
---## Demos

### Basic

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-demo-basic-name">
        <span class="pf-c-form__label-text">Full name</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        required
        type="text"
        id="form-demo-basic-name"
        name="form-demo-basic-name"
        aria-describedby="form-demo-basic-name-helper"
      />

      <p
        class="pf-c-form__helper-text"
        id="form-demo-basic-name-helper"
        aria-live="polite"
      >Include your middle name if you have one.</p>
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-demo-basic-email">
        <span class="pf-c-form__label-text">Email</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="email"
        id="form-demo-basic-email"
        name="form-demo-basic-email"
      />
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-demo-basic-phone">
        <span class="pf-c-form__label-text">Phone number</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>

      <button class="pf-c-form__group-label-help" aria-label="More info">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        required
        type="tel"
        placeholder="555-555-5555"
        id="form-demo-basic-phone"
        name="form-demo-basic-phone"
      />
    </div>
  </div>
  <div
    class="pf-c-form__group"
    role="group"
    aria-labelledby="form-demo-basic-contact-legend"
  >
    <div class="pf-c-form__group-label" id="form-demo-basic-contact-legend">
      <span class="pf-c-form__label">
        <span class="pf-c-form__label-text">How can we contact you?</span>
      </span>
    </div>
    <div class="pf-c-form__group-control pf-m-inline">
      <div class="pf-c-check">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="form-demo-basic-contact-check-1"
          name="form-demo-basic-contact-check-1"
        />

        <label
          class="pf-c-check__label"
          for="form-demo-basic-contact-check-1"
        >Email</label>
      </div>
      <div class="pf-c-check">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="form-demo-basic-contact-check-2"
          name="form-demo-basic-contact-check-2"
        />

        <label
          class="pf-c-check__label"
          for="form-demo-basic-contact-check-2"
        >Phone</label>
      </div>
      <div class="pf-c-check">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="form-demo-basic-contact-check-3"
          name="form-demo-basic-contact-check-3"
        />

        <label
          class="pf-c-check__label"
          for="form-demo-basic-contact-check-3"
        >Mail</label>
      </div>
    </div>
  </div>
  <div
    class="pf-c-form__group"
    role="radiogroup"
    aria-labelledby="form-demo-basic-time-zone-legend"
  >
    <div class="pf-c-form__group-label" id="form-demo-basic-time-zone-legend">
      <span class="pf-c-form__label">
        <span class="pf-c-form__label-text">Time zone</span>
      </span>
    </div>
    <div class="pf-c-form__group-control pf-m-inline">
      <div class="pf-c-radio">
        <input
          class="pf-c-radio__input"
          type="radio"
          id="form-demo-basic-time-zone-radio-1"
          name="form-demo-basic-time-zone-radio"
        />

        <label
          class="pf-c-radio__label"
          for="form-demo-basic-time-zone-radio-1"
        >Eastern</label>
      </div>
      <div class="pf-c-radio">
        <input
          class="pf-c-radio__input"
          type="radio"
          id="form-demo-basic-time-zone-radio-2"
          name="form-demo-basic-time-zone-radio"
        />

        <label
          class="pf-c-radio__label"
          for="form-demo-basic-time-zone-radio-2"
        >Central</label>
      </div>
      <div class="pf-c-radio">
        <input
          class="pf-c-radio__input"
          type="radio"
          id="form-demo-basic-time-zone-radio-3"
          name="form-demo-basic-time-zone-radio"
        />

        <label
          class="pf-c-radio__label"
          for="form-demo-basic-time-zone-radio-3"
        >Pacific</label>
      </div>
    </div>
  </div>
  <div class="pf-c-form__group pf-m-action">
    <div class="pf-c-form__group-control">
      <div class="pf-c-form__actions">
        <button class="pf-c-button pf-m-primary" type="submit">Submit</button>
        <button class="pf-c-button pf-m-link" type="button">Cancel</button>
      </div>
    </div>
  </div>
</form>

```

### Horizontal

```html
<form novalidate class="pf-c-form pf-m-horizontal">
  <div class="pf-c-form__group -name">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-demo-horizontal">
        <span class="pf-c-form__label-text">Full name</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        required
        type="text"
        id="form-demo-horizontal"
        name="form-demo-horizontal"
        aria-describedby="form-demo-horizontal-helper"
      />
      <p
        class="pf-c-form__helper-text"
        id="form-demo-horizontal-helper"
        aria-live="polite"
      >Include your middle name if you have one.</p>
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-demo-horizontal-email">
        <span class="pf-c-form__label-text">Email</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="email"
        id="form-demo-horizontal-email"
        name="form-demo-horizontal-email"
      />
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="form-demo-horizontal-phone">
        <span class="pf-c-form__label-text">Phone number</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="tel"
        placeholder="Example, (555) 555-5555"
        id="form-demo-horizontal-phone"
        name="form-demo-horizontal-phone"
      />
    </div>
  </div>
  <div
    class="pf-c-form__group"
    role="group"
    aria-labelledby="form-demo-horizontal-contact-legend"
  >
    <div
      class="pf-c-form__group-label pf-m-no-padding-top"
      id="form-demo-horizontal-contact-legend"
    >
      <span class="pf-c-form__label">
        <span class="pf-c-form__label-text">How can we contact you?</span>
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
          id="form-demo-horizontal-contact1"
          name="form-demo-horizontal-contact1"
        />

        <label
          class="pf-c-check__label"
          for="form-demo-horizontal-contact1"
        >Email</label>
      </div>
      <div class="pf-c-check">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="form-demo-horizontal-contact2"
          name="form-demo-horizontal-contact2"
        />

        <label
          class="pf-c-check__label"
          for="form-demo-horizontal-contact2"
        >Phone</label>
      </div>
      <div class="pf-c-check">
        <input
          class="pf-c-check__input"
          type="checkbox"
          id="form-demo-horizontal-contact3"
          name="form-demo-horizontal-contact3"
        />

        <label
          class="pf-c-check__label"
          for="form-demo-horizontal-contact3"
        >Mail</label>
      </div>
    </div>
  </div>
  <div class="pf-c-form__group pf-m-action">
    <div class="pf-c-form__group-control">
      <div class="pf-c-form__actions">
        <button class="pf-c-button pf-m-primary" type="submit">Submit</button>
        <button class="pf-c-button pf-m-link" type="button">Cancel</button>
      </div>
    </div>
  </div>
</form>

```

### Grid

```html
<form novalidate class="pf-c-form">
  <div class="pf-l-grid pf-m-all-6-col-on-md pf-m-gutter">
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label class="pf-c-form__label" for="form-demo-grid-name">
          <span class="pf-c-form__label-text">Full name</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="form-demo-grid-name"
          name="form-demo-grid-name"
          aria-describedby="form-demo-grid-name-helper"
        />

        <p
          class="pf-c-form__helper-text"
          id="form-demo-grid-name-helper"
          aria-live="polite"
        >Include your middle name if you have one.</p>
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label class="pf-c-form__label" for="form-demo-grid-title">
          <span class="pf-c-form__label-text">Job title</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="form-demo-grid-title"
          name="form-demo-grid-title"
        />
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label class="pf-c-form__label" for="form-demo-grid-phone">
          <span class="pf-c-form__label-text">Phone number</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          type="tel"
          id="form-demo-grid-phone"
          name="form-demo-grid-phone"
          placeholder="555-555-5555"
        />
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label class="pf-c-form__label" for="form-demo-grid-email">
          <span class="pf-c-form__label-text">Email</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          type="email"
          id="form-demo-grid-email"
          name="form-demo-grid-email"
        />
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label class="pf-c-form__label" for="form-demo-grid-address">
          <span class="pf-c-form__label-text">Street address</span>
        </label>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          type="text"
          id="form-demo-grid-address"
          name="form-demo-grid-address"
        />
      </div>
    </div>
    <div class="pf-l-grid pf-m-all-6-col pf-m-gutter">
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label class="pf-c-form__label" for="form-demo-grid-city">
            <span class="pf-c-form__label-text">City</span>
          </label>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            id="form-demo-grid-city"
            name="form-demo-grid-city"
          />
        </div>
      </div>
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label class="pf-c-form__label" for="form-demo-grid-state">
            <span class="pf-c-form__label-text">State</span>
          </label>
        </div>
        <div class="pf-c-form__group-control">
          <select
            class="pf-c-form-control"
            id="form-demo-grid-state"
            name="form-demo-grid-state"
          >
            <option value selected>Select one</option>
            <option value="AL">Alabama</option>
            <option value="AK">Alaska</option>
            <option value="AZ">Arizona</option>
            <option value="AR">Arkansas</option>
            <option value="CA">California</option>
            <option value="CO">Colorado</option>
            <option value="CT">Connecticut</option>
            <option value="DE">Delaware</option>
            <option value="FL">Florida</option>
            <option value="GA">Georgia</option>
            <option value="HI">Hawaii</option>
            <option value="ID">Idaho</option>
            <option value="IL">Illinois</option>
            <option value="IN">Indiana</option>
            <option value="IA">Iowa</option>
            <option value="KS">Kansas</option>
            <option value="KY">Kentucky</option>
            <option value="LA">Louisiana</option>
            <option value="ME">Maine</option>
            <option value="MD">Maryland</option>
            <option value="MA">Massachusetts</option>
            <option value="MI">Michigan</option>
            <option value="MN">Minnesota</option>
            <option value="MS">Mississippi</option>
            <option value="MO">Missouri</option>
            <option value="MT">Montana</option>
            <option value="NE">Nebraska</option>
            <option value="NV">Nevada</option>
            <option value="NH">New Hampshire</option>
            <option value="NJ">New Jersey</option>
            <option value="NM">New Mexico</option>
            <option value="NY">New York</option>
            <option value="NC">North Carolina</option>
            <option value="ND">North Dakota</option>
            <option value="OH">Ohio</option>
            <option value="OK">Oklahoma</option>
            <option value="OR">Oregon</option>
            <option value="PA">Pennsylvania</option>
            <option value="RI">Rhode Island</option>
            <option value="SC">South Carolina</option>
            <option value="SD">South Dakota</option>
            <option value="TN">Tennessee</option>
            <option value="TX">Texas</option>
            <option value="UT">Utah</option>
            <option value="VT">Vermont</option>
            <option value="VA">Virginia</option>
            <option value="WA">Washington</option>
            <option value="WV">West Virginia</option>
            <option value="WI">Wisconsin</option>
            <option value="WY">Wyoming</option>
          </select>
        </div>
      </div>
    </div>
    <div class="pf-c-form__group pf-m-action">
      <div class="pf-c-form__group-control">
        <div class="pf-c-form__actions">
          <button class="pf-c-button pf-m-primary" type="submit">Submit</button>
          <button class="pf-c-button pf-m-link" type="button">Cancel</button>
        </div>
      </div>
    </div>
  </div>
</form>

```

### Sections with repeatable fields

```html
<form novalidate class="pf-c-form">
  <section
    class="pf-c-form__section"
    role="group"
    aria-labelledby="form-demo-sections-repeatable-fields-section1-title"
  >
    <div
      class="pf-c-form__section-title"
      id="form-demo-sections-repeatable-fields-section1-title"
      aria-hidden="true"
    >General settings</div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-demo-sections-repeatable-fields-clientid"
        >
          <span class="pf-c-form__label-text">Client ID</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button class="pf-c-form__group-label-help" aria-label="More info">
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="form-demo-sections-repeatable-fields-clientid"
          name="form-demo-sections-repeatable-fields-clientid"
        />
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-demo-sections-repeatable-fields-name"
        >
          <span class="pf-c-form__label-text">Full name</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button class="pf-c-form__group-label-help" aria-label="More info">
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="form-demo-sections-repeatable-fields-name"
          name="form-demo-sections-repeatable-fields-name"
        />
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-demo-sections-repeatable-fields-description"
        >
          <span class="pf-c-form__label-text">Description</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button class="pf-c-form__group-label-help" aria-label="More info">
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="form-demo-sections-repeatable-fields-description"
          name="form-demo-sections-repeatable-fields-description"
        />
      </div>
    </div>
  </section>
  <section
    class="pf-c-form__section"
    role="group"
    aria-labelledby="form-demo-sections-repeatable-fields-section2-title"
  >
    <div
      class="pf-c-form__section-title"
      id="form-demo-sections-repeatable-fields-section2-title"
      aria-hidden="true"
    >Access settings</div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-demo-sections-repeatable-fields-rooturl"
        >
          <span class="pf-c-form__label-text">Root URL</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button class="pf-c-form__group-label-help" aria-label="More info">
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="form-demo-sections-repeatable-fields-rooturl"
          name="form-demo-sections-repeatable-fields-rooturl"
        />
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          id="form-demo-sections-repeatable-fields-uris"
        >
          <span class="pf-c-form__label-text">Valid redirect URIs</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button class="pf-c-form__group-label-help" aria-label="More info">
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-control pf-m-stack">
        <div class="pf-c-input-group">
          <input
            class="pf-c-form-control"
            required
            type="text"
            id="form-demo-sections-repeatable-fields-uris-input-1"
            name="form-demo-sections-repeatable-fields-uris-input-1"
            aria-labelledby="form-demo-sections-repeatable-fields-uris form-demo-sections-repeatable-fields-uris-input-1"
          />

          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Remove"
          >
            <i class="fas fa-minus-circle" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-input-group">
          <input
            class="pf-c-form-control"
            required
            type="text"
            id="form-demo-sections-repeatable-fields-uris-input-2"
            name="form-demo-sections-repeatable-fields-uris-input-2"
            aria-labelledby="form-demo-sections-repeatable-fields-uris form-demo-sections-repeatable-fields-uris-input-2"
          />

          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Remove"
          >
            <i class="fas fa-minus-circle" aria-hidden="true"></i>
          </button>
        </div>
        <div class="pf-c-input-group">
          <input
            class="pf-c-form-control"
            required
            type="text"
            id="form-demo-sections-repeatable-fields-uris-input-3"
            name="form-demo-sections-repeatable-fields-uris-input-3"
            aria-labelledby="form-demo-sections-repeatable-fields-uris form-demo-sections-repeatable-fields-uris-input-3"
          />

          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Remove"
          >
            <i class="fas fa-minus-circle" aria-hidden="true"></i>
          </button>
        </div>
        <button class="pf-c-button pf-m-link pf-m-inline" type="button">
          <span class="pf-c-button__icon pf-m-start">
            <i class="fas fa-plus-circle" aria-hidden="true"></i>
          </span>
          Add valid redirect URI
        </button>
      </div>
    </div>
    <div class="pf-c-form__group">
      <div class="pf-c-form__group-label">
        <label
          class="pf-c-form__label"
          for="form-demo-sections-repeatable-fields-home-url"
        >
          <span class="pf-c-form__label-text">Home URL</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button class="pf-c-form__group-label-help" aria-label="More info">
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-control">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="form-demo-sections-repeatable-fields-home-url"
          name="form-demo-sections-repeatable-fields-home-url"
        />
      </div>
    </div>
  </section>
</form>

```
