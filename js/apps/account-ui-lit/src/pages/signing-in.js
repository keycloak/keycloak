import { LitElement, html, nothing } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import { getCredentials, deleteCredential } from "../api/methods.js";
import { t, label } from "../i18n.js";
import "../components/ui/index.js";

export class KcSigningIn extends LitElement {
  static properties = {
    keycloak: { state: true },
    credentials: { state: true },
    loading: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.credentials = [];
    this.loading = true;
    new ContextConsumer(this, {
      context: keycloakContext,
      subscribe: true,
      callback: async (value) => {
        this.keycloak = value;
        if (value) await this.loadCredentials();
      },
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    if (this.keycloak) await this.loadCredentials();
  }

  async loadCredentials() {
    if (!this.keycloak) return;

    try {
      this.loading = true;
      this.credentials = await getCredentials({ keycloak: this.keycloak });
    } catch (error) {
      console.error("Failed to load credentials:", error);
    } finally {
      this.loading = false;
    }
  }

  async handleDelete(credential) {
    if (!this.keycloak) return;

    try {
      await deleteCredential(this.keycloak, credential);
      await this.loadCredentials();
    } catch (error) {
      console.error("Failed to delete credential:", error);
    }
  }

  handleSetup(credential) {
    this.keycloak?.login({
      action: credential.createAction || credential.updateAction,
    });
  }

  #formatDate(timestamp) {
    if (!timestamp) return "";
    return new Date(timestamp).toLocaleDateString(undefined, {
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  }

  #renderCredential(credential, container) {
    const displayName = label(credential.userLabel, credential.type);

    return html`
      <li class="pf-v6-c-data-list__item">
        <div class="pf-v6-c-data-list__item-row">
          <div class="pf-v6-c-data-list__item-content">
            <div class="pf-v6-c-data-list__cell">
              <strong>${displayName}</strong>
              ${credential.createdDate
                ? html`<small
                    >${t("credentialCreatedDate")}:
                    ${this.#formatDate(credential.createdDate)}</small
                  >`
                : nothing}
            </div>
          </div>
          <div class="pf-v6-c-data-list__item-action">
            ${container.updateAction
              ? html`
                  <kc-button
                    label="${t("doSave")}"
                    variant="secondary"
                    size="sm"
                    @click="${() =>
                      this.handleSetup({
                        updateAction: container.updateAction,
                      })}"
                  ></kc-button>
                `
              : nothing}
            ${container.removeable
              ? html`
                  <kc-button
                    label="${t("doRemove")}"
                    variant="danger"
                    size="sm"
                    @click="${() => this.handleDelete(credential)}"
                  ></kc-button>
                `
              : nothing}
          </div>
        </div>
      </li>
    `;
  }

  #renderCategory(container) {
    const displayName = label(container.displayName, container.type);
    const hasCredentials = container.userCredentialMetadatas?.length > 0;

    return html`
      <div class="pf-v6-l-stack__item">
        <div
          class="pf-v6-l-flex pf-m-justify-content-space-between pf-m-align-items-center"
        >
          <h3 class="pf-v6-c-title pf-m-lg">${t(displayName)}</h3>
          ${container.createAction && !hasCredentials
            ? html`
                <kc-button
                  label="${t("authenticatorActionSetup")}"
                  variant="primary"
                  size="sm"
                  @click="${() =>
                    this.handleSetup({
                      createAction: container.createAction,
                    })}"
                ></kc-button>
              `
            : nothing}
        </div>
        ${hasCredentials
          ? html`
              <ul class="pf-v6-c-data-list pf-m-compact" role="list">
                ${container.userCredentialMetadatas.map((cred) =>
                  this.#renderCredential(cred.credential, container),
                )}
              </ul>
            `
          : html`<p>${t("noCredentials")}</p>`}
      </div>
    `;
  }

  render() {
    return html`
      <div class="pf-v6-c-content">
        <h1>${t("signingInSidebarTitle")}</h1>
        <p>${t("authenticatorSubMessage")}</p>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          ${this.loading
            ? html`<kc-spinner centered size="xl"></kc-spinner>`
            : this.credentials.length === 0
              ? html`<kc-empty-state
                  body="${t("noCredentials")}"
                ></kc-empty-state>`
              : html`
                  <div class="pf-v6-l-stack pf-m-gutter">
                    ${this.credentials.map((container) =>
                      this.#renderCategory(container),
                    )}
                  </div>
                `}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-signing-in", KcSigningIn);
