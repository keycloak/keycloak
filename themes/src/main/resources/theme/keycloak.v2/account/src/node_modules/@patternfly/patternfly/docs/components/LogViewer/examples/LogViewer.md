---
id: 'Log viewer'
beta: true
section: extensions
cssPrefix: pf-c-log-viewer
---import './LogViewer.css';

## Examples

### Basic

```html
<div
  class="pf-c-log-viewer"
  style="--pf-c-log-viewer__index--Width: 75px"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-basic-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-basic-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-basic-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-basic-example-select-menu-label log-viewer-basic-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-basic-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-basic-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                    />
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-basic-example-dropdown-button"
                aria-expanded="false"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
                hidden
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-basic-example-desktop-check-wrap-lines"
                              name="log-viewer-basic-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-basic-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-basic-example-desktop-check-show-timestamps"
                              name="log-viewer-basic-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-basic-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-basic-example-desktop-check-line-number"
                              name="log-viewer-basic-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-basic-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-basic-example-settings-dropdown-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-basic-example-settings-dropdown-button"
                  hidden
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-basic-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-basic-example-check-wrap-lines"
                          name="log-viewer-basic-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-basic-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-basic-example-check-show-timestamps"
                          name="log-viewer-basic-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-basic-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-basic-example-check-line-number"
                          name="log-viewer-basic-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-basic-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --openshift-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span
  class="pf-c-log-viewer__text"
>Flag --openshift-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.openshift.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.openshift.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 315px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.openshift.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 357px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 399px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 441px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.openshift.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 483px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 525px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
</div>

```

### With line numbers

```html
<div
  class="pf-c-log-viewer pf-m-line-numbers"
  style="--pf-c-log-viewer__index--Width: 75px"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-line-number-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-line-number-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-line-number-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-line-number-example-select-menu-label log-viewer-line-number-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-line-number-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-line-number-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                    />
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-line-number-example-dropdown-button"
                aria-expanded="false"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
                hidden
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-line-number-example-desktop-check-wrap-lines"
                              name="log-viewer-line-number-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-line-number-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-line-number-example-desktop-check-show-timestamps"
                              name="log-viewer-line-number-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-line-number-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-line-number-example-desktop-check-line-number"
                              name="log-viewer-line-number-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-line-number-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-line-number-example-settings-dropdown-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-line-number-example-settings-dropdown-button"
                  hidden
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-line-number-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-line-number-example-check-wrap-lines"
                          name="log-viewer-line-number-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-line-number-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-line-number-example-check-show-timestamps"
                          name="log-viewer-line-number-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-line-number-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-line-number-example-check-line-number"
                          name="log-viewer-line-number-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-line-number-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --openshift-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span
  class="pf-c-log-viewer__text"
>Flag --openshift-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.openshift.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.openshift.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 315px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.openshift.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 357px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 399px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 441px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.openshift.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 483px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 525px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
</div>

```

### With text wrapping

```html
<div
  class="pf-c-log-viewer pf-m-line-numbers pf-m-wrap-text"
  style="--pf-c-log-viewer__index--Width: 75px"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-text-wrap-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-text-wrap-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-text-wrap-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-text-wrap-example-select-menu-label log-viewer-text-wrap-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-text-wrap-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-text-wrap-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                    />
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-text-wrap-example-dropdown-button"
                aria-expanded="false"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
                hidden
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-text-wrap-example-desktop-check-wrap-lines"
                              name="log-viewer-text-wrap-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-text-wrap-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-text-wrap-example-desktop-check-show-timestamps"
                              name="log-viewer-text-wrap-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-text-wrap-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-text-wrap-example-desktop-check-line-number"
                              name="log-viewer-text-wrap-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-text-wrap-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-text-wrap-example-settings-dropdown-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-text-wrap-example-settings-dropdown-button"
                  hidden
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-text-wrap-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-text-wrap-example-check-wrap-lines"
                          name="log-viewer-text-wrap-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-text-wrap-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-text-wrap-example-check-show-timestamps"
                          name="log-viewer-text-wrap-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-text-wrap-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-text-wrap-example-check-line-number"
                          name="log-viewer-text-wrap-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-text-wrap-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --openshift-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span
  class="pf-c-log-viewer__text"
>Flag --openshift-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.openshift.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.openshift.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 315px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.openshift.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 357px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 399px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 441px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.openshift.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 483px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 525px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
</div>

```

