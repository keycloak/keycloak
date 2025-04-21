---
id: Wizard
section: components
cssPrefix: pf-c-wizard
wrapperTag: div
---import './Wizard.css'

## Examples

### Basic

```html isFullscreen
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

    <div class="pf-c-wizard__description">Here is where the description goes</div>
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
            <button class="pf-c-wizard__nav-link pf-m-current">Configuration</button>
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
            <button class="pf-c-wizard__nav-link" disabled>Review</button>
          </li>
        </ol>
      </nav>
      <main class="pf-c-wizard__main" tabindex="0">
        <div class="pf-c-wizard__main-body">
          <form novalidate class="pf-c-form">
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label class="pf-c-form__label" for="wizard-basic-form-field1">
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
                  id="wizard-basic-form-field1"
                  name="wizard-basic-form-field1"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label class="pf-c-form__label" for="wizard-basic-form-field2">
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
                  id="wizard-basic-form-field2"
                  name="wizard-basic-form-field2"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label class="pf-c-form__label" for="wizard-basic-form-field3">
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
                  id="wizard-basic-form-field3"
                  name="wizard-basic-form-field3"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label class="pf-c-form__label" for="wizard-basic-form-field4">
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
                  id="wizard-basic-form-field4"
                  name="wizard-basic-form-field4"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label class="pf-c-form__label" for="wizard-basic-form-field5">
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
                  id="wizard-basic-form-field5"
                  name="wizard-basic-form-field5"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label class="pf-c-form__label" for="wizard-basic-form-field6">
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
                  id="wizard-basic-form-field6"
                  name="wizard-basic-form-field6"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label class="pf-c-form__label" for="wizard-basic-form-field7">
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
                  id="wizard-basic-form-field7"
                  name="wizard-basic-form-field7"
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

```

### Nav expanded (mobile)

```html isFullscreen
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

    <div class="pf-c-wizard__description">Here is where the description goes</div>
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
            <button class="pf-c-wizard__nav-link pf-m-current">Configuration</button>
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
            <button class="pf-c-wizard__nav-link" disabled>Review</button>
          </li>
        </ol>
      </nav>
      <main class="pf-c-wizard__main" tabindex="0">
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

```

### With drawer

```html isFullscreen
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

    <div class="pf-c-wizard__description">Here is where the description goes</div>
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
            <button class="pf-c-wizard__nav-link pf-m-current">Configuration</button>
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
            <button class="pf-c-wizard__nav-link" disabled>Review</button>
          </li>
        </ol>
      </nav>
      <main class="pf-c-wizard__main" tabindex="0">
        <div class="pf-c-drawer pf-m-expanded pf-m-inline">
          <div class="pf-c-drawer__main">
            <div class="pf-c-drawer__content">
              <div class="pf-c-wizard__main-body">
                <button
                  class="pf-c-button pf-u-hidden pf-m-link pf-m-inline pf-u-float-right pf-u-ml-md"
                  type="button"
                >Open drawer</button>
                <form novalidate class="pf-c-form">
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label
                        class="pf-c-form__label"
                        for="wizard-with-drawer-example-form-field1"
                      >
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
                        id="wizard-with-drawer-example-form-field1"
                        name="wizard-with-drawer-example-form-field1"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label
                        class="pf-c-form__label"
                        for="wizard-with-drawer-example-form-field2"
                      >
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
                        id="wizard-with-drawer-example-form-field2"
                        name="wizard-with-drawer-example-form-field2"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label
                        class="pf-c-form__label"
                        for="wizard-with-drawer-example-form-field3"
                      >
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
                        id="wizard-with-drawer-example-form-field3"
                        name="wizard-with-drawer-example-form-field3"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label
                        class="pf-c-form__label"
                        for="wizard-with-drawer-example-form-field4"
                      >
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
                        id="wizard-with-drawer-example-form-field4"
                        name="wizard-with-drawer-example-form-field4"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label
                        class="pf-c-form__label"
                        for="wizard-with-drawer-example-form-field5"
                      >
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
                        id="wizard-with-drawer-example-form-field5"
                        name="wizard-with-drawer-example-form-field5"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label
                        class="pf-c-form__label"
                        for="wizard-with-drawer-example-form-field6"
                      >
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
                        id="wizard-with-drawer-example-form-field6"
                        name="wizard-with-drawer-example-form-field6"
                      />
                    </div>
                  </div>
                  <div class="pf-c-form__group">
                    <div class="pf-c-form__group-label">
                      <label
                        class="pf-c-form__label"
                        for="wizard-with-drawer-example-form-field7"
                      >
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
                        id="wizard-with-drawer-example-form-field7"
                        name="wizard-with-drawer-example-form-field7"
                      />
                    </div>
                  </div>
                </form>
              </div>
            </div>
            <div class="pf-c-drawer__panel pf-m-light-200 pf-m-width-33">
              <div class="pf-c-drawer__body">
                <div class="pf-c-drawer__head">
                  <div class="pf-c-drawer__actions">
                    <div class="pf-c-drawer__close">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Close drawer panel"
                      >
                        <i class="fas fa-times" aria-hidden="true"></i>
                      </button>
                    </div>
                  </div>drawer-panel
                </div>
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
      </main>
    </div>
  </div>
</div>

```

