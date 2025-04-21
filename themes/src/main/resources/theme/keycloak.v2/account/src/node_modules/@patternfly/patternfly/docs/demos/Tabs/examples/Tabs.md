---
id: 'Tabs'
section: components
---## Examples

### Open tabs

```html isFullscreen
<div class="pf-c-page" id="tabs-tables-and-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-tabs-tables-and-tabs-example"
  >Skip to content</a>
  <header class="pf-c-masthead" id="tabs-tables-and-tabs-example-masthead">
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
        id="tabs-tables-and-tabs-example-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div
              class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-spacer-none pf-m-spacer-md-on-md"
            >
              <div class="pf-c-toolbar__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Notifications"
                >
                  <span class="pf-c-notification-badge">
                    <i class="pf-icon-bell" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div
                class="pf-c-toolbar__group pf-m-icon-button-group pf-m-hidden pf-m-visible-on-lg"
              >
                <div class="pf-c-toolbar__item">
                  <nav
                    class="pf-c-app-launcher"
                    aria-label="Application launcher"
                    id="tabs-tables-and-tabs-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="tabs-tables-and-tabs-example-masthead-icon-group--app-launcher-button"
                      aria-expanded="false"
                      aria-label="Application launcher"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                    <div
                      class="pf-c-app-launcher__menu pf-m-align-right"
                      hidden
                    >
                      <div class="pf-c-app-launcher__menu-search">
                        <div class="pf-c-search-input">
                          <div class="pf-c-search-input__bar">
                            <span class="pf-c-search-input__text">
                              <span class="pf-c-search-input__icon">
                                <i
                                  class="fas fa-search fa-fw"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <input
                                class="pf-c-search-input__text-input"
                                type="text"
                                placeholder="Filter by name"
                                aria-label="Filter by name"
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider" />
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </nav>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="tabs-tables-and-tabs-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="tabs-tables-and-tabs-example-masthead-settings-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Settings</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Use the beta release</button>
                      </li>
                    </ul>
                  </div>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="tabs-tables-and-tabs-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="tabs-tables-and-tabs-example-masthead-help-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Support options</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Open support case</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >API documentation</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item pf-m-hidden-on-lg">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-menu-toggle pf-m-plain"
                    type="button"
                    aria-expanded="false"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <div class="pf-c-menu pf-m-drilldown pf-m-align-right" hidden>
                    <div class="pf-c-menu__content">
                      <section class="pf-c-menu__group pf-m-hidden-on-sm">
                        <ul class="pf-c-menu__list" role="menu">
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Username:</div>
                                <div class="pf-u-font-size-md">ned_username</div>
                              </span>
                            </button>
                          </li>
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Account number:</div>
                                <div class="pf-u-font-size-md">123456789</div>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-divider" role="separator"></li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">My profile</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span
                                  class="pf-c-menu__item-text"
                                >User management</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">Logout</span>
                              </span>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider pf-m-hidden-on-sm" />
                      <section class="pf-c-menu__group">
                        <ul class="pf-c-menu__list" role="menu">
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-cog"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Settings</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-cog"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Settings</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Customer support</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span class="pf-c-menu__item-text">About</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-pf-icon pf-icon-help"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Help</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-pf-icon pf-icon-help"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span class="pf-c-menu__item-text">Help</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Support options</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Open support case</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >API documentation</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i class="fas fa-fw fa-th" aria-hidden="true"></i>
                                </span>
                                <span
                                  class="pf-c-menu__item-text"
                                >Application launcher</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__header">
                                <button
                                  class="pf-c-menu__item"
                                  type="button"
                                  role="menuitem"
                                >
                                  <span class="pf-c-menu__item-main">
                                    <span class="pf-c-menu__item-toggle-icon">
                                      <i class="fas fa-angle-left"></i>
                                    </span>
                                    <span class="pf-c-menu__item-icon">
                                      <i
                                        class="fas fa-fw fa-th"
                                        aria-hidden="true"
                                      ></i>
                                    </span>
                                    <span
                                      class="pf-c-menu__item-text"
                                    >Application launcher</span>
                                  </span>
                                </button>
                              </div>
                              <div class="pf-c-menu__search">
                                <div class="pf-c-menu__search-input">
                                  <div class="pf-c-search-input">
                                    <div class="pf-c-search-input__bar">
                                      <span class="pf-c-search-input__text">
                                        <span class="pf-c-search-input__icon">
                                          <i
                                            class="fas fa-search fa-fw"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <input
                                          class="pf-c-search-input__text-input"
                                          type="text"
                                          placeholder="Search"
                                          aria-label="Search"
                                        />
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Favorites</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Group 1</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                            </div>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-hidden pf-m-visible-on-sm">
              <div
                class="pf-c-dropdown pf-m-full-height"
                style="--pf-c-dropdown--MaxWidth: 20ch;"
              >
                <button
                  class="pf-c-dropdown__toggle"
                  id="tabs-tables-and-tabs-example-masthead-profile-button"
                  aria-expanded="false"
                  type="button"
                >
                  <span class="pf-c-dropdown__toggle-image">
                    <img
                      class="pf-c-avatar"
                      src="/assets/images/img_avatar.svg"
                      alt="Avatar image"
                    />
                  </span>
                  <span class="pf-c-dropdown__toggle-text">Ned Username</span>
                  <span class="pf-c-dropdown__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-dropdown__menu" hidden>
                  <section class="pf-c-dropdown__group">
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Account number:</div>
                      <div>123456789</div>
                    </div>
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Username:</div>
                      <div>mshaksho@redhat.com</div>
                    </div>
                  </section>
                  <hr class="pf-c-divider" />
                  <section class="pf-c-dropdown__group">
                    <ul>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">My profile</a>
                      </li>
                      <li>
                        <a
                          class="pf-c-dropdown__menu-item"
                          href="#"
                        >User management</a>
                      </li>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">Logout</a>
                      </li>
                    </ul>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="tabs-tables-and-tabs-example-primary-nav"
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
    id="main-content-tabs-tables-and-tabs-example"
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
          <p>This is a full page demo.</p>
        </div>
      </div>
    </section>

    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-page-insets"
          id="tabs-tables-and-tabs-example-tabs"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-details-panel"
                id="tabs-tables-and-tabs-example-tabs-details-link"
              >
                <span class="pf-c-tabs__item-text">Details</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-yaml-panel"
                id="tabs-tables-and-tabs-example-tabs-yaml-link"
              >
                <span class="pf-c-tabs__item-text">YAML</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-environment-panel"
                id="tabs-tables-and-tabs-example-tabs-environment-link"
              >
                <span class="pf-c-tabs__item-text">Environment</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-logs-panel"
                id="tabs-tables-and-tabs-example-tabs-logs-link"
              >
                <span class="pf-c-tabs__item-text">Logs</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-events-panel"
                id="tabs-tables-and-tabs-example-tabs-events-link"
              >
                <span class="pf-c-tabs__item-text">Events</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-terminal-panel"
                id="tabs-tables-and-tabs-example-tabs-terminal-link"
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
          aria-labelledby="tabs-tables-and-tabs-example-tabs-details-link"
          id="tabs-tables-and-tabs-example-tabs-details-panel"
          role="tabpanel"
          tabindex="0"
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-l-flex pf-m-column">
              <div class="pf-l-flex__item pf-m-spacer-lg">
                <h2
                  class="pf-c-title pf-m-lg pf-u-mt-sm"
                  id="-details-title"
                >Pod details</h2>
              </div>
              <div class="pf-l-flex__item">
                <dl
                  class="pf-c-description-list pf-m-2-col-on-lg"
                  aria-labelledby="-details-title"
                >
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">Name</dt>
                    <dd class="pf-c-description-list__description">
                      <div
                        class="pf-c-description-list__text"
                      >3scale-control-fccb6ddb9-phyqv9</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">Status</dt>
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
                    <dt class="pf-c-description-list__term">Namespace</dt>
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
                    <dt class="pf-c-description-list__term">Restart policy</dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">Always restart</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">Labels</dt>
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
                    <dt
                      class="pf-c-description-list__term"
                    >Active deadline seconds</dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">Not configured</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">Tolerations</dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">stuff</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">Pod IP</dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">10.345.2.197</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">Annotations</dt>
                    <dd class="pf-c-description-list__description">
                      <div class="pf-c-description-list__text">stuff</div>
                    </dd>
                  </div>
                  <div class="pf-c-description-list__group">
                    <dt class="pf-c-description-list__term">Node</dt>
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
                    <dt class="pf-c-description-list__term">Created at</dt>
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
          aria-labelledby="tabs-tables-and-tabs-example-tabs-yaml-link"
          id="tabs-tables-and-tabs-example-tabs-yaml-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">YAML panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-environment-link"
          id="tabs-tables-and-tabs-example-tabs-environment-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">Environment panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-logs-link"
          id="tabs-tables-and-tabs-example-tabs-logs-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">Logs panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-events-link"
          id="tabs-tables-and-tabs-example-tabs-events-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">Events panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-terminal-link"
          id="tabs-tables-and-tabs-example-tabs-terminal-panel"
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
<div class="pf-c-page" id="tabs-tables-and-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-tabs-tables-and-tabs-example"
  >Skip to content</a>
  <header class="pf-c-masthead" id="tabs-tables-and-tabs-example-masthead">
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
        id="tabs-tables-and-tabs-example-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div
              class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-spacer-none pf-m-spacer-md-on-md"
            >
              <div class="pf-c-toolbar__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Notifications"
                >
                  <span class="pf-c-notification-badge">
                    <i class="pf-icon-bell" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div
                class="pf-c-toolbar__group pf-m-icon-button-group pf-m-hidden pf-m-visible-on-lg"
              >
                <div class="pf-c-toolbar__item">
                  <nav
                    class="pf-c-app-launcher"
                    aria-label="Application launcher"
                    id="tabs-tables-and-tabs-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="tabs-tables-and-tabs-example-masthead-icon-group--app-launcher-button"
                      aria-expanded="false"
                      aria-label="Application launcher"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                    <div
                      class="pf-c-app-launcher__menu pf-m-align-right"
                      hidden
                    >
                      <div class="pf-c-app-launcher__menu-search">
                        <div class="pf-c-search-input">
                          <div class="pf-c-search-input__bar">
                            <span class="pf-c-search-input__text">
                              <span class="pf-c-search-input__icon">
                                <i
                                  class="fas fa-search fa-fw"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <input
                                class="pf-c-search-input__text-input"
                                type="text"
                                placeholder="Filter by name"
                                aria-label="Filter by name"
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider" />
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </nav>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="tabs-tables-and-tabs-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="tabs-tables-and-tabs-example-masthead-settings-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Settings</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Use the beta release</button>
                      </li>
                    </ul>
                  </div>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="tabs-tables-and-tabs-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="tabs-tables-and-tabs-example-masthead-help-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Support options</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Open support case</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >API documentation</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item pf-m-hidden-on-lg">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-menu-toggle pf-m-plain"
                    type="button"
                    aria-expanded="false"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <div class="pf-c-menu pf-m-drilldown pf-m-align-right" hidden>
                    <div class="pf-c-menu__content">
                      <section class="pf-c-menu__group pf-m-hidden-on-sm">
                        <ul class="pf-c-menu__list" role="menu">
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Username:</div>
                                <div class="pf-u-font-size-md">ned_username</div>
                              </span>
                            </button>
                          </li>
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Account number:</div>
                                <div class="pf-u-font-size-md">123456789</div>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-divider" role="separator"></li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">My profile</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span
                                  class="pf-c-menu__item-text"
                                >User management</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">Logout</span>
                              </span>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider pf-m-hidden-on-sm" />
                      <section class="pf-c-menu__group">
                        <ul class="pf-c-menu__list" role="menu">
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-cog"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Settings</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-cog"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Settings</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Customer support</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span class="pf-c-menu__item-text">About</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-pf-icon pf-icon-help"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Help</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-pf-icon pf-icon-help"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span class="pf-c-menu__item-text">Help</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Support options</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Open support case</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >API documentation</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i class="fas fa-fw fa-th" aria-hidden="true"></i>
                                </span>
                                <span
                                  class="pf-c-menu__item-text"
                                >Application launcher</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__header">
                                <button
                                  class="pf-c-menu__item"
                                  type="button"
                                  role="menuitem"
                                >
                                  <span class="pf-c-menu__item-main">
                                    <span class="pf-c-menu__item-toggle-icon">
                                      <i class="fas fa-angle-left"></i>
                                    </span>
                                    <span class="pf-c-menu__item-icon">
                                      <i
                                        class="fas fa-fw fa-th"
                                        aria-hidden="true"
                                      ></i>
                                    </span>
                                    <span
                                      class="pf-c-menu__item-text"
                                    >Application launcher</span>
                                  </span>
                                </button>
                              </div>
                              <div class="pf-c-menu__search">
                                <div class="pf-c-menu__search-input">
                                  <div class="pf-c-search-input">
                                    <div class="pf-c-search-input__bar">
                                      <span class="pf-c-search-input__text">
                                        <span class="pf-c-search-input__icon">
                                          <i
                                            class="fas fa-search fa-fw"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <input
                                          class="pf-c-search-input__text-input"
                                          type="text"
                                          placeholder="Search"
                                          aria-label="Search"
                                        />
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Favorites</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Group 1</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                            </div>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-hidden pf-m-visible-on-sm">
              <div
                class="pf-c-dropdown pf-m-full-height"
                style="--pf-c-dropdown--MaxWidth: 20ch;"
              >
                <button
                  class="pf-c-dropdown__toggle"
                  id="tabs-tables-and-tabs-example-masthead-profile-button"
                  aria-expanded="false"
                  type="button"
                >
                  <span class="pf-c-dropdown__toggle-image">
                    <img
                      class="pf-c-avatar"
                      src="/assets/images/img_avatar.svg"
                      alt="Avatar image"
                    />
                  </span>
                  <span class="pf-c-dropdown__toggle-text">Ned Username</span>
                  <span class="pf-c-dropdown__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-dropdown__menu" hidden>
                  <section class="pf-c-dropdown__group">
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Account number:</div>
                      <div>123456789</div>
                    </div>
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Username:</div>
                      <div>mshaksho@redhat.com</div>
                    </div>
                  </section>
                  <hr class="pf-c-divider" />
                  <section class="pf-c-dropdown__group">
                    <ul>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">My profile</a>
                      </li>
                      <li>
                        <a
                          class="pf-c-dropdown__menu-item"
                          href="#"
                        >User management</a>
                      </li>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">Logout</a>
                      </li>
                    </ul>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="tabs-tables-and-tabs-example-primary-nav"
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
    id="main-content-tabs-tables-and-tabs-example"
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
          <p>This is a full page demo.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-page-insets"
          id="tabs-tables-and-tabs-example-tabs"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-details-panel"
                id="tabs-tables-and-tabs-example-tabs-details-link"
              >
                <span class="pf-c-tabs__item-text">Details</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-yaml-panel"
                id="tabs-tables-and-tabs-example-tabs-yaml-link"
              >
                <span class="pf-c-tabs__item-text">YAML</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-environment-panel"
                id="tabs-tables-and-tabs-example-tabs-environment-link"
              >
                <span class="pf-c-tabs__item-text">Environment</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-logs-panel"
                id="tabs-tables-and-tabs-example-tabs-logs-link"
              >
                <span class="pf-c-tabs__item-text">Logs</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-events-panel"
                id="tabs-tables-and-tabs-example-tabs-events-link"
              >
                <span class="pf-c-tabs__item-text">Events</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-terminal-panel"
                id="tabs-tables-and-tabs-example-tabs-terminal-link"
              >
                <span class="pf-c-tabs__item-text">Terminal</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </section>
    <section
      class="pf-c-page__main-section pf-m-limit-width pf-m-light pf-m-no-padding"
    >
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-secondary pf-m-page-insets pf-m-border-bottom"
          id="tabs-tables-and-tabs-example-tabs-secondary"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-secondary-pod-info-panel"
                id="tabs-tables-and-tabs-example-tabs-secondary-pod-info-link"
              >
                <span class="pf-c-tabs__item-text">Pod information</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="tabs-tables-and-tabs-example-tabs-secondary-editable-aspects-panel"
                id="tabs-tables-and-tabs-example-tabs-secondary-editable-aspects-link"
              >
                <span class="pf-c-tabs__item-text">Editable Aspects</span>
              </button>
            </li>
          </ul>
        </div>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-details-link"
          id="tabs-tables-and-tabs-example-tabs-details-panel"
          role="tabpanel"
          tabindex="0"
        >
          <div class="pf-c-tab-content__body pf-m-padding">
            <section
              class="pf-c-tab-content"
              aria-labelledby="tabs-tables-and-tabs-example-tabs-secondary-pod-info-link"
              id="tabs-tables-and-tabs-example-tabs-secondary-pod-info-panel"
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
                        <dt class="pf-c-description-list__term">Name</dt>
                        <dd class="pf-c-description-list__description">
                          <div
                            class="pf-c-description-list__text"
                          >3scale-control-fccb6ddb9-phyqv9</div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt class="pf-c-description-list__term">Status</dt>
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
                        <dt class="pf-c-description-list__term">Namespace</dt>
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
                        <dt class="pf-c-description-list__term">Restart policy</dt>
                        <dd class="pf-c-description-list__description">
                          <div
                            class="pf-c-description-list__text"
                          >Always restart</div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt class="pf-c-description-list__term">Pod IP</dt>
                        <dd class="pf-c-description-list__description">
                          <div class="pf-c-description-list__text">10.345.2.197</div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt
                          class="pf-c-description-list__term"
                        >Active deadline seconds</dt>
                        <dd class="pf-c-description-list__description">
                          <div
                            class="pf-c-description-list__text"
                          >Not configured</div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt class="pf-c-description-list__term">Created at</dt>
                        <dd class="pf-c-description-list__description">
                          <div class="pf-c-description-list__text">
                            <time>Oct 15, 1:51 pm</time>
                          </div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt class="pf-c-description-list__term">Node</dt>
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
              aria-labelledby="tabs-tables-and-tabs-example-tabs-secondary-editable-aspects-link"
              id="tabs-tables-and-tabs-example-tabs-secondary-editable-aspects-panel"
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
          aria-labelledby="tabs-tables-and-tabs-example-tabs-yaml-link"
          id="tabs-tables-and-tabs-example-tabs-yaml-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body pf-m-padding">YAML panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-environment-link"
          id="tabs-tables-and-tabs-example-tabs-environment-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body pf-m-padding">Environment panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-logs-link"
          id="tabs-tables-and-tabs-example-tabs-logs-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body pf-m-padding">Logs panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-events-link"
          id="tabs-tables-and-tabs-example-tabs-events-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body pf-m-padding">Events panel</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="tabs-tables-and-tabs-example-tabs-terminal-link"
          id="tabs-tables-and-tabs-example-tabs-terminal-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body pf-m-padding">Terminal panel</div>
        </section>
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
  <header class="pf-c-masthead" id="nested-tabs-example-masthead">
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
        id="nested-tabs-example-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div
              class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-spacer-none pf-m-spacer-md-on-md"
            >
              <div class="pf-c-toolbar__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Notifications"
                >
                  <span class="pf-c-notification-badge">
                    <i class="pf-icon-bell" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div
                class="pf-c-toolbar__group pf-m-icon-button-group pf-m-hidden pf-m-visible-on-lg"
              >
                <div class="pf-c-toolbar__item">
                  <nav
                    class="pf-c-app-launcher"
                    aria-label="Application launcher"
                    id="nested-tabs-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="nested-tabs-example-masthead-icon-group--app-launcher-button"
                      aria-expanded="false"
                      aria-label="Application launcher"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                    <div
                      class="pf-c-app-launcher__menu pf-m-align-right"
                      hidden
                    >
                      <div class="pf-c-app-launcher__menu-search">
                        <div class="pf-c-search-input">
                          <div class="pf-c-search-input__bar">
                            <span class="pf-c-search-input__text">
                              <span class="pf-c-search-input__icon">
                                <i
                                  class="fas fa-search fa-fw"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <input
                                class="pf-c-search-input__text-input"
                                type="text"
                                placeholder="Filter by name"
                                aria-label="Filter by name"
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider" />
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </nav>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="nested-tabs-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="nested-tabs-example-masthead-settings-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Settings</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Use the beta release</button>
                      </li>
                    </ul>
                  </div>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="nested-tabs-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="nested-tabs-example-masthead-help-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Support options</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Open support case</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >API documentation</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item pf-m-hidden-on-lg">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-menu-toggle pf-m-plain"
                    type="button"
                    aria-expanded="false"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <div class="pf-c-menu pf-m-drilldown pf-m-align-right" hidden>
                    <div class="pf-c-menu__content">
                      <section class="pf-c-menu__group pf-m-hidden-on-sm">
                        <ul class="pf-c-menu__list" role="menu">
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Username:</div>
                                <div class="pf-u-font-size-md">ned_username</div>
                              </span>
                            </button>
                          </li>
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Account number:</div>
                                <div class="pf-u-font-size-md">123456789</div>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-divider" role="separator"></li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">My profile</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span
                                  class="pf-c-menu__item-text"
                                >User management</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">Logout</span>
                              </span>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider pf-m-hidden-on-sm" />
                      <section class="pf-c-menu__group">
                        <ul class="pf-c-menu__list" role="menu">
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-cog"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Settings</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-cog"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Settings</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Customer support</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span class="pf-c-menu__item-text">About</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-pf-icon pf-icon-help"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Help</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-pf-icon pf-icon-help"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span class="pf-c-menu__item-text">Help</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Support options</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Open support case</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >API documentation</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i class="fas fa-fw fa-th" aria-hidden="true"></i>
                                </span>
                                <span
                                  class="pf-c-menu__item-text"
                                >Application launcher</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__header">
                                <button
                                  class="pf-c-menu__item"
                                  type="button"
                                  role="menuitem"
                                >
                                  <span class="pf-c-menu__item-main">
                                    <span class="pf-c-menu__item-toggle-icon">
                                      <i class="fas fa-angle-left"></i>
                                    </span>
                                    <span class="pf-c-menu__item-icon">
                                      <i
                                        class="fas fa-fw fa-th"
                                        aria-hidden="true"
                                      ></i>
                                    </span>
                                    <span
                                      class="pf-c-menu__item-text"
                                    >Application launcher</span>
                                  </span>
                                </button>
                              </div>
                              <div class="pf-c-menu__search">
                                <div class="pf-c-menu__search-input">
                                  <div class="pf-c-search-input">
                                    <div class="pf-c-search-input__bar">
                                      <span class="pf-c-search-input__text">
                                        <span class="pf-c-search-input__icon">
                                          <i
                                            class="fas fa-search fa-fw"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <input
                                          class="pf-c-search-input__text-input"
                                          type="text"
                                          placeholder="Search"
                                          aria-label="Search"
                                        />
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Favorites</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Group 1</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                            </div>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-hidden pf-m-visible-on-sm">
              <div
                class="pf-c-dropdown pf-m-full-height"
                style="--pf-c-dropdown--MaxWidth: 20ch;"
              >
                <button
                  class="pf-c-dropdown__toggle"
                  id="nested-tabs-example-masthead-profile-button"
                  aria-expanded="false"
                  type="button"
                >
                  <span class="pf-c-dropdown__toggle-image">
                    <img
                      class="pf-c-avatar"
                      src="/assets/images/img_avatar.svg"
                      alt="Avatar image"
                    />
                  </span>
                  <span class="pf-c-dropdown__toggle-text">Ned Username</span>
                  <span class="pf-c-dropdown__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-dropdown__menu" hidden>
                  <section class="pf-c-dropdown__group">
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Account number:</div>
                      <div>123456789</div>
                    </div>
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Username:</div>
                      <div>mshaksho@redhat.com</div>
                    </div>
                  </section>
                  <hr class="pf-c-divider" />
                  <section class="pf-c-dropdown__group">
                    <ul>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">My profile</a>
                      </li>
                      <li>
                        <a
                          class="pf-c-dropdown__menu-item"
                          href="#"
                        >User management</a>
                      </li>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">Logout</a>
                      </li>
                    </ul>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
          <p>This is a full page demo.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-page-insets"
          id="nested-tabs-example-tabs-tabs"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="nested-tabs-example-tabs-tabs-cluster-1-panel"
                id="nested-tabs-example-tabs-tabs-cluster-1-link"
              >
                <span class="pf-c-tabs__item-text">Cluster 1</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="nested-tabs-example-tabs-tabs-cluster-2-panel"
                id="nested-tabs-example-tabs-tabs-cluster-2-link"
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
          aria-labelledby="nested-tabs-example-tabs-tabs-cluster-1-link"
          id="nested-tabs-example-tabs-tabs-cluster-1-panel"
          role="tabpanel"
          tabindex="0"
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-l-grid pf-m-gutter">
              <div class="pf-l-grid__item pf-m-6-col-on-md pf-m-8-col-on-xl">
                <div class="pf-c-card pf-m-full-height">
                  <div class="pf-c-card__header">
                    <h2 class="pf-c-title pf-m-lg">Status</h2>
                  </div>
                  <div class="pf-c-card__body">
                    <div class="pf-l-flex pf-m-column">
                      <div class="pf-l-flex__item">
                        <div
                          class="pf-c-tabs pf-m-secondary"
                          id="nested-tabs-example-tabs-tabs-subtabs"
                        >
                          <ul class="pf-c-tabs__list">
                            <li class="pf-c-tabs__item pf-m-current">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="nested-tabs-example-tabs-tabs-subtabs-cluster-panel"
                                id="nested-tabs-example-tabs-tabs-subtabs-cluster-link"
                              >
                                <span class="pf-c-tabs__item-text">Cluster</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="nested-tabs-example-tabs-tabs-subtabs-control-plane-panel"
                                id="nested-tabs-example-tabs-tabs-subtabs-control-plane-link"
                              >
                                <span class="pf-c-tabs__item-text">Control plane</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="nested-tabs-example-tabs-tabs-subtabs-operators-panel"
                                id="nested-tabs-example-tabs-tabs-subtabs-operators-link"
                              >
                                <span class="pf-c-tabs__item-text">Operators</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="nested-tabs-example-tabs-tabs-subtabs-virtualization-panel"
                                id="nested-tabs-example-tabs-tabs-subtabs-virtualization-link"
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
                          aria-labelledby="nested-tabs-example-tabs-tabs-subtabs-cluster-link"
                          id="nested-tabs-example-tabs-tabs-subtabs-cluster-panel"
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
                          aria-labelledby="nested-tabs-example-tabs-tabs-subtabs-control-plane-link"
                          id="nested-tabs-example-tabs-tabs-subtabs-control-plane-panel"
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
                          aria-labelledby="nested-tabs-example-tabs-tabs-subtabs-operators-link"
                          id="nested-tabs-example-tabs-tabs-subtabs-operators-panel"
                          role="tabpanel"
                          tabindex="0"
                          hidden
                        >
                          <div class="pf-c-tab-content__body">Operators content</div>
                        </section>
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="nested-tabs-example-tabs-tabs-subtabs-virtualization-link"
                          id="nested-tabs-example-tabs-tabs-subtabs-virtualization-panel"
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
                        <h3 class="pf-c-title pf-m-lg">Title of card</h3>
                      </div>
                    </div>
                  </div>
                  <div class="pf-l-flex__item pf-m-flex-1">
                    <div class="pf-c-card pf-m-full-height">
                      <div class="pf-c-card__header">
                        <h3 class="pf-c-title pf-m-lg">Title of card</h3>
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
          aria-labelledby="nested-tabs-example-tabs-tabs-cluster-2-link"
          id="nested-tabs-example-tabs-tabs-cluster-2-panel"
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
  <header class="pf-c-masthead" id="table-tabs-example-masthead">
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
        id="table-tabs-example-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div
              class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-spacer-none pf-m-spacer-md-on-md"
            >
              <div class="pf-c-toolbar__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Notifications"
                >
                  <span class="pf-c-notification-badge">
                    <i class="pf-icon-bell" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div
                class="pf-c-toolbar__group pf-m-icon-button-group pf-m-hidden pf-m-visible-on-lg"
              >
                <div class="pf-c-toolbar__item">
                  <nav
                    class="pf-c-app-launcher"
                    aria-label="Application launcher"
                    id="table-tabs-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="table-tabs-example-masthead-icon-group--app-launcher-button"
                      aria-expanded="false"
                      aria-label="Application launcher"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                    <div
                      class="pf-c-app-launcher__menu pf-m-align-right"
                      hidden
                    >
                      <div class="pf-c-app-launcher__menu-search">
                        <div class="pf-c-search-input">
                          <div class="pf-c-search-input__bar">
                            <span class="pf-c-search-input__text">
                              <span class="pf-c-search-input__icon">
                                <i
                                  class="fas fa-search fa-fw"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <input
                                class="pf-c-search-input__text-input"
                                type="text"
                                placeholder="Filter by name"
                                aria-label="Filter by name"
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider" />
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </nav>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="table-tabs-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="table-tabs-example-masthead-settings-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Settings</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Use the beta release</button>
                      </li>
                    </ul>
                  </div>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="table-tabs-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="table-tabs-example-masthead-help-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Support options</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Open support case</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >API documentation</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item pf-m-hidden-on-lg">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-menu-toggle pf-m-plain"
                    type="button"
                    aria-expanded="false"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <div class="pf-c-menu pf-m-drilldown pf-m-align-right" hidden>
                    <div class="pf-c-menu__content">
                      <section class="pf-c-menu__group pf-m-hidden-on-sm">
                        <ul class="pf-c-menu__list" role="menu">
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Username:</div>
                                <div class="pf-u-font-size-md">ned_username</div>
                              </span>
                            </button>
                          </li>
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Account number:</div>
                                <div class="pf-u-font-size-md">123456789</div>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-divider" role="separator"></li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">My profile</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span
                                  class="pf-c-menu__item-text"
                                >User management</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">Logout</span>
                              </span>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider pf-m-hidden-on-sm" />
                      <section class="pf-c-menu__group">
                        <ul class="pf-c-menu__list" role="menu">
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-cog"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Settings</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-cog"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Settings</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Customer support</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span class="pf-c-menu__item-text">About</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-pf-icon pf-icon-help"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Help</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-pf-icon pf-icon-help"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span class="pf-c-menu__item-text">Help</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Support options</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Open support case</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >API documentation</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i class="fas fa-fw fa-th" aria-hidden="true"></i>
                                </span>
                                <span
                                  class="pf-c-menu__item-text"
                                >Application launcher</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__header">
                                <button
                                  class="pf-c-menu__item"
                                  type="button"
                                  role="menuitem"
                                >
                                  <span class="pf-c-menu__item-main">
                                    <span class="pf-c-menu__item-toggle-icon">
                                      <i class="fas fa-angle-left"></i>
                                    </span>
                                    <span class="pf-c-menu__item-icon">
                                      <i
                                        class="fas fa-fw fa-th"
                                        aria-hidden="true"
                                      ></i>
                                    </span>
                                    <span
                                      class="pf-c-menu__item-text"
                                    >Application launcher</span>
                                  </span>
                                </button>
                              </div>
                              <div class="pf-c-menu__search">
                                <div class="pf-c-menu__search-input">
                                  <div class="pf-c-search-input">
                                    <div class="pf-c-search-input__bar">
                                      <span class="pf-c-search-input__text">
                                        <span class="pf-c-search-input__icon">
                                          <i
                                            class="fas fa-search fa-fw"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <input
                                          class="pf-c-search-input__text-input"
                                          type="text"
                                          placeholder="Search"
                                          aria-label="Search"
                                        />
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Favorites</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Group 1</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                            </div>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-hidden pf-m-visible-on-sm">
              <div
                class="pf-c-dropdown pf-m-full-height"
                style="--pf-c-dropdown--MaxWidth: 20ch;"
              >
                <button
                  class="pf-c-dropdown__toggle"
                  id="table-tabs-example-masthead-profile-button"
                  aria-expanded="false"
                  type="button"
                >
                  <span class="pf-c-dropdown__toggle-image">
                    <img
                      class="pf-c-avatar"
                      src="/assets/images/img_avatar.svg"
                      alt="Avatar image"
                    />
                  </span>
                  <span class="pf-c-dropdown__toggle-text">Ned Username</span>
                  <span class="pf-c-dropdown__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-dropdown__menu" hidden>
                  <section class="pf-c-dropdown__group">
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Account number:</div>
                      <div>123456789</div>
                    </div>
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Username:</div>
                      <div>mshaksho@redhat.com</div>
                    </div>
                  </section>
                  <hr class="pf-c-divider" />
                  <section class="pf-c-dropdown__group">
                    <ul>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">My profile</a>
                      </li>
                      <li>
                        <a
                          class="pf-c-dropdown__menu-item"
                          href="#"
                        >User management</a>
                      </li>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">Logout</a>
                      </li>
                    </ul>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
          <p>This is a full page demo.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-tabs">
      <div class="pf-c-tabs pf-m-page-insets" id="table-tabs-example-tabs-tabs">
        <ul class="pf-c-tabs__list">
          <li class="pf-c-tabs__item pf-m-current">
            <button
              class="pf-c-tabs__link"
              aria-controls="table-tabs-example-tabs-tabs-nodes-panel"
              id="table-tabs-example-tabs-tabs-nodes-link"
            >
              <span class="pf-c-tabs__item-text">Nodes</span>
            </button>
          </li>
          <li class="pf-c-tabs__item">
            <button
              class="pf-c-tabs__link"
              aria-controls="table-tabs-example-tabs-tabs-node-connectors-panel"
              id="table-tabs-example-tabs-tabs-node-connectors-link"
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
        aria-labelledby="table-tabs-example-tabs-tabs-nodes-link"
        id="table-tabs-example-tabs-tabs-nodes-panel"
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
                    id="table-tabs-example-tabs-toolbar"
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
                              aria-controls="table-tabs-example-tabs-toolbar-expandable-content"
                            >
                              <i class="fas fa-filter" aria-hidden="true"></i>
                            </button>
                          </div>

                          <div class="pf-c-toolbar__item">
                            <div class="pf-c-select">
                              <span
                                id="table-tabs-example-tabs-toolbar-select-checkbox-status-label"
                                hidden
                              >Choose one</span>

                              <button
                                class="pf-c-select__toggle"
                                type="button"
                                id="table-tabs-example-tabs-toolbar-select-checkbox-status-toggle"
                                aria-haspopup="true"
                                aria-expanded="false"
                                aria-labelledby="table-tabs-example-tabs-toolbar-select-checkbox-status-label table-tabs-example-tabs-toolbar-select-checkbox-status-toggle"
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
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-active"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-active"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-active"
                                    />

                                    <span class="pf-c-check__label">Active</span>
                                    <span
                                      class="pf-c-check__description"
                                    >This is a description</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-canceled"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-canceled"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-canceled"
                                    />

                                    <span class="pf-c-check__label">Canceled</span>
                                    <span
                                      class="pf-c-check__description"
                                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-paused"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-paused"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-paused"
                                    />

                                    <span class="pf-c-check__label">Paused</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-warning"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-warning"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-warning"
                                    />

                                    <span class="pf-c-check__label">Warning</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-restarted"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-restarted"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-restarted"
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
                          id="table-tabs-example-tabs-toolbar-overflow-menu"
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
                                id="table-tabs-example-tabs-toolbar-overflow-menu-dropdown-toggle"
                                aria-label="Dropdown with additional options"
                                aria-expanded="false"
                              >
                                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                              </button>
                              <ul
                                class="pf-c-dropdown__menu"
                                aria-labelledby="table-tabs-example-tabs-toolbar-overflow-menu-dropdown-toggle"
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
                              <button
                                class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                                type="button"
                                id="table-tabs-example-tabs-toolbar-top-pagination-toggle"
                                aria-haspopup="listbox"
                                aria-expanded="false"
                              >
                                <span class="pf-c-options-menu__toggle-text">
                                  <b>1 - 10</b>&nbsp;of&nbsp;
                                  <b>36</b>
                                </span>
                                <div class="pf-c-options-menu__toggle-icon">
                                  <i
                                    class="fas fa-caret-down"
                                    aria-hidden="true"
                                  ></i>
                                </div>
                              </button>
                              <ul
                                class="pf-c-options-menu__menu"
                                aria-labelledby="table-tabs-example-tabs-toolbar-top-pagination-toggle"
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
                        id="table-tabs-example-tabs-toolbar-expandable-content"
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
                      <button
                        class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                        type="button"
                        id="-footer-pagination-toggle"
                        aria-haspopup="listbox"
                        aria-expanded="false"
                      >
                        <span class="pf-c-options-menu__toggle-text">
                          <b>1 - 10</b>&nbsp;of&nbsp;
                          <b>36</b>
                        </span>
                        <div class="pf-c-options-menu__toggle-icon">
                          <i class="fas fa-caret-down" aria-hidden="true"></i>
                        </div>
                      </button>
                      <ul
                        class="pf-c-options-menu__menu pf-m-top"
                        aria-labelledby="-footer-pagination-toggle"
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
                        <h2 class="pf-c-title pf-m-lg" id="-drawer-label">Node 2</h2>
                      </div>
                      <div class="pf-l-flex__item">
                        <a href="#">siemur/test-space</a>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="pf-c-drawer__body pf-m-no-padding">
                  <div class="pf-c-tabs pf-m-box pf-m-fill" id="-tabs">
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
                          aria-controls="-tabs-tab1-panel"
                          id="-tabs-tab1-link"
                        >
                          <span class="pf-c-tabs__item-text">Overview</span>
                        </button>
                      </li>
                      <li class="pf-c-tabs__item">
                        <button
                          class="pf-c-tabs__link"
                          aria-controls="-tabs-tab2-panel"
                          id="-tabs-tab2-link"
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
                    id="-tabs-tab1-panel"
                    aria-labelledby="-tabs-tab1-link"
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
                            id="-progress-example1"
                          >
                            <div
                              class="pf-c-progress__description"
                              id="-progress-example1-description"
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
                              aria-labelledby="-progress-example1-description"
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
                            id="-progress-example2"
                          >
                            <div
                              class="pf-c-progress__description"
                              id="-progress-example2-description"
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
                              aria-labelledby="-progress-example2-description"
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
                    id="-tabs-tab2-panel"
                    aria-labelledby="-tabs-tab2-link"
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
        aria-labelledby="table-tabs-example-tabs-tabs-node-connectors-link"
        id="table-tabs-example-tabs-tabs-node-connectors-panel"
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
<div class="pf-c-page" id="table-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-table-tabs-example"
  >Skip to content</a>
  <header class="pf-c-masthead" id="table-tabs-example-masthead">
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
        id="table-tabs-example-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div
              class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-spacer-none pf-m-spacer-md-on-md"
            >
              <div class="pf-c-toolbar__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Notifications"
                >
                  <span class="pf-c-notification-badge">
                    <i class="pf-icon-bell" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div
                class="pf-c-toolbar__group pf-m-icon-button-group pf-m-hidden pf-m-visible-on-lg"
              >
                <div class="pf-c-toolbar__item">
                  <nav
                    class="pf-c-app-launcher"
                    aria-label="Application launcher"
                    id="table-tabs-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="table-tabs-example-masthead-icon-group--app-launcher-button"
                      aria-expanded="false"
                      aria-label="Application launcher"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                    <div
                      class="pf-c-app-launcher__menu pf-m-align-right"
                      hidden
                    >
                      <div class="pf-c-app-launcher__menu-search">
                        <div class="pf-c-search-input">
                          <div class="pf-c-search-input__bar">
                            <span class="pf-c-search-input__text">
                              <span class="pf-c-search-input__icon">
                                <i
                                  class="fas fa-search fa-fw"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <input
                                class="pf-c-search-input__text-input"
                                type="text"
                                placeholder="Filter by name"
                                aria-label="Filter by name"
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider" />
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </nav>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="table-tabs-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="table-tabs-example-masthead-settings-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Settings</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Use the beta release</button>
                      </li>
                    </ul>
                  </div>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="table-tabs-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="table-tabs-example-masthead-help-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Support options</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Open support case</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >API documentation</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item pf-m-hidden-on-lg">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-menu-toggle pf-m-plain"
                    type="button"
                    aria-expanded="false"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <div class="pf-c-menu pf-m-drilldown pf-m-align-right" hidden>
                    <div class="pf-c-menu__content">
                      <section class="pf-c-menu__group pf-m-hidden-on-sm">
                        <ul class="pf-c-menu__list" role="menu">
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Username:</div>
                                <div class="pf-u-font-size-md">ned_username</div>
                              </span>
                            </button>
                          </li>
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Account number:</div>
                                <div class="pf-u-font-size-md">123456789</div>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-divider" role="separator"></li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">My profile</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span
                                  class="pf-c-menu__item-text"
                                >User management</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">Logout</span>
                              </span>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider pf-m-hidden-on-sm" />
                      <section class="pf-c-menu__group">
                        <ul class="pf-c-menu__list" role="menu">
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-cog"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Settings</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-cog"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Settings</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Customer support</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span class="pf-c-menu__item-text">About</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-pf-icon pf-icon-help"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Help</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-pf-icon pf-icon-help"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span class="pf-c-menu__item-text">Help</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Support options</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Open support case</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >API documentation</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i class="fas fa-fw fa-th" aria-hidden="true"></i>
                                </span>
                                <span
                                  class="pf-c-menu__item-text"
                                >Application launcher</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__header">
                                <button
                                  class="pf-c-menu__item"
                                  type="button"
                                  role="menuitem"
                                >
                                  <span class="pf-c-menu__item-main">
                                    <span class="pf-c-menu__item-toggle-icon">
                                      <i class="fas fa-angle-left"></i>
                                    </span>
                                    <span class="pf-c-menu__item-icon">
                                      <i
                                        class="fas fa-fw fa-th"
                                        aria-hidden="true"
                                      ></i>
                                    </span>
                                    <span
                                      class="pf-c-menu__item-text"
                                    >Application launcher</span>
                                  </span>
                                </button>
                              </div>
                              <div class="pf-c-menu__search">
                                <div class="pf-c-menu__search-input">
                                  <div class="pf-c-search-input">
                                    <div class="pf-c-search-input__bar">
                                      <span class="pf-c-search-input__text">
                                        <span class="pf-c-search-input__icon">
                                          <i
                                            class="fas fa-search fa-fw"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <input
                                          class="pf-c-search-input__text-input"
                                          type="text"
                                          placeholder="Search"
                                          aria-label="Search"
                                        />
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Favorites</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Group 1</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                            </div>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-hidden pf-m-visible-on-sm">
              <div
                class="pf-c-dropdown pf-m-full-height"
                style="--pf-c-dropdown--MaxWidth: 20ch;"
              >
                <button
                  class="pf-c-dropdown__toggle"
                  id="table-tabs-example-masthead-profile-button"
                  aria-expanded="false"
                  type="button"
                >
                  <span class="pf-c-dropdown__toggle-image">
                    <img
                      class="pf-c-avatar"
                      src="/assets/images/img_avatar.svg"
                      alt="Avatar image"
                    />
                  </span>
                  <span class="pf-c-dropdown__toggle-text">Ned Username</span>
                  <span class="pf-c-dropdown__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-dropdown__menu" hidden>
                  <section class="pf-c-dropdown__group">
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Account number:</div>
                      <div>123456789</div>
                    </div>
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Username:</div>
                      <div>mshaksho@redhat.com</div>
                    </div>
                  </section>
                  <hr class="pf-c-divider" />
                  <section class="pf-c-dropdown__group">
                    <ul>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">My profile</a>
                      </li>
                      <li>
                        <a
                          class="pf-c-dropdown__menu-item"
                          href="#"
                        >User management</a>
                      </li>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">Logout</a>
                      </li>
                    </ul>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
          <p>This is a full page demo.</p>
        </div>
      </div>
    </section>

    <section class="pf-c-page__main-tabs">
      <div class="pf-c-tabs pf-m-page-insets" id="table-tabs-example-tabs-tabs">
        <ul class="pf-c-tabs__list">
          <li class="pf-c-tabs__item pf-m-current">
            <button
              class="pf-c-tabs__link"
              aria-controls="table-tabs-example-tabs-tabs-nodes-panel"
              id="table-tabs-example-tabs-tabs-nodes-link"
            >
              <span class="pf-c-tabs__item-text">Nodes</span>
            </button>
          </li>
          <li class="pf-c-tabs__item">
            <button
              class="pf-c-tabs__link"
              aria-controls="table-tabs-example-tabs-tabs-node-connectors-panel"
              id="table-tabs-example-tabs-tabs-node-connectors-link"
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
        aria-labelledby="table-tabs-example-tabs-tabs-nodes-link"
        id="table-tabs-example-tabs-tabs-nodes-panel"
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
                    id="table-tabs-example-tabs-toolbar"
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
                              aria-controls="table-tabs-example-tabs-toolbar-expandable-content"
                            >
                              <i class="fas fa-filter" aria-hidden="true"></i>
                            </button>
                          </div>

                          <div class="pf-c-toolbar__item">
                            <div class="pf-c-select">
                              <span
                                id="table-tabs-example-tabs-toolbar-select-checkbox-status-label"
                                hidden
                              >Choose one</span>

                              <button
                                class="pf-c-select__toggle"
                                type="button"
                                id="table-tabs-example-tabs-toolbar-select-checkbox-status-toggle"
                                aria-haspopup="true"
                                aria-expanded="false"
                                aria-labelledby="table-tabs-example-tabs-toolbar-select-checkbox-status-label table-tabs-example-tabs-toolbar-select-checkbox-status-toggle"
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
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-active"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-active"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-active"
                                    />

                                    <span class="pf-c-check__label">Active</span>
                                    <span
                                      class="pf-c-check__description"
                                    >This is a description</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item pf-m-description"
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-canceled"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-canceled"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-canceled"
                                    />

                                    <span class="pf-c-check__label">Canceled</span>
                                    <span
                                      class="pf-c-check__description"
                                    >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-paused"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-paused"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-paused"
                                    />

                                    <span class="pf-c-check__label">Paused</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-warning"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-warning"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-warning"
                                    />

                                    <span class="pf-c-check__label">Warning</span>
                                  </label>
                                  <label
                                    class="pf-c-check pf-c-select__menu-item"
                                    for="table-tabs-example-tabs-toolbar-select-checkbox-status-restarted"
                                  >
                                    <input
                                      class="pf-c-check__input"
                                      type="checkbox"
                                      id="table-tabs-example-tabs-toolbar-select-checkbox-status-restarted"
                                      name="table-tabs-example-tabs-toolbar-select-checkbox-status-restarted"
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
                          id="table-tabs-example-tabs-toolbar-overflow-menu"
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
                                id="table-tabs-example-tabs-toolbar-overflow-menu-dropdown-toggle"
                                aria-label="Dropdown with additional options"
                                aria-expanded="false"
                              >
                                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                              </button>
                              <ul
                                class="pf-c-dropdown__menu"
                                aria-labelledby="table-tabs-example-tabs-toolbar-overflow-menu-dropdown-toggle"
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
                              <button
                                class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                                type="button"
                                id="table-tabs-example-tabs-toolbar-top-pagination-toggle"
                                aria-haspopup="listbox"
                                aria-expanded="false"
                              >
                                <span class="pf-c-options-menu__toggle-text">
                                  <b>1 - 10</b>&nbsp;of&nbsp;
                                  <b>36</b>
                                </span>
                                <div class="pf-c-options-menu__toggle-icon">
                                  <i
                                    class="fas fa-caret-down"
                                    aria-hidden="true"
                                  ></i>
                                </div>
                              </button>
                              <ul
                                class="pf-c-options-menu__menu"
                                aria-labelledby="table-tabs-example-tabs-toolbar-top-pagination-toggle"
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
                        id="table-tabs-example-tabs-toolbar-expandable-content"
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
                      <button
                        class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                        type="button"
                        id="-footer-pagination-toggle"
                        aria-haspopup="listbox"
                        aria-expanded="false"
                      >
                        <span class="pf-c-options-menu__toggle-text">
                          <b>1 - 10</b>&nbsp;of&nbsp;
                          <b>36</b>
                        </span>
                        <div class="pf-c-options-menu__toggle-icon">
                          <i class="fas fa-caret-down" aria-hidden="true"></i>
                        </div>
                      </button>
                      <ul
                        class="pf-c-options-menu__menu pf-m-top"
                        aria-labelledby="-footer-pagination-toggle"
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
                        <h2 class="pf-c-title pf-m-lg" id="-drawer-label">Node 2</h2>
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
                    id="-tabs"
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
                          aria-controls="-tabs-tab1-panel"
                          id="-tabs-tab1-link"
                        >
                          <span class="pf-c-tabs__item-text">Overview</span>
                        </button>
                      </li>
                      <li class="pf-c-tabs__item">
                        <button
                          class="pf-c-tabs__link"
                          aria-controls="-tabs-tab2-panel"
                          id="-tabs-tab2-link"
                        >
                          <span class="pf-c-tabs__item-text">Activity</span>
                        </button>
                      </li>
                      <li class="pf-c-tabs__item">
                        <button
                          class="pf-c-tabs__link"
                          aria-controls="-tabs-tab3-panel"
                          id="-tabs-tab3-link"
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
                    id="-tabs-tab1-panel"
                    aria-labelledby="-tabs-tab1-link"
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
                            id="-progress-example1"
                          >
                            <div
                              class="pf-c-progress__description"
                              id="-progress-example1-description"
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
                              aria-labelledby="-progress-example1-description"
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
                            id="-progress-example2"
                          >
                            <div
                              class="pf-c-progress__description"
                              id="-progress-example2-description"
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
                              aria-labelledby="-progress-example2-description"
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
                    id="-tabs-tab2-panel"
                    aria-labelledby="-tabs-tab2-link"
                    role="tabpanel"
                    tabindex="0"
                    hidden
                  >
                    <div class="pf-c-tab-content__body">Panel 2</div>
                  </section>
                  <section
                    class="pf-c-tab-content"
                    id="-tabs-tab3-panel"
                    aria-labelledby="-tabs-tab3-link"
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
        aria-labelledby="table-tabs-example-tabs-tabs-node-connectors-link"
        id="table-tabs-example-tabs-tabs-node-connectors-panel"
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
              class="pf-c-tabs pf-m-inset-none pf-m-secondary"
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

<div class="pf-c-page" id="modal-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-modal-tabs-example"
  >Skip to content</a>
  <header class="pf-c-masthead" id="modal-tabs-example-masthead">
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
        id="modal-tabs-example-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div
              class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-spacer-none pf-m-spacer-md-on-md"
            >
              <div class="pf-c-toolbar__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Notifications"
                >
                  <span class="pf-c-notification-badge">
                    <i class="pf-icon-bell" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div
                class="pf-c-toolbar__group pf-m-icon-button-group pf-m-hidden pf-m-visible-on-lg"
              >
                <div class="pf-c-toolbar__item">
                  <nav
                    class="pf-c-app-launcher"
                    aria-label="Application launcher"
                    id="modal-tabs-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="modal-tabs-example-masthead-icon-group--app-launcher-button"
                      aria-expanded="false"
                      aria-label="Application launcher"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                    <div
                      class="pf-c-app-launcher__menu pf-m-align-right"
                      hidden
                    >
                      <div class="pf-c-app-launcher__menu-search">
                        <div class="pf-c-search-input">
                          <div class="pf-c-search-input__bar">
                            <span class="pf-c-search-input__text">
                              <span class="pf-c-search-input__icon">
                                <i
                                  class="fas fa-search fa-fw"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <input
                                class="pf-c-search-input__text-input"
                                type="text"
                                placeholder="Filter by name"
                                aria-label="Filter by name"
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider" />
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </nav>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="modal-tabs-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="modal-tabs-example-masthead-settings-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Settings</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Use the beta release</button>
                      </li>
                    </ul>
                  </div>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="modal-tabs-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="modal-tabs-example-masthead-help-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Support options</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Open support case</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >API documentation</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item pf-m-hidden-on-lg">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-menu-toggle pf-m-plain"
                    type="button"
                    aria-expanded="false"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <div class="pf-c-menu pf-m-drilldown pf-m-align-right" hidden>
                    <div class="pf-c-menu__content">
                      <section class="pf-c-menu__group pf-m-hidden-on-sm">
                        <ul class="pf-c-menu__list" role="menu">
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Username:</div>
                                <div class="pf-u-font-size-md">ned_username</div>
                              </span>
                            </button>
                          </li>
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Account number:</div>
                                <div class="pf-u-font-size-md">123456789</div>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-divider" role="separator"></li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">My profile</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span
                                  class="pf-c-menu__item-text"
                                >User management</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">Logout</span>
                              </span>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider pf-m-hidden-on-sm" />
                      <section class="pf-c-menu__group">
                        <ul class="pf-c-menu__list" role="menu">
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-cog"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Settings</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-cog"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Settings</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Customer support</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span class="pf-c-menu__item-text">About</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-pf-icon pf-icon-help"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Help</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-pf-icon pf-icon-help"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span class="pf-c-menu__item-text">Help</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Support options</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Open support case</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >API documentation</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i class="fas fa-fw fa-th" aria-hidden="true"></i>
                                </span>
                                <span
                                  class="pf-c-menu__item-text"
                                >Application launcher</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__header">
                                <button
                                  class="pf-c-menu__item"
                                  type="button"
                                  role="menuitem"
                                >
                                  <span class="pf-c-menu__item-main">
                                    <span class="pf-c-menu__item-toggle-icon">
                                      <i class="fas fa-angle-left"></i>
                                    </span>
                                    <span class="pf-c-menu__item-icon">
                                      <i
                                        class="fas fa-fw fa-th"
                                        aria-hidden="true"
                                      ></i>
                                    </span>
                                    <span
                                      class="pf-c-menu__item-text"
                                    >Application launcher</span>
                                  </span>
                                </button>
                              </div>
                              <div class="pf-c-menu__search">
                                <div class="pf-c-menu__search-input">
                                  <div class="pf-c-search-input">
                                    <div class="pf-c-search-input__bar">
                                      <span class="pf-c-search-input__text">
                                        <span class="pf-c-search-input__icon">
                                          <i
                                            class="fas fa-search fa-fw"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <input
                                          class="pf-c-search-input__text-input"
                                          type="text"
                                          placeholder="Search"
                                          aria-label="Search"
                                        />
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Favorites</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Group 1</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                            </div>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-hidden pf-m-visible-on-sm">
              <div
                class="pf-c-dropdown pf-m-full-height"
                style="--pf-c-dropdown--MaxWidth: 20ch;"
              >
                <button
                  class="pf-c-dropdown__toggle"
                  id="modal-tabs-example-masthead-profile-button"
                  aria-expanded="false"
                  type="button"
                >
                  <span class="pf-c-dropdown__toggle-image">
                    <img
                      class="pf-c-avatar"
                      src="/assets/images/img_avatar.svg"
                      alt="Avatar image"
                    />
                  </span>
                  <span class="pf-c-dropdown__toggle-text">Ned Username</span>
                  <span class="pf-c-dropdown__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-dropdown__menu" hidden>
                  <section class="pf-c-dropdown__group">
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Account number:</div>
                      <div>123456789</div>
                    </div>
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Username:</div>
                      <div>mshaksho@redhat.com</div>
                    </div>
                  </section>
                  <hr class="pf-c-divider" />
                  <section class="pf-c-dropdown__group">
                    <ul>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">My profile</a>
                      </li>
                      <li>
                        <a
                          class="pf-c-dropdown__menu-item"
                          href="#"
                        >User management</a>
                      </li>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">Logout</a>
                      </li>
                    </ul>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">
      <nav
        class="pf-c-nav"
        id="modal-tabs-example-primary-nav"
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
    id="main-content-modal-tabs-example"
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
          <p>This is a full page demo.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-light pf-m-no-padding">
      <div class="pf-c-toolbar pf-m-page-insets" id="-toolbar">
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section pf-m-nowrap">
            <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-xl">
              <div class="pf-c-toolbar__toggle">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Show filters"
                  aria-expanded="false"
                  aria-controls="-toolbar-expandable-content"
                >
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-toolbar__item pf-m-bulk-select">
                <div class="pf-c-dropdown">
                  <div class="pf-c-dropdown__toggle pf-m-split-button">
                    <label
                      class="pf-c-dropdown__toggle-check"
                      for="-toolbar-bulk-select-toggle-check"
                    >
                      <input
                        type="checkbox"
                        id="-toolbar-bulk-select-toggle-check"
                        aria-label="Select all"
                      />
                    </label>

                    <button
                      class="pf-c-dropdown__toggle-button"
                      type="button"
                      aria-expanded="false"
                      id="-toolbar-bulk-select-toggle-button"
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
                  <span
                    id="-toolbar-select-checkbox-status-label"
                    hidden
                  >Choose one</span>

                  <button
                    class="pf-c-select__toggle"
                    type="button"
                    id="-toolbar-select-checkbox-status-toggle"
                    aria-haspopup="true"
                    aria-expanded="false"
                    aria-labelledby="-toolbar-select-checkbox-status-label -toolbar-select-checkbox-status-toggle"
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
                        for="-toolbar-select-checkbox-status-active"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-toolbar-select-checkbox-status-active"
                          name="-toolbar-select-checkbox-status-active"
                        />

                        <span class="pf-c-check__label">Active</span>
                        <span
                          class="pf-c-check__description"
                        >This is a description</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item pf-m-description"
                        for="-toolbar-select-checkbox-status-canceled"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-toolbar-select-checkbox-status-canceled"
                          name="-toolbar-select-checkbox-status-canceled"
                        />

                        <span class="pf-c-check__label">Canceled</span>
                        <span
                          class="pf-c-check__description"
                        >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="-toolbar-select-checkbox-status-paused"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-toolbar-select-checkbox-status-paused"
                          name="-toolbar-select-checkbox-status-paused"
                        />

                        <span class="pf-c-check__label">Paused</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="-toolbar-select-checkbox-status-warning"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-toolbar-select-checkbox-status-warning"
                          name="-toolbar-select-checkbox-status-warning"
                        />

                        <span class="pf-c-check__label">Warning</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="-toolbar-select-checkbox-status-restarted"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="-toolbar-select-checkbox-status-restarted"
                          name="-toolbar-select-checkbox-status-restarted"
                        />

                        <span class="pf-c-check__label">Restarted</span>
                      </label>
                    </fieldset>
                  </div>
                </div>
              </div>
            </div>

            <div class="pf-c-overflow-menu" id="-toolbar-overflow-menu">
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
                    id="-toolbar-overflow-menu-dropdown-toggle"
                    aria-label="Dropdown with additional options"
                    aria-expanded="false"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu"
                    aria-labelledby="-toolbar-overflow-menu-dropdown-toggle"
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
                  <button
                    class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
                    type="button"
                    id="-toolbar-top-pagination-toggle"
                    aria-haspopup="listbox"
                    aria-expanded="false"
                  >
                    <span class="pf-c-options-menu__toggle-text">
                      <b>1 - 10</b>&nbsp;of&nbsp;
                      <b>36</b>
                    </span>
                    <div class="pf-c-options-menu__toggle-icon">
                      <i class="fas fa-caret-down" aria-hidden="true"></i>
                    </div>
                  </button>
                  <ul
                    class="pf-c-options-menu__menu"
                    aria-labelledby="-toolbar-top-pagination-toggle"
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
            id="-toolbar-expandable-content"
            hidden
          ></div>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-fill">
      <div class="pf-l-gallery pf-m-gutter">
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
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
        <div class="pf-c-card pf-m-selectable-raised pf-m-compact" id="card-1">
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
        <div class="pf-c-card pf-m-selectable-raised pf-m-compact" id="card-2">
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
        <div class="pf-c-card pf-m-selectable-raised pf-m-compact" id="card-3">
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
        <div class="pf-c-card pf-m-selectable-raised pf-m-compact" id="card-4">
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
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-selected-raised pf-m-compact"
          id="card-5"
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
        <div
          class="pf-c-card pf-m-non-selectable-raised pf-m-compact"
          id="card-6"
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
                  disabled
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
        <div class="pf-c-card pf-m-selectable-raised pf-m-compact" id="card-7">
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
        <div class="pf-c-card pf-m-selectable-raised pf-m-compact" id="card-8">
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
        <div class="pf-c-card pf-m-selectable-raised pf-m-compact" id="card-9">
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
        <div class="pf-c-card pf-m-selectable-raised pf-m-compact" id="card-10">
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
          <button
            class="pf-c-options-menu__toggle pf-m-text pf-m-plain"
            type="button"
            id="-bottom-pagination-toggle"
            aria-haspopup="listbox"
            aria-expanded="false"
          >
            <span class="pf-c-options-menu__toggle-text">
              <b>1 - 10</b>&nbsp;of&nbsp;
              <b>36</b>
            </span>
            <div class="pf-c-options-menu__toggle-icon">
              <i class="fas fa-caret-down" aria-hidden="true"></i>
            </div>
          </button>
          <ul
            class="pf-c-options-menu__menu pf-m-top"
            aria-labelledby="-bottom-pagination-toggle"
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
  <header class="pf-c-masthead" id="gray-tabs-example-masthead">
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
        id="gray-tabs-example-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div
              class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-spacer-none pf-m-spacer-md-on-md"
            >
              <div class="pf-c-toolbar__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Notifications"
                >
                  <span class="pf-c-notification-badge">
                    <i class="pf-icon-bell" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div
                class="pf-c-toolbar__group pf-m-icon-button-group pf-m-hidden pf-m-visible-on-lg"
              >
                <div class="pf-c-toolbar__item">
                  <nav
                    class="pf-c-app-launcher"
                    aria-label="Application launcher"
                    id="gray-tabs-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="gray-tabs-example-masthead-icon-group--app-launcher-button"
                      aria-expanded="false"
                      aria-label="Application launcher"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                    <div
                      class="pf-c-app-launcher__menu pf-m-align-right"
                      hidden
                    >
                      <div class="pf-c-app-launcher__menu-search">
                        <div class="pf-c-search-input">
                          <div class="pf-c-search-input__bar">
                            <span class="pf-c-search-input__text">
                              <span class="pf-c-search-input__icon">
                                <i
                                  class="fas fa-search fa-fw"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <input
                                class="pf-c-search-input__text-input"
                                type="text"
                                placeholder="Filter by name"
                                aria-label="Filter by name"
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider" />
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </nav>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="gray-tabs-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="gray-tabs-example-masthead-settings-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Settings</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Use the beta release</button>
                      </li>
                    </ul>
                  </div>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="gray-tabs-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="gray-tabs-example-masthead-help-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Support options</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Open support case</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >API documentation</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item pf-m-hidden-on-lg">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-menu-toggle pf-m-plain"
                    type="button"
                    aria-expanded="false"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <div class="pf-c-menu pf-m-drilldown pf-m-align-right" hidden>
                    <div class="pf-c-menu__content">
                      <section class="pf-c-menu__group pf-m-hidden-on-sm">
                        <ul class="pf-c-menu__list" role="menu">
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Username:</div>
                                <div class="pf-u-font-size-md">ned_username</div>
                              </span>
                            </button>
                          </li>
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Account number:</div>
                                <div class="pf-u-font-size-md">123456789</div>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-divider" role="separator"></li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">My profile</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span
                                  class="pf-c-menu__item-text"
                                >User management</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">Logout</span>
                              </span>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider pf-m-hidden-on-sm" />
                      <section class="pf-c-menu__group">
                        <ul class="pf-c-menu__list" role="menu">
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-cog"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Settings</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-cog"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Settings</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Customer support</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span class="pf-c-menu__item-text">About</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-pf-icon pf-icon-help"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Help</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-pf-icon pf-icon-help"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span class="pf-c-menu__item-text">Help</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Support options</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Open support case</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >API documentation</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i class="fas fa-fw fa-th" aria-hidden="true"></i>
                                </span>
                                <span
                                  class="pf-c-menu__item-text"
                                >Application launcher</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__header">
                                <button
                                  class="pf-c-menu__item"
                                  type="button"
                                  role="menuitem"
                                >
                                  <span class="pf-c-menu__item-main">
                                    <span class="pf-c-menu__item-toggle-icon">
                                      <i class="fas fa-angle-left"></i>
                                    </span>
                                    <span class="pf-c-menu__item-icon">
                                      <i
                                        class="fas fa-fw fa-th"
                                        aria-hidden="true"
                                      ></i>
                                    </span>
                                    <span
                                      class="pf-c-menu__item-text"
                                    >Application launcher</span>
                                  </span>
                                </button>
                              </div>
                              <div class="pf-c-menu__search">
                                <div class="pf-c-menu__search-input">
                                  <div class="pf-c-search-input">
                                    <div class="pf-c-search-input__bar">
                                      <span class="pf-c-search-input__text">
                                        <span class="pf-c-search-input__icon">
                                          <i
                                            class="fas fa-search fa-fw"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <input
                                          class="pf-c-search-input__text-input"
                                          type="text"
                                          placeholder="Search"
                                          aria-label="Search"
                                        />
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Favorites</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Group 1</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                            </div>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-hidden pf-m-visible-on-sm">
              <div
                class="pf-c-dropdown pf-m-full-height"
                style="--pf-c-dropdown--MaxWidth: 20ch;"
              >
                <button
                  class="pf-c-dropdown__toggle"
                  id="gray-tabs-example-masthead-profile-button"
                  aria-expanded="false"
                  type="button"
                >
                  <span class="pf-c-dropdown__toggle-image">
                    <img
                      class="pf-c-avatar"
                      src="/assets/images/img_avatar.svg"
                      alt="Avatar image"
                    />
                  </span>
                  <span class="pf-c-dropdown__toggle-text">Ned Username</span>
                  <span class="pf-c-dropdown__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-dropdown__menu" hidden>
                  <section class="pf-c-dropdown__group">
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Account number:</div>
                      <div>123456789</div>
                    </div>
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Username:</div>
                      <div>mshaksho@redhat.com</div>
                    </div>
                  </section>
                  <hr class="pf-c-divider" />
                  <section class="pf-c-dropdown__group">
                    <ul>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">My profile</a>
                      </li>
                      <li>
                        <a
                          class="pf-c-dropdown__menu-item"
                          href="#"
                        >User management</a>
                      </li>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">Logout</a>
                      </li>
                    </ul>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
          <p>This is a full page demo.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-box pf-m-page-insets pf-m-color-scheme--light-300"
          id="gray-tabs-example-tabs-tabs"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-tabs-cluster-1-panel"
                id="gray-tabs-example-tabs-tabs-cluster-1-link"
              >
                <span class="pf-c-tabs__item-text">Cluster 1</span>
              </button>
            </li>
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-tabs-cluster-2-panel"
                id="gray-tabs-example-tabs-tabs-cluster-2-link"
              >
                <span class="pf-c-tabs__item-text">Cluster 2</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-tabs-cluster-3-panel"
                id="gray-tabs-example-tabs-tabs-cluster-3-link"
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
          aria-labelledby="gray-tabs-example-tabs-tabs-cluster-1-link"
          id="gray-tabs-example-tabs-tabs-cluster-1-panel"
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
          aria-labelledby="gray-tabs-example-tabs-tabs-cluster-2-link"
          id="gray-tabs-example-tabs-tabs-cluster-2-panel"
          role="tabpanel"
          tabindex="0"
        >
          <div class="pf-c-tab-content__body">
            <div class="pf-l-grid pf-m-gutter">
              <div class="pf-l-grid__item pf-m-6-col-on-md pf-m-8-col-on-xl">
                <div class="pf-c-card pf-m-full-height">
                  <div class="pf-c-card__header">
                    <h2 class="pf-c-title pf-m-lg">Status</h2>
                  </div>
                  <div class="pf-c-card__body">
                    <div class="pf-l-flex pf-m-column">
                      <div class="pf-l-flex__item">
                        <div
                          class="pf-c-tabs pf-m-secondary"
                          id="gray-tabs-example-tabs-tabs-subtabs"
                        >
                          <ul class="pf-c-tabs__list">
                            <li class="pf-c-tabs__item pf-m-current">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="gray-tabs-example-tabs-tabs-subtabs-cluster-panel"
                                id="gray-tabs-example-tabs-tabs-subtabs-cluster-link"
                              >
                                <span class="pf-c-tabs__item-text">Cluster</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="gray-tabs-example-tabs-tabs-subtabs-control-plane-panel"
                                id="gray-tabs-example-tabs-tabs-subtabs-control-plane-link"
                              >
                                <span class="pf-c-tabs__item-text">Control plane</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="gray-tabs-example-tabs-tabs-subtabs-operators-panel"
                                id="gray-tabs-example-tabs-tabs-subtabs-operators-link"
                              >
                                <span class="pf-c-tabs__item-text">Operators</span>
                              </button>
                            </li>
                            <li class="pf-c-tabs__item">
                              <button
                                class="pf-c-tabs__link"
                                aria-controls="gray-tabs-example-tabs-tabs-subtabs-virtualization-panel"
                                id="gray-tabs-example-tabs-tabs-subtabs-virtualization-link"
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
                          aria-labelledby="gray-tabs-example-tabs-tabs-subtabs-cluster-link"
                          id="gray-tabs-example-tabs-tabs-subtabs-cluster-panel"
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
                          aria-labelledby="gray-tabs-example-tabs-tabs-subtabs-control-plane-link"
                          id="gray-tabs-example-tabs-tabs-subtabs-control-plane-panel"
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
                          aria-labelledby="gray-tabs-example-tabs-tabs-subtabs-operators-link"
                          id="gray-tabs-example-tabs-tabs-subtabs-operators-panel"
                          role="tabpanel"
                          tabindex="0"
                          hidden
                        >
                          <div class="pf-c-tab-content__body">Operators content</div>
                        </section>
                        <section
                          class="pf-c-tab-content"
                          aria-labelledby="gray-tabs-example-tabs-tabs-subtabs-virtualization-link"
                          id="gray-tabs-example-tabs-tabs-subtabs-virtualization-panel"
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
                        <h3 class="pf-c-title pf-m-lg">Title of card</h3>
                      </div>
                    </div>
                  </div>
                  <div class="pf-l-flex__item pf-m-flex-1">
                    <div class="pf-c-card pf-m-full-height">
                      <div class="pf-c-card__header">
                        <h3 class="pf-c-title pf-m-lg">Title of card</h3>
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
          aria-labelledby="gray-tabs-example-tabs-tabs-cluster-3-link"
          id="gray-tabs-example-tabs-tabs-cluster-3-panel"
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
<div class="pf-c-page" id="gray-tabs-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-gray-tabs-example"
  >Skip to content</a>
  <header class="pf-c-masthead" id="gray-tabs-example-masthead">
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
        id="gray-tabs-example-masthead-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section">
            <div
              class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-spacer-none pf-m-spacer-md-on-md"
            >
              <div class="pf-c-toolbar__item">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Notifications"
                >
                  <span class="pf-c-notification-badge">
                    <i class="pf-icon-bell" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div
                class="pf-c-toolbar__group pf-m-icon-button-group pf-m-hidden pf-m-visible-on-lg"
              >
                <div class="pf-c-toolbar__item">
                  <nav
                    class="pf-c-app-launcher"
                    aria-label="Application launcher"
                    id="gray-tabs-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="gray-tabs-example-masthead-icon-group--app-launcher-button"
                      aria-expanded="false"
                      aria-label="Application launcher"
                    >
                      <i class="fas fa-th" aria-hidden="true"></i>
                    </button>
                    <div
                      class="pf-c-app-launcher__menu pf-m-align-right"
                      hidden
                    >
                      <div class="pf-c-app-launcher__menu-search">
                        <div class="pf-c-search-input">
                          <div class="pf-c-search-input__bar">
                            <span class="pf-c-search-input__text">
                              <span class="pf-c-search-input__icon">
                                <i
                                  class="fas fa-search fa-fw"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <input
                                class="pf-c-search-input__text-input"
                                type="text"
                                placeholder="Filter by name"
                                aria-label="Filter by name"
                              />
                            </span>
                          </div>
                        </div>
                      </div>
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Favorites</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider" />
                      <section class="pf-c-app-launcher__group">
                        <h1 class="pf-c-app-launcher__group-title">Group 1</h1>
                        <ul>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 1
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                          <li
                            class="pf-c-app-launcher__menu-wrapper pf-m-external pf-m-favorite"
                          >
                            <a class="pf-c-app-launcher__menu-item">
                              Link 2
                              <span
                                class="pf-c-app-launcher__menu-item-external-icon"
                              >
                                <i
                                  class="fas fa-external-link-alt"
                                  aria-hidden="true"
                                ></i>
                              </span>
                              <span class="pf-screen-reader">(opens new window)</span>
                            </a>
                            <button
                              class="pf-c-app-launcher__menu-item pf-m-action"
                              type="button"
                              aria-label="Favorite"
                            >
                              <i class="fas fa-star" aria-hidden="true"></i>
                            </button>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </nav>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="gray-tabs-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="gray-tabs-example-masthead-settings-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Settings</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Use the beta release</button>
                      </li>
                    </ul>
                  </div>
                </div>
                <div class="pf-c-toolbar__item">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="gray-tabs-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="gray-tabs-example-masthead-help-button"
                      hidden
                    >
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Support options</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >Open support case</button>
                      </li>
                      <li>
                        <button
                          class="pf-c-dropdown__menu-item"
                          type="button"
                        >API documentation</button>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
              <div class="pf-c-toolbar__item pf-m-hidden-on-lg">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-menu-toggle pf-m-plain"
                    type="button"
                    aria-expanded="false"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <div class="pf-c-menu pf-m-drilldown pf-m-align-right" hidden>
                    <div class="pf-c-menu__content">
                      <section class="pf-c-menu__group pf-m-hidden-on-sm">
                        <ul class="pf-c-menu__list" role="menu">
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Username:</div>
                                <div class="pf-u-font-size-md">ned_username</div>
                              </span>
                            </button>
                          </li>
                          <li
                            class="pf-c-menu__list-item pf-m-disabled"
                            role="none"
                          >
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              disabled
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-description">
                                <div class="pf-u-font-size-sm">Account number:</div>
                                <div class="pf-u-font-size-md">123456789</div>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-divider" role="separator"></li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">My profile</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span
                                  class="pf-c-menu__item-text"
                                >User management</span>
                              </span>
                            </button>
                          </li>
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-text">Logout</span>
                              </span>
                            </button>
                          </li>
                        </ul>
                      </section>
                      <hr class="pf-c-divider pf-m-hidden-on-sm" />
                      <section class="pf-c-menu__group">
                        <ul class="pf-c-menu__list" role="menu">
                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-cog"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Settings</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-cog"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Settings</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Customer support</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span class="pf-c-menu__item-text">About</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                              aria-expanded="false"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i
                                    class="fas fa-fw fa-pf-icon pf-icon-help"
                                    aria-hidden="true"
                                  ></i>
                                </span>
                                <span class="pf-c-menu__item-text">Help</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__content">
                                <ul class="pf-c-menu__list" role="menu">
                                  <li
                                    class="pf-c-menu__list-item pf-m-drill-up"
                                    role="none"
                                  >
                                    <button
                                      class="pf-c-menu__item"
                                      type="button"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-toggle-icon"
                                        >
                                          <i class="fas fa-angle-left"></i>
                                        </span>
                                        <span class="pf-c-menu__item-icon">
                                          <i
                                            class="fas fa-fw fa-pf-icon pf-icon-help"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span class="pf-c-menu__item-text">Help</span>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-divider" role="separator"></li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Support options</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Open support case</span>
                                      </span>
                                    </a>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >API documentation</span>
                                      </span>
                                    </a>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </li>

                          <li class="pf-c-menu__list-item" role="none">
                            <button
                              class="pf-c-menu__item"
                              type="button"
                              role="menuitem"
                            >
                              <span class="pf-c-menu__item-main">
                                <span class="pf-c-menu__item-icon">
                                  <i class="fas fa-fw fa-th" aria-hidden="true"></i>
                                </span>
                                <span
                                  class="pf-c-menu__item-text"
                                >Application launcher</span>
                                <span class="pf-c-menu__item-toggle-icon">
                                  <i class="fas fa-angle-right"></i>
                                </span>
                              </span>
                            </button>
                            <div class="pf-c-menu" hidden>
                              <div class="pf-c-menu__header">
                                <button
                                  class="pf-c-menu__item"
                                  type="button"
                                  role="menuitem"
                                >
                                  <span class="pf-c-menu__item-main">
                                    <span class="pf-c-menu__item-toggle-icon">
                                      <i class="fas fa-angle-left"></i>
                                    </span>
                                    <span class="pf-c-menu__item-icon">
                                      <i
                                        class="fas fa-fw fa-th"
                                        aria-hidden="true"
                                      ></i>
                                    </span>
                                    <span
                                      class="pf-c-menu__item-text"
                                    >Application launcher</span>
                                  </span>
                                </button>
                              </div>
                              <div class="pf-c-menu__search">
                                <div class="pf-c-menu__search-input">
                                  <div class="pf-c-search-input">
                                    <div class="pf-c-search-input__bar">
                                      <span class="pf-c-search-input__text">
                                        <span class="pf-c-search-input__icon">
                                          <i
                                            class="fas fa-search fa-fw"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <input
                                          class="pf-c-search-input__text-input"
                                          type="text"
                                          placeholder="Search"
                                          aria-label="Search"
                                        />
                                      </span>
                                    </div>
                                  </div>
                                </div>
                              </div>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Favorites</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                              <hr class="pf-c-divider" />
                              <section class="pf-c-menu__group">
                                <h1 class="pf-c-menu__group-title">Group 1</h1>
                                <ul class="pf-c-menu__list" role="menu">
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 1</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite"
                                      type="button"
                                      aria-label="Not starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                  <li class="pf-c-menu__list-item" role="none">
                                    <a
                                      class="pf-c-menu__item"
                                      href="#"
                                      role="menuitem"
                                      target="_blank"
                                    >
                                      <span class="pf-c-menu__item-main">
                                        <span
                                          class="pf-c-menu__item-text"
                                        >Link 2</span>
                                        <span
                                          class="pf-c-menu__item-external-icon"
                                        >
                                          <i
                                            class="fas fa-external-link-alt"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        <span
                                          class="pf-screen-reader"
                                        >(opens new window)</span>
                                      </span>
                                    </a>
                                    <button
                                      class="pf-c-menu__item-action pf-m-favorite pf-m-favorited"
                                      type="button"
                                      aria-label="Starred"
                                    >
                                      <span class="pf-c-menu__item-action-icon">
                                        <i
                                          class="fas fa-star"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                    </button>
                                  </li>
                                </ul>
                              </section>
                            </div>
                          </li>
                        </ul>
                      </section>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="pf-c-toolbar__item pf-m-hidden pf-m-visible-on-sm">
              <div
                class="pf-c-dropdown pf-m-full-height"
                style="--pf-c-dropdown--MaxWidth: 20ch;"
              >
                <button
                  class="pf-c-dropdown__toggle"
                  id="gray-tabs-example-masthead-profile-button"
                  aria-expanded="false"
                  type="button"
                >
                  <span class="pf-c-dropdown__toggle-image">
                    <img
                      class="pf-c-avatar"
                      src="/assets/images/img_avatar.svg"
                      alt="Avatar image"
                    />
                  </span>
                  <span class="pf-c-dropdown__toggle-text">Ned Username</span>
                  <span class="pf-c-dropdown__toggle-icon">
                    <i class="fas fa-caret-down" aria-hidden="true"></i>
                  </span>
                </button>
                <div class="pf-c-dropdown__menu" hidden>
                  <section class="pf-c-dropdown__group">
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Account number:</div>
                      <div>123456789</div>
                    </div>
                    <div class="pf-c-dropdown__menu-item pf-m-text">
                      <div class="pf-u-font-size-sm">Username:</div>
                      <div>mshaksho@redhat.com</div>
                    </div>
                  </section>
                  <hr class="pf-c-divider" />
                  <section class="pf-c-dropdown__group">
                    <ul>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">My profile</a>
                      </li>
                      <li>
                        <a
                          class="pf-c-dropdown__menu-item"
                          href="#"
                        >User management</a>
                      </li>
                      <li>
                        <a class="pf-c-dropdown__menu-item" href="#">Logout</a>
                      </li>
                    </ul>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
          <p>This is a full page demo.</p>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-tabs pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div
          class="pf-c-tabs pf-m-box pf-m-page-insets"
          id="gray-tabs-example-tabs-tabs"
        >
          <ul class="pf-c-tabs__list">
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-tabs-new-panel"
                id="gray-tabs-example-tabs-tabs-new-link"
              >
                <span class="pf-c-tabs__item-text">What's new</span>
              </button>
            </li>
            <li class="pf-c-tabs__item pf-m-current">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-tabs-get-started-panel"
                id="gray-tabs-example-tabs-tabs-get-started-link"
              >
                <span class="pf-c-tabs__item-text">Get started</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-tabs-knowledge-panel"
                id="gray-tabs-example-tabs-tabs-knowledge-link"
              >
                <span class="pf-c-tabs__item-text">Knowledge</span>
              </button>
            </li>
            <li class="pf-c-tabs__item">
              <button
                class="pf-c-tabs__link"
                aria-controls="gray-tabs-example-tabs-tabs-support-panel"
                id="gray-tabs-example-tabs-tabs-support-link"
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
          aria-labelledby="gray-tabs-example-tabs-tabs-new-link"
          id="gray-tabs-example-tabs-tabs-new-panel"
          role="tabpanel"
          tabindex="0"
          hidden
        >
          <div class="pf-c-tab-content__body">What's new content</div>
        </section>
        <section
          class="pf-c-tab-content"
          aria-labelledby="gray-tabs-example-tabs-tabs-get-started-link"
          id="gray-tabs-example-tabs-tabs-get-started-panel"
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
                  id="gray-tabs-example-tabs-subtabs"
                >
                  <ul class="pf-c-tabs__list">
                    <li class="pf-c-tabs__item pf-m-current">
                      <button
                        class="pf-c-tabs__link"
                        aria-controls="gray-tabs-example-tabs-subtabs-x86-panel"
                        id="gray-tabs-example-tabs-subtabs-x86-link"
                      >
                        <span class="pf-c-tabs__item-text">x86 architecture</span>
                      </button>
                    </li>
                    <li class="pf-c-tabs__item">
                      <button
                        class="pf-c-tabs__link"
                        aria-controls="gray-tabs-example-tabs-subtabs-additional-architectures-panel"
                        id="gray-tabs-example-tabs-subtabs-additional-architectures-link"
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
                  aria-labelledby="gray-tabs-example-tabs-subtabs-x86-link"
                  id="gray-tabs-example-tabs-subtabs-x86-panel"
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
                  aria-labelledby="gray-tabs-example-tabs-subtabs-additional-architectures-link"
                  id="gray-tabs-example-tabs-subtabs-additional-architectures-panel"
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
          aria-labelledby="gray-tabs-example-tabs-tabs-knowledge-link"
          id="gray-tabs-example-tabs-tabs-knowledge-panel"
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
          aria-labelledby="gray-tabs-example-tabs-tabs-support-link"
          id="gray-tabs-example-tabs-tabs-support-panel"
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
