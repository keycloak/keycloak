---
id: 'Menu toggle'
beta: true
section: components
cssPrefix: pf-c-menu-toggle
---import './MenuToggle.css'

## Examples

### Collapsed

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false">
  <span class="pf-c-menu-toggle__text">Collapsed</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Expanded

```html
<button
  class="pf-c-menu-toggle pf-m-expanded"
  type="button"
  aria-expanded="true"
>
  <span class="pf-c-menu-toggle__text">Expanded</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Disabled

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false" disabled>
  <span class="pf-c-menu-toggle__text">Disabled</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Count

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false">
  <span class="pf-c-menu-toggle__text">Count</span>
  <span class="pf-c-menu-toggle__count">
    <span class="pf-c-badge pf-m-unread">4 selected</span>
  </span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Primary

```html
<button
  class="pf-c-menu-toggle pf-m-primary"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__text">Collapsed</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-primary"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__icon">
    <i class="fas fa-cog" aria-hidden="true"></i>
  </span>
  <span class="pf-c-menu-toggle__text">Icon</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-primary pf-m-expanded"
  type="button"
  aria-expanded="true"
>
  <span class="pf-c-menu-toggle__text">Expanded</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-primary"
  type="button"
  aria-expanded="false"
  disabled
>
  <span class="pf-c-menu-toggle__text">Disabled</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Secondary

```html
<button
  class="pf-c-menu-toggle pf-m-secondary"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__text">Collapsed</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-secondary"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__icon">
    <i class="fas fa-cog" aria-hidden="true"></i>
  </span>
  <span class="pf-c-menu-toggle__text">Icon</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-secondary pf-m-expanded"
  type="button"
  aria-expanded="true"
>
  <span class="pf-c-menu-toggle__text">Expanded</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-secondary"
  type="button"
  aria-expanded="false"
  disabled
>
  <span class="pf-c-menu-toggle__text">Disabled</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Plain

```html
<button
  class="pf-c-menu-toggle pf-m-plain"
  type="button"
  aria-expanded="false"
  aria-label="Actions"
>
  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-plain pf-m-expanded"
  type="button"
  aria-expanded="true"
  aria-label="Actions"
>
  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-plain"
  type="button"
  aria-expanded="false"
  disabled
  aria-label="Actions"
>
  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
</button>

```

### Plain with text

```html
<button
  class="pf-c-menu-toggle pf-m-plain pf-m-text"
  type="button"
  aria-expanded="false"
  disabled
>
  <span class="pf-c-menu-toggle__text">Disabled</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-plain pf-m-text"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__text">Custom text</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Split button (checkbox)

```html
<div class="pf-c-menu-toggle pf-m-split-button pf-m-disabled">
  <label class="pf-c-check pf-m-standalone">
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-disabled-example-input"
      name="split-button-checkbox-disabled-example-input"
      aria-label="Standalone input"
      disabled
    />
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-disabled-example-toggle-button"
    aria-label="Menu toggle"
    disabled
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-split-button">
  <label class="pf-c-check pf-m-standalone">
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-example-input"
      name="split-button-checkbox-example-input"
      aria-label="Standalone input"
    />
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-expanded pf-m-split-button">
  <label class="pf-c-check pf-m-standalone">
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-expanded-example-input"
      name="split-button-checkbox-expanded-example-input"
      aria-label="Standalone input"
    />
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="true"
    id="split-button-checkbox-expanded-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>

```

### Split button (checkbox with toggle text)

```html
<div class="pf-c-menu-toggle pf-m-split-button pf-m-disabled">
  <label
    class="pf-c-check"
    for="split-button-checkbox-with-toggle-text-disabled-example-input"
  >
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-with-toggle-text-disabled-example-input"
      name="split-button-checkbox-with-toggle-text-disabled-example-input"
      disabled
    />
    <span class="pf-c-check__label pf-m-disabled">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-with-toggle-text-disabled-example-toggle-button"
    aria-label="Menu toggle"
    disabled
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-split-button">
  <label
    class="pf-c-check"
    for="split-button-checkbox-with-toggle-text-example-input"
  >
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-with-toggle-text-example-input"
      name="split-button-checkbox-with-toggle-text-example-input"
    />
    <span class="pf-c-check__label">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-with-toggle-text-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-expanded pf-m-split-button">
  <label
    class="pf-c-check"
    for="split-button-checkbox-with-toggle-text-expanded-example-input"
  >
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-with-toggle-text-expanded-example-input"
      name="split-button-checkbox-with-toggle-text-expanded-example-input"
    />
    <span class="pf-c-check__label">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="true"
    id="split-button-checkbox-with-toggle-text-expanded-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>

```

### Split button, primary

