---
id: Tree view
section: components
cssPrefix: pf-c-tree-view
beta: true
---## Examples

### Default

```html
<div class="pf-c-tree-view">
  <ul class="pf-c-tree-view__list" role="tree">
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Application launcher</span>
          </div>
        </button>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 1</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node pf-m-current">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Current</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 2</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  aria-expanded="false"
                  tabindex="0"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-toggle">
                          <span class="pf-c-tree-view__node-toggle-icon">
                            <i class="fas fa-angle-right" aria-hidden="true"></i>
                          </span>
                        </div>
                        <span class="pf-c-tree-view__node-text">Loader app 1</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 2</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 3</span>
                      </div>
                    </button>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Cost management</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Sources</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span
              class="pf-c-tree-view__node-text"
            >This is a really really really long folder name that overflows from the width of the container.</span>
          </div>
        </button>
      </div>
    </li>
  </ul>
</div>

```

### With search

```html
<div class="pf-c-tree-view">
  <div class="pf-c-tree-view__search">
    <div class="pf-c-search-input">
      <div class="pf-c-search-input__bar">
        <span class="pf-c-search-input__text">
          <span class="pf-c-search-input__icon">
            <i class="fas fa-search fa-fw" aria-hidden="true"></i>
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
  <hr class="pf-c-divider" />
  <ul class="pf-c-tree-view__list" role="tree">
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Application launcher</span>
          </div>
        </button>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 1</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node pf-m-current">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Current</span>
                  </div>
                </button>
              </div>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 2</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  aria-expanded="false"
                  tabindex="0"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-toggle">
                          <span class="pf-c-tree-view__node-toggle-icon">
                            <i class="fas fa-angle-right" aria-hidden="true"></i>
                          </span>
                        </div>
                        <span class="pf-c-tree-view__node-text">Loader app 1</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 2</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 3</span>
                      </div>
                    </button>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Cost management</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Sources</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span
              class="pf-c-tree-view__node-text"
            >This is a really really really long folder name that overflows from the width of the container.</span>
          </div>
        </button>
      </div>
    </li>
  </ul>
</div>

```

### With checkboxes

