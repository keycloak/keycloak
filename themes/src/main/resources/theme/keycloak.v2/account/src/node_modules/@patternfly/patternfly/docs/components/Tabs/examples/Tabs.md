---
id: Tabs
section: components
cssPrefix: pf-c-tabs
---import './Tabs.css'

## Examples

### Default

```html
<div class="pf-c-tabs" id="default-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="default-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        disabled
        id="default-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="default-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-example-network-wired-link">
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Accessibility

| Attribute              | Applied to                                                               | Outcome                                                                                    |
| ---------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------ |
| `disabled`             | `button.pf-c-tabs__link`                                                 | Indicates that a tabs link is disabled. **Required when disabled**                         |
| `aria-disabled="true"` | `a.pf-c-tabs__link.pf-m-disabled`, `.pf-c-tabs__link.pf-m-aria-disabled` | Indicates to assistive technology that a tabs link is disabled. **Required when disabled** |

### Usage

| Class                 | Applied to          | Outcome                                                                     |
| --------------------- | ------------------- | --------------------------------------------------------------------------- |
| `.pf-m-disabled`      | `a.pf-c-tabs__link` | Modifies a tabs link for disabled styles.                                   |
| `.pf-m-aria-disabled` | `.pf-c-tabs__link`  | Modifies a tabs link for disabled styles, but is still hoverable/focusable. |

### Default overflow beginning of list

```html
<div
  class="pf-c-tabs pf-m-scrollable"
  id="default-overflow-beginning-of-list-example"
>
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-cloud-link"
      >
        <span class="pf-c-tabs__item-text">Hybrid Cloud</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-automation-link"
      >
        <span class="pf-c-tabs__item-text">Automation</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-overflow-beginning-of-list-example-files-link"
      >
        <span class="pf-c-tabs__item-text">Files</span>
      </button>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Accessibility

| Attribute            | Applied to                  | Outcome                                                                                                          |
| -------------------- | --------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `disabled`           | `.pf-c-tabs__scroll-button` | Indicates that a scroll button is disabled, when at the first or last item of a list. **Required when disabled** |
| `aria-hidden="true"` | `.pf-c-tabs__scroll-button` | Hides the icon from assistive technologies.**Required when not scrollable**                                      |

### Usage

| Class                       | Applied to   | Outcome                                   |
| --------------------------- | ------------ | ----------------------------------------- |
| `.pf-m-scrollable`          | `.pf-c-tabs` | Enables the directional scroll buttons.   |
| `.pf-c-tabs__scroll-button` | `<button>`   | Initiates a tabs component scroll button. |

### Horizontal overflow

```html isBeta
<div class="pf-c-tabs" id="horizontal-overflow-example">
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-overflow">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-example-more-link"
      >
        <span class="pf-c-tabs__item-text">More</span>
        <span class="pf-c-tabs__link-toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </li>
  </ul>
</div>

```

### Horizontal overflow expanded

```html isBeta
<div class="pf-c-tabs" id="horizontal-overflow-expanded-example">
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-expanded-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-expanded-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-expanded-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-expanded-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-expanded-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-expanded-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-overflow">
      <button
        class="pf-c-tabs__link pf-m-expanded"
        aria-expanded="true"
        id="horizontal-overflow-expanded-example-more-link"
      >
        <span class="pf-c-tabs__item-text">More</span>
        <span class="pf-c-tabs__link-toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </li>
  </ul>
</div>

```

### Horizontal overflow selected

```html isBeta
<div class="pf-c-tabs" id="horizontal-overflow-selected-example">
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-selected-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-selected-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-selected-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-selected-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-selected-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-selected-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-overflow">
      <button
        class="pf-c-tabs__link"
        id="horizontal-overflow-selected-example-more-link"
      >
        <span class="pf-c-tabs__item-text">More</span>
        <span class="pf-c-tabs__link-toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </li>
  </ul>
</div>

```

### Vertical

```html
<div class="pf-c-tabs pf-m-vertical" id="vertical-example">
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="vertical-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="vertical-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="vertical-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        disabled
        id="vertical-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="vertical-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="vertical-example-network-wired-link">
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>
</div>

```

### Box

