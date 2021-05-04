export default class KeysTab {
  private tabName = "#pf-tab-keys-keys";
  private useJwksUrl = "useJwksUrl";
  private saveKeys = "saveKeys";
  private generate = "generate";
  private keyAlias = "keyAlias";
  private keyPassword = "keyPassword";
  private storePassword = "storePassword";
  private confirm = "confirm";

  goToTab() {
    cy.get(this.tabName).click();
    return this;
  }

  checkSaveDisabled(disabled = true) {
    cy.getId(this.saveKeys).should((!disabled ? "not." : "") + "be.disabled");
    return this;
  }

  toggleUseJwksUrl() {
    cy.getId(this.useJwksUrl).click({ force: true });
    return this;
  }

  clickGenerate() {
    cy.getId(this.generate).click();
    return this;
  }

  clickConfirm() {
    cy.getId(this.confirm).click();
    return this;
  }

  fillGenerateModal(
    keyAlias: string,
    keyPassword: string,
    storePassword: string
  ) {
    cy.getId(this.keyAlias)
      .type(keyAlias)
      .getId(this.keyPassword)
      .type(keyPassword)
      .getId(this.storePassword)
      .type(storePassword);
    return this;
  }
}
