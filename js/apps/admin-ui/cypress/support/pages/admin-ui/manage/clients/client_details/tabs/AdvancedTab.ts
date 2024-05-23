import PageObject from "../../../../components/PageObject";

export default class AdvancedTab extends PageObject {
  #clusterNodesExpandBtn =
    ".pf-v5-c-expandable-section .pf-v5-c-expandable-section__toggle";
  #testClusterAvailability = "#testClusterAvailability";
  #emptyClusterElement = "empty-state";
  #registerNodeManuallyBtn = "no-nodes-registered-empty-action";
  #deleteClusterNodeDrpDwn =
    '[aria-label="Registered cluster nodes"] [aria-label="Kebab toggle"]';
  #deleteClusterNodeBtn =
    '[aria-label="Registered cluster nodes"] [role="menu"] [type="button"]';
  #nodeHostInput = "node";
  #addNodeConfirmBtn = "#add-node-confirm";

  #accessTokenSignatureAlgorithmInput = "#access🍺token🍺signed🍺response🍺alg";
  #fineGrainSaveBtn = "#fineGrainSave";
  #fineGrainRevertBtn = "#fineGrainRevert";
  #OIDCCompatabilitySaveBtn = "OIDCCompatabilitySave";
  #OIDCCompatabilityRevertBtn = "OIDCCompatabilityRevert";
  #OIDCAdvancedSaveBtn = "OIDCAdvancedSave";
  #OIDCAdvancedRevertBtn = "OIDCAdvancedRevert";
  #OIDCAuthFlowOverrideSaveBtn = "OIDCAuthFlowOverrideSave";
  #OIDCAuthFlowOverrideRevertBtn = "OIDCAuthFlowOverrideRevert";

  #excludeSessionStateSwitch =
    "#excludeSessionStateFromAuthenticationResponse-switch";
  #useRefreshTokenSwitch = "#useRefreshTokens";
  #useRefreshTokenForClientCredentialsGrantSwitch =
    "#useRefreshTokenForClientCredentialsGrant";
  #useLowerCaseBearerTypeSwitch = "#useLowerCaseBearerType";

  #oAuthMutualSwitch =
    "attributes.tls🍺client🍺certificate🍺bound🍺access🍺tokens";
  #keyForCodeExchangeInput = "#keyForCodeExchange";
  #pushedAuthorizationRequestRequiredSwitch =
    "attributes.require🍺pushed🍺authorization🍺requests";

  #browserFlowInput = "#browser";
  #directGrantInput = "#direct_grant";

  #jumpToOIDCCompatabilitySettings =
    "jump-link-openid-connect-compatibility-modes";
  #jumpToAdvancedSettings = "jump-link-advanced-settings";
  #jumpToAuthFlowOverride = "jump-link-authentication-flow-overrides";

