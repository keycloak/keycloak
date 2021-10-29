---
id: 'Banner'
section: components
beta: true
cssPrefix: pf-c-banner
wrapperTag: div
---## Examples

### Basic

```html isFullscreen
<div class="pf-c-page" id="page-layout-table-simple">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-page-layout-table-simple"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="page-layout-table-simple-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="page-layout-table-simple-primary-nav"
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
              id="page-layout-table-simple-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="page-layout-table-simple-dropdown-kebab-1-button"
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
              id="page-layout-table-simple-dropdown-kebab-2-button"
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
              aria-labelledby="page-layout-table-simple-dropdown-kebab-2-button"
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
        id="page-layout-table-simple-primary-nav"
        aria-label="Global"
      >
        <ul class="pf-c-nav__list">
          <li class="pf-c-nav__item pf-m-expandable pf-m-expanded pf-m-current">
            <button class="pf-c-nav__link" aria-expanded="true">
              Components
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-layout-table-simple-subnav-title1"
            >
              <h2
                class="pf-c-nav__subnav-title pf-screen-reader"
                id="page-layout-table-simple-subnav-title1"
              >First nav item</h2>
              <ul class="pf-c-nav__list">
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Forms</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Table</a>
                </li>
                <li class="pf-c-nav__item">
                  <a
                    href="#"
                    class="pf-c-nav__link pf-m-current"
                    aria-current="page"
                  >Data list</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Icons</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">Layouts</a>
                </li>
                <li class="pf-c-nav__item">
                  <a href="#" class="pf-c-nav__link">List</a>
                </li>
              </ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button class="pf-c-nav__link" aria-expanded="false">
              Patterns
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-layout-table-simple-subnav-title2"
              hidden
            >
              <h2
                class="pf-c-nav__subnav-title pf-screen-reader"
                id="page-layout-table-simple-subnav-title2"
              >Second nav item</h2>
              <ul class="pf-c-nav__list"></ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button class="pf-c-nav__link" aria-expanded="false">
              Typography
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-layout-table-simple-subnav-title3"
              hidden
            >
              <h2
                class="pf-c-nav__subnav-title pf-screen-reader"
                id="page-layout-table-simple-subnav-title3"
              >Third nav item</h2>
              <ul class="pf-c-nav__list"></ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button class="pf-c-nav__link" aria-expanded="false">
              Icons
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-layout-table-simple-subnav-title4"
              hidden
            >
              <h2
                class="pf-c-nav__subnav-title pf-screen-reader"
                id="page-layout-table-simple-subnav-title4"
              >Second nav item</h2>
              <ul class="pf-c-nav__list"></ul>
            </section>
          </li>
          <li class="pf-c-nav__item pf-m-expandable">
            <button class="pf-c-nav__link" aria-expanded="false">
              Colors
              <span class="pf-c-nav__toggle">
                <span class="pf-c-nav__toggle-icon">
                  <i class="fas fa-angle-right" aria-hidden="true"></i>
                </span>
              </span>
            </button>
            <section
              class="pf-c-nav__subnav"
              aria-labelledby="page-layout-table-simple-subnav-title5"
              hidden
            >
              <h2
                class="pf-c-nav__subnav-title pf-screen-reader"
                id="page-layout-table-simple-subnav-title5"
              >Second nav item</h2>
              <ul class="pf-c-nav__list"></ul>
            </section>
          </li>
        </ul>
      </nav>
    </div>
  </div>
  <main
    class="pf-c-page__main"
    tabindex="-1"
    id="main-content-page-layout-table-simple"
  >
    <div class="pf-c-banner pf-m-sticky">
      <div
        class="pf-l-flex pf-m-justify-content-center pf-m-justify-content-space-between-on-lg pf-m-nowrap"
      >
        <div class="pf-u-display-none pf-u-display-block-on-lg">Localhost</div>
        <div
          class="pf-u-display-none pf-u-display-block-on-lg"
        >This message is sticky to the top or bottom of the page.</div>
        <div
          class="pf-u-display-none-on-lg"
        >Drop some text on mobile, truncate if needed.</div>
        <div class="pf-u-display-none pf-u-display-block-on-lg">Ned Username</div>
      </div>
    </div>
    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Table demos</h1>
        <p>Below is an example of a Table.</p>
      </div>
    </section>
    <section class="pf-c-page__main-section pf-m-no-padding pf-m-padding-on-xl">
      <div class="pf-c-card">
        <table
          class="pf-c-table pf-m-grid-xl"
          role="grid"
          aria-label="This is a table with checkboxes"
          id="page-layout-table-simple-table"
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
              <td role="cell"></td>

              <td role="cell"></td>
            </tr>
          </thead>

          <tbody role="rowgroup">
            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow1"
                  aria-labelledby="page-layout-table-simple-table-node1"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div id="page-layout-table-simple-table-node1">Node 1</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 10
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 25
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 5
                </span>
              </td>
              <td role="cell" data-label="Last commit">2 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown pf-m-expanded">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="page-layout-table-simple-table-dropdown-kebab-1-button"
                    aria-expanded="true"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="page-layout-table-simple-table-dropdown-kebab-1-button"
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
              </td>
            </tr>

            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow2"
                  aria-labelledby="page-layout-table-simple-table-node2"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div id="page-layout-table-simple-table-node2">Node 2</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 8
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 30
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 2
                </span>
              </td>
              <td role="cell" data-label="Last commit">2 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown pf-m-expanded">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="page-layout-table-simple-table-dropdown-kebab-2-button"
                    aria-expanded="true"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="page-layout-table-simple-table-dropdown-kebab-2-button"
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
              </td>
            </tr>

            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow3"
                  aria-labelledby="page-layout-table-simple-table-node3"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div id="page-layout-table-simple-table-node3">Node 3</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 12
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 48
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 13
                </span>
              </td>
              <td role="cell" data-label="Last commit">30 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown pf-m-expanded">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="page-layout-table-simple-table-dropdown-kebab-3-button"
                    aria-expanded="true"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="page-layout-table-simple-table-dropdown-kebab-3-button"
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
              </td>
            </tr>

            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow4"
                  aria-labelledby="page-layout-table-simple-table-node4"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div id="page-layout-table-simple-table-node4">Node 4</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 3
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 8
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 20
                </span>
              </td>
              <td role="cell" data-label="Last commit">8 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown pf-m-expanded">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="page-layout-table-simple-table-dropdown-kebab-4-button"
                    aria-expanded="true"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="page-layout-table-simple-table-dropdown-kebab-4-button"
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
              </td>
            </tr>

            <tr role="row">
              <td class="pf-c-table__check" role="cell">
                <input
                  type="checkbox"
                  name="checkrow5"
                  aria-labelledby="page-layout-table-simple-table-node5"
                />
              </td>
              <th role="columnheader" data-label="Repository name">
                <div>
                  <div id="page-layout-table-simple-table-node5">Node 5</div>
                  <a href="#">siemur/test-space</a>
                </div>
              </th>
              <td role="cell" data-label="Branches">
                <span>
                  <i class="fas fa-code-branch"></i> 34
                </span>
              </td>
              <td role="cell" data-label="Pull requests">
                <span>
                  <i class="fas fa-code"></i> 21
                </span>
              </td>
              <td role="cell" data-label="Workspaces">
                <span>
                  <i class="fas fa-cube"></i> 26
                </span>
              </td>
              <td role="cell" data-label="Last commit">2 days ago</td>
              <td role="cell" data-label="Action">
                <a href="#">Action link</a>
              </td>
              <td class="pf-c-table__action" role="cell">
                <div class="pf-c-dropdown pf-m-expanded">
                  <button
                    class="pf-c-dropdown__toggle pf-m-plain"
                    id="page-layout-table-simple-table-dropdown-kebab-5-button"
                    aria-expanded="true"
                    type="button"
                    aria-label="Actions"
                  >
                    <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                  </button>
                  <ul
                    class="pf-c-dropdown__menu pf-m-align-right"
                    aria-labelledby="page-layout-table-simple-table-dropdown-kebab-5-button"
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
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</div>

```

