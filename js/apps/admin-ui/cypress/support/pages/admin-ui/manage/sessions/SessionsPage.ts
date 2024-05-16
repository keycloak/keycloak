export default class SessionsPage {
  #revocationActionItem = "revocation";
  #setToNowButton = "set-to-now-button";
  #actionDropdown = "action-dropdown";
  #clearNotBeforeButton = "clear-not-before-button";
  #pushButton = "modal-test-connection-button";
  #notBeforeInput = "not-before-input";

  setToNow() {
    this.#openRevocationDialog();
    cy.findByTestId(this.#setToNowButton).click();
    Cypress.session.clearAllSavedSessions();
  }

  checkNotBeforeValueExists() {
    this.#openRevocationDialog();
    cy.findByTestId(this.#notBeforeInput).should("not.have.value", "None");
  }

  clearNotBefore() {
    this.#openRevocationDialog();
    cy.findByTestId(this.#clearNotBeforeButton).click();
  }

  checkNotBeforeCleared() {
    this.#openRevocationDialog();
    cy.findByTestId(this.#notBeforeInput).should("have.value", "None");
  }

  pushRevocation() {
    this.#openRevocationDialog();
    cy.findByTestId(this.#pushButton).click();
  }

  #openRevocationDialog() {
    cy.findByTestId(this.#actionDropdown).click();
    cy.intercept("/admin/realms/master").as("fetchRealm");
    cy.findByTestId(this.#revocationActionItem).click();
    cy.wait("@fetchRealm");
  }
}
