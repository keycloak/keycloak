import { LitElement, html, nothing } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import { getDevices, deleteSession } from "../api/methods.js";
import { t, label } from "../i18n.js";
import "../components/ui/index.js";

export class KcDeviceActivity extends LitElement {
  static properties = {
    keycloak: { state: true },
    devices: { state: true },
    loading: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.devices = [];
    this.loading = true;
    new ContextConsumer(this, {
      context: keycloakContext,
      subscribe: true,
      callback: async (value) => {
        this.keycloak = value;
        if (value) await this.loadDevices();
      },
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    if (this.keycloak) await this.loadDevices();
  }

  async loadDevices() {
    if (!this.keycloak) return;

    try {
      this.loading = true;
      const devices = await getDevices({ keycloak: this.keycloak });
      this.devices = this.#sortDevices(devices);
    } catch (error) {
      console.error("Failed to load devices:", error);
    } finally {
      this.loading = false;
    }
  }

  #sortDevices(devices) {
    if (!devices || devices.length === 0) return [];

    const currentIdx = devices.findIndex((d) => d.current);
    if (currentIdx > 0) {
      const [current] = devices.splice(currentIdx, 1);
      devices.unshift(current);
    }

    if (devices[0]?.sessions) {
      const sessionIdx = devices[0].sessions.findIndex((s) => s.current);
      if (sessionIdx > 0) {
        const [currentSession] = devices[0].sessions.splice(sessionIdx, 1);
        devices[0].sessions.unshift(currentSession);
      }
    }

    return devices;
  }

  async handleSignOut(sessionId) {
    if (!this.keycloak) return;

    try {
      await deleteSession(this.keycloak, sessionId);
      await this.loadDevices();
    } catch (error) {
      console.error("Failed to sign out session:", error);
    }
  }

  async handleSignOutAll() {
    if (!this.keycloak) return;

    try {
      await deleteSession(this.keycloak);
      this.keycloak.logout();
    } catch (error) {
      console.error("Failed to sign out all sessions:", error);
    }
  }

  #formatDate(timestamp) {
    const date = new Date(timestamp * 1000);
    return date.toLocaleString(undefined, {
      dateStyle: "long",
      timeStyle: "short",
    });
  }

  #getDeviceName(device, session) {
    const os = device.os?.toLowerCase().includes("unknown")
      ? t("unknownOperatingSystem")
      : device.os;
    const osVersion = device.osVersion?.toLowerCase().includes("unknown")
      ? ""
      : device.osVersion;

