import { LitElement, html, nothing } from "lit";
import { ContextConsumer } from "@lit/context";
import { keycloakContext } from "../keycloak-context.js";
import { getPersonalInfo, savePersonalInfo } from "../api/methods.js";
import { t, label } from "../i18n.js";
import { environment } from "../environment.js";
import "../components/ui/index.js";

export class KcPersonalInfo extends LitElement {
  static properties = {
    keycloak: { state: true },
    user: { state: true },
    loading: { state: true },
    saving: { state: true },
    alert: { state: true },
    formData: { state: true },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.keycloak = null;
    this.user = null;
    this.loading = true;
    this.saving = false;
    this.alert = null;
    this.formData = {};
    new ContextConsumer(this, {
      context: keycloakContext,
      subscribe: true,
      callback: async (value) => {
        this.keycloak = value;
        if (value) await this.loadUserInfo();
      },
    });
  }

  async connectedCallback() {
    super.connectedCallback();
    if (this.keycloak) await this.loadUserInfo();
  }

  async loadUserInfo() {
    if (!this.keycloak) return;

    try {
      this.loading = true;
      this.user = await getPersonalInfo({ keycloak: this.keycloak });
      this.initFormData();
    } catch (error) {
      console.error("Failed to load user info:", error);
    } finally {
      this.loading = false;
    }
  }

  initFormData() {
    if (!this.user) return;

    this.formData = {
      username: this.user.username || "",
      email: this.user.email || "",
      firstName: this.user.firstName || "",
      lastName: this.user.lastName || "",
    };

    const attributes = this.user?.userProfileMetadata?.attributes || [];
    const attrMap = new Map(attributes.map((a) => [a.name, a]));

    if (this.user.attributes) {
      Object.entries(this.user.attributes).forEach(([key, value]) => {
        const attr = attrMap.get(key);
        const isMulti =
          attr?.multivalued ||
          attr?.annotations?.inputType === "multiselect" ||
          attr?.annotations?.inputType === "multiselect-checkboxes";

        if (isMulti) {
          this.formData[key] = Array.isArray(value) ? value : [value];
        } else {
          this.formData[key] = Array.isArray(value) ? value[0] : value;
        }
      });
    }
  }

  handleInputChange(field, value) {
    this.formData = { ...this.formData, [field]: value };
  }

  async handleSave() {
    if (!this.keycloak || !this.user) return;

    try {
      this.saving = true;

      const rootAttributes = ["username", "email", "firstName", "lastName"];
      const attributes = {};

      Object.entries(this.formData).forEach(([key, value]) => {
        if (!rootAttributes.includes(key)) {
          attributes[key] = Array.isArray(value) ? value : [value];
        }
      });

      const updatedUser = {
        ...this.user,
        username: this.formData.username,
        email: this.formData.email,
        firstName: this.formData.firstName,
        lastName: this.formData.lastName,
        attributes,
      };

      await savePersonalInfo(this.keycloak, updatedUser);
      this.alert = { type: "success", message: t("accountUpdatedMessage") };
      await this.keycloak.updateToken(-1);
    } catch (error) {
      console.error("Failed to save user info:", error);
      this.alert = { type: "danger", message: t("accountUpdatedError") };
    } finally {
      this.saving = false;
    }
  }

  handleCancel() {
    this.initFormData();
    this.alert = undefined;
  }

  handleDeleteAccount() {
    this.keycloak?.login({ action: "delete_account" });
  }

