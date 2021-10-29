---
id: Primary-detail
section: demos
wrapperTag: div
---## Demos

### Primary-detail expanded

```html isFullscreen
<div class="pf-c-page" id="primary-detail-expanded-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-primary-detail-expanded-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="primary-detail-expanded-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="primary-detail-expanded-example-primary-nav"
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
              id="primary-detail-expanded-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="primary-detail-expanded-example-dropdown-kebab-1-button"
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
              id="primary-detail-expanded-example-dropdown-kebab-2-button"
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
              aria-labelledby="primary-detail-expanded-example-dropdown-kebab-2-button"
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
        id="primary-detail-expanded-example-primary-nav"
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

    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Primary-detail expanded</h1>
        <p>Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5</p>
      </div>
    </section>

    <div class="pf-c-divider" role="separator"></div>
    <section class="pf-c-page__main-section pf-m-no-padding">
      <!-- Drawer -->
      <div class="pf-c-drawer pf-m-expanded pf-m-inline-on-2xl">
        <div class="pf-c-drawer__main">
          <!-- Content -->
          <div class="pf-c-drawer__content">
            <div class="pf-c-drawer__body">
              <div
                class="pf-c-toolbar pf-m-page-insets"
                id="primary-detail-expanded-example-toolbar"
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
                          aria-controls="primary-detail-expanded-example-toolbar-expandable-content"
                        >
                          <i class="fas fa-filter" aria-hidden="true"></i>
                        </button>
                      </div>

                      <div class="pf-c-toolbar__item pf-m-search-filter">
                        <div
                          class="pf-c-input-group"
                          aria-label="search filter"
                          role="group"
                        >
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle"
                              id="primary-detail-expanded-example-toolbar--button"
                              aria-expanded="false"
                              type="button"
                            >
                              <span class="pf-c-dropdown__toggle-text">Name</span>
                              <span class="pf-c-dropdown__toggle-icon">
                                <i class="fas fa-caret-down" aria-hidden="true"></i>
                              </span>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu"
                              aria-labelledby="primary-detail-expanded-example-toolbar--button"
                              hidden
                            >
                              <li>
                                <a
                                  class="pf-c-dropdown__menu-item"
                                  href="#"
                                >Link</a>
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
                                <a
                                  class="pf-c-dropdown__menu-item"
                                  href="#"
                                >Separated link</a>
                              </li>
                            </ul>
                          </div>
                          <input
                            class="pf-c-form-control"
                            type="search"
                            id="primary-detail-expanded-example-toolbar--search-filter-input"
                            name="primary-detail-expanded-example-toolbar-search-filter-input"
                            aria-label="input with dropdown and button"
                            aria-describedby="primary-detail-expanded-example-toolbar--button"
                          />
                        </div>
                      </div>

                      <div class="pf-c-toolbar__group pf-m-filter-group">
                        <div class="pf-c-toolbar__item">
                          <div class="pf-c-select">
                            <span
                              id="primary-detail-expanded-example-toolbar-select-checkbox-status-label"
                              hidden
                            >Choose one</span>

                            <button
                              class="pf-c-select__toggle"
                              type="button"
                              id="primary-detail-expanded-example-toolbar-select-checkbox-status-toggle"
                              aria-haspopup="true"
                              aria-expanded="false"
                              aria-labelledby="primary-detail-expanded-example-toolbar-select-checkbox-status-label primary-detail-expanded-example-toolbar-select-checkbox-status-toggle"
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
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-status-active"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-status-active"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-status-active"
                                  />

                                  <span class="pf-c-check__label">Active</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a description</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-status-canceled"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-status-canceled"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-status-canceled"
                                  />

                                  <span class="pf-c-check__label">Canceled</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-status-paused"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-status-paused"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-status-paused"
                                  />

                                  <span class="pf-c-check__label">Paused</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-status-warning"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-status-warning"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-status-warning"
                                  />

                                  <span class="pf-c-check__label">Warning</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-status-restarted"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-status-restarted"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-status-restarted"
                                  />

                                  <span class="pf-c-check__label">Restarted</span>
                                </label>
                              </fieldset>
                            </div>
                          </div>
                        </div>
                        <div class="pf-c-toolbar__item">
                          <div class="pf-c-select">
                            <span
                              id="primary-detail-expanded-example-toolbar-select-checkbox-risk-label"
                              hidden
                            >Choose one</span>

                            <button
                              class="pf-c-select__toggle"
                              type="button"
                              id="primary-detail-expanded-example-toolbar-select-checkbox-risk-toggle"
                              aria-haspopup="true"
                              aria-expanded="false"
                              aria-labelledby="primary-detail-expanded-example-toolbar-select-checkbox-risk-label primary-detail-expanded-example-toolbar-select-checkbox-risk-toggle"
                            >
                              <div class="pf-c-select__toggle-wrapper">
                                <span class="pf-c-select__toggle-text">Risk</span>
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
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-risk-active"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-risk-active"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-risk-active"
                                  />

                                  <span class="pf-c-check__label">Active</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a description</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-risk-canceled"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-risk-canceled"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-risk-canceled"
                                  />

                                  <span class="pf-c-check__label">Canceled</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-risk-paused"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-risk-paused"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-risk-paused"
                                  />

                                  <span class="pf-c-check__label">Paused</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-risk-warning"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-risk-warning"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-risk-warning"
                                  />

                                  <span class="pf-c-check__label">Warning</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-expanded-example-toolbar-select-checkbox-risk-restarted"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-expanded-example-toolbar-select-checkbox-risk-restarted"
                                    name="primary-detail-expanded-example-toolbar-select-checkbox-risk-restarted"
                                  />

                                  <span class="pf-c-check__label">Restarted</span>
                                </label>
                              </fieldset>
                            </div>
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
                        <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
                      </button>
                    </div>

                    <div
                      class="pf-c-overflow-menu"
                      id="primary-detail-expanded-example-toolbar-overflow-menu"
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
                            >Create instance</button>
                          </div>
                        </div>
                      </div>
                      <div class="pf-c-overflow-menu__control">
                        <div class="pf-c-dropdown">
                          <button
                            class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                            type="button"
                            id="primary-detail-expanded-example-toolbar-overflow-menu-dropdown-toggle"
                            aria-label="Dropdown with additional options"
                            aria-expanded="false"
                          >
                            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                          </button>
                          <ul
                            class="pf-c-dropdown__menu"
                            aria-labelledby="primary-detail-expanded-example-toolbar-overflow-menu-dropdown-toggle"
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
                          <div
                            class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                          >
                            <span class="pf-c-options-menu__toggle-text">
                              <b>1 - 10</b>&nbsp;of&nbsp;
                              <b>36</b>
                            </span>
                            <button
                              class="pf-c-options-menu__toggle-button"
                              id="primary-detail-expanded-example-toolbar-top-pagination-toggle"
                              aria-haspopup="listbox"
                              aria-expanded="false"
                              aria-label="Items per page"
                            >
                              <span
                                class="pf-c-options-menu__toggle-button-icon"
                              >
                                <i class="fas fa-caret-down" aria-hidden="true"></i>
                              </span>
                            </button>
                          </div>
                          <ul
                            class="pf-c-options-menu__menu"
                            aria-labelledby="primary-detail-expanded-example-toolbar-top-pagination-toggle"
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
                    id="primary-detail-expanded-example-toolbar-expandable-content"
                    hidden
                  ></div>
                </div>
              </div>
              <ul
                class="pf-c-data-list"
                role="list"
                aria-label="Simple data list example"
                id="primary-detail-expanded-example-data-list"
              >
                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-expanded-example-data-list-item-1"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-expanded-example-data-list-item-1"
                              >patternfly</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>
                                Working repo for PatternFly 4
                                <a href>https://pf4.patternfly.org/</a>
                              </small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>10</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-expanded-example-data-list-item-2"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-expanded-example-data-list-item-2"
                              >patternfly-elements</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>PatternFly elements</small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>9</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>2</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-check-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>11</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-exclamation-triangle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-times-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>1</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-expanded-example-data-list-item-3"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <p
                          id="primary-detail-expanded-example-data-list-item-3"
                        >patternfly-unified-design-kit</p>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-expanded-example-data-list-item-4"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-expanded-example-data-list-item-4"
                              >patternfly</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>
                                Working repo for PatternFly 4
                                <a href>https://pf4.patternfly.org/</a>
                              </small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>10</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-expanded-example-data-list-item-5"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-expanded-example-data-list-item-5"
                              >patternfly-elements</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>PatternFly elements</small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>9</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>2</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-check-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>11</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-exclamation-triangle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-times-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>1</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- Panel -->
          <div class="pf-c-drawer__panel">
            <!-- Panel header -->
            <div class="pf-c-drawer__body">
              <div class="pf-l-flex pf-m-column">
                <div class="pf-l-flex__item">
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
                    <h2
                      class="pf-c-title pf-m-lg"
                      id="primary-detail-expanded-example-drawer-label"
                    >Node 2</h2>
                  </div>
                </div>
                <div class="pf-l-flex__item">
                  <a href="#">siemur/test-space</a>
                </div>
              </div>
            </div>

            <!-- Tabs -->
            <div class="pf-c-drawer__body pf-m-no-padding">
              <div
                class="pf-c-tabs pf-m-box pf-m-fill"
                id="primary-detail-expanded-example-tabs"
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
                      aria-controls="primary-detail-expanded-example-tabs-tab1-panel"
                      id="primary-detail-expanded-example-tabs-tab1-link"
                    >
                      <span class="pf-c-tabs__item-text">Overview</span>
                    </button>
                  </li>
                  <li class="pf-c-tabs__item">
                    <button
                      class="pf-c-tabs__link"
                      aria-controls="primary-detail-expanded-example-tabs-tab2-panel"
                      id="primary-detail-expanded-example-tabs-tab2-link"
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

            <!-- Tab content -->
            <div class="pf-c-drawer__body">
              <section
                class="pf-c-tab-content"
                id="primary-detail-expanded-example-tabs-tab1-panel"
                aria-labelledby="primary-detail-expanded-example-tabs-tab1-link"
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
                        id="primary-detail-expanded-example-progress-example1"
                      >
                        <div
                          class="pf-c-progress__description"
                          id="primary-detail-expanded-example-progress-example1-description"
                        >Capacity</div>
                        <div class="pf-c-progress__status" aria-hidden="true">
                          <span class="pf-c-progress__measure">33%</span>
                        </div>
                        <div
                          class="pf-c-progress__bar"
                          role="progressbar"
                          aria-valuemin="0"
                          aria-valuemax="100"
                          aria-valuenow="33"
                          aria-labelledby="primary-detail-expanded-example-progress-example1-description"
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
                        id="primary-detail-expanded-example-progress-example2"
                      >
                        <div
                          class="pf-c-progress__description"
                          id="primary-detail-expanded-example-progress-example2-description"
                        >Modules</div>
                        <div class="pf-c-progress__status" aria-hidden="true">
                          <span class="pf-c-progress__measure">66%</span>
                        </div>
                        <div
                          class="pf-c-progress__bar"
                          role="progressbar"
                          aria-valuemin="0"
                          aria-valuemax="100"
                          aria-valuenow="66"
                          aria-labelledby="primary-detail-expanded-example-progress-example2-description"
                          aria-label="Progress 2"
                        >
                          <div
                            class="pf-c-progress__indicator"
                            style="width:66%;"
                          ></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </section>
              <section
                class="pf-c-tab-content"
                id="primary-detail-expanded-example-tabs-tab2-panel"
                aria-labelledby="primary-detail-expanded-example-tabs-tab2-link"
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
    </section>
  </main>
</div>

```

