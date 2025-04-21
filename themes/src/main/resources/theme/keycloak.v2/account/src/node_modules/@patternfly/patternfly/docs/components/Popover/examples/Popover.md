---
id: Popover
section: components
cssPrefix: pf-c-popover
---import './Popover.css'

## Examples

### Top

```html
<div
  class="pf-c-popover pf-m-top"
  aria-modal="true"
  aria-labelledby="popover-top-header"
  aria-describedby="popover-top-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-top-header">Popover header</h1>
    <div
      class="pf-c-popover__body"
      id="popover-top-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Right

```html
<div
  class="pf-c-popover pf-m-right"
  aria-modal="true"
  aria-labelledby="popover-right-header"
  aria-describedby="popover-right-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-right-header">Popover header</h1>
    <div
      class="pf-c-popover__body"
      id="popover-right-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Bottom

```html
<div
  class="pf-c-popover pf-m-bottom"
  aria-modal="true"
  aria-labelledby="popover-bottom-header"
  aria-describedby="popover-bottom-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-bottom-header">Popover header</h1>
    <div
      class="pf-c-popover__body"
      id="popover-bottom-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Left

```html
<div
  class="pf-c-popover pf-m-left"
  aria-modal="true"
  aria-labelledby="popover-left-header"
  aria-describedby="popover-left-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-left-header">Popover header</h1>
    <div
      class="pf-c-popover__body"
      id="popover-left-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Left with top and bottom positions

```html
<div
  class="pf-c-popover pf-m-left-top"
  aria-modal="true"
  aria-labelledby="popover-left-start-header"
  aria-describedby="popover-left-start-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-left-start-header">Popover header</h1>
    <div
      class="pf-c-popover__body"
      id="popover-left-start-body"
    >This popover is to the left and at the start position</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>
<br />
<div
  class="pf-c-popover pf-m-left-bottom"
  aria-modal="true"
  aria-labelledby="popover-left-end-header"
  aria-describedby="popover-left-end-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-left-end-header">Popover header</h1>
    <div
      class="pf-c-popover__body"
      id="popover-left-end-body"
    >This popover is to the left and at the end position</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Bottom with left and right positions

```html
<div
  class="pf-c-popover pf-m-bottom-left"
  aria-modal="true"
  aria-labelledby="popover-bottom-start-header"
  aria-describedby="popover-bottom-start-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1
      class="pf-c-title pf-m-md"
      id="popover-bottom-start-header"
    >Popover header</h1>
    <div
      class="pf-c-popover__body"
      id="popover-bottom-start-body"
    >This popover is to the bottom and at the start position</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>
<br />
<div
  class="pf-c-popover pf-m-bottom-right"
  aria-modal="true"
  aria-labelledby="popover-bottom-end-header"
  aria-describedby="popover-bottom-end-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-bottom-end-header">Popover header</h1>
    <div
      class="pf-c-popover__body"
      id="popover-bottom-end-body"
    >This popover is to the bottom and at the end position</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Without header/footer

```html
<div
  class="pf-c-popover pf-m-right"
  aria-modal="true"
  aria-label="Popover with no header example"
  aria-describedby="popover-no-header-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <div
      class="pf-c-popover__body"
      id="popover-no-header-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
  </div>
</div>

```

### No padding

```html
<div
  class="pf-c-popover pf-m-right pf-m-no-padding"
  aria-modal="true"
  aria-label="Popover with no padding example"
  aria-describedby="popover-no-padding-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <div
      class="pf-c-popover__body"
      id="popover-no-padding-body"
    >This popover has no padding and is intended for use with content that has its own spacing and should touch the edges of the popover container.</div>
  </div>
</div>

```

### Width auto

```html
<div
  class="pf-c-popover pf-m-right pf-m-width-auto"
  aria-modal="true"
  aria-labelledby="popover-width-auto-header"
  aria-describedby="popover-width-auto-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-width-auto-header">Popover header</h1>
    <div class="pf-c-popover__body" id="popover-width-auto-body">Popovers body</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Popover with icon in the title

```html
<div
  class="pf-c-popover pf-m-left"
  aria-modal="true"
  aria-labelledby="popover-top-header"
  aria-describedby="popover-top-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <header class="pf-c-popover__header">
      <h1 class="pf-c-popover__title pf-m-icon" id="popover-top-header">
        <span class="pf-c-popover__title-icon">
          <i class="fas fa-fw fa-bullhorn" aria-hidden="true"></i>
        </span>
        <span class="pf-c-popover__title-text">Popover with icon title</span>
      </h1>
    </header>
    <div
      class="pf-c-popover__body"
      id="popover-top-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Default alert popover

