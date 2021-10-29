---
id: 'Tabs'
section: components
---## Examples

### Open tabs

```html isFullscreen
<div class="pf-c-page" id="open-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-open-tabs-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="open-tabs-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="open-tabs-example-primary-nav"
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
              id="open-tabs-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="open-tabs-example-dropdown-kebab-1-button"
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
              id="open-tabs-example-dropdown-kebab-2-button"
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
              aria-labelledby="open-tabs-example-dropdown-kebab-2-button"
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
        id="open-tabs-example-primary-nav"
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
    id="main-content-open-tabs-example"
  >
    <section class="pf-c-page__main-breadcrumb pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
          <ol class="pf-c-breadcrumb__list">
            <li class="pf-c-breadcrumb__item">
              <a href="#" class="pf-c-breadcrumb__link">Overview</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Pods</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a
                href="#"
                class="pf-c-breadcrumb__link pf-m-current"
                aria-current="page"
              >Pod details</a>
            </li>
          </ol>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div
          class="pf-l-flex pf-m-space-items-md pf-m-align-items-flex-start pf-m-nowrap"
        >
          <div class="pf-l-flex__item">
            <span class="pf-c-label pf-m-blue">
              <span class="pf-c-label__content">N</span>
            </span>
          </div>
          <div class="pf-l-flex__item">
            <h1 class="pf-c-title pf-m-2xl">3scale-control-fccb6ddb9-phyqv9</h1>
          </div>
          <div class="pf-l-flex__item pf-m-flex-none">
            <span class="pf-c-label">
              <span class="pf-c-label__content">
                <span class="pf-c-label__icon">
                  <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
                </span>
                Running
              </span>
            </span>
          </div>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-page-insets"
          id="open-tabs-example-tabs-list"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-tabs-example-tabs-list-details-panel"
                id="open-tabs-example-tabs-list-details-link"
              >
                <span class="pf-c-tabs__item-text">Details</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-tabs-example-tabs-list-yaml-panel"
                id="open-tabs-example-tabs-list-yaml-link"
              >
                <span class="pf-c-tabs__item-text">YAML</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-tabs-example-tabs-list-environment-panel"
                id="open-tabs-example-tabs-list-environment-link"
              >
                <span class="pf-c-tabs__item-text">Environment</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-tabs-example-tabs-list-logs-panel"
                id="open-tabs-example-tabs-list-logs-link"
              >
                <span class="pf-c-tabs__item-text">Logs</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-tabs-example-tabs-list-events-panel"
                id="open-tabs-example-tabs-list-events-link"
              >
                <span class="pf-c-tabs__item-text">Events</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-tabs-example-tabs-list-terminal-panel"
                id="open-tabs-example-tabs-list-terminal-link"
              >
                <span class="pf-c-tabs__item-text">Terminal</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <section
          class="pf-c-tab-content"
          aria-labelledby="open-tabs-example-tabs-list-details-link"
          id="open-tabs-example-tabs-list-details-panel"
          role="tabpanel"
          tabindex="0"
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-l-flex pf-m-column">
              <div class="pf-l-flex__item pf-m-spacer-lg">
                <h2
                  class="pf-c-title pf-m-lg pf-u-mt-sm"
                  id="open-tabs-example-tabs-list-details-title"
                >Pod details</h2>
              </div>
              <div class="pf-l-flex__item">
                <dl
                  class="pf-c-description-list pf-m-2-col-on-lg"
                  aria-labelledby="open-tabs-example-tabs-list-details-title"
                >
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Name</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div
                        class="pf-c-description-list__text"
                      >3scale-control-fccb6ddb9-phyqv9</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Status</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">
                        <div class="pf-l-flex pf-m-space-items-sm">
                          <div class="pf-l-flex__item">
                            <i
                              class="fas fa-fw fa-check-circle"
                              aria-hidden="true"
                            ></i>
                          </div>
                          <div class="pf-l-flex__item">Running</div>
                        </div>
                      </div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Namespace</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">
                        <div class="pf-l-flex pf-m-space-items-sm">
                          <div class="pf-l-flex__item">
                            <span class="pf-c-label pf-m-cyan">
                              <span class="pf-c-label__content">NS</span>
                            </span>
                          </div>
                          <div class="pf-l-flex__item">
                            <a href="#">knative-serving-ingress</a>
                          </div>
                        </div>
                      </div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Restart policy</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">Always restart</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Labels</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">
                        <div class="pf-c-label-group">
                          <div class="pf-c-label-group__main">
                            <ul
                              class="pf-c-label-group__list"
                              role="list"
                              aria-label="Group of labels"
                            >
                              <li class="pf-c-label-group__list-item">
                                <span class="pf-c-label pf-m-outline">
                                  <span
                                    class="pf-c-label__content"
                                  >app=3scale-gateway</span>
                                </span>
                              </li>
                              <li class="pf-c-label-group__list-item">
                                <span class="pf-c-label pf-m-outline">
                                  <span
                                    class="pf-c-label__content"
                                  >pod-template-has=6747686899</span>
                                </span>
                              </li>
                            </ul>
                          </div>
                        </div>
                      </div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span
                        class="pf-c-description-list__text"
                      >Active deadline seconds</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">Not configured</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Tolerations</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">stuff</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Pod IP</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">10..345.2.197</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Annotations</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">stuff</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Node</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">
                        <div class="pf-l-flex pf-m-space-items-sm">
                          <div class="pf-l-flex__item">
                            <span class="pf-c-label pf-m-purple">
                              <span class="pf-c-label__content">N</span>
                            </span>
                          </div>
                          <div
                            class="pf-l-flex__item"
                          >ip-10-0-233-118.us-east-2.computer.external</div>
                        </div>
                      </div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">
                      <span class="pf-c-description-list__text">Created at</span>
                    </dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">
                        <time>Oct 15, 1:51 pm</time>
                      </div>
                    </dd>
                  </div>
                </dl>
              </div>
            </div>
          </div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="open-tabs-example-tabs-list-yaml-link"
          id="open-tabs-example-tabs-list-yaml-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">YAML panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="open-tabs-example-tabs-list-environment-link"
          id="open-tabs-example-tabs-list-environment-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">Environment panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="open-tabs-example-tabs-list-logs-link"
          id="open-tabs-example-tabs-list-logs-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">Logs panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="open-tabs-example-tabs-list-events-link"
          id="open-tabs-example-tabs-list-events-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">Events panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="open-tabs-example-tabs-list-terminal-link"
          id="open-tabs-example-tabs-list-terminal-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">Terminal panel</div>
        </section>
      </div>
    </section>
  </main>
</div>

```

### Open tabs with secondary tabs

