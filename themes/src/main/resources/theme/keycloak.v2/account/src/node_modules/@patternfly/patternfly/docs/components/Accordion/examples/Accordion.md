---
id: Accordion
section: components
cssPrefix: pf-c-accordion
---## Examples

### Fluid

```html
<div class="pf-c-accordion">
  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item one</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item two</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item three</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button
      class="pf-c-accordion__toggle pf-m-expanded"
      type="button"
      aria-expanded="true"
    >
      <span class="pf-c-accordion__toggle-text">Item four</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-expanded">
    <div
      class="pf-c-accordion__expanded-content-body"
    >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis molestie lorem lacinia dolor aliquet faucibus. Suspendisse gravida imperdiet accumsan. Aenean auctor lorem justo, vitae tincidunt enim blandit vel. Aenean quis tempus dolor. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item five</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>
</div>

```

### Fixed

```html
<div class="pf-c-accordion">
  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item one</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-fixed" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button
      class="pf-c-accordion__toggle pf-m-expanded"
      type="button"
      aria-expanded="true"
    >
      <span class="pf-c-accordion__toggle-text">Item two</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-expanded pf-m-fixed">
    <div
      class="pf-c-accordion__expanded-content-body"
    >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis molestie lorem lacinia dolor aliquet faucibus. Suspendisse gravida imperdiet accumsan. Aenean auctor lorem justo, vitae tincidunt enim blandit vel. Aenean quis tempus dolor. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</div>
    <div
      class="pf-c-accordion__expanded-content-body"
    >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis molestie lorem lacinia dolor aliquet faucibus. Suspendisse gravida imperdiet accumsan. Aenean auctor lorem justo, vitae tincidunt enim blandit vel. Aenean quis tempus dolor. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</div>
    <div
      class="pf-c-accordion__expanded-content-body"
    >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis molestie lorem lacinia dolor aliquet faucibus. Suspendisse gravida imperdiet accumsan. Aenean auctor lorem justo, vitae tincidunt enim blandit vel. Aenean quis tempus dolor. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item three</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-fixed" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item four</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-fixed" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item five</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-fixed" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>
</div>

```

### Definition list

```html
<dl class="pf-c-accordion">
  <dt>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item one</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </dt>
  <dd class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </dd>

  <dt>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item two</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </dt>
  <dd class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </dd>

  <dt>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item three</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </dt>
  <dd class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </dd>

  <dt>
    <button
      class="pf-c-accordion__toggle pf-m-expanded"
      type="button"
      aria-expanded="true"
    >
      <span class="pf-c-accordion__toggle-text">Item four</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </dt>
  <dd class="pf-c-accordion__expanded-content pf-m-expanded">
    <div
      class="pf-c-accordion__expanded-content-body"
    >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis molestie lorem lacinia dolor aliquet faucibus. Suspendisse gravida imperdiet accumsan. Aenean auctor lorem justo, vitae tincidunt enim blandit vel. Aenean quis tempus dolor. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</div>
  </dd>

  <dt>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item five</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </dt>
  <dd class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </dd>
</dl>

```

### Bordered

```html
<div class="pf-c-accordion pf-m-bordered">
  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item one</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button
      class="pf-c-accordion__toggle pf-m-expanded"
      type="button"
      aria-expanded="true"
    >
      <span class="pf-c-accordion__toggle-text">Item two</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-expanded">
    <div class="pf-c-accordion__expanded-content-body">
      <a href="#">Lorem ipsum</a> dolor sit amet, consectetur adipiscing elit. Duis molestie lorem lacinia dolor aliquet faucibus. Suspendisse gravida imperdiet accumsan. Aenean auctor lorem justo, vitae tincidunt enim blandit vel. Aenean quis tempus dolor. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
    </div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item three</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item four</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item five</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>
</div>

```

### Large bordered

