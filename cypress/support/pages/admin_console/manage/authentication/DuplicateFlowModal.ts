export default class DuplicateFlowModal {
  private aliasInput = "alias";
  private descriptionInput = "description";
  private confirmButton = "confirm";
  private errorText = ".pf-m-error";

  fill(name?: string, description?: string) {
    cy.findByTestId(this.aliasInput).clear();
    if (name) {
      cy.findByTestId(this.aliasInput).type(name);
      if (description) cy.get(this.descriptionInput).type(description);
    }

    cy.findByTestId(this.confirmButton).click();
    return this;
  }

  shouldShowError(message: string) {
    cy.get(this.errorText).invoke("text").should("contain", message);
  }
}
