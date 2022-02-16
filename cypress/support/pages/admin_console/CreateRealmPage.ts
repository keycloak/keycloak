export default class CreateRealmPage {
  private browseBtn = "#kc-realm-filename-browse-button";
  private clearBtn = ".pf-c-file-upload__file-select button:last-child";
  private realmFileNameInput = "#kc-realm-filename";
  private realmNameInput = "#kc-realm-name";
  private enabledSwitch =
    '[for="kc-realm-enabled-switch"] span.pf-c-switch__toggle';
  private createBtn = '.pf-c-form__group:last-child button[type="submit"]';
  private cancelBtn = '.pf-c-form__group:last-child button[type="button"]';

  fillRealmName(realmName: string) {
    cy.get(this.realmNameInput).clear().type(realmName);

    return this;
  }

  createRealm() {
    cy.get(this.createBtn).click();

    return this;
  }

  cancelRealmCreation() {
    cy.get(this.cancelBtn).click();

    return this;
  }
}
