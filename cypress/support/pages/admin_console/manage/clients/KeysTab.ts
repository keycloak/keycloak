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
    cy.findByTestId(this.saveKeys).should(
      (!disabled ? "not." : "") + "be.disabled"
    );
    return this;
  }

  toggleUseJwksUrl() {
    cy.findByTestId(this.useJwksUrl).click({ force: true });
    return this;
  }

  clickGenerate() {
    cy.findByTestId(this.generate).click();
    return this;
  }

  clickConfirm() {
    cy.findByTestId(this.confirm).click();
    return this;
  }

  fillGenerateModal(
    keyAlias: string,
    keyPassword: string,
    storePassword: string
  ) {
    cy.findByTestId(this.keyAlias).type(keyAlias);
    cy.findByTestId(this.keyPassword).type(keyPassword);
    cy.findByTestId(this.storePassword).type(storePassword);
    return this;
  }
}
