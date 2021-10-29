---
id: Navigation
section: components
---## Examples

### Default nav

```html isFullscreen
<div class="pf-c-page" id="page-default-nav-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-default-nav-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="page-default-nav-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="page-default-nav-example-primary-nav"
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
              id="page-default-nav-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-default-nav-example-dropdown-kebab-1-button"
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
              id="page-default-nav-example-dropdown-kebab-2-button"
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
              aria-labelledby="page-default-nav-example-dropdown-kebab-2-button"
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
        id="page-default-nav-example-primary-nav"
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
    id="main-content-page-default-nav-example"
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

### Expandable nav

```html isFullscreen
<div class="pf-c-page" id="page-expandable-nav-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-expandable-nav-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="page-expandable-nav-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="page-expandable-nav-example-expandable-nav"
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
              id="page-expandable-nav-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-expandable-nav-example-dropdown-kebab-1-button"
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
              id="page-expandable-nav-example-dropdown-kebab-2-button"
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
              aria-labelledby="page-expandable-nav-example-dropdown-kebab-2-button"
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
        id="page-expandable-nav-example-expandable-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item pf-m-expandable pf-m-expanded pf-m-current">
            <button
              class="pf-c-nav__link"
              id="page-expandable-nav-example-expandable-nav-link1"
              aria-expanded="true"
            >
              System panel
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-expandable-nav-example-expandable-nav-link1"
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Overview</a>
                </li>
                <li class="pf-c-nav__item">
                  <a
                    href="#"
                    class="pf-c-nav__link pf-m-current"
                    aria-current="page"
                  >Resource usage</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Hypervisors</a>
                </li>
                <li class="pf-c-divider" role="separator"></li>

                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Instances</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Volumes</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Networks</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button
              class="pf-c-nav__link"
              id="page-expandable-nav-example-expandable-nav-link2"
              aria-expanded="false"
            >
              Policy
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-expandable-nav-example-expandable-nav-link2"
              hidden
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 1</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 2</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button
              class="pf-c-nav__link"
              id="page-expandable-nav-example-expandable-nav-link3"
              aria-expanded="false"
            >
              Authentication
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-expandable-nav-example-expandable-nav-link3"
              hidden
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 1</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 2</a>
                </li>
              </ul>
            </section>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-page-expandable-nav-example"
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

### Horizontal nav

