import { LitElement, html } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import {
  getLinkedAccounts,
  linkAccount,
  unlinkAccount,
} from "../api/methods.js";
import { t, label } from "../i18n.js";
import "../components/ui/index.js";

export class KcLinkedAccounts extends LitElement {
  static properties = {
    keycloak: { state: true },
    accounts: { state: true },
    loading: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.accounts = [];
    this.loading = true;
    new ContextConsumer(this, {
      context: keycloakContext,
      subscribe: true,
      callback: async (value) => {
        this.keycloak = value;
        if (value) await this.loadAccounts();
      },
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    if (this.keycloak) await this.loadAccounts();
  }

  async loadAccounts() {
    if (!this.keycloak) return;

    try {
      this.loading = true;
      this.accounts = await getLinkedAccounts({ keycloak: this.keycloak });
    } catch (error) {
      console.error("Failed to load linked accounts:", error);
    } finally {
      this.loading = false;
    }
  }

  async handleLink(account) {
    if (!this.keycloak) return;

    try {
      await linkAccount(this.keycloak, account);
    } catch (error) {
      console.error("Failed to link account:", error);
    }
  }

  async handleUnlink(account) {
    if (!this.keycloak) return;

    try {
      await unlinkAccount(this.keycloak, account);
      await this.loadAccounts();
    } catch (error) {
      console.error("Failed to unlink account:", error);
    }
  }

  #renderAccount(account) {
    const displayName = label(account.displayName, account.providerAlias);

    return html`
      <li class="pf-v6-c-data-list__item">
        <div class="pf-v6-c-data-list__item-row">
          <div class="pf-v6-c-data-list__item-content">
            <div class="pf-v6-c-data-list__cell">
              <strong>${displayName}</strong>
              ${account.linked
                ? html`<span class="pf-v6-c-label pf-m-green"
                    ><span class="pf-v6-c-label__content"
                      >${account.linkedUsername}</span
                    ></span
                  >`
                : html`<span class="pf-v6-c-label pf-m-outline"
                    ><span class="pf-v6-c-label__content"
                      >${t("notLinked")}</span
                    ></span
                  >`}
            </div>
          </div>
          <div class="pf-v6-c-data-list__item-action">
            ${account.linked
              ? html`
                  <kc-button
                    label="${t("doRemove")}"
                    variant="danger"
                    size="sm"
                    @click="${() => this.handleUnlink(account)}"
                  ></kc-button>
                `
              : html`
                  <kc-button
                    label="${t("doLink")}"
                    variant="primary"
                    size="sm"
                    @click="${() => this.handleLink(account)}"
                  ></kc-button>
                `}
          </div>
        </div>
      </li>
    `;
  }

  render() {
    const linkedAccounts = this.accounts.filter((a) => a.linked);
    const unlinkedAccounts = this.accounts.filter((a) => !a.linked);

    return html`
      <div class="pf-v6-c-content">
        <h1>${t("linkedAccountsHtmlTitle")}</h1>
        <p>${t("identityProviderMessage")}</p>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          ${this.loading
            ? html`<kc-spinner centered size="xl"></kc-spinner>`
            : this.accounts.length === 0
              ? html`<kc-empty-state
                  body="${t("noLinkedAccounts")}"
                ></kc-empty-state>`
              : html`
                  <div class="pf-v6-l-stack pf-m-gutter">
                    <div class="pf-v6-l-stack__item">
                      <h2 class="pf-v6-c-title pf-m-lg">
                        ${t("linkedLoginProviders")}
                      </h2>
                      ${linkedAccounts.length > 0
                        ? html`
                            <ul class="pf-v6-c-data-list" role="list">
                              ${linkedAccounts.map((account) =>
                                this.#renderAccount(account),
                              )}
                            </ul>
                          `
                        : html`<p>${t("linkedEmpty")}</p>`}
                    </div>
                    <div class="pf-v6-l-stack__item">
                      <h2 class="pf-v6-c-title pf-m-lg">
                        ${t("unlinkedLoginProviders")}
                      </h2>
                      ${unlinkedAccounts.length > 0
                        ? html`
                            <ul class="pf-v6-c-data-list" role="list">
                              ${unlinkedAccounts.map((account) =>
                                this.#renderAccount(account),
                              )}
                            </ul>
                          `
                        : html`<p>${t("unlinkedEmpty")}</p>`}
                    </div>
                  </div>
                `}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-linked-accounts", KcLinkedAccounts);
