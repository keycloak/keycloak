export default class CreateUserPage {
  usernameInput: string;
  saveBtn: string;
  cancelBtn: string;

  constructor() {
    this.usernameInput = "#kc-username";

    this.saveBtn = "[data-testid=create-user]";
    this.cancelBtn = "[data-testid=cancel-create-user]";
  }

  //#region General Settings
  fillRealmRoleData(username: string) {
    cy.get(this.usernameInput).clear();

    if (username) {
      cy.get(this.usernameInput).type(username);
    }

    return this;
  }

  goToCreateUser() {
    cy.get("[data-testid=add-user").click();

    return this;
  }

  save() {
    cy.get(this.saveBtn).click();

    return this;
  }

  cancel() {
    cy.get(this.cancelBtn).click();

    return this;
  }
}