### Expandable collapsed

```html isFullscreen
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

    <div class="pf-c-wizard__description">Here is where the description goes</div>
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
            <button
              class="pf-c-wizard__nav-link pf-m-current"
              aria-current="page"
            >Information</button>
          </li>
          <li class="pf-c-wizard__nav-item pf-m-expandable">
            <button class="pf-c-wizard__nav-link" aria-expanded="false">
              <span class="pf-c-wizard__nav-link-text">Configuration</span>
              <span class="pf-c-wizard__nav-link-toggle">
                <span class="pf-c-wizard__nav-link-toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <ol class="pf-c-wizard__nav-list">
              <li class="pf-c-wizard__nav-item">
                <button class="pf-c-wizard__nav-link">Substep A</button>
              </li>
              <li class="pf-c-wizard__nav-item">
                <button class="pf-c-wizard__nav-link">Substep B</button>
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
            <button class="pf-c-wizard__nav-link" disabled>Review</button>
          </li>
        </ol>
      </nav>
      <main class="pf-c-wizard__main" tabindex="0">
        <div class="pf-c-wizard__main-body">
          <form novalidate class="pf-c-form">
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-collapsed-form-field1"
                >
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
                  id="wizard-expandable-collapsed-form-field1"
                  name="wizard-expandable-collapsed-form-field1"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-collapsed-form-field2"
                >
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
                  id="wizard-expandable-collapsed-form-field2"
                  name="wizard-expandable-collapsed-form-field2"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-collapsed-form-field3"
                >
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
                  id="wizard-expandable-collapsed-form-field3"
                  name="wizard-expandable-collapsed-form-field3"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-collapsed-form-field4"
                >
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
                  id="wizard-expandable-collapsed-form-field4"
                  name="wizard-expandable-collapsed-form-field4"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-collapsed-form-field5"
                >
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
                  id="wizard-expandable-collapsed-form-field5"
                  name="wizard-expandable-collapsed-form-field5"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-collapsed-form-field6"
                >
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
                  id="wizard-expandable-collapsed-form-field6"
                  name="wizard-expandable-collapsed-form-field6"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-collapsed-form-field7"
                >
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
                  id="wizard-expandable-collapsed-form-field7"
                  name="wizard-expandable-collapsed-form-field7"
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

```

### Expandable expanded

```html isFullscreen
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

    <div class="pf-c-wizard__description">Here is where the description goes</div>
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
          <li class="pf-c-wizard__nav-item pf-m-expandable pf-m-expanded">
            <button
              class="pf-c-wizard__nav-link pf-m-current"
              aria-expanded="true"
            >
              <span class="pf-c-wizard__nav-link-text">Configuration</span>
              <span class="pf-c-wizard__nav-link-toggle">
                <span class="pf-c-wizard__nav-link-toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
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
            <button class="pf-c-wizard__nav-link" disabled>Review</button>
          </li>
        </ol>
      </nav>
      <main class="pf-c-wizard__main" tabindex="0">
        <div class="pf-c-wizard__main-body">
          <form novalidate class="pf-c-form">
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-expanded-form-field1"
                >
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
                  id="wizard-expandable-expanded-form-field1"
                  name="wizard-expandable-expanded-form-field1"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-expanded-form-field2"
                >
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
                  id="wizard-expandable-expanded-form-field2"
                  name="wizard-expandable-expanded-form-field2"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-expanded-form-field3"
                >
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
                  id="wizard-expandable-expanded-form-field3"
                  name="wizard-expandable-expanded-form-field3"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-expanded-form-field4"
                >
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
                  id="wizard-expandable-expanded-form-field4"
                  name="wizard-expandable-expanded-form-field4"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-expanded-form-field5"
                >
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
                  id="wizard-expandable-expanded-form-field5"
                  name="wizard-expandable-expanded-form-field5"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-expanded-form-field6"
                >
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
                  id="wizard-expandable-expanded-form-field6"
                  name="wizard-expandable-expanded-form-field6"
                />
              </div>
            </div>
            <div class="pf-c-form__group">
              <div class="pf-c-form__group-label">
                <label
                  class="pf-c-form__label"
                  for="wizard-expandable-expanded-form-field7"
                >
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
                  id="wizard-expandable-expanded-form-field7"
                  name="wizard-expandable-expanded-form-field7"
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

```

