---
id: Label
section: components
cssPrefix: pf-c-label
---import './Label.css'

## Examples

### Filled

```html
<span class="pf-c-label">
  <span class="pf-c-label__content">Grey</span>
</span>

<span class="pf-c-label">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Grey icon
  </span>
</span>

<span class="pf-c-label">
  <span class="pf-c-label__content">Grey removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-grey-close-button"
    aria-label="Remove"
    aria-labelledby="default-grey-close-button default-grey-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Grey icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-grey-icon-close-button"
    aria-label="Remove"
    aria-labelledby="default-grey-icon-close-button default-grey-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label">
  <a class="pf-c-label__content" href="#">Grey link</a>
</span>

<span class="pf-c-label">
  <a class="pf-c-label__content" href="#">Grey link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-grey-link-close-button"
    aria-label="Remove"
    aria-labelledby="default-grey-link-close-button default-grey-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Grey label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-grey-icon-close-truncate-button"
    aria-label="Remove"
    aria-labelledby="default-grey-icon-close-truncate-button default-grey-icon-close-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-blue">
  <span class="pf-c-label__content">Blue</span>
</span>

<span class="pf-c-label pf-m-blue">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Blue icon
  </span>
</span>

<span class="pf-c-label pf-m-blue">
  <span class="pf-c-label__content">Blue removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-blue-close-button"
    aria-label="Remove"
    aria-labelledby="default-blue-close-button default-blue-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-blue">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Blue icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-blue-icon-close-button"
    aria-label="Remove"
    aria-labelledby="default-blue-icon-close-button default-blue-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-blue">
  <a class="pf-c-label__content" href="#">Blue link</a>
</span>

<span class="pf-c-label pf-m-blue">
  <a class="pf-c-label__content" href="#">Blue link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-blue-link-close-button"
    aria-label="Remove"
    aria-labelledby="default-blue-link-close-button default-blue-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-blue">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Blue label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-blue-icon-close-truncate-button"
    aria-label="Remove"
    aria-labelledby="default-blue-icon-close-truncate-button default-blue-icon-close-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-green">
  <span class="pf-c-label__content">Green</span>
</span>

<span class="pf-c-label pf-m-green">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Green icon
  </span>
</span>

<span class="pf-c-label pf-m-green">
  <span class="pf-c-label__content">Green removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-green-close-button"
    aria-label="Remove"
    aria-labelledby="default-green-close-button default-green-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-green">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Green icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-green-icon-close-button"
    aria-label="Remove"
    aria-labelledby="default-green-icon-close-button default-green-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-green">
  <a class="pf-c-label__content" href="#">Green link</a>
</span>

<span class="pf-c-label pf-m-green">
  <a class="pf-c-label__content" href="#">Green link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-green-link-close-button"
    aria-label="Remove"
    aria-labelledby="default-green-link-close-button default-green-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-green">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Green label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-green-icon-close-truncate-button"
    aria-label="Remove"
    aria-labelledby="default-green-icon-close-truncate-button default-green-icon-close-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-orange">
  <span class="pf-c-label__content">Orange</span>
</span>

<span class="pf-c-label pf-m-orange">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Orange icon
  </span>
</span>

<span class="pf-c-label pf-m-orange">
  <span class="pf-c-label__content">Orange removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-orange-close-button"
    aria-label="Remove"
    aria-labelledby="default-orange-close-button default-orange-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-orange">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Orange icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-orange-icon-close-button"
    aria-label="Remove"
    aria-labelledby="default-orange-icon-close-button default-orange-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-orange">
  <a class="pf-c-label__content" href="#">Orange link</a>
</span>

<span class="pf-c-label pf-m-orange">
  <a class="pf-c-label__content" href="#">Orange link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-orange-link-close-button"
    aria-label="Remove"
    aria-labelledby="default-orange-link-close-button default-orange-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-orange">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Orange label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-orange-icon-close-truncate-button"
    aria-label="Remove"
    aria-labelledby="default-orange-icon-close-truncate-button default-orange-icon-close-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-red">
  <span class="pf-c-label__content">Red</span>
</span>

<span class="pf-c-label pf-m-red">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Red icon
  </span>
</span>

<span class="pf-c-label pf-m-red">
  <span class="pf-c-label__content">Red removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-red-close-button"
    aria-label="Remove"
    aria-labelledby="default-red-close-button default-red-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-red">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Red icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-red-icon-close-button"
    aria-label="Remove"
    aria-labelledby="default-red-icon-close-button default-red-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-red">
  <a class="pf-c-label__content" href="#">Red link</a>
</span>

<span class="pf-c-label pf-m-red">
  <a class="pf-c-label__content" href="#">Red link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-red-link-close-button"
    aria-label="Remove"
    aria-labelledby="default-red-link-close-button default-red-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-red">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Red label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-red-icon-close-truncate-button"
    aria-label="Remove"
    aria-labelledby="default-red-icon-close-truncate-button default-red-icon-close-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-purple">
  <span class="pf-c-label__content">Purple</span>
</span>

<span class="pf-c-label pf-m-purple">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Purple icon
  </span>
</span>

<span class="pf-c-label pf-m-purple">
  <span class="pf-c-label__content">Purple removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-purple-close-button"
    aria-label="Remove"
    aria-labelledby="default-purple-close-button default-purple-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-purple">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Purple icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-purple-icon-close-button"
    aria-label="Remove"
    aria-labelledby="default-purple-icon-close-button default-purple-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-purple">
  <a class="pf-c-label__content" href="#">Purple link</a>
</span>

<span class="pf-c-label pf-m-purple">
  <a class="pf-c-label__content" href="#">Purple link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-purple-link-close-button"
    aria-label="Remove"
    aria-labelledby="default-purple-link-close-button default-purple-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-purple">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Purple label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-purple-icon-close-truncate-button"
    aria-label="Remove"
    aria-labelledby="default-purple-icon-close-truncate-button default-purple-icon-close-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-cyan">
  <span class="pf-c-label__content">Cyan</span>
</span>

<span class="pf-c-label pf-m-cyan">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Cyan icon
  </span>
</span>

<span class="pf-c-label pf-m-cyan">
  <span class="pf-c-label__content">Cyan removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-cyan-close-button"
    aria-label="Remove"
    aria-labelledby="default-cyan-close-button default-cyan-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-cyan">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Cyan icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-cyan-icon-close-button"
    aria-label="Remove"
    aria-labelledby="default-cyan-icon-close-button default-cyan-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-cyan">
  <a class="pf-c-label__content" href="#">Cyan link</a>
</span>

<span class="pf-c-label pf-m-cyan">
  <a class="pf-c-label__content" href="#">Cyan link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-cyan-link-close-button"
    aria-label="Remove"
    aria-labelledby="default-cyan-link-close-button default-cyan-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-cyan">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Cyan label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="default-cyan-icon-close-truncate-button"
    aria-label="Remove"
    aria-labelledby="default-cyan-icon-close-truncate-button default-cyan-icon-close-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-gold">
  <span class="pf-c-label__content">Gold</span>
</span>

<span class="pf-c-label pf-m-gold">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Gold icon
  </span>
</span>

<span class="pf-c-label pf-m-gold">
  <span class="pf-c-label__content">Gold removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="gold-removable-button"
    aria-label="Remove"
    aria-labelledby="gold-removable-button gold-removable-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-gold">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Gold icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="gold-icon-removable-button"
    aria-label="Remove"
    aria-labelledby="gold-icon-removable-button gold-icon-removable-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-gold">
  <a class="pf-c-label__content" href="#">Gold link</a>
</span>

<span class="pf-c-label pf-m-gold">
  <a class="pf-c-label__content" href="#">Gold link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="gold-link-removable-button"
    aria-label="Remove"
    aria-labelledby="gold-link-removable-button gold-link-removable-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-gold">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Gold label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="gold-truncate-button"
    aria-label="Remove"
    aria-labelledby="gold-truncate-button gold-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

```

