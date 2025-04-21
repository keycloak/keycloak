---
id: Notification drawer
section: components
---## Demos

### Collapsed

```html isFullscreen
<div class="pf-c-page" id="drawer-collapsed-example-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-drawer-collapsed-example-page"
  >Skip to content</a>
  <header class="pf-c-masthead" id="drawer-collapsed-example-page-masthead">
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
        id="drawer-collapsed-example-page-masthead-toolbar"
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
                    id="drawer-collapsed-example-page-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="drawer-collapsed-example-page-masthead-icon-group--app-launcher-button"
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
                      id="drawer-collapsed-example-page-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-collapsed-example-page-masthead-settings-button"
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
                      id="drawer-collapsed-example-page-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-collapsed-example-page-masthead-help-button"
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
                  id="drawer-collapsed-example-page-masthead-profile-button"
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
        id="drawer-collapsed-example-page-primary-nav"
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
                    <p>This is a full page demo.</p>
                  </div>
                </div>
              </section>
              <section class="pf-c-page__main-section pf-m-limit-width">
                <div class="pf-c-page__main-body">
                  <div class="pf-l-gallery pf-m-gutter">
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                  </div>
                </div>
              </section>
            </main>
          </div>
        </div>
        <div class="pf-c-drawer__panel pf-m-width-33" hidden>
          <div class="pf-c-drawer__body pf-m-no-padding">
            <div class="pf-c-notification-drawer">
              <div class="pf-c-notification-drawer__header">
                <h1 class="pf-c-notification-drawer__header-title">Notifications</h1>
                <span class="pf-c-notification-drawer__header-status">3 unread</span>
                <div class="pf-c-notification-drawer__header-action">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="drawer-demo-notification-drawer-basic-header-action-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Actions"
                    >
                      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-demo-notification-drawer-basic-header-action-button"
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
              </div>
              <div class="pf-c-notification-drawer__body">
                <ul class="pf-c-notification-drawer__list">
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-info"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-info-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Info notification:</span>
                        Unread
                        info notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-1-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-1-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is an info notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >5 minutes ago</div>
                  </li>

                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Default notification:</span>
                        Unread
                        recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-2-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-2-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >10 minutes ago</div>
                  </li>

                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Default notification:</span>
                        Unread
                        recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-3-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-3-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >20 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-warning pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i
                          class="fas fa-exclamation-triangle"
                          aria-hidden="true"
                        ></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Warning notification:</span>
                        Read warning notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-4-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-4-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a warning notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >20 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Read success notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-5-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-5-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a success notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >30 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-6-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-6-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >40 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                        style="--pf-c-notification-drawer__list-item-header-title--max-lines: 2"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-7-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-7-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >50 minutes ago</div>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Expanded read

```html isFullscreen
<div class="pf-c-page" id="drawer-expanded-read-example-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-drawer-expanded-read-example-page"
  >Skip to content</a>
  <header class="pf-c-masthead" id="drawer-expanded-read-example-page-masthead">
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
        id="drawer-expanded-read-example-page-masthead-toolbar"
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
                  <span class="pf-c-notification-badge pf-m-read">
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
                    id="drawer-expanded-read-example-page-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="drawer-expanded-read-example-page-masthead-icon-group--app-launcher-button"
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
                      id="drawer-expanded-read-example-page-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-expanded-read-example-page-masthead-settings-button"
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
                      id="drawer-expanded-read-example-page-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-expanded-read-example-page-masthead-help-button"
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
                  id="drawer-expanded-read-example-page-masthead-profile-button"
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
        id="drawer-expanded-read-example-page-primary-nav"
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
                    <p>This is a full page demo.</p>
                  </div>
                </div>
              </section>
              <section class="pf-c-page__main-section pf-m-limit-width">
                <div class="pf-c-page__main-body">
                  <div class="pf-l-gallery pf-m-gutter">
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                  </div>
                </div>
              </section>
            </main>
          </div>
        </div>
        <div class="pf-c-drawer__panel pf-m-width-33">
          <div class="pf-c-drawer__body pf-m-no-padding">
            <div class="pf-c-notification-drawer">
              <div class="pf-c-notification-drawer__header">
                <h1 class="pf-c-notification-drawer__header-title">Notifications</h1>
                <span class="pf-c-notification-drawer__header-status">0 unread</span>
                <div class="pf-c-notification-drawer__header-action">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="drawer-demo-notification-drawer-basic-header-action-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Actions"
                    >
                      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-demo-notification-drawer-basic-header-action-button"
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
              </div>
              <div class="pf-c-notification-drawer__body">
                <ul class="pf-c-notification-drawer__list">
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-info"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-info-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Info notification:</span>
                        Read
                        info notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-1-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-1-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is an info notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >5 minutes ago</div>
                  </li>

                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-default"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Default notification:</span>
                        Read
                        recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-2-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-2-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >10 minutes ago</div>
                  </li>

                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-default"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Default notification:</span>
                        Read
                        recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-3-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-3-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >20 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-warning pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i
                          class="fas fa-exclamation-triangle"
                          aria-hidden="true"
                        ></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Warning notification:</span>
                        Read warning notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-4-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-4-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a warning notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >20 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Read success notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-5-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-5-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a success notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >30 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-6-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-6-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >40 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                        style="--pf-c-notification-drawer__list-item-header-title--max-lines: 2"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-7-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-7-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >50 minutes ago</div>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Expanded unread

