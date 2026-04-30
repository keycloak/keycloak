import { LitElement, html, nothing } from "lit";

/**
 * Text input component
 *
 * @element kc-text-input
 *
 * @prop {string} name - Input name/id
 * @prop {string} value - Input value
 * @prop {'text' | 'email' | 'password' | 'number' | 'tel' | 'url' | 'date' | 'time' | 'datetime-local'} type - Input type
 * @prop {string} placeholder - Placeholder text
 * @prop {boolean} required - Whether field is required
 * @prop {boolean} disabled - Whether field is disabled
 * @prop {boolean} readonly - Whether field is read-only
 *
 * @fires input - When input value changes
 * @fires change - When input loses focus after change
 */
export class KcTextInput extends LitElement {
  static properties = {
    name: { type: String },
    value: { type: String },
    type: { type: String },
    placeholder: { type: String },
    required: { type: Boolean },
    disabled: { type: Boolean },
    readonly: { type: Boolean },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.name = "";
    this.value = "";
    this.type = "text";
    this.placeholder = "";
    this.required = false;
    this.disabled = false;
    this.readonly = false;
  }

  #handleInput = (e) => {
    e.stopPropagation();
    this.value = e.target.value;
    this.dispatchEvent(
      new CustomEvent("input", {
        detail: { value: this.value },
        bubbles: true,
      }),
    );
  };

  #handleChange = (e) => {
    e.stopPropagation();
    this.dispatchEvent(
      new CustomEvent("change", {
        detail: { value: e.target.value },
        bubbles: true,
      }),
    );
  };

  render() {
    return html`
      <span
        class="pf-v6-c-form-control${this.required ? " pf-m-required" : ""}"
      >
        <input
          type="${this.type}"
          id="${this.name}"
          name="${this.name}"
          .value="${this.value}"
          placeholder="${this.placeholder}"
          ?disabled="${this.disabled}"
          ?readonly="${this.readonly}"
          ?required="${this.required}"
          @input="${this.#handleInput}"
          @change="${this.#handleChange}"
        />
      </span>
    `;
  }
}

customElements.define("kc-text-input", KcTextInput);

/**
 * Textarea component
 *
 * @element kc-textarea
 *
 * @prop {string} name - Textarea name/id
 * @prop {string} value - Textarea value
 * @prop {string} placeholder - Placeholder text
 * @prop {number} rows - Number of rows
 * @prop {number} cols - Number of columns
 * @prop {boolean} required - Whether field is required
 * @prop {boolean} disabled - Whether field is disabled
 * @prop {boolean} readonly - Whether field is read-only
 *
 * @fires input - When textarea value changes
 */
export class KcTextarea extends LitElement {
  static properties = {
    name: { type: String },
    value: { type: String },
    placeholder: { type: String },
    rows: { type: Number },
    cols: { type: Number },
    required: { type: Boolean },
    disabled: { type: Boolean },
    readonly: { type: Boolean },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.name = "";
    this.value = "";
    this.placeholder = "";
    this.rows = 3;
    this.cols = 20;
    this.required = false;
    this.disabled = false;
    this.readonly = false;
  }

  #handleInput = (e) => {
    e.stopPropagation();
    this.value = e.target.value;
    this.dispatchEvent(
      new CustomEvent("input", {
        detail: { value: this.value },
        bubbles: true,
      }),
    );
  };

  render() {
    return html`
      <span
        class="pf-v6-c-form-control${this.required ? " pf-m-required" : ""}"
      >
        <textarea
          id="${this.name}"
          name="${this.name}"
          .value="${this.value}"
          placeholder="${this.placeholder}"
          rows="${this.rows}"
          cols="${this.cols}"
          ?disabled="${this.disabled}"
          ?readonly="${this.readonly}"
          ?required="${this.required}"
          @input="${this.#handleInput}"
        ></textarea>
      </span>
    `;
  }
}

customElements.define("kc-textarea", KcTextarea);

/**
 * Form group component - wraps a form control with label
 *
 * @element kc-form-group
 *
 * @prop {string} name - Field name/id (for label's "for" attribute)
 * @prop {string} label - Label text
 * @prop {boolean} required - Whether to show required indicator
 * @prop {string} helperText - Optional helper text below the control
 * @prop {string} errorText - Optional error message
 * @prop {TemplateResult} content - The form control to render
 */
export class KcFormGroup extends LitElement {
  static properties = {
    name: { type: String },
    label: { type: String },
    required: { type: Boolean },
    helperText: { type: String },
    errorText: { type: String },
    content: { attribute: false },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.name = "";
    this.label = "";
    this.required = false;
    this.helperText = "";
    this.errorText = "";
    this.content = nothing;
  }

  render() {
    return html`
      <div class="pf-v6-c-form__group${this.errorText ? " pf-m-error" : ""}">
        <div class="pf-v6-c-form__group-label">
          <label class="pf-v6-c-form__label" for="${this.name}">
            <span class="pf-v6-c-form__label-text">${this.label}</span>
            ${this.required
              ? html`&nbsp;<span
                    class="pf-v6-c-form__label-required"
                    aria-hidden="true"
                    >&#42;</span
                  >`
              : nothing}
          </label>
        </div>
        <div class="pf-v6-c-form__group-control">
          ${this.content}
          ${this.helperText && !this.errorText
            ? html`<p class="pf-v6-c-form__helper-text">${this.helperText}</p>`
            : nothing}
          ${this.errorText
            ? html`<p class="pf-v6-c-form__helper-text pf-m-error">
                ${this.errorText}
              </p>`
            : nothing}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-form-group", KcFormGroup);