### Outline

```html
<span class="pf-c-label pf-m-outline">
  <span class="pf-c-label__content">Grey</span>
</span>

<span class="pf-c-label pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Grey icon
  </span>
</span>

<span class="pf-c-label pf-m-outline">
  <span class="pf-c-label__content">Grey removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-grey-close-button"
    aria-label="Remove"
    aria-labelledby="outline-grey-close-button outline-grey-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Grey icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-grey-icon-close-button"
    aria-label="Remove"
    aria-labelledby="outline-grey-icon-close-button outline-grey-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-outline">
  <a class="pf-c-label__content" href="#">Grey link</a>
</span>

<span class="pf-c-label pf-m-outline">
  <a class="pf-c-label__content" href="#">Grey link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-grey-link-close-button"
    aria-label="Remove"
    aria-labelledby="outline-grey-link-close-button outline-grey-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-blue pf-m-outline">
  <span class="pf-c-label__content">Blue</span>
</span>

<span class="pf-c-label pf-m-blue pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Blue icon
  </span>
</span>

<span class="pf-c-label pf-m-blue pf-m-outline">
  <span class="pf-c-label__content">Blue removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-blue-close-button"
    aria-label="Remove"
    aria-labelledby="outline-blue-close-button outline-blue-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-blue pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Blue icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-blue-icon-close-button"
    aria-label="Remove"
    aria-labelledby="outline-blue-icon-close-button outline-blue-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-outline pf-m-blue">
  <a class="pf-c-label__content" href="#">Blue link</a>
</span>

<span class="pf-c-label pf-m-outline pf-m-blue">
  <a class="pf-c-label__content" href="#">Blue link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-blue-link-close-button"
    aria-label="Remove"
    aria-labelledby="outline-blue-link-close-button outline-blue-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-green pf-m-outline">
  <span class="pf-c-label__content">Green</span>
</span>

<span class="pf-c-label pf-m-green pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Green icon
  </span>
</span>

<span class="pf-c-label pf-m-green pf-m-outline">
  <span class="pf-c-label__content">Green removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-green-close-button"
    aria-label="Remove"
    aria-labelledby="outline-green-close-button outline-green-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-green pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Green icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-green-icon-close-button"
    aria-label="Remove"
    aria-labelledby="outline-green-icon-close-button outline-green-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-outline pf-m-green">
  <a class="pf-c-label__content" href="#">Green link</a>
</span>

<span class="pf-c-label pf-m-outline pf-m-green">
  <a class="pf-c-label__content" href="#">Green link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-green-link-close-button"
    aria-label="Remove"
    aria-labelledby="outline-green-link-close-button outline-green-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-orange pf-m-outline">
  <span class="pf-c-label__content">Orange</span>
</span>

<span class="pf-c-label pf-m-orange pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Orange icon
  </span>
</span>

<span class="pf-c-label pf-m-orange pf-m-outline">
  <span class="pf-c-label__content">Orange removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-orange-close-button"
    aria-label="Remove"
    aria-labelledby="outline-orange-close-button outline-orange-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-orange pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Orange icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-orange-icon-close-button"
    aria-label="Remove"
    aria-labelledby="outline-orange-icon-close-button outline-orange-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-outline pf-m-orange">
  <a class="pf-c-label__content" href="#">Orange link</a>
</span>

<span class="pf-c-label pf-m-outline pf-m-orange">
  <a class="pf-c-label__content" href="#">Orange link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-orange-link-close-button"
    aria-label="Remove"
    aria-labelledby="outline-orange-link-close-button outline-orange-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-red pf-m-outline">
  <span class="pf-c-label__content">Red</span>
</span>

<span class="pf-c-label pf-m-red pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Red icon
  </span>
</span>

<span class="pf-c-label pf-m-red pf-m-outline">
  <span class="pf-c-label__content">Red removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-red-close-button"
    aria-label="Remove"
    aria-labelledby="outline-red-close-button outline-red-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-red pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Red icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-red-icon-close-button"
    aria-label="Remove"
    aria-labelledby="outline-red-icon-close-button outline-red-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-outline pf-m-red">
  <a class="pf-c-label__content" href="#">Red link</a>
</span>

<span class="pf-c-label pf-m-outline pf-m-red">
  <a class="pf-c-label__content" href="#">Red link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-red-link-close-button"
    aria-label="Remove"
    aria-labelledby="outline-red-link-close-button outline-red-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-purple pf-m-outline">
  <span class="pf-c-label__content">Purple</span>
</span>

<span class="pf-c-label pf-m-purple pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Purple icon
  </span>
</span>

<span class="pf-c-label pf-m-purple pf-m-outline">
  <span class="pf-c-label__content">Purple removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-purple-close-button"
    aria-label="Remove"
    aria-labelledby="outline-purple-close-button outline-purple-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-purple pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Purple icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-purple-icon-close-button"
    aria-label="Remove"
    aria-labelledby="outline-purple-icon-close-button outline-purple-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-outline pf-m-purple">
  <a class="pf-c-label__content" href="#">Purple link</a>
</span>

<span class="pf-c-label pf-m-outline pf-m-purple">
  <a class="pf-c-label__content" href="#">Purple link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-purple-link-close-button"
    aria-label="Remove"
    aria-labelledby="outline-purple-link-close-button outline-purple-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-cyan pf-m-outline">
  <span class="pf-c-label__content">Cyan</span>
</span>

<span class="pf-c-label pf-m-cyan pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Cyan icon
  </span>
</span>

<span class="pf-c-label pf-m-cyan pf-m-outline">
  <span class="pf-c-label__content">Cyan removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-cyan-close-button"
    aria-label="Remove"
    aria-labelledby="outline-cyan-close-button outline-cyan-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-cyan pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Cyan icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-cyan-icon-close-button"
    aria-label="Remove"
    aria-labelledby="outline-cyan-icon-close-button outline-cyan-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-outline pf-m-cyan">
  <a class="pf-c-label__content" href="#">Cyan link</a>
</span>

<span class="pf-c-label pf-m-outline pf-m-cyan">
  <a class="pf-c-label__content" href="#">Cyan link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="outline-cyan-link-close-button"
    aria-label="Remove"
    aria-labelledby="outline-cyan-link-close-button outline-cyan-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<br />
<br />

<span class="pf-c-label pf-m-gold pf-m-outline">
  <span class="pf-c-label__content">Gold</span>
</span>

<span class="pf-c-label pf-m-gold pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Gold icon
  </span>
</span>

<span class="pf-c-label pf-m-gold pf-m-outline">
  <span class="pf-c-label__content">Gold removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="gold-outline-removable-button"
    aria-label="Remove"
    aria-labelledby="gold-outline-removable-button gold-outline-removable-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-gold pf-m-outline">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Gold icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="gold-outline-icon-removable-button"
    aria-label="Remove"
    aria-labelledby="gold-outline-icon-removable-button gold-outline-icon-removable-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-gold pf-m-outline">
  <a class="pf-c-label__content" href="#">Gold link</a>
</span>

<span class="pf-c-label pf-m-gold pf-m-outline">
  <a class="pf-c-label__content" href="#">Gold link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="gold-outline-link-removable-button"
    aria-label="Remove"
    aria-labelledby="gold-outline-link-removable-button gold-outline-link-removable-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

```

