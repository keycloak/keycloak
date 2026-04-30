import { LitElement, html, nothing } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import { getResources } from "../api/methods.js";
import { t, label } from "../i18n.js";
import "../components/ui/index.js";

export class KcResources extends LitElement {
  static properties = {
    keycloak: { state: true },
    resources: { state: true },
    loading: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.resources = [];
    this.loading = true;
    new ContextConsumer(this, {
      context: keycloakContext,
      subscribe: true,
      callback: async (value) => {
        this.keycloak = value;
        if (value) await this.loadResources();
      },
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    if (this.keycloak) await this.loadResources();
  }

  async loadResources() {
    if (!this.keycloak) return;

    try {
      this.loading = true;
      this.resources = await getResources({ keycloak: this.keycloak });
    } catch (error) {
      console.error("Failed to load resources:", error);
    } finally {
      this.loading = false;
    }
  }

  #renderResource(resource) {
    const displayName = label(resource.name) || resource._id;

    return html`
      <li class="pf-v6-c-data-list__item">
        <div class="pf-v6-c-data-list__item-row">
          <div class="pf-v6-c-data-list__item-content">
            <div class="pf-v6-c-data-list__cell">
              <strong>${displayName}</strong>
            </div>
            <div class="pf-v6-c-data-list__cell">
              <dl class="pf-v6-c-description-list pf-m-horizontal pf-m-compact">
                ${resource.owner?.name
                  ? html`
                      <div class="pf-v6-c-description-list__group">
                        <dt class="pf-v6-c-description-list__term">
                          ${t("owner")}
                        </dt>
                        <dd class="pf-v6-c-description-list__description">
                          ${resource.owner.name}
                        </dd>
                      </div>
                    `
                  : nothing}
                ${resource.uris?.length > 0
                  ? html`
                      <div class="pf-v6-c-description-list__group">
                        <dt class="pf-v6-c-description-list__term">
                          ${t("uris")}
                        </dt>
                        <dd class="pf-v6-c-description-list__description">
                          ${resource.uris.join(", ")}
                        </dd>
                      </div>
                    `
                  : nothing}
                ${resource.scopes?.length > 0
                  ? html`
                      <div class="pf-v6-c-description-list__group">
                        <dt class="pf-v6-c-description-list__term">
                          ${t("scopes")}
                        </dt>
                        <dd class="pf-v6-c-description-list__description">
                          <div class="pf-v6-c-label-group">
                            ${resource.scopes.map(
                              (scope) => html`
                                <span class="pf-v6-c-label pf-m-outline">
                                  <span class="pf-v6-c-label__content"
                                    >${label(
                                      scope.displayName,
                                      scope.name,
                                    )}</span
                                  >
                                </span>
                              `,
                            )}
                          </div>
                        </dd>
                      </div>
                    `
                  : nothing}
              </dl>
            </div>
          </div>
        </div>
      </li>
    `;
  }

  render() {
    return html`
      <div class="pf-v6-c-content">
        <h1>${t("myResources")}</h1>
        <p>${t("resourceIntroMessage")}</p>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          ${this.loading
            ? html`<kc-spinner centered size="xl"></kc-spinner>`
            : this.resources.length === 0
              ? html`<kc-empty-state
                  body="${t("noResources")}"
                ></kc-empty-state>`
              : html`
                  <ul class="pf-v6-c-data-list" role="list">
                    ${this.resources.map((resource) =>
                      this.#renderResource(resource),
                    )}
                  </ul>
                `}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-resources", KcResources);