```html isFullscreen
<div class="pf-c-page" id="open-with-secondary-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-open-with-secondary-tabs-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="open-with-secondary-tabs-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="open-with-secondary-tabs-example-primary-nav"
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
              id="open-with-secondary-tabs-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="open-with-secondary-tabs-example-dropdown-kebab-1-button"
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
              id="open-with-secondary-tabs-example-dropdown-kebab-2-button"
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
              aria-labelledby="open-with-secondary-tabs-example-dropdown-kebab-2-button"
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
        id="open-with-secondary-tabs-example-primary-nav"
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
    id="main-content-open-with-secondary-tabs-example"
  >
    <section class="pf-c-page__main-breadcrumb pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <nav class="pf-c-breadcrumb" aria-label="breadcrumb">
          <ol class="pf-c-breadcrumb__list">
            <li class="pf-c-breadcrumb__item">
              <a href="#" class="pf-c-breadcrumb__link">Overview</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a href="#" class="pf-c-breadcrumb__link">Pods</a>
            </li>
            <li class="pf-c-breadcrumb__item">
              <span class="pf-c-breadcrumb__item-divider">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>

              <a
                href="#"
                class="pf-c-breadcrumb__link pf-m-current"
                aria-current="page"
              >Pod details</a>
            </li>
          </ol>
        </nav>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div
          class="pf-l-flex pf-m-space-items-md pf-m-align-items-flex-start pf-m-nowrap"
        >
          <div class="pf-l-flex__item">
            <span class="pf-c-label pf-m-blue">
              <span class="pf-c-label__content">N</span>
            </span>
          </div>
          <div class="pf-l-flex__item">
            <h1 class="pf-c-title pf-m-2xl">3scale-control-fccb6ddb9-phyqv9</h1>
          </div>
          <div class="pf-l-flex__item pf-m-flex-none">
            <span class="pf-c-label">
              <span class="pf-c-label__content">
                <span class="pf-c-label__icon">
                  <i class="fas fa-fw fa-info-circle" aria-hidden="true"></i>
                </span>
                Running
              </span>
            </span>
          </div>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-page-insets"
          id="open-with-secondary-tabs-example-tabs-list"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-with-secondary-tabs-example-tabs-list-details-panel"
                id="open-with-secondary-tabs-example-tabs-list-details-link"
              >
                <span class="pf-c-tabs__item-text">Details</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-with-secondary-tabs-example-tabs-list-yaml-panel"
                id="open-with-secondary-tabs-example-tabs-list-yaml-link"
              >
                <span class="pf-c-tabs__item-text">YAML</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-with-secondary-tabs-example-tabs-list-environment-panel"
                id="open-with-secondary-tabs-example-tabs-list-environment-link"
              >
                <span class="pf-c-tabs__item-text">Environment</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-with-secondary-tabs-example-tabs-list-logs-panel"
                id="open-with-secondary-tabs-example-tabs-list-logs-link"
              >
                <span class="pf-c-tabs__item-text">Logs</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-with-secondary-tabs-example-tabs-list-events-panel"
                id="open-with-secondary-tabs-example-tabs-list-events-link"
              >
                <span class="pf-c-tabs__item-text">Events</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="open-with-secondary-tabs-example-tabs-list-terminal-panel"
                id="open-with-secondary-tabs-example-tabs-list-terminal-link"
              >
                <span class="pf-c-tabs__item-text">Terminal</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-l-flex pf-m-column">
          <div class="pf-l-flex__item">
            <div
              class="pf-c-tabs pf-m-secondary pf-m-inset-none"
              id="open-with-secondary-tabs-example-tabs-list-secondary"
            >
              <ul class="pf-c-tabs__list">
                <li class="pf-c-tabs__item pf-m-current">
                  <button
                    class="pf-c-tabs__link"
                    aria-controls="open-with-secondary-tabs-example-tabs-list-secondary-pod-info-panel"
                    id="open-with-secondary-tabs-example-tabs-list-secondary-pod-info-link"
                  >
                    <span class="pf-c-tabs__item-text">Pod information</span>
                  </button>
                </li>
                <li class="pf-c-tabs__item">
                  <button
                    class="pf-c-tabs__link"
                    aria-controls="open-with-secondary-tabs-example-tabs-list-secondary-editable-aspects-panel"
                    id="open-with-secondary-tabs-example-tabs-list-secondary-editable-aspects-link"
                  >
                    <span class="pf-c-tabs__item-text">Editable Aspects</span>
                  </button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-l-flex__item">
            <section
              class="pf-c-tab-content"
              aria-labelledby="open-with-secondary-tabs-example-tabs-list-details-link"
              id="open-with-secondary-tabs-example-tabs-list-details-panel"
              role="tabpanel"
              tabindex="0"
            >
              <div class="pf-c-tab-content__body">
                <section
                  class="pf-c-tab-content"
                  aria-labelledby="open-with-secondary-tabs-example-tabs-list-secondary-pod-info-link"
                  id="open-with-secondary-tabs-example-tabs-list-secondary-pod-info-panel"
                  role="tabpanel"
                  tabindex="0"
                >
                  <div class="pf-c-tab-content__body">
                    <div class="pf-l-flex pf-m-column">
                      <div class="pf-l-flex__item">
                        <dl
                          class="pf-c-description-list pf-m-2-col-on-lg"
                          aria-label="Pod information list"
                        >
                          <div class="pf-c-description-list__group">
                            <dt class="pf-c-description-list__term">
                              <span class="pf-c-description-list__text">Name</span>
                            </dt>
                            <dd class="pf-c-description-list__description">
                              <div
                                class="pf-c-description-list__text"
                              >3scale-control-fccb6ddb9-phyqv9</div>
                            </dd>
                          </div>
                          <div class="pf-c-description-list__group">
                            <dt class="pf-c-description-list__term">
                              <span class="pf-c-description-list__text">Status</span>
                            </dt>
                            <dd class="pf-c-description-list__description">
                              <div class="pf-c-description-list__text">
                                <div class="pf-l-flex pf-m-space-items-sm">
                                  <div class="pf-l-flex__item">
                                    <i
                                      class="fas fa-fw fa-check-circle"
                                      aria-hidden="true"
                                    ></i>
                                  </div>
                                  <div class="pf-l-flex__item">Running</div>
                                </div>
                              </div>
                            </dd>
                          </div>
                          <div class="pf-c-description-list__group">
                            <dt class="pf-c-description-list__term">
                              <span
                                class="pf-c-description-list__text"
                              >Namespace</span>
                            </dt>
                            <dd class="pf-c-description-list__description">
                              <div class="pf-c-description-list__text">
                                <div class="pf-l-flex pf-m-space-items-sm">
                                  <div class="pf-l-flex__item">
                                    <span class="pf-c-label pf-m-cyan">
                                      <span class="pf-c-label__content">NS</span>
                                    </span>
                                  </div>
                                  <div class="pf-l-flex__item">
                                    <a href="#">knative-serving-ingress</a>
                                  </div>
                                </div>
                              </div>
                            </dd>
                          </div>
                          <div class="pf-c-description-list__group">
                            <dt class="pf-c-description-list__term">
                              <span
                                class="pf-c-description-list__text"
                              >Restart policy</span>
                            </dt>
                            <dd class="pf-c-description-list__description">
                              <div
                                class="pf-c-description-list__text"
                              >Always restart</div>
                            </dd>
                          </div>
                          <div class="pf-c-description-list__group">
                            <dt class="pf-c-description-list__term">
                              <span class="pf-c-description-list__text">Pod IP</span>
                            </dt>
                            <dd class="pf-c-description-list__description">
                              <div
                                class="pf-c-description-list__text"
                              >10..345.2.197</div>
                            </dd>
                          </div>
                          <div class="pf-c-description-list__group">
                            <dt class="pf-c-description-list__term">
                              <span
                                class="pf-c-description-list__text"
                              >Active deadline seconds</span>
                            </dt>
                            <dd class="pf-c-description-list__description">
                              <div
                                class="pf-c-description-list__text"
                              >Not configured</div>
                            </dd>
                          </div>
                          <div class="pf-c-description-list__group">
                            <dt class="pf-c-description-list__term">
                              <span
                                class="pf-c-description-list__text"
                              >Created at</span>
                            </dt>
                            <dd class="pf-c-description-list__description">
                              <div class="pf-c-description-list__text">
                                <time>Oct 15, 1:51 pm</time>
                              </div>
                            </dd>
                          </div>
                          <div class="pf-c-description-list__group">
                            <dt class="pf-c-description-list__term">
                              <span class="pf-c-description-list__text">Node</span>
                            </dt>
                            <dd class="pf-c-description-list__description">
                              <div class="pf-c-description-list__text">
                                <div class="pf-l-flex pf-m-space-items-sm">
                                  <div class="pf-l-flex__item">
                                    <span class="pf-c-label pf-m-purple">
                                      <span class="pf-c-label__content">N</span>
                                    </span>
                                  </div>
                                  <div
                                    class="pf-l-flex__item"
                                  >ip-10-0-233-118.us-east-2.computer.external</div>
                                </div>
                              </div>
                            </dd>
                          </div>
                        </dl>
                      </div>
                    </div>
                  </div>
                </section>
                <section
                  class="pf-c-tab-content"
                  aria-labelledby="open-with-secondary-tabs-example-tabs-list-secondary-editable-aspects-link"
                  id="open-with-secondary-tabs-example-tabs-list-secondary-editable-aspects-panel"
                  role="tabpanel"
                  tabindex="0"
                  hidden
                >
                  <div class="pf-c-tab-content__body">Editable aspects panel</div>
                </section>
              </div>
            </section>
            <section
              class="pf-c-tab-content"
              aria-labelledby="open-with-secondary-tabs-example-tabs-list-yaml-link"
              id="open-with-secondary-tabs-example-tabs-list-yaml-panel"
              role="tabpanel"
              tabindex="0"
              hidden
            >
              <div class="pf-c-tab-content__body">YAML panel</div>
            </section>
            <section
              class="pf-c-tab-content"
              aria-labelledby="open-with-secondary-tabs-example-tabs-list-environment-link"
              id="open-with-secondary-tabs-example-tabs-list-environment-panel"
              role="tabpanel"
              tabindex="0"
              hidden
            >
              <div class="pf-c-tab-content__body">Environment panel</div>
            </section>
            <section
              class="pf-c-tab-content"
              aria-labelledby="open-with-secondary-tabs-example-tabs-list-logs-link"
              id="open-with-secondary-tabs-example-tabs-list-logs-panel"
              role="tabpanel"
              tabindex="0"
              hidden
            >
              <div class="pf-c-tab-content__body">Logs panel</div>
            </section>
            <section
              class="pf-c-tab-content"
              aria-labelledby="open-with-secondary-tabs-example-tabs-list-events-link"
              id="open-with-secondary-tabs-example-tabs-list-events-panel"
              role="tabpanel"
              tabindex="0"
              hidden
            >
              <div class="pf-c-tab-content__body">Events panel</div>
            </section>
            <section
              class="pf-c-tab-content"
              aria-labelledby="open-with-secondary-tabs-example-tabs-list-terminal-link"
              id="open-with-secondary-tabs-example-tabs-list-terminal-panel"
              role="tabpanel"
              tabindex="0"
              hidden
            >
              <div class="pf-c-tab-content__body">Terminal panel</div>
            </section>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Nested tabs

```html isFullscreen
<div class="pf-c-page" id="nested-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-nested-tabs-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="nested-tabs-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="nested-tabs-example-primary-nav"
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
              id="nested-tabs-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="nested-tabs-example-dropdown-kebab-1-button"
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
              id="nested-tabs-example-dropdown-kebab-2-button"
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
              aria-labelledby="nested-tabs-example-dropdown-kebab-2-button"
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
        id="nested-tabs-example-primary-nav"
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
    id="main-content-nested-tabs-example"
  >
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <h1 class="pf-c-title pf-m-2xl pf-u-mt-md">Overview</h1>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div class="pf-c-tabs pf-m-page-insets" id="nested-tabs-example-tabs">
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="nested-tabs-example-tabs-cluster-1-panel"
                id="nested-tabs-example-tabs-cluster-1-link"
              >
                <span class="pf-c-tabs__item-text">Cluster 1</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="nested-tabs-example-tabs-cluster-2-panel"
                id="nested-tabs-example-tabs-cluster-2-link"
              >
                <span class="pf-c-tabs__item-text">Cluster 2</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <section
          class="pf-c-tab-content"
          aria-labelledby="nested-tabs-example-tabs-cluster-1-link"
          id="nested-tabs-example-tabs-cluster-1-panel"
          role="tabpanel"
          tabindex="0"
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-l-grid pf-m-gutter">
              <div class="pf-l-grid__item pf-m-6-col-on-md pf-m-8-col-on-xl">
                <div class="pf-c-card pf-m-full-height">
                  <div class="pf-c-card__header">
                    <h2 class="pf-c-title pf-m-lg pf-u-font-weight-light">Status</h2>
                  </div>
                  <div class="pf-c-card__body">
                    <div class="pf-l-flex pf-m-column">
                      <div class="pf-l-flex__item">
                        <div
                          class="pf-c-tabs pf-m-no-border-bottom"
                          id="nested-tabs-example-tabs-subtabs"
                        >
                          <ul class="pf-c-tabs__list">
                            <li class="pf-c-tabs__item pf-m-current">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="nested-tabs-example-tabs-subtabs-cluster-panel"
                                id="nested-tabs-example-tabs-subtabs-cluster-link"
                              >
                                <span class="pf-c-tabs__item-text">Cluster</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="nested-tabs-example-tabs-subtabs-control-plane-panel"
                                id="nested-tabs-example-tabs-subtabs-control-plane-link"
                              >
                                <span class="pf-c-tabs__item-text">Control plane</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="nested-tabs-example-tabs-subtabs-operators-panel"
                                id="nested-tabs-example-tabs-subtabs-operators-link"
                              >
                                <span class="pf-c-tabs__item-text">Operators</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="nested-tabs-example-tabs-subtabs-virtualization-panel"
                                id="nested-tabs-example-tabs-subtabs-virtualization-link"
                              >
                                <span
                                  class="pf-c-tabs__item-text"
                                >Virtualization</span>
                              </button>
                            </li>
                          </ul>
                        </div>
                      </div>
                      <div class="pf-l-flex__item">
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="nested-tabs-example-tabs-subtabs-cluster-link"
                          id="nested-tabs-example-tabs-subtabs-cluster-panel"
                          role="tabpanel"
                          tabindex="0"
                        >
                          <div class="pf-c-tab-content__body">
                            <div class="pf-c-content">
                              <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce in odio porttitor, feugiat risus in, feugiat arcu. Nullam euismod enim eget fringilla condimentum. Maecenas tincidunt et metus id aliquet. Integer et fermentum purus. Nulla tempor velit arcu, vitae semper purus iaculis at. Sed malesuada auctor luctus. Pellentesque et leo urna. Aliquam vitae felis congue lacus mattis fringilla. Nullam et ultricies erat, sed dignissim elit. Cras mattis pulvinar aliquam. In ac est nulla. Pellentesque fermentum nibh ac sapien porta, ut congue orci aliquam. Sed nisl est, tempor eu pharetra eget, ullamcorper ut augue. Vestibulum eleifend libero eu nulla cursus lacinia.</p>
                            </div>
                          </div>
                        </section>
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="nested-tabs-example-tabs-subtabs-control-plane-link"
                          id="nested-tabs-example-tabs-subtabs-control-plane-panel"
                          role="tabpanel"
                          tabindex="0"
                          hidden
                        >
                          <div
                            class="pf-c-tab-content__body"
                          >Control plane content</div>
                        </section>
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="nested-tabs-example-tabs-subtabs-operators-link"
                          id="nested-tabs-example-tabs-subtabs-operators-panel"
                          role="tabpanel"
                          tabindex="0"
                          hidden
                        >
                          <div class="pf-c-tab-content__body">Operators content</div>
                        </section>
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="nested-tabs-example-tabs-subtabs-virtualization-link"
                          id="nested-tabs-example-tabs-subtabs-virtualization-panel"
                          role="tabpanel"
                          tabindex="0"
                          hidden
                        >
                          <div
                            class="pf-c-tab-content__body"
                          >Virtualization content</div>
                        </section>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="pf-l-grid__item pf-m-6-col-on-md pf-m-4-col-on-xl">
                <div class="pf-l-flex pf-m-column pf-u-h-100">
                  <div class="pf-l-flex__item pf-m-flex-1">
                    <div class="pf-c-card pf-m-full-height">
                      <div class="pf-c-card__header">
                        <h3
                          class="pf-c-title pf-m-lg pf-u-font-weight-light"
                        >Title of card</h3>
                      </div>
                    </div>
                  </div>
                  <div class="pf-l-flex__item pf-m-flex-1">
                    <div class="pf-c-card pf-m-full-height">
                      <div class="pf-c-card__header">
                        <h3
                          class="pf-c-title pf-m-lg pf-u-font-weight-light"
                        >Title of card</h3>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="nested-tabs-example-tabs-cluster-2-link"
          id="nested-tabs-example-tabs-cluster-2-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-c-content">
              <p>Cluster 2 content</p>
            </div>
          </div>
        </section>
      </div>
    </section>
  </main>
</div>

