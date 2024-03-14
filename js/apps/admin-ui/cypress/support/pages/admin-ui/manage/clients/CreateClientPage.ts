import Select from "../../../../forms/Select";
import CommonPage from "../../../CommonPage";

export default class CreateClientPage extends CommonPage {
  #clientTypeDrpDwn = "#protocol";
  #clientIdInput = "#clientId";
  #clientIdError = "#clientId + div";
  #clientNameInput = "#name";
  #clientDescriptionInput = "#kc-description";
  #alwaysDisplayInUISwitch =
    '[for="kc-always-display-in-ui-switch"] .pf-c-switch__toggle';
  #frontchannelLogoutSwitch =
    '[for="kc-frontchannelLogout-switch"] .pf-c-switch__toggle';

  #clientAuthenticationSwitch =
    '[for="kc-authentication-switch"] > .pf-c-switch__toggle';
  #clientAuthenticationSwitchInput = "#kc-authentication-switch";
  #clientAuthorizationSwitch =
    '[for="kc-authorization-switch"] > .pf-c-switch__toggle';
  #clientAuthorizationSwitchInput = "#kc-authorization-switch";
  #standardFlowChkBx = "#kc-flow-standard";
  #directAccessChkBx = "#kc-flow-direct";
  #implicitFlowChkBx = "#kc-flow-implicit";
  #oidcCibaGrantChkBx = "#kc-oidc-ciba-grant";
  #deviceAuthGrantChkBx = "#kc-oauth-device-authorization-grant";
  #serviceAccountRolesChkBx = "#kc-flow-service-account";

  #rootUrlInput = "rootUrl";
  #homeUrlInput = "baseUrl";
  #firstValidRedirectUrlInput = "redirectUris0";
  #firstWebOriginsInput = "webOrigins0";
  #adminUrlInput = "adminUrl";

  #loginThemeDrpDwn = "#login_theme";
  #loginThemeList = 'ul[aria-label="Login theme"]';
  #consentRequiredSwitch = '[for="kc-consent-switch"] > .pf-c-switch__toggle';
  #consentRequiredSwitchInput = "#kc-consent-switch";
  #displayClientOnScreenSwitch = '[for="kc-display-on-client-switch"]';
  #displayClientOnScreenSwitchInput = "#kc-display-on-client-switch";
  #clientConsentScreenText = "#kc-consent-screen-text";

  #frontChannelLogoutSwitch =
    '[for="kc-frontchannelLogout-switch"] > .pf-c-switch__toggle';
  #frontChannelLogoutSwitchInput = "#kc-frontchannelLogout-switch";
  #frontChannelLogoutInput = "frontchannelLogoutUrl";
  #backChannelLogoutInput = "backchannelLogoutUrl";
  #backChannelLogoutRequiredSwitchInput = "#backchannelLogoutSessionRequired";
  #backChannelLogoutRevoqueSwitch =
    '.pf-c-form__group-control [for="backchannelLogoutRevokeOfflineSessions"] > .pf-c-switch__toggle';
  #backChannelLogoutRevoqueSwitchInput =
    "#backchannelLogoutRevokeOfflineSessions";

  #actionDrpDwn = "action-dropdown";
  #deleteClientBtn = "delete-client";

  #saveBtn = "save";
  #continueBtn = "next";
  #backBtn = "back";
  #cancelBtn = "cancel";

