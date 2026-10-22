class ExampleUiWidget extends HTMLElement {
  connectedCallback() {
    const realm = this.context?.realm ?? "unknown";
    this.textContent = `Example UI widget for realm: ${realm}`;
  }
}

customElements.define("example-ui-widget", ExampleUiWidget);
