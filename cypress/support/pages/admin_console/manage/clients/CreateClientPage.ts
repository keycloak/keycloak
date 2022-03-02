export default class CreateClientPage {
  clientTypeDrpDwn = ".pf-c-select__toggle";
  clientTypeError = ".pf-c-select + div";
  clientTypeList = ".pf-c-select__toggle + ul";
  clientIdInput = "#kc-client-id";
  clientIdError = "#kc-client-id + div";
  clientNameInput = "#kc-name";
  clientDescriptionInput = "#kc-description";
  alwaysDisplayInConsoleSwitch =
    '[for="kc-always-display-in-console-switch"]  .pf-c-switch__toggle';
  frontchannelLogoutSwitch =
    '[for="kc-frontchannelLogout-switch"]  .pf-c-switch__toggle';
  clientAuthenticationSwitch = '[for="kc-authentication"] .pf-c-switch__toggle';
  standardFlowChkBx = "#kc-flow-standard";
  directAccessChkBx = "#kc-flow-direct";
  implicitFlowChkBx = "#kc-flow-implicit";
  serviceAccountRolesChkBx = "#kc-flow-service-account";

  continueBtn = ".pf-c-wizard__footer .pf-m-primary";
  backBtn = ".pf-c-wizard__footer .pf-m-secondary";
  cancelBtn = ".pf-c-wizard__footer .pf-m-link";

  //#region General Settings
  selectClientType(clientType: string) {
    cy.get(this.clientTypeDrpDwn).click();
    cy.get(this.clientTypeList).findByTestId(`option-${clientType}`).click();

    return this;
  }

  fillClientData(
    id: string,
    name = "",
    description = "",
    alwaysDisplay?: boolean,
    frontchannelLogout?: boolean
  ) {
    cy.get(this.clientIdInput).clear();

    if (id) {
      cy.get(this.clientIdInput).type(id);
    }

    if (name) {
      cy.get(this.clientNameInput).type(name);
    }

    if (description) {
      cy.get(this.clientDescriptionInput).type(description);
    }

    if (alwaysDisplay) {
      cy.get(this.alwaysDisplayInConsoleSwitch).click();
    }

    if (frontchannelLogout) {
      cy.get(this.frontchannelLogoutSwitch).click();
    }

    return this;
  }

  changeSwitches(switches: string[]) {
    for (const uiSwitch of switches) {
      cy.findByTestId(uiSwitch).check({ force: true });
    }
    return this;
  }

  checkClientIdRequiredMessage(exist = true) {
    cy.get(this.clientIdError).should((!exist ? "not." : "") + "exist");

    return this;
  }
  //#endregion

  //#region Capability config
  switchClientAuthentication() {
    cy.get(this.clientAuthenticationSwitch).click();

    return this;
  }

  clickStandardFlow() {
    cy.get(this.standardFlowChkBx).click();

    return this;
  }

  clickDirectAccess() {
    cy.get(this.directAccessChkBx).click();

    return this;
  }

  clickImplicitFlow() {
    cy.get(this.implicitFlowChkBx).click();

    return this;
  }

  clickServiceAccountRoles() {
    cy.get(this.serviceAccountRolesChkBx).click();

    return this;
  }
  //#endregion

  continue() {
    cy.get(this.continueBtn).click();

    return this;
  }

  back() {
    cy.get(this.backBtn).click();

    return this;
  }

  cancel() {
    cy.get(this.cancelBtn).click();

    return this;
  }
}
