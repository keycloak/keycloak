---
id: 'Card view'
section: demos
---## Examples

### Card view

```html isFullscreen
<div class="pf-c-page" id="card-view-basic-example">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-card-view-basic-example"
  >Skip to content</a>
  <header class="pf-c-masthead" id="card-view-basic-example-masthead">
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
        id="card-view-basic-example-masthead-toolbar"
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
                    id="card-view-basic-example-masthead-icon-group--app-launcher"
                  >
                    <button
                      class="pf-c-app-launcher__toggle"
                      type="button"
                      id="card-view-basic-example-masthead-icon-group--app-launcher-button"
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
                      id="card-view-basic-example-masthead-settings-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Settings"
                    >
                      <i class="fas fa-cog" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="card-view-basic-example-masthead-settings-button"
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
                      id="card-view-basic-example-masthead-help-button"
                      aria-expanded="false"
                      type="button"
                      aria-label="Help"
                    >
                      <i class="fas fa-question-circle" aria-hidden="true"></i>
                    </button>
                    <ul
                      class="pf-c-dropdown__menu pf-m-align-right"
                      aria-labelledby="card-view-basic-example-masthead-help-button"
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
                  id="card-view-basic-example-masthead-profile-button"
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
        id="card-view-basic-example-expandable-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item pf-m-expandable pf-m-expanded pf-m-current">
            <button
              class="pf-c-nav__link"
              id="card-view-basic-example-expandable-nav-link1"
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
              aria-labelledby="card-view-basic-example-expandable-nav-link1"
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
              id="card-view-basic-example-expandable-nav-link2"
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
              aria-labelledby="card-view-basic-example-expandable-nav-link2"
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
              id="card-view-basic-example-expandable-nav-link3"
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
              aria-labelledby="card-view-basic-example-expandable-nav-link3"
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
    id="main-content-card-view-basic-example"
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
      <div
        class="pf-c-toolbar pf-m-page-insets"
        id="card-view-basic-example-toolbar"
      >
        <div class="pf-c-toolbar__content">
          <div class="pf-c-toolbar__content-section pf-m-nowrap">
            <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-xl">
              <div class="pf-c-toolbar__toggle">
                <button
                  class="pf-c-button pf-m-plain"
                  type="button"
                  aria-label="Show filters"
                  aria-expanded="false"
                  aria-controls="card-view-basic-example-toolbar-expandable-content"
                >
                  <i class="fas fa-filter" aria-hidden="true"></i>
                </button>
              </div>
              <div class="pf-c-toolbar__item pf-m-bulk-select">
                <div class="pf-c-dropdown">
                  <div class="pf-c-dropdown__toggle pf-m-split-button">
                    <label
                      class="pf-c-dropdown__toggle-check"
                      for="card-view-basic-example-toolbar-bulk-select-toggle-check"
                    >
                      <input
                        type="checkbox"
                        id="card-view-basic-example-toolbar-bulk-select-toggle-check"
                        aria-label="Select all"
                      />
                    </label>

                    <button
                      class="pf-c-dropdown__toggle-button"
                      type="button"
                      aria-expanded="false"
                      id="card-view-basic-example-toolbar-bulk-select-toggle-button"
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
                    id="card-view-basic-example-toolbar-select-checkbox-status-label"
                    hidden
                  >Choose one</span>

                  <button
                    class="pf-c-select__toggle"
                    type="button"
                    id="card-view-basic-example-toolbar-select-checkbox-status-toggle"
                    aria-haspopup="true"
                    aria-expanded="false"
                    aria-labelledby="card-view-basic-example-toolbar-select-checkbox-status-label card-view-basic-example-toolbar-select-checkbox-status-toggle"
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
                        for="card-view-basic-example-toolbar-select-checkbox-status-active"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="card-view-basic-example-toolbar-select-checkbox-status-active"
                          name="card-view-basic-example-toolbar-select-checkbox-status-active"
                        />

                        <span class="pf-c-check__label">Active</span>
                        <span
                          class="pf-c-check__description"
                        >This is a description</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item pf-m-description"
                        for="card-view-basic-example-toolbar-select-checkbox-status-canceled"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="card-view-basic-example-toolbar-select-checkbox-status-canceled"
                          name="card-view-basic-example-toolbar-select-checkbox-status-canceled"
                        />

                        <span class="pf-c-check__label">Canceled</span>
                        <span
                          class="pf-c-check__description"
                        >This is a really long description that describes the menu item. This is a really long description that describes the menu item.</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="card-view-basic-example-toolbar-select-checkbox-status-paused"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="card-view-basic-example-toolbar-select-checkbox-status-paused"
                          name="card-view-basic-example-toolbar-select-checkbox-status-paused"
                        />

                        <span class="pf-c-check__label">Paused</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="card-view-basic-example-toolbar-select-checkbox-status-warning"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="card-view-basic-example-toolbar-select-checkbox-status-warning"
                          name="card-view-basic-example-toolbar-select-checkbox-status-warning"
                        />

                        <span class="pf-c-check__label">Warning</span>
                      </label>
                      <label
                        class="pf-c-check pf-c-select__menu-item"
                        for="card-view-basic-example-toolbar-select-checkbox-status-restarted"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="card-view-basic-example-toolbar-select-checkbox-status-restarted"
                          name="card-view-basic-example-toolbar-select-checkbox-status-restarted"
                        />

                        <span class="pf-c-check__label">Restarted</span>
                      </label>
                    </fieldset>
                  </div>
                </div>
              </div>
            </div>

            <div
              class="pf-c-overflow-menu"
              id="card-view-basic-example-toolbar-overflow-menu"
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
                </div>
              </div>
              <div class="pf-c-overflow-menu__control">
                <div class="pf-c-dropdown">
                  <button
                    class="pf-c-button pf-c-dropdown__toggle pf-m-plain"
                    type="button"
                    id="card-view-basic-example-toolbar-overflow-menu-dropdown-toggle"
                    aria-label="Dropdown with additional options"
                    aria-expanded="false"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu"
                    aria-labelledby="card-view-basic-example-toolbar-overflow-menu-dropdown-toggle"
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
                    id="card-view-basic-example-toolbar-top-pagination-toggle"
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
                    aria-labelledby="card-view-basic-example-toolbar-top-pagination-toggle"
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
            id="card-view-basic-example-toolbar-expandable-content"
            hidden
          ></div>
        </div>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-fill">
      <div class="pf-l-gallery pf-m-gutter">
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-empty-state"
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
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-1"
        >
          <div class="pf-c-card__header">
            <img src="/assets/images/pf-logo-small.svg" alt="PatternFly logo" />
            <div class="pf-c-card__actions">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="card-view-basic-example-gallery-card-1-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-1-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-1-check"
                  name="card-view-basic-example-gallery-card-1-check"
                  aria-labelledby="card-view-basic-example-gallery-card-1-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p
              id="card-view-basic-example-gallery-card-1-check-label"
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
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-2"
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
                  id="card-view-basic-example-gallery-card-2-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-2-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-2-check"
                  name="card-view-basic-example-gallery-card-2-check"
                  aria-labelledby="card-view-basic-example-gallery-card-2-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-view-basic-example-gallery-card-2-check-label">ActiveMQ</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >The ActiveMQ component allows messages to be sent to a JMS Queue or Topic; or messages to be consumed from a JMS Queue or Topic using Apache ActiveMQ.</div>
        </div>
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-3"
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
                  id="card-view-basic-example-gallery-card-3-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-3-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-3-check"
                  name="card-view-basic-example-gallery-card-3-check"
                  aria-labelledby="card-view-basic-example-gallery-card-3-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p
              id="card-view-basic-example-gallery-card-3-check-label"
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
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-4"
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
                  id="card-view-basic-example-gallery-card-4-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-4-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-4-check"
                  name="card-view-basic-example-gallery-card-4-check"
                  aria-labelledby="card-view-basic-example-gallery-card-4-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-view-basic-example-gallery-card-4-check-label">Avro</p>
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
          id="card-view-basic-example-gallery-card-5"
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
                  id="card-view-basic-example-gallery-card-5-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-5-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-5-check"
                  name="card-view-basic-example-gallery-card-5-check"
                  aria-labelledby="card-view-basic-example-gallery-card-5-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p
              id="card-view-basic-example-gallery-card-5-check-label"
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
          class="pf-c-card pf-m-non-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-6"
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
                  id="card-view-basic-example-gallery-card-6-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-6-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-6-check"
                  name="card-view-basic-example-gallery-card-6-check"
                  disabled
                  aria-labelledby="card-view-basic-example-gallery-card-6-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-view-basic-example-gallery-card-6-check-label">Crypto</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >For providing flexible endpoints to sign and verify exchanges using the Signature Service of the Java Cryptographic Extension.</div>
        </div>
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-7"
        >
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
                  id="card-view-basic-example-gallery-card-7-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-7-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-7-check"
                  name="card-view-basic-example-gallery-card-7-check"
                  aria-labelledby="card-view-basic-example-gallery-card-7-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-view-basic-example-gallery-card-7-check-label">DropBox</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >The dropbox: component allows you to treat Dropbox remote folders as a producer or consumer of messages.</div>
        </div>
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-8"
        >
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
                  id="card-view-basic-example-gallery-card-8-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-8-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-8-check"
                  name="card-view-basic-example-gallery-card-8-check"
                  aria-labelledby="card-view-basic-example-gallery-card-8-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p
              id="card-view-basic-example-gallery-card-8-check-label"
            >JBoss Data Grid</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div
            class="pf-c-card__body"
          >Read or write to a fully-supported distributed cache and data grid for faster integration services.</div>
        </div>
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-9"
        >
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
                  id="card-view-basic-example-gallery-card-9-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-9-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-9-check"
                  name="card-view-basic-example-gallery-card-9-check"
                  aria-labelledby="card-view-basic-example-gallery-card-9-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-view-basic-example-gallery-card-9-check-label">REST</p>
            <div class="pf-c-content">
              <small>Provided by Red Hat</small>
            </div>
          </div>
          <div class="pf-c-card__body">
            The rest component allows to define REST endpoints (consumer) using the Rest DSL and plugin to other Camel components as the REST transport.
            From Camel 2.18 onwards the rest component can also be used as a client (producer) to call REST services.
          </div>
        </div>
        <div
          class="pf-c-card pf-m-selectable-raised pf-m-compact"
          id="card-view-basic-example-gallery-card-10"
        >
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
                  id="card-view-basic-example-gallery-card-10-dropdown-kebab-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="card-view-basic-example-gallery-card-10-dropdown-kebab-button"
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
                  id="card-view-basic-example-gallery-card-10-check"
                  name="card-view-basic-example-gallery-card-10-check"
                  aria-labelledby="card-view-basic-example-gallery-card-10-check-label"
                />
              </div>
            </div>
          </div>
          <div class="pf-c-card__title">
            <p id="card-view-basic-example-gallery-card-10-check-label">SWAGGER</p>
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
            id="pagination-options-menu-bottom-example-toggle"
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
