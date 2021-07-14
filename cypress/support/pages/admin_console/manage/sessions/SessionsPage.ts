export default class SessionsPage {
  sessionTypeDrpDwn: string;
  sessionTypeList: string;
  allSessionTypesOption: string;
  regularSSOOption: string;
  offlineOption: string;
  directGrantOption: string;
  serviceAccountOption: string;
  selectedType: string;

  constructor() {
    this.sessionTypeDrpDwn = ".pf-c-select__toggle";
    this.sessionTypeList = ".pf-c-select__toggle + ul";
    this.allSessionTypesOption = "all-sessions-option";
    this.regularSSOOption = "regular-sso-option";
    this.offlineOption = "offline-option";
    this.directGrantOption = "direct-grant-option";
    this.serviceAccountOption = "service-account-option";
    this.selectedType = ".pf-c-select__toggle-text";
  }

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
    cy.getId(this.allSessionTypesOption).click();
    cy.get(this.selectedType).should('have.text', 'All session types');
  }

  selectRegularSSO() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.getId(this.regularSSOOption).click();
    cy.get(this.selectedType).should('have.text', 'Regular SSO');
  }

  selectOffline() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.getId(this.offlineOption).click();
    cy.get(this.selectedType).should('have.text', 'Offline');
  }

  selectDirectGrant() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.getId(this.directGrantOption).click();
    cy.get(this.selectedType).should('have.text', 'Direct grant');
  }

  selectServiceAccount() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.getId(this.serviceAccountOption).click();
    cy.get(this.selectedType).should('have.text', 'Service account');
  }
}