```html
<div class="pf-c-tabs pf-m-box" id="box-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="box-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" disabled id="box-example-disabled-link">
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="box-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-example-network-wired-link">
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Box overflow

```html
<div class="pf-c-tabs pf-m-box pf-m-scrollable" id="box-overflow-example">
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-overflow-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="box-overflow-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-overflow-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-overflow-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-overflow-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="box-overflow-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-overflow-example-cloud-link">
        <span class="pf-c-tabs__item-text">Hybrid Cloud</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-overflow-example-automation-link">
        <span class="pf-c-tabs__item-text">Automation</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-overflow-example-files-link">
        <span class="pf-c-tabs__item-text">Files</span>
      </button>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Box vertical

```html
<div class="pf-c-tabs pf-m-box pf-m-vertical" id="box-vertical-example">
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-vertical-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="box-vertical-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-vertical-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        disabled
        id="box-vertical-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="box-vertical-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="box-vertical-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>
</div>

```

### Box tabs color scheme light 300

```html
<div
  class="pf-c-tabs pf-m-box pf-m-color-scheme--light-300"
  id="box-tabs-alt-color-scheme"
>
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-tabs-alt-color-scheme-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="box-tabs-alt-color-scheme-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="box-tabs-alt-color-scheme-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        disabled
        id="box-tabs-alt-color-scheme-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="box-tabs-alt-color-scheme-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="box-tabs-alt-color-scheme-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>
<div class="tabs-example-block tabs-example-block--m-color-scheme--light-300"></div>

```

### Inset

```html
<div
  class="pf-c-tabs pf-m-inset-sm-on-md pf-m-inset-lg-on-lg pf-m-inset-2xl-on-xl"
  id="inset-example"
>
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="inset-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-example-network-wired-link">
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Inset box

```html
<div
  class="pf-c-tabs pf-m-box pf-m-inset-sm-on-md pf-m-inset-lg-on-lg pf-m-inset-2xl-on-xl"
  id="inset-box-example"
>
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-box-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="inset-box-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-box-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-box-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-box-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="inset-box-example-network-wired-link">
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Page insets

```html
<div class="pf-c-tabs pf-m-page-insets" id="page-insets-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="page-insets-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="page-insets-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="page-insets-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="page-insets-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="page-insets-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="page-insets-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Usage

| Class                                                                     | Applied to   | Outcome                                                                                           |
| ------------------------------------------------------------------------- | ------------ | ------------------------------------------------------------------------------------------------- |
| `.pf-m-inset-{none, sm, md, lg, xl, 2xl, 3xl}{-on-[sm, md, lg, xl, 2xl]}` | `.pf-c-tabs` | Modifies the tabs component padding/inset to visually match padding of other adjacent components. |
| `.pf-m-page-insets`                                                       | `.pf-c-tabs` | Modifies the tabs component padding/inset to visually match padding of page elements.             |

### Icons and text

```html
<div class="pf-c-tabs" id="icons-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="icons-example-users-link">
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-fas fa-users" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="icons-example-containers-link">
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-fas fa-box" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="icons-example-database-link">
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-database" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="icons-example-server-link">
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-server" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="icons-example-system-link">
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-laptop" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="icons-example-network-wired-link">
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-project-diagram" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Tabs with sub tabs

```html
<div class="pf-c-tabs pf-m-scrollable" id="default-parent-example">
  <button class="pf-c-tabs__scroll-button" aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-parent-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="default-parent-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-parent-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-parent-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-parent-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="default-parent-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

<div
  class="pf-c-tabs pf-m-secondary pf-m-scrollable"
  id="default-child-example"
>
  <button class="pf-c-tabs__scroll-button" aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-child-example-sub-1-link">
        <span class="pf-c-tabs__item-text">Item 1</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-child-example-sub-2-link">
        <span class="pf-c-tabs__item-text">Item 2</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="default-child-example-sub-3-link">
        <span class="pf-c-tabs__item-text">Item 3</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-child-example-sub-4-link">
        <span class="pf-c-tabs__item-text">Item 4</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-child-example-sub-5-link">
        <span class="pf-c-tabs__item-text">Item 5</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="default-child-example-sub-6-link">
        <span class="pf-c-tabs__item-text">Item 6</span>
      </button>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Box tabs with sub tabs

```html
<div class="pf-c-tabs pf-m-box pf-m-scrollable" id="box-parent-example">
  <button class="pf-c-tabs__scroll-button" aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-parent-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="box-parent-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-parent-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-parent-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-parent-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="box-parent-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

