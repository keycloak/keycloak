---
title: Modal box
section: components
cssPrefix: pf-c-modal-box
---

## Examples
```hbs title=Basic
{{#> modal-box modal-box--attribute='aria-labelledby="modal-title" aria-describedby="modal-description"'}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close"'}}
   <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
  {{#> modal-box-header}}
    {{#> modal-box-title modal-box-title--attribute='id="modal-title"'}}
      Modal title
    {{/modal-box-title}}
  {{/modal-box-header}}
  {{#> modal-box-body modal-box-body--attribute='id="modal-description"'}}
    To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.
  {{/modal-box-body}}
  {{#> modal-box-footer}}
    Modal footer
  {{/modal-box-footer}}
{{/modal-box}}
```

```hbs title=Small
{{#> modal-box modal-box--modifier="pf-m-sm" modal-box--attribute='aria-labelledby="modal-sm-title" aria-describedby="modal-sm-description"'}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close dialog"'}}
    <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
  {{#> modal-box-header}}
    {{#> modal-box-title modal-box-title--attribute='id="modal-sm-title"'}}
      Modal title
    {{/modal-box-title}}
  {{/modal-box-header}}
  {{#> modal-box-body modal-box-body--attribute='id="modal-sm-description"'}}
    Static text describing modal purpose. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
    tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
    quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
    consequat.
  {{/modal-box-body}}
  {{#> modal-box-footer}}
    Modal footer
  {{/modal-box-footer}}
{{/modal-box}}
```

```hbs title=Medium
{{#> modal-box modal-box--modifier="pf-m-md" modal-box--attribute='aria-labelledby="modal-md-title" aria-describedby="modal-md-description"'}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close dialog"'}}
    <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
  {{#> modal-box-header}}
    {{#> modal-box-title modal-box-title--attribute='id="modal-md-title"'}}
      Modal title
    {{/modal-box-title}}
  {{/modal-box-header}}
  {{#> modal-box-body modal-box-body--attribute='id="modal-md-description"'}}
    Static text describing modal purpose. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
    tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
    quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
    consequat.
  {{/modal-box-body}}
  {{#> modal-box-footer}}
    Modal footer
  {{/modal-box-footer}}
{{/modal-box}}
```

```hbs title=Large
{{#> modal-box modal-box--modifier="pf-m-lg" modal-box--attribute='aria-labelledby="modal-lg-title" aria-describedby="modal-lg-description"'}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close"'}}
    <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
  {{#> modal-box-header}}
    {{#> modal-box-title modal-box-title--attribute='id="modal-lg-title"'}}
      Modal title
    {{/modal-box-title}}
  {{/modal-box-header}}
  {{#> modal-box-body modal-box-body--attribute='id="modal-lg-description"'}}
    Static text describing modal purpose. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod
    tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam,
    quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
    consequat.
  {{/modal-box-body}}
  {{#> modal-box-footer}}
    Modal footer
  {{/modal-box-footer}}
{{/modal-box}}
```

```hbs title=Without-title
{{#> modal-box modal-box--attribute='aria-label="Example of a modal without a title" aria-describedby="modal-no-title-description"'}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close"'}}
    <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
  {{#> modal-box-body}}
    <span id="modal-no-title-description">When static text describing the modal is available, it can be wrapped with an ID referring to the modal's aria-describedby value. Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</span> Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  {{/modal-box-body}}
  {{#> modal-box-footer}}
    Modal footer
  {{/modal-box-footer}}
{{/modal-box}}
```

```hbs title=With-description
{{#> modal-box modal-box--attribute='aria-labelledby="modal-with-description-title" aria-describedby="modal-with-description-description"'}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close"'}}
   <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
  {{#> modal-box-header}}
    {{#> modal-box-title modal-box-title--attribute='id="modal-with-description-title"'}}
      Modal title
    {{/modal-box-title}}
    {{#> modal-box-description modal-box-description--attribute='id="modal-with-description-description"'}}
      A description is used when you want to provide more info about the modal than the title is able to describe. The content in the description is static and will not scroll with the rest of the modal body.
    {{/modal-box-description}}
  {{/modal-box-header}}
  {{#> modal-box-body}}
    To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.
  {{/modal-box-body}}
  {{#> modal-box-footer}}
    Modal footer
  {{/modal-box-footer}}
{{/modal-box}}
```