### Primary-detail collapsed

```html isFullscreen
<div class="pf-c-page" id="primary-detail-collapsed-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-primary-detail-collapsed-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="primary-detail-collapsed-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="primary-detail-collapsed-example-primary-nav"
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
              id="primary-detail-collapsed-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="primary-detail-collapsed-example-dropdown-kebab-1-button"
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
              id="primary-detail-collapsed-example-dropdown-kebab-2-button"
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
              aria-labelledby="primary-detail-collapsed-example-dropdown-kebab-2-button"
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
        id="primary-detail-collapsed-example-primary-nav"
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

    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Primary-detail collapsed</h1>
        <p>Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5</p>
      </div>
    </section>

    <div class="pf-c-divider" role="separator"></div>
    <section class="pf-c-page__main-section pf-m-no-padding">
      <!-- Drawer -->
      <div class="pf-c-drawer pf-m-inline-on-2xl">
        <div class="pf-c-drawer__main">
          <!-- Content -->
          <div class="pf-c-drawer__content">
            <div class="pf-c-drawer__body">
              <div
                class="pf-c-toolbar pf-m-page-insets"
                id="primary-detail-collapsed-example-toolbar"
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
                          aria-controls="primary-detail-collapsed-example-toolbar-expandable-content"
                        >
                          <i class="fas fa-filter" aria-hidden="true"></i>
                        </button>
                      </div>

                      <div class="pf-c-toolbar__item pf-m-search-filter">
                        <div
                          class="pf-c-input-group"
                          aria-label="search filter"
                          role="group"
                        >
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle"
                              id="primary-detail-collapsed-example-toolbar--button"
                              aria-expanded="false"
                              type="button"
                            >
                              <span class="pf-c-dropdown__toggle-text">Name</span>
                              <span class="pf-c-dropdown__toggle-icon">
                                <i class="fas fa-caret-down" aria-hidden="true"></i>
                              </span>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu"
                              aria-labelledby="primary-detail-collapsed-example-toolbar--button"
                              hidden
                            >
                              <li>
                                <a
                                  class="pf-c-dropdown__menu-item"
                                  href="#"
                                >Link</a>
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
                                <a
                                  class="pf-c-dropdown__menu-item"
                                  href="#"
                                >Separated link</a>
                              </li>
                            </ul>
                          </div>
                          <input
                            class="pf-c-form-control"
                            type="search"
                            id="primary-detail-collapsed-example-toolbar--search-filter-input"
                            name="primary-detail-collapsed-example-toolbar-search-filter-input"
                            aria-label="input with dropdown and button"
                            aria-describedby="primary-detail-collapsed-example-toolbar--button"
                          />
                        </div>
                      </div>

                      <div class="pf-c-toolbar__group pf-m-filter-group">
                        <div class="pf-c-toolbar__item">
                          <div class="pf-c-select">
                            <span
                              id="primary-detail-collapsed-example-toolbar-select-checkbox-status-label"
                              hidden
                            >Choose one</span>

                            <button
                              class="pf-c-select__toggle"
                              type="button"
                              id="primary-detail-collapsed-example-toolbar-select-checkbox-status-toggle"
                              aria-haspopup="true"
                              aria-expanded="false"
                              aria-labelledby="primary-detail-collapsed-example-toolbar-select-checkbox-status-label primary-detail-collapsed-example-toolbar-select-checkbox-status-toggle"
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
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-status-active"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-status-active"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-status-active"
                                  />

                                  <span class="pf-c-check__label">Active</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a description</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-status-canceled"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-status-canceled"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-status-canceled"
                                  />

                                  <span class="pf-c-check__label">Canceled</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-status-paused"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-status-paused"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-status-paused"
                                  />

                                  <span class="pf-c-check__label">Paused</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-status-warning"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-status-warning"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-status-warning"
                                  />

                                  <span class="pf-c-check__label">Warning</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-status-restarted"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-status-restarted"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-status-restarted"
                                  />

                                  <span class="pf-c-check__label">Restarted</span>
                                </label>
                              </fieldset>
                            </div>
                          </div>
                        </div>
                        <div class="pf-c-toolbar__item">
                          <div class="pf-c-select">
                            <span
                              id="primary-detail-collapsed-example-toolbar-select-checkbox-risk-label"
                              hidden
                            >Choose one</span>

                            <button
                              class="pf-c-select__toggle"
                              type="button"
                              id="primary-detail-collapsed-example-toolbar-select-checkbox-risk-toggle"
                              aria-haspopup="true"
                              aria-expanded="false"
                              aria-labelledby="primary-detail-collapsed-example-toolbar-select-checkbox-risk-label primary-detail-collapsed-example-toolbar-select-checkbox-risk-toggle"
                            >
                              <div class="pf-c-select__toggle-wrapper">
                                <span class="pf-c-select__toggle-text">Risk</span>
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
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-risk-active"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-risk-active"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-risk-active"
                                  />

                                  <span class="pf-c-check__label">Active</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a description</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-risk-canceled"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-risk-canceled"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-risk-canceled"
                                  />

                                  <span class="pf-c-check__label">Canceled</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-risk-paused"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-risk-paused"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-risk-paused"
                                  />

                                  <span class="pf-c-check__label">Paused</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-risk-warning"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-risk-warning"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-risk-warning"
                                  />

                                  <span class="pf-c-check__label">Warning</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-collapsed-example-toolbar-select-checkbox-risk-restarted"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-collapsed-example-toolbar-select-checkbox-risk-restarted"
                                    name="primary-detail-collapsed-example-toolbar-select-checkbox-risk-restarted"
                                  />

                                  <span class="pf-c-check__label">Restarted</span>
                                </label>
                              </fieldset>
                            </div>
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
                        <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
                      </button>
                    </div>

                    <div
                      class="pf-c-overflow-menu"
                      id="primary-detail-collapsed-example-toolbar-overflow-menu"
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
                            >Create instance</button>
                          </div>
                        </div>
                      </div>
                      <div class="pf-c-overflow-menu__control">
                        <div class="pf-c-dropdown">
                          <button
                            class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                            type="button"
                            id="primary-detail-collapsed-example-toolbar-overflow-menu-dropdown-toggle"
                            aria-label="Dropdown with additional options"
                            aria-expanded="false"
                          >
                            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                          </button>
                          <ul
                            class="pf-c-dropdown__menu"
                            aria-labelledby="primary-detail-collapsed-example-toolbar-overflow-menu-dropdown-toggle"
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
                          <div
                            class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                          >
                            <span class="pf-c-options-menu__toggle-text">
                              <b>1 - 10</b>&nbsp;of&nbsp;
                              <b>36</b>
                            </span>
                            <button
                              class="pf-c-options-menu__toggle-button"
                              id="primary-detail-collapsed-example-toolbar-top-pagination-toggle"
                              aria-haspopup="listbox"
                              aria-expanded="false"
                              aria-label="Items per page"
                            >
                              <span
                                class="pf-c-options-menu__toggle-button-icon"
                              >
                                <i class="fas fa-caret-down" aria-hidden="true"></i>
                              </span>
                            </button>
                          </div>
                          <ul
                            class="pf-c-options-menu__menu"
                            aria-labelledby="primary-detail-collapsed-example-toolbar-top-pagination-toggle"
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
                    id="primary-detail-collapsed-example-toolbar-expandable-content"
                    hidden
                  ></div>
                </div>
              </div>
              <ul
                class="pf-c-data-list"
                role="list"
                aria-label="Simple data list example"
                id="primary-detail-collapsed-example-data-list"
              >
                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-collapsed-example-data-list-item-1"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-collapsed-example-data-list-item-1"
                              >patternfly</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>
                                Working repo for PatternFly 4
                                <a href>https://pf4.patternfly.org/</a>
                              </small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>10</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-collapsed-example-data-list-item-2"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-collapsed-example-data-list-item-2"
                              >patternfly-elements</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>PatternFly elements</small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>9</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>2</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-check-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>11</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-exclamation-triangle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-times-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>1</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-collapsed-example-data-list-item-3"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <p
                          id="primary-detail-collapsed-example-data-list-item-3"
                        >patternfly-unified-design-kit</p>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-collapsed-example-data-list-item-4"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-collapsed-example-data-list-item-4"
                              >patternfly</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>
                                Working repo for PatternFly 4
                                <a href>https://pf4.patternfly.org/</a>
                              </small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>10</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-collapsed-example-data-list-item-5"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-collapsed-example-data-list-item-5"
                              >patternfly-elements</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>PatternFly elements</small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>9</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>2</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-check-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>11</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-exclamation-triangle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-times-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>1</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- Panel -->
          <div class="pf-c-drawer__panel" hidden>
            <!-- Content header -->
            <div class="pf-c-drawer__body">
              <div class="pf-l-flex pf-m-column">
                <div class="pf-l-flex__item">
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
                    <h2
                      class="pf-c-title pf-m-lg"
                      id="primary-detail-collapsed-example-drawer-label"
                    >Node 2</h2>
                  </div>
                </div>
                <div class="pf-l-flex__item">
                  <a href="#">siemur/test-space</a>
                </div>
              </div>
            </div>

            <!-- Tabs -->
            <div class="pf-c-drawer__body pf-m-no-padding">
              <div
                class="pf-c-tabs pf-m-box pf-m-fill"
                id="primary-detail-collapsed-example-tabs"
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
                      aria-controls="primary-detail-collapsed-example-tabs-tab1-panel"
                      id="primary-detail-collapsed-example-tabs-tab1-link"
                    >
                      <span class="pf-c-tabs__item-text">Overview</span>
                    </button>
                  </li>
                  <li class="pf-c-tabs__item">
                    <button
                      class="pf-c-tabs__link"
                      aria-controls="primary-detail-collapsed-example-tabs-tab2-panel"
                      id="primary-detail-collapsed-example-tabs-tab2-link"
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

            <!-- Tab content -->
            <div class="pf-c-drawer__body">
              <section
                class="pf-c-tab-content"
                id="primary-detail-collapsed-example-tabs-tab1-panel"
                aria-labelledby="primary-detail-collapsed-example-tabs-tab1-link"
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
                        id="primary-detail-collapsed-example-progress-example1"
                      >
                        <div
                          class="pf-c-progress__description"
                          id="primary-detail-collapsed-example-progress-example1-description"
                        >Capacity</div>
                        <div class="pf-c-progress__status" aria-hidden="true">
                          <span class="pf-c-progress__measure">33%</span>
                        </div>
                        <div
                          class="pf-c-progress__bar"
                          role="progressbar"
                          aria-valuemin="0"
                          aria-valuemax="100"
                          aria-valuenow="33"
                          aria-labelledby="primary-detail-collapsed-example-progress-example1-description"
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
                        id="primary-detail-collapsed-example-progress-example2"
                      >
                        <div
                          class="pf-c-progress__description"
                          id="primary-detail-collapsed-example-progress-example2-description"
                        >Modules</div>
                        <div class="pf-c-progress__status" aria-hidden="true">
                          <span class="pf-c-progress__measure">66%</span>
                        </div>
                        <div
                          class="pf-c-progress__bar"
                          role="progressbar"
                          aria-valuemin="0"
                          aria-valuemax="100"
                          aria-valuenow="66"
                          aria-labelledby="primary-detail-collapsed-example-progress-example2-description"
                          aria-label="Progress 2"
                        >
                          <div
                            class="pf-c-progress__indicator"
                            style="width:66%;"
                          ></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </section>
              <section
                class="pf-c-tab-content"
                id="primary-detail-collapsed-example-tabs-tab2-panel"
                aria-labelledby="primary-detail-collapsed-example-tabs-tab2-link"
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
    </section>
  </main>
</div>

```

