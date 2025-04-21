---
id: Notification drawer
section: components
beta: true
cssPrefix: pf-c-notification-drawer
---## Examples

### Basic

```html
<div class="pf-c-notification-drawer">
  <div class="pf-c-notification-drawer__header">
    <h1 class="pf-c-notification-drawer__header-title">Notifications</h1>
    <span class="pf-c-notification-drawer__header-status">3 unread</span>
    <div class="pf-c-notification-drawer__header-action">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="notification-drawer-basic-header-action-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="notification-drawer-basic-header-action-button"
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
      <div class="pf-c-notification-drawer__header-action-close">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
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
          <span class="pf-c-notification-drawer__list-item-header-icon">
            <i class="fas fa-info-circle" aria-hidden="true"></i>
          </span>
          <h2 class="pf-c-notification-drawer__list-item-header-title">
            <span class="pf-screen-reader">Info notification:</span>
            Unread
            info notification title
          </h2>
        </div>
        <div class="pf-c-notification-drawer__list-item-action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="notification-drawer-basicdropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="notification-drawer-basicdropdown-kebab-1-button"
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
          class="pf-c-notification-drawer__list-item-description"
        >This is an info notification description.</div>
        <div class="pf-c-notification-drawer__list-item-timestamp">5 minutes ago</div>
      </li>

      <li
        class="pf-c-notification-drawer__list-item pf-m-hoverable pf-m-default"
        tabindex="0"
      >
        <div class="pf-c-notification-drawer__list-item-header">
          <span class="pf-c-notification-drawer__list-item-header-icon">
            <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
          </span>
          <h2 class="pf-c-notification-drawer__list-item-header-title">
            <span class="pf-screen-reader">Default notification:</span>
            Unread
            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
          </h2>
        </div>
        <div class="pf-c-notification-drawer__list-item-action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="notification-drawer-basicdropdown-kebab-2-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="notification-drawer-basicdropdown-kebab-2-button"
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
          <span class="pf-c-notification-drawer__list-item-header-icon">
            <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
          </span>
          <h2 class="pf-c-notification-drawer__list-item-header-title">
            <span class="pf-screen-reader">Default notification:</span>
            Unread
            recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
          </h2>
        </div>
        <div class="pf-c-notification-drawer__list-item-action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="notification-drawer-basicdropdown-kebab-3-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="notification-drawer-basicdropdown-kebab-3-button"
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
          <span class="pf-c-notification-drawer__list-item-header-icon">
            <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
          </span>
          <h2 class="pf-c-notification-drawer__list-item-header-title">
            <span class="pf-screen-reader">Warning notification:</span>
            Read warning notification title
          </h2>
        </div>
        <div class="pf-c-notification-drawer__list-item-action">
          <div class="pf-c-dropdown pf-m-top">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="notification-drawer-basicdropdown-kebab-4-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="notification-drawer-basicdropdown-kebab-4-button"
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
          <span class="pf-c-notification-drawer__list-item-header-icon">
            <i class="fas fa-check-circle" aria-hidden="true"></i>
          </span>
          <h2 class="pf-c-notification-drawer__list-item-header-title">
            <span class="pf-screen-reader">Success notification:</span>
            Read success notification title
          </h2>
        </div>
        <div class="pf-c-notification-drawer__list-item-action">
          <div class="pf-c-dropdown pf-m-top">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="notification-drawer-basicdropdown-kebab-5-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="notification-drawer-basicdropdown-kebab-5-button"
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
          <span class="pf-c-notification-drawer__list-item-header-icon">
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
              id="notification-drawer-basicdropdown-kebab-6-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="notification-drawer-basicdropdown-kebab-6-button"
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
          <span class="pf-c-notification-drawer__list-item-header-icon">
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
              id="notification-drawer-basicdropdown-kebab-7-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="notification-drawer-basicdropdown-kebab-7-button"
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
          class="pf-c-notification-drawer__list-item-description"
        >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
        <div
          class="pf-c-notification-drawer__list-item-timestamp"
        >50 minutes ago</div>
      </li>
    </ul>
  </div>
</div>

```

### Groups