```html isFullscreen
<div class="pf-c-page" id="drawer-expanded-unread-example-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-drawer-expanded-unread-example-page"
  >Skip to content</a>
  <header
    class="pf-c-masthead"
    id="drawer-expanded-unread-example-page-masthead"
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
        id="drawer-expanded-unread-example-page-masthead-toolbar"
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
                  <span class="pf-c-notification-badge pf-m-unread">
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
                    id="drawer-expanded-unread-example-page-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="drawer-expanded-unread-example-page-masthead-icon-group--app-launcher-button"
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
                      id="drawer-expanded-unread-example-page-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-expanded-unread-example-page-masthead-settings-button"
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
                      id="drawer-expanded-unread-example-page-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-expanded-unread-example-page-masthead-help-button"
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
                  id="drawer-expanded-unread-example-page-masthead-profile-button"
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
        id="drawer-expanded-unread-example-page-primary-nav"
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
                    <p>This is a full page demo.</p>
                  </div>
                </div>
              </section>
              <section class="pf-c-page__main-section pf-m-limit-width">
                <div class="pf-c-page__main-body">
                  <div class="pf-l-gallery pf-m-gutter">
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                  </div>
                </div>
              </section>
            </main>
          </div>
        </div>
        <div class="pf-c-drawer__panel pf-m-width-33">
          <div class="pf-c-drawer__body pf-m-no-padding">
            <div class="pf-c-notification-drawer">
              <div class="pf-c-notification-drawer__header">
                <h1 class="pf-c-notification-drawer__header-title">Notifications</h1>
                <span class="pf-c-notification-drawer__header-status">3 unread</span>
                <div class="pf-c-notification-drawer__header-action">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="drawer-demo-notification-drawer-basic-header-action-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Actions"
                    >
                      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-demo-notification-drawer-basic-header-action-button"
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
              </div>
              <div class="pf-c-notification-drawer__body">
                <ul class="pf-c-notification-drawer__list">
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-info"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-info-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Info notification:</span>
                        Unread
                        info notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-1-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-1-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is an info notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >5 minutes ago</div>
                  </li>

                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Default notification:</span>
                        Unread
                        recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-2-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-2-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >10 minutes ago</div>
                  </li>

                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Default notification:</span>
                        Unread
                        recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-3-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-3-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >20 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-warning pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i
                          class="fas fa-exclamation-triangle"
                          aria-hidden="true"
                        ></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Warning notification:</span>
                        Read warning notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-4-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-4-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a warning notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >20 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Read success notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-5-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-5-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a success notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >30 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-6-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-6-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >40 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                        style="--pf-c-notification-drawer__list-item-header-title--max-lines: 2"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-7-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-7-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >50 minutes ago</div>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Expanded attention

```html isFullscreen
<div class="pf-c-page" id="drawer-expanded-attention-example-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-drawer-expanded-attention-example-page"
  >Skip to content</a>
  <header
    class="pf-c-masthead"
    id="drawer-expanded-attention-example-page-masthead"
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
        id="drawer-expanded-attention-example-page-masthead-toolbar"
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
                  <span class="pf-c-notification-badge pf-m-attention">
                    <i class="pf-icon-attention-bell" aria-hidden="true"></i>
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
                    id="drawer-expanded-attention-example-page-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="drawer-expanded-attention-example-page-masthead-icon-group--app-launcher-button"
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
                      id="drawer-expanded-attention-example-page-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-expanded-attention-example-page-masthead-settings-button"
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
                      id="drawer-expanded-attention-example-page-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-expanded-attention-example-page-masthead-help-button"
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
                  id="drawer-expanded-attention-example-page-masthead-profile-button"
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
        id="drawer-expanded-attention-example-page-primary-nav"
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
                    <p>This is a full page demo.</p>
                  </div>
                </div>
              </section>
              <section class="pf-c-page__main-section pf-m-limit-width">
                <div class="pf-c-page__main-body">
                  <div class="pf-l-gallery pf-m-gutter">
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                  </div>
                </div>
              </section>
            </main>
          </div>
        </div>
        <div class="pf-c-drawer__panel pf-m-width-33">
          <div class="pf-c-drawer__body pf-m-no-padding">
            <div class="pf-c-notification-drawer">
              <div class="pf-c-notification-drawer__header">
                <h1 class="pf-c-notification-drawer__header-title">Notifications</h1>
                <span class="pf-c-notification-drawer__header-status">3 unread</span>
                <div class="pf-c-notification-drawer__header-action">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="drawer-demo-notification-drawer-basic-header-action-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Actions"
                    >
                      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-demo-notification-drawer-basic-header-action-button"
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
              </div>
              <div class="pf-c-notification-drawer__body">
                <ul class="pf-c-notification-drawer__list">
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-info"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-info-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Info notification:</span>
                        Unread
                        info notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-1-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-1-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is an info notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >5 minutes ago</div>
                  </li>

                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-danger"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Danger notification:</span>
                        Unread danger notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-2-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-2-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a danger notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >10 minutes ago</div>
                  </li>

                  <li
                    class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                    tabindex="0"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Default notification:</span>
                        Unread
                        recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-3-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-3-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >20 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-warning pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i
                          class="fas fa-exclamation-triangle"
                          aria-hidden="true"
                        ></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Warning notification:</span>
                        Read warning notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-4-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-4-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a warning notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >20 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Read success notification title
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-5-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-5-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This is a success notification description.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >30 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-6-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-6-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >40 minutes ago</div>
                  </li>
                  <li
                    class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                  >
                    <div class="pf-c-notification-drawer__list-item-header">
                      <span
                        class="pf-c-notification-drawer__list-item-header-icon"
                      >
                        <i class="fas fa-check-circle" aria-hidden="true"></i>
                      </span>
                      <h2
                        class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                        style="--pf-c-notification-drawer__list-item-header-title--max-lines: 2"
                      >
                        <span class="pf-screen-reader">Success notification:</span>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                      </h2>
                    </div>
                    <div class="pf-c-notification-drawer__list-item-action">
                      <div class="pf-c-dropdown pf-m-top">
                        <button
                          class="pf-c-dropdown__toggle pf-m-plain"
                          id="drawer-demo-notification-drawer-basicdropdown-kebab-7-button"
                          aria-expanded="false"
                          type="button"
                          aria-label="Actions"
                        >
                          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                        </button>
                        <ul
                          class="pf-c-dropdown__menu pf-m-align-right"
                          aria-labelledby="drawer-demo-notification-drawer-basicdropdown-kebab-7-button"
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
                      class="pf-c-notification-drawer__list-item-description"
                    >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
                    <div
                      class="pf-c-notification-drawer__list-item-timestamp"
                    >50 minutes ago</div>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