```html
<div class="pf-c-tree-view">
  <ul class="pf-c-tree-view__list" role="tree">
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <button
              class="pf-c-tree-view__node-toggle"
              aria-label="Toggle"
              aria-expanded="true"
            >
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </button>
            <span class="pf-c-tree-view__node-check">
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="check-1"
                  aria-label="Tree view node check"
                />
              </div>
            </span>
            <label
              class="pf-c-tree-view__node-text"
              for="check-1"
            >Application launcher</label>
          </div>
        </div>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <button
                  class="pf-c-tree-view__node-toggle"
                  aria-label="Toggle"
                  aria-expanded="true"
                >
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </button>
                <span class="pf-c-tree-view__node-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-2"
                      checked
                      aria-label="Tree view node check checked"
                    />
                  </div>
                </span>
                <label
                  class="pf-c-tree-view__node-text"
                  for="check-2"
                >Application 1</label>
              </div>
            </div>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <div class="pf-c-tree-view__node" tabindex="0">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-3"
                          checked
                          aria-label="Tree view node check checked"
                        />
                      </div>
                    </span>
                    <label
                      class="pf-c-tree-view__node-text"
                      for="check-3"
                    >Settings</label>
                  </div>
                </div>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <div class="pf-c-tree-view__node" tabindex="0">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-4"
                          checked
                          aria-label="Tree view node check checked"
                        />
                      </div>
                    </span>
                    <label
                      class="pf-c-tree-view__node-text"
                      for="check-4"
                    >Loader</label>
                  </div>
                </div>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <div class="pf-c-tree-view__node" tabindex="0">
                  <div class="pf-c-tree-view__node-container">
                    <button
                      class="pf-c-tree-view__node-toggle"
                      aria-label="Toggle"
                      aria-expanded="false"
                    >
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </button>
                    <span class="pf-c-tree-view__node-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-5"
                          checked
                          aria-label="Tree view node check checked"
                        />
                      </div>
                    </span>
                    <label
                      class="pf-c-tree-view__node-text"
                      for="check-5"
                    >Loader</label>
                  </div>
                </div>
              </div>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <button
                  class="pf-c-tree-view__node-toggle"
                  aria-label="Toggle"
                  aria-expanded="true"
                >
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </button>
                <span class="pf-c-tree-view__node-check">
                  <div class="pf-c-check pf-m-standalone">
                    <input
                      class="pf-c-check__input"
                      type="checkbox"
                      id="check-6"
                      aria-label="Tree view node check"
                    />
                  </div>
                </span>
                <label
                  class="pf-c-tree-view__node-text"
                  for="check-6"
                >Application 2</label>
              </div>
            </div>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <div class="pf-c-tree-view__node" tabindex="0">
                  <div class="pf-c-tree-view__node-container">
                    <button
                      class="pf-c-tree-view__node-toggle"
                      aria-label="Toggle"
                      aria-expanded="false"
                    >
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </button>
                    <span class="pf-c-tree-view__node-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-7"
                          aria-label="Tree view node check"
                        />
                      </div>
                    </span>
                    <label
                      class="pf-c-tree-view__node-text"
                      for="check-7"
                    >Settings</label>
                  </div>
                </div>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <div class="pf-c-tree-view__node" tabindex="0">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-8"
                          aria-label="Tree view node check"
                        />
                      </div>
                    </span>
                    <label
                      class="pf-c-tree-view__node-text"
                      for="check-8"
                    >Settings</label>
                  </div>
                </div>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <div class="pf-c-tree-view__node" tabindex="0">
                  <div class="pf-c-tree-view__node-container">
                    <button
                      class="pf-c-tree-view__node-toggle"
                      aria-label="Toggle"
                      aria-expanded="true"
                    >
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </button>
                    <span class="pf-c-tree-view__node-check">
                      <div class="pf-c-check pf-m-standalone">
                        <input
                          class="pf-c-check__input"
                          type="checkbox"
                          id="check-9"
                          aria-label="Tree view node check"
                        />
                      </div>
                    </span>
                    <label
                      class="pf-c-tree-view__node-text"
                      for="check-9"
                    >Current</label>
                  </div>
                </div>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  aria-expanded="false"
                  tabindex="0"
                >
                  <div class="pf-c-tree-view__content">
                    <div class="pf-c-tree-view__node" tabindex="0">
                      <div class="pf-c-tree-view__node-container">
                        <button
                          class="pf-c-tree-view__node-toggle"
                          aria-label="Toggle"
                          aria-expanded="false"
                        >
                          <span class="pf-c-tree-view__node-toggle-icon">
                            <i class="fas fa-angle-right" aria-hidden="true"></i>
                          </span>
                        </button>
                        <span class="pf-c-tree-view__node-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="check-10"
                              aria-label="Tree view node check"
                            />
                          </div>
                        </span>
                        <label
                          class="pf-c-tree-view__node-text"
                          for="check-10"
                        >Loader app 1</label>
                      </div>
                    </div>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <div class="pf-c-tree-view__node" tabindex="0">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="check-11"
                              checked
                              aria-label="Tree view node check checked"
                            />
                          </div>
                        </span>
                        <label
                          class="pf-c-tree-view__node-text"
                          for="check-11"
                        >Loader app 2</label>
                      </div>
                    </div>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <div class="pf-c-tree-view__node" tabindex="0">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-check">
                          <div class="pf-c-check pf-m-standalone">
                            <input
                              class="pf-c-check__input"
                              type="checkbox"
                              id="check-12"
                              aria-label="Tree view node check"
                            />
                          </div>
                        </span>
                        <label
                          class="pf-c-tree-view__node-text"
                          for="check-12"
                        >Loader app 3</label>
                      </div>
                    </div>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <button
              class="pf-c-tree-view__node-toggle"
              aria-label="Toggle"
              aria-expanded="false"
            >
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </button>
            <span class="pf-c-tree-view__node-check">
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="check-13"
                  aria-label="Tree view node check"
                />
              </div>
            </span>
            <label
              class="pf-c-tree-view__node-text"
              for="check-13"
            >Cost management</label>
          </div>
        </div>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <button
              class="pf-c-tree-view__node-toggle"
              aria-label="Toggle"
              aria-expanded="false"
            >
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </button>
            <span class="pf-c-tree-view__node-check">
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="check-14"
                  aria-label="Tree view node check"
                />
              </div>
            </span>
            <label class="pf-c-tree-view__node-text" for="check-14">Sources</label>
          </div>
        </div>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <button
              class="pf-c-tree-view__node-toggle"
              aria-label="Toggle"
              aria-expanded="false"
            >
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </button>
            <span class="pf-c-tree-view__node-check">
              <div class="pf-c-check pf-m-standalone">
                <input
                  class="pf-c-check__input"
                  type="checkbox"
                  id="check-15"
                  checked
                  aria-label="Tree view node check checked"
                />
              </div>
            </span>
            <label
              class="pf-c-tree-view__node-text"
              for="check-15"
            >This is a really really really long folder name that overflows from the width of the container.</label>
          </div>
        </div>
      </div>
    </li>
  </ul>
</div>

```