### Compact

```html
<span class="pf-c-label pf-m-compact">
  <span class="pf-c-label__content">Compact</span>
</span>

<span class="pf-c-label pf-m-compact">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Compact icon
  </span>
</span>

<span class="pf-c-label pf-m-compact">
  <span class="pf-c-label__content">Compact removable</span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="compact-close-button"
    aria-label="Remove"
    aria-labelledby="compact-close-button compact-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-compact">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    Compact icon removable
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="compact-icon-close-button"
    aria-label="Remove"
    aria-labelledby="compact-icon-close-button compact-icon-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-compact">
  <a class="pf-c-label__content" href="#">Compact link</a>
</span>

<span class="pf-c-label pf-m-compact">
  <a class="pf-c-label__content" href="#">Compact link removable</a>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="compact-link-close-button"
    aria-label="Remove"
    aria-labelledby="compact-link-close-button compact-link-close-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-compact">
  <span class="pf-c-label__content">
    <span class="pf-c-label__icon">
      <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
    </span>
    <span class="pf-c-label__text">Compact label with icon that truncates</span>
  </span>
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="compact-icon-close-truncate-button"
    aria-label="Remove"
    aria-labelledby="compact-icon-close-truncate-button compact-icon-close-truncate-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

```

### Overflow

This style of label is used to indicate overflow within a label group.