    return `${os}${osVersion ? ` ${osVersion}` : ""} / ${session.browser}`;
  }

  #makeClientsString(clients) {
    if (!clients || clients.length === 0) return "";

    return clients
      .map((client) =>
        client.clientName
          ? label(client.clientName, client.clientId)
          : client.clientId,
      )
      .join(", ");
  }

  #hasMultipleSessions() {
    if (!this.devices) return false;
    return (
      this.devices.length > 1 || (this.devices[0]?.sessions?.length ?? 0) > 1
    );
  }

  #renderSession(device, session) {
    return html`
      <li class="pf-v6-c-data-list__item">
        <div class="pf-v6-c-data-list__item-row">
          <div class="pf-v6-c-data-list__item-content">
            <div class="pf-v6-c-data-list__cell pf-m-icon">
              <svg
                viewBox="0 0 576 512"
                fill="currentColor"
                width="24"
                height="24"
                aria-hidden="true"
              >
                ${device.mobile
                  ? html`<path
                      d="M16 64C16 28.7 44.7 0 80 0H304c35.3 0 64 28.7 64 64V448c0 35.3-28.7 64-64 64H80c-35.3 0-64-28.7-64-64V64zM144 448c0 8.8 7.2 16 16 16h64c8.8 0 16-7.2 16-16s-7.2-16-16-16H160c-8.8 0-16 7.2-16 16zM304 64H80V384H304V64z"
                    />`
                  : html`<path
                      d="M64 0C28.7 0 0 28.7 0 64V352c0 35.3 28.7 64 64 64H240l-10.7 32H160c-17.7 0-32 14.3-32 32s14.3 32 32 32H416c17.7 0 32-14.3 32-32s-14.3-32-32-32H346.7L336 416H512c35.3 0 64-28.7 64-64V64c0-35.3-28.7-64-64-64H64zM512 64V352H64V64H512z"
                    />`}
              </svg>
            </div>
            <div class="pf-v6-c-data-list__cell">
              <span class="pf-v6-c-data-list__cell-text">
                <strong>${this.#getDeviceName(device, session)}</strong>
                ${session.current
                  ? html`<span class="pf-v6-c-label pf-m-green pf-m-outline"
                      ><span class="pf-v6-c-label__content"
                        >${t("currentSession")}</span
                      ></span
                    >`
                  : nothing}
              </span>
              <dl class="pf-v6-c-description-list pf-m-horizontal pf-m-compact">
                <div class="pf-v6-c-description-list__group">
                  <dt class="pf-v6-c-description-list__term">
                    ${t("ipAddress")}
                  </dt>
                  <dd class="pf-v6-c-description-list__description">
                    ${session.ipAddress}
                  </dd>
                </div>
                <div class="pf-v6-c-description-list__group">
                  <dt class="pf-v6-c-description-list__term">
                    ${t("lastAccessedOn")}
                  </dt>
                  <dd class="pf-v6-c-description-list__description">
                    ${this.#formatDate(session.lastAccess)}
                  </dd>
                </div>
                <div class="pf-v6-c-description-list__group">
                  <dt class="pf-v6-c-description-list__term">
                    ${t("clients")}
                  </dt>
                  <dd class="pf-v6-c-description-list__description">
                    ${this.#makeClientsString(session.clients)}
                  </dd>
                </div>
                <div class="pf-v6-c-description-list__group">
                  <dt class="pf-v6-c-description-list__term">
                    ${t("started")}
                  </dt>
                  <dd class="pf-v6-c-description-list__description">
                    ${this.#formatDate(session.started)}
                  </dd>
                </div>
                <div class="pf-v6-c-description-list__group">
                  <dt class="pf-v6-c-description-list__term">
                    ${t("expires")}
                  </dt>
                  <dd class="pf-v6-c-description-list__description">
                    ${this.#formatDate(session.expires)}
                  </dd>
                </div>
              </dl>
            </div>
          </div>
          <div class="pf-v6-c-data-list__item-action">
            ${!session.current
              ? html`
                  <kc-button
                    label="${t("doSignOut")}"
                    variant="secondary"
                    @click="${() => this.handleSignOut(session.id)}"
                  ></kc-button>
                `
              : nothing}
          </div>
        </div>
      </li>
    `;
  }

  render() {
    const refreshIcon = html`
      <svg
        viewBox="0 0 512 512"
        fill="currentColor"
        width="1em"
        height="1em"
        aria-hidden="true"
      >
        <path
          d="M105.1 202.6c7.7-21.8 20.2-42.3 37.8-59.8c62.5-62.5 163.8-62.5 226.3 0L386.3 160H352c-17.7 0-32 14.3-32 32s14.3 32 32 32H463.5c0 0 0 0 0 0h.4c17.7 0 32-14.3 32-32V80c0-17.7-14.3-32-32-32s-32 14.3-32 32v35.2L414.4 97.6c-87.5-87.5-229.3-87.5-316.8 0C73.2 122 55.6 150.7 44.8 181.4c-5.9 16.7 2.9 34.9 19.5 40.8s34.9-2.9 40.8-19.5zM39 289.3c-5 1.5-9.8 4.2-13.7 8.2c-4 4-6.7 8.8-8.1 14c-.3 1.2-.6 2.5-.8 3.8c-.3 1.7-.4 3.4-.4 5.1V432c0 17.7 14.3 32 32 32s32-14.3 32-32V396.9l17.6 17.5 0 0c87.5 87.4 229.3 87.4 316.7 0c24.4-24.4 42.1-53.1 52.9-83.7c5.9-16.7-2.9-34.9-19.5-40.8s-34.9 2.9-40.8 19.5c-7.7 21.8-20.2 42.3-37.8 59.8c-62.5 62.5-163.8 62.5-226.3 0l-.1-.1L125 352H160c17.7 0 32-14.3 32-32s-14.3-32-32-32H48.4c-1.6 0-3.2 .1-4.8 .3s-3.1 .5-4.6 1z"
        />
      </svg>
    `;

    return html`
      <div class="pf-v6-c-content">
        <h1>${t("deviceActivityHtmlTitle")}</h1>
        <p>${t("signedInDevicesExplanation")}</p>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          ${this.loading
            ? html`<kc-spinner centered size="xl"></kc-spinner>`
            : this.devices.length === 0
              ? html`<kc-empty-state body="${t("noDevices")}"></kc-empty-state>`
              : html`
                  <div
                    class="pf-v6-l-flex pf-m-justify-content-space-between pf-m-align-items-center"
                  >
                    <h2 class="pf-v6-c-title pf-m-lg">
                      ${t("signedInDevices")}
                    </h2>
                    <kc-button
                      label="${t("refreshPage")}"
                      variant="link"
                      @click="${() => this.loadDevices()}"
                    >
                      <span slot="icon-start">${refreshIcon}</span>
                    </kc-button>
                  </div>
                  <ul class="pf-v6-c-data-list" role="list">
                    ${this.devices.flatMap((device) =>
                      device.sessions.map((session) =>
                        this.#renderSession(device, session),
                      ),
                    )}
                  </ul>
                  ${this.#hasMultipleSessions()
                    ? html`
                        <div class="pf-v6-l-flex pf-m-justify-content-flex-end">
                          <kc-button
                            label="${t("signOutAllDevices")}"
                            variant="danger"
                            @click="${() => this.handleSignOutAll()}"
                          ></kc-button>
                        </div>
                      `
                    : nothing}
                `}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-device-activity", KcDeviceActivity);
