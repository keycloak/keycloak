import { LitElement, html, nothing } from "lit";
import { environment } from "../environment.js";
import { t } from "../i18n.js";

export class KcNav extends LitElement {
  static properties = {
    currentPath: { type: String },
    menuItems: { type: Array },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.currentPath = "";
    this.menuItems = [];
  }

  /**
   * @param {import("../types/menu.js").MenuItem} item
   * @returns {boolean}
   */
  isVisible(item) {
    if (!item.isVisible) return true;
    return environment.features[item.isVisible] === true;
  }

  /**
   * @param {string | undefined} path
   * @returns {boolean}
   */
  isActive(path) {
    if (path === undefined) return false;
    if (path === "" && (this.currentPath === "" || this.currentPath === "/")) {
      return true;
    }
    return this.currentPath === path || this.currentPath === `/${path}`;
  }

  /**
   * @param {import("../types/menu.js").MenuItem} item
   */
  renderMenuItem(item) {
    if (!this.isVisible(item)) return nothing;

    if (item.children) {
      const visibleChildren = item.children.filter((child) =>
        this.isVisible(child),
      );
      if (visibleChildren.length === 0) return nothing;

      return html`
        <section class="pf-v6-c-nav__section">
          <h2 class="pf-v6-c-nav__section-title">${t(item.label)}</h2>
          <ul class="pf-v6-c-nav__list">
            ${visibleChildren.map((child) => this.renderNavLink(child))}
          </ul>
        </section>
      `;
    }

    return this.renderNavLink(item);
  }

  renderNavLink(item) {
    if (!this.isVisible(item)) return nothing;

    return html`
      <li class="pf-v6-c-nav__item">
        <a
          class="pf-v6-c-nav__link ${this.isActive(item.path)
            ? "pf-m-current"
            : ""}"
          href="#/${item.path || ""}"
          @click="${(e) => this.handleNavClick(e, item.path || "")}"
          aria-current="${this.isActive(item.path) ? "page" : nothing}"
        >
          ${t(item.label)}
        </a>
      </li>
    `;
  }

  /**
   * @param {Event} e
   * @param {string} path
   */
  handleNavClick(e, path) {
    e.preventDefault();
    window.location.hash = `#/${path}`;
    this.dispatchEvent(
      new CustomEvent("navigate", {
        detail: { path },
        bubbles: true,
        composed: true,
      }),
    );
  }

  render() {
    return html`
      <nav class="pf-v6-c-nav" aria-label="Account navigation">
        <ul class="pf-v6-c-nav__list">
          ${this.menuItems.map((item) => this.renderMenuItem(item))}
        </ul>
      </nav>
    `;
  }
}

customElements.define("kc-nav", KcNav);
