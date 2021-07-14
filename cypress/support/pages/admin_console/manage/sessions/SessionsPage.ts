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
    this.allSessionTypesOption = '[data-testid="all-sessions-option"]';
    this.regularSSOOption = '[data-testid="regular-sso-option"';
    this.offlineOption = '[data-testid="offline-option"';
    this.directGrantOption = '[data-testid="direct-grant-option"';
    this.serviceAccountOption = '[data-testid="service-account-option"';
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
    cy.get(this.allSessionTypesOption).click();
    cy.get(this.selectedType).should('have.text', 'All session types');
  }

  selectRegularSSO() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.get(this.regularSSOOption).click();
    cy.get(this.selectedType).should('have.text', 'Regular SSO');
  }

  selectOffline() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.get(this.offlineOption).click();
    cy.get(this.selectedType).should('have.text', 'Offline');
  }

  selectDirectGrant() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.get(this.directGrantOption).click();
    cy.get(this.selectedType).should('have.text', 'Direct grant');
  }

  selectServiceAccount() {
    cy.get(this.sessionTypeDrpDwn).click();
    cy.get(this.serviceAccountOption).click();
    cy.get(this.selectedType).should('have.text', 'Service account');
  }
}
