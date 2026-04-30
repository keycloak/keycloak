import { LitElement, html } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import { getGroups } from "../api/methods.js";
import { t } from "../i18n.js";
import "../components/ui/index.js";

export class KcGroups extends LitElement {
  static properties = {
    keycloak: { state: true },
    groups: { state: true },
    loading: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.groups = [];
    this.loading = true;
    new ContextConsumer(this, {
      context: keycloakContext,
      subscribe: true,
      callback: async (value) => {
        this.keycloak = value;
        if (value) await this.loadGroups();
      },
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    if (this.keycloak) await this.loadGroups();
  }

  async loadGroups() {
    if (!this.keycloak) return;

    try {
      this.loading = true;
      this.groups = await getGroups({ keycloak: this.keycloak });
    } catch (error) {
      console.error("Failed to load groups:", error);
    } finally {
      this.loading = false;
    }
  }

  #renderGroup(group) {
    return html`
      <li class="pf-v6-c-data-list__item">
        <div class="pf-v6-c-data-list__item-row">
          <div class="pf-v6-c-data-list__item-content">
            <div class="pf-v6-c-data-list__cell">
              <strong>${group.name}</strong>
            </div>
            <div class="pf-v6-c-data-list__cell pf-m-secondary">
              ${group.path}
            </div>
          </div>
        </div>
      </li>
    `;
  }

  render() {
    return html`
      <div class="pf-v6-c-content">
        <h1>${t("group")}</h1>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          ${this.loading
            ? html`<kc-spinner centered size="xl"></kc-spinner>`
            : this.groups.length === 0
              ? html`<kc-empty-state body="${t("noGroups")}"></kc-empty-state>`
              : html`
                  <ul class="pf-v6-c-data-list" role="list">
                    ${this.groups.map((group) => this.#renderGroup(group))}
                  </ul>
                `}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-groups", KcGroups);