```

### Tables and tabs

```html isFullscreen
<div class="pf-c-page" id="table-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-table-tabs-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="table-tabs-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="table-tabs-example-primary-nav"
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
              id="table-tabs-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="table-tabs-example-dropdown-kebab-1-button"
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
              id="table-tabs-example-dropdown-kebab-2-button"
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
              aria-labelledby="table-tabs-example-dropdown-kebab-2-button"
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
        id="table-tabs-example-primary-nav"
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
    id="main-content-table-tabs-example"
  >
    <section class="pf-c-page__main-section pf-m-light">
      <h1 class="pf-c-title pf-m-2xl pf-u-mt-md">Nodes</h1>
    </section>
    <section class="pf-c-page__main-tabs">
      <div class="pf-c-tabs pf-m-page-insets" id="table-tabs-example-tabs">
        <ul class="pf-c-tabs__list">
          <li class="pf-c-tabs__item pf-m-current">
            <button
              class="pf-c-tabs__link"
              aria-controls="table-tabs-example-tabs-nodes-panel"
              id="table-tabs-example-tabs-nodes-link"
            >
              <span class="pf-c-tabs__item-text">Nodes</span>
            </button>
          </li>
          <li class="pf-c-tabs__item">
            <button
              class="pf-c-tabs__link"
              aria-controls="table-tabs-example-tabs-node-connectors-panel"
              id="table-tabs-example-tabs-node-connectors-link"
            >
              <span class="pf-c-tabs__item-text">Node connectors</span>
            </button>
          </li>
        </ul>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-no-padding pf-m-light">
      <section
        class="pf-c-tab-content"
        aria-labelledby="table-tabs-example-tabs-nodes-link"
        id="table-tabs-example-tabs-nodes-panel"
        role="tabpanel"
        tabindex="0"
      >
        <div class="pf-c-tab-content__body">
          <div
            class="pf-c-drawer pf-m-expanded pf-m-inline-on-2xlsss pf-m-inline"
          >
            <div class="pf-c-drawer__main">
              <!-- Content -->
              <div class="pf-c-drawer__content">
                <div class="pf-c-drawer__body">
                  <div
                    class="pf-c-toolbar pf-m-page-insets"
                    id="table-tabs-example-toolbar"
                  >
                    <div class="pf-c-toolbar__content">
                      <div class="pf-c-toolbar__content-section pf-m-nowrap">
                        <div
                          class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-xl"
                        >
                          <div class="pf-c-toolbar__toggle">
                            <button
                              class="pf-c-button pf-m-plain"
                              type="button"
                              aria-label="Show filters"
                              aria-expanded="false"
                              aria-controls="table-tabs-example-toolbar-expandable-content"
                            >
                              <i class="fas fa-filter" aria-hidden="true"></i>
                            </button>
                          </div>

                          <div class="pf-c-toolbar__item">
                            <div class="pf-c-select">
                              <span
                                id="table-tabs-example-toolbar-select-checkbox-status-label"
                                hidden
                              >Choose one</span>

                              <button
                                class="pf-c-select__toggle"
                                type="button"
                                id="table-tabs-example-toolbar-select-checkbox-status-toggle"
                                aria-haspopup="true"
                                aria-expanded="false"
                                aria-labelledby="table-tabs-example-toolbar-select-checkbox-status-label table-tabs-example-toolbar-select-checkbox-status-toggle"
                              >
                                <div class="pf-c-select__toggle-wrapper">
                                  <span class="pf-c-select__toggle-text">Name</span>
                                </div>
                                <span class="pf-c-select__toggle-arrow">
                                  <i
                                    class="fas fa-caret-down"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                              </button>

                              <div class="pf-c-select__menu" hidden>
                                <fieldset
                                  class="pf-c-select__menu-fieldset"
                                  aria-label="Select input"
                                >
                                  <label
                                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                                    for="table-tabs-example-toolbar-select-checkbox-status-active"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-toolbar-select-checkbox-status-active"
                                      name="table-tabs-example-toolbar-select-checkbox-status-active"
                                    />

                                    <span class="pf-c-check__label">Active</span>
                                    <span
                                      class="pf-c-check__description"
                                    >This is a description</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                                    for="table-tabs-example-toolbar-select-checkbox-status-canceled"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-toolbar-select-checkbox-status-canceled"
                                      name="table-tabs-example-toolbar-select-checkbox-status-canceled"
                                    />

                                    <span class="pf-c-check__label">Canceled</span>
                                    <span
                                      class="pf-c-check__description"
                                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-toolbar-select-checkbox-status-paused"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-toolbar-select-checkbox-status-paused"
                                      name="table-tabs-example-toolbar-select-checkbox-status-paused"
                                    />

                                    <span class="pf-c-check__label">Paused</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-toolbar-select-checkbox-status-warning"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-toolbar-select-checkbox-status-warning"
                                      name="table-tabs-example-toolbar-select-checkbox-status-warning"
                                    />

                                    <span class="pf-c-check__label">Warning</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-toolbar-select-checkbox-status-restarted"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-toolbar-select-checkbox-status-restarted"
                                      name="table-tabs-example-toolbar-select-checkbox-status-restarted"
                                    />

                                    <span class="pf-c-check__label">Restarted</span>
                                  </label>
                                </fieldset>
                              </div>
                            </div>
                          </div>
                        </div>

                        <div class="pf-c-toolbar__item">
                          <button
                            class="pf-c-button pf-m-plain"
                            type="button"
                            aria-label="Sort"
                          >
                            <i
                              class="fas fa-sort-amount-down"
                              aria-hidden="true"
                            ></i>
                          </button>
                        </div>

                        <div
                          class="pf-c-overflow-menu"
                          id="table-tabs-example-toolbar-overflow-menu"
                        >
                          <div
                            class="pf-c-overflow-menu__content pf-u-display-none pf-u-display-flex-on-lg"
                          >
                            <div
                              class="pf-c-overflow-menu__group pf-m-button-group"
                            >
                              <div class="pf-c-overflow-menu__item">
                                <button
                                  class="pf-c-button pf-m-primary"
                                  type="button"
                                >Generate</button>
                              </div>

                              <div class="pf-c-overflow-menu__item">
                                <button
                                  class="pf-c-button pf-m-secondary"
                                  type="button"
                                >Deploy</button>
                              </div>
                            </div>
                          </div>
                          <div class="pf-c-overflow-menu__control">
                            <div class="pf-c-dropdown">
                              <button
                                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                type="button"
                                id="table-tabs-example-toolbar-overflow-menu-dropdown-toggle"
                                aria-label="Dropdown with additional options"
                                aria-expanded="false"
                              >
                                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                              </button>
                              <ul
                                class="pf-c-dropdown__menu"
                                aria-labelledby="table-tabs-example-toolbar-overflow-menu-dropdown-toggle"
                                hidden
                              >
                                <li>
                                  <button
                                    class="pf-c-dropdown__menu-item"
                                  >Action 7</button>
                                </li>
                              </ul>
                            </div>
                          </div>
                        </div>

                        <div class="pf-c-toolbar__item pf-m-pagination">
                          <div class="pf-c-pagination pf-m-compact">
                            <div class="pf-c-options-menu">
                              <div
                                class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                              >
                                <span class="pf-c-options-menu__toggle-text">
                                  <b>1 - 10</b>&nbsp;of&nbsp;
                                  <b>36</b>
                                </span>
                                <button
                                  class="pf-c-options-menu__toggle-button"
                                  id="table-tabs-example-toolbar-top-pagination-toggle"
                                  aria-haspopup="listbox"
                                  aria-expanded="false"
                                  aria-label="Items per page"
                                >
                                  <span
                                    class="pf-c-options-menu__toggle-button-icon"
                                  >
                                    <i
                                      class="fas fa-caret-down"
                                      aria-hidden="true"
                                    ></i>
                                  </span>
                                </button>
                              </div>
                              <ul
                                class="pf-c-options-menu__menu"
                                aria-labelledby="table-tabs-example-toolbar-top-pagination-toggle"
                                hidden
                              >
                                <li>
                                  <button
                                    class="pf-c-options-menu__menu-item"
                                    type="button"
                                  >5 per page</button>
                                </li>
                                <li>
                                  <button
                                    class="pf-c-options-menu__menu-item"
                                    type="button"
                                  >
                                    10 per page
                                    <div
                                      class="pf-c-options-menu__menu-item-icon"
                                    >
                                      <i
                                        class="fas fa-check"
                                        aria-hidden="true"
                                      ></i>
                                    </div>
                                  </button>
                                </li>
                                <li>
                                  <button
                                    class="pf-c-options-menu__menu-item"
                                    type="button"
                                  >20 per page</button>
                                </li>
                              </ul>
                            </div>
                            <nav
                              class="pf-c-pagination__nav"
                              aria-label="Toolbar top pagination"
                            >
                              <div
                                class="pf-c-pagination__nav-control pf-m-prev"
                              >
                                <button
                                  class="pf-c-button pf-m-plain"
                                  type="button"
                                  disabled
                                  aria-label="Go to previous page"
                                >
                                  <i
                                    class="fas fa-angle-left"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                              </div>
                              <div
                                class="pf-c-pagination__nav-control pf-m-next"
                              >
                                <button
                                  class="pf-c-button pf-m-plain"
                                  type="button"
                                  aria-label="Go to next page"
                                >
                                  <i
                                    class="fas fa-angle-right"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                              </div>
                            </nav>
                          </div>
                        </div>
                      </div>

                      <div
                        class="pf-c-toolbar__expandable-content pf-m-hidden"
                        id="table-tabs-example-toolbar-expandable-content"
                        hidden
                      ></div>
                    </div>
                  </div>
                  <hr class="pf-c-divider" />
                  <table
                    class="pf-c-table pf-m-grid-md"
                    role="grid"
                    aria-label="This is a table with checkboxes"
                    id="table-tabs-example-table"
                  >
                    <thead>
                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="check-all"
                            aria-label="Select all rows"
                          />
                        </td>
                        <th role="columnheader" scope="col">Repositories</th>
                        <th role="columnheader" scope="col">Branches</th>
                        <th role="columnheader" scope="col">Pull requests</th>
                        <th role="columnheader" scope="col">Workspaces</th>
                        <th role="columnheader" scope="col">Last commit</th>
                        <td></td>
                      </tr>
                    </thead>

                    <tbody role="rowgroup">
                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow1"
                            aria-labelledby="table-tabs-example-table-node1"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node1">Node 1</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">10</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">25</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">5</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-1"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown pf-m-expanded">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-1-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="true"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-1-dropdown-toggle"
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr class="pf-m-selected" role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow2"
                            aria-labelledby="table-tabs-example-table-node2"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node2">Node 2</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">8</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">30</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">2</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-2"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-2-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-2-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow3"
                            aria-labelledby="table-tabs-example-table-node3"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node3">Node 3</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">12</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">48</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">13</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >30 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-3"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-3-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-3-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow4"
                            aria-labelledby="table-tabs-example-table-node4"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node4">Node 4</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">3</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">8</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">20</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >8 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-4"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-4-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-4-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow5"
                            aria-labelledby="table-tabs-example-table-node5"
                          />
                        </td>
                        <td role="cell" data-label="Repository name">
                          <div>
                            <div id="table-tabs-example-table-node5">Node 5</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </td>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-5"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-5-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-5-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow6"
                            aria-labelledby="table-tabs-example-table-node6"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node6">Node 6</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-6"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-6-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-6-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow7"
                            aria-labelledby="table-tabs-example-table-node7"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node7">Node 7</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-7"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-7-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-7-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow8"
                            aria-labelledby="table-tabs-example-table-node8"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node8">Node 8</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-8"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-8-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-8-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow9"
                            aria-labelledby="table-tabs-example-table-node9"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node9">Node 9</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-9"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-9-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-9-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow10"
                            aria-labelledby="table-tabs-example-table-node10"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div id="table-tabs-example-table-node10">Node 10</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-example-table-dropdown-kebab-10"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-example-table-dropdown-kebab-10-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-example-table-dropdown-kebab-10-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <div class="pf-c-pagination pf-m-bottom">
                    <div class="pf-c-options-menu pf-m-top">
                      <div
                        class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                      >
                        <span class="pf-c-options-menu__toggle-text">
                          <b>1 - 10</b>&nbsp;of&nbsp;
                          <b>36</b>
                        </span>
                        <button
                          class="pf-c-options-menu__toggle-button"
                          id="table-tabs-example-footer-pagination-toggle"
                          aria-haspopup="listbox"
                          aria-expanded="false"
                          aria-label="Items per page"
                        >
                          <span class="pf-c-options-menu__toggle-button-icon">
                            <i class="fas fa-caret-down" aria-hidden="true"></i>
                          </span>
                        </button>
                      </div>
                      <ul
                        class="pf-c-options-menu__menu pf-m-top"
                        aria-labelledby="table-tabs-example-footer-pagination-toggle"
                        hidden
                      >
                        <li>
                          <button
                            class="pf-c-options-menu__menu-item"
                            type="button"
                          >5 per page</button>
                        </li>
                        <li>
                          <button
                            class="pf-c-options-menu__menu-item"
                            type="button"
                          >
                            10 per page
                            <div class="pf-c-options-menu__menu-item-icon">
                              <i class="fas fa-check" aria-hidden="true"></i>
                            </div>
                          </button>
                        </li>
                        <li>
                          <button
                            class="pf-c-options-menu__menu-item"
                            type="button"
                          >20 per page</button>
                        </li>
                      </ul>
                    </div>
                    <nav class="pf-c-pagination__nav" aria-label="Pagination">
                      <div class="pf-c-pagination__nav-control pf-m-first">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          disabled
                          aria-label="Go to first page"
                        >
                          <i
                            class="fas fa-angle-double-left"
                            aria-hidden="true"
                          ></i>
                        </button>
                      </div>
                      <div class="pf-c-pagination__nav-control pf-m-prev">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          disabled
                          aria-label="Go to previous page"
                        >
                          <i class="fas fa-angle-left" aria-hidden="true"></i>
                        </button>
                      </div>
                      <div class="pf-c-pagination__nav-page-select">
                        <input
                          class="pf-c-form-control"
                          aria-label="Current page"
                          type="number"
                          min="1"
                          max="4"
                          value="1"
                        />
                        <span aria-hidden="true">of 4</span>
                      </div>
                      <div class="pf-c-pagination__nav-control pf-m-next">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          aria-label="Go to next page"
                        >
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </button>
                      </div>
                      <div class="pf-c-pagination__nav-control pf-m-last">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          aria-label="Go to last page"
                        >
                          <i
                            class="fas fa-angle-double-right"
                            aria-hidden="true"
                          ></i>
                        </button>
                      </div>
                    </nav>
                  </div>
                </div>
              </div>

              <!-- Panel -->
              <div class="pf-c-drawer__panel pf-m-width-33 pf-m-width-33-on-xl">
                <div class="pf-c-drawer__body">
                  <div class="pf-c-drawer__head">
                    <div class="pf-c-drawer__actions">
                      <div class="pf-c-drawer__close">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          aria-label="Close drawer panel"
                        >
                          <i class="fas fa-times" aria-hidden="true"></i>
                        </button>
                      </div>
                    </div>
                    <div class="pf-l-flex pf-m-column pf-m-space-items-sm">
                      <div class="pf-l-flex__item">
                        <h2
                          class="pf-c-title pf-m-lg"
                          id="table-tabs-example-tabs-drawer-label"
                        >Node 2</h2>
                      </div>
                      <div class="pf-l-flex__item">
                        <a href="#">siemur/test-space</a>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="pf-c-drawer__body pf-m-no-padding">
                  <div
                    class="pf-c-tabs pf-m-box pf-m-fill"
                    id="table-tabs-example-tabs-tabs"
                  >
                    <button
                      class="pf-c-tabs__scroll-button"
                      aria-label="Scroll left"
                    >
                      <i class="fas fa-angle-left" aria-hidden="true"></i>
                    </button>
                    <ul class="pf-c-tabs__list">
                      <li class="pf-c-tabs__item pf-m-current">
                        <button
                          class="pf-c-tabs__link"
                          aria-controls="table-tabs-example-tabs-tabs-tab1-panel"
                          id="table-tabs-example-tabs-tabs-tab1-link"
                        >
                          <span class="pf-c-tabs__item-text">Overview</span>
                        </button>
                      </li>
                      <li class="pf-c-tabs__item">
                        <button
                          class="pf-c-tabs__link"
                          aria-controls="table-tabs-example-tabs-tabs-tab2-panel"
                          id="table-tabs-example-tabs-tabs-tab2-link"
                        >
                          <span class="pf-c-tabs__item-text">Activity</span>
                        </button>
                      </li>
                    </ul>
                    <button
                      class="pf-c-tabs__scroll-button"
                      aria-label="Scroll right"
                    >
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </button>
                  </div>
                </div>
                <div class="pf-c-drawer__body">
                  <section
                    class="pf-c-tab-content"
                    id="table-tabs-example-tabs-tabs-tab1-panel"
                    aria-labelledby="table-tabs-example-tabs-tabs-tab1-link"
                    role="tabpanel"
                    tabindex="0"
                  >
                    <div class="pf-c-tab-content__body">
                      <div class="pf-l-flex pf-m-column pf-m-space-items-lg">
                        <div class="pf-l-flex__item">
                          <p>The content of the drawer really is up to you. It could have form fields, definition lists, text lists, labels, charts, progress bars, etc. Spacing recommendation is 24px margins. You can put tabs in here, and can also make the drawer scrollable.</p>
                        </div>
                        <div class="pf-l-flex__item">
                          <div
                            class="pf-c-progress pf-m-sm"
                            id="table-tabs-example-tabs-progress-example1"
                          >
                            <div
                              class="pf-c-progress__description"
                              id="table-tabs-example-tabs-progress-example1-description"
                            >Capacity</div>
                            <div
                              class="pf-c-progress__status"
                              aria-hidden="true"
                            >
                              <span class="pf-c-progress__measure">33%</span>
                            </div>
                            <div
                              class="pf-c-progress__bar"
                              role="progressbar"
                              aria-valuemin="0"
                              aria-valuemax="100"
                              aria-valuenow="33"
                              aria-labelledby="table-tabs-example-tabs-progress-example1-description"
                              aria-label="Progress 1"
                            >
                              <div
                                class="pf-c-progress__indicator"
                                style="width:33%;"
                              ></div>
                            </div>
                          </div>
                        </div>
                        <div class="pf-l-flex__item">
                          <div
                            class="pf-c-progress pf-m-sm"
                            id="table-tabs-example-tabs-progress-example2"
                          >
                            <div
                              class="pf-c-progress__description"
                              id="table-tabs-example-tabs-progress-example2-description"
                            >Modules</div>
                            <div
                              class="pf-c-progress__status"
                              aria-hidden="true"
                            >
                              <span class="pf-c-progress__measure">66%</span>
                            </div>
                            <div
                              class="pf-c-progress__bar"
                              role="progressbar"
                              aria-valuemin="0"
                              aria-valuemax="100"
                              aria-valuenow="66"
                              aria-labelledby="table-tabs-example-tabs-progress-example2-description"
                              aria-label="Progress 2"
                            >
                              <div
                                class="pf-c-progress__indicator"
                                style="width:66%;"
                              ></div>
                            </div>
                          </div>
                        </div>
                        <div class="pf-l-flex pf-m-column">
                          <div class="pf-l-flex__item">
                            <h3
                              class="pf-c-title"
                              id="table-tabs-example-tabs-title"
                            >Tags</h3>
                          </div>
                          <div class="pf-l-flex__item">
                            <div class="pf-c-label-group">
                              <div class="pf-c-label-group__main">
                                <ul
                                  class="pf-c-label-group__list"
                                  role="list"
                                  aria-label="Group of labels"
                                >
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span class="pf-c-label__content">Tag 1</span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span
                                        class="pf-c-label__content"
                                      >Different tag 1</span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span
                                        class="pf-c-label__content"
                                      >Another different tag 1</span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span
                                        class="pf-c-label__content"
                                      >Another different tag 2</span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span
                                        class="pf-c-label__content"
                                      >Different than tag 1 or 2</span>
                                    </span>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </section>
                  <section
                    class="pf-c-tab-content"
                    id="table-tabs-example-tabs-tabs-tab2-panel"
                    aria-labelledby="table-tabs-example-tabs-tabs-tab2-link"
                    role="tabpanel"
                    tabindex="0"
                    hidden
                  >
                    <div class="pf-c-tab-content__body">Panel 2</div>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
      <section
        class="pf-c-tab-content"
        aria-labelledby="table-tabs-example-tabs-node-connectors-link"
        id="table-tabs-example-tabs-node-connectors-panel"
        role="tabpanel"
        tabindex="0"
        hidden
      >
        <div class="pf-c-tab-content__body">
          <div class="pf-c-content">
            <p>Node connectors content</p>
          </div>
        </div>
      </section>
    </section>
  </main>
