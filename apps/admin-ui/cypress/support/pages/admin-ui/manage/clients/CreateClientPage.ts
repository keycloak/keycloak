import CommonPage from "../../../CommonPage";

export default class CreateClientPage extends CommonPage {
  private clientTypeDrpDwn = ".pf-c-select__toggle";
  private clientTypeList = ".pf-c-select__toggle + ul";
  private clientIdInput = "#kc-client-id";
  private clientIdError = "#kc-client-id + div";
  private clientNameInput = "#kc-name";
  private clientDescriptionInput = "#kc-description";
  private alwaysDisplayInUISwitch =
    '[for="kc-always-display-in-ui-switch"] .pf-c-switch__toggle';
  private frontchannelLogoutSwitch =
    '[for="kc-frontchannelLogout-switch"] .pf-c-switch__toggle';

  private clientAuthenticationSwitch =
    '[for="kc-authentication-switch"] > .pf-c-switch__toggle';
  private clientAuthenticationSwitchInput = "#kc-authentication-switch";
  private clientAuthorizationSwitch =
    '[for="kc-authorization-switch"] > .pf-c-switch__toggle';
  private clientAuthorizationSwitchInput = "#kc-authorization-switch";
  private standardFlowChkBx = "#kc-flow-standard";
  private directAccessChkBx = "#kc-flow-direct";
  private implicitFlowChkBx = "#kc-flow-implicit";
  private oidcCibaGrantChkBx = "#kc-oidc-ciba-grant";
  private deviceAuthGrantChkBx = "#kc-oauth-device-authorization-grant";
  private serviceAccountRolesChkBx = "#kc-flow-service-account";

  private rootUrlInput = "#kc-root-url";
  private homeUrlInput = "#kc-home-url";
  private firstValidRedirectUrlInput = "redirectUris0";
  private firstWebOriginsInput = "webOrigins0";
  private adminUrlInput = "#kc-admin-url";

  private loginThemeDrpDwn = "#loginTheme";
  private loginThemeList = 'ul[aria-label="Login theme"]';
  private consentRequiredSwitch =
    '[for="kc-consent-switch"] > .pf-c-switch__toggle';
  private consentRequiredSwitchInput = "#kc-consent-switch";
  private displayClientOnScreenSwitch = '[for="kc-display-on-client-switch"]';
  private displayClientOnScreenSwitchInput = "#kc-display-on-client-switch";
  private clientConsentScreenText = "#kc-consent-screen-text";

  private frontChannelLogoutSwitch =
    '[for="kc-frontchannelLogout-switch"] > .pf-c-switch__toggle';
  private frontChannelLogoutSwitchInput = "#kc-frontchannelLogout-switch";
  private frontChannelLogoutInput = "#frontchannelLogoutUrl";
  private backChannelLogoutInput = "#backchannelLogoutUrl";
  private backChannelLogoutRequiredSwitchInput =
    "#backchannelLogoutSessionRequired";
  private backChannelLogoutRevoqueSwitch =
    '.pf-c-form__group-control [for="backchannelLogoutRevokeOfflineSessions"] > .pf-c-switch__toggle';
  private backChannelLogoutRevoqueSwitchInput =
    "#backchannelLogoutRevokeOfflineSessions";

  private actionDrpDwn = "action-dropdown";
  private deleteClientBtn = "delete-client";

  private saveBtn = "save";
  private continueBtn = "next";
  private backBtn = "back";
  private cancelBtn = "cancel";

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
      cy.get(this.alwaysDisplayInUISwitch).click();
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

