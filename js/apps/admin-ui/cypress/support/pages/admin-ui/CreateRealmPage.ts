export default class CreateRealmPage {
  private clearBtn = ".pf-c-file-upload__file-select button:last-child";
  private modalClearBtn = "clear-button";
  private realmIdInput = "#kc-realm-id";
  private enabledSwitch =
    '[for="kc-realm-enabled-switch"] span.pf-c-switch__toggle';
  private createBtn = '.pf-c-form__group:last-child button[type="submit"]';
  private cancelBtn = '.pf-c-form__group:last-child button[type="button"]';
  private codeEditor = ".pf-c-code-editor__code";

  fillRealmName(realmName: string) {
    cy.get(this.realmIdInput).clear().type(realmName);

    return this;
  }

  fillCodeEditor() {
    cy.get(this.codeEditor).click().type("clear this field");

    return this;
  }

  createRealm() {
    cy.get(this.createBtn).click();

    return this;
  }

  disableRealm() {
    cy.get(this.enabledSwitch).click();

    return this;
  }

  cancelRealmCreation() {
    cy.get(this.cancelBtn).click();

    return this;
  }

  clearTextField() {
    cy.get(this.clearBtn).click();
    cy.findByTestId(this.modalClearBtn).click();

    return this;
  }

  verifyRealmIDFieldInvalid() {
    cy.get(this.realmIdInput)
      .next("div")
      .contains("Required field")
      .should("have.class", "pf-m-error");

    return this;
  }
}