### Primary-detail content body padding

```html isFullscreen
<div class="pf-c-page" id="primary-detail-panel-body-padding">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-primary-detail-panel-body-padding"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="primary-detail-panel-body-padding-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="primary-detail-panel-body-padding-primary-nav"
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
              id="primary-detail-panel-body-padding-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="primary-detail-panel-body-padding-dropdown-kebab-1-button"
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
              id="primary-detail-panel-body-padding-dropdown-kebab-2-button"
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
              aria-labelledby="primary-detail-panel-body-padding-dropdown-kebab-2-button"
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
        id="primary-detail-panel-body-padding-primary-nav"
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

    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Padded content example</h1>
        <p>Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5</p>
      </div>
    </section>

    <div class="pf-c-divider" role="separator"></div>
    <section class="pf-c-page__main-section pf-m-no-padding">
      <!-- Drawer -->
      <div class="pf-c-drawer pf-m-expanded pf-m-inline-on-2xl">
        <div class="pf-c-drawer__main">
          <!-- Content -->
          <div class="pf-c-drawer__content pf-m-no-background">
            <div class="pf-c-drawer__body pf-m-padding">
              <div
                class="pf-c-toolbar pf-m-page-insets"
                id="primary-detail-panel-body-padding-toolbar"
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
                          aria-controls="primary-detail-panel-body-padding-toolbar-expandable-content"
                        >
                          <i class="fas fa-filter" aria-hidden="true"></i>
                        </button>
                      </div>

                      <div class="pf-c-toolbar__group pf-m-filter-group">
                        <div class="pf-c-toolbar__item">
                          <div class="pf-c-select">
                            <span
                              id="primary-detail-panel-body-padding-toolbar-select-checkbox-status-label"
                              hidden
                            >Choose one</span>

                            <button
                              class="pf-c-select__toggle"
                              type="button"
                              id="primary-detail-panel-body-padding-toolbar-select-checkbox-status-toggle"
                              aria-haspopup="true"
                              aria-expanded="false"
                              aria-labelledby="primary-detail-panel-body-padding-toolbar-select-checkbox-status-label primary-detail-panel-body-padding-toolbar-select-checkbox-status-toggle"
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
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-status-active"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-status-active"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-status-active"
                                  />

                                  <span class="pf-c-check__label">Active</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a description</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-status-canceled"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-status-canceled"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-status-canceled"
                                  />

                                  <span class="pf-c-check__label">Canceled</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-status-paused"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-status-paused"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-status-paused"
                                  />

                                  <span class="pf-c-check__label">Paused</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-status-warning"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-status-warning"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-status-warning"
                                  />

                                  <span class="pf-c-check__label">Warning</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-status-restarted"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-status-restarted"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-status-restarted"
                                  />

                                  <span class="pf-c-check__label">Restarted</span>
                                </label>
                              </fieldset>
                            </div>
                          </div>
                        </div>
                        <div class="pf-c-toolbar__item">
                          <div class="pf-c-select">
                            <span
                              id="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-label"
                              hidden
                            >Choose one</span>

                            <button
                              class="pf-c-select__toggle"
                              type="button"
                              id="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-toggle"
                              aria-haspopup="true"
                              aria-expanded="false"
                              aria-labelledby="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-label primary-detail-panel-body-padding-toolbar-select-checkbox-risk-toggle"
                            >
                              <div class="pf-c-select__toggle-wrapper">
                                <span class="pf-c-select__toggle-text">Risk</span>
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
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-active"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-active"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-active"
                                  />

                                  <span class="pf-c-check__label">Active</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a description</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item pf-m-description"
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-canceled"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-canceled"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-canceled"
                                  />

                                  <span class="pf-c-check__label">Canceled</span>
                                  <span
                                    class="pf-c-check__description"
                                  >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-paused"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-paused"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-paused"
                                  />

                                  <span class="pf-c-check__label">Paused</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-warning"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-warning"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-warning"
                                  />

                                  <span class="pf-c-check__label">Warning</span>
                                </label>
                                <label
                                  class="pf-c-check pf-c-select__menu-item"
                                  for="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-restarted"
                                >
                                  <input
                                    class="pf-c-check__input"
                                    type="checkbox"
                                    id="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-restarted"
                                    name="primary-detail-panel-body-padding-toolbar-select-checkbox-risk-restarted"
                                  />

                                  <span class="pf-c-check__label">Restarted</span>
                                </label>
                              </fieldset>
                            </div>
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
                        <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
                      </button>
                    </div>

                    <div
                      class="pf-c-overflow-menu"
                      id="primary-detail-panel-body-padding-toolbar-overflow-menu"
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
                            >Create instance</button>
                          </div>
                        </div>
                      </div>
                      <div class="pf-c-overflow-menu__control">
                        <div class="pf-c-dropdown">
                          <button
                            class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                            type="button"
                            id="primary-detail-panel-body-padding-toolbar-overflow-menu-dropdown-toggle"
                            aria-label="Dropdown with additional options"
                            aria-expanded="false"
                          >
                            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                          </button>
                          <ul
                            class="pf-c-dropdown__menu"
                            aria-labelledby="primary-detail-panel-body-padding-toolbar-overflow-menu-dropdown-toggle"
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
                          <div
                            class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                          >
                            <span class="pf-c-options-menu__toggle-text">
                              <b>1 - 10</b>&nbsp;of&nbsp;
                              <b>36</b>
                            </span>
                            <button
                              class="pf-c-options-menu__toggle-button"
                              id="primary-detail-panel-body-padding-toolbar-top-pagination-toggle"
                              aria-haspopup="listbox"
                              aria-expanded="false"
                              aria-label="Items per page"
                            >
                              <span
                                class="pf-c-options-menu__toggle-button-icon"
                              >
                                <i class="fas fa-caret-down" aria-hidden="true"></i>
                              </span>
                            </button>
                          </div>
                          <ul
                            class="pf-c-options-menu__menu"
                            aria-labelledby="primary-detail-panel-body-padding-toolbar-top-pagination-toggle"
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
                    id="primary-detail-panel-body-padding-toolbar-expandable-content"
                    hidden
                  ></div>
                </div>
              </div>
              <ul
                class="pf-c-data-list"
                role="list"
                aria-label="Simple data list example"
                id="primary-detail-panel-body-padding-data-list"
              >
                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-panel-body-padding-data-list-item-1"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-panel-body-padding-data-list-item-1"
                              >patternfly</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>
                                Working repo for PatternFly 4
                                <a href>https://pf4.patternfly.org/</a>
                              </small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>10</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-panel-body-padding-data-list-item-2"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-panel-body-padding-data-list-item-2"
                              >patternfly-elements</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>PatternFly elements</small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>9</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>2</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-check-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>11</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-exclamation-triangle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-times-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>1</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-panel-body-padding-data-list-item-3"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <p
                          id="primary-detail-panel-body-padding-data-list-item-3"
                        >patternfly-unified-design-kit</p>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-panel-body-padding-data-list-item-4"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-panel-body-padding-data-list-item-4"
                              >patternfly</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>
                                Working repo for PatternFly 4
                                <a href>https://pf4.patternfly.org/</a>
                              </small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>10</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>

                <li
                  class="pf-c-data-list__item"
                  aria-labelledby="primary-detail-panel-body-padding-data-list-item-5"
                >
                  <div class="pf-c-data-list__item-row">
                    <div class="pf-c-data-list__item-content">
                      <div class="pf-c-data-list__cell pf-m-align-left">
                        <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                          <div
                            class="pf-l-flex pf-m-column pf-m-space-items-none"
                          >
                            <div class="pf-l-flex__item">
                              <p
                                id="primary-detail-panel-body-padding-data-list-item-5"
                              >patternfly-elements</p>
                            </div>
                            <div class="pf-l-flex__item">
                              <small>PatternFly elements</small>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-wrap">
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-code-branch"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>5</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-code" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>9</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i class="fas fa-cube" aria-hidden="true"></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>2</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-check-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>11</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-exclamation-triangle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>4</span>
                              </div>
                            </div>
                            <div class="pf-l-flex pf-m-space-items-xs">
                              <div class="pf-l-flex__item">
                                <i
                                  class="fas fa-times-circle"
                                  aria-hidden="true"
                                ></i>
                              </div>
                              <div class="pf-l-flex__item">
                                <span>1</span>
                              </div>
                            </div>
                            <div class="pf-l-flex__item">Updated 2 days ago</div>
                          </div>
                        </div>
                      </div>
                      <div
                        class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                      >
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                        <button class="pf-c-button pf-m-link" type="button">Link</button>
                      </div>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- Panel -->
          <div class="pf-c-drawer__panel">
            <!-- Panel header -->
            <div class="pf-c-drawer__body">
              <div class="pf-l-flex pf-m-column">
                <div class="pf-l-flex__item">
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
                    <h2
                      class="pf-c-title pf-m-lg"
                      id="primary-detail-panel-body-padding-drawer-label"
                    >Patternfly-elements</h2>
                  </div>
                </div>
                <div class="pf-l-flex__item">PatternFly elements</div>
              </div>
            </div>

            <!-- Tab content -->
            <div class="pf-c-drawer__body">
              <div class="pf-l-flex pf-m-column pf-m-space-items-lg">
                <div class="pf-l-flex__item">
                  <p>The content of the drawer really is up to you. It could have form fields, definition lists, text lists, labels, charts, progress bars, etc. Spacing recommendation is 24px margins. You can put tabs in here, and can also make the drawer scrollable.</p>
                </div>
                <div class="pf-l-flex__item">
                  <div
                    class="pf-c-progress pf-m-sm"
                    id="primary-detail-panel-body-padding-progress-example1"
                  >
                    <div
                      class="pf-c-progress__description"
                      id="primary-detail-panel-body-padding-progress-example1-description"
                    >Capacity</div>
                    <div class="pf-c-progress__status" aria-hidden="true">
                      <span class="pf-c-progress__measure">33%</span>
                    </div>
                    <div
                      class="pf-c-progress__bar"
                      role="progressbar"
                      aria-valuemin="0"
                      aria-valuemax="100"
                      aria-valuenow="33"
                      aria-labelledby="primary-detail-panel-body-padding-progress-example1-description"
                      aria-label="Progress 1"
                    >
                      <div class="pf-c-progress__indicator" style="width:33%;"></div>
                    </div>
                  </div>
                </div>
                <div class="pf-l-flex__item">
                  <div
                    class="pf-c-progress pf-m-sm"
                    id="primary-detail-panel-body-padding-progress-example2"
                  >
                    <div
                      class="pf-c-progress__description"
                      id="primary-detail-panel-body-padding-progress-example2-description"
                    >Modules</div>
                    <div class="pf-c-progress__status" aria-hidden="true">
                      <span class="pf-c-progress__measure">66%</span>
                    </div>
                    <div
                      class="pf-c-progress__bar"
                      role="progressbar"
                      aria-valuemin="0"
                      aria-valuemax="100"
                      aria-valuenow="66"
                      aria-labelledby="primary-detail-panel-body-padding-progress-example2-description"
                      aria-label="Progress 2"
                    >
                      <div class="pf-c-progress__indicator" style="width:66%;"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Primary-detail card view expanded

```html isFullscreen
<div class="pf-c-page" id="primary-detail-card-view-expanded-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-primary-detail-card-view-expanded-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="primary-detail-card-view-expanded-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="primary-detail-card-view-expanded-example-primary-nav"
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
              id="primary-detail-card-view-expanded-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="primary-detail-card-view-expanded-example-dropdown-kebab-1-button"
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
              id="primary-detail-card-view-expanded-example-dropdown-kebab-2-button"
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
              aria-labelledby="primary-detail-card-view-expanded-example-dropdown-kebab-2-button"
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
        id="primary-detail-card-view-expanded-example-primary-nav"
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

    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Main title</h1>
        <p>Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5</p>
      </div>
    </section>

    <section class="pf-c-page__main-section pf-m-no-padding">
      <!-- Drawer -->
      <div class="pf-c-drawer pf-m-expanded pf-m-inline-on-2xl">
        <div class="pf-c-drawer__section">
          <div
            class="pf-c-toolbar pf-m-page-insets"
            id="primary-detail-card-view-expanded-example-toolbar"
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
                      aria-controls="primary-detail-card-view-expanded-example-toolbar-expandable-content"
                    >
                      <i class="fas fa-filter" aria-hidden="true"></i>
                    </button>
                  </div>

                  <div class="pf-c-toolbar__item pf-m-bulk-select">
                    <div class="pf-c-dropdown">
                      <div class="pf-c-dropdown__toggle pf-m-split-button">
                        <label
                          class="pf-c-dropdown__toggle-check"
                          for="primary-detail-card-view-expanded-example-toolbar-bulk-select-toggle-check"
                        >
                          <input
                            type="checkbox"
                            id="primary-detail-card-view-expanded-example-toolbar-bulk-select-toggle-check"
                            aria-label="Select all"
                          />
                        </label>

                        <button
                          class="pf-c-dropdown__toggle-button"
                          type="button"
                          aria-expanded="false"
                          id="primary-detail-card-view-expanded-example-toolbar-bulk-select-toggle-button"
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

                  <div class="pf-c-toolbar__item pf-m-search-filter">
                    <div
                      class="pf-c-input-group"
                      aria-label="search filter"
                      role="group"
                    >
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle"
                          id="primary-detail-card-view-expanded-example-toolbar--button"
                          aria-expanded="false"
                          type="button"
                        >
                          <span class="pf-c-dropdown__toggle-text">Name</span>
                          <span class="pf-c-dropdown__toggle-icon">
                            <i class="fas fa-caret-down" aria-hidden="true"></i>
                          </span>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu"
                          aria-labelledby="primary-detail-card-view-expanded-example-toolbar--button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <input
                        class="pf-c-form-control"
                        type="search"
                        id="primary-detail-card-view-expanded-example-toolbar--search-filter-input"
                        name="primary-detail-card-view-expanded-example-toolbar-search-filter-input"
                        aria-label="input with dropdown and button"
                        aria-describedby="primary-detail-card-view-expanded-example-toolbar--button"
                      />
                    </div>
                  </div>
                </div>

                <div class="pf-c-toolbar__item">
                  <button
                    class="pf-c-button pf-m-plain"
                    type="button"
                    aria-label="Sort"
                  >
                    <i class="fas fa-sort-amount-down" aria-hidden="true"></i>
                  </button>
                </div>

                <div
                  class="pf-c-overflow-menu"
                  id="primary-detail-card-view-expanded-example-toolbar-overflow-menu"
                >
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

                      <div class="pf-c-overflow-menu__item">
                        <button
                          class="pf-c-button pf-m-secondary"
                          type="button"
                        >Action</button>
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-overflow-menu__control">
                    <div class="pf-c-dropdown">
                      <button
                        class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                        type="button"
                        id="primary-detail-card-view-expanded-example-toolbar-overflow-menu-dropdown-toggle"
                        aria-label="Dropdown with additional options"
                        aria-expanded="false"
                      >
                        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                      </button>
                      <ul
                        class="pf-c-dropdown__menu"
                        aria-labelledby="primary-detail-card-view-expanded-example-toolbar-overflow-menu-dropdown-toggle"
                        hidden
                      >
                        <li>
                          <button class="pf-c-dropdown__menu-item">Action 7</button>
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>

                <div
                  class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right"
                >
                  <div class="pf-c-toolbar__item">
                    <button
                      class="pf-c-button pf-m-plain pf-m-active"
                      type="button"
                      aria-label="Grid view"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                  </div>
                  <div class="pf-c-toolbar__item">
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-label="List view"
                    >
                      <i class="fas fa-list-ul" aria-hidden="true"></i>
                    </button>
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
                          id="primary-detail-card-view-expanded-example-toolbar-top-pagination-toggle"
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
                        aria-labelledby="primary-detail-card-view-expanded-example-toolbar-top-pagination-toggle"
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
                id="primary-detail-card-view-expanded-example-toolbar-expandable-content"
                hidden
              ></div>
            </div>
          </div>
          <div class="pf-c-divider" role="separator"></div>
        </div>

        <div class="pf-c-drawer__main">
          <!-- Content -->
          <div class="pf-c-drawer__content pf-m-no-background">
            <div class="pf-c-drawer__body pf-m-padding">
              <div class="pf-l-gallery pf-m-gutter">
                <div
                  class="pf-c-card pf-m-selectable pf-m-selected"
                  id="primary-detail-card-view-expanded-example-card-1"
                >
                  <div class="pf-c-card__header">
                    <img
                      src="/assets/images/pf-logo-small.svg"
                      alt="PatternFly logo"
                    />
                    <div class="pf-c-card__actions">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="primary-detail-card-view-expanded-example-card-1-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-1-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-1-check"
                          name="primary-detail-card-view-expanded-example-card-1-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-1-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-1-check-label"
                    >Patternfly</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >PatternFly is a community project that promotes design commonality and improves user experience.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-2"
                >
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
                          id="primary-detail-card-view-expanded-example-card-2-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-2-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-2-check"
                          name="primary-detail-card-view-expanded-example-card-2-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-2-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-2-check-label"
                    >ActiveMQ</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >The ActiveMQ component allows messages to be sent to a JMS Queue or Topic; or messages to be consumed from a JMS Queue or Topic using Apache ActiveMQ.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-3"
                >
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
                          id="primary-detail-card-view-expanded-example-card-3-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-3-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-3-check"
                          name="primary-detail-card-view-expanded-example-card-3-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-3-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-3-check-label"
                    >Apache Spark</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >This documentation page covers the Apache Spark component for the Apache Camel.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-4"
                >
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
                          id="primary-detail-card-view-expanded-example-card-4-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-4-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-4-check"
                          name="primary-detail-card-view-expanded-example-card-4-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-4-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-4-check-label"
                    >Avro</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >This component provides a dataformat for avro, which allows serialization and deserialization of messages using Apache Avro’s binary dataformat. Moreover, it provides support for Apache Avro’s rpc, by providing producers and consumers endpoint for using avro over netty or http.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-5"
                >
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
                          id="primary-detail-card-view-expanded-example-card-5-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-5-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-5-check"
                          name="primary-detail-card-view-expanded-example-card-5-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-5-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-5-check-label"
                    >Azure Services</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >The Camel Components for Windows Azure Services provide connectivity to Azure services from Camel.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-6"
                >
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
                          id="primary-detail-card-view-expanded-example-card-6-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-6-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-6-check"
                          name="primary-detail-card-view-expanded-example-card-6-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-6-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-6-check-label"
                    >Crypto</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >For providing flexible endpoints to sign and verify exchanges using the Signature Service of the Java Cryptographic Extension.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-7"
                >
                  <div class="pf-c-card__header">
                    <img
                      src="/assets/images/pf-logo-small.svg"
                      alt="PatternFly logo"
                    />
                    <div class="pf-c-card__actions">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="primary-detail-card-view-expanded-example-card-7-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-7-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-7-check"
                          name="primary-detail-card-view-expanded-example-card-7-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-7-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-7-check-label"
                    >Patternfly</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >PatternFly is a community project that promotes design commonality and improves user experience.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-8"
                >
                  <div class="pf-c-card__header">
                    <img
                      src="/assets/images/pf-logo-small.svg"
                      alt="PatternFly logo"
                    />
                    <div class="pf-c-card__actions">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="primary-detail-card-view-expanded-example-card-8-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-8-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-8-check"
                          name="primary-detail-card-view-expanded-example-card-8-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-8-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-8-check-label"
                    >Patternfly</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >PatternFly is a community project that promotes design commonality and improves user experience.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-9"
                >
                  <div class="pf-c-card__header">
                    <img
                      src="/assets/images/pf-logo-small.svg"
                      alt="PatternFly logo"
                    />
                    <div class="pf-c-card__actions">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="primary-detail-card-view-expanded-example-card-9-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-9-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-9-check"
                          name="primary-detail-card-view-expanded-example-card-9-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-9-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-9-check-label"
                    >Patternfly</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >PatternFly is a community project that promotes design commonality and improves user experience.</div>
                </div>
                <div
                  class="pf-c-card pf-m-selectable"
                  id="primary-detail-card-view-expanded-example-card-10"
                >
                  <div class="pf-c-card__header">
                    <img
                      src="/assets/images/pf-logo-small.svg"
                      alt="PatternFly logo"
                    />
                    <div class="pf-c-card__actions">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="primary-detail-card-view-expanded-example-card-10-dropdown-kebab-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-10-dropdown-kebab-button"
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
                            <a
                              class="pf-c-dropdown__menu-item"
                              href="#"
                            >Separated link</a>
                          </li>
                        </ul>
                      </div>
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="primary-detail-card-view-expanded-example-card-10-check"
                          name="primary-detail-card-view-expanded-example-card-10-check"
                          aria-labelledby="primary-detail-card-view-expanded-example-card-10-check-label"
                        />
                      </div>
                    </div>
                  </div>
                  <div class="pf-c-card__title">
                    <p
                      id="primary-detail-card-view-expanded-example-card-10-check-label"
                    >Patternfly</p>
                    <div class="pf-c-content">
                      <small>Provided by Red Hat</small>
                    </div>
                  </div>
                  <div
                    class="pf-c-card__body"
                  >PatternFly is a community project that promotes design commonality and improves user experience.</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Panel -->
          <div class="pf-c-drawer__panel">
            <!-- Panel header -->
            <div class="pf-c-drawer__body">
              <div class="pf-l-flex pf-m-column">
                <div class="pf-l-flex__item">
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
                    <h2
                      class="pf-c-title pf-m-lg"
                      id="primary-detail-card-view-expanded-example-drawer-label"
                    >Patternfly</h2>
                  </div>
                </div>
                <div class="pf-l-flex__item">PatternFly elements</div>
              </div>
            </div>

            <div class="pf-c-drawer__body">
              <div class="pf-l-flex pf-m-column pf-m-space-items-lg">
                <div class="pf-l-flex__item">
                  <p>The content of the drawer really is up to you. It could have form fields, definition lists, text lists, labels, charts, progress bars, etc. Spacing recommendation is 24px margins. You can put tabs in here, and can also make the drawer scrollable.</p>
                </div>
                <div class="pf-l-flex__item">
                  <div
                    class="pf-c-progress"
                    id="primary-detail-card-view-expanded-example-progress-example1"
                  >
                    <div
                      class="pf-c-progress__description"
                      id="primary-detail-card-view-expanded-example-progress-example1-description"
                    >Capacity</div>
                    <div class="pf-c-progress__status" aria-hidden="true">
                      <span class="pf-c-progress__measure">33%</span>
                    </div>
                    <div
                      class="pf-c-progress__bar"
                      role="progressbar"
                      aria-valuemin="0"
                      aria-valuemax="100"
                      aria-valuenow="33"
                      aria-labelledby="primary-detail-card-view-expanded-example-progress-example1-description"
                      aria-label="Progress 1"
                    >
                      <div class="pf-c-progress__indicator" style="width:33%;"></div>
                    </div>
                  </div>
                </div>
                <div class="pf-l-flex__item">
                  <div
                    class="pf-c-progress"
                    id="primary-detail-card-view-expanded-example-progress-example2"
                  >
                    <div
                      class="pf-c-progress__description"
                      id="primary-detail-card-view-expanded-example-progress-example2-description"
                    >Modules</div>
                    <div class="pf-c-progress__status" aria-hidden="true">
                      <span class="pf-c-progress__measure">66%</span>
                    </div>
                    <div
                      class="pf-c-progress__bar"
                      role="progressbar"
                      aria-valuemin="0"
                      aria-valuemax="100"
                      aria-valuenow="66"
                      aria-labelledby="primary-detail-card-view-expanded-example-progress-example2-description"
                      aria-label="Progress 2"
                    >
                      <div class="pf-c-progress__indicator" style="width:66%;"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Primary-detail card simple list expanded on mobile

