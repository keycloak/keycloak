import { LitElement, html, nothing } from "lit";

const icons = {
  success: html`<path
    d="M512 0C229.232 0 0 229.232 0 512c0 282.784 229.232 512 512 512 282.784 0 512-229.216 512-512C1024 229.232 794.784 0 512 0zm0 961.008c-247.024 0-448-201.984-448-449.01 0-247.024 200.976-448 448-448s448 200.977 448 448-200.976 449.01-448 449.01zm204.336-636.352L415.935 626.944l-135.28-135.28c-12.496-12.496-32.752-12.496-45.264 0-12.496 12.496-12.496 32.752 0 45.248l158.384 158.4c12.496 12.48 32.752 12.48 45.264 0 1.44-1.44 2.673-3.009 3.793-4.64l318.784-320.753c12.48-12.496 12.48-32.752 0-45.263-12.512-12.496-32.768-12.496-45.28 0z"
  ></path>`,
  danger: html`<path
    d="M512 0C229.232 0 0 229.232 0 512c0 282.784 229.232 512 512 512 282.784 0 512-229.216 512-512C1024 229.232 794.784 0 512 0zm0 961.008c-247.024 0-448-201.984-448-449.01 0-247.024 200.976-448 448-448s448 200.977 448 448-200.976 449.01-448 449.01zm-47.056-160.528h96v-96h-96v96zm0-576h96v384h-96v-384z"
  ></path>`,
  warning: html`<path
    d="M512 0C229.232 0 0 229.232 0 512c0 282.784 229.232 512 512 512 282.784 0 512-229.216 512-512C1024 229.232 794.784 0 512 0zm0 961.008c-247.024 0-448-201.984-448-449.01 0-247.024 200.976-448 448-448s448 200.977 448 448-200.976 449.01-448 449.01zm-47.056-160.528h96v-96h-96v96zm0-576h96v384h-96v-384z"
  ></path>`,
  info: html`<path
    d="M512 0C229.232 0 0 229.232 0 512c0 282.784 229.232 512 512 512 282.784 0 512-229.216 512-512C1024 229.232 794.784 0 512 0zm0 961.008c-247.024 0-448-201.984-448-449.01 0-247.024 200.976-448 448-448s448 200.977 448 448-200.976 449.01-448 449.01zm-47.056-160.528h96v-96h-96v96zm0-576h96v384h-96v-384z"
  ></path>`,
};

const closeIcon = html`<path
  d="M242.72 256l100.07-100.07c12.28-12.28 12.28-32.19 0-44.48l-22.24-22.24c-12.28-12.28-32.19-12.28-44.48 0L176 189.28 75.93 89.21c-12.28-12.28-32.19-12.28-44.48 0L9.21 111.45c-12.28 12.28-12.28 32.19 0 44.48L109.28 256 9.21 356.07c-12.28 12.28-12.28 32.19 0 44.48l22.24 22.24c12.28 12.28 32.2 12.28 44.48 0L176 322.72l100.07 100.07c12.28 12.28 32.2 12.28 44.48 0l22.24-22.24c12.28-12.28 12.28-32.19 0-44.48L242.72 256z"
></path>`;

/**
 * Alert component
 *
 * @element kc-alert
 *
 * @prop {'success' | 'danger' | 'warning' | 'info'} variant - Alert variant
 * @prop {string} title - Alert title/message
 * @prop {string} description - Optional description text
 * @prop {boolean} inline - Use inline style (default: true)
 * @prop {boolean} closable - Show close button
 *
 * @slot action - Optional action button
 *
 * @fires close - When close button is clicked
 */
export class KcAlert extends LitElement {
  static properties = {
    variant: { type: String },
    title: { type: String },
    description: { type: String },
    inline: { type: Boolean },
    closable: { type: Boolean },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.variant = "info";
    this.title = "";
    this.description = "";
    this.inline = true;
    this.closable = false;
  }

  #handleClose = () => {
    this.dispatchEvent(new CustomEvent("close"));
  };

  render() {
    const icon = icons[this.variant] || icons.info;

    return html`
      <div
        class="pf-v6-c-alert pf-m-${this.variant}${this.inline
          ? " pf-m-inline"
          : ""}"
      >
        <div class="pf-v6-c-alert__icon">
          <svg
            class="pf-v6-svg"
            viewBox="0 0 1024 1024"
            fill="currentColor"
            aria-hidden="true"
            width="1em"
            height="1em"
          >
            ${icon}
          </svg>
        </div>
        <p class="pf-v6-c-alert__title">${this.title}</p>
        ${this.description
          ? html`<div class="pf-v6-c-alert__description">
              ${this.description}
            </div>`
          : nothing}
        <div class="pf-v6-c-alert__action">
          <slot name="action"></slot>
          ${this.closable
            ? html`
                <button
                  class="pf-v6-c-button pf-m-plain"
                  type="button"
                  aria-label="Close"
                  @click="${this.#handleClose}"
                >
                  <svg
                    class="pf-v6-svg"
                    viewBox="0 0 352 512"
                    fill="currentColor"
                    aria-hidden="true"
                    width="1em"
                    height="1em"
                  >
                    ${closeIcon}
                  </svg>
                </button>
              `
            : nothing}
        </div>
      </div>
    `;
  }
}

customElements.define("kc-alert", KcAlert);