```html
<div class="pf-c-menu-toggle pf-m-split-button pf-m-disabled pf-m-primary">
  <label
    class="pf-c-check"
    for="split-button-checkbox-primary-disabled-example-input"
  >
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-primary-disabled-example-input"
      name="split-button-checkbox-primary-disabled-example-input"
      disabled
    />
    <span class="pf-c-check__label pf-m-disabled">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-primary-disabled-example-toggle-button"
    aria-label="Menu toggle"
    disabled
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-split-button pf-m-primary">
  <label class="pf-c-check" for="split-button-checkbox-primary-example-input">
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-primary-example-input"
      name="split-button-checkbox-primary-example-input"
    />
    <span class="pf-c-check__label">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-primary-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-expanded pf-m-split-button pf-m-primary">
  <label
    class="pf-c-check"
    for="split-button-checkbox-primary-expanded-example-input"
  >
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-primary-expanded-example-input"
      name="split-button-checkbox-primary-expanded-example-input"
    />
    <span class="pf-c-check__label">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="true"
    id="split-button-checkbox-primary-expanded-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>

```

### Split button, secondary

```html
<div class="pf-c-menu-toggle pf-m-split-button pf-m-disabled pf-m-secondary">
  <label
    class="pf-c-check"
    for="split-button-checkbox-secondary-disabled-example-input"
  >
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-secondary-disabled-example-input"
      name="split-button-checkbox-secondary-disabled-example-input"
      disabled
    />
    <span class="pf-c-check__label pf-m-disabled">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-secondary-disabled-example-toggle-button"
    aria-label="Menu toggle"
    disabled
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-split-button pf-m-secondary">
  <label class="pf-c-check" for="split-button-checkbox-secondary-example-input">
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-secondary-example-input"
      name="split-button-checkbox-secondary-example-input"
    />
    <span class="pf-c-check__label">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-secondary-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-expanded pf-m-split-button pf-m-secondary">
  <label
    class="pf-c-check"
    for="split-button-checkbox-secondary-expanded-example-input"
  >
    <input
      class="pf-c-check__input"
      type="checkbox"
      id="split-button-checkbox-secondary-expanded-example-input"
      name="split-button-checkbox-secondary-expanded-example-input"
    />
    <span class="pf-c-check__label">10 selected</span>
  </label>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="true"
    id="split-button-checkbox-secondary-expanded-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>

```

### Split button (action)

```html
<div class="pf-c-menu-toggle pf-m-split-button pf-m-action pf-m-disabled">
  <button class="pf-c-menu-toggle__button" type="button" disabled>Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-with-toggle-action-disabled-example-toggle-button"
    aria-label="Menu toggle"
    disabled
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-split-button pf-m-action">
  <button class="pf-c-menu-toggle__button" type="button">Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-with-toggle-action-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-expanded pf-m-split-button pf-m-action">
  <button class="pf-c-menu-toggle__button" type="button">Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="true"
    id="split-button-checkbox-with-toggle-action-expanded-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>

```

### Split button, primary (action)

```html
<div
  class="pf-c-menu-toggle pf-m-split-button pf-m-action pf-m-disabled pf-m-primary"
>
  <button class="pf-c-menu-toggle__button" type="button" disabled>Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-with-toggle-action-primary-disabled-example-toggle-button"
    aria-label="Menu toggle"
    disabled
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-split-button pf-m-action pf-m-primary">
  <button class="pf-c-menu-toggle__button" type="button">Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-with-toggle-action-primary-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div
  class="pf-c-menu-toggle pf-m-expanded pf-m-split-button pf-m-action pf-m-primary"
>
  <button class="pf-c-menu-toggle__button" type="button">Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="true"
    id="split-button-checkbox-with-toggle-action-primary-expanded-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>

```

### Split button, secondary (action)

```html
<div
  class="pf-c-menu-toggle pf-m-split-button pf-m-action pf-m-disabled pf-m-secondary"
>
  <button class="pf-c-menu-toggle__button" type="button" disabled>Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-with-toggle-action-secondary-disabled-example-toggle-button"
    aria-label="Menu toggle"
    disabled
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div class="pf-c-menu-toggle pf-m-split-button pf-m-action pf-m-secondary">
  <button class="pf-c-menu-toggle__button" type="button">Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="split-button-checkbox-with-toggle-action-secondary-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>
&nbsp;
<div
  class="pf-c-menu-toggle pf-m-expanded pf-m-split-button pf-m-action pf-m-secondary"
>
  <button class="pf-c-menu-toggle__button" type="button">Action</button>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="true"
    id="split-button-checkbox-with-toggle-action-secondary-expanded-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>

```

### With icon/image and text

```html
<button
  class="pf-c-menu-toggle pf-m-secondary"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__icon">
    <i class="fas fa-cog" aria-hidden="true"></i>
  </span>
  <span class="pf-c-menu-toggle__text">Icon</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-secondary"
  type="button"
  aria-expanded="false"
  disabled
>
  <span class="pf-c-menu-toggle__icon">
    <i class="fas fa-cog" aria-hidden="true"></i>
  </span>
  <span class="pf-c-menu-toggle__text">Icon</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### With avatar and text

```html
<button class="pf-c-menu-toggle" type="button" aria-expanded="false">
  <span class="pf-c-menu-toggle__icon">
    <img
      class="pf-c-avatar pf-m-light"
      src="/assets/images/img_avatar-light.svg"
      alt="Avatar image light"
    />
  </span>
  <span class="pf-c-menu-toggle__text">Ned Username</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle pf-m-expanded"
  type="button"
  aria-expanded="true"
