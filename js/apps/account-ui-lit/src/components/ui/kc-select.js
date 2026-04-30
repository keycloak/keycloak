import { LitElement, html, nothing } from "lit";

/**
 * @typedef {Object} SelectOption
 * @property {string} value - Option value
 * @property {string} label - Option display label
 */

/**
 * Select dropdown component
 *
 * @element kc-select
 *
 * @prop {string} name - Select name/id
 * @prop {string|string[]} value - Selected value(s)
 * @prop {SelectOption[]} options - Available options
 * @prop {boolean} multiple - Allow multiple selection
 * @prop {boolean} required - Whether field is required
 * @prop {boolean} disabled - Whether field is disabled
 * @prop {string} placeholder - Placeholder option text
 *
 * @fires change - When selection changes (detail: { value: string | string[] })
 */
export class KcSelect extends LitElement {
  static properties = {
    name: { type: String },
    value: { type: String },
    options: { type: Array },
    multiple: { type: Boolean },
    required: { type: Boolean },
    disabled: { type: Boolean },
    placeholder: { type: String },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.name = "";
    this.value = "";
    this.options = [];
    this.multiple = false;
    this.required = false;
    this.disabled = false;
    this.placeholder = "";
  }

  #handleChange = (e) => {
    e.stopPropagation();
    let newValue;
    if (this.multiple) {
      newValue = Array.from(e.target.selectedOptions).map((o) => o.value);
    } else {
      newValue = e.target.value;
    }
    this.dispatchEvent(
      new CustomEvent("change", { detail: { value: newValue }, bubbles: true }),
    );
  };

  render() {
    const currentValues = this.multiple
      ? Array.isArray(this.value)
        ? this.value
        : [this.value].filter(Boolean)
      : null;

    return html`
      <span
        class="pf-v6-c-form-control${this.required ? " pf-m-required" : ""}"
      >
        <select
          id="${this.name}"
          name="${this.name}"
          ?multiple="${this.multiple}"
          ?disabled="${this.disabled}"
          ?required="${this.required}"
          @change="${this.#handleChange}"
        >
          ${this.placeholder && !this.multiple && !this.required
            ? html`<option value="">${this.placeholder}</option>`
            : nothing}
          ${this.options.map(
            (opt) => html`
              <option
                value="${opt.value}"
                ?selected="${this.multiple
                  ? currentValues?.includes(opt.value)
                  : this.value === opt.value}"
              >
                ${opt.label}
              </option>
            `,
          )}
        </select>
      </span>
    `;
  }
}

customElements.define("kc-select", KcSelect);

/**
 * Radio button group component
 *
 * @element kc-radio-group
 *
 * @prop {string} name - Radio group name
 * @prop {string} value - Selected value
 * @prop {SelectOption[]} options - Available options
 * @prop {boolean} disabled - Whether field is disabled
 * @prop {boolean} inline - Display inline (default: true)
 *
 * @fires change - When selection changes (detail: { value: string })
 */
export class KcRadioGroup extends LitElement {
  static properties = {
    name: { type: String },
    value: { type: String },
    options: { type: Array },
    disabled: { type: Boolean },
    inline: { type: Boolean },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.name = "";
    this.value = "";
    this.options = [];
    this.disabled = false;
    this.inline = true;
  }

  #handleChange = (e, optValue) => {
    e.stopPropagation();
    this.dispatchEvent(
      new CustomEvent("change", { detail: { value: optValue }, bubbles: true }),
    );
  };

  render() {
    return html`
      <div
        class="pf-v6-c-form__group-control${this.inline ? " pf-m-inline" : ""}"
        role="radiogroup"
      >
        ${this.options.map(
          (opt) => html`
            <div class="pf-v6-c-radio">
              <input
                class="pf-v6-c-radio__input"
                type="radio"
                id="${this.name}-${opt.value}"
                name="${this.name}"
                value="${opt.value}"
                ?checked="${this.value === opt.value}"
                ?disabled="${this.disabled}"
                @change="${(e) => this.#handleChange(e, opt.value)}"
              />
              <label
                class="pf-v6-c-radio__label"
                for="${this.name}-${opt.value}"
              >
                ${opt.label}
              </label>
            </div>
          `,
        )}
      </div>
    `;
  }
}

customElements.define("kc-radio-group", KcRadioGroup);

/**
 * Checkbox group component for multi-select
 *
 * @element kc-checkbox-group
 *
 * @prop {string} name - Checkbox group name
 * @prop {string[]} value - Selected values
 * @prop {SelectOption[]} options - Available options
 * @prop {boolean} disabled - Whether field is disabled
 *
 * @fires change - When selection changes (detail: { value: string[] })
 */
export class KcCheckboxGroup extends LitElement {
  static properties = {
    name: { type: String },
    value: { type: Array },
    options: { type: Array },
    disabled: { type: Boolean },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.name = "";
    this.value = [];
    this.options = [];
    this.disabled = false;
  }

  #handleChange = (e, optValue, checked) => {
    e.stopPropagation();
    const currentValues = Array.isArray(this.value)
      ? this.value
      : [this.value].filter(Boolean);
    const newValues = checked
      ? [...currentValues, optValue]
      : currentValues.filter((v) => v !== optValue);
    this.dispatchEvent(
      new CustomEvent("change", {
        detail: { value: newValues },
        bubbles: true,
      }),
    );
  };

  render() {
    const currentValues = Array.isArray(this.value)
      ? this.value
      : [this.value].filter(Boolean);

    return html`
      <div class="pf-v6-c-form__group-control" role="group">
        ${this.options.map(
          (opt) => html`
            <div class="pf-v6-c-check">
              <input
                class="pf-v6-c-check__input"
                type="checkbox"
                id="${this.name}-${opt.value}"
                name="${this.name}"
                value="${opt.value}"
                ?checked="${currentValues.includes(opt.value)}"
                ?disabled="${this.disabled}"
                @change="${(e) =>
                  this.#handleChange(e, opt.value, e.target.checked)}"
              />
              <label
                class="pf-v6-c-check__label"
                for="${this.name}-${opt.value}"
              >
                ${opt.label}
              </label>
            </div>
          `,
        )}
      </div>
    `;
  }
}

customElements.define("kc-checkbox-group", KcCheckboxGroup);

/**
 * Single checkbox component
 *
 * @element kc-checkbox
 *
 * @prop {string} name - Checkbox name/id
 * @prop {string} label - Checkbox label
 * @prop {boolean} checked - Whether checked
 * @prop {boolean} disabled - Whether disabled
 *
 * @fires change - When checked state changes (detail: { checked: boolean })
 */
export class KcCheckbox extends LitElement {
  static properties = {
    name: { type: String },
    label: { type: String },
    checked: { type: Boolean },
    disabled: { type: Boolean },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.name = "";
    this.label = "";
    this.checked = false;
    this.disabled = false;
  }

  #handleChange = (e) => {
    e.stopPropagation();
    this.dispatchEvent(
      new CustomEvent("change", {
        detail: { checked: e.target.checked },
        bubbles: true,
      }),
    );
  };

  render() {
    return html`
      <div class="pf-v6-c-check">
        <input
          class="pf-v6-c-check__input"
          type="checkbox"
          id="${this.name}"
          name="${this.name}"
          ?checked="${this.checked}"
          ?disabled="${this.disabled}"
          @change="${this.#handleChange}"
        />
        <label class="pf-v6-c-check__label" for="${this.name}"
          >${this.label}</label
        >
      </div>
    `;
  }
}

customElements.define("kc-checkbox", KcCheckbox);