### Without text wrapping

```html
<div
  class="pf-c-log-viewer pf-m-line-numbers pf-m-nowrap"
  style="--pf-c-log-viewer__index--Width: 75px"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-text-nowrap-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-text-nowrap-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-text-nowrap-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-text-nowrap-example-select-menu-label log-viewer-text-nowrap-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-text-nowrap-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-text-nowrap-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                    />
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-text-nowrap-example-dropdown-button"
                aria-expanded="false"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
                hidden
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-text-nowrap-example-desktop-check-wrap-lines"
                              name="log-viewer-text-nowrap-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-text-nowrap-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-text-nowrap-example-desktop-check-show-timestamps"
                              name="log-viewer-text-nowrap-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-text-nowrap-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-text-nowrap-example-desktop-check-line-number"
                              name="log-viewer-text-nowrap-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-text-nowrap-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-text-nowrap-example-settings-dropdown-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-text-nowrap-example-settings-dropdown-button"
                  hidden
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-text-nowrap-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-text-nowrap-example-check-wrap-lines"
                          name="log-viewer-text-nowrap-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-text-nowrap-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-text-nowrap-example-check-show-timestamps"
                          name="log-viewer-text-nowrap-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-text-nowrap-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-text-nowrap-example-check-line-number"
                          name="log-viewer-text-nowrap-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-text-nowrap-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 63px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --openshift-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 126px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span
  class="pf-c-log-viewer__text"
>Flag --openshift-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.openshift.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 168px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.openshift.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 189px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.openshift.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 252px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.openshift.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 294px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
</div>

```

### With search results

```html
<div
  class="pf-c-log-viewer pf-m-line-numbers"
  style="--pf-c-log-viewer__index--Width: 75px;"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-search-results-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-search-results-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-search-results-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-search-results-example-select-menu-label log-viewer-search-results-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-search-results-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-search-results-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                      value="openshift"
                    />
                  </span>
                  <span class="pf-c-search-input__utilities">
                    <span class="pf-c-search-input__count">
                      <span class="pf-c-badge pf-m-read">1 / 10</span>
                    </span>
                    <span class="pf-c-search-input__nav">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        disabled
                        aria-label="Previous"
                      >
                        <i class="fas fa-angle-up fa-fw" aria-hidden="true"></i>
                      </button>
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Next"
                      >
                        <i class="fas fa-angle-down fa-fw" aria-hidden="true"></i>
                      </button>
                    </span>
                    <span class="pf-c-search-input__clear">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Clear"
                      >
                        <i class="fas fa-times fa-fw" aria-hidden="true"></i>
                      </button>
                    </span>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-search-results-example-dropdown-button"
                aria-expanded="false"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
                hidden
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-search-results-example-desktop-check-wrap-lines"
                              name="log-viewer-search-results-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-search-results-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-search-results-example-desktop-check-show-timestamps"
                              name="log-viewer-search-results-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-search-results-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-search-results-example-desktop-check-line-number"
                              name="log-viewer-search-results-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-search-results-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-search-results-example-settings-dropdown-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-search-results-example-settings-dropdown-button"
                  hidden
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-search-results-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-search-results-example-check-wrap-lines"
                          name="log-viewer-search-results-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-search-results-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-search-results-example-check-show-timestamps"
                          name="log-viewer-search-results-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-search-results-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-search-results-example-check-line-number"
                          name="log-viewer-search-results-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-search-results-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                    value="openshift"
                  />
                </span>
                <span class="pf-c-search-input__utilities">
                  <span class="pf-c-search-input__count">
                    <span class="pf-c-badge pf-m-read">1 / 10</span>
                  </span>
                  <span class="pf-c-search-input__nav">
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      disabled
                      aria-label="Previous"
                    >
                      <i class="fas fa-angle-up fa-fw" aria-hidden="true"></i>
                    </button>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-label="Next"
                    >
                      <i class="fas fa-angle-down fa-fw" aria-hidden="true"></i>
                    </button>
                  </span>
                  <span class="pf-c-search-input__clear">
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-label="Clear"
                    >
                      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
                    </button>
                  </span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --<span
  class="pf-c-log-viewer__string pf-m-current"
>openshift</span>-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span class="pf-c-log-viewer__text">Flag --<span class="pf-c-log-viewer__string pf-m-match">openshift</span>-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 315px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 357px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 399px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 441px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 483px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 525px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
</div>

```

