export default class Form {
  static assertSaveButtonEnabled() {
    this.#getSaveButton().should("be.enabled");
  }

  static assertSaveButtonDisabled() {
    this.#getSaveButton().should("be.disabled");
  }

  static clickSaveButton() {
    this.#getSaveButton().click();
  }

  static #getSaveButton() {
    return cy.findByTestId("save");
  }
}
