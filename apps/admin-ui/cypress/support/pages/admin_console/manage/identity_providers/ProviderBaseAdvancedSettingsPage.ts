import PageObject from "../../components/PageObject";

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

export default class ProviderBaseGeneralSettingsPage extends PageObject {
  private scopesInput = "#scopes";
  private storeTokensSwitch = "#storeTokens";
  private storedTokensReadable = "#storedTokensReadable";
  private acceptsPromptNoneForwardFromClientSwitch = "#acceptsPromptNone";
  private disableUserInfoSwitch = "#disableUserInfo";
  private trustEmailSwitch = "#trustEmail";
  private accountLinkingOnlySwitch = "#accountLinkingOnly";
  private hideOnLoginPageSwitch = "#hideOnLoginPage";
  private firstLoginFlowSelect = "#firstBrokerLoginFlowAlias";
  private postLoginFlowSelect = "#postBrokerLoginFlowAlias";
  private syncModeSelect = "#syncMode";
  private addBtn = "createProvider";

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
      cy.get(this.firstLoginFlowSelect).parent()
    );
    return this;
  }

  public selectPostLoginFlowOption(loginFlowOption: LoginFlowOption) {
    cy.get(this.postLoginFlowSelect).click();
    super.clickSelectMenuItem(
      loginFlowOption,
      cy.get(this.postLoginFlowSelect).parent()
    );
    return this;
  }

  public selectSyncModeOption(syncModeOption: SyncModeOption) {
    cy.get(this.syncModeSelect).click();
    super.clickSelectMenuItem(
      syncModeOption,
      cy.get(this.syncModeSelect).parent()
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
    cy.get(this.postLoginFlowSelect).should("have.text", syncModeOption);
    return this;
  }
}