</div>

```

### Tables and tabs, auto width tabs

```html isFullscreen
<div class="pf-c-page" id="table-tabs-w-secondary-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-table-tabs-w-secondary-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="table-tabs-w-secondary-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="table-tabs-w-secondary-example-primary-nav"
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
              id="table-tabs-w-secondary-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="table-tabs-w-secondary-example-dropdown-kebab-1-button"
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
              id="table-tabs-w-secondary-example-dropdown-kebab-2-button"
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
              aria-labelledby="table-tabs-w-secondary-example-dropdown-kebab-2-button"
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
        id="table-tabs-w-secondary-example-primary-nav"
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
    id="main-content-table-tabs-w-secondary-example"
  >
    <section class="pf-c-page__main-section pf-m-light">
      <h1 class="pf-c-title pf-m-2xl pf-u-mt-md">Nodes</h1>
    </section>
    <section class="pf-c-page__main-tabs">
      <div
        class="pf-c-tabs pf-m-page-insets"
        id="table-tabs-w-secondary-example-tabs"
      >
        <ul class="pf-c-tabs__list">
          <li class="pf-c-tabs__item pf-m-current">
            <button
              class="pf-c-tabs__link"
              aria-controls="table-tabs-w-secondary-example-tabs-nodes-panel"
              id="table-tabs-w-secondary-example-tabs-nodes-link"
            >
              <span class="pf-c-tabs__item-text">Nodes</span>
            </button>
          </li>
          <li class="pf-c-tabs__item">
            <button
              class="pf-c-tabs__link"
              aria-controls="table-tabs-w-secondary-example-tabs-node-connectors-panel"
              id="table-tabs-w-secondary-example-tabs-node-connectors-link"
            >
              <span class="pf-c-tabs__item-text">Node connectors</span>
            </button>
          </li>
        </ul>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-no-padding pf-m-light">
      <section
        class="pf-c-tab-content"
        aria-labelledby="table-tabs-w-secondary-example-tabs-nodes-link"
        id="table-tabs-w-secondary-example-tabs-nodes-panel"
        role="tabpanel"
        tabindex="0"
      >
        <div class="pf-c-tab-content__body">
          <div
            class="pf-c-drawer pf-m-expanded pf-m-inline-on-2xlsss pf-m-inline"
          >
            <div class="pf-c-drawer__main">
              <!-- Content -->
              <div class="pf-c-drawer__content">
                <div class="pf-c-drawer__body">
                  <div
                    class="pf-c-toolbar pf-m-page-insets"
                    id="table-tabs-w-secondary-example-toolbar"
                  >
                    <div class="pf-c-toolbar__content">
                      <div class="pf-c-toolbar__content-section pf-m-nowrap">
                        <div
                          class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-xl"
                        >
                          <div class="pf-c-toolbar__toggle">
                            <button
                              class="pf-c-button pf-m-plain"
                              type="button"
                              aria-label="Show filters"
                              aria-expanded="false"
                              aria-controls="table-tabs-w-secondary-example-toolbar-expandable-content"
                            >
                              <i class="fas fa-filter" aria-hidden="true"></i>
                            </button>
                          </div>

                          <div class="pf-c-toolbar__item">
                            <div class="pf-c-select">
                              <span
                                id="table-tabs-w-secondary-example-toolbar-select-checkbox-status-label"
                                hidden
                              >Choose one</span>

                              <button
                                class="pf-c-select__toggle"
                                type="button"
                                id="table-tabs-w-secondary-example-toolbar-select-checkbox-status-toggle"
                                aria-haspopup="true"
                                aria-expanded="false"
                                aria-labelledby="table-tabs-w-secondary-example-toolbar-select-checkbox-status-label table-tabs-w-secondary-example-toolbar-select-checkbox-status-toggle"
                              >
                                <div class="pf-c-select__toggle-wrapper">
                                  <span class="pf-c-select__toggle-text">Name</span>
                                </div>
                                <span class="pf-c-select__toggle-arrow">
                                  <i
                                    class="fas fa-caret-down"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                              </button>

                              <div class="pf-c-select__menu" hidden>
                                <fieldset
                                  class="pf-c-select__menu-fieldset"
                                  aria-label="Select input"
                                >
                                  <label
                                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                                    for="table-tabs-w-secondary-example-toolbar-select-checkbox-status-active"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-w-secondary-example-toolbar-select-checkbox-status-active"
                                      name="table-tabs-w-secondary-example-toolbar-select-checkbox-status-active"
                                    />

                                    <span class="pf-c-check__label">Active</span>
                                    <span
                                      class="pf-c-check__description"
                                    >This is a description</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                                    for="table-tabs-w-secondary-example-toolbar-select-checkbox-status-canceled"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-w-secondary-example-toolbar-select-checkbox-status-canceled"
                                      name="table-tabs-w-secondary-example-toolbar-select-checkbox-status-canceled"
                                    />

                                    <span class="pf-c-check__label">Canceled</span>
                                    <span
                                      class="pf-c-check__description"
                                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-w-secondary-example-toolbar-select-checkbox-status-paused"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-w-secondary-example-toolbar-select-checkbox-status-paused"
                                      name="table-tabs-w-secondary-example-toolbar-select-checkbox-status-paused"
                                    />

                                    <span class="pf-c-check__label">Paused</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-w-secondary-example-toolbar-select-checkbox-status-warning"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-w-secondary-example-toolbar-select-checkbox-status-warning"
                                      name="table-tabs-w-secondary-example-toolbar-select-checkbox-status-warning"
                                    />

                                    <span class="pf-c-check__label">Warning</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-w-secondary-example-toolbar-select-checkbox-status-restarted"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-w-secondary-example-toolbar-select-checkbox-status-restarted"
                                      name="table-tabs-w-secondary-example-toolbar-select-checkbox-status-restarted"
                                    />

                                    <span class="pf-c-check__label">Restarted</span>
                                  </label>
                                </fieldset>
                              </div>
                            </div>
                          </div>
                        </div>

                        <div class="pf-c-toolbar__item">
                          <button
                            class="pf-c-button pf-m-plain"
                            type="button"
                            aria-label="Sort"
                          >
                            <i
                              class="fas fa-sort-amount-down"
                              aria-hidden="true"
                            ></i>
                          </button>
                        </div>

                        <div
                          class="pf-c-overflow-menu"
                          id="table-tabs-w-secondary-example-toolbar-overflow-menu"
                        >
                          <div
                            class="pf-c-overflow-menu__content pf-u-display-none pf-u-display-flex-on-lg"
                          >
                            <div
                              class="pf-c-overflow-menu__group pf-m-button-group"
                            >
                              <div class="pf-c-overflow-menu__item">
                                <button
                                  class="pf-c-button pf-m-primary"
                                  type="button"
                                >Generate</button>
                              </div>

                              <div class="pf-c-overflow-menu__item">
                                <button
                                  class="pf-c-button pf-m-secondary"
                                  type="button"
                                >Deploy</button>
                              </div>
                            </div>
                          </div>
                          <div class="pf-c-overflow-menu__control">
                            <div class="pf-c-dropdown">
                              <button
                                class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                type="button"
                                id="table-tabs-w-secondary-example-toolbar-overflow-menu-dropdown-toggle"
                                aria-label="Dropdown with additional options"
                                aria-expanded="false"
                              >
                                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                              </button>
                              <ul
                                class="pf-c-dropdown__menu"
                                aria-labelledby="table-tabs-w-secondary-example-toolbar-overflow-menu-dropdown-toggle"
                                hidden
                              >
                                <li>
                                  <button
                                    class="pf-c-dropdown__menu-item"
                                  >Action 7</button>
                                </li>
                              </ul>
                            </div>
                          </div>
                        </div>

                        <div class="pf-c-toolbar__item pf-m-pagination">
                          <div class="pf-c-pagination pf-m-compact">
                            <div class="pf-c-options-menu">
                              <div
                                class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                              >
                                <span class="pf-c-options-menu__toggle-text">
                                  <b>1 - 10</b>&nbsp;of&nbsp;
                                  <b>36</b>
                                </span>
                                <button
                                  class="pf-c-options-menu__toggle-button"
                                  id="table-tabs-w-secondary-example-toolbar-top-pagination-toggle"
                                  aria-haspopup="listbox"
                                  aria-expanded="false"
                                  aria-label="Items per page"
                                >
                                  <span
                                    class="pf-c-options-menu__toggle-button-icon"
                                  >
                                    <i
                                      class="fas fa-caret-down"
                                      aria-hidden="true"
                                    ></i>
                                  </span>
                                </button>
                              </div>
                              <ul
                                class="pf-c-options-menu__menu"
                                aria-labelledby="table-tabs-w-secondary-example-toolbar-top-pagination-toggle"
                                hidden
                              >
                                <li>
                                  <button
                                    class="pf-c-options-menu__menu-item"
                                    type="button"
                                  >5 per page</button>
                                </li>
                                <li>
                                  <button
                                    class="pf-c-options-menu__menu-item"
                                    type="button"
                                  >
                                    10 per page
                                    <div
                                      class="pf-c-options-menu__menu-item-icon"
                                    >
                                      <i
                                        class="fas fa-check"
                                        aria-hidden="true"
                                      ></i>
                                    </div>
                                  </button>
                                </li>
                                <li>
                                  <button
                                    class="pf-c-options-menu__menu-item"
                                    type="button"
                                  >20 per page</button>
                                </li>
                              </ul>
                            </div>
                            <nav
                              class="pf-c-pagination__nav"
                              aria-label="Toolbar top pagination"
                            >
                              <div
                                class="pf-c-pagination__nav-control pf-m-prev"
                              >
                                <button
                                  class="pf-c-button pf-m-plain"
                                  type="button"
                                  disabled
                                  aria-label="Go to previous page"
                                >
                                  <i
                                    class="fas fa-angle-left"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                              </div>
                              <div
                                class="pf-c-pagination__nav-control pf-m-next"
                              >
                                <button
                                  class="pf-c-button pf-m-plain"
                                  type="button"
                                  aria-label="Go to next page"
                                >
                                  <i
                                    class="fas fa-angle-right"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                              </div>
                            </nav>
                          </div>
                        </div>
                      </div>

                      <div
                        class="pf-c-toolbar__expandable-content pf-m-hidden"
                        id="table-tabs-w-secondary-example-toolbar-expandable-content"
                        hidden
                      ></div>
                    </div>
                  </div>
                  <hr class="pf-c-divider" />
                  <table
                    class="pf-c-table pf-m-grid-md"
                    role="grid"
                    aria-label="This is a table with checkboxes"
                    id="table-tabs-w-secondary-example-table"
                  >
                    <thead>
                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="check-all"
                            aria-label="Select all rows"
                          />
                        </td>
                        <th role="columnheader" scope="col">Repositories</th>
                        <th role="columnheader" scope="col">Branches</th>
                        <th role="columnheader" scope="col">Pull requests</th>
                        <th role="columnheader" scope="col">Workspaces</th>
                        <th role="columnheader" scope="col">Last commit</th>
                        <td></td>
                      </tr>
                    </thead>

                    <tbody role="rowgroup">
                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow1"
                            aria-labelledby="table-tabs-w-secondary-example-table-node1"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node1"
                            >Node 1</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">10</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">25</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">5</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-1"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown pf-m-expanded">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-1-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="true"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-1-dropdown-toggle"
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr class="pf-m-selected" role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow2"
                            aria-labelledby="table-tabs-w-secondary-example-table-node2"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node2"
                            >Node 2</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">8</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">30</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">2</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-2"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-2-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-2-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow3"
                            aria-labelledby="table-tabs-w-secondary-example-table-node3"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node3"
                            >Node 3</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">12</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">48</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">13</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >30 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-3"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-3-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-3-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow4"
                            aria-labelledby="table-tabs-w-secondary-example-table-node4"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node4"
                            >Node 4</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">3</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">8</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">20</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >8 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-4"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-4-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-4-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow5"
                            aria-labelledby="table-tabs-w-secondary-example-table-node5"
                          />
                        </td>
                        <td role="cell" data-label="Repository name">
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node5"
                            >Node 5</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </td>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-5"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-5-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-5-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow6"
                            aria-labelledby="table-tabs-w-secondary-example-table-node6"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node6"
                            >Node 6</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-6"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-6-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-6-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow7"
                            aria-labelledby="table-tabs-w-secondary-example-table-node7"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node7"
                            >Node 7</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-7"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-7-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-7-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow8"
                            aria-labelledby="table-tabs-w-secondary-example-table-node8"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node8"
                            >Node 8</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-8"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-8-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-8-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow9"
                            aria-labelledby="table-tabs-w-secondary-example-table-node9"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node9"
                            >Node 9</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-9"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-9-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-9-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>

                      <tr role="row">
                        <td class="pf-c-table__check" role="cell">
                          <input
                            type="checkbox"
                            name="checkrow10"
                            aria-labelledby="table-tabs-w-secondary-example-table-node10"
                          />
                        </td>
                        <th
                          class
                          role="columnheader"
                          data-label="Repository name"
                        >
                          <div>
                            <div
                              id="table-tabs-w-secondary-example-table-node10"
                            >Node 10</div>
                            <a href="#">siemur/test-space</a>
                          </div>
                        </th>
                        <td role="cell" data-label="Branches">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">34</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Pull requests">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">21</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code"></i>
                            </div>
                          </div>
                        </td>
                        <td role="cell" data-label="Workspaces">
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-nowrap"
                          >
                            <div class="pf-l-flex__item">26</div>
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube"></i>
                            </div>
                          </div>
                        </td>
                        <td
                          class
                          role="cell"
                          data-label="Last commit"
                        >2 days ago</td>
                        <td class="pf-c-table__action" role="cell">
                          <div
                            class="pf-c-overflow-menu"
                            id="table-tabs-w-secondary-example-table-dropdown-kebab-10"
                          >
                            <div class="pf-c-overflow-menu__control">
                              <div class="pf-c-dropdown">
                                <button
                                  class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                                  type="button"
                                  id="table-tabs-w-secondary-example-table-dropdown-kebab-10-dropdown-toggle"
                                  aria-label="Dropdown for tabs table"
                                  aria-expanded="false"
                                >
                                  <i
                                    class="fas fa-ellipsis-v"
                                    aria-hidden="true"
                                  ></i>
                                </button>
                                <ul
                                  class="pf-c-dropdown__menu pf-m-align-right"
                                  aria-labelledby="table-tabs-w-secondary-example-table-dropdown-kebab-10-dropdown-toggle"
                                  hidden
                                >
                                  <li>
                                    <button
                                      class="pf-c-dropdown__menu-item"
                                    >Action Link</button>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <div class="pf-c-pagination pf-m-bottom">
                    <div class="pf-c-options-menu pf-m-top">
                      <div
                        class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                      >
                        <span class="pf-c-options-menu__toggle-text">
                          <b>1 - 10</b>&nbsp;of&nbsp;
                          <b>36</b>
                        </span>
                        <button
                          class="pf-c-options-menu__toggle-button"
                          id="table-tabs-w-secondary-example-footer-pagination-toggle"
                          aria-haspopup="listbox"
                          aria-expanded="false"
                          aria-label="Items per page"
                        >
                          <span class="pf-c-options-menu__toggle-button-icon">
                            <i class="fas fa-caret-down" aria-hidden="true"></i>
                          </span>
                        </button>
                      </div>
                      <ul
                        class="pf-c-options-menu__menu pf-m-top"
                        aria-labelledby="table-tabs-w-secondary-example-footer-pagination-toggle"
                        hidden
                      >
                        <li>
                          <button
                            class="pf-c-options-menu__menu-item"
                            type="button"
                          >5 per page</button>
                        </li>
                        <li>
                          <button
                            class="pf-c-options-menu__menu-item"
                            type="button"
                          >
                            10 per page
                            <div class="pf-c-options-menu__menu-item-icon">
                              <i class="fas fa-check" aria-hidden="true"></i>
                            </div>
                          </button>
                        </li>
                        <li>
                          <button
                            class="pf-c-options-menu__menu-item"
                            type="button"
                          >20 per page</button>
                        </li>
                      </ul>
                    </div>
                    <nav class="pf-c-pagination__nav" aria-label="Pagination">
                      <div class="pf-c-pagination__nav-control pf-m-first">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          disabled
                          aria-label="Go to first page"
                        >
                          <i
                            class="fas fa-angle-double-left"
                            aria-hidden="true"
                          ></i>
                        </button>
                      </div>
                      <div class="pf-c-pagination__nav-control pf-m-prev">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          disabled
                          aria-label="Go to previous page"
                        >
                          <i class="fas fa-angle-left" aria-hidden="true"></i>
                        </button>
                      </div>
                      <div class="pf-c-pagination__nav-page-select">
                        <input
                          class="pf-c-form-control"
                          aria-label="Current page"
                          type="number"
                          min="1"
                          max="4"
                          value="1"
                        />
                        <span aria-hidden="true">of 4</span>
                      </div>
                      <div class="pf-c-pagination__nav-control pf-m-next">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          aria-label="Go to next page"
                        >
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </button>
                      </div>
                      <div class="pf-c-pagination__nav-control pf-m-last">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          aria-label="Go to last page"
                        >
                          <i
                            class="fas fa-angle-double-right"
                            aria-hidden="true"
                          ></i>
                        </button>
                      </div>
                    </nav>
                  </div>
                </div>
              </div>

              <!-- Panel -->
              <div class="pf-c-drawer__panel pf-m-width-33 pf-m-width-33-on-xl">
                <div class="pf-c-drawer__body">
                  <div class="pf-c-drawer__head">
                    <div class="pf-c-drawer__actions">
                      <div class="pf-c-drawer__close">
                        <button
                          class="pf-c-button pf-m-plain"
                          type="button"
                          aria-label="Close drawer panel"
                        >
                          <i class="fas fa-times" aria-hidden="true"></i>
                        </button>
                      </div>
                    </div>
                    <div class="pf-l-flex pf-m-column pf-m-space-items-sm">
                      <div class="pf-l-flex__item">
                        <h2
                          class="pf-c-title pf-m-lg"
                          id="table-tabs-w-secondary-example-tabs-drawer-label"
                        >Node 2</h2>
                      </div>
                      <div class="pf-l-flex__item">
                        <a href="#">siemur/test-space</a>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="pf-c-drawer__body pf-m-no-padding">
                  <div
                    class="pf-c-tabs pf-m-no-border-bottom pf-m-inset-md pf-m-inset-sm-on-md"
                    id="table-tabs-w-secondary-example-tabs-tabs"
                  >
                    <button
                      class="pf-c-tabs__scroll-button"
                      aria-label="Scroll left"
                    >
                      <i class="fas fa-angle-left" aria-hidden="true"></i>
                    </button>
                    <ul class="pf-c-tabs__list">
                      <li class="pf-c-tabs__item pf-m-current">
                        <button
                          class="pf-c-tabs__link"
                          aria-controls="table-tabs-w-secondary-example-tabs-tabs-tab1-panel"
                          id="table-tabs-w-secondary-example-tabs-tabs-tab1-link"
                        >
                          <span class="pf-c-tabs__item-text">Overview</span>
                        </button>
                      </li>
                      <li class="pf-c-tabs__item">
                        <button
                          class="pf-c-tabs__link"
                          aria-controls="table-tabs-w-secondary-example-tabs-tabs-tab2-panel"
                          id="table-tabs-w-secondary-example-tabs-tabs-tab2-link"
                        >
                          <span class="pf-c-tabs__item-text">Activity</span>
                        </button>
                      </li>
                      <li class="pf-c-tabs__item">
                        <button
                          class="pf-c-tabs__link"
                          aria-controls="table-tabs-w-secondary-example-tabs-tabs-tab3-panel"
                          id="table-tabs-w-secondary-example-tabs-tabs-tab3-link"
                        >
                          <span class="pf-c-tabs__item-text">Status</span>
                        </button>
                      </li>
                    </ul>
                    <button
                      class="pf-c-tabs__scroll-button"
                      aria-label="Scroll right"
                    >
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </button>
                  </div>
                </div>
                <div class="pf-c-drawer__body">
                  <section
                    class="pf-c-tab-content"
                    id="table-tabs-w-secondary-example-tabs-tabs-tab1-panel"
                    aria-labelledby="table-tabs-w-secondary-example-tabs-tabs-tab1-link"
                    role="tabpanel"
                    tabindex="0"
                  >
                    <div class="pf-c-tab-content__body">
                      <div class="pf-l-flex pf-m-column pf-m-space-items-lg">
                        <div class="pf-l-flex__item">
                          <p>The content of the drawer really is up to you. It could have form fields, definition lists, text lists, labels, charts, progress bars, etc. Spacing recommendation is 24px margins. You can put tabs in here, and can also make the drawer scrollable.</p>
                        </div>
                        <div class="pf-l-flex__item">
                          <div
                            class="pf-c-progress pf-m-sm"
                            id="table-tabs-w-secondary-example-tabs-progress-example1"
                          >
                            <div
                              class="pf-c-progress__description"
                              id="table-tabs-w-secondary-example-tabs-progress-example1-description"
                            >Capacity</div>
                            <div
                              class="pf-c-progress__status"
                              aria-hidden="true"
                            >
                              <span class="pf-c-progress__measure">33%</span>
                            </div>
                            <div
                              class="pf-c-progress__bar"
                              role="progressbar"
                              aria-valuemin="0"
                              aria-valuemax="100"
                              aria-valuenow="33"
                              aria-labelledby="table-tabs-w-secondary-example-tabs-progress-example1-description"
                              aria-label="Progress 1"
                            >
                              <div
                                class="pf-c-progress__indicator"
                                style="width:33%;"
                              ></div>
                            </div>
                          </div>
                        </div>
                        <div class="pf-l-flex__item">
                          <div
                            class="pf-c-progress pf-m-sm"
                            id="table-tabs-w-secondary-example-tabs-progress-example2"
                          >
                            <div
                              class="pf-c-progress__description"
                              id="table-tabs-w-secondary-example-tabs-progress-example2-description"
                            >Modules</div>
                            <div
                              class="pf-c-progress__status"
                              aria-hidden="true"
                            >
                              <span class="pf-c-progress__measure">66%</span>
                            </div>
                            <div
                              class="pf-c-progress__bar"
                              role="progressbar"
                              aria-valuemin="0"
                              aria-valuemax="100"
                              aria-valuenow="66"
                              aria-labelledby="table-tabs-w-secondary-example-tabs-progress-example2-description"
                              aria-label="Progress 2"
                            >
                              <div
                                class="pf-c-progress__indicator"
                                style="width:66%;"
                              ></div>
                            </div>
                          </div>
                        </div>
                        <div class="pf-l-flex pf-m-column">
                          <div class="pf-l-flex__item">
                            <h3
                              class="pf-c-title"
                              id="table-tabs-w-secondary-example-tabs-title"
                            >Tags</h3>
                          </div>
                          <div class="pf-l-flex__item">
                            <div class="pf-c-label-group">
                              <div class="pf-c-label-group__main">
                                <ul
                                  class="pf-c-label-group__list"
                                  role="list"
                                  aria-label="Group of labels"
                                >
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span class="pf-c-label__content">Tag 1</span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span
                                        class="pf-c-label__content"
                                      >Different tag 1</span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span
                                        class="pf-c-label__content"
                                      >Another different tag 1</span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span
                                        class="pf-c-label__content"
                                      >Another different tag 2</span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-outline">
                                      <span
                                        class="pf-c-label__content"
                                      >Different than tag 1 or 2</span>
                                    </span>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </section>
                  <section
                    class="pf-c-tab-content"
                    id="table-tabs-w-secondary-example-tabs-tabs-tab2-panel"
                    aria-labelledby="table-tabs-w-secondary-example-tabs-tabs-tab2-link"
                    role="tabpanel"
                    tabindex="0"
                    hidden
                  >
                    <div class="pf-c-tab-content__body">Panel 2</div>
                  </section>
                  <section
                    class="pf-c-tab-content"
                    id="table-tabs-w-secondary-example-tabs-tabs-tab3-panel"
                    aria-labelledby="table-tabs-w-secondary-example-tabs-tabs-tab3-link"
                    role="tabpanel"
                    tabindex="0"
                    hidden
                  >
                    <div class="pf-c-tab-content__body">Panel 3</div>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
      <section
        class="pf-c-tab-content"
        aria-labelledby="table-tabs-w-secondary-example-tabs-node-connectors-link"
        id="table-tabs-w-secondary-example-tabs-node-connectors-panel"
        role="tabpanel"
        tabindex="0"
        hidden
      >
        <div class="pf-c-tab-content__body">
          <div class="pf-c-content">
            <p>Node connectors content</p>
          </div>
        </div>
      </section>
    </section>
  </main>
