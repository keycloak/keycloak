---
id: Wizard
section: components
wrapperTag: div
---## Demos

### Basic

```html isFullscreen
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-lg"
      aria-modal="true"
      aria-label="Basic wizard"
    >
      <div class="pf-c-wizard">
        <div class="pf-c-wizard__header">
          <button
            class="pf-c-button pf-m-plain pf-c-wizard__close"
            type="button"
            aria-label="Close"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
          <h1 class="pf-c-title pf-m-3xl pf-c-wizard__title">Wizard title</h1>

          <p class="pf-c-wizard__description">Here is where the description goes</p>
        </div>
        <button
          aria-label="Wizard Header Toggle"
          class="pf-c-wizard__toggle"
          aria-expanded="false"
        >
          <span class="pf-c-wizard__toggle-list">
            <span class="pf-c-wizard__toggle-list-item">
              <span class="pf-c-wizard__toggle-num">2</span>
              Configuration
              <i
                class="fas fa-angle-right pf-c-wizard__toggle-separator"
                aria-hidden="true"
              ></i>
            </span>
            <span class="pf-c-wizard__toggle-list-item">Substep B</span>
          </span>
          <span class="pf-c-wizard__toggle-icon">
            <i class="fas fa-caret-down" aria-hidden="true"></i>
          </span>
        </button>
        <div class="pf-c-wizard__outer-wrap">
          <div class="pf-c-wizard__inner-wrap">
            <nav class="pf-c-wizard__nav" aria-label="Steps">
              <ol class="pf-c-wizard__nav-list">
                <li class="pf-c-wizard__nav-item">
                  <button class="pf-c-wizard__nav-link">Information</button>
                </li>
                <li class="pf-c-wizard__nav-item">
                  <button
                    class="pf-c-wizard__nav-link pf-m-current"
                  >Configuration</button>
                  <ol class="pf-c-wizard__nav-list">
                    <li class="pf-c-wizard__nav-item">
                      <button class="pf-c-wizard__nav-link">Substep A</button>
                    </li>
                    <li class="pf-c-wizard__nav-item">
                      <button
                        class="pf-c-wizard__nav-link pf-m-current"
                        aria-current="page"
                      >Substep B</button>
                    </li>
                    <li class="pf-c-wizard__nav-item">
                      <button class="pf-c-wizard__nav-link">Substep C</button>
                    </li>
                  </ol>
                </li>
                <li class="pf-c-wizard__nav-item">
                  <button class="pf-c-wizard__nav-link">Additional</button>
                </li>
                <li class="pf-c-wizard__nav-item">
                  <button
                    class="pf-c-wizard__nav-link pf-m-disabled"
                    aria-disabled="true"
                    tabindex="-1"
                  >Review</button>
                </li>
              </ol>
            </nav>
            <main class="pf-c-wizard__main">
              <div class="pf-c-wizard__main-body">
                <form novalidate class="pf-c-form">
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field1">
                        <span class="pf-c-form__label-text">Field 1</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field1"
                        name="-form-field1"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field2">
                        <span class="pf-c-form__label-text">Field 2</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field2"
                        name="-form-field2"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field3">
                        <span class="pf-c-form__label-text">Field 3</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field3"
                        name="-form-field3"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field4">
                        <span class="pf-c-form__label-text">Field 4</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field4"
                        name="-form-field4"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field5">
                        <span class="pf-c-form__label-text">Field 5</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field5"
                        name="-form-field5"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field6">
                        <span class="pf-c-form__label-text">Field 6</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field6"
                        name="-form-field6"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field7">
                        <span class="pf-c-form__label-text">Field 7</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field7"
                        name="-form-field7"
                      />
                    </div>
                  </div>
                </form>
              </div>
            </main>
          </div>
          <footer class="pf-c-wizard__footer">
            <button class="pf-c-button pf-m-primary" type="submit">Next</button>
            <button class="pf-c-button pf-m-secondary" type="button">Back</button>
            <div class="pf-c-wizard__footer-cancel">
              <button class="pf-c-button pf-m-link" type="button">Cancel</button>
            </div>
          </footer>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Nav expanded (mobile)

```html isFullscreen
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-lg"
      aria-modal="true"
      aria-label="Wizard with expanded mobile nav"
    >
      <div class="pf-c-wizard">
        <div class="pf-c-wizard__header">
          <button
            class="pf-c-button pf-m-plain pf-c-wizard__close"
            type="button"
            aria-label="Close"
          >
            <i class="fas fa-times" aria-hidden="true"></i>
          </button>
          <h1 class="pf-c-title pf-m-3xl pf-c-wizard__title">Wizard title</h1>

          <p class="pf-c-wizard__description">Here is where the description goes</p>
        </div>
        <button
          aria-label="Wizard Header Toggle"
          class="pf-c-wizard__toggle pf-m-expanded"
          aria-expanded="true"
        >
          <span class="pf-c-wizard__toggle-list">
            <span class="pf-c-wizard__toggle-list-item">
              <span class="pf-c-wizard__toggle-num">2</span>
              Configuration
              <i
                class="fas fa-angle-right pf-c-wizard__toggle-separator"
                aria-hidden="true"
              ></i>
            </span>
            <span class="pf-c-wizard__toggle-list-item">Substep B</span>
          </span>
          <span class="pf-c-wizard__toggle-icon">
            <i class="fas fa-caret-down" aria-hidden="true"></i>
          </span>
        </button>
        <div class="pf-c-wizard__outer-wrap">
          <div class="pf-c-wizard__inner-wrap">
            <nav class="pf-c-wizard__nav pf-m-expanded" aria-label="Steps">
              <ol class="pf-c-wizard__nav-list">
                <li class="pf-c-wizard__nav-item">
                  <button class="pf-c-wizard__nav-link">Information</button>
                </li>
                <li class="pf-c-wizard__nav-item">
                  <button
                    class="pf-c-wizard__nav-link pf-m-current"
                  >Configuration</button>
                  <ol class="pf-c-wizard__nav-list">
                    <li class="pf-c-wizard__nav-item">
                      <button class="pf-c-wizard__nav-link">Substep A</button>
                    </li>
                    <li class="pf-c-wizard__nav-item">
                      <button
                        class="pf-c-wizard__nav-link pf-m-current"
                        aria-current="page"
                      >Substep B</button>
                    </li>
                    <li class="pf-c-wizard__nav-item">
                      <button class="pf-c-wizard__nav-link">Substep C</button>
                    </li>
                  </ol>
                </li>
                <li class="pf-c-wizard__nav-item">
                  <button class="pf-c-wizard__nav-link">Additional</button>
                </li>
                <li class="pf-c-wizard__nav-item">
                  <button
                    class="pf-c-wizard__nav-link pf-m-disabled"
                    aria-disabled="true"
                    tabindex="-1"
                  >Review</button>
                </li>
              </ol>
            </nav>
            <main class="pf-c-wizard__main">
              <div class="pf-c-wizard__main-body">
                <form novalidate class="pf-c-form">
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field1">
                        <span class="pf-c-form__label-text">Field 1</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field1"
                        name="-form-field1"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field2">
                        <span class="pf-c-form__label-text">Field 2</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field2"
                        name="-form-field2"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field3">
                        <span class="pf-c-form__label-text">Field 3</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field3"
                        name="-form-field3"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field4">
                        <span class="pf-c-form__label-text">Field 4</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field4"
                        name="-form-field4"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field5">
                        <span class="pf-c-form__label-text">Field 5</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field5"
                        name="-form-field5"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field6">
                        <span class="pf-c-form__label-text">Field 6</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field6"
                        name="-form-field6"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label class="pf-c-form__label" for="-form-field7">
                        <span class="pf-c-form__label-text">Field 7</span>
                        <span
                          class="pf-c-form__label-required"
                          aria-hidden="true"
                        >&#42;</span>
                      </label>
                    </div>
                    <div class="pf-c-form__group-control">
                      <input
                        class="pf-c-form-control"
                        required
                        type="text"
                        id="-form-field7"
                        name="-form-field7"
                      />
                    </div>
                  </div>
                </form>
              </div>
            </main>
          </div>
          <footer class="pf-c-wizard__footer">
            <button class="pf-c-button pf-m-primary" type="submit">Next</button>
            <button class="pf-c-button pf-m-secondary" type="button">Back</button>
            <div class="pf-c-wizard__footer-cancel">
              <button class="pf-c-button pf-m-link" type="button">Cancel</button>
            </div>
          </footer>
        </div>
      </div>
    </div>
  </div>