### With icons

```html
<div class="pf-c-tree-view">
  <ul class="pf-c-tree-view__list" role="tree">
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-icon">
              <i class="fas fa-folder-open" aria-hidden="true"></i>
            </span>
            <span class="pf-c-tree-view__node-text">Application launcher</span>
          </div>
        </button>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-icon">
                  <i class="fas fa-folder-open" aria-hidden="true"></i>
                </span>
                <span class="pf-c-tree-view__node-text">Application 1</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-icon">
                      <i class="fas fa-folder" aria-hidden="true"></i>
                    </span>
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node pf-m-current">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-icon">
                      <i class="fas fa-folder" aria-hidden="true"></i>
                    </span>
                    <span class="pf-c-tree-view__node-text">Current</span>
                  </div>
                </button>
              </div>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-icon">
                  <i class="fas fa-folder-open" aria-hidden="true"></i>
                </span>
                <span class="pf-c-tree-view__node-text">Application 2</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-icon">
                      <i class="fas fa-folder" aria-hidden="true"></i>
                    </span>
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-icon">
                      <i class="fas fa-folder-open" aria-hidden="true"></i>
                    </span>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  aria-expanded="false"
                  tabindex="0"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-toggle">
                          <span class="pf-c-tree-view__node-toggle-icon">
                            <i class="fas fa-angle-right" aria-hidden="true"></i>
                          </span>
                        </div>
                        <span class="pf-c-tree-view__node-icon">
                          <i class="fas fa-folder" aria-hidden="true"></i>
                        </span>
                        <span class="pf-c-tree-view__node-text">Loader app 1</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-icon">
                          <i class="fas fa-folder" aria-hidden="true"></i>
                        </span>
                        <span class="pf-c-tree-view__node-text">Loader app 2</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-icon">
                          <i class="fas fa-folder" aria-hidden="true"></i>
                        </span>
                        <span class="pf-c-tree-view__node-text">Loader app 3</span>
                      </div>
                    </button>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-icon">
              <i class="fas fa-folder" aria-hidden="true"></i>
            </span>
            <span class="pf-c-tree-view__node-text">Cost management</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-icon">
              <i class="fas fa-folder" aria-hidden="true"></i>
            </span>
            <span class="pf-c-tree-view__node-text">Sources</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-icon">
              <i class="fas fa-folder" aria-hidden="true"></i>
            </span>
            <span
              class="pf-c-tree-view__node-text"
            >This is a really really really long folder name that overflows from the width of the container.</span>
          </div>
        </button>
      </div>
    </li>
  </ul>
</div>

```

### With badges