</div>

```

### Modal tabs

```html isFullscreen
<div aria-hidden="true">Page content</div>
<div class="pf-c-backdrop">
  <div class="pf-l-bullseye">
    <div
      class="pf-c-modal-box pf-m-sm"
      aria-modal="true"
      aria-labelledby="modal-tabs-example-modal-title"
      aria-describedby="modal-tabs-example-modal-description"
    >
      <button
        class="pf-c-button pf-m-plain"
        type="button"
        aria-label="Close dialog"
      >
        <i class="fas fa-times" aria-hidden="true"></i>
      </button>
      <header class="pf-c-modal-box__header">
        <h1
          class="pf-c-modal-box__title"
          id="modal-tabs-example-modal-title"
        >Modal title</h1>
      </header>
      <div
        class="pf-c-modal-box__body"
        id="modal-tabs-example-modal-description"
      >
        <div class="pf-l-grid pf-m-gutter">
          <div class="pf-l-grid__item">
            <div
              class="pf-c-tabs pf-m-no-border-bottom pf-m-inset-none pf-m-secondary"
              id="modal-tabs-example-tabs"
            >
              <ul class="pf-c-tabs__list">
                <li class="pf-c-tabs__item pf-m-current">
                  <button
                    class="pf-c-tabs__link"
                    aria-controls="modal-tabs-example-tabs-details-panel"
                    id="modal-tabs-example-tabs-details-link"
                  >
                    <span class="pf-c-tabs__item-text">Details</span>
                  </button>
                </li>
                <li class="pf-c-tabs__item">
                  <button
                    class="pf-c-tabs__link"
                    aria-controls="modal-tabs-example-tabs-documentation-panel"
                    id="modal-tabs-example-tabs-documentation-link"
                  >
                    <span class="pf-c-tabs__item-text">Documentation</span>
                  </button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-l-grid__item">
            <section
              class="pf-c-tab-content"
              aria-labelledby="modal-tabs-example-tabs-details-link"
              id="modal-tabs-example-tabs-details-panel"
              role="tabpanel"
              tabindex="0"
            >
              <div class="pf-c-tab-content__body">
                <p>To support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby to support screen reader user awareness of the dialog text, the dialog text is wrapped in a div that is referenced by aria-describedby.</p>
              </div>
            </section>
            <section
              class="pf-c-tab-content"
              aria-labelledby="modal-tabs-example-tabs-documentation-link"
              id="modal-tabs-example-tabs-documentation-panel"
              role="tabpanel"
              tabindex="0"
              hidden
            >
              <div class="pf-c-tab-content__body">Documentation tab content</div>
            </section>
          </div>
        </div>
      </div>
      <footer class="pf-c-modal-box__footer">
        <button class="pf-c-button pf-m-primary" type="button">Save</button>
        <button class="pf-c-button pf-m-link" type="button">Cancel</button>
      </footer>
    </div>
  </div>