<div class="pf-c-tabs pf-m-secondary pf-m-scrollable" id="box-child-example">
  <button class="pf-c-tabs__scroll-button" aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-child-example-sub-1-link">
        <span class="pf-c-tabs__item-text">Item 1</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-child-example-sub-2-link">
        <span class="pf-c-tabs__item-text">Item 2</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="box-child-example-sub-3-link">
        <span class="pf-c-tabs__item-text">Item 3</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-child-example-sub-4-link">
        <span class="pf-c-tabs__item-text">Item 4</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-child-example-sub-5-link">
        <span class="pf-c-tabs__item-text">Item 5</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="box-child-example-sub-6-link">
        <span class="pf-c-tabs__item-text">Item 6</span>
      </button>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Filled

```html
<div class="pf-c-tabs pf-m-fill" id="filled-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="filled-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="filled-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="filled-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Filled with icons

```html
<div class="pf-c-tabs pf-m-fill" id="filled-with-icons-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="filled-with-icons-example-users-link">
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-fas fa-users" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="filled-with-icons-example-containers-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-fas fa-box" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="filled-with-icons-example-database-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-database" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Filled box

```html
<div class="pf-c-tabs pf-m-fill pf-m-box" id="filled-box-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="filled-box-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button class="pf-c-tabs__link" id="filled-box-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="filled-box-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Filled box with icons

```html
<div class="pf-c-tabs pf-m-fill pf-m-box" id="filled-box-with-icons-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="filled-box-with-icons-example-users-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-fas fa-users" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="filled-box-with-icons-example-containers-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-fas fa-box" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="filled-box-with-icons-example-database-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-database" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

## Usage

| Class        | Applied to   | Outcome                                                     |
| ------------ | ------------ | ----------------------------------------------------------- |
| `.pf-m-fill` | `.pf-c-tabs` | Modifies the tabs to fill the available space. **Required** |

### Using the nav element

```html
<nav
  class="pf-c-tabs pf-m-scrollable"
  aria-label="Local"
  id="default-scroll-nav-example"
>
  <button class="pf-c-tabs__scroll-button" aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link"
        href="#"
        id="default-scroll-nav-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </a>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <a
        class="pf-c-tabs__link"
        href="#"
        id="default-scroll-nav-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link"
        href="#"
        id="default-scroll-nav-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </a>
    </li>

    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link pf-m-disabled"
        aria-disabled="true"
        tabindex="-1"
        href="#"
        id="default-scroll-nav-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        href="#"
        id="default-scroll-nav-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link"
        href="#"
        id="default-scroll-nav-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </a>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</nav>

```

### Sub nav using the nav element

```html
<nav class="pf-c-tabs" aria-label="Local" id="primary-nav-example">
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <a class="pf-c-tabs__link" href="#" id="primary-nav-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </a>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <a
        class="pf-c-tabs__link"
        href="#"
        id="primary-nav-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link"
        href="#"
        id="primary-nav-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </a>
    </li>

    <li class="pf-c-tabs__item">
      <a class="pf-c-tabs__link" href="#" id="primary-nav-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a class="pf-c-tabs__link" href="#" id="primary-nav-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link"
        href="#"
        id="primary-nav-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </a>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</nav>

<nav
  class="pf-c-tabs pf-m-secondary"
  aria-label="Local secondary"
  id="secondary-nav-example"
>
  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll left"
  >
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <a class="pf-c-tabs__link" href="#" id="secondary-nav-example-sub-1-link">
        <span class="pf-c-tabs__item-text">Item 1</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a class="pf-c-tabs__link" href="#" id="secondary-nav-example-sub-2-link">
        <span class="pf-c-tabs__item-text">Item 2</span>
      </a>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <a class="pf-c-tabs__link" href="#" id="secondary-nav-example-sub-3-link">
        <span class="pf-c-tabs__item-text">Item 3</span>
      </a>
    </li>

    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link pf-m-disabled"
        aria-disabled="true"
        tabindex="-1"
        href="#"
        id="secondary-nav-example-sub-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        href="#"
        id="secondary-nav-example-sub-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </a>
    </li>
    <li class="pf-c-tabs__item">
      <a class="pf-c-tabs__link" href="#" id="secondary-nav-example-sub-6-link">
        <span class="pf-c-tabs__item-text">Item 6</span>
      </a>
    </li>
  </ul>

  <button
    class="pf-c-tabs__scroll-button"
    disabled
    aria-hidden="true"
    aria-label="Scroll right"
  >
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</nav>

```