  expandClusterNode() {
    cy.get(this.#clusterNodesExpandBtn).click();
    return this;
  }

  checkTestClusterAvailability(active: boolean) {
    cy.get(this.#testClusterAvailability).should(
      (active ? "not." : "") + "have.class",
      "pf-m-disabled",
    );
    return this;
  }

  checkEmptyClusterNode() {
    cy.findByTestId(this.#emptyClusterElement).should("exist");
    return this;
  }

  registerNodeManually() {
    cy.findByTestId(this.#registerNodeManuallyBtn).click();
    return this;
  }

  deleteClusterNode() {
    cy.get(this.#deleteClusterNodeDrpDwn).click();
    cy.get(this.#deleteClusterNodeBtn).click();
    return this;
  }

  fillHost(host: string) {
    cy.findByTestId(this.#nodeHostInput).type(host);
    return this;
  }

  saveHost() {
    cy.get(this.#addNodeConfirmBtn).click();
    return this;
  }

  selectAccessTokenSignatureAlgorithm(algorithm: string) {
    cy.get(this.#accessTokenSignatureAlgorithmInput).click();
    cy.get(".pf-v5-c-menu__list").contains(algorithm).click();

    return this;
  }

  checkAccessTokenSignatureAlgorithm(algorithm: string) {
    cy.get(this.#accessTokenSignatureAlgorithmInput).should(
      "have.text",
      algorithm,
    );
    return this;
  }

  saveFineGrain() {
    cy.get(this.#fineGrainSaveBtn).click();
    return this;
  }

  revertFineGrain() {
    cy.get(this.#fineGrainRevertBtn).click();
    return this;
  }

  saveCompatibility() {
    cy.findByTestId(this.#OIDCCompatabilitySaveBtn).click();
    return this;
  }

  revertCompatibility() {
    cy.findByTestId(this.#OIDCCompatabilityRevertBtn).click();
    cy.findByTestId(this.#jumpToOIDCCompatabilitySettings).click();
    //uncomment when revert function reverts all switches, rather than just the first one
    //this.assertSwitchStateOn(cy.get(this.useRefreshTokenForClientCredentialsGrantSwitch));
    this.assertSwitchStateOn(cy.get(this.#excludeSessionStateSwitch));
    return this;
  }

  jumpToCompatability() {
    cy.findByTestId(this.#jumpToOIDCCompatabilitySettings).click();
    return this;
  }

  clickAllCompatibilitySwitch() {
    cy.get(this.#excludeSessionStateSwitch).parent().click();
    this.assertSwitchStateOn(cy.get(this.#excludeSessionStateSwitch));
    cy.get(this.#useRefreshTokenSwitch).parent().click();
    this.assertSwitchStateOff(cy.get(this.#useRefreshTokenSwitch));
    cy.get(this.#useRefreshTokenForClientCredentialsGrantSwitch)
      .parent()
      .click();
    this.assertSwitchStateOn(
      cy.get(this.#useRefreshTokenForClientCredentialsGrantSwitch),
    );
    cy.get(this.#useLowerCaseBearerTypeSwitch).parent().click();
    this.assertSwitchStateOn(cy.get(this.#useLowerCaseBearerTypeSwitch));
    return this;
  }

  clickExcludeSessionStateSwitch() {
    cy.get(this.#excludeSessionStateSwitch).parent().click();
    this.assertSwitchStateOff(cy.get(this.#excludeSessionStateSwitch));
  }
  clickUseRefreshTokenForClientCredentialsGrantSwitch() {
    cy.get(this.#useRefreshTokenForClientCredentialsGrantSwitch)
      .parent()
      .click();
    this.assertSwitchStateOff(
      cy.get(this.#useRefreshTokenForClientCredentialsGrantSwitch),
    );
  }

  saveAdvanced() {
    cy.findByTestId(this.#OIDCAdvancedSaveBtn).click();
    return this;
  }

  revertAdvanced() {
    cy.findByTestId(this.#OIDCAdvancedRevertBtn).click();
    return this;
  }

  jumpToAdvanced() {
    cy.findByTestId(this.#jumpToAdvancedSettings).click();
    return this;
  }

  clickAdvancedSwitches() {
    cy.findByTestId(this.#oAuthMutualSwitch).parent().click();
    cy.findByTestId(this.#pushedAuthorizationRequestRequiredSwitch)
      .parent()
      .click();
    return this;
  }

  checkAdvancedSwitchesOn() {
    cy.findByTestId(this.#oAuthMutualSwitch).scrollIntoView();
    this.assertSwitchStateOn(cy.findByTestId(this.#oAuthMutualSwitch));
    this.assertSwitchStateOn(
      cy.findByTestId(this.#pushedAuthorizationRequestRequiredSwitch),
    );
    return this;
  }

  checkAdvancedSwitchesOff() {
    this.assertSwitchStateOff(cy.findByTestId(this.#oAuthMutualSwitch));
    this.assertSwitchStateOff(
      cy.findByTestId(this.#pushedAuthorizationRequestRequiredSwitch),
    );
    return this;
  }

  selectKeyForCodeExchangeInput(input: string) {
    cy.get(this.#keyForCodeExchangeInput).click();
    cy.get(this.#keyForCodeExchangeInput)
      .parent()
      .get("ul")
      .contains(input)
      .click();
    return this;
  }

  checkKeyForCodeExchangeInput(input: string) {
    cy.get(this.#keyForCodeExchangeInput).should("have.text", input);
    return this;
  }

  saveAuthFlowOverride() {
    cy.findByTestId(this.#OIDCAuthFlowOverrideSaveBtn).click();
    return this;
  }

  revertAuthFlowOverride() {
    cy.findByTestId(this.#OIDCAuthFlowOverrideRevertBtn).click();
    return this;
  }

  jumpToAuthFlow() {
    cy.findByTestId(this.#jumpToAuthFlowOverride).click();
    return this;
  }

  selectBrowserFlowInput(input: string) {
    cy.get(this.#browserFlowInput).click();
    cy.get(this.#browserFlowInput).parent().get("ul").contains(input).click();
    return this;
  }

  selectDirectGrantInput(input: string) {
    cy.get(this.#directGrantInput).click();
    cy.get(this.#directGrantInput).parent().get("ul").contains(input).click();
    return this;
  }

  checkBrowserFlowInput(input: string) {
    cy.get(this.#browserFlowInput).should("have.text", input);
    return this;
  }

  checkDirectGrantInput(input: string) {
    cy.get(this.#directGrantInput).should("have.text", input);
    return this;
  }
}
