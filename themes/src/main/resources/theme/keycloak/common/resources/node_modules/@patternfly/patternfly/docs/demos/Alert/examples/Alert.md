---
id: Alert
section: components
---## Demos

### Toast

```html isFullscreen
<div class="pf-c-page" id="alert-toast-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-alert-toast-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="alert-toast-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="alert-toast-example-primary-nav"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </div>
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>

    <div class="pf-c-page__header-tools">
      <div class="pf-c-page__header-tools-group">
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Settings"
          >
            <i class="fas fa-cog" aria-hidden="true"></i>
          </button>
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Help"
          >
            <i class="pf-icon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-page__header-tools-group">
        <div class="pf-c-page__header-tools-item pf-m-hidden-on-lg">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="alert-toast-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="alert-toast-example-dropdown-kebab-1-button"
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
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-md"
        >
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="alert-toast-example-dropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
            >
              <span class="pf-c-dropdown__toggle-text">John Smith</span>
              <span class="pf-c-dropdown__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>
            <ul
              class="pf-c-dropdown__menu"
              aria-labelledby="alert-toast-example-dropdown-kebab-2-button"
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
      <img
        class="pf-c-avatar"
        src="/assets/images/img_avatar.svg"
        alt="Avatar image"
      />
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="alert-toast-example-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-alert-toast-example"
  >
    <section class="pf-c-page__main-breadcrumb pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
          <ol class="pf-c-breadcrumb__list">
            <li class="pf-c-breadcrumb__item">
              <a href="#" class="pf-c-breadcrumb__link">Section home</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a
                href="#"
                class="pf-c-breadcrumb__link pf-m-current"
                aria-current="page"
              >Section landing</a>
            </li>
          </ol>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div class="pf-l-gallery pf-m-gutter">
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>
<ul class="pf-c-alert-group pf-m-toast">
  <li class="pf-c-alert-group__item">
    <div class="pf-c-alert pf-m-success" aria-label="Success alert">
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title">
        <span class="pf-screen-reader">Success alert:</span>
        Newest notification
      </p>
      <div class="pf-c-alert__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close success alert: Newest notification"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-alert__description">
        <p>This is a description of the notification content.</p>
      </div>
    </div>
  </li>
  <li class="pf-c-alert-group__item">
    <div class="pf-c-alert pf-m-warning" aria-label="Warning alert">
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-exclamation-triangle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title">
        <span class="pf-screen-reader">Info alert:</span>
        Second newest notification
      </p>
      <div class="pf-c-alert__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close warning alert: second newest notification"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-alert__description">
        <p>This is a description of the notification content.</p>
      </div>
    </div>
  </li>
  <li class="pf-c-alert-group__item">
    <div class="pf-c-alert pf-m-danger" aria-label="Danger alert">
      <div class="pf-c-alert__icon">
        <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
      </div>
      <p class="pf-c-alert__title">
        <span class="pf-screen-reader">Last notification</span>
        Last notification
      </p>
      <div class="pf-c-alert__action">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close danger alert: Last notification"
        >
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
      </div>
      <div class="pf-c-alert__description">
        <p>This is a description of the notification content.</p>
      </div>
    </div>
  </li>
</ul>

```

### Inline Alert in Horizontal Form