### Vertical expandable

```html
<div
  class="pf-c-tabs pf-m-expandable pf-m-vertical"
  id="vertical-expandable-example"
>
  <div class="pf-c-tabs__toggle">
    <div class="pf-c-tabs__toggle-button">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        aria-expanded="false"
        id="vertical-expandable-example-toggle-button"
        aria-labelledby="vertical-expandable-example-toggle-text vertical-expandable-example-toggle-button"
      >
        <span class="pf-c-tabs__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
        <span
          class="pf-c-tabs__toggle-text"
          id="vertical-expandable-example-toggle-text"
        >Containers</span>
      </button>
    </div>
  </div>

  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>
</div>

```

### Vertical expanded

```html
<div
  class="pf-c-tabs pf-m-expandable pf-m-expanded pf-m-vertical"
  id="vertical-expanded-example"
>
  <div class="pf-c-tabs__toggle">
    <div class="pf-c-tabs__toggle-button">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        aria-expanded="true"
        id="vertical-expanded-example-toggle-button"
        aria-labelledby="vertical-expanded-example-toggle-text vertical-expanded-example-toggle-button"
      >
        <span class="pf-c-tabs__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
        <span
          class="pf-c-tabs__toggle-text"
          id="vertical-expanded-example-toggle-text"
        >Containers</span>
      </button>
    </div>
  </div>

  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="vertical-expanded-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>
</div>

```

### Vertical expandable (responsive)

```html
<div
  class="pf-c-tabs pf-m-expandable pf-m-vertical pf-m-non-expandable-on-md pf-m-expandable-on-lg pf-m-non-expandable-on-xl"
  id="vertical-expandable-responsive-example"
>
  <div class="pf-c-tabs__toggle">
    <div class="pf-c-tabs__toggle-button">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        aria-expanded="false"
        id="vertical-expandable-responsive-example-toggle-button"
        aria-labelledby="vertical-expandable-responsive-example-toggle-text vertical-expandable-responsive-example-toggle-button"
      >
        <span class="pf-c-tabs__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
        <span
          class="pf-c-tabs__toggle-text"
          id="vertical-expandable-responsive-example-toggle-text"
        >Containers</span>
      </button>
    </div>
  </div>

  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>
</div>

```

### Vertical expandable (legacy)

```html
<div
  class="pf-c-tabs pf-m-expandable pf-m-vertical"
  id="vertical-expandable-example"
>
  <div class="pf-c-tabs__toggle">
    <div class="pf-c-tabs__toggle-button">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        aria-expanded="false"
        id="vertical-expandable-example-toggle-button"
        aria-labelledby="vertical-expandable-example-toggle-text vertical-expandable-example-toggle-button"
      >
        <span class="pf-c-tabs__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </div>
    <span
      class="pf-c-tabs__toggle-text"
      id="vertical-expandable-example-toggle-text"
    >Containers</span>
  </div>

  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>
</div>

```

### Vertical expanded (legacy)

```html
<div
  class="pf-c-tabs pf-m-expandable pf-m-expanded pf-m-vertical"
  id="vertical-expanded-example"
>
  <div class="pf-c-tabs__toggle">
    <div class="pf-c-tabs__toggle-button">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        aria-expanded="true"
        id="vertical-expanded-example-toggle-button"
        aria-labelledby="vertical-expanded-example-toggle-text vertical-expanded-example-toggle-button"
      >
        <span class="pf-c-tabs__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </div>
    <span
      class="pf-c-tabs__toggle-text"
      id="vertical-expanded-example-toggle-text"
    >Containers</span>
  </div>

  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button class="pf-c-tabs__link" id="vertical-expanded-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expanded-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>
</div>

```

### Vertical expandable (responsive, legacy)

