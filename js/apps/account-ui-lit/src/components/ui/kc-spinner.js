import { LitElement, html } from "lit";

/**
 * Loading spinner component
 *
 * @element kc-spinner
 *
 * @prop {'sm' | 'md' | 'lg' | 'xl'} size - Spinner size (default: 'md')
 * @prop {string} label - Accessibility label (default: 'Loading')
 * @prop {boolean} centered - Center in container using bullseye layout
 * @prop {boolean} fullHeight - Use full viewport height (only with centered)
 */
export class KcSpinner extends LitElement {
  static properties = {
    size: { type: String },
    label: { type: String },
    centered: { type: Boolean },
    fullHeight: { type: Boolean },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.size = "md";
    this.label = "Loading";
    this.centered = false;
    this.fullHeight = false;
  }

  #renderSpinner() {
    const sizeClass = this.size !== "md" ? `pf-m-${this.size}` : "";

    return html`
      <span
        class="pf-v6-c-spinner ${sizeClass}"
        role="progressbar"
        aria-label="${this.label}"
      >
        <span class="pf-v6-c-spinner__clipper"></span>
        <span class="pf-v6-c-spinner__lead-ball"></span>
        <span class="pf-v6-c-spinner__tail-ball"></span>
      </span>
    `;
  }

  render() {
    if (this.centered) {
      const style = this.fullHeight ? "min-height: 100vh;" : "";
      return html`
        <div class="pf-v6-l-bullseye" style="${style}">
          <div class="pf-v6-l-bullseye__item">${this.#renderSpinner()}</div>
        </div>
      `;
    }

    return this.#renderSpinner();
  }
}

customElements.define("kc-spinner", KcSpinner);