</div>
<div class="pf-c-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="-primary-nav"
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
              id="-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="-dropdown-kebab-1-button"
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
              id="-dropdown-kebab-2-button"
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
              aria-labelledby="-dropdown-kebab-2-button"
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
      <nav class="pf-c-nav" id="-primary-nav" aria-label="Global">
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
  <main class="pf-c-page__main" tabindex="-1" id="main-content-">
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <div class="pf-c-content">
          <h1>Projects</h1>
          <p>This is a demo that showcases Patternfly Cards.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-light pf-m-no-padding">
      <div class="pf-c-toolbar">
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section pf-m-nowrap">
            <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-xl">
              <div class="pf-c-toolbar__toggle">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Show filters"
                  aria-expanded="false"
                  aria-controls="-expandable-content"
                >
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </button>
              </div>

              <div class="pf-c-toolbar__item pf-m-bulk-select">
                <div class="pf-c-dropdown">
                  <div class="pf-c-dropdown__toggle pf-m-split-button">
                    <label
                      class="pf-c-dropdown__toggle-check"
                      for="-bulk-select-toggle-check"
                    >
                      <input
                        type="checkbox"
                        id="-bulk-select-toggle-check"
                        aria-label="Select all"
                      />
                    </label>

                    <button
                      class="pf-c-dropdown__toggle-button"
                      type="button"
                      aria-expanded="false"
                      id="-bulk-select-toggle-button"
                      aria-label="Dropdown toggle"
                    >
                      <i class="fas fa-caret-down" aria-hidden="true"></i>
                    </button>
                  </div>
                  <ul class="pf-c-dropdown__menu" hidden>
                    <li>
                      <button
                        class="pf-c-dropdown__menu-item"
                        type="button"
                      >Select all</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-dropdown__menu-item"
                        type="button"
                      >Select none</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-dropdown__menu-item"
                        type="button"
                      >Other action</button>
                    </li>
                  </ul>
                </div>
              </div>

              <div class="pf-c-toolbar__item">
                <div class="pf-c-select">
                  <span id="-select-checkbox-status-label" hidden>Choose one</span>

                  <button
                    class="pf-c-select__toggle"
                    type="button"
                    id="-select-checkbox-status-toggle"
                    aria-haspopup="true"
                    aria-expanded="false"
                    aria-labelledby="-select-checkbox-status-label -select-checkbox-status-toggle"
                  >
                    <div class="pf-c-select__toggle-wrapper">
                      <span class="pf-c-select__toggle-text">Status</span>
                    </div>
                    <span class="pf-c-select__toggle-arrow">
                      <i class="fas fa-caret-down" aria-hidden="true"></i>
                    </span>
                  </button>

                  <div class="pf-c-select__menu" hidden>
                    <fieldset
                      class="pf-c-select__menu-fieldset"
                      aria-label="Select input"
                    >
                      <label
                        class="pf-c-check pf-c-select__menu-item pf-m-description"
                        for="-select-checkbox-status-active"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-select-checkbox-status-active"
                          name="-select-checkbox-status-active"
                        />

                        <span class="pf-c-check__label">Active</span>
                        <span
                          class="pf-c-check__description"
                        >This is a description</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item pf-m-description"
                        for="-select-checkbox-status-canceled"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-select-checkbox-status-canceled"
                          name="-select-checkbox-status-canceled"
                        />

                        <span class="pf-c-check__label">Canceled</span>
                        <span
                          class="pf-c-check__description"
                        >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="-select-checkbox-status-paused"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-select-checkbox-status-paused"
                          name="-select-checkbox-status-paused"
                        />

                        <span class="pf-c-check__label">Paused</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="-select-checkbox-status-warning"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-select-checkbox-status-warning"
                          name="-select-checkbox-status-warning"
                        />

                        <span class="pf-c-check__label">Warning</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="-select-checkbox-status-restarted"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-select-checkbox-status-restarted"
                          name="-select-checkbox-status-restarted"
                        />

                        <span class="pf-c-check__label">Restarted</span>
                      </label>
                    </fieldset>
                  </div>
                </div>
              </div>
            </div>

            <div class="pf-c-overflow-menu" id="-overflow-menu">
              <div
                class="pf-c-overflow-menu__content pf-u-display-none pf-u-display-flex-on-lg"
              >
                <div class="pf-c-overflow-menu__group pf-m-button-group">
                  <div class="pf-c-overflow-menu__item">
                    <button
                      class="pf-c-button pf-m-primary"
                      type="button"
                    >Create instance</button>
                  </div>
                </div>
              </div>
              <div class="pf-c-overflow-menu__control">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                    type="button"
                    id="-overflow-menu-dropdown-toggle"
                    aria-label="Dropdown with additional options"
                    aria-expanded="false"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu"
                    aria-labelledby="-overflow-menu-dropdown-toggle"
                    hidden
                  >
                    <li>
                      <button class="pf-c-dropdown__menu-item">Action 7</button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>

            <div class="pf-c-toolbar__item pf-m-pagination">
              <div class="pf-c-pagination pf-m-compact">
                <div class="pf-c-options-menu">
                  <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
                    <span class="pf-c-options-menu__toggle-text">
                      <b>1 - 10</b>&nbsp;of&nbsp;
                      <b>36</b>
                    </span>
                    <button
                      class="pf-c-options-menu__toggle-button"
                      id="-top-pagination-toggle"
                      aria-haspopup="listbox"
                      aria-expanded="false"
                      aria-label="Items per page"
                    >
                      <span class="pf-c-options-menu__toggle-button-icon">
                        <i class="fas fa-caret-down" aria-hidden="true"></i>
                      </span>
                    </button>
                  </div>
                  <ul
                    class="pf-c-options-menu__menu"
                    aria-labelledby="-top-pagination-toggle"
                    hidden
                  >
                    <li>
                      <button
                        class="pf-c-options-menu__menu-item"
                        type="button"
                      >5 per page</button>
                    </li>
                    <li>
                      <button
                        class="pf-c-options-menu__menu-item"
                        type="button"
                      >
                        10 per page
                        <div class="pf-c-options-menu__menu-item-icon">
                          <i class="fas fa-check" aria-hidden="true"></i>
                        </div>
                      </button>
                    </li>
                    <li>
                      <button
                        class="pf-c-options-menu__menu-item"
                        type="button"
                      >20 per page</button>
                    </li>
                  </ul>
                </div>
                <nav
                  class="pf-c-pagination__nav"
                  aria-label="Toolbar top pagination"
                >
                  <div class="pf-c-pagination__nav-control pf-m-prev">
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      disabled
                      aria-label="Go to previous page"
                    >
                      <i class="fas fa-angle-left" aria-hidden="true"></i>
                    </button>
                  </div>
                  <div class="pf-c-pagination__nav-control pf-m-next">
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-label="Go to next page"
                    >
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </button>
                  </div>
                </nav>
              </div>
            </div>
          </div>

          <div
            class="pf-c-toolbar__expandable-content pf-m-hidden"
            id="-expandable-content"
            hidden
          ></div>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-fill">
      <div class="pf-l-gallery pf-m-gutter">
        <div
          class="pf-c-card pf-m-hoverable pf-m-compact"
          id="card-empty-state"
        >
          <div class="pf-l-bullseye">
            <div class="pf-c-empty-state pf-m-xs">
              <div class="pf-c-empty-state__content">
                <i class="fas fa-plus-circle pf-c-empty-state__icon"></i>
                <h2 class="pf-c-title pf-m-md">Add a new card to your page</h2>
                <div class="pf-c-empty-state__secondary">
                  <button class="pf-c-button pf-m-link" type="button">Add card</button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-1">
          <div class="pf-c-card__header">
            <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-1-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-1-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-1-check"
                  name="card-1-check"
                  aria-labelledby="card-1-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-1-check-label">Patternfly</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >PatternFly is a community project that promotes design commonality and improves user experience.</div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-2">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/activemq-core_200x150.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-2-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-2-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-2-check"
                  name="card-2-check"
                  aria-labelledby="card-2-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-2-check-label">ActiveMQ</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >The ActiveMQ component allows messages to be sent to a JMS Queue or Topic; or messages to be consumed from a JMS Queue or Topic using Apache ActiveMQ.</div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-3">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/camel-spark_200x150.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-3-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-3-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-3-check"
                  name="card-3-check"
                  aria-labelledby="card-3-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-3-check-label">Apache Spark</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >This documentation page covers the Apache Spark component for the Apache Camel.</div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-4">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/camel-avro_200x150.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-4-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-4-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-4-check"
                  name="card-4-check"
                  aria-labelledby="card-4-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-4-check-label">Avro</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >This component provides a dataformat for avro, which allows serialization and deserialization of messages using Apache Avro’s binary dataformat. Moreover, it provides support for Apache Avro’s rpc, by providing producers and consumers endpoint for using avro over netty or http.</div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-5">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/FuseConnector_Icons_AzureServices.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-5-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-5-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-5-check"
                  name="card-5-check"
                  aria-labelledby="card-5-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-5-check-label">Azure Services</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >The Camel Components for Windows Azure Services provide connectivity to Azure services from Camel.</div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-6">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/camel-saxon_200x150.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-6-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-6-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-6-check"
                  name="card-6-check"
                  aria-labelledby="card-6-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-6-check-label">Crypto</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >For providing flexible endpoints to sign and verify exchanges using the Signature Service of the Java Cryptographic Extension.</div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-7">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/camel-dropbox_200x150.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-7-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-7-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-7-check"
                  name="card-7-check"
                  aria-labelledby="card-7-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-7-check-label">DropBox</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >The dropbox: component allows you to treat Dropbox remote folders as a producer or consumer of messages.</div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-8">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/camel-infinispan_200x150.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-8-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-8-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-8-check"
                  name="card-8-check"
                  aria-labelledby="card-8-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-8-check-label">JBoss Data Grid</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >Read or write to a fully-supported distributed cache and data grid for faster integration services.</div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-9">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/FuseConnector_Icons_REST.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-9-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-9-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-9-check"
                  name="card-9-check"
                  aria-labelledby="card-9-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-9-check-label">REST</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div class="pf-c-card__body">
            The rest component allows to define REST endpoints (consumer) using the Rest DSL and plugin to other Camel components as the REST transport.
            From Camel 2.18 onwards the rest component can also be used as a client (producer) to call REST services.
          </div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact" id="card-10">
          <div class="pf-c-card__header">
            <img
              src="/assets/images/camel-swagger-java_200x150.png"
              width="60px"
              alt="Logo"
            />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-10-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-10-dropdown-kebab-button"
                  hidden
                >
                  <li>
                    <a class="pf-c-dropdown__menu-item" href="#">Link</a>
                  </li>
                  <li>
                    <button
                      class="pf-c-dropdown__menu-item"
                      type="button"
                    >Action</button>
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
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="card-10-check"
                  name="card-10-check"
                  aria-labelledby="card-10-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-10-check-label">SWAGGER</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >Expose REST services and their APIs using Swagger specification.</div>
        </div>
      </div>
    </section>
    <section
      class="pf-c-page__main-section pf-m-no-padding pf-m-light pf-m-sticky-bottom pf-m-no-fill"
    >
      <div class="pf-c-pagination pf-m-bottom">
        <div class="pf-c-options-menu pf-m-top">
          <div class="pf-c-options-menu__toggle pf-m-text pf-m-plain">
            <span class="pf-c-options-menu__toggle-text">
              <b>1 - 10</b>&nbsp;of&nbsp;
              <b>36</b>
            </span>
            <button
              class="pf-c-options-menu__toggle-button"
              id="table-tabs-w-secondary-example-bottom-pagination-toggle"
              aria-haspopup="listbox"
              aria-expanded="false"
              aria-label="Items per page"
            >
              <span class="pf-c-options-menu__toggle-button-icon">
                <i class="fas fa-caret-down" aria-hidden="true"></i>
              </span>
            </button>
          </div>
          <ul
            class="pf-c-options-menu__menu pf-m-top"
            aria-labelledby="table-tabs-w-secondary-example-bottom-pagination-toggle"
            hidden
          >
            <li>
              <button
                class="pf-c-options-menu__menu-item"
                type="button"
              >5 per page</button>
            </li>
            <li>
              <button class="pf-c-options-menu__menu-item" type="button">
                10 per page
                <div class="pf-c-options-menu__menu-item-icon">
                  <i class="fas fa-check" aria-hidden="true"></i>
                </div>
              </button>
            </li>
            <li>
              <button
                class="pf-c-options-menu__menu-item"
                type="button"
              >20 per page</button>
            </li>
          </ul>
        </div>
        <nav class="pf-c-pagination__nav" aria-label="Pagination">
          <div class="pf-c-pagination__nav-control pf-m-first">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              disabled
              aria-label="Go to first page"
            >
              <i class="fas fa-angle-double-left" aria-hidden="true"></i>
            </button>
          </div>
          <div class="pf-c-pagination__nav-control pf-m-prev">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              disabled
              aria-label="Go to previous page"
            >
              <i class="fas fa-angle-left" aria-hidden="true"></i>
            </button>
          </div>
          <div class="pf-c-pagination__nav-page-select">
            <input
              class="pf-c-form-control"
              aria-label="Current page"
              type="number"
              min="1"
              max="4"
              value="1"
            />
            <span aria-hidden="true">of 4</span>
          </div>
          <div class="pf-c-pagination__nav-control pf-m-next">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Go to next page"
            >
              <i class="fas fa-angle-right" aria-hidden="true"></i>
            </button>
          </div>
          <div class="pf-c-pagination__nav-control pf-m-last">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              aria-label="Go to last page"
            >
              <i class="fas fa-angle-double-right" aria-hidden="true"></i>
            </button>
          </div>
        </nav>
      </div>
    </section>
  </main>