```html
<div class="pf-c-notification-drawer">
  <div class="pf-c-notification-drawer__header">
    <h1 class="pf-c-notification-drawer__header-title">Notifications</h1>
    <span class="pf-c-notification-drawer__header-status">9 unread</span>
    <div class="pf-c-notification-drawer__header-action">
      <div class="pf-c-dropdown">
        <button
          class="pf-c-dropdown__toggle pf-m-plain"
          id="notification-drawer-groups-header-action-button"
          aria-expanded="false"
          type="button"
          aria-label="Actions"
        >
          <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
        </button>
        <ul
          class="pf-c-dropdown__menu pf-m-align-right"
          aria-labelledby="notification-drawer-groups-header-action-button"
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
      <div class="pf-c-notification-drawer__header-action-close">
        <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
          <i class="fas fa-times" aria-hidden="true"></i>
        </button>
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
            <div class="pf-c-notification-drawer__group-toggle-count">
              <span class="pf-c-badge pf-m-unread">2</span>
            </div>
            <span class="pf-c-notification-drawer__group-toggle-icon">
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-info-circle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Info notification:</span>
                Unread
                info notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group1dropdown-kebab-1-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group1dropdown-kebab-1-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Default notification:</span>
                Unread
                recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group1dropdown-kebab-2-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group1dropdown-kebab-2-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Default notification:</span>
                Unread
                recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group1dropdown-kebab-3-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group1dropdown-kebab-3-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Warning notification:</span>
                Read warning notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown pf-m-top">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group1dropdown-kebab-4-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group1dropdown-kebab-4-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-check-circle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Success notification:</span>
                Read success notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown pf-m-top">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group1dropdown-kebab-5-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group1dropdown-kebab-5-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
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
                  id="notification-drawer-groups-group1dropdown-kebab-6-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group1dropdown-kebab-6-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
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
                  id="notification-drawer-groups-group1dropdown-kebab-7-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group1dropdown-kebab-7-button"
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
              class="pf-c-notification-drawer__list-item-description"
            >This example uses ".pf-m-truncate" and sets "--pf-c-notification-drawer__list-item-header-title--max-lines: 2" to limit title to two lines and truncate any overflow text with ellipses.</div>
            <div
              class="pf-c-notification-drawer__list-item-timestamp"
            >50 minutes ago</div>
          </li>
        </ul>
      </section>
      <section class="pf-c-notification-drawer__group pf-m-expanded">
        <h1>
          <button
            class="pf-c-notification-drawer__group-toggle"
            aria-expanded="true"
          >
            <div
              class="pf-c-notification-drawer__group-toggle-title"
            >Second notification group</div>
            <div class="pf-c-notification-drawer__group-toggle-count">
              <span class="pf-c-badge pf-m-unread">2</span>
            </div>
            <span class="pf-c-notification-drawer__group-toggle-icon">
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-info-circle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Info notification:</span>
                Unread
                info notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group2dropdown-kebab-1-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group2dropdown-kebab-1-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Default notification:</span>
                Unread
                recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group2dropdown-kebab-2-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group2dropdown-kebab-2-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Default notification:</span>
                Unread
                recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group2dropdown-kebab-3-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group2dropdown-kebab-3-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Warning notification:</span>
                Read warning notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown pf-m-top">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group2dropdown-kebab-4-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group2dropdown-kebab-4-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-check-circle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Success notification:</span>
                Read success notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown pf-m-top">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group2dropdown-kebab-5-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group2dropdown-kebab-5-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
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
                  id="notification-drawer-groups-group2dropdown-kebab-6-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group2dropdown-kebab-6-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
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
                  id="notification-drawer-groups-group2dropdown-kebab-7-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group2dropdown-kebab-7-button"
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
            <div class="pf-c-notification-drawer__group-toggle-count">
              <span class="pf-c-badge pf-m-unread">3</span>
            </div>
            <span class="pf-c-notification-drawer__group-toggle-icon">
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-info-circle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Info notification:</span>
                Unread
                info notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group3dropdown-kebab-1-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group3dropdown-kebab-1-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Default notification:</span>
                Unread
                recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group3dropdown-kebab-2-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group3dropdown-kebab-2-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Default notification:</span>
                Unread
                recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group3dropdown-kebab-3-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group3dropdown-kebab-3-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Warning notification:</span>
                Read warning notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown pf-m-top">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group3dropdown-kebab-4-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group3dropdown-kebab-4-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-check-circle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Success notification:</span>
                Read success notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown pf-m-top">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group3dropdown-kebab-5-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group3dropdown-kebab-5-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
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
                  id="notification-drawer-groups-group3dropdown-kebab-6-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group3dropdown-kebab-6-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
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
                  id="notification-drawer-groups-group3dropdown-kebab-7-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group3dropdown-kebab-7-button"
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
            <div class="pf-c-notification-drawer__group-toggle-count">
              <span class="pf-c-badge pf-m-unread">2</span>
            </div>
            <span class="pf-c-notification-drawer__group-toggle-icon">
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-info-circle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Info notification:</span>
                Unread
                info notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group4dropdown-kebab-1-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group4dropdown-kebab-1-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Default notification:</span>
                Unread
                recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group4dropdown-kebab-2-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group4dropdown-kebab-2-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-arrow-circle-up" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Default notification:</span>
                Unread
                recommendation notification title. This is a long title to show how the title will wrap if it is long and wraps to multiple lines.
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group4dropdown-kebab-3-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group4dropdown-kebab-3-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-exclamation-triangle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Warning notification:</span>
                Read warning notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown pf-m-top">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group4dropdown-kebab-4-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group4dropdown-kebab-4-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
                <i class="fas fa-check-circle" aria-hidden="true"></i>
              </span>
              <h2 class="pf-c-notification-drawer__list-item-header-title">
                <span class="pf-screen-reader">Success notification:</span>
                Read success notification title
              </h2>
            </div>
            <div class="pf-c-notification-drawer__list-item-action">
              <div class="pf-c-dropdown pf-m-top">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="notification-drawer-groups-group4dropdown-kebab-5-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group4dropdown-kebab-5-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
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
                  id="notification-drawer-groups-group4dropdown-kebab-6-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group4dropdown-kebab-6-button"
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
              <span class="pf-c-notification-drawer__list-item-header-icon">
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
                  id="notification-drawer-groups-group4dropdown-kebab-7-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Actions"
                >
                  <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="notification-drawer-groups-group4dropdown-kebab-7-button"
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

```

