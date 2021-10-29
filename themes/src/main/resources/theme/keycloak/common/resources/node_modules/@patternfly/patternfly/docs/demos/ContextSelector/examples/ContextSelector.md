---
id: 'Context selector'
section: components
---## Examples

### Context selector in masthead

```html isFullscreen
<div class="pf-c-page" id="context-selector-in-masthead">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#context-selector-in-masthead-main"
  >Skip to content</a>
  <header class="pf-c-masthead" id="context-selector-in-masthead-masthead">
    <span class="pf-c-masthead__toggle">
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Global navigation"
      >
        <i class="fas fa-bars" aria-hidden="true"></i>
      </button>
    </span>

    <div class="pf-c-masthead__main">
      <a class="pf-c-masthead__brand" href="#">
        <picture
          class="pf-c-brand pf-m-picture"
          style="--pf-c-brand--Width: 180px; --pf-c-brand--Width-on-md: 180px; --pf-c-brand--Width-on-2xl: 220px;"
        >
          <source
            media="(min-width: 768px)"
            srcset="/assets/images/logo__pf--reverse-on-md.svg"
          />
          <source srcset="/assets/images/logo__pf--reverse--base.svg" />
          <img
            src="/assets/images/logo__pf--reverse--base.png"
            alt="Fallback patternFly default logo"
          />
        </picture>
      </a>
    </div>
    <div class="pf-c-masthead__content">
      <div
        class="pf-c-toolbar pf-m-full-height pf-m-static"
        id="context-selector-in-masthead-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div class="pf-c-toolbar__item">
              <div class="pf-c-context-selector pf-m-full-height">
                <span
                  id="context-selector-in-masthead-masthead-context-selector-label"
                  hidden
                >Selected project:</span>
                <button
                  class="pf-c-context-selector__toggle"
                  aria-expanded="false"
                  id="context-selector-in-masthead-masthead-context-selector-toggle"
                  aria-labelledby="context-selector-in-masthead-masthead-context-selector-label context-selector-in-masthead-masthead-context-selector-toggle"
                >
                  <span
                    class="pf-c-context-selector__toggle-text"
                  >Context selector</span>
                  <span class="pf-c-context-selector__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-context-selector__menu" hidden>
                  <div class="pf-c-context-selector__menu-search">
                    <div class="pf-c-input-group">
                      <input
                        class="pf-c-form-control"
                        type="search"
                        placeholder="Search"
                        id="context-selector-in-masthead-masthead-context-selectortextInput1"
                        name="context-selector-in-masthead-masthead-context-selectortextInput1"
                        aria-labelledby="context-selector-in-masthead-masthead-context-selector-search-button"
                      />
                      <button
                        class="pf-c-button pf-m-control"
                        type="button"
                        id="context-selector-in-masthead-masthead-context-selector-search-button"
                        aria-label="Search menu items"
                      >
                        <i class="fas fa-search" aria-hidden="true"></i>
                      </button>
                    </div>
                  </div>
                  <ul class="pf-c-context-selector__menu-list">
                    <li>
                      <a
                        class="pf-c-context-selector__menu-list-item"
                        href="#"
                      >Link</a>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >Action</button>
                    </li>
                    <li>
                      <a
                        class="pf-c-context-selector__menu-list-item pf-m-disabled"
                        href="#"
                        aria-disabled="true"
                        tabindex="-1"
                      >Disabled link</a>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                        disabled
                      >Disabled action</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >My project</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >OpenShift cluster</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >Production Ansible</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >AWS</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >Azure</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >My project</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >OpenShift cluster</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >Production Ansible</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >AWS</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-context-selector__menu-list-item"
                        type="button"
                      >Azure</button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>

  <aside class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="context-selector-in-masthead-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </aside>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="context-selector-in-masthead-main"
  >
    <section class="pf-c-page__main-breadcrumb pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
          <ol class="pf-c-breadcrumb__list">
            <li class="pf-c-breadcrumb__item">
              <a href="#" class="pf-c-breadcrumb__link">Section home</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a
                href="#"
                class="pf-c-breadcrumb__link pf-m-current"
                aria-current="page"
              >Section landing</a>
            </li>
          </ol>
        </nav>
      </div>
    </section>

    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>

    <section class="pf-c-page__main-section pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div class="pf-l-gallery pf-m-gutter">
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Context selector in sidebar

```html isFullscreen
<div class="pf-c-page" id="context-selector-in-sidebar">
  <header class="pf-c-page__header">
    <a
      class="pf-c-skip-to-content pf-c-button pf-m-primary"
      href="#main-content-context-selector-in-sidebar"
    >Skip to content</a>
    <header class="pf-c-masthead" id="context-selector-in-sidebar-masthead">
      <span class="pf-c-masthead__toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Global navigation"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </span>
      <div class="pf-c-masthead__main">
        <a class="pf-c-masthead__brand" href="#">
          <img
            class="pf-c-brand"
            src="/assets/images/PF-Masthead-Logo.svg"
            alt="PatternFly logo"
          />
        </a>
      </div>
      <div class="pf-c-masthead__content">test</div>
    </header>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body pf-m-menu">
      <div class="pf-c-context-selector pf-m-page-insets pf-m-large">
        <span
          id="context-selector-collapsed-example-label"
          hidden
        >Selected project:</span>
        <button
          class="pf-c-context-selector__toggle"
          aria-expanded="false"
          id="context-selector-collapsed-example-toggle"
          aria-labelledby="context-selector-collapsed-example-label context-selector-collapsed-example-toggle"
        >
          <span class="pf-c-context-selector__toggle-text">My project</span>
          <span class="pf-c-context-selector__toggle-icon">
            <i class="fas fa-caret-down" aria-hidden="true"></i>
          </span>
        </button>
        <div class="pf-c-context-selector__menu" hidden>
          <div class="pf-c-context-selector__menu-search">
            <div class="pf-c-input-group">
              <input
                class="pf-c-form-control"
                type="search"
                placeholder="Search"
                id="textInput1"
                name="textInput1"
                aria-labelledby="context-selector-collapsed-example-search-button"
              />
              <button
                class="pf-c-button pf-m-control"
                type="button"
                id="context-selector-collapsed-example-search-button"
                aria-label="Search menu items"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
          </div>
          <ul class="pf-c-context-selector__menu-list">
            <li>
              <a class="pf-c-context-selector__menu-list-item" href="#">Link</a>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Action</button>
            </li>
            <li>
              <a
                class="pf-c-context-selector__menu-list-item pf-m-disabled"
                href="#"
                aria-disabled="true"
                tabindex="-1"
              >Disabled link</a>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
                disabled
              >Disabled action</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >My project</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >OpenShift cluster</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Production Ansible</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >AWS</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Azure</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >My project</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >OpenShift cluster</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Production Ansible</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >AWS</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Azure</button>
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="context-selector-in-sidebar-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-context-selector-in-sidebar"
  >
    <section
      class="pf-c-page__main-section pf-m-limit-width pf-m-light pf-m-shadow-bottom"
    >
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>
    <section
      class="pf-c-page__main-section pf-m-limit-width pf-m-overflow-scroll"
    >
      <div class="pf-c-page__main-body">
        <div class="pf-l-gallery pf-m-gutter">
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
        </div>
      </div>
    </section>
    <section
      class="pf-c-page__main-section pf-m-limit-width pf-m-no-fill pf-m-light pf-m-shadow-top"
    >
      <div class="pf-c-page__main-body">
        <p>PatternFly is an open source design system built to drive consistency and unify teams. From documentation and components to code examples and tutorials, PatternFly is a place where design and development can thrive. We’re on a mission to help teams build consistent, accessible, and scalable enterprise product experiences—the open source way.</p>
      </div>
    </section>
  </main>
