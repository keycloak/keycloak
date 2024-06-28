import FormValidation from "../../forms/FormValidation";

export default class CreateRealmPage {
  #modalClearBtn = "clear-button";
  #realmNameInput = "realm";
  #enabledSwitch = ".pf-v5-c-toolbar .pf-v5-c-switch__toggle";
  #createBtn = '.pf-v5-c-form__group:last-child button[type="submit"]';
  #cancelBtn = '.pf-v5-c-form__group:last-child button[type="button"]';
  #codeEditor = ".pf-v5-c-code-editor__code";

  #getClearBtn() {
    return cy.findByText("Clear");
  }

  fillRealmName(realmName: string) {
    cy.findByTestId(this.#realmNameInput).clear().type(realmName);

    return this;
  }

  fillCodeEditor() {
    cy.get(this.#codeEditor).click().type("clear this field");

    return this;
  }

  createRealm() {
    cy.get(this.#createBtn).click();

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