</div>

```

### In page

```html isFullscreen
<div class="pf-c-page" id="wizard-in-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-wizard-in-page"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="wizard-in-page-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="wizard-in-page-primary-nav"
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
              id="wizard-in-page-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="wizard-in-page-dropdown-kebab-1-button"
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
              id="wizard-in-page-dropdown-kebab-2-button"
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
              aria-labelledby="wizard-in-page-dropdown-kebab-2-button"
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
      <nav class="pf-c-nav" id="wizard-in-page-primary-nav" aria-label="Global">
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Policy</a>
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
  <main class="pf-c-page__main" tabindex="-1" id="main-content-wizard-in-page">
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
    <section class="pf-c-page__main-wizard pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div class="pf-c-wizard">
          <button
            aria-label="Wizard Header Toggle"
            class="pf-c-wizard__toggle"
            aria-expanded="false"
          >
            <span class="pf-c-wizard__toggle-list">
              <span class="pf-c-wizard__toggle-list-item">
                <span class="pf-c-wizard__toggle-num">2</span>
                Configuration
                <i
                  class="fas fa-angle-right pf-c-wizard__toggle-separator"
                  aria-hidden="true"
                ></i>
              </span>
              <span class="pf-c-wizard__toggle-list-item">Substep B</span>
            </span>
            <span class="pf-c-wizard__toggle-icon">
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </span>
          </button>
          <div class="pf-c-wizard__outer-wrap">
            <div class="pf-c-wizard__inner-wrap">
              <nav class="pf-c-wizard__nav" aria-label="Steps">
                <ol class="pf-c-wizard__nav-list">
                  <li class="pf-c-wizard__nav-item">
                    <button class="pf-c-wizard__nav-link">Information</button>
                  </li>
                  <li class="pf-c-wizard__nav-item">
                    <button
                      class="pf-c-wizard__nav-link pf-m-current"
                    >Configuration</button>
                    <ol class="pf-c-wizard__nav-list">
                      <li class="pf-c-wizard__nav-item">
                        <button class="pf-c-wizard__nav-link">Substep A</button>
                      </li>
                      <li class="pf-c-wizard__nav-item">
                        <button
                          class="pf-c-wizard__nav-link pf-m-current"
                          aria-current="page"
                        >Substep B</button>
                      </li>
                      <li class="pf-c-wizard__nav-item">
                        <button class="pf-c-wizard__nav-link">Substep C</button>
                      </li>
                    </ol>
                  </li>
                  <li class="pf-c-wizard__nav-item">
                    <button class="pf-c-wizard__nav-link">Additional</button>
                  </li>
                  <li class="pf-c-wizard__nav-item">
                    <button
                      class="pf-c-wizard__nav-link pf-m-disabled"
                      aria-disabled="true"
                      tabindex="-1"
                    >Review</button>
                  </li>
                </ol>
              </nav>
              <div class="pf-c-wizard__main">
                <div class="pf-c-wizard__main-body">
                  <form novalidate class="pf-c-form">
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field1">
                          <span class="pf-c-form__label-text">Field 1</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field1"
                          name="-form-field1"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field2">
                          <span class="pf-c-form__label-text">Field 2</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field2"
                          name="-form-field2"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field3">
                          <span class="pf-c-form__label-text">Field 3</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field3"
                          name="-form-field3"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field4">
                          <span class="pf-c-form__label-text">Field 4</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field4"
                          name="-form-field4"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field5">
                          <span class="pf-c-form__label-text">Field 5</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field5"
                          name="-form-field5"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field6">
                          <span class="pf-c-form__label-text">Field 6</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field6"
                          name="-form-field6"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field7">
                          <span class="pf-c-form__label-text">Field 7</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field7"
                          name="-form-field7"
                        />
                      </div>
                    </div>
                  </form>
                </div>
              </div>
            </div>
            <footer class="pf-c-wizard__footer">
              <button class="pf-c-button pf-m-primary" type="submit">Next</button>
              <button class="pf-c-button pf-m-secondary" type="button">Back</button>
              <div class="pf-c-wizard__footer-cancel">
                <button class="pf-c-button pf-m-link" type="button">Cancel</button>
              </div>
            </footer>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### In page nav expanded (mobile)