### Top/bottom

```html isFullscreen
<div
  class="pf-l-flex pf-m-column pf-m-nowrap pf-m-space-items-none"
  style="height: 100%;"
>
  <div class="pf-l-flex__item">
    <div class="pf-c-banner pf-m-sticky">
      <div
        class="pf-l-flex pf-m-justify-content-center pf-m-justify-content-space-between-on-lg pf-m-nowrap"
        style="height: 100%;"
      >
        <div class="pf-u-display-none pf-u-display-block-on-lg">Localhost</div>
        <div
          class="pf-u-display-none pf-u-display-block-on-lg"
        >This message is sticky to the top or bottom of the page.</div>
        <div
          class="pf-u-display-none-on-lg"
        >Drop some text on mobile, truncate if needed.</div>
        <div class="pf-u-display-none pf-u-display-block-on-lg">Ned Username</div>
      </div>
    </div>
  </div>
  <div class="pf-l-flex__item pf-m-grow" style="min-height: 0;">
    <div class="pf-c-page" id="page-layout-table-top-bottom">
      <a
        class="pf-c-skip-to-content pf-c-button pf-m-primary"
        href="#main-content-page-layout-table-top-bottom"
      >Skip to content</a>
      <header class="pf-c-page__header">
        <div class="pf-c-page__header-brand">
          <div class="pf-c-page__header-brand-toggle">
            <button
              class="pf-c-button pf-m-plain"
              type="button"
              id="page-layout-table-top-bottom-nav-toggle"
              aria-label="Global navigation"
              aria-expanded="true"
              aria-controls="page-layout-table-top-bottom-primary-nav"
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
                  id="page-layout-table-top-bottom-dropdown-kebab-1-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="page-layout-table-top-bottom-dropdown-kebab-1-button"
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
            </div>
            <div
              class="pf-c-page__header-tools-item pf-m-hidden pf-m-visible-on-md"
            >
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="page-layout-table-top-bottom-dropdown-kebab-2-button"
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
                  aria-labelledby="page-layout-table-top-bottom-dropdown-kebab-2-button"
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
            id="page-layout-table-top-bottom-primary-nav"
            aria-label="Global"
          >
            <ul class="pf-c-nav__list">
              <li
                class="pf-c-nav__item pf-m-expandable pf-m-expanded pf-m-current"
              >
                <button class="pf-c-nav__link" aria-expanded="true">
                  Components
                  <span class="pf-c-nav__toggle">
                    <span class="pf-c-nav__toggle-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </span>
                  </span>
                </button>
                <section
                  class="pf-c-nav__subnav"
                  aria-labelledby="page-layout-table-top-bottom-subnav-title1"
                >
                  <h2
                    class="pf-c-nav__subnav-title pf-screen-reader"
                    id="page-layout-table-top-bottom-subnav-title1"
                  >First nav item</h2>
                  <ul class="pf-c-nav__list">
                    <li class="pf-c-nav__item">
                      <a href="#" class="pf-c-nav__link">Forms</a>
                    </li>
                    <li class="pf-c-nav__item">
                      <a href="#" class="pf-c-nav__link">Table</a>
                    </li>
                    <li class="pf-c-nav__item">
                      <a
                        href="#"
                        class="pf-c-nav__link pf-m-current"
                        aria-current="page"
                      >Data list</a>
                    </li>
                    <li class="pf-c-nav__item">
                      <a href="#" class="pf-c-nav__link">Icons</a>
                    </li>
                    <li class="pf-c-nav__item">
                      <a href="#" class="pf-c-nav__link">Layouts</a>
                    </li>
                    <li class="pf-c-nav__item">
                      <a href="#" class="pf-c-nav__link">List</a>
                    </li>
                  </ul>
                </section>
              </li>
              <li class="pf-c-nav__item pf-m-expandable">
                <button class="pf-c-nav__link" aria-expanded="false">
                  Patterns
                  <span class="pf-c-nav__toggle">
                    <span class="pf-c-nav__toggle-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </span>
                  </span>
                </button>
                <section
                  class="pf-c-nav__subnav"
                  aria-labelledby="page-layout-table-top-bottom-subnav-title2"
                  hidden
                >
                  <h2
                    class="pf-c-nav__subnav-title pf-screen-reader"
                    id="page-layout-table-top-bottom-subnav-title2"
                  >Second nav item</h2>
                  <ul class="pf-c-nav__list"></ul>
                </section>
              </li>
              <li class="pf-c-nav__item pf-m-expandable">
                <button class="pf-c-nav__link" aria-expanded="false">
                  Typography
                  <span class="pf-c-nav__toggle">
                    <span class="pf-c-nav__toggle-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </span>
                  </span>
                </button>
                <section
                  class="pf-c-nav__subnav"
                  aria-labelledby="page-layout-table-top-bottom-subnav-title3"
                  hidden
                >
                  <h2
                    class="pf-c-nav__subnav-title pf-screen-reader"
                    id="page-layout-table-top-bottom-subnav-title3"
                  >Third nav item</h2>
                  <ul class="pf-c-nav__list"></ul>
                </section>
              </li>
              <li class="pf-c-nav__item pf-m-expandable">
                <button class="pf-c-nav__link" aria-expanded="false">
                  Icons
                  <span class="pf-c-nav__toggle">
                    <span class="pf-c-nav__toggle-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </span>
                  </span>
                </button>
                <section
                  class="pf-c-nav__subnav"
                  aria-labelledby="page-layout-table-top-bottom-subnav-title4"
                  hidden
                >
                  <h2
                    class="pf-c-nav__subnav-title pf-screen-reader"
                    id="page-layout-table-top-bottom-subnav-title4"
                  >Second nav item</h2>
                  <ul class="pf-c-nav__list"></ul>
                </section>
              </li>
              <li class="pf-c-nav__item pf-m-expandable">
                <button class="pf-c-nav__link" aria-expanded="false">
                  Colors
                  <span class="pf-c-nav__toggle">
                    <span class="pf-c-nav__toggle-icon">
                      <i class="fas fa-angle-right" aria-hidden="true"></i>
                    </span>
                  </span>
                </button>
                <section
                  class="pf-c-nav__subnav"
                  aria-labelledby="page-layout-table-top-bottom-subnav-title5"
                  hidden
                >
                  <h2
                    class="pf-c-nav__subnav-title pf-screen-reader"
                    id="page-layout-table-top-bottom-subnav-title5"
                  >Second nav item</h2>
                  <ul class="pf-c-nav__list"></ul>
                </section>
              </li>
            </ul>
          </nav>
        </div>
      </div>
      <main
        class="pf-c-page__main"
        tabindex="-1"
        id="main-content-page-layout-table-top-bottom"
      >
        <section class="pf-c-page__main-section pf-m-light">
          <div class="pf-c-content">
            <h1>Table demos</h1>
            <p>Below is an example of a Table.</p>
          </div>
        </section>
        <section
          class="pf-c-page__main-section pf-m-no-padding pf-m-padding-on-xl"
        >
          <div class="pf-c-card">
            <table
              class="pf-c-table pf-m-grid-xl"
              role="grid"
              aria-label="This is a table with checkboxes"
              id="page-layout-table-top-bottom-table"
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
                  <td role="cell"></td>

                  <td role="cell"></td>
                </tr>
              </thead>

              <tbody role="rowgroup">
                <tr role="row">
                  <td class="pf-c-table__check" role="cell">
                    <input
                      type="checkbox"
                      name="checkrow1"
                      aria-labelledby="page-layout-table-top-bottom-table-node1"
                    />
                  </td>
                  <th role="columnheader" data-label="Repository name">
                    <div>
                      <div id="page-layout-table-top-bottom-table-node1">Node 1</div>
                      <a href="#">siemur/test-space</a>
                    </div>
                  </th>
                  <td role="cell" data-label="Branches">
                    <span>
                      <i class="fas fa-code-branch"></i> 10
                    </span>
                  </td>
                  <td role="cell" data-label="Pull requests">
                    <span>
                      <i class="fas fa-code"></i> 25
                    </span>
                  </td>
                  <td role="cell" data-label="Workspaces">
                    <span>
                      <i class="fas fa-cube"></i> 5
                    </span>
                  </td>
                  <td role="cell" data-label="Last commit">2 days ago</td>
                  <td role="cell" data-label="Action">
                    <a href="#">Action link</a>
                  </td>
                  <td class="pf-c-table__action" role="cell">
                    <div class="pf-c-dropdown pf-m-expanded">
                      <button
                        class="pf-c-dropdown__toggle pf-m-plain"
                        id="page-layout-table-top-bottom-table-dropdown-kebab-1-button"
                        aria-expanded="true"
                        type="button"
                        aria-label="Actions"
                      >
                        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                      </button>
                      <ul
                        class="pf-c-dropdown__menu pf-m-align-right"
                        aria-labelledby="page-layout-table-top-bottom-table-dropdown-kebab-1-button"
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
                  </td>
                </tr>

                <tr role="row">
                  <td class="pf-c-table__check" role="cell">
                    <input
                      type="checkbox"
                      name="checkrow2"
                      aria-labelledby="page-layout-table-top-bottom-table-node2"
                    />
                  </td>
                  <th role="columnheader" data-label="Repository name">
                    <div>
                      <div id="page-layout-table-top-bottom-table-node2">Node 2</div>
                      <a href="#">siemur/test-space</a>
                    </div>
                  </th>
                  <td role="cell" data-label="Branches">
                    <span>
                      <i class="fas fa-code-branch"></i> 8
                    </span>
                  </td>
                  <td role="cell" data-label="Pull requests">
                    <span>
                      <i class="fas fa-code"></i> 30
                    </span>
                  </td>
                  <td role="cell" data-label="Workspaces">
                    <span>
                      <i class="fas fa-cube"></i> 2
                    </span>
                  </td>
                  <td role="cell" data-label="Last commit">2 days ago</td>
                  <td role="cell" data-label="Action">
                    <a href="#">Action link</a>
                  </td>
                  <td class="pf-c-table__action" role="cell">
                    <div class="pf-c-dropdown pf-m-expanded">
                      <button
                        class="pf-c-dropdown__toggle pf-m-plain"
                        id="page-layout-table-top-bottom-table-dropdown-kebab-2-button"
                        aria-expanded="true"
                        type="button"
                        aria-label="Actions"
                      >
                        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                      </button>
                      <ul
                        class="pf-c-dropdown__menu pf-m-align-right"
                        aria-labelledby="page-layout-table-top-bottom-table-dropdown-kebab-2-button"
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
                  </td>
                </tr>

                <tr role="row">
                  <td class="pf-c-table__check" role="cell">
                    <input
                      type="checkbox"
                      name="checkrow3"
                      aria-labelledby="page-layout-table-top-bottom-table-node3"
                    />
                  </td>
                  <th role="columnheader" data-label="Repository name">
                    <div>
                      <div id="page-layout-table-top-bottom-table-node3">Node 3</div>
                      <a href="#">siemur/test-space</a>
                    </div>
                  </th>
                  <td role="cell" data-label="Branches">
                    <span>
                      <i class="fas fa-code-branch"></i> 12
                    </span>
                  </td>
                  <td role="cell" data-label="Pull requests">
                    <span>
                      <i class="fas fa-code"></i> 48
                    </span>
                  </td>
                  <td role="cell" data-label="Workspaces">
                    <span>
                      <i class="fas fa-cube"></i> 13
                    </span>
                  </td>
                  <td role="cell" data-label="Last commit">30 days ago</td>
                  <td role="cell" data-label="Action">
                    <a href="#">Action link</a>
                  </td>
                  <td class="pf-c-table__action" role="cell">
                    <div class="pf-c-dropdown pf-m-expanded">
                      <button
                        class="pf-c-dropdown__toggle pf-m-plain"
                        id="page-layout-table-top-bottom-table-dropdown-kebab-3-button"
                        aria-expanded="true"
                        type="button"
                        aria-label="Actions"
                      >
                        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                      </button>
                      <ul
                        class="pf-c-dropdown__menu pf-m-align-right"
                        aria-labelledby="page-layout-table-top-bottom-table-dropdown-kebab-3-button"
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
                  </td>
                </tr>

                <tr role="row">
                  <td class="pf-c-table__check" role="cell">
                    <input
                      type="checkbox"
                      name="checkrow4"
                      aria-labelledby="page-layout-table-top-bottom-table-node4"
                    />
                  </td>
                  <th role="columnheader" data-label="Repository name">
                    <div>
                      <div id="page-layout-table-top-bottom-table-node4">Node 4</div>
                      <a href="#">siemur/test-space</a>
                    </div>
                  </th>
                  <td role="cell" data-label="Branches">
                    <span>
                      <i class="fas fa-code-branch"></i> 3
                    </span>
                  </td>
                  <td role="cell" data-label="Pull requests">
                    <span>
                      <i class="fas fa-code"></i> 8
                    </span>
                  </td>
                  <td role="cell" data-label="Workspaces">
                    <span>
                      <i class="fas fa-cube"></i> 20
                    </span>
                  </td>
                  <td role="cell" data-label="Last commit">8 days ago</td>
                  <td role="cell" data-label="Action">
                    <a href="#">Action link</a>
                  </td>
                  <td class="pf-c-table__action" role="cell">
                    <div class="pf-c-dropdown pf-m-expanded">
                      <button
                        class="pf-c-dropdown__toggle pf-m-plain"
                        id="page-layout-table-top-bottom-table-dropdown-kebab-4-button"
                        aria-expanded="true"
                        type="button"
                        aria-label="Actions"
                      >
                        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                      </button>
                      <ul
                        class="pf-c-dropdown__menu pf-m-align-right"
                        aria-labelledby="page-layout-table-top-bottom-table-dropdown-kebab-4-button"
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
                  </td>
                </tr>

                <tr role="row">
                  <td class="pf-c-table__check" role="cell">
                    <input
                      type="checkbox"
                      name="checkrow5"
                      aria-labelledby="page-layout-table-top-bottom-table-node5"
                    />
                  </td>
                  <th role="columnheader" data-label="Repository name">
                    <div>
                      <div id="page-layout-table-top-bottom-table-node5">Node 5</div>
                      <a href="#">siemur/test-space</a>
                    </div>
                  </th>
                  <td role="cell" data-label="Branches">
                    <span>
                      <i class="fas fa-code-branch"></i> 34
                    </span>
                  </td>
                  <td role="cell" data-label="Pull requests">
                    <span>
                      <i class="fas fa-code"></i> 21
                    </span>
                  </td>
                  <td role="cell" data-label="Workspaces">
                    <span>
                      <i class="fas fa-cube"></i> 26
                    </span>
                  </td>
                  <td role="cell" data-label="Last commit">2 days ago</td>
                  <td role="cell" data-label="Action">
                    <a href="#">Action link</a>
                  </td>
                  <td class="pf-c-table__action" role="cell">
                    <div class="pf-c-dropdown pf-m-expanded">
                      <button
                        class="pf-c-dropdown__toggle pf-m-plain"
                        id="page-layout-table-top-bottom-table-dropdown-kebab-5-button"
                        aria-expanded="true"
                        type="button"
                        aria-label="Actions"
                      >
                        <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                      </button>
                      <ul
                        class="pf-c-dropdown__menu pf-m-align-right"
                        aria-labelledby="page-layout-table-top-bottom-table-dropdown-kebab-5-button"
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
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  </div>
  <div class="pf-l-flex__item">
    <div class="pf-c-banner pf-m-sticky">
      <div
        class="pf-l-flex pf-m-justify-content-center pf-m-justify-content-space-between-on-lg pf-m-nowrap"
        style="height: 100%;"
      >
        <div class="pf-u-display-none pf-u-display-block-on-lg">Localhost</div>
        <div
          class="pf-u-display-none pf-u-display-block-on-lg"
        >This message is sticky to the top or bottom of the page.</div>
        <div
          class="pf-u-display-none-on-lg"
        >Drop some text on mobile, truncate if needed.</div>
        <div class="pf-u-display-none pf-u-display-block-on-lg">Ned Username</div>
      </div>
    </div>
  </div>
</div>

```