### Finished

```html isFullscreen
<div class="pf-c-wizard pf-m-finished">
  <div class="pf-c-wizard__header">
    <button
      class="pf-c-button pf-m-plain pf-c-wizard__close"
      type="button"
      aria-label="Close"
    >
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-3xl pf-c-wizard__title">Wizard title</h1>

    <div class="pf-c-wizard__description">Here is where the description goes</div>
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
            <button class="pf-c-wizard__nav-link">Configuration</button>
            <ol class="pf-c-wizard__nav-list">
              <li class="pf-c-wizard__nav-item">
                <button class="pf-c-wizard__nav-link">Substep A</button>
              </li>
              <li class="pf-c-wizard__nav-item">
                <button class="pf-c-wizard__nav-link">Substep B</button>
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
            <button class="pf-c-wizard__nav-link">Review</button>
          </li>
        </ol>
      </nav>
      <main class="pf-c-wizard__main" tabindex="0">
        <div class="pf-c-wizard__main-body">
          <div class="pf-l-bullseye">
            <div class="pf-c-empty-state pf-m-lg">
              <div class="pf-c-empty-state__content">
                <i
                  class="fas fa- fa-cogs pf-c-empty-state__icon"
                  aria-hidden="true"
                ></i>

                <h1
                  class="pf-c-title pf-m-lg"
                  id="wizard-finished-empty-state-title"
                >Validating credentials</h1>
                <div class="pf-c-empty-state__body">
                  <div
                    class="pf-c-progress pf-m-singleline"
                    id="progress-singleline-example"
                  >
                    <div
                      class="pf-c-progress__description"
                      id="progress-singleline-example-description"
                    ></div>
                    <div class="pf-c-progress__status" aria-hidden="true">
                      <span class="pf-c-progress__measure">33%</span>
                    </div>
                    <div
                      class="pf-c-progress__bar"
                      role="progressbar"
                      aria-valuemin="0"
                      aria-valuemax="100"
                      aria-valuenow="33"
                      aria-labelledby="wizard-finished-empty-state-title"
                      aria-label="Progress status"
                    >
                      <div class="pf-c-progress__indicator" style="width:33%;"></div>
                    </div>
                  </div>
                </div>
                <div
                  class="pf-c-empty-state__body"
                >Description can be used to further elaborate on the validation step, or give the user a better idea of how long the process will take.</div>
                <div class="pf-c-empty-state__secondary">
                  <button class="pf-c-button pf-m-link" type="button">Cancel</button>
                </div>
              </div>
            </div>
          </div>
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

```

## Documentation

### Accessibility