### With max height

```html
<div
  class="pf-c-log-viewer pf-m-line-numbers"
  style="--pf-c-log-viewer__index--Width: 75px; --pf-c-log-viewer--MaxHeight: 300px;"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-max-height-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-max-height-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-max-height-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-max-height-example-select-menu-label log-viewer-max-height-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-max-height-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-max-height-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                    />
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-max-height-example-dropdown-button"
                aria-expanded="false"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
                hidden
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-max-height-example-desktop-check-wrap-lines"
                              name="log-viewer-max-height-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-max-height-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-max-height-example-desktop-check-show-timestamps"
                              name="log-viewer-max-height-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-max-height-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-max-height-example-desktop-check-line-number"
                              name="log-viewer-max-height-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-max-height-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-max-height-example-settings-dropdown-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-max-height-example-settings-dropdown-button"
                  hidden
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-max-height-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-max-height-example-check-wrap-lines"
                          name="log-viewer-max-height-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-max-height-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-max-height-example-check-show-timestamps"
                          name="log-viewer-max-height-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-max-height-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-max-height-example-check-line-number"
                          name="log-viewer-max-height-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-max-height-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --openshift-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span
  class="pf-c-log-viewer__text"
>Flag --openshift-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.openshift.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.openshift.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 315px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.openshift.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 357px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 399px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 441px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.openshift.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 483px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 525px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
</div>

```

### With dropdown, drilldown, search expanded

```html
<div
  class="pf-c-log-viewer pf-m-line-numbers"
  style="--pf-c-log-viewer__index--Width: 75px"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-dropdowns-expanded-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-dropdowns-expanded-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-dropdowns-expanded-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-dropdowns-expanded-example-select-menu-label log-viewer-dropdowns-expanded-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-dropdowns-expanded-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-dropdowns-expanded-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                    />
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown pf-m-expanded">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-dropdowns-expanded-example-dropdown-button"
                aria-expanded="true"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-dropdowns-expanded-example-desktop-check-wrap-lines"
                              name="log-viewer-dropdowns-expanded-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-dropdowns-expanded-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-dropdowns-expanded-example-desktop-check-show-timestamps"
                              name="log-viewer-dropdowns-expanded-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-dropdowns-expanded-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-dropdowns-expanded-example-desktop-check-line-number"
                              name="log-viewer-dropdowns-expanded-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-dropdowns-expanded-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
                          <span class="pf-c-menu__item-toggle-icon">
                            <i class="fas fa-angle-right"></i>
                          </span>
                        </span>
                      </button>
                      <div class="pf-c-menu">
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown pf-m-expanded">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-dropdowns-expanded-example-settings-dropdown-button"
                  aria-expanded="true"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-dropdowns-expanded-example-settings-dropdown-button"
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-dropdowns-expanded-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-dropdowns-expanded-example-check-wrap-lines"
                          name="log-viewer-dropdowns-expanded-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-dropdowns-expanded-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-dropdowns-expanded-example-check-show-timestamps"
                          name="log-viewer-dropdowns-expanded-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-dropdowns-expanded-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-dropdowns-expanded-example-check-line-number"
                          name="log-viewer-dropdowns-expanded-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-dropdowns-expanded-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --openshift-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span
  class="pf-c-log-viewer__text"
>Flag --openshift-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.openshift.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.openshift.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 315px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.openshift.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 357px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 399px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 441px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.openshift.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 483px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 525px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
</div>

```