```html
<div class="pf-c-tree-view">
  <ul class="pf-c-tree-view__list" role="tree">
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Application launcher</span>
            <span class="pf-c-tree-view__node-count">
              <span class="pf-c-badge pf-m-read">2</span>
            </span>
          </div>
        </button>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 1</span>
                <span class="pf-c-tree-view__node-count">
                  <span class="pf-c-badge pf-m-read">2</span>
                </span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node pf-m-current">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Current</span>
                  </div>
                </button>
              </div>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 2</span>
                <span class="pf-c-tree-view__node-count">
                  <span class="pf-c-badge pf-m-read">2</span>
                </span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Settings</span>
                    <span class="pf-c-tree-view__node-count">
                      <span class="pf-c-badge pf-m-read">2</span>
                    </span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                    <span class="pf-c-tree-view__node-count">
                      <span class="pf-c-badge pf-m-read">2</span>
                    </span>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  aria-expanded="false"
                  tabindex="0"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-toggle">
                          <span class="pf-c-tree-view__node-toggle-icon">
                            <i class="fas fa-angle-right" aria-hidden="true"></i>
                          </span>
                        </div>
                        <span class="pf-c-tree-view__node-text">Loader app 1</span>
                        <span class="pf-c-tree-view__node-count">
                          <span class="pf-c-badge pf-m-read">2</span>
                        </span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 2</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 3</span>
                      </div>
                    </button>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Cost management</span>
            <span class="pf-c-tree-view__node-count">
              <span class="pf-c-badge pf-m-read">2</span>
            </span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Sources</span>
            <span class="pf-c-tree-view__node-count">
              <span class="pf-c-badge pf-m-read">2</span>
            </span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span
              class="pf-c-tree-view__node-text"
            >This is a really really really long folder name that overflows from the width of the container.</span>
            <span class="pf-c-tree-view__node-count">
              <span class="pf-c-badge pf-m-read">2</span>
            </span>
          </div>
        </button>
      </div>
    </li>
  </ul>
</div>

```

### With action item

```html
<div class="pf-c-tree-view">
  <ul class="pf-c-tree-view__list" role="tree">
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Application launcher</span>
          </div>
        </button>
        <div class="pf-c-tree-view__action">
          <button
            class="pf-c-button pf-m-plain"
            type="button"
            aria-label="Actions"
          >
            <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
          </button>
        </div>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 1</span>
              </div>
            </button>
            <div class="pf-c-tree-view__action">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Copy"
              >
                <i class="fas fa-clipboard" aria-hidden="true"></i>
              </button>
            </div>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node pf-m-current">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Current</span>
                  </div>
                </button>
              </div>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 2</span>
              </div>
            </button>
            <div class="pf-c-tree-view__action">
              <button
                class="pf-c-button pf-m-plain"
                type="button"
                aria-label="Action"
              >
                <i class="fas fa-bars" aria-hidden="true"></i>
              </button>
            </div>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  aria-expanded="false"
                  tabindex="0"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-toggle">
                          <span class="pf-c-tree-view__node-toggle-icon">
                            <i class="fas fa-angle-right" aria-hidden="true"></i>
                          </span>
                        </div>
                        <span class="pf-c-tree-view__node-text">Loader app 1</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 2</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 3</span>
                      </div>
                    </button>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Cost management</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Sources</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span
              class="pf-c-tree-view__node-text"
            >This is a really really really long folder name that overflows from the width of the container.</span>
          </div>
        </button>
        <div class="pf-c-tree-view__action">
          <div class="pf-c-dropdown">
            <button
              class="pf-c-dropdown__toggle pf-m-plain"
              id="dropdown-kebab-button"
              aria-expanded="false"
              type="button"
              aria-label="Actions"
            >
              <i class="fas fa-ellipsis-v" aria-hidden="true"></i>
            </button>
            <ul
              class="pf-c-dropdown__menu pf-m-align-right"
              aria-labelledby="dropdown-kebab-button"
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
    </li>
  </ul>
</div>

```

### With non-expandable top level nodes

```html
<div class="pf-c-tree-view">
  <ul class="pf-c-tree-view__list" role="tree">
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Application launcher</span>
          </div>
        </button>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 1</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node pf-m-current">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Current</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 2</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  aria-expanded="false"
                  tabindex="0"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-toggle">
                          <span class="pf-c-tree-view__node-toggle-icon">
                            <i class="fas fa-angle-right" aria-hidden="true"></i>
                          </span>
                        </div>
                        <span class="pf-c-tree-view__node-text">Loader app 1</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 2</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 3</span>
                      </div>
                    </button>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <span class="pf-c-tree-view__node-text">Cost management</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Sources</span>
          </div>
        </button>
      </div>
    </li>
    <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <span
              class="pf-c-tree-view__node-text"
            >This is a really really really long folder name that overflows from the width of the container.</span>
          </div>
        </button>
      </div>
    </li>
  </ul>
</div>

```

