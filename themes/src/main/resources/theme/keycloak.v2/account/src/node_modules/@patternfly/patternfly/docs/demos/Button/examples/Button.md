---
id: 'Button'
section: components
cssPrefix: pf-d-button
---## Examples

### Progress button - initial

```html
<form novalidate class="pf-c-form pf-m-limit-width">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="progress-button-example-login">
        <span class="pf-c-form__label-text">Username</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="progress-button-example-login"
        name="progress-button-example-login"
        value="johndoe"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label class="pf-c-form__label" for="progress-button-example-password">
        <span class="pf-c-form__label-text">Password</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="password"
        value="p@ssw0rd"
        id="progress-button-example-password"
        name="progress-button-example-password"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group pf-m-action">
    <div class="pf-c-form__actions">
      <button
        class="pf-c-button pf-m-primary"
        type="submit"
      >Link account and log in</button>
    </div>
  </div>
</form>

```

### Progress button - loading

```html
<form novalidate class="pf-c-form pf-m-limit-width">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label
        class="pf-c-form__label"
        for="progress-button-loading-example-login"
      >
        <span class="pf-c-form__label-text">Username</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="progress-button-loading-example-login"
        name="progress-button-loading-example-login"
        value="johndoe"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label
        class="pf-c-form__label"
        for="progress-button-loading-example-password"
      >
        <span class="pf-c-form__label-text">Password</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="password"
        value="p@ssw0rd"
        id="progress-button-loading-example-password"
        name="progress-button-loading-example-password"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group pf-m-action">
    <div class="pf-c-form__actions">
      <button
        class="pf-c-button pf-m-progress pf-m-in-progress pf-m-primary"
        type="submit"
      >
        <span class="pf-c-button__progress">
          <span
            class="pf-c-spinner pf-m-md"
            role="progressbar"
            aria-label="Loading..."
          >
            <span class="pf-c-spinner__clipper"></span>
            <span class="pf-c-spinner__lead-ball"></span>
            <span class="pf-c-spinner__tail-ball"></span>
          </span>
        </span>
        Linking account
      </button>
    </div>
  </div>
</form>

```

### Progress button - complete

```html
<form novalidate class="pf-c-form pf-m-limit-width">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label
        class="pf-c-form__label"
        for="progress-button-complete-example-login"
      >
        <span class="pf-c-form__label-text">Username</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="text"
        id="progress-button-complete-example-login"
        name="progress-button-complete-example-login"
        value="johndoe"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label">
      <label
        class="pf-c-form__label"
        for="progress-button-complete-example-password"
      >
        <span class="pf-c-form__label-text">Password</span>
        <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
      </label>
    </div>
    <div class="pf-c-form__group-control">
      <input
        class="pf-c-form-control"
        type="password"
        value="p@ssw0rd"
        id="progress-button-complete-example-password"
        name="progress-button-complete-example-password"
        required
      />
    </div>
  </div>
  <div class="pf-c-form__group pf-m-action">
    <div class="pf-c-form__actions">
      <button class="pf-c-button pf-m-primary pf-m-start" type="submit">
        <span class="pf-c-button__icon pf-m-start">
          <i class="fas fa-check-circle" aria-hidden="true"></i>
        </span>
        Logged in
      </button>
    </div>
  </div>
</form>

```