```

### Expanded with groups

```html isFullscreen
<div class="pf-c-page" id="drawer-expanded-with-groups-example-page">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-drawer-expanded-with-groups-example-page"
  >Skip to content</a>
  <header
    class="pf-c-masthead"
    id="drawer-expanded-with-groups-example-page-masthead"
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
        id="drawer-expanded-with-groups-example-page-masthead-toolbar"
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
                  <span class="pf-c-notification-badge pf-m-unread">
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
                    id="drawer-expanded-with-groups-example-page-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="drawer-expanded-with-groups-example-page-masthead-icon-group--app-launcher-button"
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
                      id="drawer-expanded-with-groups-example-page-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-expanded-with-groups-example-page-masthead-settings-button"
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
                      id="drawer-expanded-with-groups-example-page-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-expanded-with-groups-example-page-masthead-help-button"
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
                  id="drawer-expanded-with-groups-example-page-masthead-profile-button"
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
        id="drawer-expanded-with-groups-example-page-primary-nav"
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
                    <p>This is a full page demo.</p>
                  </div>
                </div>
              </section>
              <section class="pf-c-page__main-section pf-m-limit-width">
                <div class="pf-c-page__main-body">
                  <div class="pf-l-gallery pf-m-gutter">
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                    <div class="pf-c-card">
                      <div class="pf-c-card__body">This is a card</div>
                    </div>
                  </div>
                </div>
              </section>
            </main>
          </div>
        </div>
        <div class="pf-c-drawer__panel pf-m-width-33">
          <div class="pf-c-drawer__body pf-m-no-padding">
            <div class="pf-c-notification-drawer">
              <div class="pf-c-notification-drawer__header">
                <h1 class="pf-c-notification-drawer__header-title">Notifications</h1>
                <span class="pf-c-notification-drawer__header-status">9 unread</span>
                <div class="pf-c-notification-drawer__header-action">
                  <div class="pf-c-dropdown">
                    <button
                      class="pf-c-dropdown__toggle pf-m-plain"
                      id="drawer-demo-notification-drawer-groups-header-action-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Actions"
                    >
                      <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="drawer-demo-notification-drawer-groups-header-action-button"
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
              </div>
              <div class="pf-c-notification-drawer__body">
                <div class="pf-c-notification-drawer__group-list">
                  <section class="pf-c-notification-drawer__group">
                    <h1>
                      <button
                        class="pf-c-notification-drawer__group-toggle"
                        aria-expanded="false"
                      >
                        <div
                          class="pf-c-notification-drawer__group-toggle-title"
                        >First notification group</div>
                        <div
                          class="pf-c-notification-drawer__group-toggle-count"
                        >
                          <span class="pf-c-badge pf-m-unread">2</span>
                        </div>
                        <span
                          class="pf-c-notification-drawer__group-toggle-icon"
                        >
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </span>
                      </button>
                    </h1>
                    <ul class="pf-c-notification-drawer__list" hidden>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-info"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-info-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Info notification:</span>
                            Unread
                            info notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group1dropdown-kebab-1-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group1dropdown-kebab-1-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is an info notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >5 minutes ago</div>
                      </li>

                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-arrow-circle-up"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Default notification:</span>
                            Unread
                            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group1dropdown-kebab-2-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group1dropdown-kebab-2-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >10 minutes ago</div>
                      </li>

                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-arrow-circle-up"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Default notification:</span>
                            Unread
                            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group1dropdown-kebab-3-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group1dropdown-kebab-3-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >20 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-warning pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-exclamation-triangle"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Warning notification:</span>
                            Read warning notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group1dropdown-kebab-4-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group1dropdown-kebab-4-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a warning notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >20 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Read success notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group1dropdown-kebab-5-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group1dropdown-kebab-5-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a success notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >30 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group1dropdown-kebab-6-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group1dropdown-kebab-6-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >40 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                            style="--pf-c-notification-drawer__list-item-header-title--max-lines: 2"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group1dropdown-kebab-7-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group1dropdown-kebab-7-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >50 minutes ago</div>
                      </li>
                    </ul>
                  </section>
                  <section
                    class="pf-c-notification-drawer__group pf-m-expanded"
                  >
                    <h1>
                      <button
                        class="pf-c-notification-drawer__group-toggle"
                        aria-expanded="true"
                      >
                        <div
                          class="pf-c-notification-drawer__group-toggle-title"
                        >Second notification group</div>
                        <div
                          class="pf-c-notification-drawer__group-toggle-count"
                        >
                          <span class="pf-c-badge pf-m-unread">3</span>
                        </div>
                        <span
                          class="pf-c-notification-drawer__group-toggle-icon"
                        >
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </span>
                      </button>
                    </h1>
                    <ul class="pf-c-notification-drawer__list">
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-info"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-info-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Info notification:</span>
                            Unread
                            info notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group2dropdown-kebab-1-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group2dropdown-kebab-1-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is an info notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >5 minutes ago</div>
                      </li>

                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-arrow-circle-up"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Default notification:</span>
                            Unread
                            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group2dropdown-kebab-2-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group2dropdown-kebab-2-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >10 minutes ago</div>
                      </li>

                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-arrow-circle-up"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Default notification:</span>
                            Unread
                            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group2dropdown-kebab-3-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group2dropdown-kebab-3-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >20 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-warning pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-exclamation-triangle"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Warning notification:</span>
                            Read warning notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group2dropdown-kebab-4-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group2dropdown-kebab-4-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a warning notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >20 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Read success notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group2dropdown-kebab-5-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group2dropdown-kebab-5-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a success notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >30 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group2dropdown-kebab-6-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group2dropdown-kebab-6-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >40 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                            style="--pf-c-notification-drawer__list-item-header-title--max-lines: 2"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group2dropdown-kebab-7-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group2dropdown-kebab-7-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >50 minutes ago</div>
                      </li>
                    </ul>
                  </section>
                  <section class="pf-c-notification-drawer__group">
                    <h1>
                      <button
                        class="pf-c-notification-drawer__group-toggle"
                        aria-expanded="false"
                      >
                        <div
                          class="pf-c-notification-drawer__group-toggle-title"
                        >Third notification group</div>
                        <div
                          class="pf-c-notification-drawer__group-toggle-count"
                        >
                          <span class="pf-c-badge pf-m-unread">2</span>
                        </div>
                        <span
                          class="pf-c-notification-drawer__group-toggle-icon"
                        >
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </span>
                      </button>
                    </h1>
                    <ul class="pf-c-notification-drawer__list" hidden>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-info"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-info-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Info notification:</span>
                            Unread
                            info notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group3dropdown-kebab-1-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group3dropdown-kebab-1-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is an info notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >5 minutes ago</div>
                      </li>

                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-arrow-circle-up"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Default notification:</span>
                            Unread
                            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group3dropdown-kebab-2-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group3dropdown-kebab-2-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >10 minutes ago</div>
                      </li>

                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-arrow-circle-up"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Default notification:</span>
                            Unread
                            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group3dropdown-kebab-3-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group3dropdown-kebab-3-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >20 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-warning pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-exclamation-triangle"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Warning notification:</span>
                            Read warning notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group3dropdown-kebab-4-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group3dropdown-kebab-4-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a warning notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >20 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Read success notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group3dropdown-kebab-5-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group3dropdown-kebab-5-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a success notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >30 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group3dropdown-kebab-6-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group3dropdown-kebab-6-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >40 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                            style="--pf-c-notification-drawer__list-item-header-title--max-lines: 2"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group3dropdown-kebab-7-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group3dropdown-kebab-7-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >50 minutes ago</div>
                      </li>
                    </ul>
                  </section>
                  <section class="pf-c-notification-drawer__group">
                    <h1>
                      <button
                        class="pf-c-notification-drawer__group-toggle"
                        aria-expanded="false"
                      >
                        <div
                          class="pf-c-notification-drawer__group-toggle-title"
                        >Fourth notification group</div>
                        <div
                          class="pf-c-notification-drawer__group-toggle-count"
                        >
                          <span class="pf-c-badge pf-m-unread">2</span>
                        </div>
                        <span
                          class="pf-c-notification-drawer__group-toggle-icon"
                        >
                          <i class="fas fa-angle-right" aria-hidden="true"></i>
                        </span>
                      </button>
                    </h1>
                    <ul class="pf-c-notification-drawer__list" hidden>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-info"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-info-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Info notification:</span>
                            Unread
                            info notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group4dropdown-kebab-1-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group4dropdown-kebab-1-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is an info notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >5 minutes ago</div>
                      </li>

                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-arrow-circle-up"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Default notification:</span>
                            Unread
                            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group4dropdown-kebab-2-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group4dropdown-kebab-2-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >10 minutes ago</div>
                      </li>

                      <li
                        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
                        tabindex="0"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-arrow-circle-up"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Default notification:</span>
                            Unread
                            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group4dropdown-kebab-3-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group4dropdown-kebab-3-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a recommendation notification description. This is a long description to show how the title will wrap if it is long and wraps to multiple lines.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >20 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-warning pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i
                              class="fas fa-exclamation-triangle"
                              aria-hidden="true"
                            ></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Warning notification:</span>
                            Read warning notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group4dropdown-kebab-4-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group4dropdown-kebab-4-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a warning notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >20 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Read success notification title
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group4dropdown-kebab-5-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group4dropdown-kebab-5-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This is a success notification description.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >30 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group4dropdown-kebab-6-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group4dropdown-kebab-6-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This example uses ".pf-m-truncate" to limit the title to a single line and truncate any overflow text with ellipses.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >40 minutes ago</div>
                      </li>
                      <li
                        class="pf-c-notification-drawer__list-item pf-m-read pf-m-success pf-m-hoverable"
                      >
                        <div class="pf-c-notification-drawer__list-item-header">
                          <span
                            class="pf-c-notification-drawer__list-item-header-icon"
                          >
                            <i class="fas fa-check-circle" aria-hidden="true"></i>
                          </span>
                          <h2
                            class="pf-c-notification-drawer__list-item-header-title pf-m-truncate"
                            style="--pf-c-notification-drawer__list-item-header-title--max-lines: 2"
                          >
                            <span class="pf-screen-reader">Success notification:</span>
                            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent quis odio risus. Ut dictum vitae sapien at posuere. Nullam suscipit massa quis lacus pellentesque scelerisque. Donec non maximus neque, quis ornare nunc. Vivamus in nibh sed libero feugiat feugiat. Nulla lacinia rutrum est, a commodo odio vestibulum suscipit. Nullam id quam et quam porttitor interdum quis nec tellus. Vestibulum arcu dui, pulvinar eu tellus in, semper mattis diam. Sed commodo tincidunt lacus non pulvinar. Curabitur tempor molestie vestibulum. Vivamus vel mi dignissim, efficitur neque eget, efficitur massa. Mauris vitae nunc augue. Donec augue lorem, malesuada et quam vitae, volutpat mattis nisi. Nullam nec venenatis ex, quis lobortis purus. Sed nisl dolor, mattis sit amet tincidunt quis, mollis sed massa.
                          </h2>
                        </div>
                        <div class="pf-c-notification-drawer__list-item-action">
                          <div class="pf-c-dropdown pf-m-top">
                            <button
                              class="pf-c-dropdown__toggle pf-m-plain"
                              id="drawer-demo-notification-drawer-groups-group4dropdown-kebab-7-button"
                              aria-expanded="false"
                              type="button"
                              aria-label="Actions"
                            >
                              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                            </button>
                            <ul
                              class="pf-c-dropdown__menu pf-m-align-right"
                              aria-labelledby="drawer-demo-notification-drawer-groups-group4dropdown-kebab-7-button"
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
                        </div>
                        <div
                          class="pf-c-notification-drawer__list-item-description"
                        >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
                        <div
                          class="pf-c-notification-drawer__list-item-timestamp"
                        >50 minutes ago</div>
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
  </div>
</div>

```

## Documentation

This demo implements the notification drawer in context of the page component.
