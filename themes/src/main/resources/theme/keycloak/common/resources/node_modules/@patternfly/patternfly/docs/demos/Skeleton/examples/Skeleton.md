---
id: Skeleton
section: components
---## Demos

### Skeleton card

```html isFullscreen
<div class="pf-c-page" id="skeleton-card-view">
  <a
    class="pf-c-skip-to-content pf-c-button pf-m-primary"
    href="#main-content-skeleton-card-view"
  >Skip to content</a>
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">
        <button
          class="pf-c-button pf-m-plain"
          type="button"
          id="skeleton-card-view-nav-toggle"
          aria-label="Global navigation"
          aria-expanded="true"
          aria-controls="skeleton-card-view-primary-nav"
        >
          <i class="fas fa-bars" aria-hidden="true"></i>
        </button>
      </div>
      <a href="#" class="pf-c-page__header-brand-link">
        <img
          class="pf-c-brand"
          src="/assets/images/PF-Masthead-Logo.svg"
          alt="Patternfly Logo"
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
              id="skeleton-card-view-dropdown-kebab-1-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="skeleton-card-view-dropdown-kebab-1-button"
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
              id="skeleton-card-view-dropdown-kebab-2-button"
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
              aria-labelledby="skeleton-card-view-dropdown-kebab-2-button"
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
        id="skeleton-card-view-primary-nav"
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
    id="main-content-skeleton-card-view"
  >
    <section class="pf-c-page__main-section pf-m-light">
      <div class="pf-c-content">
        <h1>Projects</h1>
        <p>This is a demo that showcases PatternFly cards.</p>
      </div>
    </section>
    <section class="pf-c-page__main-section">
      <div class="pf-l-gallery pf-m-gutter">
        <div class="pf-c-card pf-m-hoverable pf-m-compact">
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-66"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-c-skeleton">
              <div class="pf-l-bullseye">
                <div class="pf-c-skeleton pf-m-circle pf-m-width-md"></div>
              </div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-75"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact">
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-66"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-c-skeleton">
              <div class="pf-l-bullseye">
                <div class="pf-c-skeleton pf-m-circle pf-m-width-md"></div>
              </div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-75"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact">
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-66"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-c-skeleton">
              <div class="pf-l-bullseye">
                <div class="pf-c-skeleton pf-m-circle pf-m-width-md"></div>
              </div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-75"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact">
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-66"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-c-skeleton">
              <div class="pf-l-bullseye">
                <div class="pf-c-skeleton pf-m-circle pf-m-width-md"></div>
              </div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-75"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact">
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-66"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-c-skeleton">
              <div class="pf-l-bullseye">
                <div class="pf-c-skeleton pf-m-circle pf-m-width-md"></div>
              </div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-75"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact">
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-66"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-c-skeleton">
              <div class="pf-l-bullseye">
                <div class="pf-c-skeleton pf-m-circle pf-m-width-md"></div>
              </div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-75"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
        </div>
        <div class="pf-c-card pf-m-hoverable pf-m-compact">
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-66"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-c-skeleton">
              <div class="pf-l-bullseye">
                <div class="pf-c-skeleton pf-m-circle pf-m-width-md"></div>
              </div>
            </div>
          </div>
          <div class="pf-c-card__body">
            <div class="pf-l-flex pf-m-column pf-m-spacer-md">
              <div class="pf-c-skeleton"></div>
              <div class="pf-c-skeleton pf-m-width-25"></div>
              <div class="pf-c-skeleton pf-m-width-75"></div>
              <div class="pf-c-skeleton pf-m-width-50"></div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```
