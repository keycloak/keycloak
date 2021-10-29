---
id: Drawer
section: components
wrapperTag: div
---## Demos

### Collapsed

```html isFullscreen
<div class="pf-c-page" id="drawer-collapsed">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-drawer-collapsed"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="drawer-collapsed-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="drawer-collapsed-primary-nav"
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
              id="drawer-collapsed-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="drawer-collapsed-dropdown-kebab-1-button"
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
              id="drawer-collapsed-dropdown-kebab-2-button"
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
              aria-labelledby="drawer-collapsed-dropdown-kebab-2-button"
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
        id="drawer-collapsed-primary-nav"
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
  </div>
  <div class="pf-c-page__drawer">
    <div class="pf-c-drawer">
      <div class="pf-c-drawer__main">
        <div class="pf-c-drawer__content">
          <div class="pf-c-drawer__body">
            <main class="pf-c-page__main" tabindex="-1">
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
              <section
                class="pf-c-page__main-section pf-m-limit-width pf-m-light"
              >
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
        </div>
        <div class="pf-c-drawer__panel pf-m-width-33" hidden>
          <div class="pf-c-drawer__body">
            <p>drawer panel</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Expanded

```html isFullscreen
<div class="pf-c-page" id="drawer-expanded">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-drawer-expanded"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="drawer-expanded-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="drawer-expanded-primary-nav"
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
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg pf-m-selected"
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
              id="drawer-expanded-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="drawer-expanded-dropdown-kebab-1-button"
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
              id="drawer-expanded-dropdown-kebab-2-button"
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
              aria-labelledby="drawer-expanded-dropdown-kebab-2-button"
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
        id="drawer-expanded-primary-nav"
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
  </div>
  <div class="pf-c-page__drawer">
    <div class="pf-c-drawer pf-m-expanded">
      <div class="pf-c-drawer__main">
        <div class="pf-c-drawer__content">
          <div class="pf-c-drawer__body">
            <main class="pf-c-page__main" tabindex="-1">
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
              <section
                class="pf-c-page__main-section pf-m-limit-width pf-m-light"
              >
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
        </div>
        <div class="pf-c-drawer__panel pf-m-width-33">
          <div class="pf-c-drawer__body">
            <p>drawer panel</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Expanded bottom

```html isFullscreen
<div class="pf-c-page" id="drawer-expanded-bottom">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-drawer-expanded-bottom"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="drawer-expanded-bottom-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="drawer-expanded-bottom-primary-nav"
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
          class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-lg pf-m-selected"
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
              id="drawer-expanded-bottom-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="drawer-expanded-bottom-dropdown-kebab-1-button"
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
              id="drawer-expanded-bottom-dropdown-kebab-2-button"
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
              aria-labelledby="drawer-expanded-bottom-dropdown-kebab-2-button"
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
        id="drawer-expanded-bottom-primary-nav"
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
  </div>
  <div class="pf-c-page__drawer">
    <div class="pf-c-drawer pf-m-expanded pf-m-panel-bottom">
      <div class="pf-c-drawer__main">
        <div class="pf-c-drawer__content">
          <div class="pf-c-drawer__body">
            <main class="pf-c-page__main" tabindex="-1">
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
              <section
                class="pf-c-page__main-section pf-m-limit-width pf-m-light"
              >
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
        </div>
        <div class="pf-c-drawer__panel pf-m-width-33">
          <div class="pf-c-drawer__body">
            <p>drawer panel</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

## Documentation

This demo implements the drawer in context of the page component.
