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

      <button
        class="pf-c-form__group-label-help"
        aria-label="More information for phone number field"
        aria-describedby="form-demo-basic-phone"
      >
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

      <button
        class="pf-c-form__group-label-help"
        aria-label="More information for contact field"
        aria-describedby="form-demo-horizontal-contact-legend"
      >
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

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for client id field"
          aria-describedby="form-demo-sections-repeatable-fields-clientid"
        >
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

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for full name field"
          aria-describedby="form-demo-sections-repeatable-fields-name"
        >
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

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for description field"
          aria-describedby="form-demo-sections-repeatable-fields-description"
        >
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

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for root URL field"
          aria-describedby="form-demo-sections-repeatable-fields-rooturl"
        >
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

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for valid redirect URIs field"
          aria-describedby="form-demo-sections-repeatable-fields-uris"
        >
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

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for home URL field"
          aria-describedby="form-demo-sections-repeatable-fields-home-url"
        >
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

### Complex form

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label
        class="pf-c-form__label"
        for="form-demo-sections-complex-formform-demo-sections-complex-form-name"
      >
        <span class="pf-c-form__label-text">Name</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>

      <button
        class="pf-c-form__group-label-help"
        aria-label="More information for name field"
        aria-describedby="form-demo-sections-complex-formform-demo-sections-complex-form-name"
      >
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="form-demo-sections-complex-formform-demo-sections-complex-form-name"
        name="form-demo-sections-complex-formform-demo-sections-complex-form-name"
        required
      />
    </div>
  </div>

  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label
        class="pf-c-form__label"
        for="form-demo-sections-complex-formform-demo-sections-complex-form-labels"
      >
        <span class="pf-c-form__label-text">Labels</span>
      </label>

      <button
        class="pf-c-form__group-label-help"
        aria-label="More information for labels field"
        aria-describedby="form-demo-sections-complex-formform-demo-sections-complex-form-labels"
      >
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
    <div class="pf-c-form__group-control">
      <div
        class="pf-c-text-input-group"
        id="form-demo-sections-complex-formform-demo-sections-complex-form-labels"
      >
        <div class="pf-c-text-input-group__main">
          <div class="pf-c-label-group">
            <div class="pf-c-label-group__main">
              <ul
                class="pf-c-label-group__list"
                role="list"
                aria-label="Group of labels"
              >
                <li class="pf-c-label-group__list-item">
                  <span class="pf-c-label">
                    <span class="pf-c-label__content">
                      <span class="pf-c-label__icon">
                        <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
                      </span>
                      prometheus=k8s
                    </span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      id="-label-1-button"
                      aria-label="Remove"
                      aria-labelledby="-label-1-button -label-1-text"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </span>
                </li>
                <li class="pf-c-label-group__list-item">
                  <span class="pf-c-label pf-m-blue">
                    <span class="pf-c-label__content">
                      <span class="pf-c-label__icon">
                        <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
                      </span>
                      new
                    </span>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      id="-label-2-button"
                      aria-label="Remove"
                      aria-labelledby="-label-2-button -label-2-text"
                    >
                      <i class="fas fa-times" aria-hidden="true"></i>
                    </button>
                  </span>
                </li>
                <li class="pf-c-label-group__list-item">
                  <button class="pf-c-label pf-m-add" type="button">
                    <span class="pf-c-label__content">Add Label</span>
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
      </div>
    </div>
  </div>

  <div
    class="pf-c-form__field-group"
    role="group"
    aria-labelledby="form-demo-sections-complex-form-alerting-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="false"
          aria-label="Details"
          id="form-demo-sections-complex-form-alerting-toggle"
          aria-labelledby="form-demo-sections-complex-form-alerting-title form-demo-sections-complex-form-alerting-toggle"
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
            id="form-demo-sections-complex-form-alerting-title"
          >Alerting</div>
        </div>
        <div
          class="pf-c-form__field-group-header-description"
        >Define details regarding alerting.</div>
      </div>
    </div>
  </div>

  <div
    class="pf-c-form__field-group"
    role="group"
    aria-labelledby="form-demo-sections-complex-form-query-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="false"
          aria-label="Details"
          id="form-demo-sections-complex-form-query-toggle"
          aria-labelledby="form-demo-sections-complex-form-query-title form-demo-sections-complex-form-query-toggle"
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
            id="form-demo-sections-complex-form-query-title"
          >Query</div>
        </div>
        <div
          class="pf-c-form__field-group-header-description"
        >The query specification defines the query command line flags when starting.</div>
      </div>
    </div>
  </div>

  <div
    class="pf-c-form__field-group"
    role="group"
    aria-labelledby="form-demo-sections-complex-form-affinity-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="false"
          aria-label="Details"
          id="form-demo-sections-complex-form-affinity-toggle"
          aria-labelledby="form-demo-sections-complex-form-affinity-title form-demo-sections-complex-form-affinity-toggle"
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
            id="form-demo-sections-complex-form-affinity-title"
          >Affinity</div>
        </div>
        <div
          class="pf-c-form__field-group-header-description"
        >If specified, the pod's scheduling constraints.</div>
      </div>
    </div>
    <div class="pf-c-form__field-group-body" hidden>
      <div
        class="pf-c-form__field-group pf-m-expanded"
        role="group"
        aria-labelledby="form-demo-sections-complex-form-node-affinity-title"
      >
        <div class="pf-c-form__field-group-toggle">
          <div class="pf-c-form__field-group-toggle-button">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-expanded="true"
              aria-label="Details"
              id="form-demo-sections-complex-form-node-affinity-toggle"
              aria-labelledby="form-demo-sections-complex-form-node-affinity-title form-demo-sections-complex-form-node-affinity-toggle"
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
                id="form-demo-sections-complex-form-node-affinity-title"
              >Node affinity</div>
            </div>
            <div
              class="pf-c-form__field-group-header-description"
            >Describes node affinity scheduling rules for the pod.</div>
          </div>
        </div>
        <div class="pf-c-form__field-group-body">
          <div
            class="pf-c-form__field-group"
            role="group"
            aria-labelledby="form-demo-sections-complex-form-node-affinity-required-title"
          >
            <div class="pf-c-form__field-group-toggle">
              <div class="pf-c-form__field-group-toggle-button">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-expanded="false"
                  aria-label="Details"
                  id="form-demo-sections-complex-form-node-affinity-required-toggle"
                  aria-labelledby="form-demo-sections-complex-form-node-affinity-required-title form-demo-sections-complex-form-node-affinity-required-toggle"
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
                    id="form-demo-sections-complex-form-node-affinity-required-title"
                  >Required during scheduling, ignored during execution</div>
                </div>
                <div
                  class="pf-c-form__field-group-header-description"
                >The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements.</div>
              </div>
            </div>
          </div>

          <div
            class="pf-c-form__field-group pf-m-expanded"
            role="group"
            aria-labelledby="form-demo-sections-complex-form-node-affinity-required-2-title"
          >
            <div class="pf-c-form__field-group-toggle">
              <div class="pf-c-form__field-group-toggle-button">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-expanded="true"
                  aria-label="Details"
                  id="form-demo-sections-complex-form-node-affinity-required-2-toggle"
                  aria-labelledby="form-demo-sections-complex-form-node-affinity-required-2-title form-demo-sections-complex-form-node-affinity-required-2-toggle"
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
                    id="form-demo-sections-complex-form-node-affinity-required-2-title"
                  >Required during scheduling, ignored during execution</div>
                </div>
                <div
                  class="pf-c-form__field-group-header-description"
                >The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements.</div>
              </div>
            </div>
            <div class="pf-c-form__field-group-body">
              <section
                class="pf-c-form__section"
                role="group"
                aria-labelledby="form-demo-sections-complex-form-node-selector-terms-title"
              >
                <div class="pf-c-form__group">
                  <div class="pf-c-form__group-label">
                    <label
                      class="pf-c-form__label"
                      id="form-demo-sections-complex-form-node-selector-terms-title"
                    >
                      <span class="pf-c-form__label-text">Node selector terms</span>
                      <span
                        class="pf-c-form__label-required"
                        aria-hidden="true"
                      >&#42;</span>
                    </label>
                  </div>
                  <div class="pf-c-form__group-control pf-m-stack">
                    <div class="pf-c-input-group">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="form-demo-sections-complex-form-node-selector-terms-input-1"
                        name="form-demo-sections-complex-form-node-selector-terms-input-1"
                        aria-labelledby="form-demo-sections-complex-form-node-selector-terms form-demo-sections-complex-form-node-selector-terms-title"
                      />

                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Remove"
                      >
                        <i class="fas fa-minus-circle" aria-hidden="true"></i>
                      </button>
                    </div>
                    <button
                      class="pf-c-button pf-m-link pf-m-inline"
                      type="button"
                    >
                      <span class="pf-c-button__icon pf-m-start">
                        <i class="fas fa-plus-circle" aria-hidden="true"></i>
                      </span>
                      Add valid redirect URI
                    </button>
                  </div>
                </div>
              </section>
            </div>
          </div>
        </div>
      </div>

      <div
        class="pf-c-form__field-group"
        role="group"
        aria-labelledby="form-demo-sections-complex-form-pod-affinity-title"
      >
        <div class="pf-c-form__field-group-toggle">
          <div class="pf-c-form__field-group-toggle-button">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-expanded="false"
              aria-label="Details"
              id="form-demo-sections-complex-form-pod-affinity-toggle"
              aria-labelledby="form-demo-sections-complex-form-pod-affinity-title form-demo-sections-complex-form-pod-affinity-toggle"
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
                id="form-demo-sections-complex-form-pod-affinity-title"
              >Pod affinity</div>
            </div>
            <div
              class="pf-c-form__field-group-header-description"
            >Describes pod affinity scheduling rules (e.g. co-locate the pod in the same node, zone, etc. as some other pods).</div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div
    class="pf-c-form__field-group pf-m-expanded"
    role="group"
    aria-labelledby="form-demo-sections-complex-form-routing-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="true"
          aria-label="Details"
          id="form-demo-sections-complex-form-routing-toggle"
          aria-labelledby="form-demo-sections-complex-form-routing-title form-demo-sections-complex-form-routing-toggle"
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
            id="form-demo-sections-complex-form-routing-title"
          >Routing</div>
        </div>
      </div>
    </div>
    <div class="pf-c-form__field-group-body">
      <div
        class="pf-c-form__group"
        role="group"
        aria-labelledby="form-demo-sections-complex-formform-demo-sections-complex-form-routing-create-route-legend"
      >
        <div class="pf-c-form__group-control">
          <div class="pf-c-check">
            <input
              class="pf-c-check__input"
              type="checkbox"
              id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-create-route-create-route"
              name="form-demo-sections-complex-formform-demo-sections-complex-form-routing-create-route-create-route"
            />

            <label
              class="pf-c-check__label"
              for="form-demo-sections-complex-formform-demo-sections-complex-form-routing-create-route-create-route"
            >Create a route to the application</label>
          </div>
          <div
            class="pf-c-form__helper-text"
            id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-create-route-legend"
            aria-live="polite"
          >
            <div class="pf-c-helper-text">
              <div class="pf-c-helper-text__item">
                <span
                  class="pf-c-helper-text__item-text"
                >Exposes your appplication at a public URL.</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label
            class="pf-c-form__label"
            for="form-demo-sections-complex-formform-demo-sections-complex-form-routing-hostname"
          >
            <span class="pf-c-form__label-text">Hostname</span>
          </label>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            type="text"
            id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-hostname"
            name="form-demo-sections-complex-formform-demo-sections-complex-form-routing-hostname"
          />

          <div
            class="pf-c-form__helper-text"
            id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-hostname-helper"
            aria-live="polite"
          >
            <div class="pf-c-helper-text">
              <div class="pf-c-helper-text__item">
                <span
                  class="pf-c-helper-text__item-text"
                >Public hostname for the route. If not specified, a hostname is generated.</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="pf-c-form__group">
        <div class="pf-c-form__group-label">
          <label
            class="pf-c-form__label"
            for="form-demo-sections-complex-formform-demo-sections-complex-form-routing-path"
          >
            <span class="pf-c-form__label-text">Path</span>
          </label>
        </div>
        <div class="pf-c-form__group-control">
          <input
            class="pf-c-form-control"
            type="text"
            placeholder="/"
            id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-path"
            name="form-demo-sections-complex-formform-demo-sections-complex-form-routing-path"
            required
          />

          <div
            class="pf-c-form__helper-text"
            id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-path-helper"
            aria-live="polite"
          >
            <div class="pf-c-helper-text">
              <div class="pf-c-helper-text__item">
                <span
                  class="pf-c-helper-text__item-text"
                >Path that the router watches to route traffic to the service.</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div
        class="pf-c-form__group"
        role="group"
        aria-labelledby="form-demo-sections-complex-formform-demo-sections-complex-form-routing-security-legend"
      >
        <div
          class="pf-c-form__group-label"
          id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-security-legend"
        >
          <span
            class="pf-c-form__label"
            for="form-demo-sections-complex-formform-demo-sections-complex-form-routing-security"
          >
            <span class="pf-c-form__label-text">Security</span>
          </span>
        </div>
        <div class="pf-c-form__group-control">
          <div class="pf-c-check">
            <input
              class="pf-c-check__input"
              type="checkbox"
              id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-security-check-1"
              name="form-demo-sections-complex-formform-demo-sections-complex-form-routing-security-check-1"
            />

            <label
              class="pf-c-check__label"
              for="form-demo-sections-complex-formform-demo-sections-complex-form-routing-security-check-1"
            >Secure Route</label>
          </div>
          <div
            class="pf-c-form__helper-text"
            id="form-demo-sections-complex-formform-demo-sections-complex-form-routing-security-helper"
            aria-live="polite"
          >
            <div class="pf-c-helper-text">
              <div class="pf-c-helper-text__item">
                <span
                  class="pf-c-helper-text__item-text"
                >Routes can be secured using several TLS termination types for serving certificates.</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div
    class="pf-c-form__field-group pf-m-expanded"
    role="group"
    aria-labelledby="form-demo-sections-complex-form-health-checks-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="true"
          aria-label="Details"
          id="form-demo-sections-complex-form-health-checks-toggle"
          aria-labelledby="form-demo-sections-complex-form-health-checks-title form-demo-sections-complex-form-health-checks-toggle"
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
            id="form-demo-sections-complex-form-health-checks-title"
          >Health checks</div>
        </div>
      </div>
    </div>
    <div class="pf-c-form__field-group-body">
      <div
        class="pf-c-form__field-group pf-m-expanded"
        role="group"
        aria-labelledby="form-demo-sections-complex-form-readiness-title"
      >
        <div class="pf-c-form__field-group-header">
          <div class="pf-c-form__field-group-header-main">
            <div class="pf-c-form__field-group-header-title">
              <div
                class="pf-c-form__field-group-header-title-text"
                id="form-demo-sections-complex-form-readiness-title"
              >Readiness probe</div>
            </div>
            <div
              class="pf-c-form__field-group-header-description"
            >A readiness probe checks if the container is ready to handle requests. A failed readiness probe means that a container should not receive any traffic from a proxy, even if it's running.</div>
          </div>
        </div>
        <div class="pf-c-form__field-group-body">
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-control">
              <button class="pf-c-button pf-m-link pf-m-inline" type="button">
                <span class="pf-c-button__icon pf-m-start">
                  <i class="fas fa-plus-circle" aria-hidden="true"></i>
                </span>
                Add liveness probe
              </button>
            </div>
          </div>
        </div>
      </div>
      <div
        class="pf-c-form__field-group pf-m-expanded"
        role="group"
        aria-labelledby="form-demo-sections-complex-form-startup-title"
      >
        <div class="pf-c-form__field-group-header">
          <div class="pf-c-form__field-group-header-main">
            <div class="pf-c-form__field-group-header-title">
              <div
                class="pf-c-form__field-group-header-title-text"
                id="form-demo-sections-complex-form-startup-title"
              >Liveness probe</div>
            </div>
            <div
              class="pf-c-form__field-group-header-description"
            >A startup probe checks if the application within the container is started.</div>
          </div>
        </div>
        <div class="pf-c-form__field-group-body">
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-control">
              <button class="pf-c-button pf-m-link pf-m-inline" type="button">
                <span class="pf-c-button__icon pf-m-start">
                  <i class="fas fa-plus-circle" aria-hidden="true"></i>
                </span>
                Add startup probe
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div
    class="pf-c-form__field-group"
    role="group"
    aria-labelledby="form-demo-sections-complex-form-build-configuration-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="false"
          aria-label="Details"
          id="form-demo-sections-complex-form-build-configuration-toggle"
          aria-labelledby="form-demo-sections-complex-form-build-configuration-title form-demo-sections-complex-form-build-configuration-toggle"
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
            id="form-demo-sections-complex-form-build-configuration-title"
          >Build configuration</div>
        </div>
      </div>
      <div class="pf-c-form__field-group-header-actions">
        <button class="pf-c-button pf-m-secondary" type="button">Import</button>
      </div>
    </div>
  </div>

  <div
    class="pf-c-form__field-group"
    role="group"
    aria-labelledby="form-demo-sections-complex-form-deployment-title"
  >
    <div class="pf-c-form__field-group-toggle">
      <div class="pf-c-form__field-group-toggle-button">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-expanded="false"
          aria-label="Details"
          id="form-demo-sections-complex-form-deployment-toggle"
          aria-labelledby="form-demo-sections-complex-form-deployment-title form-demo-sections-complex-form-deployment-toggle"
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
            id="form-demo-sections-complex-form-deployment-title"
          >Deployment</div>
        </div>
      </div>
    </div>
  </div>

  <div class="pf-c-form__actions">
    <button class="pf-c-button pf-m-primary" type="submit">Save</button>
    <button class="pf-c-button pf-m-secondary" type="reset">Cancel</button>
  </div>
</form>

```
