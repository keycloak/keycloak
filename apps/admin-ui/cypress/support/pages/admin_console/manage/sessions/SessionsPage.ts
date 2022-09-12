export default class SessionsPage {
  sessionTypeList = ".pf-c-select__toggle + ul";
  allSessionTypesOption = "all-sessions-option";
  regularSSOOption = "regular-sso-option";
  offlineOption = "offline-option";
  directGrantOption = "direct-grant-option";
  serviceAccountOption = "service-account-option";
  selectedType = ".pf-c-select__toggle-text";
  revocationActionItem = "revocation";
  setToNowButton = "set-to-now-button";
  actionDropdown = "action-dropdown";
  clearNotBeforeButton = "clear-not-before-button";
  pushButton = "modal-test-connection-button";
  notBeforeInput = "not-before-input";
  logoutAll = "logout-all";
  logoutAllConfirm = "confirm";

  setToNow() {
    cy.findByTestId(this.actionDropdown).should("exist").click();
    cy.findByTestId(this.revocationActionItem).should("exist").click();
    cy.findByTestId(this.setToNowButton).should("exist").click();
  }

  checkNotBeforeValueExists() {
    cy.findByTestId(this.actionDropdown).should("exist").click();
    cy.findByTestId(this.revocationActionItem).should("exist").click();
    cy.findByTestId(this.notBeforeInput).should("not.have.value", "None");
  }

  clearNotBefore() {
    cy.findByTestId(this.actionDropdown).should("exist").click();
    cy.findByTestId(this.revocationActionItem).should("exist").click();
    cy.findByTestId(this.clearNotBeforeButton).should("exist").click();
  }

  checkNotBeforeCleared() {
    cy.findByTestId(this.actionDropdown).should("exist").click();
    cy.findByTestId(this.revocationActionItem).should("exist").click();
    cy.findByTestId(this.notBeforeInput).should("have.value", "None");
  }

  logoutAllSessions() {
    cy.findByTestId(this.actionDropdown).should("exist").click();
    cy.findByTestId(this.logoutAll).should("exist").click();
    cy.findByTestId(this.logoutAllConfirm).should("exist").click();
  }

  pushRevocation() {
    cy.findByTestId(this.actionDropdown).should("exist").click();
    cy.findByTestId(this.revocationActionItem).should("exist").click();
    cy.findByTestId(this.pushButton).should("exist").click();
  }
}