```html
<div
  class="pf-c-popover pf-m-default pf-m-left"
  aria-modal="true"
  aria-labelledby="popover-top-header"
  aria-describedby="popover-top-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <header class="pf-c-popover__header">
      <h1 class="pf-c-popover__title pf-m-icon" id="popover-top-header">
        <span class="pf-c-popover__title-icon">
          <i class="fas fa-fw fa-bell" aria-hidden="true"></i>
        </span>
        <span class="pf-u-screen-reader">
          Default
          alert:
        </span>
        <span class="pf-c-popover__title-text">Default popover title</span>
      </h1>
    </header>
    <div
      class="pf-c-popover__body"
      id="popover-top-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Info alert popover

```html
<div
  class="pf-c-popover pf-m-info pf-m-top"
  aria-modal="true"
  aria-labelledby="popover-top-header"
  aria-describedby="popover-top-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <header class="pf-c-popover__header">
      <h1 class="pf-c-popover__title pf-m-icon" id="popover-top-header">
        <span class="pf-c-popover__title-icon">
          <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
        </span>
        <span class="pf-u-screen-reader">
          Info
          alert:
        </span>
        <span class="pf-c-popover__title-text">Info popover title</span>
      </h1>
    </header>
    <div
      class="pf-c-popover__body"
      id="popover-top-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Success alert popover

```html
<div
  class="pf-c-popover pf-m-success pf-m-top"
  aria-modal="true"
  aria-labelledby="popover-top-header"
  aria-describedby="popover-top-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <header class="pf-c-popover__header">
      <h1 class="pf-c-popover__title pf-m-icon" id="popover-top-header">
        <span class="pf-c-popover__title-icon">
          <i class="fas fa-fw fa-check-circle" aria-hidden="true"></i>
        </span>
        <span class="pf-u-screen-reader">
          Success
          alert:
        </span>
        <span class="pf-c-popover__title-text">Success popover title</span>
      </h1>
    </header>
    <div
      class="pf-c-popover__body"
      id="popover-top-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Warning alert popover

```html
<div
  class="pf-c-popover pf-m-warning pf-m-top"
  aria-modal="true"
  aria-labelledby="popover-top-header"
  aria-describedby="popover-top-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <header class="pf-c-popover__header">
      <h1 class="pf-c-popover__title pf-m-icon" id="popover-top-header">
        <span class="pf-c-popover__title-icon">
          <i class="fas fa-fw fa-exclamation-triangle" aria-hidden="true"></i>
        </span>
        <span class="pf-u-screen-reader">
          Warning
          alert:
        </span>
        <span class="pf-c-popover__title-text">Warning popover title</span>
      </h1>
    </header>
    <div
      class="pf-c-popover__body"
      id="popover-top-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

### Danger alert popover

```html
<div
  class="pf-c-popover pf-m-danger pf-m-top"
  aria-modal="true"
  aria-labelledby="popover-top-header"
  aria-describedby="popover-top-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <header class="pf-c-popover__header">
      <h1 class="pf-c-popover__title pf-m-icon" id="popover-top-header">
        <span class="pf-c-popover__title-icon">
          <i class="fas fa-fw fa-exclamation-circle" aria-hidden="true"></i>
        </span>
        <span class="pf-u-screen-reader">
          Danger
          alert:
        </span>
        <span class="pf-c-popover__title-text">Danger popover title</span>
      </h1>
    </header>
    <div
      class="pf-c-popover__body"
      id="popover-top-body"
    >Popovers are triggered by click rather than hover. Click again to close.</div>
    <footer class="pf-c-popover__footer">Popover footer</footer>
  </div>
</div>

```

## Documentation

### Overview

A popover is used to provide contextual information for another component on click.  The popover itself is made up of two main elements: arrow and content. The content element follows the pattern of the popover box component, with a close icon in the top right corner, as well as a header and body.  One of the directional modifiers (`.pf-m-left`, `.pf-m-top`, etc.) is required on the popover component

### Accessibility

