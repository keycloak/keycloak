import { LitElement, html, nothing } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import { getOrganizations } from "../api/methods.js";
import { t, label } from "../i18n.js";
import "../components/ui/index.js";

export class KcOrganizations extends LitElement {
  static properties = {
    keycloak: { state: true },
    organizations: { state: true },
    loading: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.organizations = [];
    this.loading = true;
    new ContextConsumer(this, {
      context: keycloakContext,
      subscribe: true,
      callback: async (value) => {
        this.keycloak = value;
        if (value) await this.loadOrganizations();
      },
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    if (this.keycloak) await this.loadOrganizations();
  }

  async loadOrganizations() {
    if (!this.keycloak) return;

    try {
      this.loading = true;
      this.organizations = await getOrganizations({ keycloak: this.keycloak });
    } catch (error) {
      console.error("Failed to load organizations:", error);
    } finally {
      this.loading = false;
    }
  }

  #renderOrganization(org) {
    const displayName = label(org.name) || org.alias;

    return html`
      <li class="pf-v6-c-data-list__item">
        <div class="pf-v6-c-data-list__item-row">
          <div class="pf-v6-c-data-list__item-content">
            <div class="pf-v6-c-data-list__cell">
              <strong>${displayName}</strong>
              ${org.description ? html`<p>${org.description}</p>` : nothing}
            </div>
            <div class="pf-v6-c-data-list__cell">
              ${org.domains?.length > 0
                ? html`
                    <div class="pf-v6-c-label-group">
                      ${org.domains.map(
                        (domain) => html`
                          <span
                            class="pf-v6-c-label ${domain.verified
                              ? "pf-m-green"
                              : ""} pf-m-outline"
                          >
                            <span class="pf-v6-c-label__content"
                              >${domain.name}</span
                            >
                          </span>
                        `,
                      )}
                    </div>
                  `
                : nothing}
            </div>
          </div>
        </div>
      </li>
    `;
  }

  render() {
    return html`
      <div class="pf-v6-c-content">
        <h1>${t("organizations")}</h1>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          ${this.loading
            ? html`<kc-spinner centered size="xl"></kc-spinner>`
            : this.organizations.length === 0
              ? html`<kc-empty-state
                  body="${t("noOrganizations")}"
                ></kc-empty-state>`
              : html`
                  <ul class="pf-v6-c-data-list" role="list">
                    ${this.organizations.map((org) =>
                      this.#renderOrganization(org),
                    )}
                  </ul>
                `}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-organizations", KcOrganizations);