</div>

```

### Context selector in sidebar expanded

```html isFullscreen
<div class="pf-c-page" id="context-selector-in-sidebar-expanded">
  <header class="pf-c-page__header">
    <a
      class="pf-c-skip-to-content pf-c-button pf-m-primary"
      href="#main-content-context-selector-in-sidebar-expanded"
    >Skip to content</a>
    <header
      class="pf-c-masthead"
      id="context-selector-in-sidebar-expanded-masthead"
    >
      <span class="pf-c-masthead__toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          aria-label="Global navigation"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </span>
      <div class="pf-c-masthead__main">
        <a class="pf-c-masthead__brand" href="#">
          <img
            class="pf-c-brand"
            src="/assets/images/PF-Masthead-Logo.svg"
            alt="PatternFly logo"
          />
        </a>
      </div>
      <div class="pf-c-masthead__content">test</div>
    </header>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body pf-m-menu">
      <div
        class="pf-c-context-selector pf-m-expanded pf-m-page-insets pf-m-large"
      >
        <span
          id="context-selector-collapsed-example-label"
          hidden
        >Selected project:</span>
        <button
          class="pf-c-context-selector__toggle"
          aria-expanded="true"
          id="context-selector-collapsed-example-toggle"
          aria-labelledby="context-selector-collapsed-example-label context-selector-collapsed-example-toggle"
        >
          <span class="pf-c-context-selector__toggle-text">My project</span>
          <span class="pf-c-context-selector__toggle-icon">
            <i class="fas fa-caret-down" aria-hidden="true"></i>
          </span>
        </button>
        <div class="pf-c-context-selector__menu">
          <div class="pf-c-context-selector__menu-search">
            <div class="pf-c-input-group">
              <input
                class="pf-c-form-control"
                type="search"
                placeholder="Search"
                id="textInput1"
                name="textInput1"
                aria-labelledby="context-selector-collapsed-example-search-button"
              />
              <button
                class="pf-c-button pf-m-control"
                type="button"
                id="context-selector-collapsed-example-search-button"
                aria-label="Search menu items"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
          </div>
          <ul class="pf-c-context-selector__menu-list">
            <li>
              <a class="pf-c-context-selector__menu-list-item" href="#">Link</a>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Action</button>
            </li>
            <li>
              <a
                class="pf-c-context-selector__menu-list-item pf-m-disabled"
                href="#"
                aria-disabled="true"
                tabindex="-1"
              >Disabled link</a>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
                disabled
              >Disabled action</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >My project</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >OpenShift cluster</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Production Ansible</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >AWS</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Azure</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >My project</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >OpenShift cluster</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Production Ansible</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >AWS</button>
            </li>
            <li>
              <button
                class="pf-c-context-selector__menu-list-item"
                type="button"
              >Azure</button>
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="context-selector-in-sidebar-expanded-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-context-selector-in-sidebar-expanded"
  >
    <section
      class="pf-c-page__main-section pf-m-limit-width pf-m-light pf-m-shadow-bottom"
    >
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>
    <section
      class="pf-c-page__main-section pf-m-limit-width pf-m-overflow-scroll"
    >
      <div class="pf-c-page__main-body">
        <div class="pf-l-gallery pf-m-gutter">
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
        </div>
      </div>
    </section>
    <section
      class="pf-c-page__main-section pf-m-limit-width pf-m-no-fill pf-m-light pf-m-shadow-top"
    >
      <div class="pf-c-page__main-body">
        <p>PatternFly is an open source design system built to drive consistency and unify teams. From documentation and components to code examples and tutorials, PatternFly is a place where design and development can thrive. We’re on a mission to help teams build consistent, accessible, and scalable enterprise product experiences—the open source way.</p>
      </div>
    </section>
  </main>
