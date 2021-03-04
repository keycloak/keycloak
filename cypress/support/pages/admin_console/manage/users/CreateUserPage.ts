export default class CreateUserPage {
  usernameInput: string;
  usersEmptyState: string;
  emptyStateCreateUserBtn: string;
  searchPgCreateUserBtn: string;
  saveBtn: string;
  cancelBtn: string;

  constructor() {
    this.usernameInput = "#kc-username";

    this.usersEmptyState = "[data-testid=empty-state]";
    this.emptyStateCreateUserBtn = "[data-testid=empty-primary-action]";
    this.searchPgCreateUserBtn = "[data-testid=create-new-user]";
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
    cy.wait(100);
    cy.get("body").then((body) => {
      if (body.find(this.usersEmptyState).length > 0) {
        cy.get(this.emptyStateCreateUserBtn).click();
      } else if (body.find("[data-testid=search-users-title]").length > 0) {
        cy.get(this.searchPgCreateUserBtn).click();
      } else {
        cy.get("[data-testid=add-user]").click();
      }
    });

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