  #getInputType(attr) {
    const inputType = attr.annotations?.inputType;
    if (inputType?.startsWith("html5-")) {
      return inputType.substring(6);
    }
    return inputType || "text";
  }

  #getOptions(attr) {
    return attr.validators?.options?.options || [];
  }

  #getOptionLabel(attr, option) {
    const optionLabels = attr.annotations?.inputOptionLabels;
    const prefix = attr.annotations?.inputOptionLabelsI18nPrefix;
    if (optionLabels?.[option]) {
      return label(optionLabels[option], option);
    }
    if (prefix) {
      return t(`${prefix}.${option}`) || option;
    }
    return option;
  }

  #renderTextInput(attr, value) {
    return html`
      <kc-text-input
        name="${attr.name}"
        .value="${value}"
        type="${this.#getInputType(attr)}"
        placeholder="${attr.annotations?.inputTypePlaceholder || ""}"
        ?required="${attr.required}"
        ?disabled="${attr.readOnly}"
        @input="${(e) => this.handleInputChange(attr.name, e.detail.value)}"
      ></kc-text-input>
    `;
  }

  #renderTextarea(attr, value) {
    return html`
      <kc-textarea
        name="${attr.name}"
        .value="${value}"
        placeholder="${attr.annotations?.inputTypePlaceholder || ""}"
        rows="${attr.annotations?.inputTypeRows || 3}"
        cols="${attr.annotations?.inputTypeCols || 20}"
        ?required="${attr.required}"
        ?disabled="${attr.readOnly}"
        @input="${(e) => this.handleInputChange(attr.name, e.detail.value)}"
      ></kc-textarea>
    `;
  }

  #renderSelect(attr, value) {
    const options = this.#getOptions(attr).map((opt) => ({
      value: opt,
      label: this.#getOptionLabel(attr, opt),
    }));
    const isMulti = attr.annotations?.inputType === "multiselect";

    return html`
      <kc-select
        name="${attr.name}"
        .value="${value}"
        .options="${options}"
        ?multiple="${isMulti}"
        ?required="${attr.required}"
        ?disabled="${attr.readOnly}"
        placeholder="${!isMulti && !attr.required ? t("selectOne") : ""}"
        @change="${(e) => this.handleInputChange(attr.name, e.detail.value)}"
      ></kc-select>
    `;
  }

  #renderRadioButtons(attr, value) {
    const options = this.#getOptions(attr).map((opt) => ({
      value: opt,
      label: this.#getOptionLabel(attr, opt),
    }));

    return html`
      <kc-radio-group
        name="${attr.name}"
        value="${value}"
        .options="${options}"
        ?disabled="${attr.readOnly}"
        @change="${(e) => this.handleInputChange(attr.name, e.detail.value)}"
      ></kc-radio-group>
    `;
  }

  #renderCheckboxes(attr, value) {
    const options = this.#getOptions(attr).map((opt) => ({
      value: opt,
      label: this.#getOptionLabel(attr, opt),
    }));
    const currentValues = Array.isArray(value)
      ? value
      : [value].filter(Boolean);

    return html`
      <kc-checkbox-group
        name="${attr.name}"
        .value="${currentValues}"
        .options="${options}"
        ?disabled="${attr.readOnly}"
        @change="${(e) => this.handleInputChange(attr.name, e.detail.value)}"
      ></kc-checkbox-group>
    `;
  }

  renderField(attr) {
    const value = this.formData[attr.name] || attr.defaultValue || "";
    const fieldLabel = label(attr.displayName, attr.name);
    const inputType = attr.annotations?.inputType || "text";

    let fieldContent;
    switch (inputType) {
      case "textarea":
        fieldContent = this.#renderTextarea(attr, value);
        break;
      case "select":
      case "multiselect":
        fieldContent = this.#renderSelect(attr, value);
        break;
      case "select-radiobuttons":
        fieldContent = this.#renderRadioButtons(attr, value);
        break;
      case "multiselect-checkboxes":
        fieldContent = this.#renderCheckboxes(attr, value);
        break;
      default:
        fieldContent = this.#renderTextInput(attr, value);
    }

    if (["select-radiobuttons", "multiselect-checkboxes"].includes(inputType)) {
      return html`
        <div class="pf-v6-c-form__group">
          <div class="pf-v6-c-form__group-label">
            <label class="pf-v6-c-form__label" for="${attr.name}">
              <span class="pf-v6-c-form__label-text">${fieldLabel}</span
              >${attr.required
                ? html`&nbsp;<span
                      class="pf-v6-c-form__label-required"
                      aria-hidden="true"
                      >&#42;</span
                    >`
                : nothing}
            </label>
          </div>
          ${fieldContent}
        </div>
      `;
    }

    return html`
      <kc-form-group
        name="${attr.name}"
        label="${fieldLabel}"
        ?required="${attr.required}"
        .content="${fieldContent}"
      ></kc-form-group>
    `;
  }

  #renderAlert() {
    if (!this.alert) return nothing;

    return html`
      <kc-alert
        variant="${this.alert.type}"
        title="${this.alert.message}"
      ></kc-alert>
    `;
  }

  renderForm() {
    const attributes = this.user?.userProfileMetadata?.attributes || [];
    const displayAttributes = attributes.length
      ? attributes
      : [
          { name: "username", displayName: t("username"), readOnly: true },
          { name: "email", displayName: t("email"), required: true },
          { name: "firstName", displayName: t("firstName") },
          { name: "lastName", displayName: t("lastName") },
        ];

    const allReadOnly = displayAttributes.every((attr) => attr.readOnly);

    return html`
      ${this.#renderAlert()}

      <form
        class="pf-v6-c-form pf-m-horizontal"
        novalidate
        @submit="${(e) => e.preventDefault()}"
      >
        ${displayAttributes.map((attr) => this.renderField(attr))}
        ${!allReadOnly
          ? html`
              <div class="pf-v6-c-form__group pf-m-action">
                <div class="pf-v6-c-form__group-control">
                  <div class="pf-v6-c-form__actions">
                    <kc-button
                      label="${t("doSave")}"
                      variant="primary"
                      type="submit"
                      ?loading="${this.saving}"
                      @click="${() => this.handleSave()}"
                    ></kc-button>
                    <kc-button
                      label="${t("doCancel")}"
                      variant="link"
                      @click="${() => this.handleCancel()}"
                    ></kc-button>
                  </div>
                </div>
              </div>
            `
          : nothing}
      </form>

      ${environment.features.deleteAccountAllowed
        ? html`
            <kc-alert variant="warning" title="${t("deleteAccount")}">
              <kc-button
                slot="action"
                label="${t("doRemove")}"
                variant="danger"
                @click="${() => this.handleDeleteAccount()}"
              ></kc-button>
            </kc-alert>
          `
        : nothing}
    `;
  }

  render() {
    return html`
      <div class="pf-v6-c-content">
        <h1>${t("personalInfoHtmlTitle")}</h1>
        <p>${t("personalInfoIntroMessage")}</p>
      </div>
      <div class="pf-v6-c-card">
        <div class="pf-v6-c-card__body">
          ${this.loading ? html`<kc-spinner></kc-spinner>` : this.renderForm()}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-personal-info", KcPersonalInfo);