### Guides

```html
<div class="pf-c-tree-view pf-m-guides">
  <ul class="pf-c-tree-view__list" role="tree">
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Application launcher</span>
          </div>
        </button>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 1</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node pf-m-current">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Current</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
            </li>
          </ul>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <span class="pf-c-tree-view__node-text">Application 2</span>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item"
              role="treeitem"
              aria-expanded="false"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Settings</span>
                  </div>
                </button>
              </div>
            </li>
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <span class="pf-c-tree-view__node-text">Loader</span>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  aria-expanded="false"
                  tabindex="0"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-toggle">
                          <span class="pf-c-tree-view__node-toggle-icon">
                            <i class="fas fa-angle-right" aria-hidden="true"></i>
                          </span>
                        </div>
                        <span class="pf-c-tree-view__node-text">Loader app 1</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 2</span>
                      </div>
                    </button>
                  </div>
                </li>
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <button class="pf-c-tree-view__node">
                      <div class="pf-c-tree-view__node-container">
                        <span class="pf-c-tree-view__node-text">Loader app 3</span>
                      </div>
                    </button>
                  </div>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Cost management</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span class="pf-c-tree-view__node-text">Sources</span>
          </div>
        </button>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item"
      role="treeitem"
      aria-expanded="false"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <span
              class="pf-c-tree-view__node-text"
            >This is a really really really long folder name that overflows from the width of the container.</span>
          </div>
        </button>
      </div>
    </li>
  </ul>
</div>

```

### Compact

```html
<div class="pf-c-tree-view pf-m-compact">
  <ul class="pf-c-tree-view__list" role="tree">
    <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-content">
              <span class="pf-c-tree-view__node-title">apiVersion</span>
              <div
                class="pf-c-tree-view__node-text"
              >APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest internal value and may reject unrecognized values.</div>
            </div>
          </div>
        </div>
      </div>
    </li>
    <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-content">
              <span class="pf-c-tree-view__node-title">kind</span>
              <div
                class="pf-c-tree-view__node-text"
              >Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client submits requests to. Cannot be updated is CamelCase. More info:</div>
            </div>
          </div>
        </div>
      </div>
    </li>
    <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-content">
              <span class="pf-c-tree-view__node-title">metadata</span>
              <div class="pf-c-tree-view__node-text">Standard object metadata</div>
            </div>
          </div>
        </div>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <div class="pf-c-tree-view__node-content">
              <span class="pf-c-tree-view__node-title">spec</span>
              <div
                class="pf-c-tree-view__node-text"
              >Specification of the desired behavior of deployment.</div>
            </div>
          </div>
        </button>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">minReadySeconds</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Minimum number of seconds for which a newly created pod should be ready without any of its container crashing, for it to be considered available. Default to 0 (pod will be considered available as soon as it is ready).</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">paused</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Indicates that the deployment is paused</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span
                    class="pf-c-tree-view__node-title"
                  >progressDeadlineSeconds</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >The maximum time in seconds for a deployment to make progress before it is considered to be failed. The deployment controller will continue to process failed deployments and a condition with a ProgressDeadlineExceeded reason will be surfaced in the deployment status. Note that the progress will not de estimated during the time a deployment is paused. Defaults to 600s.</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">replicas</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Number of desired pods. This is a pointer to distinguish between explicit zero and not specified. Defaults to 1.</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">revisionHistoryLimit</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >The number of old ReplicaSets to retain to allow rollback. This is a pointer to distinguish between explicit zero and not specified. Defaults to 10.</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">Selector</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Label selector for pods. Existing ReplicaSets whose pods are selected by this will be the ones affected by this deployment</span>
                </div>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <div class="pf-c-tree-view__node-content">
                      <span class="pf-c-tree-view__node-title">matchExpressions</span>
                      <span
                        class="pf-c-tree-view__node-text"
                      >matchExpressions is a list of the label selector requirements. The requirements and ANDed.</span>
                    </div>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <div class="pf-c-tree-view__node" tabindex="0">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-content">
                          <span class="pf-c-tree-view__node-title">matchLabels</span>
                          <span
                            class="pf-c-tree-view__node-text"
                          >matchExpressions is a list of the label selector requirements. The requirements and ANDed.</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </li>
              </ul>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <div class="pf-c-tree-view__node" tabindex="0">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-content">
                      <span class="pf-c-tree-view__node-title">matchLabels</span>
                      <span
                        class="pf-c-tree-view__node-text"
                      >Map of {key.value} pairs. A single {key.value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In" and the values array contains only "value". The requirements are ANDed.</span>
                    </div>
                  </div>
                </div>
              </div>
            </li>
          </ul>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">matchLabels</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Map of {key.value} pairs. A single {key.value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In" and the values array contains only "value". The requirements are ANDed.</span>
                </div>
              </div>
            </div>
          </div>
        </li>
      </ul>
    </li>
  </ul>
</div>

```

