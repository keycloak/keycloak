---
id: 'Card view'
section: demos
---## Examples

### Card view

```html isFullscreen
<div class="pf-c-page" id="card-view-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-card-view-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="card-view-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="card-view-example-primary-nav"
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
              id="card-view-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="card-view-example-dropdown-kebab-1-button"
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
              id="card-view-example-dropdown-kebab-2-button"
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
              aria-labelledby="card-view-example-dropdown-kebab-2-button"
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
        id="card-view-example-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item">
            <a
              href="#"
              class="pf-c-nav__link pf-m-current"
              aria-current="page"
            >System Panel</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Policy</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Authentication</a>
          </li>
          <li class="pf-c-nav__item">
            <a href="#" class="pf-c-nav__link">Network Services</a>
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
    id="main-content-card-view-example"
  >
    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Projects</h1>
        <p>This is a demo that showcases PatternFly cards.</p>
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
              id="pagination-options-menu-bottom-example-toggle"
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
            aria-labelledby="pagination-options-menu-bottom-example-toggle"
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

## Documentation
