---
id: Modal
section: components
cssPrefix: pf-c-modal-box
---## Examples

### Basic

```html
<div
  class="pf-c-modal-box"
  aria-modal="true"
  aria-labelledby="modal-title"
  aria-describedby="modal-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title" id="modal-title">Modal title</h1>
  </header>
  <div
    class="pf-c-modal-box__body"
    id="modal-description"
  >To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### With help button

```html
<div
  class="pf-c-modal-box"
  aria-modal="true"
  aria-labelledby="modal-help-title"
  aria-describedby="modal-help-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header pf-m-help">
    <div class="pf-c-modal-box__header-main">
      <h1
        class="pf-c-modal-box__title"
        id="modal-help-title"
      >Modal title Modal title Modal title Modal title Modal title Modal title Modal title Modal title</h1>
      <div
        class="pf-c-modal-box__description"
        id="modal-help-description"
      >A description is used when you want to provide more info about the modal than the title is able to describe. The content in the description is static and will not scroll with the rest of the modal body.</div>
    </div>
    <div class="pf-c-modal-box__header-help">
      <button class="pf-c-button pf-m-plain" type="button" aria-label="Help">
        <i class="pficon pf-icon-help" aria-hidden="true"></i>
      </button>
    </div>
  </header>
  <div
    class="pf-c-modal-box__body"
  >To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Small

```html
<div
  class="pf-c-modal-box pf-m-sm"
  aria-modal="true"
  aria-labelledby="modal-sm-title"
  aria-describedby="modal-sm-description"
>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Close dialog"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title" id="modal-sm-title">Modal title</h1>
  </header>
  <div class="pf-c-modal-box__body" id="modal-sm-description">
    Static text describing modal purpose. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
    tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
    quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
    consequat.
  </div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Medium

```html
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
    <h1 class="pf-c-modal-box__title" id="modal-md-title">Modal title</h1>
  </header>
  <div class="pf-c-modal-box__body" id="modal-md-description">
    Static text describing modal purpose. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
    tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
    quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
    consequat.
  </div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Large

```html
<div
  class="pf-c-modal-box pf-m-lg"
  aria-modal="true"
  aria-labelledby="modal-lg-title"
  aria-describedby="modal-lg-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title" id="modal-lg-title">Modal title</h1>
  </header>
  <div class="pf-c-modal-box__body" id="modal-lg-description">
    Static text describing modal purpose. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
    tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
    quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
    consequat.
  </div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Without title

```html
<div
  class="pf-c-modal-box"
  aria-modal="true"
  aria-label="Example of a modal without a title"
  aria-describedby="modal-no-title-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <div class="pf-c-modal-box__body">
    <span
      id="modal-no-title-description"
    >When static text describing the modal is available, it can be wrapped with an ID referring to the modal's aria-describedby value. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</span> Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  </div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### With description

```html
<div
  class="pf-c-modal-box"
  aria-modal="true"
  aria-labelledby="modal-with-description-title"
  aria-describedby="modal-with-description-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1
      class="pf-c-modal-box__title"
      id="modal-with-description-title"
    >Modal title</h1>
    <div
      class="pf-c-modal-box__description"
      id="modal-with-description-description"
    >A description is used when you want to provide more info about the modal than the title is able to describe. The content in the description is static and will not scroll with the rest of the modal body.</div>
  </header>
  <div
    class="pf-c-modal-box__body"
  >To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Custom title

```html
<div
  class="pf-c-modal-box"
  aria-modal="true"
  aria-labelledby="modal-custom-title"
  aria-describedby="modal-custom-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-title pf-m-4xl" id="modal-custom-title">Custom title</h1>
  </header>
  <div
    class="pf-c-modal-box__body"
    id="modal-custom-description"
  >To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Modal box as generic container

```html
<div
  class="pf-c-modal-box"
  aria-modal="true"
  aria-labelledby="modal-generic-container-description"
>
  <p
    id="modal-generic-container-description"
  >The modal box children elements can be removed, and the modal serves as a generic modal container. One use case of this is when creating a wizard in a modal.</p>
</div>

```

### Icon