### Compact, no background

```html
<div class="pf-c-tree-view pf-m-compact pf-m-no-background">
  <ul class="pf-c-tree-view__list" role="tree">
    <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-content">
              <span class="pf-c-tree-view__node-title">apiVersion</span>
              <div
                class="pf-c-tree-view__node-text"
              >APIVersion defines the versioned schema of this representation of an object. Servers should convert recognized schemas to the latest internal value and may reject unrecognized values.</div>
            </div>
          </div>
        </div>
      </div>
    </li>
    <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-content">
              <span class="pf-c-tree-view__node-title">kind</span>
              <div
                class="pf-c-tree-view__node-text"
              >Kind is a string value representing the REST resource this object represents. Servers may infer this from the endpoint the client submits requests to. Cannot be updated is CamelCase. More info:</div>
            </div>
          </div>
        </div>
      </div>
    </li>
    <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
      <div class="pf-c-tree-view__content">
        <div class="pf-c-tree-view__node" tabindex="0">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-content">
              <span class="pf-c-tree-view__node-title">metadata</span>
              <div class="pf-c-tree-view__node-text">Standard object metadata</div>
            </div>
          </div>
        </div>
      </div>
    </li>
    <li
      class="pf-c-tree-view__list-item pf-m-expanded"
      role="treeitem"
      aria-expanded="true"
      tabindex="0"
    >
      <div class="pf-c-tree-view__content">
        <button class="pf-c-tree-view__node">
          <div class="pf-c-tree-view__node-container">
            <div class="pf-c-tree-view__node-toggle">
              <span class="pf-c-tree-view__node-toggle-icon">
                <i class="fas fa-angle-right" aria-hidden="true"></i>
              </span>
            </div>
            <div class="pf-c-tree-view__node-content">
              <span class="pf-c-tree-view__node-title">spec</span>
              <div
                class="pf-c-tree-view__node-text"
              >Specification of the desired behavior of deployment.</div>
            </div>
          </div>
        </button>
      </div>
      <ul class="pf-c-tree-view__list" role="group">
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">minReadySeconds</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Minimum number of seconds for which a newly created pod should be ready without any of its container crashing, for it to be considered available. Default to 0 (pod will be considered available as soon as it is ready).</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">paused</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Indicates that the deployment is paused</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span
                    class="pf-c-tree-view__node-title"
                  >progressDeadlineSeconds</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >The maximum time in seconds for a deployment to make progress before it is considered to be failed. The deployment controller will continue to process failed deployments and a condition with a ProgressDeadlineExceeded reason will be surfaced in the deployment status. Note that the progress will not de estimated during the time a deployment is paused. Defaults to 600s.</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">replicas</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Number of desired pods. This is a pointer to distinguish between explicit zero and not specified. Defaults to 1.</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">revisionHistoryLimit</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >The number of old ReplicaSets to retain to allow rollback. This is a pointer to distinguish between explicit zero and not specified. Defaults to 10.</span>
                </div>
              </div>
            </div>
          </div>
        </li>
        <li
          class="pf-c-tree-view__list-item pf-m-expanded"
          role="treeitem"
          aria-expanded="true"
          tabindex="0"
        >
          <div class="pf-c-tree-view__content">
            <button class="pf-c-tree-view__node">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-toggle">
                  <span class="pf-c-tree-view__node-toggle-icon">
                    <i class="fas fa-angle-right" aria-hidden="true"></i>
                  </span>
                </div>
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">Selector</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Label selector for pods. Existing ReplicaSets whose pods are selected by this will be the ones affected by this deployment</span>
                </div>
              </div>
            </button>
          </div>
          <ul class="pf-c-tree-view__list" role="group">
            <li
              class="pf-c-tree-view__list-item pf-m-expanded"
              role="treeitem"
              aria-expanded="true"
              tabindex="0"
            >
              <div class="pf-c-tree-view__content">
                <button class="pf-c-tree-view__node">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-toggle">
                      <span class="pf-c-tree-view__node-toggle-icon">
                        <i class="fas fa-angle-right" aria-hidden="true"></i>
                      </span>
                    </div>
                    <div class="pf-c-tree-view__node-content">
                      <span class="pf-c-tree-view__node-title">matchExpressions</span>
                      <span
                        class="pf-c-tree-view__node-text"
                      >matchExpressions is a list of the label selector requirements. The requirements and ANDed.</span>
                    </div>
                  </div>
                </button>
              </div>
              <ul class="pf-c-tree-view__list" role="group">
                <li
                  class="pf-c-tree-view__list-item"
                  role="treeitem"
                  tabindex="-1"
                >
                  <div class="pf-c-tree-view__content">
                    <div class="pf-c-tree-view__node" tabindex="0">
                      <div class="pf-c-tree-view__node-container">
                        <div class="pf-c-tree-view__node-content">
                          <span class="pf-c-tree-view__node-title">matchLabels</span>
                          <span
                            class="pf-c-tree-view__node-text"
                          >matchExpressions is a list of the label selector requirements. The requirements and ANDed.</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </li>
              </ul>
            </li>
            <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
              <div class="pf-c-tree-view__content">
                <div class="pf-c-tree-view__node" tabindex="0">
                  <div class="pf-c-tree-view__node-container">
                    <div class="pf-c-tree-view__node-content">
                      <span class="pf-c-tree-view__node-title">matchLabels</span>
                      <span
                        class="pf-c-tree-view__node-text"
                      >Map of {key.value} pairs. A single {key.value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In" and the values array contains only "value". The requirements are ANDed.</span>
                    </div>
                  </div>
                </div>
              </div>
            </li>
          </ul>
        </li>
        <li class="pf-c-tree-view__list-item" role="treeitem" tabindex="-1">
          <div class="pf-c-tree-view__content">
            <div class="pf-c-tree-view__node" tabindex="0">
              <div class="pf-c-tree-view__node-container">
                <div class="pf-c-tree-view__node-content">
                  <span class="pf-c-tree-view__node-title">matchLabels</span>
                  <span
                    class="pf-c-tree-view__node-text"
                  >Map of {key.value} pairs. A single {key.value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In" and the values array contains only "value". The requirements are ANDed.</span>
                </div>
              </div>
            </div>
          </div>
        </li>
      </ul>
    </li>
  </ul>
</div>

```

