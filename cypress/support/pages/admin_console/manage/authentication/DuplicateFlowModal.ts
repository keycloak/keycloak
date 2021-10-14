export default class DuplicateFlowModal {
  private aliasInput = "alias";
  private descriptionInput = "description";
  private confirmButton = "confirm";

  fill(name?: string, description?: string) {
    if (name) {
      cy.findByTestId(this.aliasInput).clear().type(name);
      if (description) cy.get(this.descriptionInput).type(description);
    }

    cy.findByTestId(this.confirmButton).click();
    return this;
  }
}
