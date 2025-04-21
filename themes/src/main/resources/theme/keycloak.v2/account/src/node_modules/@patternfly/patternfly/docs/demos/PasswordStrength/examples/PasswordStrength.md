---
id: 'Password strength'
beta: true
section: demos
---## Examples

### Initial state

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label pf-m-info">
      <div class="pf-c-form__group-label-main">
        <label
          class="pf-c-form__label"
          for="password-strength-demo--initial-password"
        >
          <span class="pf-c-form__label-text">Password</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for password field"
          aria-describedby="password-strength-demo--initial-password"
        >
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-label-info"></div>
    </div>
    <div class="pf-c-form__group-control">
      <div class="pf-c-input-group">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="password-strength-demo--initial-password"
          name="password-strength-demo--initial-password"
          aria-label="Password input"
          value
          placeholder="Password"
        />
        <button
          class="pf-c-button pf-m-control"
          type="button"
          aria-label="Show password"
        >
          <i class="fas fa-eye-slash" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__helper-text">
        <ul class="pf-c-helper-text">
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-indeterminate">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-minus" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Must be at least 14 characters</span>
          </li>
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-indeterminate">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-minus" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Cannot contain the word "redhat"</span>
          </li>
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-indeterminate">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-minus" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Must include at least 3 of the following: lowercase letters, uppercase letters, numbers, symbols</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</form>

```

### Invalid password

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label pf-m-info">
      <div class="pf-c-form__group-label-main">
        <label
          class="pf-c-form__label"
          for="password-strength-demo--invalid-password"
        >
          <span class="pf-c-form__label-text">Password</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for password field"
          aria-describedby="password-strength-demo--invalid-password"
        >
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-label-info"></div>
    </div>
    <div class="pf-c-form__group-control">
      <div class="pf-c-input-group">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="password-strength-demo--invalid-password"
          name="password-strength-demo--invalid-password"
          aria-label="Password input"
          value="Marie$RedHat78"
          placeholder="Password"
        />
        <button
          class="pf-c-button pf-m-control"
          type="button"
          aria-label="Show password"
        >
          <i class="fas fa-eye-slash" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__helper-text">
        <ul class="pf-c-helper-text">
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Must be at least 14 characters</span>
          </li>
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-error">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Cannot contain the word "redhat"</span>
          </li>
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Must include at least 3 of the following: lowercase letters, uppercase letters, numbers, symbols</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</form>

```

### Valid, weak password

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label pf-m-info">
      <div class="pf-c-form__group-label-main">
        <label
          class="pf-c-form__label"
          for="password-strength-demo--weak-password"
        >
          <span class="pf-c-form__label-text">Password</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for password field"
          aria-describedby="password-strength-demo--weak-password"
        >
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-label-info">
        <div class="pf-c-helper-text">
          <div class="pf-c-helper-text__item pf-m-error">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
            </span>
            <span class="pf-c-helper-text__item-text">Weak</span>
          </div>
        </div>
      </div>
    </div>
    <div class="pf-c-form__group-control">
      <div class="pf-c-input-group">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="password-strength-demo--weak-password"
          name="password-strength-demo--weak-password"
          aria-label="Password input"
          value="Marie$Can3Read"
          placeholder="Password"
        />
        <button
          class="pf-c-button pf-m-control"
          type="button"
          aria-label="Show password"
        >
          <i class="fas fa-eye-slash" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__helper-text">
        <ul class="pf-c-helper-text">
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Must be at least 14 characters</span>
          </li>
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Cannot contain the word "redhat"</span>
          </li>
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Must include at least 3 of the following: lowercase letters, uppercase letters, numbers, symbols</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</form>

```

### Valid, strong password

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label pf-m-info">
      <div class="pf-c-form__group-label-main">
        <label
          class="pf-c-form__label"
          for="password-strength-demo--strong-password"
        >
          <span class="pf-c-form__label-text">Password</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for password field"
          aria-describedby="password-strength-demo--strong-password"
        >
          <i class="pficon pf-icon-help" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__group-label-info">
        <div class="pf-c-helper-text">
          <div class="pf-c-helper-text__item pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span class="pf-c-helper-text__item-text">Strong</span>
          </div>
        </div>
      </div>
    </div>
    <div class="pf-c-form__group-control">
      <div class="pf-c-input-group">
        <input
          class="pf-c-form-control"
          required
          type="text"
          id="password-strength-demo--strong-password"
          name="password-strength-demo--strong-password"
          aria-label="Password input"
          value="Marie$Can8Read3Pass@Word"
          placeholder="Password"
        />
        <button
          class="pf-c-button pf-m-control"
          type="button"
          aria-label="Show password"
        >
          <i class="fas fa-eye-slash" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-form__helper-text">
        <ul class="pf-c-helper-text">
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Must be at least 14 characters</span>
          </li>
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Cannot contain the word "redhat"</span>
          </li>
          <li class="pf-c-helper-text__item pf-m-dynamic pf-m-success">
            <span class="pf-c-helper-text__item-icon">
              <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-helper-text__item-text"
            >Must include at least 3 of the following: lowercase letters, uppercase letters, numbers, symbols</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</form>

```

## Documentation

This demo implements a password strength meter. It uses multiple helper text items in the form helper text area below the input field to indicate whether the password meets validity criteria. Once the validity criteria are met, it also places helper text in the info area above the input field to indicate the strength of the password.