```html isFullscreen
<div class="pf-c-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="-primary-nav"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </div>
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>
    <div class="pf-c-page__header-tools">
      <div class="pf-c-page__header-tools-group">
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Settings"
          >
            <i class="fas fa-cog" aria-hidden="true"></i>
          </button>
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Help"
          >
            <i class="pf-icon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-page__header-tools-group">
        <div class="pf-c-page__header-tools-item pf-m-hidden-on-lg">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="-dropdown-kebab-1-button"
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
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-md"
        >
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="-dropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
            >
              <span class="pf-c-dropdown__toggle-text">John Smith</span>
              <span class="pf-c-dropdown__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>
            <ul
              class="pf-c-dropdown__menu"
              aria-labelledby="-dropdown-kebab-2-button"
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
      <img
        class="pf-c-avatar"
        src="/assets/images/img_avatar.svg"
        alt="Avatar image"
      />
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav class="pf-c-nav" id="-primary-nav" aria-label="Global">
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main class="pf-c-page__main" tabindex="-1" id="main-content-">
    <section class="pf-c-page__main-subnav pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-nav pf-m-horizontal-subnav" aria-label="Local">
          <ul class="pf-c-nav__list">
            <li class="pf-c-nav__item">
              <a
                href="#"
                class="pf-c-nav__link pf-m-current"
                aria-current="page"
              >Item 1</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Item 2</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Item 3</a>
            </li>
          </ul>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1 class="pf-c-title pf-m-2xl">Horizontal form demo</h1>
        <p>Below is an example of a horizontal form.</p>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-light">
      <form novalidate class="pf-c-form pf-m-horizontal">
        <div class="pf-c-form__alert">
          <div
            class="pf-c-alert pf-m-danger pf-m-inline"
            aria-label="Inline danger alert"
          >
            <div class="pf-c-alert__icon">
              <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
            </div>
            <p class="pf-c-alert__title">
              <span class="pf-screen-reader">Danger alert:</span>
              Fill out all required fields before continuing.
            </p>
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="horizontal-align-labels-1-form-name"
            >
              <span class="pf-c-form__label-text">Name</span>
              <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              required
              type="text"
              id="horizontal-align-labels-1-form-name"
              name="horizontal-align-labels-1-form-name"
              aria-invalid="true"
              aria-describedby="horizontal-align-labels-1-form-name-helper"
            />
            <p
              class="pf-c-form__helper-text pf-m-error"
              id="horizontal-align-labels-1-form-name-helper"
              aria-live="polite"
            >Required field</p>
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="horizontal-align-labels-1-form-email"
            >
              <span class="pf-c-form__label-text">Email</span>
              <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              value="patternfly@patternfly.com"
              id="horizontal-align-labels-1-form-email"
              name="horizontal-align-labels-1-form-email"
              required
            />
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label
              class="pf-c-form__label"
              for="horizontal-align-labels-1-form-phone"
            >
              <span class="pf-c-form__label-text">Phone number</span>
              <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              required
              type="text"
              id="horizontal-align-labels-1-form-phone"
              name="horizontal-align-labels-1-form-phone"
              aria-invalid="true"
              aria-describedby="horizontal-align-labels-1-form-phone-helper"
            />
            <p
              class="pf-c-form__helper-text pf-m-error"
              id="horizontal-align-labels-1-form-phone-helper"
              aria-live="polite"
            >Required field</p>
          </div>
        </div>
        <div
          class="pf-c-form__group"
          role="group"
          aria-labelledby="horizontal-align-labels-1-check-group-legend"
        >
          <div
            class="pf-c-form__group-label pf-m-no-padding-top"
            id="horizontal-align-labels-1-check-group-legend"
          >
            <span
              class="pf-c-form__label"
              for="horizontal-align-labels-1-form-experience"
            >
              <span class="pf-c-form__label-text">Your experience</span>
              <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
            </span>
          </div>
          <div class="pf-c-form__group-control">
            <p
              class="pf-c-form__helper-text pf-m-error"
              id="horizontal-align-labels-1-simple-form-info-helper"
              aria-live="polite"
            >
              <span class="pf-c-form__helper-text-icon">
                <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
              </span>
              This is a requied field
            </p>
            <div class="pf-c-check">
              <input
                class="pf-c-check__input"
                type="checkbox"
                id="alt-form-checkbox1"
                name="alt-form-checkbox1"
              />

              <label
                class="pf-c-check__label"
                for="alt-form-checkbox1"
              >Follow up via email.</label>
            </div>
            <div class="pf-c-check">
              <input
                class="pf-c-check__input"
                type="checkbox"
                id="alt-form-checkbox2"
                name="alt-form-checkbox2"
              />

              <label
                class="pf-c-check__label"
                for="alt-form-checkbox2"
              >Remember my password for 30 days.</label>
            </div>
          </div>
        </div>
        <div class="pf-c-form__group pf-m-action">
          <div class="pf-c-form__group-control">
            <div class="pf-c-form__actions">
              <button class="pf-c-button pf-m-primary" type="submit">Submit</button>
              <button class="pf-c-button pf-m-secondary" type="reset">Cancel</button>
            </div>
          </div>
        </div>
      </form>
    </section>
  </main>
</div>

```

### Inline Alert in Stacked Form

