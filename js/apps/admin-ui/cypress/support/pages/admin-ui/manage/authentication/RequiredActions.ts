export default class RequiredActions {
  private toId(name: string) {
    return name.replace(/\s/g, "\\ ");
  }

  private toKey(name: string) {
    return name.replace(/\s/g, "-");
  }

  private getEnabled(name: string) {
    return `#enable-${this.toKey(name)}`;
  }
  private getDefault(name: string) {
    return `#default-${this.toKey(name)}`;
  }

  goToTab() {
    cy.findByTestId("requiredActions").click();
  }

  enableAction(name: string) {
    cy.get(this.getEnabled(name)).click({ force: true });
    return this;
  }

  isChecked(name: string) {
    cy.get(this.getEnabled(name)).should("be.checked");
    return this;
  }

  isDefaultEnabled(name: string) {
    cy.get(this.getDefault(name)).should("be.enabled");
    return this;
  }

  setAsDefault(name: string) {
    cy.get(this.getDefault(name)).click({ force: true });
    return this;
  }

  isDefaultChecked(name: string) {
    cy.get(this.getEnabled(name)).should("be.checked");
    return this;
  }

  moveRowTo(from: string, to: string) {
    cy.get("#" + this.toId(from)).drag("#" + this.toId(to));

    return this;
  }
}