</div>

```

### Gray tabs

```html isFullscreen
<div class="pf-c-page" id="gray-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-gray-tabs-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="gray-tabs-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="gray-tabs-example-primary-nav"
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
              id="gray-tabs-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="gray-tabs-example-dropdown-kebab-1-button"
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
              id="gray-tabs-example-dropdown-kebab-2-button"
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
              aria-labelledby="gray-tabs-example-dropdown-kebab-2-button"
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
        id="gray-tabs-example-primary-nav"
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
    id="main-content-gray-tabs-example"
  >
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <h1 class="pf-c-title pf-m-2xl pf-u-mt-md">Overview</h1>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-box pf-m-page-insets pf-m-color-scheme--light-300"
          id="gray-tabs-example-tabs"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-cluster-1-panel"
                id="gray-tabs-example-tabs-cluster-1-link"
              >
                <span class="pf-c-tabs__item-text">Cluster 1</span>
              </button>
            </li>
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-cluster-2-panel"
                id="gray-tabs-example-tabs-cluster-2-link"
              >
                <span class="pf-c-tabs__item-text">Cluster 2</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-cluster-3-panel"
                id="gray-tabs-example-tabs-cluster-3-link"
              >
                <span class="pf-c-tabs__item-text">Cluster 3</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <section
          class="pf-c-tab-content"
          aria-labelledby="gray-tabs-example-tabs-cluster-1-link"
          id="gray-tabs-example-tabs-cluster-1-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-c-content">
              <p>Cluster 1 content</p>
            </div>
          </div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="gray-tabs-example-tabs-cluster-2-link"
          id="gray-tabs-example-tabs-cluster-2-panel"
          role="tabpanel"
          tabindex="0"
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-l-grid pf-m-gutter">
              <div class="pf-l-grid__item pf-m-6-col-on-md pf-m-8-col-on-xl">
                <div class="pf-c-card pf-m-full-height">
                  <div class="pf-c-card__header">
                    <h2 class="pf-c-title pf-m-lg pf-u-font-weight-light">Status</h2>
                  </div>
                  <div class="pf-c-card__body">
                    <div class="pf-l-flex pf-m-column">
                      <div class="pf-l-flex__item">
                        <div
                          class="pf-c-tabs pf-m-no-border-bottom"
                          id="gray-tabs-example-tabs-subtabs"
                        >
                          <ul class="pf-c-tabs__list">
                            <li class="pf-c-tabs__item pf-m-current">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="gray-tabs-example-tabs-subtabs-cluster-panel"
                                id="gray-tabs-example-tabs-subtabs-cluster-link"
                              >
                                <span class="pf-c-tabs__item-text">Cluster</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="gray-tabs-example-tabs-subtabs-control-plane-panel"
                                id="gray-tabs-example-tabs-subtabs-control-plane-link"
                              >
                                <span class="pf-c-tabs__item-text">Control plane</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="gray-tabs-example-tabs-subtabs-operators-panel"
                                id="gray-tabs-example-tabs-subtabs-operators-link"
                              >
                                <span class="pf-c-tabs__item-text">Operators</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="gray-tabs-example-tabs-subtabs-virtualization-panel"
                                id="gray-tabs-example-tabs-subtabs-virtualization-link"
                              >
                                <span
                                  class="pf-c-tabs__item-text"
                                >Virtualization</span>
                              </button>
                            </li>
                          </ul>
                        </div>
                      </div>
                      <div class="pf-l-flex__item">
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="gray-tabs-example-tabs-subtabs-cluster-link"
                          id="gray-tabs-example-tabs-subtabs-cluster-panel"
                          role="tabpanel"
                          tabindex="0"
                        >
                          <div class="pf-c-tab-content__body">
                            <div class="pf-c-content">
                              <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce in odio porttitor, feugiat risus in, feugiat arcu. Nullam euismod enim eget fringilla condimentum. Maecenas tincidunt et metus id aliquet. Integer et fermentum purus. Nulla tempor velit arcu, vitae semper purus iaculis at. Sed malesuada auctor luctus. Pellentesque et leo urna. Aliquam vitae felis congue lacus mattis fringilla. Nullam et ultricies erat, sed dignissim elit. Cras mattis pulvinar aliquam. In ac est nulla. Pellentesque fermentum nibh ac sapien porta, ut congue orci aliquam. Sed nisl est, tempor eu pharetra eget, ullamcorper ut augue. Vestibulum eleifend libero eu nulla cursus lacinia.</p>
                            </div>
                          </div>
                        </section>
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="gray-tabs-example-tabs-subtabs-control-plane-link"
                          id="gray-tabs-example-tabs-subtabs-control-plane-panel"
                          role="tabpanel"
                          tabindex="0"
                          hidden
                        >
                          <div
                            class="pf-c-tab-content__body"
                          >Control plane content</div>
                        </section>
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="gray-tabs-example-tabs-subtabs-operators-link"
                          id="gray-tabs-example-tabs-subtabs-operators-panel"
                          role="tabpanel"
                          tabindex="0"
                          hidden
                        >
                          <div class="pf-c-tab-content__body">Operators content</div>
                        </section>
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="gray-tabs-example-tabs-subtabs-virtualization-link"
                          id="gray-tabs-example-tabs-subtabs-virtualization-panel"
                          role="tabpanel"
                          tabindex="0"
                          hidden
                        >
                          <div
                            class="pf-c-tab-content__body"
                          >Virtualization content</div>
                        </section>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="pf-l-grid__item pf-m-6-col-on-md pf-m-4-col-on-xl">
                <div class="pf-l-flex pf-m-column pf-u-h-100">
                  <div class="pf-l-flex__item pf-m-flex-1">
                    <div class="pf-c-card pf-m-full-height">
                      <div class="pf-c-card__header">
                        <h3
                          class="pf-c-title pf-m-lg pf-u-font-weight-light"
                        >Title of card</h3>
                      </div>
                    </div>
                  </div>
                  <div class="pf-l-flex__item pf-m-flex-1">
                    <div class="pf-c-card pf-m-full-height">
                      <div class="pf-c-card__header">
                        <h3
                          class="pf-c-title pf-m-lg pf-u-font-weight-light"
                        >Title of card</h3>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="gray-tabs-example-tabs-cluster-3-link"
          id="gray-tabs-example-tabs-cluster-3-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-c-content">
              <p>Cluster 1 content</p>
            </div>
          </div>
        </section>
      </div>
    </section>
  </main>