## Documentation

### Overview

### Accessibility

| Attribute                          | Applied to                                 | Outcome                                                                                                                                                                                                                             |
| ---------------------------------- | ------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="tree"`                      | `.pf-c-tree-view__list`                    | Identifies the `ul` as a tree widget. **Place on the outermost `ul` only**                                                                                                                                                          |
| `role="group"`                     | `.pf-c-tree-view__list`                    | Identifies the `ul` element as a container of treeitem elements that form a branch of the tree. **Place on all `ul`s except the first `ul`**                                                                                        |
| `role="treeitem"`                  | `.pf-c-tree-view__list-item`               | Hides the implicit listitem role of the li element from assistive technologies.                                                                                                                                                     |
| `aria-expanded="false"`            | `.pf-c-tree-view__list-item`               | For an expandable item, indicates the parent node is closed, i.e., the descendant elements are not visible.                                                                                                                         |
| `aria-expanded="true"`             | `.pf-c-tree-view__list-item.pf-m-expanded` | Indicates the parent node is open, i.e., the descendant elements are visible.                                                                                                                                                       |
| `tabindex="-1"`                    | `.pf-c-tree-view__list-item`               | Makes the element with the treeitem role focusable without including it in the tab sequence of the page.                                                                                                                            |
| `tabindex="0"`                     | `.pf-c-tree-view__list-item`               | Includes the element with the treeitem role in the tab sequence. Only one treeitem in the tree has tabindex="0". When the user moves focus in the tree, the element included in the tab sequence changes to the element with focus. |
| `aria-label="[button label text]"` | `.pf-c-tree-view__action`                  | Provides an accessible name for the button when an icon is used instead of text. **Required when icon is used with no supporting text**                                                                                             |

### Usage

| Class                                 | Applied                                                                        | Outcome                                                                                  |
| ------------------------------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------- |
| `.pf-c-tree-view`                     | `<div>`                                                                        | Initiates the tree view container. **Required**                                          |
| `.pf-c-tree-view__list`               | `<ul>`                                                                         | Initiates a tree view list. **Required**                                                 |
| `.pf-c-tree-view__list-item`          | `<li>`                                                                         | Initiates a tree view list item. **Required**                                            |
| `.pf-c-tree-view__content`            | `<div>`                                                                        | Initiates a tree view node. **Required**                                                 |
| `.pf-c-tree-view__node`               | `<button>`, `<a>`                                                              | Initiates a tree view node. **Required**                                                 |
| `.pf-c-tree-view__node-container`     | `<span>`                                                                       | Initiates a tree view node container. **Required for compact variant**                   |
| `.pf-c-tree-view__node-content`       | `<span>`                                                                       | Initiates a tree view node content container used to stack elements.                     |
| `.pf-c-tree-view__node-count`         | `<span>`                                                                       | Initiates a tree view node count.                                                        |
| `.pf-c-tree-view__node-toggle`        | `<div>`                                                                        | Initiates a tree view toggle.                                                            |
| `.pf-c-tree-view__node-toggle-button` | `<button>`                                                                     | Initiates a tree view toggle button.                                                     |
| `.pf-c-tree-view__node-toggle-icon`   | `<span>`                                                                       | Initiates a tree view toggle icon.                                                       |
| `.pf-c-tree-view__node-title`         | `<span>`                                                                       | Initiates a tree view node title.                                                        |
| `.pf-c-tree-view__node-text`          | `<span>`                                                                       | Initiates tree view text.                                                                |
| `.pf-c-tree-view__node-icon`          | `<span>`                                                                       | Initiates a tree view icon.                                                              |
| `.pf-c-tree-view__node-check`         | `<span>`                                                                       | Initiates a tree view check.                                                             |
| `.pf-c-tree-view__action`             | `<div>`                                                                        | Initiates a tree view action wrapper.                                                    |
| `.pf-c-tree-view__search`             | `<div>`                                                                        | Initiates a tree view search wrapper.                                                    |
| `.pf-m-guides`                        | `.pf-c-tree-view`                                                              | Modifies the tree view to the guides presentation.                                       |
| `.pf-m-compact`                       | `.pf-c-tree-view`                                                              | Modifies the tree view to the compact presentation.                                      |
| `.pf-m-no-background`                 | `.pf-c-tree-view.pf-m-compact`                                                 | Modifies the tree view compact variant node containers to have a transparent background. |
| `.pf-m-current`                       | `.pf-c-tree-view__node`                                                        | Modifies the tree view node to be current.                                               |
| `.pf-m-truncate`                      | `.pf-c-tree-view`, `.pf-c-tree-view__node-title`, `.pf-c-tree-view__node-text` | Modifies the tree view title or text to truncate.                                        |