</div>

```

### Context selector in page content

```html isFullscreen
<div class="pf-c-page" id="context-selector-in-page-content">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-context-selector-in-page-content"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="context-selector-in-page-content-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="context-selector-in-page-content-primary-nav"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </div>
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>

    <div class="pf-c-page__header-tools">
      <div class="pf-c-page__header-tools-group">
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Settings"
          >
            <i class="fas fa-cog" aria-hidden="true"></i>
          </button>
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg"
        >
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Help"
          >
            <i class="pf-icon pf-icon-help" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <div class="pf-c-page__header-tools-group">
        <div class="pf-c-page__header-tools-item pf-m-hidden-on-lg">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="context-selector-in-page-content-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="context-selector-in-page-content-dropdown-kebab-1-button"
              hidden
            >
              <li>
                <a class="pf-c-dropdown__menu-item" href="#">Link</a>
              </li>
              <li>
                <button class="pf-c-dropdown__menu-item" type="button">Action</button>
              </li>
              <li>
                <a
                  class="pf-c-dropdown__menu-item pf-m-disabled"
                  href="#"
                  aria-disabled="true"
                  tabindex="-1"
                >Disabled link</a>
              </li>
              <li>
                <button
                  class="pf-c-dropdown__menu-item"
                  type="button"
                  disabled
                >Disabled action</button>
              </li>
              <li class="pf-c-divider" role="separator"></li>
              <li>
                <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
              </li>
            </ul>
          </div>
        </div>
        <div
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-md"
        >
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="context-selector-in-page-content-dropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
            >
              <span class="pf-c-dropdown__toggle-text">John Smith</span>
              <span class="pf-c-dropdown__toggle-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>
            <ul
              class="pf-c-dropdown__menu"
              aria-labelledby="context-selector-in-page-content-dropdown-kebab-2-button"
              hidden
            >
              <li>
                <a class="pf-c-dropdown__menu-item" href="#">Link</a>
              </li>
              <li>
                <button class="pf-c-dropdown__menu-item" type="button">Action</button>
              </li>
              <li>
                <a
                  class="pf-c-dropdown__menu-item pf-m-disabled"
                  href="#"
                  aria-disabled="true"
                  tabindex="-1"
                >Disabled link</a>
              </li>
              <li>
                <button
                  class="pf-c-dropdown__menu-item"
                  type="button"
                  disabled
                >Disabled action</button>
              </li>
              <li class="pf-c-divider" role="separator"></li>
              <li>
                <a class="pf-c-dropdown__menu-item" href="#">Separated link</a>
              </li>
            </ul>
          </div>
        </div>
      </div>
      <img
        class="pf-c-avatar"
        src="/assets/images/img_avatar.svg"
        alt="Avatar image"
      />
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="context-selector-in-page-content-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">System panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network services</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Server</a>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-context-selector-in-page-content"
  >
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-no-padding">
      <div class="pf-c-page__main-body">
        <div class="pf-c-toolbar pf-m-inset-none" id="toolbar-simple-example">
          <div class="pf-c-toolbar__content">
            <div class="pf-c-toolbar__content-section">
              <div class="pf-c-toolbar__item">
                <div
                  class="pf-c-context-selector pf-m-page-insets pf-m-width-auto"
                  style="--pf-c-context-selector--Width: 270px;"
                >
                  <span
                    id="context-selector-in-page-content-context-selector-label"
                    hidden
                  >Selected project:</span>
                  <button
                    class="pf-c-context-selector__toggle pf-m-text pf-m-plain"
                    aria-expanded="false"
                    id="context-selector-in-page-content-context-selector-toggle"
                    aria-labelledby="context-selector-in-page-content-context-selector-label context-selector-in-page-content-context-selector-toggle"
                  >
                    <span
                      class="pf-c-context-selector__toggle-text"
                    >Project: openshift-apple1</span>
                    <span class="pf-c-context-selector__toggle-icon">
                      <i class="fas fa-caret-down" aria-hidden="true"></i>
                    </span>
                  </button>
                  <div class="pf-c-context-selector__menu" hidden>
                    <div class="pf-c-context-selector__menu-search">
                      <div class="pf-c-input-group">
                        <input
                          class="pf-c-form-control"
                          type="search"
                          placeholder="Search"
                          id="textInput1"
                          name="textInput1"
                          aria-labelledby="context-selector-in-page-content-context-selector-search-button"
                        />
                        <button
                          class="pf-c-button pf-m-control"
                          type="button"
                          id="context-selector-in-page-content-context-selector-search-button"
                          aria-label="Search menu items"
                        >
                          <i class="fas fa-search" aria-hidden="true"></i>
                        </button>
                      </div>
                    </div>
                    <ul class="pf-c-context-selector__menu-list">
                      <li>
                        <a
                          class="pf-c-context-selector__menu-list-item"
                          href="#"
                        >Link</a>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >Action</button>
                      </li>
                      <li>
                        <a
                          class="pf-c-context-selector__menu-list-item pf-m-disabled"
                          href="#"
                          aria-disabled="true"
                          tabindex="-1"
                        >Disabled link</a>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                          disabled
                        >Disabled action</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >My project</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >OpenShift cluster</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >Production Ansible</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >AWS</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >Azure</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >My project</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >OpenShift cluster</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >Production Ansible</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >AWS</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-context-selector__menu-list-item"
                          type="button"
                        >Azure</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item">
                <div class="pf-c-select">
                  <span
                    id="context-selector-in-page-content-select-label"
                    hidden
                  >Choose one</span>

                  <button
                    class="pf-c-select__toggle pf-m-plain"
                    type="button"
                    id="context-selector-in-page-content-select-toggle"
                    aria-haspopup="true"
                    aria-expanded="false"
                    aria-labelledby="context-selector-in-page-content-select-label context-selector-in-page-content-select-toggle"
                  >
                    <div class="pf-c-select__toggle-wrapper">
                      <span class="pf-c-select__toggle-text">All applications</span>
                    </div>
                    <span class="pf-c-select__toggle-arrow">
                      <i class="fas fa-caret-down" aria-hidden="true"></i>
                    </span>
                  </button>

                  <ul
                    class="pf-c-select__menu"
                    role="listbox"
                    aria-labelledby="context-selector-in-page-content-select-label"
                    hidden
                  >
                    <li role="presentation">
                      <button
                        class="pf-c-select__menu-item"
                        role="option"
                      >Running</button>
                    </li>
                    <li role="presentation">
                      <button
                        class="pf-c-select__menu-item pf-m-selected"
                        role="option"
                        aria-selected="true"
                      >
                        Stopped
                        <span class="pf-c-select__menu-item-icon">
                          <i class="fas fa-check" aria-hidden="true"></i>
                        </span>
                      </button>
                    </li>
                    <li role="presentation">
                      <button class="pf-c-select__menu-item" role="option">Down</button>
                    </li>
                    <li role="presentation">
                      <button
                        class="pf-c-select__menu-item"
                        role="option"
                      >Degraded</button>
                    </li>
                    <li role="presentation">
                      <button
                        class="pf-c-select__menu-item"
                        role="option"
                      >Needs maintenance</button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
    <hr class="pf-c-divider" />
    <section class="pf-c-page__main-breadcrumb pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
          <ol class="pf-c-breadcrumb__list">
            <li class="pf-c-breadcrumb__item">
              <a href="#" class="pf-c-breadcrumb__link">Section home</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Section title</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a
                href="#"
                class="pf-c-breadcrumb__link pf-m-current"
                aria-current="page"
              >Section landing</a>
            </li>
          </ol>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div class="pf-l-gallery pf-m-gutter">
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
          <div class="pf-l-gallery__item">
            <div class="pf-c-card">
              <div class="pf-c-card__body">This is a card</div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```
