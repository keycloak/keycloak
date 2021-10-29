---
id: About modal
section: components
---## Demos

### Basic

This demo implements the about modal, including the backdrop.

```html isFullscreen
<div aria-hidden="true">Page content</div>
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
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
        <h1
          class="pf-c-title pf-m-4xl"
          id="about-modal-title"
        >Red Hat OpenShift Container Platform</h1>
      </div>
      <div class="pf-c-about-modal-box__hero"></div>
      <div class="pf-c-about-modal-box__content">
        <div class="pf-c-content">
          <dl>
            <dt>CFME version</dt>
            <dd>5.5.3.4.20102789036450</dd>
            <dt>Cloudforms version</dt>
            <dd>4.1</dd>
            <dt>Server name</dt>
            <dd>40DemoMaster</dd>
            <dt>User name</dt>
            <dd>Administrator</dd>
            <dt>User role</dt>
            <dd>EvmRole-super_administrator</dd>
            <dt>Browser version</dt>
            <dd>601.2</dd>
            <dt>Browser OS</dt>
            <dd>Mac</dd>
          </dl>
        </div>
        <p
          class="pf-c-about-modal-box__strapline"
        >Trademark and copyright information here</p>
      </div>
    </div>
  </div>
</div>

```
