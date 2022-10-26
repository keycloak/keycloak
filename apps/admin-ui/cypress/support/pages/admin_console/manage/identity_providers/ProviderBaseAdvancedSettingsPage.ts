import PageObject from "../../components/PageObject";
import Masthead from "../../Masthead";

const masthead = new Masthead();

export enum LoginFlowOption {
  none = "None",
  browser = "browser",
  directGrant = "direct grant",
  registration = "registration",
  resetCredentials = "reset credentials",
  firstBrokerLogin = "first broker login",
  dockerAuth = "docker auth",
  httpChallenge = "http challenge",
}

export enum SyncModeOption {
  import = "Import",
  legacy = "Legacy",
  force = "Force",
}

export enum PromptSelect {
  unspecified = "Unspecified",
  none = "None",
  consent = "Consent",
  login = "Login",
  select = "Select account",
}

export enum ClientAuthentication {
  post = "Client secret sent as basic auth",
  basicAuth = "Client secret as jwt",
  jwt = "JWT signed with private key",
  jwtPrivKey = "Client secret sent as post",
}

export default class ProviderBaseGeneralSettingsPage extends PageObject {
  private scopesInput = "#scopes";
  private storeTokensSwitch = "#storeTokens";
  private storedTokensReadable = "#storedTokensReadable";
  private acceptsPromptNoneForwardFromClientSwitch = "#acceptsPromptNone";
  private advancedSettingsToggle = ".pf-c-expandable-section__toggle";
  private passLoginHintSwitch = "#passLoginHint";
  private passMaxAgeSwitch = "#passMaxAge";
  private passCurrentLocaleSwitch = "#passCurrentLocale";
  private backchannelLogoutSwitch = "#backchannelLogout";
  private promptSelect = "#prompt";
  private disableUserInfoSwitch = "#disableUserInfo";
  private trustEmailSwitch = "#trustEmail";
  private accountLinkingOnlySwitch = "#accountLinkingOnly";
  private hideOnLoginPageSwitch = "#hideOnLoginPage";
  private firstLoginFlowSelect = "#firstBrokerLoginFlowAlias";
  private postLoginFlowSelect = "#postBrokerLoginFlowAlias";
  private syncModeSelect = "#syncMode";
  private addBtn = "createProvider";
  private saveBtn = "save";
  private revertBtn = "revert";

  private validateSignature = "#validateSignature";
  private JwksSwitch = "#useJwksUrl";
  private jwksUrl = "jwksUrl";
  private pkceSwitch = "#pkceEnabled";
  private pkceMethod = "#pkceMethod";
  private clientAuth = "#clientAuthentication";

  public clickSaveBtn() {
    cy.findByTestId(this.saveBtn).click();
  }

  public clickRevertBtn() {
    cy.findByTestId(this.revertBtn).click();
  }

  public typeScopesInput(text: string) {
    cy.get(this.scopesInput).type(text).blur();
    return this;
  }

  public clickStoreTokensSwitch() {
    cy.get(this.storeTokensSwitch).parent().click();
    return this;
  }

  public clickStoredTokensReadableSwitch() {
    cy.get(this.storedTokensReadable).parent().click();
    return this;
  }

  public clickAcceptsPromptNoneForwardFromClientSwitch() {
    cy.get(this.acceptsPromptNoneForwardFromClientSwitch).parent().click();
    return this;
  }

  public clickDisableUserInfoSwitch() {
    cy.get(this.disableUserInfoSwitch).parent().click();
    return this;
  }

  public clickTrustEmailSwitch() {
    cy.get(this.trustEmailSwitch).parent().click();
    return this;
  }

  public clickAccountLinkingOnlySwitch() {
    cy.get(this.accountLinkingOnlySwitch).parent().click();
    return this;
  }

  public clickHideOnLoginPageSwitch() {
    cy.get(this.hideOnLoginPageSwitch).parent().click();
    return this;
  }

  public selectFirstLoginFlowOption(loginFlowOption: LoginFlowOption) {
    cy.get(this.firstLoginFlowSelect).click();
    super.clickSelectMenuItem(
      loginFlowOption,
      cy.get(".pf-c-select__menu-item").contains(loginFlowOption)
    );
    return this;
  }

  public selectPostLoginFlowOption(loginFlowOption: LoginFlowOption) {
    cy.get(this.postLoginFlowSelect).click();
    super.clickSelectMenuItem(
      loginFlowOption,
      cy.get(".pf-c-select__menu-item").contains(loginFlowOption)
    );
    return this;
  }

  public selectSyncModeOption(syncModeOption: SyncModeOption) {
    cy.get(this.syncModeSelect).click();
    super.clickSelectMenuItem(
      syncModeOption,
      cy.get(".pf-c-select__menu-item").contains(syncModeOption)
    );
    return this;
  }

  public selectPromptOption(promptOption: PromptSelect) {
    cy.get(this.promptSelect).click();
    super.clickSelectMenuItem(
      promptOption,
      cy.get(".pf-c-select__menu-item").contains(promptOption).parent()
    );
    return this;
  }

  public clickAdd() {
    cy.findByTestId(this.addBtn).click();
    return this;
  }

  public assertScopesInputEqual(text: string) {
    cy.get(this.scopesInput).should("have.text", text).parent();
    return this;
  }

