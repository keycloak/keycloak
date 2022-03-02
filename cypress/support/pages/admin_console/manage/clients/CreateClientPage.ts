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
  clientAuthenticationSwitch =
    '[for="kc-authentication-switch"] > .pf-c-switch__toggle';
  clientAuthorizationSwitch =
    '[for="kc-authorization-switch"] > .pf-c-switch__toggle';
  standardFlowChkBx = "#kc-flow-standard";
  directAccessChkBx = "#kc-flow-direct";
  implicitFlowChkBx = "#kc-flow-implicit";
  oidcCibaGrantChkBx = "#kc-oidc-ciba-grant";
  deviceAuthGrantChkBx = "#kc-oauth-device-authorization-grant";
  serviceAccountRolesChkBx = "#kc-flow-service-account";

  saveBtn = "save";
  continueBtn = "next";
  backBtn = "back";
  cancelBtn = "cancel";

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

  checkGeneralSettingsStepActive() {
    cy.get(".pf-c-wizard__nav-link")
      .contains("General Settings")
      .should("have.class", "pf-m-current");

    return this;
  }
  //#endregion

  //#region Capability config
  switchClientAuthentication() {
    cy.get(this.clientAuthenticationSwitch).click();

    return this;
  }

  switchClientAuthorization() {
    cy.get(this.clientAuthorizationSwitch).click();

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

  clickOAuthDeviceAuthorizationGrant() {
    cy.get(this.deviceAuthGrantChkBx).click();

    return this;
  }

  clickOidcCibaGrant() {
    cy.get(this.oidcCibaGrantChkBx).click();

    return this;
  }
  //#endregion

  save() {
    cy.findByTestId(this.saveBtn).click();

    return this;
  }

  continue() {
    cy.findByTestId(this.continueBtn).click();

    return this;
  }

  back() {
    cy.findByTestId(this.backBtn).click();

    return this;
  }

  cancel() {
    cy.findByTestId(this.cancelBtn).click();

    return this;
  }
}
