import { LitElement, html } from "lit";

/**
 * Button component
 *
 * @element kc-button
 *
 * @prop {string} label - Button text
 * @prop {'primary' | 'secondary' | 'tertiary' | 'danger' | 'warning' | 'link' | 'plain'} variant - Button variant
 * @prop {'sm' | 'lg'} size - Button size
 * @prop {boolean} disabled - Whether button is disabled
 * @prop {boolean} loading - Show loading state
 * @prop {'button' | 'submit' | 'reset'} type - Button type attribute
 * @prop {string} href - If set, renders as a link instead of button
 *
 * @slot icon-start - Icon before label
 * @slot icon-end - Icon after label
 *
 * @fires click - When button is clicked
 */
export class KcButton extends LitElement {
  static properties = {
    label: { type: String },
    variant: { type: String },
    size: { type: String },
    disabled: { type: Boolean },
    loading: { type: Boolean },
    type: { type: String },
    href: { type: String },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.label = "";
    this.variant = "primary";
    this.size = undefined;
    this.disabled = false;
    this.loading = false;
    this.type = "button";
    this.href = undefined;
  }

  #getClasses() {
    return [
      "pf-v6-c-button",
      `pf-m-${this.variant}`,
      this.size === "sm" ? "pf-m-small" : "",
      this.size === "lg" ? "pf-m-large" : "",
    ]
      .filter(Boolean)
      .join(" ");
  }

  render() {
    const content = html`
      <slot name="icon-start"></slot>
      ${this.loading ? "..." : this.label}
      <slot name="icon-end"></slot>
    `;

    if (this.href) {
      return html`
        <a class="${this.#getClasses()}" href="${this.href}">${content}</a>
      `;
    }

    return html`
      <button
        class="${this.#getClasses()}"
        type="${this.type}"
        ?disabled="${this.disabled || this.loading}"
      >
        ${content}
      </button>
    `;
  }
}

customElements.define("kc-button", KcButton);