>
  <span class="pf-c-menu-toggle__icon">
    <img
      class="pf-c-avatar pf-m-light"
      src="/assets/images/img_avatar-light.svg"
      alt="Avatar image light"
    />
  </span>
  <span class="pf-c-menu-toggle__text">Ned Username</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

&nbsp;
<button
  class="pf-c-menu-toggle"
  type="button"
  aria-expanded="false"
  disabled
>
  <span class="pf-c-menu-toggle__icon">
    <img
      class="pf-c-avatar pf-m-light"
      src="/assets/images/img_avatar-light.svg"
      alt="Avatar image light"
    />
  </span>
  <span class="pf-c-menu-toggle__text">Ned Username</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Full height

```html
<button
  class="pf-c-menu-toggle pf-m-full-height"
  type="button"
  aria-expanded="false"
>
  <span class="pf-c-menu-toggle__text">Full height</span>
  <span class="pf-c-menu-toggle__controls">
    <span class="pf-c-menu-toggle__toggle-icon">
      <i class="fas fa-caret-down" aria-hidden="true"></i>
    </span>
  </span>
</button>

```

### Typeahead

```html
<div class="pf-c-menu-toggle pf-m-typeahead pf-m-full-width">
  <div class="pf-c-text-input-group pf-m-plain">
    <div class="pf-c-text-input-group__main">
      <span class="pf-c-text-input-group__text">
        <input
          class="pf-c-text-input-group__text-input"
          type="text"
          value
          aria-label="Type to filter"
        />
      </span>
    </div>
    <div class="pf-c-text-input-group__utilities">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Clear input"
      >
        <i class="fas fa-times fa-fw" aria-hidden="true"></i>
      </button>
    </div>
  </div>
  <button
    class="pf-c-menu-toggle__button"
    type="button"
    aria-expanded="false"
    id="typeahead-example-toggle-button"
    aria-label="Menu toggle"
  >
    <span class="pf-c-menu-toggle__controls">
      <span class="pf-c-menu-toggle__toggle-icon">
        <i class="fas fa-caret-down" aria-hidden="true"></i>
      </span>
    </span>
  </button>
</div>

```

### Accessibility

| Class                           | Applied to                                       | Outcome                                                             |
| ------------------------------- | ------------------------------------------------ | ------------------------------------------------------------------- |
| `aria-expanded="true"`          | `.pf-c-menu-toggle`, `.pf-c-menu-toggle__button` | Indicates that the menu toggle component is in the expanded state.  |
| `aria-expanded="false"`         | `.pf-c-menu-toggle`, `.pf-c-menu-toggle__button` | Indicates that the menu toggle component is in the collapsed state. |
| `aria-label="Descriptive text"` | `.pf-c-menu-toggle.pf-m-plain`                   | Gives the plain menu toggle component an accessible label.          |
| `disabled`                      | `.pf-c-menu-toggle`, `.pf-c-menu-toggle__button` | Indicates that the menu toggle component is disabled.               |

### Usage

| Class                            | Applied                               | Outcome                                                                    |
| -------------------------------- | ------------------------------------- | -------------------------------------------------------------------------- |
| `.pf-c-menu-toggle`              | `<button>`                            | Initiates the menu toggle component.                                       |
| `.pf-c-menu-toggle__icon`        | `<span>`                              | Defines the menu toggle component icon/image.                              |
| `.pf-c-menu-toggle__text`        | `<span>`                              | Defines the menu toggle component text.                                    |
| `.pf-c-menu-toggle__count`       | `<span>`                              | Defines the menu toggle component count.                                   |
| `.pf-c-menu-toggle__controls`    | `<span>`                              | Defines the menu toggle component controls.                                |
| `.pf-c-menu-toggle__toggle-icon` | `<span>`                              | Defines the menu toggle component toggle/arrow icon.                       |
| `.pf-c-menu-toggle__button`      | `<button>`                            | Initiates the menu toggle button.                                          |
| `.pf-m-split-button`             | `.pf-c-menu-toggle`                   | Modifies the menu toggle component for the split button variation.         |
| `.pf-m-action`                   | `.pf-c-menu-toggle.pf-m-split-button` | Modifies the menu toggle component for the action, split button variation. |
| `.pf-m-disabled`                 | `.pf-c-menu-toggle`                   | Modifies the menu toggle component for the disabled variation.             |
| `.pf-m-primary`                  | `.pf-c-menu-toggle`                   | Modifies the menu toggle component for the primary variation.              |
| `.pf-m-secondary`                | `.pf-c-menu-toggle`                   | Modifies the menu toggle component for the secondary variation.            |
| `.pf-m-text`                     | `.pf-c-menu-toggle`                   | Modifies the menu toggle component for the text variation.                 |
| `.pf-m-plain`                    | `.pf-c-menu-toggle`                   | Modifies the menu toggle component for the plain variation.                |
| `.pf-m-expanded`                 | `.pf-c-menu-toggle`                   | Modifies the menu toggle component for the expanded state.                 |
| `.pf-m-full-height`              | `.pf-c-menu-toggle`                   | Modifies the menu toggle component to full height of parent.               |
