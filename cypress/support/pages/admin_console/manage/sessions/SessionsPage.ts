export default class SessionsPage {
  sessionTypeDrpDwn = ".pf-c-select__toggle";
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
  notBeforeInput = "not-before-input";
  logoutAll = "logout-all";
  logoutAllConfirm = "logout-all-confirm-button";

  shouldDisplay() {
    cy.get(this.sessionTypeDrpDwn).should("exist");
  }

  shouldNotBeEmpty() {
    cy.get(this.sessionTypeDrpDwn).should("exist").click();
    cy.get(this.sessionTypeList).should("exist");

    return this;
  }

  selectAllSessionsType() {
    cy.get(this.sessionTypeDrpDwn).should("exist").click();
    cy.findByTestId(this.allSessionTypesOption).should("exist").click();
    cy.get(this.selectedType).should("have.text", "All session types");
  }

  selectRegularSSO() {
    cy.get(this.sessionTypeDrpDwn).should("exist").click();
    cy.findByTestId(this.regularSSOOption).should("exist").click();
    cy.get(this.selectedType).should("have.text", "Regular SSO");
  }

  selectOffline() {
    cy.get(this.sessionTypeDrpDwn).should("exist").click();
    cy.findByTestId(this.offlineOption).should("exist").click();
    cy.get(this.selectedType).should("have.text", "Offline");
  }

  selectDirectGrant() {
    cy.get(this.sessionTypeDrpDwn).should("exist").click();
    cy.findByTestId(this.directGrantOption).should("exist").click();
    cy.get(this.selectedType).should("have.text", "Direct grant");
  }

  selectServiceAccount() {
    cy.get(this.sessionTypeDrpDwn).should("exist").click();
    cy.findByTestId(this.serviceAccountOption).should("exist").click();
    cy.get(this.selectedType).should("have.text", "Service account");
  }

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
}