```html isFullscreen
<div class="pf-c-page" id="wizard-in-page-expanded">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-wizard-in-page-expanded"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="wizard-in-page-expanded-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="wizard-in-page-expanded-primary-nav"
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
              id="wizard-in-page-expanded-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="wizard-in-page-expanded-dropdown-kebab-1-button"
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
              id="wizard-in-page-expanded-dropdown-kebab-2-button"
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
              aria-labelledby="wizard-in-page-expanded-dropdown-kebab-2-button"
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
        id="wizard-in-page-expanded-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Policy</a>
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
    id="main-content-wizard-in-page-expanded"
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
    <section class="pf-c-page__main-wizard pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div class="pf-c-wizard">
          <button
            aria-label="Wizard Header Toggle"
            class="pf-c-wizard__toggle pf-m-expanded"
            aria-expanded="true"
          >
            <span class="pf-c-wizard__toggle-list">
              <span class="pf-c-wizard__toggle-list-item">
                <span class="pf-c-wizard__toggle-num">2</span>
                Configuration
                <i
                  class="fas fa-angle-right pf-c-wizard__toggle-separator"
                  aria-hidden="true"
                ></i>
              </span>
              <span class="pf-c-wizard__toggle-list-item">Substep B</span>
            </span>
            <span class="pf-c-wizard__toggle-icon">
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </span>
          </button>
          <div class="pf-c-wizard__outer-wrap">
            <div class="pf-c-wizard__inner-wrap">
              <nav class="pf-c-wizard__nav pf-m-expanded" aria-label="Steps">
                <ol class="pf-c-wizard__nav-list">
                  <li class="pf-c-wizard__nav-item">
                    <button class="pf-c-wizard__nav-link">Information</button>
                  </li>
                  <li class="pf-c-wizard__nav-item">
                    <button
                      class="pf-c-wizard__nav-link pf-m-current"
                    >Configuration</button>
                    <ol class="pf-c-wizard__nav-list">
                      <li class="pf-c-wizard__nav-item">
                        <button class="pf-c-wizard__nav-link">Substep A</button>
                      </li>
                      <li class="pf-c-wizard__nav-item">
                        <button
                          class="pf-c-wizard__nav-link pf-m-current"
                          aria-current="page"
                        >Substep B</button>
                      </li>
                      <li class="pf-c-wizard__nav-item">
                        <button class="pf-c-wizard__nav-link">Substep C</button>
                      </li>
                    </ol>
                  </li>
                  <li class="pf-c-wizard__nav-item">
                    <button class="pf-c-wizard__nav-link">Additional</button>
                  </li>
                  <li class="pf-c-wizard__nav-item">
                    <button
                      class="pf-c-wizard__nav-link pf-m-disabled"
                      aria-disabled="true"
                      tabindex="-1"
                    >Review</button>
                  </li>
                </ol>
              </nav>
              <div class="pf-c-wizard__main">
                <div class="pf-c-wizard__main-body">
                  <form novalidate class="pf-c-form">
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field1">
                          <span class="pf-c-form__label-text">Field 1</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field1"
                          name="-form-field1"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field2">
                          <span class="pf-c-form__label-text">Field 2</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field2"
                          name="-form-field2"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field3">
                          <span class="pf-c-form__label-text">Field 3</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field3"
                          name="-form-field3"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field4">
                          <span class="pf-c-form__label-text">Field 4</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field4"
                          name="-form-field4"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field5">
                          <span class="pf-c-form__label-text">Field 5</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field5"
                          name="-form-field5"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field6">
                          <span class="pf-c-form__label-text">Field 6</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field6"
                          name="-form-field6"
                        />
                      </div>
                    </div>
                    <div class="pf-c-form__group">
                      <div class="pf-c-form__group-label">
                        <label class="pf-c-form__label" for="-form-field7">
                          <span class="pf-c-form__label-text">Field 7</span>
                          <span
                            class="pf-c-form__label-required"
                            aria-hidden="true"
                          >&#42;</span>
                        </label>
                      </div>
                      <div class="pf-c-form__group-control">
                        <input
                          class="pf-c-form-control"
                          required
                          type="text"
                          id="-form-field7"
                          name="-form-field7"
                        />
                      </div>
                    </div>
                  </form>
                </div>
              </div>
            </div>
            <footer class="pf-c-wizard__footer">
              <button class="pf-c-button pf-m-primary" type="submit">Next</button>
              <button class="pf-c-button pf-m-secondary" type="button">Back</button>
              <div class="pf-c-wizard__footer-cancel">
                <button class="pf-c-button pf-m-link" type="button">Cancel</button>
              </div>
            </footer>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```
