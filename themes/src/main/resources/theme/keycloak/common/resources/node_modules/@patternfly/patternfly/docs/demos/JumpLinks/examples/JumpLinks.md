---
id: Jump links
beta: true
section: components
---## Examples

### Vertical jump links collapsed on mobile

```html isFullscreen
<div class="pf-c-page" id="vertical-collapsed-on-mobile">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-vertical-collapsed-on-mobile"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="vertical-collapsed-on-mobile-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="vertical-collapsed-on-mobile-primary-nav"
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
              id="vertical-collapsed-on-mobile-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="vertical-collapsed-on-mobile-dropdown-kebab-1-button"
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
              id="vertical-collapsed-on-mobile-dropdown-kebab-2-button"
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
              aria-labelledby="vertical-collapsed-on-mobile-dropdown-kebab-2-button"
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
        id="vertical-collapsed-on-mobile-primary-nav"
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
    id="main-content-vertical-collapsed-on-mobile"
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
    <section class="pf-c-page__main-section pf-m-no-padding">
      <div class="pf-c-sidebar">
        <div class="pf-c-sidebar__main">
          <div class="pf-c-sidebar__panel pf-m-sticky pf-m-gutter">
            <section class="pf-c-page__main-section">
              <nav
                class="pf-c-jump-links pf-m-vertical pf-m-non-expandable-on-md pf-m-expandable"
              >
                <div class="pf-c-jump-links__label">Jump to section</div>
                <ul class="pf-c-jump-links__list">
                  <li class="pf-c-jump-links__item pf-m-current">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-collapsed-on-mobile-first"
                    >
                      <span class="pf-c-jump-links__link-text">First section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-collapsed-on-mobile-second"
                    >
                      <span class="pf-c-jump-links__link-text">Second section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-collapsed-on-mobile-third"
                    >
                      <span class="pf-c-jump-links__link-text">Third section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-collapsed-on-mobile-fourth"
                    >
                      <span class="pf-c-jump-links__link-text">Fourth section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-collapsed-on-mobile-fifth"
                    >
                      <span class="pf-c-jump-links__link-text">Fifth section</span>
                    </a>
                  </li>
                </ul>
              </nav>
            </section>
          </div>
          <div class="pf-c-sidebar__content">
            <section class="pf-c-page__main-section">
              <div class="pf-c-content">
                <h2 id="vertical-collapsed-on-mobile-first">First section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-collapsed-on-mobile-second">Second section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-collapsed-on-mobile-third">Third section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-collapsed-on-mobile-fourth">Fourth section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-collapsed-on-mobile-fifth">Fifth section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>
              </div>
            </section>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Vertical jump links expanded on mobile

```html isFullscreen
<div class="pf-c-page" id="vertical-expanded-on-mobile">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-vertical-expanded-on-mobile"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="vertical-expanded-on-mobile-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="vertical-expanded-on-mobile-primary-nav"
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
              id="vertical-expanded-on-mobile-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="vertical-expanded-on-mobile-dropdown-kebab-1-button"
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
              id="vertical-expanded-on-mobile-dropdown-kebab-2-button"
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
              aria-labelledby="vertical-expanded-on-mobile-dropdown-kebab-2-button"
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
        id="vertical-expanded-on-mobile-primary-nav"
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
    id="main-content-vertical-expanded-on-mobile"
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
    <section class="pf-c-page__main-section pf-m-no-padding">
      <div class="pf-c-sidebar">
        <div class="pf-c-sidebar__main">
          <div class="pf-c-sidebar__panel pf-m-sticky pf-m-gutter">
            <section class="pf-c-page__main-section">
              <nav
                class="pf-c-jump-links pf-m-vertical pf-m-non-expandable-on-md pf-m-expanded pf-m-expandable"
              >
                <div class="pf-c-jump-links__label">Jump to section</div>
                <ul class="pf-c-jump-links__list">
                  <li class="pf-c-jump-links__item pf-m-current">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-expanded-on-mobile-first"
                    >
                      <span class="pf-c-jump-links__link-text">First section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-expanded-on-mobile-second"
                    >
                      <span class="pf-c-jump-links__link-text">Second section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-expanded-on-mobile-third"
                    >
                      <span class="pf-c-jump-links__link-text">Third section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-expanded-on-mobile-fourth"
                    >
                      <span class="pf-c-jump-links__link-text">Fourth section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-expanded-on-mobile-fifth"
                    >
                      <span class="pf-c-jump-links__link-text">Fifth section</span>
                    </a>
                  </li>
                </ul>
              </nav>
            </section>
          </div>
          <div class="pf-c-sidebar__content">
            <section class="pf-c-page__main-section">
              <div class="pf-c-content">
                <h2 id="vertical-expanded-on-mobile-first">First section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-expanded-on-mobile-second">Second section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-expanded-on-mobile-third">Third section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-expanded-on-mobile-fourth">Fourth section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-expanded-on-mobile-fifth">Fifth section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>
              </div>
            </section>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Vertical jump links toggle text on mobile