```html isFullscreen
<div class="pf-c-page" id="page-layout-horizontal-nav">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-layout-horizontal-nav"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>
    <div class="pf-c-page__header-nav">
      <nav
        class="pf-c-nav pf-m-horizontal pf-m-scrollable"
        id="page-layout-horizontal-nav-horizontal-nav"
        aria-label="Global"
      >
        <button
          class="pf-c-nav__scroll-button"
          disabled
          aria-label="Scroll left"
        >
          <i class="fas fa-angle-left" aria-hidden="true"></i>
        </button>
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Horizontal nav item 1</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Horizontal nav item 2</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Horizontal nav item 3</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Horizontal nav item 4</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Horizontal nav item 5</a>
          </li>
        </ul>
        <button class="pf-c-nav__scroll-button" aria-label="Scroll right">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </button>
      </nav>
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
              id="page-layout-horizontal-nav-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-layout-horizontal-nav-dropdown-kebab-1-button"
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
              id="page-layout-horizontal-nav-dropdown-kebab-2-button"
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
              aria-labelledby="page-layout-horizontal-nav-dropdown-kebab-2-button"
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
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-page-layout-horizontal-nav"
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

### Horizontal subnav

```html isFullscreen
<div class="pf-c-page" id="page-layout-horizontal-subnav">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-layout-horizontal-subnav"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="page-layout-horizontal-subnav-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="page-layout-horizontal-subnav-tertiary-nav"
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
              id="page-layout-horizontal-subnav-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-layout-horizontal-subnav-dropdown-kebab-1-button"
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
              id="page-layout-horizontal-subnav-dropdown-kebab-2-button"
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
              aria-labelledby="page-layout-horizontal-subnav-dropdown-kebab-2-button"
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
        id="page-layout-horizontal-subnav-tertiary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item pf-m-expandable pf-m-expanded pf-m-current">
            <button
              class="pf-c-nav__link"
              id="tertiary-nav-link1"
              aria-expanded="true"
            >
              System panel
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="tertiary-nav-link1"
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Overview</a>
                </li>
                <li class="pf-c-nav__item">
                  <a
                    href="#"
                    class="pf-c-nav__link pf-m-current"
                    aria-current="page"
                  >Resource usage</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Hypervisors</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Instances</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Volumes</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Networks</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button
              class="pf-c-nav__link"
              id="tertiary-nav-link2"
              aria-expanded="false"
            >
              Policy
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="tertiary-nav-link2"
              hidden
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 1</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 2</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button
              class="pf-c-nav__link"
              id="tertiary-nav-link3"
              aria-expanded="false"
            >
              Authentication
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="tertiary-nav-link3"
              hidden
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 1</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 2</a>
                </li>
              </ul>
            </section>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-page-layout-horizontal-subnav"
  >
    <section class="pf-c-page__main-subnav pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav
          class="pf-c-nav pf-m-horizontal-subnav pf-m-scrollable"
          aria-label="Local"
        >
          <button
            class="pf-c-nav__scroll-button"
            disabled
            aria-label="Scroll left"
          >
            <i class="fas fa-angle-left" aria-hidden="true"></i>
          </button>
          <ul class="pf-c-nav__list">
            <li class="pf-c-nav__item">
              <a
                href="#"
                class="pf-c-nav__link pf-m-current"
                aria-current="page"
              >Horizontal subnav item 1</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Horizontal subnav item 2</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Horizontal subnav item 3</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Horizontal subnav item 4</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Horizontal subnav item 5</a>
            </li>
          </ul>
          <button class="pf-c-nav__scroll-button" aria-label="Scroll right">
            <i class="fas fa-angle-right" aria-hidden="true"></i>
          </button>
        </nav>
      </div>
    </section>
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
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-fill">
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
      class="pf-c-page__main-section pf-m-limit-width pf-m-no-fill pf-m-light"
    >
      <div class="pf-c-page__main-body">
        <p>PatternFly is an open source design system built to drive consistency and unify teams. From documentation and components to code examples and tutorials, PatternFly is a place where design and development can thrive. We’re on a mission to help teams build consistent, accessible, and scalable enterprise product experiences—the open source way.</p>
      </div>
    </section>
  </main>
</div>

```

### Horizontal nav with horizontal subnav

```html isFullscreen
<div class="pf-c-page" id="page-layout-horizontal-nav-horizontal-subnav">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-layout-horizontal-nav-horizontal-subnav"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="PatternFly logo"
        />
      </a>
    </div>
    <div class="pf-c-page__header-nav">
      <nav
        class="pf-c-nav pf-m-horizontal pf-m-scrollable"
        id="page-layout-horizontal-nav-horizontal-subnav-horizontal-nav"
        aria-label="Global"
      >
        <button
          class="pf-c-nav__scroll-button"
          disabled
          aria-label="Scroll left"
        >
          <i class="fas fa-angle-left" aria-hidden="true"></i>
        </button>
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Horizontal nav item 1</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Horizontal nav item 2</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Horizontal nav item 3</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Horizontal nav item 4</a>
          </li>
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >Horizontal nav item 5</a>
          </li>
        </ul>
        <button class="pf-c-nav__scroll-button" aria-label="Scroll right">
          <i class="fas fa-angle-right" aria-hidden="true"></i>
        </button>
      </nav>
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
              id="page-layout-horizontal-nav-horizontal-subnav-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-layout-horizontal-nav-horizontal-subnav-dropdown-kebab-1-button"
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
              id="page-layout-horizontal-nav-horizontal-subnav-dropdown-kebab-2-button"
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
              aria-labelledby="page-layout-horizontal-nav-horizontal-subnav-dropdown-kebab-2-button"
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
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-page-layout-horizontal-nav-horizontal-subnav"
  >
    <section class="pf-c-page__main-subnav pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav
          class="pf-c-nav pf-m-horizontal-subnav pf-m-scrollable"
          aria-label="Local"
        >
          <button
            class="pf-c-nav__scroll-button"
            disabled
            aria-label="Scroll left"
          >
            <i class="fas fa-angle-left" aria-hidden="true"></i>
          </button>
          <ul class="pf-c-nav__list">
            <li class="pf-c-nav__item">
              <a
                href="#"
                class="pf-c-nav__link pf-m-current"
                aria-current="page"
              >Horizontal subnav item 1</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Horizontal subnav item 2</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Horizontal subnav item 3</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Horizontal subnav item 4</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Horizontal subnav item 5</a>
            </li>
          </ul>
          <button class="pf-c-nav__scroll-button" aria-label="Scroll right">
            <i class="fas fa-angle-right" aria-hidden="true"></i>
          </button>
        </nav>
      </div>
    </section>
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