</div>

```

### Nested, unindented tabs

```html isFullscreen
<div class="pf-c-page" id="nested-unindented-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-nested-unindented-tabs-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="nested-unindented-tabs-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="nested-unindented-tabs-example-primary-nav"
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
              id="nested-unindented-tabs-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="nested-unindented-tabs-example-dropdown-kebab-1-button"
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
              id="nested-unindented-tabs-example-dropdown-kebab-2-button"
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
              aria-labelledby="nested-unindented-tabs-example-dropdown-kebab-2-button"
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
        id="nested-unindented-tabs-example-primary-nav"
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
    id="main-content-nested-unindented-tabs-example"
  >
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <h1 class="pf-c-title pf-m-2xl">Red Hat Enterprise Linux</h1>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-box pf-m-page-insets"
          id="nested-unindented-tabs-example-tabs"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="nested-unindented-tabs-example-tabs-new-panel"
                id="nested-unindented-tabs-example-tabs-new-link"
              >
                <span class="pf-c-tabs__item-text">What's new</span>
              </button>
            </li>
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="nested-unindented-tabs-example-tabs-get-started-panel"
                id="nested-unindented-tabs-example-tabs-get-started-link"
              >
                <span class="pf-c-tabs__item-text">Get started</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="nested-unindented-tabs-example-tabs-knowledge-panel"
                id="nested-unindented-tabs-example-tabs-knowledge-link"
              >
                <span class="pf-c-tabs__item-text">Knowledge</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="nested-unindented-tabs-example-tabs-support-panel"
                id="nested-unindented-tabs-example-tabs-support-link"
              >
                <span class="pf-c-tabs__item-text">Support</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-light">
      <div class="pf-c-page__main-body">
        <section
          class="pf-c-tab-content"
          aria-labelledby="nested-unindented-tabs-example-tabs-new-link"
          id="nested-unindented-tabs-example-tabs-new-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">What's new content</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="nested-unindented-tabs-example-tabs-get-started-link"
          id="nested-unindented-tabs-example-tabs-get-started-panel"
          role="tabpanel"
          tabindex="0"
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-l-grid pf-m-gutter">
              <div class="pf-l-grid__item">
                <h1
                  class="pf-c-title pf-m-2xl"
                >Get started with Red Hat Enterprise Linux</h1>
              </div>
              <div class="pf-l-grid__item">
                <div
                  class="pf-c-tabs pf-m-inset-none pf-m-secondary"
                  id="nested-unindented-tabs-example-subtabs"
                >
                  <ul class="pf-c-tabs__list">
                    <li class="pf-c-tabs__item pf-m-current">
                      <button
                        class="pf-c-tabs__link"
                        aria-controls="nested-unindented-tabs-example-subtabs-x86-panel"
                        id="nested-unindented-tabs-example-subtabs-x86-link"
                      >
                        <span class="pf-c-tabs__item-text">x86 architecture</span>
                      </button>
                    </li>
                    <li class="pf-c-tabs__item">
                      <button
                        class="pf-c-tabs__link"
                        aria-controls="nested-unindented-tabs-example-subtabs-additional-architectures-panel"
                        id="nested-unindented-tabs-example-subtabs-additional-architectures-link"
                      >
                        <span
                          class="pf-c-tabs__item-text"
                        >Additional Architectures</span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
              <div class="pf-l-grid__item">
                <section
                  class="pf-c-tab-content"
                  aria-labelledby="nested-unindented-tabs-example-subtabs-x86-link"
                  id="nested-unindented-tabs-example-subtabs-x86-panel"
                  role="tabpanel"
                  tabindex="0"
                >
                  <div class="pf-c-tab-content__body">
                    <div class="pf-l-grid pf-m-gutter">
                      <div class="pf-l-grid__item">
                        <div class="pf-c-content">
                          <p>To perform a standard x86_64 installation using the GUI, you'll need to:</p>
                        </div>
                      </div>
                      <div
                        class="pf-l-grid pf-m-all-6-col-on-md pf-m-all-3-col-on-2xl pf-m-gutter"
                      >
                        <div class="pf-c-card">
                          <div
                            class="pf-c-card__header"
                          >Check system requirements</div>
                          <div class="pf-c-card__body">
                            <p>
                              Your physical or virtual machine should meet the
                              <a href="#">system requirement</a>.
                            </p>
                          </div>
                        </div>
                        <div class="pf-c-card">
                          <div
                            class="pf-c-card__header"
                          >Download an installation ISO image</div>
                          <div class="pf-c-card__body">
                            <p>
                              <a href="#">Download</a>&nbsp;the binary DVD ISO.
                            </p>
                          </div>
                        </div>
                        <div class="pf-c-card">
                          <div
                            class="pf-c-card__header"
                          >Create a bootable installation media</div>
                          <div class="pf-c-card__body">
                            <p>
                              <a href="#">Create</a>&nbsp;a bootable installation media, for example a USB flash drive.
                            </p>
                          </div>
                        </div>
                        <div class="pf-c-card">
                          <div
                            class="pf-c-card__header"
                          >Install and register your system</div>
                          <div class="pf-c-card__body">
                            <p>Boot the installation, register your system, attach RHEL subscriptions, and in stall RHEL from the Red Hat Content Delivery Network (CDN) using the GUI.</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </section>
                <section
                  class="pf-c-tab-content"
                  aria-labelledby="nested-unindented-tabs-example-subtabs-additional-architectures-link"
                  id="nested-unindented-tabs-example-subtabs-additional-architectures-panel"
                  role="tabpanel"
                  tabindex="0"
                  hidden
                >
                  <div class="pf-c-tab-content__body">
                    <p>Additional architectural content</p>
                  </div>
                </section>
              </div>
            </div>
          </div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="nested-unindented-tabs-example-tabs-knowledge-link"
          id="nested-unindented-tabs-example-tabs-knowledge-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-c-content">
              <p>Knowledge content</p>
            </div>
          </div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="nested-unindented-tabs-example-tabs-support-link"
          id="nested-unindented-tabs-example-tabs-support-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-c-content">
              <p>Support content</p>
            </div>
          </div>
        </section>
      </div>
    </section>
  </main>
</div>

```

<!-- ### Vertical tabs
```hbs isFullscreen
{{#> tabs--page-wrapper tabs--page-wrapper--id="vertical-tabs-example"}}
  {{> page-template-breadcrumb page-template-breadcrumbs--IsOS="true"}}
  {{#> page-main-section page-main-section--IsLimitWidth="true" page-main-section--modifier="pf-m-light"}}
    {{#> title title--modifier="pf-m-2xl pf-u-mt-md"}}
      Install OpenShift on AWS
    {{/title}}
  {{/page-main-section}}
  {{#> page-main-section page-main-section--IsLimitWidth="true" tabs--id=(concat tabs--page-wrapper--id '-tabs') tab-content--id=(concat tabs--page-wrapper--id '-tabs')}}
    {{#> card card--modifier="pf-m-full-height"}}
      {{#> sidebar}}
        {{#> sidebar-panel}}
          {{#> tabs tabs--modifier="pf-m-page-insets pf-m-vertical pf-m-box" }}
            {{#> tabs-list}}
              {{> __tabs-item
                __tabs-item--current="true"
                __tabs-item--id="installation"
                __tabs-item--aria-label="Installation"
                __tabs-item--text="Installation"
                __tabs-item--attribute=(concat 'aria-controls="' tabs--id '-installation-panel"')}}
              {{> __tabs-item
                __tabs-item--id="limits"
                __tabs-item--aria-label="Limits"
                __tabs-item--text="Limits"
                __tabs-item--attribute=(concat 'aria-controls="' tabs--id '-limits-panel"')}}
              {{> __tabs-item
                __tabs-item--id="cluster-installation"
                __tabs-item--aria-label="Cluster installation"
                __tabs-item--text="Cluster installation"
                __tabs-item--attribute=(concat 'aria-controls="' tabs--id '-cluster-installation-panel"')}}
              {{> __tabs-item
                __tabs-item--id="subscription-support"
                __tabs-item--aria-label="Subscription and support"
                __tabs-item--text="Subscription and support"
                __tabs-item--attribute=(concat 'aria-controls="' tabs--id '-subscription-support-panel"')}}
            {{/tabs-list}}
          {{/tabs}}
        {{/sidebar-panel}}
        {{#> sidebar-content}}
          {{#> tab-content tab-content--IsActive="true" tab-content--attribute=(concat 'aria-labelledby="' tab-content--id '-installation-link" id="' tab-content--id '-installation-panel"') tab-content-body--modifier="pf-m-padding"}}
            {{#> content}}
              <h2>Installation content</h2>
              <p>This document is a guide for preparing a new AWS account for use with OpenShift. It will help prepare an account to create a single cluster and provide insight for adjustments which may be needed for additional cluster.</p>
              <p>Follow along with these steps and the links below to configure your AWS account and provision on OpenShift cluster.</p>
              <ol>
                <li>
                  <a href="#">
                    Limits
                  </a>
                </li>
                <li>
                  <a href="#">
                    Cluster installation
                  </a>
                </li>
                <li>
                  <a href="#">
                    Subscription and support
                  </a>
                </li>
              </ol>
            {{/content}}
          {{/tab-content}}
          {{#> tab-content tab-content--attribute=(concat 'aria-labelledby="' tab-content--id '-limits-link" id="' tab-content--id '-limits-panel"') tab-content-body--modifier="pf-m-padding"}}
            {{#> content}}
              <p>Limits content</p>
            {{/content}}
          {{/tab-content}}
          {{#> tab-content tab-content--attribute=(concat 'aria-labelledby="' tab-content--id '-cluster-installation-link" id="' tab-content--id '-cluster-installation-panel"') tab-content-body--modifier="pf-m-padding"}}
            {{#> content}}
              <p>Cluster installation content</p>
            {{/content}}
          {{/tab-content}}
          {{#> tab-content tab-content--attribute=(concat 'aria-labelledby="' tab-content--id '-subscription-support-link" id="' tab-content--id '-subscription-support-panel"') tab-content-body--modifier="pf-m-padding"}}
            {{#> content}}
              <p>Subscription and support content</p>
            {{/content}}
          {{/tab-content}}
        {{/sidebar-content}}
      {{/sidebar}}
    {{/card}}
  {{/page-main-section}}
{{/tabs--page-wrapper}}
``` -->
