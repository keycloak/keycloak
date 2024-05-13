import PageObject from "../../components/PageObject";
import Masthead from "../../Masthead";

const masthead = new Masthead();

export enum LoginFlowOption {
  empty = "First login flow override",
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
  post = "Client secret sent as post",
  basicAuth = "Client secret sent as basic auth",
  jwt = "JWT signed with client secret",
  jwtPrivKey = "JWT signed with private key",
}

export enum ClientAssertionSigningAlg {
  algorithmNotSpecified = "Algorithm not specified",
  es256 = "ES256",
  es384 = "ES384",
  es512 = "ES512",
  hs256 = "HS256",
  hs384 = "HS384",
  hs512 = "HS512",
  ps256 = "PS256",
  ps384 = "PS384",
  ps512 = "PS512",
  rs256 = "RS256",
  rs384 = "RS384",
  rs512 = "RS512",
}

export default class ProviderBaseGeneralSettingsPage extends PageObject {
  #scopesInput = "#scopes";
  #storeTokensSwitch = "#storeTokens";
  #storedTokensReadable = "#storedTokensReadable";
  #isAccessTokenJWT = "#isAccessTokenJWT";
  #acceptsPromptNoneForwardFromClientSwitch = "#acceptsPromptNone";
  #advancedSettingsToggle = ".pf-v5-c-expandable-section__toggle";
  #passLoginHintSwitch = "#passLoginHint";
  #passMaxAgeSwitch = "#passMaxAge";
  #passCurrentLocaleSwitch = "#passCurrentLocale";
  #backchannelLogoutSwitch = "#backchannelLogout";
  #promptSelect = "#prompt";
  #disableUserInfoSwitch = "#disableUserInfo";
  #trustEmailSwitch = "#trustEmail";
  #doNotStoreUsers = "#doNotStoreUsers";
  #accountLinkingOnlySwitch = "#accountLinkingOnly";
  #hideOnLoginPageSwitch = "#hideOnLoginPage";
  #firstLoginFlowSelect = "#firstBrokerLoginFlowAliasOverride";
  #postLoginFlowSelect = "#postBrokerLoginFlowAlias";
  #syncModeSelect = "#syncMode";
  #essentialClaimSwitch = "#filteredByClaim";
  #claimNameInput = "#kc-claim-filter-name";
  #claimValueInput = "#kc-claim-filter-value";
  #addBtn = "createProvider";
  #saveBtn = "idp-details-save";
  #revertBtn = "idp-details-revert";

  #validateSignature = "#config\\.validateSignature";
  #jwksSwitch = "#config\\.useJwksUrl";
  #jwksUrl = "config.jwksUrl";
  #pkceSwitch = "#config\\.pkceEnabled";
  #pkceMethod = "#pkceMethod";
  #clientAuth = "#clientAuthentication";
  #clientAssertionSigningAlg = "#clientAssertionSigningAlg";
  #clientAssertionAudienceInput = "#clientAssertionAudience";
  #jwtX509HeadersSwitch = "#jwtX509HeadersEnabled";

