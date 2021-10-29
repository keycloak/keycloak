---
id: Modal
section: components
---## Demos

### Basic

```html isFullscreen
<div aria-hidden="true">Page content</div>
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-sm"
      aria-modal="true"
      aria-labelledby="modal-title-modal-basic"
      aria-describedby="modal-description-modal-basic"
    >
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Close dialog"
      >
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
      <header class="pf-c-modal-box__header">
        <h1
          class="pf-c-modal-box__title"
          id="modal-title-modal-basic"
        >Overwrite existing file?</h1>
      </header>
      <div class="pf-c-modal-box__body" id="modal-description-modal-basic">
        <p>general_modal_final_finalfinal_v9_actualfinal.sketch</p>
        <p>A file with this name already exists, would you like to overwrite the existing file or save a new copy?</p>
      </div>
      <footer class="pf-c-modal-box__footer">
        <button class="pf-c-button pf-m-primary" type="button">Overwrite</button>
        <button class="pf-c-button pf-m-link" type="button">Save a copy</button>
      </footer>
    </div>
  </div>
</div>

```

### Scrollable content

```html isFullscreen
<div aria-hidden="true">Page content</div>
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-sm"
      aria-modal="true"
      aria-labelledby="modal-scroll-title"
      aria-describedby="modal-scroll-description"
    >
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Close dialog"
      >
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
      <header class="pf-c-modal-box__header">
        <h1
          class="pf-c-modal-box__title"
          id="modal-scroll-title"
        >This is a long header title that will truncate because modal titles should be very short. Use the modal body to provide more info.</h1>
        <div
          class="pf-c-modal-box__description"
          id="modal-scroll-description"
        >This is a modal description. The description will not scroll with the body contents.</div>
      </header>
      <div class="pf-c-modal-box__body">
        <p>general_modal_final_finalfinal_v9_actualfinal.sketch</p>
        <p>A file with this name already exists, would you like to overwrite the existing file or save a new copy?</p>
        <p>Curabitur ligula sapien, tincidunt non, euismod vitae, posuere imperdiet, leo. Integer tincidunt. Integer tincidunt. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.</p>
        <p>
          Duis leo. Praesent blandit laoreet nibh. Ut a nisl id ante tempus hendrerit. Maecenas nec odio et ante tincidunt tempus.
          Ut a nisl id ante tempus hendrerit. Nulla sit amet est. Suspendisse nisl elit, rhoncus eget, elementum ac, condimentum eget, diam. Praesent turpis. Phasellus accumsan cursus velit. Vestibulum purus quam, scelerisque ut, mollis sed, nonummy id, metus. Cras ultricies mi eu turpis hendrerit fringilla. Praesent porttitor, nulla vitae posuere iaculis, arcu nisl dignissim dolor, a pretium mi sem ut ipsum.
        </p>
        <p>Etiam sit amet orci eget eros faucibus tincidunt. Aliquam eu nunc. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Nunc nec neque.</p>
        <p>Ut a nisl id ante tempus hendrerit. Nulla sit amet est. Suspendisse nisl elit, rhoncus eget, elementum ac, condimentum eget, diam. Praesent turpis. Phasellus accumsan cursus velit. Vestibulum purus quam, scelerisque ut, mollis sed, nonummy id, metus. Cras ultricies mi eu turpis hendrerit fringilla. Praesent porttitor, nulla vitae posuere iaculis, arcu nisl dignissim dolor, a pretium mi sem ut ipsum.</p>
        <p>Etiam sit amet orci eget eros faucibus tincidunt. Aliquam eu nunc. Aenean leo ligula, porttitor eu, consequat vitae, eleifend ac, enim. Nunc nec neque.</p>
      </div>
      <footer class="pf-c-modal-box__footer">
        <button class="pf-c-button pf-m-primary" type="button">Overwrite</button>
        <button class="pf-c-button pf-m-link" type="button">Save a copy</button>
      </footer>
    </div>
  </div>
</div>

```

### Medium