  public assertStoreTokensSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.storeTokensSwitch), isOn);
    return this;
  }

  public assertStoredTokensReadableTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.storedTokensReadable).parent(), isOn);
    return this;
  }

  public assertAcceptsPromptNoneForwardFromClientSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.get(this.acceptsPromptNoneForwardFromClientSwitch).parent(),
      isOn
    );
    return this;
  }

  public assertDisableUserInfoSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.get(this.disableUserInfoSwitch).parent(),
      isOn
    );
    return this;
  }

  public assertTrustEmailSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.trustEmailSwitch).parent(), isOn);
    return this;
  }

  public assertAccountLinkingOnlySwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.accountLinkingOnlySwitch), isOn);
    return this;
  }

  public assertHideOnLoginPageSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.get(this.hideOnLoginPageSwitch).parent(),
      isOn
    );
    return this;
  }

  public assertFirstLoginFlowSelectOptionEqual(
    loginFlowOption: LoginFlowOption
  ) {
    cy.get(this.firstLoginFlowSelect).should("have.text", loginFlowOption);
    return this;
  }

  public assertPostLoginFlowSelectOptionEqual(
    loginFlowOption: LoginFlowOption
  ) {
    cy.get(this.postLoginFlowSelect).should("have.text", loginFlowOption);
    return this;
  }

  public assertSyncModeSelectOptionEqual(syncModeOption: SyncModeOption) {
    cy.get(this.syncModeSelect).should("have.text", syncModeOption);
    return this;
  }

  public assertOIDCUrl(url: string) {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    cy.findByTestId(url + "Url")
      .clear()
      .type("invalidUrl");
    this.clickSaveBtn();
    masthead.checkNotificationMessage(
      "Could not update the provider The url [" + url + "_url] is malformed",
      true
    );
    this.clickRevertBtn();
    //cy.findByTestId(url + "Url").contains
    //("http://localhost:8180/realms/master/protocol/openid-connect/" + url)
    return this;
  }

  public assertOIDCSignatureSwitch() {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    cy.findByTestId(this.jwksUrl).should("exist");
    super.assertSwitchStateOn(cy.get(this.JwksSwitch));

    cy.get(this.JwksSwitch).parent().click();
    cy.findByTestId(this.jwksUrl).should("not.exist");
    super.assertSwitchStateOff(cy.get(this.JwksSwitch));

    cy.get(this.validateSignature).parent().click();
    cy.findByTestId(this.jwksUrl).should("not.exist");
    super.assertSwitchStateOff(cy.get(this.validateSignature));

    this.clickRevertBtn();
    cy.findByTestId(this.jwksUrl).should("exist");
    return this;
  }

  public assertOIDCPKCESwitch() {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    super.assertSwitchStateOff(cy.get(this.pkceSwitch));
    cy.get(this.pkceMethod).should("not.exist");
    cy.get(this.pkceSwitch).parent().click();

    super.assertSwitchStateOn(cy.get(this.pkceSwitch));
    cy.get(this.pkceMethod).should("exist");
    return this;
  }

  public assertOIDCClientAuthentication(option: ClientAuthentication) {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    cy.get(this.clientAuth)
      .click()
      .get(".pf-c-select__menu-item")
      .contains(option)
      .click();
    return this;
  }

  public assertOIDCSettingsAdvancedSwitches() {
    cy.get(this.advancedSettingsToggle).scrollIntoView().click();

    cy.get(this.passLoginHintSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.passLoginHintSwitch));

    cy.get(this.passMaxAgeSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.passMaxAgeSwitch));

    cy.get(this.passCurrentLocaleSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.passCurrentLocaleSwitch));

    cy.get(this.backchannelLogoutSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.backchannelLogoutSwitch));

    cy.get(this.disableUserInfoSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.disableUserInfoSwitch));

    this.clickAcceptsPromptNoneForwardFromClientSwitch();
    super.assertSwitchStateOn(
      cy.get(this.acceptsPromptNoneForwardFromClientSwitch)
    );
    return this;
  }

  public assertAdvancedSettings() {
    cy.findByTestId("jump-link-advanced-settings").click();

    this.clickStoreTokensSwitch();
    this.assertStoreTokensSwitchTurnedOn(true);
    this.clickStoredTokensReadableSwitch();
    this.assertStoredTokensReadableTurnedOn(true);
    this.clickTrustEmailSwitch();
    this.assertTrustEmailSwitchTurnedOn(true);
    this.clickAccountLinkingOnlySwitch();
    this.assertAccountLinkingOnlySwitchTurnedOn(true);
    this.clickHideOnLoginPageSwitch();
    this.assertHideOnLoginPageSwitchTurnedOn(true);

    this.selectFirstLoginFlowOption(LoginFlowOption.browser);
    this.selectPostLoginFlowOption(LoginFlowOption.directGrant);
    this.selectSyncModeOption(SyncModeOption.legacy);

    this.clickRevertBtn();
    this.assertStoreTokensSwitchTurnedOn(false);
    this.assertStoredTokensReadableTurnedOn(false);
    this.assertTrustEmailSwitchTurnedOn(false);
    this.assertAccountLinkingOnlySwitchTurnedOn(false);
    this.assertHideOnLoginPageSwitchTurnedOn(false);

    this.assertFirstLoginFlowSelectOptionEqual(
      LoginFlowOption.firstBrokerLogin
    );
    this.assertPostLoginFlowSelectOptionEqual(LoginFlowOption.none);
    this.assertSyncModeSelectOptionEqual(SyncModeOption.import);
    return this;
  }
}