### With popover open

```html
<div
  class="pf-c-log-viewer pf-m-line-numbers"
  style="--pf-c-log-viewer__index--Width: 75px"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-popover-expanded-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-popover-expanded-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-popover-expanded-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-popover-expanded-example-select-menu-label log-viewer-popover-expanded-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-popover-expanded-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-popover-expanded-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                    />
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-popover-expanded-example-dropdown-button"
                aria-expanded="false"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
                hidden
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-popover-expanded-example-desktop-check-wrap-lines"
                              name="log-viewer-popover-expanded-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-popover-expanded-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-popover-expanded-example-desktop-check-show-timestamps"
                              name="log-viewer-popover-expanded-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-popover-expanded-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-popover-expanded-example-desktop-check-line-number"
                              name="log-viewer-popover-expanded-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-popover-expanded-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-popover-expanded-example-settings-dropdown-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-popover-expanded-example-settings-dropdown-button"
                  hidden
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-popover-expanded-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-popover-expanded-example-check-wrap-lines"
                          name="log-viewer-popover-expanded-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-popover-expanded-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-popover-expanded-example-check-show-timestamps"
                          name="log-viewer-popover-expanded-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-popover-expanded-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-popover-expanded-example-check-line-number"
                          name="log-viewer-popover-expanded-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-popover-expanded-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                  />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --openshift-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span
  class="pf-c-log-viewer__text"
>Flag --openshift-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.openshift.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.openshift.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 315px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.openshift.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 357px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 399px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 441px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.openshift.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 483px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.openshift.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 525px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.openshift.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
</div>
<div
  class="pf-c-popover pf-m-bottom"
  aria-modal="true"
  aria-labelledby="popover-bottom-header"
  aria-describedby="popover-bottom-body"
>
  <div class="pf-c-popover__arrow"></div>
  <div class="pf-c-popover__content">
    <button class="pf-c-button pf-m-plain" type="button" aria-label="Close">
      <i class="fas fa-times" aria-hidden="true"></i>
    </button>
    <h1 class="pf-c-title pf-m-md" id="popover-bottom-header">Clear this log?</h1>
    <div
      class="pf-c-popover__body"
      id="popover-bottom-body"
    >Any current log data will be lost.</div>
    <footer class="pf-c-popover__footer">
      <button class="pf-c-button pf-m-link" type="button">Clear</button>
      <button class="pf-c-button pf-m-link" type="button">Cancel</button>
    </footer>
  </div>
</div>

```

### Dark