| Attribute                                             | Applies to                                                            | Outcome                                                                                                                                                                                                                                                             |
| ----------------------------------------------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="dialog"`                                       | `.pf-c-popover`                                                       | Identifies the element that serves as the popover container. **Note:** `role="dialog"` is not included on the static examples on this page as it interferes with VoiceOver. Refer to the react examples to see the role in use. **Required**                        |
| `aria-labelledby="[id value of .pf-c-title]"`         | `.pf-c-popover`                                                       | Gives the popover an accessible name by referring to the element that provides the dialog title. **Required when .pf-c-title is present**                                                                                                                           |
| `aria-label="[title of popover]"`                     | `.pf-c-popover`                                                       | Gives the popover an accessible name. **Required when .pf-c-title is _not_ present**                                                                                                                                                                                |
| `aria-describedby="[id value of applicable content]"` | `.pf-c-popover`                                                       | Gives the popover an accessible description by referring to the popover content that describes the primary message or purpose of the dialog. Not used if there is no static text that describes the popover.                                                        |
| `aria-modal="true"`                                   | `.pf-c-popover`                                                       | Tells assistive technologies that the windows underneath the current popover are not available for interaction. **Required**                                                                                                                                        |
| `aria-label="Close"`                                  | `.pf-c-button`                                                        | Provides an accessible name for the close button as it uses an icon instead of text. **Required**                                                                                                                                                                   |
| `aria-hidden="true"`                                  | Parent element containing the page contents when the popover is open. | Hides main contents of the page from screen readers. The element with `.pf-c-popover` must not be a descendent of the element with `aria-hidden="true"`. For more info, see [trapping focus](/accessibility/product-development-guide#trapping-focus). **Required** |

### Usage

| Class                       | Applied to                                        | Outcome                                                                                                                                   |
| --------------------------- | ------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-popover`             | `<div>`                                           | Creates a popover. Always use it with a modifier class that positions the popover relative to the element that triggered it. **Required** |
| `.pf-c-popover__arrow`      | `<div>`                                           | Creates an arrow pointing towards the element the popover describes. **Required**                                                         |
| `.pf-c-popover__content`    | `<div>`                                           | Creates the content area of the popover. **Required**                                                                                     |
| `.pf-c-button`              | `<button>`                                        | Positions the close icon in the top-right corner of the popover. **Required**                                                             |
| `.pf-c-popover__header`     | `<header>`                                        | Creates the popover header                                                                                                                |
| `.pf-c-popover__title`      | `<h1>`,`<h2>`,`<h3>`,`<h4>`,`<h5>`,`<h6>`,`<div>` | Creates the popover title                                                                                                                 |
| `.pf-c-title`               | `<h1>`,`<h2>`,`<h3>`,`<h4>`,`<h5>`,`<h6>`         | Initiates a title. Always use it with a modifier class. See the [title component](/components/title) for more info.                       |
| `.pf-c-popover__title-icon` | `<span>`                                          | Creates the popover title icon                                                                                                            |
| `.pf-c-popover__title-text` | `<span>`                                          | Creates the popover title text                                                                                                            |
| `.pf-c-popover__body`       | `<div>`                                           | The popover's body text. **Required**                                                                                                     |
| `.pf-c-popover__footer`     | `<footer>`                                        | Initiates a popover footer.                                                                                                               |
| `.pf-m-left{-top/bottom}`   | `.pf-c-popover`                                   | Positions the popover to the left (or left top/left bottom) of the element.                                                               |
| `.pf-m-right{-top/bottom}`  | `.pf-c-popover`                                   | Positions the popover to the right (or right top/right bottom) of the element.                                                            |
| `.pf-m-top{-left/right}`    | `.pf-c-popover`                                   | Positions the popover to the top (or top left/top right) of the element.                                                                  |
| `.pf-m-bottom{-left/right}` | `.pf-c-popover`                                   | Positions the popover to the bottom (or bottom left/bottom right) of the element.                                                         |
| `.pf-m-no-padding`          | `.pf-c-popover`                                   | Removes the outer padding from the popover content.                                                                                       |
| `.pf-m-width-auto`          | `.pf-c-popover`                                   | Allows popover width to be defined by the popover content.                                                                                |
| `.pf-m-icon`                | `.pf-c-popover__title`                            | Modifies the title layout to accommodate an icon.                                                                                         |
| `.pf-m-default`             | `.pf-c-popover`                                   | Modifies for the default alert state.                                                                                                     |
| `.pf-m-info`                | `.pf-c-popover`                                   | Modifies for the info alert state.                                                                                                        |
| `.pf-m-success`             | `.pf-c-popover`                                   | Modifies for the success alert state.                                                                                                     |
| `.pf-m-warning`             | `.pf-c-popover`                                   | Modifies for the warning alert state.                                                                                                     |
| `.pf-m-danger`              | `.pf-c-popover`                                   | Modifies for the danger alert state.                                                                                                      |
