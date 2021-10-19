export default class RequiredActions {
  private toId(name: string) {
    return name.replace(/\s/g, "\\ ");
  }
  private getEnabled(name: string) {
    return `#enable-${this.toId(name)}`;
  }
  private getDefault(name: string) {
    return `#default-${this.toId(name)}`;
  }

  goToTab() {
    cy.get("#pf-tab-requiredActions-requiredActions").click();
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
    cy.get("#" + this.toId(from))
      .trigger("dragstart", {
        dataTransfer: new DataTransfer(),
      })
      .trigger("dragleave");

    cy.get("#" + this.toId(to))
      .trigger("dragenter")
      .trigger("dragover")
      .trigger("drop")
      .trigger("dragend");

    return this;
  }
}