```html
<div
  class="pf-c-tabs pf-m-expandable pf-m-vertical pf-m-non-expandable-on-md pf-m-expandable-on-lg pf-m-non-expandable-on-xl"
  id="vertical-expandable-responsive-example"
>
  <div class="pf-c-tabs__toggle">
    <div class="pf-c-tabs__toggle-button">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Details"
        aria-expanded="false"
        id="vertical-expandable-responsive-example-toggle-button"
        aria-labelledby="vertical-expandable-responsive-example-toggle-text vertical-expandable-responsive-example-toggle-button"
      >
        <span class="pf-c-tabs__toggle-icon">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </span>
      </button>
    </div>
    <span
      class="pf-c-tabs__toggle-text"
      id="vertical-expandable-responsive-example-toggle-text"
    >Containers</span>
  </div>

  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
    </li>
    <li class="pf-c-tabs__item pf-m-current">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
    </li>

    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
    </li>
    <li class="pf-c-tabs__item">
      <button
        class="pf-c-tabs__link"
        id="vertical-expandable-responsive-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
    </li>
  </ul>
</div>

```

### Close button

```html
<div class="pf-c-tabs pf-m-scrollable" id="close-default-example">
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-default-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-default-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-default-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-default-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="close-default-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-default-example-close-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Close disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
          disabled
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-default-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

<br />
<br />

<div class="pf-c-tabs pf-m-box pf-m-scrollable" id="close-box-example">
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-box-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="close-box-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-box-example-close-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Close disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
          disabled
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-network-wired-link">
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

<br />
<br />

<div
  class="pf-c-tabs pf-m-box pf-m-color-scheme--light-300 pf-m-scrollable"
  id="close-box-light-300-example"
>
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-box-light-300-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="close-box-light-300-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-box-light-300-example-close-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Close disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
          disabled
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

<br />
<br />

<div class="pf-c-tabs pf-m-scrollable" id="close-icons-text-example">
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-icons-text-example-users-link">
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-fas fa-users" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-icons-text-example-containers-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-fas fa-box" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-icons-text-example-database-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-database" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-icons-text-example-disabled-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-server" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="close-icons-text-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-laptop" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-icons-text-example-close-disabled-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-laptop" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Close disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
          disabled
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-icons-text-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-icon">
          <i class="fas fa-project-diagram" aria-hidden="true"></i>
        </span>
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

<br />
<br />

<div class="pf-c-tabs pf-m-fill pf-m-scrollable" id="close-filled-example">
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-filled-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button class="pf-c-tabs__link" id="close-filled-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-filled-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-filled-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="close-filled-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-filled-example-close-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Close disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
          disabled
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-filled-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

<br />
<br />

<div class="pf-c-tabs pf-m-scrollable" id="close-secondary-primary-example">
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-primary-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-primary-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-primary-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-secondary-primary-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="close-secondary-primary-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-secondary-primary-example-close-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Close disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
          disabled
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-primary-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>
<div
  class="pf-c-tabs pf-m-secondary pf-m-scrollable"
  id="close-secondary-secondary-example"
>
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-secondary-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-secondary-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-secondary-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-secondary-secondary-example-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link pf-m-aria-disabled"
        aria-disabled="true"
        id="close-secondary-secondary-example-aria-disabled-link"
      >
        <span class="pf-c-tabs__item-text">ARIA disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action pf-m-disabled">
      <button
        class="pf-c-tabs__link"
        disabled
        id="close-secondary-secondary-example-close-disabled-link"
      >
        <span class="pf-c-tabs__item-text">Close disabled</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
          disabled
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-secondary-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
</div>

```

### Add tab button