```hbs title=Custom-title
{{#> modal-box modal-box--attribute='aria-labelledby="modal-custom-title" aria-describedby="modal-custom-description"'}}
  {{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Close"'}}
   <i class="fas fa-times" aria-hidden="true"></i>
  {{/button}}
  {{#> modal-box-header}}
    {{#> title title--modifier="pf-m-4xl" title--attribute='id="modal-custom-title"'}}Custom title{{/title}}
  {{/modal-box-header}}
  {{#> modal-box-body modal-box-body--attribute='id="modal-custom-description"'}}
    To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.
  {{/modal-box-body}}
  {{#> modal-box-footer}}
    Modal footer
  {{/modal-box-footer}}
{{/modal-box}}
```

```hbs title=Modal-box-as-generic-container
{{#> modal-box modal-box--attribute='aria-labelledby="modal-generic-container-description"'}}
  <p id="modal-generic-container-description">The modal box children elements can be removed, and the modal serves as a generic modal container. One use case of this is when creating a wizard in a modal.</p>
{{/modal-box}}
```

## Documentation
### Overview
A modal box is a generic rectangular container that can be used to build modals. A modal box can have the following sections: header, title, description, body, and footer. With normal use of the modal, a title or body is required. Alternatively, no child elements can be used, and the `.pf-c-modal-box` container will  serve as a generic container with no padding for custom modal content. If no `.pf-c-modal-box__title` is used, `aria-label="[title of modal]"` must be provided for `.pf-c-modal-box`.


### Accessibility
| Attribute | Applies to | Outcome |
| -- | -- | -- |
| `role="dialog"` | `.pf-c-modal-box` | Identifies the element that serves as the modal container. **Required**|
| `aria-labelledby="[id value of .pf-c-modal-box__title or custom modal title]"` | `.pf-c-modal-box` | Gives the modal an accessible name by referring to the element that provides the dialog title. **Required when .pf-c-title is present** |
| `aria-label="[title of modal]"` | `.pf-c-modal-box` | Gives the modal an accessible name. **Required when `.pf-c-modal-box__title` is _not_ present** |
| `aria-describedby="[id value of applicable content]"` | `.pf-c-modal-box` | Gives the modal an accessible description by referring to the modal content that describes the primary message or purpose of the dialog. Not used if there is no static text that describes the modal. |
| `aria-modal="true"` | `.pf-c-modal-box` | Tells assistive technologies that the windows underneath the current modal are not available for interaction. **Required**|
| `aria-label="Close"` | `.pf-c-modal-box__close .pf-c-button` | Provides an accessible name for the close button as it uses an icon instead of text. **Required**|
| `aria-hidden="true"` | Parent element containing the page contents when modal is open | Hides main contents of the page from screen readers. The element with `.pf-c-modal-box` must not be a descendent of the element with `aria-hidden="true"`. For more info see [trapping focus](/accessibility-guide#trapping-focus). **Required** |

### Usage
| Class | Applied | Outcome |
| -- | -- | -- |
| `.pf-c-modal-box` | `<div>` | Initiates a modal box. **Required** |
| `.pf-c-button.pf-m-plain` | `<button>` | Initiates a modal box close button. |
| `.pf-c-modal-box__header` | `<header>` | Initiates a modal box header. **Required** if using a `.pf-c-modal-box__title`. |
| `.pf-c-modal-box__title` | `<h1>`,`<h2>`,`<h3>`,`<h4>`,`<h5>`,`<h6>`, `<div>` | Initiates a modal box title. |
| `.pf-c-modal-box__description` | `<div>` | Initiates a modal box description. A modal title is **required** if using a modal description. |
| `.pf-c-modal-box__body` | `<div>` | Initiates a modal box body. |
| `.pf-c-modal-box__footer` | `<footer>` | Initiates a modal box footer. |
| `.pf-m-sm` | `.pf-c-modal-box` | Modifies for a small modal box width. |
| `.pf-m-md` | `.pf-c-modal-box` | Modifies for a medium modal box width. |
| `.pf-m-lg` | `.pf-c-modal-box` | Modifies for a large modal box width. |