```html
<div
  class="pf-c-log-viewer pf-m-line-numbers pf-m-dark"
  style="--pf-c-log-viewer__index--Width: 75px"
  tabindex="0"
  aria-label="Log viewer"
>
  <div class="pf-c-log-viewer__header">
    <div
      class="pf-c-toolbar"
      id="log-viewer-line-number-example-toolbar"
      role="toolbar"
    >
      <div class="pf-c-toolbar__content">
        <div class="pf-c-toolbar__content-section pf-m-nowrap">
          <div class="pf-c-toolbar__item pf-m-search-filter">
            <div class="pf-c-select">
              <span
                id="log-viewer-line-number-example-select-menu-label"
                hidden
              >Choose one</span>

              <button
                class="pf-c-select__toggle"
                type="button"
                id="log-viewer-line-number-example-select-menu-toggle"
                aria-haspopup="true"
                aria-expanded="false"
                aria-labelledby="log-viewer-line-number-example-select-menu-label log-viewer-line-number-example-select-menu-toggle"
              >
                <div class="pf-c-select__toggle-wrapper">
                  <span class="pf-c-select__toggle-text">System log</span>
                </div>
                <span class="pf-c-select__toggle-arrow">
                  <i class="fas fa-caret-down" aria-hidden="true"></i>
                </span>
              </button>
              <ul
                class="pf-c-select__menu"
                role="listbox"
                aria-labelledby="log-viewer-line-number-example-select-menu-label"
                hidden
              >
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 1</button>
                </li>
                <li role="presentation">
                  <button
                    class="pf-c-select__menu-item pf-m-selected"
                    role="option"
                    aria-selected="true"
                  >
                    System log
                    <span class="pf-c-select__menu-item-icon">
                      <i class="fas fa-check" aria-hidden="true"></i>
                    </span>
                  </button>
                </li>
                <li role="presentation">
                  <button class="pf-c-select__menu-item" role="option">Dataset 3</button>
                </li>
              </ul>
            </div>
          </div>
          <div class="pf-c-toolbar__group pf-m-toggle-group pf-m-show-on-lg">
            <div class="pf-c-toolbar__toggle">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Show filters"
                aria-expanded="false"
                aria-controls="log-viewer-line-number-example-toolbar-expandable-content"
              >
                <i class="fas fa-search" aria-hidden="true"></i>
              </button>
            </div>
            <div
              class="pf-c-toolbar__item pf-m-search-filter"
              style="--pf-c-toolbar__item--Width: 268px"
            >
              <div class="pf-c-search-input">
                <div class="pf-c-search-input__bar">
                  <span class="pf-c-search-input__text">
                    <span class="pf-c-search-input__icon">
                      <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                    </span>
                    <input
                      class="pf-c-search-input__text-input"
                      type="text"
                      placeholder="Find"
                      aria-label="Find"
                      value="openshift"
                    />
                  </span>
                  <span class="pf-c-search-input__utilities">
                    <span class="pf-c-search-input__count">
                      <span class="pf-c-badge pf-m-read">1 / 10</span>
                    </span>
                    <span class="pf-c-search-input__nav">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        disabled
                        aria-label="Previous"
                      >
                        <i class="fas fa-angle-up fa-fw" aria-hidden="true"></i>
                      </button>
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Next"
                      >
                        <i class="fas fa-angle-down fa-fw" aria-hidden="true"></i>
                      </button>
                    </span>
                    <span class="pf-c-search-input__clear">
                      <button
                        class="pf-c-button pf-m-plain"
                        type="button"
                        aria-label="Clear"
                      >
                        <i class="fas fa-times fa-fw" aria-hidden="true"></i>
                      </button>
                    </span>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="pf-c-toolbar__item pf-m-spacer-none">
            <button class="pf-c-button pf-m-link" type="button">
              <span class="pf-c-button__icon pf-m-start">
                <i class="fas fa-pause" aria-hidden="true"></i>
              </span>
              Pause
            </button>
          </div>
          <div
            class="pf-c-toolbar__item pf-m-align-right pf-m-hidden-on-lg pf-m-spacer-none"
          >
            <div class="pf-c-dropdown">
              <button
                class="pf-c-dropdown__toggle pf-m-plain"
                id="log-viewer-line-number-example-dropdown-button"
                aria-expanded="false"
                type="button"
                aria-label="Actions"
              >
                <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
              </button>
              <div
                class="pf-c-menu pf-m-drilldown pf-m-align-right"
                style="--pf-c-menu--Width: 200px;"
                hidden
              >
                <div class="pf-c-menu__content">
                  <ul class="pf-c-menu__list" role="menu">
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Clear log</span>
                        </span>
                      </button>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-line-number-example-desktop-check-wrap-lines"
                              name="log-viewer-line-number-example-desktop-check-wrap-lines"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-line-number-example-desktop-check-wrap-lines"
                            >Wrap lines</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-line-number-example-desktop-check-show-timestamps"
                              name="log-viewer-line-number-example-desktop-check-show-timestamps"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-line-number-example-desktop-check-show-timestamps"
                            >Show timestamps</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <li class="pf-c-menu__list-item" role="none">
                      <div
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                      >
                        <span class="pf-c-menu__item-main">
                          <div class="pf-c-check">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="log-viewer-line-number-example-desktop-check-line-number"
                              name="log-viewer-line-number-example-desktop-check-line-number"
                            />

                            <label
                              class="pf-c-check__label"
                              for="log-viewer-line-number-example-desktop-check-line-number"
                            >Display line number</label>
                          </div>
                        </span>
                      </div>
                    </li>
                    <hr class="pf-c-divider" />
                    <li class="pf-c-menu__list-item" role="none">
                      <button
                        class="pf-c-menu__item"
                        type="button"
                        role="menuitem"
                        aria-expanded="false"
                      >
                        <span class="pf-c-menu__item-main">
                          <span class="pf-c-menu__item-text">Launch</span>
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
                                tabindex="0"
                              >
                                <span class="pf-c-menu__item-main">
                                  <span class="pf-c-menu__item-toggle-icon">
                                    <i class="fas fa-angle-left"></i>
                                  </span>
                                  <span class="pf-c-menu__item-text">Launch</span>
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
                                  <span
                                    class="pf-c-menu__item-text"
                                  >Launch option 1</span>
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
                                  >Launch option 2</span>
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
                                  >Launch option 3</span>
                                </span>
                              </button>
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
                          <span class="pf-c-menu__item-text">Download</span>
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
                          <span class="pf-c-menu__item-text">Full screen</span>
                        </span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <div
            class="pf-c-toolbar__group pf-m-icon-button-group pf-m-align-right pf-m-hidden pf-m-visible-on-lg"
          >
            <div class="pf-c-toolbar__item">
              <button class="pf-c-button pf-m-link" type="button">Clear log</button>
            </div>
            <div class="pf-c-toolbar__item">
              <div class="pf-c-dropdown">
                <button
                  class="pf-c-dropdown__toggle pf-m-plain"
                  id="log-viewer-line-number-example-settings-dropdown-button"
                  aria-expanded="false"
                  type="button"
                  aria-label="Settings"
                >
                  <i class="fas fa-cog" aria-hidden="true"></i>
                </button>
                <ul
                  class="pf-c-dropdown__menu pf-m-align-right"
                  aria-labelledby="log-viewer-line-number-example-settings-dropdown-button"
                  hidden
                >
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-line-number-example-check-wrap-lines"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-line-number-example-check-wrap-lines"
                          name="log-viewer-line-number-example-check-wrap-lines"
                        />

                        <span class="pf-c-check__label">Wrap lines</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-line-number-example-check-show-timestamps"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-line-number-example-check-show-timestamps"
                          name="log-viewer-line-number-example-check-show-timestamps"
                        />

                        <span class="pf-c-check__label">Show timestamps</span>
                      </label>
                    </div>
                  </li>
                  <li>
                    <div class="pf-c-dropdown__menu-item">
                      <label
                        class="pf-c-check"
                        for="log-viewer-line-number-example-check-line-number"
                      >
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="log-viewer-line-number-example-check-line-number"
                          name="log-viewer-line-number-example-check-line-number"
                        />

                        <span class="pf-c-check__label">Display line number</span>
                      </label>
                    </div>
                  </li>
                </ul>
              </div>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Open external link"
              >
                <i class="fas fa-external-link-alt" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Download"
              >
                <i class="fas fa-download" aria-hidden="true"></i>
              </button>
            </div>
            <div class="pf-c-toolbar__item">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Full screen"
              >
                <i class="fas fa-expand" aria-hidden="true"></i>
              </button>
            </div>
          </div>
        </div>
        <div
          class="pf-c-toolbar__expandable-content pf-m-hidden"
          id="log-viewer-line-number-example-toolbar-expandable-content"
          hidden
        >
          <div
            class="pf-c-toolbar__item pf-m-search-filter"
            style="--pf-c-toolbar__item--Width: 24ch"
          >
            <div class="pf-c-search-input">
              <div class="pf-c-search-input__bar">
                <span class="pf-c-search-input__text">
                  <span class="pf-c-search-input__icon">
                    <i class="fas fa-search fa-fw" aria-hidden="true"></i>
                  </span>
                  <input
                    class="pf-c-search-input__text-input"
                    type="text"
                    placeholder="Find"
                    aria-label="Find"
                    value="openshift"
                  />
                </span>
                <span class="pf-c-search-input__utilities">
                  <span class="pf-c-search-input__count">
                    <span class="pf-c-badge pf-m-read">1 / 10</span>
                  </span>
                  <span class="pf-c-search-input__nav">
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      disabled
                      aria-label="Previous"
                    >
                      <i class="fas fa-angle-up fa-fw" aria-hidden="true"></i>
                    </button>
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-label="Next"
                    >
                      <i class="fas fa-angle-down fa-fw" aria-hidden="true"></i>
                    </button>
                  </span>
                  <span class="pf-c-search-input__clear">
                    <button
                      class="pf-c-button pf-m-plain"
                      type="button"
                      aria-label="Clear"
                    >
                      <i class="fas fa-times fa-fw" aria-hidden="true"></i>
                    </button>
                  </span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div class="pf-c-log-viewer__main" role="log">
    <div class="pf-c-log-viewer__scroll-container" tabindex="0">
      <!--prettyhtml-ignore-start-->
      <div class="pf-c-log-viewer__list" style="--pf-c-log-viewer__list--Height: 301080px;">
              <div class="pf-c-log-viewer__list-item" style="top: 0px;">
                <span class="pf-c-log-viewer__index">1</span>
                <span class="pf-c-log-viewer__text">Copying system trust bundle</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 21px;">
                <span class="pf-c-log-viewer__index">2</span>
                <span
  class="pf-c-log-viewer__text"
>Waiting for port :6443 to be released.</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 42px;">
                <span class="pf-c-log-viewer__index">3</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.084507 1 loader.go:379] Config loaded from file: /etc/kubernetes/static-pod-resources/configmaps/kube-apiserver-cert-syncer-kubeconfig/kubeconfig</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 84px;">
                <span class="pf-c-log-viewer__index">4</span>
                <span
  class="pf-c-log-viewer__text"
>Copying termination logs to "/var/log/kube-apiserver/termination.log"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 105px;">
                <span class="pf-c-log-viewer__index">5</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.087543 1 main.go:124] Touching termination lock file "/var/log/kube-apiserver/.terminating"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 147px;">
                <span class="pf-c-log-viewer__index">6</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.088797 1 main.go:182] Launching sub-process "/usr/bin/hyperkube kube-apiserver --<span
  class="pf-c-log-viewer__string pf-m-current"
>openshift</span>-config=/etc/kubernetes/static-pod-resources/configmaps/config/config.yaml --advertise-address=10.0.171.12 -v=2"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 210px;">
                <span class="pf-c-log-viewer__index">7</span>
                <span class="pf-c-log-viewer__text">Flag --<span class="pf-c-log-viewer__string pf-m-match">openshift</span>-config has been deprecated, to be removed</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 231px;">
                <span class="pf-c-log-viewer__index">8</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238681 17 plugins.go:84] Registered admission plugin "authorization.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/RestrictSubjectBindings"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 273px;">
                <span class="pf-c-log-viewer__index">9</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238763 17 plugins.go:84] Registered admission plugin "image.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/ImagePolicy"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 315px;">
                <span class="pf-c-log-viewer__index">10</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238775 17 plugins.go:84] Registered admission plugin "route.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/IngressAdmission"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 357px;">
                <span class="pf-c-log-viewer__index">11</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238783 17 plugins.go:84] Registered admission plugin "scheduling.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/OriginPodNodeEnvironment"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 399px;">
                <span class="pf-c-log-viewer__index">12</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238792 17 plugins.go:84] Registered admission plugin "autoscaling.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/ClusterResourceOverride"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 441px;">
                <span class="pf-c-log-viewer__index">13</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238801 17 plugins.go:84] Registered admission plugin "quota.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/ClusterResourceQuota"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 483px;">
                <span class="pf-c-log-viewer__index">14</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238810 17 plugins.go:84] Registered admission plugin "autoscaling.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/RunOnceDuration"</span>
              </div>
              <div class="pf-c-log-viewer__list-item" style="top: 525px;">
                <span class="pf-c-log-viewer__index">15</span>
                <span
  class="pf-c-log-viewer__text"
>I0223 20:04:25.238819 17 plugins.go:84] Registered admission plugin "scheduling.<span
  class="pf-c-log-viewer__string pf-m-match"
>openshift</span>.io/PodNodeConstraints"</span>
              </div>
            </div>
      <!--prettyhtml-ignore-end-->
    </div>
  </div>
  <button class="pf-c-button pf-m-primary" type="button">Jump to the bottom</button>
</div>

```