### Accessibility

| Attribute               | Applied to                                                                                                 | Outcome                                                                                                                               |
| ----------------------- | ---------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-expanded="false"` | `.pf-c-notification-drawer__group-toggle`                                                                  | Indicates that the group notification list is hidden.                                                                                 |
| `aria-expanded="true"`  | `.pf-c-notification-drawer__group-toggle`                                                                  | Indicates that the group notification list is visible.                                                                                |
| `hidden`                | `.pf-c-notification-drawer__list`                                                                          | Indicates that the group notification list is hidden so that it isn't visible in the UI and isn't accessed by assistive technologies. |
| `tabindex="0"`          | `.pf-c-notification-drawer__list-item.pf-m-hoverable`                                                      | Inserts the hoverable list item into the tab order of the page so that it is focusable.                                               |
| `aria-hidden="true"`    | `.pf-c-notification-drawer__group-toggle-icon > *`, `.pf-c-notification-drawer__list-item-header-icon > *` | Hides icon for assistive technologies.                                                                                                |

### Usage

| Class                                               | Applied to                                          | Outcome                                                                                                                                                                                                                                                        |
| --------------------------------------------------- | --------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-notification-drawer`                         | `<div>`                                             | Initiates the notification drawer. **Required**                                                                                                                                                                                                                |
| `.pf-c-notification-drawer__header`                 | `<div>`                                             | Initiates the notification drawer header. **Required**                                                                                                                                                                                                         |
| `.pf-c-notification-drawer__header-title`           | `<h1>`                                              | Initiates the notification drawer header title. **Required**                                                                                                                                                                                                   |
| `.pf-c-notification-drawer__header-status`          | `<span>`                                            | Initiates the notification drawer header status.                                                                                                                                                                                                               |
| `.pf-c-notification-drawer__header-action`          | `<div>`                                             | Initiates the notification drawer header action.                                                                                                                                                                                                               |
| `.pf-c-notification-drawer__header-action-close`    | `<div>`                                             | Initiates the notification drawer header action button.                                                                                                                                                                                                        |
| `.pf-c-notification-drawer__body`                   | `<div>`                                             | Initiates the notification drawer body. **Required**                                                                                                                                                                                                           |
| `.pf-c-notification-drawer__list`                   | `<ul>`                                              | Initiates a notification list. **Required**                                                                                                                                                                                                                    |
| `.pf-c-notification-drawer__list-item`              | `<li>`                                              | Initiates a notification list item. **Always use with a state modifier - one of `.pf-m-info`, `.pf-m-warning`, `.pf-m-danger`, `.pf-m-success`.** **Required**                                                                                                 |
| `.pf-c-notification-drawer__list-item-header`       | `<div>`                                             | Initiates a notification list item header. **Required**                                                                                                                                                                                                        |
| `.pf-c-notification-drawer__list-item-header-icon`  | `<span>`                                            | Initiates a notification list item header icon. **Required**                                                                                                                                                                                                   |
| `.pf-c-notification-drawer__list-item-header-title` | `<h2>`                                              | Initiates a notification list item header title. **Required**                                                                                                                                                                                                  |
| `.pf-c-notification-drawer__list-item-action`       | `<div>`                                             | Initiates a notification list item action.                                                                                                                                                                                                                     |
| `.pf-c-notification-drawer__list-item-description`  | `<div>`                                             | Initiates a notification list item description. **Required**                                                                                                                                                                                                   |
| `.pf-c-notification-drawer__list-item-timestamp`    | `<div>`                                             | Initiates a notification list item timestamp. **Required**                                                                                                                                                                                                     |
| `.pf-c-notification-drawer__group-list`             | `<div>`                                             | Initiates a notification group list. **Required when notifications are grouped**                                                                                                                                                                               |
| `.pf-c-notification-drawer__group`                  | `<section>`                                         | Initiates a notification group. **Required**                                                                                                                                                                                                                   |
| `.pf-c-notification-drawer__group-toggle`           | `<button>`                                          | Initiates a notification group toggle. **Required**                                                                                                                                                                                                            |
| `.pf-c-notification-drawer__group-title`            | `<div>`                                             | Initiates a notification group toggle title. **Required**                                                                                                                                                                                                      |
| `.pf-c-notification-drawer__group-count`            | `<div>`                                             | Initiates a notification group toggle count.                                                                                                                                                                                                                   |
| `.pf-c-notification-drawer__group-icon`             | `<span>`                                            | Initiates a notification group toggle icon. **Required**                                                                                                                                                                                                       |
| `.pf-m-default`                                     | `.pf-c-notification-drawer__list-item`              | Modifies a notification list item for the default state.                                                                                                                                                                                                       |
| `.pf-m-info`                                        | `.pf-c-notification-drawer__list-item`              | Modifies a notification list item for the info state.                                                                                                                                                                                                          |
| `.pf-m-warning`                                     | `.pf-c-notification-drawer__list-item`              | Modifies a notification list item for the warning state.                                                                                                                                                                                                       |
| `.pf-m-danger`                                      | `.pf-c-notification-drawer__list-item`              | Modifies a notification list item for the danger state.                                                                                                                                                                                                        |
| `.pf-m-success`                                     | `.pf-c-notification-drawer__list-item`              | Modifies a notification list item for the success state.                                                                                                                                                                                                       |
| `.pf-m-read`                                        | `.pf-c-notification-drawer__list-item`              | Modifies a notification list item for the read state.                                                                                                                                                                                                          |
| `.pf-m-hoverable`                                   | `.pf-c-notification-drawer__list-item`              | Modifies a notification list item hover states to inidicate it is clickable.                                                                                                                                                                                   |
| `.pf-m-expanded`                                    | `.pf-c-notification-drawer__group`                  | Modifies a notification group for the expanded state.                                                                                                                                                                                                          |
| `.pf-m-truncate`                                    | `.pf-c-notification-drawer__list-item-header-title` | Modifies the title to display a single line and truncate any overflow text with ellipses. **Note:** you can specify the max number of lines to show by setting the `--pf-c-notification-drawer__list-item-header-title--max-lines` (the default value is `1`). |
