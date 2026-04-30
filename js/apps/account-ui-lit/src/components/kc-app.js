import { LitElement, html, nothing } from "lit";
import { ContextProvider } from "@lit/context";
import { keycloakContext, initKeycloak } from "../keycloak-context.js";
import { initI18n, t, label } from "../i18n.js";
import { fetchContentJson } from "../api/fetch-content.js";
import { environment } from "../environment.js";
import { flattenMenuItems } from "../types/menu.js";
import { joinPath } from "../utils/join-path.js";
import "./ui/index.js";

import "./kc-nav.js";

export class KcApp extends LitElement {
  static properties = {
    keycloak: { state: true },
    loading: { state: true },
    currentPath: { state: true },
    menuItems: { state: true },
    pageElement: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.loading = true;
    this.currentPath = "";
    this.menuItems = [];
    this.pageElement = null;
    /** @type {Set<string>} */
    this.loadedModules = new Set();
    this._keycloakProvider = new ContextProvider(this, {
      context: keycloakContext,
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    await this.initialize();
    await this.setupRouting();
  }

  #handleSignOut = () => {
    this.keycloak?.logout();
  };

  #getLoggedInUserName() {
    const token = this.keycloak?.tokenParsed;
    if (!token) {
      return t("unknownUser");
    }

    const givenName = token.given_name;
    const familyName = token.family_name;
    const preferredUsername = token.preferred_username;

    if (givenName && familyName) {
      return t("fullName", { givenName, familyName });
    }

    return givenName || familyName || preferredUsername || t("unknownUser");
  }

  async initialize() {
    try {
      await initI18n();
      this.keycloak = await initKeycloak();
      this._keycloakProvider.setValue(this.keycloak);
      this.menuItems = await fetchContentJson();
      await this.loadCurrentPage();
    } catch (error) {
      console.error("Failed to initialize:", error);
    } finally {
      this.loading = false;
    }
  }

  async setupRouting() {
    const handleHashChange = async () => {
      const hash = window.location.hash.slice(2);
      this.currentPath = hash || "";
      await this.loadCurrentPage();
    };

    window.addEventListener("hashchange", handleHashChange);
    await handleHashChange();
  }

  /**
   * @param {string} path
   * @returns {import("../types/menu.js").MenuItem | undefined}
   */
  findMenuItem(path) {
    const allItems = flattenMenuItems(this.menuItems);
    return allItems.find((item) => {
      if (item.path === path) return true;
      if (path === "" && item.path === "") return true;
      return false;
    });
  }

  /**
   * @param {import("../types/menu.js").MenuItem} item
   * @returns {boolean}
   */
  isVisible(item) {
    if (!item.isVisible) return true;
    return environment.features[item.isVisible] === true;
  }

  async loadCurrentPage() {
    const menuItem = this.findMenuItem(this.currentPath);

    if (!menuItem?.component || !menuItem.modulePath) {
      const defaultItem = this.findMenuItem("");
      if (defaultItem?.component && defaultItem.modulePath) {
        await this.loadPageComponent(defaultItem);
      }
      return;
    }

    if (!this.isVisible(menuItem)) {
      const defaultItem = this.findMenuItem("");
      if (defaultItem?.component && defaultItem.modulePath) {
        await this.loadPageComponent(defaultItem);
      }
      return;
    }

    await this.loadPageComponent(menuItem);
  }

  /**
   * @param {import("../types/menu.js").MenuItem} menuItem
   */
  async loadPageComponent(menuItem) {
    if (!menuItem.modulePath || !menuItem.component) return;

    try {
      if (!this.loadedModules.has(menuItem.modulePath)) {
        const baseUrl = new URL(
          environment.resourceUrl + "/",
          window.location.origin,
        );
        const modulePath = new URL(menuItem.modulePath, baseUrl).href;
        await import(modulePath);
        this.loadedModules.add(menuItem.modulePath);
      }

      this.pageElement = document.createElement(menuItem.component);
    } catch (error) {
      console.error(
        `Failed to load page component: ${menuItem.component}`,
        error,
      );
      this.pageElement = null;
    }
  }

  render() {
    if (this.loading) {
      return html`<kc-spinner size="xl" centered fullHeight></kc-spinner>`;
    }

    const brandImage = environment.logo || "logo.svg";
    const logoUrl = environment.logoUrl || "/";
    const username = this.#getLoggedInUserName();

    const userMenuItems = [
      { label: t("doSignOut"), onClick: this.#handleSignOut },
    ];

    return html`
      <div class="pf-v6-c-page">
        <header class="pf-v6-c-masthead">
          <div class="pf-v6-c-masthead__main">
            <div class="pf-v6-c-masthead__brand">
              <a class="pf-v6-c-masthead__logo" href="${logoUrl}">
                <img
                  class="pf-v6-c-brand"
                  src="${joinPath(environment.resourceUrl, brandImage)}"
                  alt="Logo"
                />
              </a>
            </div>
          </div>
          <div class="pf-v6-c-masthead__content">
            <div class="pf-v6-c-toolbar">
              <div class="pf-v6-c-toolbar__content">
                <div class="pf-v6-c-toolbar__content-section">
                  <div class="pf-v6-c-toolbar__group pf-m-align-end">
                    ${environment.referrerUrl
                      ? html`
                          <div class="pf-v6-c-toolbar__item">
                            <kc-button
                              variant="link"
                              label="${t("backTo", {
                                app: label(environment.referrerName, ""),
                              })}"
                              href="${environment.referrerUrl.replace(
                                "_hash_",
                                "#",
                              )}"
                            ></kc-button>
                          </div>
                        `
                      : nothing}
                    <div class="pf-v6-c-toolbar__item">
                      <kc-dropdown
                        label="${username}"
                        .items="${userMenuItems}"
                      ></kc-dropdown>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </header>
        <div class="pf-v6-c-page__sidebar pf-m-expanded">
          <div class="pf-v6-c-page__sidebar-body">
            <kc-nav
              .currentPath="${this.currentPath}"
              .menuItems="${this.menuItems}"
            ></kc-nav>
          </div>
        </div>
        <div class="pf-v6-c-page__main-container" tabindex="-1">
          <main class="pf-v6-c-page__main" tabindex="-1">
            <section class="pf-v6-c-page__main-section">
              <div class="pf-v6-c-page__main-body">
                ${this.pageElement ? this.pageElement : nothing}
              </div>
            </section>
          </main>
        </div>
      </div>
    `;
  }
}

customElements.define("kc-app", KcApp);