```html isFullscreen
<div class="pf-c-page" id="vertical-toggle-text-on-mobile">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-vertical-toggle-text-on-mobile"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="vertical-toggle-text-on-mobile-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="vertical-toggle-text-on-mobile-primary-nav"
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
              id="vertical-toggle-text-on-mobile-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="vertical-toggle-text-on-mobile-dropdown-kebab-1-button"
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
              id="vertical-toggle-text-on-mobile-dropdown-kebab-2-button"
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
              aria-labelledby="vertical-toggle-text-on-mobile-dropdown-kebab-2-button"
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
        id="vertical-toggle-text-on-mobile-primary-nav"
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
    id="main-content-vertical-toggle-text-on-mobile"
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
    <section class="pf-c-page__main-section pf-m-no-padding">
      <div class="pf-c-sidebar">
        <div class="pf-c-sidebar__main">
          <div class="pf-c-sidebar__panel pf-m-sticky pf-m-gutter">
            <section class="pf-c-page__main-section">
              <nav
                class="pf-c-jump-links pf-m-vertical pf-m-non-expandable-on-md pf-m-expandable"
              >
                <div class="pf-c-jump-links__label pf-m-toggle">Jump to section</div>
                <ul class="pf-c-jump-links__list">
                  <li class="pf-c-jump-links__item pf-m-current">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-toggle-text-on-mobile-first"
                    >
                      <span class="pf-c-jump-links__link-text">First section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-toggle-text-on-mobile-second"
                    >
                      <span class="pf-c-jump-links__link-text">Second section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-toggle-text-on-mobile-third"
                    >
                      <span class="pf-c-jump-links__link-text">Third section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-toggle-text-on-mobile-fourth"
                    >
                      <span class="pf-c-jump-links__link-text">Fourth section</span>
                    </a>
                  </li>
                  <li class="pf-c-jump-links__item">
                    <a
                      class="pf-c-jump-links__link"
                      href="#vertical-toggle-text-on-mobile-fifth"
                    >
                      <span class="pf-c-jump-links__link-text">Fifth section</span>
                    </a>
                  </li>
                </ul>
              </nav>
            </section>
          </div>
          <div class="pf-c-sidebar__content">
            <section class="pf-c-page__main-section">
              <div class="pf-c-content">
                <h2 id="vertical-toggle-text-on-mobile-first">First section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-toggle-text-on-mobile-second">Second section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-toggle-text-on-mobile-third">Third section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-toggle-text-on-mobile-fourth">Fourth section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

                <h2 id="vertical-toggle-text-on-mobile-fifth">Fifth section</h2>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>
              </div>
            </section>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Horizontal jump links

```html isFullscreen
<div class="pf-c-page" id="horizontal">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-horizontal"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="horizontal-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="horizontal-primary-nav"
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
              id="horizontal-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="horizontal-dropdown-kebab-1-button"
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
              id="horizontal-dropdown-kebab-2-button"
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
              aria-labelledby="horizontal-dropdown-kebab-2-button"
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
      <nav class="pf-c-nav" id="horizontal-primary-nav" aria-label="Global">
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
  <main class="pf-c-page__main" tabindex="-1" id="main-content-horizontal">
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
    <section class="pf-c-page__main-section pf-m-sticky-top">
      <nav class="pf-c-jump-links pf-m-center">
        <div class="pf-c-jump-links__label pf-m-toggle">Jump to section</div>
        <ul class="pf-c-jump-links__list">
          <li class="pf-c-jump-links__item pf-m-current">
            <a class="pf-c-jump-links__link" href="#horizontal-first">
              <span class="pf-c-jump-links__link-text">First section</span>
            </a>
          </li>
          <li class="pf-c-jump-links__item">
            <a class="pf-c-jump-links__link" href="#horizontal-second">
              <span class="pf-c-jump-links__link-text">Second section</span>
            </a>
          </li>
          <li class="pf-c-jump-links__item">
            <a class="pf-c-jump-links__link" href="#horizontal-third">
              <span class="pf-c-jump-links__link-text">Third section</span>
            </a>
          </li>
          <li class="pf-c-jump-links__item">
            <a class="pf-c-jump-links__link" href="#horizontal-fourth">
              <span class="pf-c-jump-links__link-text">Fourth section</span>
            </a>
          </li>
          <li class="pf-c-jump-links__item">
            <a class="pf-c-jump-links__link" href="#horizontal-fifth">
              <span class="pf-c-jump-links__link-text">Fifth section</span>
            </a>
          </li>
        </ul>
      </nav>
    </section>
    <section class="pf-c-page__main-section">
      <div class="pf-c-content">
        <h2 id="horizontal-first">First section</h2>
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

        <h2 id="horizontal-second">Second section</h2>
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

        <h2 id="horizontal-third">Third section</h2>
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

        <h2 id="horizontal-fourth">Fourth section</h2>
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>

        <h2 id="horizontal-fifth">Fifth section</h2>
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas sed dui ullamcorper, dignissim purus eu, mattis leo. Curabitur eleifend turpis ipsum, aliquam pretium risus efficitur vel. Etiam eget enim vitae quam facilisis pharetra at eget diam. Suspendisse ut vulputate magna. Nunc viverra posuere orci sit amet pulvinar. Quisque dui justo, egestas ac accumsan suscipit, tristique eu risus. In aliquet libero ante, ac pulvinar lectus pretium in. Ut enim tellus, vulputate et lorem et, hendrerit rutrum diam. Cras pharetra dapibus elit vitae ullamcorper. Nulla facilisis euismod diam, at sodales sem laoreet hendrerit. Curabitur porttitor ex nulla. Proin ligula leo, egestas ac nibh a, pellentesque mollis augue. Donec nec augue vehicula eros pulvinar vehicula eget rutrum neque. Duis sit amet interdum lorem. Vivamus porttitor nec quam a accumsan. Nam pretium vitae leo vitae rhoncus.</p>
      </div>
    </section>
  </main>
</div>

```
