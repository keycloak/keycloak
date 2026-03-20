import { LitElement, html, nothing } from "lit";

/**
 * @typedef {Object} DropdownItem
 * @property {string} label - Item label
 * @property {Function} [onClick] - Click handler
 * @property {string} [href] - Link URL (renders as anchor instead of button)
 * @property {boolean} [disabled] - Whether item is disabled
 * @property {boolean} [separator] - Render as separator instead of item
 */

const caretDownIcon = html`
  <svg
    class="pf-v6-svg"
    viewBox="0 0 320 512"
    fill="currentColor"
    aria-hidden="true"
    width="1em"
    height="1em"
  >
    <path
      d="M31.3 192h257.3c17.8 0 26.7 21.5 14.1 34.1L174.1 354.8c-7.8 7.8-20.5 7.8-28.3 0L17.2 226.1C4.6 213.5 13.5 192 31.3 192z"
    ></path>
  </svg>
`;

/**
 * Dropdown menu component
 *
 * @element kc-dropdown
 *
 * @prop {string} label - Toggle button label
 * @prop {DropdownItem[]} items - Menu items
 * @prop {'left' | 'right'} align - Menu alignment (default: 'right')
 *
 * @example
 * <kc-dropdown
 *   label="Username"
 *   .items="${[{ label: 'Sign out', onClick: () => signOut() }]}"
 * ></kc-dropdown>
 */
export class KcDropdown extends LitElement {
  static properties = {
    label: { type: String },
    items: { type: Array },
    align: { type: String },
    _open: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.label = "";
    this.items = [];
    this.align = "right";
    this._open = false;
  }

  connectedCallback() {
    super.connectedCallback();
    document.addEventListener("click", this.#handleOutsideClick);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    document.removeEventListener("click", this.#handleOutsideClick);
  }

  #handleOutsideClick = (e) => {
    if (!this.contains(e.target)) {
      this._open = false;
    }
  };

  #toggle = (e) => {
    e.stopPropagation();
    this._open = !this._open;
  };

  #handleItemClick = (item) => {
    this._open = false;
    if (item.onClick) {
      item.onClick();
    }
  };

  #renderItem(item) {
    if (item.separator) {
      return html`<li
        class="pf-v6-c-menu__list-item pf-m-divider"
        role="separator"
      ></li>`;
    }

    const content = html`
      <span class="pf-v6-c-menu__item-main">
        <span class="pf-v6-c-menu__item-text">${item.label}</span>
      </span>
    `;

    if (item.href) {
      return html`
        <li class="pf-v6-c-menu__list-item" role="none">
          <a
            class="pf-v6-c-menu__item"
            href="${item.href}"
            role="menuitem"
            ?aria-disabled="${item.disabled}"
            @click="${() => (this._open = false)}"
          >
            ${content}
          </a>
        </li>
      `;
    }

    return html`
      <li class="pf-v6-c-menu__list-item" role="none">
        <button
          class="pf-v6-c-menu__item"
          type="button"
          role="menuitem"
          ?disabled="${item.disabled}"
          @click="${() => this.#handleItemClick(item)}"
        >
          ${content}
        </button>
      </li>
    `;
  }

  #renderMenu() {
    const menuStyle =
      this.align === "right"
        ? "position: absolute; top: 100%; right: 0; z-index: 9999; min-width: max-content;"
        : "position: absolute; top: 100%; left: 0; z-index: 9999; min-width: max-content;";

    return html`
      <div class="pf-v6-c-menu" style="${menuStyle}">
        <div class="pf-v6-c-menu__content">
          <ul class="pf-v6-c-menu__list" role="menu">
            ${this.items.map((item) => this.#renderItem(item))}
          </ul>
        </div>
      </div>
    `;
  }

  render() {
    return html`
      <div
        class="pf-v6-c-menu-toggle ${this._open ? "pf-m-expanded" : ""}"
        style="position: relative; display: inline-block;"
      >
        <button
          class="pf-v6-c-menu-toggle__button"
          type="button"
          @click="${this.#toggle}"
          aria-expanded="${this._open}"
          style="display: inline-flex; align-items: center; gap: 0.5rem;"
        >
          <span class="pf-v6-c-menu-toggle__text">${this.label}</span>
          <span class="pf-v6-c-menu-toggle__controls">
            <span class="pf-v6-c-menu-toggle__toggle-icon"
              >${caretDownIcon}</span
            >
          </span>
        </button>
        ${this._open ? this.#renderMenu() : nothing}
      </div>
    `;
  }
}

customElements.define("kc-dropdown", KcDropdown);