### Legacy tertiary nav

```html isFullscreen
<div class="pf-c-page" id="page-layout-legacy-tertiary-nav">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-layout-legacy-tertiary-nav"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="page-layout-legacy-tertiary-nav-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="page-layout-legacy-tertiary-nav-tertiary-nav"
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
              id="page-layout-legacy-tertiary-nav-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-layout-legacy-tertiary-nav-dropdown-kebab-1-button"
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
              id="page-layout-legacy-tertiary-nav-dropdown-kebab-2-button"
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
              aria-labelledby="page-layout-legacy-tertiary-nav-dropdown-kebab-2-button"
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
        id="page-layout-legacy-tertiary-nav-tertiary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item pf-m-expandable pf-m-expanded pf-m-current">
            <button
              class="pf-c-nav__link"
              id="tertiary-nav-link1"
              aria-expanded="true"
            >
              System panel
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="tertiary-nav-link1"
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Overview</a>
                </li>
                <li class="pf-c-nav__item">
                  <a
                    href="#"
                    class="pf-c-nav__link pf-m-current"
                    aria-current="page"
                  >Resource usage</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Hypervisors</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Instances</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Volumes</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Networks</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button
              class="pf-c-nav__link"
              id="tertiary-nav-link2"
              aria-expanded="false"
            >
              Policy
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="tertiary-nav-link2"
              hidden
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 1</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 2</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button
              class="pf-c-nav__link"
              id="tertiary-nav-link3"
              aria-expanded="false"
            >
              Authentication
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="tertiary-nav-link3"
              hidden
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 1</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 2</a>
                </li>
              </ul>
            </section>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-page-layout-legacy-tertiary-nav"
  >
    <section class="pf-c-page__main-nav pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav
          class="pf-c-nav pf-m-horizontal pf-m-tertiary pf-m-scrollable"
          aria-label="Local"
        >
          <button
            class="pf-c-nav__scroll-button"
            disabled
            aria-label="Scroll left"
          >
            <i class="fas fa-angle-left" aria-hidden="true"></i>
          </button>
          <ul class="pf-c-nav__list">
            <li class="pf-c-nav__item">
              <a
                href="#"
                class="pf-c-nav__link pf-m-current"
                aria-current="page"
              >Tertiary nav item 1</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Tertiary nav item 2</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Tertiary nav item 3</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Tertiary nav item 4</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Tertiary nav item 5</a>
            </li>
          </ul>
          <button class="pf-c-nav__scroll-button" aria-label="Scroll right">
            <i class="fas fa-angle-right" aria-hidden="true"></i>
          </button>
        </nav>
      </div>
    </section>
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
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-fill">
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
      class="pf-c-page__main-section pf-m-limit-width pf-m-no-fill pf-m-light"
    >
      <div class="pf-c-page__main-body">
        <p>PatternFly is an open source design system built to drive consistency and unify teams. From documentation and components to code examples and tutorials, PatternFly is a place where design and development can thrive. We’re on a mission to help teams build consistent, accessible, and scalable enterprise product experiences—the open source way.</p>
      </div>
    </section>
  </main>
</div>

```