```html
<div class="pf-c-tabs pf-m-scrollable" id="close-default-example">
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-default-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-default-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-default-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-default-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-default-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-default-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
  <span class="pf-c-tabs__add">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Add tab">
      <i class="fas fa-plus" aria-hidden="true"></i>
    </button>
  </span>
</div>
<div
  class="pf-c-tabs pf-m-secondary pf-m-scrollable"
  id="close-secondary-example"
>
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-secondary-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-secondary-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-secondary-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-secondary-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
  <span class="pf-c-tabs__add">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Add tab">
      <i class="fas fa-plus" aria-hidden="true"></i>
    </button>
  </span>
</div>

<br />
<br />

<div class="pf-c-tabs pf-m-box pf-m-scrollable" id="close-box-example">
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-users-link">
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-containers-link">
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-database-link">
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-server-link">
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-system-link">
        <span class="pf-c-tabs__item-text">System</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button class="pf-c-tabs__link" id="close-box-example-network-wired-link">
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
  <span class="pf-c-tabs__add">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Add tab">
      <i class="fas fa-plus" aria-hidden="true"></i>
    </button>
  </span>
</div>

<br />
<br />

<div
  class="pf-c-tabs pf-m-box pf-m-color-scheme--light-300 pf-m-scrollable"
  id="close-box-light-300-example"
>
  <button class="pf-c-tabs__scroll-button" disabled aria-label="Scroll left">
    <i class="fas fa-angle-left" aria-hidden="true"></i>
  </button>
  <ul class="pf-c-tabs__list">
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-users-link"
      >
        <span class="pf-c-tabs__item-text">Users</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-current pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-containers-link"
      >
        <span class="pf-c-tabs__item-text">Containers</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-database-link"
      >
        <span class="pf-c-tabs__item-text">Database</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>

    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-server-link"
      >
        <span class="pf-c-tabs__item-text">Server</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-system-link"
      >
        <span class="pf-c-tabs__item-text">System</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
    <li class="pf-c-tabs__item pf-m-action">
      <button
        class="pf-c-tabs__link"
        id="close-box-light-300-example-network-wired-link"
      >
        <span class="pf-c-tabs__item-text">Network</span>
      </button>
      <span class="pf-c-tabs__item-close">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Close tab"
        >
          <span class="pf-c-tabs__item-close-icon">
            <i class="fas fa-times" aria-hidden="true"></i>
          </span>
        </button>
      </span>
    </li>
  </ul>

  <button class="pf-c-tabs__scroll-button" aria-label="Scroll right">
    <i class="fas fa-angle-right" aria-hidden="true"></i>
  </button>
  <span class="pf-c-tabs__add">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Add tab">
      <i class="fas fa-plus" aria-hidden="true"></i>
    </button>
  </span>
</div>

```

