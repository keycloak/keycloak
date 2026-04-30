import { LitElement, html, nothing } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import { getApplications, deleteConsent } from "../api/methods.js";
import { t, label } from "../i18n.js";
import "../components/ui/index.js";

export class KcApplications extends LitElement {
  static properties = {
    keycloak: { state: true },
    applications: { state: true },
    loading: { state: true },
    expandedApps: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.applications = [];
    this.loading = true;
    this.expandedApps = new Set();
    new ContextConsumer(this, {
      context: keycloakContext,
      subscribe: true,
      callback: async (value) => {
        this.keycloak = value;
        if (value) await this.loadApplications();
      },
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    if (this.keycloak) await this.loadApplications();
  }

  async loadApplications() {
    if (!this.keycloak) return;

    try {
      this.loading = true;
      this.applications = await getApplications({ keycloak: this.keycloak });
    } catch (error) {
      console.error("Failed to load applications:", error);
    } finally {
      this.loading = false;
    }
  }

  async handleRevokeConsent(clientId) {
    if (!this.keycloak) return;

    try {
      await deleteConsent(this.keycloak, clientId);
      await this.loadApplications();
    } catch (error) {
      console.error("Failed to revoke consent:", error);
    }
  }

  toggleExpanded(clientId) {
    const newSet = new Set(this.expandedApps);
    if (newSet.has(clientId)) {
      newSet.delete(clientId);
    } else {
      newSet.add(clientId);
    }
    this.expandedApps = newSet;
  }

  #getAppType(app) {
    const type = app.userConsentRequired
      ? t("thirdPartyApp")
      : t("internalApp");
    return app.offlineAccess ? `${type}, ${t("offlineAccess")}` : type;
  }

  #renderApp(app) {
    const isExpanded = this.expandedApps.has(app.clientId);
    const displayName = label(app.clientName, app.clientId);

    return html`
      <li class="pf-v6-c-data-list__item ${isExpanded ? "pf-m-expanded" : ""}">
        <div class="pf-v6-c-data-list__item-row">
          <div class="pf-v6-c-data-list__item-control">
            <div class="pf-v6-c-data-list__toggle">
              <button
                class="pf-v6-c-button pf-m-plain"
                @click="${() => this.toggleExpanded(app.clientId)}"
                aria-expanded="${isExpanded}"
              >
                <span class="pf-v6-c-data-list__toggle-icon">
                  <svg
                    viewBox="0 0 256 512"
                    fill="currentColor"
                    width="1em"
                    height="1em"
                    aria-hidden="true"
                  >
                    <path
                      d="M64 448c-8.188 0-16.38-3.125-22.62-9.375c-12.5-12.5-12.5-32.75 0-45.25L178.8 256L41.38 118.6c-12.5-12.5-12.5-32.75 0-45.25s32.75-12.5 45.25 0l160 160c12.5 12.5 12.5 32.75 0 45.25l-160 160C80.38 444.9 72.19 448 64 448z"
                    />
                  </svg>
                </span>
              </button>
            </div>
          </div>
          <div class="pf-v6-c-data-list__item-content">
            <div class="pf-v6-c-data-list__cell">
              ${app.effectiveUrl
                ? html`<a
                    href="${app.effectiveUrl}"
                    target="_blank"
                    rel="noopener noreferrer"
                    test-id="referrer-link"
                    >${displayName}</a
                  >`
                : html`<strong>${displayName}</strong>`}
            </div>
            <div class="pf-v6-c-data-list__cell">
              <span
                class="pf-v6-c-label ${app.inUse
                  ? "pf-m-green"
                  : ""} pf-m-outline"
              >
                <span class="pf-v6-c-label__content"
                  >${app.inUse ? t("inUse") : t("notInUse")}</span
                >
              </span>
            </div>
          </div>
        </div>
        <section
          class="pf-v6-c-data-list__expandable-content"
          ?hidden="${!isExpanded}"
        >
          <div class="pf-v6-c-data-list__expandable-content-body">
            <dl class="pf-v6-c-description-list">
              <div class="pf-v6-c-description-list__group">
                <dt class="pf-v6-c-description-list__term">${t("client")}</dt>
                <dd class="pf-v6-c-description-list__description">
                  ${app.clientId}
                </dd>
              </div>
              ${app.description
                ? html`
                    <div class="pf-v6-c-description-list__group">
                      <dt class="pf-v6-c-description-list__term">
                        ${t("clientDescription")}
                      </dt>
                      <dd class="pf-v6-c-description-list__description">
                        ${label(app.description)}
                      </dd>
                    </div>
                  `
                : nothing}
              <div class="pf-v6-c-description-list__group">
                <dt class="pf-v6-c-description-list__term">
                  ${t("applicationType")}
                </dt>
                <dd class="pf-v6-c-description-list__description">
                  ${this.#getAppType(app)}
                </dd>
              </div>
            </dl>
            ${app.consent?.grantedScopes?.length > 0
              ? html`
                  <div class="pf-v6-c-content">
                    <h4>${t("hasAccessTo")}</h4>
                    <ul>
                      ${app.consent.grantedScopes.map(
                        (scope) =>
                          html`<li>
                            ${label(scope.displayText, scope.name)}
                          </li>`,
                      )}
                    </ul>
                  </div>
                  ${app.consent.createdDate
                    ? html`<p>
                        ${t("accessGrantedOn")}:
                        ${new Date(
                          app.consent.createdDate,
                        ).toLocaleDateString()}
                      </p>`
                    : nothing}
                  <kc-button
                    label="${t("removeAccess")}"
                    variant="danger"
                    size="sm"
                    @click="${() => this.handleRevokeConsent(app.clientId)}"
                  ></kc-button>
                `
              : nothing}
          </div>
        </section>
      </li>
    `;
  }

  render() {
    return html`
      <div class="pf-v6-c-content">
        <h1>${t("applicationsHtmlTitle")}</h1>
        <p>${t("applicationsIntroMessage")}</p>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          ${this.loading
            ? html`<kc-spinner centered size="xl"></kc-spinner>`
            : this.applications.length === 0
              ? html`<kc-empty-state
                  body="${t("noApplications")}"
                ></kc-empty-state>`
              : html`
                  <ul class="pf-v6-c-data-list pf-m-compact" role="list">
                    ${this.applications.map((app) => this.#renderApp(app))}
                  </ul>
                `}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-applications", KcApplications);