```html
<div class="pf-c-accordion pf-m-display-lg pf-m-bordered">
  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item one</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button
      class="pf-c-accordion__toggle pf-m-expanded"
      type="button"
      aria-expanded="true"
    >
      <span class="pf-c-accordion__toggle-text">Item two</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-expanded">
    <div
      class="pf-c-accordion__expanded-content-body"
    >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis molestie lorem lacinia dolor aliquet faucibus. Suspendisse gravida imperdiet accumsan. Aenean auctor lorem justo, vitae tincidunt enim blandit vel. Aenean quis tempus dolor. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item three</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>

  <h3>
    <button
      class="pf-c-accordion__toggle pf-m-expanded"
      type="button"
      aria-expanded="true"
    >
      <span class="pf-c-accordion__toggle-text">Item four</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content pf-m-expanded">
    <div
      class="pf-c-accordion__expanded-content-body"
    >Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis molestie lorem lacinia dolor aliquet faucibus. Suspendisse gravida imperdiet accumsan. Aenean auctor lorem justo, vitae tincidunt enim blandit vel. Aenean quis tempus dolor. Lorem ipsum dolor sit amet, consectetur adipiscing elit.</div>
    <div class="pf-c-accordion__expanded-content-body">
      <button
        class="pf-c-button pf-m-link pf-m-inline pf-m-display-lg"
        type="button"
      >
        Call to action
        <span class="pf-c-button__icon pf-m-end">
          <i class="fas fa-arrow-right" aria-hidden="true"></i>
        </span>
      </button>
    </div>
  </div>

  <h3>
    <button class="pf-c-accordion__toggle" type="button" aria-expanded="false">
      <span class="pf-c-accordion__toggle-text">Item five</span>

      <span class="pf-c-accordion__toggle-icon">
        <i class="fas fa-angle-right" aria-hidden="true"></i>
      </span>
    </button>
  </h3>
  <div class="pf-c-accordion__expanded-content" hidden>
    <div class="pf-c-accordion__expanded-content-body">This text is hidden</div>
  </div>
</div>

```

## Documentation

### Overview

There are two variations to build the accordion component:
One way uses `<div>` and `<h1 - h6>` tags to build the component.
In these examples `.pf-c-accordion` uses `<div>`, `.pf-c-accordion__toggle` uses `<h3><button>`, and `.pf-c-accordion__expanded-content` uses `<div>`. The heading level that you use should fit within the rest of the headings outlined on your page.

Another variation is using the definition list:
In these examples `.pf-c-accordion` uses `<dl>`, `.pf-c-accordion__toggle` uses `<dt><button>`, and `.pf-c-accordion__expanded-content` uses `<dd>`.

### Accessibility

| Attribute               | Applied to                          | Outcome                                                                                              |
| ----------------------- | ----------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `aria-expanded="false"` | `.pf-c-accordion__toggle`           | Indicates that the expanded content element is hidden. **Required**                                  |
| `aria-expanded="true"`  | `.pf-c-accordion__toggle`           | Indicates that the expanded content element is visible. **Required**                                 |
| `hidden`                | `.pf-c-accordion__expanded-content` | Indicates that the expanded content element is hidden. Use with `aria-expanded="false"` **Required** |
| `aria-hidden="true"`    | `.pf-c-accordion__toggle-icon`      | Hides the icon from assistive technologies.**Required**                                              |

### Usage

| Class                                    | Applied to                                                     | Outcome                                                                                          |
| ---------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `.pf-c-accordion`                        | `<div>`, `<dl>`                                                | Initiates an accordion component. **Required**                                                   |
| `.pf-c-accordion__toggle`                | `<h1-h6><button>`, `<dt><button>`                              | Initiates a toggle in the accordion. **Required**                                                |
| `.pf-c-accordion__toggle-text`           | `<span>`                                                       | Initiates the text inside the toggle. **Required**                                               |
| `.pf-c-accordion__toggle-icon`           | `<span>`                                                       | Initiates the toggle icon wrapper. **Required**                                                  |
| `.pf-c-accordion__expanded-content`      | `<div>`, `<dd>`                                                | Initiates expanded content. **Must be paired with a button**                                     |
| `.pf-c-accordion__expanded-content-body` | `<div>`                                                        | Initiates expanded content body. **Required**                                                    |
| `.pf-m-bordered`                         | `.pf-c-accordion`                                              | Modifies the accordion to add borders between items.                                             |
| `.pf-m-display-lg`                       | `.pf-c-accordion`                                              | Modifies the accordion for large display styling. This variation is for marketing/web use cases. |
| `.pf-m-expanded`                         | `.pf-c-accordion__toggle`, `.pf-c-accordion__expanded-content` | Modifies the accordion button and expanded content for the expanded state.                       |
| `.pf-m-fixed`                            | `.pf-c-accordion__expanded-content`                            | Modifies the expanded content for the fixed state.                                               |
