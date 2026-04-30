import { LitElement, html, nothing } from "lit";

/**
 * Empty state component
 *
 * @element kc-empty-state
 *
 * @prop {string} title - Empty state title
 * @prop {string} body - Optional body text
 *
 * @slot icon - Optional icon
 * @slot primary-action - Optional primary action button
 * @slot secondary-action - Optional secondary action
 */
export class KcEmptyState extends LitElement {
  static properties = {
    title: { type: String },
    body: { type: String },
  };

  createRenderRoot() {
    this.innerHTML = "";
    return this;
  }

  constructor() {
    super();
    this.title = "";
    this.body = "";
  }

  render() {
    return html`
      <div class="pf-v6-c-empty-state">
        <div class="pf-v6-c-empty-state__icon">
          <slot name="icon"></slot>
        </div>
        <div class="pf-v6-c-empty-state__content">
          ${this.title
            ? html`
                <div class="pf-v6-c-empty-state__header">
                  <h1 class="pf-v6-c-empty-state__title-text">${this.title}</h1>
                </div>
              `
            : nothing}
          ${this.body
            ? html`<div class="pf-v6-c-empty-state__body">${this.body}</div>`
            : nothing}
          <div class="pf-v6-c-empty-state__footer">
            <div class="pf-v6-c-empty-state__actions">
              <slot name="primary-action"></slot>
            </div>
            <div class="pf-v6-c-empty-state__actions pf-m-secondary">
              <slot name="secondary-action"></slot>
            </div>
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define("kc-empty-state", KcEmptyState);