```html
<div
  class="pf-c-modal-box"
  aria-modal="true"
  aria-labelledby="icon-title"
  aria-describedby="icon-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title pf-m-icon" id="icon-title">
      <span class="pf-c-modal-box__title-icon">
        <i class="fas fa-fw fa-bullhorn" aria-hidden="true"></i>
      </span>
      <span class="pf-c-modal-box__title-text">Modal with icon title</span>
    </h1>
  </header>
  <div class="pf-c-modal-box__body" id="icon-description">Modal description</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Default alert

```html
<div
  class="pf-c-modal-box pf-m-default"
  aria-modal="true"
  aria-labelledby="default-alert-title"
  aria-describedby="default-alert-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title pf-m-icon" id="default-alert-title">
      <span class="pf-c-modal-box__title-icon">
        <i class="fas fa-fw fa-bell" aria-hidden="true"></i>
      </span>
      <span class="pf-u-screen-reader">
        Default
        alert:
      </span>
      <span class="pf-c-modal-box__title-text">Default alert modal title</span>
    </h1>
  </header>
  <div
    class="pf-c-modal-box__body"
    id="default-alert-description"
  >Modal description</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Info alert

```html
<div
  class="pf-c-modal-box pf-m-info"
  aria-modal="true"
  aria-labelledby="info-alert-title"
  aria-describedby="info-alert-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title pf-m-icon" id="info-alert-title">
      <span class="pf-c-modal-box__title-icon">
        <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
      </span>
      <span class="pf-u-screen-reader">
        Info
        alert:
      </span>
      <span class="pf-c-modal-box__title-text">Info alert modal title</span>
    </h1>
  </header>
  <div
    class="pf-c-modal-box__body"
    id="info-alert-description"
  >Modal description</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Success alert

```html
<div
  class="pf-c-modal-box pf-m-success"
  aria-modal="true"
  aria-labelledby="success-alert-title"
  aria-describedby="success-alert-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title pf-m-icon" id="success-alert-title">
      <span class="pf-c-modal-box__title-icon">
        <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
      </span>
      <span class="pf-u-screen-reader">
        Success
        alert:
      </span>
      <span class="pf-c-modal-box__title-text">Success alert modal title</span>
    </h1>
  </header>
  <div
    class="pf-c-modal-box__body"
    id="success-alert-description"
  >Modal description</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Warning alert

```html
<div
  class="pf-c-modal-box pf-m-warning"
  aria-modal="true"
  aria-labelledby="warning-alert-title"
  aria-describedby="warning-alert-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title pf-m-icon" id="warning-alert-title">
      <span class="pf-c-modal-box__title-icon">
        <i class="fas fa-fw fa-exclamation-triangle" aria-hidden="true"></i>
      </span>
      <span class="pf-u-screen-reader">
        Warning
        alert:
      </span>
      <span class="pf-c-modal-box__title-text">Warning alert modal title</span>
    </h1>
  </header>
  <div
    class="pf-c-modal-box__body"
    id="warning-alert-description"
  >Modal description</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

### Danger alert

```html
<div
  class="pf-c-modal-box pf-m-danger"
  aria-modal="true"
  aria-labelledby="danger-alert-title"
  aria-describedby="danger-alert-description"
>
  <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
  <header class="pf-c-modal-box__header">
    <h1 class="pf-c-modal-box__title pf-m-icon" id="danger-alert-title">
      <span class="pf-c-modal-box__title-icon">
        <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
      </span>
      <span class="pf-u-screen-reader">
        Danger
        alert:
      </span>
      <span class="pf-c-modal-box__title-text">Danger alert modal title</span>
    </h1>
  </header>
  <div
    class="pf-c-modal-box__body"
    id="danger-alert-description"
  >To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.</div>
  <footer class="pf-c-modal-box__footer">Modal footer</footer>
</div>