The tabs component should only be used to change content views within a page. The similar-looking but semantically different [horizontal nav component](/components/navigation/#horizontal) is available for general navigation use cases.

Tabs should be used with the [tab content component](/components/tab-content).

Whenever a list of tabs is unique on the current page, it can be used in a `<nav>` element. Cases where the same set of tabs are duplicated in multiple regions on a page (e.g. cards on a dashboard) are less likely to benefit from using the `<nav>` element.

### Accessibility

| Attribute                       | Applied to                                                               | Outcome                                                                                                                                  |
| ------------------------------- | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-label="Descriptive text"` | `nav.pf-c-tabs`, `nav.pf-c-tabs.pf-m-secondary`                          | Gives the `<nav>` an accessible label. **Required when `.pf-c-tabs` is used with `<nav>`**                                               |
| `aria-label="Descriptive text"` | `.pf-c-inline-edit__toggle > button`                                     | Provides an accessible description for toggle button. **Required**                                                                       |
| `disabled`                      | `button.pf-c-tabs__link`                                                 | Indicates that a tabs link is disabled. **Required when disabled**                                                                       |
| `aria-disabled="true"`          | `a.pf-c-tabs__link.pf-m-disabled`, `.pf-c-tabs__link.pf-m-aria-disabled` | Indicates to assistive technology that a tabs link is disabled. **Required when disabled**                                               |
| `disabled`                      | `.pf-c-tabs__scroll-button`                                              | Indicates that a scroll button is disable, typically when at the first or last item of a list or scroll buttons are hidden. **Required** |
| `aria-expanded="true"`          | `.pf-c-tabs__item`                                                       | Indicates that the overflow menu tab is expanded. **Required when expanded**                                                             |

### Usage

| Class                                                            | Applied to          | Outcome                                                                                                                                                                                                         |
| ---------------------------------------------------------------- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-tabs`                                                     | `<nav>`, `<div>`    | Initiates the tabs component. **Required**                                                                                                                                                                      |
| `.pf-c-tabs__list`                                               | `<ul>`              | Initiates a tabs component list. **Required**                                                                                                                                                                   |
| `.pf-c-tabs__item`                                               | `<li>`              | Initiates a tabs component item. **Required**                                                                                                                                                                   |
| `.pf-c-tabs__item-text`                                          | `<span>`            | Initiates a tabs component item icon. **Required**                                                                                                                                                              |
| `.pf-c-tabs__item-icon`                                          | `<span>`            | Initiates a tabs component item text. **Required**                                                                                                                                                              |
| `.pf-c-tabs__item-close`                                         | `<span>`            | Initiates a tabs component item close.                                                                                                                                                                          |
| `.pf-c-tabs__item-close-icon`                                    | `<span>`            | Initiates a tabs component item close icon.                                                                                                                                                                     |
| `.pf-c-tabs__link`                                               | `<button>`, `<a>`   | Initiates a tabs component link. **Required**                                                                                                                                                                   |
| `.pf-c-tabs__scroll-button`                                      | `<button>`          | Initiates a tabs component scroll button.                                                                                                                                                                       |
| `.pf-c-tabs__add`                                                | `<span>`            | Initiates a tabs component add button.                                                                                                                                                                          |
| `.pf-c-tabs__toggle`                                             | `<div>`             | Initiates a tabs expandable toggle.                                                                                                                                                                             |
| `.pf-c-tabs__toggle-button`                                      | `<button>`          | Initiates a tabs expandable toggle button.                                                                                                                                                                      |
| `.pf-c-tabs__toggle-icon`                                        | `<span>`            | Initiates a tabs expandable toggle icon.                                                                                                                                                                        |
| `.pf-c-tabs__toggle-text`                                        | `<span>`            | Initiates a tabs expandable toggle text.                                                                                                                                                                        |
| `.pf-m-secondary`                                                | `.pf-c-tabs`        | Applies secondary styling to the tab component.                                                                                                                                                                 |
| `.pf-m-no-border-bottom`                                         | `.pf-c-tabs`        | Removes bottom border from a tab component.                                                                                                                                                                     |
| `.pf-m-border-bottom`                                            | `.pf-c-tabs`        | Adds a bottom border to secondary tabs.                                                                                                                                                                         |
| `.pf-m-box`                                                      | `.pf-c-tabs`        | Applies box styling to the tab component.                                                                                                                                                                       |
| `.pf-m-vertical`                                                 | `.pf-c-tabs`        | Applies vertical styling to the tab component.                                                                                                                                                                  |
| `.pf-m-fill`                                                     | `.pf-c-tabs`        | Modifies the tabs to fill the available space.                                                                                                                                                                  |
| `.pf-m-current`                                                  | `.pf-c-tabs__item`  | Indicates that a tab item is currently selected.                                                                                                                                                                |
| `.pf-m-action`                                                   | `.pf-c-tabs__item`  | Indicates that a tab item contains actions other than the tab link.                                                                                                                                             |
| `.pf-m-overflow`                                                 | `.pf-c-tabs__item`  | Applies overflow menu styling to a tab item.                                                                                                                                                                    |
| `.pf-m-expanded`                                                 | `.pf-c-tabs__item`  | Applies expanded styling to the overflow menu tab item.                                                                                                                                                         |
| `.pf-m-inset-{none, sm, md, lg, xl, 2xl}{-on-[md, lg, xl, 2xl]}` | `.pf-c-tabs`        | Modifies the tabs component padding/inset to visually match padding of other adjacent components.                                                                                                               |
| `.pf-m-page-insets`                                              | `.pf-c-tabs`        | Modifies the tabs component padding/inset to visually match padding of page elements.                                                                                                                           |
| `.pf-m-color-scheme--light-300`                                  | `.pf-c-tabs`        | Modifies the tabs component tab background colors.                                                                                                                                                              |
| `.pf-m-expandable{-on-[breakpoint]}`                             | `.pf-c-tabs`        | Modifies the tabs component to be expandable via a toggle at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). **Note:** works with vertical tabs only. |
| `.pf-m-non-expandable{-on-[breakpoint]}`                         | `.pf-c-tabs`        | Modifies the tabs component to be non-expandable at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                   |
| `.pf-m-expanded`                                                 | `.pf-c-tabs`        | Modifies the expandable tabs component for the expanded state.                                                                                                                                                  |
| `.pf-m-disabled`                                                 | `a.pf-c-tabs__link` | Modifies a tabs link for disabled styles.                                                                                                                                                                       |
| `.pf-m-aria-disabled`                                            | `.pf-c-tabs__link`  | Modifies a tabs link for disabled styles, but is still hoverable/focusable.                                                                                                                                     |
