---
id: About modal
section: components
cssPrefix: pf-c-about-modal-box
---## Examples

### Basic

```html isFullscreen
<div
  class="pf-c-about-modal-box"
  role="dialog"
  aria-modal="true"
  aria-labelledby="about-modal-title"
>
  <div class="pf-c-about-modal-box__brand">
    <img
      class="pf-c-about-modal-box__brand-image"
      src="/assets/images/pf_mini_logo_white.svg"
      alt="PatternFly brand logo"
    />
  </div>
  <div class="pf-c-about-modal-box__close">
    <button
      class="pf-c-button pf-m-plain"
      type="button"
      aria-label="Close dialog"
    >
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
  </div>
  <div class="pf-c-about-modal-box__header">
    <h1 class="pf-c-title pf-m-4xl" id="about-modal-title">Product name</h1>
  </div>
  <div class="pf-c-about-modal-box__hero"></div>
  <div class="pf-c-about-modal-box__content">
    <div class="pf-c-about-modal-box__body">content</div>
    <p
      class="pf-c-about-modal-box__strapline"
    >Trademark and copyright information here</p>
  </div>
</div>

```

## Documentation

### Accessibility

| Attribute                                                  | Applies to                                                          | Outcome                                                                                                                                                                                                                                                               |
| ---------------------------------------------------------- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="dialog"`                                            | `.pf-c-about-modal-box`                                             | Identifies the element that serves as the modal container. **Required**                                                                                                                                                                                               |
| `aria-labelledby="[id value of element describing modal]"` | `.pf-c-about-modal-box`                                             | Gives the modal an accessible name by referring to the element that provides the dialog title. **Required when adequate titling element is present**                                                                                                                  |
| `aria-label="[title of modal]"`                            | `.pf-c-about-modal-box`                                             | Gives the modal an accessible name. **Required when adequate titling element is _not_ present**                                                                                                                                                                       |
| `aria-describedby="[id value of applicable content]"`      | `.pf-c-about-modal-box`                                             | Gives the modal an accessible description by referring to the modal content that describes the primary message or purpose of the dialog. Not used if there is no static text that describes the modal.                                                                |
| `aria-modal="true"`                                        | `.pf-c-modal-box`                                                   | Tells assistive technologies that the windows underneath the current modal are not available for interaction. **Required**                                                                                                                                            |
| `aria-label="Close Dialog"`                                | `.pf-c-modal-box__close .pf-c-button`                               | Provides an accessible name for the close button as it uses an icon instead of text. **Required**                                                                                                                                                                     |
| `aria-hidden="true"`                                       | Parent element containing the page contents when the modal is open. | Hides main contents of the page from screen readers. The element with `.pf-c-modal-box` must not be a descendent of the element with `aria-hidden="true"`. For more info, see [trapping focus](/accessibility/product-development-guide#trapping-focus). **Required** |

### Usage

| Class                                | Applied to           | Outcome                               |
| ------------------------------------ | -------------------- | ------------------------------------- |
| `.pf-c-about-modal-box`              | `<div>`, `<article>` | Initiates a modal box.                |
| `.pf-c-about-modal-box__brand`       | `<div>`              | Initiates a modal box brand cell.     |
| `.pf-c-about-modal-box__brand-image` | `<img>`              | Initiates a modal box brand image.    |
| `.pf-c-about-modal-box__close`       | `<div>`              | Initiates a modal box close cell.     |
| `.pf-c-about-modal-box__header`      | `<div>`, `<header>`  | Initiates a modal box header cell.    |
| `.pf-c-about-modal-box__hero`        | `<div>`              | Initiates a modal box hero cell.      |
| `.pf-c-about-modal-box__content`     | `<div>`              | Initiates a modal box content cell.   |
| `.pf-c-about-modal-box__body`        | `<div>`              | Initiates a modal box body cell.      |
| `.pf-c-about-modal-box__strapline`   | `<p>`                | Initiates a modal box strapline cell. |