```html
<button class="pf-c-label pf-m-overflow" type="button">
  <span class="pf-c-label__content">Overflow</span>
</button>

```

### Editable

**Note: Editable label behavior must be handled with JavaScript.**

-   `.pf-c-label__editable-text` onClick event should:
    -   Set `.pf-m-editable-active` on `.pf-c-label`
    -   Change `.pf-c-label__editable-text`from a button to an input
-   Return keypress, when content is editable, should:
    -   Be captured to prevent line wrapping and save updates to label text
    -   Remove `.pf-m-editable-active` from `.pf-c-label`
    -   Change `.pf-c-label__editable-text` back from an input to a button and set the `currvalue` of the button to the contents of the input
-   Esc keypress, when content is editable, should:
    -   Undo any update to label text
    -   Remove `.pf-m-editable-active` from `.pf-c-label`
    -   Change `.pf-c-label__editable-text` back to a button

```html isBeta
<span class="pf-c-label pf-m-blue pf-m-editable">
  <button
    class="pf-c-label__content"
    id="editable-label-editable-content"
    currvalue="Editable label"
    aria-label="Editable text"
  >Editable label</button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="editable-label-button"
    aria-label="Remove"
    aria-labelledby="editable-label-button editable-label-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span class="pf-c-label pf-m-blue pf-m-editable pf-m-editable-active">
  <input
    class="pf-c-label__content"
    id="editable-label-active-editable-content"
    type="text"
    value="Editable active"
    aria-label="Editable text"
  />
</span>

<span class="pf-c-label pf-m-compact pf-m-blue pf-m-editable">
  <button
    class="pf-c-label__content"
    id="compact-editable-label-editable-content"
    currvalue="Compact editable label"
    aria-label="Editable text"
  >Compact editable label</button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    id="compact-editable-label-button"
    aria-label="Remove"
    aria-labelledby="compact-editable-label-button compact-editable-label-text"
  >
    <i class="fas fa-times" aria-hidden="true"></i>
  </button>
</span>

<span
  class="pf-c-label pf-m-compact pf-m-blue pf-m-editable pf-m-editable-active"
>
  <input
    class="pf-c-label__content"
    id="compact-editable-label-active-editable-content"
    type="text"
    value="Compact editable active"
    aria-label="Editable text"
  />
</span>

```