```html isFullscreen
<div aria-hidden="true">Page content</div>
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-md"
      aria-modal="true"
      aria-labelledby="modal-md-title"
      aria-describedby="modal-md-description"
    >
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Close dialog"
      >
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
      <header class="pf-c-modal-box__header">
        <h1
          class="pf-c-modal-box__title"
          id="modal-md-title"
        >This is a long header title that will truncate because modal titles should be very short. Use the modal body to provide more info.</h1>
      </header>
      <div class="pf-c-modal-box__body">
        <p
          id="modal-md-description"
        >The "aria-describedby" attribute can be applied to any text that adequately describes the modal's purpose. It does not have to be assigned to ".pf-c-modal-box__body"</p>
        <p>Form here</p>
      </div>
      <footer class="pf-c-modal-box__footer">
        <button class="pf-c-button pf-m-primary" type="button">Save</button>
        <button class="pf-c-button pf-m-link" type="button">Cancel</button>
      </footer>
    </div>
  </div>
</div>

```

### Large

```html isFullscreen
<div aria-hidden="true">Page content</div>
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-lg"
      aria-modal="true"
      aria-labelledby="modal-lg-title"
      aria-describedby="modal-lg-description"
    >
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Close dialog"
      >
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
      <header class="pf-c-modal-box__header">
        <h1
          class="pf-c-modal-box__title"
          id="modal-lg-title"
        >This is a long header title that will truncate because modal titles should be very short. Use the modal body to provide more info.</h1>
      </header>
      <div class="pf-c-modal-box__body">
        <p
          id="modal-lg-description"
        >The "aria-describedby" attribute can be applied to any text that adequately describes the modal's purpose. It does not have to be assigned to ".pf-c-modal-box__body"</p>
        <p>Form here</p>
      </div>
      <footer class="pf-c-modal-box__footer">
        <button class="pf-c-button pf-m-primary" type="button">Save</button>
        <button class="pf-c-button pf-m-link" type="button">Cancel</button>
      </footer>
    </div>
  </div>
</div>

```

### Top aligned

```html isFullscreen
<div aria-hidden="true">Page content</div>
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-sm pf-m-align-top"
      aria-modal="true"
      aria-labelledby="modal-top-aligned-title"
      aria-describedby="modal-top-aligned-description"
    >
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Close dialog"
      >
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
      <header class="pf-c-modal-box__header">
        <h1
          class="pf-c-modal-box__title"
          id="modal-top-aligned-title"
        >Modal header</h1>
      </header>
      <div class="pf-c-modal-box__body">
        <p
          id="modal-top-aligned-description"
        >The "aria-describedby" attribute can be applied to any text that adequately describes the modal's purpose. It does not have to be assigned to ".pf-c-modal-box__body"</p>
        <p>Form here</p>
      </div>
      <footer class="pf-c-modal-box__footer">
        <button class="pf-c-button pf-m-primary" type="button">Save</button>
        <button class="pf-c-button pf-m-link" type="button">Cancel</button>
      </footer>
    </div>
  </div>
</div>

```

### Modal with form

```html isFullscreen
<div aria-hidden="true">Page content</div>
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-sm"
      aria-modal="true"
      aria-labelledby="modal-title-modal-with-form"
      aria-describedby="modal-description-modal-with-form"
    >
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Close dialog"
      >
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
      <header class="pf-c-modal-box__header">
        <h1
          class="pf-c-modal-box__title"
          id="modal-title-modal-with-form"
        >Create account</h1>
      </header>
      <div class="pf-c-modal-box__body" id="modal-description-modal-with-form">
        <p>Enter your personal information below to create an account.</p>
      </div>
      <div class="pf-c-modal-box__body">
        <form novalidate class="pf-c-form" id="modal-with-form-form">
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label class="pf-c-form__label" for="modal-with-form-form-name">
                <span class="pf-c-form__label-text">Name</span>
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
                id="modal-with-form-form-name"
                name="modal-with-form-form-name"
                required
              />
            </div>
          </div>
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label class="pf-c-form__label" for="modal-with-form-form-email">
                <span class="pf-c-form__label-text">E-mail</span>
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
                id="modal-with-form-form-email"
                name="modal-with-form-form-email"
                required
              />
            </div>
          </div>
          <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
              <label
                class="pf-c-form__label"
                for="modal-with-form-form-address"
              >
                <span class="pf-c-form__label-text">Address</span>
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
                id="modal-with-form-form-address"
                name="modal-with-form-form-address"
                required
              />
            </div>
          </div>
        </form>
      </div>
      <footer class="pf-c-modal-box__footer">
        <button
          class="pf-c-button pf-m-primary"
          type="button"
          form="modal-with-form-form"
        >Create</button>
        <button class="pf-c-button pf-m-link" type="button">Cancel</button>
      </footer>
    </div>
  </div>
</div>

```
