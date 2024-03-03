export default class DuplicateFlowModal {
  #nameInput = "alias";
  #descriptionInput = "description";
  #confirmButton = "confirm";
  #errorText = ".pf-m-error";

  fill(name?: string, description?: string) {
    cy.findByTestId(this.#nameInput).clear();
    if (name) {
      cy.findByTestId(this.#nameInput).type(name);
      if (description)
        cy.findByTestId(this.#descriptionInput).type(description);
    }

    cy.findByTestId(this.#confirmButton).click();
    return this;
  }

  shouldShowError(message: string) {
    cy.get(this.#errorText).invoke("text").should("contain", message);
  }
}
