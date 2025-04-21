---
id: 'Dashboard'
beta: true
section: demos
cssPrefix: pf-d-dashboard
---## Examples

### Basic

```html isFullscreen
<div class="pf-c-page" id="dashboard-demo">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-dashboard-demo"
  >Skip to content</a>
  <header class="pf-c-masthead" id="dashboard-demo-masthead">
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
        id="dashboard-demo-masthead-toolbar"
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
                    id="dashboard-demo-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="dashboard-demo-masthead-icon-group--app-launcher-button"
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
                      id="dashboard-demo-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="dashboard-demo-masthead-settings-button"
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
                      id="dashboard-demo-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="dashboard-demo-masthead-help-button"
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
                  id="dashboard-demo-masthead-profile-button"
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
      <nav class="pf-c-nav" id="dashboard-demo-primary-nav" aria-label="Global">
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
  <main class="pf-c-page__main" tabindex="-1" id="main-content-dashboard-demo">
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

    <section class="pf-c-page__main-section pf-m-limit-width">
      <div class="pf-c-page__main-body">
        <div class="pf-l-grid pf-m-gutter">
          <div class="pf-c-card pf-m-expanded">
            <div class="pf-c-card__header">
              <div class="pf-c-card__header-toggle">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Details"
                  id="dashboard-demo-expandable-status-card-1-toggle"
                  aria-labelledby="dashboard-demo-expandable-status-card-1-title dashboard-demo-expandable-status-card-1-toggle"
                >
                  <span class="pf-c-card__header-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </button>
              </div>
              <div class="pf-c-card__actions">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="dashboard-demo-expandable-status-card-1-dropdown-kebab-right-aligned-button"
                    aria-expanded="false"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu"
                    aria-labelledby="dashboard-demo-expandable-status-card-1-dropdown-kebab-right-aligned-button"
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
              </div>
              <div
                class="pf-c-card__title"
                id="dashboard-demo-expandable-status-card-1-title"
              >
                <h2 class="pf-c-title pf-m-xl">Improve recommended pathways</h2>
              </div>
            </div>
            <div class="pf-c-card__expandable-content">
              <div class="pf-l-flex pf-m-column pf-m-row-on-md">
                <div
                  class="pf-l-flex pf-m-flex-1 pf-m-align-self-stretch pf-m-align-items-stretch"
                >
                  <div class="pf-c-card pf-m-plain">
                    <div class="pf-c-card__body">
                      <div
                        class="pf-l-flex pf-m-column pf-u-h-100 pf-m-space-items-sm"
                      >
                        <div
                          class="pf-l-flex pf-m-space-items-sm pf-m-column-on-md pf-m-row-on-lg pf-m-spacer-md-on-md pf-m-spacer-sm-on-lg"
                        >
                          <div class="pf-c-label-group">
                            <div class="pf-c-label-group__main">
                              <ul
                                class="pf-c-label-group__list"
                                role="list"
                                aria-label="Group of labels"
                              >
                                <li class="pf-c-label-group__list-item">
                                  <span
                                    class="pf-c-label pf-m-blue pf-m-outline"
                                  >
                                    <span class="pf-c-label__content">
                                      <span class="pf-c-label__icon">
                                        <i
                                          class="pf-icon pf-icon-port"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                      Performance
                                    </span>
                                  </span>
                                </li>
                              </ul>
                            </div>
                          </div>
                          <a href="#">378 systems</a>
                        </div>
                        <div class="pf-l-flex__item pf-m-spacer-md">
                          <p>Upgrade your kernel version to remediate ntpd time sync issues, kernel panics, network instabilities and issues with system performance</p>
                        </div>
                        <div
                          class="pf-l-flex pf-m-grow pf-m-column pf-m-row-on-lg pf-m-justify-content-flex-end pf-m-justify-content-flex-start-on-lg pf-m-align-content-flex-end-on-lg"
                          style="row-gap: var(--pf-global--spacer--md);"
                        >
                          <div
                            class="pf-l-flex__item"
                            style="margin-bottom: -.25em"
                          >
                            <span class="pf-c-label pf-m-red">
                              <span class="pf-c-label__content">Incident</span>
                            </span>
                          </div>
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-align-items-center pf-m-nowrap"
                            style="row-gap: var(--pf-global--spacer--md);"
                          >
                            <i
                              class="pf-icon pf-icon-on pf-u-color-400"
                              style="line-height: 1"
                              aria-hidden="true"
                            ></i>
                            <p class="pf-u-color-200">
                              System reboot
                              <b class="pf-u-color-100">is not</b> required
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="pf-c-card__footer">
                      <a class="pf-c-button pf-m-link pf-m-inline" href="#">
                        View pathway
                        <span class="pf-c-button__icon pf-m-end">
                          <i class="fas fa-arrow-right" aria-hidden="true"></i>
                        </span>
                      </a>
                    </div>
                  </div>
                </div>
                <hr class="pf-c-divider pf-m-vertical-on-md pf-m-inset-3xl" />
                <div
                  class="pf-l-flex pf-m-flex-1 pf-m-align-self-stretch pf-m-align-items-stretch"
                >
                  <div class="pf-c-card pf-m-plain">
                    <div class="pf-c-card__body">
                      <div
                        class="pf-l-flex pf-m-column pf-u-h-100 pf-m-space-items-sm"
                      >
                        <div
                          class="pf-l-flex pf-m-space-items-sm pf-m-column-on-md pf-m-row-on-lg pf-m-spacer-md-on-md pf-m-spacer-sm-on-lg"
                        >
                          <div class="pf-c-label-group">
                            <div class="pf-c-label-group__main">
                              <ul
                                class="pf-c-label-group__list"
                                role="list"
                                aria-label="Group of labels"
                              >
                                <li class="pf-c-label-group__list-item">
                                  <span
                                    class="pf-c-label pf-m-blue pf-m-outline"
                                  >
                                    <span class="pf-c-label__content">
                                      <span class="pf-c-label__icon">
                                        <i
                                          class="fas fa-cube"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                      Stablility
                                    </span>
                                  </span>
                                </li>
                                <li class="pf-c-label-group__list-item">
                                  <button
                                    class="pf-c-label pf-m-overflow"
                                    type="button"
                                  >
                                    <span class="pf-c-label__content">1 more</span>
                                  </button>
                                </li>
                              </ul>
                            </div>
                          </div>
                          <a href="#">211 systems</a>
                        </div>
                        <div class="pf-l-flex__item pf-m-spacer-md">
                          <p>Adjust your networking configuration to get ahead of network perfomance degradations and packet losses</p>
                        </div>
                        <div
                          class="pf-l-flex pf-m-grow pf-m-column pf-m-row-on-lg pf-m-justify-content-flex-end pf-m-justify-content-flex-start-on-lg pf-m-align-content-flex-end-on-lg"
                          style="row-gap: var(--pf-global--spacer--md);"
                        >
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-align-items-center pf-m-nowrap"
                            style="row-gap: var(--pf-global--spacer--md);"
                          >
                            <i
                              class="pf-icon pf-icon-on pf-u-danger-color-100"
                              style="line-height: 1"
                              aria-hidden="true"
                            ></i>
                            <p class="pf-u-color-200">
                              System reboot
                              <b class="pf-u-color-100">is</b> required
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="pf-c-card__footer">
                      <a class="pf-c-button pf-m-link pf-m-inline" href="#">
                        View pathway
                        <span class="pf-c-button__icon pf-m-end">
                          <i class="fas fa-arrow-right" aria-hidden="true"></i>
                        </span>
                      </a>
                    </div>
                  </div>
                </div>
                <hr class="pf-c-divider pf-m-vertical-on-md pf-m-inset-3xl" />
                <div
                  class="pf-l-flex pf-m-flex-1 pf-m-align-self-stretch pf-m-align-items-stretch"
                >
                  <div class="pf-c-card pf-m-plain">
                    <div class="pf-c-card__body">
                      <div
                        class="pf-l-flex pf-m-column pf-u-h-100 pf-m-space-items-sm"
                      >
                        <div
                          class="pf-l-flex pf-m-space-items-sm pf-m-column-on-md pf-m-row-on-lg pf-m-spacer-md-on-md pf-m-spacer-sm-on-lg"
                        >
                          <div class="pf-c-label-group">
                            <div class="pf-c-label-group__main">
                              <ul
                                class="pf-c-label-group__list"
                                role="list"
                                aria-label="Group of labels"
                              >
                                <li class="pf-c-label-group__list-item">
                                  <span
                                    class="pf-c-label pf-m-blue pf-m-outline"
                                  >
                                    <span class="pf-c-label__content">
                                      <span class="pf-c-label__icon">
                                        <i
                                          class="pf-icon pf-icon-automation"
                                          aria-hidden="true"
                                        ></i>
                                      </span>
                                      Availability
                                    </span>
                                  </span>
                                </li>
                              </ul>
                            </div>
                          </div>
                          <a href="#">166 systems</a>
                        </div>
                        <div class="pf-l-flex__item pf-m-spacer-md">
                          <p>Fine tune your Oracle DB configuration to improve database performance and avoid process failure</p>
                        </div>
                        <div
                          class="pf-l-flex pf-m-grow pf-m-column pf-m-row-on-lg pf-m-justify-content-flex-end pf-m-justify-content-flex-start-on-lg pf-m-align-content-flex-end-on-lg"
                          style="row-gap: var(--pf-global--spacer--md);"
                        >
                          <div
                            class="pf-l-flex__item"
                            style="margin-bottom: -.25em"
                          >
                            <span class="pf-c-label pf-m-red">
                              <span class="pf-c-label__content">Incident</span>
                            </span>
                          </div>
                          <div
                            class="pf-l-flex pf-m-space-items-sm pf-m-align-items-center pf-m-nowrap"
                            style="row-gap: var(--pf-global--spacer--md);"
                          >
                            <i
                              class="pf-icon pf-icon-on pf-u-color-400"
                              style="line-height: 1"
                              aria-hidden="true"
                            ></i>
                            <p class="pf-u-color-200">
                              System reboot
                              <b class="pf-u-color-100">is not</b> required
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="pf-c-card__footer">
                      <a class="pf-c-button pf-m-link pf-m-inline" href="#">
                        View pathway
                        <span class="pf-c-button__icon pf-m-end">
                          <i class="fas fa-arrow-right" aria-hidden="true"></i>
                        </span>
                      </a>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-l-grid__item pf-m-gutter pf-m-4-col-on-lg pf-m-6-col-on-2xl"
            style="--pf-l-grid--item--Order-on-lg:3"
          >
            <div class="pf-l-flex pf-m-column">
              <div
                class="pf-c-card pf-m-expanded"
                id="dashboard-demo-status-card-1"
              >
                <div class="pf-c-card__header">
                  <h2 class="pf-c-title pf-m-xl">Status</h2>
                </div>
                <div class="pf-c-card__body">
                  <div
                    class="pf-l-gallery pf-m-gutter"
                    style="--pf-l-gallery--GridTemplateColumns--min: 100%; --pf-l-gallery--GridTemplateColumns--min-on-sm: 180px; --pf-l-gallery--GridTemplateColumns--min-on-lg: 150px; --pf-l-gallery--GridTemplateColumns--max-on-sm: 1fr;"
                  >
                    <div class="pf-l-flex pf-m-space-items-sm pf-m-nowrap">
                      <div class="pf-l-flex__item">
                        <i
                          class="fas fa-check-circle pf-u-success-color-100"
                          aria-hidden="true"
                        ></i>
                      </div>
                      <div class="pf-l-flex__item">
                        <span>Cluster</span>
                      </div>
                    </div>
                    <div class="pf-l-flex pf-m-space-items-sm pf-m-nowrap">
                      <div class="pf-l-flex__item">
                        <i
                          class="fas fa-exclamation-circle pf-u-danger-color-100"
                          aria-hidden="true"
                        ></i>
                      </div>
                      <div class="pf-l-flex__item pf-u-text-nowrap">
                        <span class="popover-parent">
                          <a href="#">Control Panel</a>
                        </span>
                      </div>
                    </div>
                    <div class="pf-l-flex pf-m-space-items-sm pf-m-nowrap">
                      <div class="pf-l-flex__item pf-u-text-nowrap">
                        <i
                          class="fas fa-exclamation-circle pf-u-danger-color-100"
                          aria-hidden="true"
                        ></i>
                      </div>
                      <div class="pf-l-flex pf-m-column pf-m-space-items-none">
                        <div class="pf-l-flex__item">
                          <a href="#">Operators</a>
                        </div>
                        <div class="pf-l-flex__item">
                          <span class="pf-u-color-200">1 degraged</span>
                        </div>
                      </div>
                    </div>
                    <div class="pf-l-flex pf-m-space-items-sm pf-m-nowrap">
                      <div class="pf-l-flex__item">
                        <i
                          class="fas fa-check-circle pf-u-success-color-100"
                          aria-hidden="true"
                        ></i>
                      </div>
                      <div class="pf-l-flex pf-m-column pf-m-space-items-none">
                        <div class="pf-l-flex__item">
                          <a href="#">Image Vulnerabilities</a>
                        </div>
                        <div class="pf-l-flex__item">
                          <span class="pf-u-color-200">0 vulnerabilities</span>
                        </div>
                      </div>
                    </div>
                    <div class="pf-l-flex pf-m-space-items-sm pf-m-nowrap">
                      <div class="pf-l-flex__item">
                        <i
                          class="fas fa-check-circle pf-u-success-color-100"
                          aria-hidden="true"
                        ></i>
                      </div>
                      <div class="pf-l-flex pf-m-column pf-m-space-items-none">
                        <div class="pf-l-flex__item">
                          <a href="#">Storage</a>
                        </div>
                        <div class="pf-l-flex__item">
                          <span class="pf-u-color-200">Degraded</span>
                        </div>
                      </div>
                    </div>
                    <div class="pf-l-flex pf-m-space-items-sm pf-m-nowrap">
                      <div class="pf-l-flex__item">
                        <i
                          class="fas fa-check-circle pf-u-success-color-100"
                          aria-hidden="true"
                        ></i>
                      </div>
                      <div class="pf-l-flex pf-m-column pf-m-space-items-none">
                        <div class="pf-l-flex__item">
                          <a href="#">Hardware</a>
                        </div>
                      </div>
                    </div>
                    <div class="pf-l-flex pf-m-space-items-sm pf-m-nowrap">
                      <div class="pf-l-flex__item">
                        <i
                          class="fas fa-check-circle pf-u-success-color-100"
                          aria-hidden="true"
                        ></i>
                      </div>
                      <div class="pf-l-flex pf-m-column pf-m-space-items-none">
                        <div class="pf-l-flex__item">
                          <a href="#">Insights</a>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <hr class="pf-c-divider" />
                <div class="pf-c-notification-drawer">
                  <div class="pf-c-notification-drawer__body">
                    <section class="pf-c-notification-drawer__group">
                      <button
                        class="pf-c-notification-drawer__group-toggle"
                        aria-expanded="false"
                      >
                        <div
                          class="pf-c-notification-drawer__group-toggle-title"
                        >
                          <div class="pf-l-flex">
                            <div
                              class="pf-l-flex__item pf-m-spacer-md"
                            >Notifications</div>
                            <div class="pf-c-label-group">
                              <div class="pf-c-label-group__main">
                                <ul
                                  class="pf-c-label-group__list"
                                  role="list"
                                  aria-label="Group of labels"
                                >
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-red">
                                      <span class="pf-c-label__content">
                                        <span class="pf-c-label__icon">
                                          <i
                                            class="fas fa-fw fa-exclamation-circle"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        1
                                      </span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span
                                      class="pf-c-label pf-m-orange pf-m-default"
                                    >
                                      <span class="pf-c-label__content">
                                        <span class="pf-c-label__icon">
                                          <i
                                            class="fas fa-fw fa-exclamation-triangle"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        3
                                      </span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-green">
                                      <span class="pf-c-label__content">
                                        <span class="pf-c-label__icon">
                                          <i
                                            class="fas fa-fw fa-check-circle"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        3
                                      </span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-blue">
                                      <span class="pf-c-label__content">
                                        <span class="pf-c-label__icon">
                                          <i
                                            class="fas fa-fw fa-info-circle"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        3
                                      </span>
                                    </span>
                                  </li>
                                  <li class="pf-c-label-group__list-item">
                                    <span class="pf-c-label pf-m-cyan">
                                      <span class="pf-c-label__content">
                                        <span class="pf-c-label__icon">
                                          <i
                                            class="fas fa-fw fa-bell"
                                            aria-hidden="true"
                                          ></i>
                                        </span>
                                        3
                                      </span>
                                    </span>
                                  </li>
                                </ul>
                              </div>
                            </div>
                          </div>
                        </div>
                        <span
                          class="pf-c-notification-drawer__group-toggle-icon"
                        >
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </span>
                      </button>
                      <ul class="pf-c-notification-drawer__list" hidden>
                        <li
                          class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-danger"
                          tabindex="0"
                        >
                          <div
                            class="pf-c-notification-drawer__list-item-header"
                          >
                            <span
                              class="pf-c-notification-drawer__list-item-header-icon"
                            >
                              <i
                                class="fas fa-exclamation-circle"
                                aria-hidden="true"
                              ></i>
                            </span>
                            <h2
                              class="pf-c-notification-drawer__list-item-header-title pf-u-danger-color-200"
                            >
                              <span
                                class="pf-screen-reader"
                              >Danger notification:</span>
                              Critical alert regarding control plane
                            </h2>
                          </div>
                          <div
                            class="pf-c-notification-drawer__list-item-description"
                          >This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        </li>
                        <li
                          class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-warning"
                          tabindex="0"
                        >
                          <div
                            class="pf-c-notification-drawer__list-item-header"
                          >
                            <span
                              class="pf-c-notification-drawer__list-item-header-icon"
                            >
                              <i
                                class="fas fa-exclamation-triangle"
                                aria-hidden="true"
                              ></i>
                            </span>
                            <h2
                              class="pf-c-notification-drawer__list-item-header-title pf-u-warning-color-200"
                            >
                              <span
                                class="pf-screen-reader"
                              >Warning notification:</span>
                              Warning alert
                            </h2>
                          </div>
                          <div
                            class="pf-c-notification-drawer__list-item-description"
                          >This is a warning notification description.</div>
                        </li>
                      </ul>
                    </section>
                  </div>
                </div>
              </div>
              <!-- inventory -->
              <div class="pf-c-card" id="dashboard-demo-line-chart-card-1">
                <div class="pf-c-card__header">
                  <div class="pf-c-card__actions pf-m-no-offset">
                    <div class="pf-c-select">
                      <span
                        id="dashboard-demo-line-chart-card-1-select-dropdown-label"
                        hidden
                      >Choose one</span>

                      <button
                        class="pf-c-select__toggle pf-m-plain"
                        type="button"
                        id="dashboard-demo-line-chart-card-1-select-dropdown-toggle"
                        aria-haspopup="true"
                        aria-expanded="false"
                        aria-labelledby="dashboard-demo-line-chart-card-1-select-dropdown-label dashboard-demo-line-chart-card-1-select-dropdown-toggle"
                      >
                        <div class="pf-c-select__toggle-wrapper">
                          <span class="pf-c-select__toggle-text">24 hours</span>
                        </div>
                        <span class="pf-c-select__toggle-arrow">
                          <i class="fas fa-caret-down" aria-hidden="true"></i>
                        </span>
                      </button>

                      <ul
                        class="pf-c-select__menu pf-m-align-right"
                        role="listbox"
                        aria-labelledby="dashboard-demo-line-chart-card-1-select-dropdown-label"
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
                    class="pf-c-card__title"
                    id="dashboard-demo-line-chart-card-1-title"
                  >
                    <h2 class="pf-c-title pf-m-xl">Cluster utilizations</h2>
                  </div>
                </div>
                <div
                  class="pf-c-card pf-m-plain pf-m-expanded"
                  id="dashboard-demo-line-chart-card-1-group-1"
                >
                  <div class="pf-c-card__header pf-m-toggle-right">
                    <div class="pf-c-card__header-toggle">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Details"
                        id="dashboard-demo-line-chart-card-1-group-1-toggle"
                        aria-labelledby="dashboard-demo-line-chart-card-1-group-1-title dashboard-demo-line-chart-card-1-group-1-toggle"
                      >
                        <span class="pf-c-card__header-toggle-icon">
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </span>
                      </button>
                    </div>
                    <div
                      class="pf-c-card__title"
                      id="dashboard-demo-line-chart-card-1-group-1-title"
                    >CPU 1</div>
                  </div>
                  <div class="pf-c-card__expandable-content">
                    <div class="pf-c-card__body">
                      <div class="pf-l-grid pf-m-gutter">
                        <div class="pf-l-grid pf-m-gutter">
                          <div class="pf-l-grid__item pf-m-4-col-on-md">
                            <div
                              class="pf-l-flex pf-m-column-on-md pf-m-space-items-none-on-md pf-m-justify-content-center-on-md pf-u-h-100-on-md"
                            >
                              <div class="pf-l-flex__item">
                                <b>Temperature</b>
                              </div>
                              <hr
                                class="pf-c-divider pf-m-vertical pf-m-inset-sm pf-u-hidden-on-md"
                              />
                              <div class="pf-l-flex__item">
                                <span>64C</span>
                              </div>
                            </div>
                          </div>
                          <div class="pf-l-grid__item pf-m-8-col-on-md">
                            <div class="pf-l-grid pf-m-gutter">
                              <div class="pf-l-grid__item pf-m-2-col">
                                <div
                                  class="pf-l-flex pf-m-column pf-m-space-items-none pf-m-align-items-flex-end-on-md"
                                >
                                  <div class="pf-l-flex__item">100C</div>
                                  <div class="pf-l-flex__item">50C</div>
                                  <div class="pf-l-flex__item">0C</div>
                                </div>
                              </div>
                              <div class="pf-l-grid__item pf-m-10-col">
                                <div class="ws-chart">
                                  <img
                                    src="/assets/images/img_line-chart-2.png"
                                    alt="Line chart"
                                  />
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                        <hr class="pf-c-divider pf-u-hidden-on-md" />
                        <div class="pf-l-grid pf-m-gutter">
                          <div class="pf-l-grid__item pf-m-4-col-on-md">
                            <div
                              class="pf-l-flex pf-m-column-on-md pf-m-space-items-none-on-md pf-m-justify-content-center-on-md pf-u-h-100-on-md"
                            >
                              <div class="pf-l-flex__item">
                                <b>Speed</b>
                              </div>
                              <hr
                                class="pf-c-divider pf-m-vertical pf-m-inset-sm pf-u-hidden-on-md"
                              />
                              <div class="pf-l-flex__item">
                                <span>2.3Ghz</span>
                              </div>
                            </div>
                          </div>
                          <div class="pf-l-grid__item pf-m-8-col-on-md">
                            <div class="pf-l-grid pf-m-gutter">
                              <div class="pf-l-grid__item pf-m-2-col">
                                <div
                                  class="pf-l-flex pf-m-column pf-m-space-items-none pf-m-align-items-flex-end-on-md"
                                >
                                  <div class="pf-l-flex__item">36hz</div>
                                  <div class="pf-l-flex__item">1.5Ghz</div>
                                  <div class="pf-l-flex__item">0Ghz</div>
                                </div>
                              </div>
                              <div class="pf-l-grid__item pf-m-10-col">
                                <div class="ws-chart">
                                  <img
                                    src="/assets/images/img_line-chart-2.png"
                                    alt="Line chart"
                                  />
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div
                  class="pf-c-card pf-m-plain"
                  id="dashboard-demo-line-chart-card-1-group-2"
                >
                  <div class="pf-c-card__header pf-m-toggle-right">
                    <div class="pf-c-card__header-toggle">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Details"
                        id="dashboard-demo-line-chart-card-1-group-2-toggle"
                        aria-labelledby="dashboard-demo-line-chart-card-1-group-2-title dashboard-demo-line-chart-card-1-group-2-toggle"
                      >
                        <span class="pf-c-card__header-toggle-icon">
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </span>
                      </button>
                    </div>
                    <div
                      class="pf-c-card__title"
                      id="dashboard-demo-line-chart-card-1-group-2-title"
                    >Pod count</div>
                  </div>
                </div>
                <div
                  class="pf-c-card pf-m-plain"
                  id="dashboard-demo-line-chart-card-1-group-3"
                >
                  <div class="pf-c-card__header pf-m-toggle-right">
                    <div class="pf-c-card__header-toggle">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Details"
                        id="dashboard-demo-line-chart-card-1-group-3-toggle"
                        aria-labelledby="dashboard-demo-line-chart-card-1-group-3-title dashboard-demo-line-chart-card-1-group-3-toggle"
                      >
                        <span class="pf-c-card__header-toggle-icon">
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </span>
                      </button>
                    </div>
                    <div
                      class="pf-c-card__title"
                      id="dashboard-demo-line-chart-card-1-group-3-title"
                    >Memory</div>
                  </div>
                </div>
              </div>
              <div class="pf-c-card">
                <div class="pf-c-card__title">
                  <h2 class="pf-c-title pf-m-xl">Recomendations by severity</h2>
                </div>
                <div class="pf-c-card__body">
                  <div class="pf-l-flex pf-m-inline-flex">
                    <div class="pf-l-grid pf-m-gutter pf-m-all-3-col">
                      <div
                        class="pf-l-flex pf-m-column pf-m-space-items-xs pf-m-align-items-center"
                      >
                        <span
                          class="pf-u-font-size-2xl pf-u-primary-color-100"
                        >2</span>
                        <span class="pf-u-font-color-200">Critical</span>
                      </div>
                      <div
                        class="pf-l-flex pf-m-column pf-m-space-items-xs pf-m-align-items-center"
                      >
                        <span
                          class="pf-u-font-size-2xl pf-u-primary-color-100"
                        >5</span>
                        <span class="pf-u-font-color-200">Important</span>
                      </div>
                      <div
                        class="pf-l-flex pf-m-column pf-m-space-items-xs pf-m-align-items-center"
                      >
                        <span
                          class="pf-u-font-size-2xl pf-u-primary-color-100"
                        >7</span>
                        <span class="pf-u-font-color-200">Moderate</span>
                      </div>
                      <div
                        class="pf-l-flex pf-m-column pf-m-space-items-xs pf-m-align-items-center"
                      >
                        <span
                          class="pf-u-font-size-2xl pf-u-primary-color-100"
                        >12</span>
                        <span class="pf-u-font-color-200">Low</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="pf-c-card__title">
                  <h2 class="pf-c-title pf-m-xl">Recomendations by category</h2>
                </div>
                <div class="pf-c-card__body">
                  <img
                    src="/assets/images/img_pie-chart-with-legend.png"
                    alt="Pie chart"
                    width="450"
                  />
                </div>
                <div class="pf-c-card__footer">
                  <a href="#">View more</a>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-l-grid__item pf-m-gutter pf-m-4-col-on-lg pf-m-3-col-on-2xl"
            style="--pf-l-grid--item--Order-on-lg:2"
          >
            <div class="pf-l-flex pf-m-column pf-m-row-on-md pf-m-column-on-lg">
              <div class="pf-l-flex__item pf-m-flex-1">
                <div class="pf-c-card" id="dashboard-demo-details-card-1">
                  <div class="pf-c-card__title">
                    <h2 class="pf-c-title pf-m-xl">Details</h2>
                  </div>
                  <div class="pf-c-card__body">
                    <dl class="pf-c-description-list">
                      <div class="pf-c-description-list__group">
                        <dt
                          class="pf-c-description-list__term"
                        >Cluster API Address</dt>
                        <dd class="pf-c-description-list__description">
                          <div class="pf-c-description-list__text">
                            <a href="#">https://api1.devcluster.openshift.com</a>
                          </div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt class="pf-c-description-list__term">Cluster ID</dt>
                        <dd class="pf-c-description-list__description">
                          <div
                            class="pf-c-description-list__text"
                          >63b97ac1-b850-41d9-8820-239becde9e86</div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt class="pf-c-description-list__term">Provider</dt>
                        <dd class="pf-c-description-list__description">
                          <div class="pf-c-description-list__text">AWS</div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt
                          class="pf-c-description-list__term"
                        >OpenShift Version</dt>
                        <dd class="pf-c-description-list__description">
                          <div
                            class="pf-c-description-list__text"
                          >4.5.0.ci-2020-06-16-015028</div>
                        </dd>
                      </div>
                      <div class="pf-c-description-list__group">
                        <dt class="pf-c-description-list__term">Update Channel</dt>
                        <dd class="pf-c-description-list__description">
                          <div class="pf-c-description-list__text">stable-4.5</div>
                        </dd>
                      </div>
                    </dl>
                  </div>
                  <hr class="pf-c-divider" />
                  <div class="pf-c-card__footer">
                    <a href="#">View Settings</a>
                  </div>
                </div>
              </div>
              <div class="pf-l-flex__item pf-m-flex-1">
                <div class="pf-c-card" id="dashboard-demo-data-list-card-1">
                  <div class="pf-c-card__header pf-u-align-items-flex-start">
                    <div
                      class="pf-c-card__title"
                      id="dashboard-demo-data-list-card-1-title1"
                    >
                      <h2 class="pf-c-title pf-m-lg">Inventory</h2>
                    </div>
                  </div>
                  <ul
                    class="pf-c-data-list pf-m-grid-none"
                    role="list"
                    aria-label="Simple data list example"
                    id="dashboard-demo-data-list-card-1-data-list"
                  >
                    <li
                      class="pf-c-data-list__item"
                      aria-labelledby="dashboard-demo-data-list-card-1-data-list-item-1"
                    >
                      <div class="pf-c-data-list__item-row">
                        <div class="pf-c-data-list__item-content">
                          <div
                            class="pf-c-data-list__cell"
                            id="dashboard-demo-data-list-card-1-data-list-item-1"
                          >3 Nodes</div>
                          <div
                            class="pf-c-data-list__cell pf-m-no-fill pf-m-align-right"
                          >
                            <a href="#">
                              <div class="pf-l-flex pf-m-space-items-sm">
                                <span>3</span>
                                <i
                                  class="fas fa-check-circle pf-u-success-color-100"
                                  aria-hidden="true"
                                ></i>
                              </div>
                            </a>
                          </div>
                        </div>
                      </div>
                    </li>
                    <li
                      class="pf-c-data-list__item"
                      aria-labelledby="dashboard-demo-data-list-card-1-data-list-item-2"
                    >
                      <div class="pf-c-data-list__item-row">
                        <div class="pf-c-data-list__item-content">
                          <div
                            class="pf-c-data-list__cell"
                            id="dashboard-demo-data-list-card-1-data-list-item-2"
                          >8 Disks</div>
                          <div
                            class="pf-c-data-list__cell pf-m-no-fill pf-m-align-right"
                          >
                            <a href="#">
                              <div class="pf-l-flex pf-m-space-items-sm">
                                <span>8</span>
                                <i
                                  class="fas fa-check-circle pf-u-success-color-100"
                                  aria-hidden="true"
                                ></i>
                              </div>
                            </a>
                          </div>
                        </div>
                      </div>
                    </li>
                    <li
                      class="pf-c-data-list__item"
                      aria-labelledby="dashboard-demo-data-list-card-1-data-list-item-3"
                    >
                      <div class="pf-c-data-list__item-row">
                        <div class="pf-c-data-list__item-content">
                          <div
                            class="pf-c-data-list__cell"
                            id="dashboard-demo-data-list-card-1-data-list-item-3"
                          >20 Pods</div>
                          <div
                            class="pf-c-data-list__cell pf-m-no-fill pf-m-align-right"
                          >
                            <a href="#">
                              <div class="pf-l-flex pf-m-space-items-sm">
                                <span>20</span>
                                <i
                                  class="fas fa-check-circle pf-u-success-color-100"
                                  aria-hidden="true"
                                ></i>
                              </div>
                            </a>
                          </div>
                        </div>
                      </div>
                    </li>
                    <li
                      class="pf-c-data-list__item"
                      aria-labelledby="dashboard-demo-data-list-card-1-data-list-item-4"
                    >
                      <div class="pf-c-data-list__item-row">
                        <div class="pf-c-data-list__item-content">
                          <div
                            class="pf-c-data-list__cell"
                            id="dashboard-demo-data-list-card-1-data-list-item-4"
                          >12 PVs</div>
                          <div
                            class="pf-c-data-list__cell pf-m-no-fill pf-m-align-right"
                          >
                            <a href="#">
                              <div class="pf-l-flex pf-m-space-items-sm">
                                <span>12</span>
                                <i
                                  class="fas fa-check-circle pf-u-success-color-100"
                                  aria-hidden="true"
                                ></i>
                              </div>
                            </a>
                          </div>
                        </div>
                      </div>
                    </li>
                    <li
                      class="pf-c-data-list__item"
                      aria-labelledby="dashboard-demo-data-list-card-1-data-list-item-5"
                    >
                      <div class="pf-c-data-list__item-row">
                        <div class="pf-c-data-list__item-content">
                          <div
                            class="pf-c-data-list__cell"
                            id="dashboard-demo-data-list-card-1-data-list-item-5"
                          >18 PVCs</div>
                          <div
                            class="pf-c-data-list__cell pf-m-no-fill pf-m-align-right"
                          >
                            <a href="#">
                              <div class="pf-l-flex pf-m-space-items-sm">
                                <span>18</span>
                                <i
                                  class="fas fa-check-circle pf-u-success-color-100"
                                  aria-hidden="true"
                                ></i>
                              </div>
                            </a>
                          </div>
                        </div>
                      </div>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-l-grid__item pf-m-4-col-on-lg pf-m-3-col-on-2xl"
            style="--pf-l-grid--item--Order-on-lg:4"
          >
            <div class="pf-l-flex pf-m-column">
              <div class="pf-c-card" id="dashboard-demo-events-card-1">
                <div class="pf-c-card__header">
                  <div class="pf-c-card__actions pf-m-no-offset">
                    <div class="pf-c-select">
                      <span
                        id="dashboard-demo-events-card-1-select-dropdown-label"
                        hidden
                      >Choose one</span>

                      <button
                        class="pf-c-select__toggle pf-m-plain"
                        type="button"
                        id="dashboard-demo-events-card-1-select-dropdown-toggle"
                        aria-haspopup="true"
                        aria-expanded="false"
                        aria-labelledby="dashboard-demo-events-card-1-select-dropdown-label dashboard-demo-events-card-1-select-dropdown-toggle"
                      >
                        <div class="pf-c-select__toggle-wrapper">
                          <span class="pf-c-select__toggle-text">Status</span>
                        </div>
                        <span class="pf-c-select__toggle-arrow">
                          <i class="fas fa-caret-down" aria-hidden="true"></i>
                        </span>
                      </button>

                      <ul
                        class="pf-c-select__menu pf-m-align-right"
                        role="listbox"
                        aria-labelledby="dashboard-demo-events-card-1-select-dropdown-label"
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
                    class="pf-c-card__title"
                    id="dashboard-demo-events-card-1-title1"
                    style="padding-top: 3px;"
                  >
                    <h2 class="pf-c-title pf-m-xl">Events</h2>
                  </div>
                </div>
                <div class="pf-c-card__body">
                  <dl class="pf-c-description-list pf-m-compact">
                    <div class="pf-c-description-list__group">
                      <dt class="pf-c-description-list__term">
                        <div class="pf-l-flex pf-m-nowrap">
                          <div class="pf-l-flex__item pf-m-spacer-sm">
                            <i
                              class="fas fa-exclamation-circle pf-u-danger-color-100"
                              aria-hidden="true"
                            ></i>
                          </div>
                          <div class="pf-l-flex__item">Readiness probe failed</div>
                        </div>
                      </dt>
                      <dd class="pf-c-description-list__description">
                        <div
                          class="pf-c-description-list__text"
                        >Readiness probe failed: Get https://10.131.0.7:5000/healthz: dial tcp 10.131.0.7:5000: connect: connection refused</div>
                      </dd>
                      <dd class="pf-c-description-list__description">
                        <div class="pf-c-description-list__text">
                          <time
                            class="pf-u-color-200 pf-u-font-size-sm"
                          >Jun 17, 11:02 am</time>
                        </div>
                      </dd>
                    </div>
                    <div class="pf-c-description-list__group">
                      <dt class="pf-c-description-list__term">
                        <div class="pf-l-flex pf-m-nowrap">
                          <div class="pf-l-flex__item pf-m-spacer-sm">
                            <i
                              class="fas fa-check-circle pf-u-success-color-100"
                              aria-hidden="true"
                            ></i>
                          </div>
                          <div class="pf-l-flex__item">Successful assignment</div>
                        </div>
                      </dt>
                      <dd class="pf-c-description-list__description">
                        <div
                          class="pf-c-description-list__text"
                        >Successfully assigned default/example to ip-10-0-130-149.ec2.internal</div>
                      </dd>
                      <dd class="pf-c-description-list__description">
                        <div class="pf-c-description-list__text">
                          <time
                            class="pf-u-color-200 pf-u-font-size-sm"
                          >Jun 17, 11:13 am</time>
                        </div>
                      </dd>
                    </div>
                    <div class="pf-c-description-list__group">
                      <dt class="pf-c-description-list__term">
                        <div class="pf-l-flex pf-m-nowrap">
                          <div class="pf-l-flex__item pf-m-spacer-sm">
                            <span
                              class="pf-c-spinner pf-m-md"
                              role="progressbar"
                              aria-label="Loading"
                            >
                              <span class="pf-c-spinner__clipper"></span>
                              <span class="pf-c-spinner__lead-ball"></span>
                              <span class="pf-c-spinner__tail-ball"></span>
                            </span>
                          </div>
                          <div class="pf-l-flex__item">Pulling image</div>
                        </div>
                      </dt>
                      <dd class="pf-c-description-list__description">
                        <div
                          class="pf-c-description-list__text"
                        >Pulling image "openshift/hello-openshift"</div>
                      </dd>
                      <dd class="pf-c-description-list__description">
                        <div class="pf-c-description-list__text">
                          <time
                            class="pf-u-color-200 pf-u-font-size-sm"
                          >Jun 17, 10:59 am</time>
                        </div>
                      </dd>
                    </div>
                    <div class="pf-c-description-list__group">
                      <dt class="pf-c-description-list__term">
                        <div class="pf-l-flex pf-m-nowrap">
                          <div class="pf-l-flex__item pf-m-spacer-sm">
                            <i
                              class="fas fa-check-circle pf-u-success-color-100"
                              aria-hidden="true"
                            ></i>
                          </div>
                          <div class="pf-l-flex__item">Created container</div>
                        </div>
                      </dt>
                      <dd class="pf-c-description-list__description">
                        <div
                          class="pf-c-description-list__text"
                        >Created container hello-openshift</div>
                      </dd>
                      <dd class="pf-c-description-list__description">
                        <div class="pf-c-description-list__text">
                          <time
                            class="pf-u-color-200 pf-u-font-size-sm"
                          >Jun 17, 10:45 am</time>
                        </div>
                      </dd>
                    </div>

                    <div class="pf-c-description-list__group">
                      <dt class="pf-c-description-list__term">
                        <div class="pf-l-flex pf-m-nowrap">
                          <div class="pf-l-flex__item pf-m-spacer-sm">
                            <i
                              class="fas fa-exclamation-triangle pf-u-warning-color-100"
                              aria-hidden="true"
                            ></i>
                          </div>
                          <div
                            class="pf-l-flex__item"
                          >CPU utilitization over 50%</div>
                        </div>
                      </dt>
                      <dd class="pf-c-description-list__description">
                        <div
                          class="pf-c-description-list__text"
                        >Migrated 2 pods to other hosts</div>
                      </dd>
                      <dd class="pf-c-description-list__description">
                        <div class="pf-c-description-list__text">
                          <time
                            class="pf-u-color-200 pf-u-font-size-sm"
                          >Jun 17, 10:33 am</time>
                        </div>
                      </dd>
                    </div>

                    <div class="pf-c-description-list__group">
                      <dt class="pf-c-description-list__term">
                        <div class="pf-l-flex pf-m-nowrap">
                          <div class="pf-l-flex__item pf-m-spacer-sm">
                            <i
                              class="fas fa-exclamation-circle pf-u-danger-color-100"
                              aria-hidden="true"
                            ></i>
                          </div>
                          <div class="pf-l-flex__item">Rook-osd-10-328949</div>
                        </div>
                      </dt>
                      <dd class="pf-c-description-list__description">
                        <div
                          class="pf-c-description-list__text"
                        >Rebuild initiated as Disk 5 failed</div>
                      </dd>
                      <dd class="pf-c-description-list__description">
                        <div class="pf-c-description-list__text">
                          <time
                            class="pf-u-color-200 pf-u-font-size-sm"
                          >Jun 17, 10:33 am</time>
                        </div>
                      </dd>
                    </div>

                    <div class="pf-c-description-list__group">
                      <dt class="pf-c-description-list__term">
                        <div class="pf-l-flex pf-m-nowrap">
                          <div class="pf-l-flex__item pf-m-spacer-sm">
                            <i
                              class="fas fa-check-circle pf-u-success-color-100"
                              aria-hidden="true"
                            ></i>
                          </div>
                          <div class="pf-l-flex__item">Created container</div>
                        </div>
                      </dt>
                      <dd class="pf-c-description-list__description">
                        <div
                          class="pf-c-description-list__text"
                        >Created container hello-openshift-123</div>
                      </dd>
                      <dd class="pf-c-description-list__description">
                        <div class="pf-c-description-list__text">
                          <time
                            class="pf-u-color-200 pf-u-font-size-sm"
                          >Jun 17, 10:31 am</time>
                        </div>
                      </dd>
                    </div>

                    <div class="pf-c-description-list__group">
                      <dt class="pf-c-description-list__term">
                        <div class="pf-l-flex pf-m-nowrap">
                          <div class="pf-l-flex__item pf-m-spacer-sm">
                            <i
                              class="fas fa-check-circle pf-u-success-color-100"
                              aria-hidden="true"
                            ></i>
                          </div>
                          <div class="pf-l-flex__item">Created container</div>
                        </div>
                      </dt>
                      <dd class="pf-c-description-list__description">
                        <div
                          class="pf-c-description-list__text"
                        >Created container hello-openshift-456</div>
                      </dd>
                      <dd class="pf-c-description-list__description">
                        <div class="pf-c-description-list__text">
                          <time
                            class="pf-u-color-200 pf-u-font-size-sm"
                          >Jun 17, 10:30 am</time>
                        </div>
                      </dd>
                    </div>
                  </dl>
                </div>
                <hr class="pf-c-divider" />
                <div class="pf-c-card__footer">
                  <a href="#">View all events</a>
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