### Grouped nav

```html isFullscreen
<div class="pf-c-page" id="page-layout-grouped-nav">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-layout-grouped-nav"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="page-layout-grouped-nav-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="page-layout-grouped-nav-grouped-nav"
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
              id="page-layout-grouped-nav-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-layout-grouped-nav-dropdown-kebab-1-button"
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
              id="page-layout-grouped-nav-dropdown-kebab-2-button"
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
              aria-labelledby="page-layout-grouped-nav-dropdown-kebab-2-button"
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
        id="page-layout-grouped-nav-grouped-nav"
        aria-label="Global"
      >
        <section class="pf-c-nav__section" aria-labelledby="grouped-title1">
          <h2 class="pf-c-nav__section-title" id="grouped-title1">System panel</h2>
          <ul class="pf-c-nav__list">
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Overview</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Resource usage</a>
            </li>
            <li class="pf-c-nav__item">
              <a
                href="#"
                class="pf-c-nav__link pf-m-current"
                aria-current="page"
              >Hypervisors</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Instances</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Volumes</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Networks</a>
            </li>
          </ul>
        </section>
        <section class="pf-c-nav__section" aria-labelledby="grouped-title2">
          <h2 class="pf-c-nav__section-title" id="grouped-title2">Policy</h2>
          <ul class="pf-c-nav__list">
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Hosts</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Virtual machines</a>
            </li>
            <li class="pf-c-nav__item">
              <a href="#" class="pf-c-nav__link">Storage</a>
            </li>
          </ul>
        </section>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-page-layout-grouped-nav"
  >
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Main title</h1>
          <p>This is a demo of the page component.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-light"></section>
    <section class="pf-c-page__main-section pf-m-dark-200"></section>
    <section class="pf-c-page__main-section"></section>
  </main>
</div>

```

### Light theme sidebar and nav

```html isFullscreen
<div class="pf-c-page" id="page-light-sidebar-nav-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-light-sidebar-nav-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="page-light-sidebar-nav-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="page-light-sidebar-nav-example-expandable-nav"
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
              id="page-light-sidebar-nav-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-light-sidebar-nav-example-dropdown-kebab-1-button"
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
              id="page-light-sidebar-nav-example-dropdown-kebab-2-button"
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
              aria-labelledby="page-light-sidebar-nav-example-dropdown-kebab-2-button"
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

  <div class="pf-c-page__sidebar pf-m-light">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav pf-m-light"
        id="page-light-sidebar-nav-example-expandable-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item pf-m-expandable pf-m-expanded pf-m-current">
            <button
              class="pf-c-nav__link"
              id="page-light-sidebar-nav-example-expandable-nav-link1"
              aria-expanded="true"
            >
              System panel
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-light-sidebar-nav-example-expandable-nav-link1"
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Overview</a>
                </li>
                <li class="pf-c-nav__item">
                  <a
                    href="#"
                    class="pf-c-nav__link pf-m-current"
                    aria-current="page"
                  >Resource usage</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Hypervisors</a>
                </li>
                <li class="pf-c-divider" role="separator"></li>

                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Instances</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Volumes</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Networks</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button
              class="pf-c-nav__link"
              id="page-light-sidebar-nav-example-expandable-nav-link2"
              aria-expanded="false"
            >
              Policy
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-light-sidebar-nav-example-expandable-nav-link2"
              hidden
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 1</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 2</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button
              class="pf-c-nav__link"
              id="page-light-sidebar-nav-example-expandable-nav-link3"
              aria-expanded="false"
            >
              Authentication
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-light-sidebar-nav-example-expandable-nav-link3"
              hidden
            >
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 1</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Subnav link 2</a>
                </li>
              </ul>
            </section>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-page-light-sidebar-nav-example"
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