```html isFullscreen
<div class="pf-c-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="-primary-nav"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </div>
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>
    <div class="pf-c-page__header-tools">
      <div class="pf-c-page__header-tools-group">
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Settings"
          >
            <i class="fas fa-cog" aria-hidden="true"></i>
          </button>
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Help"
          >
            <i class="pf-icon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-page__header-tools-group">
        <div class="pf-c-page__header-tools-item pf-m-hidden-on-lg">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="-dropdown-kebab-1-button"
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
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-md"
        >
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="-dropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
            >
              <span class="pf-c-dropdown__toggle-text">John Smith</span>
              <span class="pf-c-dropdown__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>
            <ul
              class="pf-c-dropdown__menu"
              aria-labelledby="-dropdown-kebab-2-button"
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
      <img
        class="pf-c-avatar"
        src="/assets/images/img_avatar.svg"
        alt="Avatar image"
      />
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav class="pf-c-nav" id="-primary-nav" aria-label="Global">
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main class="pf-c-page__main" tabindex="-1" id="main-content-">
    <section class="pf-c-page__main-subnav pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-nav pf-m-horizontal-subnav" aria-label="Local">
          <ul class="pf-c-nav__list">
            <li class="pf-c-nav__item">
              <a
                href="#"
                class="pf-c-nav__link pf-m-current"
                aria-current="page"
              >Item 1</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Item 2</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Item 3</a>
            </li>
          </ul>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1 class="pf-c-title pf-m-2xl">Stacked form demo</h1>
        <p>Below is an example of a stacked form.</p>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-light">
      <form novalidate class="pf-c-form pf-m-limit-width">
        <div class="pf-c-form__alert">
          <div
            class="pf-c-alert pf-m-danger pf-m-inline"
            aria-label="Inline danger alert"
          >
            <div class="pf-c-alert__icon">
              <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
            </div>
            <p class="pf-c-alert__title">
              <span class="pf-screen-reader">Danger alert:</span>
              Fill out all required fields before continuing.
            </p>
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label class="pf-c-form__label" for="stacked-labels-1-form-name">
              <span class="pf-c-form__label-text">Full name</span>
              <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              required
              type="text"
              id="stacked-labels-1-form-name"
              name="stacked-labels-1-form-name"
              aria-invalid="true"
              aria-describedby="stacked-labels-1-form-name-helper"
            />
            <p
              class="pf-c-form__helper-text pf-m-error"
              id="stacked-labels-1-form-name-helper-name"
              aria-live="polite"
            >Required field</p>
          </div>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label class="pf-c-form__label" for="stacked-labels-1-form-email">
              <span class="pf-c-form__label-text">Email</span>
              <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
            </label>
          </div>
          <div class="pf-c-form__group-control">
            <input
              class="pf-c-form-control"
              type="text"
              value="patternfly.com"
              id="stacked-labels-1-form-email"
              name="stacked-labels-1-form-email"
              required
            />
          </div>
          <p
            class="pf-c-form__helper-text pf-m-error"
            id="stacked-labels-1-form-email-helper-email"
            aria-live="polite"
          >Enter a valid email address: example@gmail.com</p>
        </div>
        <div class="pf-c-form__group">
          <div class="pf-c-form__group-label">
            <label class="pf-c-form__label" for="stacked-labels-1-form-state">
              <span class="pf-c-form__label-text">State of residence</span>
              <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
            </label>
          </div>
          <select
            class="pf-c-form-control"
            required
            aria-invalid="true"
            id="select-group-error"
            name="select-group-error"
            aria-label="Error state select group example"
          >
            <option value>Select a state</option>
            <option value="Option 1">CA</option>
            <option value="Option 2">FL</option>
            <option value="Option 3">MA</option>
            <option value="Option 4">NY</option>
          </select>
          <p
            class="pf-c-form__helper-text pf-m-error"
            id="stacked-labels-1-form-email-helper-state"
            aria-live="polite"
          >Required field</p>
        </div>
        <div
          class="pf-c-form__group"
          role="group"
          aria-labelledby="stacked-labels-1-check-group-legend"
        >
          <div
            class="pf-c-form__group-label pf-m-no-padding-top"
            id="stacked-labels-1-check-group-legend"
          >
            <span
              class="pf-c-form__label"
              for="stacked-labels-1-form-experience"
            >
              <span class="pf-c-form__label-text">How can we contact you?</span>
              <span class="pf-c-form__label-required" aria-hidden="true">&#42;</span>
            </span>
            <p
              class="pf-c-form__helper-text pf-m-error"
              id="stacked-labels-1-simple-form-info-helper-contact"
              aria-live="polite"
            >
              <span class="pf-c-form__helper-text-icon">
                <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
              </span>
              This is a requied field
            </p>
          </div>
          <div class="pf-c-form__group-control pf-m-inline">
            <div class="pf-c-check">
              <input
                class="pf-c-check__input"
                type="checkbox"
                id="alt-form-checkbox1"
                name="alt-form-checkbox1"
              />

              <label class="pf-c-check__label" for="alt-form-checkbox1">Email</label>
            </div>
            <div class="pf-c-check">
              <input
                class="pf-c-check__input"
                type="checkbox"
                id="alt-form-checkbox2"
                name="alt-form-checkbox2"
              />

              <label class="pf-c-check__label" for="alt-form-checkbox2">Phone</label>
            </div>
            <div class="pf-c-check">
              <input
                class="pf-c-check__input"
                type="checkbox"
                id="alt-form-checkbox3"
                name="alt-form-checkbox3"
              />

              <label class="pf-c-check__label" for="alt-form-checkbox3">Mail</label>
            </div>
          </div>
        </div>
        <div class="pf-c-form__group pf-m-action">
          <div class="pf-c-form__group-control">
            <div class="pf-c-form__actions">
              <button class="pf-c-button pf-m-primary" type="submit">Submit</button>
              <button class="pf-c-button pf-m-secondary" type="reset">Cancel</button>
            </div>
          </div>
        </div>
      </form>
    </section>
  </main>
</div>

```
