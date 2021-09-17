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
    cy.get(this.sessionTypeDrpDwn).click();
    cy.get(this.sessionTypeList).should("exist");

    return this;
  }

  selectAllSessionsType() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.findByTestId(this.allSessionTypesOption).click();
    cy.get(this.selectedType).should("have.text", "All session types");
  }

  selectRegularSSO() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.findByTestId(this.regularSSOOption).click();
    cy.get(this.selectedType).should("have.text", "Regular SSO");
  }

  selectOffline() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.findByTestId(this.offlineOption).click();
    cy.get(this.selectedType).should("have.text", "Offline");
  }

  selectDirectGrant() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.findByTestId(this.directGrantOption).click();
    cy.get(this.selectedType).should("have.text", "Direct grant");
  }

  selectServiceAccount() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.findByTestId(this.serviceAccountOption).click();
    cy.get(this.selectedType).should("have.text", "Service account");
  }

  setToNow() {
    cy.findByTestId(this.actionDropdown).click();
    cy.findByTestId(this.revocationActionItem).click();
    cy.findByTestId(this.setToNowButton).click();
  }

  checkNotBeforeValueExists() {
    cy.findByTestId(this.actionDropdown).click();
    cy.findByTestId(this.revocationActionItem).click();
    cy.findByTestId(this.notBeforeInput).should("not.have.value", "None");
  }

  clearNotBefore() {
    cy.findByTestId(this.actionDropdown).click();
    cy.findByTestId(this.revocationActionItem).click();
    cy.findByTestId(this.clearNotBeforeButton).click();
  }

  checkNotBeforeCleared() {
    cy.findByTestId(this.actionDropdown).click();
    cy.findByTestId(this.revocationActionItem).click();
    cy.findByTestId(this.notBeforeInput).should("have.value", "None");
  }

  logoutAllSessions() {
    cy.findByTestId(this.actionDropdown).click();
    cy.findByTestId(this.logoutAll).click();
    cy.findByTestId(this.logoutAllConfirm).click();
  }
}
