export default class RequiredActions {
  #toId(name: string) {
    return name.replace(/\s/g, "\\ ");
  }

  #toKey(name: string) {
    return name.replace(/\s/g, "-");
  }

  #getEnabledSwitch(name: string) {
    return `#enable-${this.#toKey(name)}`;
  }
  #getDefaultSwitch(name: string) {
    return `#default-${this.#toKey(name)}`;
  }

  goToTab() {
    cy.findByTestId("requiredActions").click();
  }

  switchAction(name: string) {
    cy.get(this.#getEnabledSwitch(name)).scrollIntoView();
    cy.get(this.#getEnabledSwitch(name)).click({ force: true });
    return this;
  }

  isChecked(name: string) {
    cy.get(this.#getEnabledSwitch(name)).should("be.checked");
    return this;
  }

  isDefaultEnabled(name: string) {
    cy.get(this.#getDefaultSwitch(name)).should("be.enabled");
    return this;
  }

  setAsDefault(name: string) {
    cy.get(this.#getDefaultSwitch(name)).click({ force: true });
    return this;
  }

  isDefaultChecked(name: string) {
    cy.get(this.#getEnabledSwitch(name)).should("be.checked");
    return this;
  }

  moveRowTo(from: string, to: string) {
    cy.get("#" + this.#toId(from)).drag("#" + this.#toId(to));

    return this;
  }
}