  checkCapabilityConfigElements() {
    cy.get(this.oidcCibaGrantChkBx).scrollIntoView();

    cy.get(this.clientAuthenticationSwitchInput).should("not.be.disabled");
    cy.get(this.clientAuthorizationSwitchInput).should("be.disabled");

    cy.get(this.standardFlowChkBx).should("not.be.disabled");
    cy.get(this.directAccessChkBx).should("not.be.disabled");
    cy.get(this.implicitFlowChkBx).should("not.be.disabled");
    cy.get(this.serviceAccountRolesChkBx).should("be.disabled");
    cy.get(this.deviceAuthGrantChkBx).should("not.be.disabled");
    cy.get(this.oidcCibaGrantChkBx).should("be.disabled");

    cy.get(this.clientAuthenticationSwitch).click();
    cy.get(this.clientAuthorizationSwitchInput).should("not.be.disabled");
    cy.get(this.serviceAccountRolesChkBx).should("not.be.disabled");
    cy.get(this.oidcCibaGrantChkBx).should("not.be.disabled");

    cy.get(this.clientAuthorizationSwitch).click();
    cy.get(this.serviceAccountRolesChkBx).should("be.disabled");
    cy.get(this.oidcCibaGrantChkBx).should("not.be.disabled");

    cy.get(this.clientAuthorizationSwitch).click();
    cy.get(this.serviceAccountRolesChkBx).should("not.be.disabled");

    cy.get(this.clientAuthenticationSwitch).click();
    cy.get(this.serviceAccountRolesChkBx).should("be.disabled");
    cy.get(this.oidcCibaGrantChkBx).should("be.disabled");

    return this;
  }

  checkAccessSettingsElements() {
    cy.get(this.adminUrlInput).scrollIntoView();
    cy.get(this.rootUrlInput).should("not.be.disabled");
    cy.get(this.homeUrlInput).should("not.be.disabled");
    cy.findByTestId(this.firstValidRedirectUrlInput).should("not.be.disabled");
    cy.findByTestId(this.firstWebOriginsInput).should("not.be.disabled");
    cy.get(this.adminUrlInput).should("not.be.disabled");

    return this;
  }

  checkLoginSettingsElements() {
    cy.get(this.clientConsentScreenText).scrollIntoView();
    cy.get(this.loginThemeDrpDwn).should("not.be.disabled");
    cy.get(this.consentRequiredSwitchInput).should("not.be.disabled");
    cy.get(this.displayClientOnScreenSwitchInput).should("be.disabled");
    cy.get(this.clientConsentScreenText).should("be.disabled");

    cy.get(this.loginThemeDrpDwn).click();
    cy.get(this.loginThemeList).findByText("base").should("exist");
    cy.get(this.loginThemeList).findByText("keycloak").should("exist");
    cy.get(this.loginThemeDrpDwn).click();

    cy.get(this.consentRequiredSwitch).click();
    cy.get(this.displayClientOnScreenSwitchInput).should("not.be.disabled");
    cy.get(this.clientConsentScreenText).should("be.disabled");

    cy.get(this.displayClientOnScreenSwitch).click();
    cy.get(this.clientConsentScreenText).should("not.be.disabled");

    cy.get(this.displayClientOnScreenSwitch).click();
    cy.get(this.clientConsentScreenText).should("be.disabled");
    cy.get(this.consentRequiredSwitch).click();
    cy.get(this.displayClientOnScreenSwitchInput).should("be.disabled");

    return this;
  }

  checkLogoutSettingsElements() {
    cy.get(this.backChannelLogoutRevoqueSwitch).scrollIntoView();
    cy.get(this.frontChannelLogoutSwitchInput).should("not.be.disabled");
    cy.get(this.frontChannelLogoutInput).should("not.be.disabled");
    cy.get(this.backChannelLogoutInput).should("not.be.disabled");
    cy.get(this.backChannelLogoutRequiredSwitchInput).should("not.be.disabled");
    cy.get(this.backChannelLogoutRevoqueSwitchInput).should("not.be.disabled");

    cy.get(this.frontChannelLogoutSwitch).click();
    cy.get(this.frontChannelLogoutInput).should("not.exist");
    cy.get(this.frontChannelLogoutSwitch).click();
    cy.get(this.frontChannelLogoutInput).should("not.be.disabled");

    return this;
  }

  deleteClientFromActionDropdown() {
    cy.findAllByTestId(this.actionDrpDwn).click();
    cy.findAllByTestId(this.deleteClientBtn).click();

    return this;
  }
}
