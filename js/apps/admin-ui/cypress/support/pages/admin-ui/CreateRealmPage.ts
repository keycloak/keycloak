import FormValidation from "../../forms/FormValidation";

export default class CreateRealmPage {
  #modalClearBtn = "clear-button";
  #realmNameInput = "realm";
  #enabledSwitch = ".pf-v5-c-toolbar .pf-v5-c-switch__toggle";
  #createBtn = '.pf-v5-c-form__group:last-child button[type="submit"]';
  #cancelBtn = '.pf-v5-c-form__group:last-child button[type="button"]';
  #codeEditor = ".w-tc-editor-text";

  #getClearBtn() {
    return cy.findByText("Clear");
  }

  fillRealmName(realmName: string) {
    cy.findByTestId(this.#realmNameInput).clear().type(realmName);

    return this;
  }

  fillCodeEditor() {
    cy.get(this.#codeEditor).type("clear this field");

    return this;
  }

  createRealm(wait = true) {
    if (wait) {
      cy.intercept("POST", "/admin/realms").as("createRealm");
      cy.get(this.#createBtn).click();
      cy.wait("@createRealm");
    } else {
      cy.get(this.#createBtn).click();
    }

    return this;
  }

  disableRealm() {
    cy.get(this.#enabledSwitch).click();

    return this;
  }

  cancelRealmCreation() {
    cy.get(this.#cancelBtn).click();

    return this;
  }

  clearTextField() {
    this.#getClearBtn().click();
    cy.findByTestId(this.#modalClearBtn).click();

    return this;
  }

  verifyRealmNameFieldInvalid() {
    FormValidation.assertRequired(cy.findByTestId(this.#realmNameInput));

    return this;
  }
}