```html isFullscreen
<div class="pf-c-page" id="primary-detail-card-simple-list-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-primary-detail-card-simple-list-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="primary-detail-card-simple-list-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="primary-detail-card-simple-list-example-primary-nav"
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
              id="primary-detail-card-simple-list-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="primary-detail-card-simple-list-example-dropdown-kebab-1-button"
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
              id="primary-detail-card-simple-list-example-dropdown-kebab-2-button"
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
              aria-labelledby="primary-detail-card-simple-list-example-dropdown-kebab-2-button"
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
        id="primary-detail-card-simple-list-example-primary-nav"
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

    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Primary-detail, in card, simple list</h1>
        <p>Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5</p>
      </div>
    </section>

    <section class="pf-c-page__main-section">
      <div class="pf-c-card">
        <!-- Drawer -->
        <div class="pf-c-drawer pf-m-expanded pf-m-static">
          <div class="pf-c-drawer__main">
            <!-- Content -->
            <div class="pf-c-drawer__content">
              <div class="pf-c-simple-list">
                <section class="pf-c-simple-list__section">
                  <h2 class="pf-c-simple-list__title">Section title</h2>
                  <ul class="pf-c-simple-list__list">
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link pf-m-current"
                        type="button"
                      >List item 1</button>
                    </li>
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 2</button>
                    </li>
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 3</button>
                    </li>
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 4</button>
                    </li>
                  </ul>
                </section>
                <section class="pf-c-simple-list__section">
                  <h2 class="pf-c-simple-list__title">Section title</h2>
                  <ul class="pf-c-simple-list__list">
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 5</button>
                    </li>
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 6</button>
                    </li>
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 7</button>
                    </li>
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 8</button>
                    </li>
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 9</button>
                    </li>
                    <li class="pf-c-simple-list__item">
                      <button
                        class="pf-c-simple-list__item-link"
                        type="button"
                      >List item 10</button>
                    </li>
                  </ul>
                </section>
              </div>
            </div>

            <!-- Panel -->
            <div
              class="pf-c-drawer__panel pf-m-width-75-on-xl"
              id="primary-detail-card-simple-list-example-panel"
              aria-label="Panel"
            >
              <!-- Panel header -->
              <div class="pf-c-drawer__body">
                <div class="pf-l-flex pf-m-column">
                  <div class="pf-l-flex__item">
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
                      <h2
                        class="pf-c-title pf-m-lg"
                        id="primary-detail-card-simple-list-example-drawer-label"
                      >Patternfly-elements</h2>
                    </div>
                  </div>
                </div>
              </div>

              <div class="pf-c-drawer__body">
                <div class="pf-l-flex pf-m-column pf-m-space-items-lg">
                  <div class="pf-l-flex__item">
                    <p>The content of the drawer really is up to you. It could have form fields, definition lists, text lists, labels, charts, progress bars, etc. Spacing recommendation is 24px margins. You can put tabs in here, and can also make the drawer scrollable.</p>
                  </div>
                  <div class="pf-l-flex__item">
                    <div
                      class="pf-c-progress"
                      id="primary-detail-card-simple-list-example-progress-example1"
                    >
                      <div
                        class="pf-c-progress__description"
                        id="primary-detail-card-simple-list-example-progress-example1-description"
                      >Capacity</div>
                      <div class="pf-c-progress__status" aria-hidden="true">
                        <span class="pf-c-progress__measure">33%</span>
                      </div>
                      <div
                        class="pf-c-progress__bar"
                        role="progressbar"
                        aria-valuemin="0"
                        aria-valuemax="100"
                        aria-valuenow="33"
                        aria-labelledby="primary-detail-card-simple-list-example-progress-example1-description"
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
                      class="pf-c-progress"
                      id="primary-detail-card-simple-list-example-progress-example2"
                    >
                      <div
                        class="pf-c-progress__description"
                        id="primary-detail-card-simple-list-example-progress-example2-description"
                      >Modules</div>
                      <div class="pf-c-progress__status" aria-hidden="true">
                        <span class="pf-c-progress__measure">66%</span>
                      </div>
                      <div
                        class="pf-c-progress__bar"
                        role="progressbar"
                        aria-valuemin="0"
                        aria-valuemax="100"
                        aria-valuenow="66"
                        aria-labelledby="primary-detail-card-simple-list-example-progress-example2-description"
                        aria-label="Progress 2"
                      >
                        <div
                          class="pf-c-progress__indicator"
                          style="width:66%;"
                        ></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Primary-detail card data list expanded on mobile

```html isFullscreen
<div class="pf-c-page" id="primary-detail-card-data-list-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-primary-detail-card-data-list-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="primary-detail-card-data-list-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="primary-detail-card-data-list-example-primary-nav"
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
              id="primary-detail-card-data-list-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="primary-detail-card-data-list-example-dropdown-kebab-1-button"
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
              id="primary-detail-card-data-list-example-dropdown-kebab-2-button"
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
              aria-labelledby="primary-detail-card-data-list-example-dropdown-kebab-2-button"
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
        id="primary-detail-card-data-list-example-primary-nav"
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

    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Primary-detail, in card, data list</h1>
        <p>Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5</p>
      </div>
    </section>

    <section class="pf-c-page__main-section">
      <div class="pf-c-card">
        <!-- Drawer -->
        <div class="pf-c-drawer pf-m-expanded pf-m-static">
          <div class="pf-c-drawer__main">
            <!-- Content -->
            <div class="pf-c-drawer__content">
              <div class="pf-c-drawer__body">
                <div
                  class="pf-c-toolbar"
                  id="primary-detail-card-data-list-example-toolbar"
                >
                  <div class="pf-c-toolbar__content">
                    <div class="pf-c-toolbar__content-section">
                      <div class="pf-c-toolbar__item">
                        <div class="pf-c-select" style="width: 150px">
                          <span
                            id="primary-detail-card-data-list-example-toolbar-select-dropdown-label"
                            hidden
                          >Choose one</span>

                          <button
                            class="pf-c-select__toggle"
                            type="button"
                            id="primary-detail-card-data-list-example-toolbar-select-dropdown-toggle"
                            aria-haspopup="true"
                            aria-expanded="false"
                            aria-labelledby="primary-detail-card-data-list-example-toolbar-select-dropdown-label primary-detail-card-data-list-example-toolbar-select-dropdown-toggle"
                          >
                            <div class="pf-c-select__toggle-wrapper">
                              <span class="pf-c-select__toggle-text">Dropdown</span>
                            </div>
                            <span class="pf-c-select__toggle-arrow">
                              <i class="fas fa-caret-down" aria-hidden="true"></i>
                            </span>
                          </button>

                          <ul
                            class="pf-c-select__menu"
                            role="listbox"
                            aria-labelledby="primary-detail-card-data-list-example-toolbar-select-dropdown-label"
                            hidden
                            style="width: 150px"
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
                              <button
                                class="pf-c-select__menu-item"
                                role="option"
                              >Down</button>
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
                      <div class="pf-c-toolbar__item pf-m-pagination">
                        <div class="pf-c-pagination">
                          <div class="pf-c-pagination__total-items">
                            <b>1 - 10</b> of
                            <b>37</b>
                          </div>
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
                                id="primary-detail-card-data-list-example-toolbar-pagination-options-menu-toggle"
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
                              aria-labelledby="primary-detail-card-data-list-example-toolbar-pagination-options-menu-toggle"
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
                        </div>
                      </div>
                    </div>
                    <div
                      class="pf-c-toolbar__expandable-content pf-m-hidden"
                      id="primary-detail-card-data-list-example-toolbar-expandable-content"
                      hidden
                    ></div>
                  </div>
                </div>
                <ul
                  class="pf-c-data-list"
                  role="list"
                  aria-label="Selectable rows data list example"
                  id="primary-detail-card-data-list-example-data-list"
                >
                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-1"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-1"
                          >Node 1</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable pf-m-selected"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-2"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-2"
                          >Node 2</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-3"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-3"
                          >Node 3</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-4"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-4"
                          >Node 4</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-5"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-5"
                          >Node 5</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-6"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-6"
                          >Node 6</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-7"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-7"
                          >Node 7</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-8"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-8"
                          >Node 8</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-9"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-9"
                          >Node 9</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>

                  <li
                    class="pf-c-data-list__item pf-m-selectable"
                    aria-labelledby="primary-detail-card-data-list-example-data-list-item-10"
                    tabindex="0"
                  >
                    <div class="pf-c-data-list__item-row">
                      <div class="pf-c-data-list__item-content">
                        <div class="pf-c-data-list__cell">
                          <div
                            id="primary-detail-card-data-list-example-data-list-item-10"
                          >Node 10</div>
                          <a href="#">siemur/test-space</a>
                        </div>
                      </div>
                    </div>
                  </li>
                </ul>
              </div>
            </div>

            <!-- Panel -->
            <div
              class="pf-c-drawer__panel pf-m-width-75-on-2xl"
              id="primary-detail-card-data-list-example-panel"
              aria-label="Panel"
            >
              <!-- Panel header -->
              <div class="pf-c-drawer__body">
                <div class="pf-l-flex pf-m-column">
                  <div class="pf-l-flex__item">
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
                      <h2
                        class="pf-c-title pf-m-lg"
                        id="primary-detail-card-data-list-example-drawer-label"
                      >Patternfly-elements</h2>
                    </div>
                  </div>
                </div>
              </div>

              <div class="pf-c-drawer__body">
                <div class="pf-l-flex pf-m-column pf-m-space-items-lg">
                  <div class="pf-l-flex__item">
                    <p>The content of the drawer really is up to you. It could have form fields, definition lists, text lists, labels, charts, progress bars, etc. Spacing recommendation is 24px margins. You can put tabs in here, and can also make the drawer scrollable.</p>
                  </div>
                  <div class="pf-l-flex__item">
                    <div
                      class="pf-c-progress"
                      id="primary-detail-card-data-list-example-progress-example1"
                    >
                      <div
                        class="pf-c-progress__description"
                        id="primary-detail-card-data-list-example-progress-example1-description"
                      >Capacity</div>
                      <div class="pf-c-progress__status" aria-hidden="true">
                        <span class="pf-c-progress__measure">33%</span>
                      </div>
                      <div
                        class="pf-c-progress__bar"
                        role="progressbar"
                        aria-valuemin="0"
                        aria-valuemax="100"
                        aria-valuenow="33"
                        aria-labelledby="primary-detail-card-data-list-example-progress-example1-description"
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
                      class="pf-c-progress"
                      id="primary-detail-card-data-list-example-progress-example2"
                    >
                      <div
                        class="pf-c-progress__description"
                        id="primary-detail-card-data-list-example-progress-example2-description"
                      >Modules</div>
                      <div class="pf-c-progress__status" aria-hidden="true">
                        <span class="pf-c-progress__measure">66%</span>
                      </div>
                      <div
                        class="pf-c-progress__bar"
                        role="progressbar"
                        aria-valuemin="0"
                        aria-valuemax="100"
                        aria-valuenow="66"
                        aria-labelledby="primary-detail-card-data-list-example-progress-example2-description"
                        aria-label="Progress 2"
                      >
                        <div
                          class="pf-c-progress__indicator"
                          style="width:66%;"
                        ></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