  //#region General Settings
  selectClientType(clientType: string) {
    Select.selectItem(cy.get(this.#clientTypeDrpDwn), clientType);

    return this;
  }

  fillClientData(
    id: string,
    name = "",
    description = "",
    alwaysDisplay?: boolean,
    frontchannelLogout?: boolean,
  ) {
    cy.get(this.#clientIdInput).clear();

    if (id) {
      cy.get(this.#clientIdInput).type(id);
    }

    if (name) {
      cy.get(this.#clientNameInput).type(name);
    }

    if (description) {
      cy.get(this.#clientDescriptionInput).type(description);
    }

    if (alwaysDisplay) {
      cy.get(this.#alwaysDisplayInUISwitch).click();
    }

    if (frontchannelLogout) {
      cy.get(this.#frontchannelLogoutSwitch).click();
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
    cy.get(this.#clientIdError).should((!exist ? "not." : "") + "exist");

    return this;
  }

  checkGeneralSettingsStepActive() {
    cy.get(".pf-c-wizard__nav-link")
      .contains("General settings")
      .should("have.class", "pf-m-current");

    return this;
  }
  //#endregion

  //#region Capability config
  switchClientAuthentication() {
    cy.get(this.#clientAuthenticationSwitch).click();

    return this;
  }

  switchClientAuthorization() {
    cy.get(this.#clientAuthorizationSwitch).click();

    return this;
  }

  clickStandardFlow() {
    cy.get(this.#standardFlowChkBx).click();

    return this;
  }

  clickDirectAccess() {
    cy.get(this.#directAccessChkBx).click();

    return this;
  }

  clickImplicitFlow() {
    cy.get(this.#implicitFlowChkBx).click();

    return this;
  }

  clickServiceAccountRoles() {
    cy.get(this.#serviceAccountRolesChkBx).click();

    return this;
  }

  clickOAuthDeviceAuthorizationGrant() {
    cy.get(this.#deviceAuthGrantChkBx).click();

    return this;
  }

  clickOidcCibaGrant() {
    cy.get(this.#oidcCibaGrantChkBx).click();

    return this;
  }
  //#endregion

  save() {
    cy.findByTestId(this.#saveBtn).click();

    return this;
  }

  continue() {
    cy.findByTestId(this.#continueBtn).click();

    return this;
  }

  back() {
    cy.findByTestId(this.#backBtn).click();

    return this;
  }

  cancel() {
    cy.findByTestId(this.#cancelBtn).click();

    return this;
  }

  checkCapabilityConfigElements() {
    cy.get(this.#oidcCibaGrantChkBx).scrollIntoView();

    cy.get(this.#clientAuthenticationSwitchInput).should("not.be.disabled");
    cy.get(this.#clientAuthorizationSwitchInput).should("be.disabled");

    cy.get(this.#standardFlowChkBx).should("not.be.disabled");
    cy.get(this.#directAccessChkBx).should("not.be.disabled");
    cy.get(this.#implicitFlowChkBx).should("not.be.disabled");
    cy.get(this.#serviceAccountRolesChkBx).should("be.disabled");
    cy.get(this.#deviceAuthGrantChkBx).should("not.be.disabled");
    cy.get(this.#oidcCibaGrantChkBx).should("be.disabled");

    cy.get(this.#clientAuthenticationSwitch).click();
    cy.get(this.#clientAuthorizationSwitchInput).should("not.be.disabled");
    cy.get(this.#serviceAccountRolesChkBx).should("not.be.disabled");
    cy.get(this.#oidcCibaGrantChkBx).should("not.be.disabled");

    cy.get(this.#clientAuthorizationSwitch).click();
    cy.get(this.#serviceAccountRolesChkBx).should("be.disabled");
    cy.get(this.#oidcCibaGrantChkBx).should("not.be.disabled");

    cy.get(this.#clientAuthorizationSwitch).click();
    cy.get(this.#serviceAccountRolesChkBx).should("not.be.disabled");

    cy.get(this.#clientAuthenticationSwitch).click();
    cy.get(this.#serviceAccountRolesChkBx).should("be.disabled");
    cy.get(this.#oidcCibaGrantChkBx).should("be.disabled");

    return this;
  }

  checkAccessSettingsElements() {
    cy.findByTestId(this.#adminUrlInput).scrollIntoView();
    cy.findByTestId(this.#rootUrlInput).should("not.be.disabled");
    cy.findByTestId(this.#homeUrlInput).should("not.be.disabled");
    cy.findByTestId(this.#firstValidRedirectUrlInput).should("not.be.disabled");
    cy.findByTestId(this.#firstWebOriginsInput).should("not.be.disabled");
    cy.findByTestId(this.#adminUrlInput).should("not.be.disabled");

    return this;
  }

  checkLoginSettingsElements() {
    cy.get(this.#clientConsentScreenText).scrollIntoView();
    cy.get(this.#loginThemeDrpDwn).should("not.be.disabled");
    cy.get(this.#consentRequiredSwitchInput).should("not.be.disabled");
    cy.get(this.#displayClientOnScreenSwitchInput).should("be.disabled");
    cy.get(this.#clientConsentScreenText).should("be.disabled");

    cy.get(this.#loginThemeDrpDwn).click();
    cy.get(this.#loginThemeList).findByText("base").should("exist");
    cy.get(this.#loginThemeList).findByText("keycloak").should("exist");
    cy.get(this.#loginThemeDrpDwn).click();

    cy.get(this.#consentRequiredSwitch).click();
    cy.get(this.#displayClientOnScreenSwitchInput).should("not.be.disabled");
    cy.get(this.#clientConsentScreenText).should("be.disabled");

    cy.get(this.#displayClientOnScreenSwitch).click();
    cy.get(this.#clientConsentScreenText).should("not.be.disabled");

    cy.get(this.#displayClientOnScreenSwitch).click();
    cy.get(this.#clientConsentScreenText).should("be.disabled");
    cy.get(this.#consentRequiredSwitch).click();
    cy.get(this.#displayClientOnScreenSwitchInput).should("be.disabled");

    return this;
  }

  checkLogoutSettingsElements() {
    cy.get(this.#backChannelLogoutRevoqueSwitch).scrollIntoView();
    cy.get(this.#frontChannelLogoutSwitchInput).should("not.be.disabled");
    cy.findByTestId(this.#frontChannelLogoutInput).should("not.be.disabled");
    cy.findByTestId(this.#backChannelLogoutInput).should("not.be.disabled");
    cy.get(this.#backChannelLogoutRequiredSwitchInput).should(
      "not.be.disabled",
    );
    cy.get(this.#backChannelLogoutRevoqueSwitchInput).should("not.be.disabled");

    cy.get(this.#frontChannelLogoutSwitch).click();
    cy.findByTestId(this.#frontChannelLogoutInput).should("not.exist");
    cy.get(this.#frontChannelLogoutSwitch).click();
    cy.findByTestId(this.#frontChannelLogoutInput).should("not.be.disabled");

    return this;
  }

  deleteClientFromActionDropdown() {
    cy.findAllByTestId(this.#actionDrpDwn).click();
    cy.findAllByTestId(this.#deleteClientBtn).click();

    return this;
  }
}