### Add label

This style of label is used to add new labels to a label group.

```html isBeta
<button class="pf-c-label pf-m-add" type="button">
  <span class="pf-c-label__content">Add Label</span>
</button>

```

## Documentation

### Usage

| Class                        | Applied to                  | Outcome                                                                                                                                                                                                                  |
| ---------------------------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `.pf-c-label`                | `<span>`, `<button>`        | Initiates a label. Without a color modifier, the label's default style is grey. Use a color modifier to change the label color. Use a `<button>` if the label is an overflow label used in the label group. **Required** |
| `.pf-c-label__content`       | `<span>`, `<a>`, `<button>` | Iniates a label content. Use as an `<a>` element if the label serves as a link. Use a `<button>` if the label serves as an action. **Required**                                                                          |
| `.pf-c-label__icon`          | `<span>`                    | Initiates a label icon.                                                                                                                                                                                                  |
| `.pf-c-label__text`          | `<span>`                    | Initiates label text.                                                                                                                                                                                                    |
| `.pf-c-label__editable-text` | `<button>`, `<input>`       | Initiates editable label text. See the [editable](#editable) example for details about handling behavior in Javascript.                                                                                                  |
| `.pf-m-outline`              | `.pf-c-label`               | Modifies label for outline styles.                                                                                                                                                                                       |
| `.pf-m-compact`              | `.pf-c-label`               | Modifies label for compact styles.                                                                                                                                                                                       |
| `.pf-m-overflow`             | `.pf-c-label`               | Modifies label for overflow styles for use in a label group.                                                                                                                                                             |
| `.pf-m-add`                  | `.pf-c-label`               | Modifies label for add styles for use in a label group.                                                                                                                                                                  |
| `.pf-m-blue`                 | `.pf-c-label`               | Modifies the label to have blue colored styling.                                                                                                                                                                         |
| `.pf-m-green`                | `.pf-c-label`               | Modifies the label to have green colored styling.                                                                                                                                                                        |
| `.pf-m-orange`               | `.pf-c-label`               | Modifies the label to have orange colored styling.                                                                                                                                                                       |
| `.pf-m-red`                  | `.pf-c-label`               | Modifies the label to have red colored styling.                                                                                                                                                                          |
| `.pf-m-purple`               | `.pf-c-label`               | Modifies the label to have purple colored styling.                                                                                                                                                                       |
| `.pf-m-cyan`                 | `.pf-c-label`               | Modifies the label to have cyan colored styling.                                                                                                                                                                         |
| `.pf-m-gold`                 | `.pf-c-label`               | Modifies the label to have gold colored styling.                                                                                                                                                                         |
| `.pf-m-editable`             | `.pf-c-label`               | Modifies label for editable styles.                                                                                                                                                                                      |
| `.pf-m-editable-active`      | `.pf-c-label.pf-m-editable` | Modifies editable label for active styles.                                                                                                                                                                               |