  public clickSaveBtn() {
    cy.findByTestId(this.#saveBtn).click();
  }

  public clickRevertBtn() {
    cy.findByTestId(this.#revertBtn).click();
  }

  public typeScopesInput(text: string) {
    cy.get(this.#scopesInput).type(text).blur();
    return this;
  }

  public ensureAdvancedSettingsAreVisible() {
    cy.findByTestId("jump-link-general-settings").click();
    cy.findByTestId("jump-link-advanced-settings").click();
  }

  public clickStoreTokensSwitch() {
    cy.get(this.#storeTokensSwitch).parent().click();
    return this;
  }

  public clickStoredTokensReadableSwitch() {
    cy.get(this.#storedTokensReadable).parent().click();
    return this;
  }

  public clickIsAccessTokenJWTSwitch() {
    cy.get(this.#isAccessTokenJWT).parent().click();
    return this;
  }

  public clickAcceptsPromptNoneForwardFromClientSwitch() {
    cy.get(this.#acceptsPromptNoneForwardFromClientSwitch).parent().click();
    return this;
  }

  public clickDisableUserInfoSwitch() {
    cy.get(this.#disableUserInfoSwitch).parent().click();
    return this;
  }

  public clickTrustEmailSwitch() {
    cy.get(this.#trustEmailSwitch).parent().click();
    return this;
  }

  public clickAccountLinkingOnlySwitch() {
    cy.get(this.#accountLinkingOnlySwitch).parent().click();
    return this;
  }

  public clickHideOnLoginPageSwitch() {
    cy.get(this.#hideOnLoginPageSwitch).parent().click();
    return this;
  }

  public clickEssentialClaimSwitch() {
    cy.get(this.#essentialClaimSwitch).parent().click();
    return this;
  }

  public clickdoNotStoreUsersSwitch() {
    cy.get(this.#doNotStoreUsers).parent().click();
    return this;
  }

  public typeClaimNameInput(text: string) {
    cy.get(this.#claimNameInput).type(text).blur();
    return this;
  }

  public typeClaimValueInput(text: string) {
    cy.get(this.#claimValueInput).type(text).blur();
    return this;
  }

  public selectFirstLoginFlowOption(loginFlowOption: LoginFlowOption) {
    cy.get(this.#firstLoginFlowSelect).click();
    super.clickSelectMenuItem(
      loginFlowOption,
      cy.get(".pf-v5-c-menu__list-item").contains(loginFlowOption),
    );
    return this;
  }

  public selectPostLoginFlowOption(loginFlowOption: LoginFlowOption) {
    cy.get(this.#postLoginFlowSelect).click();
    super.clickSelectMenuItem(
      loginFlowOption,
      cy.get(".pf-v5-c-menu__list-item").contains(loginFlowOption),
    );
    return this;
  }

  public selectClientAssertSignAlg(
    clientAssertionSigningAlg: ClientAssertionSigningAlg,
  ) {
    cy.get(this.#clientAssertionSigningAlg).click();
    super.clickSelectMenuItem(
      clientAssertionSigningAlg,
      cy.get(".pf-v5-c-menu__list-item").contains(clientAssertionSigningAlg),
    );
    return this;
  }

  public typeClientAssertionAudience(text: string) {
    cy.get(this.#clientAssertionAudienceInput).type(text).blur();
    return this;
  }

  public selectSyncModeOption(syncModeOption: SyncModeOption) {
    cy.get(this.#syncModeSelect).click();
    super.clickSelectMenuItem(
      syncModeOption,
      cy.get(".pf-v5-c-menu__list-item").contains(syncModeOption),
    );
    return this;
  }

  public selectPromptOption(promptOption: PromptSelect) {
    cy.get(this.#promptSelect).click();
    super.clickSelectMenuItem(
      promptOption,
      cy.get(".pf-v5-c-menu__list-item").contains(promptOption).parent(),
    );
    return this;
  }

  public clickAdd() {
    cy.findByTestId(this.#addBtn).click();
    return this;
  }

  public assertScopesInputEqual(text: string) {
    cy.get(this.#scopesInput).should("have.value", text).parent();
    return this;
  }

  public assertStoreTokensSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.#storeTokensSwitch), isOn);
    return this;
  }

  public assertStoredTokensReadableTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.get(this.#storedTokensReadable).parent(),
      isOn,
    );
    return this;
  }

  public assertIsAccessTokenJWTTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.#isAccessTokenJWT).parent(), isOn);
    return this;
  }

  public assertAcceptsPromptNoneForwardFromClientSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.get(this.#acceptsPromptNoneForwardFromClientSwitch).parent(),
      isOn,
    );
    return this;
  }

  public assertDisableUserInfoSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.get(this.#disableUserInfoSwitch).parent(),
      isOn,
    );
    return this;
  }

  public assertTrustEmailSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.#trustEmailSwitch).parent(), isOn);
    return this;
  }

  public assertAccountLinkingOnlySwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.#accountLinkingOnlySwitch), isOn);
    return this;
  }

  public assertHideOnLoginPageSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.get(this.#hideOnLoginPageSwitch).parent(),
      isOn,
    );
    return this;
  }

  public assertDoNotImportUsersSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(cy.get(this.#doNotStoreUsers).parent(), isOn);
    return this;
  }

  public assertEssentialClaimSwitchTurnedOn(isOn: boolean) {
    super.assertSwitchStateOn(
      cy.get(this.#essentialClaimSwitch).parent(),
      isOn,
    );
    return this;
  }

  public assertClaimInputEqual(text: string) {
    cy.get(this.#claimNameInput).should("have.value", text).parent();
    return this;
  }

  public assertClaimValueInputEqual(text: string) {
    cy.get(this.#claimValueInput).should("have.value", text).parent();
    return this;
  }

  public assertFirstLoginFlowSelectOptionEqual(
    loginFlowOption: LoginFlowOption,
  ) {
    cy.get(this.#firstLoginFlowSelect).should("have.text", loginFlowOption);
    return this;
  }

  public assertPostLoginFlowSelectOptionEqual(
    loginFlowOption: LoginFlowOption,
  ) {
    cy.get(this.#postLoginFlowSelect).should("have.text", loginFlowOption);
    return this;
  }

  public assertSyncModeSelectOptionEqual(syncModeOption: SyncModeOption) {
    cy.get(this.#syncModeSelect).should("have.text", syncModeOption);
    return this;
  }

  public assertSyncModeShown(isShown: boolean) {
    cy.get(this.#syncModeSelect).should(isShown ? "exist" : "not.exist");
    return this;
  }

  public assertClientAssertSigAlgSelectOptionEqual(
    clientAssertionSigningAlg: ClientAssertionSigningAlg,
  ) {
    cy.get(this.#clientAssertionSigningAlg).should(
      "have.text",
      clientAssertionSigningAlg,
    );
    return this;
  }

  public assertClientAssertionAudienceInputEqual(text: string) {
    cy.get(this.#clientAssertionAudienceInput)
      .should("have.value", text)
      .parent();
    return this;
  }

  public assertOIDCUrl(url: string) {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    cy.findByTestId(`config.${url}Url`).clear();
    cy.findByTestId(`config.${url}Url`).type("invalidUrl");
    this.clickSaveBtn();
    masthead.checkNotificationMessage(
      "Could not update the provider The url [" + url + "_url] is malformed",
      true,
    );
    this.clickRevertBtn();
    //cy.findByTestId(url + "Url").contains
    //("http://localhost:8180/realms/master/protocol/openid-connect/" + url)
    return this;
  }

  public assertOIDCSignatureSwitch() {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    cy.findByTestId(this.#jwksUrl).should("exist");
    super.assertSwitchStateOn(cy.get(this.#jwksSwitch));

    cy.get(this.#jwksSwitch).parent().click();
    cy.findByTestId(this.#jwksUrl).should("not.exist");
    super.assertSwitchStateOff(cy.get(this.#jwksSwitch));

    cy.get(this.#validateSignature).parent().click();
    cy.findByTestId(this.#jwksUrl).should("not.exist");
    super.assertSwitchStateOff(cy.get(this.#validateSignature));

    this.clickRevertBtn();
    cy.findByTestId(this.#jwksUrl).should("exist");
    return this;
  }

  public assertOIDCPKCESwitch() {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    super.assertSwitchStateOff(cy.get(this.#pkceSwitch));
    cy.get(this.#pkceMethod).should("not.exist");
    cy.get(this.#pkceSwitch).parent().click();

    super.assertSwitchStateOn(cy.get(this.#pkceSwitch));
    cy.get(this.#pkceMethod).should("exist");
    return this;
  }

  public assertOIDCClientAuthentication(option: ClientAuthentication) {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    cy.get(this.#clientAuth)
      .click()
      .get(".pf-v5-c-menu__list-item")
      .contains(option)
      .click();
    return this;
  }

  public assertOIDCClientAuthSignAlg(alg: string) {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    cy.get(this.#clientAssertionSigningAlg)
      .click()
      .get(".pf-v5-c-menu__list-item")
      .contains(alg)
      .click();
    return this;
  }

  public assertOIDCJWTX509HeadersSwitch() {
    cy.findByTestId("jump-link-openid-connect-settings").click();
    cy.get(this.#clientAuth)
      .click()
      .get(".pf-v5-c-menu__list-item")
      .contains(ClientAuthentication.post)
      .click();
    cy.get(this.#jwtX509HeadersSwitch).should("not.exist");
    cy.get(this.#clientAuth)
      .click()
      .get(".pf-v5-c-menu__list-item")
      .contains(ClientAuthentication.basicAuth)
      .click();
    cy.get(this.#jwtX509HeadersSwitch).should("not.exist");
    cy.get(this.#clientAuth)
      .click()
      .get(".pf-v5-c-menu__list-item")
      .contains(ClientAuthentication.jwt)
      .click();
    cy.get(this.#jwtX509HeadersSwitch).should("not.exist");
    cy.get(this.#clientAuth)
      .click()
      .get(".pf-v5-c-menu__list-item")
      .contains(ClientAuthentication.jwtPrivKey)
      .click();
    cy.get(this.#jwtX509HeadersSwitch).should("exist");

    super.assertSwitchStateOff(cy.get(this.#jwtX509HeadersSwitch));
    cy.get(this.#jwtX509HeadersSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#jwtX509HeadersSwitch));
    return this;
  }

  public assertOIDCSettingsAdvancedSwitches() {
    cy.get(this.#advancedSettingsToggle).scrollIntoView().click();

    cy.get(this.#passLoginHintSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#passLoginHintSwitch));

    cy.get(this.#passMaxAgeSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#passMaxAgeSwitch));

    cy.get(this.#passCurrentLocaleSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#passCurrentLocaleSwitch));

    cy.get(this.#backchannelLogoutSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#backchannelLogoutSwitch));

    cy.get(this.#disableUserInfoSwitch).parent().click();
    super.assertSwitchStateOn(cy.get(this.#disableUserInfoSwitch));

    this.clickAcceptsPromptNoneForwardFromClientSwitch();
    super.assertSwitchStateOn(
      cy.get(this.#acceptsPromptNoneForwardFromClientSwitch),
    );
    return this;
  }

  public assertAdvancedSettings() {
    cy.findByTestId("jump-link-advanced-settings").click();

    this.clickStoreTokensSwitch();
    this.assertStoreTokensSwitchTurnedOn(true);
    this.clickStoredTokensReadableSwitch();
    this.assertStoredTokensReadableTurnedOn(true);
    this.clickIsAccessTokenJWTSwitch();
    this.assertIsAccessTokenJWTTurnedOn(true);
    this.clickTrustEmailSwitch();
    this.assertTrustEmailSwitchTurnedOn(true);
    this.clickAccountLinkingOnlySwitch();
    this.assertAccountLinkingOnlySwitchTurnedOn(true);
    this.clickHideOnLoginPageSwitch();
    this.assertHideOnLoginPageSwitchTurnedOn(true);

    this.selectFirstLoginFlowOption(LoginFlowOption.browser);
    this.selectPostLoginFlowOption(LoginFlowOption.directGrant);
    this.selectSyncModeOption(SyncModeOption.import);

    this.clickRevertBtn();
    cy.get(this.#advancedSettingsToggle).scrollIntoView().click();
    this.assertStoreTokensSwitchTurnedOn(false);
    this.assertStoredTokensReadableTurnedOn(false);
    this.assertIsAccessTokenJWTTurnedOn(false);
    this.assertTrustEmailSwitchTurnedOn(false);
    this.assertAccountLinkingOnlySwitchTurnedOn(false);
    this.assertHideOnLoginPageSwitchTurnedOn(false);

    this.assertFirstLoginFlowSelectOptionEqual(LoginFlowOption.empty);
    this.assertPostLoginFlowSelectOptionEqual(LoginFlowOption.none);
    this.assertSyncModeSelectOptionEqual(SyncModeOption.legacy);
    this.assertClientAssertSigAlgSelectOptionEqual(
      ClientAssertionSigningAlg.algorithmNotSpecified,
    );
    return this;
  }
}
