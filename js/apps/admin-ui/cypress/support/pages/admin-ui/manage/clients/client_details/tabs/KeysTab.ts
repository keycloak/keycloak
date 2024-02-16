import CommonPage from "../../../../../CommonPage";

export default class KeysTab extends CommonPage {
  #generateBtn = "generate";
  #confirmBtn = "confirm";
  #useJwksUrl = "useJwksUrl";
  #keyAlias = "keyAlias";
  #keyPassword = "keyPassword";
  #storePassword = "storePassword";

  toggleUseJwksUrl() {
    cy.findByTestId(this.#useJwksUrl).click({ force: true });
    return this;
  }

  clickGenerate() {
    cy.findByTestId(this.#generateBtn).click();
    return this;
  }

  clickConfirm() {
    cy.findByTestId(this.#confirmBtn).click();
    return this;
  }

  fillGenerateModal(
    archiveFormat: string,
    keyAlias: string,
    keyPassword: string,
    storePassword: string,
  ) {
    cy.get("#archiveFormat").click();
    cy.findAllByRole("option").contains(archiveFormat).click();
    cy.findByTestId(this.#keyAlias).type(keyAlias);
    cy.findByTestId(this.#keyPassword).type(keyPassword);
    cy.findByTestId(this.#storePassword).type(storePassword);
    return this;
  }
}