### Inline modifier

```html isFullscreen
<div class="pf-c-page" id="independent-scroll-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-independent-scroll-example"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="independent-scroll-example-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="independent-scroll-example-primary-nav"
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
              id="independent-scroll-example-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="independent-scroll-example-dropdown-kebab-1-button"
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
              id="independent-scroll-example-dropdown-kebab-2-button"
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
              aria-labelledby="independent-scroll-example-dropdown-kebab-2-button"
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
        id="independent-scroll-example-primary-nav"
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

    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Primary-detail expanded, with data-list and .pf-m-inline modifier demo</h1>
        <p>Body text should be Red Hat Text Regular at 16px. It should have leading of 24px because of its relative line height of 1.5</p>
      </div>
    </section>
    <div class="pf-c-divider" role="separator"></div>
    <!-- Drawer -->
    <div class="pf-c-drawer pf-m-expanded pf-m-inline-on-2xl">
      <div class="pf-c-drawer__main">
        <!-- Content -->
        <div class="pf-c-drawer__content">
          <div class="pf-c-drawer__body">
            <div
              class="pf-c-toolbar pf-m-page-insets"
              id="independent-scroll-example-toolbar"
            >
              <div class="pf-c-toolbar__content">
                <div class="pf-c-toolbar__content-section pf-m-nowrap">
                  <div class="pf-c-toolbar__item">
                    <div class="pf-c-select" style="width: 150px">
                      <span
                        id="independent-scroll-example-toolbar-select-status-label"
                        hidden
                      >Choose one</span>

                      <button
                        class="pf-c-select__toggle"
                        type="button"
                        id="independent-scroll-example-toolbar-select-status-toggle"
                        aria-haspopup="true"
                        aria-expanded="false"
                        aria-labelledby="independent-scroll-example-toolbar-select-status-label independent-scroll-example-toolbar-select-status-toggle"
                      >
                        <div class="pf-c-select__toggle-wrapper">
                          <span class="pf-c-select__toggle-icon">
                            <i class="fas fa-bookmark" aria-hidden="true"></i>
                          </span>
                          <span class="pf-c-select__toggle-text">Status</span>
                        </div>
                        <span class="pf-c-select__toggle-arrow">
                          <i class="fas fa-caret-down" aria-hidden="true"></i>
                        </span>
                      </button>

                      <ul
                        class="pf-c-select__menu"
                        role="listbox"
                        aria-labelledby="independent-scroll-example-toolbar-select-status-label"
                        hidden
                        style="width: 150px"
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
                          <button
                            class="pf-c-select__menu-item"
                            role="option"
                          >Down</button>
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

                  <div
                    class="pf-c-overflow-menu"
                    id="independent-scroll-example-toolbar-overflow-menu"
                  >
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

                        <div class="pf-c-overflow-menu__item">
                          <button
                            class="pf-c-button pf-m-secondary"
                            type="button"
                          >Action</button>
                        </div>
                      </div>
                    </div>
                    <div class="pf-c-overflow-menu__control">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                          type="button"
                          id="independent-scroll-example-toolbar-overflow-menu-dropdown-toggle"
                          aria-label="Dropdown with additional options"
                          aria-expanded="false"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu"
                          aria-labelledby="independent-scroll-example-toolbar-overflow-menu-dropdown-toggle"
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
                        <div
                          class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                        >
                          <span class="pf-c-options-menu__toggle-text">
                            <b>1 - 10</b>&nbsp;of&nbsp;
                            <b>36</b>
                          </span>
                          <button
                            class="pf-c-options-menu__toggle-button"
                            id="independent-scroll-example-toolbar-top-pagination-toggle"
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
                          aria-labelledby="independent-scroll-example-toolbar-top-pagination-toggle"
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
                  id="independent-scroll-example-toolbar-expandable-content"
                  hidden
                ></div>
              </div>
            </div>
            <ul
              class="pf-c-data-list"
              role="list"
              aria-label="Simple data list example"
              id="independent-scroll-example-data-list"
            >
              <li
                class="pf-c-data-list__item"
                aria-labelledby="independent-scroll-example-data-list-item-1"
              >
                <div class="pf-c-data-list__item-row">
                  <div class="pf-c-data-list__item-content">
                    <div class="pf-c-data-list__cell pf-m-align-left">
                      <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                        <div
                          class="pf-l-flex pf-m-column pf-m-space-items-none"
                        >
                          <div class="pf-l-flex__item">
                            <p
                              id="independent-scroll-example-data-list-item-1"
                            >patternfly</p>
                          </div>
                          <div class="pf-l-flex__item">
                            <small>
                              Working repo for PatternFly 4
                              <a href>https://pf4.patternfly.org/</a>
                            </small>
                          </div>
                        </div>
                        <div class="pf-l-flex pf-m-wrap">
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>10</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>4</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>5</span>
                            </div>
                          </div>
                          <div class="pf-l-flex__item">Updated 2 days ago</div>
                        </div>
                      </div>
                    </div>
                    <div
                      class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                    >
                      <button
                        class="pf-c-button pf-m-secondary"
                        type="button"
                      >Action</button>
                      <button class="pf-c-button pf-m-link" type="button">Link</button>
                    </div>
                  </div>
                </div>
              </li>

              <li
                class="pf-c-data-list__item"
                aria-labelledby="independent-scroll-example-data-list-item-2"
              >
                <div class="pf-c-data-list__item-row">
                  <div class="pf-c-data-list__item-content">
                    <div class="pf-c-data-list__cell pf-m-align-left">
                      <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                        <div
                          class="pf-l-flex pf-m-column pf-m-space-items-none"
                        >
                          <div class="pf-l-flex__item">
                            <p
                              id="independent-scroll-example-data-list-item-2"
                            >patternfly-elements</p>
                          </div>
                          <div class="pf-l-flex__item">
                            <small>PatternFly elements</small>
                          </div>
                        </div>
                        <div class="pf-l-flex pf-m-wrap">
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>5</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>9</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>2</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-check-circle" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>11</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i
                                class="fas fa-exclamation-triangle"
                                aria-hidden="true"
                              ></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>4</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-times-circle" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>1</span>
                            </div>
                          </div>
                          <div class="pf-l-flex__item">Updated 2 days ago</div>
                        </div>
                      </div>
                    </div>
                    <div
                      class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                    >
                      <button
                        class="pf-c-button pf-m-secondary"
                        type="button"
                      >Action</button>
                      <button class="pf-c-button pf-m-link" type="button">Link</button>
                    </div>
                  </div>
                </div>
              </li>

              <li
                class="pf-c-data-list__item"
                aria-labelledby="independent-scroll-example-data-list-item-3"
              >
                <div class="pf-c-data-list__item-row">
                  <div class="pf-c-data-list__item-content">
                    <div class="pf-c-data-list__cell pf-m-align-left">
                      <p
                        id="independent-scroll-example-data-list-item-3"
                      >patternfly-unified-design-kit</p>
                    </div>
                    <div
                      class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                    >
                      <button
                        class="pf-c-button pf-m-secondary"
                        type="button"
                      >Action</button>
                      <button class="pf-c-button pf-m-link" type="button">Link</button>
                    </div>
                  </div>
                </div>
              </li>

              <li
                class="pf-c-data-list__item"
                aria-labelledby="independent-scroll-example-data-list-item-4"
              >
                <div class="pf-c-data-list__item-row">
                  <div class="pf-c-data-list__item-content">
                    <div class="pf-c-data-list__cell pf-m-align-left">
                      <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                        <div
                          class="pf-l-flex pf-m-column pf-m-space-items-none"
                        >
                          <div class="pf-l-flex__item">
                            <p
                              id="independent-scroll-example-data-list-item-4"
                            >patternfly</p>
                          </div>
                          <div class="pf-l-flex__item">
                            <small>
                              Working repo for PatternFly 4
                              <a href>https://pf4.patternfly.org/</a>
                            </small>
                          </div>
                        </div>
                        <div class="pf-l-flex pf-m-wrap">
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>10</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>4</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>5</span>
                            </div>
                          </div>
                          <div class="pf-l-flex__item">Updated 2 days ago</div>
                        </div>
                      </div>
                    </div>
                    <div
                      class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                    >
                      <button
                        class="pf-c-button pf-m-secondary"
                        type="button"
                      >Action</button>
                      <button class="pf-c-button pf-m-link" type="button">Link</button>
                    </div>
                  </div>
                </div>
              </li>

              <li
                class="pf-c-data-list__item"
                aria-labelledby="independent-scroll-example-data-list-item-5"
              >
                <div class="pf-c-data-list__item-row">
                  <div class="pf-c-data-list__item-content">
                    <div class="pf-c-data-list__cell pf-m-align-left">
                      <div class="pf-l-flex pf-m-column pf-m-space-items-md">
                        <div
                          class="pf-l-flex pf-m-column pf-m-space-items-none"
                        >
                          <div class="pf-l-flex__item">
                            <p
                              id="independent-scroll-example-data-list-item-5"
                            >patternfly-elements</p>
                          </div>
                          <div class="pf-l-flex__item">
                            <small>PatternFly elements</small>
                          </div>
                        </div>
                        <div class="pf-l-flex pf-m-wrap">
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code-branch" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>5</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-code" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>9</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-cube" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>2</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-check-circle" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>11</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i
                                class="fas fa-exclamation-triangle"
                                aria-hidden="true"
                              ></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>4</span>
                            </div>
                          </div>
                          <div class="pf-l-flex pf-m-space-items-xs">
                            <div class="pf-l-flex__item">
                              <i class="fas fa-times-circle" aria-hidden="true"></i>
                            </div>
                            <div class="pf-l-flex__item">
                              <span>1</span>
                            </div>
                          </div>
                          <div class="pf-l-flex__item">Updated 2 days ago</div>
                        </div>
                      </div>
                    </div>
                    <div
                      class="pf-c-data-list__cell pf-m-align-right pf-m-no-fill"
                    >
                      <button
                        class="pf-c-button pf-m-secondary"
                        type="button"
                      >Action</button>
                      <button class="pf-c-button pf-m-link" type="button">Link</button>
                    </div>
                  </div>
                </div>
              </li>
            </ul>
          </div>
        </div>

        <!-- Panel -->
        <div class="pf-c-drawer__panel">
          <!-- Panel header -->
          <div class="pf-c-drawer__body">
            <div class="pf-l-flex pf-m-column">
              <div class="pf-l-flex__item">
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
                  <h2
                    class="pf-c-title pf-m-lg"
                    id="independent-scroll-example-drawer-label"
                  >Node 2</h2>
                </div>
              </div>
              <div class="pf-l-flex__item">
                <a href="#">siemur/test-space</a>
              </div>
            </div>
          </div>

          <!-- Tabs -->
          <div class="pf-c-drawer__body pf-m-no-padding">
            <div
              class="pf-c-tabs pf-m-box pf-m-fill"
              id="independent-scroll-example-tabs"
            >
              <button class="pf-c-tabs__scroll-button" aria-label="Scroll left">
                <i class="fas fa-angle-left" aria-hidden="true"></i>
              </button>
              <ul class="pf-c-tabs__list">
                <li class="pf-c-tabs__item pf-m-current">
                  <button
                    class="pf-c-tabs__link"
                    aria-controls="independent-scroll-example-tabs-tab1-panel"
                    id="independent-scroll-example-tabs-tab1-link"
                  >
                    <span class="pf-c-tabs__item-text">Overview</span>
                  </button>
                </li>
                <li class="pf-c-tabs__item">
                  <button
                    class="pf-c-tabs__link"
                    aria-controls="independent-scroll-example-tabs-tab2-panel"
                    id="independent-scroll-example-tabs-tab2-link"
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

          <!-- Tab content -->
          <div class="pf-c-drawer__body">
            <section
              class="pf-c-tab-content"
              id="independent-scroll-example-tabs-tab1-panel"
              aria-labelledby="independent-scroll-example-tabs-tab1-link"
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
                      id="independent-scroll-example-progress-example1"
                    >
                      <div
                        class="pf-c-progress__description"
                        id="independent-scroll-example-progress-example1-description"
                      >Capacity</div>
                      <div class="pf-c-progress__status" aria-hidden="true">
                        <span class="pf-c-progress__measure">33%</span>
                      </div>
                      <div
                        class="pf-c-progress__bar"
                        role="progressbar"
                        aria-valuemin="0"
                        aria-valuemax="100"
                        aria-valuenow="33"
                        aria-labelledby="independent-scroll-example-progress-example1-description"
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
                      id="independent-scroll-example-progress-example2"
                    >
                      <div
                        class="pf-c-progress__description"
                        id="independent-scroll-example-progress-example2-description"
                      >Modules</div>
                      <div class="pf-c-progress__status" aria-hidden="true">
                        <span class="pf-c-progress__measure">66%</span>
                      </div>
                      <div
                        class="pf-c-progress__bar"
                        role="progressbar"
                        aria-valuemin="0"
                        aria-valuemax="100"
                        aria-valuenow="66"
                        aria-labelledby="independent-scroll-example-progress-example2-description"
                        aria-label="Progress 2"
                      >
                        <div
                          class="pf-c-progress__indicator"
                          style="width:66%;"
                        ></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>
            <section
              class="pf-c-tab-content"
              id="independent-scroll-example-tabs-tab2-panel"
              aria-labelledby="independent-scroll-example-tabs-tab2-link"
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
  </main>
</div>

```

## Documentation

This demo implements the drawer in context of the page component.