## Documentation

### Accessibility

| Attribute                 | Applied                                      | Outcome                                                                                                                                               |
| ------------------------- | -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| `aria-label="Log viewer"` | `.pf-c-log-viewer`                           | Provides an accessible label for the log viewer.                                                                                                      |
| `role="log"`              | `.pf-c-log-viewer__main`                     | Identifies an element that creates a live region where new information is added in a meaningful order and old information may disappear. **Required** |
| `aria-live="polite"`      | `.pf-c-log-viewer__list`                     | Allows assistive technologies to automatically read new content within the aria-live region on the place where the text is displayed.                 |
| `aria-atomic="true"`      | `.pf-c-log-viewer__list`                     | Allows assistive technologies to notify a user when log messages are added.                                                                           |
| `tabindex="0"`            | `.pf-c-log-viewer`, `.pf-c-log-viewer__list` | Inserts the element into the tab order of the page so that it is focusable. **Required**                                                              |
| `aria-hidden="true"`      | `.pf-c-log-viewer__index`                    | Hides an index from assistive technologies.                                                                                                           |

### Usage

| Class                                                      | Applied to                 | Outcome                                                                                                                                                |
| ---------------------------------------------------------- | -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `.pf-c-log-viewer`                                         | `<div>`                    | Initiates a log viewer. **Required**                                                                                                                   |
| `.pf-c-log-viewer__header`                                 | `<div>`                    | Initiates the header container for a log viewer. **Required**                                                                                          |
| `.pf-c-log-viewer__main`                                   | `<div>`                    | Initiates the main container for a log viewer. **Required**                                                                                            |
| `.pf-c-log-viewer__scroll-container`                       | `<div>`                    | Initiates the scroll container for a log viewer. **Required**                                                                                          |
| `.pf-c-log-viewer__list`                                   | `<ul>`                     | Initiates the log viewer list. **Required**                                                                                                            |
| `.pf-c-log-viewer__list-item`                              | `<li>`                     | Initiates a log viewer list item. **Required**                                                                                                         |
| `.pf-c-log-viewer__index`                                  | `<span>`                   | Initiates a log viewer index element. **Required for numbered list**                                                                                   |
| `.pf-c-log-viewer__text`                                   | `<div>`                    | Initiates a log viewer text element. **Required**                                                                                                      |
| `.pf-c-log-viewer__string`                                 | `<span>`                   | Initiates a log viewer string element.                                                                                                                 |
| `.pf-c-log-viewer__timestamp`                              | `<div>`                    | Initiates a log viewer text element. **Required**                                                                                                      |
| `.pf-m-wrap-text`                                          | `.pf-c-log-viewer`         | Modifies the log viewer text to wrap.                                                                                                                  |
| `.pf-m-nowrap`                                             | `.pf-c-log-viewer`         | Modifies the log viewer text to not wrap.                                                                                                              |
| `.pf-m-line-numbers`                                       | `.pf-c-log-viewer`         | Modifies the log viewer to display line numbers.                                                                                                       |
| `.pf-m-dark`                                               | `.pf-c-log-viewer`         | Modifies the log viewer content for dark theme.                                                                                                        |
| `.pf-m-match`                                              | `.pf-c-log-viewer__string` | Indicates a string is a search result.                                                                                                                 |
| `.pf-m-current`                                            | `.pf-c-log-viewer__string` | Indicates a string is the current search result.                                                                                                       |
| `--pf-c-log-viewer--MaxHeight{-on-[breakpoint]}: {height}` | `.pf-c-log-viewer`         | Modifies the height value of a log viewer at optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). |
