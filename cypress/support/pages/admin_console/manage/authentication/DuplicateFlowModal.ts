export default class DuplicateFlowModal {
  private aliasInput = "alias";
  private descriptionInput = "description";
  private confirmButton = "confirm";

  fill(name?: string, description?: string) {
    if (name) {
      cy.getId(this.aliasInput).type(name);
      if (description) cy.get(this.descriptionInput).type(description);
    }

    cy.getId(this.confirmButton).click();
    return this;
  }
}