```

## Documentation

### Overview

A modal box is a generic rectangular container that can be used to build modals. A modal box can have the following sections: header, title, description, body, and footer. With normal use of the modal, a title or body is required. Alternatively, no child elements can be used, and the `.pf-c-modal-box` container will  serve as a generic container with no padding for custom modal content. If no `.pf-c-modal-box__title` is used, `aria-label="[title of modal]"` must be provided for `.pf-c-modal-box`.

### Accessibility

| Attribute                                                                      | Applies to                                                     | Outcome                                                                                                                                                                                                                                                              |
| ------------------------------------------------------------------------------ | -------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="dialog"`                                                                | `.pf-c-modal-box`                                              | Identifies the element that serves as the modal container. **Note:** `role="dialog"` is not included on the static examples on this page as it interferes with VoiceOver. Refer to the react examples to see the role in use. **Required**                           |
| `aria-labelledby="[id value of .pf-c-modal-box__title or custom modal title]"` | `.pf-c-modal-box`                                              | Gives the modal an accessible name by referring to the element that provides the dialog title. **Required when .pf-c-title is present**                                                                                                                              |
| `aria-label="[title of modal]"`                                                | `.pf-c-modal-box`                                              | Gives the modal an accessible name. **Required when `.pf-c-modal-box__title` is _not_ present**                                                                                                                                                                      |
| `aria-describedby="[id value of applicable content]"`                          | `.pf-c-modal-box`                                              | Gives the modal an accessible description by referring to the modal content that describes the primary message or purpose of the dialog. Not used if there is no static text that describes the modal.                                                               |
| `aria-modal="true"`                                                            | `.pf-c-modal-box`                                              | Tells assistive technologies that the windows underneath the current modal are not available for interaction. **Required**                                                                                                                                           |
| `aria-label="Close"`                                                           | `.pf-c-modal-box__close .pf-c-button`                          | Provides an accessible name for the close button as it uses an icon instead of text. **Required**                                                                                                                                                                    |
| `aria-hidden="true"`                                                           | Parent element containing the page contents when modal is open | Hides main contents of the page from screen readers. The element with `.pf-c-modal-box` must not be a descendent of the element with `aria-hidden="true"`. For more info see [trapping focus](/accessibility/product-development-guide#trapping-focus). **Required** |
| `form="[id of form in modal body]"`                                            | `.pf-c-modal-box__footer .pf-c-button`                         | Associates a submit button in the modal footer with a form in the modal body. For use when the submit button is outside of the `<form>` that the button submits.                                                                                                     |
| `tabindex="0"`                                                                 | `.pf-c-modal-box__body`                                        | If a modal box body has overflow content that triggers a scrollbar, to ensure that the content is keyboard accessible, the body must include either a focusable element within the scrollable region or the body itself must be focusable by adding `tabindex="0"`.  |

### Usage

| Class                          | Applied                                            | Outcome                                                                                               |
| ------------------------------ | -------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `.pf-c-modal-box`              | `<div>`                                            | Initiates a modal box. **Required**                                                                   |
| `.pf-c-button.pf-m-plain`      | `<button>`                                         | Initiates a modal box close button.                                                                   |
| `.pf-c-modal-box__header`      | `<header>`                                         | Initiates a modal box header. **Required** if using a `.pf-c-modal-box__title`.                       |
| `.pf-c-modal-box__header-main` | `<div>`                                            | Initiates a modal box header main container. **Required** when `pf-c-modal-box__header-help` is used. |
| `.pf-c-modal-box__header-help` | `<div>`                                            | Initiates the help button container in the modal box header actions.                                  |
| `.pf-c-modal-box__title`       | `<h1>`,`<h2>`,`<h3>`,`<h4>`,`<h5>`,`<h6>`, `<div>` | Initiates a modal box title. **Required** if using a modal description.                               |
| `.pf-c-modal-box__title-icon`  | `<span>`                                           | Initiates a container for the modal box title icon.                                                   |
| `.pf-c-modal-box__title-text`  | `<span>`                                           | Initiates a container for the modal box title text.                                                   |
| `.pf-c-modal-box__description` | `<div>`                                            | Initiates a modal box description.                                                                    |
| `.pf-c-modal-box__body`        | `<div>`                                            | Initiates a modal box body.                                                                           |
| `.pf-c-modal-box__footer`      | `<footer>`                                         | Initiates a modal box footer.                                                                         |
| `.pf-m-sm`                     | `.pf-c-modal-box`                                  | Modifies for a small modal box width.                                                                 |
| `.pf-m-md`                     | `.pf-c-modal-box`                                  | Modifies for a medium modal box width.                                                                |
| `.pf-m-lg`                     | `.pf-c-modal-box`                                  | Modifies for a large modal box width.                                                                 |
| `.pf-m-align-top`              | `.pf-c-modal-box`                                  | Modifies for top alignment.                                                                           |
| `.pf-m-icon`                   | `.pf-c-modal-box__title`                           | Modifies the title layout to accommodate an icon.                                                     |
| `.pf-m-default`                | `.pf-c-modal-box`                                  | Modifies for the default alert state.                                                                 |
| `.pf-m-info`                   | `.pf-c-modal-box`                                  | Modifies for the info alert state.                                                                    |
| `.pf-m-success`                | `.pf-c-modal-box`                                  | Modifies for the success alert state.                                                                 |
| `.pf-m-warning`                | `.pf-c-modal-box`                                  | Modifies for the warning alert state.                                                                 |
| `.pf-m-danger`                 | `.pf-c-modal-box`                                  | Modifies for the danger alert state.                                                                  |
| `.pf-m-help`                   | `.pf-c-modal-box__header`                          | Modifies the modal box header to support the help action                                              |
