---
id: 'Password generator'
section: demos
---## Examples

### Provide a generated password

```html
<form novalidate class="pf-c-form">
  <div class="pf-c-form__group">
    <div class="pf-c-form__group-label pf-m-info">
      <div class="pf-c-form__group-label-main">
        <label
          class="pf-c-form__label"
          for="password-generator-demo--initial-password"
        >
          <span class="pf-c-form__label-text">Password</span>
          <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
        </label>

        <button
          class="pf-c-form__group-label-help"
          aria-label="More information for password field"
          aria-describedby="password-generator-demo--initial-password"
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
          type="password"
          id="password-generator-demo--initial-password"
          name="password-generator-demo--initial-password"
          aria-label="Password input"
          value
          placeholder="Password"
        />
        <button
          class="pf-c-button pf-m-control"
          type="button"
          aria-label="Show password"
        >
          <i class="fas fa-eye" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-menu">
        <div class="pf-c-menu__content">
          <ul class="pf-c-menu__list" role="menu">
            <li class="pf-c-menu__list-item" role="none">
              <button class="pf-c-menu__item" type="button" role="menuitem">
                <span class="pf-c-menu__item-main">
                  <span
                    class="pf-c-menu__item-text"
                  >Use suggested password: fqu9kKe676JmKt2</span>
                </span>
              </button>
              <button
                class="pf-c-menu__item-action"
                type="button"
                aria-label="Generate a new suggested password"
              >
                <span class="pf-c-menu__item-action-icon">
                  <i class="fas fa-fw fa-redo" aria-hidden="true"></i>
                </span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</form>

```

## Documentation

This demo shows how to use a menu in conjunction with a form input to provide a generated password and an associated button for refresh.