| Attribute               | Applied to                                                  | Outcome                                                                                                                                                                                                                                                                          |
| ----------------------- | ----------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-expanded="true"`  | `.pf-c-wizard__toggle`                                      | Indicates that the steps menu is visible. **Required**                                                                                                                                                                                                                           |
| `aria-expanded="false"` | `.pf-c-wizard__toggle`                                      | Indicates that the steps menu is hidden. **Required**                                                                                                                                                                                                                            |
| `aria-label="close"`    | `.pf-c-wizard__toggle-icon`                                 | Gives the close button an accessible name. **Required**                                                                                                                                                                                                                          |
| `aria-hidden="true"`    | `.pf-c-wizard__toggle-icon`, `.pf-c-wizard__toggle-divider` | Hides the icon from assistive technologies. **Required**                                                                                                                                                                                                                         |
| `aria-label="Steps"`    | `.pf-c-wizard__nav`                                         | Gives the steps nav element an accessible name. **Required**                                                                                                                                                                                                                     |
| `disabled`              | `button.pf-c-wizard__nav-link`                              | Indicates that the element is disabled. **Required when a nav item is disabled**                                                                                                                                                                                                 |
| `aria-disabled="true"`  | `a.pf-c-wizard__nav-link`                                   | Indicates that the element is disabled. **Required for disabled links with `.pf-m-disabled`**                                                                                                                                                                                    |
| `aria-current="page"`   | `.pf-c-wizard__nav-link`                                    | Indicates the current page link. Can only occur once on page. **Required for the current link**                                                                                                                                                                                  |
| `aria-expanded="true"`  | `.pf-c-wizard__nav-link`                                    | Indicates that the link subnav is visible. **Required**                                                                                                                                                                                                                          |
| `aria-expanded="false"` | `.pf-c-wizard__nav-link`                                    | Indicates that the link subnav is hidden. **Required**                                                                                                                                                                                                                           |
| `tabindex="-1"`         | `a.pf-c-wizard__nav-link`                                   | Removes a link from keyboard focus. **Required for disabled links with `.pf-m-disabled`**                                                                                                                                                                                        |
| `tabindex="0"`          | `.pf-c-wizard__main`                                        | If the wizard main section has overflow content that triggers a scrollbar, to ensure that the content is keyboard accessible, the section must include either a focusable element within the scrollable region or the section itself must be focusable by adding `tabindex="0"`. |

### Usage

| Class                                | Applied to                                  | Outcome                                                                                                                               |
| ------------------------------------ | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-wizard`                       | `<div>`                                     | Initiates the wizard component. **Required**                                                                                          |
| `.pf-c-wizard__header`               | `<header>`                                  | Initiates the header. **Required** when the wizard is in a modal. Not recommended to use when the wizard is placed on a page.         |
| `.pf-c-wizard__close`                | `.pf-c-button.pf-m-plain`                   | Initiates the close button. **Required**                                                                                              |
| `.pf-c-wizard__title`                | `.pf-c-title.pf-m-3xl`                      | Initiates the title. **Required**                                                                                                     |
| `.pf-c-wizard__description`          | `<div>`, `<p>`                              | Initiates the description.                                                                                                            |
| `.pf-c-wizard__toggle`               | `<button>`                                  | Initiates the mobile steps menu toggle button. **Required**                                                                           |
| `.pf-c-wizard__toggle-list`          | `<span>`                                    | Initiates the toggle list. **Required**                                                                                               |
| `.pf-c-wizard__toggle-list-item`     | `<span>`                                    | Initiates a toggle list item. **Required**                                                                                            |
| `.pf-c-wizard__toggle-num`           | `<span>`                                    | Initiates the step number. **Required**                                                                                               |
| `.pf-c-wizard__toggle-separator`     | `<i>`                                       | Initiates the separator between steps.                                                                                                |
| `.pf-c-wizard__toggle-icon`          | `<span>`                                    | Initiates the toggle icon wrapper. **Required**                                                                                       |
| `.pf-c-wizard__outer-wrap`           | `<div>`                                     | Initiates the outer wrapper. **Required**                                                                                             |
| `.pf-c-wizard__inner-wrap`           | `<div>`                                     | Initiates the inner wrapper. **Required**                                                                                             |
| `.pf-c-wizard__nav`                  | `<nav>`                                     | Initiates the steps nav. **Required**                                                                                                 |
| `.pf-c-wizard__nav-list`             | `<ol>`                                      | Initiates a list of steps. **Required**                                                                                               |
| `.pf-c-wizard__nav-item`             | `<li>`                                      | Initiates a step list item. **Required**                                                                                              |
| `.pf-c-wizard__nav-link`             | `<a>`                                       | Initiates a step link. **Required**                                                                                                   |
| `.pf-c-wizard__nav-link-text`        | `<span>`                                    | Initiates the link text container. **Required when nav item is expandable**                                                           |
| `.pf-c-wizard__nav-link-toggle`      | `<span>`                                    | Initiates the toggle container. **Required when nav item is expandable**                                                              |
| `.pf-c-wizard__nav-link-toggle-icon` | `<span>`                                    | Initiates the toggle icon container. **Required when nav item is expandable**                                                         |
| `.pf-c-wizard__main`                 | `<main>`, `<div>`                           | Initiates the main container. **Required** Note: use the `<main>` element when when there are no other `<main>` elements on the page. |
| `.pf-c-wizard__main-body`            | `<div>`                                     | Initiates the main container body section. **Required**                                                                               |
| `.pf-c-wizard__footer`               | `<footer>`                                  | Initiates the footer. **Required**                                                                                                    |
| `.pf-c-wizard__footer-cancel`        | `<div>`                                     | Initiates the cancel button. **Required**                                                                                             |
| `.pf-m-expanded`                     | `.pf-c-wizard__toggle`, `.pf-c-wizard__nav` | Modifies the mobile steps toggle and steps menu for the expanded state.                                                               |
| `.pf-m-finished`                     | `.pf-c-wizard`                              | Modifies the wizard for the finished state.                                                                                           |
| `.pf-m-expandable`                   | `.pf-c-wizard__nav-item`                    | Modifies a nav item for the expandable state.                                                                                         |
| `.pf-m-expanded`                     | `.pf-c-wizard__nav-item`                    | Modifies a nav item for the expanded state.                                                                                           |
| `.pf-m-current`                      | `.pf-c-wizard__nav-link`                    | Modifies a step link for the current state. **Required**                                                                              |
| `.pf-m-disabled`                     | `.pf-c-wizard__nav-link`                    | Modifies a step link for the disabled state.                                                                                          |
| `.pf-m-no-padding`                   | `.pf-c-wizard__main-body`                   | Modifies the main container body to remove the padding.                                                                               |
